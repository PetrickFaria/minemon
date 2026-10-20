# Lote 4 — progresso e retomada

Base aprovada: `8bba5b0563d2170a77c1546ea81e32e717569af1`, Actions [39469489652](https://github.com/PetrickFaria/minemon/actions/runs/39469489652).
Branch: `feature/minemon-gen1-batch4-cobblemon`.

Escopo exclusivo #031–#040: Nidoqueen, Nidoran♂, Nidorino, Nidoking, Clefairy, Clefable, Vulpix, Ninetales, Jigglypuff e Wigglytuff. Lote 5 não autorizado.

## Etapas

- [x] Confirmar base aprovada e ausência de commits posteriores na branch base.
- [x] Criar branch própria preservando os três lotes anteriores.
- [x] Auditar modelos, variantes, texturas, ossos, clips e posers Cobblemon 1.8.2.
- [x] Importar exclusivamente dez espécies, preservando outras 141 entradas.
- [ ] Compilar, testes Java, GameTests servidor, smoke cliente cumulativo e inspeção real do JAR.
- [ ] Publicar links de commit, execução aprovada e artefato.

## Recuperação

Consultar primeiro os commits desta branch e suas execuções Actions. Não reiniciar execuções em andamento. A auditoria e preparação de recursos têm somente `contents: read`; a publicação é feita pelo conector GitHub. Nenhum código do arquivo Cobblemon é executado, apenas recursos são lidos com SHA-256 fixado. As correções de Charmander e Charizard, os primeiros 30 recursos, registros/atributos/IA e mecânicas permanecem fora das alterações.

Avaliação visual manual pendente por decisão do usuário. Aprovação automatizada não substitui inspeção no Minecraft.

Auditoria fonte: Actions 39473590254, 39473802916 e 39473935826. Nidoqueen/Nidorino usam posers Kotlin compilados, inspecionados com javap sem execução. Ninetales/Jigglypuff/Wigglytuff usam posers JSON procedurais. Adaptação preserva a pose nativa e acrescenta ciclo simples nos membros declarados; relógio adaptado ao Minemon, sem alegar equivalência exata ao limb swing Cobblemon. Nidoqueen, Ninetales, Jigglypuff e Wigglytuff têm idle estático nativo: manter e validar estabilidade. Demais seis conservam idle animado. Nidoran♂, Nidoking e Vulpix têm ground_run nativo; demais reutilizam caminhada em alta velocidade, sem inventar clip de corrida.

Inconsistência nativa identificada: poser Jigglypuff aponta leg_left/right ausentes na geometria 1.8.2; ciclo bípede foi mapeado para foot_left/right existentes, mantendo offsets e canais Y/Z. Nidorino também tem canais de orelhas ausentes na geometria; somente a lista exata auditada é removida na preparação runtime, sem criar ossos.

Preparação validada: [Actions 39475067832](https://github.com/PetrickFaria/minemon/actions/runs/39475067832), recursos das 151 espécies e preservação das outras 141 entradas aprovados. Correção adicional estritamente local: cry de Jigglypuff tinha animation_length=1.25 dentro de sound_effects; a duração foi movida para o clip, preservando timestamp e efeito. Nenhum som original foi substituído.

O gate adicional compara manifest e perfis com a base aprovada, bloqueia sobrescrita de recursos existentes e inspeciona o JAR para todos os primeiros 40 modelos/texturas/animações. O smoke conserva os gates anteriores e verifica estabilidade das quatro poses de idle nativamente estáticas; não exige nem inventa oscilação para elas.

## Recursos por espécie

| Nº | Pokémon | Repouso | Caminhada | Corrida |
|---:|---|---|---|---|
| 31 | Nidoqueen | Pose nativa estática | Poser adaptado, pose nativa preservada | Caminhada em maior velocidade; sem clip próprio na fonte |
| 32 | Nidoran♂ | Clip nativo animado | Clip nativo | Clip nativo ground_run |
| 33 | Nidorino | Clip nativo animado | Poser adaptado, pose nativa preservada | Caminhada em maior velocidade; sem clip próprio na fonte |
| 34 | Nidoking | Clip nativo animado | Clip nativo | Clip nativo ground_run |
| 35 | Clefairy | Clip nativo animado | Clip nativo | Caminhada em maior velocidade; sem clip próprio na fonte |
| 36 | Clefable | Clip nativo animado | Clip nativo | Caminhada em maior velocidade; sem clip próprio na fonte |
| 37 | Vulpix | Clip nativo animado | Clip nativo | Clip nativo ground_run |
| 38 | Ninetales | Pose nativa estática | Poser adaptado, pose nativa preservada | Caminhada em maior velocidade; sem clip próprio na fonte |
| 39 | Jigglypuff | Pose nativa estática | Poser adaptado, pose nativa preservada | Caminhada em maior velocidade; sem clip próprio na fonte |
| 40 | Wigglytuff | Pose nativa estática | Poser adaptado, pose nativa preservada | Caminhada em maior velocidade; sem clip próprio na fonte |

## Pendências visuais

Verificar no Minecraft escala aparente, contato dos pés, recorte/culling, ritmo das caminhadas adaptadas, pose de Nidoqueen e caudas de Vulpix/Ninetales. Conferir também regressões visuais dos primeiros 30, especialmente chama de Charmander e braços de Charizard. Nenhuma avaliação visual manual é declarada.

As adaptações não reproduzem o relógio limb-swing completo do Cobblemon. Não há novo clip de corrida onde a fonte não o fornece. Não foram importadas formas Alola, shiny ou texturas alpha como novas funcionalidades.

## Inspeção estrutural

Dez geometrias: ossos únicos, pais presentes, ausência de ciclos, pivôs/rotações finitos e dimensões dos cubos não negativas. Texturas conferidas contra dimensões declaradas. Canais que apontam para ossos ausentes têm lista exata em minemon-gen1-batch4-source-evidence.json e são removidos somente da cópia runtime pelo adaptador existente.

## Logs herdados

A inicialização contém mensagens de parser Molang que também aparecem na execução base aprovada (por exemplo query.r.velocity_y/yaw_change em recursos legados). As expressões do Lote 4 usam somente q/query.anim_time e math.sin/cos/clamp. Nenhum teste foi desativado; recursos legados fora do lote foram preservados conforme o escopo.

## Mecânicas preservadas

Conferidos sem alteração nas dez espécies: mode, scale, width, height, health, attack, speed, cores dos ovos, sons, número e slug. Nenhum arquivo de entidades, IA, captura ou batalha foi modificado. Nidoking passa a usar o physical nativo de 3 segundos; attackTicks=60 sincroniza somente o controlador visual (handleEntityEvent/predicate de MinemonCreature), sem alterar dano, cooldown ou MeleeAttackGoal. Clefable conserva duração visual de 40 ticks; demais oito conservam 12 ticks.

## Validação em andamento

Commit de implementação: `f9cda38a0834ebfd7cde21a13286a5b752a5ae05`.
Execução: [39475562845](https://github.com/PetrickFaria/minemon/actions/runs/39475562845).
Em 2026-10-20 18:53 UTC: compilação completa, testes Java, preservação e 13 GameTests aprovados. Smoke cliente ainda em execução; inspeção do JAR e publicação do artefato pendentes. Não repetir ou reiniciar esta execução ao retomar. Este checkpoint altera somente documentação e não dispara outro build.
