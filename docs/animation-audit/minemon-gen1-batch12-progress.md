# Minemon Gen 1 — Lote 12 (#111–#120)

## Estado

- Branch: `feature/minemon-gen1-batch12-cobblemon`.
- Commit-base: `0816533addee80e9e17a684c94bed399f4a9488f` (checkpoint documental do Lote 11 após aprovação).
- Commit aprovado de código do Lote 11: `c0cfdc43f80df32bac99a44b1375464c40087561`.
- GitHub Actions #39828753587: sucesso; artefato #12450059381, expiração 2026-11-05 18:57:57 UTC.
- Progresso confirmado antes de iniciar: #001–#110 integrados e validados; registros #001–#151 preservados.
- Nenhuma branch dos Lotes 12–16 existia na consulta inicial.

## Escopo exclusivo deste lote

| # | Espécie | Estado |
|---:|---|---|
| 111 | Rhyhorn | Recursos integrados; validação CI pendente |
| 112 | Rhydon | Recursos integrados; validação CI pendente |
| 113 | Chansey | Recursos integrados; validação CI pendente |
| 114 | Tangela | Bind-pose fallback documentado; validação CI pendente |
| 115 | Kangaskhan | Recursos integrados; validação CI pendente |
| 116 | Horsea | Recursos integrados; validação CI pendente |
| 117 | Seadra | Recursos integrados; validação CI pendente |
| 118 | Goldeen | Recursos integrados; validação CI pendente |
| 119 | Seaking | Recursos integrados; validação CI pendente |
| 120 | Staryu | Recursos integrados; validação CI pendente |

Fonte local auditada: Cobblemon Fabric 1.8.2, SHA-256 `cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2`. A auditoria preliminar encontrou clips nativos de locomoção, animações aquáticas e anatomia articulada; os clips ativos e a compatibilidade GeckoLib serão registrados após inspeção e importação por espécie. Não selecionar animação de corrida sintética nem inventar movimento ausente.

## Checkpoint B/C — integração e auditoria por espécie

- Integrados os modelos, texturas, licenças e animações das dez espécies a partir do arquivo de origem com o SHA acima. Modelos fêmea/macho mantêm a variante masculina Kanto quando a fonte não tem um modelo neutro; os clips compartilhados permanecem associados ao rig importado.
- Atualizadas somente as definições/perfis #111–#120; os dez arquivos de modelo, animação e textura #001–#110 foram comparados byte a byte com o commit-base e permanecem intactos.
- Corridas nativas selecionadas apenas para Rhyhorn, Rhydon, Chansey e Staryu. Kangaskhan, Horsea, Seadra, Goldeen e Seaking não receberam corrida inventada.
- Clips aquáticos nativos `water_idle`/`water_swim` foram preservados quando disponíveis para Horsea, Seadra, Goldeen, Seaking e Staryu; o controller atual usa os estados terrestres existentes e não foi expandido.
- Tangela não tem clips de idle/walk/run nem poser na fonte. Foi adicionado somente um canal neutro de bind pose para manter o modelo estável; fallback `static` explícito, sem movimento sintetizado.
- `tools/verify_batch12.py`, verificadores cumulativos dos Lotes 8–11, `verify_project.py`, `verify_runtime.py` e o preflight Molang passaram localmente.
- Os perfis de Goldeen e Seaking foram ajustados para joints presentes nos clips nativos selecionados após a primeira checagem estrutural apontar divergências.

## Testes pendentes e próximo passo

- A execução #39855369559 terminou em falha no GameTest `wildaggressionactuallyexpires`; 12 dos 13 GameTests passaram.
- Os testes focados, suíte Java e build passaram. O smoke completo do cliente também concluiu com sucesso (etapa 15/19).
- A inspeção do JAR e o upload foram pulados devido à falha do GameTest; nenhum JAR do Lote 12 foi publicado por essa execução.
- Diagnóstico #12462837662 (expira 2026-10-29 22:20:33 UTC); logs do servidor #12460040102 (expira 2026-10-29 21:15:41 UTC).
- Falha observada: `Expired aggression must not be reacquired by HurtByTargetGoal`. Corrigindo a invariável para limpar qualquer alvo residual de criatura selvagem quando o contador de agressividade chega a zero; o teste mantém a asserção e agora inclui estado da entidade na mensagem de erro.
- Correção em preparação sobre o checkpoint `20893c4684e99789461a9526ff424bae74cb316e`; ainda não validada por CI. Próximo passo: executar os testes e workflow uma única vez após publicar a correção. O Lote 13 permanece não iniciado.

## Diagnóstico do primeiro smoke

- Run #39841242841: preflight, testes Java, build e GameTests PASS; smoke processou os 151 registros e as janelas de renderização/movimento, mas falhou na verificação final de deslocamento de Tangela.
- Erro: `No rendered movement for tangela; displacement=true bodyChanged=false staticFallback=false`.
- Causa: perfil do manifesto classificava como `idle`; o smoke geral reconhece a pose sem gait somente quando `movement_fallback` é `static`.
- Correção local: o manifesto/importador agora classificam Tangela como `movement_fallback=static`; o fallback permanece uma pose neutra, sem animação de movimento nova.
- Verificadores estruturais e cumulativos reexecutados após a correção: PASS; build Java direcionado local não está disponível neste container porque a distribuição Gradle 7.5.1 não pôde ser baixada.
- Diagnósticos do run #39841242841: artifact #12456257638 (expira em 2026-10-29 20:30:07 UTC); logs de GameTests #12452159446 (expira em 2026-10-29 19:37:33 UTC). Esse run não publicou JAR porque falhou antes da inspeção.
- Commit da correção: `52abd5e93bf1f8da41d74702baf138380f991d09` (`Fix static Tangela smoke fallback classification`).
- Próximo passo: aguardar o Actions #39850003592; Java/build e GameTests passaram; smoke está em andamento.

## Segundo diagnóstico de smoke — Actions #39850003592

- Java/build e GameTests: PASS. O smoke do cliente falhou na verificação de articulação para Alakazam: `idle=0 walk=5 run=4 required=1`.
- Causa: o clip nativo `animation.alakazam.ground_idle` contém movimento real em pés, pernas, mãos, quadris e torso. O perfil de repouso observava apenas torso e bigodes; as variações medidas nesses joints ficaram abaixo do limiar do smoke, embora o clip seja animado.
- Correção publicada no commit `d20a00bdd60c05f5b04c89fbddf3f68f867f97c1`: perfil de repouso agora mede `foot_left`, `foot_right`, `left_lower_leg`, `right_lower_leg`, `hand_left` e `hand_right`, todos presentes e animados no clip-fonte. Nenhuma animação, modelo ou textura de Alakazam foi alterada; a exigência do smoke continua sendo pelo menos uma articulação em movimento.
- Os verificadores cumulativos dos Lotes 8–12 foram ajustados para permitir somente essa alteração pontual de perfil e validar o conjunto exato; recursos anteriores permanecem byte a byte preservados.
- Run #39850003592: smoke falhou deterministicamente no perfil de Alakazam; inspeção e JAR foram corretamente pulados. Diagnóstico artifact #12459171999.
- Verificadores direcionados Batch 12, cumulativos Batch 8–11, runtime e preflight Molang: PASS localmente após a correção. Build Java local indisponível por ausência de distribuição Gradle 7.5.1 no cache e rede de dependências bloqueada; CI é a validação oficial.
- Próxima etapa: publicar este ajuste pontual e executar uma única validação GitHub Actions sequencial. Não iniciar o Lote 13 até o smoke e a publicação do JAR passarem.

## Validações e publicações

- Último commit de código em validação: `d20a00bdd60c05f5b04c89fbddf3f68f867f97c1`.
- Actions #39855369559: testes focados e suíte Java/build PASS; smoke completo do cliente PASS; GameTests 12/13 PASS, com falha em `wildaggressionactuallyexpires`; não houve JAR.
- Actions #39841242841: Java/build e GameTests PASS; smoke falhou na classificação do fallback estático de Tangela.
- Actions #39850003592: Java/build e GameTests PASS; smoke falhou no perfil de repouso de Alakazam; não houve retry automático nem JAR.
- Artefato de JAR do Lote 12: ainda não gerado.
- Correção de alvo residual preparada localmente após o run; ainda pendente de publicação e validação. Inspecionar/publicar JAR somente após todos os GameTests e smokes passarem; não iniciar o Lote 13 antes disso.

## Correções visuais protegidas

- Graveler e Golem: corrida aprovada.
- Pidgeotto e Pidgeot: asas extras removidas e aparência aprovada.
- Charizard: movimento dos braços aprovado.
- Preservar todas essas correções.

## Pendências visuais não bloqueantes

- Charmander: chama da cauda melhorou, mas ainda precisa de refinamento.
- Charmeleon: chama da cauda ainda insatisfatória.
- Charizard: chama da cauda ainda insatisfatória; braços continuam protegidos.
- Não modificar essas três chamas neste trabalho.

## Aprovação visual manual

A avaliação visual manual dos novos Pokémon permanece pendente e separada das validações automatizadas.
