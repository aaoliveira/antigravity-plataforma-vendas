'use client';

import React, { useEffect } from 'react';
import { DraftListingVersionDto } from '@/types/draftlisting';

interface VersionHistoryDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  versionHistory: DraftListingVersionDto[];
  currentVersionNumber: number;
  onSelectVersion: (version: DraftListingVersionDto) => void;
}

export const VersionHistoryDrawer: React.FC<VersionHistoryDrawerProps> = ({
  isOpen,
  onClose,
  versionHistory,
  currentVersionNumber,
  onSelectVersion,
}) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    if (isOpen) {
      window.addEventListener('keydown', handleKeyDown);
    }
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex justify-end bg-slate-900/50 backdrop-blur-sm transition-opacity"
      role="dialog"
      aria-modal="true"
      aria-label="Histórico de Versões do Rascunho"
    >
      <div className="w-full max-w-md bg-white dark:bg-slate-900 h-full shadow-2xl flex flex-col border-l border-slate-200 dark:border-slate-800">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-slate-200 dark:border-slate-800">
          <div>
            <h3 className="text-base font-semibold text-slate-900 dark:text-slate-100">
              Histórico de Versões
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              {versionHistory.length} versões registradas de forma auditável
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800"
            aria-label="Fechar gaveta de histórico"
          >
            ✕
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-4 flex flex-col gap-3">
          {versionHistory.map((ver) => {
            const isCurrent = ver.versionNumber === currentVersionNumber;
            const displayTitle = ver.effectiveTitle;

            return (
              <div
                key={ver.id}
                className={`p-3.5 rounded-xl border text-xs flex flex-col gap-2 transition-colors ${
                  isCurrent
                    ? 'border-indigo-500 bg-indigo-50/60 dark:bg-indigo-950/30 dark:border-indigo-600'
                    : 'border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900/50 hover:border-slate-300 dark:hover:border-slate-700'
                }`}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-slate-900 dark:text-slate-100">
                      Versão {ver.versionNumber}
                    </span>
                    {isCurrent && (
                      <span className="px-1.5 py-0.5 rounded text-[10px] font-semibold bg-indigo-600 text-white">
                        Ativa
                      </span>
                    )}
                    {ver.userEdited ? (
                      <span className="px-1.5 py-0.5 rounded text-[10px] font-medium bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300">
                        Edição Humana
                      </span>
                    ) : (
                      <span className="px-1.5 py-0.5 rounded text-[10px] font-medium bg-blue-100 text-blue-800 dark:bg-blue-950/60 dark:text-blue-300">
                        IA Original
                      </span>
                    )}
                  </div>
                  <span className="text-[10px] text-slate-400">
                    {new Date(ver.createdAt).toLocaleString('pt-BR')}
                  </span>
                </div>

                <div className="text-slate-700 dark:text-slate-300 line-clamp-2 font-medium">
                  {displayTitle}
                </div>

                <div className="flex items-center justify-between text-[11px] text-slate-500 dark:text-slate-400 pt-1 border-t border-slate-200/60 dark:border-slate-800">
                  <span>
                    Título: {ver.titleCharCount}c | Desc: {ver.descriptionCharCount}c
                  </span>
                  <button
                    onClick={() => {
                      onSelectVersion(ver);
                      onClose();
                    }}
                    className="text-indigo-600 dark:text-indigo-400 hover:underline font-semibold"
                  >
                    Visualizar Versão
                  </button>
                </div>

                {ver.reviewNotes && (
                  <p className="text-[11px] text-slate-500 italic bg-white/60 dark:bg-slate-800/60 p-1.5 rounded">
                    &quot;{ver.reviewNotes}&quot;
                  </p>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
