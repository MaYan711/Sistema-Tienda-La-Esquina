import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Observable, finalize, forkJoin, map, of, switchMap } from 'rxjs';
import { NotificationType, StockNotification } from '../../../core/models/store.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { NotificationService } from '../../../core/services/notification.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

@Component({
  selector: 'app-alerts-page',
  imports: [DecimalPipe, FormFeedbackComponent, PageHeadingComponent, ReactiveFormsModule],
  templateUrl: './alerts.html',
  styleUrl: './alerts.scss',
})
export class AlertsPageComponent implements OnInit {
  private readonly notificationsApi = inject(NotificationService);
  private readonly errors = inject(ApiErrorService);
  private readonly fb = inject(FormBuilder);

  readonly notifications = signal<StockNotification[]>([]);
  readonly loading = signal(false);
  readonly summaryLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);

  readonly lowStockCount = signal(0);
  readonly outOfStockCount = signal(0);
  readonly reviewedTodayCount = signal(0);

  private readonly guatemalaFormatter = new Intl.DateTimeFormat('es-GT', {
    timeZone: 'America/Guatemala',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: true,
  });

  private readonly guatemalaDayFormatter = new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/Guatemala',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });

  readonly filters = this.fb.group({
    type: [''],
    readState: ['unread'],
  });

  ngOnInit(): void {
    this.loadSummary();
    this.load();
  }

  load(page = this.page()): void {
    const values = this.filters.getRawValue();
    const isRead = values.readState === 'all' ? null : values.readState === 'read';
    this.loading.set(true);
    this.errorMessage.set(null);
    this.notificationsApi
      .search({
        type: (values.type ?? '') as NotificationType | '',
        isRead,
        page,
        size: this.size(),
        sortBy: 'createdAt',
        direction: 'desc',
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => {
          this.notifications.set(response.content ?? []);
          this.page.set(response.page?.number ?? response.number ?? page);
          this.size.set(response.page?.size ?? response.size ?? this.size());
          this.totalElements.set(response.page?.totalElements ?? response.totalElements ?? response.content?.length ?? 0);
          this.totalPages.set(response.page?.totalPages ?? response.totalPages ?? 1);
        },
        error: (error: unknown) => this.errorMessage.set(this.errors.getMessage(error)),
      });
  }

  loadSummary(): void {
    this.summaryLoading.set(true);

    forkJoin({
      lowStock: this.notificationsApi.search({
        type: 'LOW_STOCK',
        isRead: false,
        page: 0,
        size: 1,
        sortBy: 'createdAt',
        direction: 'desc',
      }),
      outOfStock: this.notificationsApi.search({
        type: 'OUT_OF_STOCK',
        isRead: false,
        page: 0,
        size: 1,
        sortBy: 'createdAt',
        direction: 'desc',
      }),
      reviewedToday: this.loadReviewedTodayCount(),
    })
      .pipe(finalize(() => this.summaryLoading.set(false)))
      .subscribe({
        next: ({ lowStock, outOfStock, reviewedToday }) => {
          this.lowStockCount.set(lowStock.page?.totalElements ?? lowStock.totalElements ?? lowStock.content?.length ?? 0);
          this.outOfStockCount.set(outOfStock.page?.totalElements ?? outOfStock.totalElements ?? outOfStock.content?.length ?? 0);
          this.reviewedTodayCount.set(reviewedToday);
        },
        error: (error: unknown) => this.errorMessage.set(this.errors.getMessage(error)),
      });
  }

  applyFilters(): void {
    this.page.set(0);
    this.load(0);
  }

  markAsRead(notification: StockNotification): void {
    this.notificationsApi.markAsRead(notification.id).subscribe({
      next: () => {
        this.successMessage.set('Notificación marcada como leída.');
        this.load();
        this.loadSummary();
      },
      error: (error: unknown) => this.errorMessage.set(this.errors.getMessage(error)),
    });
  }

  previousPage(): void {
    if (this.page() > 0) this.load(this.page() - 1);
  }

  nextPage(): void {
    if (this.page() + 1 < this.totalPages()) this.load(this.page() + 1);
  }

  formatGuatemalaDate(value: string | null): string {
    if (!value) return '—';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : this.guatemalaFormatter.format(date);
  }

  private loadReviewedTodayCount(): Observable<number> {
    const loadPage = (page: number) =>
      this.notificationsApi.search({
        isRead: true,
        page,
        size: 100,
        sortBy: 'readAt',
        direction: 'desc',
      });

    return loadPage(0).pipe(
      switchMap((firstPage) => {
        const firstContent = firstPage.content ?? [];
        const totalPages = firstPage.page?.totalPages ?? firstPage.totalPages ?? 1;

        if (totalPages <= 1) {
          return of(firstContent);
        }

        const remainingRequests = Array.from({ length: totalPages - 1 }, (_, index) => loadPage(index + 1));
        return forkJoin(remainingRequests).pipe(
          map((responses) => [
            ...firstContent,
            ...responses.flatMap((response) => response.content ?? []),
          ]),
        );
      }),
      map((notifications) => notifications.filter((notification) => this.isReviewedToday(notification.readAt)).length),
    );
  }

  private isReviewedToday(value: string | null): boolean {
    if (!value) return false;
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return false;
    return this.guatemalaDayFormatter.format(date) === this.guatemalaDayFormatter.format(new Date());
  }
}
