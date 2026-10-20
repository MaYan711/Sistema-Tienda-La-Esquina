import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { InputTextModule } from 'primeng/inputtext';
import { finalize, map, switchMap } from 'rxjs';
import {
  OperatingHourDayConfig,
  OperatingHourException,
  OperatingHourExceptionRequest,
  OperatingHoursConfiguration,
} from '../../../core/models/store.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { AuthService } from '../../../core/services/auth.service';
import { AuthSessionService } from '../../../core/services/auth-session.service';
import { OperatingHoursService } from '../../../core/services/operating-hours.service';
import { FieldErrorComponent } from '../../../shared/components/field-error/field-error';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

type TwoFactorMode = 'enable' | 'disable';
type TimeField = 'openingTime' | 'closingTime';
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
export class SecurityPageComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly errors = inject(ApiErrorService);
  private readonly operatingHoursApi = inject(OperatingHoursService);
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

  readonly scheduleLoading = signal(false);
  readonly scheduleSaving = signal(false);
  readonly scheduleError = signal<string | null>(null);
  readonly scheduleSuccess = signal<string | null>(null);
  readonly scheduleConfiguration = signal<OperatingHoursConfiguration | null>(null);
  readonly weeklySchedule = signal<OperatingHourDayConfig[]>([]);

  readonly exceptionSaving = signal(false);
  readonly exceptionError = signal<string | null>(null);
  readonly exceptionSuccess = signal<string | null>(null);
  readonly editingExceptionId = signal<number | null>(null);
  readonly exceptionForm = this.formBuilder.nonNullable.group({
    date: ['', [Validators.required]],
    closed: [false],
    openingTime: ['06:00', [Validators.required]],
    closingTime: ['22:00', [Validators.required]],
    reason: [''],
  });

  readonly minExceptionDate = this.todayIso();

  ngOnInit(): void {
    if (this.isAdmin()) {
      this.loadOperatingHoursConfiguration();
    }
  }

  isAdmin(): boolean {
    return this.session.currentUser?.role === 'ADMIN';
  }

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

  loadOperatingHoursConfiguration(): void {
    this.scheduleLoading.set(true);
    this.scheduleError.set(null);

    this.operatingHoursApi
      .getConfiguration()
      .pipe(finalize(() => this.scheduleLoading.set(false)))
      .subscribe({
        next: (configuration) => {
          this.applyConfiguration(configuration);
          this.resetExceptionForm();
        },
        error: (error: unknown) => {
          this.scheduleError.set(this.errors.getMessage(error));
        },
      });
  }

  setDayEnabled(dayOfWeek: string, enabled: boolean): void {
    this.weeklySchedule.update((days) =>
      days.map((day) =>
        day.dayOfWeek === dayOfWeek
          ? { ...day, enabled }
          : day,
      ),
    );
  }

  setDayTime(dayOfWeek: string, field: TimeField, value: string): void {
    this.weeklySchedule.update((days) =>
      days.map((day) =>
        day.dayOfWeek === dayOfWeek
          ? { ...day, [field]: value }
          : day,
      ),
    );
  }

  saveWeeklySchedule(): void {
    const days = this.weeklySchedule();
    if (days.length !== 7) {
      this.scheduleError.set('No fue posible cargar los siete días de la semana.');
      return;
    }

    this.scheduleSaving.set(true);
    this.scheduleError.set(null);
    this.scheduleSuccess.set(null);

    this.operatingHoursApi
      .updateWeekly({
        days: days.map((day) => ({
          dayOfWeek: day.dayOfWeek,
          enabled: day.enabled,
          openingTime: day.openingTime,
          closingTime: day.closingTime,
        })),
      })
      .pipe(finalize(() => this.scheduleSaving.set(false)))
      .subscribe({
        next: (configuration) => {
          this.applyConfiguration(configuration);
          this.scheduleSuccess.set('Horario semanal actualizado correctamente.');
        },
        error: (error: unknown) => {
          this.scheduleError.set(this.errors.getMessage(error));
        },
      });
  }

  saveException(): void {
    if (this.exceptionForm.invalid) {
      this.exceptionForm.markAllAsTouched();
      return;
    }

    const value = this.exceptionForm.getRawValue();
    const request: OperatingHourExceptionRequest = {
      date: value.date,
      closed: value.closed,
      openingTime: value.closed ? null : value.openingTime,
      closingTime: value.closed ? null : value.closingTime,
      reason: value.reason.trim() || null,
    };

    this.exceptionSaving.set(true);
    this.exceptionError.set(null);
    this.exceptionSuccess.set(null);

    const editingId = this.editingExceptionId();
    const operation = editingId === null
      ? this.operatingHoursApi.createException(request)
      : this.operatingHoursApi.updateException(editingId, request);

    operation
      .pipe(finalize(() => this.exceptionSaving.set(false)))
      .subscribe({
        next: () => {
          this.exceptionSuccess.set(
            editingId === null
              ? 'Excepción de horario registrada correctamente.'
              : 'Excepción de horario actualizada correctamente.',
          );
          this.resetExceptionForm();
          this.refreshConfigurationWithoutResettingMessage();
        },
        error: (error: unknown) => {
          this.exceptionError.set(this.errors.getMessage(error));
        },
      });
  }

  editException(exception: OperatingHourException): void {
    this.editingExceptionId.set(exception.id);
    this.exceptionError.set(null);
    this.exceptionSuccess.set(null);
    this.exceptionForm.setValue({
      date: exception.date,
      closed: exception.closed,
      openingTime: exception.openingTime ?? '06:00',
      closingTime: exception.closingTime ?? '22:00',
      reason: exception.reason ?? '',
    });
  }

  cancelExceptionEdit(): void {
    this.resetExceptionForm();
    this.exceptionError.set(null);
  }

  deleteException(exception: OperatingHourException): void {
    if (!window.confirm(`¿Eliminar la excepción del ${this.formatDate(exception.date)}?`)) {
      return;
    }

    this.exceptionError.set(null);
    this.exceptionSuccess.set(null);
    this.operatingHoursApi.deleteException(exception.id).subscribe({
      next: () => {
        this.exceptionSuccess.set('Excepción eliminada correctamente.');
        if (this.editingExceptionId() === exception.id) {
          this.resetExceptionForm();
        }
        this.refreshConfigurationWithoutResettingMessage();
      },
      error: (error: unknown) => {
        this.exceptionError.set(this.errors.getMessage(error));
      },
    });
  }

  formatDate(value: string): string {
    const [year, month, day] = value.split('-');
    return `${day}/${month}/${year}`;
  }

  private applyConfiguration(configuration: OperatingHoursConfiguration): void {
    this.scheduleConfiguration.set(configuration);
    this.weeklySchedule.set(configuration.weekly.map((day) => ({ ...day })));
  }

  private refreshConfigurationWithoutResettingMessage(): void {
    this.operatingHoursApi.getConfiguration().subscribe({
      next: (configuration) => this.applyConfiguration(configuration),
      error: (error: unknown) => this.exceptionError.set(this.errors.getMessage(error)),
    });
  }

  private resetExceptionForm(): void {
    const configuration = this.scheduleConfiguration();
    this.editingExceptionId.set(null);
    this.exceptionForm.reset({
      date: '',
      closed: false,
      openingTime: configuration?.defaultOpeningTime ?? '06:00',
      closingTime: configuration?.defaultClosingTime ?? '22:00',
      reason: '',
    });
  }

  private todayIso(): string {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
