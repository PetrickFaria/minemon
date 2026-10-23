# Minemon Gen 1 — Lote 16 (#151 Mew)

## Retomada e estado remoto — 2026-10-23

- Branch: `feature/minemon-gen1-visual-fixes-batch16`.
- Base aprovada: Lote 15, código `276abc1428c8882a837e770a356f48a8d122bbab`, checkpoint `731dd5aefde687ca92aa6f5a80678f1417179f0d`.
- Checkpoints remotos de código/documentação: `b7a2d697711009a1ace42f48543c9c74ade33981`, `745ca1ccc03a2e31d59e87dcbed671220f42f9eb`, `e7014710206fed0b712a86a85c87be89154a664a` e `3bd1295e465a525f21759db44390028dda94dfa9`. O último ajusta a validação do overlay emissivo. Commit de auditoria isolada: `60cb2a9de7aca6216af67fa142bdaa30b1acbbd0`.
- Mew (#151) usa geometria, textura e animações exatas do Cobblemon Fabric 1.8.2; `verify_batch16.py` verifica hashes, ossos, clips e Pokédex com 151 registros. O poser nativo mapeia hover a `ground_idle` e fly a `ground_walk`; não existem clips nativos `air_idle`, `air_fly` ou `ground_run` no recurso de Mew. Não foram inventados.
- Adaptados `q.look(head, 1, 1, 20, -90, 60, -37)` e `q.pitch_tilt(body)`. O smoke cumulativo mede movimento de cabeça/corpo.

## Ponyta/Rapidash e locomoção

- Actions de auditoria #39984162304 passou e confirmou os resolvers `0_ponyta_base.json` e `0_rapidash_base.json`: camada `emissive`, quatro quadros, `fps: 10`, `loop: true`. A evidência completa está em `minemon-gen1-batch16-visual-source-evidence.json`.
- O código local usa exatamente os quatro quadros normais da fonte por espécie, seleciona quadro a 10 fps e aplica uma segunda passagem translúcida/emissiva do modelo inteiro. Pixels transparentes preservam a textura base; UVs e transforms nativos mantêm o fogo preso aos ossos originais. A seleção shiny não existe atualmente no Minemon; os quadros shiny são documentados, mas não usados.
- Auditoria confirmou Geodude com `ground_walk` e `ground_run` distintos; Graveler e Golem usam corrida nativa rolando com `body_roll`. Os perfis anteriores foram preservados, sem novas animações.
- Charmander, Charmeleon e Charizard têm clips independentes de movimento de chama declarados. Nenhuma edição nos canais dos braços do Charizard.

## Validação e execução atual

- Local: `verify_runtime.py`, `verify_project.py`, verificadores de Lotes 8–16, `verify_pidge_wing_visibility.py` e `git diff --check` passaram antes das últimas atualizações documentais/emissivas. Java local é 17; Gradle não iniciou porque o wrapper precisa baixar Gradle 7.5.1 e a rede local bloqueia o download.
- Actions #39980988503 testa somente o commit anterior `a94d84f4466d0cb0652adc6add8b386d7de18ca4`. Java, build e GameTests passaram; o smoke completo do cliente continua em execução/sem conclusão confirmada. Não é a validação do commit de código b7a2.
- Source audit #39980988543 passou para os recursos Mew/fire; audit #39984162304 passou e confirmou os resolvers.
- O código de Mew e overlays ainda não tem build final, smoke final, inspeção de JAR ou artefato aprovado. Os checkpoints novos foram publicados com `[skip ci]` porque #39980988503 ainda estava executando o smoke do commit anterior. Não declarar Lote 16 concluído.

## Proteções

Preservar os braços do Charizard, asas corrigidas de Pidgeotto/Pidgeot, corrida aprovada de Graveler/Golem, voo do Moltres, Mewtwo, validação opcional de Kabutops e todos os lotes 1–15. Avaliação visual manual de Mew e overlays permanece pendente.

## Próxima ação

Consultar novamente #39980988503 sem lançar retry. Quando concluir, executar uma única validação completa do branch atualizado com Mew e overlays; corrigir falhas reais, inspecionar o JAR e registrar Action/artefato/expiração. Até lá, manter a ressalva de que não há aprovação final nem novo JAR.
