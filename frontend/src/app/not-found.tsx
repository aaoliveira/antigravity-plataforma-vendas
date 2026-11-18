import React from 'react';
import { AccessDeniedFeedback } from '@/components/AccessDeniedFeedback';

export default function NotFound() {
  return (
    <main
      role="main"
      className="min-h-screen flex items-center justify-center p-4 bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100"
    >
      <AccessDeniedFeedback />
    </main>
  );
}
