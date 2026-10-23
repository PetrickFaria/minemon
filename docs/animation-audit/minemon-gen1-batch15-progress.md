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
- GameTests do servidor: PASS (13/13) na execução #39900310676. A execução #39910815037 falhou antes de build/GameTests/smoke no teste Java novo; essas etapas não foram aprovadas nessa execução. Inspeção do JAR e publicação ainda não iniciadas.
- GitHub Actions do Lote 15: #39900310676, commit `34b9ea1a739012d2a5375a4c1ef30a80f1bab76a`, terminou `failure` no smoke completo do cliente; build/Testes Java e GameTests (13/13) passaram. Inspeção e publicação do JAR foram puladas.
- O smoke #39905354017 percorreu a janela de renderização de 151/151 espécies e falhou na validação final com `Relevant locomotion joints did not animate for articuno idle=0 walk=0 run=0 required=1`. Nenhum JAR foi inspecionado ou publicado.
- Diagnóstico confirmado: Articuno seleciona `air_idle`/`air_fly`, não possui `ground_run`, e os clips de fonte usam poses estáticas. Cabeça e asas têm vetores de rotação diferentes entre as poses; a validação estava pedindo movimento terrestre contínuo. Moltres também usa vetores estáticos nos clips aéreos, com transições reais em cabeça e asas. Zapdos já tem tratamento equivalente.
- Correção publicada no commit `2e90b489e76e6697e13dd344cbe39f8434f1e31e`: classifica Articuno, Zapdos e Moltres como transições entre poses aéreas nativas estáveis e verifica vetores de rotação reais de `head`, `wing_open_left` e `wing_open_right`, sem inventar gait terrestre. A validação dos elementos de fogo de Moltres permanece ativa.
- GitHub Actions #39910815037, commit `2e90b489e76e6697e13dd344cbe39f8434f1e31e`: verificadores L8–L15 e preflight Molang passaram; o teste Java novo falhou com NPE na comparação de canais position/scale opcionais, pois nem todo clip declara os mesmos canais. Build completo, GameTests e smoke não iniciaram. Diagnósticos #12482588332 expiram em 2026-10-30 06:21:01 UTC; log do servidor #12483135744 expira em 2026-10-30 06:20:57 UTC; nenhum artefato é JAR.
- Correção publicada no commit `04843ec48425b0f6645ea2f0cf91a01e33f5da4f`: o teste compara somente o canal de rotação nativo, presente nos três ossos de cada pose; exige diferença real em cada osso perfilado. Isso evita acessar canais opcionais ausentes e mantém a verificação de movimento aéreo real.
- GitHub Actions #39911103192, commit `04843ec48425b0f6645ea2f0cf91a01e33f5da4f`: verificadores, preflight Molang, teste Java focado, suíte Java/build e GameTests do servidor passaram. O smoke do cliente percorreu a validação cumulativa até Mewtwo (#150) e falhou com `Relevant locomotion joints did not animate for mewtwo idle=0 walk=0 run=0 required=1`; inspeção/publicação do JAR foram puladas.
- Diagnóstico confirmado na fonte: Mewtwo tem `canFly=true`, não declara caminhada, e o poser seleciona `air_idle` ao pairar e `air_fly` ao voar, com `q.pitch_tilt('body')` e `q.look('head', ...)`. Os clips aéreos são poses nativas estáveis diferentes; o único canal com expressão de tempo no `air_idle` é o locator de item, sem movimento visível. A integração atual forçava modo terrestre e o smoke exigia ciclo contínuo; o comportamento apropriado é modo flying e validar uma transição estável nas articulações reais.
- Correção local preparada: Mewtwo seleciona `air_idle`/`air_fly`, com perfil de transição em `arm_left`, `arm_right`, `tail` e `tail9`, idle visível estável, e adaptações de poser `q.look` e `q.pitch_tilt` para `head` e `body`. Não se inventa caminhada/corrida terrestre. `verify_batch15.py`, verificadores L12–L14, `verify_project.py`, `verify_runtime.py` e `git diff --check` passaram localmente. A execução local do teste Gradle direcionado não iniciou: o wrapper tentou baixar Gradle 7.5.1 e a rede deste ambiente bloqueou a conexão; os testes Java/build/GameTests/smoke desta correção dependem do próximo GitHub Actions.
- Diagnósticos #12485476502 expiram em 2026-10-30 07:19:18 UTC e registram o erro de Mewtwo; logs do servidor #12483450585 expiram em 2026-10-30 06:25:40 UTC. Nenhum contém JAR. A tentativa #39910815037 falhou no teste Java antes de iniciar cliente.
- Correção publicada no commit `c8e45c5d83b4c99e14a249c7091b1616c6e2cb10`: `air_idle`/`air_fly` nativos, transição estável e adaptações de `q.look`/`q.pitch_tilt` do Mewtwo. Verificadores L8–L15 e preflight Molang passaram no Actions #39917910668. O teste Java focado e a suíte Java/build passaram. GameTests do servidor passaram nesta execução; o smoke completo do cliente está em andamento. Inspeção e publicação do JAR ainda pendentes. O run anterior #39911103192 terminou com falha em Mewtwo; não foi aprovado nem publicou JAR.
- GitHub Actions ativo: [#39917910668](https://github.com/PetrickFaria/minemon/actions/runs/39917910668), commit testado `c8e45c5d83b4c99e14a249c7091b1616c6e2cb10`; etapa atual: smoke completo do cliente após aprovação de Java/build e GameTests. Não iniciar outro workflow enquanto este estiver ativo. Próxima ação: acompanhar este mesmo run até smoke, inspeção e eventual publicação do JAR. O Lote 16 só começa após Lote 15 aprovado.
