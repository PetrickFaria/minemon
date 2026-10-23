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
