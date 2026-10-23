# Minemon Gen 1 — Lote 13 (#121–#130)

## Estado atual

- Branch: `feature/minemon-gen1-batch13-cobblemon`.
- Commit-base: `64a0164bfd2a2dba28dc7b24a3e508f8fcb2b95e`, checkpoint documental do Lote 12 após aprovação.
- Código-base aprovado: `364fab1caf136d2e645f6ca77c4199570230eabd`.
- Lote 12: Actions #39865755919 aprovado; artefato JAR #12466881117.
- Checkpoint remoto da primeira metade (#121–#125): `a2d838460bd3c8aaafc823f1c94273f9bb29bd2a`.
- Primeira metade publicada: Starmie, Mr. Mime, Scyther, Jynx e Electabuzz.
- Segunda metade (#126–#130) integrada: Magmar, Pinsir, Tauros, Magikarp e Gyarados.
- Todos os dez recursos estão publicados; o commit final de código aprovado é `c91e12ac3067317f97898a16f5deec3bfa919277`.
- Actions #39878283990 terminou com sucesso; artefato JAR #12470668831 publicado. O ZIP contém `Minemon-1.19.2-2.0.0.jar` e expira em `2026-11-06T01:07:44Z`.
- Verificações estruturais cumulativas locais passaram. A validação completa será executada uma vez após o commit cumulativo.

## Fonte e decisões de adaptação

- Fonte auditada: Cobblemon Fabric 1.8.2; SHA-256 `cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2`.
- #121 Starmie: clips terrestres idle/walk/run e clips nativos aquáticos preservados.
- #122 Mr. Mime: forma original de Kanto; modelo/textura fonte `mr_mime`, slug de runtime `mrmime`. Sem variante Galar.
- #123 Scyther: modelo masculino Kanto; idle aéreo da fonte; sem clip nativo de corrida ou voo selecionado.
- #124 Jynx: forma Kanto; idle de origem sem walk/run nativos, mantido sem movimento sintetizado.
- #125 Electabuzz: clips nativos idle/walk; sem corrida nativa selecionada.
- Não foram encontrados usos dos aliases Molang `query.r.yaw_change` ou `query.r.velocity_y` nos clips selecionados do Lote 13.
- A auditoria por espécie e os eventos extraídos estão em `minemon-gen1-batch13-source-evidence.json`.

## Verificações deste checkpoint

- Importação segmentada #121–#125 e verificação de preservação: PASS; 146 entradas de manifesto fora do lote permaneceram inalteradas.
- `verify_project.py`: PASS.
- `verify_runtime.py`: PASS; 151 espécies em ordem da Pokédex.
- Conferência direcionada da presença dos três grupos de recursos (modelo, textura e animação) para os cinco Pokémon: PASS.
- `git diff --check`: PASS.
- Testes Java/build: PASS na Actions #39874670779.
- GameTests do servidor: PASS na Actions #39874670779.
- Smoke completo do cliente: PASS (151/151; locomoção, renderização/roster e verificações visuais de regressão passaram).
- Inspeção do JAR e upload do artefato: PASS na Actions #39878283990.

## Verificações cumulativas locais

- Importador Python e verificadores: compilação sintática PASS.
- `verify_project.py`, `verify_runtime.py`, verificadores Batch 8–13, `verify_pidge_wing_visibility.py`, preflight Molang e `git diff --check`: PASS.
- Primeiro commit remoto parcial usa `[skip ci]`; nenhum workflow foi iniciado para uma metade incompleta.
- Testes Java/build, GameTests e smoke do cliente: PASS no commit `f9ebd874fa3bf5a9d28d24b47ac924c5e2ec1279`, execução #39874670779.
- A inspeção do JAR identificou que o verificador exigia um clip `sit` em Starmie. Esse clip não está selecionado nem existe como ação nativa na fonte; não criar animação artificial para satisfazer essa checagem.
- Correção aplicada no commit `c91e12ac3067317f97898a16f5deec3bfa919277`: ações não selecionadas da fonte não são exigidas artificialmente para #101–#130; referências selecionadas continuam obrigatórias.

## Próxima etapa

1. Lote 13 concluído e aprovado; não iniciar o Lote 14 nesta tarefa.
2. Avaliação visual manual dos dez Pokémon continua pendente.

## Diagnóstico da primeira validação do Lote 13

- Actions #39874294212, commit `225944d5615137c96f75912bc0960735f38c9e19`: falhou na etapa 6 (`verify_project.py`), antes dos testes Java; GameTests, smoke e inspeção do JAR foram pulados.
- Erro: `UnicodeDecodeError` ao ler `assets/minemon/animations/gyarados_cobblemon_1_8_2/gyarados.animation.json`.
- Causa confirmada: o arquivo local correto tinha 486.334 bytes; o blob remoto tinha 450.060 bytes, por truncamento na transmissão da API. A comparação SHA dos demais arquivos alterados não encontrou divergências.
- Ação corretiva: restaurar exatamente o blob local validado do Gyarados, sem modificar animação, testes ou critérios; reenviar o arquivo em blocos e validar que o SHA do blob remoto corresponda ao arquivo local.
- Próximo passo: publicar a correção pontual e executar uma nova validação completa, única, do commit corrigido.

## Resultado da validação corrigida — Actions #39874670779

- Commit testado: `f9ebd874fa3bf5a9d28d24b47ac924c5e2ec1279`.
- Etapas 6–8 (recursos, Molang e regressões cumulativas): PASS.
- Testes focados, suíte Java/build e GameTests do servidor: PASS.
- Smoke do cliente: PASS; concluiu 151/151 espécies e marcou `PASS LOCOMOTION`, `PASS ROSTER`, `PASS CHARMANDER TAIL FLAME`, `PASS FIRE-STARTER ARMS`, `PASS PIDGE WINGS`, `PASS SOURCE LOOK` e perfis de articulação.
- Etapa 16 (inspeção executável do JAR): FAIL em `verify_runtime.py` com `ValueError: Missing prepared sit animation starmie`.
- Causa confirmada: o verificador tinha uma exceção para ações nativas ausentes nos Lotes 11–12 (#101–#120), mas não para #121–#130. A definição de Starmie seleciona idle, walk, corrida nativa e sleep, sem clip `sit`; não sintetizar poses de ação inexistentes.
- Consequência: etapa 17 de upload foi ignorada; não existe artefato JAR aprovado deste run. O artefato de diagnóstico `minemon-test-diagnostics` (#12468769237) expira em `2026-10-30T00:23:52Z` e o pacote auxiliar de testes do servidor (#12468101600) expira em `2026-10-29T23:55:16Z`.
- Correção aprovada: o verificador considera os clips de ação efetivamente selecionados; a validação local do runtime preparado passou para 151 espécies.

## Aprovação final do Lote 13

- Branch de código: `feature/minemon-gen1-batch13-cobblemon`.
- Commit de código aprovado: `c91e12ac3067317f97898a16f5deec3bfa919277`.
- GitHub Actions: #39878283990 — sucesso; suíte Java/build, GameTests, smoke cliente, inspeção do JAR e upload aprovados.
- Artefato: `Minemon-1.19.2-2.0.0-gen1`, ID `12470668831`, expira em `2026-11-06T01:07:44Z`.
- Arquivo dentro do ZIP: `Minemon-1.19.2-2.0.0.jar`; tamanho 6.283.155 bytes; SHA-256 `cfcb623b7b379b0bca188996f7bd8b46881dfbc3bccca4a5bd4d7cc78b324874`.
- ZIP do artefato: 3.587.321 bytes; SHA-256 `557437a26d42153b4a242cbe162832126c6021552e0222c807fd131ef4354781`.
- Inspeção direta do JAR: `pack.mcmeta`, `META-INF/mods.toml`, classe principal, catálogo e Pokédex presentes; 130 espécies têm modelos/texturas/animações atualizados; 151 registros em ordem; verificadores cumulativos Lotes 8–13 e asas de Pidgeotto/Pidgeot aprovados.
- Smoke cliente: 151/151, com `PASS LOCOMOTION`, `PASS ROSTER`, `PASS CHARMANDER TAIL FLAME`, `PASS FIRE-STARTER ARMS`, `PASS PIDGE WINGS`, `PASS SOURCE LOOK` e perfis de articulação.
- Golem e Graveler, asas de Pidgeotto/Pidgeot e braços de Charizard preservados. As chamas de Charmander, Charmeleon e Charizard seguem como pendência visual, sem alteração.
- Avaliação visual manual dos Pokémon #121–#130 permanece pendente e não foi confundida com aprovação automatizada.

## Correções visuais protegidas

- Graveler e Golem: corrida aprovada.
- Pidgeotto e Pidgeot: correção das asas aprovada.
- Charizard: movimento dos braços aprovado.
- As três correções permanecem sem alterações.

## Pendências visuais não bloqueantes

- Charmander: chama da cauda melhorou, mas ainda precisa de refinamento.
- Charmeleon: chama da cauda ainda insatisfatória.
- Charizard: chama da cauda ainda insatisfatória; braços aprovados.
- Não modificar nem declarar as chamas visualmente aprovadas neste lote.
- A avaliação visual manual dos dez Pokémon novos continua pendente e separada dos testes automatizados.
