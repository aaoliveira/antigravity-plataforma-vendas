'use client';

import React, { useState } from 'react';
import { FeeResolutionResult } from '@/types/pricing';

export function FeeSimulator() {
  const [marketplace, setMarketplace] = useState<string>('MERCADO_LIVRE');
  const [salePrice, setSalePrice] = useState<string>('120.00');
  const [listingType, setListingType] = useState<string>('gold_pro');
  const [category, setCategory] = useState<string>('');
  const [referenceDate, setReferenceDate] = useState<string>(new Date().toISOString().slice(0, 16));

  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<FeeResolutionResult | null>(null);
  const [errorFeedback, setErrorFeedback] = useState<string | null>(null);

  const handleSimulate = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorFeedback(null);
    setResult(null);

    const price = parseFloat(salePrice);
    if (!salePrice.trim() || isNaN(price) || price <= 0) {
      setErrorFeedback('Informe um preço de venda válido maior que zero.');
      return;
    }

    setIsLoading(true);
    try {
      const payload = {
        marketplace,
        salePrice: price,
        listingType: listingType.trim() || null,
        category: category.trim() || null,
        referenceDate: new Date(referenceDate).toISOString(),
      };

      const res = await fetch('/api/pricing/fee-rules/resolve', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Falha ao resolver taxas para o cenário.');
      }

      const data: FeeResolutionResult = await res.json();
      setResult(data);
    } catch (err: any) {
      setErrorFeedback(err.message || 'Erro de comunicação ao resolver taxas.');
    } finally {
      setIsLoading(false);
    }
  };

  // Checagem se alguma regra aplicada é manual ou desatualizada
  const hasManualRule = result?.resolvedFees.some((f) => f.sourceType === 'MANUAL_OVERRIDE') ?? false;
  const priceNum = parseFloat(salePrice) || 0;
  const netEstimated = result ? Math.max(0, priceNum - result.totalEstimatedFees) : 0;

  return (
    <div className="space-y-8">
      {/* Formulário de Parâmetros da Simulação */}
      <section
        aria-labelledby="simulator-heading"
        className="bg-white dark:bg-slate-900 rounded-xl p-6 border border-slate-200 dark:border-slate-800 shadow-sm"
      >
        <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800">
          <div>
            <h2 id="simulator-heading" className="text-base font-bold text-slate-900 dark:text-white">
              Simulador Determinístico de Taxas e Comissões
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              Calcule com precisão as tarifas aplicáveis a um anúncio utilizando exclusivamente regras ativas e vigentes.
            </p>
          </div>
          <span className="text-xs px-2.5 py-1 bg-purple-50 dark:bg-purple-950/40 text-purple-700 dark:text-purple-300 rounded font-medium border border-purple-200 dark:border-purple-900">
            Resolução Determinística
          </span>
        </div>

        {errorFeedback && (
          <div
            role="alert"
            className="mt-4 p-3.5 bg-rose-50 dark:bg-rose-950/40 text-rose-800 dark:text-rose-300 border border-rose-200 dark:border-rose-900 rounded-lg text-xs font-medium flex items-center gap-2"
          >
            <span>⚠️</span>
            <span>{errorFeedback}</span>
          </div>
        )}

        <form onSubmit={handleSimulate} noValidate className="mt-5 space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
            <div>
              <label htmlFor="sim-marketplace" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Marketplace <span className="text-rose-500">*</span>
              </label>
              <select
                id="sim-marketplace"
                value={marketplace}
                onChange={(e) => setMarketplace(e.target.value)}
                className="w-full text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="MERCADO_LIVRE">Mercado Livre</option>
                <option value="SHOPEE">Shopee</option>
                <option value="AMAZON">Amazon Brasil</option>
                <option value="MAGALU">Magazine Luiza</option>
              </select>
            </div>

            <div>
              <label htmlFor="sim-sale-price" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Preço de Venda do Anúncio (R$) <span className="text-rose-500">*</span>
              </label>
              <input
                id="sim-sale-price"
                type="number"
                step="0.01"
                min="0.01"
                placeholder="100.00"
                value={salePrice}
                onChange={(e) => setSalePrice(e.target.value)}
                className="w-full text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label htmlFor="sim-listing-type" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Modalidade de Anúncio
              </label>
              <input
                id="sim-listing-type"
                type="text"
                placeholder="Ex: gold_pro ou standard"
                value={listingType}
                onChange={(e) => setListingType(e.target.value)}
                className="w-full text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label htmlFor="sim-reference-date" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Data de Referência da Venda <span className="text-rose-500">*</span>
              </label>
              <input
                id="sim-reference-date"
                type="datetime-local"
                value={referenceDate}
                onChange={(e) => setReferenceDate(e.target.value)}
                className="w-full text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          <div className="flex justify-end pt-2">
            <button
              type="submit"
              disabled={isLoading}
              className="px-5 py-2.5 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white text-xs font-bold rounded-lg shadow transition-colors flex items-center justify-center gap-2 min-h-[44px]"
            >
              {isLoading ? (
                <>
                  <span className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  <span>Calculando Taxas...</span>
                </>
              ) : (
                <span>Simular e Resolver Taxas</span>
              )}
            </button>
          </div>
        </form>
      </section>

      {/* Resultados da Simulação */}
      {result && (
        <section
          aria-labelledby="simulation-results-heading"
          className="bg-white dark:bg-slate-900 rounded-xl p-6 border border-slate-200 dark:border-slate-800 shadow-sm space-y-6"
        >
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-4 border-b border-slate-100 dark:border-slate-800">
            <div>
              <h3 id="simulation-results-heading" className="text-sm font-bold text-slate-900 dark:text-white">
                Demonstrativo de Taxas Calculadas &bull; {result.marketplace}
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                Venda simulada de R$ {priceNum.toFixed(2)} em {new Date(result.referenceDate).toLocaleDateString('pt-BR')}.
              </p>
            </div>

            <span
              className={`text-xs px-2.5 py-1 rounded font-bold uppercase tracking-wider ${
                result.status === 'SUCCESS'
                  ? 'bg-emerald-100 dark:bg-emerald-950/60 text-emerald-800 dark:text-emerald-300 border border-emerald-300 dark:border-emerald-800'
                  : result.status === 'PARTIAL'
                  ? 'bg-amber-100 dark:bg-amber-950/60 text-amber-800 dark:text-amber-300 border border-amber-300 dark:border-amber-800'
                  : 'bg-rose-100 dark:bg-rose-950/60 text-rose-800 dark:text-rose-300 border border-rose-300 dark:border-rose-800'
              }`}
            >
              Status: {result.status}
            </span>
          </div>

          {/* ========================================================
              ALERTAS ANTI-ALUCINAÇÃO ESTREITOS
          ======================================================== */}

          {/* 1. Alerta de Taxas Ausentes (Não Inventar Valores) */}
          {result.hasMissingFees && (
            <div
              role="alert"
              className="p-4 bg-amber-50 dark:bg-amber-950/40 border-2 border-amber-400 dark:border-amber-700 rounded-xl text-amber-950 dark:text-amber-200 flex items-start gap-3"
            >
              <span className="text-xl">⚠️</span>
              <div className="space-y-1">
                <p className="text-xs font-bold uppercase tracking-wide text-amber-800 dark:text-amber-300">
                  Regra Anti-Alucinação: Taxas Obrigatórias Ausentes
                </p>
                <p className="text-xs leading-relaxed">
                  Não foram encontradas regras cadastradas para as seguintes cobranças:{' '}
                  <strong>{result.missingFeeTypes.join(', ')}</strong>. A plataforma{' '}
                  <strong className="underline decoration-amber-500">NUNCA assume 0%</strong> nem interpola
                  tabelas de terceiros sem fonte contratual comprovada.
                </p>
                <p className="text-xs font-medium pt-1 text-amber-700 dark:text-amber-400">
                  {result.message || 'Por favor, utilize a aba "Regras de Taxas" para cadastrar a fonte oficial correspondente.'}
                </p>
              </div>
            </div>
          )}

          {/* 2. Alerta de Uso de Regra Manual Não Oficial */}
          {hasManualRule && (
            <div
              role="alert"
              className="p-3.5 bg-yellow-50 dark:bg-yellow-950/30 border border-yellow-300 dark:border-yellow-800/80 rounded-lg text-yellow-900 dark:text-yellow-200 text-xs flex items-center gap-2"
            >
              <span className="text-base">⚠️</span>
              <span>
                <strong>Atenção:</strong> O cálculo utilizou uma ou mais regras de taxas classificadas como{' '}
                <strong className="underline">Entrada Manual (Não Oficial)</strong>. O resultado deve ser tratado como estimativa preliminar.
              </span>
            </div>
          )}

          {/* Cards de Resumo / KPIs */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="p-4 rounded-xl bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700">
              <span className="text-xs text-slate-500 dark:text-slate-400 font-medium">Preço Bruto Informado</span>
              <div className="text-lg font-bold text-slate-900 dark:text-white mt-1">
                R$ {priceNum.toFixed(2)}
              </div>
            </div>

            <div className="p-4 rounded-xl bg-rose-50/50 dark:bg-rose-950/20 border border-rose-200 dark:border-rose-900/50">
              <span className="text-xs text-rose-600 dark:text-rose-400 font-medium">Total de Taxas Estimadas</span>
              <div className="text-lg font-bold text-rose-600 dark:text-rose-400 mt-1">
                - R$ {result.totalEstimatedFees.toFixed(2)}
              </div>
            </div>

            <div className="p-4 rounded-xl bg-emerald-50/50 dark:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-900/50">
              <span className="text-xs text-emerald-600 dark:text-emerald-400 font-medium">Receita Líquida Estimada</span>
              <div className="text-lg font-bold text-emerald-600 dark:text-emerald-400 mt-1">
                R$ {netEstimated.toFixed(2)}
              </div>
            </div>
          </div>

          {/* Tabela de Itens Resolvidos */}
          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-600 dark:text-slate-300 mb-2">
              Composição das Cobranças Identificadas ({result.resolvedFees.length})
            </h4>

            {result.resolvedFees.length === 0 ? (
              <p className="text-xs text-slate-500 italic py-2">
                Nenhuma taxa pôde ser resolvida com os parâmetros informados.
              </p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="border-b border-slate-200 dark:border-slate-800 text-slate-500 dark:text-slate-400">
                      <th className="py-2.5 px-3 font-semibold">Tipo</th>
                      <th className="py-2.5 px-3 font-semibold">Regra / Descrição</th>
                      <th className="py-2.5 px-3 font-semibold">Alíquota / Fixo</th>
                      <th className="py-2.5 px-3 font-semibold">Total Calculado</th>
                      <th className="py-2.5 px-3 font-semibold">Proveniência</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 dark:divide-slate-800 text-slate-800 dark:text-slate-200">
                    {result.resolvedFees.map((fee, idx) => (
                      <tr key={idx} className={fee.sourceType === 'MANUAL_OVERRIDE' ? 'bg-amber-50/30 dark:bg-amber-950/10' : ''}>
                        <td className="py-3 px-3 font-semibold text-slate-900 dark:text-white">
                          {fee.feeType}
                        </td>
                        <td className="py-3 px-3">
                          <span className="font-medium">{fee.name}</span>
                          {fee.notes && <span className="block text-[10px] text-slate-400">{fee.notes}</span>}
                        </td>
                        <td className="py-3 px-3 text-slate-600 dark:text-slate-300">
                          {fee.appliedPercentage !== null && fee.appliedPercentage !== undefined && (
                            <span>{fee.appliedPercentage.toFixed(2)}%</span>
                          )}
                          {fee.appliedFixedAmount !== null && fee.appliedFixedAmount !== undefined && (
                            <span className="ml-1">
                              {fee.appliedPercentage !== null ? '+ ' : ''}R$ {fee.appliedFixedAmount.toFixed(2)}
                            </span>
                          )}
                        </td>
                        <td className="py-3 px-3 font-bold text-rose-600 dark:text-rose-400">
                          R$ {fee.calculatedAmount.toFixed(2)}
                        </td>
                        <td className="py-3 px-3 text-[11px]">
                          {fee.sourceType === 'MANUAL_OVERRIDE' ? (
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded font-bold bg-amber-100 dark:bg-amber-900/60 text-amber-800 dark:text-amber-200">
                              ⚠️ Manual
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded font-medium bg-emerald-100 dark:bg-emerald-900/40 text-emerald-800 dark:text-emerald-300">
                              🛡️ Oficial
                            </span>
                          )}
                          <span className="block text-[10px] text-slate-400 truncate max-w-[150px] mt-0.5" title={fee.sourceReference}>
                            {fee.sourceReference}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </section>
      )}
    </div>
  );
}
