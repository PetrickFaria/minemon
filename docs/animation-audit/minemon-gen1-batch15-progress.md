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


## Retomada e diagnóstico da falha mais recente — 2026-10-23

- Branch remota mais recente: `feature/minemon-gen1-batch15-cobblemon`, cabeça `51d634a52648cd2fa0c70a3e6a199f32b66849b3` (checkpoint documental após o commit de código `c8e45c5d83b4c99e14a249c7091b1616c6e2cb10`). Não havia commit remoto posterior nem execução posterior no momento da consulta.
- Actions #39917910668 terminou `failure` no passo 15, `Launch Minecraft client and validate GeckoLib cache`. Os passos de verificadores cumulativos, preflight Molang, testes Java direcionados, suíte Java/build e GameTests (13/13) passaram. Inspeção e upload do JAR foram pulados; o artefato #12489738179 contém somente diagnósticos, expira em 2026-10-30 08:48:51 UTC.
- Falha exata do cliente: `Missing settled source pose moltres:head` ao comparar `IDLE_FINAL` com `WALK_STEADY`. A fonte nativa contém canais de rotação `head` tanto em `animation.moltres.air_idle` quanto em `animation.moltres.air_fly`; o perfil `pose_transition` exigia `head`, mas a lista `walk` usada para coletar articulações não continha essa junta. Portanto, a causa confirmada é uma omissão no perfil de amostragem, não incompatibilidade do clip nem ausência de movimento nativo.
- Correção desta retomada: incluir `head` no perfil de movimento do Moltres; reforçar o teste Java e `verify_batch15.py` para exigir que todas as juntas de transição aérea também sejam coletadas no perfil de movimento. O teste continua validando as três juntas nativas (`head`, `wing_open_left`, `wing_open_right`) e não adiciona gait terrestre.
- Após a correção, `verify_batch8.py` a `verify_batch15.py`, `verify_project.py`, `verify_runtime.py`, validação JSON e `git diff --check` passaram. O teste Gradle focado não pôde iniciar neste ambiente porque o wrapper precisaria baixar Gradle 7.5.1 e a rede está indisponível; a validação Java correspondente será feita no Actions do commit corretivo. Nenhum smoke novo foi iniciado.
- A solicitação visual de Charmander/Charmeleon/Charizard, Geodude/Graveler/Golem e Ponyta/Rapidash não foi iniciada nesta recuperação; fica registrada como próxima tarefa visual depois da base técnica do Lote 15. As correções aprovadas de corrida de Graveler/Golem, asas de Pidgeotto/Pidgeot e braços de Charizard permanecem protegidas. As três chamas continuam sem aprovação visual e não foram alteradas.
- Checkpoint atual: correção publicada no commit `5c5e02bf85a29584c50989c378139d365474dd51`. GitHub Actions [#39956230056](https://github.com/PetrickFaria/minemon/actions/runs/39956230056) está `in_progress`, testando exatamente esse commit. Testes Java focados, suíte/build e GameTests passaram; etapa 15 (`Launch Minecraft client and validate GeckoLib cache`) está em execução. Não iniciar outro workflow. Inspeção do JAR e upload ainda estão pendentes. O smoke anterior #39917910668 falhou em `moltres:head` por perfil incompleto, agora corrigido. Próxima ação: acompanhar #39956230056 até cliente, inspeção real do JAR e publicação. Só declarar Lote 15 aprovado se toda a execução passar e o artefato corresponder ao SHA testado.

- Checkpoint de acompanhamento — 2026-10-23 13:18 UTC: a execução [#39956230056](https://github.com/PetrickFaria/minemon/actions/runs/39956230056) continua `in_progress` no passo 15 (smoke completo do cliente), testando o commit de código `5c5e02bf85a29584c50989c378139d365474dd51`. A consulta mais recente não forneceu logs parciais nem novo estado do job. Os passos 6–14 seguem concluídos com sucesso conforme o checkpoint anterior; os passos 16–17 (inspeção e upload do JAR) estão pendentes. O único artefato disponível é `minemon-server-tests` (#12502911002), com expiração em 2026-10-30 12:51:36 UTC; ele não contém o JAR. Próxima ação: consultar novamente o mesmo run e só então verificar/publicar o JAR se o smoke passar. Nenhum novo smoke foi iniciado.

## Resultado da validação #39956230056 — 2026-10-23

- Execução terminou `failure` no passo 16, `Verify executable mod JAR contents`, testando o commit de código `5c5e02bf85a29584c50989c378139d365474dd51`.
- Smoke completo do cliente aprovado: `PASS MENU`, `PASS CREATIVE ORDER` (151 ovos), `PASS WORLD`, `PASS COMBAT`, `PASS CHARMANDER TAIL FLAME`, `PASS LOCOMOTION`, `PASS PIDGE WINGS`, `PASS SOURCE LOOK`, `PASS SOURCE PITCH`, `PASS FIRE-STARTER ARMS` e `PASS ROSTER`. A validação cumulativa renderizou os 151 registros; perfis detalhados 131 e 19 gaps de fallback declarados.
- Build/Testes Java e GameTests do servidor (13/13) passaram antes do cliente.
- Erro exclusivo da inspeção: `ValueError: Missing prepared sit animation kabutops`, em `tools/verify_runtime.py`. A fonte nativa Cobblemon 1.8.2 de Kabutops declara `ground_idle`, `ground_walk`, `battle_idle`, `sleep`, `blink` e `cry`; não declara `sit`. A inspeção verificava `sit` como obrigatório por intervalo de número. O mesmo inventário foi auditado em #141–#150: nenhum deles declara `sit`; os estados `sleep` e ataque variam conforme os clipes nativos. Não há recurso `sit` ausente do JAR; ele não existe na fonte e não está declarado no manifesto.
- Correção local: a inspeção agora deriva os estados obrigatórios dos clipes declarados por espécie. Estados sem referência no manifesto são opcionais; referências declaradas continuam obrigatórias e o verificador ainda falha se o clipe estiver ausente do JAR. Nenhum clipe ou animação foi criado.
- Validações locais pós-correção: `verify_runtime.py`, `verify_batch15.py`, `verify_project.py`, `git diff --check` e simulação de preparação/validação dos recursos preparados para as 151 espécies passaram. Um caso de regressão confirmou que declarar um `sit` inexistente para Kabutops continua falhando.
- Artefato #12505361258 (`minemon-test-diagnostics`) expirará em 2026-10-30 13:30:44 UTC; contém logs e evidências do smoke, não o JAR. Nenhum artefato JAR foi publicado porque a inspeção falhou.
- Próximo passo: publicar a correção de `verify_runtime.py` e este checkpoint; iniciar um único Actions completo no novo commit. O smoke completo deve ser confirmado novamente, seguido da inspeção empacotada e publicação do JAR. Lote 15 permanece não aprovado até essas etapas concluírem com sucesso.

- Correção e checkpoint publicados em `276abc1428c8882a837e770a356f48a8d122bbab` (2026-10-23 13:45 UTC): inspeção de estados preparada baseada nas animações realmente declaradas no manifesto. Verificação local simulou a preparação runtime para 151 espécies; os dez do Lote 15 passaram sem clipe `sit`; um teste negativo confirmou que um `sit` explicitamente declarado mas ausente continua reprovado. Verificadores do runtime, do Lote 15 e do projeto passaram; `git diff --check` passou.
- GitHub Actions [#39963816100](https://github.com/PetrickFaria/minemon/actions/runs/39963816100) foi criado para o commit `276abc1428c8882a837e770a356f48a8d122bbab`; status ao registrar: `queued`. Não iniciar outro workflow. A execução anterior #39956230056 concluiu com falha somente na inspeção do JAR; o smoke cliente passou integralmente. Próxima ação: acompanhar #39963816100, confirmar todos os passos, inspeção empacotada e artefato JAR.
