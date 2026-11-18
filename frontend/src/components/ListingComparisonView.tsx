'use client';

import React, { useState, useMemo } from 'react';
import {
  ListingPriceHistoryDrawer,
  ListingPriceHistoryResponse,
} from './ListingPriceHistoryDrawer';

export interface ComparableListingItem {
  listingId: string;
  title: string;
  brand?: string;
  model?: string;
  gtin?: string;
  category?: string;
  condition?: 'NEW' | 'USED' | 'REFURBISHED' | 'RECONDITIONED' | 'NOT_SPECIFIED' | string;
  attributes?: Record<string, string>;
  price: number;
  currency?: string;
  shippingCost?: number | null;
  shippingStatus?: 'KNOWN' | 'UNKNOWN' | 'FREE' | string;
  finalPriceWithShipping?: number | null;
  observedAt?: string;
  marketplace: string;
  sellerName?: string;
  sourceUrl?: string;
  isOwnListing?: boolean;
}

export interface EquivalenceScoreBreakdown {
  gtinScore: number;
  brandScore: number;
  modelScore: number;
  titleScore: number;
  attributesScore: number;
  overallScore: number;
  thresholdApplied: number;
  isEquivalent: boolean;
  isCompatible: boolean;
  compatibilityStatus: 'COMPATIBLE' | 'INCOMPATIBLE' | 'PARTIALLY_COMPATIBLE' | string;
  incompatibilityReasons: string[];
  warningMessage?: string | null;
  explanation: string;
}

export interface PriceComparisonResult {
  basePrice: number;
  targetPrice: number;
  priceDifference: number;
  priceVariationPercentage?: number | null;
  isTemporalWindowComparable: boolean;
  timeDeltaDays?: number | null;
  temporalStatus: 'COMPARABLE' | 'NON_COMPARABLE_EXPIRED' | 'FUTURE_DATA_ANOMALY' | string;
  temporalWarningMessage?: string | null;
  baseShippingCost?: number | null;
  targetShippingCost?: number | null;
  baseShippingStatus?: 'KNOWN' | 'UNKNOWN' | 'FREE' | string;
  targetShippingStatus?: 'KNOWN' | 'UNKNOWN' | 'FREE' | string;
  baseFinalPriceWithShipping?: number | null;
  targetFinalPriceWithShipping?: number | null;
  totalPriceDifferenceWithShipping?: number | null;
  totalPriceVariationPercentageWithShipping?: number | null;
  shippingNotice?: string | null;
}

export interface ListingComparisonResult {
  baseListing: ComparableListingItem;
  targetListing: ComparableListingItem;
  similarity: EquivalenceScoreBreakdown;
  priceComparison: PriceComparisonResult;
  comparedAt?: string;
}

// Exemplos representativos padrão com casos reais de equivalência, borderline e incompatibilidade
const DEFAULT_BASE_LISTING: ComparableListingItem = {
  listingId: 'OWN-S23-128',
  title: 'Smartphone Samsung Galaxy S23 5G 128GB Preto 8GB RAM',
  brand: 'Samsung',
  model: 'Galaxy S23',
  gtin: '7891234567890',
  category: 'Celulares e Telefones',
  condition: 'NEW',
  attributes: {
    Armazenamento: '128 GB',
    'Memória RAM': '8 GB',
    Voltagem: 'Bivolt',
    Cor: 'Preto',
    Conectividade: '5G',
  },
  price: 3499.0,
  currency: 'BRL',
  shippingCost: 0.0,
  shippingStatus: 'FREE',
  finalPriceWithShipping: 3499.0,
  observedAt: new Date().toISOString(),
  marketplace: 'MERCADO_LIVRE',
  sellerName: 'Loja Oficial Própria',
  sourceUrl: 'https://lista.mercadolivre.com.br/galaxy-s23',
  isOwnListing: true,
};

const DEFAULT_COMPARISONS: ListingComparisonResult[] = [
  {
    baseListing: DEFAULT_BASE_LISTING,
    targetListing: {
      listingId: 'COMP-MLB-01',
      title: 'Samsung Galaxy S23 128GB 5G Preto Original Lacrado',
      brand: 'Samsung',
      model: 'Galaxy S23',
      gtin: '7891234567890',
      category: 'Celulares e Telefones',
      condition: 'NEW',
      attributes: {
        Armazenamento: '128 GB',
        'Memória RAM': '8 GB',
        Voltagem: 'Bivolt',
        Cor: 'Preto',
        Conectividade: '5G',
      },
      price: 3299.0,
      currency: 'BRL',
      shippingCost: 0.0,
      shippingStatus: 'FREE',
      finalPriceWithShipping: 3299.0,
      observedAt: new Date(Date.now() - 2 * 24 * 3600 * 1000).toISOString(),
      marketplace: 'MERCADO_LIVRE',
      sellerName: 'EletroTech Mega Store',
      sourceUrl: 'https://produto.mercadolivre.com.br/MLB-101',
      isOwnListing: false,
    },
    similarity: {
      gtinScore: 1.0,
      brandScore: 1.0,
      modelScore: 0.95,
      titleScore: 0.88,
      attributesScore: 1.0,
      overallScore: 0.955,
      thresholdApplied: 0.75,
      isEquivalent: true,
      isCompatible: true,
      compatibilityStatus: 'COMPATIBLE',
      incompatibilityReasons: [],
      warningMessage: null,
      explanation:
        'Correspondência exata de GTIN (7891234567890) e marca idêntica (Samsung). Atributos técnicos idênticos.',
    },
    priceComparison: {
      basePrice: 3499.0,
      targetPrice: 3299.0,
      priceDifference: -200.0,
      priceVariationPercentage: -5.72,
      isTemporalWindowComparable: true,
      timeDeltaDays: 2,
      temporalStatus: 'COMPARABLE',
      temporalWarningMessage: null,
      baseShippingCost: 0.0,
      targetShippingCost: 0.0,
      baseShippingStatus: 'FREE',
      targetShippingStatus: 'FREE',
      baseFinalPriceWithShipping: 3499.0,
      targetFinalPriceWithShipping: 3299.0,
      totalPriceDifferenceWithShipping: -200.0,
      totalPriceVariationPercentageWithShipping: -5.72,
      shippingNotice: null,
    },
    comparedAt: new Date().toISOString(),
  },
  {
    baseListing: DEFAULT_BASE_LISTING,
    targetListing: {
      listingId: 'COMP-SHOPEE-02',
      title: 'Samsung Galaxy S23 5G 256GB Preto 8GB RAM Lacrado',
      brand: 'Samsung',
      model: 'Galaxy S23',
      gtin: '7891234567999',
      category: 'Celulares e Telefones',
      condition: 'NEW',
      attributes: {
        Armazenamento: '256 GB', // Divergente
        'Memória RAM': '8 GB',
        Voltagem: 'Bivolt',
        Cor: 'Preto',
        Conectividade: '5G',
      },
      price: 3899.0,
      currency: 'BRL',
      shippingCost: 29.9,
      shippingStatus: 'KNOWN',
      finalPriceWithShipping: 3928.9,
      observedAt: new Date(Date.now() - 5 * 24 * 3600 * 1000).toISOString(),
      marketplace: 'SHOPEE',
      sellerName: 'Shopee Phone Hub',
      sourceUrl: 'https://shopee.com.br/s23-256',
      isOwnListing: false,
    },
    similarity: {
      gtinScore: 0.0,
      brandScore: 1.0,
      modelScore: 0.9,
      titleScore: 0.78,
      attributesScore: 0.7,
      overallScore: 0.625,
      thresholdApplied: 0.75,
      isEquivalent: false,
      isCompatible: true,
      compatibilityStatus: 'PARTIALLY_COMPATIBLE',
      incompatibilityReasons: ['Armazenamento divergente: 256 GB vs 128 GB'],
      warningMessage:
        'Score abaixo do limiar (62.5% < 75.0%). Variante de capacidade superior (256GB). Requer revisão manual.',
      explanation:
        'Apesar da mesma marca e linha, a capacidade de armazenamento difere (256GB vs 128GB). Não é equivalente estrito.',
    },
    priceComparison: {
      basePrice: 3499.0,
      targetPrice: 3899.0,
      priceDifference: 400.0,
      priceVariationPercentage: 11.43,
      isTemporalWindowComparable: true,
      timeDeltaDays: 5,
      temporalStatus: 'COMPARABLE',
      temporalWarningMessage: null,
      baseShippingCost: 0.0,
      targetShippingCost: 29.9,
      baseShippingStatus: 'FREE',
      targetShippingStatus: 'KNOWN',
      baseFinalPriceWithShipping: 3499.0,
      targetFinalPriceWithShipping: 3928.9,
      totalPriceDifferenceWithShipping: 429.9,
      totalPriceVariationPercentageWithShipping: 12.29,
      shippingNotice: null,
    },
    comparedAt: new Date().toISOString(),
  },
  {
    baseListing: DEFAULT_BASE_LISTING,
    targetListing: {
      listingId: 'COMP-MLB-03-USED',
      title: 'Galaxy S23 128GB Preto Usado Poucas Marcas de Uso Na Caixa',
      brand: 'Samsung',
      model: 'Galaxy S23',
      gtin: '7891234567890',
      category: 'Celulares e Telefones',
      condition: 'USED', // Incompatível por condição
      attributes: {
        Armazenamento: '128 GB',
        'Memória RAM': '8 GB',
        Voltagem: 'Bivolt',
        Cor: 'Preto',
        Conectividade: '5G',
      },
      price: 2199.0,
      currency: 'BRL',
      shippingCost: null, // Frete desconhecido
      shippingStatus: 'UNKNOWN',
      finalPriceWithShipping: null,
      observedAt: new Date(Date.now() - 1 * 24 * 3600 * 1000).toISOString(),
      marketplace: 'MERCADO_LIVRE',
      sellerName: 'Vendedor Particular RS',
      sourceUrl: 'https://produto.mercadolivre.com.br/MLB-303',
      isOwnListing: false,
    },
    similarity: {
      gtinScore: 1.0,
      brandScore: 1.0,
      modelScore: 0.95,
      titleScore: 0.72,
      attributesScore: 1.0,
      overallScore: 0.0,
      thresholdApplied: 0.75,
      isEquivalent: false,
      isCompatible: false, // INCOMPATÍVEL
      compatibilityStatus: 'INCOMPATIBLE',
      incompatibilityReasons: [
        'Condições de item incompatíveis: Base é NOVO (NEW), mas anúncio concorrente é USADO (USED).',
      ],
      warningMessage:
        'INCOMPATÍVEL: Proibido comparar anúncio Novo com Usado sem aviso impeditivo.',
      explanation:
        'O item possui condição USADO enquanto a referência é NOVO. Não é permitido equiparar preços entre produtos novos e usados.',
    },
    priceComparison: {
      basePrice: 3499.0,
      targetPrice: 2199.0,
      priceDifference: -1300.0,
      priceVariationPercentage: -37.15,
      isTemporalWindowComparable: true,
      timeDeltaDays: 1,
      temporalStatus: 'COMPARABLE',
      temporalWarningMessage: null,
      baseShippingCost: 0.0,
      targetShippingCost: null,
      baseShippingStatus: 'FREE',
      targetShippingStatus: 'UNKNOWN',
      baseFinalPriceWithShipping: 3499.0,
      targetFinalPriceWithShipping: null, // Frete desconhecido, total não inferido
      totalPriceDifferenceWithShipping: null,
      totalPriceVariationPercentageWithShipping: null,
      shippingNotice: 'Frete não informado pelo canal para o anúncio concorrente.',
    },
    comparedAt: new Date().toISOString(),
  },
  {
    baseListing: DEFAULT_BASE_LISTING,
    targetListing: {
      listingId: 'COMP-AMZ-04-IPHONE',
      title: 'Apple iPhone 13 128GB Meia-noite Tela 6,1 Câmera Dupla',
      brand: 'Apple', // Incompatível por marca
      model: 'iPhone 13',
      gtin: '194252707203',
      category: 'Celulares e Telefones',
      condition: 'NEW',
      attributes: {
        Armazenamento: '128 GB',
        'Memória RAM': '4 GB',
        Voltagem: 'Bivolt',
        Cor: 'Meia-noite',
        Conectividade: '5G',
      },
      price: 3699.0,
      currency: 'BRL',
      shippingCost: 0.0,
      shippingStatus: 'FREE',
      finalPriceWithShipping: 3699.0,
      observedAt: new Date(Date.now() - 48 * 24 * 3600 * 1000).toISOString(), // 48 dias atrás (> 30 dias)
      marketplace: 'AMAZON',
      sellerName: 'Apple Store Autorizada',
      sourceUrl: 'https://amazon.com.br/iphone-13',
      isOwnListing: false,
    },
    similarity: {
      gtinScore: 0.0,
      brandScore: 0.0,
      modelScore: 0.1,
      titleScore: 0.25,
      attributesScore: 0.4,
      overallScore: 0.0,
      thresholdApplied: 0.75,
      isEquivalent: false,
      isCompatible: false, // INCOMPATÍVEL
      compatibilityStatus: 'INCOMPATIBLE',
      incompatibilityReasons: [
        'Marcas conflitantes: Base é Samsung, alvo é Apple.',
        'Modelos distintos: Galaxy S23 vs iPhone 13.',
      ],
      warningMessage:
        'INCOMPATÍVEL: Produtos de fabricantes distintos com arquitetura incompatível.',
      explanation:
        'Marcas e modelos totalmente divergentes (Samsung vs Apple). Falsa equivalência evitada por regra de integridade.',
    },
    priceComparison: {
      basePrice: 3499.0,
      targetPrice: 3699.0,
      priceDifference: 200.0,
      priceVariationPercentage: null, // Janela temporal > 30d, variação OCULTADA
      isTemporalWindowComparable: false,
      timeDeltaDays: 48,
      temporalStatus: 'NON_COMPARABLE_EXPIRED',
      temporalWarningMessage:
        'Observações com intervalo superior a 30 dias (48 dias). Variação percentual oculta por integridade temporal.',
      baseShippingCost: 0.0,
      targetShippingCost: 0.0,
      baseShippingStatus: 'FREE',
      targetShippingStatus: 'FREE',
      baseFinalPriceWithShipping: 3499.0,
      targetFinalPriceWithShipping: 3699.0,
      totalPriceDifferenceWithShipping: null,
      totalPriceVariationPercentageWithShipping: null,
      shippingNotice: null,
    },
    comparedAt: new Date().toISOString(),
  },
];

interface ListingComparisonViewProps {
  initialBaseListing?: ComparableListingItem;
  initialComparisons?: ListingComparisonResult[];
  onOpenSearch?: () => void;
}

export function ListingComparisonView({
  initialBaseListing,
  initialComparisons,
  onOpenSearch,
}: ListingComparisonViewProps) {
  const [baseListing] = useState<ComparableListingItem>(
    initialBaseListing || DEFAULT_BASE_LISTING
  );
  const [comparisons] = useState<ListingComparisonResult[]>(
    initialComparisons || DEFAULT_COMPARISONS
  );

  // Filtros
  const [confidenceFilter, setConfidenceFilter] = useState<string>('ALL');
  const [compatibilityFilter, setCompatibilityFilter] = useState<string>('ALL');
  const [marketplaceFilter, setMarketplaceFilter] = useState<string>('ALL');
  const [viewMode, setViewMode] = useState<'matrix' | 'cards'>('matrix');

  // Modal / Drawer de Histórico de Preços
  const [priceHistoryTarget, setPriceHistoryTarget] = useState<{
    marketplace: string;
    externalListingId: string;
    title: string;
  } | null>(null);

  // Modal de Detalhes do Score (Explicabilidade)
  const [scoreBreakdownTarget, setScoreBreakdownTarget] = useState<ListingComparisonResult | null>(
    null
  );

  // Helpers de Formatação
  const formatPrice = (val: number | null | undefined, currency = 'BRL') => {
    if (val === null || val === undefined) return '---';
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: currency || 'BRL',
    }).format(val);
  };

  const formatDate = (isoStr?: string) => {
    if (!isoStr) return '---';
    try {
      return new Intl.DateTimeFormat('pt-BR', {
        dateStyle: 'short',
        timeStyle: 'short',
      }).format(new Date(isoStr));
    } catch {
      return isoStr;
    }
  };

  const formatPercentage = (val: number | null | undefined) => {
    if (val === null || val === undefined) return '---';
    const prefix = val > 0 ? '+' : '';
    return `${prefix}${val.toFixed(2)}%`;
  };

  const formatScore = (val: number) => {
    return `${(val * 100).toFixed(1)}%`;
  };

  // Filtragem
  const filteredComparisons = useMemo(() => {
    return comparisons.filter((item) => {
      // Filtro de Marketplace
      if (marketplaceFilter !== 'ALL' && item.targetListing.marketplace !== marketplaceFilter) {
        return false;
      }

      // Filtro de Compatibilidade
      if (compatibilityFilter === 'COMPATIBLE_ONLY' && !item.similarity.isCompatible) {
        return false;
      }
      if (compatibilityFilter === 'INCOMPATIBLE_ONLY' && item.similarity.isCompatible) {
        return false;
      }

      // Filtro de Confiança
      if (confidenceFilter === 'HIGH') {
        return item.similarity.isCompatible && item.similarity.overallScore >= 0.75;
      }
      if (confidenceFilter === 'BORDERLINE') {
        return (
          item.similarity.isCompatible &&
          item.similarity.overallScore >= 0.5 &&
          item.similarity.overallScore < 0.75
        );
      }
      if (confidenceFilter === 'LOW_OR_INCOMPATIBLE') {
        return !item.similarity.isCompatible || item.similarity.overallScore < 0.5;
      }

      return true;
    });
  }, [comparisons, marketplaceFilter, compatibilityFilter, confidenceFilter]);

  // Contadores para KPIs
  const highConfidenceCount = comparisons.filter(
    (c) => c.similarity.isCompatible && c.similarity.overallScore >= 0.75
  ).length;
  const borderlineCount = comparisons.filter(
    (c) =>
      c.similarity.isCompatible &&
      c.similarity.overallScore >= 0.5 &&
      c.similarity.overallScore < 0.75
  ).length;
  const incompatibleCount = comparisons.filter((c) => !c.similarity.isCompatible).length;

  return (
    <div className="space-y-6">
      {/* Cabeçalho da Seção */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center gap-2">
            <span>⚖️ Comparação Lado a Lado de Listings Equivalentes</span>
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Contraste estruturado entre produto próprio de referência e anúncios de mercado.
            Identificação transparente de diferenças com alerta contra falsa equivalência.
          </p>
        </div>

        {onOpenSearch && (
          <button
            onClick={onOpenSearch}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-semibold transition-colors min-h-[44px] flex items-center justify-center gap-2 shadow-sm"
          >
            <span>🔍 Buscar Novos Candidatos</span>
          </button>
        )}
      </div>

      {/* Regra de Ouro / Banner Informativo */}
      <div className="p-4 rounded-xl bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-blue-950/40 dark:to-indigo-950/30 border border-blue-200 dark:border-blue-800 text-xs text-blue-900 dark:text-blue-200 space-y-1 shadow-sm">
        <div className="flex items-center gap-2 font-bold text-sm text-blue-950 dark:text-blue-100">
          <span>🛡️ Princípios de Integridade Comparativa</span>
        </div>
        <p>
          <strong>1. Diferenças Claras:</strong> Marca, modelo, condição, especificações e GTIN são contrastados atributo a atributo.
        </p>
        <p>
          <strong>2. Sem Falsa Equivalência:</strong> Anúncios com incompatibilidade estrutural (ex.: NOVO vs USADO ou marcas divergentes) são sinalizados e proibidos de receber paridade visual.
        </p>
        <p>
          <strong>3. Variação Temporal Segura & Frete:</strong> Observações com intervalo superior a 30 dias têm percentuais de variação de preço ocultados, e frete não informado nunca é inventado.
        </p>
      </div>

      {/* KPI Cards de Resumo da Análise */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div className="p-3.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
          <span className="text-xs font-medium text-slate-500 dark:text-slate-400">Total Analisado</span>
          <div className="mt-1 flex items-baseline justify-between">
            <span className="text-2xl font-bold text-slate-900 dark:text-white">
              {comparisons.length}
            </span>
            <span className="text-xs text-slate-400">Anúncios</span>
          </div>
        </div>

        <div className="p-3.5 rounded-xl border border-emerald-200 dark:border-emerald-800/60 bg-emerald-50/40 dark:bg-emerald-950/20 shadow-sm">
          <span className="text-xs font-semibold text-emerald-700 dark:text-emerald-400">
            Alta Confiança (≥ 75%)
          </span>
          <div className="mt-1 flex items-baseline justify-between">
            <span className="text-2xl font-bold text-emerald-800 dark:text-emerald-300">
              {highConfidenceCount}
            </span>
            <span className="text-xs text-emerald-600/70">Equivalentes</span>
          </div>
        </div>

        <div className="p-3.5 rounded-xl border border-amber-200 dark:border-amber-800/60 bg-amber-50/40 dark:bg-amber-950/20 shadow-sm">
          <span className="text-xs font-semibold text-amber-700 dark:text-amber-400">
            Revisão / Borderline (50-74%)
          </span>
          <div className="mt-1 flex items-baseline justify-between">
            <span className="text-2xl font-bold text-amber-800 dark:text-amber-300">
              {borderlineCount}
            </span>
            <span className="text-xs text-amber-600/70">Divergências</span>
          </div>
        </div>

        <div className="p-3.5 rounded-xl border border-red-200 dark:border-red-800/60 bg-red-50/40 dark:bg-red-950/20 shadow-sm">
          <span className="text-xs font-semibold text-red-700 dark:text-red-400">
            Incompatíveis / Baixa Conf.
          </span>
          <div className="mt-1 flex items-baseline justify-between">
            <span className="text-2xl font-bold text-red-800 dark:text-red-300">
              {incompatibleCount}
            </span>
            <span className="text-xs text-red-600/70">Não Equivalentes</span>
          </div>
        </div>
      </div>

      {/* Barra de Filtros e Visualização */}
      <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div className="flex flex-wrap items-center gap-3">
          {/* Filtro Confiança */}
          <div>
            <label
              htmlFor="filter-confidence"
              className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1"
            >
              Nível de Confiança
            </label>
            <select
              id="filter-confidence"
              aria-label="Nível de Confiança"
              value={confidenceFilter}
              onChange={(e) => setConfidenceFilter(e.target.value)}
              className="text-xs border border-slate-300 dark:border-slate-700 rounded-lg px-2.5 py-2 bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-200 min-h-[44px]"
            >
              <option value="ALL">Todos os Níveis</option>
              <option value="HIGH">Alta Confiança (≥ 75%)</option>
              <option value="BORDERLINE">Revisão / Borderline (50-74%)</option>
              <option value="LOW_OR_INCOMPATIBLE">Incompatíveis / Baixa Confiança (&lt; 50%)</option>
            </select>
          </div>

          {/* Filtro Compatibilidade */}
          <div>
            <label
              htmlFor="filter-compatibility"
              className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1"
            >
              Status de Compatibilidade
            </label>
            <select
              id="filter-compatibility"
              aria-label="Status de Compatibilidade"
              value={compatibilityFilter}
              onChange={(e) => setCompatibilityFilter(e.target.value)}
              className="text-xs border border-slate-300 dark:border-slate-700 rounded-lg px-2.5 py-2 bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-200 min-h-[44px]"
            >
              <option value="ALL">Todas as Condições</option>
              <option value="COMPATIBLE_ONLY">Apenas Compatíveis</option>
              <option value="INCOMPATIBLE_ONLY">Apenas Incompatíveis</option>
            </select>
          </div>

          {/* Filtro Marketplace */}
          <div>
            <label
              htmlFor="filter-marketplace"
              className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1"
            >
              Marketplace
            </label>
            <select
              id="filter-marketplace"
              aria-label="Marketplace"
              value={marketplaceFilter}
              onChange={(e) => setMarketplaceFilter(e.target.value)}
              className="text-xs border border-slate-300 dark:border-slate-700 rounded-lg px-2.5 py-2 bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-200 min-h-[44px]"
            >
              <option value="ALL">Todos os Canais</option>
              <option value="MERCADO_LIVRE">Mercado Livre</option>
              <option value="SHOPEE">Shopee</option>
              <option value="AMAZON">Amazon</option>
            </select>
          </div>
        </div>

        {/* Modo de Visualização */}
        <div className="flex items-center gap-1 bg-slate-100 dark:bg-slate-800 p-1 rounded-lg self-start md:self-end">
          <button
            type="button"
            onClick={() => setViewMode('matrix')}
            aria-pressed={viewMode === 'matrix'}
            className={`px-3 py-1.5 text-xs font-semibold rounded-md min-h-[38px] transition-colors ${
              viewMode === 'matrix'
                ? 'bg-white dark:bg-slate-700 text-blue-600 dark:text-blue-400 shadow-sm'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
            }`}
          >
            📊 Matriz Lado a Lado
          </button>
          <button
            type="button"
            onClick={() => setViewMode('cards')}
            aria-pressed={viewMode === 'cards'}
            className={`px-3 py-1.5 text-xs font-semibold rounded-md min-h-[38px] transition-colors ${
              viewMode === 'cards'
                ? 'bg-white dark:bg-slate-700 text-blue-600 dark:text-blue-400 shadow-sm'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
            }`}
          >
            🗂️ Cards de Análise
          </button>
        </div>
      </div>

      {/* Conteúdo Principal */}
      {filteredComparisons.length === 0 ? (
        <div className="p-12 text-center border border-slate-200 dark:border-slate-800 rounded-xl bg-white dark:bg-slate-900">
          <span className="text-3xl">🔍</span>
          <p className="mt-2 text-sm font-semibold text-slate-700 dark:text-slate-300">
            Nenhum anúncio corresponde aos filtros selecionados.
          </p>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Experimente selecionar &quot;Todos os Níveis&quot; ou &quot;Todas as Condições&quot;.
          </p>
        </div>
      ) : viewMode === 'matrix' ? (
        /* VISUALIZAÇÃO MATRIZ LADO A LADO */
        <div className="border border-slate-200 dark:border-slate-800 rounded-xl bg-white dark:bg-slate-900 shadow-sm overflow-x-auto">
          <table className="w-full text-left text-xs min-w-[900px] border-collapse">
            {/* Headers da Tabela */}
            <thead>
              <tr className="border-b border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/60">
                <th className="py-3.5 px-4 font-bold text-slate-600 dark:text-slate-300 w-44 sticky left-0 bg-slate-50 dark:bg-slate-800/90 z-10">
                  Critério / Atributo
                </th>
                {/* Coluna Anúncio Base */}
                <th className="py-3.5 px-4 font-bold text-slate-900 dark:text-white w-64 border-x border-slate-200 dark:border-slate-800 bg-blue-50/40 dark:bg-blue-950/30">
                  <div className="space-y-1">
                    <span className="inline-block px-2 py-0.5 rounded text-[10px] font-bold bg-blue-100 text-blue-800 dark:bg-blue-900/60 dark:text-blue-300">
                      🎯 REFERÊNCIA PRÓPRIA
                    </span>
                    <p className="font-semibold text-xs text-slate-900 dark:text-white line-clamp-2">
                      {baseListing.title}
                    </p>
                    <span className="text-[10px] text-slate-500 dark:text-slate-400">
                      {baseListing.marketplace} &middot; {baseListing.sellerName}
                    </span>
                  </div>
                </th>
                {/* Colunas Candidatos */}
                {filteredComparisons.map((item) => {
                  const isComp = item.similarity.isCompatible;
                  const score = item.similarity.overallScore;
                  const isHigh = isComp && score >= 0.75;
                  const isBorderline = isComp && score >= 0.5 && score < 0.75;

                  return (
                    <th
                      key={item.targetListing.listingId}
                      className={`py-3.5 px-4 font-bold w-64 border-r border-slate-200 dark:border-slate-800 align-top ${
                        !isComp
                          ? 'bg-red-50/25 dark:bg-red-950/20'
                          : isBorderline
                          ? 'bg-amber-50/25 dark:bg-amber-950/20'
                          : 'bg-white dark:bg-slate-900'
                      }`}
                    >
                      <div className="space-y-1.5">
                        {/* Status de Equivalência no Cabeçalho */}
                        {!isComp ? (
                          <span className="inline-block px-2 py-0.5 rounded text-[10px] font-bold bg-red-100 text-red-800 dark:bg-red-900/70 dark:text-red-200 border border-red-300 dark:border-red-800">
                            ⛔ INCOMPATÍVEL
                          </span>
                        ) : isHigh ? (
                          <span className="inline-block px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800 dark:bg-emerald-900/60 dark:text-emerald-300 border border-emerald-300 dark:border-emerald-800">
                            ✅ EQUIVALENTE (ALTA CONF.)
                          </span>
                        ) : (
                          <span className="inline-block px-2 py-0.5 rounded text-[10px] font-bold bg-amber-100 text-amber-800 dark:bg-amber-900/60 dark:text-amber-300 border border-amber-300 dark:border-amber-800">
                            ⚠️ NÃO EQUIVALENTE (REVISÃO)
                          </span>
                        )}

                        <p className="font-semibold text-xs text-slate-900 dark:text-white line-clamp-2">
                          {item.targetListing.title}
                        </p>
                        <p className="text-[10px] text-slate-500 dark:text-slate-400">
                          {item.targetListing.marketplace} &middot; {item.targetListing.sellerName}
                        </p>

                        <div className="pt-1 flex items-center justify-between gap-1">
                          <button
                            type="button"
                            onClick={() => setScoreBreakdownTarget(item)}
                            className="text-[10px] text-blue-600 dark:text-blue-400 hover:underline min-h-[36px] flex items-center gap-1 font-semibold"
                          >
                            <span>🔍 Ver Score ({formatScore(score)})</span>
                          </button>

                          <button
                            type="button"
                            onClick={() =>
                              setPriceHistoryTarget({
                                marketplace: item.targetListing.marketplace,
                                externalListingId: item.targetListing.listingId,
                                title: item.targetListing.title,
                              })
                            }
                            className="text-[10px] text-slate-600 dark:text-slate-300 hover:text-blue-600 dark:hover:text-blue-400 hover:underline min-h-[36px] flex items-center gap-1"
                          >
                            <span>📈 Histórico</span>
                          </button>
                        </div>
                      </div>
                    </th>
                  );
                })}
              </tr>
            </thead>

            {/* Linhas da Matriz */}
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {/* LINHA: ALERTA ANTI-FALSA EQUIVALÊNCIA */}
              {filteredComparisons.some((item) => !item.similarity.isCompatible) && (
                <tr className="bg-red-50/40 dark:bg-red-950/30">
                  <td className="py-3 px-4 font-bold text-red-700 dark:text-red-300 sticky left-0 bg-red-50/90 dark:bg-red-950/90 z-10">
                    Alerta de Integridade
                  </td>
                  <td className="py-3 px-4 border-x border-slate-200 dark:border-slate-800 text-slate-400">
                    ---
                  </td>
                  {filteredComparisons.map((item) => {
                    const isComp = item.similarity.isCompatible;
                    return (
                      <td
                        key={`warn-${item.targetListing.listingId}`}
                        className="py-3 px-4 border-r border-slate-200 dark:border-slate-800"
                      >
                        {!isComp ? (
                          <div className="p-2.5 rounded-lg bg-red-100/70 dark:bg-red-900/50 border border-red-300 dark:border-red-700 text-[11px] text-red-900 dark:text-red-200 space-y-1">
                            <span className="font-bold flex items-center gap-1">
                              ⛔ ALERTA ANTI-FALSA EQUIVALÊNCIA
                            </span>
                            <p>
                              Este item possui divergências estruturais críticas. NÃO deve ser usado
                              como balizador de preço.
                            </p>
                            {item.similarity.incompatibilityReasons.map((reason, rIdx) => (
                              <p key={rIdx} className="font-medium text-red-800 dark:text-red-300">
                                &bull; {reason}
                              </p>
                            ))}
                          </div>
                        ) : (
                          <span className="text-slate-400">Compatível</span>
                        )}
                      </td>
                    );
                  })}
                </tr>
              )}

              {/* LINHA: PREÇO BASE & VARIAÇÃO */}
              <tr>
                <td className="py-3 px-4 font-semibold text-slate-700 dark:text-slate-300 sticky left-0 bg-white dark:bg-slate-900 z-10">
                  Preço do Anúncio
                </td>
                <td className="py-3 px-4 border-x border-slate-200 dark:border-slate-800 font-bold text-slate-900 dark:text-white bg-blue-50/20 dark:bg-blue-950/10">
                  {formatPrice(baseListing.price, baseListing.currency)}
                </td>
                {filteredComparisons.map((item) => {
                  const targetPrice = item.targetListing.price;
                  const diff = item.priceComparison.priceDifference;
                  const pct = item.priceComparison.priceVariationPercentage;
                  const isTemporalValid = item.priceComparison.isTemporalWindowComparable;

                  return (
                    <td
                      key={`price-${item.targetListing.listingId}`}
                      className="py-3 px-4 border-r border-slate-200 dark:border-slate-800"
                    >
                      <div className="space-y-1">
                        <span className="font-bold text-sm text-slate-900 dark:text-white">
                          {formatPrice(targetPrice, item.targetListing.currency)}
                        </span>

                        {isTemporalValid ? (
                          <div
                            className={`text-[11px] font-semibold flex items-center gap-1 ${
                              diff < 0
                                ? 'text-emerald-600 dark:text-emerald-400'
                                : diff > 0
                                ? 'text-red-600 dark:text-red-400'
                                : 'text-slate-600 dark:text-slate-400'
                            }`}
                          >
                            <span>
                              {diff < 0 ? '▼' : diff > 0 ? '▲' : '='} {formatPrice(diff)} (
                              {formatPercentage(pct)})
                            </span>
                          </div>
                        ) : (
                          <div className="p-1 rounded bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800 text-[10px] text-amber-800 dark:text-amber-300">
                            ⚠️ Janela &gt; 30d (Variação oculta)
                          </div>
                        )}
                      </div>
                    </td>
                  );
                })}
              </tr>

              {/* LINHA: FRETE */}
              <tr>
                <td className="py-3 px-4 font-semibold text-slate-700 dark:text-slate-300 sticky left-0 bg-white dark:bg-slate-900 z-10">
                  Frete
                </td>
                <td className="py-3 px-4 border-x border-slate-200 dark:border-slate-800 bg-blue-50/20 dark:bg-blue-950/10">
                  {baseListing.shippingCost === 0 ? (
                    <span className="font-semibold text-emerald-600 dark:text-emerald-400">
                      ✓ Grátis
                    </span>
                  ) : baseListing.shippingCost !== null &&
                    baseListing.shippingCost !== undefined ? (
                    formatPrice(baseListing.shippingCost, baseListing.currency)
                  ) : (
                    <span className="text-slate-400">Não informado</span>
                  )}
                </td>
                {filteredComparisons.map((item) => {
                  const status = item.priceComparison.targetShippingStatus;
                  const cost = item.targetListing.shippingCost;

                  return (
                    <td
                      key={`shipping-${item.targetListing.listingId}`}
                      className="py-3 px-4 border-r border-slate-200 dark:border-slate-800"
                    >
                      {status === 'UNKNOWN' ? (
                        <div className="text-[11px] text-amber-700 dark:text-amber-400 font-medium flex items-center gap-1">
                          <span>⚠️ Não informado pelo canal</span>
                        </div>
                      ) : cost === 0 || status === 'FREE' ? (
                        <span className="font-semibold text-emerald-600 dark:text-emerald-400">
                          ✓ Grátis
                        </span>
                      ) : (
                        <span>{formatPrice(cost, item.targetListing.currency)}</span>
                      )}
                    </td>
                  );
                })}
              </tr>

              {/* LINHA: PREÇO TOTAL COM FRETE */}
              <tr>
                <td className="py-3 px-4 font-semibold text-slate-700 dark:text-slate-300 sticky left-0 bg-white dark:bg-slate-900 z-10">
                  Preço Total c/ Frete
                </td>
                <td className="py-3 px-4 border-x border-slate-200 dark:border-slate-800 font-bold bg-blue-50/20 dark:bg-blue-950/10">
                  {baseListing.finalPriceWithShipping !== null &&
                  baseListing.finalPriceWithShipping !== undefined ? (
                    formatPrice(baseListing.finalPriceWithShipping, baseListing.currency)
                  ) : (
                    <span className="text-slate-400">Frete pendente</span>
                  )}
                </td>
                {filteredComparisons.map((item) => {
                  const finalPrice = item.priceComparison.targetFinalPriceWithShipping;
                  const totalDiff = item.priceComparison.totalPriceDifferenceWithShipping;
                  const totalPct = item.priceComparison.totalPriceVariationPercentageWithShipping;

                  return (
                    <td
                      key={`total-${item.targetListing.listingId}`}
                      className="py-3 px-4 border-r border-slate-200 dark:border-slate-800"
                    >
                      {finalPrice !== null && finalPrice !== undefined ? (
                        <div className="space-y-0.5">
                          <span className="font-bold text-slate-900 dark:text-white">
                            {formatPrice(finalPrice, item.targetListing.currency)}
                          </span>
                          {totalDiff !== null && totalDiff !== undefined && (
                            <p className="text-[10px] text-slate-500">
                              Diff: {formatPrice(totalDiff)} ({formatPercentage(totalPct)})
                            </p>
                          )}
                        </div>
                      ) : (
                        <span className="text-slate-400 italic">
                          -- (Frete desconhecido não inferido)
                        </span>
                      )}
                    </td>
                  );
                })}
              </tr>

              {/* LINHA: CONDIÇÃO DO ITEM */}
              <tr>
                <td className="py-3 px-4 font-semibold text-slate-700 dark:text-slate-300 sticky left-0 bg-white dark:bg-slate-900 z-10">
                  Condição
                </td>
                <td className="py-3 px-4 border-x border-slate-200 dark:border-slate-800 bg-blue-50/20 dark:bg-blue-950/10 font-semibold">
                  {baseListing.condition === 'NEW' ? 'Novo (Lacrado)' : baseListing.condition}
                </td>
                {filteredComparisons.map((item) => {
                  const isSame = item.targetListing.condition === baseListing.condition;
                  return (
                    <td
                      key={`cond-${item.targetListing.listingId}`}
                      className={`py-3 px-4 border-r border-slate-200 dark:border-slate-800 ${
                        !isSame ? 'bg-red-50/30 dark:bg-red-950/20' : ''
                      }`}
                    >
                      <div className="flex items-center gap-1.5">
                        <span
                          className={`text-xs px-2 py-0.5 rounded font-semibold ${
                            isSame
                              ? 'bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-200'
                              : 'bg-red-100 text-red-800 dark:bg-red-900/60 dark:text-red-300'
                          }`}
                        >
                          {item.targetListing.condition === 'NEW'
                            ? 'Novo'
                            : item.targetListing.condition === 'USED'
                            ? 'Usado'
                            : item.targetListing.condition}
                        </span>
                        <span className="text-[10px]">
                          {isSame ? (
                            <span className="text-emerald-600 font-bold">✓ Idêntico</span>
                          ) : (
                            <span className="text-red-600 font-bold">⚠️ Divergente</span>
                          )}
                        </span>
                      </div>
                    </td>
                  );
                })}
              </tr>

              {/* LINHA: MARCA */}
              <tr>
                <td className="py-3 px-4 font-semibold text-slate-700 dark:text-slate-300 sticky left-0 bg-white dark:bg-slate-900 z-10">
                  Marca
                </td>
                <td className="py-3 px-4 border-x border-slate-200 dark:border-slate-800 bg-blue-50/20 dark:bg-blue-950/10 font-semibold">
                  {baseListing.brand || '---'}
                </td>
                {filteredComparisons.map((item) => {
                  const isSame =
                    (item.targetListing.brand || '').toLowerCase() ===
                    (baseListing.brand || '').toLowerCase();
                  return (
                    <td
                      key={`brand-${item.targetListing.listingId}`}
                      className={`py-3 px-4 border-r border-slate-200 dark:border-slate-800 ${
                        !isSame ? 'bg-red-50/30 dark:bg-red-950/20' : ''
                      }`}
                    >
                      <div className="flex items-center gap-1.5">
                        <span className="font-semibold text-slate-900 dark:text-white">
                          {item.targetListing.brand || '---'}
                        </span>
                        <span className="text-[10px]">
                          {isSame ? (
                            <span className="text-emerald-600 font-bold">✓ Idêntico</span>
                          ) : (
                            <span className="text-red-600 font-bold">⚠️ Divergente</span>
                          )}
                        </span>
                      </div>
                    </td>
                  );
                })}
              </tr>

              {/* LINHA: MODELO */}
              <tr>
                <td className="py-3 px-4 font-semibold text-slate-700 dark:text-slate-300 sticky left-0 bg-white dark:bg-slate-900 z-10">
                  Modelo
                </td>
                <td className="py-3 px-4 border-x border-slate-200 dark:border-slate-800 bg-blue-50/20 dark:bg-blue-950/10 font-semibold">
                  {baseListing.model || '---'}
                </td>
                {filteredComparisons.map((item) => {
                  const isSame =
                    (item.targetListing.model || '').toLowerCase() ===
                    (baseListing.model || '').toLowerCase();
                  return (
                    <td
                      key={`model-${item.targetListing.listingId}`}
                      className={`py-3 px-4 border-r border-slate-200 dark:border-slate-800 ${
                        !isSame ? 'bg-amber-50/30 dark:bg-amber-950/20' : ''
                      }`}
                    >
                      <div className="flex items-center gap-1.5">
                        <span className="font-semibold text-slate-900 dark:text-white">
                          {item.targetListing.model || '---'}
                        </span>
                        <span className="text-[10px]">
                          {isSame ? (
                            <span className="text-emerald-600 font-bold">✓ Idêntico</span>
                          ) : (
                            <span className="text-amber-600 font-bold">⚠️ Divergente</span>
                          )}
                        </span>
                      </div>
                    </td>
                  );
                })}
              </tr>

              {/* LINHA: GTIN / EAN */}
              <tr>
                <td className="py-3 px-4 font-semibold text-slate-700 dark:text-slate-300 sticky left-0 bg-white dark:bg-slate-900 z-10">
                  GTIN / EAN
                </td>
                <td className="py-3 px-4 border-x border-slate-200 dark:border-slate-800 bg-blue-50/20 dark:bg-blue-950/10 font-mono">
                  {baseListing.gtin || 'Não informado'}
                </td>
                {filteredComparisons.map((item) => {
                  const isSame =
                    Boolean(baseListing.gtin && item.targetListing.gtin) &&
                    baseListing.gtin === item.targetListing.gtin;
                  return (
                    <td
                      key={`gtin-${item.targetListing.listingId}`}
                      className="py-3 px-4 border-r border-slate-200 dark:border-slate-800 font-mono"
                    >
                      <div className="flex items-center gap-1.5">
                        <span>{item.targetListing.gtin || 'Não informado'}</span>
                        {item.targetListing.gtin && baseListing.gtin && (
                          <span className="text-[10px]">
                            {isSame ? (
                              <span className="text-emerald-600 font-bold">✓ Idêntico</span>
                            ) : (
                              <span className="text-amber-600 font-bold">⚠️ Divergente</span>
                            )}
                          </span>
                        )}
                      </div>
                    </td>
                  );
                })}
              </tr>

              {/* LINHAS: ESPECIFICAÇÕES TÉCNICAS RELEVANTES */}
              {['Armazenamento', 'Memória RAM', 'Voltagem'].map((attrKey) => {
                const baseVal = baseListing.attributes?.[attrKey] || '---';

                return (
                  <tr key={`attr-${attrKey}`}>
                    <td className="py-3 px-4 font-semibold text-slate-700 dark:text-slate-300 sticky left-0 bg-white dark:bg-slate-900 z-10">
                      {attrKey}
                    </td>
                    <td className="py-3 px-4 border-x border-slate-200 dark:border-slate-800 bg-blue-50/20 dark:bg-blue-950/10 font-medium">
                      {baseVal}
                    </td>
                    {filteredComparisons.map((item) => {
                      const targetVal = item.targetListing.attributes?.[attrKey] || '---';
                      const isSame =
                        baseVal.toLowerCase() === targetVal.toLowerCase() && baseVal !== '---';

                      return (
                        <td
                          key={`attr-${attrKey}-${item.targetListing.listingId}`}
                          className={`py-3 px-4 border-r border-slate-200 dark:border-slate-800 ${
                            !isSame && baseVal !== '---' && targetVal !== '---'
                              ? 'bg-amber-50/30 dark:bg-amber-950/20'
                              : ''
                          }`}
                        >
                          <div className="flex items-center gap-1.5">
                            <span className="font-medium text-slate-900 dark:text-white">
                              {targetVal}
                            </span>
                            {baseVal !== '---' && targetVal !== '---' && (
                              <span className="text-[10px]">
                                {isSame ? (
                                  <span className="text-emerald-600 font-bold">✓ Idêntico</span>
                                ) : (
                                  <span className="text-amber-600 font-bold">⚠️ Divergente</span>
                                )}
                              </span>
                            )}
                          </div>
                        </td>
                      );
                    })}
                  </tr>
                );
              })}

              {/* LINHA: DATA DA OBSERVAÇÃO & JANELA */}
              <tr>
                <td className="py-3 px-4 font-semibold text-slate-700 dark:text-slate-300 sticky left-0 bg-white dark:bg-slate-900 z-10">
                  Data da Observação
                </td>
                <td className="py-3 px-4 border-x border-slate-200 dark:border-slate-800 bg-blue-50/20 dark:bg-blue-950/10 font-mono">
                  {formatDate(baseListing.observedAt)}
                </td>
                {filteredComparisons.map((item) => {
                  const deltaDays = item.priceComparison.timeDeltaDays;
                  const isTemporalValid = item.priceComparison.isTemporalWindowComparable;

                  return (
                    <td
                      key={`date-${item.targetListing.listingId}`}
                      className="py-3 px-4 border-r border-slate-200 dark:border-slate-800 font-mono"
                    >
                      <div className="space-y-0.5">
                        <p>{formatDate(item.targetListing.observedAt)}</p>
                        <p
                          className={`text-[10px] font-semibold ${
                            isTemporalValid
                              ? 'text-slate-500'
                              : 'text-amber-600 dark:text-amber-400'
                          }`}
                        >
                          {deltaDays !== null && deltaDays !== undefined
                            ? deltaDays === 0
                              ? 'Mesmo dia'
                              : `Defasagem: ${deltaDays} dias`
                            : '---'}
                          {!isTemporalValid && ' (> 30d)'}
                        </p>
                      </div>
                    </td>
                  );
                })}
              </tr>

              {/* LINHA: NÍVEL DE CONFIANÇA & EXPLICABILIDADE */}
              <tr>
                <td className="py-3 px-4 font-bold text-slate-800 dark:text-slate-200 sticky left-0 bg-white dark:bg-slate-900 z-10">
                  Score de Similaridade
                </td>
                <td className="py-3 px-4 border-x border-slate-200 dark:border-slate-800 bg-blue-50/20 dark:bg-blue-950/10 font-bold text-blue-700 dark:text-blue-300">
                  100% (Referência)
                </td>
                {filteredComparisons.map((item) => {
                  const isComp = item.similarity.isCompatible;
                  const score = item.similarity.overallScore;
                  const isHigh = isComp && score >= 0.75;
                  const isBorderline = isComp && score >= 0.5 && score < 0.75;

                  return (
                    <td
                      key={`score-${item.targetListing.listingId}`}
                      className="py-3 px-4 border-r border-slate-200 dark:border-slate-800"
                    >
                      <div className="space-y-1.5">
                        <div className="flex items-center justify-between">
                          <span
                            className={`px-2 py-0.5 rounded text-xs font-bold ${
                              !isComp
                                ? 'bg-red-100 text-red-800 dark:bg-red-900/60 dark:text-red-300'
                                : isHigh
                                ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/60 dark:text-emerald-300'
                                : isBorderline
                                ? 'bg-amber-100 text-amber-800 dark:bg-amber-900/60 dark:text-amber-300'
                                : 'bg-red-100 text-red-800 dark:bg-red-900/60 dark:text-red-300'
                            }`}
                          >
                            {formatScore(score)}
                          </span>

                          <span className="text-[10px] text-slate-400">Limiar: 75%</span>
                        </div>

                        <p className="text-[10px] text-slate-500 line-clamp-2">
                          {item.similarity.explanation}
                        </p>

                        <button
                          type="button"
                          onClick={() => setScoreBreakdownTarget(item)}
                          className="text-[11px] font-semibold text-blue-600 dark:text-blue-400 hover:underline min-h-[36px] flex items-center"
                        >
                          Explicabilidade Detalhada &rarr;
                        </button>
                      </div>
                    </td>
                  );
                })}
              </tr>
            </tbody>
          </table>
        </div>
      ) : (
        /* VISUALIZAÇÃO EM CARDS DE ANÁLISE */
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredComparisons.map((item) => {
            const isComp = item.similarity.isCompatible;
            const score = item.similarity.overallScore;
            const isHigh = isComp && score >= 0.75;
            const isBorderline = isComp && score >= 0.5 && score < 0.75;
            const diff = item.priceComparison.priceDifference;
            const pct = item.priceComparison.priceVariationPercentage;
            const isTemporalValid = item.priceComparison.isTemporalWindowComparable;

            return (
              <div
                key={item.targetListing.listingId}
                className={`rounded-xl border p-5 flex flex-col justify-between shadow-sm transition-shadow ${
                  !isComp
                    ? 'border-red-300 dark:border-red-800 bg-red-50/15 dark:bg-red-950/20'
                    : isBorderline
                    ? 'border-amber-300 dark:border-amber-800 bg-amber-50/15 dark:bg-amber-950/20'
                    : 'border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900'
                }`}
              >
                <div className="space-y-4">
                  {/* Topo do Card */}
                  <div className="flex items-center justify-between gap-2">
                    {!isComp ? (
                      <span className="px-2.5 py-1 rounded-md text-xs font-bold bg-red-100 text-red-800 dark:bg-red-900/60 dark:text-red-200 border border-red-300 dark:border-red-800">
                        ⛔ INCOMPATÍVEL
                      </span>
                    ) : isHigh ? (
                      <span className="px-2.5 py-1 rounded-md text-xs font-bold bg-emerald-100 text-emerald-800 dark:bg-emerald-900/60 dark:text-emerald-300 border border-emerald-300 dark:border-emerald-800">
                        ✅ EQUIVALENTE (ALTA CONF.)
                      </span>
                    ) : (
                      <span className="px-2.5 py-1 rounded-md text-xs font-bold bg-amber-100 text-amber-800 dark:bg-amber-900/60 dark:text-amber-300 border border-amber-300 dark:border-amber-800">
                        ⚠️ NÃO EQUIVALENTE (REVISÃO)
                      </span>
                    )}

                    <span className="text-xs font-mono font-bold text-slate-700 dark:text-slate-300">
                      Score: {formatScore(score)}
                    </span>
                  </div>

                  {/* Alerta Anti-Falsa Equivalência */}
                  {!isComp && (
                    <div className="p-3 rounded-lg bg-red-100/80 dark:bg-red-900/40 border border-red-300 dark:border-red-700 text-xs text-red-900 dark:text-red-200 space-y-1">
                      <p className="font-bold">⛔ ALERTA ANTI-FALSA EQUIVALÊNCIA</p>
                      <p className="text-[11px]">
                        Não utilize este item como balizador de preços. Ele difere em aspectos
                        essenciais:
                      </p>
                      {item.similarity.incompatibilityReasons.map((r, rIdx) => (
                        <p key={rIdx} className="font-semibold text-[11px]">
                          &bull; {r}
                        </p>
                      ))}
                    </div>
                  )}

                  {/* Título e Canal */}
                  <div>
                    <h3 className="text-sm font-bold text-slate-900 dark:text-white line-clamp-2">
                      {item.targetListing.title}
                    </h3>
                    <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                      {item.targetListing.marketplace} &middot; Vendedor:{' '}
                      {item.targetListing.sellerName}
                    </p>
                  </div>

                  {/* Bloco de Preços */}
                  <div className="p-3 rounded-lg bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-800 space-y-1.5">
                    <div className="flex items-baseline justify-between">
                      <span className="text-xs text-slate-500">Preço Concorrente:</span>
                      <span className="text-base font-bold text-slate-900 dark:text-white">
                        {formatPrice(item.targetListing.price, item.targetListing.currency)}
                      </span>
                    </div>

                    <div className="flex items-baseline justify-between text-xs">
                      <span className="text-slate-500">Preço de Referência:</span>
                      <span className="font-medium text-slate-700 dark:text-slate-300">
                        {formatPrice(baseListing.price, baseListing.currency)}
                      </span>
                    </div>

                    {isTemporalValid ? (
                      <div className="flex items-baseline justify-between text-xs pt-1 border-t border-slate-200 dark:border-slate-700">
                        <span className="font-semibold text-slate-600 dark:text-slate-300">
                          Variação:
                        </span>
                        <span
                          className={`font-bold ${
                            diff < 0
                              ? 'text-emerald-600 dark:text-emerald-400'
                              : diff > 0
                              ? 'text-red-600 dark:text-red-400'
                              : 'text-slate-600'
                          }`}
                        >
                          {diff < 0 ? '▼' : diff > 0 ? '▲' : '='} {formatPrice(diff)} (
                          {formatPercentage(pct)})
                        </span>
                      </div>
                    ) : (
                      <div className="p-2 rounded bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800 text-[11px] text-amber-800 dark:text-amber-300">
                        ⚠️ Intervalo &gt; 30 dias entre observações. Variação percentual oculta.
                      </div>
                    )}

                    <div className="flex items-baseline justify-between text-xs">
                      <span className="text-slate-500">Frete:</span>
                      <span className="font-medium">
                        {item.priceComparison.targetShippingStatus === 'UNKNOWN' ? (
                          <span className="text-amber-600 font-semibold">
                            ⚠️ Não informado pelo canal
                          </span>
                        ) : item.targetListing.shippingCost === 0 ? (
                          <span className="text-emerald-600 font-bold">Grátis</span>
                        ) : (
                          formatPrice(
                            item.targetListing.shippingCost,
                            item.targetListing.currency
                          )
                        )}
                      </span>
                    </div>
                  </div>

                  {/* Resumo de Atributos Divergentes */}
                  <div className="space-y-1 text-xs">
                    <span className="font-semibold text-slate-600 dark:text-slate-300 block">
                      Diferenças de Atributos:
                    </span>
                    <div className="grid grid-cols-2 gap-1.5 text-[11px]">
                      <div className="p-1.5 rounded bg-slate-100 dark:bg-slate-800">
                        <span className="text-slate-400 block text-[10px]">Condição</span>
                        <span className="font-semibold">
                          {item.targetListing.condition === baseListing.condition ? (
                            <span className="text-emerald-600">✓ Idêntico</span>
                          ) : (
                            <span className="text-red-600">⚠️ {item.targetListing.condition}</span>
                          )}
                        </span>
                      </div>
                      <div className="p-1.5 rounded bg-slate-100 dark:bg-slate-800">
                        <span className="text-slate-400 block text-[10px]">Marca</span>
                        <span className="font-semibold">
                          {item.targetListing.brand === baseListing.brand ? (
                            <span className="text-emerald-600">✓ Idêntico</span>
                          ) : (
                            <span className="text-red-600">⚠️ {item.targetListing.brand}</span>
                          )}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Ações do Card */}
                <div className="mt-5 pt-3 border-t border-slate-200 dark:border-slate-800 flex items-center justify-between gap-2">
                  <button
                    type="button"
                    onClick={() => setScoreBreakdownTarget(item)}
                    className="text-xs font-semibold text-blue-600 dark:text-blue-400 hover:underline min-h-[44px] flex items-center"
                  >
                    🔍 Ver Detalhes do Score
                  </button>

                  <button
                    type="button"
                    onClick={() =>
                      setPriceHistoryTarget({
                        marketplace: item.targetListing.marketplace,
                        externalListingId: item.targetListing.listingId,
                        title: item.targetListing.title,
                      })
                    }
                    className="px-3 py-1.5 text-xs font-medium border border-slate-300 dark:border-slate-700 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 min-h-[44px] flex items-center"
                  >
                    📈 Histórico Auditável
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* MODAL DE DETALHES DO SCORE / EXPLICABILIDADE */}
      {scoreBreakdownTarget && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="score-breakdown-title"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm overflow-y-auto"
        >
          <div className="relative w-full max-w-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-2xl p-6 overflow-hidden max-h-[90vh] flex flex-col">
            <div className="flex items-center justify-between pb-4 border-b border-slate-200 dark:border-slate-800">
              <div className="flex items-center gap-2">
                <span className="text-xl">📊</span>
                <h2
                  id="score-breakdown-title"
                  className="text-base font-bold text-slate-900 dark:text-white"
                >
                  Explicabilidade do Score de Equivalência
                </h2>
              </div>
              <button
                onClick={() => setScoreBreakdownTarget(null)}
                aria-label="Fechar detalhes do score"
                className="p-2 min-h-[44px] min-w-[44px] rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
              >
                ✕
              </button>
            </div>

            <div className="py-4 space-y-4 overflow-y-auto flex-1 text-xs">
              {/* Resumo do Score */}
              <div className="p-4 rounded-xl bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-800 flex items-center justify-between">
                <div>
                  <span className="text-xs text-slate-500">Score Global Ponderado:</span>
                  <p className="text-2xl font-bold text-slate-900 dark:text-white">
                    {formatScore(scoreBreakdownTarget.similarity.overallScore)}
                  </p>
                  <span className="text-[10px] text-slate-400">
                    Limiar Mínimo Exigido: {formatScore(scoreBreakdownTarget.similarity.thresholdApplied)}
                  </span>
                </div>

                <div>
                  {!scoreBreakdownTarget.similarity.isCompatible ? (
                    <span className="px-3 py-1.5 rounded-lg text-xs font-bold bg-red-100 text-red-800 dark:bg-red-900/70 dark:text-red-200 border border-red-300">
                      ⛔ INCOMPATÍVEL
                    </span>
                  ) : scoreBreakdownTarget.similarity.isEquivalent ? (
                    <span className="px-3 py-1.5 rounded-lg text-xs font-bold bg-emerald-100 text-emerald-800 dark:bg-emerald-900/60 dark:text-emerald-200 border border-emerald-300">
                      ✅ EQUIVALENTE
                    </span>
                  ) : (
                    <span className="px-3 py-1.5 rounded-lg text-xs font-bold bg-amber-100 text-amber-800 dark:bg-amber-900/60 dark:text-amber-200 border border-amber-300">
                      ⚠️ NÃO EQUIVALENTE
                    </span>
                  )}
                </div>
              </div>

              {/* Justificativa Textual */}
              <div className="p-3 rounded-lg border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900">
                <span className="font-bold text-slate-900 dark:text-white block mb-1">
                  Justificativa do Algoritmo:
                </span>
                <p className="text-slate-600 dark:text-slate-300">
                  {scoreBreakdownTarget.similarity.explanation}
                </p>
              </div>

              {/* Incompatibilidades */}
              {scoreBreakdownTarget.similarity.incompatibilityReasons.length > 0 && (
                <div className="p-3 rounded-lg bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800 text-red-900 dark:text-red-200">
                  <span className="font-bold block mb-1">Motivos de Incompatibilidade:</span>
                  <ul className="list-disc pl-4 space-y-0.5">
                    {scoreBreakdownTarget.similarity.incompatibilityReasons.map((r, rIdx) => (
                      <li key={rIdx}>{r}</li>
                    ))}
                  </ul>
                </div>
              )}

              {/* Barras de Peso dos Componentes */}
              <div className="space-y-3">
                <span className="font-bold text-slate-900 dark:text-white block">
                  Decomposição Ponderada dos Critérios:
                </span>

                {/* GTIN */}
                <div className="space-y-1">
                  <div className="flex justify-between text-[11px]">
                    <span className="font-medium text-slate-700 dark:text-slate-300">
                      GTIN / Código de Barras (Peso: 35%)
                    </span>
                    <span className="font-bold">
                      {formatScore(scoreBreakdownTarget.similarity.gtinScore)}
                    </span>
                  </div>
                  <div className="h-2 w-full bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-blue-600 rounded-full"
                      style={{
                        width: `${Math.min(
                          Math.max(scoreBreakdownTarget.similarity.gtinScore * 100, 0),
                          100
                        )}%`,
                      }}
                    />
                  </div>
                </div>

                {/* Marca */}
                <div className="space-y-1">
                  <div className="flex justify-between text-[11px]">
                    <span className="font-medium text-slate-700 dark:text-slate-300">
                      Marca do Fabricante (Peso: 20%)
                    </span>
                    <span className="font-bold">
                      {formatScore(scoreBreakdownTarget.similarity.brandScore)}
                    </span>
                  </div>
                  <div className="h-2 w-full bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-indigo-600 rounded-full"
                      style={{
                        width: `${Math.min(
                          Math.max(scoreBreakdownTarget.similarity.brandScore * 100, 0),
                          100
                        )}%`,
                      }}
                    />
                  </div>
                </div>

                {/* Modelo */}
                <div className="space-y-1">
                  <div className="flex justify-between text-[11px]">
                    <span className="font-medium text-slate-700 dark:text-slate-300">
                      Modelo do Produto (Peso: 20%)
                    </span>
                    <span className="font-bold">
                      {formatScore(scoreBreakdownTarget.similarity.modelScore)}
                    </span>
                  </div>
                  <div className="h-2 w-full bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-purple-600 rounded-full"
                      style={{
                        width: `${Math.min(
                          Math.max(scoreBreakdownTarget.similarity.modelScore * 100, 0),
                          100
                        )}%`,
                      }}
                    />
                  </div>
                </div>

                {/* Título */}
                <div className="space-y-1">
                  <div className="flex justify-between text-[11px]">
                    <span className="font-medium text-slate-700 dark:text-slate-300">
                      Similaridade do Título (Peso: 15%)
                    </span>
                    <span className="font-bold">
                      {formatScore(scoreBreakdownTarget.similarity.titleScore)}
                    </span>
                  </div>
                  <div className="h-2 w-full bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-cyan-600 rounded-full"
                      style={{
                        width: `${Math.min(
                          Math.max(scoreBreakdownTarget.similarity.titleScore * 100, 0),
                          100
                        )}%`,
                      }}
                    />
                  </div>
                </div>

                {/* Atributos Técnicos */}
                <div className="space-y-1">
                  <div className="flex justify-between text-[11px]">
                    <span className="font-medium text-slate-700 dark:text-slate-300">
                      Especificações Técnicas (Peso: 10%)
                    </span>
                    <span className="font-bold">
                      {formatScore(scoreBreakdownTarget.similarity.attributesScore)}
                    </span>
                  </div>
                  <div className="h-2 w-full bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-teal-600 rounded-full"
                      style={{
                        width: `${Math.min(
                          Math.max(scoreBreakdownTarget.similarity.attributesScore * 100, 0),
                          100
                        )}%`,
                      }}
                    />
                  </div>
                </div>
              </div>
            </div>

            <div className="pt-4 border-t border-slate-200 dark:border-slate-800 flex justify-end">
              <button
                type="button"
                onClick={() => setScoreBreakdownTarget(null)}
                className="px-4 py-2 text-sm font-medium text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg min-h-[44px]"
              >
                Fechar Detalhes
              </button>
            </div>
          </div>
        </div>
      )}

      {/* DRAWER DE HISTÓRICO DE PREÇOS AUDITÁVEL */}
      {priceHistoryTarget && (
        <ListingPriceHistoryDrawer
          isOpen={Boolean(priceHistoryTarget)}
          onClose={() => setPriceHistoryTarget(null)}
          marketplace={priceHistoryTarget.marketplace}
          externalListingId={priceHistoryTarget.externalListingId}
          listingTitle={priceHistoryTarget.title}
          initialData={
            priceHistoryTarget.externalListingId === 'COMP-MLB-01'
              ? {
                  marketplace: 'MERCADO_LIVRE',
                  externalListingId: 'COMP-MLB-01',
                  title: 'Samsung Galaxy S23 128GB 5G Preto Original Lacrado',
                  dataPointsCount: 3,
                  hasHistoricalTrend: true,
                  minPrice: 3299.0,
                  maxPrice: 3599.0,
                  latestPrice: 3299.0,
                  earliestPrice: 3599.0,
                  overallVariationPercentage: -8.34,
                  timeWindowDays: 14,
                  trendDescription: 'Redução de preço de 8.34% nos últimos 14 dias.',
                  historyNotice:
                    'Histórico sem retroatividade inventada: apenas coletas reais com timestamp auditável são exibidas.',
                  points: [
                    {
                      id: 'pt-1',
                      price: 3599.0,
                      currency: 'BRL',
                      shippingCost: 0.0,
                      observedAt: new Date(Date.now() - 14 * 24 * 3600 * 1000).toISOString(),
                      sourceType: 'OBSERVED',
                      isPromotional: false,
                      note: 'Coleta inicial de mercado',
                    },
                    {
                      id: 'pt-2',
                      price: 3449.0,
                      currency: 'BRL',
                      shippingCost: 0.0,
                      observedAt: new Date(Date.now() - 7 * 24 * 3600 * 1000).toISOString(),
                      sourceType: 'OBSERVED',
                      isPromotional: false,
                      note: 'Reajuste pelo vendedor',
                    },
                    {
                      id: 'pt-3',
                      price: 3299.0,
                      currency: 'BRL',
                      shippingCost: 0.0,
                      observedAt: new Date(Date.now() - 2 * 24 * 3600 * 1000).toISOString(),
                      sourceType: 'OBSERVED',
                      isPromotional: true,
                      note: 'Promoção ativa',
                    },
                  ],
                }
              : null
          }
        />
      )}
    </div>
  );
}
