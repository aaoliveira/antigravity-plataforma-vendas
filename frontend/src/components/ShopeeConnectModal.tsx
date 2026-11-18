'use client';

import React, { useEffect } from 'react';

interface ShopeeConnectModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  isInitiating?: boolean;
}

export const ShopeeConnectModal: React.FC<ShopeeConnectModalProps> = ({
  isOpen,
  onClose,
  onConfirm,
  isInitiating = false,
}) => {
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && isOpen && !isInitiating) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, isInitiating, onClose]);

  if (!isOpen) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="shopee-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6"
    >
      {/* Backdrop com desfoque */}
      <div
        onClick={!isInitiating ? onClose : undefined}
        className="fixed inset-0 bg-slate-950/70 backdrop-blur-sm transition-opacity"
        aria-hidden="true"
      />

      {/* Conteúdo do Modal */}
      <div className="relative w-full max-w-lg rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-6 shadow-2xl z-10 space-y-5 animate-in fade-in zoom-in-95 duration-150">
        {/* Cabeçalho */}
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-orange-100 dark:bg-orange-950/60 flex items-center justify-center text-orange-600 dark:text-orange-400 font-bold text-lg border border-orange-300 dark:border-orange-800">
              SHP
            </div>
            <div>
              <h3 id="shopee-modal-title" className="text-lg font-bold text-slate-900 dark:text-white">
                Conectar Loja da Shopee
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Shopee Open Platform OpenAPI v2 &middot; Assinatura HMAC-SHA256
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isInitiating}
            aria-label="Fechar modal"
            className="text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 min-h-[44px] min-w-[44px] flex items-center justify-center disabled:opacity-50"
          >
            ✕
          </button>
        </div>

        {/* Garantia Fundamental de Segurança: SEM SENHA */}
        <div className="p-3.5 rounded-xl bg-blue-50/80 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-900 flex items-start gap-3">
          <svg className="w-5 h-5 text-blue-600 dark:text-blue-400 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
          </svg>
          <div className="text-xs text-blue-900 dark:text-blue-200 space-y-1">
            <strong className="font-semibold block">A sua senha NUNCA é solicitada nesta aplicação</strong>
            <p className="leading-relaxed">
              Você será redirecionado para a página oficial da Shopee Open Platform para autenticar sua loja e conceder autorização com total segurança. Nenhuma credencial primária é manipulada por nós.
            </p>
          </div>
        </div>

        {/* Orientações e Permissões Solicitadas */}
        <div className="space-y-3">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
            Orientações e Permissões Oficiais (Apenas Leitura):
          </span>
          <div className="space-y-2 text-xs">
            <div className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900/40 flex items-start gap-3">
              <span className="text-emerald-600 dark:text-emerald-400 font-bold text-sm mt-0.5">✓</span>
              <div>
                <strong className="text-slate-900 dark:text-slate-100 font-semibold">Loja Ativa na Shopee (Shop Level)</strong>
                <p className="text-slate-600 dark:text-slate-400 mt-0.5 leading-relaxed">
                  Autorização de acesso aos produtos cadastrados (<code className="font-mono bg-slate-200 dark:bg-slate-800 px-1 py-0.5 rounded">v2.product.get_item_list</code>) e pedidos recebidos da sua loja.
                </p>
              </div>
            </div>

            <div className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900/40 flex items-start gap-3">
              <span className="text-emerald-600 dark:text-emerald-400 font-bold text-sm mt-0.5">✓</span>
              <div>
                <strong className="text-slate-900 dark:text-slate-100 font-semibold">Métricas Oficiais de Anúncios</strong>
                <p className="text-slate-600 dark:text-slate-400 mt-0.5 leading-relaxed">
                  Acesso a métricas consolidadas oficiais de desempenho da loja via OpenAPI v2 (<code className="font-mono bg-slate-200 dark:bg-slate-800 px-1 py-0.5 rounded">v2.analytics</code>).
                </p>
              </div>
            </div>

            <div className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900/40 flex items-start gap-3">
              <span className="text-emerald-600 dark:text-emerald-400 font-bold text-sm mt-0.5">✓</span>
              <div>
                <strong className="text-slate-900 dark:text-slate-100 font-semibold">Webhooks Push em Tempo Real</strong>
                <p className="text-slate-600 dark:text-slate-400 mt-0.5 leading-relaxed">
                  Sincronização atômica de status. Caso a autorização seja revogada no painel da Shopee, nossa plataforma invalida a conexão local e expurga os tokens imediatamente.
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Garantia de Não-Escrita */}
        <div className="p-3 rounded-xl bg-amber-50/70 dark:bg-amber-950/30 border border-amber-200/80 dark:border-amber-900/50 flex items-start gap-2.5 text-xs text-amber-900 dark:text-amber-200 leading-relaxed">
          <span className="font-bold text-sm">🛡</span>
          <div>
            <strong>Garantia de Risco Zero:</strong> A permissão de alteração de catálogo/preços (<code className="font-mono bg-amber-100 dark:bg-amber-900/60 px-1 py-0.5 rounded">write</code>) <strong>NÃO</strong> é solicitada. A aplicação opera com exclusividade em modo de análise e monitoramento.
          </div>
        </div>

        {/* Botões de Ação */}
        <div className="flex flex-col-reverse sm:flex-row sm:items-center sm:justify-end gap-2.5 pt-3 border-t border-slate-100 dark:border-slate-800">
          <button
            type="button"
            onClick={onClose}
            disabled={isInitiating}
            className="w-full sm:w-auto px-4 py-2.5 rounded-lg border border-slate-300 dark:border-slate-700 text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 font-medium text-xs sm:text-sm transition-colors min-h-[44px] flex items-center justify-center disabled:opacity-50"
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={isInitiating}
            className="w-full sm:w-auto px-5 py-2.5 rounded-lg bg-orange-600 hover:bg-orange-700 text-white font-semibold text-xs sm:text-sm transition-colors shadow-sm min-h-[44px] flex items-center justify-center gap-2 disabled:opacity-50"
          >
            {isInitiating ? (
              <>
                <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                <span>Iniciando Redirecionamento...</span>
              </>
            ) : (
              <>
                <span>Continuar para Shopee</span>
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                </svg>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
