import { DatePipe, DecimalPipe } from '@angular/common';
import {
  Component,
  DestroyRef,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  debounceTime,
  distinctUntilChanged,
  finalize,
} from 'rxjs';

import {
  Product,
  Sale,
} from '../../../core/models/store.models';

import { ApiErrorService } from '../../../core/services/api-error.service';
import { ProductService } from '../../../core/services/product.service';
import { SaleService } from '../../../core/services/sale.service';

import { FormFeedbackComponent } from '../../../shared/components/form-feedback/form-feedback';
import { PageHeadingComponent } from '../../../shared/components/page-heading/page-heading';

interface PendingSaleItem {
  product: Product;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

@Component({
  selector: 'app-sales-page',
  imports: [
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    FormFeedbackComponent,
    PageHeadingComponent,
  ],
  templateUrl: './sales.html',
  styleUrl: './sales.scss',
})
export class SalesPageComponent implements OnInit {
  private readonly salesApi = inject(SaleService);
  private readonly productsApi = inject(ProductService);
  private readonly errors = inject(ApiErrorService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly sales = signal<Sale[]>([]);
  readonly productResults = signal<Product[]>([]);
  readonly selectedProduct = signal<Product | null>(null);
  readonly selectedItems = signal<PendingSaleItem[]>([]);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly searchingProducts = signal(false);

  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);

  readonly filters = this.fb.nonNullable.group({
    reference: [''],
    fromDate: [''],
    toDate: [''],
  });

  readonly productForm = this.fb.nonNullable.group({
    search: [''],
    productId: [0],
    quantity: [
      1,
      [
        Validators.required,
        Validators.min(0.001),
      ],
    ],
  });

  readonly paymentForm = this.fb.nonNullable.group({
    cashReceived: [
      0,
      [
        Validators.required,
        Validators.min(0),
      ],
    ],
    notes: [''],
  });

  ngOnInit(): void {
    this.loadSales(0);

    this.filters.controls.reference.valueChanges
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.page.set(0);
        this.loadSales(0);
      });

    this.productForm.controls.search.valueChanges
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((value) => {
        const search = value.trim();

        if (!search) {
          this.productResults.set([]);
          this.selectedProduct.set(null);
          this.productForm.patchValue(
            { productId: 0 },
            { emitEvent: false },
          );
          return;
        }

        this.searchingProducts.set(true);

        this.productsApi
          .search({
            search,
            active: true,
            page: 0,
            size: 8,
            sortBy: 'name',
            direction: 'asc',
          })
          .pipe(
            finalize(() =>
              this.searchingProducts.set(false),
            ),
          )
          .subscribe({
            next: (response) => {
              const products = (response.content ?? []).filter(
                (product) => Number(product.currentStock) > 0,
              );

              this.productResults.set(products);
            },
            error: (error: unknown) => {
              this.errorMessage.set(
                this.errors.getMessage(error),
              );
            },
          });
      });
  }

  loadSales(page = this.page()): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    const values = this.filters.getRawValue();

    this.salesApi
      .search({
        reference: values.reference,
        fromDate: values.fromDate,
        toDate: values.toDate,
        page,
        size: this.size(),
        sortBy: 'saleDate',
        direction: 'desc',
      })
      .pipe(
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (response) => {
          this.sales.set(response.content ?? []);

          const metadata = response.page;

          this.page.set(
            metadata?.number ??
              response.number ??
              page,
          );

          this.size.set(
            metadata?.size ??
              response.size ??
              this.size(),
          );

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
          this.errorMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  selectProduct(product: Product): void {
  this.errorMessage.set(null);
  this.successMessage.set(null);

  this.selectedProduct.set(product);

  this.productForm.patchValue(
    {
      productId: product.id,
      search: `${product.code} - ${product.name}`,
      quantity: 1,
    },
    {
      emitEvent: false,
    },
  );

  this.productResults.set([]);
}

  addProduct(): void {
  this.errorMessage.set(null);
  this.successMessage.set(null);

  const raw = this.productForm.getRawValue();

    if (!raw.productId || raw.quantity <= 0) {
      this.errorMessage.set(
        'Selecciona un producto e ingresa una cantidad válida.',
      );
      return;
    }

    const product = this.selectedProduct();

    if (!product || product.id !== raw.productId) {
      this.errorMessage.set(
        'No fue posible identificar el producto seleccionado.',
      );
      return;
    }

    if (
      this.selectedItems().some(
        (item) => item.product.id === product.id,
      )
    ) {
      this.errorMessage.set(
        'El producto ya fue agregado a la venta.',
      );
      return;
    }

    const quantity = Number(raw.quantity);
    const availableStock = Number(product.currentStock);

    if (
      !product.unit.allowsDecimal &&
      !Number.isInteger(quantity)
    ) {
      this.errorMessage.set(
        `La unidad de ${product.name} no permite cantidades decimales.`,
      );
      return;
    }

    if (quantity > availableStock) {
      this.errorMessage.set(
        `Solo hay ${availableStock} unidades disponibles de ${product.name}.`,
      );
      return;
    }

    const unitPrice = Number(product.salePrice);

    const item: PendingSaleItem = {
      product,
      quantity,
      unitPrice,
      lineTotal: quantity * unitPrice,
    };

    this.selectedItems.update((items) => [
      ...items,
      item,
    ]);

    this.clearProductSelection();
    this.errorMessage.set(null);
  }

  removeItem(productId: number): void {
    this.selectedItems.update((items) =>
      items.filter(
        (item) => item.product.id !== productId,
      ),
    );
  }

  updateQuantity(
    productId: number,
    event: Event,
  ): void {
    const input = event.target as HTMLInputElement;
    const quantity = Number(input.value);

    this.selectedItems.update((items) =>
      items.map((item) => {
        if (item.product.id !== productId) {
          return item;
        }

        if (!Number.isFinite(quantity) || quantity <= 0) {
          return item;
        }

        if (
          !item.product.unit.allowsDecimal &&
          !Number.isInteger(quantity)
        ) {
          this.errorMessage.set(
            `La unidad de ${item.product.name} no permite cantidades decimales.`,
          );
          input.value = String(item.quantity);
          return item;
        }

        if (
          quantity >
          Number(item.product.currentStock)
        ) {
          this.errorMessage.set(
            `Solo hay ${item.product.currentStock} unidades disponibles de ${item.product.name}.`,
          );
          input.value = String(item.quantity);
          return item;
        }

        return {
          ...item,
          quantity,
          lineTotal: quantity * item.unitPrice,
        };
      }),
    );
  }

  totalAmount(): number {
    return this.selectedItems().reduce(
      (total, item) =>
        total + item.lineTotal,
      0,
    );
  }

  cashReceived(): number {
    return Number(
      this.paymentForm.controls.cashReceived.value,
    );
  }

  changeAmount(): number {
    const change =
      this.cashReceived() - this.totalAmount();

    return change > 0 ? change : 0;
  }

  hasEnoughCash(): boolean {
    return (
      this.selectedItems().length > 0 &&
      this.cashReceived() >= this.totalAmount()
    );
  }

  saveSale(): void {
    if (this.selectedItems().length === 0) {
      this.errorMessage.set(
        'Agrega al menos un producto a la venta.',
      );
      return;
    }

    if (this.paymentForm.invalid) {
      this.paymentForm.markAllAsTouched();
      return;
    }

    if (!this.hasEnoughCash()) {
      this.errorMessage.set(
        'El efectivo recibido debe ser igual o mayor al total de la venta.',
      );
      return;
    }

    const payment =
      this.paymentForm.getRawValue();

    this.saving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.salesApi
      .create({
        cashReceived: Number(
          payment.cashReceived,
        ),
        notes:
          payment.notes.trim() || null,
        items: this.selectedItems().map(
          (item) => ({
            productId: item.product.id,
            quantity: item.quantity,
          }),
        ),
      })
      .pipe(
        finalize(() => this.saving.set(false)),
      )
      .subscribe({
        next: (sale) => {
          this.successMessage.set(
            `Venta ${sale.saleNumber} registrada correctamente. Cambio: Q ${Number(
              sale.changeAmount,
            ).toFixed(2)}.`,
          );

          this.resetSale();
          this.loadSales(0);
        },
        error: (error: unknown) => {
          this.errorMessage.set(
            this.errors.getMessage(error),
          );
        },
      });
  }

  resetSale(): void {
    this.selectedItems.set([]);

    this.clearProductSelection();

    this.paymentForm.reset({
      cashReceived: 0,
      notes: '',
    });
  }

  clearFilters(): void {
    this.filters.reset({
      reference: '',
      fromDate: '',
      toDate: '',
    });

    this.page.set(0);
    this.loadSales(0);
  }

  previousPage(): void {
    if (this.page() > 0) {
      this.loadSales(this.page() - 1);
    }
  }

  nextPage(): void {
    if (
      this.page() + 1 <
      this.totalPages()
    ) {
      this.loadSales(this.page() + 1);
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

  private clearProductSelection(): void {
    this.selectedProduct.set(null);
    this.productResults.set([]);

    this.productForm.reset(
      {
        search: '',
        productId: 0,
        quantity: 1,
      },
      {
        emitEvent: false,
      },
    );
  }
}