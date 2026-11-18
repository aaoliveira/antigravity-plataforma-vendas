import React from 'react';

interface LoadingSkeletonProps {
  rows?: number;
  className?: string;
}

export const LoadingSkeleton: React.FC<LoadingSkeletonProps> = ({ rows = 3, className = '' }) => {
  return (
    <div
      role="status"
      aria-label="Carregando conteúdo..."
      className={`space-y-3 w-full animate-pulse ${className}`}
    >
      <div className="h-6 bg-slate-200 dark:bg-slate-800 rounded-lg w-1/3" />
      <div className="h-4 bg-slate-200 dark:bg-slate-800 rounded-md w-2/3" />
      <div className="space-y-2 pt-2">
        {Array.from({ length: rows }).map((_, idx) => (
          <div key={idx} className="h-10 bg-slate-200 dark:bg-slate-800 rounded-lg w-full" />
        ))}
      </div>
      <span className="sr-only">Carregando...</span>
    </div>
  );
};
