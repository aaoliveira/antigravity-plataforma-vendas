import { test, expect } from '@playwright/test';
import { setupMockRoutes } from './helpers/mock-routes';
import { ConsoleMonitor } from './helpers/console-monitor';
import { checkA11y } from './helpers/a11y-helper';

test.describe('Fluxo 07: Responsividade Mobile, Menu Drawer, Dock e Zero Overflow', () => {
  test.beforeEach(async ({ page }) => {
    await setupMockRoutes(page, { authenticated: true });
  });

  test('deve garantir ausência de overflow horizontal, funcionamento do menu drawer e navegação rápida', async ({ page, isMobile }) => {
    const consoleMonitor = new ConsoleMonitor(page);
    await page.goto('/');

    // 1. Verificação de Zero Overflow Horizontal (scrollWidth <= clientWidth)
    const isOverflowing = await page.evaluate(() => {
      const docEl = document.documentElement;
      const main = document.querySelector('main');
      return docEl.scrollWidth > docEl.clientWidth || (main ? main.scrollWidth > main.clientWidth : false);
    });
    expect(isOverflowing, 'Não deve haver overflow horizontal em nenhum breakpoint').toBeFalsy();

    // 2. Em telas mobile, testar gaveta drawer e fechamento via Escape
    if (isMobile) {
      const openDrawerBtn = page.locator('button[aria-label="Abrir menu de navegação"]');
      await expect(openDrawerBtn).toBeVisible();
      await openDrawerBtn.dispatchEvent('click');

      const drawerDialog = page.locator('div[role="dialog"][aria-label="Menu principal de navegação móvel"]');
      await expect(drawerDialog).toBeVisible();

      // Fechar com tecla Escape
      await page.keyboard.press('Escape');
      await expect(drawerDialog).not.toBeVisible();

      // Testar Dock Inferior (seletor de atalhos rápidos)
      const bottomDock = page.locator('nav[aria-label="Navegação rápida móvel"]');
      if (await bottomDock.isVisible()) {
        const dockButtons = bottomDock.locator('button');
        const count = await dockButtons.count();
        expect(count).toBeGreaterThan(0);
      }
    }

    // 3. Auditoria de Acessibilidade
    await checkA11y(page, 'Responsividade e Navegação Mobile');

    // 4. Sem erros de console
    consoleMonitor.assertNoErrors();
  });
});
