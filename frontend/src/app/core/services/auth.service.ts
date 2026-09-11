import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, throwError, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AccessTokenResponse,
  ChallengeVerificationRequest,
  LoginRequest,
  LoginResponse,
  LoginVerificationRequest,
  PasswordChangeRequest,
  RecoveryRequest,
  RecoveryVerificationRequest,
  TwoFactorRequest,
  UserResponse,
} from '../models/auth.models';
import {
  AdminPingResponse,
  ChallengeResponse,
  MessageResponse,
  PasswordChangeResponse,
} from '../models/api.models';
import { ApiErrorService } from './api-error.service';
import { AuthSessionService } from './auth-session.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly authUrl = `${environment.apiBaseUrl}/auth`;

  constructor(
    private readonly http: HttpClient,
    private readonly session: AuthSessionService,
    private readonly errors: ApiErrorService,
  ) {}

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.authUrl}/login`, request);
  }

  verifyLogin(request: LoginVerificationRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.authUrl}/login/verify`, request);
  }

  requestRecovery(request: RecoveryRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.authUrl}/password-recovery`, request);
  }

  verifyRecovery(request: RecoveryVerificationRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.authUrl}/password-recovery/verify`, request);
  }

  changePassword(request: PasswordChangeRequest): Observable<PasswordChangeResponse> {
    return this.http.post<PasswordChangeResponse>(`${this.authUrl}/password/change`, request);
  }

  requestTwoFactorChange(
    enabled: boolean,
    request: TwoFactorRequest,
  ): Observable<ChallengeResponse> {
    const action = enabled ? 'enable' : 'disable';
    return this.http.post<ChallengeResponse>(`${this.authUrl}/2fa/${action}`, request);
  }

  confirmTwoFactorChange(
    enabled: boolean,
    request: ChallengeVerificationRequest,
  ): Observable<MessageResponse> {
    const action = enabled ? 'enable' : 'disable';
    return this.http.post<MessageResponse>(`${this.authUrl}/2fa/${action}/verify`, request);
  }

  currentUser(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.authUrl}/me`);
  }

  loadCurrentUser(): Observable<UserResponse> {
    return this.currentUser().pipe(
      tap((user) => this.session.setUser(user)),
      catchError((error: unknown) => {
        if (this.errors.isAuthenticationFailure(error)) {
          this.session.clear();
        }
        return throwError(() => error);
      }),
    );
  }

  adminPing(): Observable<AdminPingResponse> {
    return this.http.get<AdminPingResponse>(`${environment.apiBaseUrl}/admin/ping`);
  }

  establishSession(response: AccessTokenResponse): Observable<UserResponse> {
    this.session.establish(response);
    return this.loadCurrentUser();
  }

  establishLogin(response: LoginResponse): Observable<UserResponse> {
    if (!response.accessToken || !response.tokenType) {
      return throwError(
        () => new Error('La respuesta de autenticación no contiene un token válido.'),
      );
    }

    return this.establishSession({
      accessToken: response.accessToken,
      tokenType: response.tokenType,
      expiresIn: response.expiresIn,
    });
  }

  logout(): void {
    this.session.clear();
  }
}
