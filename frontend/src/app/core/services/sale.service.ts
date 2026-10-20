import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CreateSaleRequest,
  OperatingHoursStatus,
  PageResponse,
  Sale,
  SaleDetail,
  SaleReceipt,
  SaleSearchParams,
} from '../models/store.models';

@Injectable({ providedIn: 'root' })
export class SaleService {
  private readonly url = `${environment.apiBaseUrl}/sales`;

  constructor(private readonly http: HttpClient) {}

  search(params: SaleSearchParams): Observable<PageResponse<Sale>> {
    let httpParams = new HttpParams();

    if (params.fromDate) {
      httpParams = httpParams.set('fromDate', params.fromDate);
    }

    if (params.toDate) {
      httpParams = httpParams.set('toDate', params.toDate);
    }

    if (params.reference?.trim()) {
      httpParams = httpParams.set(
        'reference',
        params.reference.trim(),
      );
    }

    httpParams = httpParams.set('page', params.page ?? 0);
    httpParams = httpParams.set('size', params.size ?? 10);
    httpParams = httpParams.set(
      'sortBy',
      params.sortBy ?? 'saleDate',
    );
    httpParams = httpParams.set(
      'direction',
      params.direction ?? 'desc',
    );

    return this.http.get<PageResponse<Sale>>(this.url, {
      params: httpParams,
    });
  }

  getOperatingHours(): Observable<OperatingHoursStatus> {
    return this.http.get<OperatingHoursStatus>(
      `${this.url}/operating-hours`,
    );
  }

  getById(id: number): Observable<Sale> {
    return this.http.get<Sale>(`${this.url}/${id}`);
  }

  getDetail(id: number): Observable<SaleDetail> {
    return this.http.get<SaleDetail>(`${this.url}/${id}/detail`);
  }

  getReceipt(id: number): Observable<SaleReceipt> {
    return this.http.get<SaleReceipt>(`${this.url}/${id}/receipt`);
  }

  create(request: CreateSaleRequest): Observable<Sale> {
    return this.http.post<Sale>(this.url, request);
  }
}