import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  InventoryAdjustmentRequest,
  InventoryAdjustmentResponse,
  InventoryMovement,
  InventoryMovementType,
  PageResponse,
} from '../models/store.models';

@Injectable({ providedIn: 'root' })
export class InventoryService {
  private readonly url = `${environment.apiBaseUrl}/inventory`;

  constructor(private readonly http: HttpClient) {}

  adjustStock(request: InventoryAdjustmentRequest): Observable<InventoryAdjustmentResponse> {
    return this.http.post<InventoryAdjustmentResponse>(`${this.url}/adjustments`, request);
  }

  movements(filters: {
    productId?: number | null;
    movementType?: InventoryMovementType | '';
    page?: number;
    size?: number;
    sortBy?: string;
    direction?: 'asc' | 'desc';
  }): Observable<PageResponse<InventoryMovement>> {
    let params = new HttpParams()
      .set('page', filters.page ?? 0)
      .set('size', filters.size ?? 10)
      .set('sortBy', filters.sortBy ?? 'createdAt')
      .set('direction', filters.direction ?? 'desc');
    if (filters.productId != null) params = params.set('productId', filters.productId);
    if (filters.movementType) params = params.set('movementType', filters.movementType);
    return this.http.get<PageResponse<InventoryMovement>>(`${this.url}/movements`, { params });
  }
}
