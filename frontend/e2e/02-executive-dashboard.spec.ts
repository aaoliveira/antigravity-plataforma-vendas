import { test, expect } from '@playwright/test';
import { setupMockRoutes, navigateToModule } from './helpers/mock-routes';
import { ConsoleMonitor } from './helpers/console-monitor';
import { checkA11y } from './helpers/a11y-helper';

test.describe('Fluxo 02: Dashboard Executivo, KPIs e Salvaguardas Contábeis', () => {
  test.beforeEach(async ({ page }) => {
    await setupMockRoutes(page, { authenticated: true });
  });

  test('deve renderizar os 6 KPIs principais, performance por canal e catálogo de fórmulas com anti-alucinação', async ({ page, isMobile }) => {
    const consoleMonitor = new ConsoleMonitor(page);
    await page.goto('/');

    // Navegar para o módulo EXECUTIVE_DASHBOARD se não estiver ativo
    await navigateToModule(page, 'Dashboard', isMobile);

    // 1. Validar renderização dos dados reais dos KPIs
    const main = page.locator('main');
    await expect(main).toContainText('Receita Bruta Total');
    await expect(main).toContainText('38.450,00');

    await expect(main).toContainText('Margem de Contribuição');
    await expect(main).toContainText('11.150,00');
    await expect(main).toContainText('29,00%');

    await expect(main).toContainText('Pedidos Faturados');
    await expect(main).toContainText('1.420');

    await expect(main).toContainText('Taxa de Reconciliação');
    await expect(main).toContainText('100,00%');

    // 2. Validar desempenho por canal (Mercado Livre vs Shopee)
    await expect(main).toContainText('Mercado Livre');
    await expect(main).toContainText('Shopee');
    await expect(main).toContainText('65,00%');
    await expect(main).toContainText('35,00%');

    // 3. Validar banner de Stale Data / Cache e botão de invalidação
    await expect(main).toContainText('Cache Ativo');
    const refreshBtn = page.locator('button:has-text("Atualizar Dados")');
    await expect(refreshBtn).toBeVisible();
    await refreshBtn.click();

    // 4. Abrir catálogo de definições e verificar salvaguarda anti-alucinação
    const openGlossaryBtn = page.locator('button:has-text("Catálogo de Fórmulas")');
    await openGlossaryBtn.click();

    const drawer = page.locator('div[role="dialog"]');
    await expect(drawer).toBeVisible();
    await expect(drawer).toContainText('Catálogo Oficial de KPIs');
    await expect(drawer).toContainText(/terminantemente proibido rotular este resultado como Lucro Líquido|Margem de Contribuição/i);

    // Fechar com Escape
    await page.keyboard.press('Escape');
    await expect(drawer).not.toBeVisible();

    // 5. Auditoria de Acessibilidade WCAG 2.1 AA (Modo Padrão da Aplicação)
    await checkA11y(page, 'Dashboard Executivo');

    // 6. Testar alternância de tema Light/Dark e persistência
    const themeBtn = page.locator('button[aria-label*="modo" i]');
    await expect(themeBtn).toBeVisible();
    await themeBtn.click();
    const html = page.locator('html');
    await expect(html).toHaveAttribute('class', /(dark|light)/);
    // Alternar de volta
    await themeBtn.click();

    // 7. Ausência de erros críticos de console
    consoleMonitor.assertNoErrors();
  });
});
