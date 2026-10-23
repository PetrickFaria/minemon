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
