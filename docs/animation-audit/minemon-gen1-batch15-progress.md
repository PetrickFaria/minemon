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
- GameTests do servidor: PASS (13/13) na execução #39900310676; o run atual também passou seus 13 GameTests. Smoke completo do cliente: falhou ao concluir a validação de articulação. Inspeção do JAR e publicação ainda não iniciadas; permanecem condicionadas à aprovação do smoke.
- GitHub Actions do Lote 15: #39900310676, commit `34b9ea1a739012d2a5375a4c1ef30a80f1bab76a`, terminou `failure` no smoke completo do cliente; build/Testes Java e GameTests (13/13) passaram. Inspeção e publicação do JAR foram puladas.
- O smoke #39905354017 percorreu a janela de renderização de 151/151 espécies e falhou na validação final com `Relevant locomotion joints did not animate for articuno idle=0 walk=0 run=0 required=1`. Nenhum JAR foi inspecionado ou publicado.
- Diagnóstico confirmado: Articuno seleciona `air_idle`/`air_fly`, não possui `ground_run`, e os clips de fonte usam poses estáticas. Cabeça e asas têm vetores de rotação diferentes entre as poses; a validação estava pedindo movimento terrestre contínuo. Moltres também usa vetores estáticos nos clips aéreos, com transições reais em cabeça e asas. Zapdos já tem tratamento equivalente.
- Correção local em preparação: classificar Articuno, Zapdos e Moltres como transições entre poses aéreas nativas estáveis; verificar individualmente vetores reais de `head`, `wing_open_left` e `wing_open_right`, sem inventar gait terrestre. Preservar a checagem dos elementos de fogo de Moltres e manter todas as espécies no smoke cumulativo.
- Diagnósticos do run: artefato #12482143855 (`minemon-test-diagnostics`), expira em 2026-10-30 06:12:44 UTC. Ele contém logs do cliente, sem JAR. Artefato #12480507971 (`minemon-server-tests`) expira em 2026-10-30 05:25:07 UTC.
- Próxima ação: executar verificadores locais e teste Java focado para as três aves; publicar a correção e executar uma validação completa. Não iniciar o Lote 16 até o smoke do cliente e a inspeção/publicação do JAR do Lote 15 serem aprovados.
- Artefato do JAR do Lote 15: ainda não publicado.
