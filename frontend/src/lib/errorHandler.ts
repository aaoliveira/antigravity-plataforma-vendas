/**
 * Utilitário para tratamento padronizado e seguro de erros na interface do usuário (UI).
 * Aplica diretrizes OWASP ASVS para prevenção de enumeração cross-tenant e ocultação
 * mandatória de UUIDs técnicos e metadados internos.
 */

// Regex para identificação e mascaramento de UUIDs v4 técnicos
const UUID_REGEX = /[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/g;

/**
 * Mensagem uniforme para 403 e 404: impede categoricamente que invasores descubram
 * se um recurso existe em outro tenant (Prevenção contra Resource Enumeration & Timing Attacks).
 */
export const UNIFORM_ACCESS_OR_NOT_FOUND_MESSAGE =
  'Recurso não encontrado ou acesso restrito.';

export const UNIFORM_SESSION_EXPIRED_MESSAGE =
  'Sua sessão expirou ou não possui autorização válida. Por favor, autentique-se novamente.';

export const UNIFORM_SERVER_ERROR_MESSAGE =
  'Ocorreu uma instabilidade momentânea no servidor. Tente novamente em alguns instantes.';

/**
 * Sanitiza qualquer texto de erro, removendo UUIDs internos, caminhos de arquivo ou segredos.
 *
 * @param message texto bruto do erro
 * @returns texto sanitizado seguro para exibição
 */
export function sanitizeErrorMessage(message?: string | null): string {
  if (!message || typeof message !== 'string') {
    return UNIFORM_ACCESS_OR_NOT_FOUND_MESSAGE;
  }

  // Remove qualquer UUID exposto no texto
  let sanitized = message.replace(UUID_REGEX, '[recurso]');

  // Remove eventuais referências técnicas internas
  sanitized = sanitized
    .replace(/tenant_id/gi, 'organização')
    .replace(/organization_id/gi, 'organização')
    .replace(/java\.[a-zA-Z0-9_.]+/g, '')
    .replace(/Exception:/g, '')
    .trim();

  return sanitized || UNIFORM_ACCESS_OR_NOT_FOUND_MESSAGE;
}

/**
 * Retorna uma mensagem amigável e segura com base no status HTTP recebido.
 * Trata 403 e 404 com a exata mesma resposta para impedir enumeração de dados cross-tenant.
 */
export function getUniformFeedbackForHttpStatus(status: number): {
  title: string;
  description: string;
  actionLabel: string;
} {
  switch (status) {
    case 401:
      return {
        title: 'Sessão Expirada',
        description: UNIFORM_SESSION_EXPIRED_MESSAGE,
        actionLabel: 'Fazer Login',
      };
    case 403:
    case 404:
      return {
        title: 'Recurso Não Encontrado ou Restrito',
        description: UNIFORM_ACCESS_OR_NOT_FOUND_MESSAGE,
        actionLabel: 'Voltar ao Início',
      };
    case 500:
    case 502:
    case 503:
      return {
        title: 'Instabilidade no Servidor',
        description: UNIFORM_SERVER_ERROR_MESSAGE,
        actionLabel: 'Tentar Novamente',
      };
    default:
      return {
        title: 'Aviso do Sistema',
        description: UNIFORM_ACCESS_OR_NOT_FOUND_MESSAGE,
        actionLabel: 'Voltar ao Início',
      };
  }
}
