import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  debounceTime,
  distinctUntilChanged,
  finalize,
  forkJoin,
} from 'rxjs';

import {
  Product,
  StockEntry,
  Supplier,
} from '../../../core/models/store.models';

import { ApiErrorService } from '../../../core/services/api-error.service';
import { ProductService } from '../../../core/services/product.service';
import { StockEntryService } from '../../../core/services/stock-entry.service';
import { SupplierService } from '../../../core/services/supplier.service';

import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

interface PendingEntryItem {
  product: Product;
  quantity: number;
  unitCost: number;
  lineTotal: number;
}

@Component({
  selector: 'app-stock-entries-page',
  imports: [
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    FormFeedbackComponent,
    PageHeadingComponent,
  ],
  templateUrl: './stock-entries.html',
  styleUrl: './stock-entries.scss',
})
export class StockEntriesPageComponent implements OnInit {
  private readonly stockEntriesApi = inject(StockEntryService);
  private readonly suppliersApi = inject(SupplierService);
  private readonly productsApi = inject(ProductService);
  private readonly errors = inject(ApiErrorService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly entries = signal<StockEntry[]>([]);
readonly suppliers = signal<Supplier[]>([]);
readonly productResults = signal<Product[]>([]);
readonly selectedProduct = signal<Product | null>(null);
readonly selectedItems = signal<PendingEntryItem[]>([]);

  readonly loading = signal(false);
  readonly saving = signal(false);

  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);

  readonly entryModalOpen = signal(false);

  readonly filters = this.fb.group({
    supplierId: [''],
    reference: [''],
    fromDate: [''],
    toDate: [''],
  });

  readonly entryForm = this.fb.nonNullable.group({
    supplierId: [0, [Validators.required, Validators.min(1)]],
    entryDate: [this.today(), Validators.required],
    documentNumber: [''],
    notes: [''],
  });

  readonly productForm = this.fb.nonNullable.group({
    search: [''],
    productId: [0],
    quantity: [1, [Validators.required, Validators.min(0.001)]],
    unitCost: [0, [Validators.required, Validators.min(0.0001)]],
  });

  ngOnInit(): void {
    forkJoin({
      suppliers: this.suppliersApi.search({
        active: true,
        page: 0,
        size: 100,
        sortBy: 'name',
        direction: 'asc',
      }),
      entries: this.stockEntriesApi.search({
        page: 0,
        size: this.size(),
        sortBy: 'entryDate',
        direction: 'desc',
      }),
    }).subscribe({
      next: ({ suppliers, entries }) => {
        this.suppliers.set(suppliers.content ?? []);
        this.applyEntryResponse(entries, 0);
      },
      error: (error: unknown) => {
        this.errorMessage.set(this.errors.getMessage(error));
      },
    });

    this.filters.controls.reference.valueChanges
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.page.set(0);
        this.loadEntries(0);
      });

    this.productForm.controls.search.valueChanges
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((value) => {
        const search = value?.trim() ?? '';

        if (!search) {
          this.productResults.set([]);
          return;
        }

        this.productsApi
          .search({
            search,
            active: true,
            page: 0,
            size: 8,
            sortBy: 'name',
            direction: 'asc',
          })
          .subscribe({
            next: (response) => {
              this.productResults.set(response.content ?? []);
            },
            error: (error: unknown) => {
              this.errorMessage.set(this.errors.getMessage(error));
            },
          });
      });
  }

  loadEntries(page = this.page()): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    const values = this.filters.getRawValue();

    this.stockEntriesApi
      .search({
        supplierId: values.supplierId ? Number(values.supplierId) : null,
        reference: values.reference ?? '',
        fromDate: values.fromDate ?? '',
        toDate: values.toDate ?? '',
        page,
        size: this.size(),
        sortBy: 'entryDate',
        direction: 'desc',
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => this.applyEntryResponse(response, page),
        error: (error: unknown) => {
          this.errorMessage.set(this.errors.getMessage(error));
        },
      });
  }

  private applyEntryResponse(response: any, page: number): void {
    this.entries.set(response.content ?? []);

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
  }

  openCreate(): void {
  this.entryForm.reset({
    supplierId: this.suppliers()[0]?.id ?? 0,
    entryDate: this.today(),
    documentNumber: '',
    notes: '',
  });

  this.productForm.reset({
    search: '',
    productId: 0,
    quantity: 1,
    unitCost: 0,
  });

  this.selectedProduct.set(null);
  this.selectedItems.set([]);
  this.productResults.set([]);
  this.entryModalOpen.set(true);
}

  closeCreate(): void {
  this.entryModalOpen.set(false);
  this.selectedProduct.set(null);
  this.selectedItems.set([]);
  this.productResults.set([]);
}

  selectProduct(product: Product): void {
    this.selectedProduct.set(product);

    this.productForm.patchValue({
        productId: product.id,
        search: `${product.code} - ${product.name}`,
        unitCost: Number(product.purchasePrice),
        quantity: 1,
    });

    this.productResults.set([]);
    }

  addProduct(): void {
    const raw = this.productForm.getRawValue();

    if (!raw.productId || raw.quantity <= 0 || raw.unitCost <= 0) {
      this.errorMessage.set(
        'Selecciona un producto e ingresa cantidad y costo válidos.',
      );
      return;
    }

    const product = this.findProduct(raw.productId);

    if (!product) {
      this.errorMessage.set('No fue posible identificar el producto.');
      return;
    }

    if (
      this.selectedItems().some(
        (item) => item.product.id === product.id,
      )
    ) {
      this.errorMessage.set(
        'El producto ya fue agregado a esta entrada.',
      );
      return;
    }

    if (
      !product.unit.allowsDecimal &&
      !Number.isInteger(Number(raw.quantity))
    ) {
      this.errorMessage.set(
        `La unidad de ${product.name} no permite cantidades decimales.`,
      );
      return;
    }

    const quantity = Number(raw.quantity);
    const unitCost = Number(raw.unitCost);

    const item: PendingEntryItem = {
      product,
      quantity,
      unitCost,
      lineTotal: quantity * unitCost,
    };

    this.selectedItems.update((items) => [...items, item]);
    this.selectedProduct.set(null);

    this.productForm.reset({
      search: '',
      productId: 0,
      quantity: 1,
      unitCost: 0,
    });

    this.errorMessage.set(null);
  }

  removeItem(productId: number): void {
    this.selectedItems.update((items) =>
      items.filter((item) => item.product.id !== productId),
    );
  }

  totalAmount(): number {
    return this.selectedItems().reduce(
      (total, item) => total + item.lineTotal,
      0,
    );
  }

  saveEntry(): void {
    if (this.entryForm.invalid) {
      this.entryForm.markAllAsTouched();
      return;
    }

    if (this.selectedItems().length === 0) {
      this.errorMessage.set(
        'Agrega al menos un producto a la entrada.',
      );
      return;
    }

    const raw = this.entryForm.getRawValue();

    this.saving.set(true);
    this.errorMessage.set(null);

    this.stockEntriesApi
      .create({
        supplierId: Number(raw.supplierId),
        entryDate: raw.entryDate,
        documentNumber: raw.documentNumber.trim() || null,
        notes: raw.notes.trim() || null,
        items: this.selectedItems().map((item) => ({
          productId: item.product.id,
          quantity: item.quantity,
          unitCost: item.unitCost,
        })),
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          this.successMessage.set(
            'Entrada de mercadería registrada correctamente.',
          );

          this.closeCreate();
          this.loadEntries(0);
        },
        error: (error: unknown) => {
          this.errorMessage.set(this.errors.getMessage(error));
        },
      });
  }

  clearFilters(): void {
    this.filters.reset({
      supplierId: '',
      reference: '',
      fromDate: '',
      toDate: '',
    });

    this.page.set(0);
    this.loadEntries(0);
  }

  previousPage(): void {
    if (this.page() > 0) {
      this.loadEntries(this.page() - 1);
    }
  }

  nextPage(): void {
    if (this.page() + 1 < this.totalPages()) {
      this.loadEntries(this.page() + 1);
    }
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

  private findProduct(id: number): Product | null {
  const selected = this.selectedProduct();

  if (selected?.id === id) {
    return selected;
  }

  return null;
}

  private today(): string {
    const formatter = new Intl.DateTimeFormat('en-CA', {
      timeZone: 'America/Guatemala',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    });

    return formatter.format(new Date());
  }
}