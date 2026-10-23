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
| 131 | Lapras | integrado; testes finais pendentes |
| 132 | Ditto | integrado; testes finais pendentes |
| 133 | Eevee | integrado; testes finais pendentes |
| 134 | Vaporeon | integrado; testes finais pendentes |
| 135 | Jolteon | integrado; testes finais pendentes |
| 136 | Flareon | integrado; testes finais pendentes |
| 137 | Porygon | integrado; testes finais pendentes |
| 138 | Omanyte | integrado; testes finais pendentes |
| 139 | Omastar | integrado; testes finais pendentes |
| 140 | Kabuto | integrado; testes finais pendentes |

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

- Verificadores locais cumulativos Lotes 8–14: PASS.
- Teste direcionado Java: bloqueado antes da execução porque Gradle Wrapper 7.5.1 não pôde ser baixado por restrição de rede local.
- GitHub Actions do Lote 14: pendente de publicação do commit de código.
- Artefato do Lote 14: ainda não existe.
- Pokémon concluídos: 10/10 recursos integrados; aprovação automatizada final pendente.
- Próximo checkpoint: executar testes Java, GameTests, smoke cliente e inspeção do JAR em uma única execução GitHub Actions após o commit.
- Avaliação visual manual: pendente para os dez Pokémon novos.

## Checkpoint 2 — Integração #131–#135

- Lapras, Ditto, Eevee, Vaporeon e Jolteon integrados do arquivo Cobblemon auditado.
- Verificações estáticas dos JSONs, dimensões PNG, hierarquia dos ossos, pivôs e referência de clips passaram.
- Lapras manteve os clips terrestres, aquáticos e de superfície; Ditto manteve os clips de expressão/locomoção e nenhuma mecânica de transformação foi adicionada.
- Os três Eeveelutions mantêm os rigs Kanto e o idle nativo. A fonte não fornece walk/run; foram preservados como indisponíveis, com fallback estático documentado.

## Checkpoint 3 — Integração #136–#140

- Flareon, Porygon, Omanyte, Omastar e Kabuto integrados.
- Porygon preserva seu ground_run nativo; seu modo permanece ground para selecionar os clips terrestres da fonte.
- Omanyte e Omastar mantêm os clips aquáticos nativos, incluindo idle/swim. Omastar conserva os tentáculos articulados; Kabuto conserva as quatro pernas e os segmentos existentes no rig.
- O clip de Kabuto referencia body2, ausente no rig; essa referência foi registrada em stripped_bones e o perfil só verifica ossos reais. Nenhum osso ou movimento foi inventado.
- Verificadores cumulativos dos Lotes 8–14 passaram localmente. verify_project.py, verify_runtime.py, preflight Molang, compilação sintática dos scripts e git diff --check passaram.

## Estado da validação Java

- Java local: OpenJDK 17.0.20.
- A tentativa direcionada do Gradle não começou: o wrapper tentou baixar Gradle 7.5.1 e o ambiente respondeu Network is unreachable. Isso não é uma falha de teste Java nem aprovação.
- A suíte direcionada e completa será executada no GitHub Actions depois da publicação do commit. GameTests e cliente permanecem pendentes.
- Pokémon concluídos: 10/10 recursos integrados; aprovação técnica completa ainda pendente.

## Próximo passo

Publicar o commit de código e executar um único workflow completo do GitHub Actions. Se um gate falhar, investigar o relatório correspondente antes de novo smoke completo. Ainda não iniciar o Lote 15.
