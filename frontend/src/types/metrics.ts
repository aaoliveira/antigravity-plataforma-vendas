/**
 * Tipos TypeScript para Módulo de Inteligência Analítica e Métricas Oficiais (P018)
 * Espelham rigorosamente os DTOs e Enums do backend modular.
 */

export type MetricScope = 'ACCOUNT' | 'LISTING';

export type MetricGranularity = 'DAILY' | 'PERIOD_TOTAL';

export type MetricAvailabilityStatus =
  | 'AVAILABLE'
  | 'UNAVAILABLE'
  | 'INSUFFICIENT_DATA'
  | 'UNSUPPORTED_BY_CHANNEL';

export type MetricProvenance =
  | 'OFFICIAL_API'
  | 'DERIVED_FORMULA'
  | 'MANUAL_IMPORT';

export type MetricUnit =
  | 'COUNT'
  | 'CURRENCY'
  | 'PERCENTAGE'
  | 'RATIO';

export interface AccountMetricDto {
  id: string;
  tenantId: string;
  sellerAccountId: string;
  marketplace: string;
  listingId: string | null;
  externalListingId: string | null;
  targetIdentifier: string;
  metricScope: MetricScope;
  granularity: MetricGranularity;
  windowStart: string; // ISO-8601 UTC
  windowEnd: string; // ISO-8601 UTC
  metricKey: string;
  metricValue: number | null;
  metricUnit: MetricUnit;
  provenance: MetricProvenance;
  formulaDefinition: string | null;
  denominatorMetric: string | null;
  denominatorValue: number | null;
  numeratorMetric: string | null;
  numeratorValue: number | null;
  availabilityStatus: MetricAvailabilityStatus;
  unavailabilityReason: string | null;
  sourceEndpoint: string;
  ingestedAt: string;
  metadataJson: string | null;
}

export interface MetricsSummaryDto {
  totalMetricsRecorded: number;
  availableMetricsCount: number;
  unavailableMetricsCount: number;
  insufficientDataCount: number;
  unsupportedByChannelCount: number;
  totalVisits: number;
  totalOrders: number;
  totalUnitsSold: number;
  totalRevenue: number;
  totalClicks: number;
  totalImpressions: number;
  overallConversionRateOrders: number;
  overallCtr: number;
  antiHallucinationNotice: string;
}

export interface MetricCatalogEntry {
  marketplace: string;
  metricKey: string;
  displayName: string;
  scope: MetricScope;
  supportedGranularities: MetricGranularity[];
  unit: MetricUnit;
  available: boolean;
  unavailabilityReason: string | null;
  officialSourceEndpoint: string;
  maxSupportedWindowDays: number;
  rateLimitPerMinute: number;
  formulaDefinition: string | null;
}

export interface TemporalMetricComparison {
  current: number;
  previous: number | null;
  absoluteDelta: number | null;
  percentageDelta: number | null;
  hasPreviousData: boolean;
}

export interface MetricDataPoint {
  date: string; // YYYY-MM-DD
  displayDate: string;
  value: number | null;
  isAvailable: boolean;
  isGap: boolean;
  availabilityStatus: MetricAvailabilityStatus;
  unavailabilityReason?: string | null;
}
