# Minemon Gen 1 — Lote 9

## Objetivo

Integrar exclusivamente os recursos Cobblemon Fabric 1.8.2 dos Pokémon #081–#090, preservando os Lotes 1–8, os 151 registros e os recursos/funcionalidades existentes. A avaliação visual manual no Minecraft continuará pendente. Não iniciar o Lote 10.

## Branch e base

- Branch de trabalho: `feature/minemon-gen1-batch9-cobblemon`.
- Base mais recente do Lote 8: `85ed009ff141c7ecfa849969ae354b065596d8e6`.
- Commit de integração final do Lote 9 e workflow: `65f025f76e5db2f50142105c65bf46c11d392f34`.
- Commit corretivo da sintaxe Molang ativa do Weedle: `166da28cfa9acc1b9838bc650ec7581ab3a351db`.
- Correção pontual de compilação publicada na branch do Lote 8: `40458f0a81b3395ec7fc30400c65171fab606253` — substitui a referência incorreta `COBALT_QUERY` pela constante existente `COBBLEMON_QUERY` em `gametest/ActiveAnimationMolangPreflightTest.java`.
- O Lote 9 foi criado sobre o commit de correção acima, mantendo todos os commits e recursos prévios.

## Pokémon do lote

| # | Espécie | Estado |
|---:|---|---|
| 081 | Magnemite | Importado #081–#085; verificação estrutural direcionada aprovada |
| 082 | Magneton | Importado #081–#085; verificação estrutural direcionada aprovada |
| 083 | Farfetch'd | Importado #081–#085; verificação estrutural direcionada aprovada |
| 084 | Doduo | Importado #081–#085; verificação estrutural direcionada aprovada |
| 085 | Dodrio | Importado #081–#085; verificação estrutural direcionada aprovada |
| 086 | Seel | Importado #086–#090; idle de pose estática e fallback idle sem clip nativo de caminhada |
| 087 | Dewgong | Importado #086–#090; verificação estrutural direcionada aprovada |
| 088 | Grimer | Importado #086–#090; idle, caminhada e corrida nativos preservados |
| 089 | Muk | Importado #086–#090; idle de pose estática e fallback idle sem clip nativo de caminhada |
| 090 | Shellder | Importado #086–#090; verificação estrutural direcionada aprovada |

A correspondência está confirmada no planejamento original `docs/animation-audit/minemon-gen1-16-batch-plan.md`: Lote 9 = #081–#090.

## Checkpoint 1 — correção e preparação

- A execução #152 ([39692112208](https://github.com/PetrickFaria/minemon/actions/runs/39692112208)) terminou com falha em `:compileTestJava`, antes de GameTests, smoke do cliente, inspeção/publicação do JAR. Causa exata: identificador `COBALT_QUERY` não declarado; a classe declara `COBBLEMON_QUERY`.
- Correção publicada como commit independente em `feature/minemon-gen1-batch8-cobblemon`: `40458f0a81b3395ec7fc30400c65171fab606253`.
- A execução automática #153 ([39693679007](https://github.com/PetrickFaria/minemon/actions/runs/39693679007)) confirmou `compileTestJava` aprovado após o commit `40458f0`. O job de testes Java falhou em dois testes, antes de GameTests, smoke do cliente, JAR e artefato.
- Diagnósticos #153: `ActiveAnimationMolangPreflightTest` encontrou `-(...)` no idle ativo do Weedle; `SmokeSpeciesSelectionTest` esperava excluir Golem apesar de solicitar `#076`. #39696380089 confirmou que o teste de seleção passou e restou uma falha Molang. A primeira alteração de sintaxe havia atingido por engano o arquivo legado inativo; o log identificou o recurso correto `weedle_cobblemon_1_8_2`, agora ajustado com a reescrita equivalente `-1*(...)`. O arquivo legado foi restaurado.
- Artefatos de diagnóstico de #153 expiravam em 2026-10-28; os XML foram baixados e inspecionados. O JAR não foi gerado/publicado por esse run.
- A verificação prévia Molang informa 783 referências `query.r` em clips não selecionados, principalmente clipes montados/riding; nenhuma ocorre nos clips ativos selecionados. Elas foram reportadas e não suprimidas. Essa verificação antecipada passa.
- O Gradle local continua indisponível para compilação porque a distribuição 7.5.1 não está no cache e o host não consegue baixá-la. A compilação dos testes Java e os outros testes Java executaram em Actions; a validação completa ainda requer nova execução após a correção no arquivo ativo.
- Recursos #081–#085 importados do Cobblemon Fabric 1.8.2, cujo SHA-256 auditado é `cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2`. Adaptados `q.` → `query.` e `Math.` → `math.`. Verificador analisa rigs, dimensões da textura, hierarquias, referências de ossos e eventos.
- #081 Magnemite, #082 Magneton e #083 Farfetch'd preservam os clips aéreos nativos. #084 Doduo e #085 Dodrio preservam as duas/três cabeças, pescoços e pernas articuladas. Nenhum tem clip nativo de corrida terrestre.
- A branch de trabalho foi criada sobre `40458f0`; commit inicial da continuidade `2503dd085402211ec2e9c6287fca83ab4747d2d5`. Primeiro checkpoint remoto #081–#085: `5c6301642a5fc0a411375fb411ac6fff2c8a7a71` (`[skip ci] Complete batch 9 checkpoint #081-#085`).
- Verificações direcionadas locais aprovadas: `verify_project.py`, `verify_batch9.py --segment first`, `verify_batch8.py`, `verify_pidge_wing_visibility.py` e `verify_molang_compatibility.py`. A última informa compatibilidade de clips ativos e deixa explícitas as referências não selecionadas. Testes Java completos e client smoke permanecem pendentes.
- #086–#090 importados do mesmo arquivo de referência/hash. Adaptações de namespace `q.` → `query.` e `Math.` → `math.`; hierarquias, texturas, bones referenciados e eventos foram verificados. #086 Seel e #089 Muk não têm `ground_walk` na fonte e estão explicitamente marcados com fallback `idle`; seus clips idle originais são poses estáticas, então os perfis do smoke exigem estabilidade da pose em vez de movimento inventado. Não foi fabricada corrida. #087 Dewgong mantém animações aquáticas nativas, sem introdução de sistema de estados de água. Apenas #088 Grimer fornece `ground_run` nativo no lote.
- Checkpoints estruturais #086–#090 aprovados: `verify_batch9.py --segment second`, `verify_project.py`, `verify_batch8.py`, `verify_pidge_wing_visibility.py` e `verify_molang_compatibility.py`. A verificação completa dos dez passa localmente. `verify_project.py` confirma 151 registros da Pokédex, 1.025 definições de espécies e todos os recursos referenciados presentes. `py_compile` dos verificadores/importador e `git diff --check` também passam.
- Limitação Molang documentada: o preflight encontra 798 chamadas `query.r` exclusivamente em clips retidos mas não selecionados para a locomoção ativa (animações montadas, ações especiais e o nado montado do Dewgong). Os nomes de queries que GeckoLib 3.1.40 não reconhece continuam registrados/visíveis nos relatórios; não foram silenciosamente reescritos. O uso visual dessas animações fora do escopo ativo permanece não validado.
- Segundo checkpoint remoto #086–#090: `246d02a98c80271a23d83cf400275399cf73ad55` (`[skip ci] Complete batch 9 checkpoint #086-#090`). Esse commit contém os recursos completos do Lote 9 e os ajustes de registro/perfil de animação.
- Resultado estrutural cumulativo dos dez: `verify_batch9.py`, `verify_batch8.py`, `verify_project.py`, `verify_pidge_wing_visibility.py` e `verify_molang_compatibility.py` aprovados localmente. O preflight reporta 798 referências a funções `query.r` apenas em clips retidos não selecionados (incluindo animações montadas e nado montado de Dewgong); não suprime nem altera essas expressões. Nenhum clip ativo de repouso/caminhada/corrida as utiliza.
- Workflow `.github/workflows/build.yml` executa verificadores cumulativos dos Lotes 8 e 9 no código e no JAR, além da regressão das asas. A execução [Build Minemon #39696380089](https://github.com/PetrickFaria/minemon/actions/runs/39696380089), SHA `65f025f76e5db2f50142105c65bf46c11d392f34`, terminou com falha somente em `ActiveAnimationMolangPreflightTest`. `compileTestJava`, os demais testes Java, verificações de recursos, preflight antecipado, regressões dos Lotes 3–8, teste de seleção e teste de asas passaram.
- A causa foi a sintaxe `-(...)` no idle ativo de Weedle. Corrigida no arquivo importado selecionado pelo manifesto com a expressão equivalente `-1*(...)`; arquivo legado inativo restaurado. Verificadores locais cumulativos passaram após a correção.
- Nova execução [Build Minemon #39696750113](https://github.com/PetrickFaria/minemon/actions/runs/39696750113), SHA `166da28cfa9acc1b9838bc650ec7581ab3a351db`, iniciada por push. Na última consulta: job em andamento no checkout; compile/testes Java, GameTests, smoke integral, inspeção e publicação do JAR ainda pendentes. Não inicie outra execução enquanto esta estiver ativa.
- GameTests, smoke integral do cliente, inspeção do JAR e upload do artefato foram pulados no run #39696380089. Nenhum JAR aprovado foi publicado.

## Próximo passo

Acompanhar a execução #39696750113 já existente a partir do checkout. Se surgir outra falha determinística, usar os diagnósticos existentes e corrigir apenas sua causa. Antes de declarar aprovação, confirmar Java, GameTests, smoke integral do cliente, verificação cumulativa de 90, Pokédex de 151 e inspeção/publicação do JAR. Não iniciar Lote 10.

## Preservação e validação final

Preservar integralmente os 80 Pokémon anteriores, a chama do Charmander, os braços do Charizard, as asas de Pidgeotto/Pidgeot, os perfis Golem/Graveler, as otimizações de validação, os recursos não atualizados e os registros das 151 espécies.

A validação completa continuará pendente até compilação, testes Java, GameTests, smoke integral do cliente, regressões cumulativas, inspeção real do JAR e publicação do artefato. A avaliação visual manual dentro do Minecraft será documentada como pendência separada.


## Recuperação de validação — 2026-10-22

- Branch: `feature/minemon-gen1-batch9-cobblemon`; ponto inicial: `bae3dcb65b46b3ddbc105fbeb9e85e08b0c3da60`.
- Diagnóstico da execução `39696750113`: o preflight do GeckoLib falhou ao analisar a rotação Z da cauda nos clips `animation.pidgeotto.air_idle` e `animation.pidgeotto.air_fly`, arquivo `assets/minemon/animations/pidgeotto_cobblemon_1_8_2/pidgeotto.animation.json`. A expressão iniciava com unário `+`, que o parser Molang do GeckoLib 3.1.40 rejeita. A expressão equivalente sem o sinal inicial preserva o movimento nativo.
- Integrada a inicialização/reutilização de contexto Molang aprovada no Lote 8; a cobertura do preflight agora é cumulativa para #001–#090. O seletor recebe uma asserção isolada para `#076 -> golem`.
- Integradas as correções aprovadas de perfil do Geodude: walk/run medem `body` e `torso`; idle conserva os braços animados. O verificador e o gerador foram alinhados. Os testes existentes de Golem e Graveler continuam exigindo movimento real de `body`/presença de `body_roll`.
- O workflow executa os dois testes Java direcionados antes da suíte completa. Recursos de Pokémon #081–#090 não foram substituídos.
- Estado ao registrar este checkpoint: alterações preparadas para validação; testes Java, GameTests, smoke do cliente, inspeção de JAR e publicação ainda pendentes. Não há aprovação automática declarada nesta etapa.

- A primeira execução do Actions após o checkpoint, `39768867126`, confirmou o preflight Molang Python, mas interrompeu no verificador cumulativo: os validadores não previam a correção de sintaxe Molang estritamente equivalente do Pidgeotto. Atualização em andamento: permitir exclusivamente a remoção do unário `+` nas duas expressões conhecidas, e aceitar no perfil do Geodude somente walk/run `body` + `torso`; demais recursos e perfis anteriores continuam comparados integralmente. A compilação/testes Java não chegaram a iniciar nesta execução.

- Segunda execução, `39769162818`, passou o preflight Python mas o verificador estrito detectou normalização de `-0` causada pela serialização JSON feita ao preparar a primeira correção. Para evitar qualquer alteração acessória, o arquivo do Pidgeotto foi recomposto do conteúdo-base e recebeu somente a substituição textual das duas expressões. A falha foi do mecanismo de patch, não de animação; Java ainda não iniciou.

- Terceira execução, `39769345922`: a comparação estrita do conteúdo passou, mas a lista de caminhos permitidos dos dois verificadores ainda não incluía o arquivo do Pidgeotto. O ajuste desta etapa adiciona somente esse caminho, cuja diferença já é validada byte a byte contra a correção Molang esperada. Testes Java seguem pendentes.

- Quarta execução, `39769581001`, passou as validações cumulativas e o teste do seletor, mas o preflight Molang encontrou outra sintaxe inválida no clip ativo `animation.pidgeotto.air_fly`: as rotações X dos ossos `foot_left` e `foot_right` usavam `*+9.5`. A varredura das animações dos primeiros 90 confirmou que esses são os dois casos remanescentes do mesmo tipo. Removido apenas o sinal unário antes de `9.5`; os verificadores exigem essa correção exata e preservam os demais bytes.
