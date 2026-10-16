import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  OperatingHourException,
  OperatingHourExceptionRequest,
  OperatingHoursConfiguration,
  UpdateOperatingHoursRequest,
} from '../models/store.models';

@Injectable({ providedIn: 'root' })
export class OperatingHoursService {
  private readonly url = `${environment.apiBaseUrl}/operating-hours`;

  constructor(private readonly http: HttpClient) {}

  getConfiguration(): Observable<OperatingHoursConfiguration> {
    return this.http.get<OperatingHoursConfiguration>(
      `${this.url}/configuration`,
    );
  }

  updateWeekly(
    request: UpdateOperatingHoursRequest,
  ): Observable<OperatingHoursConfiguration> {
    return this.http.put<OperatingHoursConfiguration>(
      `${this.url}/weekly`,
      request,
    );
  }

  createException(
    request: OperatingHourExceptionRequest,
  ): Observable<OperatingHourException> {
    return this.http.post<OperatingHourException>(
      `${this.url}/exceptions`,
      request,
    );
  }

  updateException(
    id: number,
    request: OperatingHourExceptionRequest,
  ): Observable<OperatingHourException> {
    return this.http.put<OperatingHourException>(
      `${this.url}/exceptions/${id}`,
      request,
    );
  }

  deleteException(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/exceptions/${id}`);
  }
}
