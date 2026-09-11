import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, finalize, of, switchMap } from 'rxjs';
import { InventoryMovement, InventoryMovementType, Product } from '../../../core/models/store.models';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { InventoryService } from '../../../core/services/inventory.service';
import { ProductService } from '../../../core/services/product.service';
import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

@Component({
  selector: 'app-inventory-movements-page',
  imports: [DecimalPipe, FormFeedbackComponent, PageHeadingComponent, ReactiveFormsModule],
  templateUrl: './inventory-movements.html',
  styleUrl: './inventory-movements.scss',
})
export class InventoryMovementsPageComponent implements OnInit {
  private readonly inventory = inject(InventoryService);
  private readonly productsApi = inject(ProductService);
  private readonly errors = inject(ApiErrorService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly movements = signal<InventoryMovement[]>([]);
  readonly productMatches = signal<Product[]>([]);
  readonly selectedProductId = signal<number | null>(null);
  readonly searchFocused = signal(false);
  readonly productSearchLoading = signal(false);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);

  readonly filters = this.fb.group({
    productSearch: [''],
    movementType: [''],
  });

  private readonly guatemalaFormatter = new Intl.DateTimeFormat('es-GT', {
    timeZone: 'America/Guatemala',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: true,
  });

  ngOnInit(): void {
    this.filters.controls.productSearch.valueChanges
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        switchMap((rawValue) => {
          this.selectedProductId.set(null);
          const search = (rawValue ?? '').trim();
          if (!search) {
            this.productMatches.set([]);
            this.page.set(0);
            this.load(0);
            return of(null);
          }

          this.productSearchLoading.set(true);
          return this.productsApi.search({
            search,
            active: true,
            page: 0,
            size: 8,
            sortBy: 'name',
            direction: 'asc',
          }).pipe(finalize(() => this.productSearchLoading.set(false)));
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (response) => {
          if (response) this.productMatches.set(response.content ?? []);
        },
        error: (error: unknown) => {
          this.productMatches.set([]);
          this.errorMessage.set(this.errors.getMessage(error));
        },
      });

    this.filters.controls.movementType.valueChanges
      .pipe(distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.page.set(0);
        this.load(0);
      });

    this.load();
  }

  load(page = this.page()): void {
    const values = this.filters.getRawValue();
    this.loading.set(true);
    this.errorMessage.set(null);
    this.inventory.movements({
      productId: this.selectedProductId(),
      movementType: (values.movementType ?? '') as InventoryMovementType | '',
      page,
      size: this.size(),
      sortBy: 'createdAt',
      direction: 'desc',
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (response) => {
        this.movements.set(response.content ?? []);
        this.page.set(response.page?.number ?? response.number ?? page);
        this.size.set(response.page?.size ?? response.size ?? this.size());
        this.totalElements.set(response.page?.totalElements ?? response.totalElements ?? response.content?.length ?? 0);
        this.totalPages.set(response.page?.totalPages ?? response.totalPages ?? 1);
      },
      error: (error: unknown) => this.errorMessage.set(this.errors.getMessage(error)),
    });
  }

  selectProduct(product: Product): void {
    this.selectedProductId.set(product.id);
    this.filters.controls.productSearch.setValue(`${product.code} - ${product.name}`, { emitEvent: false });
    this.productMatches.set([]);
    this.searchFocused.set(false);
    this.page.set(0);
    this.load(0);
  }

  clearProductSearch(): void {
    this.selectedProductId.set(null);
    this.productMatches.set([]);
    this.filters.controls.productSearch.setValue('', { emitEvent: false });
    this.page.set(0);
    this.load(0);
  }

  applyFilters(): void {
    this.page.set(0);
    this.load(0);
  }

  previousPage(): void { if (this.page() > 0) this.load(this.page() - 1); }
  nextPage(): void { if (this.page() + 1 < this.totalPages()) this.load(this.page() + 1); }

  movementLabel(type: InventoryMovementType): string {
    if (type === 'ADJUSTMENT') return 'Ajuste';
    if (type === 'SALE') return 'Venta';
    return 'Entrada';
  }

  formatGuatemalaDate(value: string): string {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : this.guatemalaFormatter.format(date);
  }
}
