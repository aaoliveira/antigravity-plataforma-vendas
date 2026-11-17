import React from "react";
import { describe, it, expect } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { MarketIntelligenceView } from "./MarketIntelligenceView";

describe("MarketIntelligenceView Component", () => {
  it("renders search controls, official filters and initial results", () => {
    render(<MarketIntelligenceView />);

    expect(screen.getByText("Market Intelligence: Pesquisa de Mercado")).toBeDefined();
    expect(screen.getByPlaceholderText(/Ex: teclado mecanico, fone bluetooth/i)).toBeDefined();
    expect(screen.getByText("Filtros Oficiais:")).toBeDefined();
    expect(screen.getAllByText(/OBSERVED/i).length).toBeGreaterThanOrEqual(1);
  });

  it("triggers search and updates result list with observed items", async () => {
    render(<MarketIntelligenceView />);

    const searchInput = screen.getByPlaceholderText(/Ex: teclado mecanico, fone bluetooth/i);
    fireEvent.change(searchInput, { target: { value: "mouse gamer" } });

    const searchButton = screen.getByRole("button", { name: "Pesquisar" });
    fireEvent.click(searchButton);

    await waitFor(() => {
      expect(screen.getByText(/Resultados para:/i)).toBeDefined();
    }, { timeout: 3000 });
  });

  it("displays anti-scraping notice when Shopee is selected", () => {
    render(<MarketIntelligenceView />);

    const marketplaceSelect = screen.getByLabelText("Marketplace");
    fireEvent.change(marketplaceSelect, { target: { value: "SHOPEE" } });

    expect(screen.getByText(/Busca Pública Não Disponível na API Oficial da Shopee/i)).toBeDefined();
    expect(screen.getByText(/é expressamente proibido realizar scraping não autorizado/i)).toBeDefined();
  });

  it("switches tabs between public search, saved searches, and favorites", () => {
    render(<MarketIntelligenceView />);

    const savedTab = screen.getByRole("button", { name: /Buscas Salvas/i });
    fireEvent.click(savedTab);
    expect(screen.getByText("Buscas Salvas & Listas de Monitoramento")).toBeDefined();

    const favTab = screen.getByRole("button", { name: /Produtos & Oportunidades/i });
    fireEvent.click(favTab);
    expect(screen.getByText("Produtos de Mercado Favoritados")).toBeDefined();
  });
});
