import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged, finalize } from 'rxjs';
import { Supplier } from '../../../core/models/store.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { SupplierService } from '../../../core/services/supplier.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

@Component({
  selector: 'app-suppliers-page',
  imports: [
    ReactiveFormsModule,
    FormFeedbackComponent,
    PageHeadingComponent,
  ],
  templateUrl: './suppliers.html',
  styleUrl: './suppliers.scss',
})
export class SuppliersPageComponent implements OnInit {
  private readonly suppliersApi = inject(SupplierService);
  private readonly errors = inject(ApiErrorService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly suppliers = signal<Supplier[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);

  readonly selectedSupplier = signal<Supplier | null>(null);
  readonly supplierModalOpen = signal(false);

  readonly filters = this.fb.group({
    search: [''],
    active: ['true'],
  });

  readonly supplierForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(160)]],
    nit: ['', [Validators.maxLength(32)]],
    phone: ['', [Validators.maxLength(32)]],
    email: ['', [Validators.email, Validators.maxLength(160)]],
    address: ['', [Validators.maxLength(255)]],
  });

  ngOnInit(): void {
    this.filters.controls.search.valueChanges
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.page.set(0);
        this.loadSuppliers(0);
      });

    this.filters.controls.active.valueChanges
      .pipe(
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.page.set(0);
        this.loadSuppliers(0);
      });

    this.loadSuppliers();
  }

  loadSuppliers(page = this.page()): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    const values = this.filters.getRawValue();

    let active: boolean | null = null;

    if (values.active === 'true') {
      active = true;
    } else if (values.active === 'false') {
      active = false;
    }

    this.suppliersApi
      .search({
        search: values.search ?? '',
        active,
        page,
        size: this.size(),
        sortBy: 'name',
        direction: 'asc',
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => {
          this.suppliers.set(response.content ?? []);

          const metadata = response.page;

          this.page.set(metadata?.number ?? response.number ?? page);
          this.size.set(metadata?.size ?? response.size ?? this.size());
          this.totalElements.set(
            metadata?.totalElements ??
              response.totalElements ??
              response.content?.length ??
              0,
          );
          this.totalPages.set(
            metadata?.totalPages ??
              response.totalPages ??
              1,
          );
        },
        error: (error: unknown) => {
          this.errorMessage.set(this.errors.getMessage(error));
        },
      });
  }

  clearFilters(): void {
    this.filters.reset({
      search: '',
      active: 'true',
    });

    this.page.set(0);
    this.loadSuppliers(0);
  }

  openCreate(): void {
    this.selectedSupplier.set(null);

    this.supplierForm.reset({
      name: '',
      nit: '',
      phone: '',
      email: '',
      address: '',
    });

    this.supplierModalOpen.set(true);
  }

  openEdit(supplier: Supplier): void {
    this.selectedSupplier.set(supplier);

    this.supplierForm.reset({
      name: supplier.name,
      nit: supplier.nit ?? '',
      phone: supplier.phone ?? '',
      email: supplier.email ?? '',
      address: supplier.address ?? '',
    });

    this.supplierModalOpen.set(true);
  }

  closeModal(): void {
    this.supplierModalOpen.set(false);
    this.selectedSupplier.set(null);
  }

  saveSupplier(): void {
    if (this.supplierForm.invalid) {
      this.supplierForm.markAllAsTouched();
      return;
    }

    const raw = this.supplierForm.getRawValue();
    const current = this.selectedSupplier();

    const request = {
      name: raw.name.trim(),
      nit: raw.nit.trim(),
      phone: raw.phone.trim() || null,
      email: raw.email.trim() || null,
      address: raw.address.trim() || null,
    };

    this.saving.set(true);
    this.errorMessage.set(null);

    const request$ = current
      ? this.suppliersApi.update(current.id, request)
      : this.suppliersApi.create(request);

    request$
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          this.successMessage.set(
            current
              ? 'Proveedor actualizado correctamente.'
              : 'Proveedor creado correctamente.',
          );

          this.closeModal();
          this.loadSuppliers();
        },
        error: (error: unknown) => {
          this.errorMessage.set(this.errors.getMessage(error));
        },
      });
  }

  toggleActive(supplier: Supplier): void {
    const action = supplier.active ? 'desactivar' : 'activar';

    if (!confirm(`¿Deseas ${action} ${supplier.name}?`)) {
      return;
    }

    this.suppliersApi
      .setActive(supplier.id, !supplier.active)
      .subscribe({
        next: () => {
          this.successMessage.set(
            supplier.active
              ? 'Proveedor desactivado correctamente.'
              : 'Proveedor activado correctamente.',
          );

          this.loadSuppliers();
        },
        error: (error: unknown) => {
          this.errorMessage.set(this.errors.getMessage(error));
        },
      });
  }

  previousPage(): void {
    if (this.page() > 0) {
      this.loadSuppliers(this.page() - 1);
    }
  }

  nextPage(): void {
    if (this.page() + 1 < this.totalPages()) {
      this.loadSuppliers(this.page() + 1);
    }
  }

  activeSupplierCount(): number {
  return this.suppliers().filter((supplier) => supplier.active).length;
}

  pageStart(): number {
    return this.totalElements() === 0
      ? 0
      : this.page() * this.size() + 1;
  }

  pageEnd(): number {
    return Math.min(
      (this.page() + 1) * this.size(),
      this.totalElements(),
    );
  }
}