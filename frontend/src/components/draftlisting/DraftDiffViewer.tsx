'use client';

import React from 'react';

interface DraftDiffViewerProps {
  originalTitle: string;
  reviewedTitle: string;
  originalDescription: string;
  reviewedDescription: string;
}

export const DraftDiffViewer: React.FC<DraftDiffViewerProps> = ({
  originalTitle,
  reviewedTitle,
  originalDescription,
  reviewedDescription,
}) => {
  const isTitleModified = originalTitle.trim() !== reviewedTitle.trim();
  const isDescriptionModified = originalDescription.trim() !== reviewedDescription.trim();

  return (
    <div className="flex flex-col gap-4 border border-slate-200 dark:border-slate-800 rounded-xl p-4 bg-slate-50 dark:bg-slate-900/60">
      <div className="flex items-center justify-between border-b border-slate-200 dark:border-slate-800 pb-2">
        <h4 className="text-sm font-semibold text-slate-900 dark:text-slate-100 flex items-center gap-2">
          <span>Comparação de Conteúdo (Original vs Revisado)</span>
          {isTitleModified || isDescriptionModified ? (
            <span className="px-2 py-0.5 rounded text-[11px] font-medium bg-indigo-100 text-indigo-800 dark:bg-indigo-950/60 dark:text-indigo-300">
              Modificado pelo Operador
            </span>
          ) : (
            <span className="px-2 py-0.5 rounded text-[11px] font-medium bg-slate-200 text-slate-700 dark:bg-slate-800 dark:text-slate-300">
              Idêntico ao Gerado por IA
            </span>
          )}
        </h4>
      </div>

      {/* Título Comparado */}
      <div className="flex flex-col gap-2">
        <span className="text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wide">
          Título do Anúncio
        </span>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
          <div className="p-3 bg-white dark:bg-slate-800/80 rounded-lg border border-slate-200 dark:border-slate-700">
            <div className="flex items-center justify-between mb-1.5 text-slate-500 dark:text-slate-400">
              <span className="font-medium">Original (IA Generativa)</span>
              <span>{originalTitle.length} carac.</span>
            </div>
            <p className="text-slate-800 dark:text-slate-200 font-mono leading-relaxed break-words">
              {originalTitle || <span className="italic text-slate-400">Sem título original</span>}
            </p>
          </div>

          <div className={`p-3 bg-white dark:bg-slate-800/80 rounded-lg border ${
            isTitleModified ? 'border-indigo-400 dark:border-indigo-600' : 'border-slate-200 dark:border-slate-700'
          }`}>
            <div className="flex items-center justify-between mb-1.5 text-slate-500 dark:text-slate-400">
              <span className="font-medium text-indigo-700 dark:text-indigo-400">Revisado (Ativo)</span>
              <span>{reviewedTitle.length} carac.</span>
            </div>
            <p className="text-slate-900 dark:text-slate-100 font-mono leading-relaxed break-words font-semibold">
              {reviewedTitle || <span className="italic text-slate-400">Sem título revisado</span>}
            </p>
          </div>
        </div>
      </div>

      {/* Descrição Comparada */}
      <div className="flex flex-col gap-2">
        <span className="text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wide">
          Descrição Completa
        </span>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
          <div className="p-3 bg-white dark:bg-slate-800/80 rounded-lg border border-slate-200 dark:border-slate-700 max-h-60 overflow-y-auto">
            <div className="flex items-center justify-between mb-1.5 text-slate-500 dark:text-slate-400">
              <span className="font-medium">Original (IA Generativa)</span>
              <span>{originalDescription.length} carac.</span>
            </div>
            <pre className="text-slate-700 dark:text-slate-300 whitespace-pre-wrap font-sans text-xs leading-relaxed">
              {originalDescription || <span className="italic text-slate-400">Sem descrição original</span>}
            </pre>
          </div>

          <div className={`p-3 bg-white dark:bg-slate-800/80 rounded-lg border max-h-60 overflow-y-auto ${
            isDescriptionModified ? 'border-indigo-400 dark:border-indigo-600' : 'border-slate-200 dark:border-slate-700'
          }`}>
            <div className="flex items-center justify-between mb-1.5 text-slate-500 dark:text-slate-400">
              <span className="font-medium text-indigo-700 dark:text-indigo-400">Revisado (Ativo)</span>
              <span>{reviewedDescription.length} carac.</span>
            </div>
            <pre className="text-slate-900 dark:text-slate-100 whitespace-pre-wrap font-sans text-xs leading-relaxed">
              {reviewedDescription || <span className="italic text-slate-400">Sem descrição revisada</span>}
            </pre>
          </div>
        </div>
      </div>
    </div>
  );
};
