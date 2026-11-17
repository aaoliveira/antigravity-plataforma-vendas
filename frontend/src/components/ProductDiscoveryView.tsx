'use client';

import React, { useState } from 'react';
import {
  FileText,
  Upload,
  CheckCircle,
  XCircle,
  AlertTriangle,
  Search,
  Image as ImageIcon,
  Trash2,
  ShieldCheck,
  ExternalLink,
  Edit3,
  Percent,
  Barcode,
  Layers,
  ArrowRight
} from 'lucide-react';

export interface CatalogItemModel {
  id: string;
  catalogId: string;
  name: string;
  supplierSku: string | null;
  ean: string | null;
  description: string | null;
  costPrice: number | null;
  currency: string;
  weightKg: number | null;
  dimensionsCm: string | null;
  extractionMethod: 'PDF_TEXT' | 'OCR' | 'TABLE_PARSER';
  overallConfidence: number;
  confidenceDetails?: {
    name?: number;
    ean?: number;
    price?: number;
    eanValid?: boolean;
    ocrRequired?: boolean;
  };
  reviewStatus: 'PENDING_REVIEW' | 'REVIEWED' | 'REJECTED';
  createdAt: string;
}

export interface CatalogModel {
  id: string;
  filename: string;
  supplierName: string;
  status: 'PENDING' | 'PARSED' | 'FAILED';
  totalItems: number;
  reviewedItems: number;
  fileSizeBytes: number;
  failureReason?: string | null;
  createdAt: string;
}

export interface ProductMatchModel {
  id: string;
  catalogItemId: string;
  marketplace: string;
  matchedExternalId: string;
  matchedTitle: string;
  matchedPrice: number;
  similarityScore: number;
  matchSource: 'EAN_EXACT' | 'NAME_SIMILARITY' | 'IMAGE_VECTOR';
  status: 'CANDIDATE' | 'CONFIRMED' | 'REJECTED';
  estimatedGrossMargin: number | null;
  matchUrl: string;
}

export interface VisualCandidateModel {
  externalId: string;
  marketplace: string;
  title: string;
  price: number;
  visualSimilarityScore: number;
  url: string;
}

const initialCatalogs: CatalogModel[] = [
  {
    id: 'cat-01',
    filename: 'catalogo_distribuidora_tech_2026.pdf',
    supplierName: 'Distribuidora Tech Brasil Ltda',
    status: 'PARSED',
    totalItems: 3,
    reviewedItems: 1,
    fileSizeBytes: 2450000,
    createdAt: '2026-11-17T14:30:00Z',
  },
  {
    id: 'cat-02',
    filename: 'tabela_perifericos_importados.pdf',
    supplierName: 'Global Parts Importação',
    status: 'PARSED',
    totalItems: 2,
    reviewedItems: 0,
    fileSizeBytes: 1200000,
    createdAt: '2026-11-17T15:10:00Z',
  },
];

const initialItems: CatalogItemModel[] = [
  {
    id: 'item-01',
    catalogId: 'cat-01',
    name: 'Teclado Mecânico RGB Switch Blue ABNT2',
    supplierSku: 'TEC-RGB-BL-01',
    ean: '7891234567895',
    description: 'Teclado mecânico gamer padrão ABNT2 com iluminação RGB individual e switch azul táctil.',
    costPrice: 89.90,
    currency: 'BRL',
    weightKg: 0.85,
    dimensionsCm: '35x12x4 cm',
    extractionMethod: 'PDF_TEXT',
    overallConfidence: 0.95,
    confidenceDetails: { name: 0.95, ean: 1.0, price: 0.90, eanValid: true },
    reviewStatus: 'REVIEWED',
    createdAt: '2026-11-17T14:30:15Z',
  },
  {
    id: 'item-02',
    catalogId: 'cat-01',
    name: 'Mouse Gamer 16000 DPI Sensor Óptico',
    supplierSku: 'MOU-GAM-16K',
    ean: '7891112223334',
    description: 'Mouse ergonômico 6 botões programáveis com software de macro integrado.',
    costPrice: 45.00,
    currency: 'BRL',
    weightKg: 0.12,
    dimensionsCm: '12x6x4 cm',
    extractionMethod: 'PDF_TEXT',
    overallConfidence: 0.88,
    confidenceDetails: { name: 0.90, ean: 1.0, price: 0.85, eanValid: true },
    reviewStatus: 'PENDING_REVIEW',
    createdAt: '2026-11-17T14:30:20Z',
  },
  {
    id: 'item-03',
    catalogId: 'cat-01',
    name: 'Headset Gamer 7.1 Surround USB',
    supplierSku: 'HDS-71-USB',
    ean: '7898887776665',
    description: 'Headset circumaural com drivers de 50mm e microfone com cancelamento de ruído.',
    costPrice: 65.50,
    currency: 'BRL',
    weightKg: 0.32,
    dimensionsCm: '20x18x9 cm',
    extractionMethod: 'PDF_TEXT',
    overallConfidence: 0.92,
    confidenceDetails: { name: 0.92, ean: 1.0, price: 0.90, eanValid: true },
    reviewStatus: 'PENDING_REVIEW',
    createdAt: '2026-11-17T14:30:25Z',
  },
  {
    id: 'item-04',
    catalogId: 'cat-02',
    name: 'Webcam Full HD 1080p 60fps com Microfone',
    supplierSku: 'CAM-1080P-60',
    ean: '7895554443332',
    description: 'Câmera USB plug and play para streaming e videoconferência com foco automático.',
    costPrice: 78.00,
    currency: 'BRL',
    weightKg: 0.18,
    dimensionsCm: '10x5x5 cm',
    extractionMethod: 'PDF_TEXT',
    overallConfidence: 0.85,
    confidenceDetails: { name: 0.85, ean: 0.90, price: 0.85, eanValid: true },
    reviewStatus: 'PENDING_REVIEW',
    createdAt: '2026-11-17T15:10:05Z',
  },
  {
    id: 'item-05',
    catalogId: 'cat-02',
    name: 'Catálogo Digitalizado de Cabos (Requer OCR)',
    supplierSku: 'SCAN-CABOS-01',
    ean: null,
    description: 'Documento recebido como imagem sem camada textual selecionável.',
    costPrice: null,
    currency: 'BRL',
    weightKg: null,
    dimensionsCm: null,
    extractionMethod: 'OCR',
    overallConfidence: 0.40,
    confidenceDetails: { ocrRequired: true, name: 0.40 },
    reviewStatus: 'PENDING_REVIEW',
    createdAt: '2026-11-17T15:10:10Z',
  },
];

const initialMatches: ProductMatchModel[] = [
  {
    id: 'match-01',
    catalogItemId: 'item-01',
    marketplace: 'MERCADO_LIVRE',
    matchedExternalId: 'MLB10293847',
    matchedTitle: 'Teclado Gamer Mecânico RGB Switch Blue ABNT2 Original',
    matchedPrice: 199.90,
    similarityScore: 0.98,
    matchSource: 'EAN_EXACT',
    status: 'CONFIRMED',
    estimatedGrossMargin: 0.5502,
    matchUrl: 'https://mercadolivre.com.br/item/MLB10293847',
  },
  {
    id: 'match-02',
    catalogItemId: 'item-01',
    marketplace: 'SHOPEE',
    matchedExternalId: 'SP88991234',
    matchedTitle: 'Teclado Mecânico Gamer ABNT2 Led Rgb Pc Computador',
    matchedPrice: 179.00,
    similarityScore: 0.89,
    matchSource: 'NAME_SIMILARITY',
    status: 'CANDIDATE',
    estimatedGrossMargin: 0.4977,
    matchUrl: 'https://shopee.com.br/product/SP88991234',
  },
  {
    id: 'match-03',
    catalogItemId: 'item-02',
    marketplace: 'MERCADO_LIVRE',
    matchedExternalId: 'MLB99887766',
    matchedTitle: 'Mouse Gamer 16000 Dpi Rgb Rápido Envio Full',
    matchedPrice: 119.90,
    similarityScore: 0.98,
    matchSource: 'EAN_EXACT',
    status: 'CANDIDATE',
    estimatedGrossMargin: 0.6246,
    matchUrl: 'https://mercadolivre.com.br/item/MLB99887766',
  },
];

export const ProductDiscoveryView: React.FC = () => {
  const [activeSubTab, setActiveSubTab] = useState<'CATALOGS' | 'ITEMS' | 'SIMILARITY' | 'VISUAL_SEARCH'>('CATALOGS');
  const [catalogs, setCatalogs] = useState<CatalogModel[]>(initialCatalogs);
  const [items, setItems] = useState<CatalogItemModel[]>(initialItems);
  const [matches, setMatches] = useState<ProductMatchModel[]>(initialMatches);
  const [selectedCatalogId, setSelectedCatalogId] = useState<string>('ALL');
  const [reviewFilter, setReviewFilter] = useState<'ALL' | 'PENDING_REVIEW' | 'REVIEWED' | 'REJECTED'>('ALL');

  // Upload Form State
  const [isUploading, setIsUploading] = useState(false);
  const [uploadSupplierName, setUploadSupplierName] = useState('');
  const [uploadFilename, setUploadFilename] = useState('');
  const [uploadFeedback, setUploadFeedback] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  // Review Item Modal / State
  const [editingItem, setEditingItem] = useState<CatalogItemModel | null>(null);
  const [editName, setEditName] = useState('');
  const [editSku, setEditSku] = useState('');
  const [editEan, setEditEan] = useState('');
  const [editCostPrice, setEditCostPrice] = useState<string>('');
  const [editWeight, setEditWeight] = useState<string>('');
  const [editDimensions, setEditDimensions] = useState('');
  const [editDescription, setEditDescription] = useState('');

  // Visual Search State
  const [visualCandidates, setVisualCandidates] = useState<VisualCandidateModel[]>([
    {
      externalId: 'MLB3049281',
      marketplace: 'MERCADO_LIVRE',
      title: 'Mouse Gamer Sem Fio Recarregável RGB Ultra Leve',
      price: 139.90,
      visualSimilarityScore: 0.935,
      url: 'https://mercadolivre.com.br/item/MLB3049281',
    },
    {
      externalId: 'SP9182736',
      marketplace: 'SHOPEE',
      title: 'Mouse Óptico Gamer RGB 6 Botões com Fio USB',
      price: 99.00,
      visualSimilarityScore: 0.862,
      url: 'https://shopee.com.br/product/SP9182736',
    },
  ]);
  const [isSearchingVisual, setIsSearchingVisual] = useState(false);
  const [visualImageName, setVisualImageName] = useState<string>('amostra_mouse_gamer.png');

  // Handlers
  const handleSimulateUpload = (e: React.FormEvent) => {
    e.preventDefault();
    if (!uploadFilename.trim() || !uploadSupplierName.trim()) {
      setUploadFeedback({ message: 'Informe o nome do arquivo PDF e a razão social do fornecedor.', type: 'error' });
      return;
    }

    setIsUploading(true);
    setUploadFeedback(null);

    setTimeout(() => {
      const newCatId = `cat-${Date.now()}`;
      const newCatalog: CatalogModel = {
        id: newCatId,
        filename: uploadFilename.endsWith('.pdf') ? uploadFilename : `${uploadFilename}.pdf`,
        supplierName: uploadSupplierName,
        status: 'PARSED',
        totalItems: 2,
        reviewedItems: 0,
        fileSizeBytes: 1850000,
        createdAt: new Date().toISOString(),
      };

      const newItem1: CatalogItemModel = {
        id: `item-${Date.now()}-1`,
        catalogId: newCatId,
        name: 'Suporte Articulado para Monitor com Pistão a Gás',
        supplierSku: 'SUP-GAS-01',
        ean: '7899998887771',
        description: 'Suporte ergonômico de mesa com ajuste de altura e rotação 360.',
        costPrice: 110.00,
        currency: 'BRL',
        weightKg: 2.4,
        dimensionsCm: '40x25x10 cm',
        extractionMethod: 'PDF_TEXT',
        overallConfidence: 0.94,
        confidenceDetails: { name: 0.95, ean: 1.0, price: 0.92, eanValid: true },
        reviewStatus: 'PENDING_REVIEW',
        createdAt: new Date().toISOString(),
      };

      const newItem2: CatalogItemModel = {
        id: `item-${Date.now()}-2`,
        catalogId: newCatId,
        name: 'Hub USB 3.0 4 Portas de Alta Velocidade',
        supplierSku: 'HUB-4P-USB3',
        ean: '7891234567895',
        description: 'Hub portátil com proteção contra sobretensão e cabo reforçado.',
        costPrice: 28.50,
        currency: 'BRL',
        weightKg: 0.08,
        dimensionsCm: '10x3x1 cm',
        extractionMethod: 'PDF_TEXT',
        overallConfidence: 0.91,
        confidenceDetails: { name: 0.92, ean: 1.0, price: 0.90, eanValid: true },
        reviewStatus: 'PENDING_REVIEW',
        createdAt: new Date().toISOString(),
      };

      setCatalogs(prev => [newCatalog, ...prev]);
      setItems(prev => [newItem1, newItem2, ...prev]);
      setIsUploading(false);
      setUploadFilename('');
      setUploadSupplierName('');
      setUploadFeedback({
        message: `Catálogo "${newCatalog.filename}" extraído com sucesso! 2 itens identificados para conferência humana.`,
        type: 'success',
      });
      setActiveSubTab('ITEMS');
    }, 600);
  };

  const handleDeleteCatalog = (catalogId: string) => {
    if (!window.confirm('Tem certeza que deseja excluir este catálogo e todos os seus itens extraídos?')) {
      return;
    }
    setCatalogs(prev => prev.filter(c => c.id !== catalogId));
    setItems(prev => prev.filter(i => i.catalogId !== catalogId));
  };

  const handleOpenReview = (item: CatalogItemModel) => {
    setEditingItem(item);
    setEditName(item.name);
    setEditSku(item.supplierSku || '');
    setEditEan(item.ean || '');
    setEditCostPrice(item.costPrice !== null ? item.costPrice.toString() : '');
    setEditWeight(item.weightKg !== null ? item.weightKg.toString() : '');
    setEditDimensions(item.dimensionsCm || '');
    setEditDescription(item.description || '');
  };

  const handleSaveReview = () => {
    if (!editingItem) return;

    const parsedPrice = parseFloat(editCostPrice.replace(',', '.'));
    const parsedWeight = parseFloat(editWeight.replace(',', '.'));

    setItems(prev =>
      prev.map(i => {
        if (i.id === editingItem.id) {
          return {
            ...i,
            name: editName,
            supplierSku: editSku || null,
            ean: editEan || null,
            description: editDescription || null,
            costPrice: isNaN(parsedPrice) ? null : parsedPrice,
            weightKg: isNaN(parsedWeight) ? null : parsedWeight,
            dimensionsCm: editDimensions || null,
            reviewStatus: 'REVIEWED',
          };
        }
        return i;
      })
    );

    // Atualiza contagem de revisados no catálogo
    setCatalogs(prev =>
      prev.map(c => {
        if (c.id === editingItem.catalogId) {
          return { ...c, reviewedItems: c.reviewedItems + 1 };
        }
        return c;
      })
    );

    setEditingItem(null);
  };

  const handleRejectItem = (itemId: string) => {
    setItems(prev =>
      prev.map(i => {
        if (i.id === itemId) {
          return { ...i, reviewStatus: 'REJECTED' };
        }
        return i;
      })
    );
  };

  const handleConfirmMatch = (matchId: string) => {
    setMatches(prev =>
      prev.map(m => {
        if (m.id === matchId) {
          return { ...m, status: 'CONFIRMED' };
        }
        return m;
      })
    );
  };

  const handleRejectMatch = (matchId: string) => {
    setMatches(prev =>
      prev.map(m => {
        if (m.id === matchId) {
          return { ...m, status: 'REJECTED' };
        }
        return m;
      })
    );
  };

  const handleSimulateVisualSearch = () => {
    setIsSearchingVisual(true);
    setTimeout(() => {
      setVisualCandidates([
        {
          externalId: 'MLB4091823',
          marketplace: 'MERCADO_LIVRE',
          title: 'Produto Localizado por Reconhecimento Visual HD',
          price: 154.90,
          visualSimilarityScore: 0.952,
          url: 'https://mercadolivre.com.br/item/MLB4091823',
        },
        {
          externalId: 'SP7766554',
          marketplace: 'SHOPEE',
          title: 'Similar Visualmente Correspondente Embalagem Branca',
          price: 129.00,
          visualSimilarityScore: 0.884,
          url: 'https://shopee.com.br/product/SP7766554',
        },
      ]);
      setIsSearchingVisual(false);
    }, 500);
  };

  const filteredItems = items.filter(item => {
    if (selectedCatalogId !== 'ALL' && item.catalogId !== selectedCatalogId) return false;
    if (reviewFilter !== 'ALL' && item.reviewStatus !== reviewFilter) return false;
    return true;
  });

  return (
    <div className="space-y-6">
      {/* Security & Invariants Banner */}
      <div className="bg-amber-950/30 border border-amber-600/40 rounded-xl p-5 text-amber-200">
        <div className="flex items-start gap-3">
          <ShieldCheck className="w-6 h-6 text-amber-400 shrink-0 mt-0.5" />
          <div className="space-y-2">
            <div className="flex items-center gap-2 flex-wrap">
              <h2 className="text-lg font-bold text-amber-300">
                Product Discovery Avançado &amp; Catálogos de Fornecedores
              </h2>
              <span className="text-xs bg-amber-500/20 text-amber-300 border border-amber-500/40 px-2 py-0.5 rounded font-mono">
                FR-DIS-001 a FR-DIS-008
              </span>
              <span className="text-xs bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 px-2 py-0.5 rounded">
                Anti-Prompt Injection Ativo (OWASP LLM01)
              </span>
              <span className="text-xs bg-blue-500/20 text-blue-300 border border-blue-500/40 px-2 py-0.5 rounded">
                Validação Humana Mandatória
              </span>
              <span className="text-xs bg-purple-500/20 text-purple-300 border border-purple-500/40 px-2 py-0.5 rounded">
                Zero Ação Externa Automatizada
              </span>
            </div>
            <p className="text-xs text-amber-200/90 leading-relaxed">
              Catálogos PDF e imagens externas de fornecedores são classificados como fontes de dados não confiáveis.
              O pipeline aplica sanitização contra injeção de instruções maliciosas, valida dígitos verificadores de EAN/GTIN matematicamente,
              exige conferência humana de cada campo antes da persistência no catálogo mestre e calcula margem bruta estimada contra bases licenciadas de mercado.
            </p>
          </div>
        </div>
      </div>

      {/* Sub-tab Navigation */}
      <div className="flex border-b border-slate-700 space-x-4 text-sm font-medium">
        <button
          onClick={() => setActiveSubTab('CATALOGS')}
          className={`pb-3 px-2 border-b-2 transition-colors flex items-center gap-2 ${
            activeSubTab === 'CATALOGS'
              ? 'border-amber-500 text-amber-400 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <FileText className="w-4 h-4" />
          <span>Catálogos PDF ({catalogs.length})</span>
        </button>

        <button
          onClick={() => setActiveSubTab('ITEMS')}
          className={`pb-3 px-2 border-b-2 transition-colors flex items-center gap-2 ${
            activeSubTab === 'ITEMS'
              ? 'border-amber-500 text-amber-400 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <CheckCircle className="w-4 h-4" />
          <span>Itens Extraídos &amp; Revisão Humana ({items.length})</span>
        </button>

        <button
          onClick={() => setActiveSubTab('SIMILARITY')}
          className={`pb-3 px-2 border-b-2 transition-colors flex items-center gap-2 ${
            activeSubTab === 'SIMILARITY'
              ? 'border-amber-500 text-amber-400 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <Percent className="w-4 h-4" />
          <span>Similaridade &amp; Margem Estimada ({matches.length})</span>
        </button>

        <button
          onClick={() => setActiveSubTab('VISUAL_SEARCH')}
          className={`pb-3 px-2 border-b-2 transition-colors flex items-center gap-2 ${
            activeSubTab === 'VISUAL_SEARCH'
              ? 'border-amber-500 text-amber-400 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <ImageIcon className="w-4 h-4" />
          <span>Busca Visual por Imagem</span>
        </button>
      </div>

      {/* SUB-TAB 1: CATALOGS */}
      {activeSubTab === 'CATALOGS' && (
        <div className="space-y-6">
          {/* Upload Form Card */}
          <div className="bg-slate-800/80 border border-slate-700 rounded-xl p-5 space-y-4">
            <h3 className="text-base font-semibold text-slate-200 flex items-center gap-2">
              <Upload className="w-4 h-4 text-amber-400" />
              Upload Seguro de Catálogo de Fornecedor (PDF)
            </h3>
            <p className="text-xs text-slate-400">
              Arquivos são inspecionados para validação de magic bytes (%PDF-), limitação estrita a 25MB, bloqueio de executáveis e neutralização de injeções de prompt em texto.
            </p>

            <form onSubmit={handleSimulateUpload} className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">
                  Fornecedor / Distribuidora
                </label>
                <input
                  type="text"
                  placeholder="Ex: Fornecedor Tech Distribuição"
                  value={uploadSupplierName}
                  onChange={e => setUploadSupplierName(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-amber-500"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">
                  Nome do Arquivo PDF
                </label>
                <input
                  type="text"
                  placeholder="Ex: catalogo_novos_produtos.pdf"
                  value={uploadFilename}
                  onChange={e => setUploadFilename(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-amber-500"
                />
              </div>

              <div className="flex items-end">
                <button
                  type="submit"
                  disabled={isUploading}
                  className="w-full bg-amber-600 hover:bg-amber-500 text-white font-medium px-4 py-2 rounded-lg text-sm transition-colors flex items-center justify-center gap-2 disabled:opacity-50"
                >
                  <Upload className="w-4 h-4" />
                  <span>{isUploading ? 'Processando & Sanitizando...' : 'Enviar e Extrair Catálogo'}</span>
                </button>
              </div>
            </form>

            {uploadFeedback && (
              <div
                className={`p-3 rounded-lg text-xs font-medium ${
                  uploadFeedback.type === 'success'
                    ? 'bg-emerald-950/40 text-emerald-300 border border-emerald-600/40'
                    : 'bg-rose-950/40 text-rose-300 border border-rose-600/40'
                }`}
              >
                {uploadFeedback.message}
              </div>
            )}
          </div>

          {/* Catalogs Table */}
          <div className="bg-slate-800/80 border border-slate-700 rounded-xl overflow-hidden">
            <div className="p-4 border-b border-slate-700 flex justify-between items-center">
              <h4 className="text-sm font-semibold text-slate-200">Catálogos Processados no Tenant</h4>
              <span className="text-xs text-slate-400 font-mono">Total: {catalogs.length} catálogos</span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-300">
                <thead className="bg-slate-900/60 text-slate-400 border-b border-slate-700 uppercase tracking-wider">
                  <tr>
                    <th className="p-3">Arquivo &amp; Fornecedor</th>
                    <th className="p-3">Tamanho</th>
                    <th className="p-3">Status</th>
                    <th className="p-3">Itens Extraídos</th>
                    <th className="p-3">Conferidos</th>
                    <th className="p-3">Data</th>
                    <th className="p-3 text-right">Ações</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-700/60">
                  {catalogs.map(cat => (
                    <tr key={cat.id} className="hover:bg-slate-700/30 transition-colors">
                      <td className="p-3">
                        <div className="font-semibold text-slate-100 flex items-center gap-1.5">
                          <FileText className="w-3.5 h-3.5 text-amber-400" />
                          <span>{cat.filename}</span>
                        </div>
                        <div className="text-[11px] text-slate-400">{cat.supplierName}</div>
                      </td>
                      <td className="p-3 font-mono">
                        {(cat.fileSizeBytes / (1024 * 1024)).toFixed(2)} MB
                      </td>
                      <td className="p-3">
                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                            cat.status === 'PARSED'
                              ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                              : cat.status === 'FAILED'
                              ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30'
                              : 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                          }`}
                        >
                          {cat.status}
                        </span>
                      </td>
                      <td className="p-3 font-mono font-semibold text-slate-200">
                        {cat.totalItems}
                      </td>
                      <td className="p-3">
                        <span className="font-mono text-emerald-400 font-semibold">{cat.reviewedItems}</span>
                        <span className="text-slate-400"> / {cat.totalItems}</span>
                      </td>
                      <td className="p-3 text-slate-400">
                        {new Date(cat.createdAt).toLocaleDateString('pt-BR')}
                      </td>
                      <td className="p-3 text-right">
                        <button
                          onClick={() => handleDeleteCatalog(cat.id)}
                          className="text-rose-400 hover:text-rose-300 p-1 rounded hover:bg-rose-500/10 transition-colors"
                          title="Excluir catálogo com segurança"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* SUB-TAB 2: ITEMS & HUMAN REVIEW */}
      {activeSubTab === 'ITEMS' && (
        <div className="space-y-4">
          {/* Filter Bar */}
          <div className="flex flex-wrap items-center justify-between gap-3 bg-slate-800/80 p-3 rounded-xl border border-slate-700 text-xs">
            <div className="flex items-center gap-2">
              <span className="text-slate-400">Filtrar por Catálogo:</span>
              <select
                value={selectedCatalogId}
                onChange={e => setSelectedCatalogId(e.target.value)}
                className="bg-slate-900 border border-slate-700 text-slate-200 rounded px-2.5 py-1"
              >
                <option value="ALL">Todos os Catálogos ({items.length})</option>
                {catalogs.map(c => (
                  <option key={c.id} value={c.id}>
                    {c.supplierName} ({c.filename})
                  </option>
                ))}
              </select>
            </div>

            <div className="flex items-center gap-2">
              <span className="text-slate-400">Status de Revisão:</span>
              <div className="inline-flex rounded-md shadow-sm">
                {(['ALL', 'PENDING_REVIEW', 'REVIEWED', 'REJECTED'] as const).map(status => (
                  <button
                    key={status}
                    onClick={() => setReviewFilter(status)}
                    className={`px-3 py-1 text-[11px] font-medium border border-slate-700 first:rounded-l-md last:rounded-r-md ${
                      reviewFilter === status
                        ? 'bg-amber-600 text-white font-semibold'
                        : 'bg-slate-900 text-slate-400 hover:text-slate-200'
                    }`}
                  >
                    {status === 'ALL'
                      ? 'Todos'
                      : status === 'PENDING_REVIEW'
                      ? 'Pendentes'
                      : status === 'REVIEWED'
                      ? 'Confirmados'
                      : 'Rejeitados'}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Items Table */}
          <div className="bg-slate-800/80 border border-slate-700 rounded-xl overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-300">
                <thead className="bg-slate-900/60 text-slate-400 border-b border-slate-700 uppercase tracking-wider">
                  <tr>
                    <th className="p-3">Produto Extraído</th>
                    <th className="p-3">SKU / EAN</th>
                    <th className="p-3">Custo Extraído</th>
                    <th className="p-3">Método / Confiança</th>
                    <th className="p-3">Status Revisão</th>
                    <th className="p-3 text-right">Ação Humana Mandatória</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-700/60">
                  {filteredItems.map(item => (
                    <tr key={item.id} className="hover:bg-slate-700/30 transition-colors">
                      <td className="p-3 max-w-xs">
                        <div className="font-semibold text-slate-100">{item.name}</div>
                        {item.description && (
                          <div className="text-[11px] text-slate-400 truncate">{item.description}</div>
                        )}
                        {(item.weightKg || item.dimensionsCm) && (
                          <div className="text-[10px] text-slate-400 mt-0.5">
                            {item.weightKg && <span>{item.weightKg} kg</span>}
                            {item.weightKg && item.dimensionsCm && <span> • </span>}
                            {item.dimensionsCm && <span>{item.dimensionsCm}</span>}
                          </div>
                        )}
                      </td>
                      <td className="p-3">
                        <div className="font-mono text-slate-200">
                          {item.supplierSku || <span className="text-slate-500 italic">Sem SKU</span>}
                        </div>
                        <div className="flex items-center gap-1 mt-0.5">
                          <Barcode className="w-3 h-3 text-slate-400" />
                          {item.ean ? (
                            <span className="font-mono text-emerald-400 font-medium">{item.ean}</span>
                          ) : (
                            <span className="text-rose-400 italic">EAN Ausente</span>
                          )}
                        </div>
                      </td>
                      <td className="p-3">
                        {item.costPrice !== null ? (
                          <span className="font-mono font-semibold text-slate-100">
                            R$ {item.costPrice.toFixed(2)}
                          </span>
                        ) : (
                          <span className="text-slate-500 italic">Indisponível</span>
                        )}
                      </td>
                      <td className="p-3">
                        <div className="flex items-center gap-1.5">
                          <span
                            className={`px-1.5 py-0.2 rounded text-[10px] font-mono ${
                              item.extractionMethod === 'OCR'
                                ? 'bg-purple-500/20 text-purple-300 border border-purple-500/30'
                                : 'bg-blue-500/20 text-blue-300 border border-blue-500/30'
                            }`}
                          >
                            {item.extractionMethod}
                          </span>
                          <span className="font-mono font-semibold text-slate-200">
                            {(item.overallConfidence * 100).toFixed(0)}%
                          </span>
                        </div>
                        {item.confidenceDetails?.ocrRequired && (
                          <span className="text-[10px] text-amber-400 block mt-0.5">
                            Requer OCR Óptico
                          </span>
                        )}
                      </td>
                      <td className="p-3">
                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                            item.reviewStatus === 'REVIEWED'
                              ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                              : item.reviewStatus === 'REJECTED'
                              ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30'
                              : 'bg-amber-500/20 text-amber-300 border border-amber-500/30 animate-pulse'
                          }`}
                        >
                          {item.reviewStatus === 'PENDING_REVIEW'
                            ? 'PENDENTE DE REVISÃO'
                            : item.reviewStatus === 'REVIEWED'
                            ? 'CONFERIDO & CONFIRMADO'
                            : 'REJEITADO'}
                        </span>
                      </td>
                      <td className="p-3 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => handleOpenReview(item)}
                            className="bg-amber-600/30 hover:bg-amber-600/50 text-amber-300 border border-amber-500/40 px-2.5 py-1 rounded text-xs flex items-center gap-1 transition-colors"
                          >
                            <Edit3 className="w-3.5 h-3.5" />
                            <span>Conferir</span>
                          </button>
                          {item.reviewStatus !== 'REJECTED' && (
                            <button
                              onClick={() => handleRejectItem(item.id)}
                              className="text-rose-400 hover:text-rose-300 p-1 rounded hover:bg-rose-500/10 transition-colors"
                              title="Rejeitar item"
                            >
                              <XCircle className="w-4 h-4" />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Edit / Review Modal */}
          {editingItem && (
            <div className="fixed inset-0 bg-black/70 flex items-center justify-center p-4 z-50">
              <div className="bg-slate-900 border border-slate-700 rounded-xl max-w-lg w-full p-6 space-y-4">
                <div className="flex justify-between items-center border-b border-slate-800 pb-3">
                  <h3 className="text-base font-semibold text-slate-100 flex items-center gap-2">
                    <CheckCircle className="w-5 h-5 text-amber-400" />
                    Conferência Humana Mandatória de Extração
                  </h3>
                  <button
                    onClick={() => setEditingItem(null)}
                    className="text-slate-400 hover:text-slate-200"
                  >
                    ✕
                  </button>
                </div>

                <div className="space-y-3 text-xs">
                  <div>
                    <label className="block text-slate-300 font-medium mb-1">Nome do Produto</label>
                    <input
                      type="text"
                      value={editName}
                      onChange={e => setEditName(e.target.value)}
                      className="w-full bg-slate-800 border border-slate-700 rounded px-3 py-2 text-slate-200 focus:outline-none focus:border-amber-500"
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-slate-300 font-medium mb-1">SKU do Fornecedor</label>
                      <input
                        type="text"
                        value={editSku}
                        onChange={e => setEditSku(e.target.value)}
                        className="w-full bg-slate-800 border border-slate-700 rounded px-3 py-2 text-slate-200 focus:outline-none focus:border-amber-500"
                      />
                    </div>
                    <div>
                      <label className="block text-slate-300 font-medium mb-1">Código EAN / GTIN</label>
                      <input
                        type="text"
                        value={editEan}
                        onChange={e => setEditEan(e.target.value)}
                        className="w-full bg-slate-800 border border-slate-700 rounded px-3 py-2 text-slate-200 font-mono focus:outline-none focus:border-amber-500"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-3 gap-3">
                    <div>
                      <label className="block text-slate-300 font-medium mb-1">Preço de Custo (R$)</label>
                      <input
                        type="text"
                        value={editCostPrice}
                        onChange={e => setEditCostPrice(e.target.value)}
                        className="w-full bg-slate-800 border border-slate-700 rounded px-3 py-2 text-slate-200 font-mono focus:outline-none focus:border-amber-500"
                      />
                    </div>
                    <div>
                      <label className="block text-slate-300 font-medium mb-1">Peso (kg)</label>
                      <input
                        type="text"
                        value={editWeight}
                        onChange={e => setEditWeight(e.target.value)}
                        className="w-full bg-slate-800 border border-slate-700 rounded px-3 py-2 text-slate-200 font-mono focus:outline-none focus:border-amber-500"
                      />
                    </div>
                    <div>
                      <label className="block text-slate-300 font-medium mb-1">Dimensões (LxAxP)</label>
                      <input
                        type="text"
                        value={editDimensions}
                        onChange={e => setEditDimensions(e.target.value)}
                        placeholder="Ex: 30x20x10 cm"
                        className="w-full bg-slate-800 border border-slate-700 rounded px-3 py-2 text-slate-200 focus:outline-none focus:border-amber-500"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-slate-300 font-medium mb-1">Descrição</label>
                    <textarea
                      rows={3}
                      value={editDescription}
                      onChange={e => setEditDescription(e.target.value)}
                      className="w-full bg-slate-800 border border-slate-700 rounded px-3 py-2 text-slate-200 focus:outline-none focus:border-amber-500"
                    />
                  </div>
                </div>

                <div className="flex justify-end gap-3 pt-3 border-t border-slate-800">
                  <button
                    onClick={() => setEditingItem(null)}
                    className="px-4 py-2 rounded text-xs font-medium text-slate-400 hover:text-slate-200"
                  >
                    Cancelar
                  </button>
                  <button
                    onClick={handleSaveReview}
                    className="bg-emerald-600 hover:bg-emerald-500 text-white px-4 py-2 rounded text-xs font-medium transition-colors flex items-center gap-1.5"
                  >
                    <CheckCircle className="w-4 h-4" />
                    <span>Confirmar Revisão Humana</span>
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* SUB-TAB 3: SIMILARITY & ESTIMATED MARGIN */}
      {activeSubTab === 'SIMILARITY' && (
        <div className="space-y-4">
          <div className="bg-slate-800/80 border border-slate-700 rounded-xl p-4">
            <h3 className="text-sm font-semibold text-slate-200 flex items-center gap-2 mb-1">
              <Percent className="w-4 h-4 text-amber-400" />
              Correspondência com Produtos de Mercado &amp; Margem Bruta Estimada
            </h3>
            <p className="text-xs text-slate-400">
              Produtos do catálogo de fornecedores são comparados com anúncios ativos dos canais autorizados.
              A margem bruta estimada é calculada determinísticamente: <code className="text-amber-300 font-mono">(Preço Mercado - Custo Fornecedor) / Preço Mercado</code>.
              Nenhum produto é associado automaticamente sem confirmação expressa do seller.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {matches.map(match => {
              const matchedItem = items.find(i => i.id === match.catalogItemId);
              return (
                <div
                  key={match.id}
                  className={`border rounded-xl p-4 space-y-3 transition-colors ${
                    match.status === 'CONFIRMED'
                      ? 'bg-slate-800/90 border-emerald-500/40'
                      : match.status === 'REJECTED'
                      ? 'bg-slate-800/40 border-rose-500/30 opacity-70'
                      : 'bg-slate-800/80 border-slate-700'
                  }`}
                >
                  <div className="flex justify-between items-start gap-2">
                    <div>
                      <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-blue-500/20 text-blue-300 border border-blue-500/30">
                        {match.marketplace}
                      </span>
                      <span className="text-[10px] font-mono ml-2 text-slate-400">
                        ID: {match.matchedExternalId}
                      </span>
                    </div>
                    <span
                      className={`text-[10px] font-semibold px-2 py-0.5 rounded ${
                        match.status === 'CONFIRMED'
                          ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40'
                          : match.status === 'REJECTED'
                          ? 'bg-rose-500/20 text-rose-300 border border-rose-500/40'
                          : 'bg-amber-500/20 text-amber-300 border border-amber-500/40'
                      }`}
                    >
                      {match.status}
                    </span>
                  </div>

                  <div>
                    <h4 className="text-sm font-semibold text-slate-100 flex items-center gap-1.5">
                      <span>{match.matchedTitle}</span>
                      <a
                        href={match.matchUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="text-blue-400 hover:text-blue-300"
                        title="Ver no marketplace"
                      >
                        <ExternalLink className="w-3.5 h-3.5" />
                      </a>
                    </h4>
                    {matchedItem && (
                      <div className="text-xs text-slate-400 mt-1 flex items-center gap-1">
                        <span>Item Origem:</span>
                        <strong className="text-slate-200">{matchedItem.name}</strong>
                        <span className="text-slate-400">
                          (Custo R$ {matchedItem.costPrice?.toFixed(2) || '0.00'})
                        </span>
                      </div>
                    )}
                  </div>

                  <div className="grid grid-cols-3 gap-2 bg-slate-900/60 p-2.5 rounded-lg border border-slate-700/60 text-xs">
                    <div>
                      <div className="text-slate-400 text-[10px]">Preço Mercado</div>
                      <div className="font-mono font-bold text-slate-100">
                        R$ {match.matchedPrice.toFixed(2)}
                      </div>
                    </div>

                    <div>
                      <div className="text-slate-400 text-[10px]">Similaridade</div>
                      <div className="font-mono font-bold text-amber-400">
                        {(match.similarityScore * 100).toFixed(0)}%
                        <span className="text-[10px] text-slate-400 font-normal ml-1">
                          ({match.matchSource})
                        </span>
                      </div>
                    </div>

                    <div>
                      <div className="text-slate-400 text-[10px]">Margem Bruta Estimada</div>
                      <div className="font-mono font-bold text-emerald-400">
                        {match.estimatedGrossMargin !== null
                          ? `${(match.estimatedGrossMargin * 100).toFixed(1)}%`
                          : 'N/A'}
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center justify-end gap-2 pt-1 border-t border-slate-700/60">
                    {match.status === 'CANDIDATE' ? (
                      <>
                        <button
                          onClick={() => handleRejectMatch(match.id)}
                          className="bg-rose-500/20 hover:bg-rose-500/30 text-rose-300 border border-rose-500/30 px-3 py-1 rounded text-xs font-medium transition-colors"
                        >
                          Rejeitar
                        </button>
                        <button
                          onClick={() => handleConfirmMatch(match.id)}
                          className="bg-emerald-600 hover:bg-emerald-500 text-white px-3 py-1 rounded text-xs font-medium transition-colors flex items-center gap-1"
                        >
                          <CheckCircle className="w-3.5 h-3.5" />
                          <span>Confirmar Correspondência</span>
                        </button>
                      </>
                    ) : match.status === 'CONFIRMED' ? (
                      <span className="text-xs text-emerald-400 flex items-center gap-1">
                        <CheckCircle className="w-3.5 h-3.5" />
                        Correspondência Confirmada pelo Usuário
                      </span>
                    ) : (
                      <span className="text-xs text-rose-400 flex items-center gap-1">
                        <XCircle className="w-3.5 h-3.5" />
                        Correspondência Rejeitada
                      </span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* SUB-TAB 4: VISUAL SEARCH */}
      {activeSubTab === 'VISUAL_SEARCH' && (
        <div className="space-y-6">
          <div className="bg-slate-800/80 border border-slate-700 rounded-xl p-5 space-y-4">
            <h3 className="text-base font-semibold text-slate-200 flex items-center gap-2">
              <ImageIcon className="w-4 h-4 text-amber-400" />
              Busca Visual por Imagem de Produto
            </h3>
            <p className="text-xs text-slate-400">
              Faça upload de fotos de produtos ou embalagens (PNG ou JPEG com magic bytes validados) para identificar anúncios equivalentes em bases de produtos licenciadas.
            </p>

            <div className="flex flex-wrap items-center gap-4">
              <div className="flex items-center gap-2">
                <input
                  type="text"
                  value={visualImageName}
                  onChange={e => setVisualImageName(e.target.value)}
                  placeholder="Nome do arquivo de imagem..."
                  className="bg-slate-900 border border-slate-700 text-slate-200 rounded-lg px-3 py-2 text-xs w-64 focus:outline-none focus:border-amber-500"
                />
              </div>

              <button
                onClick={handleSimulateVisualSearch}
                disabled={isSearchingVisual}
                className="bg-amber-600 hover:bg-amber-500 text-white font-medium px-4 py-2 rounded-lg text-xs transition-colors flex items-center gap-2 disabled:opacity-50"
              >
                <Search className="w-4 h-4" />
                <span>{isSearchingVisual ? 'Analisando Imagem...' : 'Executar Busca por Imagem'}</span>
              </button>
            </div>
          </div>

          {/* Visual Candidates */}
          <div className="bg-slate-800/80 border border-slate-700 rounded-xl p-4 space-y-4">
            <h4 className="text-sm font-semibold text-slate-200 flex items-center gap-2">
              <Layers className="w-4 h-4 text-blue-400" />
              Candidatos Encontrados por Similaridade Visual
            </h4>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {visualCandidates.map((candidate, idx) => (
                <div
                  key={idx}
                  className="bg-slate-900/60 border border-slate-700 rounded-lg p-3.5 space-y-2"
                >
                  <div className="flex justify-between items-start">
                    <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-blue-500/20 text-blue-300 border border-blue-500/30">
                      {candidate.marketplace}
                    </span>
                    <span className="text-xs font-mono font-bold text-amber-400">
                      Similaridade: {(candidate.visualSimilarityScore * 100).toFixed(1)}%
                    </span>
                  </div>

                  <h5 className="text-xs font-semibold text-slate-100 flex items-center gap-1.5">
                    <span>{candidate.title}</span>
                    <a
                      href={candidate.url}
                      target="_blank"
                      rel="noreferrer"
                      className="text-blue-400 hover:text-blue-300"
                    >
                      <ExternalLink className="w-3 h-3" />
                    </a>
                  </h5>

                  <div className="flex justify-between items-center text-xs pt-1 border-t border-slate-800">
                    <span className="text-slate-400">Preço Atual:</span>
                    <span className="font-mono font-bold text-slate-100">
                      R$ {candidate.price.toFixed(2)}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
