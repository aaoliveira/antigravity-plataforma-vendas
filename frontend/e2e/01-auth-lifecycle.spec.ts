import { test, expect } from '@playwright/test';
import { setupMockRoutes } from './helpers/mock-routes';
import { ConsoleMonitor } from './helpers/console-monitor';
import { checkA11y } from './helpers/a11y-helper';

test.describe('Fluxo 01: Ciclo de Vida de Autenticação e Sessão', () => {
  test('deve validar mensagens de erro, sessão expirada e login bem-sucedido', async ({ page }) => {
    const consoleMonitor = new ConsoleMonitor(page);
    await setupMockRoutes(page, { authenticated: false });

    // 1. Acesso à página de login
    await page.goto('/login');
    await expect(page).toHaveTitle(/Seller Intelligence/i);
    await expect(page.locator('h1')).toContainText('Acesso à Plataforma');

    // 2. Acessibilidade WCAG 2.1 AA na tela de login
    await checkA11y(page, 'Tela de Login');

    // 3. Tentativa de login com credenciais incorretas
    await page.fill('#email-input', 'invalido@teste.com');
    await page.fill('#password-input', 'SenhaErrada123');
    await page.click('button[type="submit"]');

    const alertMessage = page.getByRole('alert').filter({ hasText: 'Falha de Autenticação' });
    await expect(alertMessage).toBeVisible();

    // 4. Teste de aviso de sessão expirada via query param
    await page.goto('/login?expired=true');
    const expiredStatus = page.locator('div[role="status"]');
    await expect(expiredStatus).toBeVisible();
    await expect(expiredStatus).toContainText('Sessão Expirada');

    // 5. Login com credenciais válidas e transição para o shell
    await setupMockRoutes(page, { authenticated: true });
    await page.fill('#email-input', 'admin@sellerintelligence.com.br');
    await page.fill('#password-input', 'Admin@123456');
    await page.click('button[type="submit"]');

    // Deve redirecionar para a raiz e carregar o AppShell
    await page.waitForURL('/');
    await expect(page.locator('header')).toBeVisible();
    await expect(page.locator('header h1')).toContainText('Seller Intelligence Platform');

    // 6. Verificar ausência de erros no console
    consoleMonitor.assertNoErrors();
  });
});
