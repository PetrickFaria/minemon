# Minemon Gen 1 — Lote 13 (#121–#130)

## Estado atual

- Branch: `feature/minemon-gen1-batch13-cobblemon`.
- Commit-base: `64a0164bfd2a2dba28dc7b24a3e508f8fcb2b95e`, checkpoint documental do Lote 12 após aprovação.
- Código-base aprovado: `364fab1caf136d2e645f6ca77c4199570230eabd`.
- Lote 12: Actions #39865755919 aprovado; artefato JAR #12466881117.
- Checkpoint publicado agora: primeira metade (#121–#125), aguardando o SHA do commit.
- Pokémon integrados neste checkpoint: Starmie, Mr. Mime, Scyther, Jynx e Electabuzz.
- Pokémon restantes: Magmar, Pinsir, Tauros, Magikarp e Gyarados.
- Nenhum teste Java, GameTest ou smoke do Lote 13 foi executado ainda. Nenhum JAR do Lote 13 foi gerado.
- A validação completa será executada uma vez, após integrar a segunda metade.

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

## Próxima etapa

1. Integrar #126–#130: Magmar, Pinsir, Tauros, Magikarp e Gyarados.
2. Atualizar os verificadores cumulativos dos Lotes 8–12 e adicionar o teste de compatibilidade do Lote 13.
3. Executar os verificadores estruturais e Molang direcionados.
4. Publicar o commit completo e executar uma validação GitHub Actions; não iniciar workflows duplicados.

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
