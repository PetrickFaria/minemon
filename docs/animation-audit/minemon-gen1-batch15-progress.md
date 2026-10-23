# Minemon — Lote 15 (#141–#150)

## Estado de retomada — 2026-10-23

- Branch: `feature/minemon-gen1-batch15-cobblemon`.
- Commit-base aprovado: `d0f0cd6ca74bd7dae9139c53cf56b38f25cfbcb4` (checkpoint documental do Lote 14, sobre o commit de código aprovado `76fcb3074cc65c62dea6b94c291de90925494e95`).
- Commit de código em validação: `34b9ea1a739012d2a5375a4c1ef30a80f1bab76a` (`Fix batch 15 Gson test compatibility`), sobre `47dd4781db2e58dd998a9ef14e4b935098539a02` e base aprovada do Lote 14 `d0f0cd6ca74bd7dae9139c53cf56b38f25cfbcb4`.
- Lote 14: GitHub Actions #39885955086 aprovado; artefato #12474937645 publicado e inspecionado.
- GitHub Actions #39900049407 terminou com falha no passo de compilação Java focada do teste novo: Gson não oferece `JsonArray.asList()` na versão do projeto. Os testes não chegaram a executar, portanto nenhuma aprovação Java/GameTest/smoke foi declarada.

## Auditoria da fonte

- Arquivo Cobblemon Fabric 1.8.2: SHA-256 `cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2`.
- #141 Kabutops: clips terrestres de idle/walk e poses nativas; sem ground_run.
- #142 Aerodactyl: clips terrestres e aéreos; mantém ground_idle/walk/run e articulações de asas.
- #143 Snorlax: ground_idle/walk e ações características; sem ground_run.
- #144 Articuno e #145 Zapdos: selecionar air_idle/air_fly, sem inventar locomoção terrestre.
- #146 Moltres: selecionar air_idle/air_fly; elementos de fogo do modelo são animados nos clips nativos.
- #147 Dratini e #148 Dragonair: segmentos corporais e clips terrestres/aquáticos; ambos têm ground_run nativo.
- #149 Dragonite e #150 Mewtwo: rigs com membros e cauda; preservar os clips da fonte e selecionar o modo terrestre disponível.
- Evidência da fonte registrada em `minemon-gen1-batch15-source-evidence.json`.

## Integração atual

- Pokémon importados: Kabutops, Aerodactyl, Snorlax, Articuno, Zapdos, Moltres, Dratini, Dragonair, Dragonite e Mewtwo.
- Importador atualizado: `tools/import_cobblemon_182_batch.py`; adiciona suporte a Lote 15 e aos segmentos 141–145/146–150.
- Perfis de articulação e definições runtime foram atualizados; teste Java direcionado e verificador estrutural do Lote 15 foram adicionados.
- Primeira metade (#141–#145): importada. Segunda metade (#146–#150): importada.
- Corrigidos perfis para conferir apenas canais presentes nos clips nativos selecionados. O walk do Aerodactyl mantém o torso rígido; o run nativo ainda valida o torso. Dratini e Dragonite usam os segmentos terminais reais de seus rigs. Snorlax, Articuno, Zapdos, Moltres e Mewtwo não exigem ossos sem canal nos clips correspondentes.
- Nenhuma implementação posterior à #150 está autorizada nesta etapa.

## Correções e pendências visuais protegidas

- Preservar corrida aprovada de Graveler e Golem, asas corrigidas de Pidgeotto/Pidgeot e braços aprovados de Charizard.
- Não alterar as chamas de Charmander, Charmeleon e Charizard. Permanecem pendentes de aprovação visual manual.
- Avaliação visual manual de #141–#150 ainda pendente; testes automatizados não equivalem à aprovação visual.

## Validações e próximo passo

- Base cumulativa do Lote 14: testes Java, GameTests, smoke de todos os 151 registros, verificadores dos Lotes 8–14 e inspeção do JAR aprovados.
- Verificadores cumulativos locais L8–L15: PASS. `verify_project.py`, `verify_runtime.py`, preflight Molang e `git diff --check`: PASS. O preflight reporta separadamente expressões incompatíveis em clips de montaria retidos do Aerodactyl/Dragonite e espécies antigas; não são clips de locomoção ativos.
- Teste Java direcionado do Lote 15: PASS na execução #39900310676.
- Suíte Java completa e compilação/build: PASS na execução #39900310676.
- GameTests do servidor: PASS (13/13) na execução #39900310676; log no artefato #12479050705 (`minemon-server-tests`), expira em 2026-10-30 04:28:33 UTC. Smoke completo do cliente: em andamento. Inspeção do JAR e publicação ainda não iniciadas; permanecem condicionadas à aprovação do smoke.
- GitHub Actions do Lote 15: #39900310676, commit `34b9ea1a739012d2a5375a4c1ef30a80f1bab76a`, terminou `failure` no smoke completo do cliente; build/Testes Java e GameTests (13/13) passaram. Inspeção e publicação do JAR foram puladas.
- Diagnóstico confirmado: o cliente seleciona `animation.zapdos.air_idle` em repouso e `animation.zapdos.air_fly` em movimento. A fonte não tem `ground_run`; os dois clips aéreos fornecem poses estáticas de repouso/voo, sem ciclos temporais nos ossos testados. A validação de articulação terrestre exigia movimento contínuo desses canais e reprovou com idle/walk/run=0. O erro é a expectativa da validação, não ausência de clips nem rig quebrado.
- Correção em preparação: classificar Zapdos como transição nativa entre poses aéreas estáveis; conferir em execução os canais `head`, `wing_open_left` e `wing_open_right` entre air_idle e air_fly. Manter validação de estabilidade de idle e movimento real do corpo/voo; não inventar ground gait nem retirar Zapdos dos verificadores.
- Diagnóstico registrado em 2026-10-23; commit local de correção ainda não publicado. Após publicar, executar primeiro o teste Java direcionado e os verificadores cumulativos; depois uma única validação completa, sem duplicar workflow.
- Artefato #12479299684 contém apenas diagnósticos (`minemon-test-diagnostics`), expira em 2026-10-30 05:02:32 UTC; não é um JAR aprovado. O artefato de GameTests #12479050705 expirará em 2026-10-30 04:28:33 UTC.
- Próxima ação: concluir verificação direcionada da expectativa aérea e disparar validação completa após publicação da correção. Só declarar o Lote 15 aprovado depois do smoke de cliente passar e inspecionar o JAR então.
- Artefato do JAR do Lote 15: ainda não publicado.
