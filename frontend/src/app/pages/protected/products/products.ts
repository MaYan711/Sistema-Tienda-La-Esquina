import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { debounceTime, distinctUntilChanged, finalize, forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  MeasurementUnit,
  Product,
  ProductCategory,
  StockStatus,
} from '../../../core/models/store.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { AuthSessionService } from '../../../core/services/auth-session.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { InventoryService } from '../../../core/services/inventory.service';
import { ProductService } from '../../../core/services/product.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

@Component({
  selector: 'app-products-page',
  imports: [DecimalPipe, FormFeedbackComponent, PageHeadingComponent, ReactiveFormsModule, TranslocoPipe],
  templateUrl: './products.html',
  styleUrl: './products.scss',
})
export class ProductsPageComponent implements OnInit {
  private readonly productsApi = inject(ProductService);
  private readonly catalogApi = inject(CatalogService);
  private readonly inventoryApi = inject(InventoryService);
  private readonly session = inject(AuthSessionService);
  private readonly errors = inject(ApiErrorService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly products = signal<Product[]>([]);
  readonly categories = signal<ProductCategory[]>([]);
  readonly units = signal<MeasurementUnit[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly selectedProduct = signal<Product | null>(null);
  readonly productModalOpen = signal(false);
  readonly adjustmentModalOpen = signal(false);

  readonly filters = this.fb.group({
    search: [''],
    categoryId: [''],
    stockStatus: [''],
  });

  readonly productForm = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.maxLength(64)]],
    name: ['', [Validators.required, Validators.maxLength(160)]],
    description: [''],
    imageUrl: [''],
    categoryId: [0, [Validators.required, Validators.min(1)]],
    unitId: [0, [Validators.required, Validators.min(1)]],
    purchasePrice: [0, [Validators.required, Validators.min(0)]],
    salePrice: [0, [Validators.required, Validators.min(0)]],
    initialStock: [0, [Validators.required, Validators.min(0)]],
    minimumStock: [0, [Validators.required, Validators.min(0)]],
  });

  readonly adjustmentForm = this.fb.nonNullable.group({
    newStock: [0, [Validators.required, Validators.min(0)]],
    reason: ['', [Validators.required, Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    forkJoin({
      categories: this.catalogApi.categories(),
      units: this.catalogApi.measurementUnits(),
    }).subscribe({
      next: ({ categories, units }) => {
        this.categories.set(categories.filter((item) => item.active));
        this.units.set(units.filter((item) => item.active));
      },
      error: (error: unknown) => this.errorMessage.set(this.errors.getMessage(error)),
    });
    this.filters.controls.search.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.page.set(0);
        this.loadProducts(0);
      });

    this.filters.controls.categoryId.valueChanges
      .pipe(distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.page.set(0);
        this.loadProducts(0);
      });

    this.filters.controls.stockStatus.valueChanges
      .pipe(distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.page.set(0);
        this.loadProducts(0);
      });

    this.loadProducts();
  }

  isAdmin(): boolean {
    return this.session.currentUser?.role === 'ADMIN';
  }

  loadProducts(page = this.page()): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    const values = this.filters.getRawValue();
    this.productsApi
      .search({
        search: values.search ?? '',
        categoryId: values.categoryId ? Number(values.categoryId) : null,
        active: true,
        stockStatus: (values.stockStatus ?? '') as StockStatus | '',
        page,
        size: this.size(),
        sortBy: 'name',
        direction: 'asc',
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => {
          this.products.set(response.content ?? []);
          const metadata = response.page;
          this.page.set(metadata?.number ?? response.number ?? page);
          this.size.set(metadata?.size ?? response.size ?? this.size());
          this.totalElements.set(metadata?.totalElements ?? response.totalElements ?? response.content?.length ?? 0);
          this.totalPages.set(metadata?.totalPages ?? response.totalPages ?? 1);
        },
        error: (error: unknown) => this.errorMessage.set(this.errors.getMessage(error)),
      });
  }

  applyFilters(): void {
    this.page.set(0);
    this.loadProducts(0);
  }

  clearFilters(): void {
    this.filters.reset({ search: '', categoryId: '', stockStatus: '' });
    this.page.set(0);
    this.loadProducts(0);
  }

  previousPage(): void {
    if (this.page() > 0) this.loadProducts(this.page() - 1);
  }

  nextPage(): void {
    if (this.page() + 1 < this.totalPages()) this.loadProducts(this.page() + 1);
  }

  openCreate(): void {
    this.selectedProduct.set(null);
    this.productForm.reset({
      code: '',
      name: '',
      description: '',
      imageUrl: '',
      categoryId: this.categories()[0]?.id ?? 0,
      unitId: this.units()[0]?.id ?? 0,
      purchasePrice: 0,
      salePrice: 0,
      initialStock: 0,
      minimumStock: 0,
    });
    this.productForm.controls.initialStock.enable();
    this.productModalOpen.set(true);
  }

  openEdit(product: Product): void {
    this.selectedProduct.set(product);
    this.productForm.reset({
      code: product.code,
      name: product.name,
      description: product.description ?? '',
      imageUrl: product.imageUrl ?? '',
      categoryId: product.category.id,
      unitId: product.unit.id,
      purchasePrice: Number(product.purchasePrice),
      salePrice: Number(product.salePrice),
      initialStock: Number(product.currentStock),
      minimumStock: Number(product.minimumStock),
    });
    this.productForm.controls.initialStock.disable();
    this.productModalOpen.set(true);
  }

  closeProductModal(): void {
    this.productModalOpen.set(false);
    this.selectedProduct.set(null);
  }

  saveProduct(): void {
    if (this.productForm.invalid) {
      this.productForm.markAllAsTouched();
      return;
    }
    const raw = this.productForm.getRawValue();
    this.saving.set(true);
    this.errorMessage.set(null);
    const current = this.selectedProduct();
    const request$ = current
      ? this.productsApi.update(current.id, {
          code: raw.code.trim(),
          name: raw.name.trim(),
          description: raw.description.trim() || null,
          imageUrl: raw.imageUrl.trim() || null,
          categoryId: Number(raw.categoryId),
          unitId: Number(raw.unitId),
          purchasePrice: Number(raw.purchasePrice),
          salePrice: Number(raw.salePrice),
          minimumStock: Number(raw.minimumStock),
        })
      : this.productsApi.create({
          code: raw.code.trim(),
          name: raw.name.trim(),
          description: raw.description.trim() || null,
          imageUrl: raw.imageUrl.trim() || null,
          categoryId: Number(raw.categoryId),
          unitId: Number(raw.unitId),
          purchasePrice: Number(raw.purchasePrice),
          salePrice: Number(raw.salePrice),
          initialStock: Number(raw.initialStock),
          minimumStock: Number(raw.minimumStock),
        });

    request$.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.successMessage.set(current ? 'Producto actualizado correctamente.' : 'Producto creado correctamente.');
        this.closeProductModal();
        this.loadProducts();
      },
      error: (error: unknown) => this.errorMessage.set(this.errors.getMessage(error)),
    });
  }

  toggleActive(product: Product): void {
    if (!confirm(`¿Deseas ${product.active ? 'desactivar' : 'activar'} ${product.name}?`)) return;
    this.productsApi.setActive(product.id, !product.active).subscribe({
      next: () => {
        this.successMessage.set(product.active ? 'Producto desactivado.' : 'Producto activado.');
        this.loadProducts();
      },
      error: (error: unknown) => this.errorMessage.set(this.errors.getMessage(error)),
    });
  }

  openAdjustment(product: Product): void {
    this.selectedProduct.set(product);
    this.adjustmentForm.reset({ newStock: Number(product.currentStock), reason: '' });
    this.adjustmentModalOpen.set(true);
  }

  closeAdjustmentModal(): void {
    this.adjustmentModalOpen.set(false);
    this.selectedProduct.set(null);
  }

  saveAdjustment(): void {
    const product = this.selectedProduct();
    if (!product || this.adjustmentForm.invalid) {
      this.adjustmentForm.markAllAsTouched();
      return;
    }
    const raw = this.adjustmentForm.getRawValue();
    this.saving.set(true);
    this.inventoryApi
      .adjustStock({ productId: product.id, newStock: Number(raw.newStock), reason: raw.reason.trim() })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          this.successMessage.set('Inventario ajustado correctamente.');
          this.closeAdjustmentModal();
          this.loadProducts();
        },
        error: (error: unknown) => this.errorMessage.set(this.errors.getMessage(error)),
      });
  }

  statusLabel(status: StockStatus): string {
    return status === 'AVAILABLE' ? 'Disponible' : status === 'LOW_STOCK' ? 'Bajo' : 'Agotado';
  }

  pageStart(): number {
    return this.totalElements() === 0 ? 0 : this.page() * this.size() + 1;
  }

  pageEnd(): number {
    return Math.min((this.page() + 1) * this.size(), this.totalElements());
  }
}
