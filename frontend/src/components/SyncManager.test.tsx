import React from "react";
import { describe, it, expect } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { SyncManager } from "./SyncManager";

describe("SyncManager Component", () => {
  it("renders overview metrics, jobs history and CSV contingency section", () => {
    render(<SyncManager />);

    expect(screen.getByText("Central de Sincronização & Catálogo")).toBeDefined();
    expect(screen.getByText("Produtos / SKUs")).toBeDefined();
    expect(screen.getByText("Anúncios Sincronizados")).toBeDefined();
    expect(screen.getAllByText("Pedidos Próprios").length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("Histórico de Jobs de Sincronização")).toBeDefined();
    expect(screen.getByText("Importação de Contingência por Arquivo (CSV)")).toBeDefined();
  });

  it("triggers manual sync and displays success message", async () => {
    render(<SyncManager />);

    const syncButton = screen.getByRole("button", { name: /Sincronizar Agora/i });
    fireEvent.click(syncButton);

    await waitFor(() => {
      expect(screen.getByText(/Sincronização manual de CATALOG concluída com sucesso!/i)).toBeDefined();
    }, { timeout: 3000 });
  });

  it("provides buttons to download catalog and orders templates", () => {
    render(<SyncManager />);

    const catalogBtn = screen.getByRole("button", { name: /Baixar Template Catálogo/i });
    const ordersBtn = screen.getByRole("button", { name: /Baixar Template Pedidos/i });

    expect(catalogBtn).toBeDefined();
    expect(ordersBtn).toBeDefined();
  });
});
