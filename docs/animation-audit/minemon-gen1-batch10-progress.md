# Minemon Gen 1 — Lote 10 (#091–#100)

## Objetivo e escopo

Integrar exclusivamente os dez Pokémon #091–#100 do Cobblemon Fabric 1.8.2, preservando o estado aprovado dos Lotes 1–9, os 151 registros e as funcionalidades existentes. Não iniciar o Lote 11.

O planejamento original de 16 lotes confirma que o Lote 10 é #091–#100: Cloyster, Gastly, Haunter, Gengar, Onix, Drowzee, Hypno, Krabby, Kingler e Voltorb.

## Branch e commits

- Branch de trabalho: `feature/minemon-gen1-batch10-cobblemon`.
- Base: checkpoint documental mais recente do Lote 9, `eeb3054e6b461dbd713d28029a717f31fa098492` (pai do código aprovado).
- Commit de código aprovado do Lote 9: `637afd0a572e16f175a04b4614bd7369805530f0`.
- Actions aprovado do Lote 9: [39779248077](https://github.com/PetrickFaria/minemon/actions/runs/39779248077), artefato #12428477962.
- Checkpoint documental: `117c0b5d583bb25c789cf378822b1f0d79df1b61` (`[skip ci] Record batch 10 preparation checkpoint`).
- Commit de implementação publicado: `3efec0c410a759d78018b0533c4e52e4050db515` (`Implement Gen 1 batch 10 Cobblemon assets and validation`).
- Último commit remoto confirmado: `5237c1ee64f0ad14350f8bf897b107ce1b12e5cd` (`[skip ci] Record corrected batch 10 validation run`).
- Actions: [Build Minemon #39800529455](https://github.com/PetrickFaria/minemon/actions/runs/39800529455), SHA `3efec0c410a759d78018b0533c4e52e4050db515`; estado `queued` na última consulta. Não iniciar outro workflow.

## Fonte e auditoria

- Fonte Cobblemon Fabric 1.8.2 conferida contra o SHA-256 já auditado: `cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2`.
- O importador usa modelos, texturas, rigs e clips nativos da fonte; aplica somente as adaptações de namespace Molang já usadas nos lotes anteriores (`q.` → `query.`, `Math.` → `math.`) e a normalização necessária para clips ativos.
- Os verificadores estruturais confirmam dimensões de textura, hierarquia/pivôs, referências dos ossos e eventos nativos.

## Checkpoint 1 — preparação e auditoria

| # | Pokémon | Estado da fonte/adaptação |
|---:|---|---|
| 091 | Cloyster | Idle e caminhada nativos; preservados clips de concha, flutuação e nado. Sem corrida terrestre nativa. |
| 092 | Gastly | Idle e caminhada nativos; preservados corpo, língua, extensão e expressões. Sem corrida terrestre nativa. |
| 093 | Haunter | O poser usa o `ground_idle` também para caminhar; mantido modo flutuante e fallback explícito `idle`, sem gait inventado. Perfil de idle estático validado contra a pose nativa. |
| 094 | Gengar | `air_idle` e `air_fly` nativos; braços, pernas e corpo incluídos nos perfis corretos de voo. |
| 095 | Onix | Idle e caminhada nativos; os 14 segmentos, pivôs e clips de rotação dos segmentos preservados. Sem corrida terrestre nativa. |
| 096 | Drowzee | Idle, caminhada e corrida nativos; braços e pernas articulados. |
| 097 | Hypno | Idle, caminhada e corrida nativos; clipes preservam pêndulo, fios, peso e articulações de braços/pernas. |
| 098 | Krabby | Idle e caminhada nativos; patas e pinças articuladas; `snip_left`/`snip_right` preservados. Sem corrida terrestre nativa. |
| 099 | Kingler | Idle e caminhada nativos; patas, braços e pinças preservados. Sem corrida terrestre nativa. |
| 100 | Voltorb | Idle e caminhada nativos movimentam apenas o corpo esférico; não há membros ou corrida inventados. |

### Verificações executadas no checkpoint

- `tools/verify_batch10.py`: aprovado localmente.
- `tools/verify_batch9.py`: aprovado; recursos e definições do Lote 9 preservados.
- `tools/verify_batch8.py`: aprovado; recursos e definições do Lote 8 preservados.
- `tools/verify_pidge_wing_visibility.py`: aprovado.
- `tools/verify_project.py`: aprovado; 151 registros e referências a recursos presentes.
- `tools/verify_molang_compatibility.py`: aprovado para clips ativos; continua reportando queries de montaria somente em clips retidos não selecionados, sem suprimir ou reescrever esses casos.
- O teste Gradle direcionado não executou localmente: a distribuição Gradle 7.5.1 não está em cache e o host recusou a conexão de rede. A compilação/testes Java aguardam a execução Actions acima. Não foram iniciados GameTests nem Minecraft.
- Primeira execução `39800529455`: falhou em `verify_project.py` antes do Gradle. O log identificou `UnicodeDecodeError` ao ler `runtime_species.json`; os outros verificadores foram pulados.
- Causa confirmada no envio do commit: o blob do manifesto no GitHub tinha 300,060 bytes e conteúdo inválido, enquanto o arquivo local tem 314,741 bytes. O conector truncou o conteúdo base64 grande no transporte. O recurso local é JSON válido; não há falha no parser ou nos dados.
- Correção de transporte preparada: o blob foi recriado em chunks e seu SHA GitHub `c628e42506c7a46bce68e18593cc3b3fd846e2b1` corresponde exatamente ao SHA calculado sobre o arquivo local. A próxima execução usará esse conteúdo integral; ainda precisa validar.
- Correção publicada no commit `fdc57c5483e34320ac92b5351b7cd01aef70d204` (`Fix complete Batch 10 manifest publication`).
- [Build Minemon #39801348003](https://github.com/PetrickFaria/minemon/actions/runs/39801348003), SHA `fdc57c5483e34320ac92b5351b7cd01aef70d204`, terminou com falha nos testes Java direcionados em 2026-10-22. Verificações estruturais, Molang estático e regressões anteriores passaram antes disso.
- Artefatos de diagnóstico da execução inicial foram mantidos pelo GitHub até 2026-10-29 14:59 UTC: [logs de testes](https://github.com/PetrickFaria/minemon/actions/runs/39800529455/artifacts/12435801254) e [logs do servidor](https://github.com/PetrickFaria/minemon/actions/runs/39800529455/artifacts/12434573908). Não contêm JAR.

## Pokémon implementados

Os dez recursos do Lote 10 foram importados e publicados no commit `3efec0c`. Os primeiros 90 manifestos, perfis, modelos, texturas e animações foram comparados com a base `eeb3054` e permaneceram inalterados.

O documento de evidência da fonte é `docs/animation-audit/minemon-gen1-batch10-source-evidence.json`.

## Correções aprovadas preservadas

- Golem e Graveler: animações de corrida aprovadas.
- Pidgeotto e Pidgeot: asas extras removidas.
- Charizard: animação dos braços aprovada.
- Recursos e poses atuais das chamas de Charmander, Charmeleon e Charizard não foram alterados.

## Pendências visuais

- Charmander: chama da cauda melhorou, mas ainda requer refinamento.
- Charmeleon e Charizard: chamas da cauda ainda não estão visualmente satisfatórias.
- A avaliação visual manual dos dez Pokémon novos continua pendente. Nenhuma dessas pendências impede o gate técnico.

## Checkpoint após commit `d64abd231c5910d8c71312e1527f0a5a26e3e9ca`

- Commit de correção publicado: `d64abd231c5910d8c71312e1527f0a5a26e3e9ca` (`Fix Batch 10 active Drowzee Molang and manifest test parsing`).
- Nova execução única: [Build Minemon #39802299460](https://github.com/PetrickFaria/minemon/actions/runs/39802299460), SHA `d64abd231c5910d8c71312e1527f0a5a26e3e9ca`.
- Testes Java focados: aprovados (species selector, preflight Molang e compatibilidade Batch 10).
- `clean test build`: aprovado; portanto compilação e suíte Java completa passaram.
- GameTests do servidor: aprovados.
- Smoke completo do cliente: em andamento, passo 15 do job `119906024322`; não repetir nem iniciar outro workflow. Após sucesso, o mesmo workflow inspecionará e publicará o JAR. Estado deve ser retomado pela URL acima.
- Ainda sem artefato de JAR, aguardando smoke do cliente, inspeção do JAR e upload.

## Correção após a execução #39801348003

- Causa Molang identificada: no clip `animation.drowzee.ground_run`, osso `arm_left`, rotação no tempo 0.5, a expressão `-18.4733+ath.sin(query.anim_time*90*4+75)*20` continha o typo `ath.sin`. Corrigido para `math.sin`; o parser GeckoLib 3.1.40 rejeitou corretamente a expressão original.
- Os dois testes de compatibilidade do Batch 10 tentavam converter `runtime_species.json` em `JsonObject`, apesar de o manifesto ser um array. Foi adicionado leitor de `JsonArray`, mantendo intactas as asserções de ordem e conteúdo.
- Nenhum critério de teste foi removido ou afrouxado. As alterações corrigidas aguardam publicação e nova execução.
- A etapa Java focada interrompeu corretamente o workflow; não foram iniciados a suíte completa, GameTests nem smoke do cliente e nenhum JAR foi produzido.

## Próxima etapa

Acompanhar a execução #39802299460, atualmente no smoke do cliente. Se passar, verificar inspeção real e artefato JAR do mesmo commit; se falhar, usar logs/diagnósticos desse run e corrigir a causa sem repetir automaticamente o smoke completo.

## Estado atual

- Pokémon implementados e publicados: #091–#100.
- Último commit de código: `d64abd231c5910d8c71312e1527f0a5a26e3e9ca`; último commit remoto confirmado antes deste checkpoint documental: `5237c1ee64f0ad14350f8bf897b107ce1b12e5cd`.
- Workflow de validação em andamento: `39802299460`; Java, build e GameTests passaram; smoke do cliente continua ativo.
- JAR ainda não publicado; aguardar smoke, inspeção e upload nesta mesma execução.
