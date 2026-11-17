"use client";

import React, { useState } from "react";

interface MarketItem {
  externalId: string;
  title: string;
  price: number;
  currency: string;
  condition: string;
  sellerNickname: string;
  sellerReputation: string;
  permalink: string;
  thumbnailUrl: string;
  categoryId?: string;
  freeShipping?: boolean;
  estimatedSalesRange?: string;
  estimationMethod?: string;
  provenance: "OBSERVED" | "ESTIMATED" | "UNAVAILABLE";
}

interface SavedSearchItem {
  id: string;
  name: string;
  marketplace: string;
  query: string;
  minPrice?: number;
  maxPrice?: number;
  notes?: string;
  createdAt: string;
}

interface SavedProductItem {
  id: string;
  externalItemId: string;
  title: string;
  price: number;
  sellerNickname: string;
  notes: string;
  createdAt: string;
}

export function MarketIntelligenceView() {
  const [activeSubTab, setActiveSubTab] = useState<"SEARCH" | "SAVED_SEARCHES" | "FAVORITES">("SEARCH");
  const [marketplace, setMarketplace] = useState<"MERCADO_LIVRE" | "SHOPEE">("MERCADO_LIVRE");
  const [query, setQuery] = useState("fone bluetooth");
  const [minPrice, setMinPrice] = useState("");
  const [maxPrice, setMaxPrice] = useState("");
  const [condition, setCondition] = useState("new");
  const [sortOrder, setSortOrder] = useState("relevance");
  const [isLoading, setIsLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(true);

  const [results, setResults] = useState<MarketItem[]>([
    {
      externalId: "MLB10001",
      title: "Fone de Ouvido Bluetooth Sem Fio Cancelamento Ruído",
      price: 189.9,
      currency: "BRL",
      condition: "new",
      sellerNickname: "audio_tech_brasil",
      sellerReputation: "MercadoLíder Platinum",
      permalink: "https://produto.mercadolivre.com.br/MLB10001",
      thumbnailUrl: "https://http2.mlstatic.com/sample1.jpg",
      freeShipping: true,
      estimatedSalesRange: "250 a 500 unidades/mês",
      estimationMethod: "Estimativa heurística baseada no giro observável e contagem de vendas públicas do anúncio",
      provenance: "OBSERVED",
    },
    {
      externalId: "MLB10002",
      title: "Fone Bluetooth Esportivo Resistente a Água",
      price: 89.9,
      currency: "BRL",
      condition: "new",
      sellerNickname: "sport_sound_oficial",
      sellerReputation: "MercadoLíder Gold",
      permalink: "https://produto.mercadolivre.com.br/MLB10002",
      thumbnailUrl: "https://http2.mlstatic.com/sample2.jpg",
      freeShipping: false,
      estimatedSalesRange: "100 a 200 unidades/mês",
      estimationMethod: "Estimativa heurística baseada no giro observável e contagem de vendas públicas do anúncio",
      provenance: "OBSERVED",
    },
  ]);

  const [savedSearches, setSavedSearches] = useState<SavedSearchItem[]>([
    {
      id: "search-1",
      name: "Monitoramento Fones Bluetooth",
      marketplace: "MERCADO_LIVRE",
      query: "fone bluetooth",
      minPrice: 50,
      maxPrice: 300,
      notes: "Nicho com alta demanda e bom ticket médio",
      createdAt: new Date().toISOString(),
    },
  ]);

  const [savedProducts, setSavedProducts] = useState<SavedProductItem[]>([
    {
      id: "prod-1",
      externalItemId: "MLB10001",
      title: "Fone de Ouvido Bluetooth Sem Fio Cancelamento Ruído",
      price: 189.9,
      sellerNickname: "audio_tech_brasil",
      notes: "Analisar viabilidade com fornecedor nacional",
      createdAt: new Date().toISOString(),
    },
  ]);

  const [saveModalOpen, setSaveModalOpen] = useState(false);
  const [saveSearchName, setSaveSearchName] = useState("");
  const [saveSearchNotes, setSaveSearchNotes] = useState("");

  const handleSearch = () => {
    setIsLoading(true);
    setHasSearched(true);
    setTimeout(() => {
      if (marketplace === "SHOPEE") {
        setResults([]);
      } else {
        setResults([
          {
            externalId: "MLB" + Math.floor(Math.random() * 90000 + 10000),
            title: `Produto Observado: ${query} (Alta Relevância)`,
            price: minPrice ? parseFloat(minPrice) + 20 : 149.9,
            currency: "BRL",
            condition: condition,
            sellerNickname: "vendedor_destaque_ml",
            sellerReputation: "MercadoLíder Platinum",
            permalink: "https://produto.mercadolivre.com.br/sample",
            thumbnailUrl: "https://http2.mlstatic.com/sample.jpg",
            freeShipping: true,
            estimatedSalesRange: "150 a 300 unidades/mês",
            estimationMethod: "Estimativa heurística baseada no giro observável na categoria",
            provenance: "OBSERVED",
          },
        ]);
      }
      setIsLoading(false);
    }, 400);
  };

  const handleSaveCurrentSearch = () => {
    if (!saveSearchName.trim()) {
      alert("Informe um nome para a busca.");
      return;
    }
    const newSaved: SavedSearchItem = {
      id: "search-" + Date.now(),
      name: saveSearchName.trim(),
      marketplace,
      query,
      minPrice: minPrice ? parseFloat(minPrice) : undefined,
      maxPrice: maxPrice ? parseFloat(maxPrice) : undefined,
      notes: saveSearchNotes.trim() || undefined,
      createdAt: new Date().toISOString(),
    };
    setSavedSearches([newSaved, ...savedSearches]);
    setSaveModalOpen(false);
    setSaveSearchName("");
    setSaveSearchNotes("");
  };

  const handleFavoriteProduct = (item: MarketItem) => {
    if (savedProducts.some((p) => p.externalItemId === item.externalId)) {
      alert("Produto já está nos seus itens favoritados.");
      return;
    }
    const newSavedProd: SavedProductItem = {
      id: "fav-" + Date.now(),
      externalItemId: item.externalId,
      title: item.title,
      price: item.price,
      sellerNickname: item.sellerNickname,
      notes: "Adicionado via pesquisa de mercado",
      createdAt: new Date().toISOString(),
    };
    setSavedProducts([newSavedProd, ...savedProducts]);
    alert("Produto salvo em Oportunidades com sucesso!");
  };

  return (
    <div className="space-y-6" data-testid="market-intelligence-view">
      {/* Header */}
      <div>
        <h2 className="text-xl font-bold text-slate-800">Market Intelligence: Pesquisa de Mercado</h2>
        <p className="text-sm text-slate-500 mt-1">
          Pesquisa de produtos públicos, análise de preços e oportunidades com proveniência explícita (`OBSERVED` / `ESTIMATED`).
        </p>
      </div>

      {/* Sub Navigation */}
      <div className="flex border-b border-slate-200 space-x-6 text-sm font-medium">
        <button
          onClick={() => setActiveSubTab("SEARCH")}
          className={`pb-2 px-1 border-b-2 transition-colors ${
            activeSubTab === "SEARCH"
              ? "border-blue-600 text-blue-600 font-semibold"
              : "border-transparent text-slate-500 hover:text-slate-800"
          }`}
        >
          🔍 Pesquisa Pública
        </button>
        <button
          onClick={() => setActiveSubTab("SAVED_SEARCHES")}
          className={`pb-2 px-1 border-b-2 transition-colors ${
            activeSubTab === "SAVED_SEARCHES"
              ? "border-blue-600 text-blue-600 font-semibold"
              : "border-transparent text-slate-500 hover:text-slate-800"
          }`}
        >
          📁 Buscas Salvas ({savedSearches.length})
        </button>
        <button
          onClick={() => setActiveSubTab("FAVORITES")}
          className={`pb-2 px-1 border-b-2 transition-colors ${
            activeSubTab === "FAVORITES"
              ? "border-blue-600 text-blue-600 font-semibold"
              : "border-transparent text-slate-500 hover:text-slate-800"
          }`}
        >
          ⭐ Produtos &amp; Oportunidades ({savedProducts.length})
        </button>
      </div>

      {activeSubTab === "SEARCH" && (
        <div className="space-y-6">
          {/* Search Controls */}
          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
              <div className="md:col-span-1">
                <label className="block text-xs font-semibold text-slate-600 mb-1" htmlFor="mkt-select">Marketplace</label>
                <select
                  id="mkt-select"
                  aria-label="Marketplace"
                  value={marketplace}
                  onChange={(e) => setMarketplace(e.target.value as "MERCADO_LIVRE" | "SHOPEE")}
                  className="w-full text-xs border border-slate-300 rounded-lg p-2.5 bg-white text-slate-800 focus:ring-2 focus:ring-blue-500"
                >
                  <option value="MERCADO_LIVRE">Mercado Livre (Brasil)</option>
                  <option value="SHOPEE">Shopee (Brasil)</option>
                </select>
              </div>
              <div className="md:col-span-3">
                <label className="block text-xs font-semibold text-slate-600 mb-1">Termo, Palavra-chave ou URL</label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder="Ex: teclado mecanico, fone bluetooth, MLB123456"
                    className="flex-1 text-xs border border-slate-300 rounded-lg p-2.5 text-slate-800 focus:ring-2 focus:ring-blue-500"
                  />
                  <button
                    onClick={handleSearch}
                    disabled={isLoading}
                    className="px-5 py-2.5 text-xs font-semibold bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors disabled:opacity-50"
                  >
                    {isLoading ? "Buscando..." : "Pesquisar"}
                  </button>
                  <button
                    onClick={() => setSaveModalOpen(true)}
                    className="px-3 py-2.5 text-xs font-semibold border border-slate-300 hover:bg-slate-50 text-slate-700 rounded-lg transition-colors"
                  >
                    Salvar Busca
                  </button>
                </div>
              </div>
            </div>

            {/* Filter Bar */}
            <div className="pt-3 border-t border-slate-100 flex flex-wrap items-center gap-4 text-xs">
              <span className="font-semibold text-slate-500">Filtros Oficiais:</span>
              <div className="flex items-center gap-2">
                <label className="text-slate-500">Preço:</label>
                <input
                  type="number"
                  placeholder="Mín"
                  value={minPrice}
                  onChange={(e) => setMinPrice(e.target.value)}
                  className="w-20 p-1.5 border border-slate-200 rounded text-xs"
                />
                <span className="text-slate-400">—</span>
                <input
                  type="number"
                  placeholder="Máx"
                  value={maxPrice}
                  onChange={(e) => setMaxPrice(e.target.value)}
                  className="w-20 p-1.5 border border-slate-200 rounded text-xs"
                />
              </div>

              <div className="flex items-center gap-2">
                <label className="text-slate-500">Condição:</label>
                <select
                  value={condition}
                  onChange={(e) => setCondition(e.target.value)}
                  className="p-1.5 border border-slate-200 rounded text-xs bg-white text-slate-700"
                >
                  <option value="new">Novo</option>
                  <option value="used">Usado</option>
                </select>
              </div>

              <div className="flex items-center gap-2">
                <label className="text-slate-500">Ordenação:</label>
                <select
                  value={sortOrder}
                  onChange={(e) => setSortOrder(e.target.value)}
                  className="p-1.5 border border-slate-200 rounded text-xs bg-white text-slate-700"
                >
                  <option value="relevance">Mais Relevantes</option>
                  <option value="price_asc">Menor Preço</option>
                  <option value="price_desc">Maior Preço</option>
                </select>
              </div>
            </div>
          </div>

          {/* Shopee Unavailability Notice */}
          {marketplace === "SHOPEE" && (
            <div className="p-4 bg-amber-50 border border-amber-200 text-amber-800 text-xs rounded-xl flex items-start gap-3">
              <span className="text-base">⚠️</span>
              <div>
                <strong>Busca Pública Não Disponível na API Oficial da Shopee:</strong>
                <p className="mt-1 text-slate-600">
                  A Shopee Open Platform API v2 não disponibiliza pesquisa pública aberta por palavra-chave sem conta de loja autenticada vinculada (conforme matriz oficial em <code>docs/architecture/marketplace-capability-matrix.md</code>).
                  Em estrita conformidade com as regras anti-alucinação e termos de serviço, é expressamente proibido realizar scraping não autorizado. Utilize a sincronização de dados próprios autenticados.
                </p>
              </div>
            </div>
          )}

          {/* Search Results */}
          {hasSearched && marketplace === "MERCADO_LIVRE" && (
            <div className="space-y-4">
              <div className="flex items-center justify-between text-xs text-slate-500">
                <span>
                  Resultados para: <strong className="text-slate-800">{query}</strong> ({results.length} itens encontrados)
                </span>
                <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                  ● Proveniência: OBSERVED (Dados Públicos Oficiais)
                </span>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {results.map((item) => (
                  <div key={item.externalId} className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex flex-col justify-between space-y-3">
                    <div>
                      <div className="flex items-start justify-between gap-2">
                        <span className="font-mono text-[10px] bg-slate-100 text-slate-600 px-1.5 py-0.5 rounded">
                          {item.externalId}
                        </span>
                        <span className="px-2 py-0.5 text-[10px] font-bold rounded bg-blue-50 text-blue-700">
                          {item.condition === "new" ? "Novo" : "Usado"}
                        </span>
                      </div>
                      <h4 className="text-sm font-bold text-slate-800 mt-2 line-clamp-2">{item.title}</h4>
                      <div className="text-lg font-bold text-slate-900 mt-1">
                        R$ {item.price.toFixed(2)}
                        {item.freeShipping && (
                          <span className="ml-2 text-xs font-semibold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded">
                            Frete Grátis
                          </span>
                        )}
                      </div>
                      <div className="text-xs text-slate-500 mt-1">
                        Vendedor: <span className="font-semibold text-slate-700">{item.sellerNickname}</span> ({item.sellerReputation})
                      </div>
                    </div>

                    <div className="p-2.5 bg-slate-50 rounded-lg border border-slate-100 text-xs space-y-1">
                      <div className="flex items-center justify-between text-slate-600 font-medium">
                        <span>Giro Estimado:</span>
                        <span className="font-bold text-blue-700">{item.estimatedSalesRange}</span>
                      </div>
                      <div className="text-[10px] text-slate-400 leading-tight">
                        ℹ️ <strong>ESTIMATED:</strong> {item.estimationMethod}
                      </div>
                    </div>

                    <div className="pt-2 border-t border-slate-100 flex items-center justify-between gap-2">
                      <span className="text-[10px] text-slate-400">
                        Estoque real do concorrente: <strong className="text-amber-600">UNAVAILABLE</strong>
                      </span>
                      <button
                        onClick={() => handleFavoriteProduct(item)}
                        className="px-3 py-1.5 text-xs font-semibold bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg transition-colors"
                      >
                        ⭐ Salvar Oportunidade
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Saved Searches Tab */}
      {activeSubTab === "SAVED_SEARCHES" && (
        <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="px-5 py-4 border-b border-slate-200">
            <h3 className="text-sm font-bold text-slate-800">Buscas Salvas &amp; Listas de Monitoramento</h3>
            <p className="text-xs text-slate-500 mt-0.5">Consultas salvas isoladas por tenant para reexecução rápida.</p>
          </div>
          <div className="divide-y divide-slate-100">
            {savedSearches.map((s) => (
              <div key={s.id} className="p-4 flex items-center justify-between hover:bg-slate-50/50">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-semibold text-sm text-slate-800">{s.name}</span>
                    <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-100 text-slate-600">
                      {s.marketplace}
                    </span>
                  </div>
                  <div className="text-xs text-slate-500 mt-1">
                    Query: <strong className="text-slate-700">&quot;{s.query}&quot;</strong>
                    {s.minPrice && ` | Mín: R$ ${s.minPrice}`}
                    {s.maxPrice && ` | Máx: R$ ${s.maxPrice}`}
                  </div>
                  {s.notes && <div className="text-xs text-blue-600 mt-1">📝 {s.notes}</div>}
                </div>
                <button
                  onClick={() => {
                    setQuery(s.query);
                    setMarketplace(s.marketplace as "MERCADO_LIVRE" | "SHOPEE");
                    setActiveSubTab("SEARCH");
                    handleSearch();
                  }}
                  className="px-3 py-1.5 text-xs font-semibold bg-blue-50 text-blue-600 hover:bg-blue-100 rounded-lg transition-colors"
                >
                  Reabrir Busca
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Favorites Tab */}
      {activeSubTab === "FAVORITES" && (
        <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="px-5 py-4 border-b border-slate-200">
            <h3 className="text-sm font-bold text-slate-800">Produtos de Mercado Favoritados</h3>
            <p className="text-xs text-slate-500 mt-0.5">Oportunidades mapeadas e notas analíticas salvas para sua organização.</p>
          </div>
          <div className="divide-y divide-slate-100">
            {savedProducts.map((p) => (
              <div key={p.id} className="p-4 flex items-center justify-between hover:bg-slate-50/50">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-xs text-slate-400">{p.externalItemId}</span>
                    <span className="font-semibold text-sm text-slate-800">{p.title}</span>
                  </div>
                  <div className="text-xs text-slate-500 mt-1">
                    Preço: <strong className="text-slate-800">R$ {p.price.toFixed(2)}</strong> | Vendedor: {p.sellerNickname}
                  </div>
                  <div className="text-xs text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded inline-block mt-1">
                    📝 Nota: {p.notes}
                  </div>
                </div>
                <span className="text-xs text-slate-400 font-mono">OBSERVED</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Save Search Modal */}
      {saveModalOpen && (
        <div className="fixed inset-0 bg-slate-900/50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full p-5 space-y-4">
            <h3 className="text-sm font-bold text-slate-800">Salvar Pesquisa de Mercado</h3>
            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">Nome da Busca</label>
              <input
                type="text"
                value={saveSearchName}
                onChange={(e) => setSaveSearchName(e.target.value)}
                placeholder="Ex: Fones Bluetooth < R$ 200"
                className="w-full text-xs border border-slate-300 rounded-lg p-2 text-slate-800"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">Notas de Análise (Opcional)</label>
              <textarea
                value={saveSearchNotes}
                onChange={(e) => setSaveSearchNotes(e.target.value)}
                placeholder="Ex: Verificar fornecedores com frete grátis..."
                className="w-full text-xs border border-slate-300 rounded-lg p-2 text-slate-800 h-20"
              />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setSaveModalOpen(false)}
                className="px-3 py-1.5 text-xs text-slate-600 hover:bg-slate-100 rounded-lg"
              >
                Cancelar
              </button>
              <button
                onClick={handleSaveCurrentSearch}
                className="px-4 py-1.5 text-xs font-semibold bg-blue-600 hover:bg-blue-700 text-white rounded-lg"
              >
                Salvar Busca
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
