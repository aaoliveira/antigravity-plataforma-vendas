import React, { useState } from 'react';
import { ChatMessage, StructuredAiResponseDto } from '@/types/aiassistant';
import { AiUsageMetricsPill } from './AiUsageMetricsPill';

interface AiChatMessageProps {
  message: ChatMessage;
  onOpenSources: (meta: StructuredAiResponseDto) => void;
  onFeedback: (messageId: string, type: 'helpful' | 'not_helpful', comment?: string) => void;
}

export const AiChatMessage: React.FC<AiChatMessageProps> = ({
  message,
  onOpenSources,
  onFeedback,
}) => {
  const [feedbackGiven, setFeedbackGiven] = useState<'helpful' | 'not_helpful' | null>(
    message.feedback?.type || null
  );
  const [showCommentBox, setShowCommentBox] = useState(false);
  const [commentText, setCommentText] = useState(message.feedback?.comment || '');

  const handleFeedback = (type: 'helpful' | 'not_helpful') => {
    setFeedbackGiven(type);
    onFeedback(message.id, type, commentText);
  };

  const handleSaveComment = () => {
    if (feedbackGiven) {
      onFeedback(message.id, feedbackGiven, commentText);
      setShowCommentBox(false);
    }
  };

  if (message.sender === 'user') {
    return (
      <div className="flex justify-end mb-4" data-testid="user-message">
        <div className="max-w-[85%] sm:max-w-xl rounded-2xl rounded-tr-none px-4 py-3 bg-indigo-600 text-white shadow-sm">
          <p className="text-sm whitespace-pre-wrap leading-relaxed">{message.content}</p>
          <div className="text-[10px] text-indigo-200 mt-1.5 text-right font-mono">
            {new Date(message.timestamp).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}
          </div>
        </div>
      </div>
    );
  }

  // Se a mensagem for um erro de segurança/Prompt Injection bloqueado
  if (message.isError) {
    return (
      <div className="flex justify-start mb-4" data-testid="assistant-error-message">
        <div className="max-w-[90%] sm:max-w-2xl rounded-2xl rounded-tl-none p-4 bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900/60 shadow-sm text-rose-900 dark:text-rose-200">
          <div className="flex items-center gap-2 mb-2 text-rose-700 dark:text-rose-400 font-semibold text-xs uppercase tracking-wide">
            <svg className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            <span>Bloqueio de Segurança: {message.threatType || 'Entrada Não Autorizada'}</span>
          </div>
          <p className="text-sm whitespace-pre-wrap leading-relaxed">{message.content}</p>
          <div className="text-[10px] text-rose-500 dark:text-rose-400 mt-2 font-mono">
            Incidente registrado em auditoria de segurança • {new Date(message.timestamp).toLocaleTimeString('pt-BR')}
          </div>
        </div>
      </div>
    );
  }

  const meta = message.responseMeta;

  return (
    <div className="flex justify-start mb-6" data-testid="assistant-message">
      <div className="max-w-[95%] sm:max-w-2xl w-full rounded-2xl rounded-tl-none p-4 sm:p-5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
        {/* Header da Mensagem do Assistente */}
        <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 dark:border-slate-800 pb-3">
          <div className="flex items-center gap-2">
            <span className="p-1.5 rounded-lg bg-indigo-50 dark:bg-indigo-900/50 text-indigo-600 dark:text-indigo-400">
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </span>
            <div>
              <span className="text-xs font-semibold text-slate-800 dark:text-slate-200">
                Assistente de Inteligência
              </span>
              {meta && (
                <span className="ml-2 text-[11px] font-mono text-slate-500 dark:text-slate-400">
                  {meta.promptKey} ({meta.promptVersion})
                </span>
              )}
            </div>
          </div>

          {meta && <AiUsageMetricsPill metrics={meta.usageMetrics} />}
        </div>

        {/* Conteúdo Principal da Análise */}
        <div className="text-sm text-slate-800 dark:text-slate-200 leading-relaxed whitespace-pre-wrap">
          {message.content}
        </div>

        {/* Avisos de Dados Indisponíveis se houver */}
        {meta?.unavailabilityNotes && meta.unavailabilityNotes.length > 0 && (
          <div className="p-3 rounded-xl bg-amber-50/80 dark:bg-amber-950/30 border border-amber-200/80 dark:border-amber-900/40 text-xs text-amber-800 dark:text-amber-300">
            <div className="flex items-center gap-1.5 font-semibold mb-1">
              <svg className="w-4 h-4 text-amber-600 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              <span>Avisos de Indisponibilidade de Dados:</span>
            </div>
            <ul className="list-disc list-inside space-y-0.5">
              {meta.unavailabilityNotes.map((note, idx) => (
                <li key={idx}>{note}</li>
              ))}
            </ul>
          </div>
        )}

        {/* Fontes Consultadas e Botão de Inspeção */}
        {meta && meta.sources && meta.sources.length > 0 && (
          <div className="flex flex-wrap items-center justify-between gap-2 p-2.5 rounded-xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200 dark:border-slate-800 text-xs">
            <div className="flex flex-wrap items-center gap-1.5">
              <span className="font-medium text-slate-500 dark:text-slate-400">Fontes:</span>
              {meta.sources.map((src, idx) => (
                <span
                  key={idx}
                  className="px-2 py-0.5 rounded-md bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800/60 font-mono text-[11px]"
                >
                  {src}
                </span>
              ))}
            </div>

            <button
              type="button"
              onClick={() => onOpenSources(meta)}
              className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-indigo-600 dark:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-indigo-950/50 font-medium transition-colors min-h-[36px]"
              aria-label="Inspecionar fontes e dados da resposta"
            >
              <span>Inspecionar Fontes & Dados</span>
              <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </button>
          </div>
        )}

        {/* Disclaimer Anti-Alucinação & Aviso de Modo Somente Leitura */}
        <div className="space-y-1.5 pt-1 text-[11px] text-slate-500 dark:text-slate-400">
          <div className="flex items-start gap-1.5 leading-normal">
            <svg className="w-3.5 h-3.5 text-slate-400 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
            <p>
              {meta?.antiHallucinationDisclaimer ||
                'Valores e projeções calculados deterministicamente pelo Seller Intelligence Platform. Métricas não fornecidas pelas APIs oficiais dos marketplaces são explicitamente sinalizadas como indisponíveis, prevenindo alucinações.'}
            </p>
          </div>
          <p className="text-amber-600 dark:text-amber-400/90 pl-5 italic font-medium" data-testid="read-only-notice">
            Nota de Operação (MVP): Esta resposta é consultiva. Nenhuma ação ou alteração foi executada no marketplace.
          </p>
        </div>

        {/* Rodapé da Mensagem: Feedback e Data */}
        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 dark:border-slate-800 pt-3">
          <div className="flex items-center gap-2">
            <span className="text-xs text-slate-500 dark:text-slate-400">A resposta foi útil?</span>
            <button
              type="button"
              onClick={() => handleFeedback('helpful')}
              className={`p-1.5 rounded-lg border transition-colors min-h-[36px] min-w-[36px] flex items-center justify-center ${
                feedbackGiven === 'helpful'
                  ? 'bg-emerald-100 dark:bg-emerald-950/60 border-emerald-400 text-emerald-700 dark:text-emerald-300'
                  : 'border-slate-200 dark:border-slate-700 text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800'
              }`}
              aria-label="Avaliar como resposta útil"
              title="Resposta útil"
            >
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 10h4.764a2 2 0 011.789 2.894l-3.5 7A2 2 0 0115.263 21h-4.017c-.163 0-.326-.02-.485-.06L7 20m7-10V5a2 2 0 00-2-2h-.095c-.5 0-.905.405-.905.905 0 .714-.211 1.412-.608 2.006L7 11v9m7-10h-2M7 20H5a2 2 0 01-2-2v-6a2 2 0 012-2h2.5" />
              </svg>
            </button>
            <button
              type="button"
              onClick={() => handleFeedback('not_helpful')}
              className={`p-1.5 rounded-lg border transition-colors min-h-[36px] min-w-[36px] flex items-center justify-center ${
                feedbackGiven === 'not_helpful'
                  ? 'bg-rose-100 dark:bg-rose-950/60 border-rose-400 text-rose-700 dark:text-rose-300'
                  : 'border-slate-200 dark:border-slate-700 text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800'
              }`}
              aria-label="Avaliar como resposta não útil"
              title="Resposta não útil"
            >
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 14H5.236a2 2 0 01-1.789-2.894l3.5-7A2 2 0 018.736 3h4.018a2 2 0 01.485.06l3.76.94m-7 10v5a2 2 0 002 2h.096c.5 0 .905-.405.905-.904 0-.715.211-1.413.608-2.008L17 13V4m-7 10h2m5-10h2a2 2 0 012 2v6a2 2 0 01-2 2h-2.5" />
              </svg>
            </button>

            {feedbackGiven && (
              <button
                type="button"
                onClick={() => setShowCommentBox(!showCommentBox)}
                className="text-xs text-indigo-600 dark:text-indigo-400 hover:underline ml-1"
              >
                {showCommentBox ? 'Fechar comentário' : 'Adicionar comentário'}
              </button>
            )}
          </div>

          <div className="text-[11px] text-slate-400 font-mono">
            {new Date(message.timestamp).toLocaleString('pt-BR')}
          </div>
        </div>

        {/* Caixa de comentário de feedback */}
        {showCommentBox && (
          <div className="pt-2 border-t border-slate-100 dark:border-slate-800 flex gap-2">
            <input
              type="text"
              value={commentText}
              onChange={(e) => setCommentText(e.target.value)}
              placeholder="O que pode ser melhorado nesta resposta?"
              className="flex-1 text-xs px-3 py-2 rounded-lg bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              aria-label="Comentário sobre a resposta"
            />
            <button
              type="button"
              onClick={handleSaveComment}
              className="px-3 py-1.5 bg-indigo-600 text-white rounded-lg text-xs font-medium hover:bg-indigo-700 transition-colors"
            >
              Salvar
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
