# Minemon Gen 1 — Lote 9

## Objetivo

Integrar exclusivamente os recursos Cobblemon Fabric 1.8.2 dos Pokémon #081–#090, preservando os Lotes 1–8, os 151 registros e os recursos/funcionalidades existentes. A avaliação visual manual no Minecraft continuará pendente. Não iniciar o Lote 10.

## Branch e base

- Branch de trabalho: `feature/minemon-gen1-batch9-cobblemon`.
- Base mais recente do Lote 8: `85ed009ff141c7ecfa849969ae354b065596d8e6`.
- Correção pontual de compilação publicada na branch do Lote 8: `40458f0a81b3395ec7fc30400c65171fab606253` — substitui a referência incorreta `COBALT_QUERY` pela constante existente `COBBLEMON_QUERY` em `gametest/ActiveAnimationMolangPreflightTest.java`.
- O Lote 9 foi criado sobre o commit de correção acima, mantendo todos os commits e recursos prévios.

## Pokémon do lote

| # | Espécie | Estado |
|---:|---|---|
| 081 | Magnemite | Importado #081–#085; verificação estrutural direcionada aprovada |
| 082 | Magneton | Importado #081–#085; verificação estrutural direcionada aprovada |
| 083 | Farfetch'd | Importado #081–#085; verificação estrutural direcionada aprovada |
| 084 | Doduo | Importado #081–#085; verificação estrutural direcionada aprovada |
| 085 | Dodrio | Importado #081–#085; verificação estrutural direcionada aprovada |
| 086 | Seel | Pendente de auditoria/importação |
| 087 | Dewgong | Pendente de auditoria/importação |
| 088 | Grimer | Pendente de auditoria/importação |
| 089 | Muk | Pendente de auditoria/importação |
| 090 | Shellder | Pendente de auditoria/importação |

A correspondência está confirmada no planejamento original `docs/animation-audit/minemon-gen1-16-batch-plan.md`: Lote 9 = #081–#090.

## Checkpoint 1 — correção e preparação

- A execução #152 ([39692112208](https://github.com/PetrickFaria/minemon/actions/runs/39692112208)) terminou com falha em `:compileTestJava`, antes de GameTests, smoke do cliente, inspeção/publicação do JAR. Causa exata: identificador `COBALT_QUERY` não declarado; a classe declara `COBBLEMON_QUERY`.
- Correção publicada como commit independente em `feature/minemon-gen1-batch8-cobblemon`: `40458f0a81b3395ec7fc30400c65171fab606253`.
- A execução automática #153 ([39693679007](https://github.com/PetrickFaria/minemon/actions/runs/39693679007)) confirmou `compileTestJava` aprovado após o commit `40458f0`. O job de testes Java falhou em dois testes, antes de GameTests, smoke do cliente, JAR e artefato.
- Diagnósticos #153: `ActiveAnimationMolangPreflightTest` encontrou `-(...)` no idle ativo do Weedle, sintaxe que GeckoLib 3.1.40 não analisa; foi feita apenas a reescrita algébrica equivalente `-1*(...)`, sem mudar o movimento. `SmokeSpeciesSelectionTest` esperava excluir Golem apesar de solicitar `#076`; a expectativa agora inclui corretamente Graveler, Golem e Farfetch'd.
- Artefatos de diagnóstico de #153 expiravam em 2026-10-28; os XML foram baixados e inspecionados. O JAR não foi gerado/publicado por esse run.
- A verificação prévia Molang informa 783 referências `query.r` em clips não selecionados, principalmente clipes montados/riding; nenhuma ocorre nos clips ativos selecionados. Elas foram reportadas e não suprimidas. Essa verificação antecipada passa.
- O Gradle local continua indisponível para compilação porque a distribuição 7.5.1 não está no cache e o host não consegue baixá-la. O próximo Actions após o checkpoint testará novamente os dois testes corrigidos.
- Recursos #081–#085 importados do Cobblemon Fabric 1.8.2, cujo SHA-256 auditado é `cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2`. Adaptados `q.` → `query.` e `Math.` → `math.`. Verificador analisa rigs, dimensões da textura, hierarquias, referências de ossos e eventos.
- #081 Magnemite, #082 Magneton e #083 Farfetch'd preservam os clips aéreos nativos. #084 Doduo e #085 Dodrio preservam as duas/três cabeças, pescoços e pernas articuladas. Nenhum tem clip nativo de corrida terrestre.
- A branch de trabalho foi criada sobre `40458f0`; commit inicial da continuidade `2503dd085402211ec2e9c6287fca83ab4747d2d5`. Ainda sem commit remoto dos recursos #081–#085 neste ponto de edição.
- Verificações direcionadas locais aprovadas: `verify_project.py`, `verify_batch9.py --segment first`, `verify_batch8.py`, `verify_pidge_wing_visibility.py` e `verify_molang_compatibility.py`. A última informa compatibilidade de clips ativos e deixa explícitas as referências não selecionadas. Testes Java completos e client smoke permanecem pendentes.
- Nenhum Pokémon #086–#090 foi importado ainda. A validação integral do Lote 8 continua pendente; o Lote 9 avança conforme autorização expressa, mas não há JAR aprovado.

## Próximo passo

Publicar o checkpoint #081–#085 com `[skip ci]` depois de registrar os resultados estruturais; importar #086–#090 e publicar o segundo checkpoint. Ao final, atualizar o workflow para executar verificações cumulativas dos lotes 8 e 9 e então iniciar uma única validação completa. Não iniciar Lote 10.

## Preservação e validação final

Preservar integralmente os 80 Pokémon anteriores, a chama do Charmander, os braços do Charizard, as asas de Pidgeotto/Pidgeot, os perfis Golem/Graveler, as otimizações de validação, os recursos não atualizados e os registros das 151 espécies.

A validação completa continuará pendente até compilação, testes Java, GameTests, smoke integral do cliente, regressões cumulativas, inspeção real do JAR e publicação do artefato. A avaliação visual manual dentro do Minecraft será documentada como pendência separada.
