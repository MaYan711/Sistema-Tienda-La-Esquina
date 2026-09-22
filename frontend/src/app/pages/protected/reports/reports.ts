import {
  Component,
  DestroyRef,
  ElementRef,
  OnInit,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import {
  Chart,
  ChartConfiguration,
  registerables,
} from 'chart.js';

import {
  ReportDashboardData,
  ReportExportType,
} from '../../../core/models/report.models';
import { ReportService } from '../../../core/services/report.service';
import { ReportExportService } from '../../../core/services/report-export.service';
import { ApiErrorService } from '../../../core/services/api-error.service';

Chart.register(...registerables);

type ReportViewType =
  | 'summary'
  | 'sales'
  | 'margin'
  | 'products-sold'
  | 'resolved-alerts'
  | 'daily-sales'
  | 'top-products'
  | 'categories';

@Component({
  selector: 'app-reports-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
  ],
  templateUrl: './reports.html',
  styleUrl: './reports.scss',
})
export class ReportsPageComponent implements OnInit {

  private readonly reportsApi = inject(ReportService);
  private readonly exportService = inject(ReportExportService);
  private readonly errors = inject(ApiErrorService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  @ViewChild('dailySalesCanvas')
  private dailySalesCanvas?: ElementRef<HTMLCanvasElement>;

  @ViewChild('topProductsCanvas')
  private topProductsCanvas?: ElementRef<HTMLCanvasElement>;

  private dailySalesChart?: Chart;
  private topProductsChart?: Chart;

  readonly loading = signal(false);
  readonly exporting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly data = signal<ReportDashboardData | null>(null);

  readonly generatedFrom = signal('');
  readonly generatedTo = signal('');

  readonly filters = this.fb.nonNullable.group({
    fromDate: [this.firstDayOfCurrentMonth()],
    toDate: [this.today()],
    reportType: ['summary' as ReportViewType],
  });

  readonly exportForm = this.fb.nonNullable.group({
    report: ['sales' as ReportExportType],
  });

  readonly reportOptions: ReadonlyArray<{
    value: ReportViewType;
    label: string;
  }> = [
    { value: 'summary', label: 'Resumen general' },
    { value: 'sales', label: 'Ventas por período' },
    { value: 'margin', label: 'Margen de ventas' },
    { value: 'products-sold', label: 'Productos vendidos' },
    { value: 'resolved-alerts', label: 'Alertas resueltas' },
    { value: 'daily-sales', label: 'Ventas por día' },
    { value: 'top-products', label: 'Productos más vendidos' },
    { value: 'categories', label: 'Resumen por categoría' },
  ];

  readonly exportOptions: ReadonlyArray<{
    value: ReportExportType;
    label: string;
  }> = [
    { value: 'sales', label: 'Ventas del período' },
    { value: 'margin', label: 'Margen estimado' },
    { value: 'products-sold', label: 'Productos vendidos' },
    { value: 'resolved-alerts', label: 'Alertas resueltas' },
    { value: 'daily-sales', label: 'Ventas por día' },
    { value: 'top-products', label: 'Productos más vendidos' },
    { value: 'categories', label: 'Resumen por categoría' },
  ];

  ngOnInit(): void {
    this.generateReport();
  }

  generateReport(): void {
    const {
  fromDate,
  toDate,
  reportType,
} = this.filters.getRawValue();
    

    this.errorMessage.set(null);

    if (!fromDate || !toDate) {
      this.errorMessage.set(
        'Debes seleccionar una fecha inicial y una fecha final.',
      );
      return;
    }

    if (fromDate > toDate) {
      this.errorMessage.set(
        'La fecha inicial no puede ser posterior a la fecha final.',
      );
      return;
    }

    this.loading.set(true);

    this.reportsApi
      .getDashboard(fromDate, toDate)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (response) => {
          this.data.set(response);
          this.generatedFrom.set(fromDate);
          this.generatedTo.set(toDate);

          if (reportType !== 'summary') {
                this.exportForm.controls.report.setValue(
                    reportType as ReportExportType,
                );
                }

          window.setTimeout(() => {
            this.renderCharts();
          });
        },
        error: (error: unknown) => {
          this.errorMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  show(section: ReportViewType): boolean {
    const selected = this.filters.controls.reportType.value;

    if (selected === 'summary') {
      return true;
    }

    if (selected === section) {
      return true;
    }

    if (
      selected === 'sales' &&
      section === 'daily-sales'
    ) {
      return true;
    }

    if (
      selected === 'products-sold' &&
      section === 'top-products'
    ) {
      return true;
    }

    return false;
  }

  exportPdf(): void {
    const data = this.data();

    if (!data) {
      return;
    }

    this.exporting.set(true);

    try {
      this.exportService.exportPdf(
        this.exportForm.controls.report.value,
        data,
        this.generatedFrom(),
        this.generatedTo(),
        this.getDailyChartImage(),
        this.getTopProductsChartImage(),
      );
    } finally {
      this.exporting.set(false);
    }
  }

  async exportExcel(): Promise<void> {
  const data = this.data();

  if (!data) {
    return;
  }

  this.exporting.set(true);
  this.errorMessage.set(null);

  try {
    await this.exportService.exportExcel(
      this.exportForm.controls.report.value,
      data,
      this.generatedFrom(),
      this.generatedTo(),
      this.getDailyChartImage(),
      this.getTopProductsChartImage(),
    );
  } catch (error) {
    console.error(
      'Error al generar Excel:',
      error,
    );

    this.errorMessage.set(
      'No fue posible generar el archivo Excel. Intenta nuevamente.',
    );
  } finally {
    this.exporting.set(false);
  }
}

  money(value: number | null | undefined): string {
    return new Intl.NumberFormat('es-GT', {
      style: 'currency',
      currency: 'GTQ',
      minimumFractionDigits: 2,
    }).format(value ?? 0);
  }

  number(value: number | null | undefined): string {
    return new Intl.NumberFormat('es-GT', {
      maximumFractionDigits: 2,
    }).format(value ?? 0);
  }

  percentage(
    value: number | null | undefined,
  ): string {
    if (value == null) {
      return 'Sin comparación';
    }

    const prefix = value > 0 ? '+' : '';

    return `${prefix}${this.number(value)}%`;
  }

  percentageClass(
    value: number | null | undefined,
  ): string {
    if (value == null || value === 0) {
      return 'neutral';
    }

    return value > 0
      ? 'positive'
      : 'negative';
  }

  todayLabel(): string {
    return new Intl.DateTimeFormat(
      'es-GT',
      {
        weekday: 'long',
        day: '2-digit',
        month: 'long',
        year: 'numeric',
      },
    ).format(new Date());
  }

  formatDate(value: string): string {
    if (!value) {
      return '';
    }

    const [year, month, day] = value.split('-');

    return `${day}/${month}/${year}`;
  }

  private renderCharts(): void {
    const current = this.data();

    if (!current) {
      return;
    }

    this.dailySalesChart?.destroy();
    this.topProductsChart?.destroy();

    if (this.dailySalesCanvas) {
      const configuration: ChartConfiguration = {
        type: 'line',
        data: {
          labels: current.dailySales.dailySales.map(
            (item) => this.formatDate(item.date),
          ),
          datasets: [
            {
              label: 'Ventas',
              data: current.dailySales.dailySales.map(
                (item) => item.totalSales,
              ),
              borderColor: '#15624f',
              backgroundColor: 'rgba(21, 98, 79, 0.12)',
              pointBackgroundColor: '#15624f',
              pointBorderColor: '#15624f',
              tension: 0.35,
              fill: true,
              borderWidth: 3,
              pointRadius: 3,
              pointHoverRadius: 5,
            },
          ],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              display: false,
            },
            tooltip: {
              callbacks: {
                label: (context) =>
                  this.money(
                    Number(context.raw ?? 0),
                  ),
              },
            },
          },
          scales: {
            x: {
              grid: {
                display: false,
              },
              ticks: {
                maxTicksLimit: 8,
              },
            },
            y: {
              beginAtZero: true,
              ticks: {
                callback: (value) =>
                  `Q${Number(value).toFixed(0)}`,
              },
            },
          },
        },
      };

      this.dailySalesChart = new Chart(
        this.dailySalesCanvas.nativeElement,
        configuration,
      );
    }

    if (this.topProductsCanvas) {
      const configuration: ChartConfiguration = {
        type: 'bar',
        data: {
          labels: current.topProducts.products.map(
            (item) => item.productName,
          ),
          datasets: [
            {
              label: 'Cantidad vendida',
              data: current.topProducts.products.map(
                (item) => item.quantitySold,
              ),
              backgroundColor: '#f4c542',
              borderColor: '#dba91f',
              borderWidth: 1,
              borderRadius: 8,
            },
          ],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          indexAxis: 'y',
          plugins: {
            legend: {
              display: false,
            },
          },
          scales: {
            x: {
              beginAtZero: true,
              grid: {
                color: 'rgba(0, 0, 0, 0.06)',
              },
            },
            y: {
              grid: {
                display: false,
              },
              ticks: {
                callback: (_value, index) => {
                  const label =
                    current.topProducts.products[index]
                      ?.productName ?? '';

                  return label.length > 28
                    ? `${label.substring(0, 28)}...`
                    : label;
                },
              },
            },
          },
        },
      };

      this.topProductsChart = new Chart(
        this.topProductsCanvas.nativeElement,
        configuration,
      );
    }
  }

  private getDailyChartImage(): string | null {
    return this.dailySalesCanvas?.nativeElement
      .toDataURL('image/png', 1)
      ?? null;
  }

  private getTopProductsChartImage(): string | null {
    return this.topProductsCanvas?.nativeElement
      .toDataURL('image/png', 1)
      ?? null;
  }

  currentExportLabel(): string {
  const selected =
    this.exportForm.controls.report.value;

  return (
    this.exportOptions.find(
      (option) =>
        option.value === selected,
    )?.label ??
    'Reporte'
  );
}

  private today(): string {
    const date = new Date();

    return [
      date.getFullYear(),
      String(date.getMonth() + 1).padStart(2, '0'),
      String(date.getDate()).padStart(2, '0'),
    ].join('-');
  }

  private firstDayOfCurrentMonth(): string {
    const date = new Date();

    return [
      date.getFullYear(),
      String(date.getMonth() + 1).padStart(2, '0'),
      '01',
    ].join('-');
  }
}