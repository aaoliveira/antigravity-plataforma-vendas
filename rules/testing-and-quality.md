# Testes e qualidade

- Nenhuma funcionalidade é concluída sem critérios de aceite e testes executados.
- Backend: JUnit 5, AssertJ, Mockito, Testcontainers e ArchUnit. Frontend: Vitest/Testing Library. E2E: Playwright.
- Rodar migrações em banco limpo; validar rollback quando aplicável.
- Testar autorização por tenant/empresa/loja em APIs, busca, relatórios e exportações.
- Testar idempotência, retry, timeout, dead-letter e eventos duplicados para jobs.
- Não declarar teste executado sem saída/comando registrado.
- Registrar resultado, ambiente, commit/hash e limitações em docs/delivery.
- DoD: critérios atendidos, testes verdes, documentação atualizada, telemetria sem segredos e revisão de segurança.