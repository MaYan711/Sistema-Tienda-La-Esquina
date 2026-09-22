import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CategoryReport,
  DailySalesReport,
  MarginReport,
  ProductsSoldReport,
  ReportDashboardData,
  ResolvedAlertsReport,
  SalesReport,
  TopProductsReport,
} from '../models/report.models';

@Injectable({
  providedIn: 'root',
})
export class ReportService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/reports`;

  getSales(
    fromDate: string,
    toDate: string,
  ): Observable<SalesReport> {
    return this.http.get<SalesReport>(
      `${this.url}/sales`,
      {
        params: this.dateParams(fromDate, toDate),
      },
    );
  }

  getMargin(
    fromDate: string,
    toDate: string,
  ): Observable<MarginReport> {
    return this.http.get<MarginReport>(
      `${this.url}/margin`,
      {
        params: this.dateParams(fromDate, toDate),
      },
    );
  }

  getProductsSold(
    fromDate: string,
    toDate: string,
  ): Observable<ProductsSoldReport> {
    return this.http.get<ProductsSoldReport>(
      `${this.url}/products-sold`,
      {
        params: this.dateParams(fromDate, toDate),
      },
    );
  }

  getResolvedAlerts(
    fromDate: string,
    toDate: string,
  ): Observable<ResolvedAlertsReport> {
    return this.http.get<ResolvedAlertsReport>(
      `${this.url}/resolved-alerts`,
      {
        params: this.dateParams(fromDate, toDate),
      },
    );
  }

  getDailySales(
    fromDate: string,
    toDate: string,
  ): Observable<DailySalesReport> {
    return this.http.get<DailySalesReport>(
      `${this.url}/daily-sales`,
      {
        params: this.dateParams(fromDate, toDate),
      },
    );
  }

  getTopProducts(
    fromDate: string,
    toDate: string,
  ): Observable<TopProductsReport> {
    return this.http.get<TopProductsReport>(
      `${this.url}/top-products`,
      {
        params: this.dateParams(fromDate, toDate),
      },
    );
  }

  getCategories(
    fromDate: string,
    toDate: string,
  ): Observable<CategoryReport> {
    return this.http.get<CategoryReport>(
      `${this.url}/categories`,
      {
        params: this.dateParams(fromDate, toDate),
      },
    );
  }

  getDashboard(
    fromDate: string,
    toDate: string,
  ): Observable<ReportDashboardData> {
    return forkJoin({
      sales: this.getSales(fromDate, toDate),
      margin: this.getMargin(fromDate, toDate),
      productsSold: this.getProductsSold(
        fromDate,
        toDate,
      ),
      resolvedAlerts: this.getResolvedAlerts(
        fromDate,
        toDate,
      ),
      dailySales: this.getDailySales(
        fromDate,
        toDate,
      ),
      topProducts: this.getTopProducts(
        fromDate,
        toDate,
      ),
      categories: this.getCategories(
        fromDate,
        toDate,
      ),
    });
  }

  private dateParams(
    fromDate: string,
    toDate: string,
  ): HttpParams {
    return new HttpParams()
      .set('fromDate', fromDate)
      .set('toDate', toDate);
  }
}