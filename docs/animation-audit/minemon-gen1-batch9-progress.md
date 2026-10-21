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
| 081 | Magnemite | Pendente de auditoria/importação |
| 082 | Magneton | Pendente de auditoria/importação |
| 083 | Farfetch'd | Pendente de auditoria/importação |
| 084 | Doduo | Pendente de auditoria/importação |
| 085 | Dodrio | Pendente de auditoria/importação |
| 086 | Seel | Pendente de auditoria/importação |
| 087 | Dewgong | Pendente de auditoria/importação |
| 088 | Grimer | Pendente de auditoria/importação |
| 089 | Muk | Pendente de auditoria/importação |
| 090 | Shellder | Pendente de auditoria/importação |

A correspondência está confirmada no planejamento original `docs/animation-audit/minemon-gen1-16-batch-plan.md`: Lote 9 = #081–#090.

## Checkpoint 1 — correção e preparação

- A execução #152 ([39692112208](https://github.com/PetrickFaria/minemon/actions/runs/39692112208)) terminou com falha em `:compileTestJava`, antes de GameTests, smoke do cliente, inspeção/publicação do JAR. Causa exata: identificador `COBALT_QUERY` não declarado; a classe declara `COBBLEMON_QUERY`.
- Correção publicada como commit independente em `feature/minemon-gen1-batch8-cobblemon`: `40458f0a81b3395ec7fc30400c65171fab606253`.
- Tentativa local de `./gradlew --no-daemon compileTestJava` na mesma versão falhou antes de iniciar Gradle porque a distribuição Gradle 7.5.1 não pôde ser baixada (`Network is unreachable`). Não é resultado de compilação; validação Java segue pendente do Actions.
- Branch do Lote 9 criada sobre o commit corrigido. Nenhuma espécie #081–#090 foi alterada ainda.
- Não foi iniciada validação final duplicada; a próxima execução completa deverá usar a branch do Lote 9 depois dos checkpoints de implementação. A verificação de `compileTestJava` permanece obrigatória.

## Próximo passo

Auditar os modelos, texturas, posers, animações, expressões Molang e eventos sonoros de #081–#090 no pacote exato Cobblemon Fabric 1.8.2; registrar disponibilidade/incompatibilidades e então implementar #081–#085 como primeiro checkpoint. Após cada grupo, executar verificadores direcionados e publicar commit intermediário.

## Preservação e validação final

Preservar integralmente os 80 Pokémon anteriores, a chama do Charmander, os braços do Charizard, as asas de Pidgeotto/Pidgeot, os perfis Golem/Graveler, as otimizações de validação, os recursos não atualizados e os registros das 151 espécies.

A validação completa continuará pendente até compilação, testes Java, GameTests, smoke integral do cliente, regressões cumulativas, inspeção real do JAR e publicação do artefato. A avaliação visual manual dentro do Minecraft será documentada como pendência separada.
