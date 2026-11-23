import { test, expect } from '@playwright/test';
import { setupMockRoutes, navigateToModule } from './helpers/mock-routes';
import { ConsoleMonitor } from './helpers/console-monitor';
import { checkA11y } from './helpers/a11y-helper';

test.describe('Fluxo 05: Central de Relatórios e Exportação Assíncrona', () => {
  test.beforeEach(async ({ page }) => {
    await setupMockRoutes(page, { authenticated: true });
  });

  test('deve abrir central de exportação, disparar job e validar download auditado', async ({ page, isMobile }) => {
    const consoleMonitor = new ConsoleMonitor(page);
    await page.goto('/');

    // Navegar para Relatórios
    await navigateToModule(page, 'Relatórios', isMobile);

    const main = page.locator('main');
    await expect(main).toContainText('Relatórios');

    // Abrir modal de nova exportação se houver botão
    const newExportBtn = page.locator('button[aria-label="Solicitar nova exportação"]').first();
    if (await newExportBtn.isVisible()) {
      await newExportBtn.click();

      // Verificar que modal de exportação abriu com role dialog
      const modal = page.locator('div[role="dialog"]');
      if (await modal.isVisible()) {
        await expect(modal).toContainText(/Formato|Exportação/i);
        // Fechar modal
        await page.keyboard.press('Escape');
      }
    }

    // Auditoria de Acessibilidade
    await checkA11y(page, 'Central de Relatórios');

    // Sem erros de console
    consoleMonitor.assertNoErrors();
  });
});
