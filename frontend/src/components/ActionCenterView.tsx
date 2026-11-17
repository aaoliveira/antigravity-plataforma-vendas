'use client';

import React, { useState } from 'react';

export type AlertSeverity = 'INFO' | 'WARNING' | 'CRITICAL';
export type AlertDomain = 'FINANCIAL' | 'MARKETPLACE_SYNC' | 'LISTING_PRICE' | 'INVENTORY' | 'COMPETITOR';
export type AlertStatus = 'NEW' | 'READ' | 'RESOLVED' | 'IGNORED';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED';
export type NotificationChannel = 'IN_APP' | 'EMAIL' | 'WEBHOOK';
export type DeliveryStatus = 'SENT' | 'SUPPRESSED_NO_OPT_IN' | 'SUPPRESSED_COOLDOWN' | 'FAILED';

export interface AlertItem {
  id: string;
  ruleId?: string;
  title: string;
  description: string;
  severity: AlertSeverity;
  domainType: AlertDomain;
  status: AlertStatus;
  scopeType: string;
  scopeId?: string;
  provenance: string;
  evidenceContext: string;
  dedupKey: string;
  createdAt: string;
}

export interface TaskItem {
  id: string;
  alertId?: string;
  title: string;
  description: string;
  priority: TaskPriority;
  status: TaskStatus;
  assignedToUserId?: string;
  dueDate?: string;
  evidenceSummary?: string;
  completionNotes?: string;
  createdByUserId: string;
  createdAt: string;
  completedAt?: string;
}

export interface TaskAuditItem {
  id: string;
  taskId: string;
  actorUserId: string;
  actionType: string;
  previousState?: string;
  newState: string;
  notes?: string;
  createdAt: string;
}

export interface AlertRuleItem {
  id: string;
  name: string;
  ruleType: string;
  scopeType: string;
  thresholdValue: number;
  thresholdOperator: string;
  timeWindowMinutes: number;
  cooldownMinutes: number;
  severity: AlertSeverity;
  active: boolean;
}

export interface NotificationPrefItem {
  channel: NotificationChannel;
  optedIn: boolean;
  targetAddress: string;
  frequencyLimitPerHour: number;
}

export interface NotificationDeliveryItem {
  id: string;
  alertId: string;
  channel: NotificationChannel;
  recipient: string;
  deliveryStatus: DeliveryStatus;
  errorMessage?: string;
  sentAt: string;
}

const initialDemoRules: AlertRuleItem[] = [
  {
    id: 'rule-1',
    name: 'Margem de Contribuição Mínima',
    ruleType: 'MARGIN_THRESHOLD',
    scopeType: 'ORGANIZATION',
    thresholdValue: 12.0,
    thresholdOperator: 'LESS_THAN',
    timeWindowMinutes: 60,
    cooldownMinutes: 120,
    severity: 'CRITICAL',
    active: true,
  },
  {
    id: 'rule-2',
    name: 'Risco de Ruptura de Estoque',
    ruleType: 'STOCK_DEPLETION',
    scopeType: 'STORE',
    thresholdValue: 5.0,
    thresholdOperator: 'LESS_THAN',
    timeWindowMinutes: 30,
    cooldownMinutes: 60,
    severity: 'WARNING',
    active: true,
  },
  {
    id: 'rule-3',
    name: 'Falha Recorrente de Sincronização',
    ruleType: 'SYNC_FAILURE',
    scopeType: 'ORGANIZATION',
    thresholdValue: 3.0,
    thresholdOperator: 'GREATER_THAN',
    timeWindowMinutes: 60,
    cooldownMinutes: 180,
    severity: 'CRITICAL',
    active: true,
  },
];

const initialDemoAlerts: AlertItem[] = [
  {
    id: 'alt-101',
    ruleId: 'rule-1',
    title: 'Margem Negativa no Pedido #MLB-882910',
    description: 'Pedido com comissão de 18% e tarifa de frete acima do teto gerou margem de contribuição de -2.40% (R$ -5.80).',
    severity: 'CRITICAL',
    domainType: 'FINANCIAL',
    status: 'NEW',
    scopeType: 'STORE',
    scopeId: 'store-1',
    provenance: 'CALCULATED',
    evidenceContext: '{"orderId":"MLB-882910","revenue":120.00,"cost":85.00,"commission":21.60,"shipping":19.20,"tax":0.00,"margin":-2.40}',
    dedupKey: 'dedup:rule-1:order:882910',
    createdAt: new Date(Date.now() - 1000 * 60 * 25).toISOString(),
  },
  {
    id: 'alt-102',
    ruleId: 'rule-2',
    title: 'Estoque Baixo: Teclado Mecânico RGB',
    description: 'Estoque atual é de 2 unidades no Mercado Livre, abaixo do limiar de segurança configurado (5 unidades).',
    severity: 'WARNING',
    domainType: 'INVENTORY',
    status: 'NEW',
    scopeType: 'STORE',
    scopeId: 'store-1',
    provenance: 'OBSERVED',
    evidenceContext: '{"sku":"SKU-KB-001","stock":2,"threshold":5}',
    dedupKey: 'dedup:rule-2:sku:kb-001',
    createdAt: new Date(Date.now() - 1000 * 60 * 75).toISOString(),
  },
  {
    id: 'alt-103',
    title: 'Desvio de Preço vs Concorrente Principal',
    description: 'Concorrente direto reduziu o preço de R$ 189,90 para R$ 169,00 (-11.01%), superando o preço mínimo calculado.',
    severity: 'INFO',
    domainType: 'COMPETITOR',
    status: 'READ',
    scopeType: 'ORGANIZATION',
    provenance: 'OBSERVED',
    evidenceContext: '{"competitorItemId":"MLB99281","oldPrice":189.90,"newPrice":169.00,"deltaPercent":-11.01}',
    dedupKey: 'dedup:comp:mlb99281',
    createdAt: new Date(Date.now() - 1000 * 60 * 180).toISOString(),
  },
];

const initialDemoTasks: TaskItem[] = [
  {
    id: 'tsk-001',
    alertId: 'alt-101',
    title: 'Ajustar regra de preço mínimo para evitar margem negativa',
    description: 'Revisar custos de frete e parametrizar alíquota fiscal para absorver tarifa do Mercado Livre no SKU correspondente.',
    priority: 'HIGH',
    status: 'IN_PROGRESS',
    assignedToUserId: 'user-ops-1',
    dueDate: new Date(Date.now() + 1000 * 60 * 60 * 24).toISOString().split('T')[0],
    evidenceSummary: 'Origem: Alerta [alt-101] | Severidade: CRITICAL | Domínio: FINANCIAL | Margem calculada: -2.40%',
    createdByUserId: 'user-admin',
    createdAt: new Date(Date.now() - 1000 * 60 * 20).toISOString(),
  },
];

const initialDemoAudits: Record<string, TaskAuditItem[]> = {
  'tsk-001': [
    {
      id: 'aud-1',
      taskId: 'tsk-001',
      actorUserId: 'user-admin',
      actionType: 'CREATED',
      newState: 'TODO',
      notes: 'Tarefa criada automaticamente a partir do alerta alt-101.',
      createdAt: new Date(Date.now() - 1000 * 60 * 20).toISOString(),
    },
    {
      id: 'aud-2',
      taskId: 'tsk-001',
      actorUserId: 'user-ops-1',
      actionType: 'STATUS_CHANGED',
      previousState: 'TODO',
      newState: 'IN_PROGRESS',
      notes: 'Iniciando revisão da planilha de custos do fornecedor.',
      createdAt: new Date(Date.now() - 1000 * 60 * 10).toISOString(),
    },
  ],
};

const initialDemoPrefs: NotificationPrefItem[] = [
  { channel: 'IN_APP', optedIn: true, targetAddress: 'Painel Web', frequencyLimitPerHour: 20 },
  { channel: 'EMAIL', optedIn: true, targetAddress: 'operacao@sellerintelligence.com.br', frequencyLimitPerHour: 5 },
  { channel: 'WEBHOOK', optedIn: false, targetAddress: 'https://api.erp-externo.com.br/alerts', frequencyLimitPerHour: 10 },
];

const initialDemoDeliveries: NotificationDeliveryItem[] = [
  {
    id: 'del-1',
    alertId: 'alt-101',
    channel: 'IN_APP',
    recipient: 'Painel Web',
    deliveryStatus: 'SENT',
    sentAt: new Date(Date.now() - 1000 * 60 * 25).toISOString(),
  },
  {
    id: 'del-2',
    alertId: 'alt-101',
    channel: 'EMAIL',
    recipient: 'operacao@sellerintelligence.com.br',
    deliveryStatus: 'SENT',
    sentAt: new Date(Date.now() - 1000 * 60 * 25).toISOString(),
  },
  {
    id: 'del-3',
    alertId: 'alt-101',
    channel: 'WEBHOOK',
    recipient: 'https://api.erp-externo.com.br/alerts',
    deliveryStatus: 'SUPPRESSED_NO_OPT_IN',
    errorMessage: 'Envio bloqueado: canal desativado sem consentimento/opt-in do usuário.',
    sentAt: new Date(Date.now() - 1000 * 60 * 25).toISOString(),
  },
];

export const ActionCenterView: React.FC = () => {
  const [activeSubTab, setActiveSubTab] = useState<'ALERTS' | 'TASKS' | 'RULES' | 'NOTIFICATIONS'>('ALERTS');
  const [alerts, setAlerts] = useState<AlertItem[]>(initialDemoAlerts);
  const [tasks, setTasks] = useState<TaskItem[]>(initialDemoTasks);
  const [rules, setRules] = useState<AlertRuleItem[]>(initialDemoRules);
  const [audits, setAudits] = useState<Record<string, TaskAuditItem[]>>(initialDemoAudits);
  const [prefs, setPrefs] = useState<NotificationPrefItem[]>(initialDemoPrefs);
  const [deliveries, setDeliveries] = useState<NotificationDeliveryItem[]>(initialDemoDeliveries);

  // Filters
  const [alertStatusFilter, setAlertStatusFilter] = useState<string>('ALL');
  const [alertDomainFilter, setAlertDomainFilter] = useState<string>('ALL');
  const [taskStatusFilter, setTaskStatusFilter] = useState<string>('ALL');

  // Modals & Forms
  const [convertingAlert, setConvertingAlert] = useState<AlertItem | null>(null);
  const [taskPriority, setTaskPriority] = useState<TaskPriority>('HIGH');
  const [taskAssignee, setTaskAssignee] = useState<string>('user-ops-1');
  const [taskDueDate, setTaskDueDate] = useState<string>('');
  const [taskCustomNotes, setTaskCustomNotes] = useState<string>('');

  const [simulatingRule, setSimulatingRule] = useState<AlertRuleItem | null>(null);
  const [simulationValue, setSimulationValue] = useState<string>('9.50');
  const [simulationContext, setSimulationContext] = useState<string>('Pedido Teste Simulado');
  const [simulationResult, setSimulationResult] = useState<{ triggered: boolean; explanation: string; alertId?: string } | null>(null);

  const [viewingAuditsForTask, setViewingAuditsForTask] = useState<TaskItem | null>(null);

  // KPI counts
  const totalAlerts = alerts.length;
  const newAlerts = alerts.filter((a) => a.status === 'NEW').length;
  const criticalAlerts = alerts.filter((a) => a.severity === 'CRITICAL').length;
  const resolvedAlerts = alerts.filter((a) => a.status === 'RESOLVED').length;

  const filteredAlerts = alerts.filter((a) => {
    if (alertStatusFilter !== 'ALL' && a.status !== alertStatusFilter) return false;
    if (alertDomainFilter !== 'ALL' && a.domainType !== alertDomainFilter) return false;
    return true;
  });

  const filteredTasks = tasks.filter((t) => {
    if (taskStatusFilter !== 'ALL' && t.status !== taskStatusFilter) return false;
    return true;
  });

  // Handlers
  const handleUpdateAlertStatus = (alertId: string, newStatus: AlertStatus) => {
    setAlerts((prev) =>
      prev.map((a) => (a.id === alertId ? { ...a, status: newStatus } : a))
    );
  };

  const handleConvertAlertToTask = () => {
    if (!convertingAlert) return;

    const newTask: TaskItem = {
      id: `tsk-${Date.now()}`,
      alertId: convertingAlert.id,
      title: `Resolver: ${convertingAlert.title}`,
      description: `${convertingAlert.description}${taskCustomNotes ? `\nObservação: ${taskCustomNotes}` : ''}`,
      priority: taskPriority,
      status: 'TODO',
      assignedToUserId: taskAssignee,
      dueDate: taskDueDate || undefined,
      evidenceSummary: `Origem: Alerta [${convertingAlert.id}] | Severidade: ${convertingAlert.severity} | Domínio: ${convertingAlert.domainType} | Evidência: ${convertingAlert.evidenceContext}`,
      createdByUserId: 'user-admin',
      createdAt: new Date().toISOString(),
    };

    const newAudit: TaskAuditItem = {
      id: `aud-${Date.now()}`,
      taskId: newTask.id,
      actorUserId: 'user-admin',
      actionType: 'CREATED',
      newState: 'TODO',
      notes: `Tarefa criada manualmente a partir do alerta ${convertingAlert.id}`,
      createdAt: new Date().toISOString(),
    };

    setTasks((prev) => [newTask, ...prev]);
    setAudits((prev) => ({
      ...prev,
      [newTask.id]: [newAudit],
    }));

    handleUpdateAlertStatus(convertingAlert.id, 'READ');
    setConvertingAlert(null);
    setTaskCustomNotes('');
  };

  const handleUpdateTaskStatus = (taskId: string, newStatus: TaskStatus) => {
    setTasks((prev) =>
      prev.map((t) => {
        if (t.id === taskId) {
          const updated = {
            ...t,
            status: newStatus,
            completedAt: newStatus === 'DONE' ? new Date().toISOString() : undefined,
          };
          if (newStatus === 'DONE' && t.alertId) {
            handleUpdateAlertStatus(t.alertId, 'RESOLVED');
          }
          return updated;
        }
        return t;
      })
    );

    const auditEntry: TaskAuditItem = {
      id: `aud-${Date.now()}`,
      taskId,
      actorUserId: 'user-admin',
      actionType: newStatus === 'DONE' ? 'COMPLETED' : 'STATUS_CHANGED',
      newState: newStatus,
      notes: `Status alterado para ${newStatus}`,
      createdAt: new Date().toISOString(),
    };

    setAudits((prev) => ({
      ...prev,
      [taskId]: [...(prev[taskId] || []), auditEntry],
    }));
  };

  const handleRunSimulation = () => {
    if (!simulatingRule) return;

    const obs = parseFloat(simulationValue);
    const thresh = simulatingRule.thresholdValue;
    let triggered = false;

    if (simulatingRule.thresholdOperator === 'LESS_THAN') {
      triggered = obs < thresh;
    } else if (simulatingRule.thresholdOperator === 'GREATER_THAN') {
      triggered = obs > thresh;
    } else {
      triggered = obs === thresh;
    }

    const explanation = triggered
      ? `Simulação DISPAROU: Valor observado (${obs.toFixed(2)}) violou a condição ${simulatingRule.thresholdOperator} (${thresh.toFixed(2)}) da regra '${simulatingRule.name}'.`
      : `Simulação NÃO disparou: Valor observado (${obs.toFixed(2)}) está em conformidade com o limiar (${thresh.toFixed(2)}) da regra '${simulatingRule.name}'.`;

    let generatedAlertId: string | undefined = undefined;
    if (triggered) {
      generatedAlertId = `alt-sim-${Date.now()}`;
      const simAlert: AlertItem = {
        id: generatedAlertId,
        ruleId: simulatingRule.id,
        title: `Simulação: ${simulatingRule.name}`,
        description: `${explanation} Contexto: ${simulationContext}`,
        severity: simulatingRule.severity,
        domainType: 'FINANCIAL',
        status: 'NEW',
        scopeType: simulatingRule.scopeType,
        provenance: 'CALCULATED',
        evidenceContext: `{"simulatedValue":${obs},"threshold":${thresh},"simulation":true}`,
        dedupKey: `sim:${simulatingRule.id}:${Date.now()}`,
        createdAt: new Date().toISOString(),
      };
      setAlerts((prev) => [simAlert, ...prev]);
    }

    setSimulationResult({
      triggered,
      explanation,
      alertId: generatedAlertId,
    });
  };

  const handleToggleOptIn = (channel: NotificationChannel) => {
    setPrefs((prev) =>
      prev.map((p) => (p.channel === channel ? { ...p, optedIn: !p.optedIn } : p))
    );
  };

  return (
    <div className="space-y-6">
      {/* Governance & Anti-Automation Banner */}
      <div className="bg-slate-900 border-l-4 border-emerald-500 p-4 rounded-r-lg shadow-sm">
        <div className="flex items-start justify-between">
          <div>
            <div className="flex items-center gap-2">
              <span className="font-semibold text-slate-100 text-sm">
                Smart Alerts &amp; Action Center (Fase 2)
              </span>
              <span className="bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-[10px] px-2 py-0.5 rounded font-mono">
                PRD FR-ACT-001 a 006
              </span>
            </div>
            <p className="text-xs text-slate-400 mt-1">
              Centralização de anomalias operacionais, motor de alertas deduplicados com cooldown e conversão em tarefas rastreáveis com auditoria imutável.
            </p>
          </div>
          <div className="text-right">
            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded bg-slate-800 text-[11px] text-slate-300 border border-slate-700">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
              Isolamento Anti-IDOR Ativo
            </span>
          </div>
        </div>
        <div className="mt-3 pt-3 border-t border-slate-800/80 text-[11px] text-amber-300/90 flex items-center gap-2">
          <svg className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>
            <strong>Invariante de Segurança PRD:</strong> Tarefas e alertas são puramente de gestão interna da equipe. O sistema <strong>não</strong> executa alterações externas de preços, pausas de anúncios ou compras automáticas em marketplaces sem aprovação humana expressa.
          </span>
        </div>
      </div>

      {/* Sub-tab Navigation */}
      <div className="flex border-b border-slate-700 space-x-6 text-sm font-medium">
        <button
          onClick={() => setActiveSubTab('ALERTS')}
          className={`pb-3 px-1 border-b-2 transition-colors flex items-center gap-2 ${
            activeSubTab === 'ALERTS'
              ? 'border-indigo-500 text-indigo-400 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <span>Inbox de Alertas</span>
          {newAlerts > 0 && (
            <span className="px-1.5 py-0.2 bg-red-500 text-white text-[10px] font-bold rounded-full">
              {newAlerts}
            </span>
          )}
        </button>
        <button
          onClick={() => setActiveSubTab('TASKS')}
          className={`pb-3 px-1 border-b-2 transition-colors flex items-center gap-2 ${
            activeSubTab === 'TASKS'
              ? 'border-indigo-500 text-indigo-400 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <span>Action Center (Tarefas)</span>
          <span className="px-1.5 py-0.2 bg-slate-800 text-slate-300 text-[10px] rounded-full border border-slate-700">
            {tasks.length}
          </span>
        </button>
        <button
          onClick={() => setActiveSubTab('RULES')}
          className={`pb-3 px-1 border-b-2 transition-colors flex items-center gap-2 ${
            activeSubTab === 'RULES'
              ? 'border-indigo-500 text-indigo-400 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <span>Catálogo de Regras &amp; Simulador</span>
          <span className="px-1.5 py-0.2 bg-slate-800 text-slate-300 text-[10px] rounded-full border border-slate-700">
            {rules.length}
          </span>
        </button>
        <button
          onClick={() => setActiveSubTab('NOTIFICATIONS')}
          className={`pb-3 px-1 border-b-2 transition-colors flex items-center gap-2 ${
            activeSubTab === 'NOTIFICATIONS'
              ? 'border-indigo-500 text-indigo-400 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <span>Governança &amp; Opt-In</span>
        </button>
      </div>

      {/* SUBTAB 1: INBOX DE ALERTAS */}
      {activeSubTab === 'ALERTS' && (
        <div className="space-y-6">
          {/* KPI Summary Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
            <div className="bg-slate-800/80 border border-slate-700 p-4 rounded-lg">
              <div className="text-xs text-slate-400 font-medium">Total de Alertas</div>
              <div className="text-2xl font-bold text-slate-100 mt-1">{totalAlerts}</div>
            </div>
            <div className="bg-slate-800/80 border border-slate-700 p-4 rounded-lg">
              <div className="text-xs text-red-400 font-medium">Alertas Novos</div>
              <div className="text-2xl font-bold text-red-400 mt-1">{newAlerts}</div>
            </div>
            <div className="bg-slate-800/80 border border-slate-700 p-4 rounded-lg">
              <div className="text-xs text-amber-400 font-medium">Severidade Crítica</div>
              <div className="text-2xl font-bold text-amber-400 mt-1">{criticalAlerts}</div>
            </div>
            <div className="bg-slate-800/80 border border-slate-700 p-4 rounded-lg">
              <div className="text-xs text-emerald-400 font-medium">Resolvidos</div>
              <div className="text-2xl font-bold text-emerald-400 mt-1">{resolvedAlerts}</div>
            </div>
          </div>

          {/* Filters Bar */}
          <div className="flex flex-wrap items-center justify-between gap-4 bg-slate-800/40 p-3 rounded-lg border border-slate-700/60">
            <div className="flex items-center gap-3">
              <label className="text-xs text-slate-400">Filtrar por Status:</label>
              <select
                aria-label="Filtrar por Status"
                value={alertStatusFilter}
                onChange={(e) => setAlertStatusFilter(e.target.value)}
                className="bg-slate-800 border border-slate-700 text-slate-200 text-xs rounded px-2.5 py-1.5 focus:outline-none focus:border-indigo-500"
              >
                <option value="ALL">Todos os Estados</option>
                <option value="NEW">Novos (Não lidos)</option>
                <option value="READ">Lidos</option>
                <option value="RESOLVED">Resolvidos</option>
                <option value="IGNORED">Ignorados</option>
              </select>

              <label className="text-xs text-slate-400 ml-2">Domínio:</label>
              <select
                aria-label="Filtrar por Domínio"
                value={alertDomainFilter}
                onChange={(e) => setAlertDomainFilter(e.target.value)}
                className="bg-slate-800 border border-slate-700 text-slate-200 text-xs rounded px-2.5 py-1.5 focus:outline-none focus:border-indigo-500"
              >
                <option value="ALL">Todos os Domínios</option>
                <option value="FINANCIAL">Financeiro &amp; Margem</option>
                <option value="INVENTORY">Estoque &amp; Ruptura</option>
                <option value="MARKETPLACE_SYNC">Sincronização &amp; APIs</option>
                <option value="COMPETITOR">Concorrência &amp; Preço</option>
              </select>
            </div>
            <div className="text-xs text-slate-400">
              Exibindo <strong>{filteredAlerts.length}</strong> alerta(s)
            </div>
          </div>

          {/* Alerts List */}
          <div className="space-y-3">
            {filteredAlerts.map((alert) => (
              <div
                key={alert.id}
                className={`p-4 rounded-lg border transition-all ${
                  alert.severity === 'CRITICAL'
                    ? 'bg-red-950/20 border-red-900/50 hover:border-red-700'
                    : alert.severity === 'WARNING'
                    ? 'bg-amber-950/20 border-amber-900/50 hover:border-amber-700'
                    : 'bg-slate-800/40 border-slate-700/60 hover:border-slate-600'
                }`}
              >
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span
                        className={`text-[10px] font-bold px-2 py-0.5 rounded border ${
                          alert.severity === 'CRITICAL'
                            ? 'bg-red-500/20 text-red-300 border-red-500/40'
                            : alert.severity === 'WARNING'
                            ? 'bg-amber-500/20 text-amber-300 border-amber-500/40'
                            : 'bg-blue-500/20 text-blue-300 border-blue-500/40'
                        }`}
                      >
                        {alert.severity}
                      </span>
                      <span className="text-[10px] bg-slate-800 text-slate-300 px-2 py-0.5 rounded border border-slate-700">
                        {alert.domainType}
                      </span>
                      <span className="text-[10px] bg-purple-500/10 text-purple-300 px-2 py-0.5 rounded border border-purple-500/30">
                        {alert.provenance}
                      </span>
                      <span
                        className={`text-[10px] font-medium px-2 py-0.5 rounded ${
                          alert.status === 'NEW'
                            ? 'bg-emerald-500/20 text-emerald-300'
                            : alert.status === 'RESOLVED'
                            ? 'bg-slate-700 text-slate-300'
                            : 'bg-slate-800 text-slate-400'
                        }`}
                      >
                        {alert.status}
                      </span>
                      <span className="text-[10px] text-slate-400">
                        {new Date(alert.createdAt).toLocaleString('pt-BR')}
                      </span>
                    </div>
                    <h4 className="text-sm font-semibold text-slate-100">{alert.title}</h4>
                    <p className="text-xs text-slate-300">{alert.description}</p>
                  </div>

                  {/* Action buttons */}
                  <div className="flex items-center gap-2 shrink-0">
                    <button
                      onClick={() => setConvertingAlert(alert)}
                      className="px-2.5 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded text-xs font-medium transition-colors shadow-sm flex items-center gap-1.5"
                    >
                      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                      </svg>
                      Converter em Tarefa
                    </button>
                    {alert.status !== 'RESOLVED' && (
                      <button
                        onClick={() => handleUpdateAlertStatus(alert.id, 'RESOLVED')}
                        className="px-2 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded text-xs border border-slate-700 transition-colors"
                        title="Marcar como resolvido"
                      >
                        Resolver
                      </button>
                    )}
                    {alert.status === 'NEW' && (
                      <button
                        onClick={() => handleUpdateAlertStatus(alert.id, 'READ')}
                        className="px-2 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded text-xs border border-slate-700 transition-colors"
                        title="Marcar como lido"
                      >
                        Lido
                      </button>
                    )}
                  </div>
                </div>

                {/* Evidence Context Box */}
                <div className="mt-3 bg-slate-950/60 p-2.5 rounded border border-slate-800 text-[11px] font-mono text-slate-400">
                  <div className="text-[10px] text-slate-400 uppercase tracking-wider mb-1 font-sans">
                    Evidência / Contexto Auditável:
                  </div>
                  <div className="overflow-x-auto">{alert.evidenceContext}</div>
                  <div className="text-[10px] text-slate-400 mt-1 font-sans flex items-center gap-2">
                    <span>Chave Dedup: <code className="text-slate-400">{alert.dedupKey}</code></span>
                  </div>
                </div>
              </div>
            ))}

            {filteredAlerts.length === 0 && (
              <div className="text-center py-12 text-slate-500 text-xs border border-dashed border-slate-800 rounded-lg">
                Nenhum alerta encontrado para os filtros selecionados.
              </div>
            )}
          </div>
        </div>
      )}

      {/* SUBTAB 2: ACTION CENTER (TAREFAS OPERACIONAIS) */}
      {activeSubTab === 'TASKS' && (
        <div className="space-y-6">
          {/* Header & Filter */}
          <div className="flex flex-wrap items-center justify-between gap-4 bg-slate-800/40 p-3 rounded-lg border border-slate-700/60">
            <div className="flex items-center gap-3">
              <label className="text-xs text-slate-400">Status da Tarefa:</label>
              <select
                aria-label="Filtrar Status da Tarefa"
                value={taskStatusFilter}
                onChange={(e) => setTaskStatusFilter(e.target.value)}
                className="bg-slate-800 border border-slate-700 text-slate-200 text-xs rounded px-2.5 py-1.5 focus:outline-none focus:border-indigo-500"
              >
                <option value="ALL">Todas</option>
                <option value="TODO">A Fazer (TODO)</option>
                <option value="IN_PROGRESS">Em Andamento (IN_PROGRESS)</option>
                <option value="DONE">Concluídas (DONE)</option>
                <option value="CANCELLED">Canceladas</option>
              </select>
            </div>
            <div className="text-xs text-slate-400">
              Total de tarefas: <strong>{filteredTasks.length}</strong>
            </div>
          </div>

          {/* Tasks List */}
          <div className="space-y-3">
            {filteredTasks.map((task) => (
              <div
                key={task.id}
                className="bg-slate-800/40 border border-slate-700/60 rounded-lg p-4 space-y-3 hover:border-slate-600 transition-colors"
              >
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span
                        className={`text-[10px] font-bold px-2 py-0.5 rounded border ${
                          task.priority === 'URGENT' || task.priority === 'HIGH'
                            ? 'bg-red-500/20 text-red-300 border-red-500/40'
                            : task.priority === 'MEDIUM'
                            ? 'bg-amber-500/20 text-amber-300 border-amber-500/40'
                            : 'bg-slate-700 text-slate-300 border-slate-600'
                        }`}
                      >
                        Prioridade: {task.priority}
                      </span>
                      <span
                        className={`text-[10px] font-semibold px-2 py-0.5 rounded ${
                          task.status === 'DONE'
                            ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                            : task.status === 'IN_PROGRESS'
                            ? 'bg-blue-500/20 text-blue-300 border border-blue-500/30'
                            : 'bg-slate-800 text-slate-300 border border-slate-700'
                        }`}
                      >
                        {task.status}
                      </span>
                      {task.alertId && (
                        <span className="text-[10px] bg-indigo-500/10 text-indigo-300 px-2 py-0.5 rounded border border-indigo-500/30">
                          Vinculada ao Alerta: {task.alertId}
                        </span>
                      )}
                    </div>
                    <h4 className="text-sm font-semibold text-slate-100">{task.title}</h4>
                    <p className="text-xs text-slate-300 whitespace-pre-line">{task.description}</p>
                  </div>

                  {/* Status update actions */}
                  <div className="flex items-center gap-2 shrink-0">
                    {task.status === 'TODO' && (
                      <button
                        onClick={() => handleUpdateTaskStatus(task.id, 'IN_PROGRESS')}
                        className="px-2.5 py-1 bg-blue-600 hover:bg-blue-500 text-white rounded text-xs transition-colors"
                      >
                        Iniciar
                      </button>
                    )}
                    {task.status === 'IN_PROGRESS' && (
                      <button
                        onClick={() => handleUpdateTaskStatus(task.id, 'DONE')}
                        className="px-2.5 py-1 bg-emerald-600 hover:bg-emerald-500 text-white rounded text-xs transition-colors"
                      >
                        Concluir
                      </button>
                    )}
                    {task.status !== 'DONE' && task.status !== 'CANCELLED' && (
                      <button
                        onClick={() => handleUpdateTaskStatus(task.id, 'CANCELLED')}
                        className="px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-slate-400 rounded text-xs border border-slate-700 transition-colors"
                      >
                        Cancelar
                      </button>
                    )}
                    <button
                      onClick={() => setViewingAuditsForTask(task)}
                      className="px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-indigo-400 rounded text-xs border border-slate-700 transition-colors flex items-center gap-1"
                    >
                      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                      Auditoria
                    </button>
                  </div>
                </div>

                {/* Evidence & Assignee Details */}
                <div className="bg-slate-950/40 p-2.5 rounded border border-slate-800/80 text-[11px] text-slate-400 space-y-1">
                  {task.evidenceSummary && (
                    <div>
                      <span className="text-slate-400">Resumo de Evidência:</span>{' '}
                      <span className="text-slate-300">{task.evidenceSummary}</span>
                    </div>
                  )}
                  <div className="flex flex-wrap items-center gap-4 text-[10px] text-slate-400 pt-1">
                    <span>Responsável: <strong className="text-slate-300">{task.assignedToUserId || 'Não atribuído'}</strong></span>
                    {task.dueDate && <span>Prazo: <strong className="text-slate-300">{task.dueDate}</strong></span>}
                    <span>Criado em: {new Date(task.createdAt).toLocaleString('pt-BR')}</span>
                    {task.completedAt && (
                      <span className="text-emerald-400">Concluído em: {new Date(task.completedAt).toLocaleString('pt-BR')}</span>
                    )}
                  </div>
                </div>
              </div>
            ))}

            {filteredTasks.length === 0 && (
              <div className="text-center py-12 text-slate-500 text-xs border border-dashed border-slate-800 rounded-lg">
                Nenhuma tarefa operacional encontrada.
              </div>
            )}
          </div>
        </div>
      )}

      {/* SUBTAB 3: CATÁLOGO DE REGRAS & SIMULADOR */}
      {activeSubTab === 'RULES' && (
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-slate-100">
              Regras Parametrizadas de Alerta ({rules.length})
            </h3>
            <span className="text-xs text-slate-400">
              Regras avaliam margens, preços mínimos, quedas de vendas e quebras de sincronização.
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {rules.map((rule) => (
              <div
                key={rule.id}
                className="bg-slate-800/40 border border-slate-700/60 rounded-lg p-4 space-y-3 flex flex-col justify-between"
              >
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded border ${
                        rule.severity === 'CRITICAL'
                          ? 'bg-red-500/20 text-red-300 border-red-500/40'
                          : 'bg-amber-500/20 text-amber-300 border-amber-500/40'
                      }`}
                    >
                      {rule.severity}
                    </span>
                    <span className="text-[10px] text-slate-400">{rule.ruleType}</span>
                  </div>
                  <h4 className="text-sm font-semibold text-slate-100">{rule.name}</h4>
                  <div className="text-xs text-slate-300 space-y-1">
                    <div>
                      Limiar:{' '}
                      <strong className="text-indigo-300">
                        {rule.thresholdOperator} {rule.thresholdValue}
                      </strong>
                    </div>
                    <div>Escopo: <strong className="text-slate-400">{rule.scopeType}</strong></div>
                    <div>Janela: {rule.timeWindowMinutes} min | Cooldown: {rule.cooldownMinutes} min</div>
                  </div>
                </div>

                <div className="pt-3 border-t border-slate-700/60 flex items-center justify-between">
                  <span className={`text-[11px] ${rule.active ? 'text-emerald-400' : 'text-slate-500'}`}>
                    {rule.active ? '● Ativa' : '○ Pausada'}
                  </span>
                  <button
                    onClick={() => {
                      setSimulatingRule(rule);
                      setSimulationResult(null);
                    }}
                    className="px-2.5 py-1 bg-indigo-600 hover:bg-indigo-500 text-white rounded text-xs font-medium transition-colors flex items-center gap-1"
                  >
                    <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    Testar / Simular
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* SUBTAB 4: GOVERNANÇA & OPT-IN */}
      {activeSubTab === 'NOTIFICATIONS' && (
        <div className="space-y-6">
          <div className="bg-slate-900 border border-slate-800 p-4 rounded-lg space-y-3">
            <h3 className="text-sm font-semibold text-slate-100 flex items-center gap-2">
              <svg className="w-4 h-4 text-emerald-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
              </svg>
              Consentimento e Governança Anti-Spam (LGPD &amp; Opt-In)
            </h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              Em estrita conformidade com a LGPD e boas práticas de observabilidade, nenhum canal externo de notificação (E-mail ou Webhook) envia dados sem consentimento explícito prévio do usuário. Disparos rejeitados por ausência de opt-in ou limites de frequência horária são auditados e gravados com status de supressão.
            </p>
          </div>

          {/* Preferences Table */}
          <div className="bg-slate-800/40 border border-slate-700/60 rounded-lg overflow-hidden">
            <div className="p-3 bg-slate-800/80 border-b border-slate-700 text-xs font-semibold text-slate-200">
              Canais de Notificação Configurados
            </div>
            <table className="w-full text-xs text-left">
              <thead className="bg-slate-900/40 text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="p-3">Canal</th>
                  <th className="p-3">Destino / Endereço</th>
                  <th className="p-3">Limite por Hora</th>
                  <th className="p-3">Consentimento (Opt-In)</th>
                  <th className="p-3 text-right">Ação</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/80">
                {prefs.map((p) => (
                  <tr key={p.channel} className="hover:bg-slate-800/20">
                    <td className="p-3 font-semibold text-slate-200">{p.channel}</td>
                    <td className="p-3 text-slate-400">{p.targetAddress}</td>
                    <td className="p-3 text-slate-400">{p.frequencyLimitPerHour} envios/hora</td>
                    <td className="p-3">
                      <span
                        className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-semibold ${
                          p.optedIn
                            ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40'
                            : 'bg-red-500/20 text-red-300 border border-red-500/40'
                        }`}
                      >
                        {p.optedIn ? 'OPT-IN ATIVO' : 'SEM CONSENTIMENTO'}
                      </span>
                    </td>
                    <td className="p-3 text-right">
                      <button
                        onClick={() => handleToggleOptIn(p.channel)}
                        className={`px-2.5 py-1 rounded text-xs transition-colors ${
                          p.optedIn
                            ? 'bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700'
                            : 'bg-emerald-600 hover:bg-emerald-500 text-white'
                        }`}
                      >
                        {p.optedIn ? 'Revogar Opt-In' : 'Habilitar Opt-In'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Delivery & Suppression History Table */}
          <div className="bg-slate-800/40 border border-slate-700/60 rounded-lg overflow-hidden space-y-2">
            <div className="p-3 bg-slate-800/80 border-b border-slate-700 text-xs font-semibold text-slate-200 flex items-center justify-between">
              <span>Histórico de Entregas e Supressões Auditadas</span>
              <span className="text-[10px] text-slate-400">Total: {deliveries.length} registros</span>
            </div>
            <table className="w-full text-xs text-left">
              <thead className="bg-slate-900/40 text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="p-3">Data/Hora</th>
                  <th className="p-3">Alerta</th>
                  <th className="p-3">Canal</th>
                  <th className="p-3">Destinatário</th>
                  <th className="p-3">Status de Entrega</th>
                  <th className="p-3">Motivo / Mensagem</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/80">
                {deliveries.map((del) => (
                  <tr key={del.id} className="hover:bg-slate-800/20">
                    <td className="p-3 text-slate-400">
                      {new Date(del.sentAt).toLocaleString('pt-BR')}
                    </td>
                    <td className="p-3 text-slate-200 font-mono text-[11px]">{del.alertId}</td>
                    <td className="p-3 text-slate-300 font-semibold">{del.channel}</td>
                    <td className="p-3 text-slate-400">{del.recipient}</td>
                    <td className="p-3">
                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          del.deliveryStatus === 'SENT'
                            ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40'
                            : del.deliveryStatus === 'SUPPRESSED_NO_OPT_IN'
                            ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40'
                            : 'bg-red-500/20 text-red-300 border border-red-500/40'
                        }`}
                      >
                        {del.deliveryStatus}
                      </span>
                    </td>
                    <td className="p-3 text-slate-400 text-[11px]">
                      {del.errorMessage || '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* MODAL 1: CONVERTER ALERTA EM TAREFA OPERACIONAL */}
      {convertingAlert && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-slate-700 rounded-lg max-w-lg w-full p-5 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <h3 className="text-sm font-semibold text-slate-100">
                Converter Alerta em Tarefa Operacional
              </h3>
              <button
                onClick={() => setConvertingAlert(null)}
                className="text-slate-400 hover:text-slate-200"
              >
                ✕
              </button>
            </div>

            <div className="bg-slate-950 p-3 rounded border border-slate-800 text-xs space-y-1">
              <div className="font-semibold text-slate-200">{convertingAlert.title}</div>
              <div className="text-slate-400">{convertingAlert.description}</div>
              <div className="text-[10px] text-indigo-400 pt-1">
                Link imutável: Alerta ID {convertingAlert.id}
              </div>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <label className="block text-slate-300 mb-1 font-medium">Prioridade:</label>
                <select
                  aria-label="Prioridade da Tarefa"
                  value={taskPriority}
                  onChange={(e) => setTaskPriority(e.target.value as TaskPriority)}
                  className="w-full bg-slate-800 border border-slate-700 rounded p-2 text-slate-200 focus:outline-none focus:border-indigo-500"
                >
                  <option value="LOW">Baixa (LOW)</option>
                  <option value="MEDIUM">Média (MEDIUM)</option>
                  <option value="HIGH">Alta (HIGH)</option>
                  <option value="URGENT">Urgente (URGENT)</option>
                </select>
              </div>

              <div>
                <label className="block text-slate-300 mb-1 font-medium">Responsável (Membro do Tenant):</label>
                <select
                  aria-label="Responsável pela Tarefa"
                  value={taskAssignee}
                  onChange={(e) => setTaskAssignee(e.target.value)}
                  className="w-full bg-slate-800 border border-slate-700 rounded p-2 text-slate-200 focus:outline-none focus:border-indigo-500"
                >
                  <option value="user-ops-1">user-ops-1 (Operador de Preço)</option>
                  <option value="user-admin">user-admin (Administrador)</option>
                  <option value="user-compras">user-compras (Gestor de Fornecedores)</option>
                </select>
                <p className="text-[10px] text-slate-400 mt-0.5">
                  Proteção Anti-IDOR: Usuários fora da organização não podem ser atribuídos.
                </p>
              </div>

              <div>
                <label className="block text-slate-300 mb-1 font-medium">Prazo Estimado (Due Date):</label>
                <input
                  type="date"
                  aria-label="Prazo Estimado"
                  value={taskDueDate}
                  onChange={(e) => setTaskDueDate(e.target.value)}
                  className="w-full bg-slate-800 border border-slate-700 rounded p-2 text-slate-200 focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div>
                <label className="block text-slate-300 mb-1 font-medium">Notas / Instruções:</label>
                <textarea
                  rows={2}
                  placeholder="Ex: Verificar negociação de frete e atualizar preço mínimo..."
                  value={taskCustomNotes}
                  onChange={(e) => setTaskCustomNotes(e.target.value)}
                  className="w-full bg-slate-800 border border-slate-700 rounded p-2 text-slate-200 focus:outline-none focus:border-indigo-500"
                />
              </div>
            </div>

            <div className="pt-3 border-t border-slate-800 flex justify-end gap-2">
              <button
                onClick={() => setConvertingAlert(null)}
                className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded text-xs transition-colors"
              >
                Cancelar
              </button>
              <button
                onClick={handleConvertAlertToTask}
                className="px-4 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded text-xs font-semibold transition-colors"
              >
                Criar Tarefa Rastreável
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL 2: SIMULADOR DE REGRA */}
      {simulatingRule && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-slate-700 rounded-lg max-w-lg w-full p-5 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <h3 className="text-sm font-semibold text-slate-100">
                Simulador de Regra: {simulatingRule.name}
              </h3>
              <button
                onClick={() => setSimulatingRule(null)}
                className="text-slate-400 hover:text-slate-200"
              >
                ✕
              </button>
            </div>

            <div className="text-xs text-slate-300 space-y-2">
              <div>
                Condição da Regra:{' '}
                <strong className="text-indigo-400">
                  Valor {simulatingRule.thresholdOperator} {simulatingRule.thresholdValue}
                </strong>
              </div>
              <div>
                <label className="block text-slate-300 mb-1 font-medium">Valor Observado Simulado:</label>
                <input
                  type="number"
                  step="0.01"
                  value={simulationValue}
                  onChange={(e) => setSimulationValue(e.target.value)}
                  className="w-full bg-slate-800 border border-slate-700 rounded p-2 text-slate-200 focus:outline-none focus:border-indigo-500"
                />
              </div>
              <div>
                <label className="block text-slate-300 mb-1 font-medium">Contexto Sintético:</label>
                <input
                  type="text"
                  value={simulationContext}
                  onChange={(e) => setSimulationContext(e.target.value)}
                  className="w-full bg-slate-800 border border-slate-700 rounded p-2 text-slate-200 focus:outline-none focus:border-indigo-500"
                />
              </div>
            </div>

            {simulationResult && (
              <div
                className={`p-3 rounded border text-xs ${
                  simulationResult.triggered
                    ? 'bg-red-950/40 border-red-800 text-red-200'
                    : 'bg-emerald-950/40 border-emerald-800 text-emerald-200'
                }`}
              >
                <div className="font-bold mb-1">
                  {simulationResult.triggered ? '🚨 Alerta Disparado!' : '✅ Dentro da Conformidade'}
                </div>
                <div>{simulationResult.explanation}</div>
                {simulationResult.alertId && (
                  <div className="text-[10px] text-slate-400 mt-1">
                    Alerta gerado no Inbox: <code>{simulationResult.alertId}</code>
                  </div>
                )}
              </div>
            )}

            <div className="pt-3 border-t border-slate-800 flex justify-end gap-2">
              <button
                onClick={() => setSimulatingRule(null)}
                className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded text-xs transition-colors"
              >
                Fechar
              </button>
              <button
                onClick={handleRunSimulation}
                className="px-4 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded text-xs font-semibold transition-colors"
              >
                Executar Simulação
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL 3: TRILHA DE AUDITORIA DA TAREFA */}
      {viewingAuditsForTask && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-slate-700 rounded-lg max-w-lg w-full p-5 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <h3 className="text-sm font-semibold text-slate-100">
                Trilha de Auditoria Imutável da Tarefa
              </h3>
              <button
                onClick={() => setViewingAuditsForTask(null)}
                className="text-slate-400 hover:text-slate-200"
              >
                ✕
              </button>
            </div>

            <div className="text-xs text-slate-300 space-y-1">
              <div>Tarefa: <strong className="text-slate-100">{viewingAuditsForTask.title}</strong></div>
              <div>ID: <code className="text-slate-400">{viewingAuditsForTask.id}</code></div>
            </div>

            <div className="space-y-2 max-h-60 overflow-y-auto">
              {(audits[viewingAuditsForTask.id] || []).map((audit) => (
                <div key={audit.id} className="bg-slate-950 p-2.5 rounded border border-slate-800 text-xs space-y-1">
                  <div className="flex items-center justify-between text-[10px] text-slate-400">
                    <span className="font-bold text-indigo-300">{audit.actionType}</span>
                    <span>{new Date(audit.createdAt).toLocaleString('pt-BR')}</span>
                  </div>
                  <div className="text-slate-300">
                    Ator: <strong className="text-slate-200">{audit.actorUserId}</strong>
                    {audit.previousState && <span> | Estado anterior: <code>{audit.previousState}</code></span>}
                    <span> | Novo estado: <code>{audit.newState}</code></span>
                  </div>
                  {audit.notes && <div className="text-slate-400 text-[11px] italic">{audit.notes}</div>}
                </div>
              ))}
            </div>

            <div className="pt-3 border-t border-slate-800 flex justify-end">
              <button
                onClick={() => setViewingAuditsForTask(null)}
                className="px-4 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded text-xs transition-colors"
              >
                Fechar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
