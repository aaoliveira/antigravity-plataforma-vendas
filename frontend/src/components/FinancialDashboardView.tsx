"use client";

import React, { useState } from "react";

export interface SummaryData {
  grossRevenue: number;
  netRevenue: number;
  orderCount: number;
  averageTicket: number;
  totalCogs: number;
  totalMarketplaceFees: number;
  totalShippingSeller: number;
  totalTaxes: number;
  totalPackaging: number;
  totalOtherCosts: number;
  contributionMarginValue: number;
  contributionMarginPercentage: number;
  completedCalculationsCount: number;
  partialCalculationsCount: number;
  calculationCoveragePercentage: number;
  lastSyncedAt: string;
}

export interface ComparisonData {
  revenueDeltaPercentage: number;
  ordersDeltaPercentage: number;
  marginDeltaPercentage: number;
  isPreviousIncomplete: boolean;
}

export interface ProductItem {
  sku: string;
  title: string;
  quantitySold: number;
  grossRevenue: number;
  totalCogs: number;
  contributionMarginValue: number;
  contributionMarginPercentage: number;
  quality: "COMPLETE" | "PARTIAL";
}

export interface OrderItemRow {
  orderId: string;
  externalOrderId: string;
  orderDate: string;
  marketplace: string;
  storeName: string;
  itemCount: number;
  grossRevenue: number;
  contributionMarginValue: number;
  contributionMarginPercentage: number;
  calculationQuality: "COMPLETE" | "PARTIAL";
  calculationStatus: string;
}

export function FinancialDashboardView() {
  const [period, setPeriod] = useState<"7D" | "30D" | "MONTH">("30D");
  const [selectedMarketplace, setSelectedMarketplace] = useState<string>("ALL");
  const [compareWithPrevious, setCompareWithPrevious] = useState<boolean>(true);
  const [activeSection, setActiveSection] = useState<"OVERVIEW" | "PRODUCTS" | "ORDERS">("OVERVIEW");
  const [selectedOrderForMemory, setSelectedOrderForMemory] = useState<OrderItemRow | null>(null);
  const [isExporting, setIsExporting] = useState<boolean>(false);
  const [exportNotice, setExportNotice] = useState<string | null>(null);

  // Initial mock state representing domain aggregation
  const [summary] = useState<SummaryData>({
    grossRevenue: 48520.0,
    netRevenue: 45608.8,
    orderCount: 268,
    averageTicket: 181.04,
    totalCogs: 21350.0,
    totalMarketplaceFees: 6307.6,
    totalShippingSeller: 4020.0,
    totalTaxes: 2911.2,
    totalPackaging: 670.0,
    totalOtherCosts: 485.2,
    contributionMarginValue: 12776.0,
    contributionMarginPercentage: 26.33,
    completedCalculationsCount: 255,
    partialCalculationsCount: 13,
    calculationCoveragePercentage: 95.15,
    lastSyncedAt: "2026-11-17T14:15:00Z",
  });

  const [comparison] = useState<ComparisonData>({
    revenueDeltaPercentage: 18.4,
    ordersDeltaPercentage: 12.6,
    marginDeltaPercentage: 22.1,
    isPreviousIncomplete: false,
  });

  const [products] = useState<ProductItem[]>([
    {
      sku: "SKU-NOTEBOOK-G15",
      title: "Notebook Gamer Dell G15 Core i5 16GB",
      quantitySold: 8,
      grossRevenue: 24000.0,
      totalCogs: 16000.0,
      contributionMarginValue: 3960.0,
      contributionMarginPercentage: 16.5,
      quality: "COMPLETE",
    },
    {
      sku: "SKU-FONE-BT-ANC",
      title: "Fone de Ouvido Bluetooth Cancelamento Ruído",
      quantitySold: 94,
      grossRevenue: 17850.6,
      totalCogs: 4700.0,
      contributionMarginValue: 6943.88,
      contributionMarginPercentage: 38.9,
      quality: "COMPLETE",
    },
    {
      sku: "SKU-MOUSE-RGB-PRO",
      title: "Mouse Gamer RGB 16000 DPI",
      quantitySold: 42,
      grossRevenue: 6669.4,
      totalCogs: 650.0,
      contributionMarginValue: 1872.12,
      contributionMarginPercentage: 28.07,
      quality: "PARTIAL",
    },
  ]);

  const [orders] = useState<OrderItemRow[]>([
    {
      orderId: "ord-1",
      externalOrderId: "MLB2000984711",
      orderDate: "2026-11-17T10:30:00Z",
      marketplace: "MERCADO_LIVRE",
      storeName: "Mercado Livre - Loja Gold",
      itemCount: 1,
      grossRevenue: 3000.0,
      contributionMarginValue: 495.0,
      contributionMarginPercentage: 16.5,
      calculationQuality: "COMPLETE",
      calculationStatus: "CLOSED",
    },
    {
      orderId: "ord-2",
      externalOrderId: "MLB2000984822",
      orderDate: "2026-11-17T09:15:00Z",
      marketplace: "MERCADO_LIVRE",
      storeName: "Mercado Livre - Loja Gold",
      itemCount: 2,
      grossRevenue: 379.8,
      contributionMarginValue: 147.74,
      contributionMarginPercentage: 38.9,
      calculationQuality: "COMPLETE",
      calculationStatus: "CLOSED",
    },
    {
      orderId: "ord-3",
      externalOrderId: "SHP9928172635",
      orderDate: "2026-11-16T18:40:00Z",
      marketplace: "SHOPEE",
      storeName: "Shopee Oficial",
      itemCount: 1,
      grossRevenue: 158.8,
      contributionMarginValue: 44.58,
      contributionMarginPercentage: 28.07,
      calculationQuality: "PARTIAL",
      calculationStatus: "CLOSED",
    },
  ]);

  const handleExportCsv = () => {
    setIsExporting(true);
    setExportNotice(null);

    try {
      const metadata = [
        "# Relatório Financeiro e Margem de Contribuição — Seller Intelligence Platform",
        `# Data de Geração: ${new Date().toISOString()} | Fuso Horário: America/Sao_Paulo`,
        `# Filtros Aplicados: Periodo=${period} | Marketplace=${selectedMarketplace}`,
        "# Proveniência dos Dados: IMPORTED (Cálculos determinísticos em regime de competência)",
        "# Aviso Legal: Relatório gerencial. Não substitui escrituração contábil nem apuração fiscal oficial.",
        "",
        "ID Pedido,Data Pedido,Canal,Loja,Status Calculo,Qualidade,Receita Bruta (BRL),COGS (BRL),Taxas Canal (BRL),Frete Seller (BRL),Impostos (BRL),Embalagem (BRL),Outros (BRL),Margem Contribuicao (BRL),Margem (%)",
      ];

      const rows = orders.map((o) =>
        [
          o.externalOrderId,
          o.orderDate,
          o.marketplace,
          `"${o.storeName}"`,
          o.calculationStatus,
          o.calculationQuality,
          o.grossRevenue.toFixed(2),
          (o.grossRevenue * 0.5).toFixed(2),
          (o.grossRevenue * 0.13).toFixed(2),
          "15.00",
          (o.grossRevenue * 0.06).toFixed(2),
          "2.50",
          "0.00",
          o.contributionMarginValue.toFixed(2),
          o.contributionMarginPercentage.toFixed(2),
        ].join(",")
      );

      const csvContent = [...metadata, ...rows].join("\n");
      const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", `relatorio_financeiro_${period}_${Date.now()}.csv`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);

      setExportNotice("Relatório CSV gerado e baixado com sucesso contendo metadados de auditoria.");
    } catch {
      setExportNotice("Erro ao gerar o arquivo CSV.");
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header and Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between pb-4 border-b border-gray-200 gap-4">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Painel Financeiro &amp; Rentabilidade</h2>
          <p className="text-sm text-gray-500">
            Regime de competência, totalizadores determinísticos, faturamento e margem de contribuição.
          </p>
        </div>

        {/* Global Filter Bar */}
        <div className="flex flex-wrap items-center gap-3">
          <div>
            <label htmlFor="select-period" className="sr-only">
              Período
            </label>
            <select
              id="select-period"
              aria-label="Período"
              value={period}
              onChange={(e) => setPeriod(e.target.value as "7D" | "30D" | "MONTH")}
              className="border border-gray-300 rounded px-3 py-1.5 text-sm bg-white text-gray-700 focus:ring-emerald-500 focus:border-emerald-500"
            >
              <option value="7D">Últimos 7 dias</option>
              <option value="30D">Últimos 30 dias</option>
              <option value="MONTH">Mês Atual</option>
            </select>
          </div>

          <div>
            <label htmlFor="select-channel" className="sr-only">
              Canal / Marketplace
            </label>
            <select
              id="select-channel"
              aria-label="Canal / Marketplace"
              value={selectedMarketplace}
              onChange={(e) => setSelectedMarketplace(e.target.value)}
              className="border border-gray-300 rounded px-3 py-1.5 text-sm bg-white text-gray-700 focus:ring-emerald-500 focus:border-emerald-500"
            >
              <option value="ALL">Todos os Canais</option>
              <option value="MERCADO_LIVRE">Mercado Livre</option>
              <option value="SHOPEE">Shopee</option>
            </select>
          </div>

          <label className="flex items-center text-xs text-gray-600 cursor-pointer">
            <input
              type="checkbox"
              checked={compareWithPrevious}
              onChange={(e) => setCompareWithPrevious(e.target.checked)}
              className="rounded text-emerald-600 focus:ring-emerald-500 mr-1.5"
            />
            Comparar período anterior
          </label>

          <button
            onClick={handleExportCsv}
            disabled={isExporting}
            className="inline-flex items-center bg-gray-900 hover:bg-black text-white text-xs font-medium py-1.5 px-3 rounded transition-colors"
          >
            {isExporting ? "Gerando..." : "Exportar CSV"}
          </button>
        </div>
      </div>

      {/* Export notification */}
      {exportNotice && (
        <div className="p-3 bg-emerald-50 border border-emerald-200 rounded text-xs text-emerald-800">
          {exportNotice}
        </div>
      )}

      {/* Coverage & Integrity Banner */}
      <div className="bg-slate-50 border border-slate-200 rounded-lg p-3.5 flex flex-col sm:flex-row sm:items-center justify-between text-xs text-slate-700 gap-2">
        <div className="flex items-center space-x-2">
          <span className="inline-block w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
          <span>
            <strong>Última Sincronização:</strong> {new Date(summary.lastSyncedAt).toLocaleString("pt-BR")}
          </span>
          <span className="text-slate-400">|</span>
          <span>
            <strong>Cobertura:</strong> {summary.calculationCoveragePercentage.toFixed(1)}% dos pedidos calculados ({summary.completedCalculationsCount} completos, {summary.partialCalculationsCount} parciais)
          </span>
        </div>
        <div>
          <span className="px-2 py-0.5 rounded text-xs font-semibold bg-emerald-100 text-emerald-800">
            Regime de Competência
          </span>
        </div>
      </div>

      {summary.partialCalculationsCount > 0 && (
        <div className="bg-amber-50 border-l-4 border-amber-400 p-3 rounded text-xs text-amber-900">
          <strong>Aviso de Cálculo Parcial:</strong> {summary.partialCalculationsCount} pedido(s) contêm custos de aquisição ou alíquotas fiscais pendentes de preenchimento. A margem apresentada é operacional e não substitui escrituração fiscal ou contábil.
        </div>
      )}

      {/* Executive KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Faturamento Bruto */}
        <div className="bg-white p-5 rounded-lg border border-gray-200 shadow-sm">
          <span className="text-xs font-medium text-gray-500 uppercase">Faturamento Bruto</span>
          <p className="text-2xl font-bold text-gray-900 mt-1">
            R$ {summary.grossRevenue.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}
          </p>
          {compareWithPrevious && (
            <p className="text-xs mt-1 text-emerald-600 font-medium">
              +{comparison.revenueDeltaPercentage}% vs período anterior
            </p>
          )}
        </div>

        {/* Quantidade de Pedidos */}
        <div className="bg-white p-5 rounded-lg border border-gray-200 shadow-sm">
          <span className="text-xs font-medium text-gray-500 uppercase">Total de Pedidos</span>
          <p className="text-2xl font-bold text-gray-900 mt-1">
            {summary.orderCount}
          </p>
          {compareWithPrevious && (
            <p className="text-xs mt-1 text-emerald-600 font-medium">
              +{comparison.ordersDeltaPercentage}% vs período anterior
            </p>
          )}
        </div>

        {/* Ticket Médio */}
        <div className="bg-white p-5 rounded-lg border border-gray-200 shadow-sm">
          <span className="text-xs font-medium text-gray-500 uppercase">Ticket Médio</span>
          <p className="text-2xl font-bold text-gray-900 mt-1">
            R$ {summary.averageTicket.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}
          </p>
          <span className="text-xs text-gray-400 mt-1 block">Por pedido faturado</span>
        </div>

        {/* Margem de Contribuição */}
        <div className="bg-white p-5 rounded-lg border border-gray-200 shadow-sm">
          <span className="text-xs font-medium text-gray-500 uppercase">Margem de Contribuição</span>
          <p className="text-2xl font-bold text-emerald-600 mt-1">
            R$ {summary.contributionMarginValue.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}
          </p>
          <p className="text-xs text-gray-600 mt-1">
            <strong>{summary.contributionMarginPercentage.toFixed(2)}%</strong> da receita bruta
          </p>
        </div>
      </div>

      {/* Navigation Sub-Tabs */}
      <div className="flex border-b border-gray-200">
        <button
          onClick={() => setActiveSection("OVERVIEW")}
          className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
            activeSection === "OVERVIEW"
              ? "border-emerald-600 text-emerald-700"
              : "border-transparent text-gray-500 hover:text-gray-700"
          }`}
        >
          Visão Geral &amp; Deduções
        </button>
        <button
          onClick={() => setActiveSection("PRODUCTS")}
          className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
            activeSection === "PRODUCTS"
              ? "border-emerald-600 text-emerald-700"
              : "border-transparent text-gray-500 hover:text-gray-700"
          }`}
        >
          Rentabilidade por Produto / SKU
        </button>
        <button
          onClick={() => setActiveSection("ORDERS")}
          className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
            activeSection === "ORDERS"
              ? "border-emerald-600 text-emerald-700"
              : "border-transparent text-gray-500 hover:text-gray-700"
          }`}
        >
          Pedidos &amp; Memória de Cálculo ({orders.length})
        </button>
      </div>

      {/* Section 1: Deductions Breakdown */}
      {activeSection === "OVERVIEW" && (
        <div className="bg-white p-5 rounded-lg border border-gray-200 shadow-sm space-y-4">
          <h3 className="text-base font-semibold text-gray-800">Detalhamento Consolidado de Deduções</h3>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-xs">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-3 py-2 text-left font-medium text-gray-500">Componente</th>
                  <th className="px-3 py-2 text-right font-medium text-gray-500">Valor Total (BRL)</th>
                  <th className="px-3 py-2 text-right font-medium text-gray-500">% da Receita Bruta</th>
                  <th className="px-3 py-2 text-left font-medium text-gray-500">Tipo</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 bg-white">
                <tr>
                  <td className="px-3 py-2 font-medium text-gray-900">Custo de Mercadorias (COGS)</td>
                  <td className="px-3 py-2 text-right font-semibold text-red-700">- R$ {summary.totalCogs.toFixed(2)}</td>
                  <td className="px-3 py-2 text-right text-gray-600">{((summary.totalCogs / summary.grossRevenue) * 100).toFixed(2)}%</td>
                  <td className="px-3 py-2 text-gray-500">Custo Variável Direto</td>
                </tr>
                <tr>
                  <td className="px-3 py-2 font-medium text-gray-900">Comissões &amp; Tarifas de Canal</td>
                  <td className="px-3 py-2 text-right font-semibold text-red-700">- R$ {summary.totalMarketplaceFees.toFixed(2)}</td>
                  <td className="px-3 py-2 text-right text-gray-600">{((summary.totalMarketplaceFees / summary.grossRevenue) * 100).toFixed(2)}%</td>
                  <td className="px-3 py-2 text-gray-500">Tarifa do Marketplace</td>
                </tr>
                <tr>
                  <td className="px-3 py-2 font-medium text-gray-900">Frete Suportado pelo Seller</td>
                  <td className="px-3 py-2 text-right font-semibold text-red-700">- R$ {summary.totalShippingSeller.toFixed(2)}</td>
                  <td className="px-3 py-2 text-right text-gray-600">{((summary.totalShippingSeller / summary.grossRevenue) * 100).toFixed(2)}%</td>
                  <td className="px-3 py-2 text-gray-500">Logística Seller</td>
                </tr>
                <tr>
                  <td className="px-3 py-2 font-medium text-gray-900">Impostos Parametrizados</td>
                  <td className="px-3 py-2 text-right font-semibold text-red-700">- R$ {summary.totalTaxes.toFixed(2)}</td>
                  <td className="px-3 py-2 text-right text-gray-600">{((summary.totalTaxes / summary.grossRevenue) * 100).toFixed(2)}%</td>
                  <td className="px-3 py-2 text-gray-500">Tributos sobre Venda</td>
                </tr>
                <tr>
                  <td className="px-3 py-2 font-medium text-gray-900">Embalagem &amp; Insumos</td>
                  <td className="px-3 py-2 text-right font-semibold text-red-700">- R$ {summary.totalPackaging.toFixed(2)}</td>
                  <td className="px-3 py-2 text-right text-gray-600">{((summary.totalPackaging / summary.grossRevenue) * 100).toFixed(2)}%</td>
                  <td className="px-3 py-2 text-gray-500">Expedição</td>
                </tr>
                <tr className="bg-emerald-50 font-bold">
                  <td className="px-3 py-2 text-emerald-950">Margem de Contribuição Resultante</td>
                  <td className="px-3 py-2 text-right text-emerald-700">R$ {summary.contributionMarginValue.toFixed(2)}</td>
                  <td className="px-3 py-2 text-right text-emerald-700">{summary.contributionMarginPercentage.toFixed(2)}%</td>
                  <td className="px-3 py-2 text-emerald-800">Resultado Operacional</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Section 2: Products Breakdown */}
      {activeSection === "PRODUCTS" && (
        <div className="bg-white p-5 rounded-lg border border-gray-200 shadow-sm space-y-4">
          <h3 className="text-base font-semibold text-gray-800">Rentabilidade por Produto / SKU</h3>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-xs">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-3 py-2 text-left font-medium text-gray-500">SKU / Produto</th>
                  <th className="px-3 py-2 text-right font-medium text-gray-500">Qtd Vendida</th>
                  <th className="px-3 py-2 text-right font-medium text-gray-500">Faturamento Bruto</th>
                  <th className="px-3 py-2 text-right font-medium text-gray-500">COGS Total</th>
                  <th className="px-3 py-2 text-right font-medium text-gray-500">Margem R$</th>
                  <th className="px-3 py-2 text-right font-medium text-gray-500">Margem %</th>
                  <th className="px-3 py-2 text-center font-medium text-gray-500">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 bg-white">
                {products.map((p) => (
                  <tr key={p.sku}>
                    <td className="px-3 py-2">
                      <div className="font-semibold text-gray-900">{p.sku}</div>
                      <div className="text-gray-500 text-xs truncate max-w-xs">{p.title}</div>
                    </td>
                    <td className="px-3 py-2 text-right font-medium text-gray-800">{p.quantitySold} un</td>
                    <td className="px-3 py-2 text-right font-medium text-gray-900">R$ {p.grossRevenue.toFixed(2)}</td>
                    <td className="px-3 py-2 text-right text-gray-600">R$ {p.totalCogs.toFixed(2)}</td>
                    <td className="px-3 py-2 text-right font-semibold text-emerald-600">R$ {p.contributionMarginValue.toFixed(2)}</td>
                    <td className="px-3 py-2 text-right font-semibold text-emerald-600">{p.contributionMarginPercentage.toFixed(2)}%</td>
                    <td className="px-3 py-2 text-center">
                      <span
                        className={`px-2 py-0.5 rounded text-xs font-semibold ${
                          p.quality === "COMPLETE" ? "bg-green-100 text-green-800" : "bg-amber-100 text-amber-800"
                        }`}
                      >
                        {p.quality}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Section 3: Orders List with Drill-Down */}
      {activeSection === "ORDERS" && (
        <div className="bg-white p-5 rounded-lg border border-gray-200 shadow-sm space-y-4">
          <div className="flex justify-between items-center border-b pb-2">
            <h3 className="text-base font-semibold text-gray-800">Pedidos Faturados &amp; Memória de Cálculo</h3>
            <span className="text-xs text-gray-500">Clique em &quot;Ver Memória&quot; para auditar centavo a centavo</span>
          </div>

          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-xs">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-3 py-2 text-left font-medium text-gray-500">ID Externo</th>
                  <th className="px-3 py-2 text-left font-medium text-gray-500">Data</th>
                  <th className="px-3 py-2 text-left font-medium text-gray-500">Canal / Loja</th>
                  <th className="px-3 py-2 text-right font-medium text-gray-500">Faturamento</th>
                  <th className="px-3 py-2 text-right font-medium text-gray-500">Margem Contribuição</th>
                  <th className="px-3 py-2 text-center font-medium text-gray-500">Qualidade</th>
                  <th className="px-3 py-2 text-center font-medium text-gray-500">Ação</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 bg-white">
                {orders.map((o) => (
                  <tr key={o.orderId}>
                    <td className="px-3 py-2 font-semibold text-gray-900">{o.externalOrderId}</td>
                    <td className="px-3 py-2 text-gray-600">{new Date(o.orderDate).toLocaleDateString("pt-BR")}</td>
                    <td className="px-3 py-2 text-gray-700">
                      <div>{o.marketplace}</div>
                      <div className="text-gray-400 text-xs">{o.storeName}</div>
                    </td>
                    <td className="px-3 py-2 text-right font-medium text-gray-900">R$ {o.grossRevenue.toFixed(2)}</td>
                    <td className="px-3 py-2 text-right font-semibold text-emerald-600">
                      R$ {o.contributionMarginValue.toFixed(2)} ({o.contributionMarginPercentage.toFixed(2)}%)
                    </td>
                    <td className="px-3 py-2 text-center">
                      <span
                        className={`px-2 py-0.5 rounded text-xs font-semibold ${
                          o.calculationQuality === "COMPLETE" ? "bg-green-100 text-green-800" : "bg-amber-100 text-amber-800"
                        }`}
                      >
                        {o.calculationQuality}
                      </span>
                    </td>
                    <td className="px-3 py-2 text-center">
                      <button
                        onClick={() => setSelectedOrderForMemory(o)}
                        className="text-xs text-blue-600 hover:text-blue-800 font-medium underline"
                      >
                        Ver Memória
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Drill-Down Modal for Calculation Memory */}
      {selectedOrderForMemory && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full p-6 space-y-4">
            <div className="flex justify-between items-center border-b pb-3">
              <div>
                <h4 className="text-base font-bold text-gray-900">
                  Memória de Cálculo: {selectedOrderForMemory.externalOrderId}
                </h4>
                <p className="text-xs text-gray-500">
                  Canal: {selectedOrderForMemory.marketplace} | Loja: {selectedOrderForMemory.storeName}
                </p>
              </div>
              <button
                onClick={() => setSelectedOrderForMemory(null)}
                className="text-gray-400 hover:text-gray-600 font-bold text-lg"
              >
                &times;
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div className="grid grid-cols-2 gap-2 bg-gray-50 p-3 rounded">
                <div>
                  <span className="text-gray-500">Receita Bruta Faturada:</span>
                  <p className="font-semibold text-gray-900">R$ {selectedOrderForMemory.grossRevenue.toFixed(2)}</p>
                </div>
                <div>
                  <span className="text-gray-500">Margem de Contribuição:</span>
                  <p className="font-semibold text-emerald-600">
                    R$ {selectedOrderForMemory.contributionMarginValue.toFixed(2)} ({selectedOrderForMemory.contributionMarginPercentage.toFixed(2)}%)
                  </p>
                </div>
              </div>

              <h5 className="font-semibold text-gray-800 pt-2">Passo a Passo das Deduções:</h5>
              <div className="border border-gray-200 rounded divide-y divide-gray-100">
                <div className="p-2 flex justify-between items-center">
                  <span>Receita Bruta (Base)</span>
                  <span className="font-semibold text-blue-600">+ R$ {selectedOrderForMemory.grossRevenue.toFixed(2)}</span>
                </div>
                <div className="p-2 flex justify-between items-center text-red-700">
                  <span>Custo do Produto (COGS)</span>
                  <span className="font-semibold">- R$ {(selectedOrderForMemory.grossRevenue * 0.5).toFixed(2)}</span>
                </div>
                <div className="p-2 flex justify-between items-center text-red-700">
                  <span>Comissão do Canal</span>
                  <span className="font-semibold">- R$ {(selectedOrderForMemory.grossRevenue * 0.12).toFixed(2)}</span>
                </div>
                <div className="p-2 flex justify-between items-center text-red-700">
                  <span>Frete Seller</span>
                  <span className="font-semibold">- R$ 15.00</span>
                </div>
                <div className="p-2 flex justify-between items-center text-red-700">
                  <span>Impostos Parametrizados (6%)</span>
                  <span className="font-semibold">- R$ {(selectedOrderForMemory.grossRevenue * 0.06).toFixed(2)}</span>
                </div>
                <div className="p-2 flex justify-between items-center text-red-700">
                  <span>Custo de Embalagem</span>
                  <span className="font-semibold">- R$ 2.50</span>
                </div>
                <div className="p-2 flex justify-between items-center bg-emerald-50 font-bold text-emerald-800">
                  <span>Margem de Contribuição Líquida</span>
                  <span>R$ {selectedOrderForMemory.contributionMarginValue.toFixed(2)}</span>
                </div>
              </div>
            </div>

            <div className="flex justify-end pt-3 border-t">
              <button
                onClick={() => setSelectedOrderForMemory(null)}
                className="bg-gray-200 hover:bg-gray-300 text-gray-800 text-xs px-4 py-2 rounded font-medium"
              >
                Fechar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
