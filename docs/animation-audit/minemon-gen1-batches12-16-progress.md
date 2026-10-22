# Minemon Gen 1 — Progresso consolidado dos Lotes 12–16

## Último lote concluído

- Lote 11 (#101–#110), aprovado no Actions #39828753587, commit de código `c0cfdc43f80df32bac99a44b1375464c40087561`; checkpoint remoto posterior `0816533addee80e9e17a684c94bed399f4a9488f`.
- Artefato do Lote 11: #12450059381. Expira em 2026-11-05 18:57:57 UTC.

## Estado atual

- Lote ativo: 12 (#111–#120), branch `feature/minemon-gen1-batch12-cobblemon`.
- Base: `0816533addee80e9e17a684c94bed399f4a9488f`.
- Lotes 1–11/110 espécies: preservados e validados na base aprovada.
- Lote 12: recursos #111–#120 integrados; verificadores estruturais, suíte Java/build e GameTests passaram no Actions #39841242841. Smoke falhou somente na classificação da pose estática de Tangela; correção local `movement_fallback=static` já passou nos verificadores estruturais. Nova aprovação Java/Actions e JAR pendentes.
- Diagnósticos do primeiro run: artifact #12456257638 (expira 2026-10-29 20:30:07 UTC); logs do servidor #12452159446 (expira 2026-10-29 19:37:33 UTC). Nenhum JAR foi publicado para esse run.
- Lotes 13–16: ainda não iniciados.
- Próximo passo: validar/publicar o Lote 12; somente depois iniciar o Lote 13.

## Sequência autorizada

| Lote | Pokémon | Branch | Estado |
|---:|---:|---|---|
| 12 | #111–#120 | `feature/minemon-gen1-batch12-cobblemon` | Corrigindo classificação do fallback estático; nova validação pendente |
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
