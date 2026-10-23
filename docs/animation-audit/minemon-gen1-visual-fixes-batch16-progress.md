# Minemon — correções visuais e Lote 16

## Checkpoint — 2026-10-23

- Branch remota: `feature/minemon-gen1-visual-fixes-batch16`.
- Base técnica aprovada: Lote 15, código `276abc1428c8882a837e770a356f48a8d122bbab`, checkpoint documental `731dd5aefde687ca92aa6f5a80678f1417179f0d`.
- Lote 15: Actions #39963816100 passou; o ZIP de artefato #12510114037 continha `Minemon-1.19.2-2.0.0.jar`, expiração GitHub 2026-11-06 15:00:35 UTC.
- A branch `main` está 262 commits atrás do checkpoint aprovado; não contém avanços posteriores e não é base adequada. As branches antigas de pilotos visuais também não superam o checkpoint L15.
- Commit remoto a94d84f4466d0cb0652adc6add8b386d7de18ca4 já publicou o primeiro checkpoint (caudas e auditoria de fonte Mew). Actions #39980988503 ainda executa o smoke do cliente para esse commit; não foi iniciado outro workflow completo.

## Correções e integração em andamento

- Charmander, Charmeleon e Charizard declaram uma animação independente da chama da cauda e o smoke verifica os três. Os braços de Charizard não foram alterados.
- Auditoria de `ground_walk`/`ground_run` de Geodude, Graveler e Golem confirmou os perfis nativos existentes: Geodude tem walk e run distintos; Graveler e Golem usam corrida rolando por `body_roll`. Nenhuma mudança de movimento foi necessária.
- Ponyta e Rapidash possuem ossos de fogo com UVs apontando para mapas suplementares. Foram extraídos os quatro quadros normais oficiais de cada espécie do JAR Cobblemon Fabric 1.8.2 (SHA-256 da fonte: `cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2`). A integração local renderiza os overlays nos ossos originais, preservando seus transforms e UVs, em quatro quadros por uma segunda passagem de overlay no modelo inteiro, preservando alpha, transforms e UVs nativos. A auditoria do resolver nativo #39984162304 confirmou `fps: 10`, `loop: true` e `emissive: true`; a implementação acompanha esse ritmo e usa full-bright. Aprovação visual manual permanece pendente.
- Os oito PNGs de fogo estão em `assets/minemon/textures/pokemon/{ponyta,rapidash}_cobblemon_1_8_2/fire/normal/` e serão copiados ao namespace runtime por `processResources`.
- Mew (#151) agora usa modelo, textura e animação exatos do Cobblemon 1.8.2. Os clips nativos disponíveis são `ground_idle`, `ground_walk` e `blink`; o poser mapeia hover para idle e fly para walk. Não foram inventados clips aéreos, de corrida, sentar ou dormir. A adaptação de cabeça/corpo reproduz os inputs nativos `q.look` e `q.pitch_tilt`.

## Verificações locais

- `verify_runtime.py`, `verify_project.py`, verificadores cumulativos 8–16, verificador das asas de Pidgeotto/Pidgeot e `git diff --check` passaram após a integração de Mew e dos overlays.
- O ambiente local tem Java 17. O Gradle wrapper não iniciou porque a distribuição Gradle 7.5.1 não está disponível localmente e a rede de shell retorna `Network is unreachable`.
- As validações Java, GameTests e início do smoke de cliente foram aprovados na execução remota #39980988503 para commit a94d84f. O smoke ainda estava em andamento na última consulta; não há ainda conclusão/JAR para esse commit.
- Checkpoint remoto de código: `b7a2d697711009a1ace42f48543c9c74ade33981` (skip CI) contém Mew, overlays, renderer, smoke e documentação. Commit `60cb2a9de7aca6216af67fa142bdaa30b1acbbd0` apenas atualiza o workflow de auditoria para registrar resolver; Actions #39984162304 passou. Correções de emissividade, camada overlay de modelo inteiro, evidência e verificação de cadence estão agora publicadas até `3bd1295e465a525f21759db44390028dda94dfa9`; ainda aguardam build/smoke desse commit.

## Correções protegidas e pendências

- Preservar: braços do Charizard; asas corrigidas de Pidgeotto/Pidgeot; corrida de Graveler/Golem; voo do Moltres; correções do Mewtwo; tratamento opcional de Kabutops; recursos dos Lotes 1–15.
- A avaliação visual das chamas das caudas dos três iniciais e dos overlays de Ponyta/Rapidash permanece manual e não aprovada por este relatório.
- Pendências técnicas: obter o resultado do smoke de #39980988503; publicar as alterações atuais; executar a validação completa com o renderer de fogo; inspecionar o JAR resultante.

## Próxima ação

Aguardar/consultar #39980988503; registrar no próximo checkpoint a emissividade/10 fps confirmadas e a evidência; após a execução atual terminar, disparar um único build completo do código final, corrigir falhas comprovadas, inspecionar o JAR e só então declarar aprovação.
# Checkpoint — caudas de fogo e fumaça de Gastly — 2026-10-23

- Base remota conferida: branch `feature/minemon-gen1-visual-fixes-batch16`, HEAD `772bfc3cac1482e59b06be6516aa76025d143c39`; é um registro documental posterior ao código aprovado `48f3336c52fa2380a9372a18cf9d1015380e462a`. Actions #39991039004 aprovou esse código e publicou o JAR da Primeira Geração. Nenhum commit remoto posterior foi encontrado.
- Estado auditado: o conversor `prepare_tail_flame` usava a primeira face e copiava o mesmo sprite para as duas faces de cada chama. Os JSONs Cobblemon de Charmander, Charmeleon e Charizard possuem duas faces com UVs, pivôs/rotações e dimensões próprios. O preparo também remove os eventos de partícula Bedrock, e o runtime não tinha um adaptador para os dois eventos nativos `ghostly_smoke` de Gastly.
- Alterações locais nesta etapa: adaptar cada plano de chama separadamente com UV/pivô/rotação Cobblemon preservados; restaurar no cliente o efeito `ghostly_smoke` em idle/caminhada, usando os seis quadros originais da textura Cobblemon, tintura roxa, taxa de emissão, raio, vida útil e curva de tamanho da fonte; acrescentar checagens de recursos e um diagnóstico no smoke do cliente. Os braços do Charizard e os outros rigs não foram editados.
- Evidências: source JAR Cobblemon Fabric 1.8.2 SHA-256 `cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2`; textura original de fumaça SHA-256 `ae6b0f7aba6209c14202cbedfac44e9d56ccfac323f0e9af052a3a0c169a593b`. A página da Cobbledex abriu; o player YouTube da referência de Gastly permaneceu em tela preta nesta sessão, então a validação dessa correção se baseia nos recursos nativos do Cobblemon.
- Primeiro checkpoint remoto: commit `1922069aeafaf5ca71e0972a10b76899415eefa7` na branch `fix/minemon-tailflames-gastly-smoke`. Actions #40012679922 falhou no passo cumulativo do Lote 8 antes do build; os sete recursos novos do Gastly não estavam nas listas explícitas de arquivos autorizados dos verificadores anteriores.
- Correção desse gate: os validadores cumulativos dos Lotes 8–15 agora autorizam somente a definição da partícula e seus seis quadros originais. Não houve alteração dos critérios de validação de espécies, modelos, animações ou testes.
- Testes locais concluídos após a correção: verificadores cumulativos dos Lotes 8–16, asas Pidgeotto/Pidgeot e runtime 151/151 passaram; `py_compile` e `git diff --check` passaram. O preparo isolado dos três modelos confirmou duas faces independentes por chama, pixels das UVs nativas e pivôs/rotações preservados; os seis quadros de fumaça têm dimensões e alfa válidos.
- O Gradle local não iniciou porque o wrapper 7.5.1 não pôde ser baixado (`Network is unreachable`). Assim, Java, GameTests, smoke completo do cliente e inspeção do JAR não foram executados localmente; estão pendentes da próxima execução remota.
- Próxima etapa: publicar os ajustes do gate e este checkpoint sobre `1922069`; consultar a execução correspondente. Confirmar compilação, testes Java, GameTests, smoke de cliente (incluindo partículas Gastly e cobertura das caudas) e inspeção do JAR antes de declarar aprovação. Nenhum JAR novo foi gerado ou publicado até aqui.
- Preservações explícitas: roster de 151, Mew, braços do Charizard, asas Pidgeotto/Pidgeot, Graveler/Golem, Moltres, Mewtwo e ativos/funcionalidades existentes.


### Checkpoint — Forge API correction and validation #40013606910

- Compile failure in run #40013345746 was traced to using `RegisterParticleProvidersEvent.registerSpriteSet`, which is absent from Forge 43.5.0 / Minecraft 1.19.2. Changed only the registration call to the documented 1.19.2 overload `event.register(type, SpriteParticleRegistration)`. Code commit: `54b345b18ca3716926ea264d1619af998af6c866`.
- Validation run #40013606910: resource validation, Molang preflight, cumulative checks for Lotes 8–16 and Pidge wings passed; focused Java tests and full build passed; all 13 required server GameTests passed. Full client smoke was still running at checkpoint, so its result, JAR inspection, and artifact publication remain pending. Do not treat this run as approved until those steps complete.
- The preceding run #40013345746 stopped at Java compilation and did not run GameTests/client smoke or generate a JAR. No new JAR is approved yet.
- Next step: monitor the existing client smoke in run #40013606910 (job #120619927367). If it passes, verify runtime/JAR inspection and artifact metadata, then record the result here. If it fails, diagnose the specific client error before deciding on a focused correction.


### Checkpoint — corrigir o sprite ausente do Gastly

- A execução #40013606910 concluiu com sucesso no commit de código `54b345b18ca3716926ea264d1619af998af6c866`: testes Java direcionados, build, 13 GameTests, smoke de cliente, inspeção do JAR e publicação do artefato `Minemon-1.19.2-2.0.0-gen1` (artefato Actions #12532045749; expira em 6 de novembro de 2026).
- A captura enviada depois da publicação mostra quads de textura ausente pretos e magenta envolvendo Gastly. Os seis PNGs da fumaça nativa têm alpha e carregam corretamente como arquivos. A causa foi o identificador de sprite no JSON: `minemon_gen1:particle/gastly_ghostly_smoke_N`. No Forge 1.19.2 o caminho depois dos dois-pontos já é relativo a `textures/particle/`; portanto o jogo procurava incorretamente `textures/particle/particle/gastly_ghostly_smoke_N.png`.
- Correção em preparação: remover o segmento redundante `particle/` nos seis identificadores e ajustar `verify_runtime.py` para exigir o mapeamento correto. Nenhum arquivo de modelo, animação ou outro Pokémon será alterado. O JAR aprovado anterior permanece válido como base, mas contém o efeito Gastly com o caminho incorreto.
- Próxima etapa: executar verificadores cumulativos e o workflow completo no novo commit; conferir o registro de recursos e a captura de Gastly do smoke, inspecionar o JAR e publicar o artefato somente após a validação.


### Checkpoint — correção do caminho dos sprites de fumaça do Gastly — 2026-10-23

- Branch de correção: `fix/minemon-tailflames-gastly-smoke`; commit de código: `3264bd336efc0b3452c402bdeefbc6a91c4ac639` (`Fix Gastly smoke particle atlas paths`).
- Diagnóstico confirmado pela captura: os planos quadrados são o fallback de textura ausente. Os seis JSONs apontavam para `minemon_gen1:particle/gastly_ghostly_smoke_N`, fazendo o atlas procurar o segmento `particle/particle/`; os PNGs válidos estão no local esperado `textures/particle/gastly_ghostly_smoke_N.png`. Alpha e conteúdo dos seis quadros originais não foram modificados.
- Correção: removido o prefixo redundante `particle/` dos seis identificadores e atualizado `tools/verify_runtime.py` para validar os nomes corretos. Nenhum modelo, textura de Pokémon, animação, efeito de outro Pokémon ou funcionalidade foi alterado nesta correção.
- Actions #40025249120 testa exatamente o commit acima. Na última consulta, verificações de recursos, preflight Molang, validadores cumulativos dos Lotes 8–15 e Pidgeotto/Pidgeot, testes Java/build e GameTests (13/13) passaram. O smoke completo do cliente segue em andamento; inspeção do JAR, captura de tela do Gastly e publicação do JAR permanecem pendentes. Não declarar aprovação enquanto esses passos não terminarem.
- O artefato de logs de GameTests #12532985518 foi criado pela mesma execução; expira em 2026-10-30 21:16:32 UTC. Ainda não há artefato JAR publicado nesta execução.
- Próxima ação: consultar a execução #40025249120, sem iniciar workflow duplicado. Após sucesso, verificar a captura do Gastly em ângulos disponíveis, confirmar a inspeção do JAR e obter o artefato final. Se o smoke falhar, analisar a evidência antes de uma nova alteração.
