import { test, expect } from '@playwright/test';
import { setupMockRoutes, navigateToModule } from './helpers/mock-routes';
import { ConsoleMonitor } from './helpers/console-monitor';
import { checkA11y } from './helpers/a11y-helper';

test.describe('Fluxo 03: Catálogo Canônico e Pedidos com Fatos Contábeis', () => {
  test.beforeEach(async ({ page }) => {
    await setupMockRoutes(page, { authenticated: true });
  });

  test('deve navegar para o catálogo, validar anúncios e consultar pedidos com apuração contábil', async ({ page, isMobile }) => {
    const consoleMonitor = new ConsoleMonitor(page);
    await page.goto('/');

    // 1. Navegação para Catálogo de Anúncios
    await navigateToModule(page, 'Catálogo', isMobile);

    const main = page.locator('main');
    await expect(main).toContainText('Catálogo');
    await expect(main).toContainText('Teclado Mecânico Gamer RGB');
    await expect(main).toContainText('Mouse Gamer Ergonômico');
    await expect(main).toContainText('249,90');

    // 2. Navegação para Pedidos e Conciliação
    await navigateToModule(page, 'Pedidos', isMobile);

    await expect(main).toContainText('Pedidos');
    await expect(main).toContainText('2000008899');
    await expect(main).toContainText('Pago');

    // 3. Auditoria de Acessibilidade
    await checkA11y(page, 'Catálogo e Pedidos');

    // 4. Sem erros de console
    consoleMonitor.assertNoErrors();
  });
});
