import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CreateProductRequest,
  PageResponse,
  Product,
  ProductSearchParams,
  UpdateProductRequest,
} from '../models/store.models';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly url = `${environment.apiBaseUrl}/products`;

  constructor(private readonly http: HttpClient) {}

  search(params: ProductSearchParams): Observable<PageResponse<Product>> {
    let httpParams = new HttpParams();
    if (params.search?.trim()) httpParams = httpParams.set('search', params.search.trim());
    if (params.categoryId != null) httpParams = httpParams.set('categoryId', params.categoryId);
    if (params.active != null) httpParams = httpParams.set('active', params.active);
    if (params.stockStatus) httpParams = httpParams.set('stockStatus', params.stockStatus);
    httpParams = httpParams.set('page', params.page ?? 0);
    httpParams = httpParams.set('size', params.size ?? 10);
    httpParams = httpParams.set('sortBy', params.sortBy ?? 'name');
    httpParams = httpParams.set('direction', params.direction ?? 'asc');
    return this.http.get<PageResponse<Product>>(this.url, { params: httpParams });
  }

  getById(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.url}/${id}`);
  }

  create(request: CreateProductRequest): Observable<Product> {
    return this.http.post<Product>(this.url, request);
  }

  update(id: number, request: UpdateProductRequest): Observable<Product> {
    return this.http.put<Product>(`${this.url}/${id}`, request);
  }

  setActive(id: number, active: boolean): Observable<Product> {
    return this.http.patch<Product>(`${this.url}/${id}/status`, { active });
  }
}
