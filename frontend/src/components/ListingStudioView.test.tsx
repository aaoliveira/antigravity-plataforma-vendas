import React from "react";
import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { ListingStudioView } from "./ListingStudioView";

describe("ListingStudioView Component", () => {
  it("renders quality scorecard, 4 pillars and factual guardrail information gaps", () => {
    render(<ListingStudioView />);

    expect(screen.getByText(/AI Listing Studio — Análise & Otimização/i)).toBeDefined();
    expect(screen.getByText("Score de Qualidade do Anúncio")).toBeDefined();
    expect(screen.getByText("78")).toBeDefined();
    expect(screen.getByText("Título")).toBeDefined();
    expect(screen.getByText("Descrição")).toBeDefined();
    expect(screen.getByText("Ficha Técnica")).toBeDefined();
    expect(screen.getByText("Fotos / Galeria")).toBeDefined();
    expect(screen.getByText(/Lacunas Identificadas no Cadastro:/i)).toBeDefined();
    expect(screen.getByText(/Factual Guardrail Ativo/i)).toBeDefined();
  });

  it("generates AI assisted title, displays AI suggestion badge and updates editable input", async () => {
    render(<ListingStudioView />);

    const generateTitleBtn = screen.getByRole("button", { name: /Otimizar Título com IA/i });
    fireEvent.click(generateTitleBtn);

    await waitFor(() => {
      expect(screen.getByText(/SUGESTÃO IA \(Prompt v1.0.0\)/i)).toBeDefined();
      expect(screen.getByText(/Justificativa da IA:/i)).toBeDefined();
    });

    const titleInput = screen.getByLabelText(/Título do Anúncio \(Editável\)/i) as HTMLInputElement;
    expect(titleInput.value).toContain("Dell");
  });

  it("generates structured technical description with sections and confirmed attributes", async () => {
    render(<ListingStudioView />);

    const generateDescBtn = screen.getByRole("button", { name: /Gerar Descrição Técnica/i });
    fireEvent.click(generateDescBtn);

    await waitFor(() => {
      expect(screen.getByText(/SUGESTÃO IA \(Prompt v1.0.0\)/i)).toBeDefined();
      expect(screen.getAllByText(/## Apresentação do Produto/i).length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText(/## Ficha Técnica Confirmada/i).length).toBeGreaterThanOrEqual(1);
    });
  });

  it("saves a draft version and updates version history without publishing", async () => {
    render(<ListingStudioView />);

    const titleInput = screen.getByLabelText(/Título do Anúncio \(Editável\)/i);
    fireEvent.change(titleInput, { target: { value: "Monitor Gamer Dell 27 4K - Versão Customizada Humana" } });

    const saveDraftBtn = screen.getByRole("button", { name: /Salvar Rascunho da Versão/i });
    fireEvent.click(saveDraftBtn);

    expect(screen.getByText(/Rascunho da Versão 2 salvo com sucesso!/i)).toBeDefined();
    expect(screen.getByText("v2")).toBeDefined();
    expect(screen.getByText("Monitor Gamer Dell 27 4K - Versão Customizada Humana")).toBeDefined();
  });

  it("simulates AI provider failure and presents explicit error alert without fabricated text", async () => {
    render(<ListingStudioView />);

    const failBtn = screen.getByRole("button", { name: /Simular Falha de Provedor/i });
    fireEvent.click(failBtn);

    await waitFor(() => {
      expect(screen.getByRole("alert")).toBeDefined();
      expect(screen.getByText(/Serviço de IA temporariamente indisponível/i)).toBeDefined();
    });
  });
});
