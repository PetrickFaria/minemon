# Minemon Gen 1 — Progresso consolidado dos Lotes 12–16

## Estado atual

- Último lote concluído e aprovado: Lote 14 (#131–#140), commit de código `76fcb3074cc65c62dea6b94c291de90925494e95`; execução #39885955086 concluída com sucesso.
- Artefato aprovado: [Actions artifact #12474937645](https://github.com/PetrickFaria/minemon/actions/runs/39885955086/artifacts/12474937645), ZIP `Minemon-1.19.2-2.0.0-gen1`, contendo `Minemon-1.19.2-2.0.0.jar`; expira 2026-11-06 02:49:53 UTC.
- JAR baixado e inspecionado; runtime e verificadores cumulativos dos Lotes 8–14 passaram, 140 Pokémon atualizados e 151 registros preservados.
- Lotes 1–14 aprovados; 140 Pokémon preservados cumulativamente.
- Próximo lote ativo: Lote 15 (#141–#150), branch `feature/minemon-gen1-batch15-cobblemon`, criada sobre o checkpoint documental final do Lote 14. Integração dos dez recursos está em andamento local.

| Lote | Pokédex | Branch | Estado |
|---:|---|---|---|
| 12 | #111–#120 | `feature/minemon-gen1-batch12-cobblemon` | aprovado; Actions #39865755919; artefato #12466881117 |
| 13 | #121–#130 | `feature/minemon-gen1-batch13-cobblemon` | aprovado; Actions #39878283990; artefato #12470668831 |
| 14 | #131–#140 | `feature/minemon-gen1-batch14-cobblemon` | aprovado; Actions #39885955086; artefato #12474937645 |
| 15 | #141–#150 | `feature/minemon-gen1-batch15-cobblemon` | commit `34b9ea1a739012d2a5375a4c1ef30a80f1bab76a`; Actions #39900310676: Java, build e 13/13 GameTests PASS; smoke completo do cliente em andamento; JAR pendente |
| 16 | #151 | `feature/minemon-gen1-batch16-cobblemon` | não iniciado |

## Correções preservadas e pendências

- Graveler e Golem: corrida aprovada.
- Pidgeotto e Pidgeot: correção das asas aprovada.
- Charizard: braços aprovados.
- Chamas de Charmander, Charmeleon e Charizard: refinamento visual futuro; não alterar nos Lotes 14–16.
- Avaliação visual manual de Pokémon novos permanece distinta da aprovação automatizada.

## Regra de retomada

Retomar do commit remoto e da execução de Actions mais recentes; não iniciar lote seguinte sobre falha técnica bloqueante. Cada lote recebe seu documento próprio, checkpoints remotos, validação automatizada completa, inspeção do JAR e artefato antes do próximo.


## Checkpoint aprovado do Lote 14 — 2026-10-23

- Branch Lote 14: `feature/minemon-gen1-batch14-cobblemon`.
- Commit de código aprovado: `76fcb3074cc65c62dea6b94c291de90925494e95`; Actions #39885955086 success.
- JAR: `Minemon-1.19.2-2.0.0.jar`, dentro de [artefato ZIP #12474937645](https://github.com/PetrickFaria/minemon/actions/runs/39885955086/artifacts/12474937645); expira em 2026-11-06 02:49:53 UTC.
- Testes: compilação/build, suíte Java, 13 GameTests, smoke cliente e inspeção real do JAR passaram. Verificadores runtime e cumulativos L8–14 passaram; 140 espécies atualizadas, 151 registros.
- Correções aprovadas preservadas; avaliação manual dos Pokémon novos pendente; chamas dos três iniciais de fogo continuam pendentes e intactas.
- Próximo passo: iniciar Lote 15 #141–#150 a partir do commit documental final do Lote 14.

## Checkpoint inicial do Lote 15 — 2026-10-23

- Base: `d0f0cd6ca74bd7dae9139c53cf56b38f25cfbcb4`; execução L14 #39885955086 permaneceu aprovada para `76fcb3074cc65c62dea6b94c291de90925494e95`.
- Branch remota/local: `feature/minemon-gen1-batch15-cobblemon`.
- Os recursos nativos de #141–#150 foram importados da fonte Cobblemon 1.8.2 e a evidência de fonte foi gerada.
- Verificadores estruturais e de preservação dos Lotes 8–15 passaram localmente; `verify_project.py`, `verify_runtime.py`, preflight Molang e `git diff --check` passaram.
- O teste Java direcionado local não iniciou porque o wrapper não conseguiu baixar Gradle 7.5.1 (`Network is unreachable`); a validação Java foi delegada ao workflow. GameTests, smoke do cliente, JAR e publicação ainda não foram aprovados.
- Actions #39900049407 falhou em `compileTestJava` porque `JsonArray.asList()` não existe na versão de Gson usada; a correção foi publicada em `34b9ea1a…` por iteração explícita.
- Na execução #39900310676, teste Java direcionado, suíte/build e 13/13 GameTests passaram. Logs do servidor estão no artefato #12479050705, expiração 2026-10-30 04:28:33 UTC. O smoke completo do cliente está em andamento.
- Próxima ação: consultar a mesma execução #39900310676 até a conclusão do smoke; inspecionar o JAR apenas se todos os testes passarem.
