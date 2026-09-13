import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CreateSaleRequest,
  PageResponse,
  Sale,
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

  getById(id: number): Observable<Sale> {
    return this.http.get<Sale>(`${this.url}/${id}`);
  }

  create(request: CreateSaleRequest): Observable<Sale> {
    return this.http.post<Sale>(this.url, request);
  }
}