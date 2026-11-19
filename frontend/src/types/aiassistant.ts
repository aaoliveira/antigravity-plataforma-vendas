export type AiProvider = 'GEMINI' | 'OPENAI' | 'CLAUDE' | 'MOCK_PROVIDER';

export interface PromptTemplateDto {
  key: string;
  version: string;
  systemInstructions: string;
  allowedTools: string[];
  description: string;
  registeredAt: string;
}

export interface AiToolDefinitionDto {
  name: string;
  description: string;
  parametersSchema: Record<string, unknown>;
}

export interface AiPromptRequestDto {
  promptKey: string;
  promptVersion?: string;
  provider?: AiProvider;
  model?: string;
  userInput: string;
  entityContext?: Record<string, unknown>;
  temperature?: number;
}

export interface AiUsageMetricsDto {
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  latencyMs: number;
  estimatedCostBrl: number;
}

export interface StructuredAiResponseDto {
  executionId: string;
  tenantId: string;
  provider: AiProvider;
  model: string;
  promptKey: string;
  promptVersion: string;
  analysisText: string;
  structuredData?: Record<string, unknown>;
  sources: string[];
  timestamps: Record<string, string>;
  toolsExecuted: string[];
  usageMetrics: AiUsageMetricsDto;
  unavailabilityNotes: string[];
  antiHallucinationDisclaimer: string;
}

export interface AiUsageAuditRecord {
  id: string;
  tenantId: string;
  userId?: string;
  provider: string;
  modelName: string;
  promptKey: string;
  promptVersion: string;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  latencyMs: number;
  estimatedCostBrl: number;
  toolsInvoked?: string;
  injectionDetected: boolean;
  injectionThreatType?: string;
  status: string;
  errorCode?: string;
  metadataJson?: string;
  createdAt: string;
}

export interface ChatMessage {
  id: string;
  sender: 'user' | 'assistant';
  content: string;
  timestamp: string;
  responseMeta?: StructuredAiResponseDto;
  isError?: boolean;
  threatType?: string;
  feedback?: {
    type: 'helpful' | 'not_helpful';
    comment?: string;
  };
}
