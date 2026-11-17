import { parseMarketplaceUrl } from '../parsers/marketplaceUrlParser';

/**
 * Content Script Mínimo para a Seller Intelligence Platform
 * Permissões mínimas: executa document_idle e somente extrai metadados públicos canônicos.
 */

if (typeof chrome !== 'undefined' && chrome.runtime && chrome.runtime.onMessage) {
  chrome.runtime.onMessage.addListener((message, _sender, sendResponse) => {
    if (message && message.type === 'INSPECT_PAGE_META') {
      const canonicalTag = document.querySelector('link[rel="canonical"]');
      const canonicalUrl = canonicalTag ? canonicalTag.getAttribute('href') || window.location.href : window.location.href;
      const parsed = parseMarketplaceUrl(canonicalUrl, document.title);

      sendResponse({
        success: true,
        data: parsed,
      });
    }
    return true;
  });
}
