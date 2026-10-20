import { Injectable } from '@angular/core';
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';

import { SaleReceipt } from '../models/store.models';

@Injectable({ providedIn: 'root' })
export class SaleReceiptExportService {
  downloadPdf(receipt: SaleReceipt): void {
    const doc = new jsPDF({
      orientation: 'portrait',
      unit: 'mm',
      format: 'a4',
    });

    const pageWidth = doc.internal.pageSize.getWidth();

    doc.setFillColor(18, 63, 52);
    doc.rect(0, 0, pageWidth, 35, 'F');

    doc.setTextColor(255, 255, 255);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(18);
    doc.text(receipt.storeName, 15, 15);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    doc.text('Comprobante interno de compra', 15, 22);

    doc.setFillColor(244, 197, 66);
    doc.roundedRect(pageWidth - 52, 9, 37, 16, 3, 3, 'F');
    doc.setTextColor(18, 63, 52);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(9);
    doc.text('COMPROBANTE', pageWidth - 33.5, 19, { align: 'center' });

    doc.setTextColor(30, 49, 43);
    doc.setFontSize(15);
    doc.text(receipt.documentTitle, 15, 48);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    doc.setTextColor(90, 96, 92);
    doc.text(`Venta: ${receipt.saleNumber}`, 15, 56);
    doc.text(`Fecha: ${this.formatDateTime(receipt.saleDate)}`, 15, 62);
    doc.text(`Atendido por: ${receipt.soldByEmail}`, 15, 68);
    doc.text(`Método de pago: Efectivo`, 15, 74);

    autoTable(doc, {
      startY: 82,
      head: [['Producto', 'Cant.', 'Precio', 'Subtotal']],
      body: receipt.items.map((item) => [
        `${item.productCode} - ${item.productName}`,
        this.quantity(item.quantity),
        this.money(item.unitPrice),
        this.money(item.lineTotal),
      ]),
      theme: 'grid',
      headStyles: {
        fillColor: [23, 99, 79],
        textColor: 255,
        fontStyle: 'bold',
      },
      alternateRowStyles: {
        fillColor: [249, 247, 240],
      },
      styles: {
        fontSize: 8.5,
        cellPadding: 3,
      },
      columnStyles: {
        0: { cellWidth: 92 },
        1: { halign: 'right', cellWidth: 20 },
        2: { halign: 'right', cellWidth: 32 },
        3: { halign: 'right', cellWidth: 32 },
      },
      margin: { bottom: 45 },
    });

    const lastTable = (doc as jsPDF & {
      lastAutoTable?: { finalY: number };
    }).lastAutoTable;

    const pageHeight = doc.internal.pageSize.getHeight();
    let summaryY = (lastTable?.finalY ?? 90) + 8;

    if (summaryY > pageHeight - 65) {
      doc.addPage();
      summaryY = 24;
    }

    doc.setFillColor(255, 249, 230);
    doc.setDrawColor(233, 218, 174);
    doc.roundedRect(pageWidth - 82, summaryY, 67, 42, 3, 3, 'FD');

    const labelX = pageWidth - 76;
    const valueX = pageWidth - 20;
    let y = summaryY + 9;

    this.summaryLine(doc, 'Subtotal', this.money(receipt.subtotalAmount), labelX, valueX, y);
    y += 8;
    this.summaryLine(doc, 'Total', this.money(receipt.totalAmount), labelX, valueX, y, true);
    y += 8;
    this.summaryLine(doc, 'Efectivo', this.money(receipt.cashReceived), labelX, valueX, y);
    y += 8;
    this.summaryLine(doc, 'Cambio', this.money(receipt.changeAmount), labelX, valueX, y);

    if (receipt.notes) {
      doc.setTextColor(90, 96, 92);
      doc.setFont('helvetica', 'bold');
      doc.setFontSize(8);
      doc.text('Observaciones', 15, summaryY + 8);
      doc.setFont('helvetica', 'normal');
      doc.text(doc.splitTextToSize(receipt.notes, 105), 15, summaryY + 15);
    }

    this.addFooters(doc);

    doc.save(`${receipt.saleNumber}_comprobante.pdf`);
  }

  print(receipt: SaleReceipt): boolean {
    const popup = window.open('', '_blank', 'width=820,height=900');

    if (!popup) {
      return false;
    }

    popup.document.open();
    popup.document.write(this.printableHtml(receipt));
    popup.document.close();
    popup.focus();

    window.setTimeout(() => {
      popup.print();
    }, 250);

    return true;
  }

  private printableHtml(receipt: SaleReceipt): string {
    const rows = receipt.items
      .map(
        (item) => `
          <tr>
            <td><strong>${this.escapeHtml(item.productName)}</strong><br><small>${this.escapeHtml(item.productCode)}</small></td>
            <td class="right">${this.quantity(item.quantity)}</td>
            <td class="right">${this.money(item.unitPrice)}</td>
            <td class="right"><strong>${this.money(item.lineTotal)}</strong></td>
          </tr>`,
      )
      .join('');

    const notes = receipt.notes
      ? `<div class="notes"><strong>Observaciones</strong><p>${this.escapeHtml(receipt.notes)}</p></div>`
      : '';

    return `<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <title>${this.escapeHtml(receipt.saleNumber)} - Comprobante</title>
  <style>
    *{box-sizing:border-box} body{margin:0;background:#f4f1e8;color:#173f34;font-family:Arial,sans-serif}
    .receipt{width:760px;max-width:100%;margin:24px auto;background:#fff;padding:34px;border:1px solid #ded8ca}
    .brand{display:flex;justify-content:space-between;gap:20px;border-bottom:4px solid #f4c542;padding-bottom:18px;margin-bottom:22px}
    h1{font-size:24px;margin:0 0 4px}.muted{color:#6d756f;font-size:13px}.badge{background:#f4c542;border-radius:10px;padding:10px 14px;font-weight:800;height:max-content}
    .meta{display:grid;grid-template-columns:1fr 1fr;gap:8px 18px;margin:18px 0 24px;font-size:13px}.meta strong{display:block;color:#6d756f;font-size:11px;text-transform:uppercase;margin-bottom:3px}
    table{width:100%;border-collapse:collapse} th{background:#17634f;color:#fff;text-align:left;padding:10px;font-size:12px} td{padding:10px;border-bottom:1px solid #e8e3d8;font-size:13px}.right{text-align:right}
    .totals{width:310px;margin:22px 0 0 auto;background:#fff8df;border:1px solid #ead89c;border-radius:10px;padding:14px}.total-line{display:flex;justify-content:space-between;padding:5px 0}.total-line.main{font-size:18px;font-weight:800;border-top:1px solid #dbc980;margin-top:5px;padding-top:10px}
    .notes{margin-top:22px;background:#f5f8f5;border-radius:8px;padding:12px;font-size:13px}.notes p{margin:5px 0 0}
    .footer{text-align:center;color:#7b817d;font-size:11px;margin-top:28px;border-top:1px solid #e5e1d8;padding-top:15px}
    @media print{body{background:#fff}.receipt{border:0;margin:0;width:100%;padding:18px}.no-print{display:none}}
  </style>
</head>
<body>
  <main class="receipt">
    <header class="brand"><div><h1>${this.escapeHtml(receipt.storeName)}</h1><div class="muted">Comprobante interno de compra</div></div><div class="badge">COMPROBANTE</div></header>
    <section class="meta">
      <div><strong>Venta</strong>${this.escapeHtml(receipt.saleNumber)}</div>
      <div><strong>Fecha</strong>${this.escapeHtml(this.formatDateTime(receipt.saleDate))}</div>
      <div><strong>Atendido por</strong>${this.escapeHtml(receipt.soldByEmail)}</div>
      <div><strong>Método de pago</strong>Efectivo</div>
    </section>
    <table><thead><tr><th>Producto</th><th class="right">Cant.</th><th class="right">Precio</th><th class="right">Subtotal</th></tr></thead><tbody>${rows}</tbody></table>
    <section class="totals">
      <div class="total-line"><span>Subtotal</span><strong>${this.money(receipt.subtotalAmount)}</strong></div>
      <div class="total-line main"><span>Total</span><span>${this.money(receipt.totalAmount)}</span></div>
      <div class="total-line"><span>Efectivo</span><strong>${this.money(receipt.cashReceived)}</strong></div>
      <div class="total-line"><span>Cambio</span><strong>${this.money(receipt.changeAmount)}</strong></div>
    </section>
    ${notes}
    <footer class="footer">Gracias por su compra.<br>Comprobante interno; no sustituye una factura fiscal.</footer>
  </main>
</body>
</html>`;
  }

  private addFooters(doc: jsPDF): void {
    const pageCount = doc.getNumberOfPages();

    for (let page = 1; page <= pageCount; page++) {
      doc.setPage(page);

      const pageWidth = doc.internal.pageSize.getWidth();
      const pageHeight = doc.internal.pageSize.getHeight();

      doc.setDrawColor(220, 220, 214);
      doc.line(15, pageHeight - 18, pageWidth - 15, pageHeight - 18);
      doc.setFont('helvetica', 'normal');
      doc.setFontSize(7.5);
      doc.setTextColor(120, 124, 121);
      doc.text('Gracias por su compra.', 15, pageHeight - 11);
      doc.text(`Página ${page} de ${pageCount}`, pageWidth / 2, pageHeight - 11, {
        align: 'center',
      });
      doc.text(
        'Comprobante interno; no sustituye una factura fiscal.',
        pageWidth - 15,
        pageHeight - 11,
        { align: 'right' },
      );
    }
  }

  private summaryLine(
    doc: jsPDF,
    label: string,
    value: string,
    labelX: number,
    valueX: number,
    y: number,
    main = false,
  ): void {
    doc.setFont('helvetica', main ? 'bold' : 'normal');
    doc.setFontSize(main ? 10 : 8.5);
    doc.setTextColor(55, 68, 61);
    doc.text(label, labelX, y);
    doc.setTextColor(18, 63, 52);
    doc.text(value, valueX, y, { align: 'right' });
  }

  private money(value: number): string {
    return `Q ${Number(value).toLocaleString('es-GT', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })}`;
  }

  private quantity(value: number): string {
    return Number(value).toLocaleString('es-GT', {
      maximumFractionDigits: 3,
    });
  }

  private formatDateTime(value: string): string {
    return new Intl.DateTimeFormat('es-GT', {
      dateStyle: 'medium',
      timeStyle: 'short',
      timeZone: 'America/Guatemala',
    }).format(new Date(value));
  }

  private escapeHtml(value: string): string {
    return value
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#039;');
  }
}
