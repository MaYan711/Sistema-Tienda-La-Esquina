import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { debounceTime, distinctUntilChanged, finalize } from 'rxjs';
import { Role } from '../../../core/models/auth.models';
import { ManagedUser } from '../../../core/models/user.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { AuthSessionService } from '../../../core/services/auth-session.service';
import { UserService } from '../../../core/services/user.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

@Component({
  selector: 'app-users-page',
  imports: [CommonModule, FormFeedbackComponent, PageHeadingComponent, ReactiveFormsModule, TranslocoPipe],
  templateUrl: './users.html',
  styleUrl: './users.scss',
})
export class UsersPageComponent implements OnInit {
  private readonly usersApi = inject(UserService);
  private readonly session = inject(AuthSessionService);
  private readonly errors = inject(ApiErrorService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);

  readonly users = signal<ManagedUser[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly selectedUser = signal<ManagedUser | null>(null);
  readonly userModalOpen = signal(false);

  readonly filters = this.fb.group({
    search: [''],
    role: [''],
    enabled: [''],
  });

  readonly userForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72), Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/)]],
    role: ['EMPLOYEE' as Role, [Validators.required]],
  });

  ngOnInit(): void {
    this.filters.controls.search.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.resetAndLoad());

    this.filters.controls.role.valueChanges
      .pipe(distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.resetAndLoad());

    this.filters.controls.enabled.valueChanges
      .pipe(distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.resetAndLoad());

    this.loadUsers();
  }

  loadUsers(page = this.page()): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    const values = this.filters.getRawValue();
    const enabled = values.enabled === '' ? null : values.enabled === 'true';

    this.usersApi
      .search({
        search: values.search ?? '',
        role: (values.role ?? '') as Role | '',
        enabled,
        page,
        size: this.size(),
        sortBy: 'email',
        direction: 'asc',
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => {
          this.users.set(response.content ?? []);
          const metadata = response.page;
          this.page.set(metadata?.number ?? response.number ?? page);
          this.size.set(metadata?.size ?? response.size ?? this.size());
          this.totalElements.set(metadata?.totalElements ?? response.totalElements ?? response.content?.length ?? 0);
          this.totalPages.set(metadata?.totalPages ?? response.totalPages ?? 1);
        },
        error: (error: unknown) => this.errorMessage.set(this.errors.getMessage(error)),
      });
  }

  clearFilters(): void {
    this.filters.reset({ search: '', role: '', enabled: '' });
    this.page.set(0);
    this.loadUsers(0);
  }

  previousPage(): void {
    if (this.page() > 0) this.loadUsers(this.page() - 1);
  }

  nextPage(): void {
    if (this.page() + 1 < this.totalPages()) this.loadUsers(this.page() + 1);
  }

  openCreate(): void {
    this.selectedUser.set(null);
    this.userForm.reset({ email: '', password: '', role: 'EMPLOYEE' });
    this.userForm.controls.password.enable();
    this.userForm.controls.password.setValidators([
      Validators.required,
      Validators.minLength(8),
      Validators.maxLength(72),
      Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/),
    ]);
    this.userForm.controls.role.enable();
    this.userForm.controls.password.updateValueAndValidity();
    this.userFormOpen();
  }

  openEdit(user: ManagedUser): void {
    this.selectedUser.set(user);
    this.userForm.reset({ email: user.email, password: '', role: user.role });
    this.userForm.controls.password.clearValidators();
    this.userForm.controls.password.disable();
    if (this.isCurrentUser(user)) this.userForm.controls.role.disable();
    else this.userForm.controls.role.enable();
    this.userForm.controls.password.updateValueAndValidity();
    this.userFormOpen();
  }

  closeUserModal(): void {
    this.userModalOpen.set(false);
    this.selectedUser.set(null);
  }

  saveUser(): void {
    if (this.userForm.invalid) {
      this.userForm.markAllAsTouched();
      return;
    }

    const current = this.selectedUser();
    const raw = this.userForm.getRawValue();
    const changesOwnEmail = !!current && this.isCurrentUser(current) && current.email !== raw.email.trim().toLowerCase();
    this.saving.set(true);
    this.errorMessage.set(null);

    const request$ = current
      ? this.usersApi.update(current.id, {
          email: raw.email.trim().toLowerCase(),
          role: this.isCurrentUser(current) ? current.role : raw.role,
        })
      : this.usersApi.create({
          email: raw.email.trim().toLowerCase(),
          password: raw.password,
          role: raw.role,
        });

    request$.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (user) => {
        this.successMessage.set(current ? 'Usuario actualizado correctamente.' : 'Usuario creado correctamente.');
        this.closeUserModal();
        if (changesOwnEmail) {
          alert('Tu correo fue actualizado. Por seguridad debes iniciar sesión nuevamente con el nuevo correo.');
          this.session.clear();
          void this.router.navigate(['/auth/login']);
          return;
        }
        if (this.isCurrentUser(user)) {
          this.session.setUser({ ...this.session.currentUser!, email: user.email, role: user.role, enabled: user.enabled, createdAt: user.createdAt, updatedAt: user.updatedAt });
        }
        this.loadUsers();
      },
      error: (error: unknown) => this.errorMessage.set(this.errors.getMessage(error)),
    });
  }

  toggleEnabled(user: ManagedUser): void {
    if (this.isCurrentUser(user)) return;
    const action = user.enabled ? 'desactivar' : 'activar';
    if (!confirm(`¿Deseas ${action} la cuenta ${user.email}?`)) return;

    this.usersApi.setEnabled(user.id, !user.enabled).subscribe({
      next: () => {
        this.successMessage.set(user.enabled ? 'Usuario desactivado correctamente.' : 'Usuario activado correctamente.');
        this.loadUsers();
      },
      error: (error: unknown) => this.errorMessage.set(this.errors.getMessage(error)),
    });
  }

  isCurrentUser(user: ManagedUser): boolean {
    return this.session.currentUser?.id === user.id;
  }

  initials(user: ManagedUser): string {
    return user.email.slice(0, 2).toUpperCase();
  }

  roleLabel(role: Role): string {
    return role === 'ADMIN' ? 'Administrador' : 'Empleado';
  }

  formatDate(value: string | null | undefined): string {
    if (!value) return '—';
    return new Intl.DateTimeFormat('es-GT', {
      timeZone: 'America/Guatemala',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(value));
  }

  pageStart(): number {
    return this.totalElements() === 0 ? 0 : this.page() * this.size() + 1;
  }

  pageEnd(): number {
    return Math.min((this.page() + 1) * this.size(), this.totalElements());
  }

  private resetAndLoad(): void {
    this.page.set(0);
    this.loadUsers(0);
  }

  private userFormOpen(): void {
    this.userForm.updateValueAndValidity();
    this.userModalOpen.set(true);
  }
}
