import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MeasurementUnit, ProductCategory } from '../models/store.models';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly url = `${environment.apiBaseUrl}/catalog`;

  constructor(private readonly http: HttpClient) {}

  categories(): Observable<ProductCategory[]> {
    return this.http.get<ProductCategory[]>(`${this.url}/categories`);
  }

  measurementUnits(): Observable<MeasurementUnit[]> {
    return this.http.get<MeasurementUnit[]>(`${this.url}/measurement-units`);
  }
}
