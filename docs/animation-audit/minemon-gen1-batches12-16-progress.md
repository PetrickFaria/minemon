# Minemon Gen 1 — Progresso consolidado dos Lotes 12–16

## Estado atual

- Último lote concluído e aprovado: Lote 13 (#121–#130), código aprovado `c91e12ac3067317f97898a16f5deec3bfa919277`; checkpoint documental `2b65cc53c61821111e2a2b9a5b9919066a698f74`.
- Actions #39878283990: sucesso; artefato JAR #12470668831, `Minemon-1.19.2-2.0.0.jar`, expira em 2026-11-06 01:07:44 UTC.
- Próximo lote ativo: Lote 14 (#131–#140), branch `feature/minemon-gen1-batch14-cobblemon`, criada sobre a base íntegra aprovada do Lote 13.
- Lotes 1–13 e 130 Pokémon preservados.
- Lote 14: preparação e auditoria inicial da fonte feitas; integração ainda pendente.
- Lotes 15–16: não iniciados.

| Lote | Pokédex | Branch | Estado |
|---:|---|---|---|
| 12 | #111–#120 | `feature/minemon-gen1-batch12-cobblemon` | aprovado; Actions #39865755919; artefato #12466881117 |
| 13 | #121–#130 | `feature/minemon-gen1-batch13-cobblemon` | aprovado; Actions #39878283990; artefato #12470668831 |
| 14 | #131–#140 | `feature/minemon-gen1-batch14-cobblemon` | checkpoint de preparação remoto #e2bc3a1; integração pendente |
| 15 | #141–#150 | `feature/minemon-gen1-batch15-cobblemon` | não iniciado |
| 16 | #151 | `feature/minemon-gen1-batch16-cobblemon` | não iniciado |

## Correções preservadas e pendências

- Graveler e Golem: corrida aprovada.
- Pidgeotto e Pidgeot: correção das asas aprovada.
- Charizard: braços aprovados.
- Chamas de Charmander, Charmeleon e Charizard: refinamento visual futuro; não alterar nos Lotes 14–16.
- Avaliação visual manual de Pokémon novos permanece distinta da aprovação automatizada.

## Regra de retomada

Retomar do commit remoto e da execução de Actions mais recentes; não iniciar lote seguinte sobre falha técnica bloqueante. Cada lote recebe seu documento próprio, checkpoints remotos, validação automatizada completa, inspeção do JAR e artefato antes do próximo.
