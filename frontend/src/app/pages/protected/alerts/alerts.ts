import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
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
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);

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

  readonly filters = this.fb.group({
    type: [''],
    readState: ['unread'],
  });

  ngOnInit(): void {
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

  applyFilters(): void {
    this.page.set(0);
    this.load(0);
  }

  markAsRead(notification: StockNotification): void {
    this.notificationsApi.markAsRead(notification.id).subscribe({
      next: () => {
        this.successMessage.set('Notificación marcada como leída.');
        this.load();
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
}
