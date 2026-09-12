import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CreateUserRequest,
  ManagedUser,
  UpdateUserRequest,
  UserPageResponse,
  UserSearchParams,
} from '../models/user.models';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly url = `${environment.apiBaseUrl}/users`;

  constructor(private readonly http: HttpClient) {}

  search(params: UserSearchParams): Observable<UserPageResponse<ManagedUser>> {
    let httpParams = new HttpParams();
    if (params.search?.trim()) httpParams = httpParams.set('search', params.search.trim());
    if (params.role) httpParams = httpParams.set('role', params.role);
    if (params.enabled != null) httpParams = httpParams.set('enabled', params.enabled);
    httpParams = httpParams.set('page', params.page ?? 0);
    httpParams = httpParams.set('size', params.size ?? 10);
    httpParams = httpParams.set('sortBy', params.sortBy ?? 'email');
    httpParams = httpParams.set('direction', params.direction ?? 'asc');
    return this.http.get<UserPageResponse<ManagedUser>>(this.url, { params: httpParams });
  }

  getById(id: number): Observable<ManagedUser> {
    return this.http.get<ManagedUser>(`${this.url}/${id}`);
  }

  create(request: CreateUserRequest): Observable<ManagedUser> {
    return this.http.post<ManagedUser>(this.url, request);
  }

  update(id: number, request: UpdateUserRequest): Observable<ManagedUser> {
    return this.http.put<ManagedUser>(`${this.url}/${id}`, request);
  }

  setEnabled(id: number, enabled: boolean): Observable<ManagedUser> {
    return this.http.patch<ManagedUser>(`${this.url}/${id}/status`, { enabled });
  }
}
