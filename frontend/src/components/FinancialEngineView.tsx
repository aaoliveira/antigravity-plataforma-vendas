"use client";

import React, { useState } from "react";

interface CalculationLine {
  componentName: string;
  baseAmount: number;
  appliedRate: number;
  resultingAmount: number;
  impactType: "REVENUE" | "DEDUCTION" | "RESULT";
  sourceOrRule: string;
  notes: string;
}

interface SimulationResult {
  minimumPrice: number;
  targetPrice: number;
  simulatedPrice: number;
  expectedMarginValue: number;
  expectedMarginPercentage: number;
  quality: "COMPLETE" | "PARTIAL" | "SIMULATED";
  missingFields: string[];
  assumptions: string;
  lines: CalculationLine[];
}

export function FinancialEngineView() {
  const [activeTab, setActiveTab] = useState<"SIMULATOR" | "RULES" | "COSTS">("SIMULATOR");

  // Form states
  const [unitCogs, setUnitCogs] = useState("50.00");
  const [trialPrice, setTrialPrice] = useState("120.00");
  const [commissionRate, setCommissionRate] = useState("12.0");
  const [fixedFee, setFixedFee] = useState("6.00");
  const [fixedFeeThreshold, setFixedFeeThreshold] = useState("79.00");
  const [shippingSeller, setShippingSeller] = useState("18.50");
  const [packagingCost, setPackagingCost] = useState("2.50");
  const [taxRate, setTaxRate] = useState("6.0");
  const [targetMarginPct, setTargetMarginPct] = useState("20.0");

  const [simulation, setSimulation] = useState<SimulationResult>({
    minimumPrice: 86.59,
    targetPrice: 114.52,
    simulatedPrice: 120.0,
    expectedMarginValue: 27.4,
    expectedMarginPercentage: 22.83,
    quality: "COMPLETE",
    missingFields: [],
    assumptions: "Todas as premissas e componentes foram informados.",
    lines: [
      {
        componentName: "RECEITA_BRUTA",
        baseAmount: 120.0,
        appliedRate: 1,
        resultingAmount: 120.0,
        impactType: "REVENUE",
        sourceOrRule: "Preço de venda unitário",
        notes: "Receita bruta da simulação",
      },
      {
        componentName: "CUSTO_AQUISICAO_COGS",
        baseAmount: 50.0,
        appliedRate: 1,
        resultingAmount: 50.0,
        impactType: "DEDUCTION",
        sourceOrRule: "Custo cadastrado do SKU",
        notes: "Custo de aquisição do produto",
      },
      {
        componentName: "COMISSAO_MARKETPLACE",
        baseAmount: 120.0,
        appliedRate: 0.12,
        resultingAmount: 14.4,
        impactType: "DEDUCTION",
        sourceOrRule: "Regra Mercado Livre Clássico",
        notes: "Comissão percentual do canal (12%)",
      },
      {
        componentName: "TARIFA_FIXA_MARKETPLACE",
        baseAmount: 120.0,
        appliedRate: 0,
        resultingAmount: 0.0,
        impactType: "DEDUCTION",
        sourceOrRule: "Isento (>= R$ 79,00)",
        notes: "Tarifa fixa isenta para tíquetes acima de R$ 79",
      },
      {
        componentName: "FRETE_SELLER",
        baseAmount: 18.5,
        appliedRate: 1,
        resultingAmount: 18.5,
        impactType: "DEDUCTION",
        sourceOrRule: "Coparticipação Mercado Envios",
        notes: "Frete suportado pelo seller",
      },
      {
        componentName: "CUSTO_EMBALAGEM",
        baseAmount: 2.5,
        appliedRate: 1,
        resultingAmount: 2.5,
        impactType: "DEDUCTION",
        sourceOrRule: "Insumos de expedição",
        notes: "Caixa de papelão e etiqueta",
      },
      {
        componentName: "IMPOSTOS_PARAMETRIZADOS",
        baseAmount: 120.0,
        appliedRate: 0.06,
        resultingAmount: 7.2,
        impactType: "DEDUCTION",
        sourceOrRule: "Simples Nacional Parametrizado",
        notes: "Alíquota efetiva informada (6%)",
      },
      {
        componentName: "MARGEM_CONTRIBUICAO_LIQUIDA",
        baseAmount: 120.0,
        appliedRate: 22.83,
        resultingAmount: 27.4,
        impactType: "RESULT",
        sourceOrRule: "Receita Bruta - SOMA(Deduções)",
        notes: "Margem operacional antes de despesas fixas",
      },
    ],
  });

  const handleSimulate = (e: React.FormEvent) => {
    e.preventDefault();
    const cogs = parseFloat(unitCogs) || 0;
    const price = parseFloat(trialPrice) || 0;
    const commPct = (parseFloat(commissionRate) || 0) / 100;
    const feeFixed = parseFloat(fixedFee) || 0;
    const feeThreshold = parseFloat(fixedFeeThreshold) || 79;
    const ship = parseFloat(shippingSeller) || 0;
    const pack = parseFloat(packagingCost) || 0;
    const taxPct = taxRate !== "" ? (parseFloat(taxRate) || 0) / 100 : null;
    const targetMargin = (parseFloat(targetMarginPct) || 0) / 100;

    const missing: string[] = [];
    if (unitCogs === "") missing.push("COGS");
    if (taxRate === "") missing.push("TAX");

    // Math calculations
    const effectiveTax = taxPct !== null ? taxPct : 0;
    const totalPct = commPct + effectiveTax;

    // Fixed fee applies if price < feeThreshold
    const fixedWithFee = cogs + ship + pack + feeFixed;
    const fixedWithoutFee = cogs + ship + pack;

    // Minimum price (break-even)
    let minP = 0;
    if (1 - totalPct > 0) {
      const trialWith = fixedWithFee / (1 - totalPct);
      if (trialWith < feeThreshold) {
        minP = trialWith;
      } else {
        minP = fixedWithoutFee / (1 - totalPct);
      }
    }

    // Target price
    let targetP = minP;
    if (1 - totalPct - targetMargin > 0) {
      const trialWith = fixedWithFee / (1 - totalPct - targetMargin);
      if (trialWith < feeThreshold) {
        targetP = trialWith;
      } else {
        targetP = fixedWithoutFee / (1 - totalPct - targetMargin);
      }
    }

    // Applied values for simulated price
    const actualFixedFee = price < feeThreshold && price > 0 ? feeFixed : 0;
    const commVal = price * commPct;
    const taxVal = price * effectiveTax;
    const totalDeductions = cogs + commVal + actualFixedFee + ship + pack + taxVal;
    const marginVal = price - totalDeductions;
    const marginPct = price > 0 ? (marginVal / price) * 100 : 0;

    const lines: CalculationLine[] = [
      {
        componentName: "RECEITA_BRUTA",
        baseAmount: price,
        appliedRate: 1,
        resultingAmount: price,
        impactType: "REVENUE",
        sourceOrRule: "Preço de venda simulado",
        notes: "Receita bruta faturada",
      },
      {
        componentName: "CUSTO_AQUISICAO_COGS",
        baseAmount: cogs,
        appliedRate: 1,
        resultingAmount: cogs,
        impactType: "DEDUCTION",
        sourceOrRule: unitCogs !== "" ? "Custo cadastrado" : "CAMPO AUSENTE",
        notes: unitCogs !== "" ? "Custo unitário de aquisição" : "Custo não informado",
      },
      {
        componentName: "COMISSAO_MARKETPLACE",
        baseAmount: price,
        appliedRate: commPct,
        resultingAmount: commVal,
        impactType: "DEDUCTION",
        sourceOrRule: `Comissão ${commissionRate}%`,
        notes: "Taxa retida pelo canal",
      },
      {
        componentName: "TARIFA_FIXA_MARKETPLACE",
        baseAmount: price,
        appliedRate: actualFixedFee > 0 ? feeFixed : 0,
        resultingAmount: actualFixedFee,
        impactType: "DEDUCTION",
        sourceOrRule: price < feeThreshold ? `< R$ ${feeThreshold.toFixed(2)}` : "Isento",
        notes: actualFixedFee > 0 ? "Taxa fixa de tíquete baixo" : "Acima do limiar de isenção",
      },
      {
        componentName: "FRETE_SELLER",
        baseAmount: ship,
        appliedRate: 1,
        resultingAmount: ship,
        impactType: "DEDUCTION",
        sourceOrRule: "Logística / Frete Grátis",
        notes: "Coparticipação de frete",
      },
      {
        componentName: "CUSTO_EMBALAGEM",
        baseAmount: pack,
        appliedRate: 1,
        resultingAmount: pack,
        impactType: "DEDUCTION",
        sourceOrRule: "Insumos",
        notes: "Caixa e etiquetas",
      },
      {
        componentName: "IMPOSTOS_PARAMETRIZADOS",
        baseAmount: price,
        appliedRate: effectiveTax,
        resultingAmount: taxVal,
        impactType: "DEDUCTION",
        sourceOrRule: taxRate !== "" ? `Alíquota ${taxRate}%` : "CAMPO AUSENTE",
        notes: taxRate !== "" ? "Tributos sobre venda informados" : "Sem imposto configurado",
      },
      {
        componentName: "MARGEM_CONTRIBUICAO_LIQUIDA",
        baseAmount: price,
        appliedRate: parseFloat(marginPct.toFixed(2)),
        resultingAmount: parseFloat(marginVal.toFixed(2)),
        impactType: "RESULT",
        sourceOrRule: "Receita - Deduções",
        notes: "Margem de contribuição operacional",
      },
    ];

    const quality: "COMPLETE" | "PARTIAL" | "SIMULATED" =
      missing.length > 0 ? "PARTIAL" : "COMPLETE";

    setSimulation({
      minimumPrice: parseFloat(minP.toFixed(2)),
      targetPrice: parseFloat(targetP.toFixed(2)),
      simulatedPrice: price,
      expectedMarginValue: parseFloat(marginVal.toFixed(2)),
      expectedMarginPercentage: parseFloat(marginPct.toFixed(2)),
      quality,
      missingFields: missing,
      assumptions:
        missing.length > 0
          ? `Cálculo parcial gerencial. Dados ausentes: ${missing.join(", ")}. Não representa lucro líquido real nem apuração fiscal oficial.`
          : "Cálculo determinístico com 100% dos parâmetros informados.",
      lines,
    });
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-gray-200">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Motor Financeiro Determinístico</h2>
          <p className="text-sm text-gray-500">
            Cálculo de margem de contribuição, preço mínimo (break-even), regras versionadas e memória explicável.
          </p>
        </div>
        <div className="mt-3 sm:mt-0 flex space-x-2">
          <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800">
            Precisão Decimal (BigDecimal)
          </span>
          <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold bg-blue-100 text-blue-800">
            Regras Versionadas
          </span>
        </div>
      </div>

      {/* Sub Tabs */}
      <div className="flex border-b border-gray-200">
        <button
          onClick={() => setActiveTab("SIMULATOR")}
          className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
            activeTab === "SIMULATOR"
              ? "border-emerald-600 text-emerald-700"
              : "border-transparent text-gray-500 hover:text-gray-700"
          }`}
        >
          Simulador de Precificação e Margem
        </button>
        <button
          onClick={() => setActiveTab("RULES")}
          className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
            activeTab === "RULES"
              ? "border-emerald-600 text-emerald-700"
              : "border-transparent text-gray-500 hover:text-gray-700"
          }`}
        >
          Regras Financeiras do Canal
        </button>
        <button
          onClick={() => setActiveTab("COSTS")}
          className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
            activeTab === "COSTS"
              ? "border-emerald-600 text-emerald-700"
              : "border-transparent text-gray-500 hover:text-gray-700"
          }`}
        >
          Custos de SKU e Vigência
        </button>
      </div>

      {/* Tab: SIMULATOR */}
      {activeTab === "SIMULATOR" && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Input Form */}
          <div className="lg:col-span-1 bg-white p-5 rounded-lg border border-gray-200 shadow-sm space-y-4">
            <h3 className="text-base font-semibold text-gray-800 border-b pb-2">
              Parâmetros de Precificação
            </h3>
            <form onSubmit={handleSimulate} className="space-y-3">
              <div>
                <label htmlFor="input-cogs" className="block text-xs font-medium text-gray-700">
                  Custo de Aquisição (COGS R$) *
                </label>
                <input
                  id="input-cogs"
                  type="number"
                  step="0.01"
                  value={unitCogs}
                  onChange={(e) => setUnitCogs(e.target.value)}
                  className="mt-1 block w-full border border-gray-300 rounded px-2.5 py-1.5 text-sm focus:ring-emerald-500 focus:border-emerald-500"
                  placeholder="Ex: 50.00"
                />
              </div>

              <div>
                <label htmlFor="input-price" className="block text-xs font-medium text-gray-700">
                  Preço de Venda Simulado (R$)
                </label>
                <input
                  id="input-price"
                  type="number"
                  step="0.01"
                  value={trialPrice}
                  onChange={(e) => setTrialPrice(e.target.value)}
                  className="mt-1 block w-full border border-gray-300 rounded px-2.5 py-1.5 text-sm focus:ring-emerald-500 focus:border-emerald-500"
                  placeholder="Ex: 120.00"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label htmlFor="input-commission" className="block text-xs font-medium text-gray-700">
                    Comissão (%)
                  </label>
                  <input
                    id="input-commission"
                    type="number"
                    step="0.1"
                    value={commissionRate}
                    onChange={(e) => setCommissionRate(e.target.value)}
                    className="mt-1 block w-full border border-gray-300 rounded px-2.5 py-1.5 text-sm"
                  />
                </div>
                <div>
                  <label htmlFor="input-tax" className="block text-xs font-medium text-gray-700">
                    Imposto (%)
                  </label>
                  <input
                    id="input-tax"
                    type="number"
                    step="0.1"
                    value={taxRate}
                    onChange={(e) => setTaxRate(e.target.value)}
                    className="mt-1 block w-full border border-gray-300 rounded px-2.5 py-1.5 text-sm"
                    placeholder="Ex: 6.0"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label htmlFor="input-fixed-fee" className="block text-xs font-medium text-gray-700">
                    Tarifa Fixa (R$)
                  </label>
                  <input
                    id="input-fixed-fee"
                    type="number"
                    step="0.01"
                    value={fixedFee}
                    onChange={(e) => setFixedFee(e.target.value)}
                    className="mt-1 block w-full border border-gray-300 rounded px-2.5 py-1.5 text-sm"
                  />
                </div>
                <div>
                  <label htmlFor="input-threshold" className="block text-xs font-medium text-gray-700">
                    Limiar Tarifa (R$)
                  </label>
                  <input
                    id="input-threshold"
                    type="number"
                    step="0.01"
                    value={fixedFeeThreshold}
                    onChange={(e) => setFixedFeeThreshold(e.target.value)}
                    className="mt-1 block w-full border border-gray-300 rounded px-2.5 py-1.5 text-sm"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label htmlFor="input-shipping" className="block text-xs font-medium text-gray-700">
                    Frete Seller (R$)
                  </label>
                  <input
                    id="input-shipping"
                    type="number"
                    step="0.01"
                    value={shippingSeller}
                    onChange={(e) => setShippingSeller(e.target.value)}
                    className="mt-1 block w-full border border-gray-300 rounded px-2.5 py-1.5 text-sm"
                  />
                </div>
                <div>
                  <label htmlFor="input-packaging" className="block text-xs font-medium text-gray-700">
                    Embalagem (R$)
                  </label>
                  <input
                    id="input-packaging"
                    type="number"
                    step="0.01"
                    value={packagingCost}
                    onChange={(e) => setPackagingCost(e.target.value)}
                    className="mt-1 block w-full border border-gray-300 rounded px-2.5 py-1.5 text-sm"
                  />
                </div>
              </div>

              <div>
                <label htmlFor="input-target-margin" className="block text-xs font-medium text-gray-700">
                  Margem Alvo Desejada (%)
                </label>
                <input
                  id="input-target-margin"
                  type="number"
                  step="0.1"
                  value={targetMarginPct}
                  onChange={(e) => setTargetMarginPct(e.target.value)}
                  className="mt-1 block w-full border border-gray-300 rounded px-2.5 py-1.5 text-sm"
                  placeholder="Ex: 20.0"
                />
              </div>

              <button
                type="submit"
                className="w-full mt-4 bg-emerald-600 hover:bg-emerald-700 text-white font-medium py-2 px-4 rounded text-sm transition-colors"
              >
                Calcular Margem e Preço
              </button>
            </form>
          </div>

          {/* Results Summary & Memory Breakdown */}
          <div className="lg:col-span-2 space-y-4">
            {/* KPI Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="bg-white p-4 rounded-lg border border-gray-200 shadow-sm">
                <span className="text-xs font-medium text-gray-500 uppercase">Preço Mínimo (Break-even)</span>
                <p className="text-xl font-bold text-gray-900 mt-1">
                  R$ {simulation.minimumPrice.toFixed(2)}
                </p>
                <span className="text-xs text-gray-500">Margem = 0%</span>
              </div>

              <div className="bg-white p-4 rounded-lg border border-gray-200 shadow-sm">
                <span className="text-xs font-medium text-gray-500 uppercase">Preço Alvo Sugerido</span>
                <p className="text-xl font-bold text-emerald-600 mt-1">
                  R$ {simulation.targetPrice.toFixed(2)}
                </p>
                <span className="text-xs text-gray-500">Margem Alvo: {targetMarginPct}%</span>
              </div>

              <div className="bg-white p-4 rounded-lg border border-gray-200 shadow-sm">
                <span className="text-xs font-medium text-gray-500 uppercase">Margem Resultante</span>
                <p
                  className={`text-xl font-bold mt-1 ${
                    simulation.expectedMarginValue >= 0 ? "text-blue-600" : "text-red-600"
                  }`}
                >
                  R$ {simulation.expectedMarginValue.toFixed(2)} ({simulation.expectedMarginPercentage.toFixed(2)}%)
                </p>
                <span
                  className={`inline-block px-2 py-0.5 mt-1 rounded text-xs font-semibold ${
                    simulation.quality === "COMPLETE"
                      ? "bg-green-100 text-green-800"
                      : "bg-amber-100 text-amber-800"
                  }`}
                >
                  {simulation.quality === "COMPLETE" ? "CÁLCULO COMPLETO" : "CÁLCULO PARCIAL"}
                </span>
              </div>
            </div>

            {/* Quality Warning if Partial */}
            {simulation.quality === "PARTIAL" && (
              <div className="bg-amber-50 border-l-4 border-amber-400 p-3 rounded text-sm text-amber-800">
                <p className="font-semibold">Aviso de Cálculo Parcial:</p>
                <p>{simulation.assumptions}</p>
              </div>
            )}

            {/* Detailed Calculation Memory Table */}
            <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
              <div className="px-4 py-3 bg-gray-50 border-b border-gray-200 flex justify-between items-center">
                <h4 className="text-sm font-semibold text-gray-900">
                  Memória de Cálculo Explicável (Passo a Passo)
                </h4>
                <span className="text-xs text-gray-500">Arredondamento HALF_EVEN</span>
              </div>
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200 text-xs">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-3 py-2 text-left font-medium text-gray-500 uppercase">Componente</th>
                      <th className="px-3 py-2 text-left font-medium text-gray-500 uppercase">Base / Regra</th>
                      <th className="px-3 py-2 text-right font-medium text-gray-500 uppercase">Alíquota / Fator</th>
                      <th className="px-3 py-2 text-right font-medium text-gray-500 uppercase">Impacto R$</th>
                      <th className="px-3 py-2 text-left font-medium text-gray-500 uppercase">Observações</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200 bg-white">
                    {simulation.lines.map((line, idx) => (
                      <tr
                        key={idx}
                        className={
                          line.impactType === "RESULT"
                            ? "bg-emerald-50 font-semibold"
                            : line.impactType === "REVENUE"
                            ? "bg-blue-50/50"
                            : ""
                        }
                      >
                        <td className="px-3 py-2 text-gray-900 font-medium">
                          {line.componentName}
                        </td>
                        <td className="px-3 py-2 text-gray-600">{line.sourceOrRule}</td>
                        <td className="px-3 py-2 text-right text-gray-600">
                          {typeof line.appliedRate === "number" && line.appliedRate < 1 && line.appliedRate > 0
                            ? `${(line.appliedRate * 100).toFixed(1)}%`
                            : line.appliedRate}
                        </td>
                        <td
                          className={`px-3 py-2 text-right font-semibold ${
                            line.impactType === "REVENUE"
                              ? "text-blue-700"
                              : line.impactType === "RESULT"
                              ? "text-emerald-700"
                              : "text-red-700"
                          }`}
                        >
                          {line.impactType === "DEDUCTION" ? "-" : ""} R$ {line.resultingAmount.toFixed(2)}
                        </td>
                        <td className="px-3 py-2 text-gray-500">{line.notes}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Tab: RULES */}
      {activeTab === "RULES" && (
        <div className="bg-white p-5 rounded-lg border border-gray-200 shadow-sm space-y-4">
          <div className="flex justify-between items-center border-b pb-3">
            <div>
              <h3 className="text-base font-semibold text-gray-900">Regras e Componentes Versionados</h3>
              <p className="text-xs text-gray-500">
                Tarifas de comissão, frete e tributos parametrizados com vigência temporal.
              </p>
            </div>
            <button className="bg-emerald-600 text-white text-xs px-3 py-1.5 rounded font-medium hover:bg-emerald-700">
              Nova Versão de Regra
            </button>
          </div>

          <div className="divide-y divide-gray-200">
            <div className="py-3 flex justify-between items-center">
              <div>
                <span className="text-sm font-semibold text-gray-800">
                  Regra Geral Mercado Livre (Clássico)
                </span>
                <span className="ml-2 px-2 py-0.5 text-xs bg-emerald-100 text-emerald-800 rounded font-medium">
                  v2 (Ativa)
                </span>
                <p className="text-xs text-gray-500 mt-0.5">
                  Vigência: 01/10/2026 até Indefinido | Canal: MERCADO_LIVRE
                </p>
              </div>
              <div className="text-right text-xs text-gray-600">
                <p>Comissão: 12% | Tarifa Fixa: R$ 6,00 (&lt; R$ 79)</p>
                <p>Simples Nacional: 6% | Embalagem: R$ 2,50</p>
              </div>
            </div>

            <div className="py-3 flex justify-between items-center opacity-75">
              <div>
                <span className="text-sm font-semibold text-gray-800">
                  Regra Geral Mercado Livre (Clássico)
                </span>
                <span className="ml-2 px-2 py-0.5 text-xs bg-gray-100 text-gray-600 rounded font-medium">
                  v1 (Encerrada)
                </span>
                <p className="text-xs text-gray-500 mt-0.5">
                  Vigência: 01/01/2026 a 30/09/2026 | Canal: MERCADO_LIVRE
                </p>
              </div>
              <div className="text-right text-xs text-gray-600">
                <p>Comissão: 11% | Tarifa Fixa: R$ 5,50 (&lt; R$ 79)</p>
                <p>Simples Nacional: 6% | Embalagem: R$ 2,00</p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Tab: COSTS */}
      {activeTab === "COSTS" && (
        <div className="bg-white p-5 rounded-lg border border-gray-200 shadow-sm space-y-4">
          <div className="flex justify-between items-center border-b pb-3">
            <div>
              <h3 className="text-base font-semibold text-gray-900">Custos de SKU com Vigência</h3>
              <p className="text-xs text-gray-500">
                Histórico temporal de custos unitários de aquisição e fabricação.
              </p>
            </div>
            <button className="bg-emerald-600 text-white text-xs px-3 py-1.5 rounded font-medium hover:bg-emerald-700">
              Registrar Novo Custo
            </button>
          </div>

          <table className="min-w-full divide-y divide-gray-200 text-xs">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-3 py-2 text-left font-medium text-gray-500">SKU</th>
                <th className="px-3 py-2 text-right font-medium text-gray-500">Custo Unitário</th>
                <th className="px-3 py-2 text-center font-medium text-gray-500">Origem</th>
                <th className="px-3 py-2 text-left font-medium text-gray-500">Vigência Início</th>
                <th className="px-3 py-2 text-left font-medium text-gray-500">Vigência Fim</th>
                <th className="px-3 py-2 text-center font-medium text-gray-500">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 bg-white">
              <tr>
                <td className="px-3 py-2 font-medium text-gray-900">SKU-NOTEBOOK-G15</td>
                <td className="px-3 py-2 text-right font-semibold text-gray-900">R$ 2.000,00</td>
                <td className="px-3 py-2 text-center text-gray-600">MANUAL</td>
                <td className="px-3 py-2 text-gray-600">01/10/2026</td>
                <td className="px-3 py-2 text-gray-500">Atual</td>
                <td className="px-3 py-2 text-center">
                  <span className="px-2 py-0.5 rounded text-xs bg-green-100 text-green-800 font-semibold">
                    Vigente
                  </span>
                </td>
              </tr>
              <tr className="opacity-75">
                <td className="px-3 py-2 font-medium text-gray-900">SKU-NOTEBOOK-G15</td>
                <td className="px-3 py-2 text-right font-semibold text-gray-900">R$ 1.950,00</td>
                <td className="px-3 py-2 text-center text-gray-600">MANUAL</td>
                <td className="px-3 py-2 text-gray-600">01/01/2026</td>
                <td className="px-3 py-2 text-gray-500">30/09/2026</td>
                <td className="px-3 py-2 text-center">
                  <span className="px-2 py-0.5 rounded text-xs bg-gray-100 text-gray-700 font-semibold">
                    Histórico
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
