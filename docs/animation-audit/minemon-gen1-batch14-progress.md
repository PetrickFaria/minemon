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
- GitHub Actions #39885955086 está em andamento no commit testado `76fcb3074cc65c62dea6b94c291de90925494e95` (branch `feature/minemon-gen1-batch14-cobblemon`).
- O job concluiu com sucesso os verificadores dos Lotes 8–14, preflight Molang, testes Java focados e completos, compilação e GameTests do servidor (13 testes obrigatórios). O smoke do cliente continua em execução; inspeção/upload do JAR ainda pendentes.
- Artefato publicado até agora: `minemon-server-tests`, ID `12473175412`, expira em 2026-10-30 01:50:10 UTC. Ainda não há artefato de JAR.
- Pokémon #131–#140: dez recursos integrados; aprovação automatizada final ainda pendente.
- Próximo passo: consultar a mesma execução #39885955086 até conclusão. Não iniciar Lote 15 antes da aprovação e inspeção do JAR.
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

## Estado verificado mais recente — 2026-10-23 02:26 UTC

- Branch atual: `feature/minemon-gen1-batch14-cobblemon`.
- Commit de código em validação: `76fcb3074cc65c62dea6b94c291de90925494e95` (`Implement Gen 1 batch 14 species 131-140`), árvore `cbc1a1770e1d18abdd359a2acf134186c620d639`.
- A compilação, a suíte Java (incluindo testes específicos do Lote 14), verificadores estruturais/Molang e GameTests do servidor passaram na execução #39885955086. O log confirma `All 13 required tests passed`.
- O smoke do cliente iniciou às 01:50:11 UTC e continua marcado `in_progress` no passo `Launch Minecraft client and validate GeckoLib cache`. O log remoto disponível ainda termina em 01:50:40 UTC; isso não prova progresso nem falha, então o status permanece pendente.
- Inspeção do JAR e upload do JAR não foram iniciados. O único artefato listado é o relatório `minemon-server-tests` ID `12473175412`, expiração `2026-10-30T01:50:10Z`.
- Execução em andamento: https://github.com/PetrickFaria/minemon/actions/runs/39885955086
- O smoke completo aprovado anterior (#39878283990) levou aproximadamente 36 minutos desde o início do cliente; a execução atual ainda deve ser consultada até resultado final, sem workflow duplicado.
- Próximo passo: retomar a mesma execução; depois de `success`, conferir as etapas de inspeção e upload, baixar/inspecionar o artefato real e registrar SHA/expiração. Somente então iniciar o Lote 15.

Avaliação visual manual dos novos Pokémon permanece pendente. As correções visuais aprovadas permanecem protegidas. As chamas de Charmander, Charmeleon e Charizard continuam pendentes de refinamento e não foram alteradas.
