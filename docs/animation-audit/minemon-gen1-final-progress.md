# Minemon — conclusão da primeira geração

## Estado de retomada — 2026-10-23

- Base aprovada: 150 Pokémon (Lotes 1–15); Lote 15 foi aprovado no Actions #39963816100 para commit `276abc1428c8882a837e770a356f48a8d122bbab`. O JAR do lote 15 foi o artefato #12510114037, com `Minemon-1.19.2-2.0.0.jar` e expiração informada em 2026-11-06 15:00:35 UTC.
- Branch atual: `feature/minemon-gen1-visual-fixes-batch16`. Código em checkpoints `b7a2d697711009a1ace42f48543c9c74ade33981`, `e7014710206fed0b712a86a85c87be89154a664a` e `3bd1295e465a525f21759db44390028dda94dfa9`; o commit remoto mais recente valida a cadência/emissividade nativas. Os commits recentes usaram `[skip ci]` enquanto a validação anterior seguia ativa.
- Mew (#151) e overlays de fogo de Ponyta/Rapidash estão integrados no checkpoint de código, mas não têm ainda build/smoke/JAR final aprovados.
- Audit #39984162304 passou e comprovou para Ponyta/Rapidash a textura `emissive`, 4 quadros, loop e 10 fps. Evidência: `minemon-gen1-batch16-visual-source-evidence.json`.
- Build #39980988503 corresponde ao commit antigo `a94d84f4466d0cb0652adc6add8b386d7de18ca4`; passou Java, build e GameTests, mas o smoke de cliente continua sem conclusão confirmada. Essa execução não aprova Mew nem o renderer adicionado depois.
- As validações estruturais locais cumulativas 8–16 passaram; Gradle local está bloqueado pela falta de rede para obter 7.5.1.

## Não marcar como concluído

A conclusão 151/151, smoke final, inspeção do JAR e publicação permanecem pendentes. As chamas continuam sem aprovação visual manual. Os braços Charizard, asas Pidgeotto/Pidgeot, corrida Graveler/Golem, voo Moltres, Mewtwo e os lotes anteriores permanecem protegidos.


## Checkpoint atualizado — 2026-10-23

- A execução #39980988503 foi concluída com sucesso para `a94d84f4466d0cb0652adc6add8b386d7de18ca4`, incluindo smoke do cliente e inspeção do JAR. Artefato #12514299352, nome `Minemon-1.19.2-2.0.0.jar`, expiração 2026-11-06 16:17:03 UTC. Como esse commit é anterior ao código de overlay/Mew, o artefato não é a versão final da Primeira Geração.
- A ponta de código validada estruturalmente está publicada na branch `feature/minemon-gen1-visual-fixes-batch16`, com Mew #151, overlays nativos de Ponyta/Rapidash e clips de cauda dos três iniciais. Verificadores cumulativos locais 8–16 e registros #001–#151 passaram.
- Commit de validação atual: `0811d6a487b0b89eb71c637d885e9974c85b99f8`. Actions #39985737492 está em andamento: Java/build passaram; GameTests estão em execução; smoke cliente/JAR aguardam. Nenhum artefato final desse commit disponível ainda.
- O workflow exige linha explícita de saída da articulação de Mew, além das verificações já existentes de cabeça/corpo e roster.
- Pendências de avaliação visual manual: chamas de Charmander, Charmeleon e Charizard; fogo de Ponyta/Rapidash; inspeção visual de Mew. Os testes automatizados não equivalem à aprovação visual manual.
- Próximo passo: acompanhar #39985737492; confirmar GameTests, smoke 151, inspeção e artifact. Corrigir somente eventual falha observada; não iniciar workflow duplicado.


## Retomada do smoke final de Mew — 2026-10-23

- Branch de código: `feature/minemon-gen1-visual-fixes-batch16`; correção em validação: `48f3336c52fa2380a9372a18cf9d1015380e462a`. Checkpoint remoto documental: `7006a050c1e6d2955930193cb2fae8c4a4c8efb7`.
- A validação #39985737492 falhou somente no smoke do cliente ao buscar um perfil RUN inexistente do Mew; os outros testes reportados passaram. A falha NPE foi corrigida com condição limitada à ausência comprovada de `ground_run` nativo; não se adicionou clip.
- Execução #39991039004 está em andamento para o SHA de código `48f3336c52fa2380a9372a18cf9d1015380e462a`. Testes Java direcionados, build, verificadores cumulativos e GameTests passaram. Smoke completo do cliente ainda ativo; inspeção e upload do JAR ainda aguardam essa etapa. Sem aprovação ou JAR final confirmado neste checkpoint.
- Próxima ação: consultar a mesma execução #39991039004; após sucesso, conferir inspeção e artefato. Se houver falha, registrar log e corrigir apenas a causa demonstrada.
- Preservar correções de Charizard/Pidgeotto/Pidgeot/Graveler/Golem/Moltres/Mewtwo e recursos dos Lotes 1–15. As chamas e o visual de Mew ainda não têm aprovação manual.

## Resultado final — Primeira Geração aprovada — 2026-10-23

- **Pokémon atualizados:** 151/151, incluindo Mew (#151); **registros preservados:** 151.
- Branch: `feature/minemon-gen1-visual-fixes-batch16`. HEAD documental antes deste registro: `243741275c7a258356e6767f232e97390b25ff91`. Commit de código aprovado e empacotado: `48f3336c52fa2380a9372a18cf9d1015380e462a`.
- GitHub Actions #39991039004: **success**, commit exato `48f3336c52fa2380a9372a18cf9d1015380e462a`. Passaram verificadores cumulativos/recursos, testes Java, build, GameTests do servidor, smoke completo do cliente/GeckoLib e inspeção do JAR.
- Download: [artefato Minemon-1.19.2-2.0.0-gen1](https://github.com/PetrickFaria/minemon/actions/runs/39991039004/artifacts/12521409667), contém `Minemon-1.19.2-2.0.0.jar`. Expira em `2026-11-06T18:07:57Z` (15:07:57 BRT). SHA-256 do ZIP: `65b84afc5728af01726769ecc5f7fd46981ae96b7c24c6fb0451cf46287bf015`; SHA-256 do JAR: `d5f84c85fb5a7f9b757b98575e877446efa821d90ba6272d3a3c142851cc74ad`.
- Conteúdo real inspecionado: `pack.mcmeta`, `META-INF/mods.toml`, classes, runtime species e recursos; 151 espécies, 151 geometrias, 151 animações e 159 texturas. Dependências declaradas: Minecraft 1.19.2, Forge 43.5.0 e GeckoLib 3.1.40.
- Correções técnicas preservadas: braços do Charizard, asas de Pidgeotto/Pidgeot, corridas nativas de Graveler/Golem, voo de Moltres, correções de Mewtwo e validação opcional de Kabutops.
- Evidência automatizada cobre movimento das chamas de cauda de Charmander/Charmeleon/Charizard e overlays de fogo de Ponyta/Rapidash, além de renderização/animação e articulação de Mew. Isso não substitui a revisão visual manual.
- **Pendências visuais manuais:** avaliar as chamas de Charmander, Charmeleon e Charizard; o fogo/overlays de Ponyta e Rapidash; e Mew. A avaliação manual não bloqueia a aprovação técnica do JAR.
- Próxima ação: nenhuma pendência técnica conhecida para concluir a Primeira Geração. Manter as revisões visuais manuais como follow-up separado.
