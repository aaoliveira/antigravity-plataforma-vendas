# Protocolo de Gating Sequencial de Fases e Prompts

## 1. Princípio do Bloqueio Sequencial Rígido
O ciclo de desenvolvimento da Seller Intelligence Platform é governado por **Phase Gates sequenciais**. Cada prompt numerado (`Prompt 00`, `Prompt 01`, ..., `Prompt 19`) representa uma etapa delimitada e auto-contida.

**Regra de Ouro:**
O agente é expressamente proibido de iniciar ou executar o `Prompt N+1` enquanto o `Prompt N` não estiver formalmente classificado como `APPROVED` em [docs/delivery/PHASE_STATUS.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/delivery/PHASE_STATUS.md) e [docs/delivery/phase-gate.json](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/delivery/phase-gate.json), com todas as evidências comprovadas.

---

## 2. Estados Possíveis de um Gate
- **`NOT_STARTED`:** O prompt ainda não foi iniciado; pré-requisitos precisam ser verificados.
- **`IN_PROGRESS`:** O escopo do prompt está sendo planejado, desenvolvido ou testado ativamente.
- **`APPROVED`:** Todos os critérios de aceite do prompt foram integralmente atendidos, verificados por testes automatizados reais com comandos e logs registrados, documentação atualizada e ausência de bloqueios.
- **`BLOCKED`:** O prompt encontrou uma ambiguidade crítica de negócio/fiscal, falta de endpoint documentado, ausência de credencial, quebra de testes ou impossibilidade técnica de validação.

---

## 3. Protocolo de Transição e Validação

```
       ┌────────────────────────┐
       │      NOT_STARTED       │
       └───────────┬────────────┘
                   │ Preflight OK
                   ▼
       ┌────────────────────────┐
       │      IN_PROGRESS       │◄────────┐
       └─────┬────────────┬─────┘         │
             │            │               │ Correção
  Critérios  │            │ Ambiguidade/  │ de Falhas
  Aprovados  │            │ Falha Crítica │
             ▼            ▼               │
       ┌───────────┐  ┌───────────┐       │
       │ APPROVED  │  │  BLOCKED  ├───────┘
       └─────┬─────┘  └───────────┘
             │
             ▼
    Libera Prompt N+1
```

### 3.1 Condições Obrigatórias para Marcar `APPROVED`
1. Todos os critérios de aceite listados no prompt foram testados e aprovados.
2. Comandos de teste e build foram executados no ambiente e seus outputs registrados.
3. Não há pendências críticas abertas que afetem a integridade do escopo entregue.
4. O arquivo [docs/delivery/PHASE_STATUS.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/delivery/PHASE_STATUS.md) foi atualizado com a tabela de evidências e carimbo de data/hora.
5. O arquivo de máquina [docs/delivery/phase-gate.json](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/delivery/phase-gate.json) foi sincronizado refletindo o estado `APPROVED`.

### 3.2 Condições que Exigem Marcar `BLOCKED`
O agente DEVE marcar imediatamente `BLOCKED` e parar a execução se:
- Faltar documentação ou fonte oficial para uma integração crítica de marketplace.
- O PRD ou requisito for contraditório ou omisso em relação a regras fiscais, cálculo monetário ou isolamento de dados.
- O teste automatizado falhar e a correção depender de decisões de arquitetura ou produto não aprovadas.
- O usuário tentar avançar para um prompt posterior com o gate anterior não aprovado.

---

## 4. Auditoria e Rastreabilidade do Gate
Cada transição de estado deve ser registrada no histórico do `PHASE_STATUS.md`, informando:
- Identificador do prompt.
- Status atribuído.
- Timestamp de verificação.
- Lista de critérios validados com links para testes ou arquivos de evidência.
- Assinatura do agente/revisor.
