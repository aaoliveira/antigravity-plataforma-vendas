import { describe, it, expect } from 'vitest';
import { parseMarketplaceUrl } from './marketplaceUrlParser';

describe('marketplaceUrlParser', () => {
  it('parses Mercado Livre product URL with hyphen in ID', () => {
    const url = 'https://produto.mercadolivre.com.br/MLB-2000984711-teclado-mecanico-rgb-_JM';
    const parsed = parseMarketplaceUrl(url, 'Teclado Mecânico RGB | Mercado Livre');

    expect(parsed.supported).toBe(true);
    expect(parsed.marketplace).toBe('MERCADO_LIVRE');
    expect(parsed.externalItemId).toBe('MLB2000984711');
    expect(parsed.title).toBe('Teclado Mecânico RGB');
  });

  it('parses Mercado Livre product URL without hyphen in ID', () => {
    const url = 'https://www.mercadolivre.com.br/p/MLB19283746';
    const parsed = parseMarketplaceUrl(url, 'Monitor Dell 27 Polegadas');

    expect(parsed.supported).toBe(true);
    expect(parsed.marketplace).toBe('MERCADO_LIVRE');
    expect(parsed.externalItemId).toBe('MLB19283746');
  });

  it('parses Shopee product URL using slug pattern (-i.shopid.itemid)', () => {
    const url = 'https://shopee.com.br/Fone-de-Ouvido-Bluetooth-Sem-Fio-i.123456789.9876543210';
    const parsed = parseMarketplaceUrl(url, 'Fone de Ouvido Bluetooth Sem Fio | Shopee Brasil');

    expect(parsed.supported).toBe(true);
    expect(parsed.marketplace).toBe('SHOPEE');
    expect(parsed.externalItemId).toBe('9876543210');
    expect(parsed.title).toBe('Fone de Ouvido Bluetooth Sem Fio');
  });

  it('parses Shopee product URL using /product/shopid/itemid pattern', () => {
    const url = 'https://shopee.com.br/product/998877/1122334455';
    const parsed = parseMarketplaceUrl(url);

    expect(parsed.supported).toBe(true);
    expect(parsed.marketplace).toBe('SHOPEE');
    expect(parsed.externalItemId).toBe('1122334455');
  });

  it('rejects private checkout and cart pages for privacy and security', () => {
    const checkoutUrl = 'https://www.mercadolivre.com.br/checkout/v6/payments/12345';
    const parsed = parseMarketplaceUrl(checkoutUrl);

    expect(parsed.supported).toBe(false);
    expect(parsed.externalItemId).toBeNull();
    expect(parsed.reason).toContain('Página privada de checkout');
  });

  it('rejects general non-product pages and unsupported domains', () => {
    const googleUrl = 'https://www.google.com/search?q=teste';
    const parsed = parseMarketplaceUrl(googleUrl);

    expect(parsed.supported).toBe(false);
    expect(parsed.marketplace).toBe('UNKNOWN');
    expect(parsed.reason).toContain('Domínio não suportado');
  });
});
