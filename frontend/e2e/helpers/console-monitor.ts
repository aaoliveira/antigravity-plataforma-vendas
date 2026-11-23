import { Page, expect } from '@playwright/test';

export class ConsoleMonitor {
  private errors: string[] = [];

  constructor(private page: Page) {
    this.page.on('console', (msg) => {
      if (msg.type() === 'error') {
        const text = msg.text();
        // Permite ignorar falhas benignas de 404/401 HTTP response esperadas em testes e favicon
        if (
          !text.includes('favicon.ico') &&
          !text.includes('status of 401') &&
          !text.includes('status of 404') &&
          !text.includes('Failed to load resource')
        ) {
          this.errors.push(`[Console Error] ${text}`);
        }
      }
    });

    this.page.on('pageerror', (err) => {
      this.errors.push(`[Page Error] ${err.name}: ${err.message}`);
    });
  }

  assertNoErrors() {
    expect(this.errors, 'Erros de console ou runtime não tratados foram detectados').toEqual([]);
  }

  getErrors() {
    return this.errors;
  }
}
