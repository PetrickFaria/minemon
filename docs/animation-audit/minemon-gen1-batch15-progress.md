# Minemon — Lote 15 (#141–#150)

## Estado de retomada — 2026-10-23

- Branch: `feature/minemon-gen1-batch15-cobblemon`.
- Commit-base aprovado: `d0f0cd6ca74bd7dae9139c53cf56b38f25cfbcb4` (checkpoint documental do Lote 14, sobre o commit de código aprovado `76fcb3074cc65c62dea6b94c291de90925494e95`).
- Último commit publicado nesta branch: `d0f0cd6ca74bd7dae9139c53cf56b38f25cfbcb4` (checkpoint de implementação ainda não publicado).
- Lote 14: GitHub Actions #39885955086 aprovado; artefato #12474937645 publicado e inspecionado.
- Lote 15: os dez modelos, texturas e animações de Cobblemon 1.8.2 foram importados localmente. Ainda não há commit de implementação nem execução do workflow.

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
- Teste Java direcionado do Lote 15: tentativa local bloqueada antes da execução porque o wrapper não conseguiu baixar Gradle 7.5.1 (`Network is unreachable`). Isso não é aprovação nem falha do teste. GameTests e smoke do cliente ainda não iniciados.
- Próxima ação: publicar o checkpoint de código e usar o GitHub Actions para executar testes direcionados, suíte Java, GameTests e smoke em ordem; só avançar às fases posteriores quando cada fase anterior passar.
- GitHub Actions do Lote 15: ainda não iniciado.
- Artefato do Lote 15: ainda não publicado.
