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
- Smoke completo do cliente: terminou com falha no Gengar, e por isso inspeção/upload do JAR foram pulados. A execução não está aprovada e não publicou JAR.
- Relatório: menu e carga GeckoLib aprovados, ordem dos 151 spawn eggs aprovada, mundo e combate aprovados. Falha no perfil de articulação: `gengar idle=0 walk=0`, pois o smoke exigiu oscilação temporal nos clips.
- Diagnóstico dos recursos nativos Cobblemon: `animation.gengar.air_idle` e `animation.gengar.air_fly` são loops compostos por rotações constantes (arrays), não ciclos com movimento temporal. Contudo, entre os dois clips há mudanças reais em braços, corpo e pernas. A checagem atual mediu ranges dentro de cada fase settled e, corretamente, encontrou poses estáticas; ela não comparou as poses distintas entre os estados.
- Artefatos de diagnóstico: [#12438655006](https://github.com/PetrickFaria/minemon/actions/runs/39802299460/artifacts/12438655006), expira 2026-10-29 15:33 UTC. Não contém JAR.

## Correção após smoke de cliente #39802299460

- A falha do Gengar não representa ausência dos movimentos de origem: os clips selecionados são poses nativas fixas e diferem em cinco articulações do corpo, braços e pernas.
- Correção publicada em `891d5514af5a47f8ca531368647bc80a8c64761c`: o perfil lista as articulações de transição, exige estabilidade dentro de cada fase settled e mudança entre `IDLE_FINAL` e `WALK_STEADY`. O teste Java confirma que os dois clips continuam poses estáticas distintas; nenhum clip foi alterado ou inventado.
- Execução única para esse commit: [Build Minemon #39806866639](https://github.com/PetrickFaria/minemon/actions/runs/39806866639), em fila. O workflow executará primeiro Java completo e GameTests; só então iniciará um smoke completo do cliente.

## Correção após a execução #39801348003

- Causa Molang identificada: no clip `animation.drowzee.ground_run`, osso `arm_left`, rotação no tempo 0.5, a expressão `-18.4733+ath.sin(query.anim_time*90*4+75)*20` continha o typo `ath.sin`. Corrigido para `math.sin`; o parser GeckoLib 3.1.40 rejeitou corretamente a expressão original.
- Os dois testes de compatibilidade do Batch 10 tentavam converter `runtime_species.json` em `JsonObject`, apesar de o manifesto ser um array. Foi adicionado leitor de `JsonArray`, mantendo intactas as asserções de ordem e conteúdo.
- Nenhum critério de teste foi removido ou afrouxado. As alterações corrigidas aguardam publicação e nova execução.
- A etapa Java focada interrompeu corretamente o workflow; não foram iniciados a suíte completa, GameTests nem smoke do cliente e nenhum JAR foi produzido.

## Próxima etapa

Publicar a correção específica de validação das poses nativas do Gengar. Executar os testes Java direcionados e suíte completa, GameTests do servidor e então uma única nova validação completa do cliente. Se passar, confirmar inspeção real e artefato JAR; se falhar, usar os diagnósticos dessa execução, sem retry automático.

## Estado atual

- Pokémon implementados e publicados: #091–#100.
- Último commit de código: `d64abd231c5910d8c71312e1527f0a5a26e3e9ca`; último checkpoint documental remoto: `8dbb8125fbd1392223e55c7c4acab48b12169431`.
- Último workflow `39802299460`: falhou somente no perfil de articulação do smoke do cliente por tratamento inadequado das poses estáticas do Gengar. Java, build e GameTests passaram.
- JAR não foi gerado nem publicado porque o smoke falhou; inspeção e upload foram pulados.
- Commit de código atual: `891d5514af5a47f8ca531368647bc80a8c64761c`.
- Workflow atual: `39806866639`, em fila.
- Próximo gate: acompanhar Java, GameTests e uma única validação completa do cliente dessa execução; inspecionar o JAR somente se o smoke aprovar.
