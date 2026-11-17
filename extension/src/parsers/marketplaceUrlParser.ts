import { ParsedProductPage } from '../types';

export function parseMarketplaceUrl(url: string, pageTitle?: string): ParsedProductPage {
  if (!url || typeof url !== 'string') {
    return {
      supported: false,
      marketplace: 'UNKNOWN',
      externalItemId: null,
      canonicalUrl: '',
      title: null,
      reason: 'URL inválida ou ausente.',
    };
  }

  const cleanUrl = url.trim();

  // 1. Verificação de segurança: Bloqueia páginas privadas de checkout, pagamento ou conta
  if (
    cleanUrl.includes('/checkout/') ||
    cleanUrl.includes('/carrinho') ||
    cleanUrl.includes('/cart') ||
    cleanUrl.includes('/pagamento') ||
    cleanUrl.includes('/my-account') ||
    cleanUrl.includes('/login')
  ) {
    return {
      supported: false,
      marketplace: 'UNKNOWN',
      externalItemId: null,
      canonicalUrl: cleanUrl,
      title: null,
      reason: 'Página privada de checkout ou conta. A extensão respeita sua privacidade e não analisa dados de compras ou sessões.',
    };
  }

  // 2. Mercado Livre
  if (cleanUrl.includes('mercadolivre.com.br') || cleanUrl.includes('mercadolibre.com')) {
    // Regex para extrair MLB-12345678 ou MLB12345678
    const mlbMatch = cleanUrl.match(/\b(MLB-?\d{6,15})\b/i);
    if (mlbMatch) {
      const rawId = mlbMatch[1];
      const normalizedId = rawId.replace('-', '').toUpperCase();
      return {
        supported: true,
        marketplace: 'MERCADO_LIVRE',
        externalItemId: normalizedId,
        canonicalUrl: cleanUrl,
        title: pageTitle ? cleanTitle(pageTitle) : null,
      };
    }

    return {
      supported: false,
      marketplace: 'MERCADO_LIVRE',
      externalItemId: null,
      canonicalUrl: cleanUrl,
      title: pageTitle ? cleanTitle(pageTitle) : null,
      reason: 'Página do Mercado Livre sem anúncio individual identificado. Abra a página de um produto específico para inspecionar.',
    };
  }

  // 3. Shopee
  if (cleanUrl.includes('shopee.com.br')) {
    // Padrão 1: /product/{shopid}/{itemid}
    const shopeeProdMatch = cleanUrl.match(/\/product\/\d+\/(\d+)/);
    if (shopeeProdMatch) {
      return {
        supported: true,
        marketplace: 'SHOPEE',
        externalItemId: shopeeProdMatch[1],
        canonicalUrl: cleanUrl,
        title: pageTitle ? cleanTitle(pageTitle) : null,
      };
    }

    // Padrão 2: /slug-i.{shopid}.{itemid}
    const shopeeSlugMatch = cleanUrl.match(/-i\.\d+\.(\d+)/);
    if (shopeeSlugMatch) {
      return {
        supported: true,
        marketplace: 'SHOPEE',
        externalItemId: shopeeSlugMatch[1],
        canonicalUrl: cleanUrl,
        title: pageTitle ? cleanTitle(pageTitle) : null,
      };
    }

    return {
      supported: false,
      marketplace: 'SHOPEE',
      externalItemId: null,
      canonicalUrl: cleanUrl,
      title: pageTitle ? cleanTitle(pageTitle) : null,
      reason: 'Página da Shopee sem produto individual detectado. Navegue até o anúncio do item para inspecionar.',
    };
  }

  // 4. Domínio não suportado no MVP
  return {
    supported: false,
    marketplace: 'UNKNOWN',
    externalItemId: null,
    canonicalUrl: cleanUrl,
    title: null,
    reason: 'Domínio não suportado. O assistente contextual opera exclusivamente no Mercado Livre e Shopee.',
  };
}

function cleanTitle(title: string): string {
  return title
    .replace(/\s*\|\s*Mercado\s*Livre.*$/i, '')
    .replace(/\s*\|\s*Shopee\s*Brasil.*$/i, '')
    .trim();
}
