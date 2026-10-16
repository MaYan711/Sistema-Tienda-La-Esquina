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

export interface Supplier {
  id: number;
  name: string;
  nit: string;
  phone: string | null;
  email: string | null;
  address: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SupplierSearchParams {
  search?: string;
  active?: boolean | null;
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: 'asc' | 'desc';
}

export interface CreateSupplierRequest {
  name: string;
  nit: string;
  phone: string | null;
  email: string | null;
  address: string | null;
}

export interface UpdateSupplierRequest {
  name: string;
  nit: string;
  phone: string | null;
  email: string | null;
  address: string | null;
}

export interface CreateStockEntryItemRequest {
  productId: number;
  quantity: number;
  unitCost: number;
}

export interface CreateStockEntryRequest {
  supplierId: number;
  entryDate: string;
  documentNumber: string | null;
  notes: string | null;
  items: CreateStockEntryItemRequest[];
}

export interface StockEntryItem {
  id: number;
  productId: number;
  productCode: string;
  productName: string;
  quantity: number;
  unitCost: number;
  lineTotal: number;
}

export interface StockEntry {
  id: number;
  supplierId: number;
  supplierName: string;
  entryDate: string;
  documentNumber: string | null;
  notes: string | null;
  status: 'DRAFT' | 'CONFIRMED' | 'CANCELLED';
  createdById: number;
  createdByEmail: string;
  confirmedAt: string | null;
  totalAmount: number;
  items: StockEntryItem[];
  createdAt: string;
  updatedAt: string;
}

export interface SupplierPriceComparisonItem {
  supplierId: number;
  supplierName: string;
  supplierNit: string | null;
  latestUnitCost: number;
  averageUnitCost: number;
  minimumUnitCost: number;
  maximumUnitCost: number;
  purchaseCount: number;
  lastPurchaseDate: string;
  differenceFromBest: number;
  percentageAboveBest: number;
  recommended: boolean;
}

export interface SupplierPriceComparison {
  productId: number;
  productCode: string;
  productName: string;
  currentPurchasePrice: number;
  suppliersCompared: number;
  recommendedSupplierId: number | null;
  recommendedSupplierName: string | null;
  recommendedUnitCost: number | null;
  highestUnitCost: number | null;
  savingsPerUnit: number;
  savingsPercentage: number;
  recommendation: string;
  suppliers: SupplierPriceComparisonItem[];
}

export interface StockEntrySearchParams {
  supplierId?: number | null;
  fromDate?: string;
  toDate?: string;
  reference?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: 'asc' | 'desc';
}
export interface CreateSaleItemRequest {
  productId: number;
  quantity: number;
}

export interface CreateSaleRequest {
  cashReceived: number;
  notes: string | null;
  items: CreateSaleItemRequest[];
}

export interface SaleItem {
  id: number;
  productId: number;
  productCode: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  unitCostSnapshot: number;
  lineTotal: number;
}

export interface Sale {
  id: number;
  saleNumber: string;
  soldById: number;
  soldByEmail: string;
  saleDate: string;
  subtotalAmount: number;
  totalAmount: number;
  cashReceived: number;
  changeAmount: number;
  status: 'COMPLETED' | 'CANCELLED';
  notes: string | null;
  items: SaleItem[];
  createdAt: string;
}

export interface SaleDetailItem {
  id: number;
  productId: number;
  productCode: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface SaleDetail {
  id: number;
  saleNumber: string;
  saleDate: string;
  soldById: number;
  soldByEmail: string;
  paymentMethod: 'EFECTIVO';
  status: 'COMPLETED' | 'CANCELLED';
  differentProducts: number;
  totalUnits: number;
  subtotalAmount: number;
  totalAmount: number;
  cashReceived: number;
  changeAmount: number;
  notes: string | null;
  items: SaleDetailItem[];
  createdAt: string;
}

export interface SaleReceiptItem {
  productId: number;
  productCode: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface SaleReceipt {
  storeName: string;
  documentTitle: string;
  saleId: number;
  saleNumber: string;
  saleDate: string;
  soldByEmail: string;
  paymentMethod: 'EFECTIVO';
  subtotalAmount: number;
  totalAmount: number;
  cashReceived: number;
  changeAmount: number;
  notes: string | null;
  items: SaleReceiptItem[];
}

export interface OperatingHoursStatus {
  enabled: boolean;
  timeZone: string;
  openingTime: string | null;
  closingTime: string | null;
  currentDate: string;
  currentTime: string;
  role: 'ADMIN' | 'EMPLOYEE';
  employeeRestricted: boolean;
  operationAllowed: boolean;
  closedToday: boolean;
  scheduleSource: 'DEFAULT' | 'WEEKLY' | 'EXCEPTION';
  reason: string | null;
  message: string;
}

export interface OperatingHourDayConfig {
  dayOfWeek: string;
  label: string;
  enabled: boolean;
  openingTime: string;
  closingTime: string;
  configured: boolean;
}

export interface OperatingHourException {
  id: number;
  date: string;
  closed: boolean;
  openingTime: string | null;
  closingTime: string | null;
  reason: string | null;
  updatedByEmail: string | null;
  updatedAt: string;
}

export interface OperatingHoursConfiguration {
  enabled: boolean;
  timeZone: string;
  defaultOpeningTime: string;
  defaultClosingTime: string;
  weekly: OperatingHourDayConfig[];
  exceptions: OperatingHourException[];
}

export interface OperatingHourDayRequest {
  dayOfWeek: string;
  enabled: boolean;
  openingTime: string;
  closingTime: string;
}

export interface UpdateOperatingHoursRequest {
  days: OperatingHourDayRequest[];
}

export interface OperatingHourExceptionRequest {
  date: string;
  closed: boolean;
  openingTime: string | null;
  closingTime: string | null;
  reason: string | null;
}

export interface SaleSearchParams {
  fromDate?: string;
  toDate?: string;
  reference?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: 'asc' | 'desc';
}