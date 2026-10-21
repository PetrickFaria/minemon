# Minemon Gen 1 — Lote 8

## Objetivo

Integrar exclusivamente os recursos Cobblemon Fabric 1.8.2 dos Pokémon #071–#080, preservando os Lotes 1–7, os 151 registros e as funcionalidades existentes. A avaliação visual manual dentro do Minecraft permanece pendente. Não iniciar o Lote 9.

## Branch e base

- Branch: `feature/minemon-gen1-batch8-cobblemon`.
- Base aprovada: `feature/minemon-gen1-batch7-cobblemon` @ `b77eac9c38aa0d6a1d35c3cfab36a707ac6cab24`.
- Último commit publicado nesta branch: `e9990321f38df7ef21c412c61af246e647c4f732` — checkpoint #071–#075.
- Planejamento em `docs/animation-audit/minemon-gen1-16-batch-plan.md`: lote 8 = #071–#080.
- Nenhum workflow do Lote 8 foi iniciado. A execução-base aprovada do Lote 7 é [39543680758](https://github.com/PetrickFaria/minemon/actions/runs/39543680758).

## Pokémon e estado

| # | Espécie | Estado |
|---|---|---|
| 071 | Victreebel | Implementado e publicado; validação parcial aprovada |
| 072 | Tentacool | Implementado e publicado; validação parcial aprovada |
| 073 | Tentacruel | Implementado e publicado; validação parcial aprovada |
| 074 | Geodude | Implementado e publicado; validação parcial aprovada |
| 075 | Graveler | Implementado e publicado; validação parcial aprovada |
| 076 | Golem | Implementado e publicado; validação parcial aprovada |
| 077 | Ponyta | Implementado e publicado; validação parcial aprovada |
| 078 | Rapidash | Implementado e publicado; validação parcial aprovada |
| 079 | Slowpoke | Implementado e publicado; validação parcial aprovada |
| 080 | Slowbro | Implementado e publicado; validação parcial aprovada |

## Checkpoint 1 — Preparação concluída

- Confirmados o commit-base aprovado, o plano original e a ausência de commits posteriores na branch-base quando iniciada esta branch.
- Criada a branch sobre `b77eac9c38aa0d6a1d35c3cfab36a707ac6cab24`.
- Pacote Cobblemon Fabric 1.8.2 baixado e SHA-256 confirmado: `cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2`.
- Verificados modelos, texturas, animações e posers para #071–#080.
- Clips aquáticos nativos serão preservados, sem criar seletor de natação. Clips `ground_run` nativos existem para Geodude, Graveler, Golem e Rapidash. Ponyta tem somente `ground_runold`; fica preservado sem ser selecionado como corrida. Não há corrida terrestre nativa para Victreebel, Tentacool, Tentacruel, Slowpoke e Slowbro.

## Checkpoint 2 — #071–#075 implementados localmente

- Integrados modelo, textura, animação e licença fonte para Victreebel, Tentacool, Tentacruel, Geodude e Graveler.
- Mantidos clips de água e fallback de repouso terrestre de Tentacool/Tentacruel; nenhum comportamento de natação foi introduzido.
- As expressões Molang `q.`/`Math.` são adaptadas para GeckoLib 3.1.40 preservando a expressão. O perfil de Geodude mantém os membros que se movem no repouso/caminhada/corrida.
- Adaptado `q.look('head', ...)` de Geodude ao osso `head`, com limites de pitch/yaw do poser; o smoke do cliente mede a resposta renderizada. Victreebel usa `head_ai`, sem osso compatível, e Graveler não tem osso `head`; esses posers não são falsamente declarados como adaptados.
- `python3 tools/verify_batch8.py --segment first`: passou, verificando a faixa #071–#075, hierarquia dos ossos, dimensões de textura, referências/expressões/eventos das animações e preservação byte a byte dos recursos dos primeiros 70.
- `python3 tools/verify_project.py`: passou; 151 espécies runtime em ordem, 1.025 registros de espécies no catálogo, 5.979 referências existentes, JSON/modelos/animações/PNGs/OGGs válidos.
- `python3 -m py_compile tools/import_cobblemon_182_batch.py tools/verify_batch8.py`: passou.
- Checkpoint publicado em `e9990321f38df7ef21c412c61af246e647c4f732` com `[skip ci]`. Nenhum workflow foi iniciado.

## Checkpoint 3 — #076–#080 implementados localmente

- Integrados modelo, textura, animações e licença fonte para Golem, Ponyta, Rapidash, Slowpoke e Slowbro.
- Mantidos os clips aquáticos nativos de Slowpoke/Slowbro sem seletor de natação. Ponyta conserva `ground_runold` sem selecioná-lo; `ground_run` nativo está selecionado para Golem e Rapidash. Geodude/Graveler também têm corrida nativa.
- Perfis do caminhar de Ponyta/Rapidash e Slowpoke/Slowbro cobrem tanto articulações de locomoção como as chamas/caudas/casco animados pela fonte.
- Poser `q.look('head')` foi associado apenas aos ossos `head` existentes de Golem, Slowpoke e Slowbro; Geodude permanece adaptado com limites do seu poser. Graveler não possui osso `head` compatível e seu look não é declarado como implementado.
- `python3 tools/verify_batch8.py --segment second`: passou para #076–#080 e confirmou preservação dos primeiros 70 recursos.
- `python3 tools/verify_batch8.py`: passou para #071–#080, conferindo as dez espécies, recursos, perfis, expressões/eventos e fallbacks.
- `python3 tools/verify_project.py`: passou; 151 espécies runtime, 1.025 registros catalogados e 5.979 referências existentes; JSON/modelos/animações/PNGs/OGGs válidos.
- `python3 -m py_compile tools/import_cobblemon_182_batch.py tools/verify_batch8.py`: passou.
- Checkpoint publicado em `72d68f4c077f96311a40c265a031c9a221da0072` com `[skip ci]`; nenhum workflow do Lote 8 foi iniciado.

## Checkpoint 4 — validação final

- Workflow atualizado para executar `verify_batch8.py` contra o commit-base `b77eac9c38aa0d6a1d35c3cfab36a707ac6cab24` e também contra o JAR final; a regressão `verify_pidge_wing_visibility.py` continua nos dois pontos.
- Commit de implementação/gates: `8c3ab0ddd70d97bb78a9505a97e7a6afe008310c`.
- GitHub Actions: [execução 39617375461](https://github.com/PetrickFaria/minemon/actions/runs/39617375461), concluída em `failure`.
- Os gates de recursos, compilação/testes Java e GameTests de servidor passaram. O smoke do cliente falhou ao validar a corrida real do Golem (`idle=2 walk=2 run=0`); por isso, inspeção do JAR e publicação foram puladas.
- Diagnóstico do teste: o perfil `run` media `left_upper_arm`/`right_upper_arm`, que o clip nativo `animation.golem.ground_run` mantém em pose rígida. O clip anima `body` via expressões Molang e `body_roll` por keyframes e rotação, então o perfil de corrida foi ajustado para medir esses ossos efetivamente animados. Nenhum modelo, clip ou outra espécie foi alterado.
- A verificação das asas de Pidgeotto/Pidgeot passou na etapa anterior do mesmo workflow.
- Commit-base do reparo: `ce08db1bf93bb6aa437d4d579913df0fb09c4a13`. Branch: `feature/minemon-gen1-batch8-cobblemon`.
- O JAR final, hash e expiração continuam pendentes até um workflow verde.

## Checkpoint 5 — reparo específico do smoke de Golem

- Corrigido o perfil de corrida do Golem de braços estáticos para `body` e `body_roll`, alvos realmente animados pelo clip nativo fonte. Os perfis de repouso/caminhada permanecem inalterados.
- Pokémon concluídos: #071–#080; nenhuma espécie nova iniciada. Pendência: repetir a validação final do Lote 8 após este reparo, incluindo smoke do cliente e inspeção do JAR.
- `python3 tools/verify_batch8.py`: passou; faixa #071–#080, perfis, recursos e preservação byte a byte dos primeiros 70 verificados.
- `python3 tools/verify_project.py`: passou; 151 espécies em ordem, 1.025 registros, 5.979 referências existentes e JSON/modelos/animações/PNGs/OGGs válidos.
- `python3 -m py_compile tools/verify_batch8.py` e `git diff --check`: passaram.
- `./gradlew test`: não iniciou porque o ambiente não conseguiu baixar Gradle 7.5.1 (`Network is unreachable`); os testes Java não foram ignorados e permanecem a cargo do Actions.
- Actions anterior [39617375461](https://github.com/PetrickFaria/minemon/actions/runs/39617375461) falhou no perfil de corrida do Golem. A execução de validação da correção [39643119039](https://github.com/PetrickFaria/minemon/actions/runs/39643119039) concluiu com falha no cliente: Golem passou após o ajuste, e Graveler falhou com `run=0`; build/testes Java e GameTests de servidor passaram. Diagnóstico publicado no artefato [12371321174](https://github.com/PetrickFaria/minemon/actions/runs/39643119039/artifacts/12371321174).
- Commit de código do reparo publicado: `f946031ca2bf714ba3b8022359c4e5df04b246c9` (derivado do último estado `ce08db1`).
- Resultado desta sessão: a execução `39643119039` concluiu reprovada no smoke; inspeção/publicação do JAR foram puladas. Correção seguinte registrada no Checkpoint 6. Não iniciar o Lote 9 antes de aprovação integral.


## Checkpoint 6 — reparo específico do smoke de Graveler

- O smoke atualizado confirmou que o Golem passou a ver movimento na corrida; falhou em seguida no Graveler (`idle=2 walk=2 run=0`).
- Diagnóstico: o perfil `run` do Graveler media `arm_bottom_left`/`arm_bottom_right`, estáticos no clip nativo. O clip anima `body` com expressões Molang e `body_roll` com keyframes/rotação. O perfil foi atualizado para esses ossos e a regressão Java agora verifica o canal animado de ambos os Pokémon rochosos.
- Nenhum modelo, animação fonte ou espécie fora do Lote 8 foi alterado.
- `python3 tools/verify_batch8.py`, `python3 tools/verify_project.py` e `git diff --check`: passaram após o reparo. Testes Java e smoke real seguem pendentes do próximo Actions.
- Commit de correção publicado: `a9c2cd6d226c404f0bc7d29eb65829f586314fea`; commit-base: `f0c940f00262b7bf7692c622ea5c3b8840bbf45b`.
- GitHub Actions: [39658750383](https://github.com/PetrickFaria/minemon/actions/runs/39658750383), estado inicial `in_progress`; job `build` está preparando o Gradle e ainda não concluiu os verificadores. Nenhum outro workflow foi iniciado.
- Próxima ação: consultar essa mesma execução. Não abrir Lote 9 antes da aprovação integral do Lote 8.

## Bloqueios e limitações

- A distribuição Gradle 7.5.1 não estava alcançável de `services.gradle.org` em duas tentativas locais; testes Java/build ainda não foram executados localmente. Nenhum teste foi desativado. A URL da fonte Cobblemon ficou acessível e o seu arquivo foi baixado/verificado durante a recuperação do workspace.
- Aparência visual manual dentro do Minecraft continuará pendente; os gates automatizados de cliente verificam renderização, movimento e transforms reais dos ossos.
- Erros de código/testes identificados no checkpoint parcial: nenhum após a correção dos perfis de tentáculos para os nomes de osso reais da fonte.

## Checkpoint 7 — auditoria da otimização do smoke (2026-10-21)

- Última referência de código: `a9c2cd6d226c404f0bc7d29eb65829f586314fea`; a branch recebeu o commit documental `747f344a3ebe2da7d1136309fc72ee28468903ae` após a atualização do código.
- Actions existente [39658750383](https://github.com/PetrickFaria/minemon/actions/runs/39658750383) segue `in_progress`; gates de recursos, perfis Pidge, Gradle `clean test build` e GameTests do servidor passaram. O smoke do cliente continua ativo; inspeção/upload do JAR permanecem pendentes. Não foi iniciada uma execução duplicada.
- Auditoria do workflow confirmou que qualquer falha do cliente reinicia o smoke inteiro com outro timeout de 2700 s, sem classificar falha transitória.
- O smoke completo percorre as 151 espécies com janela de 80 ticks por espécie, guarda captura de tela por espécie, e só escreve os relatórios de aprovação detalhados no final. O teste seletivo e progresso por espécie/fase ainda não existem.
- Verificação local dos recursos encontrou expressões Cobblemon `q.r.*` nos arquivos de animação de Charizard e Blastoise, incluindo `q.r.velocity_forward`. Seus efeitos de runtime ainda precisam ser classificados e verificados contra o parser GeckoLib 3.1.40; nenhuma expressão foi alterada nem silenciada nesta auditoria.
- Perfis do Golem e Graveler usam agora `body`/`body_roll` para corrida e têm regressões publicadas; o smoke real para confirmar esses perfis está dentro do mesmo run ativo.
- Etapa seguinte: após o run atual encerrar, implementar seleção de espécies usando as mesmas verificações, logs de progresso por transição de fase, preflight Molang acionável e remoção do retry integral; manter o smoke completo como gate final. Nenhum Lote 9 será iniciado.

## Checkpoint 8 — seletor, preflight e remoção de retry (2026-10-21)

- Branch: `feature/minemon-gen1-batch8-cobblemon`. Commits de implementação publicados desde o checkpoint 7: `189879b` (seletor de espécies), `be1edfd` (filtro + relatórios de progresso no smoke), `385f7b2` (encaminha `-PclientSmokeSpecies`), `f77c3ea` (remove retry integral), `ba0eea4`/`c53ee6b` (preflight Molang estático e teste Java com o parser GeckoLib), `23e2933`/`e8e8fed` (preflight antes de iniciar Minecraft e ajuste da expressão regular), `7cf65baa` (progresso de inicialização), `9fd6744` (combina o relatório ForgeGradle com o build em uma única invocação Gradle). Commits `[skip ci]` para não duplicar o Actions em andamento.
- Seleção direcionada: `./gradlew --no-daemon runClient -PclientSmokeTest -PclientSmokeSpecies=golem,graveler`; aceita slug, nome de espécie ou número da Pokédex (ex.: `#076`). Omitir a propriedade executa todas as 151 espécies. A seleção passa pelos mesmos carregamentos, renderização, textura, movimento, fases, verificações de articulação e regressões das espécies selecionadas. O setup comum, roster, mundo e combate continuam ativos.
- Validação rápida de diagnóstico (não aprova o lote): `python3 tools/verify_project.py`, `python3 tools/verify_molang_compatibility.py`, testes Java direcionados `./gradlew --no-daemon test --tests br.com.petrickfaria.minemon.gametest.SmokeSpeciesSelectionTest --tests br.com.petrickfaria.minemon.gametest.ActiveAnimationMolangPreflightTest` e cliente seletivo sob `xvfb-run`.
- Validação completa de aprovação: workflow completo executa `buildEnvironment clean test build`, GameTests do servidor, cliente sem `clientSmokeSpecies` (151 espécies), verificações de regressão, inspeção do JAR e upload. O modo rápido nunca substitui esses gates.
- O client smoke agora emite linhas `[CLIENT SMOKE]` uma vez por transição de carregamento/renderização/repouso/caminhada/corrida, com número/N, Pokédex, contagem de janelas, duração e última janela concluída. O modo completo permanece sem filtro e todos os gates finais continuam obrigatórios.
- Workflow: removido o segundo lançamento integral; timeout único permanece em 2700 s, reporta timeout versus exit code, imprime cauda de `client-validation.txt` e `latest.log`, e falha sem retry.
- Otimização de preparação: `buildEnvironment` agora roda junto de `clean test build`, preservando o relatório e removendo uma inicialização separada do Gradle.
- Molang: `verify_molang_compatibility.py` passou localmente. Achou 783 chamadas `q.r.*` em seis arquivos: Arbok, Blastoise, Charizard, Ekans, Pidgeot e Venusaur; nenhuma nas seleções ativas de repouso/caminhada/corrida. Estão em clips alternativos `snake_*` não selecionados ou `ride_*`/montaria. GeckoLib 3.1.40 realmente registra exceções “Function 'query.r.…' couldn't be found” ao desserializar essas expressões; o carregamento prossegue, mas os keyframes que dependem dessas funções não têm avaliação válida. O preflight as mostra explicitamente e falha se aparecerem em clipes ativos; não as silencia nem reescreve.
- Regressões de Golem/Graveler, Pidgeotto/Pidgeot, chama do Charmander, braços dos iniciais e verificações do roster continuam no caminho completo. O filtro só omite verificações específicas de espécies que não foram selecionadas; não reduz critérios para as selecionadas.
- Testes executados neste checkpoint: script Molang local passou (783 referências classificadas, zero em clipes ativos). Teste JUnit do seletor e teste JUnit com MolangParser foram adicionados, mas aguardam CI; o smoke direcionado ainda não foi iniciado.
- Actions preexistente [39658750383](https://github.com/PetrickFaria/minemon/actions/runs/39658750383) ainda estava no passo de cliente em `20:13Z`. Logs mostram primeira tentativa encerrada por timeout de 2700 s às `19:54:16Z`; o workflow antigo iniciou automaticamente a segunda tentativa, que não usa estas otimizações. Não há JAR publicado ainda. Nenhuma execução duplicada foi disparada.

## Próxima ação

Aguardar o run legado `39658750383` encerrar sem iniciá-lo novamente. Depois, executar uma única validação completa do HEAD atualizado, corrigir somente falhas reais e registrar o JAR e o resultado.


## Checkpoint 9 — run legado encerrado e otimização localmente auditada (2026-10-21)

- HEAD remoto confirmado: `a3f73867c3f6758c3bfe24318a572dfe9861e9f1`. A branch remota permanece `feature/minemon-gen1-batch8-cobblemon`; as alterações locais preexistentes no worktree não foram tocadas.
- Actions legado [39658750383](https://github.com/PetrickFaria/minemon/actions/runs/39658750383), SHA `a9c2cd6d226c404f0bc7d29eb65829f586314fea`, encerrou com falha no smoke do cliente: a segunda tentativa completa atingiu o timeout de 2700 s. Logs mostram o cliente abrindo mundo e conectando o jogador, sem progresso de teste disponível nesse código antigo. Por isso, não se pode atribuir uma espécie/fase específica à falha. Compilação/testes Java e GameTests do servidor passaram; verificação do JAR e publicação foram puladas. Diagnósticos: artefato [minemon-test-diagnostics](https://github.com/PetrickFaria/minemon/actions/runs/39658750383/artifacts/12380461532), expira em 2026-10-28.
- O workflow antigo já encerrou, portanto não há execução equivalente em andamento. O HEAD otimizado `a3f7386` não foi validado por Actions: todos os commits de implementação foram intencionalmente publicados com `[skip ci]` enquanto o run anterior estava ativo.
- Validação rápida local contra `a3f7386`: `verify_project.py` passou (151 espécies runtime, 1.025 registros, 5.979 referências válidas); preflight Molang passou (783 referências verificadas, zero nos clips ativos); `verify_batch8.py` e regressão `verify_pidge_wing_visibility.py` passaram; `py_compile` passou. Testes Java locais não iniciaram porque o wrapper não conseguiu baixar Gradle 7.5.1 (`Network is unreachable`). Não houve smoke direcionado local.
- Próximo passo obrigatório: executar uma única validação completa do HEAD otimizado (`buildEnvironment clean test build`, GameTests, smoke de cliente sem filtro, inspeção do JAR). O workflow atual declara `workflow_dispatch`; o acionamento manual não cria commit adicional. Até o Actions verde, testes Java/cliente e JAR da otimização permanecem pendentes. Não iniciar o Lote 9.


## Checkpoint 10 — workflow manual disparado na branch incorreta (2026-10-21)

- Execução encontrada após acionamento manual: [39684036307](https://github.com/PetrickFaria/minemon/actions/runs/39684036307), evento `workflow_dispatch`, branch `main`, SHA `b7dc3b09416546bfdd6c4a27cbfc47f6d010a4fc`.
- No momento da consulta, o job estava em andamento em `Build and run tests`; smoke do cliente ainda pendente. Esta execução usa o código da branch `main`, não contém as otimizações publicadas na branch do Lote 8 e não aprova a versão otimizada nem o Lote 8.
- Branch que precisa de validação: `feature/minemon-gen1-batch8-cobblemon`. HEAD de código otimizado: `a3f73867c3f6758c3bfe24318a572dfe9861e9f1`; HEAD atual inclui somente checkpoints documentais posteriores.
- Não iniciado outro workflow enquanto a execução manual existente permanece ativa. Próxima ação: aguardar/cancelar a execução incorreta e disparar `Build Minemon` com a branch `feature/minemon-gen1-batch8-cobblemon` selecionada. Não iniciar Lote 9 até o run dessa branch passar integralmente.


## Checkpoint 11 — tentativa 2 do run antigo em andamento (2026-10-21)

- O run [39658750383](https://github.com/PetrickFaria/minemon/actions/runs/39658750383) está na tentativa 2, evento original `push`, ainda no SHA antigo `a9c2cd6d226c404f0bc7d29eb65829f586314fea`. Não é um novo `workflow_dispatch` do HEAD otimizado.
- Nesta tentativa, verificações de recursos, regressões, compilação/testes Java e GameTests do servidor passaram. A etapa ativa é `Launch Minecraft client and validate GeckoLib cache`; inspeção do JAR e upload continuam pendentes.
- Não iniciar outra execução enquanto este smoke completo estiver ativo. Esta tentativa ainda não pode aprovar as melhorias recentes porque executa workflow/código anterior a elas.
- Próxima ação: acompanhar o encerramento deste run legado; depois, iniciar um único `workflow_dispatch` selecionando explicitamente a branch `feature/minemon-gen1-batch8-cobblemon` (HEAD de código otimizado `a3f7386`, atualmente com checkpoints documentais posteriores). Somente o resultado verde desse HEAD libera o Lote 9.
