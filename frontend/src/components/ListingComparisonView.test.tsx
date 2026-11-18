import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { ListingComparisonView } from './ListingComparisonView';

describe('ListingComparisonView', () => {
  it('renderiza matriz de comparação lado a lado com item base e candidatos de mercado', () => {
    render(<ListingComparisonView />);

    expect(screen.getByText('⚖️ Comparação Lado a Lado de Listings Equivalentes')).toBeInTheDocument();
    expect(screen.getByText('🎯 REFERÊNCIA PRÓPRIA')).toBeInTheDocument();
    expect(screen.getByText(/Smartphone Samsung Galaxy S23 5G 128GB Preto/i)).toBeInTheDocument();
    expect(screen.getByText('Samsung Galaxy S23 128GB 5G Preto Original Lacrado')).toBeInTheDocument();
    expect(screen.getByText('Samsung Galaxy S23 5G 256GB Preto 8GB RAM Lacrado')).toBeInTheDocument();
    expect(screen.getByText(/Galaxy S23 128GB Preto Usado Poucas Marcas/i)).toBeInTheDocument();
  });

  it('exibe diferenças claras de atributos com destaque de idêntico vs divergente', () => {
    render(<ListingComparisonView />);

    // Atributos idênticos do anúncio equivalente
    const identicos = screen.getAllByText('✓ Idêntico');
    expect(identicos.length).toBeGreaterThan(0);

    // Atributos divergentes de anúncios de variantes ou outras marcas
    const divergentes = screen.getAllByText('⚠️ Divergente');
    expect(divergentes.length).toBeGreaterThan(0);

    // Condição Novo vs Usado destacada
    expect(screen.getByText('Usado')).toBeInTheDocument();
  });

  it('destaca baixa confiança e itens borderline com badges apropriados', () => {
    render(<ListingComparisonView />);

    // Item de alta confiança
    expect(screen.getByText('✅ EQUIVALENTE (ALTA CONF.)')).toBeInTheDocument();

    // Item de revisão / borderline (capacidade superior 256GB)
    expect(screen.getByText('⚠️ NÃO EQUIVALENTE (REVISÃO)')).toBeInTheDocument();

    // Score visível na tabela
    expect(screen.getAllByText(/62\.5%/).length).toBeGreaterThan(0);
  });

  it('cumpre a regra anti-falsa equivalência: item incompatível NUNCA recebe badge de equivalente e exibe alerta impeditivo', () => {
    render(<ListingComparisonView />);

    // Não deve haver mais de 1 anúncio equivalente (apenas o legítimo de alta confiança)
    const badgesEquivalente = screen.getAllByText('✅ EQUIVALENTE (ALTA CONF.)');
    expect(badgesEquivalente).toHaveLength(1);

    // Anúncio usado e anúncio de marca incompatível devem receber badge INCOMPATÍVEL
    const badgesIncompativel = screen.getAllByText('⛔ INCOMPATÍVEL');
    expect(badgesIncompativel.length).toBeGreaterThanOrEqual(1);

    // Banner de alerta anti-falsa equivalência exibido
    const alertas = screen.getAllByText(/ALERTA ANTI-FALSA EQUIVALÊNCIA/i);
    expect(alertas.length).toBeGreaterThanOrEqual(1);
    expect(
      screen.getByText(/Condições de item incompatíveis: Base é NOVO \(NEW\), mas anúncio concorrente é USADO/i)
    ).toBeInTheDocument();
  });

  it('respeita integridade temporal: oculta variação de preço para observações com intervalo > 30 dias', () => {
    render(<ListingComparisonView />);

    // O anúncio do iPhone 13 tem 48 dias de defasagem (> 30 dias)
    expect(screen.getByText(/Janela > 30d \(Variação oculta\)/i)).toBeInTheDocument();
    expect(screen.getByText(/Defasagem: 48 dias/i)).toBeInTheDocument();
  });

  it('não inventa preço total quando frete não é informado pelo canal (UNKNOWN)', () => {
    render(<ListingComparisonView />);

    // O anúncio usado possui frete desconhecido
    expect(screen.getByText('⚠️ Não informado pelo canal')).toBeInTheDocument();
    expect(screen.getByText('-- (Frete desconhecido não inferido)')).toBeInTheDocument();
  });

  it('filtra corretamente por nível de confiança e compatibilidade', () => {
    render(<ListingComparisonView />);

    // Filtra apenas alta confiança
    const confidenceSelect = screen.getByLabelText('Nível de Confiança');
    fireEvent.change(confidenceSelect, { target: { value: 'HIGH' } });

    // Apenas o anúncio de alta confiança deve permanecer na tabela
    expect(screen.getByText('Samsung Galaxy S23 128GB 5G Preto Original Lacrado')).toBeInTheDocument();
    expect(screen.queryByText('Samsung Galaxy S23 5G 256GB Preto 8GB RAM Lacrado')).not.toBeInTheDocument();
    expect(screen.queryByText(/Galaxy S23 128GB Preto Usado/i)).not.toBeInTheDocument();

    // Filtra apenas incompatíveis
    fireEvent.change(confidenceSelect, { target: { value: 'ALL' } });
    const compatibilitySelect = screen.getByLabelText('Status de Compatibilidade');
    fireEvent.change(compatibilitySelect, { target: { value: 'INCOMPATIBLE_ONLY' } });

    expect(screen.queryByText('Samsung Galaxy S23 128GB 5G Preto Original Lacrado')).not.toBeInTheDocument();
    expect(screen.getByText(/Galaxy S23 128GB Preto Usado/i)).toBeInTheDocument();
  });

  it('permite alternar entre visualização em matriz e cards de análise', () => {
    render(<ListingComparisonView />);

    const cardsButton = screen.getByRole('button', { name: /Cards de Análise/i });
    fireEvent.click(cardsButton);

    // Verifica elementos do modo cards
    expect(screen.getAllByText(/Preço Concorrente:/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Preço de Referência:/i).length).toBeGreaterThan(0);
    expect(screen.getByText(/Score: 62\.5%/i)).toBeInTheDocument();

    const matrixButton = screen.getByRole('button', { name: /Matriz Lado a Lado/i });
    fireEvent.click(matrixButton);

    // Retorna para tabela matriz
    expect(screen.getByText('Critério / Atributo')).toBeInTheDocument();
  });

  it('abre e fecha modal de explicabilidade do score com decomposição de pesos', () => {
    render(<ListingComparisonView />);

    const btnVerScore = screen.getAllByRole('button', { name: /Ver Score/i })[0];
    fireEvent.click(btnVerScore);

    // Modal aberto
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText('Explicabilidade do Score de Equivalência')).toBeInTheDocument();
    expect(screen.getByText(/GTIN \/ Código de Barras \(Peso: 35%\)/i)).toBeInTheDocument();
    expect(screen.getByText(/Marca do Fabricante \(Peso: 20%\)/i)).toBeInTheDocument();

    // Fecha modal
    const btnFechar = screen.getByRole('button', { name: 'Fechar Detalhes' });
    fireEvent.click(btnFechar);

    expect(screen.queryByText('Explicabilidade do Score de Equivalência')).not.toBeInTheDocument();
  });

  it('abre drawer de histórico de preços auditável sem retroatividade fictícia', () => {
    render(<ListingComparisonView />);

    const btnHistorico = screen.getAllByRole('button', { name: /Histórico/i })[0];
    fireEvent.click(btnHistorico);

    // Drawer de histórico aberto
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText('Histórico Legítimo de Preços')).toBeInTheDocument();
    expect(screen.getByText(/Histórico sem retroatividade inventada/i)).toBeInTheDocument();
    expect(screen.getByText('Pontos de Observação Coletados (3)')).toBeInTheDocument();

    // Fecha com ESC
    fireEvent.keyDown(window, { key: 'Escape' });
    expect(screen.queryByText('Histórico Legítimo de Preços')).not.toBeInTheDocument();
  });

  it('executa callback onOpenSearch se fornecido', () => {
    const handleOpenSearch = vi.fn();
    render(<ListingComparisonView onOpenSearch={handleOpenSearch} />);

    const searchBtn = screen.getByRole('button', { name: /Buscar Novos Candidatos/i });
    fireEvent.click(searchBtn);

    expect(handleOpenSearch).toHaveBeenCalledTimes(1);
  });
});
