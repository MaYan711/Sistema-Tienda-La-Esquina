import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CreateSupplierRequest,
  PageResponse,
  Supplier,
  SupplierSearchParams,
  UpdateSupplierRequest,
} from '../models/store.models';

@Injectable({ providedIn: 'root' })
export class SupplierService {
  private readonly url = `${environment.apiBaseUrl}/suppliers`;

  constructor(private readonly http: HttpClient) {}

  search(params: SupplierSearchParams): Observable<PageResponse<Supplier>> {
    let httpParams = new HttpParams();

    if (params.search?.trim()) {
      httpParams = httpParams.set('search', params.search.trim());
    }

    if (params.active != null) {
      httpParams = httpParams.set('active', params.active);
    }

    httpParams = httpParams.set('page', params.page ?? 0);
    httpParams = httpParams.set('size', params.size ?? 10);
    httpParams = httpParams.set('sortBy', params.sortBy ?? 'name');
    httpParams = httpParams.set('direction', params.direction ?? 'asc');

    return this.http.get<PageResponse<Supplier>>(this.url, {
      params: httpParams,
    });
  }

  getById(id: number): Observable<Supplier> {
    return this.http.get<Supplier>(`${this.url}/${id}`);
  }

  create(request: CreateSupplierRequest): Observable<Supplier> {
    return this.http.post<Supplier>(this.url, request);
  }

  update(id: number, request: UpdateSupplierRequest): Observable<Supplier> {
    return this.http.put<Supplier>(`${this.url}/${id}`, request);
  }

  setActive(id: number, active: boolean): Observable<Supplier> {
    return this.http.patch<Supplier>(`${this.url}/${id}/status`, {
      active,
    });
  }
}