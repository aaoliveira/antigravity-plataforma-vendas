import React from "react";
import { describe, it, expect } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { FinancialEngineView } from "./FinancialEngineView";

describe("FinancialEngineView Component", () => {
  it("renders financial engine header, KPI cards and simulation form", () => {
    render(<FinancialEngineView />);

    expect(screen.getByText("Motor Financeiro Determinístico")).toBeDefined();
    expect(screen.getByText(/Precisão Decimal \(BigDecimal\)/i)).toBeDefined();
    expect(screen.getByText("Preço Mínimo (Break-even)")).toBeDefined();
    expect(screen.getByText("Preço Alvo Sugerido")).toBeDefined();
    expect(screen.getByText("Memória de Cálculo Explicável (Passo a Passo)")).toBeDefined();
  });

  it("calculates margin, break-even price and updates memory breakdown table", () => {
    render(<FinancialEngineView />);

    const cogsInput = screen.getByLabelText(/Custo de Aquisição/i);
    fireEvent.change(cogsInput, { target: { value: "60.00" } });

    const priceInput = screen.getByLabelText(/Preço de Venda Simulado/i);
    fireEvent.change(priceInput, { target: { value: "150.00" } });

    const calculateButton = screen.getByRole("button", { name: "Calcular Margem e Preço" });
    fireEvent.click(calculateButton);

    expect(screen.getByText("RECEITA_BRUTA")).toBeDefined();
    expect(screen.getByText("CUSTO_AQUISICAO_COGS")).toBeDefined();
    expect(screen.getByText("MARGEM_CONTRIBUICAO_LIQUIDA")).toBeDefined();
    expect(screen.getByText("CÁLCULO COMPLETO")).toBeDefined();
  });

  it("displays partial warning when critical data such as tax or COGS is missing", () => {
    render(<FinancialEngineView />);

    const taxInput = screen.getByLabelText(/Imposto \(%\)/i);
    fireEvent.change(taxInput, { target: { value: "" } });

    const calculateButton = screen.getByRole("button", { name: "Calcular Margem e Preço" });
    fireEvent.click(calculateButton);

    expect(screen.getByText("CÁLCULO PARCIAL")).toBeDefined();
    expect(screen.getByText(/Aviso de Cálculo Parcial/i)).toBeDefined();
    expect(screen.getByText(/Não representa lucro líquido real nem apuração fiscal oficial/i)).toBeDefined();
  });

  it("switches tabs between Simulator, Financial Rules, and SKU Costs", () => {
    render(<FinancialEngineView />);

    const rulesTab = screen.getByRole("button", { name: /Regras Financeiras do Canal/i });
    fireEvent.click(rulesTab);
    expect(screen.getByText("Regras e Componentes Versionados")).toBeDefined();
    expect(screen.getAllByText("Regra Geral Mercado Livre (Clássico)").length).toBeGreaterThanOrEqual(1);

    const costsTab = screen.getByRole("button", { name: /Custos de SKU e Vigência/i });
    fireEvent.click(costsTab);
    expect(screen.getByText("Custos de SKU com Vigência")).toBeDefined();
    expect(screen.getAllByText("SKU-NOTEBOOK-G15").length).toBeGreaterThanOrEqual(1);
  });
});
