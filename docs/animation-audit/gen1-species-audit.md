# Auditoria técnica de animações — 151 espécies de Kanto

**Base:** `feature/minemon-gen1-modid` @ `001657b541f4d79599c046e79fe91acb419e5681`  
**Branch de auditoria:** `feature/minemon-gen1-animation-audit`  
**Alvo:** Minecraft 1.19.2 · Forge 43.5.0 · Java 17 · GeckoLib 3.1.40 · `minemon_gen1`  
**Escopo:** inspeção estática do código/manifests/recursos e resultados do CI; nenhuma implementação foi alterada.  
**Execução de evidência:** [GitHub Actions #39316062513, tentativa 2](https://github.com/PetrickFaria/minemon/actions/runs/39316062513).

## Resumo verificável

- 151/151 espécies no manifesto; 151 geometrias JSON válidas, 151 modelos e 151 texturas presentes. O ClientSmoke do CI registrou renderização das 151 entidades com textura.
- 150/151 espécies têm arquivo de animação fonte parseável. Rhyhorn (#111) não tem arquivo fonte; a preparação cria um clipe estático para evitar referência ausente.
- 97 espécies usam um clipe fonte selecionado como caminhada; 7 reutilizam idle; 47 usam fallback estático.
- O teste de cliente confirma mudança de pelo menos um osso em 104 espécies (97 + 7). Ele **não mede membros individualmente**. Portanto, 0/151 têm gait de pernas/braços/asas comprovado por este teste; 86 das 97 candidatas a walk têm canais em ossos reconhecíveis como apêndices, uma evidência estática, não visual.
- Das 151 corridas mapeadas, 18 usam clipe fonte e 111 são geradas de walk a 1,35×; 22 espécies no modo flying não selecionam corrida terrestre.
- 22 espécies usam modo flying; 20 têm `air_idle`, 19 têm `air_fly`; 7 estão em fallback estático.
- 28 espécies contêm clipes `water_swim` ou `surfacewater_swim`, mas a seleção de estado atual não os ativa e não há navegação aquática dedicada nesse fluxo.
- 144/151 ataques usam fallback sintético. 78 têm sleep fonte ativa e 115 blink fonte ativa.
- 23 espécies têm clipes fonte excluídos pela triagem por canais que apontavam para ossos ausentes; em 19 espécies foram removidos canais para ossos ausentes de clipes selecionados. São decisões estáticas de compatibilidade, não prova de crash.
- O CI não executou inspeção visual manual de gait, voo, natação ou efeitos. “Renderizado” e “osso mudou” não equivalem a animação anatômica correta.

## Implementação compartilhada examinada

| Área | Evidência no commit base | Implicação da auditoria |
|---|---|---|
| Controladores | `MinemonCreature.java:272-274` registra `movement` e `blink`; prioridade de estado em `:277-305`. | O controlador principal escolhe clip por estado (morte/ataque/sono/sentar/mover/idle); teste de osso não mede ciclo anatômico. |
| Detecção de movimento | `MinemonCreature.java:306-309` considera movimento do evento GeckoLib, velocidade horizontal e deslocamento horizontal. | Movimento puramente vertical não é critério explícito; não há ramo de `isInWater` para escolher swim/surface-water. |
| IA aérea | `MinemonCreature.java:109-115`; `MinemonFlyingCreature.java:10-13`. | As 22 definições flying recebem navegação/controle de voo, gravidade desativada continuamente e goal de voo; o código não alterna entre voo e repouso no chão. A adequação é espécie-específica e precisa de validação. |
| Modelo/recursos | `MinemonModel.java`, `SpeciesAssetIndex.java:11`, `SpeciesDefinitions.java:14-15`. | Modelo/textura/clipes são endereçados pelo manifesto; nomes de animações são escolhidos da definição. |
| Renderização | `MinemonRenderer.java`. | Renderizador comum sem camada genérica de partícula/efeitos especiais por espécie. |
| Empacotamento e preparação | `tools/audit_candidates.py`, `tools/prepare_runtime_assets.py`. | A preparação normaliza `q.` para `query.`, remove eventos malformados, filtra clipes/canais incompatíveis e cria alguns fallbacks. O teste prova que o pacote funciona no smoke test, não que todas as expressões tenham semântica GeckoLib correta. |

### Classificação usada

- **Selecionada + mudança de osso observada:** mapeamento de walk selecionado e o smoke do cliente viu algum osso mudar; movimento de membro não testado.
- **Fallback idle:** o controlador reutiliza pose/idle como walk; sem evidência de passada.
- **Fallback estático:** sem walk animado selecionado; movimento físico da entidade ainda pode ocorrer.
- **Disponível, não selecionada:** clipe consta no arquivo fonte, mas não está na seleção de estados do runtime.
- **Não verificada:** não houve teste específico de anatomia, transição ou aparência.
- **Excluída/filtrada:** triagem removeu o clipe ou canais com nome de osso não existente no modelo escolhido. Não classificado automaticamente como incompatibilidade de GeckoLib.

## Inventário individual

Nos campos “walk/run/air/water”, o nome abreviado é o sufixo do clipe. “sint.” significa gerado a partir do walk a 1,35×. “Mudança de osso” é apenas o critério amplo do smoke test; “gait pendente” significa que membros não foram instrumentados individualmente.

| Dex | Pokémon | Modo | Animação fonte | Idle | Walk selecionado | Run / origem | Air fly | Water swim | Filtros | Smoke | Avaliação walk |
|---:|---|---|---|---|---|---|---|---|---|---|---|
| #001 | Bulbasaur | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #002 | Ivysaur | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #003 | Venusaur | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #004 | Charmander | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #005 | Charmeleon | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #006 | Charizard | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | `air_fly` | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #007 | Squirtle | ground | sim | `ground_idle` | `ground_walk` | `ground_run` / fonte | — | `water_swim` | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #008 | Wartortle | ground | sim | `ground_idle` | `ground_walk` | `ground_run` / fonte | — | `water_swim` | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #009 | Blastoise | ground | sim | `ground_idle` | `ground_walk` | `ground_run` / fonte | — | `water_swim` | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #010 | Caterpie | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #011 | Metapod | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fallback: idle reaproveitado; não confirma passada |
| #012 | Butterfree | flying | sim | `air_idle` | `air_fly` | `minemon_run` / — | `air_fly` | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #013 | Weedle | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #014 | Kakuna | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fallback: idle reaproveitado; não confirma passada |
| #015 | Beedrill | flying | sim | `air_idle` | `air_fly` | `minemon_run` / — | `air_fly` | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #016 | Pidgey | flying | sim | `air_idle` | `air_fly` | `ground_run` / — | `air_fly` | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #017 | Pidgeotto | flying | sim | `air_idle` | `air_fly` | `minemon_run` / — | `air_fly` | — | 1 clipe(s) excluído(s) | sim | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #018 | Pidgeot | flying | sim | `air_idle` | `air_fly` | `minemon_run` / — | `air_fly` | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #019 | Rattata | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #020 | Raticate | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | 1 clipe(s) excluído(s) | sim | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #021 | Spearow | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #022 | Fearow | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | — | sim | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #023 | Ekans | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fallback: idle reaproveitado; não confirma passada |
| #024 | Arbok | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #025 | Pikachu | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #026 | Raichu | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #027 | Sandshrew | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | 1 clipe(s) excluído(s) | sim | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #028 | Sandslash | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | 1 clipe(s) excluído(s) | sim | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #029 | Nidoran-F | ground | sim | `idle` | `idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #030 | Nidorina | ground | sim | `idle` | `idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #031 | Nidoqueen | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #032 | Nidoran-M | ground | sim | `idle` | `idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #033 | Nidorino | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | — | sim | osso mudou; gait pendente | fallback: idle reaproveitado; não confirma passada |
| #034 | Nidoking | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #035 | Clefairy | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #036 | Clefable | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #037 | Vulpix | ground | sim | `blink` | `blink` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #038 | Ninetales | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #039 | Jigglypuff | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #040 | Wigglytuff | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | sim | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #041 | Zubat | flying | sim | `air_idle` | `air_fly` | `minemon_run` / — | `air_fly` | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #042 | Golbat | flying | sim | `air_idle` | `air_fly` | `minemon_run` / — | `air_fly` | — | 6 clipe(s) excluído(s) | sim | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #043 | Oddish | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #044 | Gloom | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #045 | Vileplume | ground | sim | `idle` | `ground_walk` | `minemon_run` / fonte | — | — | 1 clipe(s) excluído(s) | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #046 | Paras | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #047 | Parasect | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #048 | Venonat | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #049 | Venomoth | flying | sim | `air_idle` | `air_fly` | `minemon_run` / — | `air_fly` | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #050 | Diglett | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #051 | Dugtrio | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | 3 clipe(s) excluído(s) | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #052 | Meowth | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #053 | Persian | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #054 | Psyduck | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | `water_swim` | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #055 | Golduck | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | `water_swim` | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #056 | Mankey | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #057 | Primeape | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | 2 clipe(s) excluído(s) | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #058 | Growlithe | ground | sim | `idle` | `idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #059 | Arcanine | ground | sim | `idle` | `idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #060 | Poliwag | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | `water_swim` | 2 clipe(s) excluído(s) | sim | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #061 | Poliwhirl | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | `water_swim` | 1 clipe(s) excluído(s) | sim | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #062 | Poliwrath | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | `water_swim` | 2 clipe(s) excluído(s) | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #063 | Abra | ground | sim | `ground_idle` | `ground_walk` | `ground_run` / fonte | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #064 | Kadabra | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | 1 clipe(s) excluído(s) | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #065 | Alakazam | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #066 | Machop | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #067 | Machoke | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #068 | Machamp | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #069 | Bellsprout | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #070 | Weepinbell | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #071 | Victreebel | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #072 | Tentacool | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | `water_swim` | — | — | osso mudou; gait pendente | fallback: idle reaproveitado; não confirma passada |
| #073 | Tentacruel | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | `water_swim` | — | — | osso mudou; gait pendente | fallback: idle reaproveitado; não confirma passada |
| #074 | Geodude | ground | sim | `ground_idle` | `ground_walk` | `ground_run` / fonte | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #075 | Graveler | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #076 | Golem | ground | sim | `blink` | `blink` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #077 | Ponyta | ground | sim | `ground_idle` | `ground_walk` | `ground_run` / fonte | — | — | — | sim | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #078 | Rapidash | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / fonte | — | — | 2 clipe(s) excluído(s) | sim | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #079 | Slowpoke | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | `water_swim` | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #080 | Slowbro | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | 4 clipe(s) excluído(s) | sim | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #081 | Magnemite | flying | sim | `air_idle` | `air_fly` | `minemon_run` / — | `air_fly` | — | 1 clipe(s) excluído(s) | sim | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #082 | Magneton | flying | sim | `air_idle` | `air_fly` | `minemon_run` / — | `air_fly` | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #083 | Farfetch’d | flying | sim | `air_idle` | `air_fly` | `minemon_run` / — | `air_fly` | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #084 | Doduo | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #085 | Dodrio | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | sim | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #086 | Seel | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | `water_swim` | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #087 | Dewgong | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | `water_swim` | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #088 | Grimer | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #089 | Muk | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #090 | Shellder | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #091 | Cloyster | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #092 | Gastly | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #093 | Haunter | flying | sim | `ground_idle` | `ground_idle` | `minemon_run` / — | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #094 | Gengar | flying | sim | `air_idle` | `air_idle` | `minemon_run` / — | `air_fly` | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #095 | Onix | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #096 | Drowzee | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | 2 clipe(s) excluído(s) | sim | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #097 | Hypno | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #098 | Krabby | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #099 | Kingler | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | sim | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #100 | Voltorb | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #101 | Electrode | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #102 | Exeggcute | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #103 | Exeggutor | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | 1 clipe(s) excluído(s) | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #104 | Cubone | ground | sim | `idle` | `idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #105 | Marowak | ground | sim | `idle` | `idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #106 | Hitmonlee | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #107 | Hitmonchan | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #108 | Lickitung | ground | sim | `ground_idle` | `ground_walk` | `ground_run` / fonte | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #109 | Koffing | flying | sim | `air_idle` | `air_fly` | `minemon_run` / — | `air_fly` | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #110 | Weezing | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fallback: idle reaproveitado; não confirma passada |
| #111 | Rhyhorn | ground | não | `minemon_static` | `minemon_static` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #112 | Rhydon | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #113 | Chansey | ground | sim | `ground_idle` | `ground_walk` | `ground_run` / fonte | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #114 | Tangela | ground | sim | `minemon_static` | `minemon_static` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #115 | Kangaskhan | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | 2 clipe(s) excluído(s) | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #116 | Horsea | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | `water_swim` | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #117 | Seadra | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | `water_swim` | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #118 | Goldeen | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | `water_swim` | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #119 | Seaking | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | `water_swim` | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #120 | Staryu | ground | sim | `ground_idle` | `ground_walk` | `ground_run` / fonte | — | `water_swim` | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #121 | Starmie | ground | sim | `ground_idle` | `ground_walk` | `ground_run` / fonte | — | `water_swim` | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #122 | Mr. Mime | ground | sim | `animation.mr_mime.ground_idle` | `animation.mr_mime.ground_idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #123 | Scyther | flying | sim | `air_idle` | `air_idle` | `minemon_run` / — | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #124 | Jynx | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #125 | Electabuzz | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #126 | Magmar | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #127 | Pinsir | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #128 | Tauros | ground | sim | `ground_idle` | `ground_walk` | `ground_run` / fonte | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #129 | Magikarp | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | `water_swim` | 1 clipe(s) excluído(s) | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #130 | Gyarados | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | `water_swim` | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #131 | Lapras | ground | sim | `ground_idle` | `ground_idle` | `minemon_run` / sint. | — | `surfacewater_swim` | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #132 | Ditto | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #133 | Eevee | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #134 | Vaporeon | ground | sim | `ground_idle` | `ground_walk` | `ground_run` / fonte | — | `water_swim` | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #135 | Jolteon | ground | sim | `ground_idle` | `ground_walk` | `ground_run` / fonte | — | — | 1 clipe(s) excluído(s) | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #136 | Flareon | ground | sim | `ground_idle` | `ground_walk` | `ground_run` / fonte | — | — | 1 clipe(s) excluído(s) | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #137 | Porygon | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #138 | Omanyte | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | `water_swim` | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #139 | Omastar | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | `water_swim` | 1 clipe(s) excluído(s) | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #140 | Kabuto | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | sim | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #141 | Kabutops | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #142 | Aerodactyl | flying | sim | `air_idle` | `air_fly` | `minemon_run` / — | `air_fly` | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #143 | Snorlax | ground | sim | `ground_idle` | `ground_walk` | `minemon_run` / sint. | — | — | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #144 | Articuno | flying | sim | `air_idle` | `air_idle` | `minemon_run` / — | `air_fly` | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #145 | Zapdos | flying | sim | `air_idle` | `air_idle` | `minemon_run` / — | `air_fly` | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #146 | Moltres | flying | sim | `air_idle` | `air_idle` | `minemon_run` / — | `air_fly` | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |
| #147 | Dratini | ground | sim | `ground_idle` | `ground_walk` | `ground_run` / fonte | — | `water_swim` | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #148 | Dragonair | ground | sim | `ground_idle` | `ground_walk` | `ground_run` / fonte | — | `water_swim` | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #149 | Dragonite | flying | sim | `air_idle` | `air_fly` | `minemon_run` / — | `air_fly` | `water_swim` | — | — | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #150 | Mewtwo | flying | sim | `air_idle` | `air_fly` | `minemon_run` / — | `air_fly` | — | 1 clipe(s) excluído(s) | sim | osso mudou; gait pendente | fonte selecionada; transformação de osso vista no smoke test; ciclo dos membros não verificado |
| #151 | Mew | flying | sim | `ground_idle` | `ground_idle` | `minemon_run` / — | — | — | — | — | sem mudança exigida pelo teste/fallback | fallback estático; sem clipe de locomoção selecionado |

## Achados por grupo

### Terrestres e locomoção

- **Confirmado por configuração:** 129 espécies usam o modo ground e `RandomStrollGoal`; 22 usam classe/controlador de voo.
- **Confirmado pelo CI:** todas as entidades se deslocaram no cenário de teste; apenas o subconjunto de 104 teve verificação de mudança de osso exigida/aprovada.
- **Não confirmado:** sincronização entre velocidade real, ritmo da animação, membros individuais e transição walk↔run para as 151 espécies.
- Entre as 97 candidatas de walk, 11 não têm canais reconhecidos pelo inventário heurístico como membro/apêndice: Arbok (#024), Diglett (#050), Dugtrio (#051), Magnemite (#081), Magneton (#082), Gastly (#092), Onix (#095), Voltorb (#100), Electrode (#101), Exeggcute (#102), Koffing (#109). Algumas têm segmentos, corpo, ímãs ou partes não capturadas pela heurística; não se pode concluir que estejam imóveis sem visualização.

### Voo e flutuação

- A classe `MinemonFlyingCreature` aplica `FlyingMoveControl`, `FlyingPathNavigation` e `setNoGravity(true)` continuamente. Não há estado terrestre/aéreo nem inclinação do corpo derivada da velocidade no código examinado.
- Entre os 22 modos flying, há 21 clipes air idle, 20 air fly e sete fallbacks estáticos: Haunter, Gengar, Scyther, Articuno, Zapdos, Moltres e Mew.
- O predicado de movimento base exige componente horizontal/evento em movimento; o comportamento de voo vertical sem deslocamento horizontal pode não selecionar o clipe de voo. Isso é risco de lógica inferido do código, não caso visual reproduzido.
- Pidgey, Pidgeotto, Pidgeot, Spearow e Fearow não usam todos o modo flying na configuração atual; recomenda-se avaliar voo por espécie antes de mudar tipo de navegação.

### Água e natação

- Há clipes de swim em 28 espécies; todas as definições usam `ground`, exceto Dragonite, em `flying`. O controlador não seleciona `water_idle`, `water_swim` ou `surfacewater_*` como estados.
- A classe de voo também não tem um ramo aquático; Dragonite requer transição multiestado se natação for suportada.
- Não existe evidência de navegação aquática dedicada no conjunto de goals analisado. A configuração permite que a IA terrestre tente navegar para fora da água; a saída voluntária de cada espécie não foi reproduzida nesta auditoria.
- Prioridade de teste anatômico: Horsea (#116), Seadra (#117), Goldeen (#118), Seaking (#119), Magikarp (#129); também testar espécies semiaquáticas separadamente (Squirtle, evoluções, Psyduck, Poliwag e evoluções, Seel/Dewgong, Slowpoke, Staryu, Lapras, Vaporeon, Omanyte/Omastar, Dratini/Dragonair/Dragonite). Não bloquear genericamente todos os Pokémon aquáticos na água.

### Comportamento e efeitos

- Todos compartilham `MinemonCreature`; os resultados de servidor do workflow confirmam smoke de combate/domesticação base, não regras de habitat de 151 espécies.
- A camada de renderização é genérica e não há renderer/particle layer por espécie no código examinado. A ausência de nomes de osso de efeito é indício estático, não prova que pixels não estejam embutidos na textura.

Consulte `gen1-special-effects.md` para a leitura específica de chamas/gases/eletricidade e `gen1-remediation-plan.md` para prioridades. O JSON contém caminhos, ossos, canais, expressões, clipes ativos/excluídos, fallbacks e evidência de CI para cada entrada.

## Limites da auditoria

1. A análise de animação valida JSON e compara canais/ossos; não executa o motor GeckoLib isoladamente nem prova que cada expressão Molang seja interpretada como esperado.
2. O smoke do cliente renderiza as 151 espécies e verifica movimento/alguma transformação de osso conforme o teste existente; não captura ou classifica pose de cada membro.
3. Nenhum teste manual com GUI foi executado por esta auditoria. Voo, natação, transições, efeitos e percepção visual precisam de sessão controlada dentro do Minecraft.
4. “Causa provável” e prioridade nos relatórios auxiliares são hipóteses técnicas para testar, não alterações já aprovadas.

Nenhum arquivo executável ou recurso do mod foi alterado nesta auditoria.
