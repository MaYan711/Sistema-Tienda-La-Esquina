import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { InputTextModule } from 'primeng/inputtext';
import { finalize, map, switchMap } from 'rxjs';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { AuthService } from '../../../core/services/auth.service';
import { AuthSessionService } from '../../../core/services/auth-session.service';
import { FieldErrorComponent } from '../../../shared/components/field-error/field-error';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

type TwoFactorMode = 'enable' | 'disable';
const PASSWORD_PATTERN = /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d).+$/;

@Component({
  selector: 'app-security-page',
  imports: [
    FieldErrorComponent,
    FormFeedbackComponent,
    InputTextModule,
    PageHeadingComponent,
    ReactiveFormsModule,
    TranslocoPipe,
  ],
  templateUrl: './security.html',
  styleUrl: './security.scss',
})
export class SecurityPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly errors = inject(ApiErrorService);
  readonly session = inject(AuthSessionService);

  readonly passwordSubmitted = signal(false);
  readonly passwordSubmitting = signal(false);
  readonly passwordError = signal<string | null>(null);
  readonly passwordSuccess = signal<string | null>(null);
  readonly passwordForm = this.formBuilder.nonNullable.group({
    currentPassword: ['', [Validators.required]],
    newPassword: [
      '',
      [
        Validators.required,
        Validators.minLength(8),
        Validators.maxLength(72),
        Validators.pattern(PASSWORD_PATTERN),
      ],
    ],
  });

  readonly factorMode = signal<TwoFactorMode | null>(null);
  readonly factorChallengeId = signal<string | null>(null);
  readonly factorSubmitted = signal(false);
  readonly factorVerifySubmitted = signal(false);
  readonly factorSubmitting = signal(false);
  readonly factorError = signal<string | null>(null);
  readonly factorSuccess = signal<string | null>(null);
  readonly factorRequestForm = this.formBuilder.nonNullable.group({
    currentPassword: ['', [Validators.required]],
  });
  readonly factorVerifyForm = this.formBuilder.nonNullable.group({
    otp: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  submitPasswordChange(): void {
    this.passwordSubmitted.set(true);
    this.passwordError.set(null);
    this.passwordSuccess.set(null);
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    this.passwordSubmitting.set(true);
    this.auth
      .changePassword(this.passwordForm.getRawValue())
      .pipe(finalize(() => this.passwordSubmitting.set(false)))
      .subscribe({
        next: (response) => {
          this.session.establish(response);
          this.passwordSuccess.set(response.message);
          this.passwordForm.reset();
          this.passwordSubmitted.set(false);
        },
        error: (error: unknown) => this.passwordError.set(this.errors.getMessage(error)),
      });
  }

  startTwoFactor(mode: TwoFactorMode): void {
    this.factorMode.set(mode);
    this.factorChallengeId.set(null);
    this.factorSubmitted.set(false);
    this.factorVerifySubmitted.set(false);
    this.factorError.set(null);
    this.factorSuccess.set(null);
    this.factorRequestForm.reset();
    this.factorVerifyForm.reset();
  }

  cancelTwoFactor(): void {
    this.factorMode.set(null);
    this.factorChallengeId.set(null);
    this.factorError.set(null);
    this.factorSuccess.set(null);
  }

  requestTwoFactorChallenge(): void {
    this.factorSubmitted.set(true);
    this.factorError.set(null);
    this.factorSuccess.set(null);
    const mode = this.factorMode();
    if (!mode || this.factorRequestForm.invalid) {
      this.factorRequestForm.markAllAsTouched();
      return;
    }

    this.factorSubmitting.set(true);
    this.auth
      .requestTwoFactorChange(mode === 'enable', this.factorRequestForm.getRawValue())
      .pipe(finalize(() => this.factorSubmitting.set(false)))
      .subscribe({
        next: (response) => {
          this.factorChallengeId.set(response.challengeId);
          this.factorSuccess.set(response.message);
        },
        error: (error: unknown) => this.factorError.set(this.errors.getMessage(error)),
      });
  }

  confirmTwoFactorChange(): void {
    this.factorVerifySubmitted.set(true);
    this.factorError.set(null);
    this.factorSuccess.set(null);
    const mode = this.factorMode();
    const challengeId = this.factorChallengeId();
    if (!mode || !challengeId || this.factorVerifyForm.invalid) {
      this.factorVerifyForm.markAllAsTouched();
      return;
    }

    this.factorSubmitting.set(true);
    this.auth
      .confirmTwoFactorChange(mode === 'enable', {
        challengeId,
        otp: this.factorVerifyForm.controls.otp.value,
      })
      .pipe(
        switchMap((response) => this.auth.loadCurrentUser().pipe(map(() => response))),
        finalize(() => this.factorSubmitting.set(false)),
      )
      .subscribe({
        next: (response) => {
          this.factorSuccess.set(response.message);
          this.factorMode.set(null);
          this.factorChallengeId.set(null);
        },
        error: (error: unknown) => this.factorError.set(this.errors.getMessage(error)),
      });
  }
}
