'use client';

import React from 'react';

interface CharacterLimitGaugeProps {
  currentCount: number;
  minLimit: number;
  maxLimit: number;
  label: string;
  officialSource?: string;
}

export const CharacterLimitGauge: React.FC<CharacterLimitGaugeProps> = ({
  currentCount,
  minLimit,
  maxLimit,
  label,
  officialSource,
}) => {
  const percentage = Math.min(100, Math.round((currentCount / maxLimit) * 100));

  const isExceeded = currentCount > maxLimit;
  const isBelowMin = currentCount < minLimit;
  const isNearLimit = !isExceeded && currentCount >= maxLimit * 0.85;

  let statusColor = 'bg-emerald-500';
  let textColor = 'text-emerald-700 dark:text-emerald-400';
  let badgeColor = 'bg-emerald-50 text-emerald-800 border-emerald-200 dark:bg-emerald-950/40 dark:text-emerald-300 dark:border-emerald-800';
  let statusText = 'Dentro do limite oficial';

  if (isExceeded) {
    statusColor = 'bg-red-500';
    textColor = 'text-red-700 dark:text-red-400';
    badgeColor = 'bg-red-50 text-red-800 border-red-200 dark:bg-red-950/40 dark:text-red-300 dark:border-red-800';
    statusText = `Excedeu limite máximo (+${currentCount - maxLimit})`;
  } else if (isBelowMin) {
    statusColor = 'bg-amber-500';
    textColor = 'text-amber-700 dark:text-amber-400';
    badgeColor = 'bg-amber-50 text-amber-800 border-amber-200 dark:bg-amber-950/40 dark:text-amber-300 dark:border-amber-800';
    statusText = `Abaixo do mínimo oficial (-${minLimit - currentCount})`;
  } else if (isNearLimit) {
    statusColor = 'bg-amber-500';
    textColor = 'text-amber-700 dark:text-amber-400';
    badgeColor = 'bg-amber-50 text-amber-800 border-amber-200 dark:bg-amber-950/40 dark:text-amber-300 dark:border-amber-800';
    statusText = 'Próximo do limite máximo';
  }

  return (
    <div className="flex flex-col gap-1.5 w-full text-xs">
      <div className="flex items-center justify-between">
        <span className="font-medium text-slate-700 dark:text-slate-300">{label}</span>
        <div className="flex items-center gap-2">
          <span className={`font-semibold ${textColor}`}>
            {currentCount} / {maxLimit} carac.
          </span>
          <span className={`px-2 py-0.5 rounded-full border text-[11px] font-medium ${badgeColor}`}>
            {statusText}
          </span>
        </div>
      </div>

      <div className="w-full bg-slate-200 dark:bg-slate-700 rounded-full h-2 overflow-hidden">
        <div
          className={`h-2 rounded-full transition-all duration-300 ${statusColor}`}
          style={{ width: `${percentage}%` }}
          role="progressbar"
          aria-valuenow={currentCount}
          aria-valuemin={minLimit}
          aria-valuemax={maxLimit}
          aria-label={`Contagem de caracteres para ${label}`}
        />
      </div>

      {officialSource && (
        <span className="text-[10px] text-slate-500 dark:text-slate-400 truncate" title={officialSource}>
          Regra oficial: {officialSource}
        </span>
      )}
    </div>
  );
};
