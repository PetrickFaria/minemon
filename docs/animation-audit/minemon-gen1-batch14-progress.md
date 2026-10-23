# Minemon Gen 1 — Lote 14 (#131–#140)

## Checkpoint 1 — Preparação

- Branch: `feature/minemon-gen1-batch14-cobblemon`.
- Commit-base íntegro: `2b65cc53c61821111e2a2b9a5b9919066a698f74` (documentação posterior à aprovação do Lote 13).
- Base de código aprovada: `c91e12ac3067317f97898a16f5deec3bfa919277`.
- A aprovação do Lote 13 foi confirmada na execução GitHub Actions #39878283990 (success); o artefato JAR #12470668831 aponta para esse commit e expira em 2026-11-06 01:07:44 UTC.
- As branches dos Lotes 14, 15 e 16 não existiam antes da preparação.
- A branch remota do Lote 14 foi criada sobre o commit-base acima.

### Espécies autorizadas

| # | Pokémon | Estado |
|---:|---|---|
| 131 | Lapras | pendente de integração |
| 132 | Ditto | pendente de integração |
| 133 | Eevee | pendente de integração |
| 134 | Vaporeon | pendente de integração |
| 135 | Jolteon | pendente de integração |
| 136 | Flareon | pendente de integração |
| 137 | Porygon | pendente de integração |
| 138 | Omanyte | pendente de integração |
| 139 | Omastar | pendente de integração |
| 140 | Kabuto | pendente de integração |

### Auditoria inicial da fonte

- Fonte local auditada: Cobblemon Fabric 1.8.2, SHA-256 `cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2`.
- Os dez diretórios fonte existem no arquivo. Modelos/rigs e clips nativos foram encontrados para cada espécie.
- Lapras contém clips terrestres, aquáticos e de superfície; não misturar locomoção aquática com corrida terrestre.
- Ditto contém clips nativos de idle/walk e expressões; esta integração não cria mecânica de transformação.
- Eevee e evoluções Vaporeon, Jolteon e Flareon usam rigs próprios; o arquivo oferece `ground_idle` e poses/expressões, mas não traz `ground_walk`/`ground_run` nesses quatro recursos. Não criar clips de locomoção ausentes; preservar idle e documentar fallback estático.
- Porygon, Omanyte, Omastar e Kabuto possuem clips nativos de caminhada; Porygon tem corrida nativa. Omanyte/Omastar incluem locomoção aquática nativa.
- A anatomia observada inclui concha/patas de Lapras, rosto e braços de Ditto, caudas/ornamentos e patas nas quatro formas Eevee, corpo poligonal de Porygon, tentáculos articulados de Omastar e pernas segmentadas de Kabuto.
- Os recursos antigos presentes no projeto não serão usados como substitutos dos novos recursos Cobblemon namespaced.

### Correções visuais protegidas

- Graveler e Golem: corrida aprovada.
- Pidgeotto e Pidgeot: asas extras corrigidas e aprovadas.
- Charizard: braços aprovados.
- Charmander, Charmeleon e Charizard: chamas continuam pendentes de refinamento visual; não alterar neste lote.

### Testes e publicação

- Testes do Lote 14: ainda não executados; recursos ainda pendentes.
- GitHub Actions do Lote 14: nenhum.
- Artefato do Lote 14: nenhum.
- Pokémon concluídos: nenhum.
- Próximo checkpoint: adaptar o importador e integrar #131–#135 com auditoria de articulações e verificações direcionadas.
- Avaliação visual manual: pendente para os dez Pokémon novos.
