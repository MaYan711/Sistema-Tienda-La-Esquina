export type StockStatus = 'AVAILABLE' | 'LOW_STOCK' | 'OUT_OF_STOCK';
export type InventoryMovementType = 'STOCK_ENTRY' | 'SALE' | 'ADJUSTMENT';
export type NotificationType = 'LOW_STOCK' | 'OUT_OF_STOCK';

export interface PageMetadata {
  size: number;
  number: number;
  totalElements: number;
  totalPages: number;
}

export interface PageResponse<T> {
  content: T[];
  page?: PageMetadata;
  number?: number;
  size?: number;
  totalElements?: number;
  totalPages?: number;
}

export interface ProductCategory {
  id: number;
  name: string;
  description: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface MeasurementUnit {
  id: number;
  code: string;
  name: string;
  allowsDecimal: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Product {
  id: number;
  code: string;
  name: string;
  description: string | null;
  imageUrl: string | null;
  category: ProductCategory;
  unit: MeasurementUnit;
  purchasePrice: number;
  salePrice: number;
  currentStock: number;
  minimumStock: number;
  stockStatus: StockStatus;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProductSearchParams {
  search?: string;
  categoryId?: number | null;
  active?: boolean | null;
  stockStatus?: StockStatus | '';
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: 'asc' | 'desc';
}

export interface CreateProductRequest {
  code: string;
  name: string;
  description: string | null;
  imageUrl: string | null;
  categoryId: number;
  unitId: number;
  purchasePrice: number;
  salePrice: number;
  initialStock: number;
  minimumStock: number;
}

export interface UpdateProductRequest {
  code: string;
  name: string;
  description: string | null;
  imageUrl: string | null;
  categoryId: number;
  unitId: number;
  purchasePrice: number;
  salePrice: number;
  minimumStock: number;
}

export interface InventoryAdjustmentRequest {
  productId: number;
  newStock: number;
  reason: string;
}

export interface InventoryAdjustmentResponse {
  id: number;
  productId: number;
  productCode: string;
  productName: string;
  quantityBefore: number;
  quantityAfter: number;
  quantityDelta: number;
  reason: string;
  adjustedByEmail: string;
  adjustmentDate: string;
  createdAt: string;
  stockStatus: StockStatus;
}

export interface InventoryMovement {
  id: number;
  productId: number;
  productCode: string;
  productName: string;
  movementType: InventoryMovementType;
  quantityDelta: number;
  quantityBefore: number;
  quantityAfter: number;
  sourceType: InventoryMovementType;
  sourceId: number;
  reason: string | null;
  createdByEmail: string;
  createdAt: string;
}

export interface StockNotification {
  id: number;
  productId: number;
  productCode: string;
  productName: string;
  type: NotificationType;
  title: string;
  message: string;
  currentStock: number;
  minimumStock: number;
  read: boolean;
  readByEmail: string | null;
  readAt: string | null;
  createdAt: string;
}
