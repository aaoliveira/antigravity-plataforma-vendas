import React from "react";
import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { FinancialDashboardView } from "./FinancialDashboardView";

describe("FinancialDashboardView Component", () => {
  it("renders executive KPI cards, coverage banner and filter controls", () => {
    render(<FinancialDashboardView />);

    expect(screen.getByText("Painel Financeiro & Rentabilidade")).toBeDefined();
    expect(screen.getAllByText("Faturamento Bruto").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Total de Pedidos").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Ticket Médio").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Margem de Contribuição").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText(/Regime de Competência/i).length).toBeGreaterThanOrEqual(1);
    expect(screen.getByLabelText("Período")).toBeDefined();
    expect(screen.getByLabelText("Canal / Marketplace")).toBeDefined();
  });

  it("switches sections between Overview, Products, and Orders", () => {
    render(<FinancialDashboardView />);

    const productsTab = screen.getByRole("button", { name: /Rentabilidade por Produto/i });
    fireEvent.click(productsTab);
    expect(screen.getAllByText("Rentabilidade por Produto / SKU").length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("SKU-NOTEBOOK-G15")).toBeDefined();

    const ordersTab = screen.getByRole("button", { name: /Pedidos & Memória de Cálculo/i });
    fireEvent.click(ordersTab);
    expect(screen.getByText(/Pedidos Faturados & Memória de Cálculo/i)).toBeDefined();
    expect(screen.getByText("MLB2000984711")).toBeDefined();
  });

  it("opens and closes drill-down modal for calculation memory", () => {
    render(<FinancialDashboardView />);

    const ordersTab = screen.getByRole("button", { name: /Pedidos & Memória de Cálculo/i });
    fireEvent.click(ordersTab);

    const memoryButtons = screen.getAllByRole("button", { name: /Ver Memória/i });
    expect(memoryButtons.length).toBeGreaterThan(0);
    fireEvent.click(memoryButtons[0]);

    expect(screen.getByText(/Passo a Passo das Deduções:/i)).toBeDefined();
    expect(screen.getByText(/Receita Bruta Faturada:/i)).toBeDefined();

    const closeButton = screen.getByRole("button", { name: "Fechar" });
    fireEvent.click(closeButton);

    expect(screen.queryByText(/Passo a Passo das Deduções:/i)).toBeNull();
  });

  it("triggers CSV export and presents success notification", () => {
    // Mock URL.createObjectURL and URL.revokeObjectURL
    const originalCreateObjectURL = window.URL.createObjectURL;
    const originalRevokeObjectURL = window.URL.revokeObjectURL;
    const originalAnchorClick = HTMLAnchorElement.prototype.click;

    window.URL.createObjectURL = vi.fn(() => "blob:http://localhost/dummy");
    window.URL.revokeObjectURL = vi.fn();
    HTMLAnchorElement.prototype.click = vi.fn();

    render(<FinancialDashboardView />);

    const exportBtn = screen.getByRole("button", { name: /Exportar CSV/i });
    fireEvent.click(exportBtn);

    expect(screen.getByText(/Relatório CSV gerado e baixado com sucesso/i)).toBeDefined();
    expect(HTMLAnchorElement.prototype.click).toHaveBeenCalled();

    window.URL.createObjectURL = originalCreateObjectURL;
    window.URL.revokeObjectURL = originalRevokeObjectURL;
    HTMLAnchorElement.prototype.click = originalAnchorClick;
  });
});
