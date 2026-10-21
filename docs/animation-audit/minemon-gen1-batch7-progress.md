# Minemon Gen 1 — Lote 7 e correção das asas de Pidgeotto/Pidgeot

## Base verificada

- Branch: `feature/minemon-gen1-batch7-cobblemon`
- Commit-base: `9153f58d9e6e4b279f6ca11249ca6f4a12016dfb`
- Actions do Lote 6: execução `39524383095`, concluída com sucesso; build/testes Java, GameTests do servidor, smoke do cliente, inspeção do JAR e upload passaram.
- JAR do Lote 6: `Minemon-1.19.2-2.0.0.jar`, dentro do artefato `Minemon-1.19.2-2.0.0-gen1`.
- Plano: Lote 7 é #061–#070: Poliwhirl, Poliwrath, Abra, Kadabra, Alakazam, Machop, Machoke, Machamp, Bellsprout e Weepinbell.

## Etapa A — asas

**Investigação concluída.** O modelo Cobblemon contém duas alternativas de rig: `wings_open` e `wings_closed`, cada uma com asas esquerda/direita e geometria própria. Os ossos não são cubos duplicados acidentais. O poser Cobblemon seleciona visibilidade por estado via `transformedParts[].isVisible`; essa visibilidade não é aplicada automaticamente pelo adaptador GeckoLib usado pelo Minemon, deixando ambos os rigs visíveis.

Referência inspecionada: Cobblemon Fabric 1.8.2, arquivo `Cobblemon-fabric-1.8.2+1.21.1.jar`, SHA-256 `cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2`. Posers originais em `assets/cobblemon/bedrock/pokemon/posers/0017_pidgeotto/pidgeotto.json` e `.../0018_pidgeot/pidgeot.json`.

**Correção aplicada:** clipes de voo/hover e solo/repouso mapeados pelo poser definem escala nos grupos pai. O smoke do cliente revelou que GeckoLib 3.1.40 eleva escala zero a `0,01`, ainda renderizando o rig alternativo. `MinemonModel` seleciona o grupo com a escala maior e marca o outro `GeoBone` e seus descendentes como ocultos, mantendo as duas alternativas, seus cubos e as animações nativas intactas. A transição nativa de `ground_quirk` do Pidgeotto e a troca dinâmica de asas do `ride_dive` do Pidgeot foram preservadas. O verificador permite apenas mudanças nessas trilhas de escala nas animações anteriores; os demais recursos dos Lotes 1–6 são comparados byte a byte.

Verificação automatizada incluída:

- `tools/verify_pidge_wing_visibility.py`: integridade da hierarquia e cubos das duas alternativas, escala em clipes selecionados, transições dinâmicas, uso dos clipes ativos e inspeção opcional do JAR.
- Smoke de cliente exige que, em amostras estabilizadas de Pidgeotto e Pidgeot, o grupo do rig alternativo e seus descendentes estejam ocultos (`isHidden()`/`childBonesAreHiddenToo()`) e o selecionado permaneça renderizável. Isso valida a ocultação real do GeckoLib, mas não substitui confirmação visual manual dentro do jogo.

## Etapa B — Lote 7

Recursos importados do mesmo pacote verificado, exclusivamente para #061–#070. Os modelos e texturas correspondem aos arquivos de `0061_poliwhirl` até `0070_weepinbell`; Kadabra e Alakazam usam os rigs masculinos originais porque o Minemon seleciona um único par modelo/textura por espécie e não tem seleção visual por gênero. As animações correspondentes ao rig masculino foram renomeadas para os identificadores de espécie usados pelo Minemon.

Todos os dez possuem clipes nativos de repouso e caminhada. Só Abra, Kadabra e Alakazam têm `ground_run` nativo; para os outros sete o estado de corrida continua usando a caminhada, sem criar uma corrida inventada. Os dez clipes nativos de repouso não têm movimento temporal e foram classificados como poses estáticas; nenhum balanço artificial foi adicionado. Poliwhirl/Poliwrath mantêm seus clipes aquáticos nativos. Poliwrath usa `q.biped_walk`/`q.bimanual_swing` na pose aquática do Cobblemon, mas Minemon não tem seleção runtime de natação; essa pose e os clipes foram preservados sem adicionar sistema aquático novo.

As posers de Abra, Kadabra, Alakazam e Bellsprout chamam `q.look` para o osso cabeça; o Minemon aplica o input de cabeça GeckoLib ao osso `head` correspondente (Bellsprout mapeia o alias `head_ai`). O smoke de cliente injeta entradas de olhar e mede os dois eixos da cabeça para esses quatro modelos. Weepinbell declara `q.look('head_ai')`, mas seu rig não contém `head_ai` nem `head`; a chamada é registrada como não adaptável sem inventar uma articulação.

O adaptador mantém os clipes e eventos sonoros originais e normaliza `q.*` para `query.*` e `Math.*` para `math.*`. A primeira execução de CI encontrou uma incompatibilidade real do parser GeckoLib 3.1.40 com expressões source no formato `-(...)`; elas agora são expressas como `-1*(...)`, matematicamente equivalente e sem alteração dos keyframes. `tools/verify_batch7.py` exige exatamente #061–#070, byte-preservação dos recursos aprovados #001–#060, 81 definições posteriores inalteradas, hierarquias/ossos/texturas/clipes válidos e 151 registros no JAR. O teste Java novo desserializa todos os clipes e verifica suas expressões no parser GeckoLib 3.

Etapa B está implementada; as verificações locais de recursos e preservação passaram. A execução `39533874912` encontrou a incompatibilidade Molang na prova Java; a adaptação equivalente `-1*(...)` foi publicada em `2e81d36723c649267a48a499a3f8f7b390d184ec` e passou compilação, testes Java e GameTests do servidor em `39534268151`. O smoke de cliente desse commit detectou que a escala mínima `0,01` do GeckoLib ainda deixava o rig alternativo renderizável; a ocultação direta do grupo e seus descendentes foi adicionada para nova validação.

## Etapa C — validação final

Ainda pendente: revalidar a ocultação real das asas, repetir smoke do cliente, validar o JAR contendo os 70 Pokémon e publicar o artefato final aprovado. Não iniciar o Lote 8.

## Estado desta etapa

- Testes de recursos locais: passaram para a correção das asas e preservação do Lote 6.
- Execução Gradle local: não disponível porque o wrapper precisa baixar Gradle 7.5.1 e esta sessão não tem conexão de saída para `services.gradle.org`; a validação de build seguirá no GitHub Actions após publicação.
- Próxima etapa: publicar correção de runtime da Etapa A e acompanhar o Actions; revisar relatório de articulação e conteúdo do JAR antes da entrega.

## Retomada após interrupção — 21/10/2026

- GitHub consultado antes de alterações: HEAD `430dcdc290bcab501d9e7d7cf220041efec086d5`; última execução `39535514146`, falha no verificador de asas. Nenhuma execução posterior existente.
- Causa encontrada no próprio arquivo Java: o commit anterior inseriu literalmente uma saída truncada, removendo cerca de cem linhas do smoke. Restaurado o conteúdo integral do pai `2e81d367`, preservando as verificações de movimento, articulação, combate e renderização.
- A checagem de Pidgeotto/Pidgeot agora consulta os GeoBones reais após `super.render`: rig fechado oculto com descendentes e rig aberto visível com descendentes. Não assume escala zero, incompatível com o clamp do GeckoLib. Mantidas as verificações de geometria/hierarquia/animações e a implementação de renderização.
- Verificadores locais `verify_project.py`, `verify_batch7.py` e `verify_pidge_wing_visibility.py`: aprovados. Recursos e modelos não foram reimportados ou modificados.
- Gradle local não iniciou: download do Gradle 7.5.1 bloqueado por rede (`Network is unreachable`). Compilação, Java, servidor, cliente e JAR serão validados pelo workflow completo do novo commit. Ainda não há aprovação final nem artefato final anunciado. Lote 8 não iniciado.
