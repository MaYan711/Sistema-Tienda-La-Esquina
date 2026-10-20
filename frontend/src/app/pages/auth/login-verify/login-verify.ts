import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { InputTextModule } from 'primeng/inputtext';
import { finalize, switchMap } from 'rxjs';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { AuthService } from '../../../core/services/auth.service';
import { FieldErrorComponent } from '../../../shared/components/field-error/field-error';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';

@Component({
  selector: 'app-login-verify-page',
  imports: [
    FieldErrorComponent,
    FormFeedbackComponent,
    InputTextModule,
    ReactiveFormsModule,
    RouterLink,
    TranslocoPipe,
  ],
  templateUrl: './login-verify.html',
  styleUrl: './login-verify.scss',
})
export class LoginVerifyPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly errors = inject(ApiErrorService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly submitted = signal(false);
  readonly isSubmitting = signal(false);
  readonly serverMessage = signal<string | null>(null);
  readonly challengeId = this.route.snapshot.queryParamMap.get('challengeId');
  readonly form = this.formBuilder.nonNullable.group({
    otp: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  submit(): void {
    this.submitted.set(true);
    this.serverMessage.set(null);
    if (!this.challengeId) {
      this.serverMessage.set('El desafío de inicio de sesión no está disponible.');
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.auth
      .verifyLogin({ challengeId: this.challengeId, otp: this.form.controls.otp.value })
      .pipe(
        switchMap((response) => this.auth.establishLogin(response)),
        finalize(() => this.isSubmitting.set(false)),
      )
      .subscribe({
        next: () => void this.router.navigateByUrl(this.targetAfterLogin()),
        error: (error: unknown) => this.serverMessage.set(this.errors.getMessage(error)),
      });
  }

  private targetAfterLogin(): string {
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    return returnUrl?.startsWith('/') ? returnUrl : '/app/dashboard';
  }
}
