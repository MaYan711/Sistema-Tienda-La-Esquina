import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { InputTextModule } from 'primeng/inputtext';
import { finalize, map, of, switchMap } from 'rxjs';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { AuthService } from '../../../core/services/auth.service';
import { FieldErrorComponent } from '../../../shared/components/field-error/field-error';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';

@Component({
  selector: 'app-login-page',
  imports: [
    FieldErrorComponent,
    FormFeedbackComponent,
    InputTextModule,
    ReactiveFormsModule,
    RouterLink,
    TranslocoPipe,
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class LoginPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly errors = inject(ApiErrorService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly submitted = signal(false);
  readonly isSubmitting = signal(false);
  readonly serverMessage = signal<string | null>(null);
  readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  submit(): void {
    this.submitted.set(true);
    this.serverMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.auth
      .login(this.form.getRawValue())
      .pipe(
        switchMap((response) =>
          response.requiresTwoFactor
            ? of(response)
            : this.auth.establishLogin(response).pipe(map(() => response)),
        ),
        finalize(() => this.isSubmitting.set(false)),
      )
      .subscribe({
        next: (response) => {
          if (response.requiresTwoFactor) {
            if (!response.challengeId) {
              this.serverMessage.set('No se recibió un desafío OTP válido. Intenta nuevamente.');
              return;
            }
            void this.router.navigate(['/auth/login/verify'], {
              queryParams: {
                challengeId: response.challengeId,
                returnUrl: this.targetAfterLogin(),
              },
            });
            return;
          }

          void this.router.navigateByUrl(this.targetAfterLogin());
        },
        error: (error: unknown) => this.serverMessage.set(this.errors.getMessage(error)),
      });
  }

  private targetAfterLogin(): string {
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    return returnUrl?.startsWith('/') ? returnUrl : '/app/dashboard';
  }
}
