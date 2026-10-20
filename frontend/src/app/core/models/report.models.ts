export interface SalesReport {
  fromDate: string;
  toDate: string;
  totalSales: number;
  salesCount: number;
  previousPeriodTotal: number;
  changePercentage: number | null;
}

export interface MarginReport {
  fromDate: string;
  toDate: string;
  estimatedMargin: number;
  previousPeriodMargin: number;
  changePercentage: number | null;
}

export interface ProductsSoldReport {
  fromDate: string;
  toDate: string;
  productsSold: number;
  previousPeriodProductsSold: number;
  changePercentage: number | null;
}

export interface ResolvedAlertsReport {
  fromDate: string;
  toDate: string;
  resolvedAlerts: number;
  previousPeriodResolvedAlerts: number;
  changePercentage: number | null;
}

export interface DailySalesPoint {
  date: string;
  salesCount: number;
  totalSales: number;
}

export interface DailySalesReport {
  fromDate: string;
  toDate: string;
  dailySales: DailySalesPoint[];
}

export interface TopProduct {
  productId: number;
  productCode: string;
  productName: string;
  quantitySold: number;
  totalSales: number;
}

export interface TopProductsReport {
  fromDate: string;
  toDate: string;
  products: TopProduct[];
}

export interface CategoryReportItem {
  categoryId: number;
  categoryName: string;
  productsSold: number;
  totalSales: number;
  estimatedMargin: number;
  percentageOfSales: number;
}

export interface CategoryReport {
  fromDate: string;
  toDate: string;
  categories: CategoryReportItem[];
}

export interface ReportDashboardData {
  sales: SalesReport;
  margin: MarginReport;
  productsSold: ProductsSoldReport;
  resolvedAlerts: ResolvedAlertsReport;
  dailySales: DailySalesReport;
  topProducts: TopProductsReport;
  categories: CategoryReport;
}

export type ReportExportType =
  | 'sales'
  | 'margin'
  | 'products-sold'
  | 'resolved-alerts'
  | 'daily-sales'
  | 'top-products'
  | 'categories';