# Minemon Gen 1 — Lote 16 (#151 Mew)

## Estado

- Branch remota: `feature/minemon-gen1-visual-fixes-batch16`.
- Base: Lote 15 aprovado (`276abc1428c8882a837e770a356f48a8d122bbab`), checkpoint `731dd5aefde687ca92aa6f5a80678f1417179f0d`.
- Mew foi substituído pela geometria, textura e animações originais do Cobblemon Fabric 1.8.2. `verify_batch16.py` verifica hashes, hierarquia, clips nativos e registros da Pokédex.
- Clips integrados: `ground_idle`, `ground_walk`, `blink`; o poser do Cobblemon usa `ground_idle` para hover e `ground_walk` para fly. O perfil não presume caminhada/corrida terrestres, e não adiciona animações ausentes.
- A adaptação de `q.look(head, 1, 1, 20, -90, 60, -37)` e `q.pitch_tilt(body)` foi integrada ao modelo e ao smoke do cliente.
- As validações estruturais cumulativas 8–16 e de recursos do projeto passaram localmente. Gradle/Java, GameTests e smoke do cliente ainda dependem de Actions para o checkpoint atual.

## Ações remotas

- Commit: `a94d84f4466d0cb0652adc6add8b386d7de18ca4` — `Animate fire starter tails and checkpoint visual fixes / Mew source audit`.
- Build Actions #39980988503: `https://github.com/PetrickFaria/minemon/actions/runs/39980988503`; Java, build, GameTests e verificações iniciais passaram; smoke completo do cliente está/estava em execução, sem aprovação final confirmada.
- Source audit #39980988543 passou e forneceu o modelo, textura e animação nativos de Mew, além dos mapas de fogo de Ponyta/Rapidash.
- Os arquivos novos do Mew já estão publicados em a94d84f. Alterações de renderer e overlay ainda aguardam checkpoint e Actions.

## Próxima etapa

1. Confirmar resultado final da execução #39980988503.
2. Publicar renderização dos quatro overlays de Ponyta/Rapidash e validação do smoke.
3. Executar a validação completa do commit novo e inspecionar o JAR.
4. Atualizar o checkpoint final de Gen 1 com estado real do artefato.
