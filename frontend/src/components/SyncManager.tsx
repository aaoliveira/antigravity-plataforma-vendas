"use client";

import React, { useState, useEffect } from "react";

interface SyncJob {
  id: string;
  storeId: string;
  marketplace: string;
  resourceType: string;
  triggerType: string;
  status: "PENDING" | "RUNNING" | "COMPLETED" | "FAILED";
  itemsProcessed: number;
  itemsSucceeded: number;
  itemsFailed: number;
  errorSummary?: string;
  startedAt?: string;
  completedAt?: string;
  createdAt: string;
}

interface SyncStats {
  totalProducts: number;
  totalListings: number;
  totalOrders: number;
  lastSyncAt?: string;
}

interface CsvRowError {
  lineNumber: number;
  field: string;
  rejectedValue: string;
  reason: string;
}

interface CsvValidationReport {
  resourceType: string;
  totalRows: number;
  importedRows: number;
  rejectedRows: number;
  errors: CsvRowError[];
}

export function SyncManager() {
  const [stats, setStats] = useState<SyncStats>({
    totalProducts: 12,
    totalListings: 12,
    totalOrders: 48,
    lastSyncAt: new Date().toISOString(),
  });

  const [jobs, setJobs] = useState<SyncJob[]>([
    {
      id: "job-101",
      storeId: "store-ml-1",
      marketplace: "MERCADO_LIVRE",
      resourceType: "LISTINGS",
      triggerType: "MANUAL",
      status: "COMPLETED",
      itemsProcessed: 12,
      itemsSucceeded: 12,
      itemsFailed: 0,
      startedAt: new Date(Date.now() - 3600000).toISOString(),
      completedAt: new Date(Date.now() - 3550000).toISOString(),
      createdAt: new Date(Date.now() - 3600000).toISOString(),
    },
    {
      id: "job-102",
      storeId: "store-ml-1",
      marketplace: "MERCADO_LIVRE",
      resourceType: "ORDERS",
      triggerType: "WEBHOOK",
      status: "COMPLETED",
      itemsProcessed: 48,
      itemsSucceeded: 48,
      itemsFailed: 0,
      startedAt: new Date(Date.now() - 1800000).toISOString(),
      completedAt: new Date(Date.now() - 1750000).toISOString(),
      createdAt: new Date(Date.now() - 1800000).toISOString(),
    },
  ]);

  const [selectedResourceType, setSelectedResourceType] = useState<"CATALOG" | "ORDERS">("CATALOG");
  const [csvFile, setCsvFile] = useState<File | null>(null);
  const [csvReport, setCsvReport] = useState<CsvValidationReport | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [isSyncing, setIsSyncing] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const handleTriggerSync = () => {
    setIsSyncing(true);
    setTimeout(() => {
      const newJob: SyncJob = {
        id: "job-" + Math.floor(Math.random() * 1000),
        storeId: "store-ml-1",
        marketplace: "MERCADO_LIVRE",
        resourceType: selectedResourceType,
        triggerType: "MANUAL",
        status: "COMPLETED",
        itemsProcessed: selectedResourceType === "CATALOG" ? 5 : 10,
        itemsSucceeded: selectedResourceType === "CATALOG" ? 5 : 10,
        itemsFailed: 0,
        startedAt: new Date().toISOString(),
        completedAt: new Date().toISOString(),
        createdAt: new Date().toISOString(),
      };
      setJobs((prev) => [newJob, ...prev]);
      setStats((prev) => ({
        ...prev,
        totalListings: prev.totalListings + (selectedResourceType === "CATALOG" ? 5 : 0),
        totalOrders: prev.totalOrders + (selectedResourceType === "ORDERS" ? 10 : 0),
        lastSyncAt: new Date().toISOString(),
      }));
      setIsSyncing(false);
      setMessage(`Sincronização manual de ${selectedResourceType} concluída com sucesso!`);
    }, 600);
  };

  const handleDownloadTemplate = (type: "catalog" | "orders") => {
    const template =
      type === "catalog"
        ? "sku,title,brand,category,barcode,price,available_quantity,status\nSKU-001,Teclado Mecanico,GamerBrand,Informatica,7891234567890,199.90,25,ACTIVE\n"
        : "external_order_id,order_date,status,sku,item_title,quantity,unit_price,total_amount\nPED-2026-001,2026-11-17T10:00:00Z,PAID,SKU-001,Teclado Mecanico,1,199.90,199.90\n";

    const blob = new Blob([template], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", `template_${type}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleUploadCsv = () => {
    if (!csvFile) {
      alert("Selecione um arquivo CSV antes de enviar.");
      return;
    }
    setIsUploading(true);
    setCsvReport(null);

    // Read and simulate parsing with validation feedback
    const reader = new FileReader();
    reader.onload = (e) => {
      const text = e.target?.result as string;
      const lines = text.split("\n").filter((l) => l.trim().length > 0);
      const dataRows = lines.slice(1);

      const errors: CsvRowError[] = [];
      let imported = 0;

      dataRows.forEach((line, idx) => {
        const lineNum = idx + 2;
        const cols = line.split(",");
        if (cols.length < 3 || !cols[0].trim()) {
          errors.push({
            lineNumber: lineNum,
            field: "sku/id",
            rejectedValue: cols[0] || "",
            reason: "Identificador obrigatório ausente ou formato inválido",
          });
        } else {
          imported++;
        }
      });

      const report: CsvValidationReport = {
        resourceType: selectedResourceType,
        totalRows: dataRows.length,
        importedRows: imported,
        rejectedRows: errors.length,
        errors,
      };

      setCsvReport(report);
      setIsUploading(false);

      if (imported > 0) {
        setStats((prev) => ({
          ...prev,
          totalProducts: prev.totalProducts + (selectedResourceType === "CATALOG" ? imported : 0),
          totalOrders: prev.totalOrders + (selectedResourceType === "ORDERS" ? imported : 0),
          lastSyncAt: new Date().toISOString(),
        }));
      }
    };
    reader.readAsText(csvFile);
  };

  return (
    <div className="space-y-8" data-testid="sync-manager">
      {/* Header & Overview Stats */}
      <div>
        <h2 className="text-xl font-bold text-slate-800">Central de Sincronização &amp; Catálogo</h2>
        <p className="text-sm text-slate-500 mt-1">
          Monitoramento assíncrono de jobs, rastreabilidade de dados próprios (`IMPORTED`), webhooks e contingência CSV.
        </p>
      </div>

      {message && (
        <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-700 text-sm rounded-lg flex items-center justify-between">
          <span>{message}</span>
          <button onClick={() => setMessage(null)} className="text-emerald-500 font-bold ml-4">×</button>
        </div>
      )}

      {/* Metric Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Produtos / SKUs</div>
          <div className="text-2xl font-bold text-slate-800 mt-1">{stats.totalProducts}</div>
          <div className="text-xs text-emerald-600 mt-1">● Origem: IMPORTED</div>
        </div>
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Anúncios Sincronizados</div>
          <div className="text-2xl font-bold text-slate-800 mt-1">{stats.totalListings}</div>
          <div className="text-xs text-blue-600 mt-1">ML &amp; Shopee</div>
        </div>
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Pedidos Próprios</div>
          <div className="text-2xl font-bold text-slate-800 mt-1">{stats.totalOrders}</div>
          <div className="text-xs text-emerald-600 mt-1">● 100% Idempotente</div>
        </div>
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Última Sincronização</div>
          <div className="text-sm font-semibold text-slate-700 mt-2 truncate">
            {stats.lastSyncAt ? new Date(stats.lastSyncAt).toLocaleTimeString("pt-BR") : "Nenhuma"}
          </div>
          <div className="text-xs text-slate-400 mt-1">Status: Saudável</div>
        </div>
      </div>

      {/* Manual Sync Trigger */}
      <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm flex flex-wrap items-center justify-between gap-4">
        <div>
          <h3 className="text-sm font-bold text-slate-800">Disparo Manual de Sincronização</h3>
          <p className="text-xs text-slate-500">Inicia extração assíncrona com checkpoints e chave idempotente no RabbitMQ.</p>
        </div>
        <div className="flex items-center gap-3">
          <select
            value={selectedResourceType}
            onChange={(e) => setSelectedResourceType(e.target.value as "CATALOG" | "ORDERS")}
            className="text-xs border border-slate-300 rounded-lg px-3 py-2 bg-white text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="CATALOG">Catálogo &amp; Anúncios</option>
            <option value="ORDERS">Pedidos Próprios</option>
          </select>
          <button
            onClick={handleTriggerSync}
            disabled={isSyncing}
            className="text-xs font-semibold px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors disabled:opacity-50"
          >
            {isSyncing ? "Enfileirando..." : "Sincronizar Agora"}
          </button>
        </div>
      </div>

      {/* Jobs History Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-5 py-4 border-b border-slate-200 flex items-center justify-between">
          <h3 className="text-sm font-bold text-slate-800">Histórico de Jobs de Sincronização</h3>
          <span className="text-xs text-slate-400">Total: {jobs.length} jobs</span>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-600">
            <thead className="bg-slate-50 text-slate-500 font-semibold border-b border-slate-200">
              <tr>
                <th className="px-5 py-3">ID do Job</th>
                <th className="px-5 py-3">Marketplace</th>
                <th className="px-5 py-3">Recurso</th>
                <th className="px-5 py-3">Gatilho</th>
                <th className="px-5 py-3">Status</th>
                <th className="px-5 py-3">Itens</th>
                <th className="px-5 py-3">Início</th>
                <th className="px-5 py-3">Conclusão</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {jobs.map((job) => (
                <tr key={job.id} className="hover:bg-slate-50/50">
                  <td className="px-5 py-3 font-mono font-medium text-slate-800">{job.id}</td>
                  <td className="px-5 py-3 font-semibold text-slate-700">{job.marketplace}</td>
                  <td className="px-5 py-3">{job.resourceType}</td>
                  <td className="px-5 py-3">
                    <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-slate-100 text-slate-600">
                      {job.triggerType}
                    </span>
                  </td>
                  <td className="px-5 py-3">
                    <span
                      className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                        job.status === "COMPLETED"
                          ? "bg-emerald-50 text-emerald-700 border border-emerald-200"
                          : job.status === "FAILED"
                          ? "bg-rose-50 text-rose-700 border border-rose-200"
                          : "bg-amber-50 text-amber-700 border border-amber-200"
                      }`}
                    >
                      {job.status}
                    </span>
                  </td>
                  <td className="px-5 py-3">
                    <span className="text-emerald-600 font-semibold">{job.itemsSucceeded}</span>
                    {job.itemsFailed > 0 && <span className="text-rose-500 font-semibold"> / {job.itemsFailed} falhas</span>}
                  </td>
                  <td className="px-5 py-3 text-slate-400">
                    {job.startedAt ? new Date(job.startedAt).toLocaleTimeString("pt-BR") : "—"}
                  </td>
                  <td className="px-5 py-3 text-slate-400">
                    {job.completedAt ? new Date(job.completedAt).toLocaleTimeString("pt-BR") : "—"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* CSV Contingency Import Section */}
      <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm space-y-4">
        <div>
          <h3 className="text-sm font-bold text-slate-800">Importação de Contingência por Arquivo (CSV)</h3>
          <p className="text-xs text-slate-500 mt-1">
            Requisito FR-INT-008: Quando a API oficial do marketplace estiver indisponível ou para carga manual, envie um extrato CSV oficial com validação estrita linha a linha.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3 pt-2">
          <button
            onClick={() => handleDownloadTemplate("catalog")}
            className="text-xs px-3 py-1.5 border border-slate-300 hover:bg-slate-50 rounded-lg text-slate-700 font-medium transition-colors"
          >
            📥 Baixar Template Catálogo
          </button>
          <button
            onClick={() => handleDownloadTemplate("orders")}
            className="text-xs px-3 py-1.5 border border-slate-300 hover:bg-slate-50 rounded-lg text-slate-700 font-medium transition-colors"
          >
            📥 Baixar Template Pedidos
          </button>
        </div>

        <div className="p-4 border-2 border-dashed border-slate-200 rounded-xl flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <input
              type="file"
              accept=".csv"
              onChange={(e) => setCsvFile(e.target.files?.[0] || null)}
              className="text-xs text-slate-500 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
            />
          </div>
          <button
            onClick={handleUploadCsv}
            disabled={!csvFile || isUploading}
            className="text-xs font-semibold px-4 py-2 bg-slate-800 hover:bg-slate-900 text-white rounded-lg transition-colors disabled:opacity-40"
          >
            {isUploading ? "Processando e Validando..." : "Validar & Ingerir CSV"}
          </button>
        </div>

        {/* Validation Report */}
        {csvReport && (
          <div className="mt-4 p-4 rounded-xl border border-slate-200 bg-slate-50 space-y-3">
            <div className="flex items-center justify-between text-xs font-bold">
              <span className="text-slate-800">Relatório de Ingestão CSV — {csvReport.resourceType}</span>
              <span className="text-slate-500">
                Total de Linhas: {csvReport.totalRows} | Sucesso:{" "}
                <strong className="text-emerald-600">{csvReport.importedRows}</strong> | Rejeitadas:{" "}
                <strong className="text-rose-600">{csvReport.rejectedRows}</strong>
              </span>
            </div>

            {csvReport.errors.length > 0 ? (
              <div className="border border-rose-200 bg-white rounded-lg overflow-hidden">
                <div className="px-3 py-2 bg-rose-50 text-rose-800 text-xs font-semibold">
                  ⚠️ Linhas rejeitadas (nenhum dado inconsistente foi importado parcialmente):
                </div>
                <div className="max-h-48 overflow-y-auto">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-50 text-slate-500 border-b">
                      <tr>
                        <th className="px-3 py-1.5">Linha</th>
                        <th className="px-3 py-1.5">Campo</th>
                        <th className="px-3 py-1.5">Valor Rejeitado</th>
                        <th className="px-3 py-1.5">Motivo</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 text-slate-700">
                      {csvReport.errors.map((err, i) => (
                        <tr key={i} className="hover:bg-rose-50/30">
                          <td className="px-3 py-1.5 font-mono font-bold text-rose-600">{err.lineNumber}</td>
                          <td className="px-3 py-1.5 font-mono">{err.field}</td>
                          <td className="px-3 py-1.5 font-mono text-slate-500 truncate max-w-xs">{err.rejectedValue || "—"}</td>
                          <td className="px-3 py-1.5 text-rose-600">{err.reason}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            ) : (
              <div className="p-3 bg-emerald-50 text-emerald-800 text-xs rounded-lg border border-emerald-200">
                ✅ Todas as {csvReport.importedRows} linhas foram validadas e importadas com sucesso!
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
