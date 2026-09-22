import { Injectable } from '@angular/core';
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import ExcelJS from 'exceljs';
import { saveAs } from 'file-saver';

import {
  ReportDashboardData,
  ReportExportType,
} from '../models/report.models';

@Injectable({
  providedIn: 'root',
})
export class ReportExportService {

  exportPdf(
    type: ReportExportType,
    data: ReportDashboardData,
    fromDate: string,
    toDate: string,
    dailyChartImage: string | null,
    topProductsChartImage: string | null,
  ): void {
    const doc = new jsPDF({
      orientation:
        type === 'categories'
          ? 'landscape'
          : 'portrait',
      unit: 'mm',
      format: 'a4',
    });

    this.addPdfHeader(
      doc,
      this.reportTitle(type),
      fromDate,
      toDate,
    );

    switch (type) {
  case 'sales':
    this.pdfSales(doc, data);
    break;

  case 'margin':
    this.pdfMargin(doc, data);
    break;

  case 'products-sold':
    this.pdfProductsSold(
      doc,
      data,
    );
    break;

  case 'resolved-alerts':
    this.pdfResolvedAlerts(
      doc,
      data,
    );
    break;

  case 'daily-sales':
    this.pdfDailySales(
      doc,
      data,
      dailyChartImage,
    );
    break;

  case 'top-products':
    this.pdfTopProducts(
      doc,
      data,
      topProductsChartImage,
    );
    break;

  case 'categories':
    this.pdfCategories(
      doc,
      data,
    );
    break;
}

this.addPdfFooters(doc);

doc.save(
  this.fileName(
    type,
    fromDate,
    toDate,
    'pdf',
  ),
);
  }

  async exportExcel(
    type: ReportExportType,
    data: ReportDashboardData,
    fromDate: string,
    toDate: string,
    dailyChartImage: string | null,
    topProductsChartImage: string | null,
  ): Promise<void> {
    const workbook =
      new ExcelJS.Workbook();

    workbook.creator =
      'Sistema Tienda La Esquina';

    workbook.created =
      new Date();

    const sheet =
      workbook.addWorksheet(
        this.sheetName(type),
        {
          views: [
            {
              showGridLines: false,
            },
          ],
        },
      );

    this.addExcelHeader(
      sheet,
      this.reportTitle(type),
      fromDate,
      toDate,
    );

    switch (type) {
      case 'sales':
        this.excelSales(sheet, data);
        break;

      case 'margin':
        this.excelMargin(sheet, data);
        break;

      case 'products-sold':
        this.excelProductsSold(sheet, data);
        break;

      case 'resolved-alerts':
        this.excelResolvedAlerts(
          sheet,
          data,
        );
        break;

      case 'daily-sales':
        this.excelDailySales(
          workbook,
          sheet,
          data,
          dailyChartImage,
        );
        break;

      case 'top-products':
        this.excelTopProducts(
          workbook,
          sheet,
          data,
          topProductsChartImage,
        );
        break;

      case 'categories':
        this.excelCategories(
          sheet,
          data,
        );
        break;
    }

    const buffer =
      await workbook.xlsx.writeBuffer();

    saveAs(
      new Blob(
        [buffer],
        {
          type:
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        },
      ),
      this.fileName(
        type,
        fromDate,
        toDate,
        'xlsx',
      ),
    );
  }

  private addPdfFooters(
  doc: jsPDF,
): void {
  const pages =
    doc.getNumberOfPages();

  for (
    let page = 1;
    page <= pages;
    page++
  ) {
    doc.setPage(page);

    const width =
      doc.internal.pageSize
        .getWidth();

    const height =
      doc.internal.pageSize
        .getHeight();

    doc.setDrawColor(
      222,
      222,
      216,
    );

    doc.setLineWidth(0.25);

    doc.line(
      15,
      height - 14,
      width - 15,
      height - 14,
    );

    doc.setFont(
      'helvetica',
      'normal',
    );

    doc.setFontSize(7.5);

    doc.setTextColor(
      125,
      128,
      126,
    );

    doc.text(
      'Sistema Tienda La Esquina · Reporte administrativo',
      15,
      height - 8,
    );

    doc.text(
      `Página ${page} de ${pages}`,
      width - 15,
      height - 8,
      {
        align: 'right',
      },
    );
  }
}

  private addPdfHeader(
    doc: jsPDF,
    title: string,
    fromDate: string,
    toDate: string,
  ): void {
    doc.setFillColor(18, 63, 52);
    doc.rect(0, 0, 210, 35, 'F');

    doc.setTextColor(255, 255, 255);
    doc.setFontSize(17);
    doc.setFont('helvetica', 'bold');
    doc.text(
      'Tienda La Esquina',
      15,
      15,
    );

    doc.setFontSize(9);
    doc.setFont(
      'helvetica',
      'normal',
    );
    doc.text(
      'Gestión inteligente de ventas e inventario',
      15,
      22,
    );

    doc.setFillColor(244, 197, 66);
    doc.roundedRect(
      160,
      9,
      35,
      16,
      3,
      3,
      'F',
    );

    doc.setTextColor(18, 63, 52);
    doc.setFontSize(9);
    doc.setFont(
      'helvetica',
      'bold',
    );
    doc.text(
      'REPORTE',
      177.5,
      19,
      {
        align: 'center',
      },
    );

    doc.setTextColor(30, 49, 43);
    doc.setFontSize(15);
    doc.text(
      title,
      15,
      48,
    );

    doc.setFontSize(9);
    doc.setFont(
      'helvetica',
      'normal',
    );
    doc.setTextColor(100, 105, 102);

    doc.text(
      `Período: ${this.prettyDate(fromDate)} - ${this.prettyDate(toDate)}`,
      15,
      56,
    );

    doc.text(
      `Generado: ${new Intl.DateTimeFormat(
        'es-GT',
        {
          dateStyle: 'medium',
          timeStyle: 'short',
        },
      ).format(new Date())}`,
      15,
      62,
    );
  }

  private pdfSales(
  doc: jsPDF,
  data: ReportDashboardData,
): void {
  this.pdfKpiReport(
    doc,
    'Total vendido',
    this.money(data.sales.totalSales),
    'Monto total generado por las ventas completadas durante el período.',
    data.sales.fromDate,
    data.sales.toDate,
    this.money(data.sales.previousPeriodTotal),
    data.sales.changePercentage,
    'Ventas registradas',
    String(data.sales.salesCount),
  );
}

  private pdfMargin(
  doc: jsPDF,
  data: ReportDashboardData,
): void {
  this.pdfKpiReport(
    doc,
    'Margen estimado',
    this.money(data.margin.estimatedMargin),
    'Ganancia estimada calculada con el costo histórico de los productos vendidos.',
    data.margin.fromDate,
    data.margin.toDate,
    this.money(data.margin.previousPeriodMargin),
    data.margin.changePercentage,
    'Días analizados',
    String(
      this.inclusiveDays(
        data.margin.fromDate,
        data.margin.toDate,
      ),
    ),
  );
}

  private pdfProductsSold(
  doc: jsPDF,
  data: ReportDashboardData,
): void {
  this.pdfKpiReport(
    doc,
    'Productos vendidos',
    this.numberText(
      data.productsSold.productsSold,
    ),
    'Cantidad total de unidades vendidas durante el período seleccionado.',
    data.productsSold.fromDate,
    data.productsSold.toDate,
    this.numberText(
      data.productsSold
        .previousPeriodProductsSold,
    ),
    data.productsSold.changePercentage,
    'Días analizados',
    String(
      this.inclusiveDays(
        data.productsSold.fromDate,
        data.productsSold.toDate,
      ),
    ),
  );
}

  private pdfResolvedAlerts(
  doc: jsPDF,
  data: ReportDashboardData,
): void {
  this.pdfKpiReport(
    doc,
    'Alertas resueltas',
    String(
      data.resolvedAlerts.resolvedAlerts,
    ),
    'Alertas de inventario que fueron atendidas durante el período seleccionado.',
    data.resolvedAlerts.fromDate,
    data.resolvedAlerts.toDate,
    String(
      data.resolvedAlerts
        .previousPeriodResolvedAlerts,
    ),
    data.resolvedAlerts.changePercentage,
    'Días analizados',
    String(
      this.inclusiveDays(
        data.resolvedAlerts.fromDate,
        data.resolvedAlerts.toDate,
      ),
    ),
  );
}

  private pdfDailySales(
    doc: jsPDF,
    data: ReportDashboardData,
    chartImage: string | null,
  ): void {
    if (chartImage) {
      doc.addImage(
        chartImage,
        'PNG',
        15,
        70,
        180,
        75,
      );
    }

    autoTable(doc, {
      startY:
        chartImage
          ? 153
          : 72,
      head: [[
        'Fecha',
        'Ventas',
        'Total',
      ]],
      body:
        data.dailySales.dailySales.map(
          (item) => [
            this.prettyDate(item.date),
            item.salesCount,
            this.money(
              item.totalSales,
            ),
          ],
        ),
      ...this.pdfTableTheme(),
    });
  }

  private pdfTopProducts(
    doc: jsPDF,
    data: ReportDashboardData,
    chartImage: string | null,
  ): void {
    if (chartImage) {
      doc.addImage(
        chartImage,
        'PNG',
        15,
        70,
        180,
        75,
      );
    }

    autoTable(doc, {
      startY:
        chartImage
          ? 153
          : 72,
      head: [[
        'Código',
        'Producto',
        'Cantidad',
        'Ventas',
      ]],
      body:
        data.topProducts.products.map(
          (item) => [
            item.productCode,
            item.productName,
            item.quantitySold,
            this.money(
              item.totalSales,
            ),
          ],
        ),
      ...this.pdfTableTheme(),
    });
  }

  private pdfCategories(
    doc: jsPDF,
    data: ReportDashboardData,
  ): void {
    autoTable(doc, {
      startY: 72,
      head: [[
        'Categoría',
        'Productos vendidos',
        'Ventas',
        'Margen',
        '% total',
      ]],
      body:
        data.categories.categories.map(
          (item) => [
            item.categoryName,
            item.productsSold,
            this.money(
              item.totalSales,
            ),
            this.money(
              item.estimatedMargin,
            ),
            `${item.percentageOfSales.toFixed(
              2,
            )}%`,
          ],
        ),
      foot: [[
        'TOTAL',
        data.productsSold.productsSold,
        this.money(
          data.sales.totalSales,
        ),
        this.money(
          data.margin
            .estimatedMargin,
        ),
        '100.00%',
      ]],
      ...this.pdfTableTheme(),
    });
  }

  private pdfKpiReport(
  doc: jsPDF,
  primaryLabel: string,
  primaryValue: string,
  description: string,
  fromDate: string,
  toDate: string,
  previousValue: string,
  changePercentage: number | null,
  extraLabel: string,
  extraValue: string,
): void {
  const margin = 15;
  const pageWidth =
    doc.internal.pageSize.getWidth();
  const contentWidth =
    pageWidth - margin * 2;

  doc.setFillColor(255, 249, 230);
  doc.setDrawColor(233, 218, 174);
  doc.setLineWidth(0.35);

  doc.roundedRect(
    margin,
    72,
    contentWidth,
    40,
    4,
    4,
    'FD',
  );

  doc.setFillColor(244, 197, 66);

  doc.roundedRect(
    margin,
    72,
    5,
    40,
    4,
    4,
    'F',
  );

  doc.setTextColor(103, 107, 103);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(9);

  doc.text(
    primaryLabel.toUpperCase(),
    27,
    83,
  );

  doc.setTextColor(18, 63, 52);
  doc.setFontSize(25);

  doc.text(
    primaryValue,
    27,
    98,
  );

  doc.setFont(
    'helvetica',
    'normal',
  );

  doc.setFontSize(8.5);
  doc.setTextColor(110, 115, 111);

  const descriptionLines =
    doc.splitTextToSize(
      description,
      140,
    );

  doc.text(
    descriptionLines,
    27,
    106,
  );

  doc.setTextColor(140, 104, 36);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(8);

  doc.text(
    'COMPARACIÓN',
    margin,
    125,
  );

  doc.setTextColor(22, 63, 52);
  doc.setFontSize(13);

  doc.text(
    'Resumen del período',
    margin,
    132,
  );

  const gap = 5;
  const cardWidth =
    (contentWidth - gap * 2) / 3;

  const cards = [
    {
      label: extraLabel,
      value: extraValue,
    },
    {
      label: 'Período anterior',
      value: previousValue,
    },
    {
      label: 'Variación',
      value:
        this.percent(
          changePercentage,
        ),
    },
  ];

  cards.forEach(
    (card, index) => {
      const x =
        margin +
        index *
          (cardWidth + gap);

      doc.setFillColor(
        248,
        248,
        244,
      );

      doc.setDrawColor(
        224,
        220,
        209,
      );

      doc.roundedRect(
        x,
        139,
        cardWidth,
        32,
        3,
        3,
        'FD',
      );

      doc.setTextColor(
        112,
        117,
        113,
      );

      doc.setFont(
        'helvetica',
        'bold',
      );

      doc.setFontSize(8);

      doc.text(
        card.label,
        x + 6,
        149,
      );

      doc.setTextColor(
        18,
        63,
        52,
      );

      doc.setFontSize(13);

      doc.text(
        card.value,
        x + 6,
        161,
      );
    },
  );

  const previousRange =
    this.previousPeriodRange(
      fromDate,
      toDate,
    );

  doc.setFillColor(
    239,
    246,
    242,
  );

  doc.setDrawColor(
    213,
    229,
    220,
  );

  doc.roundedRect(
    margin,
    180,
    contentWidth,
    21,
    3,
    3,
    'FD',
  );

  doc.setTextColor(
    23,
    99,
    79,
  );

  doc.setFont(
    'helvetica',
    'bold',
  );

  doc.setFontSize(8);

  doc.text(
    'PERÍODO ANTERIOR EQUIVALENTE',
    margin + 7,
    189,
  );

  doc.setFont(
    'helvetica',
    'normal',
  );

  doc.setTextColor(
    75,
    89,
    82,
  );

  doc.setFontSize(9);

  doc.text(
    previousRange,
    margin + 7,
    196,
  );
}

private inclusiveDays(
  fromDate: string,
  toDate: string,
): number {
  const from =
    this.isoDateToUtc(fromDate);

  const to =
    this.isoDateToUtc(toDate);

  const milliseconds =
    to.getTime() -
    from.getTime();

  return (
    Math.floor(
      milliseconds /
        86_400_000,
    ) + 1
  );
}

private previousPeriodRange(
  fromDate: string,
  toDate: string,
): string {
  const from =
    this.isoDateToUtc(fromDate);

  const days =
    this.inclusiveDays(
      fromDate,
      toDate,
    );

  const previousTo =
    new Date(from);

  previousTo.setUTCDate(
    previousTo.getUTCDate() - 1,
  );

  const previousFrom =
    new Date(previousTo);

  previousFrom.setUTCDate(
    previousFrom.getUTCDate() -
      (days - 1),
  );

  return (
    `${this.utcDateText(previousFrom)}` +
    ` - ` +
    `${this.utcDateText(previousTo)}`
  );
}

private isoDateToUtc(
  value: string,
): Date {
  const [
    year,
    month,
    day,
  ] = value
    .split('-')
    .map(Number);

  return new Date(
    Date.UTC(
      year,
      month - 1,
      day,
    ),
  );
}

private utcDateText(
  date: Date,
): string {
  return [
    String(
      date.getUTCDate(),
    ).padStart(2, '0'),

    String(
      date.getUTCMonth() + 1,
    ).padStart(2, '0'),

    date.getUTCFullYear(),
  ].join('/');
}

private numberText(
  value: number,
): string {
  return new Intl.NumberFormat(
    'es-GT',
    {
      maximumFractionDigits: 2,
    },
  ).format(value);
}

  private addExcelHeader(
    sheet: ExcelJS.Worksheet,
    title: string,
    fromDate: string,
    toDate: string,
  ): void {
    sheet.mergeCells('A1:E1');
    sheet.getCell('A1').value =
      'TIENDA LA ESQUINA';

    sheet.getCell('A1').font = {
      bold: true,
      size: 18,
      color: {
        argb: 'FFFFFFFF',
      },
    };

    sheet.getCell('A1').fill = {
      type: 'pattern',
      pattern: 'solid',
      fgColor: {
        argb: 'FF123F34',
      },
    };

    sheet.getCell('A1').alignment = {
      vertical: 'middle',
    };

    sheet.getRow(1).height = 30;

    sheet.mergeCells('A2:E2');
    sheet.getCell('A2').value =
      title;

    sheet.getCell('A2').font = {
      bold: true,
      size: 14,
      color: {
        argb: 'FF123F34',
      },
    };

    sheet.mergeCells('A3:E3');
    sheet.getCell('A3').value =
      `Período: ${this.prettyDate(
        fromDate,
      )} - ${this.prettyDate(
        toDate,
      )}`;

    sheet.mergeCells('A4:E4');
    sheet.getCell('A4').value =
      `Generado: ${new Intl.DateTimeFormat(
        'es-GT',
        {
          dateStyle: 'medium',
          timeStyle: 'short',
        },
      ).format(new Date())}`;
  }

  private excelSales(
    sheet: ExcelJS.Worksheet,
    data: ReportDashboardData,
  ): void {
    this.addExcelKpiReport(
      sheet,
      'TOTAL VENDIDO',
      data.sales.totalSales,
      'currency',
      'Monto total generado por las ventas completadas durante el período.',
      'Ventas registradas',
      data.sales.salesCount,
      'number',
      data.sales.previousPeriodTotal,
      'currency',
      data.sales.changePercentage,
      data.sales.fromDate,
      data.sales.toDate,
    );
  }

  private excelMargin(
    sheet: ExcelJS.Worksheet,
    data: ReportDashboardData,
  ): void {
    this.addExcelKpiReport(
      sheet,
      'MARGEN ESTIMADO',
      data.margin.estimatedMargin,
      'currency',
      'Ganancia estimada calculada con el costo histórico de los productos vendidos.',
      'Días analizados',
      this.inclusiveDays(
        data.margin.fromDate,
        data.margin.toDate,
      ),
      'number',
      data.margin.previousPeriodMargin,
      'currency',
      data.margin.changePercentage,
      data.margin.fromDate,
      data.margin.toDate,
    );
  }

  private excelProductsSold(
    sheet: ExcelJS.Worksheet,
    data: ReportDashboardData,
  ): void {
    this.addExcelKpiReport(
      sheet,
      'PRODUCTOS VENDIDOS',
      data.productsSold.productsSold,
      'number',
      'Cantidad total de unidades vendidas durante el período seleccionado.',
      'Días analizados',
      this.inclusiveDays(
        data.productsSold.fromDate,
        data.productsSold.toDate,
      ),
      'number',
      data.productsSold.previousPeriodProductsSold,
      'number',
      data.productsSold.changePercentage,
      data.productsSold.fromDate,
      data.productsSold.toDate,
    );
  }

  private excelResolvedAlerts(
    sheet: ExcelJS.Worksheet,
    data: ReportDashboardData,
  ): void {
    this.addExcelKpiReport(
      sheet,
      'ALERTAS RESUELTAS',
      data.resolvedAlerts.resolvedAlerts,
      'number',
      'Alertas de inventario que fueron atendidas durante el período seleccionado.',
      'Días analizados',
      this.inclusiveDays(
        data.resolvedAlerts.fromDate,
        data.resolvedAlerts.toDate,
      ),
      'number',
      data.resolvedAlerts.previousPeriodResolvedAlerts,
      'number',
      data.resolvedAlerts.changePercentage,
      data.resolvedAlerts.fromDate,
      data.resolvedAlerts.toDate,
    );
  }

  private addExcelKpiReport(
    sheet: ExcelJS.Worksheet,
    primaryLabel: string,
    primaryValue: number,
    primaryType: 'currency' | 'number',
    description: string,
    extraLabel: string,
    extraValue: number,
    extraType: 'currency' | 'number',
    previousValue: number,
    previousType: 'currency' | 'number',
    changePercentage: number | null,
    fromDate: string,
    toDate: string,
  ): void {
    const darkGreen = 'FF123F34';
    const green = 'FF17634F';
    const yellow = 'FFF4C542';
    const lightYellow = 'FFFFF8E1';
    const lightGreen = 'FFEEF6F2';
    const lightCard = 'FFFAF9F5';
    const borderColor = 'FFE1DDD3';
    const muted = 'FF6E746F';

    sheet.unMergeCells('A1:E1');
    sheet.unMergeCells('A2:E2');
    sheet.unMergeCells('A3:E3');
    sheet.unMergeCells('A4:E4');
    sheet.mergeCells('A1:F1');
    sheet.mergeCells('A2:F2');
    sheet.mergeCells('A3:F3');
    sheet.mergeCells('A4:F4');

    sheet.columns = [
      { width: 18 },
      { width: 18 },
      { width: 18 },
      { width: 18 },
      { width: 18 },
      { width: 18 },
    ];

    sheet.views = [
      {
        state: 'frozen',
        ySplit: 4,
        showGridLines: false,
      },
    ];

    sheet.pageSetup = {
      orientation: 'landscape',
      fitToPage: true,
      fitToWidth: 1,
      fitToHeight: 1,
      paperSize: 9,
      margins: {
        left: 0.35,
        right: 0.35,
        top: 0.5,
        bottom: 0.5,
        header: 0.2,
        footer: 0.2,
      },
    };

    sheet.pageSetup.printArea = 'A1:F18';
    sheet.headerFooter.oddFooter =
      '&LSistema Tienda La Esquina - Reporte administrativo&RPage &P de &N';

    sheet.mergeCells('A6:F6');
    sheet.getCell('A6').value = primaryLabel;
    sheet.getCell('A6').font = {
      bold: true,
      size: 10,
      color: { argb: 'FF7B651E' },
    };
    sheet.getCell('A6').alignment = {
      vertical: 'middle',
      horizontal: 'left',
    };

    sheet.mergeCells('A7:F8');
    sheet.getCell('A7').value = primaryValue;
    sheet.getCell('A7').font = {
      bold: true,
      size: 24,
      color: { argb: darkGreen },
    };
    sheet.getCell('A7').alignment = {
      vertical: 'middle',
      horizontal: 'left',
    };
    this.applyExcelNumberFormat(
      sheet.getCell('A7'),
      primaryType,
    );

    sheet.mergeCells('A9:F9');
    sheet.getCell('A9').value = description;
    sheet.getCell('A9').font = {
      size: 10,
      color: { argb: muted },
    };
    sheet.getCell('A9').alignment = {
      vertical: 'middle',
      horizontal: 'left',
      wrapText: true,
    };

    for (let row = 6; row <= 9; row++) {
      for (let col = 1; col <= 6; col++) {
        const cell = sheet.getCell(row, col);
        cell.fill = {
          type: 'pattern',
          pattern: 'solid',
          fgColor: { argb: lightYellow },
        };
        cell.border = {
          top: { style: 'thin', color: { argb: 'FFE9D9A0' } },
          bottom: { style: 'thin', color: { argb: 'FFE9D9A0' } },
          left: { style: 'thin', color: { argb: 'FFE9D9A0' } },
          right: { style: 'thin', color: { argb: 'FFE9D9A0' } },
        };
      }
    }

    sheet.getCell('A6').fill = {
      type: 'pattern',
      pattern: 'solid',
      fgColor: { argb: yellow },
    };
    sheet.getRow(6).height = 22;
    sheet.getRow(7).height = 24;
    sheet.getRow(8).height = 24;
    sheet.getRow(9).height = 30;

    sheet.mergeCells('A11:F11');
    sheet.getCell('A11').value =
      'COMPARACIÓN · RESUMEN DEL PERÍODO';
    sheet.getCell('A11').font = {
      bold: true,
      size: 11,
      color: { argb: 'FFFFFFFF' },
    };
    sheet.getCell('A11').fill = {
      type: 'pattern',
      pattern: 'solid',
      fgColor: { argb: green },
    };
    sheet.getCell('A11').alignment = {
      vertical: 'middle',
      horizontal: 'left',
    };
    sheet.getRow(11).height = 24;

    this.createExcelMetricCard(
      sheet,
      'A12:B14',
      extraLabel,
      extraValue,
      extraType,
      lightCard,
      borderColor,
      darkGreen,
      muted,
    );

    this.createExcelMetricCard(
      sheet,
      'C12:D14',
      'Período anterior',
      previousValue,
      previousType,
      lightCard,
      borderColor,
      darkGreen,
      muted,
    );

    this.createExcelMetricCard(
      sheet,
      'E12:F14',
      'Variación',
      changePercentage == null
        ? 'Sin comparación'
        : changePercentage / 100,
      changePercentage == null
        ? 'text'
        : 'percent',
      lightCard,
      borderColor,
      darkGreen,
      muted,
    );

    sheet.mergeCells('A16:F16');
    sheet.getCell('A16').value =
      'PERÍODO ANTERIOR EQUIVALENTE';
    sheet.getCell('A16').font = {
      bold: true,
      size: 9,
      color: { argb: green },
    };
    sheet.getCell('A16').alignment = {
      vertical: 'middle',
      horizontal: 'left',
    };

    sheet.mergeCells('A17:F18');
    sheet.getCell('A17').value =
      this.previousPeriodRange(
        fromDate,
        toDate,
      );
    sheet.getCell('A17').font = {
      bold: true,
      size: 12,
      color: { argb: darkGreen },
    };
    sheet.getCell('A17').alignment = {
      vertical: 'middle',
      horizontal: 'left',
    };

    for (let row = 16; row <= 18; row++) {
      for (let col = 1; col <= 6; col++) {
        const cell = sheet.getCell(row, col);
        cell.fill = {
          type: 'pattern',
          pattern: 'solid',
          fgColor: { argb: lightGreen },
        };
        cell.border = {
          top: { style: 'thin', color: { argb: 'FFD5E5DC' } },
          bottom: { style: 'thin', color: { argb: 'FFD5E5DC' } },
          left: { style: 'thin', color: { argb: 'FFD5E5DC' } },
          right: { style: 'thin', color: { argb: 'FFD5E5DC' } },
        };
      }
    }

    sheet.getRow(16).height = 22;
    sheet.getRow(17).height = 22;
    sheet.getRow(18).height = 22;
  }

  private createExcelMetricCard(
    sheet: ExcelJS.Worksheet,
    range: string,
    label: string,
    value: number | string,
    type: 'currency' | 'number' | 'percent' | 'text',
    fillColor: string,
    borderColor: string,
    valueColor: string,
    labelColor: string,
  ): void {
    const [start, end] = range.split(':');
    const startMatch = start.match(/^([A-Z]+)(\d+)$/);
    const endMatch = end.match(/^([A-Z]+)(\d+)$/);

    if (!startMatch || !endMatch) {
      return;
    }

    const startCol = this.excelColumnNumber(startMatch[1]);
    const endCol = this.excelColumnNumber(endMatch[1]);
    const startRow = Number(startMatch[2]);
    const endRow = Number(endMatch[2]);

    sheet.mergeCells(
      startRow,
      startCol,
      startRow,
      endCol,
    );

    if (endRow > startRow) {
      sheet.mergeCells(
        startRow + 1,
        startCol,
        endRow,
        endCol,
      );
    }

    const labelCell = sheet.getCell(startRow, startCol);
    labelCell.value = label;
    labelCell.font = {
      bold: true,
      size: 9,
      color: { argb: labelColor },
    };
    labelCell.alignment = {
      vertical: 'middle',
      horizontal: 'left',
      indent: 1,
    };

    const valueCell = sheet.getCell(startRow + 1, startCol);
    valueCell.value = value;
    valueCell.font = {
      bold: true,
      size: type === 'text' ? 12 : 15,
      color: { argb: valueColor },
    };
    valueCell.alignment = {
      vertical: 'middle',
      horizontal: 'left',
      wrapText: true,
      indent: 1,
    };
    this.applyExcelNumberFormat(valueCell, type);

    for (let row = startRow; row <= endRow; row++) {
      for (let col = startCol; col <= endCol; col++) {
        const cardCell = sheet.getCell(row, col);
        cardCell.fill = {
          type: 'pattern',
          pattern: 'solid',
          fgColor: { argb: fillColor },
        };
        cardCell.border = {
          top: { style: 'thin', color: { argb: borderColor } },
          bottom: { style: 'thin', color: { argb: borderColor } },
          left: { style: 'thin', color: { argb: borderColor } },
          right: { style: 'thin', color: { argb: borderColor } },
        };
      }
    }

    sheet.getRow(startRow).height = 22;
    if (endRow > startRow) {
      sheet.getRow(startRow + 1).height = 30;
      sheet.getRow(endRow).height = 30;
    }
  }

  private applyExcelNumberFormat(
  cell: ExcelJS.Cell,
  type: 'currency' | 'number' | 'percent' | 'text',
): void {
  if (type === 'currency') {
    cell.numFmt = '"Q" #,##0.00';
  } else if (type === 'percent') {
    cell.numFmt = '0.00%';
  } else if (type === 'number') {
    cell.numFmt = 'General';
  }
}

  private excelDisplayValue(
    value: number,
    type: 'currency' | 'number' | 'percent' | 'text',
  ): string {
    if (type === 'currency') {
      return new Intl.NumberFormat('es-GT', {
        style: 'currency',
        currency: 'GTQ',
        minimumFractionDigits: 2,
      }).format(value);
    }

    if (type === 'percent') {
      return new Intl.NumberFormat('es-GT', {
        style: 'percent',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }).format(value);
    }

    return new Intl.NumberFormat('es-GT', {
      maximumFractionDigits: 2,
    }).format(value);
  }

  private excelColumnNumber(
    letters: string,
  ): number {
    return letters
      .split('')
      .reduce(
        (result, char) =>
          result * 26 +
          char.charCodeAt(0) -
          64,
        0,
      );
  }

  private excelDailySales(
    workbook: ExcelJS.Workbook,
    sheet: ExcelJS.Worksheet,
    data: ReportDashboardData,
    chartImage: string | null,
  ): void {
    const startRow = 6;

    sheet.getRow(startRow).values = [
      'Fecha',
      'Ventas',
      'Total',
    ];

    this.styleHeaderRow(
      sheet.getRow(startRow),
    );

    data.dailySales.dailySales.forEach(
      (item, index) => {
        const row =
          sheet.getRow(
            startRow + 1 + index,
          );

        row.values = [
          this.prettyDate(
            item.date,
          ),
          item.salesCount,
          item.totalSales,
        ];

        row.getCell(3).numFmt =
          '"Q" #,##0.00';
      },

      
    );

    sheet.columns = [
      { width: 18 },
      { width: 16 },
      { width: 20 },
      { width: 16 },
      { width: 16 },
    ];

    if (chartImage) {
      this.addExcelImage(
        workbook,
        sheet,
        chartImage,
        'E6:L23',
      );
    }

    const totalRowNumber =
  startRow +
  data.dailySales.dailySales.length +
  1;

const totalSalesCount =
  data.dailySales.dailySales.reduce(
    (sum, item) =>
      sum + item.salesCount,
    0,
  );

const totalSalesAmount =
  data.dailySales.dailySales.reduce(
    (sum, item) =>
      sum + item.totalSales,
    0,
  );

const totalRow =
  sheet.getRow(totalRowNumber);

totalRow.values = [
  'TOTAL',
  totalSalesCount,
  totalSalesAmount,
];

totalRow.font = {
  bold: true,
  color: {
    argb: 'FF123F34',
  },
};

totalRow.fill = {
  type: 'pattern',
  pattern: 'solid',
  fgColor: {
    argb: 'FFFFF0BC',
  },
};

totalRow.getCell(3).numFmt =
  '"Q" #,##0.00';

totalRow.eachCell((cell) => {
  cell.border = {
    top: {
      style: 'thin',
      color: {
        argb: 'FFD8C785',
      },
    },
    bottom: {
      style: 'thin',
      color: {
        argb: 'FFD8C785',
      },
    },
  };
});
  }

  

  private excelTopProducts(
    workbook: ExcelJS.Workbook,
    sheet: ExcelJS.Worksheet,
    data: ReportDashboardData,
    chartImage: string | null,
  ): void {
    const startRow = 6;

    sheet.getRow(startRow).values = [
      'Código',
      'Producto',
      'Cantidad',
      'Ventas',
    ];

    this.styleHeaderRow(
      sheet.getRow(startRow),
    );

    data.topProducts.products.forEach(
      (item, index) => {
        const row =
          sheet.getRow(
            startRow + 1 + index,
          );

        row.values = [
          item.productCode,
          item.productName,
          item.quantitySold,
          item.totalSales,
        ];

        row.getCell(4).numFmt =
          '"Q" #,##0.00';
      },
    );

    sheet.columns = [
      { width: 18 },
      { width: 45 },
      { width: 16 },
      { width: 18 },
      { width: 14 },
      { width: 14 },
      { width: 14 },
      { width: 14 },
      { width: 14 },
      { width: 14 },
      { width: 14 },
      { width: 14 },
    ];

    if (chartImage) {
      this.addExcelImage(
        workbook,
        sheet,
        chartImage,
        'F6:M23',
      );
    }
  }

  private excelCategories(
    sheet: ExcelJS.Worksheet,
    data: ReportDashboardData,
  ): void {
    const startRow = 6;

    sheet.getRow(startRow).values = [
      'Categoría',
      'Productos vendidos',
      'Total en ventas',
      'Margen estimado',
      '% del total',
    ];

    this.styleHeaderRow(
      sheet.getRow(startRow),
    );

    data.categories.categories.forEach(
      (item, index) => {
        const row =
          sheet.getRow(
            startRow + 1 + index,
          );

        row.values = [
          item.categoryName,
          item.productsSold,
          item.totalSales,
          item.estimatedMargin,
          item.percentageOfSales / 100,
        ];

        row.getCell(3).numFmt =
          '"Q" #,##0.00';

        row.getCell(4).numFmt =
          '"Q" #,##0.00';

        row.getCell(5).numFmt =
          '0.00%';
      },
    );

    const totalRow =
      sheet.getRow(
        startRow +
          data.categories.categories
            .length +
          1,
      );

    totalRow.values = [
      'TOTAL',
      data.productsSold.productsSold,
      data.sales.totalSales,
      data.margin.estimatedMargin,
      1,
    ];

    totalRow.font = {
      bold: true,
    };

    totalRow.fill = {
      type: 'pattern',
      pattern: 'solid',
      fgColor: {
        argb: 'FFFFF0BC',
      },
    };

    totalRow.getCell(3).numFmt =
      '"Q" #,##0.00';

    totalRow.getCell(4).numFmt =
      '"Q" #,##0.00';

    totalRow.getCell(5).numFmt =
      '0.00%';

    sheet.columns = [
      { width: 32 },
      { width: 22 },
      { width: 20 },
      { width: 20 },
      { width: 16 },
    ];
  }

  private styleHeaderRow(
    row: ExcelJS.Row,
  ): void {
    row.font = {
      bold: true,
      color: {
        argb: 'FFFFFFFF',
      },
    };

    row.fill = {
      type: 'pattern',
      pattern: 'solid',
      fgColor: {
        argb: 'FF17634F',
      },
    };

    row.height = 22;

    row.eachCell((cell) => {
      cell.alignment = {
        vertical: 'middle',
      };

      cell.border = {
        bottom: {
          style: 'thin',
          color: {
            argb: 'FFD3D8D5',
          },
        },
      };
    });
  }

 private addExcelImage(
  workbook: ExcelJS.Workbook,
  sheet: ExcelJS.Worksheet,
  dataUrl: string,
  range: string,
): void {
  if (!dataUrl.startsWith('data:image/png;base64,')) {
    return;
  }

  const imageId = workbook.addImage({
    base64: dataUrl,
    extension: 'png',
  });

  sheet.addImage(
    imageId,
    range,
  );
}

  private pdfTableTheme(): any {
  return {
    theme: 'grid',

    margin: {
      bottom: 20,
    },

    headStyles: {
      fillColor: [
        23,
        99,
        79,
      ],
      textColor: 255,
      fontStyle: 'bold',
    },

    footStyles: {
      fillColor: [
        244,
        197,
        66,
      ],
      textColor: [
        18,
        63,
        52,
      ],
      fontStyle: 'bold',
    },

    styles: {
      fontSize: 9,
      cellPadding: 3,
    },

    alternateRowStyles: {
      fillColor: [
        249,
        247,
        240,
      ],
    },
  };
}

  private reportTitle(
    type: ReportExportType,
  ): string {
    const titles: Record<
      ReportExportType,
      string
    > = {
      sales: 'Ventas del período',
      margin: 'Margen estimado',
      'products-sold':
        'Productos vendidos',
      'resolved-alerts':
        'Alertas resueltas',
      'daily-sales':
        'Ventas por día',
      'top-products':
        'Productos más vendidos',
      categories:
        'Resumen por categoría',
    };

    return titles[type];
  }

  private sheetName(
    type: ReportExportType,
  ): string {
    const names: Record<
      ReportExportType,
      string
    > = {
      sales: 'Ventas',
      margin: 'Margen',
      'products-sold':
        'Productos vendidos',
      'resolved-alerts':
        'Alertas',
      'daily-sales':
        'Ventas por día',
      'top-products':
        'Top productos',
      categories:
        'Categorías',
    };

    return names[type];
  }

  private fileName(
    type: ReportExportType,
    fromDate: string,
    toDate: string,
    extension: string,
  ): string {
    const clean =
      this.reportTitle(type)
        .normalize('NFD')
        .replace(
          /[\u0300-\u036f]/g,
          '',
        )
        .replace(
          /[^a-zA-Z0-9]+/g,
          '-',
        )
        .replace(
          /^-|-$/g,
          '',
        );

    return (
      `Tienda-La-Esquina_` +
      `${clean}_` +
      `${fromDate}_${toDate}.` +
      extension
    );
  }

  private money(
    value: number,
  ): string {
    return new Intl.NumberFormat(
      'es-GT',
      {
        style: 'currency',
        currency: 'GTQ',
      },
    ).format(value);
  }

  private percent(
    value: number | null,
  ): string {
    if (value == null) {
      return 'Sin comparación';
    }

    return `${value.toFixed(2)}%`;
  }

  private prettyDate(
    value: string,
  ): string {
    const [
      year,
      month,
      day,
    ] = value.split('-');

    return `${day}/${month}/${year}`;
  }

}