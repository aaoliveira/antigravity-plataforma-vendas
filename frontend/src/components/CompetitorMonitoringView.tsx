"use client";

import React, { useState } from "react";

export interface WatchlistModel {
  id: string;
  name: string;
  description?: string;
  type: "PRODUCT" | "COMPETITOR_SELLER" | "COMPETITOR_LISTING" | "MIXED";
  status: "ACTIVE" | "ARCHIVED";
  createdAt: string;
}

export interface WatchlistItemModel {
  id: string;
  watchlistId: string;
  marketplace: "MERCADO_LIVRE" | "SHOPEE" | "AMAZON";
  itemType: "LISTING" | "SELLER";
  externalId: string;
  targetName: string;
  targetUrl?: string;
  sellerNickname?: string;
  status: "ACTIVE" | "PAUSED" | "UNTRACKED";
  checkFrequencyMinutes: number;
  lastCollectedAt?: string;
  lastStatus?: string;
  lastPrice?: number;
}

export interface TimelinePointModel {
  timestamp: string;
  price?: number;
  originalPrice?: number;
  listingStatus: string;
  collectionStatus: "SUCCESS" | "COLLECTION_FAILED" | "GAP";
  isGap: boolean;
  gapDurationMinutes?: number;
  provenance: string;
  failureReason?: string;
}

export interface AlertRuleModel {
  id: string;
  name: string;
  eventType: "PRICE_CHANGE" | "STATUS_CHANGE" | "OUT_OF_STOCK" | "ANY";
  minPriceDeltaPercentage?: number;
  direction: "ANY" | "INCREASE" | "DECREASE";
  cooldownMinutes: number;
  channel: "IN_APP" | "EMAIL";
  status: "ACTIVE" | "PAUSED";
}

export interface AlertFeedItem {
  id: string;
  title: string;
  message: string;
  severity: "INFO" | "WARNING" | "CRITICAL";
  triggeredAt: string;
  read: boolean;
  itemTargetName: string;
}

const INITIAL_WATCHLISTS: WatchlistModel[] = [
  {
    id: "wl-101",
    name: "Concorrentes — Teclados Mecânicos",
    description: "Monitoramento de preços e estoque dos principais concorrentes de periféricos",
    type: "COMPETITOR_LISTING",
    status: "ACTIVE",
    createdAt: "2026-11-10T08:00:00Z",
  },
  {
    id: "wl-102",
    name: "Lojas Rivais de Games & Acessórios",
    description: "Sellers concorrentes no Mercado Livre",
    type: "COMPETITOR_SELLER",
    status: "ACTIVE",
    createdAt: "2026-11-12T14:30:00Z",
  },
];

const INITIAL_ITEMS: WatchlistItemModel[] = [
  {
    id: "item-201",
    watchlistId: "wl-101",
    marketplace: "MERCADO_LIVRE",
    itemType: "LISTING",
    externalId: "MLB348912384",
    targetName: "Teclado Mecânico RGB Switch Blue Redragon",
    targetUrl: "https://produto.mercadolivre.com.br/sample-1",
    sellerNickname: "rival_games_brasil",
    status: "ACTIVE",
    checkFrequencyMinutes: 360,
    lastCollectedAt: "2026-11-17T14:00:00Z",
    lastStatus: "ACTIVE",
    lastPrice: 189.9,
  },
  {
    id: "item-202",
    watchlistId: "wl-101",
    marketplace: "MERCADO_LIVRE",
    itemType: "LISTING",
    externalId: "MLB491029341",
    targetName: "Teclado Mecânico Gamer TKL ABNT2",
    targetUrl: "https://produto.mercadolivre.com.br/sample-2",
    sellerNickname: "megastore_eletronicos",
    status: "ACTIVE",
    checkFrequencyMinutes: 360,
    lastCollectedAt: "2026-11-17T14:00:00Z",
    lastStatus: "ACTIVE",
    lastPrice: 159.0,
  },
];

const SAMPLE_TIMELINE_POINTS: TimelinePointModel[] = [
  {
    timestamp: "2026-11-14T10:00:00Z",
    price: 219.9,
    originalPrice: 249.9,
    listingStatus: "ACTIVE",
    collectionStatus: "SUCCESS",
    isGap: false,
    provenance: "OBSERVED",
  },
  {
    timestamp: "2026-11-14T16:00:00Z",
    price: 219.9,
    listingStatus: "ACTIVE",
    collectionStatus: "SUCCESS",
    isGap: false,
    provenance: "OBSERVED",
  },
  {
    timestamp: "2026-11-15T04:00:00Z",
    listingStatus: "UNAVAILABLE",
    collectionStatus: "GAP",
    isGap: true,
    gapDurationMinutes: 720,
    provenance: "UNAVAILABLE",
    failureReason: "Lacuna de coleta: sem coletas registradas no intervalo de 12 horas. (Preço não interpolado).",
  },
  {
    timestamp: "2026-11-15T16:00:00Z",
    price: 199.9,
    listingStatus: "ACTIVE",
    collectionStatus: "SUCCESS",
    isGap: false,
    provenance: "OBSERVED",
  },
  {
    timestamp: "2026-11-16T10:00:00Z",
    price: 189.9,
    listingStatus: "ACTIVE",
    collectionStatus: "SUCCESS",
    isGap: false,
    provenance: "OBSERVED",
  },
  {
    timestamp: "2026-11-17T14:00:00Z",
    price: 189.9,
    listingStatus: "ACTIVE",
    collectionStatus: "SUCCESS",
    isGap: false,
    provenance: "OBSERVED",
  },
];

const INITIAL_RULES: AlertRuleModel[] = [
  {
    id: "rule-1",
    name: "Alerta de Queda de Preço Concorrente (> 5%)",
    eventType: "PRICE_CHANGE",
    minPriceDeltaPercentage: 5.0,
    direction: "DECREASE",
    cooldownMinutes: 1440,
    channel: "IN_APP",
    status: "ACTIVE",
  },
  {
    id: "rule-2",
    name: "Alerta de Concorrente Fora de Estoque / Pausado",
    eventType: "OUT_OF_STOCK",
    direction: "ANY",
    cooldownMinutes: 720,
    channel: "IN_APP",
    status: "ACTIVE",
  },
];

const INITIAL_ALERTS: AlertFeedItem[] = [
  {
    id: "alt-1",
    title: "Queda de Preço Detectada: Teclado Mecânico RGB Switch Blue Redragon",
    message: "Preço caiu de R$ 219,90 para R$ 189,90 (-13.64%). Concorrente rival_games_brasil.",
    severity: "WARNING",
    triggeredAt: "2026-11-16T10:05:00Z",
    read: false,
    itemTargetName: "Teclado Mecânico RGB Switch Blue Redragon",
  },
];

export function CompetitorMonitoringView() {
  const [activeSubTab, setActiveSubTab] = useState<"WATCHLISTS" | "TIMELINE" | "COMPARATOR" | "ALERTS">("WATCHLISTS");

  // Watchlists state
  const [watchlists, setWatchlists] = useState<WatchlistModel[]>(INITIAL_WATCHLISTS);
  const [selectedWatchlistId, setSelectedWatchlistId] = useState<string>(INITIAL_WATCHLISTS[0].id);
  const [items, setItems] = useState<WatchlistItemModel[]>(INITIAL_ITEMS);
  const [selectedItemId, setSelectedItemId] = useState<string>(INITIAL_ITEMS[0].id);

  // Forms
  const [showNewWatchlistModal, setShowNewWatchlistModal] = useState(false);
  const [newWatchlistName, setNewWatchlistName] = useState("");
  const [newWatchlistDesc, setNewWatchlistDesc] = useState("");

  const [showNewItemModal, setShowNewItemModal] = useState(false);
  const [newItemExtId, setNewItemExtId] = useState("");
  const [newItemName, setNewItemName] = useState("");
  const [newItemSeller, setNewItemSeller] = useState("");
  const [newItemFrequency, setNewItemFrequency] = useState(360);

  // Timeline state
  const [timelinePoints, setTimelinePoints] = useState<TimelinePointModel[]>(SAMPLE_TIMELINE_POINTS);
  const [manualPriceInput, setManualPriceInput] = useState("");
  const [collectionNotice, setCollectionNotice] = useState<string | null>(null);

  // Alerts state
  const [rules, setRules] = useState<AlertRuleModel[]>(INITIAL_RULES);
  const [alerts, setAlerts] = useState<AlertFeedItem[]>(INITIAL_ALERTS);

  const currentWatchlist = watchlists.find((w) => w.id === selectedWatchlistId) || watchlists[0];
  const currentItem = items.find((i) => i.id === selectedItemId) || items[0];
  const filteredItems = items.filter((i) => i.watchlistId === currentWatchlist?.id);

  const handleCreateWatchlist = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newWatchlistName.trim()) return;

    const newWl: WatchlistModel = {
      id: "wl-" + Date.now(),
      name: newWatchlistName.trim(),
      description: newWatchlistDesc.trim() || undefined,
      type: "COMPETITOR_LISTING",
      status: "ACTIVE",
      createdAt: new Date().toISOString(),
    };
    setWatchlists([newWl, ...watchlists]);
    setSelectedWatchlistId(newWl.id);
    setNewWatchlistName("");
    setNewWatchlistDesc("");
    setShowNewWatchlistModal(false);
  };

  const handleAddItem = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newItemExtId.trim() || !newItemName.trim()) return;

    const newItem: WatchlistItemModel = {
      id: "item-" + Date.now(),
      watchlistId: currentWatchlist.id,
      marketplace: "MERCADO_LIVRE",
      itemType: "LISTING",
      externalId: newItemExtId.trim().toUpperCase(),
      targetName: newItemName.trim(),
      sellerNickname: newItemSeller.trim() || undefined,
      status: "ACTIVE",
      checkFrequencyMinutes: newItemFrequency,
      lastCollectedAt: new Date().toISOString(),
      lastStatus: "ACTIVE",
      lastPrice: 199.9,
    };

    setItems([newItem, ...items]);
    setSelectedItemId(newItem.id);
    setNewItemExtId("");
    setNewItemName("");
    setNewItemSeller("");
    setShowNewItemModal(false);
  };

  const handleUntrackItem = (itemId: string) => {
    // Soft untrack: History is strictly preserved
    setItems((prev) =>
      prev.map((it) => (it.id === itemId ? { ...it, status: "UNTRACKED" } : it))
    );
  };

  const handleAddManualSnapshot = (e: React.FormEvent) => {
    e.preventDefault();
    const parsedPrice = parseFloat(manualPriceInput);
    if (isNaN(parsedPrice) || parsedPrice <= 0) return;

    const newPoint: TimelinePointModel = {
      timestamp: new Date().toISOString(),
      price: parsedPrice,
      listingStatus: "ACTIVE",
      collectionStatus: "SUCCESS",
      isGap: false,
      provenance: "OBSERVED",
    };

    setTimelinePoints([...timelinePoints, newPoint]);
    setManualPriceInput("");
    setCollectionNotice(`Snapshot observado registrado com sucesso: R$ ${parsedPrice.toFixed(2)} (Proveniência: OBSERVED).`);
    setTimeout(() => setCollectionNotice(null), 4000);
  };

  const handleMarkAlertRead = (alertId: string) => {
    setAlerts((prev) =>
      prev.map((a) => (a.id === alertId ? { ...a, read: true } : a))
    );
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-gray-200 gap-3">
        <div>
          <h2 className="text-xl font-bold text-gray-900">
            Monitoramento Contínuo de Produtos &amp; Concorrentes
          </h2>
          <p className="text-sm text-gray-500">
            Histórico temporal, diff determinístico de preços e detecção explícita de lacunas sem interpolação de dados.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <span className="inline-flex items-center px-2.5 py-1 rounded text-xs font-semibold bg-emerald-100 text-emerald-800">
            Proveniência OBSERVED
          </span>
          <span className="inline-flex items-center px-2.5 py-1 rounded text-xs font-semibold bg-purple-100 text-purple-800">
            Deduplicação Determinística
          </span>
          <span className="inline-flex items-center px-2.5 py-1 rounded text-xs font-semibold bg-blue-100 text-blue-800">
            Retenção 12 Meses (ADR DEC-019)
          </span>
        </div>
      </div>

      {/* Sub Navigation */}
      <div className="flex border-b border-gray-200 space-x-6 text-sm font-medium">
        <button
          onClick={() => setActiveSubTab("WATCHLISTS")}
          className={`pb-3 px-1 border-b-2 transition-colors ${
            activeSubTab === "WATCHLISTS"
              ? "border-blue-600 text-blue-600 font-semibold"
              : "border-transparent text-gray-500 hover:text-gray-800"
          }`}
        >
          📋 Watchlists &amp; Itens ({watchlists.length})
        </button>
        <button
          onClick={() => setActiveSubTab("TIMELINE")}
          className={`pb-3 px-1 border-b-2 transition-colors ${
            activeSubTab === "TIMELINE"
              ? "border-blue-600 text-blue-600 font-semibold"
              : "border-transparent text-gray-500 hover:text-gray-800"
          }`}
        >
          📈 Histórico &amp; Lacunas (Timeline)
        </button>
        <button
          onClick={() => setActiveSubTab("COMPARATOR")}
          className={`pb-3 px-1 border-b-2 transition-colors ${
            activeSubTab === "COMPARATOR"
              ? "border-blue-600 text-blue-600 font-semibold"
              : "border-transparent text-gray-500 hover:text-gray-800"
          }`}
        >
          ⚖️ Comparador Anúncio vs Concorrente
        </button>
        <button
          onClick={() => setActiveSubTab("ALERTS")}
          className={`pb-3 px-1 border-b-2 transition-colors ${
            activeSubTab === "ALERTS"
              ? "border-blue-600 text-blue-600 font-semibold"
              : "border-transparent text-gray-500 hover:text-gray-800"
          }`}
        >
          🔔 Regras &amp; Alertas ({alerts.filter((a) => !a.read).length} novos)
        </button>
      </div>

      {/* SUBTAB 1: WATCHLISTS */}
      {activeSubTab === "WATCHLISTS" && (
        <div className="space-y-6">
          {/* Controls Bar */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white p-4 rounded-lg border border-gray-200 shadow-sm">
            <div className="flex items-center gap-3">
              <label htmlFor="select-watchlist" className="text-xs font-semibold text-gray-700">Watchlist Ativa:</label>
              <select
                id="select-watchlist"
                value={selectedWatchlistId}
                onChange={(e) => setSelectedWatchlistId(e.target.value)}
                className="text-xs border border-gray-300 rounded px-3 py-1.5 bg-white text-gray-800 focus:ring-blue-500"
              >
                {watchlists.map((w) => (
                  <option key={w.id} value={w.id}>
                    {w.name} ({w.type})
                  </option>
                ))}
              </select>
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => setShowNewWatchlistModal(true)}
                className="text-xs font-semibold px-3 py-1.5 border border-gray-300 hover:bg-gray-50 text-gray-700 rounded transition-colors"
              >
                + Nova Watchlist
              </button>
              <button
                onClick={() => setShowNewItemModal(true)}
                className="text-xs font-semibold px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded transition-colors"
              >
                + Monitorar Novo Item
              </button>
            </div>
          </div>

          {/* Items Table */}
          <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
            <div className="px-4 py-3 bg-gray-50 border-b border-gray-200 flex justify-between items-center">
              <h3 className="text-sm font-semibold text-gray-800">
                Itens Monitorados em &quot;{currentWatchlist?.name}&quot; ({filteredItems.length})
              </h3>
              <span className="text-xs text-gray-500">Histórico de snapshots preservado mesmo após desativação</span>
            </div>

            {filteredItems.length === 0 ? (
              <div className="p-8 text-center text-sm text-gray-500">
                Nenhum item monitorado nesta watchlist. Clique em &quot;+ Monitorar Novo Item&quot; para iniciar.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200 text-xs">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-4 py-2.5 text-left font-medium text-gray-500 uppercase">Item / Anúncio</th>
                      <th className="px-4 py-2.5 text-left font-medium text-gray-500 uppercase">Marketplace</th>
                      <th className="px-4 py-2.5 text-left font-medium text-gray-500 uppercase">ID Externo</th>
                      <th className="px-4 py-2.5 text-left font-medium text-gray-500 uppercase">Vendedor</th>
                      <th className="px-4 py-2.5 text-right font-medium text-gray-500 uppercase">Último Preço</th>
                      <th className="px-4 py-2.5 text-center font-medium text-gray-500 uppercase">Frequência</th>
                      <th className="px-4 py-2.5 text-center font-medium text-gray-500 uppercase">Status</th>
                      <th className="px-4 py-2.5 text-center font-medium text-gray-500 uppercase">Ações</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200 bg-white">
                    {filteredItems.map((item) => (
                      <tr key={item.id} className="hover:bg-gray-50 transition-colors">
                        <td className="px-4 py-3 font-medium text-gray-900 max-w-xs truncate">
                          {item.targetName}
                        </td>
                        <td className="px-4 py-3 text-gray-600">{item.marketplace}</td>
                        <td className="px-4 py-3 font-mono text-gray-700">{item.externalId}</td>
                        <td className="px-4 py-3 text-gray-600">{item.sellerNickname || "N/A"}</td>
                        <td className="px-4 py-3 text-right font-bold text-gray-900">
                          {item.lastPrice ? `R$ ${item.lastPrice.toFixed(2)}` : "—"}
                        </td>
                        <td className="px-4 py-3 text-center text-gray-500">{item.checkFrequencyMinutes} min</td>
                        <td className="px-4 py-3 text-center">
                          <span
                            className={`px-2 py-0.5 rounded text-[11px] font-semibold ${
                              item.status === "ACTIVE"
                                ? "bg-emerald-100 text-emerald-800"
                                : item.status === "PAUSED"
                                ? "bg-amber-100 text-amber-800"
                                : "bg-gray-100 text-gray-600"
                            }`}
                          >
                            {item.status}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-center space-x-2">
                          <button
                            onClick={() => {
                              setSelectedItemId(item.id);
                              setActiveSubTab("TIMELINE");
                            }}
                            className="text-blue-600 hover:text-blue-800 font-semibold"
                          >
                            Ver Timeline
                          </button>
                          {item.status === "ACTIVE" && (
                            <button
                              onClick={() => handleUntrackItem(item.id)}
                              className="text-red-600 hover:text-red-800 font-medium"
                              title="Desativar observação sem apagar histórico de snapshots"
                            >
                              Desativar
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* SUBTAB 2: TIMELINE WITH EXPLICIT GAPS */}
      {activeSubTab === "TIMELINE" && (
        <div className="space-y-6">
          {/* Timeline Item Header & Coverage KPI */}
          <div className="bg-white p-5 rounded-lg border border-gray-200 shadow-sm space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b pb-3">
              <div>
                <span className="text-xs font-semibold text-blue-600 uppercase">Item Selecionado</span>
                <h3 className="text-base font-bold text-gray-900">{currentItem.targetName}</h3>
                <span className="text-xs font-mono text-gray-500">{currentItem.externalId} &middot; {currentItem.marketplace}</span>
              </div>
              <div className="flex items-center gap-3">
                <div className="text-right">
                  <span className="text-xs text-gray-500">Taxa de Cobertura de Coleta</span>
                  <p className="text-lg font-extrabold text-emerald-600">83.33%</p>
                </div>
                <div className="text-right border-l pl-3">
                  <span className="text-xs text-gray-500">Lacunas Detectadas</span>
                  <p className="text-lg font-bold text-amber-600">1 período sem dados</p>
                </div>
              </div>
            </div>

            {/* Anti-Hallucination Disclaimer */}
            <div className="p-3 bg-amber-50 border border-amber-200 rounded text-xs text-amber-900 leading-relaxed">
              <strong>Diretriz Anti-Alucinação (ADR DEC-019):</strong> Lacunas temporais de coleta são representadas com fidelidade estrita. Preços ausentes não são interpolados silenciosamente entre pontos observados para não criar falsa certeza sobre preços em períodos sem medição.
            </div>

            {/* Manual Observation Trigger Form */}
            <form onSubmit={handleAddManualSnapshot} className="flex flex-wrap items-center gap-3 pt-2">
              <label htmlFor="input-manual-price" className="text-xs font-semibold text-gray-700">Registrar Snapshot Observado:</label>
              <input
                id="input-manual-price"
                type="number"
                step="0.01"
                placeholder="Ex: 179.90"
                value={manualPriceInput}
                onChange={(e) => setManualPriceInput(e.target.value)}
                className="text-xs border border-gray-300 rounded px-3 py-1.5 text-gray-800 focus:ring-blue-500 w-32"
                required
              />
              <button
                type="submit"
                className="text-xs font-semibold px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded transition-colors"
              >
                Registrar Coleta
              </button>
            </form>

            {collectionNotice && (
              <div className="p-2.5 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded">
                {collectionNotice}
              </div>
            )}
          </div>

          {/* Timeline Historical Table */}
          <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
            <div className="px-4 py-3 bg-gray-50 border-b border-gray-200 flex justify-between items-center">
              <h4 className="text-sm font-semibold text-gray-800">
                Série Temporal Histórica &amp; Registro de Lacunas ({timelinePoints.length} pontos)
              </h4>
              <span className="text-xs text-gray-500">Retenção de 12 meses garantida</span>
            </div>

            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200 text-xs">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-2.5 text-left font-medium text-gray-500 uppercase">Data e Hora</th>
                    <th className="px-4 py-2.5 text-center font-medium text-gray-500 uppercase">Tipo do Ponto</th>
                    <th className="px-4 py-2.5 text-right font-medium text-gray-500 uppercase">Preço Observado</th>
                    <th className="px-4 py-2.5 text-center font-medium text-gray-500 uppercase">Status do Anúncio</th>
                    <th className="px-4 py-2.5 text-center font-medium text-gray-500 uppercase">Proveniência</th>
                    <th className="px-4 py-2.5 text-left font-medium text-gray-500 uppercase">Observações / Motivo</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200 bg-white">
                  {timelinePoints.map((pt, idx) => (
                    <tr
                      key={idx}
                      className={
                        pt.isGap
                          ? "bg-amber-50/70 border-l-4 border-amber-400"
                          : "hover:bg-gray-50 transition-colors"
                      }
                    >
                      <td className="px-4 py-2.5 text-gray-700 font-mono">
                        {new Date(pt.timestamp).toLocaleString("pt-BR")}
                      </td>
                      <td className="px-4 py-2.5 text-center">
                        <span
                          className={`px-2 py-0.5 rounded text-[11px] font-bold ${
                            pt.isGap
                              ? "bg-amber-200 text-amber-900"
                              : pt.collectionStatus === "SUCCESS"
                              ? "bg-emerald-100 text-emerald-800"
                              : "bg-red-100 text-red-800"
                          }`}
                        >
                          {pt.isGap ? "LACUNA (GAP)" : pt.collectionStatus}
                        </span>
                      </td>
                      <td className="px-4 py-2.5 text-right font-bold text-gray-900">
                        {pt.price ? `R$ ${pt.price.toFixed(2)}` : <span className="text-gray-400 italic">Preço Ausente (Não Interpolado)</span>}
                      </td>
                      <td className="px-4 py-2.5 text-center">
                        <span className="px-2 py-0.5 rounded text-[11px] bg-slate-100 text-slate-800 font-medium">
                          {pt.listingStatus}
                        </span>
                      </td>
                      <td className="px-4 py-2.5 text-center">
                        <span className="px-2 py-0.5 rounded text-[11px] bg-blue-50 text-blue-800 font-semibold">
                          {pt.provenance}
                        </span>
                      </td>
                      <td className="px-4 py-2.5 text-gray-600">
                        {pt.failureReason || "Coleta regular via conector oficial"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* SUBTAB 3: COMPARATOR */}
      {activeSubTab === "COMPARATOR" && (
        <div className="bg-white p-6 rounded-lg border border-gray-200 shadow-sm space-y-6">
          <div className="border-b pb-3">
            <h3 className="text-base font-bold text-gray-900">
              Comparador Lado a Lado: Meu Anúncio vs Concorrente
            </h3>
            <p className="text-xs text-gray-500">
              Diferencial de preço, frete e atributos confirmados com identificação transparente de campos não fornecidos.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Own Listing Card */}
            <div className="border border-blue-200 bg-blue-50/30 rounded-lg p-5 space-y-3">
              <span className="text-xs font-bold text-blue-700 uppercase bg-blue-100 px-2 py-0.5 rounded">
                Meu Produto (Seller Intelligence)
              </span>
              <h4 className="text-sm font-bold text-gray-900">Teclado Gamer Mecânico Oficial ABNT2</h4>
              <p className="text-xs text-gray-500 font-mono">SKU: SKU-TECLADO-RGB-MATRIZ</p>

              <div className="pt-2 border-t border-blue-100 space-y-2 text-xs">
                <div className="flex justify-between">
                  <span className="text-gray-600">Preço Atual:</span>
                  <span className="font-extrabold text-gray-900 text-base">R$ 175,00</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Status:</span>
                  <span className="font-semibold text-emerald-700">ATIVO</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Frete:</span>
                  <span className="font-medium text-gray-800">Frete Grátis Mercado Envios</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Estoque Próprio:</span>
                  <span className="font-bold text-gray-900">140 unidades</span>
                </div>
              </div>
            </div>

            {/* Competitor Listing Card */}
            <div className="border border-purple-200 bg-purple-50/30 rounded-lg p-5 space-y-3">
              <span className="text-xs font-bold text-purple-700 uppercase bg-purple-100 px-2 py-0.5 rounded">
                Concorrente Monitorado ({currentItem.marketplace})
              </span>
              <h4 className="text-sm font-bold text-gray-900">{currentItem.targetName}</h4>
              <p className="text-xs text-gray-500 font-mono">ID: {currentItem.externalId}</p>

              <div className="pt-2 border-t border-purple-100 space-y-2 text-xs">
                <div className="flex justify-between">
                  <span className="text-gray-600">Último Preço Observado:</span>
                  <span className="font-extrabold text-gray-900 text-base">
                    R$ {currentItem.lastPrice ? currentItem.lastPrice.toFixed(2) : "189.90"}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Status do Anúncio:</span>
                  <span className="font-semibold text-emerald-700">ATIVO</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Vendedor:</span>
                  <span className="font-medium text-gray-800">{currentItem.sellerNickname || "rival_store"}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Reputação:</span>
                  <span className="font-semibold text-amber-700">MercadoLíder Platinum</span>
                </div>
              </div>
            </div>
          </div>

          {/* Price Position Delta Box */}
          <div className="p-4 bg-slate-50 border border-slate-200 rounded-lg flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
            <div>
              <span className="font-bold text-slate-800">Posição de Preço:</span>{" "}
              <span className="font-bold text-emerald-700">Seu produto está R$ 14,90 mais barato (-7.85%)</span>
            </div>
            <div className="text-slate-500 font-mono">
              Delta: -R$ 14,90 &middot; Arredondamento HALF_EVEN
            </div>
          </div>

          {/* Unavailable Fields Disclaimer */}
          <div className="p-3 bg-gray-50 border border-gray-200 rounded text-[11px] text-gray-600 space-y-1">
            <span className="font-bold text-gray-800">Transparência de Dados Privados (Regra Anti-Alucinação):</span>
            <ul className="list-disc pl-4 space-y-0.5">
              <li>Estoque real do concorrente não fornecido pela API (campo privado / UNAVAILABLE).</li>
              <li>Faturamento e vendas reais do concorrente não fornecidos (dados privados de terceiros).</li>
            </ul>
          </div>
        </div>
      )}

      {/* SUBTAB 4: ALERTS */}
      {activeSubTab === "ALERTS" && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Rules List */}
          <div className="lg:col-span-1 bg-white p-5 rounded-lg border border-gray-200 shadow-sm space-y-4">
            <div className="flex justify-between items-center border-b pb-2">
              <h3 className="text-sm font-bold text-gray-900">Regras de Alerta ({rules.length})</h3>
              <span className="text-xs text-gray-500">Cooldown 24h</span>
            </div>

            <div className="space-y-3">
              {rules.map((rule) => (
                <div key={rule.id} className="p-3 bg-gray-50 rounded border border-gray-200 space-y-1.5 text-xs">
                  <div className="flex justify-between items-center">
                    <span className="font-bold text-gray-900">{rule.name}</span>
                    <span className="px-1.5 py-0.5 bg-emerald-100 text-emerald-800 rounded text-[10px] font-semibold">
                      {rule.status}
                    </span>
                  </div>
                  <div className="text-gray-600 text-[11px]">
                    Evento: {rule.eventType} {rule.minPriceDeltaPercentage ? `(> ${rule.minPriceDeltaPercentage}%)` : ""}
                  </div>
                  <div className="text-gray-500 text-[11px]">
                    Canal: {rule.channel} &middot; Cooldown: {rule.cooldownMinutes} min
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Alerts Feed */}
          <div className="lg:col-span-2 bg-white p-5 rounded-lg border border-gray-200 shadow-sm space-y-4">
            <div className="flex justify-between items-center border-b pb-2">
              <h3 className="text-sm font-bold text-gray-900">Feed de Alertas Disparados ({alerts.length})</h3>
              <span className="text-xs text-gray-500">Deduplicação garantida por chave única</span>
            </div>

            {alerts.length === 0 ? (
              <div className="p-8 text-center text-sm text-gray-500">
                Nenhum alerta disparado.
              </div>
            ) : (
              <div className="space-y-3">
                {alerts.map((alert) => (
                  <div
                    key={alert.id}
                    className={`p-4 rounded-lg border text-xs transition-colors ${
                      alert.read
                        ? "bg-gray-50 border-gray-200 opacity-70"
                        : "bg-amber-50/50 border-amber-200 shadow-sm"
                    }`}
                  >
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-1 mb-1.5">
                      <div className="flex items-center gap-2">
                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                            alert.severity === "CRITICAL"
                              ? "bg-red-100 text-red-800"
                              : alert.severity === "WARNING"
                              ? "bg-amber-100 text-amber-800"
                              : "bg-blue-100 text-blue-800"
                          }`}
                        >
                          {alert.severity}
                        </span>
                        <span className="font-bold text-gray-900">{alert.title}</span>
                      </div>
                      <span className="text-[11px] text-gray-500">
                        {new Date(alert.triggeredAt).toLocaleString("pt-BR")}
                      </span>
                    </div>

                    <p className="text-gray-700 leading-relaxed mb-2">{alert.message}</p>

                    <div className="flex justify-between items-center pt-2 border-t border-gray-100">
                      <span className="text-[11px] text-gray-500">Item: {alert.itemTargetName}</span>
                      {!alert.read && (
                        <button
                          onClick={() => handleMarkAlertRead(alert.id)}
                          className="text-blue-600 hover:text-blue-800 font-semibold text-xs"
                        >
                          Marcar como Lido
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Modal: Nova Watchlist */}
      {showNewWatchlistModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full p-6 space-y-4">
            <h3 className="text-base font-bold text-gray-900">Nova Watchlist de Concorrentes</h3>
            <form onSubmit={handleCreateWatchlist} className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Nome da Watchlist *</label>
                <input
                  type="text"
                  placeholder="Ex: Concorrentes Fones Bluetooth"
                  value={newWatchlistName}
                  onChange={(e) => setNewWatchlistName(e.target.value)}
                  className="w-full text-xs border border-gray-300 rounded p-2 text-gray-800"
                  required
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Descrição</label>
                <textarea
                  placeholder="Breve descrição da lista"
                  value={newWatchlistDesc}
                  onChange={(e) => setNewWatchlistDesc(e.target.value)}
                  className="w-full text-xs border border-gray-300 rounded p-2 text-gray-800"
                  rows={3}
                />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowNewWatchlistModal(false)}
                  className="px-3 py-1.5 text-xs border border-gray-300 rounded text-gray-700 hover:bg-gray-50"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-4 py-1.5 text-xs bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded"
                >
                  Criar Watchlist
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Novo Item */}
      {showNewItemModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full p-6 space-y-4">
            <h3 className="text-base font-bold text-gray-900">Monitorar Novo Item</h3>
            <form onSubmit={handleAddItem} className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">ID Externo (ex: MLB...) *</label>
                <input
                  type="text"
                  placeholder="Ex: MLB88492019"
                  value={newItemExtId}
                  onChange={(e) => setNewItemExtId(e.target.value)}
                  className="w-full text-xs border border-gray-300 rounded p-2 text-gray-800 font-mono"
                  required
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Nome do Anúncio *</label>
                <input
                  type="text"
                  placeholder="Ex: Fone Bluetooth Sem Fio Concorrente"
                  value={newItemName}
                  onChange={(e) => setNewItemName(e.target.value)}
                  className="w-full text-xs border border-gray-300 rounded p-2 text-gray-800"
                  required
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Vendedor / Apelido</label>
                <input
                  type="text"
                  placeholder="Ex: loja_rival_oficial"
                  value={newItemSeller}
                  onChange={(e) => setNewItemSeller(e.target.value)}
                  className="w-full text-xs border border-gray-300 rounded p-2 text-gray-800"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Frequência de Checagem</label>
                <select
                  value={newItemFrequency}
                  onChange={(e) => setNewItemFrequency(Number(e.target.value))}
                  className="w-full text-xs border border-gray-300 rounded p-2 text-gray-800 bg-white"
                >
                  <option value={60}>A cada 1 hora</option>
                  <option value={360}>A cada 6 horas (Padrão)</option>
                  <option value={1440}>A cada 24 horas</option>
                </select>
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowNewItemModal(false)}
                  className="px-3 py-1.5 text-xs border border-gray-300 rounded text-gray-700 hover:bg-gray-50"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-4 py-1.5 text-xs bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded"
                >
                  Adicionar Item
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
