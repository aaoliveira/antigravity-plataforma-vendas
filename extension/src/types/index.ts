export type MarketplaceType = 'MERCADO_LIVRE' | 'SHOPEE' | 'UNKNOWN';

export interface ParsedProductPage {
  supported: boolean;
  marketplace: MarketplaceType;
  externalItemId: string | null;
  canonicalUrl: string;
  title: string | null;
  reason?: string;
}

export type ExtensionMessageType =
  | 'INSPECT_CURRENT_TAB'
  | 'INSPECT_CURRENT_TAB_RESULT'
  | 'OPEN_PLATFORM_TAB'
  | 'CHECK_BACKEND_STATUS';

export interface ExtensionMessage<T = unknown> {
  type: ExtensionMessageType;
  payload?: T;
}

export interface BackendStatus {
  online: boolean;
  platformUrl: string;
  authenticated: boolean;
}
