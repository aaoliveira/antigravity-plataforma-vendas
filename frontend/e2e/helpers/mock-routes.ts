import { Page, expect } from '@playwright/test';

export async function navigateToModule(page: Page, moduleName: string, isMobile: boolean) {
  if (isMobile) {
    const openDrawerBtn = page.locator('button[aria-label="Abrir menu de navegação"]');
    await openDrawerBtn.dispatchEvent('click');
    const drawer = page.locator('div[role="dialog"][aria-label="Menu principal de navegação móvel"]');
    await expect(drawer).toBeVisible();
    await drawer.locator(`button:has-text("${moduleName}")`).first().dispatchEvent('click');
    await expect(drawer).not.toBeVisible();
  } else {
    await page.locator(`aside button:has-text("${moduleName}")`).first().click();
  }
}

export async function setupMockRoutes(page: Page, options: { authenticated?: boolean } = { authenticated: true }) {
  const { authenticated = true } = options;

  if (authenticated) {
    await page.context().addCookies([
      {
        name: 'seller_access_token',
        value: 'mock-valid-jwt-token-for-e2e-testing',
        domain: 'localhost',
        path: '/',
        httpOnly: true,
      },
    ]);
  } else {
    await page.context().clearCookies();
  }

  await page.route('**/api/auth/session', async (route) => {
    if (authenticated) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          authenticated: true,
          user: {
            userId: 'usr-admin-01',
            email: 'admin@sellerintelligence.com.br',
            fullName: 'Administrador Alpha',
            status: 'ACTIVE',
            memberships: [
              {
                tenantId: 'tenant-alpha-001',
                tenantName: 'Loja Alpha E-commerce',
                role: 'ADMIN',
                status: 'ACTIVE',
              },
            ],
          },
          currentTenant: {
            tenantId: 'tenant-alpha-001',
            name: 'Loja Alpha E-commerce',
            slug: 'loja-alpha',
            role: 'ADMIN',
            status: 'ACTIVE',
          },
        }),
      });
    } else {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          authenticated: false,
          user: null,
          currentTenant: null,
        }),
      });
    }
  });

  await page.route('**/api/auth/login', async (route) => {
    const postData = route.request().postDataJSON() || {};
    if (postData.email === 'admin@sellerintelligence.com.br' && postData.password === 'Admin@123456') {
      await page.context().addCookies([
        {
          name: 'seller_access_token',
          value: 'mock-valid-jwt-token-for-e2e-testing',
          domain: 'localhost',
          path: '/',
          httpOnly: true,
        },
      ]);
      await route.fulfill({
        status: 200,
        headers: {
          'Set-Cookie': 'seller_access_token=mock-valid-jwt-token-for-e2e-testing; Path=/; HttpOnly; SameSite=Lax',
        },
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          activeTenantId: 'tenant-alpha-001',
          roles: ['ADMIN'],
        }),
      });
    } else {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({
          title: 'Credenciais Inválidas',
          detail: 'E-mail ou senha incorretos.',
        }),
      });
    }
  });

  await page.route('**/api/analytics/kpis/summary*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        totalOrders: 1420,
        totalGrossRevenue: 38450.0,
        totalNetRevenue: 32900.0,
        totalContributionMarginAmount: 11150.0,
        averageContributionMarginRatio: 29.0,
        averageOrderValue: 27.08,
        reconciliationRatePercent: 100.0,
        ordersWithMissingDataCount: 0,
        divergentCount: 0,
        totalMarketplaceFees: 5383.0,
        totalFixedFees: 0.0,
        totalShippingCost: 1250.0,
        totalProductCost: 18200.0,
        totalPackagingCost: 850.0,
        totalTaxAmount: 1617.0,
        calculatedAt: new Date().toISOString(),
        cached: true,
      }),
    });
  });

  await page.route('**/api/analytics/kpis/by-channel*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          marketplace: 'MERCADO_LIVRE',
          orderCount: 920,
          grossRevenue: 24992.5,
          contributionMarginAmount: 7247.83,
          revenueSharePercent: 65.0,
        },
        {
          marketplace: 'SHOPEE',
          orderCount: 500,
          grossRevenue: 13457.5,
          contributionMarginAmount: 3902.17,
          revenueSharePercent: 35.0,
        },
      ]),
    });
  });

  await page.route('**/api/analytics/kpis/by-period*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        { period: '2026-11-17', periodLabel: '17/11', orderCount: 200, grossRevenue: 5400.0, contributionMarginAmount: 1566.0 },
        { period: '2026-11-18', periodLabel: '18/11', orderCount: 210, grossRevenue: 5670.0, contributionMarginAmount: 1644.3 },
        { period: '2026-11-19', periodLabel: '19/11', orderCount: 195, grossRevenue: 5265.0, contributionMarginAmount: 1526.85 },
        { period: '2026-11-20', periodLabel: '20/11', orderCount: 220, grossRevenue: 5940.0, contributionMarginAmount: 1722.6 },
        { period: '2026-11-21', periodLabel: '21/11', orderCount: 240, grossRevenue: 6480.0, contributionMarginAmount: 1879.2 },
        { period: '2026-11-22', periodLabel: '22/11', orderCount: 205, grossRevenue: 5535.0, contributionMarginAmount: 1605.15 },
        { period: '2026-11-23', periodLabel: '23/11', orderCount: 150, grossRevenue: 4160.0, contributionMarginAmount: 1206.4 },
      ]),
    });
  });

  await page.route('**/api/analytics/kpis/definitions', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        definitions: [
          {
            code: 'TOTAL_GROSS_REVENUE',
            name: 'Receita Bruta Total',
            formula: 'Soma do valor bruto de todos os pedidos',
            provenience: 'CONFIRMED',
            description: 'Faturamento bruto antes de taxas e deduções.',
            isEstimated: false,
          },
          {
            code: 'CONTRIBUTION_MARGIN',
            name: 'Margem de Contribuição',
            formula: 'Receita Líquida - Custos Variáveis (Tarifas + Frete + Imposto + CMV + Embalagem)',
            provenience: 'CONFIRMED',
            description: 'Resultado econômico direto gerado pelas vendas.',
            isEstimated: false,
          },
          {
            code: 'RECONCILIATION_RATE',
            name: 'Taxa de Reconciliação Contábil',
            formula: '(Pedidos Conciliados / Total de Pedidos) * 100',
            provenience: 'CONFIRMED',
            description: 'Percentual de integridade financeira de pedidos apurados.',
            isEstimated: false,
          },
        ],
        antiHallucinationDisclaimer: 'A Margem de Contribuição NÃO representa Lucro Líquido final da empresa.',
      }),
    });
  });

  await page.route('**/api/analytics/kpis/cache/invalidate', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        message: 'Cache de KPIs invalidado com sucesso para o tenant.',
      }),
    });
  });

  await page.route('**/api/catalog/listings*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [
          {
            id: 'lst-ml-001',
            sellerAccountId: 'acc-ml-01',
            externalId: 'MLB10203040',
            title: 'Teclado Mecânico Gamer RGB Switch Blue Anti-Ghosting',
            marketplace: 'MERCADO_LIVRE',
            status: 'ACTIVE',
            price: 249.9,
            currency: 'BRL',
            availableQuantity: 45,
            sku: 'TEC-MEC-RGB-01',
            listingType: 'gold_pro',
            permalink: 'https://produto.mercadolivre.com.br/MLB10203040',
            lastSyncAt: new Date().toISOString(),
          },
          {
            id: 'lst-sh-002',
            sellerAccountId: 'acc-sh-01',
            externalId: 'SHP98765432',
            title: 'Mouse Gamer Ergonômico 16000 DPI Sensor Óptico',
            marketplace: 'SHOPEE',
            status: 'ACTIVE',
            price: 159.0,
            currency: 'BRL',
            availableQuantity: 80,
            sku: 'MOU-GAM-16K-02',
            listingType: 'default',
            permalink: 'https://shopee.com.br/product/98765432',
            lastSyncAt: new Date().toISOString(),
          },
        ],
        totalElements: 2,
        totalPages: 1,
        page: 0,
        size: 10,
      }),
    });
  });

  await page.route('**/api/orders*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [
          {
            id: 'ord-001',
            marketplace: 'MERCADO_LIVRE',
            externalOrderId: '2000008899',
            buyerUsername: 'comprador_gamer',
            totalGrossAmount: 249.9,
            netAmount: 185.4,
            status: 'PAID',
            reconciliationStatus: 'CONCILIATED',
            placedAt: '2026-11-23T08:30:00Z',
            items: [
              {
                sku: 'TEC-MEC-RGB-01',
                title: 'Teclado Mecânico Gamer RGB Switch Blue',
                quantity: 1,
                unitPrice: 249.9,
              },
            ],
            financialFact: {
              marketplaceCommission: 34.99,
              fixedFee: 0.0,
              shippingCost: 20.0,
              taxAmount: 10.0,
              productCost: 80.0,
              packagingCost: 5.0,
              contributionMargin: 99.91,
              contributionMarginPercent: 39.98,
            },
          },
        ],
        totalElements: 1,
        totalPages: 1,
        page: 0,
        size: 10,
      }),
    });
  });

  await page.route('**/api/pricing/**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        sellingPrice: 200.0,
        contributionMarginAmount: 58.0,
        contributionMarginPercent: 29.0,
        markupPercent: 40.85,
        breakevenUnits: 15,
        totalDirectCost: 80.0,
        marketplaceFee: 28.0,
        shippingCost: 20.0,
        taxAmount: 14.0,
      }),
    });
  });

  await page.route('**/api/reports/exports*', async (route) => {
    if (route.request().method() === 'POST') {
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'job-exp-001',
          dataset: 'FINANCIAL_FACTS',
          status: 'COMPLETED',
          fileFormat: 'CSV',
          downloadToken: 'tok-abc-123',
          fileName: 'export-financial-facts.csv',
          totalRows: 1420,
          fileSizeBytes: 204800,
          createdAt: new Date().toISOString(),
          expiresAt: new Date(Date.now() + 3600000).toISOString(),
        }),
      });
    } else {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [
            {
              id: 'job-exp-001',
              dataset: 'FINANCIAL_FACTS',
              fileFormat: 'CSV',
              status: 'COMPLETED',
              totalRows: 1420,
              downloadToken: 'tok-abc-123',
              fileName: 'export-financial-facts.csv',
              fileSizeBytes: 204800,
              createdAt: new Date().toISOString(),
              expiresAt: new Date(Date.now() + 3600000).toISOString(),
            },
          ],
          totalElements: 1,
          totalPages: 1,
          page: 0,
          size: 50,
        }),
      });
    }
  });

  await page.route('**/api/ai/**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        answer: 'Com base no histórico recente de vendas, a sua margem de contribuição média está em 29,0%. Recomendamos revisar o custo de embalagem para melhorar em até 1,5 ponto percentual.',
        sources: ['OrderFinancialFactService', 'AggregatedKpiService'],
        calculatedAt: new Date().toISOString(),
        disclaimer: 'Estimativa baseada em dados operacionais autorizados do tenant.',
        draftTitle: 'Teclado Mecânico RGB Pro - Switch Blue Alta Precisão',
        draftDescription: 'Teclado com iluminação personalizável, cabo reforçado e resposta tátil rápida para produtividade e jogos.',
      }),
    });
  });

  await page.route('**/api/ai/drafts/rules', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          marketplace: 'MERCADO_LIVRE',
          minTitleLength: 1,
          maxTitleLength: 60,
          minDescriptionLength: 1,
          maxDescriptionLength: 5000,
          plainTextOnly: true,
          officialRuleSource: 'Manual Oficial Mercado Livre 2026',
        },
        {
          marketplace: 'SHOPEE',
          minTitleLength: 1,
          maxTitleLength: 120,
          minDescriptionLength: 1,
          maxDescriptionLength: 3000,
          plainTextOnly: true,
          officialRuleSource: 'Diretrizes Shopee Brasil 2026',
        },
      ]),
    });
  });

  await page.route('**/api/ai/drafts/draft-001', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: 'draft-001',
        tenantId: 'tenant-alpha-001',
        productId: 'prod-001',
        productVariantId: null,
        targetMarketplace: 'MERCADO_LIVRE',
        currentVersionNumber: 1,
        status: 'DRAFT',
        publicationStatus: 'DRAFT_LOCAL_ONLY',
        externalPublishBlocked: true,
        currentVersion: {
          id: 'ver-001',
          draftListingId: 'draft-001',
          versionNumber: 1,
          promptTemplateId: 'tmpl-listing-v1',
          promptTemplateVersion: '1.0.0',
          aiModelName: 'gemini-1.5-flash',
          aiProvider: 'GOOGLE',
          tone: 'INFORMATIVE_OBJECTIVE',
          targetMarketplace: 'MERCADO_LIVRE',
          inputAttributesJson: '{"brand":"AlphaTech","category":"Eletrônicos"}',
          generatedTitle: 'Teclado Mecânico RGB Pro Switch Blue',
          generatedDescription: 'Teclado mecânico de alta precisão com switches azuis táteis.',
          reviewedTitle: null,
          reviewedDescription: null,
          effectiveTitle: 'Teclado Mecânico RGB Pro Switch Blue',
          effectiveDescription: 'Teclado mecânico de alta precisão com switches azuis táteis.',
          traceableAttributesJson: '{"brand":"AlphaTech"}',
          unprovenClaimsJson: null,
          titleCharCount: 36,
          descriptionCharCount: 61,
          characterLimitValid: true,
          userEdited: false,
          reviewNotes: null,
          createdAt: new Date().toISOString(),
        },
        versionHistory: [],
        marketplaceLimits: {
          marketplace: 'MERCADO_LIVRE',
          minTitleLength: 1,
          maxTitleLength: 60,
          minDescriptionLength: 1,
          maxDescriptionLength: 5000,
          plainTextOnly: true,
          officialRuleSource: 'Manual Oficial Mercado Livre 2026',
        },
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }),
    });
  });

  await page.route('**/api/ai/drafts*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [
          {
            id: 'draft-001',
            tenantId: 'tenant-alpha-001',
            productId: 'prod-001',
            productVariantId: null,
            targetMarketplace: 'MERCADO_LIVRE',
            currentVersionNumber: 1,
            status: 'DRAFT',
            publicationStatus: 'DRAFT_LOCAL_ONLY',
            externalPublishBlocked: true,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          },
        ],
        totalElements: 1,
        totalPages: 1,
      }),
    });
  });

  await page.route('**/api/companies*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          id: 'comp-01',
          tradeName: 'Alpha Tech Comércio Ltda',
          legalName: 'Alpha Tech Comércio de Eletrônicos Ltda',
          cnpj: '99888777000166',
          taxRegime: 'SIMPLES_NACIONAL',
          status: 'ACTIVE',
        },
      ]),
    });
  });

  await page.route('**/api/integrations/**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          id: 'acc-ml-01',
          marketplace: 'MERCADO_LIVRE',
          accountName: 'Alpha Store ML',
          status: 'ACTIVE',
          connectedAt: '2026-11-20T10:00:00Z',
          expiresAt: '2027-11-20T10:00:00Z',
        },
      ]),
    });
  });
}
