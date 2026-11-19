export type FeeType = 'COMMISSION' | 'FIXED_FEE' | 'LOGISTICS_FEE' | 'PAYMENT_PROCESSING' | 'OTHER';

export type FeeValueType = 'PERCENTAGE' | 'FIXED_AMOUNT' | 'PERCENTAGE_PLUS_FIXED';

export type FeeSourceType = 'OFFICIAL_PORTAL' | 'CONTRACT' | 'MANUAL_OVERRIDE';

export type FeeResolutionStatus = 'SUCCESS' | 'PARTIAL' | 'MISSING';

export interface ProductCost {
  id: string;
  tenantId: string;
  productId: string;
  variantId?: string | null;
  companyId?: string | null;
  currency: string;
  unit: string;
  acquisitionCost: number;
  packagingCost: number;
  additionalCost: number;
  subsidizedShippingCost: number;
  taxRatePercent: number;
  otherExpensesPercent: number;
  effectiveFrom: string;
  effectiveTo?: string | null;
  version: number;
  sourceType: string;
  notes?: string | null;
  createdByName?: string | null;
  createdAt: string;
}

export interface FeeRule {
  id: string;
  tenantId: string;
  marketplace: string;
  feeType: FeeType;
  feeValueType: FeeValueType;
  percentageValue?: number | null;
  fixedAmount?: number | null;
  minPriceThreshold?: number | null;
  maxPriceThreshold?: number | null;
  listingType?: string | null;
  category?: string | null;
  effectiveFrom: string;
  effectiveTo?: string | null;
  version: number;
  isActive: boolean;
  sourceType: FeeSourceType;
  sourceReference: string;
  notes?: string | null;
  createdByName?: string | null;
  createdAt: string;
}

export interface ResolvedFeeItem {
  feeType: FeeType;
  ruleId?: string | null;
  name: string;
  sourceType: FeeSourceType;
  sourceReference: string;
  calculatedAmount: number;
  appliedPercentage?: number | null;
  appliedFixedAmount?: number | null;
  notes?: string | null;
}

export interface FeeResolutionResult {
  marketplace: string;
  referenceDate: string;
  status: FeeResolutionStatus;
  totalEstimatedFees: number;
  hasMissingFees: boolean;
  missingFeeTypes: FeeType[];
  resolvedFees: ResolvedFeeItem[];
  message?: string | null;
}

export type DeductionCategory =
  | 'DIRECT_COST'
  | 'COMMISSION'
  | 'FIXED_FEE'
  | 'LOGISTICS_FEE'
  | 'TAX'
  | 'OTHER_VARIABLE'
  | 'OTHER_FIXED';

export interface PricingDeductionItem {
  name: string;
  category: DeductionCategory;
  amount: number;
  percentageOfNetRevenue: number;
  appliedRate: number;
  isPercentage: boolean;
}

export interface PricingCalculationRequest {
  grossSellingPrice: number;
  promotionalDiscount?: number;
  acquisitionCost: number;
  packagingCost?: number;
  additionalCost?: number;
  subsidizedShippingCost?: number;
  channelCommissionPercent?: number;
  channelFixedFee?: number;
  logisticsFee?: number;
  taxRatePercent?: number;
  otherVariableExpensesPercent?: number;
  otherFixedDeductions?: number;
  minMarginPercent?: number;
  targetMarginPercent?: number;
  targetMarginAmount?: number;
  currency?: string;
}

export interface PricingCalculationResult {
  grossSellingPrice: number;
  promotionalDiscount: number;
  netSellingPrice: number;
  totalDirectCosts: number;
  totalVariableDeductions: number;
  totalFixedDeductions: number;
  totalDeductions: number;
  contributionMarginAmount: number;
  contributionMarginPercent: number;
  breakEvenPrice: number | null;
  isBreakEvenAttainable: boolean;
  minPrice: number | null;
  isMinPriceAttainable: boolean;
  targetPrice: number | null;
  isTargetPriceAttainable: boolean;
  currency: string;
  formulaVersion: string;
  calculatedAt: string;
  deductionItems: PricingDeductionItem[];
  assumptions: string[];
  warnings: string[];
}

export interface ProductMarginSimulationRequest {
  productId: string;
  variantId?: string | null;
  companyId?: string | null;
  marketplace: string;
  grossSellingPrice: number;
  promotionalDiscount?: number;
  listingType?: string | null;
  category?: string | null;
  referenceDate?: string | null;
  minMarginPercent?: number | null;
  targetMarginPercent?: number | null;
  targetMarginAmount?: number | null;
}
