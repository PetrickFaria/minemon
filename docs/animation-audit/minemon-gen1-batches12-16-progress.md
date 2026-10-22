# Minemon Gen 1 — Progresso consolidado dos Lotes 12–16

## Último lote concluído

- Lote 11 (#101–#110), aprovado no Actions #39828753587, commit de código `c0cfdc43f80df32bac99a44b1375464c40087561`; checkpoint remoto posterior `0816533addee80e9e17a684c94bed399f4a9488f`.
- Artefato do Lote 11: #12450059381. Expira em 2026-11-05 18:57:57 UTC.

## Estado atual

- Lote ativo: 12 (#111–#120), branch `feature/minemon-gen1-batch12-cobblemon`.
- Base: `0816533addee80e9e17a684c94bed399f4a9488f`.
- Lotes 1–11/110 espécies: preservados e validados na base aprovada.
- Lote 12: recursos #111–#120 integrados; Java/build e GameTests passaram nas execuções #39841242841 e #39850003592. O primeiro smoke apontou fallback de Tangela classificado incorretamente; corrigido para `static` no commit `52abd5e93bf1f8da41d74702baf138380f991d09`. O segundo smoke apontou que o perfil de repouso de Alakazam observava joints com movimento abaixo do limiar, apesar de o clip nativo movimentar pés/pernas/mãos. Ajuste de perfil publicado no commit `d20a00bdd60c05f5b04c89fbddf3f68f867f97c1`, com joints comprovadamente animados; verificações cumulativas locais PASS. Actions #39855369559: Java/build PASS e smoke completo do cliente PASS; GameTests 12/13 PASS, com falha em `wildaggressionactuallyexpires`. A inspeção/upload do JAR foram pulados, sem artefato de JAR. A invariável de alvo residual foi corrigida localmente, com CI pendente; Lote 13 não iniciado.
- Diagnósticos do primeiro run: artifact #12456257638 (expira 2026-10-29 20:30:07 UTC); logs do servidor #12452159446 (expira 2026-10-29 19:37:33 UTC). Nenhum JAR foi publicado para esse run.
- Lotes 13–16: ainda não iniciados.
- Próximo passo: publicar e validar a correção do GameTest, inspecionar/publicar o JAR do Lote 12; somente depois criar a branch e iniciar o Lote 13.

## Sequência autorizada

| Lote | Pokémon | Branch | Estado |
|---:|---:|---|---|
| 12 | #111–#120 | `feature/minemon-gen1-batch12-cobblemon` | Java/build e smoke passam; um GameTest falhou; correção local pendente de CI |
| 13 | #121–#130 | `feature/minemon-gen1-batch13-cobblemon` | Não iniciado |
| 14 | #131–#140 | `feature/minemon-gen1-batch14-cobblemon` | Não iniciado |
| 15 | #141–#150 | `feature/minemon-gen1-batch15-cobblemon` | Não iniciado |
| 16 | #151 | `feature/minemon-gen1-batch16-cobblemon` | Não iniciado |

## Regras de continuidade

- Continuar apenas do último commit remoto verificável.
- Não iniciar o próximo lote sobre falha técnica bloqueante.
- Publicar commit e artefato de cada lote aprovado antes de seguir.
- Manter atualizados os documentos de progresso por lote e este consolidado.
- Preservar recursos e correções aprovadas dos lotes anteriores.
- As chamas de Charmander, Charmeleon e Charizard continuam pendentes de avaliação visual e não devem ser alteradas.
- Graveler/Golem corrida, Pidgeotto/Pidgeot asas e braços do Charizard permanecem aprovados e protegidos.
