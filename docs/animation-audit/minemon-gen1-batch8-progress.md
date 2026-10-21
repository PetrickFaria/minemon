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
- GitHub Actions: [execução 39617375461](https://github.com/PetrickFaria/minemon/actions/runs/39617375461), status inicial `queued`, conclusão pendente. Nenhuma segunda execução foi iniciada.
- O artefato JAR ainda não pode ser confirmado ou linkado enquanto esta execução não concluir.
- Próximos gates no Actions: build completo, testes Java, GameTests do servidor, smoke do cliente, inspeção cumulativa do JAR e regressão das asas Pidgeotto/Pidgeot.
- A execução 39617375461 ficou `queued` na primeira consulta. Próxima sessão: consultar essa execução existente; não iniciar outra enquanto estiver em andamento.
- JAR, hash e expiração ainda pendentes até a conclusão aprovada do Actions.


## Bloqueios e limitações

- A distribuição Gradle 7.5.1 não estava alcançável de `services.gradle.org` em duas tentativas locais; testes Java/build ainda não foram executados localmente. Nenhum teste foi desativado. A URL da fonte Cobblemon ficou acessível e o seu arquivo foi baixado/verificado durante a recuperação do workspace.
- Aparência visual manual dentro do Minecraft continuará pendente; os gates automatizados de cliente verificam renderização, movimento e transforms reais dos ossos.
- Erros de código/testes identificados no checkpoint parcial: nenhum após a correção dos perfis de tentáculos para os nomes de osso reais da fonte.

## Próxima ação

Importar #076–#080, validar o segundo grupo e publicar o checkpoint 3. Não iniciar o Lote 9.
