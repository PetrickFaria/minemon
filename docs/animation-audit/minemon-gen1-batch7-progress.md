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

**Correção aplicada:** clipes de voo/hover e solo/repouso mapeados pelo poser agora definem escala nos grupos pai para mostrar um único par por pose. Os cubos, ossos, textura e animações de movimento das asas permanecem intactos. A transição nativa de `ground_quirk` do Pidgeotto e a troca dinâmica de asas do `ride_dive` do Pidgeot foram preservadas. O verificador permite apenas mudanças nessas trilhas de escala nas animações anteriores; os demais recursos dos Lotes 1–6 são comparados byte a byte.

Verificação automatizada incluída:

- `tools/verify_pidge_wing_visibility.py`: integridade da hierarquia e cubos das duas alternativas, escala em clipes selecionados, transições dinâmicas, uso dos clipes ativos e inspeção opcional do JAR.
- Smoke de cliente exige que os grupos `wings_closed` e `wings_open` apresentem escalas 0/1 em amostras estabilizadas de Pidgeotto e Pidgeot. Isso valida transforms carregados/renderizados pelo GeckoLib, mas não substitui confirmação visual manual dentro do jogo.

## Etapa B — Lote 7

Recursos importados do mesmo pacote verificado, exclusivamente para #061–#070. Os modelos e texturas correspondem aos arquivos de `0061_poliwhirl` até `0070_weepinbell`; Kadabra e Alakazam usam os rigs masculinos originais porque o Minemon seleciona um único par modelo/textura por espécie e não tem seleção visual por gênero. As animações correspondentes ao rig masculino foram renomeadas para os identificadores de espécie usados pelo Minemon.

Todos os dez possuem clipes nativos de repouso e caminhada. Só Abra, Kadabra e Alakazam têm `ground_run` nativo; para os outros sete o estado de corrida continua usando a caminhada, sem criar uma corrida inventada. Os dez clipes nativos de repouso não têm movimento temporal e foram classificados como poses estáticas; nenhum balanço artificial foi adicionado. Poliwhirl/Poliwrath mantêm seus clipes aquáticos nativos. Poliwrath usa `q.biped_walk`/`q.bimanual_swing` na pose aquática do Cobblemon, mas Minemon não tem seleção runtime de natação; essa pose e os clipes foram preservados sem adicionar sistema aquático novo.

As posers de Abra, Kadabra, Alakazam e Bellsprout chamam `q.look` para o osso cabeça; o Minemon aplica o input de cabeça GeckoLib ao osso `head` correspondente (Bellsprout mapeia o alias `head_ai`). O smoke de cliente injeta entradas de olhar e mede os dois eixos da cabeça para esses quatro modelos. Weepinbell declara `q.look('head_ai')`, mas seu rig não contém `head_ai` nem `head`; a chamada é registrada como não adaptável sem inventar uma articulação.

O adaptador mantém os clipes e eventos sonoros originais e normaliza `q.*` para `query.*`, incluindo `Math.*` para `math.*`. `tools/verify_batch7.py` exige exatamente #061–#070, byte-preservação dos recursos aprovados #001–#060, 81 definições posteriores inalteradas, hierarquias/ossos/texturas/clipes válidos e 151 registros no JAR. O teste Java novo desserializa todos os clipes e verifica suas expressões no parser GeckoLib 3.

Etapa B está implementada; as verificações locais de recursos e preservação passaram. A compilação/testes integrados e o smoke de cliente permanecem pendentes no Actions.

## Etapa C — validação final

Ainda pendente: compilação, testes Java, GameTests do servidor, smoke do cliente, validação dos recursos/JAR, confirmação dos 151 registros e publicação do JAR final aprovado. Não iniciar o Lote 8.

## Estado desta etapa

- Testes de recursos locais: passaram para a correção das asas e preservação do Lote 6.
- Execução Gradle local: não disponível porque o wrapper precisa baixar Gradle 7.5.1 e esta sessão não tem conexão de saída para `services.gradle.org`; a validação de build seguirá no GitHub Actions após publicação.
- Próxima etapa: publicar commit específico da Etapa B e acompanhar seu Actions; depois revisar logs, relatório de articulação e conteúdo do JAR antes da entrega.
