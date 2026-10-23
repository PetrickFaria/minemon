# Minemon Gen 1 — Lote 13 (#121–#130)

## Estado atual

- Branch: `feature/minemon-gen1-batch13-cobblemon`.
- Commit-base: `64a0164bfd2a2dba28dc7b24a3e508f8fcb2b95e`, checkpoint documental do Lote 12 após aprovação.
- Código-base aprovado: `364fab1caf136d2e645f6ca77c4199570230eabd`.
- Lote 12: Actions #39865755919 aprovado; artefato JAR #12466881117.
- Checkpoint remoto da primeira metade (#121–#125): `a2d838460bd3c8aaafc823f1c94273f9bb29bd2a`.
- Primeira metade publicada: Starmie, Mr. Mime, Scyther, Jynx e Electabuzz.
- Segunda metade (#126–#130) integrada localmente: Magmar, Pinsir, Tauros, Magikarp e Gyarados.
- Todos os dez recursos estão integrados e publicados no commit `f9ebd874fa3bf5a9d28d24b47ac924c5e2ec1279`.
- O commit anterior `225944d5615137c96f75912bc0960735f38c9e19` falhou na verificação estrutural devido a truncamento de transferência do arquivo de animação de Gyarados; o recurso foi reposto e seu blob remoto foi comparado com o SHA local.
- A execução atual é a Actions #39874670779, vinculada exatamente ao commit `f9ebd874fa3bf5a9d28d24b47ac924c5e2ec1279`.
- Verificações estruturais, preflight Molang, regressões dos Lotes 8–13, testes Java/build e GameTests do servidor passaram nessa execução.
- Smoke completo do cliente ainda está em andamento; inspeção e publicação do JAR aguardam a aprovação dessa etapa. Não há JAR do Lote 13 publicado ainda.
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
- Smoke completo do cliente: em andamento (etapa 15); não interpretar como aprovado até a conclusão.
- Inspeção e upload do JAR: pendentes (etapas 16–17).

## Verificações cumulativas locais

- Importador Python e verificadores: compilação sintática PASS.
- `verify_project.py`, `verify_runtime.py`, verificadores Batch 8–13, `verify_pidge_wing_visibility.py`, preflight Molang e `git diff --check`: PASS.
- Primeiro commit remoto parcial usa `[skip ci]`; nenhum workflow foi iniciado para uma metade incompleta.
- Testes Java/build e GameTests: PASS no commit cumulativo validado pela execução #39874670779.
- Smoke do cliente e inspeção do JAR ainda aguardam conclusão na mesma execução; não iniciar workflow duplicado.

## Próxima etapa

1. Acompanhar a etapa 15 da execução #39874670779, sem reiniciá-la.
2. Se o smoke passar, confirmar as etapas 16–17, inspecionar o artefato e registrar nome/SHA/expiração do JAR.
3. Se o smoke falhar, usar os logs e progresso preservados para corrigir somente a causa comprovada e executar as validações necessárias.
4. Registrar a aprovação ou falha final e o link do artefato neste documento.

## Diagnóstico da primeira validação do Lote 13

- Actions #39874294212, commit `225944d5615137c96f75912bc0960735f38c9e19`: falhou na etapa 6 (`verify_project.py`), antes dos testes Java; GameTests, smoke e inspeção do JAR foram pulados.
- Erro: `UnicodeDecodeError` ao ler `assets/minemon/animations/gyarados_cobblemon_1_8_2/gyarados.animation.json`.
- Causa confirmada: o arquivo local correto tinha 486.334 bytes; o blob remoto tinha 450.060 bytes, por truncamento na transmissão da API. A comparação SHA dos demais arquivos alterados não encontrou divergências.
- Ação corretiva: restaurar exatamente o blob local validado do Gyarados, sem modificar animação, testes ou critérios; reenviar o arquivo em blocos e validar que o SHA do blob remoto corresponda ao arquivo local.
- Próximo passo: publicar a correção pontual e executar uma nova validação completa, única, do commit corrigido.

## Checkpoint da validação corrigida

- Em `2026-10-23`, a Actions #39874670779 (`f9ebd874fa3bf5a9d28d24b47ac924c5e2ec1279`) passou as etapas 6–8 (recursos, Molang e regressões cumulativas), etapa 10 (testes direcionados), etapa 11 (Java/build) e etapa 12 (GameTests do servidor).
- A etapa 15 (smoke completo do cliente) está em execução. A consulta mais recente ainda não indicava saída final dessa etapa; não houve conclusão nem timeout reportado.
- As etapas 16 (inspeção real do JAR) e 17 (upload do JAR) ainda não começaram.
- O log de carregamento registra mensagens do GeckoLib 3.1.40 sobre `query.r.velocity_y`/`query.r.yaw_change` ausentes em expressões não selecionadas. O preflight dos clips ativos selecionados passou; manter isso registrado como diagnóstico e aguardar o smoke antes de classificar a consequência.
- Artefato disponível até agora: `minemon-server-tests`, ID `12468101600`, expira em `2026-10-29T23:55:16Z`. Não é o JAR final.

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
