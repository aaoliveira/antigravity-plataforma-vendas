import { test, expect } from '@playwright/test';

test.describe('Seller Intelligence Platform — Phase 1 MVP Critical Journeys', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('heading', { name: 'Seller Intelligence Platform' })).toBeVisible();
  });

  test('Journey 1: Onboarding, Company & Store Management', async ({ page }) => {
    // 1. Verify Page Hero
    await expect(page.getByRole('heading', { name: 'Estrutura Organizacional e Gestão de Acessos' })).toBeVisible();
    
    // 2. Open form and Add New Company
    await page.getByRole('button', { name: '+ Nova Empresa' }).click();

    const cnpjInput = page.getByPlaceholder('00.000.000/0000-00');
    const tradeNameInput = page.getByPlaceholder('Ex: Minha Loja Matriz');
    const legalNameInput = page.getByPlaceholder('Ex: Comercio Digital Ltda');
    
    await cnpjInput.fill('33.000.167/0001-01');
    await tradeNameInput.fill('Petro E-Commerce Matriz');
    await legalNameInput.fill('Petro Distribuidora e Varejo SA');
    
    // Submit form
    await page.getByRole('button', { name: 'Salvar Empresa' }).click();

    // Verify company listed
    await expect(page.getByText('Petro E-Commerce Matriz')).toBeVisible();
    await expect(page.getByText('33.000.167/0001-01')).toBeVisible();

    // 3. Navigate to Stores Tab
    await page.getByRole('button', { name: /Lojas & Marketplaces/i }).click();
    await expect(page.getByRole('heading', { name: 'Lojas & Contas de Marketplace' })).toBeVisible();

    // Open form and Add New Store
    await page.getByRole('button', { name: '+ Nova Loja' }).click();
    await page.getByPlaceholder('Ex: Mercado Livre - Conta Oficial').fill('Petro Loja Oficial Mercado Livre');
    await page.getByRole('button', { name: 'Salvar Loja' }).click();

    // Verify store listed
    await expect(page.getByText('Petro Loja Oficial Mercado Livre')).toBeVisible();
    await expect(page.getByText('DESCONECTADO').first()).toBeVisible();
  });

  test('Journey 2: Marketplace Capabilities & Async Sync Engine', async ({ page }) => {
    // 1. Check Capabilities Tab
    await page.getByRole('button', { name: /Capacidades & Conectores/i }).click();
    await expect(page.getByRole('heading', { name: 'Matriz Oficial de Capacidades dos Conectores' })).toBeVisible();
    await expect(page.getByText('Mercado Livre').first()).toBeVisible();
    await expect(page.getByText('Shopee').first()).toBeVisible();

    // 2. Check Sync & Catalog Tab
    await page.getByRole('button', { name: /Sincronização & Catálogo/i }).click();
    await expect(page.getByRole('heading', { name: 'Central de Sincronização & Catálogo' })).toBeVisible();

    // Trigger Manual Sync
    await page.getByRole('button', { name: 'Sincronizar Agora' }).click();
    await expect(page.getByText(/Sincronização manual de CATALOG concluída com sucesso/i)).toBeVisible();

    // Verify Contingency CSV section
    await expect(page.getByRole('heading', { name: /Importação de Contingência por Arquivo/i })).toBeVisible();
  });

  test('Journey 3: Market Intelligence & Provenance Transparency', async ({ page }) => {
    // 1. Navigate to Market Intelligence
    await page.getByRole('button', { name: /Pesquisa de Mercado/i }).click();
    await expect(page.getByRole('heading', { name: 'Market Intelligence: Pesquisa de Mercado' })).toBeVisible();

    // 2. Execute keyword search
    const searchInput = page.getByPlaceholder('Ex: teclado mecanico, fone bluetooth, MLB123456');
    await searchInput.fill('fone de ouvido bluetooth');
    await page.getByRole('button', { name: 'Pesquisar' }).click();

    // Verify results and provenance label
    await expect(page.getByText(/OBSERVED/i).first()).toBeVisible();
    await expect(page.getByText(/Produto Observado: fone de ouvido bluetooth/i)).toBeVisible();

    // Save search opportunity
    await page.getByRole('button', { name: 'Salvar Busca' }).click();
    await expect(page.getByRole('heading', { name: 'Salvar Pesquisa de Mercado' })).toBeVisible();
  });

  test('Journey 4: Deterministic Financial Engine & Executive Dashboard', async ({ page }) => {
    // 1. Navigate to Financial Tab
    await page.getByRole('button', { name: /Motor Financeiro/i }).click();
    await expect(page.getByRole('heading', { name: 'Painel Financeiro & Rentabilidade' })).toBeVisible();

    // 2. Verify KPI Cards
    await expect(page.getByText(/Faturamento Bruto/i)).toBeVisible();
    await expect(page.getByText(/Margem de Contribuição/i).first()).toBeVisible();
    await expect(page.getByText(/Total de Pedidos/i)).toBeVisible();
    await expect(page.getByText(/Ticket Médio/i)).toBeVisible();

    // 3. Verify CSV Export Button
    const exportBtn = page.getByRole('button', { name: 'Exportar CSV' });
    await expect(exportBtn).toBeVisible();

    // 4. Switch to Price Simulator Subtab
    await page.getByRole('button', { name: /Simulador & Regras/i }).click();
    await expect(page.getByRole('heading', { name: 'Motor Financeiro Determinístico' })).toBeVisible();

    // Fill Simulation Inputs
    const cogsInput = page.locator('#input-cogs');
    await cogsInput.fill('60.00');

    // Run simulation
    await page.getByRole('button', { name: 'Calcular Margem e Preço' }).click();

    // Verify calculated results
    await expect(page.getByText('Memória de Cálculo Explicável')).toBeVisible();
    await expect(page.getByText('Preço Mínimo (Break-even)', { exact: true })).toBeVisible();
  });

  test('Journey 5: AI Listing Studio (Audit Score, Factual Guardrail & Assisted Generation)', async ({ page }) => {
    // 1. Navigate to AI Studio Tab
    await page.getByRole('button', { name: /AI Listing Studio/i }).click();
    await expect(page.getByRole('heading', { name: /AI Listing Studio — Análise & Otimização/i })).toBeVisible();

    // 2. Verify 4 Audit Pillars
    await expect(page.getByRole('heading', { name: 'Score de Qualidade do Anúncio' })).toBeVisible();
    await expect(page.getByText('Título').first()).toBeVisible();
    await expect(page.getByText('Descrição').first()).toBeVisible();
    await expect(page.getByText('Ficha Técnica').first()).toBeVisible();
    await expect(page.getByText('Fotos / Galeria').first()).toBeVisible();

    // 3. Generate AI Assisted Title
    await page.getByRole('button', { name: 'Otimizar Título com IA' }).click();
    await expect(page.getByText(/SUGESTÃO IA/i).first()).toBeVisible();

    // 4. Generate AI Structured Technical Description
    await page.getByRole('button', { name: 'Gerar Descrição Técnica' }).click();
    await expect(page.getByText(/Descrição Estruturada:/i)).toBeVisible();

    // 5. Save Version Draft
    await page.getByRole('button', { name: 'Salvar Rascunho da Versão' }).click();
    await expect(page.getByText(/Rascunho da Versão 2 salvo com sucesso!/i)).toBeVisible();
  });

});
