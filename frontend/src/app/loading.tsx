import { LoadingSkeleton } from '@/components/LoadingSkeleton';

export default function Loading() {
  return (
    <div className="min-h-screen flex items-center justify-center p-6 bg-slate-50 dark:bg-slate-950">
      <div className="max-w-md w-full bg-white dark:bg-slate-900 p-6 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-blue-600 animate-pulse" />
          <div className="h-5 bg-slate-200 dark:bg-slate-800 rounded w-1/2 animate-pulse" />
        </div>
        <LoadingSkeleton rows={4} />
      </div>
    </div>
  );
}
