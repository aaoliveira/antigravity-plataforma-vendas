"use client";

import React, { useState } from "react";

export interface CriterionItem {
  id: string;
  name: string;
  weight: number;
  pointsEarned: number;
  maxPoints: number;
  status: "GOOD" | "NEEDS_IMPROVEMENT" | "CRITICAL";
  evidence: string;
  recommendation: string;
}

export interface QualityAudit {
  listingId: string;
  overallScore: number;
  titleScore: number;
  descriptionScore: number;
  attributesScore: number;
  imagesScore: number;
  criteriaBreakdown: CriterionItem[];
  actionableRecommendations: string[];
  missingRequiredAttributes: string[];
  auditedAt: string;
}

export interface ListingItem {
  id: string;
  title: string;
  sku: string;
  marketplace: string;
  price: number;
  brand: string;
  category: string;
  status: string;
  currentDescription: string;
  attributes: Record<string, string>;
}

export interface VersionItem {
  id: string;
  versionNumber: number;
  title: string;
  description: string;
  status: "DRAFT" | "ACCEPTED" | "REJECTED";
  source: string;
  modelName: string;
  promptVersion: string;
  createdAt: string;
}

const SAMPLE_LISTINGS: ListingItem[] = [
  {
    id: "lst-101",
    title: "Monitor Gamer Dell 27 Polegadas 4K Ultra HD 144Hz IPS",
    sku: "SKU-DELL-G2724D",
    marketplace: "MERCADO_LIVRE",
    price: 2499.0,
    brand: "Dell",
    category: "Monitores",
    status: "ACTIVE",
    currentDescription: "Monitor gamer Dell com tela de 27 polegadas e taxa de atualização rápida.",
    attributes: {
      marca: "Dell",
      modelo: "G2724D",
      resolucao: "4K Ultra HD",
      taxa_atualizacao: "144Hz",
      garantia: "36 meses",
    },
  },
  {
    id: "lst-102",
    title: "Mouse Gamer Super Promoção Frete Grátis Imperdível!!!",
    sku: "SKU-MOUSE-RGB",
    marketplace: "SHOPEE",
    price: 89.9,
    brand: "Genérico",
    category: "Periféricos",
    status: "ACTIVE",
    currentDescription: "Mouse com luz rgb legal para jogos.",
    attributes: {
      marca: "Genérico",
    },
  },
];

export function ListingStudioView() {
  const [listings] = useState<ListingItem[]>(SAMPLE_LISTINGS);
  const [selectedListingId, setSelectedListingId] = useState<string>(SAMPLE_LISTINGS[0].id);

  const currentListing = listings.find((l) => l.id === selectedListingId) || listings[0];

  // Audit state
  const [audit, setAudit] = useState<QualityAudit>({
    listingId: currentListing.id,
    overallScore: 78,
    titleScore: 25,
    descriptionScore: 18,
    attributesScore: 24,
    imagesScore: 11,
    criteriaBreakdown: [
      {
        id: "TITLE_LENGTH",
        name: "Comprimento do Título",
        weight: 10,
        pointsEarned: 10,
        maxPoints: 10,
        status: "GOOD",
        evidence: "54 caracteres (faixa ideal: 40 a 60).",
        recommendation: "Manter nessa extensão para exibição ideal em mobile.",
      },
      {
        id: "TITLE_NO_SPAM",
        name: "Ausência de Termos Proibidos",
        weight: 5,
        pointsEarned: 5,
        maxPoints: 5,
        status: "GOOD",
        evidence: "Sem termos promocionais ou pontuação excessiva.",
        recommendation: "Em conformidade com diretrizes dos marketplaces.",
      },
      {
        id: "TECH_ATTRIBUTES",
        name: "Ficha Técnica e Atributos",
        weight: 30,
        pointsEarned: 24,
        maxPoints: 30,
        status: "NEEDS_IMPROVEMENT",
        evidence: "4 de 5 atributos preenchidos. Faltam: Voltagem.",
        recommendation: "Preencha a voltagem para melhorar a filtragem.",
      },
      {
        id: "LISTING_IMAGES",
        name: "Galeria de Fotos",
        weight: 20,
        pointsEarned: 11,
        maxPoints: 20,
        status: "NEEDS_IMPROVEMENT",
        evidence: "Apenas 2 fotos cadastradas.",
        recommendation: "Recomendado adicionar ao menos 4 a 6 fotos em alta resolução.",
      },
    ],
    actionableRecommendations: [
      "Adicione ao menos mais 3 fotos detalhadas com fundo branco.",
      "Preencha a especificação de voltagem (Bivolt/110V/220V) na ficha técnica.",
      "Estruture a descrição em tópicos detalhados de apresentação e itens inclusos.",
    ],
    missingRequiredAttributes: ["Voltagem (Bivolt/110V/220V)", "Condição do Produto"],
    auditedAt: "17/11/2026 14:40",
  });

  // AI suggestions state
  const [isGenerating, setIsGenerating] = useState<boolean>(false);
  const [generationType, setGenerationType] = useState<"TITLE" | "DESCRIPTION">("TITLE");
  const [aiTitleSuggestion, setAiTitleSuggestion] = useState<string | null>(null);
  const [aiDescSuggestion, setAiDescSuggestion] = useState<string | null>(null);
  const [generationRationale, setGenerationRationale] = useState<string | null>(null);
  const [confirmedUsed, setConfirmedUsed] = useState<Record<string, string>>({
    marca: currentListing.brand,
    modelo: "G2724D",
    resolução: "4K Ultra HD",
  });
  const [missingIdentified, setMissingIdentified] = useState<string[]>([
    "Voltagem (Bivolt/110V/220V)",
  ]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Editor and versions state
  const [editedTitle, setEditedTitle] = useState<string>(currentListing.title);
  const [editedDescription, setEditedDescription] = useState<string>(currentListing.currentDescription);
  const [versions, setVersions] = useState<VersionItem[]>([
    {
      id: "v-1",
      versionNumber: 1,
      title: "Monitor Gamer Dell 27 4K Ultra HD 144Hz IPS G2724D",
      description: "Descrição original importada do Mercado Livre.",
      status: "DRAFT",
      source: "AI_GENERATED",
      modelName: "fake-model-v1",
      promptVersion: "v1.0.0",
      createdAt: "17/11/2026, 14:15",
    },
  ]);
  const [successNotice, setSuccessNotice] = useState<string | null>(null);

  const handleListingChange = (id: string) => {
    setSelectedListingId(id);
    const chosen = listings.find((l) => l.id === id);
    if (chosen) {
      setEditedTitle(chosen.title);
      setEditedDescription(chosen.currentDescription);
      setAiTitleSuggestion(null);
      setAiDescSuggestion(null);
      setErrorMessage(null);
      setSuccessNotice(null);
    }
  };

  const handleGenerateTitle = () => {
    setIsGenerating(true);
    setErrorMessage(null);
    setSuccessNotice(null);

    setTimeout(() => {
      setIsGenerating(false);
      setGenerationType("TITLE");
      const suggestion = `${currentListing.category} ${currentListing.brand} 27 Pol 4K 144Hz IPS`;
      setAiTitleSuggestion(suggestion);
      setEditedTitle(suggestion);
      setGenerationRationale(
        "Título otimizado ordenando Tipo de Produto + Marca + Resolução + Taxa de Atualização, sem termos promocionais proibidos."
      );
      setConfirmedUsed({
        marca: currentListing.brand,
        categoria: currentListing.category,
        tamanho: "27 Pol",
      });
      setMissingIdentified(["Voltagem"]);
    }, 400);
  };

  const handleGenerateDescription = () => {
    setIsGenerating(true);
    setErrorMessage(null);
    setSuccessNotice(null);

    setTimeout(() => {
      setIsGenerating(false);
      setGenerationType("DESCRIPTION");
      const desc = `## Apresentação do Produto\nO ${currentListing.title} entrega máxima nitidez visual e tempo de resposta ideal para produtividade e jogos.\n\n## Ficha Técnica Confirmada\n- Marca: ${currentListing.brand}\n- Categoria: ${currentListing.category}\n- Resolução: 4K Ultra HD\n- Garantia: 36 meses\n\n## Informações Importantes\n- Produto original com Nota Fiscal e procedência garantida.`;
      setAiDescSuggestion(desc);
      setEditedDescription(desc);
      setGenerationRationale(
        "Descrição estruturada em seções limpas, utilizando exclusivamente atributos confirmados no cadastro."
      );
      setConfirmedUsed({
        marca: currentListing.brand,
        categoria: currentListing.category,
      });
      setMissingIdentified(["Voltagem"]);
    }, 400);
  };

  const handleSimulateError = () => {
    setIsGenerating(true);
    setErrorMessage(null);
    setTimeout(() => {
      setIsGenerating(false);
      setErrorMessage(
        "Serviço de IA temporariamente indisponível. A plataforma não inventa textos de fallback sem confirmação oficial."
      );
    }, 300);
  };

  const handleSaveDraftVersion = () => {
    const nextVer = versions.length + 1;
    const newVersion: VersionItem = {
      id: `v-${Date.now()}`,
      versionNumber: nextVer,
      title: editedTitle,
      description: editedDescription,
      status: "DRAFT",
      source: "MANUAL_EDIT",
      modelName: "user-review",
      promptVersion: "v1.0.0",
      createdAt: new Date().toLocaleString("pt-BR"),
    };
    setVersions([newVersion, ...versions]);
    setSuccessNotice(`Rascunho da Versão ${nextVer} salvo com sucesso! O anúncio no marketplace não foi alterado.`);
  };

  const handleAcceptVersion = (versionId: string) => {
    setVersions((prev) =>
      prev.map((v) => (v.id === versionId ? { ...v, status: "ACCEPTED" } : v))
    );
    setSuccessNotice("Versão marcada como ACEITA para revisão final.");
  };

  return (
    <div className="space-y-6">
      {/* Header and Listing Picker */}
      <div className="flex flex-col md:flex-row md:items-center justify-between pb-4 border-b border-gray-200 gap-4">
        <div>
          <h2 className="text-xl font-bold text-gray-900">AI Listing Studio — Análise &amp; Otimização</h2>
          <p className="text-sm text-gray-500">
            Análise auditável de qualidade, geração assistida e gestão de rascunhos sem publicação automática no marketplace.
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <label htmlFor="select-listing" className="text-xs font-medium text-gray-700">
            Anúncio Selecionado:
          </label>
          <select
            id="select-listing"
            aria-label="Anúncio Selecionado"
            value={selectedListingId}
            onChange={(e) => handleListingChange(e.target.value)}
            className="border border-gray-300 rounded px-3 py-1.5 text-sm bg-white text-gray-800 focus:ring-purple-500 focus:border-purple-500"
          >
            {listings.map((l) => (
              <option key={l.id} value={l.id}>
                [{l.marketplace}] {l.title.substring(0, 40)}...
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Notifications */}
      {errorMessage && (
        <div role="alert" className="bg-red-50 border-l-4 border-red-500 p-4 rounded text-sm text-red-800">
          <strong>Falha de Provedor:</strong> {errorMessage}
        </div>
      )}
      {successNotice && (
        <div role="status" className="bg-emerald-50 border-l-4 border-emerald-500 p-4 rounded text-sm text-emerald-800">
          {successNotice}
        </div>
      )}

      {/* Quality Audit Scorecard */}
      <div className="bg-white border border-gray-200 rounded-lg p-5 shadow-sm space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-gray-100 pb-3">
          <div>
            <h3 className="text-base font-bold text-gray-900">Score de Qualidade do Anúncio</h3>
            <p className="text-xs text-gray-500">Metodologia auditável baseada em 4 pilares: Título, Descrição, Ficha Técnica e Fotos.</p>
          </div>
          <div className="flex items-center space-x-3">
            <span className="text-3xl font-extrabold text-purple-700">{audit.overallScore}</span>
            <span className="text-xs text-gray-500 font-medium">/ 100 pts</span>
            <span
              className={`px-2.5 py-1 rounded text-xs font-bold ${
                audit.overallScore >= 80
                  ? "bg-emerald-100 text-emerald-800"
                  : audit.overallScore >= 60
                  ? "bg-amber-100 text-amber-800"
                  : "bg-red-100 text-red-800"
              }`}
            >
              {audit.overallScore >= 80 ? "Otimizado" : audit.overallScore >= 60 ? "Requer Melhorias" : "Crítico"}
            </span>
          </div>
        </div>

        {/* 4 Pillars Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="bg-slate-50 p-3.5 rounded border border-slate-200">
            <div className="flex justify-between text-xs font-semibold text-slate-700">
              <span>Título</span>
              <span>{audit.titleScore} / 25 pts</span>
            </div>
            <div className="w-full bg-slate-200 h-2 rounded mt-2">
              <div className="bg-purple-600 h-2 rounded" style={{ width: `${(audit.titleScore / 25) * 100}%` }} />
            </div>
          </div>
          <div className="bg-slate-50 p-3.5 rounded border border-slate-200">
            <div className="flex justify-between text-xs font-semibold text-slate-700">
              <span>Descrição</span>
              <span>{audit.descriptionScore} / 25 pts</span>
            </div>
            <div className="w-full bg-slate-200 h-2 rounded mt-2">
              <div className="bg-purple-600 h-2 rounded" style={{ width: `${(audit.descriptionScore / 25) * 100}%` }} />
            </div>
          </div>
          <div className="bg-slate-50 p-3.5 rounded border border-slate-200">
            <div className="flex justify-between text-xs font-semibold text-slate-700">
              <span>Ficha Técnica</span>
              <span>{audit.attributesScore} / 30 pts</span>
            </div>
            <div className="w-full bg-slate-200 h-2 rounded mt-2">
              <div className="bg-purple-600 h-2 rounded" style={{ width: `${(audit.attributesScore / 30) * 100}%` }} />
            </div>
          </div>
          <div className="bg-slate-50 p-3.5 rounded border border-slate-200">
            <div className="flex justify-between text-xs font-semibold text-slate-700">
              <span>Fotos / Galeria</span>
              <span>{audit.imagesScore} / 20 pts</span>
            </div>
            <div className="w-full bg-slate-200 h-2 rounded mt-2">
              <div className="bg-purple-600 h-2 rounded" style={{ width: `${(audit.imagesScore / 20) * 100}%` }} />
            </div>
          </div>
        </div>

        {/* Information Gaps Banner */}
        {audit.missingRequiredAttributes.length > 0 && (
          <div className="bg-amber-50 border border-amber-200 rounded p-3 text-xs text-amber-900 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <div>
              <strong>Lacunas Identificadas no Cadastro:</strong> Os atributos{" "}
              <span className="font-semibold text-amber-950">{audit.missingRequiredAttributes.join(", ")}</span> estão
              ausentes. A IA não inventará esses dados e solicitará sua confirmação.
            </div>
            <span className="px-2 py-0.5 rounded bg-amber-200 text-amber-900 font-semibold text-[11px] self-start sm:self-auto">
              Factual Guardrail Ativo
            </span>
          </div>
        )}
      </div>

      {/* AI Assistance Studio & Actions */}
      <div className="bg-white border border-gray-200 rounded-lg p-5 shadow-sm space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-gray-100 gap-3">
          <div>
            <h3 className="text-base font-bold text-gray-900">Geração Assistida de Conteúdo</h3>
            <p className="text-xs text-gray-500">
              Prompts versionados (v1.0.0), modelo protegido contra prompt injection e saídas baseadas estritamente em fatos.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              onClick={handleGenerateTitle}
              disabled={isGenerating}
              className="bg-purple-600 hover:bg-purple-700 text-white text-xs font-semibold px-3 py-1.5 rounded transition-colors disabled:opacity-50"
            >
              {isGenerating && generationType === "TITLE" ? "Gerando Título..." : "Otimizar Título com IA"}
            </button>
            <button
              onClick={handleGenerateDescription}
              disabled={isGenerating}
              className="bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold px-3 py-1.5 rounded transition-colors disabled:opacity-50"
            >
              {isGenerating && generationType === "DESCRIPTION" ? "Gerando Descrição..." : "Gerar Descrição Técnica"}
            </button>
            <button
              onClick={handleSimulateError}
              disabled={isGenerating}
              className="bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-medium px-2.5 py-1.5 rounded border border-gray-300 transition-colors"
            >
              Simular Falha de Provedor
            </button>
          </div>
        </div>

        {/* AI Suggestion Card */}
        {(aiTitleSuggestion || aiDescSuggestion) && (
          <div className="bg-purple-50 border border-purple-200 rounded-lg p-4 space-y-3">
            <div className="flex items-center justify-between">
              <span className="px-2 py-0.5 rounded text-xs font-bold bg-purple-200 text-purple-900">
                SUGESTÃO IA (Prompt v1.0.0)
              </span>
              <span className="text-[11px] text-purple-700">Modelo: fake-model-v1 | Latência: 45ms</span>
            </div>

            {aiTitleSuggestion && (
              <div>
                <span className="text-xs font-medium text-gray-600">Título Sugerido:</span>
                <p className="text-sm font-bold text-gray-900 mt-0.5">{aiTitleSuggestion}</p>
              </div>
            )}

            {aiDescSuggestion && (
              <div>
                <span className="text-xs font-medium text-gray-600">Descrição Estruturada:</span>
                <pre className="text-xs font-mono bg-white p-3 rounded border border-purple-100 text-gray-800 whitespace-pre-wrap mt-0.5">
                  {aiDescSuggestion}
                </pre>
              </div>
            )}

            {generationRationale && (
              <p className="text-xs text-purple-950 bg-purple-100/70 p-2.5 rounded">
                <strong>Justificativa da IA:</strong> {generationRationale}
              </p>
            )}

            <div className="text-xs text-gray-600 pt-2 border-t border-purple-200/60 flex items-center justify-between">
              <span>
                <strong>Atributos Confirmados Utilizados:</strong>{" "}
                {Object.entries(confirmedUsed)
                  .map(([k, v]) => `${k}=${v}`)
                  .join(", ")}
              </span>
              <span className="text-[11px] text-gray-500 italic">
                Aviso: A sugestão deve ser revisada por humano antes de publicação.
              </span>
            </div>
          </div>
        )}
      </div>

      {/* Human Review & Draft Version Editor */}
      <div className="bg-white border border-gray-200 rounded-lg p-5 shadow-sm space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-gray-100 gap-2">
          <div>
            <h3 className="text-base font-bold text-gray-900">Revisão Humana &amp; Editor de Rascunho</h3>
            <p className="text-xs text-gray-500">
              Edite e salve versões como rascunho. O anúncio oficial no marketplace não é alterado pelo editor.
            </p>
          </div>
          <button
            onClick={handleSaveDraftVersion}
            className="bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold px-4 py-2 rounded transition-colors self-start sm:self-auto"
          >
            Salvar Rascunho da Versão
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="space-y-1">
            <label htmlFor="input-title" className="text-xs font-semibold text-gray-700">
              Título do Anúncio (Editável)
            </label>
            <input
              id="input-title"
              type="text"
              value={editedTitle}
              onChange={(e) => setEditedTitle(e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm text-gray-800 focus:ring-emerald-500 focus:border-emerald-500"
            />
            <span className="text-[11px] text-gray-500">{editedTitle.length} caracteres (ideal: 40 a 60)</span>
          </div>

          <div className="space-y-1">
            <label htmlFor="input-description" className="text-xs font-semibold text-gray-700">
              Descrição do Anúncio (Editável)
            </label>
            <textarea
              id="input-description"
              rows={5}
              value={editedDescription}
              onChange={(e) => setEditedDescription(e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm text-gray-800 focus:ring-emerald-500 focus:border-emerald-500"
            />
          </div>
        </div>
      </div>

      {/* Version History Table */}
      <div className="bg-white border border-gray-200 rounded-lg p-5 shadow-sm space-y-3">
        <h3 className="text-base font-bold text-gray-900">Histórico de Versões Salvas</h3>
        <p className="text-xs text-gray-500">Rastreabilidade completa de autor, modelo e status de aprovação de rascunhos.</p>

        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200 text-xs">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-3 py-2 text-left font-semibold text-gray-600">Versão</th>
                <th className="px-3 py-2 text-left font-semibold text-gray-600">Data</th>
                <th className="px-3 py-2 text-left font-semibold text-gray-600">Título do Rascunho</th>
                <th className="px-3 py-2 text-left font-semibold text-gray-600">Origem</th>
                <th className="px-3 py-2 text-left font-semibold text-gray-600">Status</th>
                <th className="px-3 py-2 text-right font-semibold text-gray-600">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {versions.map((v) => (
                <tr key={v.id} className="hover:bg-gray-50">
                  <td className="px-3 py-2.5 font-bold text-gray-900">v{v.versionNumber}</td>
                  <td className="px-3 py-2.5 text-gray-600">{v.createdAt}</td>
                  <td className="px-3 py-2.5 text-gray-800 max-w-xs truncate">{v.title}</td>
                  <td className="px-3 py-2.5">
                    <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-gray-100 text-gray-700">
                      {v.source} ({v.modelName})
                    </span>
                  </td>
                  <td className="px-3 py-2.5">
                    <span
                      className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                        v.status === "ACCEPTED"
                          ? "bg-emerald-100 text-emerald-800"
                          : "bg-amber-100 text-amber-800"
                      }`}
                    >
                      {v.status}
                    </span>
                  </td>
                  <td className="px-3 py-2.5 text-right">
                    {v.status !== "ACCEPTED" && (
                      <button
                        onClick={() => handleAcceptVersion(v.id)}
                        className="text-purple-600 hover:text-purple-900 font-medium"
                      >
                        Aceitar Versão
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
