import React, { useEffect, useState } from 'react';
import { ParsedProductPage, BackendStatus } from '../types';
import { parseMarketplaceUrl } from '../parsers/marketplaceUrlParser';

export function Popup() {
  const [loading, setLoading] = useState<boolean>(true);
  const [parsedPage, setParsedPage] = useState<ParsedProductPage | null>(null);
  const [backendStatus, setBackendStatus] = useState<BackendStatus>({
    online: true,
    platformUrl: 'http://localhost:3000',
    authenticated: true,
  });
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    // 1. Inspeciona a aba ativa atual usando activeTab
    if (typeof chrome !== 'undefined' && chrome.tabs && chrome.tabs.query) {
      chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
        if (tabs && tabs.length > 0 && tabs[0].url) {
          const currentUrl = tabs[0].url;
          const currentTitle = tabs[0].title;
          const parsed = parseMarketplaceUrl(currentUrl, currentTitle);
          setParsedPage(parsed);
        } else {
          setParsedPage({
            supported: false,
            marketplace: 'UNKNOWN',
            externalItemId: null,
            canonicalUrl: '',
            title: null,
            reason: 'Nenhuma aba ativa acessível detectada.',
          });
        }
        setLoading(false);
      });

      // 2. Consulta o status da plataforma via runtime message
      if (chrome.runtime && chrome.runtime.sendMessage) {
        chrome.runtime.sendMessage({ type: 'CHECK_BACKEND_STATUS' }, (res) => {
          if (chrome.runtime.lastError) {
            // Em caso de falha silenciosa, mantém fallback local
            return;
          }
          if (res) {
            setBackendStatus(res);
          }
        });
      }
    } else {
      // Mock para desenvolvimento / testes fora do browser Chromium
      setParsedPage({
        supported: true,
        marketplace: 'MERCADO_LIVRE',
        externalItemId: 'MLB2000984711',
        canonicalUrl: 'https://produto.mercadolivre.com.br/MLB-2000984711-teclado-mecanico',
        title: 'Teclado Mecânico Gamer RGB Switch Blue',
      });
      setLoading(false);
    }
  }, []);

  const handleOpenAiStudio = () => {
    if (!parsedPage || !parsedPage.externalItemId) return;
    setStatusMessage('Abrindo anúncio no AI Listing Studio...');
    setErrorMessage(null);

    const path = `/?tab=AI_STUDIO&externalId=${encodeURIComponent(parsedPage.externalItemId)}`;
    openUrl(path);
  };

  const handleOpenMarketIntelligence = () => {
    if (!parsedPage || !parsedPage.externalItemId) return;
    setStatusMessage('Abrindo pesquisa de concorrência na plataforma...');
    setErrorMessage(null);

    const query = parsedPage.title || parsedPage.externalItemId;
    const path = `/?tab=MARKET&query=${encodeURIComponent(query)}`;
    openUrl(path);
  };

  const handleOpenPlatform = () => {
    openUrl('/');
  };

  const openUrl = (path: string) => {
    if (typeof chrome !== 'undefined' && chrome.runtime && chrome.runtime.sendMessage) {
      chrome.runtime.sendMessage(
        { type: 'OPEN_PLATFORM_TAB', payload: { path } },
        () => {
          if (chrome.runtime.lastError) {
            setErrorMessage('Não foi possível comunicar com o service worker da extensão.');
          }
        }
      );
    } else if (typeof window !== 'undefined') {
      window.open(`http://localhost:3000${path}`, '_blank');
    }
  };

  return (
    <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '14px' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #e2e8f0', paddingBottom: '10px' }}>
        <div>
          <h1 style={{ fontSize: '15px', fontWeight: 'bold', margin: 0, color: '#0f172a' }}>
            Seller Intelligence
          </h1>
          <span style={{ fontSize: '11px', color: '#64748b' }}>Assistente Contextual v1.0.0</span>
        </div>

        {/* Connection status pill */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
          <span
            style={{
              width: '8px',
              height: '8px',
              borderRadius: '50%',
              backgroundColor: backendStatus.online ? '#10b981' : '#94a3b8',
              display: 'inline-block',
            }}
          />
          <span style={{ fontSize: '11px', fontWeight: 600, color: backendStatus.online ? '#065f46' : '#64748b' }}>
            {backendStatus.online ? 'Online' : 'Offline'}
          </span>
        </div>
      </div>

      {/* Notifications */}
      {statusMessage && (
        <div role="status" style={{ backgroundColor: '#f0fdf4', borderLeft: '3px solid #22c55e', padding: '8px 10px', fontSize: '12px', color: '#166534', borderRadius: '4px' }}>
          {statusMessage}
        </div>
      )}
      {errorMessage && (
        <div role="alert" style={{ backgroundColor: '#fef2f2', borderLeft: '3px solid #ef4444', padding: '8px 10px', fontSize: '12px', color: '#991b1b', borderRadius: '4px' }}>
          {errorMessage}
        </div>
      )}

      {/* Main Inspection Card */}
      {loading ? (
        <div style={{ fontSize: '13px', color: '#64748b', textAlign: 'center', padding: '20px 0' }}>
          Analisando aba ativa...
        </div>
      ) : parsedPage?.supported ? (
        <div style={{ backgroundColor: '#ffffff', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '14px', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <span
              style={{
                fontSize: '11px',
                fontWeight: 'bold',
                padding: '2px 8px',
                borderRadius: '4px',
                backgroundColor: parsedPage.marketplace === 'MERCADO_LIVRE' ? '#fef08a' : '#ffedd5',
                color: parsedPage.marketplace === 'MERCADO_LIVRE' ? '#854d0e' : '#9a3412',
              }}
            >
              {parsedPage.marketplace === 'MERCADO_LIVRE' ? 'Mercado Livre' : 'Shopee'}
            </span>
            <span style={{ fontSize: '11px', fontFamily: 'monospace', color: '#475569', fontWeight: 600 }}>
              {parsedPage.externalItemId}
            </span>
          </div>

          <p style={{ fontSize: '13px', fontWeight: 600, margin: '6px 0 12px 0', color: '#1e293b', lineHeight: 1.3 }}>
            {parsedPage.title || 'Anúncio de Produto'}
          </p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            <button
              onClick={handleOpenAiStudio}
              style={{
                backgroundColor: '#7c3aed',
                color: '#ffffff',
                border: 'none',
                borderRadius: '6px',
                padding: '8px 12px',
                fontSize: '12px',
                fontWeight: 600,
                cursor: 'pointer',
                textAlign: 'center',
              }}
            >
              Otimizar no AI Listing Studio
            </button>

            <button
              onClick={handleOpenMarketIntelligence}
              style={{
                backgroundColor: '#0284c7',
                color: '#ffffff',
                border: 'none',
                borderRadius: '6px',
                padding: '8px 12px',
                fontSize: '12px',
                fontWeight: 600,
                cursor: 'pointer',
                textAlign: 'center',
              }}
            >
              Pesquisar Concorrência
            </button>
          </div>
        </div>
      ) : (
        <div style={{ backgroundColor: '#ffffff', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '14px', textAlign: 'center' }}>
          <p style={{ fontSize: '13px', fontWeight: 600, color: '#334155', margin: '0 0 6px 0' }}>
            Nenhum produto detectado
          </p>
          <p style={{ fontSize: '12px', color: '#64748b', margin: '0 0 12px 0', lineHeight: 1.4 }}>
            {parsedPage?.reason || 'Abra uma página de anúncio no Mercado Livre ou Shopee para inspecionar com um clique.'}
          </p>
          <button
            onClick={handleOpenPlatform}
            style={{
              backgroundColor: '#0f172a',
              color: '#ffffff',
              border: 'none',
              borderRadius: '6px',
              padding: '8px 12px',
              fontSize: '12px',
              fontWeight: 600,
              cursor: 'pointer',
              width: '100%',
            }}
          >
            Abrir Plataforma Web
          </button>
        </div>
      )}

      {/* Security & Zero-Scraping Guarantee */}
      <div style={{ backgroundColor: '#f8fafc', border: '1px dashed #cbd5e1', borderRadius: '6px', padding: '10px', fontSize: '11px', color: '#64748b', lineHeight: 1.4 }}>
        <strong style={{ color: '#334155' }}>Garantia de Privacidade:</strong> Esta extensão utiliza apenas a permissão <code>activeTab</code> sob sua ação direta. Nenhum cookie de sessão do marketplace é acessado e nenhuma automação invasiva de compra ou preço é realizada.
      </div>
    </div>
  );
}
