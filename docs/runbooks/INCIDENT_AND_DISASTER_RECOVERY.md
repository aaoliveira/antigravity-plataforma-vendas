# Runbook: Resposta a Incidentes, Contenção e Fail-Closed

> **Código:** `RB-OPS-001` &middot; **Versão:** 1.0.0  
> **Finalidade:** Procedimento operacional de emergência, contenção e recuperação de falhas.  

---

## 1. Ativação de Kill Switch por Marketplace

Em caso de comportamento anômalo da API de um parceiro (ex.: HTTP 429 contínuo, alteração não documentada de payload ou risco de restrição de conta):

### 1.1 Procedimento de Desativação Imediata
Altere a propriedade do conector via variável de ambiente ou configuração em tempo real:
```bash
# Exemplo: Desativar conector do Mercado Livre imediatamente
SELLER_CONNECTORS_MERCADO_LIVRE_ENABLED=false
```
Ou no endpoint administrativo autenticado com escopo de superadmin:
```bash
curl -X POST http://localhost:8082/actuator/env \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name": "seller.connectors.mercado-livre.enabled", "value": "false"}'
```
* **Efeito:** O conector rejeita imediatamente novas chamadas externas com a resposta `FEATURE_SUSPENDED_BY_KILL_SWITCH`, evitando qualquer chamada à API externa.

---

## 2. Procedimento de Rotação de Segredos e Tokens

Se houver suspeita de comprometimento da chave de criptografia de tokens (`ENCRYPTION_SECRET_KEY`) ou credenciais OAuth:

1. **Isolar o Acesso:** Revogar a aplicação nas configurações de desenvolvedor do marketplace.
2. **Gerar Nova Chave AES-256:**
   ```bash
   openssl rand -base64 32
   ```
3. **Executar Job de Recriptografia de Tokens:**
   - Iniciar o backend com a chave antiga como secundária e nova chave como primária (`ENCRYPTION_KEY_PRIMARY` e `ENCRYPTION_KEY_SECONDARY`).
   - O job percorre a tabela `marketplace_connections` decifrando com a chave secundária e recifrando com a chave primária.
4. **Auditoria:** Gravar o evento formal em `audit_logs` registrando o motivo, responsável e timestamp.

---

## 3. Contenção Imediata de Incidente Cross-Tenant

Caso qualquer teste ou monitoramento indique anomalia no isolamento de dados:
1. **Ativação do Modo Fail-Closed:** A aplicação bloqueia imediatamente consultas para o recurso afetado.
2. **Verificação de RLS no PostgreSQL:**
   ```sql
   -- Confirmar que o RLS está forçado na tabela
   SELECT relname, relrowsecurity, relforcerowsecurity 
   FROM pg_class 
   WHERE relname = 'pedidos';
   ```
3. **Revisão de Logs Estruturados:** Filtrar pelo `trace_id` da transação para isolar o principal autenticado e o `tenant_id` envolvido.
