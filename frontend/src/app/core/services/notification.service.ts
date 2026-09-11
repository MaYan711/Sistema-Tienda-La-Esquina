import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { NotificationType, PageResponse, StockNotification } from '../models/store.models';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly url = `${environment.apiBaseUrl}/notifications`;

  constructor(private readonly http: HttpClient) {}

  search(filters: {
    productId?: number | null;
    type?: NotificationType | '';
    isRead?: boolean | null;
    page?: number;
    size?: number;
    sortBy?: string;
    direction?: 'asc' | 'desc';
  }): Observable<PageResponse<StockNotification>> {
    let params = new HttpParams()
      .set('page', filters.page ?? 0)
      .set('size', filters.size ?? 10)
      .set('sortBy', filters.sortBy ?? 'createdAt')
      .set('direction', filters.direction ?? 'desc');
    if (filters.productId != null) params = params.set('productId', filters.productId);
    if (filters.type) params = params.set('type', filters.type);
    if (filters.isRead != null) params = params.set('isRead', filters.isRead);
    return this.http.get<PageResponse<StockNotification>>(this.url, { params });
  }

  markAsRead(id: number): Observable<StockNotification> {
    return this.http.patch<StockNotification>(`${this.url}/${id}/read`, {});
  }
}
