import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CreateStockEntryRequest,
  PageResponse,
  StockEntry,
  StockEntrySearchParams,
} from '../models/store.models';

@Injectable({ providedIn: 'root' })
export class StockEntryService {
  private readonly url = `${environment.apiBaseUrl}/stock-entries`;

  constructor(private readonly http: HttpClient) {}

  search(params: StockEntrySearchParams): Observable<PageResponse<StockEntry>> {
    let httpParams = new HttpParams();

    if (params.supplierId != null) {
      httpParams = httpParams.set('supplierId', params.supplierId);
    }

    if (params.fromDate) {
      httpParams = httpParams.set('fromDate', params.fromDate);
    }

    if (params.toDate) {
      httpParams = httpParams.set('toDate', params.toDate);
    }

    if (params.reference?.trim()) {
      httpParams = httpParams.set('reference', params.reference.trim());
    }

    httpParams = httpParams.set('page', params.page ?? 0);
    httpParams = httpParams.set('size', params.size ?? 10);
    httpParams = httpParams.set('sortBy', params.sortBy ?? 'entryDate');
    httpParams = httpParams.set('direction', params.direction ?? 'desc');

    return this.http.get<PageResponse<StockEntry>>(this.url, {
      params: httpParams,
    });
  }

  getById(id: number): Observable<StockEntry> {
    return this.http.get<StockEntry>(`${this.url}/${id}`);
  }

  create(request: CreateStockEntryRequest): Observable<StockEntry> {
    return this.http.post<StockEntry>(this.url, request);
  }
}