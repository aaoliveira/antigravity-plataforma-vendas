/**
 * Tipos e interfaces TypeScript para o Dashboard Executivo de KPIs Analíticos (P022-FE).
 */

export interface KpiDefinition {
  code: string;
  name: string;
  formula: string;
  description: string;
  unit: string;
  provenance: string;
}

export interface AggregatedKpiSummary {
  tenantId: string;
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
  averageOrderValue: number;
  reconciledCount: number;
  estimatedCount: number;
  divergentCount: number;
  ordersWithMissingDataCount: number;
  reconciliationRatePercent: number;
  cached: boolean;
  calculatedAt: string;
  antiHallucinationDisclaimer: string;
  kpiDefinitions?: KpiDefinition[];
}

export interface ChannelKpiAggregate {
  marketplace: string;
  orderCount: number;
  grossRevenue: number;
  netRevenue: number;
  totalDeductions: number;
  contributionMarginAmount: number;
  averageContributionMarginRatio: number;
  revenueSharePercent: number;
  marginSharePercent: number;
}

export interface PeriodicKpiAggregate {
  periodLabel: string;
  orderCount: number;
  grossRevenue: number;
  netRevenue: number;
  contributionMarginAmount: number;
  averageOrderValue: number;
}

export interface CompanyKpiAggregate {
  companyId: string;
  companyName: string;
  cnpj: string;
  orderCount: number;
  grossRevenue: number;
  netRevenue: number;
  contributionMarginAmount: number;
  revenueSharePercent: number;
}

export type KpiGranularity = 'DAILY' | 'WEEKLY' | 'MONTHLY';
export type DashboardPeriodPreset = 'LAST_7' | 'LAST_30' | 'LAST_90' | 'CUSTOM';

export interface DashboardFilterState {
  periodPreset: DashboardPeriodPreset;
  startDate: string;
  endDate: string;
  companyId: string;
  marketplace: string;
  sellerAccountId: string;
}
