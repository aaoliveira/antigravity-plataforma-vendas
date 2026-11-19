/**
 * Tipos e contratos de dados para Fatos Financeiros, Reconciliação e DRE Simplificada (P017-FE).
 */

export type FinancialValueProvenance = 'CONFIRMED' | 'ESTIMATED' | 'USER_INFORMED' | 'MISSING';

export type ReconciliationStatus = 'ESTIMATED' | 'RECONCILED' | 'DIVERGENT' | 'PARTIAL';

export interface OrderItemFinancialFact {
  id: string;
  orderItemId: string;
  productId?: string | null;
  variantId?: string | null;
  sku?: string | null;
  title: string;
  quantity: number;
  unitPrice: number;
  grossRevenue: number;
  discountAmount: number;
  netRevenue: number;
  cancellationAmount: number;
  returnAmount: number;
  marketplaceFee: number;
  fixedFee: number;
  shippingCost: number;
  unitProductCost: number;
  productCost: number;
  packagingCost: number;
  additionalCost: number;
  taxRatePercent: number;
  taxAmount: number;
  otherExpensesAmount: number;
  totalDeductions: number;
  contributionMarginAmount: number;
  contributionMarginRatio: number;
  costProvenance: FinancialValueProvenance;
  hasMissingData: boolean;
  missingDataReasons?: string | null;
}

export interface OrderFinancialFact {
  id: string;
  orderId: string;
  sellerAccountId: string;
  companyId?: string | null;
  marketplace: string;
  externalOrderId: string;
  orderStatus: string;
  orderDate: string;
  grossRevenue: number;
  discountAmount: number;
  netRevenue: number;
  cancellationAmount: number;
  returnAmount: number;
  marketplaceFee: number;
  fixedFee: number;
  shippingCost: number;
  productCost: number;
  packagingCost: number;
  additionalCost: number;
  taxAmount: number;
  otherExpensesAmount: number;
  totalDeductions: number;
  contributionMarginAmount: number;
  contributionMarginRatio: number;
  currency: string;
  reconciliationStatus: ReconciliationStatus;
  feeProvenance: FinancialValueProvenance;
  shippingProvenance: FinancialValueProvenance;
  costProvenance: FinancialValueProvenance;
  taxProvenance: FinancialValueProvenance;
  hasMissingData: boolean;
  missingDataReasons?: string | null;
  calculationVersion: string;
  calculationTraceJson?: string | null;
  settlementExternalId?: string | null;
  settlementDate?: string | null;
  settlementNotes?: string | null;
  reconciledAt?: string | null;
  antiHallucinationDisclaimer: string;
  items?: OrderItemFinancialFact[];
  createdAt: string;
  updatedAt: string;
}

export interface FinancialFactSummary {
  totalOrders: number;
  totalGrossRevenue: number;
  totalDiscountAmount: number;
  totalNetRevenue: number;
  totalCancellationAmount: number;
  totalReturnAmount: number;
  totalMarketplaceFees: number;
  totalFixedFees: number;
  totalShippingCost: number;
  totalProductCost: number;
  totalPackagingCost: number;
  totalAdditionalCost: number;
  totalTaxAmount: number;
  totalOtherExpensesAmount: number;
  totalDeductions: number;
  totalContributionMarginAmount: number;
  averageContributionMarginRatio: number;
  reconciledCount: number;
  estimatedCount: number;
  divergentCount: number;
  ordersWithMissingDataCount: number;
  antiHallucinationDisclaimer: string;
}

export interface ReconcileSettlementCommand {
  settlementExternalId: string;
  settlementDate: string;
  confirmedMarketplaceFee?: number | null;
  confirmedFixedFee?: number | null;
  confirmedShippingCost?: number | null;
  payoutAmount?: number | null;
  notes?: string | null;
}

export type PeriodPreset = 'TODAY' | 'LAST_7' | 'LAST_30' | 'THIS_MONTH' | 'CUSTOM';

export interface FinancialFilterState {
  periodPreset: PeriodPreset;
  startDate: string;
  endDate: string;
  companyId: string;
  marketplace: string;
  sellerAccountId: string;
  sku: string;
  reconciliationStatus: string;
}
