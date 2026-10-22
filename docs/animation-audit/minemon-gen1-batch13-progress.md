# Minemon Gen 1 — Lote 13 (#121–#130)

## Estado atual

- Branch: `feature/minemon-gen1-batch13-cobblemon`.
- Commit-base: `64a0164bfd2a2dba28dc7b24a3e508f8fcb2b95e`, checkpoint documental do Lote 12 após aprovação.
- Código-base aprovado: `364fab1caf136d2e645f6ca77c4199570230eabd`.
- Lote 12: Actions #39865755919 aprovado; artefato JAR #12466881117.
- Checkpoint remoto da primeira metade (#121–#125): `a2d838460bd3c8aaafc823f1c94273f9bb29bd2a`.
- Primeira metade publicada: Starmie, Mr. Mime, Scyther, Jynx e Electabuzz.
- Segunda metade (#126–#130) integrada localmente: Magmar, Pinsir, Tauros, Magikarp e Gyarados.
- Todos os dez recursos estão integrados localmente; aguardam commit cumulativo e validação Java/Actions.
- Nenhum GameTest ou smoke do Lote 13 foi executado; nenhum JAR do Lote 13 foi gerado.
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
- Testes Java/build, GameTests e smoke completo: pendentes para o checkpoint cumulativo #121–#130.

## Verificações cumulativas locais

- Importador Python e verificadores: compilação sintática PASS.
- `verify_project.py`, `verify_runtime.py`, verificadores Batch 8–13, `verify_pidge_wing_visibility.py`, preflight Molang e `git diff --check`: PASS.
- Primeiro commit remoto parcial usa `[skip ci]`; nenhum workflow foi iniciado para uma metade incompleta.
- Testes Java/build, GameTests, smoke do cliente e inspeção do JAR permanecem pendentes para o commit cumulativo #121–#130.

## Próxima etapa

1. Revisar o diff cumulativo, confirmar que só #121–#130 e os verificadores correspondentes mudaram.
2. Publicar commit final de implementação incluindo teste Java, verificador Batch 13 e workflow.
3. Consultar essa única execução, corrigir apenas falhas comprovadas e publicar JAR após todos os gates passarem.

## Diagnóstico da primeira validação do Lote 13

- Actions #39874294212, commit `225944d5615137c96f75912bc0960735f38c9e19`: falhou na etapa 6 (`verify_project.py`), antes dos testes Java; GameTests, smoke e inspeção do JAR foram pulados.
- Erro: `UnicodeDecodeError` ao ler `assets/minemon/animations/gyarados_cobblemon_1_8_2/gyarados.animation.json`.
- Causa confirmada: o arquivo local correto tinha 486.334 bytes; o blob remoto tinha 450.060 bytes, por truncamento na transmissão da API. A comparação SHA dos demais arquivos alterados não encontrou divergências.
- Ação corretiva: restaurar exatamente o blob local validado do Gyarados, sem modificar animação, testes ou critérios; reenviar o arquivo em blocos e validar que o SHA do blob remoto corresponda ao arquivo local.
- Próximo passo: publicar a correção pontual e executar uma nova validação completa, única, do commit corrigido.

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
