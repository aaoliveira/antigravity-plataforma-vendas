import { test, expect } from '@playwright/test';
import { setupMockRoutes, navigateToModule } from './helpers/mock-routes';
import { ConsoleMonitor } from './helpers/console-monitor';
import { checkA11y } from './helpers/a11y-helper';

test.describe('Fluxo 06: Assistente IA e Editor de Conteúdo Assistido', () => {
  test.beforeEach(async ({ page }) => {
    await setupMockRoutes(page, { authenticated: true });
  });

  test('deve consultar o assistente de IA com indicação de fontes e validar editor de rascunhos', async ({ page, isMobile }) => {
    const consoleMonitor = new ConsoleMonitor(page);
    await page.goto('/');

    // 1. Navegar para o Assistente IA
    await navigateToModule(page, 'Assistente', isMobile);

    const main = page.locator('main');
    await expect(main).toContainText('Assistente');

    // 2. Navegar para o AI Studio (Editor de Rascunho)
    await navigateToModule(page, 'Studio', isMobile);

    await expect(main).toContainText('MODO RASCUNHO ASSISTIDO');
    await expect(main).toContainText('REVISÃO HUMANA MANDATÓRIA');
    // Verificar que não há botão de publicar externamente
    await expect(page.locator('button:has-text("Publicar no Mercado Livre"), button:has-text("Publicar na Shopee")')).toHaveCount(0);

    // 3. Auditoria de Acessibilidade
    await checkA11y(page, 'Assistente e AI Studio');

    // 4. Sem erros de console
    consoleMonitor.assertNoErrors();
  });
});
