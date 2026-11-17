import { describe, it, expect } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import React from "react";
import { CompetitorMonitoringView } from "./CompetitorMonitoringView";

describe("CompetitorMonitoringView Component", () => {
  it("renders header, badges and initial watchlists", () => {
    render(<CompetitorMonitoringView />);

    expect(
      screen.getByRole("heading", {
        name: /Monitoramento Contínuo de Produtos & Concorrentes/i,
      })
    ).toBeDefined();

    expect(screen.getByText("Proveniência OBSERVED")).toBeDefined();
    expect(screen.getByText("Deduplicação Determinística")).toBeDefined();
    expect(screen.getByText("Retenção 12 Meses (ADR DEC-019)")).toBeDefined();

    expect(screen.getByText("Concorrentes — Teclados Mecânicos (COMPETITOR_LISTING)")).toBeDefined();
    expect(screen.getByText("Teclado Mecânico RGB Switch Blue Redragon")).toBeDefined();
    expect(screen.getByText("MLB348912384")).toBeDefined();
  });

  it("navigates to Timeline subtab and displays explicit GAP point without interpolated price", () => {
    render(<CompetitorMonitoringView />);

    const timelineTabBtn = screen.getByRole("button", { name: /Histórico & Lacunas/i });
    fireEvent.click(timelineTabBtn);

    expect(screen.getByText(/Taxa de Cobertura de Coleta/i)).toBeDefined();
    expect(screen.getByText(/Diretriz Anti-Alucinação \(ADR DEC-019\)/i)).toBeDefined();

    // Verify explicit GAP badge
    expect(screen.getByText("LACUNA (GAP)")).toBeDefined();
    expect(screen.getByText(/Preço Ausente \(Não Interpolado\)/i)).toBeDefined();
  });

  it("navigates to Comparator subtab and verifies price position and unavailable fields disclaimer", () => {
    render(<CompetitorMonitoringView />);

    const comparatorTabBtn = screen.getByRole("button", { name: /Comparador Anúncio vs Concorrente/i });
    fireEvent.click(comparatorTabBtn);

    expect(
      screen.getByRole("heading", {
        name: /Comparador Lado a Lado: Meu Anúncio vs Concorrente/i,
      })
    ).toBeDefined();

    expect(screen.getByText("Posição de Preço:")).toBeDefined();
    expect(screen.getByText(/Seu produto está R\$ 14,90 mais barato/i)).toBeDefined();

    // Strict Anti-Hallucination verification: private third-party fields are explicitly marked unavailable
    expect(screen.getByText(/Transparência de Dados Privados \(Regra Anti-Alucinação\):/i)).toBeDefined();
    expect(screen.getByText(/Estoque real do concorrente não fornecido pela API \(campo privado \/ UNAVAILABLE\)/i)).toBeDefined();
  });

  it("navigates to Alerts subtab and marks alert as read", () => {
    render(<CompetitorMonitoringView />);

    const alertsTabBtn = screen.getByRole("button", { name: /Regras & Alertas/i });
    fireEvent.click(alertsTabBtn);

    expect(screen.getByText(/Alerta de Queda de Preço Concorrente/i)).toBeDefined();
    expect(screen.getByText(/Queda de Preço Detectada: Teclado Mecânico/i)).toBeDefined();

    const markReadBtn = screen.getByRole("button", { name: /Marcar como Lido/i });
    fireEvent.click(markReadBtn);

    // The Mark as Read button disappears once read
    expect(screen.queryByRole("button", { name: /Marcar como Lido/i })).toBeNull();
  });

  it("untracks an item without deleting it from the table", () => {
    render(<CompetitorMonitoringView />);

    const untrackButtons = screen.getAllByRole("button", { name: "Desativar" });
    fireEvent.click(untrackButtons[0]);

    // Item status changes to UNTRACKED, entity remains in DOM
    expect(screen.getByText("UNTRACKED")).toBeDefined();
    expect(screen.getByText("Teclado Mecânico RGB Switch Blue Redragon")).toBeDefined();
  });
});
