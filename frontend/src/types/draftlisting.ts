export type DraftTone = 'INFORMATIVE_OBJECTIVE' | 'TECHNICAL' | 'COMMERCIAL_POLITE';

export type DraftStatus = 'DRAFT' | 'REVIEWED' | 'APPROVED' | 'REJECTED';

export interface MarketplaceRuleLimitsDto {
  marketplace: string;
  minTitleLength: number;
  maxTitleLength: number;
  minDescriptionLength: number;
  maxDescriptionLength: number;
  plainTextOnly: boolean;
  officialRuleSource: string;
}

export interface DraftListingVersionDto {
  id: string;
  draftListingId: string;
  versionNumber: number;
  promptTemplateId: string;
  promptTemplateVersion: string;
  aiModelName: string;
  aiProvider: string;
  tone: string;
  targetMarketplace: string;
  inputAttributesJson: string;
  generatedTitle: string;
  generatedDescription: string;
  reviewedTitle: string | null;
  reviewedDescription: string | null;
  effectiveTitle: string;
  effectiveDescription: string;
  traceableAttributesJson: string;
  unprovenClaimsJson: string | null;
  titleCharCount: number;
  descriptionCharCount: number;
  characterLimitValid: boolean;
  userEdited: boolean;
  reviewNotes: string | null;
  createdAt: string;
}

export interface DraftListingSummaryDto {
  id: string;
  tenantId: string;
  productId: string | null;
  productVariantId: string | null;
  targetMarketplace: string;
  currentVersionNumber: number;
  status: DraftStatus;
  publicationStatus: string;
  externalPublishBlocked: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface DraftListingDetailDto {
  id: string;
  tenantId: string;
  productId: string | null;
  productVariantId: string | null;
  targetMarketplace: string;
  currentVersionNumber: number;
  status: DraftStatus;
  publicationStatus: string;
  externalPublishBlocked: boolean;
  currentVersion: DraftListingVersionDto;
  versionHistory: DraftListingVersionDto[];
  marketplaceLimits: MarketplaceRuleLimitsDto | null;
  createdAt: string;
  updatedAt: string;
}

export interface DraftGenerationRequest {
  productId?: string;
  productVariantId?: string;
  targetMarketplace: string;
  tone?: DraftTone;
  brand?: string;
  model?: string;
  category?: string;
  materials?: string[];
  dimensions?: Record<string, string>;
  warranty?: string;
  technicalSpecifications?: Record<string, string>;
  confirmedBenefits?: string[];
  keywords?: string[];
}

export interface DraftEditRequest {
  reviewedTitle: string;
  reviewedDescription: string;
  reviewNotes?: string;
}

export interface DraftApproveRequest {
  approvalNotes?: string;
}

export interface DraftRejectRequest {
  rejectionReason: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
