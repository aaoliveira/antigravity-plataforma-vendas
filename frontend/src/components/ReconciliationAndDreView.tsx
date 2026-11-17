'use client';

import React, { useState } from 'react';
import {
  DollarSign,
  FileSpreadsheet,
  CheckCircle2,
  AlertCircle,
  HelpCircle,
  ShieldAlert,
  ArrowDownRight,
  TrendingUp,
  Download,
  Filter,
  RefreshCw,
  Search,
  ExternalLink,
  Layers,
  FileCheck,
  Scale,
  Calendar,
  Building,
  AlertTriangle,
  History
} from 'lucide-react';

export type AccountingBasis = 'ACCRUAL' | 'CASH';

export interface SettlementModel {
  id: string;
  marketplace: string;
  externalSettlementId: string;
  originalFilename: string;
  settlementDate: string;
  grossAmount: number;
  netAmount: number;
  feeAmount: number;
  shippingAmount: number;
  adjustmentAmount: number;
  status: 'PROCESSED' | 'COMPLETED' | 'FLAGGED';
  idempotencyHash: string;
  createdAt: string;
}

export interface SettlementItemModel {
  id: string;
  settlementId: string;
  entryType: 'ORDER_PAYOUT' | 'COMMISSION_FEE' | 'TRANSACTION_FEE' | 'SHIPPING_DEBIT' | 'REFUND_CHARGEBACK' | 'ESCROW_RELEASE' | 'ADJUSTMENT';
  externalOrderId: string | null;
  orderId: string | null;
  grossAmount: number;
  netAmount: number;
  marketplaceFee: number;
  shippingCost: number;
  adjustmentAmount: number;
  reconciliationStatus: 'CONCILIATED' | 'PENDING_MATCH' | 'DISCREPANT' | 'UNIDENTIFIED' | 'DISPUTED';
  matchingMethod: 'EXACT_ORDER_ID' | 'MANUAL_OVERRIDE' | 'UNMATCHED';
  expectedOrderAmount?: number;
  discrepancyAmount?: number;
  discrepancyReason?: string;
  disputeId?: string;
  notes?: string;
}

export interface DisputeModel {
  id: string;
  settlementItemId?: string;
  marketplace: string;
  externalProtocolNumber: string;
  reason: string;
  disputedAmount: number;
  status: 'OPEN' | 'UNDER_REVIEW' | 'ACCEPTED' | 'REJECTED';
  channel: 'MARKETPLACE_SUPPORT' | 'SELLER_PORTAL' | 'LEGAL' | 'INTERNAL_AUDIT';
  automatedSubmission: boolean;
  submissionDisclaimer: string;
  notes?: string;
  createdAt: string;
}

export interface DreLineModel {
  lineNumber: number;
  code: string;
  description: string;
  calculationFormula: string;
  amount: number;
  percentageOfGrossRevenue: number;
  subtotal: boolean;
}

export interface AuditLogModel {
  id: string;
  itemId: string;
  action: string;
  previousStatus: string;
  newStatus: string;
  justification: string;
  timestamp: string;
}

const INITIAL_SETTLEMENTS: SettlementModel[] = [
  {
    id: 'settl-001',
    marketplace: 'MERCADO_LIVRE',
    externalSettlementId: 'SETTL-MLB-2026-W46',
    originalFilename: 'extrato_mercadolivre_w46.csv',
    settlementDate: '2026-11-15T10:00:00Z',
    grossAmount: 14500.00,
    netAmount: 11890.00,
    feeAmount: 2175.00,
    shippingAmount: 435.00,
    adjustmentAmount: 0.00,
    status: 'PROCESSED',
    idempotencyHash: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
    createdAt: '2026-11-15T11:30:00Z'
  },
  {
    id: 'settl-002',
    marketplace: 'SHOPEE',
    externalSettlementId: 'SETTL-SHP-2026-11A',
    originalFilename: 'shopee_wallet_payout_11a.xlsx',
    settlementDate: '2026-11-14T14:20:00Z',
    grossAmount: 8200.00,
    netAmount: 6724.00,
    feeAmount: 1148.00,
    shippingAmount: 328.00,
    adjustmentAmount: 0.00,
    status: 'COMPLETED',
    idempotencyHash: '8f434346648f6b96df89dda901c5176b10a6d83961dd3c1ac88b59b2dc327aa4',
    createdAt: '2026-11-14T15:00:00Z'
  }
];

const INITIAL_ITEMS: SettlementItemModel[] = [
  {
    id: 'item-101',
    settlementId: 'settl-001',
    entryType: 'ORDER_PAYOUT',
    externalOrderId: 'MLB-ORD-9021',
    orderId: 'ord-uuid-001',
    grossAmount: 450.00,
    netAmount: 369.00,
    marketplaceFee: 67.50,
    shippingCost: 13.50,
    adjustmentAmount: 0,
    reconciliationStatus: 'CONCILIATED',
    matchingMethod: 'EXACT_ORDER_ID',
    expectedOrderAmount: 450.00,
    discrepancyAmount: 0.00
  },
  {
    id: 'item-102',
    settlementId: 'settl-001',
    entryType: 'ORDER_PAYOUT',
    externalOrderId: 'MLB-ORD-9022',
    orderId: 'ord-uuid-002',
    grossAmount: 320.00,
    netAmount: 232.00,
    marketplaceFee: 48.00,
    shippingCost: 40.00,
    adjustmentAmount: 0,
    reconciliationStatus: 'DISCREPANT',
    matchingMethod: 'EXACT_ORDER_ID',
    expectedOrderAmount: 320.00,
    discrepancyAmount: 25.00,
    discrepancyReason: 'Frete cobrado acima do estimado na venda (Divergência de R$ 25,00)'
  },
  {
    id: 'item-103',
    settlementId: 'settl-001',
    entryType: 'COMMISSION_FEE',
    externalOrderId: null,
    orderId: null,
    grossAmount: 0.00,
    netAmount: -55.00,
    marketplaceFee: 55.00,
    shippingCost: 0,
    adjustmentAmount: 0,
    reconciliationStatus: 'UNIDENTIFIED',
    matchingMethod: 'UNMATCHED',
    discrepancyReason: 'Taxa avulsa sem ID de pedido no extrato'
  },
  {
    id: 'item-104',
    settlementId: 'settl-002',
    entryType: 'ORDER_PAYOUT',
    externalOrderId: 'SHP-ORD-7741',
    orderId: null,
    grossAmount: 180.00,
    netAmount: 147.60,
    marketplaceFee: 25.20,
    shippingCost: 7.20,
    adjustmentAmount: 0,
    reconciliationStatus: 'PENDING_MATCH',
    matchingMethod: 'UNMATCHED',
    discrepancyReason: 'Pedido não localizado na base local de pedidos'
  }
];

const INITIAL_DISPUTES: DisputeModel[] = [
  {
    id: 'disp-001',
    settlementItemId: 'item-102',
    marketplace: 'MERCADO_LIVRE',
    externalProtocolNumber: 'PROT-MLB-994821',
    reason: 'Cobrança indevida de frete adicional acima das dimensões cadastradas',
    disputedAmount: 25.00,
    status: 'UNDER_REVIEW',
    channel: 'MARKETPLACE_SUPPORT',
    automatedSubmission: false,
    submissionDisclaimer: 'Protocolo registrado para controle interno do seller. O sistema não executa envio automático à adquirente.',
    notes: 'Aberto chamado via portal do seller ML anexando fotos da cubagem.',
    createdAt: '2026-11-16T09:15:00Z'
  }
];

export const ReconciliationAndDreView: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'DRE' | 'RECONCILIATION' | 'SETTLEMENTS' | 'DISPUTES'>('DRE');
  const [accountingBasis, setAccountingBasis] = useState<AccountingBasis>('ACCRUAL');
  const [selectedMarketplace, setSelectedMarketplace] = useState<string>('TODOS');
  const [dateRange, setDateRange] = useState({
    start: '2026-11-01',
    end: '2026-11-30'
  });

  // Data states
  const [settlements, setSettlements] = useState<SettlementModel[]>(INITIAL_SETTLEMENTS);
  const [items, setItems] = useState<SettlementItemModel[]>(INITIAL_ITEMS);
  const [disputes, setDisputes] = useState<DisputeModel[]>(INITIAL_DISPUTES);
  const [auditLogs, setAuditLogs] = useState<AuditLogModel[]>([]);

  // Filter for items
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [itemSearch, setItemSearch] = useState<string>('');

  // Modals state
  const [associatingItem, setAssociatingItem] = useState<SettlementItemModel | null>(null);
  const [manualOrderIdInput, setManualOrderIdInput] = useState<string>('');
  const [manualJustificationInput, setManualJustificationInput] = useState<string>('');

  const [ignoringItem, setIgnoringItem] = useState<SettlementItemModel | null>(null);
  const [ignoreReasonInput, setIgnoreReasonInput] = useState<string>('');

  const [disputingItem, setDisputingItem] = useState<SettlementItemModel | null>(null);
  const [disputeProtocolInput, setDisputeProtocolInput] = useState<string>('');
  const [disputeReasonInput, setDisputeReasonInput] = useState<string>('');
  const [disputeChannelInput, setDisputeChannelInput] = useState<'MARKETPLACE_SUPPORT' | 'SELLER_PORTAL' | 'LEGAL' | 'INTERNAL_AUDIT'>('MARKETPLACE_SUPPORT');

  // Simulation import modal
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [importForm, setImportForm] = useState({
    marketplace: 'MERCADO_LIVRE',
    externalSettlementId: 'SETTL-MLB-2026-NEW',
    originalFilename: 'lote_novembro_extra.csv',
    grossAmount: '5000.00',
    feeAmount: '750.00',
    shippingAmount: '150.00'
  });
  const [importMessage, setImportMessage] = useState<{ type: 'success' | 'warning' | 'error'; text: string } | null>(null);

  // Filtered Items
  const filteredItems = items.filter(item => {
    if (statusFilter !== 'ALL' && item.reconciliationStatus !== statusFilter) return false;
    if (itemSearch.trim()) {
      const q = itemSearch.toLowerCase();
      const orderMatch = item.externalOrderId?.toLowerCase().includes(q);
      const reasonMatch = item.discrepancyReason?.toLowerCase().includes(q);
      const typeMatch = item.entryType.toLowerCase().includes(q);
      if (!orderMatch && !reasonMatch && !typeMatch) return false;
    }
    return true;
  });

  // Calculate dynamic DRE values
  const isAccrual = accountingBasis === 'ACCRUAL';

  // Base values depending on regime
  const grossRevenue = isAccrual ? 35400.00 : 22700.00;
  const cancellations = isAccrual ? 1200.00 : 0.00;
  const marketplaceFees = isAccrual ? 5310.00 : 3323.00;
  const shippingDeductions = isAccrual ? 1416.00 : 763.00;
  const estimatedTaxes = Math.round(grossRevenue * 0.06 * 100) / 100; // 6% Simples Nacional
  const totalDeductions = cancellations + marketplaceFees + shippingDeductions + estimatedTaxes;
  const netRevenue = grossRevenue - totalDeductions;
  const cogs = isAccrual ? 13800.00 : 8850.00; // ~39% CMV
  const grossContributionMargin = netRevenue - cogs;
  const operatingExpenses = Math.round(grossRevenue * 0.035 * 100) / 100; // 3.5%
  const netOperatingResult = grossContributionMargin - operatingExpenses;

  const dreLines: DreLineModel[] = [
    {
      lineNumber: 1,
      code: '1.0',
      description: isAccrual ? 'Receita Bruta de Vendas (Pedidos Realizados)' : 'Entradas Brutas de Repasses Liquidados',
      calculationFormula: isAccrual ? 'Soma do valor bruto de pedidos confirmados' : 'Soma das liquidações brutas dos extratos',
      amount: grossRevenue,
      percentageOfGrossRevenue: 100.0,
      subtotal: false
    },
    {
      lineNumber: 2,
      code: '1.1',
      description: '(-) Cancelamentos, Devoluções e Reversões',
      calculationFormula: isAccrual ? 'Estornos e cancelamentos com devolução total' : 'Não deduzido no caixa pois não liquida repasse',
      amount: cancellations,
      percentageOfGrossRevenue: grossRevenue > 0 ? (cancellations / grossRevenue) * 100 : 0,
      subtotal: false
    },
    {
      lineNumber: 3,
      code: '1.2',
      description: '(-) Tarifas e Comissões de Marketplace Retidas',
      calculationFormula: isAccrual ? 'Comissões contratuais apuradas no pedido' : 'Taxas de serviço e comissões debitadas no extrato',
      amount: marketplaceFees,
      percentageOfGrossRevenue: grossRevenue > 0 ? (marketplaceFees / grossRevenue) * 100 : 0,
      subtotal: false
    },
    {
      lineNumber: 4,
      code: '1.3',
      description: '(-) Custos de Envio, Logística e Fretes',
      calculationFormula: isAccrual ? 'Frete suportado pelo seller conforme anúncio' : 'Débitos de etiqueta e frete retidos no repasse',
      amount: shippingDeductions,
      percentageOfGrossRevenue: grossRevenue > 0 ? (shippingDeductions / grossRevenue) * 100 : 0,
      subtotal: false
    },
    {
      lineNumber: 5,
      code: '1.4',
      description: '(-) Estimativa Tributária Parametrizada (Simples Nacional 6.0%)',
      calculationFormula: 'Parametrização de regime tributário do tenant (6.0% s/ faturamento)',
      amount: estimatedTaxes,
      percentageOfGrossRevenue: grossRevenue > 0 ? (estimatedTaxes / grossRevenue) * 100 : 0,
      subtotal: false
    },
    {
      lineNumber: 6,
      code: '2.0',
      description: '(=) RECEITA LÍQUIDA OPERACIONAL',
      calculationFormula: 'Receita Bruta - Cancelamentos - Tarifas - Fretes - Tributos',
      amount: netRevenue,
      percentageOfGrossRevenue: grossRevenue > 0 ? (netRevenue / grossRevenue) * 100 : 0,
      subtotal: true
    },
    {
      lineNumber: 7,
      code: '2.1',
      description: '(-) Custo das Mercadorias Vendidas (CMV Efetivo)',
      calculationFormula: isAccrual ? 'Histórico versionado de custos por SKU na data do pedido' : 'Custo de reposição estimado dos itens liquidados',
      amount: cogs,
      percentageOfGrossRevenue: grossRevenue > 0 ? (cogs / grossRevenue) * 100 : 0,
      subtotal: false
    },
    {
      lineNumber: 8,
      code: '3.0',
      description: '(=) MARGEM DE CONTRIBUIÇÃO BRUTA',
      calculationFormula: 'Receita Líquida - CMV',
      amount: grossContributionMargin,
      percentageOfGrossRevenue: grossRevenue > 0 ? (grossContributionMargin / grossRevenue) * 100 : 0,
      subtotal: true
    },
    {
      lineNumber: 9,
      code: '3.1',
      description: '(-) Despesas Operacionais e Embalagens Alocáveis (3.5%)',
      calculationFormula: 'Despesas com caixas, fita, sistemas SaaS e alocação direta',
      amount: operatingExpenses,
      percentageOfGrossRevenue: grossRevenue > 0 ? (operatingExpenses / grossRevenue) * 100 : 0,
      subtotal: false
    },
    {
      lineNumber: 10,
      code: '4.0',
      description: '(=) RESULTADO OPERACIONAL GERENCIAL (LAJIR / EBITDA GERENCIAL)',
      calculationFormula: 'Margem de Contribuição - Despesas Operacionais',
      amount: netOperatingResult,
      percentageOfGrossRevenue: grossRevenue > 0 ? (netOperatingResult / grossRevenue) * 100 : 0,
      subtotal: true
    }
  ];

  // Actions
  const handleAssociateOrder = () => {
    if (!associatingItem || !manualOrderIdInput.trim() || !manualJustificationInput.trim()) return;

    setItems(prev => prev.map(it => {
      if (it.id === associatingItem.id) {
        return {
          ...it,
          externalOrderId: manualOrderIdInput.trim(),
          reconciliationStatus: 'CONCILIATED',
          matchingMethod: 'MANUAL_OVERRIDE',
          discrepancyAmount: 0,
          discrepancyReason: undefined,
          notes: `Associado manualmente: ${manualJustificationInput.trim()}`
        };
      }
      return it;
    }));

    setAuditLogs(prev => [
      {
        id: `audit-${Date.now()}`,
        itemId: associatingItem.id,
        action: 'MANUAL_ASSOCIATION',
        previousStatus: associatingItem.reconciliationStatus,
        newStatus: 'CONCILIATED',
        justification: manualJustificationInput.trim(),
        timestamp: new Date().toISOString()
      },
      ...prev
    ]);

    setAssociatingItem(null);
    setManualOrderIdInput('');
    setManualJustificationInput('');
  };

  const handleIgnoreDiscrepancy = () => {
    if (!ignoringItem || !ignoreReasonInput.trim()) return;

    setItems(prev => prev.map(it => {
      if (it.id === ignoringItem.id) {
        return {
          ...it,
          reconciliationStatus: 'CONCILIATED',
          notes: `Divergência tolerada pelo seller: ${ignoreReasonInput.trim()}`
        };
      }
      return it;
    }));

    setAuditLogs(prev => [
      {
        id: `audit-${Date.now()}`,
        itemId: ignoringItem.id,
        action: 'IGNORE_DISCREPANCY',
        previousStatus: ignoringItem.reconciliationStatus,
        newStatus: 'CONCILIATED',
        justification: ignoreReasonInput.trim(),
        timestamp: new Date().toISOString()
      },
      ...prev
    ]);

    setIgnoringItem(null);
    setIgnoreReasonInput('');
  };

  const handleCreateDispute = () => {
    if (!disputingItem || !disputeReasonInput.trim()) return;

    const newDispute: DisputeModel = {
      id: `disp-${Date.now()}`,
      settlementItemId: disputingItem.id,
      marketplace: 'MERCADO_LIVRE',
      externalProtocolNumber: disputeProtocolInput.trim() || `PROT-AUTO-${Date.now().toString().slice(-6)}`,
      reason: disputeReasonInput.trim(),
      disputedAmount: disputingItem.discrepancyAmount || disputingItem.netAmount,
      status: 'OPEN',
      channel: disputeChannelInput,
      automatedSubmission: false,
      submissionDisclaimer: 'Protocolo registrado para controle interno do seller. O sistema não executa envio automático à adquirente.',
      createdAt: new Date().toISOString()
    };

    setDisputes(prev => [newDispute, ...prev]);

    setItems(prev => prev.map(it => {
      if (it.id === disputingItem.id) {
        return {
          ...it,
          reconciliationStatus: 'DISPUTED',
          disputeId: newDispute.id
        };
      }
      return it;
    }));

    setDisputingItem(null);
    setDisputeProtocolInput('');
    setDisputeReasonInput('');
  };

  const handleSimulateImport = () => {
    const gross = parseFloat(importForm.grossAmount) || 0;
    const fee = parseFloat(importForm.feeAmount) || 0;
    const ship = parseFloat(importForm.shippingAmount) || 0;
    const net = gross - fee - ship;

    // Check idempotency
    const exists = settlements.some(
      s => s.marketplace === importForm.marketplace && s.externalSettlementId === importForm.externalSettlementId
    );

    if (exists) {
      setImportMessage({
        type: 'warning',
        text: `Idempotência ativada: O extrato '${importForm.externalSettlementId}' já foi importado previamente. Nenhuma transação duplicada.`
      });
      return;
    }

    const newSettlement: SettlementModel = {
      id: `settl-${Date.now()}`,
      marketplace: importForm.marketplace,
      externalSettlementId: importForm.externalSettlementId,
      originalFilename: importForm.originalFilename,
      settlementDate: new Date().toISOString(),
      grossAmount: gross,
      netAmount: net,
      feeAmount: fee,
      shippingAmount: ship,
      adjustmentAmount: 0,
      status: 'PROCESSED',
      idempotencyHash: 'abc' + Math.random().toString(16).slice(2, 10),
      createdAt: new Date().toISOString()
    };

    setSettlements(prev => [newSettlement, ...prev]);
    setImportMessage({
      type: 'success',
      text: `Extrato '${newSettlement.externalSettlementId}' importado com sucesso via hash SHA-256 idempotente!`
    });
    setTimeout(() => {
      setIsImportModalOpen(false);
      setImportMessage(null);
    }, 1500);
  };

  const handleExportCsv = () => {
    const headerLines = [
      '# RELATÓRIO: DRE GERENCIAL - SELLER INTELLIGENCE PLATFORM',
      `# REGIME: ${isAccrual ? 'COMPETÊNCIA (PEDIDOS/VENDAS)' : 'CAIXA (REPASSES/LIQUIDAÇÕES)'}`,
      `# MARKETPLACE: ${selectedMarketplace}`,
      `# PERÍODO: ${dateRange.start} até ${dateRange.end}`,
      '# TIMEZONE: America/Sao_Paulo',
      `# GERADO_EM: ${new Date().toISOString()}`,
      '# AVISO_LEGAL: DRE gerencial para apoio determinístico à tomada de decisão. Não substitui demonstrações contábeis oficiais nem obrigações perante a Receita Federal.',
      '',
      'Linha;Código;Descrição;Fórmula / Memória de Cálculo;Valor (R$);% Receita Bruta'
    ];

    const dataLines = dreLines.map(line => 
      `${line.lineNumber};${line.code};${line.description};${line.calculationFormula};${line.amount.toFixed(2).replace('.', ',')};${line.percentageOfGrossRevenue.toFixed(2).replace('.', ',')}%`
    );

    const csvContent = [...headerLines, ...dataLines].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `DRE_Gerencial_${isAccrual ? 'Competencia' : 'Caixa'}_${dateRange.start}_${dateRange.end}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="space-y-6" data-testid="reconciliation-and-dre-view">
      {/* Top Banner: Disclaimers and Invariants */}
      <div className="bg-gradient-to-r from-teal-950/40 via-slate-900 to-slate-900 border border-teal-500/30 rounded-xl p-4 shadow-lg">
        <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div className="flex items-start gap-3">
            <Scale className="w-6 h-6 text-teal-400 mt-1 shrink-0" />
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-lg font-semibold text-white">DRE Gerencial &amp; Conciliação de Repasses</h2>
                <span className="text-[11px] font-medium px-2 py-0.5 rounded bg-teal-500/20 text-teal-300 border border-teal-500/30">
                  Fase 2 • FR-FIN-008 &amp; FR-REC
                </span>
              </div>
              <p className="text-xs text-slate-300 mt-0.5">
                Demonstrativo gerencial com separação estrita de regimes (Caixa vs. Competência), conciliação idempotente de liquidações, auditoria manual e gestão de protocolos de contestação.
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setIsImportModalOpen(true)}
              className="px-3 py-1.5 bg-teal-600 hover:bg-teal-500 text-white rounded-lg text-xs font-medium flex items-center gap-1.5 shadow transition-colors"
            >
              <FileSpreadsheet className="w-4 h-4" />
              Importar Extrato
            </button>
          </div>
        </div>

        {/* Legal Disclaimer Box */}
        <div className="mt-3 pt-3 border-t border-slate-800 flex items-center gap-2 text-slate-400 text-[11px]">
          <ShieldAlert className="w-4 h-4 text-amber-400 shrink-0" />
          <span>
            <strong>Aviso de Conformidade:</strong> O DRE Gerencial apoia o controle financeiro do seller e <em>não substitui</em> o ERP fiscal ou demonstrações oficiais para a Receita Federal. Contestações têm efeito de protocolo interno (sem transmissão bancária desautorizada).
          </span>
        </div>
      </div>

      {/* Primary Sub-Navigation Tabs */}
      <div className="flex border-b border-slate-800 text-sm font-medium space-x-4">
        <button
          onClick={() => setActiveTab('DRE')}
          className={`pb-3 px-2 border-b-2 transition-colors flex items-center gap-2 ${
            activeTab === 'DRE'
              ? 'border-teal-400 text-teal-300 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <Layers className="w-4 h-4" />
          DRE Gerencial
        </button>
        <button
          onClick={() => setActiveTab('RECONCILIATION')}
          className={`pb-3 px-2 border-b-2 transition-colors flex items-center gap-2 ${
            activeTab === 'RECONCILIATION'
              ? 'border-teal-400 text-teal-300 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <CheckCircle2 className="w-4 h-4" />
          Conciliação de Pedidos &amp; Lançamentos
          <span className="text-xs px-1.5 py-0.2 rounded-full bg-slate-800 text-slate-300 border border-slate-700">
            {items.length}
          </span>
        </button>
        <button
          onClick={() => setActiveTab('SETTLEMENTS')}
          className={`pb-3 px-2 border-b-2 transition-colors flex items-center gap-2 ${
            activeTab === 'SETTLEMENTS'
              ? 'border-teal-400 text-teal-300 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <FileSpreadsheet className="w-4 h-4" />
          Extratos &amp; Liquidações (Lotes)
          <span className="text-xs px-1.5 py-0.2 rounded-full bg-slate-800 text-slate-300 border border-slate-700">
            {settlements.length}
          </span>
        </button>
        <button
          onClick={() => setActiveTab('DISPUTES')}
          className={`pb-3 px-2 border-b-2 transition-colors flex items-center gap-2 ${
            activeTab === 'DISPUTES'
              ? 'border-teal-400 text-teal-300 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <AlertCircle className="w-4 h-4" />
          Contestações &amp; Protocolos
          <span className="text-xs px-1.5 py-0.2 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30">
            {disputes.length}
          </span>
        </button>
      </div>

      {/* TAB 1: DRE GERENCIAL */}
      {activeTab === 'DRE' && (
        <div className="space-y-5" data-testid="dre-container">
          {/* Controls Bar: Accounting Basis + Filters */}
          <div className="flex flex-wrap items-center justify-between gap-4 bg-slate-900 border border-slate-800 p-4 rounded-xl">
            {/* Regime Switcher */}
            <div className="flex items-center gap-2">
              <span className="text-xs text-slate-400 font-medium">Regime Contábil:</span>
              <div className="flex bg-slate-950 p-1 rounded-lg border border-slate-800 text-xs font-semibold">
                <button
                  onClick={() => setAccountingBasis('ACCRUAL')}
                  className={`px-3 py-1.5 rounded-md transition-all flex items-center gap-1.5 ${
                    isAccrual
                      ? 'bg-teal-600 text-white shadow'
                      : 'text-slate-400 hover:text-slate-200'
                  }`}
                  data-testid="basis-accrual-btn"
                >
                  <Calendar className="w-3.5 h-3.5" />
                  Competência (Data do Pedido)
                </button>
                <button
                  onClick={() => setAccountingBasis('CASH')}
                  className={`px-3 py-1.5 rounded-md transition-all flex items-center gap-1.5 ${
                    !isAccrual
                      ? 'bg-teal-600 text-white shadow'
                      : 'text-slate-400 hover:text-slate-200'
                  }`}
                  data-testid="basis-cash-btn"
                >
                  <DollarSign className="w-3.5 h-3.5" />
                  Caixa (Data do Repasse)
                </button>
              </div>
            </div>

            {/* Marketplace & Date Filters */}
            <div className="flex flex-wrap items-center gap-3">
              <div className="flex items-center gap-2 text-xs">
                <span className="text-slate-400">Canal:</span>
                <select
                  value={selectedMarketplace}
                  onChange={(e) => setSelectedMarketplace(e.target.value)}
                  className="bg-slate-950 border border-slate-700 text-slate-200 rounded-lg px-2.5 py-1.5 text-xs focus:ring-1 focus:ring-teal-500"
                >
                  <option value="TODOS">Todos os Canais</option>
                  <option value="MERCADO_LIVRE">Mercado Livre</option>
                  <option value="SHOPEE">Shopee</option>
                  <option value="AMAZON">Amazon</option>
                </select>
              </div>

              <div className="flex items-center gap-2 text-xs">
                <span className="text-slate-400">Período:</span>
                <input
                  type="date"
                  value={dateRange.start}
                  onChange={(e) => setDateRange(prev => ({ ...prev, start: e.target.value }))}
                  className="bg-slate-950 border border-slate-700 text-slate-200 rounded-lg px-2 py-1 text-xs"
                />
                <span className="text-slate-500">até</span>
                <input
                  type="date"
                  value={dateRange.end}
                  onChange={(e) => setDateRange(prev => ({ ...prev, end: e.target.value }))}
                  className="bg-slate-950 border border-slate-700 text-slate-200 rounded-lg px-2 py-1 text-xs"
                />
              </div>

              <button
                onClick={handleExportCsv}
                className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-200 rounded-lg text-xs font-medium flex items-center gap-1.5 shadow transition-colors"
                data-testid="export-dre-csv-btn"
              >
                <Download className="w-3.5 h-3.5 text-teal-400" />
                Exportar CSV
              </button>
            </div>
          </div>

          {/* Basis Explanation Banner */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-lg p-3 text-xs flex items-center justify-between text-slate-300">
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-teal-400 animate-pulse"></span>
              <span>
                Visualizando em <strong>{isAccrual ? 'Regime de Competência' : 'Regime de Caixa'}</strong>:
                {isAccrual
                  ? ' reflete vendas efetuadas na data do pedido, comissões contratadas e CMV dos produtos vendidos.'
                  : ' reflete valores monetários efetivamente liquidados e creditados no extrato do marketplace.'}
              </span>
            </div>
            <span className="text-[11px] text-slate-500 font-mono">Timezone: America/Sao_Paulo (UTC-03:00)</span>
          </div>

          {/* KPI Summary Cards */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl">
              <p className="text-xs text-slate-400 font-medium">Receita Bruta</p>
              <h3 className="text-xl font-bold text-white mt-1">
                R$ {grossRevenue.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
              </h3>
              <p className="text-[11px] text-teal-400 mt-1">100,0% da operação</p>
            </div>

            <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl">
              <p className="text-xs text-slate-400 font-medium">Receita Líquida Operacional</p>
              <h3 className="text-xl font-bold text-white mt-1">
                R$ {netRevenue.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
              </h3>
              <p className="text-[11px] text-slate-400 mt-1">
                {grossRevenue > 0 ? ((netRevenue / grossRevenue) * 100).toFixed(1) : 0}% da Receita Bruta
              </p>
            </div>

            <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl">
              <p className="text-xs text-slate-400 font-medium">Margem de Contribuição Bruta</p>
              <h3 className="text-xl font-bold text-teal-300 mt-1">
                R$ {grossContributionMargin.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
              </h3>
              <p className="text-[11px] text-teal-400 mt-1">
                {grossRevenue > 0 ? ((grossContributionMargin / grossRevenue) * 100).toFixed(1) : 0}% da Receita Bruta
              </p>
            </div>

            <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl">
              <p className="text-xs text-slate-400 font-medium">Resultado Operacional Gerencial</p>
              <h3 className="text-xl font-bold text-emerald-400 mt-1">
                R$ {netOperatingResult.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
              </h3>
              <p className="text-[11px] text-emerald-400 mt-1">
                {grossRevenue > 0 ? ((netOperatingResult / grossRevenue) * 100).toFixed(1) : 0}% da Receita Bruta
              </p>
            </div>
          </div>

          {/* DRE Structured Table */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden shadow">
            <div className="p-4 border-b border-slate-800 flex items-center justify-between">
              <div>
                <h4 className="text-sm font-semibold text-white">Demonstrativo de Resultado do Exercício (Gerencial)</h4>
                <p className="text-xs text-slate-400">Padronizado em 10 níveis conforme PRD FR-FIN-008 com memórias de cálculo auditáveis.</p>
              </div>
              <span className="text-xs px-2.5 py-1 bg-slate-800 text-slate-300 rounded-md border border-slate-700 font-mono">
                {dreLines.length} Linhas Estruturadas
              </span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="bg-slate-950/80 text-slate-400 font-semibold border-b border-slate-800">
                    <th className="py-2.5 px-3 w-16">Código</th>
                    <th className="py-2.5 px-3">Descrição da Linha</th>
                    <th className="py-2.5 px-3">Fórmula / Memória de Cálculo</th>
                    <th className="py-2.5 px-3 text-right w-36">Valor (R$)</th>
                    <th className="py-2.5 px-3 text-right w-24">% Receita</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {dreLines.map((line) => {
                    const isTotal = line.subtotal;
                    return (
                      <tr
                        key={line.code}
                        className={`transition-colors ${
                          isTotal
                            ? 'bg-slate-800/50 font-semibold text-white'
                            : 'hover:bg-slate-800/30 text-slate-300'
                        }`}
                      >
                        <td className="py-2.5 px-3 font-mono text-teal-400">{line.code}</td>
                        <td className={`py-2.5 px-3 ${isTotal ? 'font-bold' : ''}`}>
                          {line.description}
                        </td>
                        <td className="py-2.5 px-3 text-slate-400 text-[11px] font-mono">
                          {line.calculationFormula}
                        </td>
                        <td className={`py-2.5 px-3 text-right font-mono ${
                          line.amount < 0 ? 'text-rose-400' : isTotal ? 'text-teal-300 font-bold' : 'text-slate-200'
                        }`}>
                          {line.amount < 0 ? '-' : ''} R$ {Math.abs(line.amount).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
                        </td>
                        <td className="py-2.5 px-3 text-right font-mono text-slate-400">
                          {line.percentageOfGrossRevenue.toFixed(1)}%
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: CONCILIAÇÃO & DIVERGÊNCIAS */}
      {activeTab === 'RECONCILIATION' && (
        <div className="space-y-4" data-testid="reconciliation-items-container">
          {/* Filter and Search Bar */}
          <div className="flex flex-wrap items-center justify-between gap-4 bg-slate-900 border border-slate-800 p-4 rounded-xl">
            <div className="flex items-center gap-2">
              <span className="text-xs text-slate-400 font-medium">Status:</span>
              <div className="flex flex-wrap gap-1">
                {(['ALL', 'CONCILIATED', 'DISCREPANT', 'PENDING_MATCH', 'UNIDENTIFIED'] as const).map(st => (
                  <button
                    key={st}
                    onClick={() => setStatusFilter(st)}
                    className={`px-2.5 py-1 rounded text-xs font-medium transition-colors ${
                      statusFilter === st
                        ? 'bg-teal-600 text-white'
                        : 'bg-slate-800 text-slate-400 hover:text-white'
                    }`}
                  >
                    {st === 'ALL' && 'Todos'}
                    {st === 'CONCILIATED' && 'Conciliados'}
                    {st === 'DISCREPANT' && 'Com Divergência'}
                    {st === 'PENDING_MATCH' && 'Pendente de De/Para'}
                    {st === 'UNIDENTIFIED' && 'Não Identificados'}
                  </button>
                ))}
              </div>
            </div>

            <div className="relative">
              <Search className="w-3.5 h-3.5 text-slate-400 absolute left-2.5 top-2.5" />
              <input
                type="text"
                placeholder="Buscar pedido, tipo ou motivo..."
                value={itemSearch}
                onChange={(e) => setItemSearch(e.target.value)}
                className="bg-slate-950 border border-slate-700 text-slate-200 rounded-lg pl-8 pr-3 py-1.5 text-xs w-64 focus:ring-1 focus:ring-teal-500"
              />
            </div>
          </div>

          {/* Items Table */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden shadow">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="bg-slate-950/80 text-slate-400 font-semibold border-b border-slate-800">
                    <th className="py-2.5 px-3">Tipo de Entrada</th>
                    <th className="py-2.5 px-3">Pedido Marketplace</th>
                    <th className="py-2.5 px-3 text-right">Bruto (R$)</th>
                    <th className="py-2.5 px-3 text-right">Tarifas (R$)</th>
                    <th className="py-2.5 px-3 text-right">Líquido Repassado (R$)</th>
                    <th className="py-2.5 px-3 text-right">Esperado (R$)</th>
                    <th className="py-2.5 px-3 text-right">Divergência (R$)</th>
                    <th className="py-2.5 px-3">Status &amp; Método</th>
                    <th className="py-2.5 px-3 text-center">Ações</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {filteredItems.length === 0 ? (
                    <tr>
                      <td colSpan={9} className="py-8 text-center text-slate-500">
                        Nenhum lançamento encontrado para os filtros selecionados.
                      </td>
                    </tr>
                  ) : (
                    filteredItems.map(item => {
                      const isDiscrepant = item.reconciliationStatus === 'DISCREPANT';
                      const isPending = item.reconciliationStatus === 'PENDING_MATCH' || item.reconciliationStatus === 'UNIDENTIFIED';
                      const isConciliated = item.reconciliationStatus === 'CONCILIATED';
                      const isDisputed = item.reconciliationStatus === 'DISPUTED';

                      return (
                        <tr key={item.id} className="hover:bg-slate-800/40 transition-colors">
                          <td className="py-2.5 px-3">
                            <span className="font-mono text-slate-300 font-medium">{item.entryType}</span>
                            {item.notes && (
                              <p className="text-[10px] text-teal-400 mt-0.5 truncate max-w-xs">{item.notes}</p>
                            )}
                          </td>
                          <td className="py-2.5 px-3 font-mono">
                            {item.externalOrderId ? (
                              <span className="text-teal-300">{item.externalOrderId}</span>
                            ) : (
                              <span className="text-slate-500 italic">Sem ID associado</span>
                            )}
                          </td>
                          <td className="py-2.5 px-3 text-right font-mono text-slate-300">
                            {item.grossAmount.toFixed(2)}
                          </td>
                          <td className="py-2.5 px-3 text-right font-mono text-rose-400">
                            -{item.marketplaceFee.toFixed(2)}
                          </td>
                          <td className="py-2.5 px-3 text-right font-mono font-semibold text-white">
                            {item.netAmount.toFixed(2)}
                          </td>
                          <td className="py-2.5 px-3 text-right font-mono text-slate-400">
                            {item.expectedOrderAmount !== undefined ? item.expectedOrderAmount.toFixed(2) : '-'}
                          </td>
                          <td className={`py-2.5 px-3 text-right font-mono ${
                            item.discrepancyAmount && item.discrepancyAmount > 0 ? 'text-amber-400 font-bold' : 'text-slate-500'
                          }`}>
                            {item.discrepancyAmount && item.discrepancyAmount > 0 ? `+${item.discrepancyAmount.toFixed(2)}` : '0,00'}
                          </td>
                          <td className="py-2.5 px-3">
                            <div className="flex flex-col gap-0.5">
                              {isConciliated && (
                                <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/20 w-fit">
                                  <CheckCircle2 className="w-3 h-3" />
                                  Conciliado
                                </span>
                              )}
                              {isDiscrepant && (
                                <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-amber-400 bg-amber-500/10 px-2 py-0.5 rounded border border-amber-500/20 w-fit">
                                  <AlertTriangle className="w-3 h-3" />
                                  Divergência
                                </span>
                              )}
                              {isPending && (
                                <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-sky-400 bg-sky-500/10 px-2 py-0.5 rounded border border-sky-500/20 w-fit">
                                  <HelpCircle className="w-3 h-3" />
                                  Pendente
                                </span>
                              )}
                              {isDisputed && (
                                <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-purple-400 bg-purple-500/10 px-2 py-0.5 rounded border border-purple-500/20 w-fit">
                                  <AlertCircle className="w-3 h-3" />
                                  Contestado
                                </span>
                              )}
                              <span className="text-[10px] text-slate-500 font-mono">
                                {item.matchingMethod}
                              </span>
                            </div>
                            {item.discrepancyReason && (
                              <p className="text-[10px] text-amber-300 mt-1 max-w-xs">{item.discrepancyReason}</p>
                            )}
                          </td>
                          <td className="py-2.5 px-3 text-center">
                            <div className="flex items-center justify-center gap-1.5">
                              {isPending && (
                                <button
                                  onClick={() => setAssociatingItem(item)}
                                  className="px-2 py-1 bg-slate-800 hover:bg-slate-700 text-teal-300 rounded text-[11px] font-medium border border-slate-700"
                                  title="Associar Pedido Manualmente"
                                >
                                  De/Para
                                </button>
                              )}
                              {isDiscrepant && (
                                <>
                                  <button
                                    onClick={() => setIgnoringItem(item)}
                                    className="px-2 py-1 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded text-[11px] font-medium border border-slate-700"
                                    title="Ignorar Divergência (Tolerância Comercial)"
                                  >
                                    Ignorar
                                  </button>
                                  <button
                                    onClick={() => setDisputingItem(item)}
                                    className="px-2 py-1 bg-amber-600/30 hover:bg-amber-600/50 text-amber-300 rounded text-[11px] font-medium border border-amber-500/40"
                                    title="Registrar Protocolo de Contestação"
                                  >
                                    Contestar
                                  </button>
                                </>
                              )}
                              {isConciliated && (
                                <span className="text-slate-500 text-[11px] italic">OK</span>
                              )}
                            </div>
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* Audit Logs Section */}
          {auditLogs.length > 0 && (
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 space-y-2">
              <div className="flex items-center gap-2 text-xs font-semibold text-slate-300">
                <History className="w-4 h-4 text-teal-400" />
                <span>Trilha de Auditoria Imutável (Ações Manuais Registradas)</span>
              </div>
              <div className="space-y-1.5">
                {auditLogs.map(log => (
                  <div key={log.id} className="text-xs bg-slate-950 p-2.5 rounded-lg border border-slate-800 flex items-center justify-between">
                    <div>
                      <span className="font-mono text-teal-400 font-semibold">{log.action}:</span>{' '}
                      <span className="text-slate-300">{log.justification}</span>
                      <span className="text-slate-500 ml-2">({log.previousStatus} &rarr; {log.newStatus})</span>
                    </div>
                    <span className="text-[10px] text-slate-500 font-mono">{log.timestamp}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* TAB 3: EXTRATOS & REPASSES (LOTES) */}
      {activeTab === 'SETTLEMENTS' && (
        <div className="space-y-4" data-testid="settlements-container">
          <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden shadow">
            <div className="p-4 border-b border-slate-800 flex items-center justify-between">
              <div>
                <h4 className="text-sm font-semibold text-white">Lotes de Liquidação &amp; Extratos Importados</h4>
                <p className="text-xs text-slate-400">
                  Garantia de idempotência via hash SHA-256 e restrição única (tenant + marketplace + external_settlement_id).
                </p>
              </div>
              <button
                onClick={() => setIsImportModalOpen(true)}
                className="px-3 py-1.5 bg-teal-600 hover:bg-teal-500 text-white rounded-lg text-xs font-medium flex items-center gap-1.5 shadow"
              >
                <FileSpreadsheet className="w-3.5 h-3.5" />
                Novo Lote
              </button>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="bg-slate-950/80 text-slate-400 font-semibold border-b border-slate-800">
                    <th className="py-2.5 px-3">Marketplace</th>
                    <th className="py-2.5 px-3">ID Liquidação Extrato</th>
                    <th className="py-2.5 px-3">Arquivo de Origem</th>
                    <th className="py-2.5 px-3">Data Liquidação</th>
                    <th className="py-2.5 px-3 text-right">Bruto (R$)</th>
                    <th className="py-2.5 px-3 text-right">Taxas Retidas (R$)</th>
                    <th className="py-2.5 px-3 text-right">Fretes (R$)</th>
                    <th className="py-2.5 px-3 text-right">Líquido Creditado (R$)</th>
                    <th className="py-2.5 px-3">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {settlements.map(st => (
                    <tr key={st.id} className="hover:bg-slate-800/30 transition-colors">
                      <td className="py-2.5 px-3 font-semibold text-white">{st.marketplace}</td>
                      <td className="py-2.5 px-3 font-mono text-teal-400">{st.externalSettlementId}</td>
                      <td className="py-2.5 px-3 text-slate-400">{st.originalFilename}</td>
                      <td className="py-2.5 px-3 text-slate-400 font-mono">
                        {new Date(st.settlementDate).toLocaleDateString('pt-BR')}
                      </td>
                      <td className="py-2.5 px-3 text-right font-mono text-slate-300">
                        {st.grossAmount.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
                      </td>
                      <td className="py-2.5 px-3 text-right font-mono text-rose-400">
                        -{st.feeAmount.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
                      </td>
                      <td className="py-2.5 px-3 text-right font-mono text-rose-400">
                        -{st.shippingAmount.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
                      </td>
                      <td className="py-2.5 px-3 text-right font-mono font-bold text-emerald-400">
                        R$ {st.netAmount.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
                      </td>
                      <td className="py-2.5 px-3">
                        <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/20">
                          <CheckCircle2 className="w-3 h-3" />
                          {st.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 4: CONTESTAÇÕES & PROTOCOLOS */}
      {activeTab === 'DISPUTES' && (
        <div className="space-y-4" data-testid="disputes-container">
          <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden shadow">
            <div className="p-4 border-b border-slate-800">
              <h4 className="text-sm font-semibold text-white">Registro de Contestações &amp; Protocolos</h4>
              <p className="text-xs text-slate-400">
                Acompanhamento interno de chamados abertos perante o canal (ex.: frete indevido, comissão errônea). O sistema não altera dados bancários automaticamente.
              </p>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="bg-slate-950/80 text-slate-400 font-semibold border-b border-slate-800">
                    <th className="py-2.5 px-3">Protocolo</th>
                    <th className="py-2.5 px-3">Canal</th>
                    <th className="py-2.5 px-3">Motivo da Contestação</th>
                    <th className="py-2.5 px-3 text-right">Valor Contestado (R$)</th>
                    <th className="py-2.5 px-3">Status</th>
                    <th className="py-2.5 px-3">Envio Automático</th>
                    <th className="py-2.5 px-3">Data de Abertura</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {disputes.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="py-8 text-center text-slate-500">
                        Nenhuma contestação cadastrada até o momento.
                      </td>
                    </tr>
                  ) : (
                    disputes.map(disp => (
                      <tr key={disp.id} className="hover:bg-slate-800/30 transition-colors">
                        <td className="py-2.5 px-3 font-mono text-amber-400 font-semibold">
                          {disp.externalProtocolNumber}
                        </td>
                        <td className="py-2.5 px-3 text-slate-300">{disp.channel}</td>
                        <td className="py-2.5 px-3 text-slate-300">
                          <p className="font-medium text-white">{disp.reason}</p>
                          {disp.notes && <p className="text-[11px] text-slate-400 mt-0.5">{disp.notes}</p>}
                        </td>
                        <td className="py-2.5 px-3 text-right font-mono font-bold text-amber-300">
                          R$ {disp.disputedAmount.toFixed(2)}
                        </td>
                        <td className="py-2.5 px-3">
                          <span className="text-[10px] font-semibold text-amber-400 bg-amber-500/10 px-2 py-0.5 rounded border border-amber-500/30">
                            {disp.status}
                          </span>
                        </td>
                        <td className="py-2.5 px-3">
                          <span className="text-[10px] text-slate-400 bg-slate-800 px-2 py-0.5 rounded">
                            Não (Controle Interno)
                          </span>
                        </td>
                        <td className="py-2.5 px-3 font-mono text-slate-400">
                          {new Date(disp.createdAt).toLocaleDateString('pt-BR')}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* MODAL: Associar Pedido Manualmente */}
      {associatingItem && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-slate-700 rounded-xl p-5 max-w-md w-full shadow-2xl space-y-4">
            <h4 className="text-sm font-semibold text-white">Associar Pedido Manualmente (De/Para)</h4>
            <p className="text-xs text-slate-400">
              Vincule este lançamento sem ID ao pedido correspondente no catálogo e informe a justificativa para a trilha de auditoria.
            </p>

            <div className="space-y-3 text-xs">
              <div>
                <label className="text-slate-300 block mb-1">ID do Pedido no Marketplace (ex: MLB-ORD-9025):</label>
                <input
                  type="text"
                  value={manualOrderIdInput}
                  onChange={(e) => setManualOrderIdInput(e.target.value)}
                  placeholder="Ex: MLB-ORD-9025"
                  className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-1.5 text-slate-200"
                />
              </div>

              <div>
                <label className="text-slate-300 block mb-1">Justificativa da Associação:</label>
                <textarea
                  rows={2}
                  value={manualJustificationInput}
                  onChange={(e) => setManualJustificationInput(e.target.value)}
                  placeholder="Ex: Pedido localizado via valor líquido e horário do repasse no extrato..."
                  className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-1.5 text-slate-200"
                />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setAssociatingItem(null)}
                className="px-3 py-1.5 bg-slate-800 text-slate-300 hover:text-white rounded-lg text-xs"
              >
                Cancelar
              </button>
              <button
                onClick={handleAssociateOrder}
                disabled={!manualOrderIdInput.trim() || !manualJustificationInput.trim()}
                className="px-3 py-1.5 bg-teal-600 hover:bg-teal-500 disabled:opacity-50 text-white rounded-lg text-xs font-semibold"
              >
                Confirmar Associação
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL: Ignorar Divergência */}
      {ignoringItem && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-slate-700 rounded-xl p-5 max-w-md w-full shadow-2xl space-y-4">
            <h4 className="text-sm font-semibold text-white">Ignorar Divergência de Valor</h4>
            <p className="text-xs text-slate-400">
              Registrar tolerância de divergência no pedido <strong>{ignoringItem.externalOrderId}</strong>. A justificativa ficará salva de forma imutável no log de auditoria.
            </p>

            <div className="text-xs">
              <label className="text-slate-300 block mb-1">Motivo / Justificativa Comercial:</label>
              <textarea
                rows={3}
                value={ignoreReasonInput}
                onChange={(e) => setIgnoreReasonInput(e.target.value)}
                placeholder="Ex: Diferença de centavos decorrente de arredondamento de ICMS/DIFAL..."
                className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-1.5 text-slate-200"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setIgnoringItem(null)}
                className="px-3 py-1.5 bg-slate-800 text-slate-300 hover:text-white rounded-lg text-xs"
              >
                Cancelar
              </button>
              <button
                onClick={handleIgnoreDiscrepancy}
                disabled={!ignoreReasonInput.trim()}
                className="px-3 py-1.5 bg-teal-600 hover:bg-teal-500 disabled:opacity-50 text-white rounded-lg text-xs font-semibold"
              >
                Confirmar e Conciliar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL: Criar Contestação */}
      {disputingItem && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-slate-700 rounded-xl p-5 max-w-md w-full shadow-2xl space-y-4">
            <h4 className="text-sm font-semibold text-white">Registrar Contestação de Repasse</h4>
            <div className="p-2.5 bg-amber-500/10 border border-amber-500/30 rounded-lg text-[11px] text-amber-300 flex items-start gap-2">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              <span>
                <strong>Atenção:</strong> O protocolo cadastrado aqui é para controle gerencial interno do seller. O envio efetivo à adquirente/marketplace deve ser feito pelo respectivo canal oficial.
              </span>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <label className="text-slate-300 block mb-1">Canal de Contestação:</label>
                <select
                  value={disputeChannelInput}
                  onChange={(e) => setDisputeChannelInput(e.target.value as any)}
                  className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-1.5 text-slate-200"
                >
                  <option value="MARKETPLACE_SUPPORT">Suporte ao Seller (Chamado)</option>
                  <option value="SELLER_PORTAL">Portal Financeiro do Marketplace</option>
                  <option value="INTERNAL_AUDIT">Auditoria Interna do Seller</option>
                  <option value="LEGAL">Notificação Jurídica</option>
                </select>
              </div>

              <div>
                <label className="text-slate-300 block mb-1">Número do Protocolo Externo (opcional):</label>
                <input
                  type="text"
                  value={disputeProtocolInput}
                  onChange={(e) => setDisputeProtocolInput(e.target.value)}
                  placeholder="Ex: PROT-MLB-1928374"
                  className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-1.5 text-slate-200"
                />
              </div>

              <div>
                <label className="text-slate-300 block mb-1">Motivo Detalhado:</label>
                <textarea
                  rows={3}
                  value={disputeReasonInput}
                  onChange={(e) => setDisputeReasonInput(e.target.value)}
                  placeholder="Ex: Débito indevido de frete por divergência de cubagem..."
                  className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-1.5 text-slate-200"
                />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setDisputingItem(null)}
                className="px-3 py-1.5 bg-slate-800 text-slate-300 hover:text-white rounded-lg text-xs"
              >
                Cancelar
              </button>
              <button
                onClick={handleCreateDispute}
                disabled={!disputeReasonInput.trim()}
                className="px-3 py-1.5 bg-amber-600 hover:bg-amber-500 disabled:opacity-50 text-white rounded-lg text-xs font-semibold"
              >
                Registrar Protocolo
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL: Simular Importação de Extrato */}
      {isImportModalOpen && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-slate-700 rounded-xl p-5 max-w-md w-full shadow-2xl space-y-4">
            <h4 className="text-sm font-semibold text-white">Importar Extrato de Liquidação / Repasse</h4>
            <p className="text-xs text-slate-400">
              Simule a importação de um lote de repasse de marketplace com verificação de idempotência.
            </p>

            {importMessage && (
              <div className={`p-3 rounded-lg text-xs flex items-center gap-2 ${
                importMessage.type === 'success'
                  ? 'bg-emerald-500/10 border border-emerald-500/30 text-emerald-300'
                  : 'bg-amber-500/10 border border-amber-500/30 text-amber-300'
              }`}>
                {importMessage.type === 'success' ? <CheckCircle2 className="w-4 h-4 shrink-0" /> : <AlertTriangle className="w-4 h-4 shrink-0" />}
                <span>{importMessage.text}</span>
              </div>
            )}

            <div className="space-y-3 text-xs">
              <div>
                <label className="text-slate-300 block mb-1">Marketplace:</label>
                <select
                  value={importForm.marketplace}
                  onChange={(e) => setImportForm(prev => ({ ...prev, marketplace: e.target.value }))}
                  className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-1.5 text-slate-200"
                >
                  <option value="MERCADO_LIVRE">Mercado Livre</option>
                  <option value="SHOPEE">Shopee</option>
                  <option value="AMAZON">Amazon</option>
                </select>
              </div>

              <div>
                <label className="text-slate-300 block mb-1">ID da Liquidação / Lote Externo:</label>
                <input
                  type="text"
                  value={importForm.externalSettlementId}
                  onChange={(e) => setImportForm(prev => ({ ...prev, externalSettlementId: e.target.value }))}
                  placeholder="Ex: SETTL-MLB-2026-W47"
                  className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-1.5 text-slate-200 font-mono"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="text-slate-300 block mb-1">Valor Bruto (R$):</label>
                  <input
                    type="number"
                    value={importForm.grossAmount}
                    onChange={(e) => setImportForm(prev => ({ ...prev, grossAmount: e.target.value }))}
                    className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-1.5 text-slate-200 font-mono"
                  />
                </div>
                <div>
                  <label className="text-slate-300 block mb-1">Comissões/Taxas (R$):</label>
                  <input
                    type="number"
                    value={importForm.feeAmount}
                    onChange={(e) => setImportForm(prev => ({ ...prev, feeAmount: e.target.value }))}
                    className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-1.5 text-slate-200 font-mono"
                  />
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => { setIsImportModalOpen(false); setImportMessage(null); }}
                className="px-3 py-1.5 bg-slate-800 text-slate-300 hover:text-white rounded-lg text-xs"
              >
                Fechar
              </button>
              <button
                onClick={handleSimulateImport}
                className="px-3 py-1.5 bg-teal-600 hover:bg-teal-500 text-white rounded-lg text-xs font-semibold"
              >
                Executar Ingestão
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
