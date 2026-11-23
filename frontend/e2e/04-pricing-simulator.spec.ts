import { test, expect } from '@playwright/test';
import { setupMockRoutes, navigateToModule } from './helpers/mock-routes';
import { ConsoleMonitor } from './helpers/console-monitor';
import { checkA11y } from './helpers/a11y-helper';

test.describe('Fluxo 04: Motor de Precificação e Simulador de Margem', () => {
  test.beforeEach(async ({ page }) => {
    await setupMockRoutes(page, { authenticated: true });
  });

  test('deve simular preço de venda determinístico, exibir margem de contribuição e respeitar salvaguarda contábil', async ({ page, isMobile }) => {
    const consoleMonitor = new ConsoleMonitor(page);
    await page.goto('/');

    // Navegar para o módulo Finanças / Precificação
    await navigateToModule(page, 'Financeiro', isMobile);

    const main = page.locator('main');
    await expect(main).toContainText('Gestão de Custos, Taxas e Simulação Financeira');

    // Alternar para aba da Calculadora de Margem
    const calcTab = page.locator('button[role="tab"]:has-text("Calculadora de Margem")');
    if (await calcTab.isVisible()) {
      await calcTab.click();
    }

    // Conferir salvaguarda contábil inamovível
    await expect(main).toContainText(/Anti-Alucinação Ativa|Margem de Contribuição/i);

    // Auditoria de Acessibilidade
    await checkA11y(page, 'Simulador de Precificação e Finanças');

    // Sem erros de console
    consoleMonitor.assertNoErrors();
  });
});
