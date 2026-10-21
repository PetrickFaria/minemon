# Minemon Gen 1 — Lote 7 e correção das asas de Pidgeotto/Pidgeot

## Base verificada

- Branch: `feature/minemon-gen1-batch7-cobblemon`
- Commit-base: `9153f58d9e6e4b279f6ca11249ca6f4a12016dfb`
- Actions do Lote 6: execução `39524383095`, concluída com sucesso; build/testes Java, GameTests do servidor, smoke do cliente, inspeção do JAR e upload passaram.
- JAR do Lote 6: `Minemon-1.19.2-2.0.0.jar`, dentro do artefato `Minemon-1.19.2-2.0.0-gen1`.
- Plano: Lote 7 é #061–#070: Poliwhirl, Poliwrath, Abra, Kadabra, Alakazam, Machop, Machoke, Machamp, Bellsprout e Weepinbell.

## Etapa A — asas

**Investigação concluída.** O modelo Cobblemon contém duas alternativas de rig: `wings_open` e `wings_closed`, cada uma com asas esquerda/direita e geometria própria. Os ossos não são cubos duplicados acidentais. O poser Cobblemon seleciona visibilidade por estado via `transformedParts[].isVisible`; essa visibilidade não é aplicada automaticamente pelo adaptador GeckoLib usado pelo Minemon, deixando ambos os rigs visíveis.

Referência inspecionada: Cobblemon Fabric 1.8.2, arquivo `Cobblemon-fabric-1.8.2+1.21.1.jar`, SHA-256 `cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2`. Posers originais em `assets/cobblemon/bedrock/pokemon/posers/0017_pidgeotto/pidgeotto.json` e `.../0018_pidgeot/pidgeot.json`.

**Correção aplicada:** clipes de voo/hover e solo/repouso mapeados pelo poser agora definem escala nos grupos pai para mostrar um único par por pose. Os cubos, ossos, textura e animações de movimento das asas permanecem intactos. A transição nativa de `ground_quirk` do Pidgeotto e a troca dinâmica de asas do `ride_dive` do Pidgeot foram preservadas. O verificador permite apenas mudanças nessas trilhas de escala nas animações anteriores; os demais recursos dos Lotes 1–6 são comparados byte a byte.

Verificação automatizada incluída:

- `tools/verify_pidge_wing_visibility.py`: integridade da hierarquia e cubos das duas alternativas, escala em clipes selecionados, transições dinâmicas, uso dos clipes ativos e inspeção opcional do JAR.
- Smoke de cliente exige que os grupos `wings_closed` e `wings_open` apresentem escalas 0/1 em amostras estabilizadas de Pidgeotto e Pidgeot. Isso valida transforms carregados/renderizados pelo GeckoLib, mas não substitui confirmação visual manual dentro do jogo.

## Etapa B — Lote 7

Ainda pendente nesta revisão. Importar apenas #061–#070 do pacote auditado Cobblemon 1.8.2; auditar modelos, texturas, clips, expressões Molang, poses procedurais, eventos e referências. Atualizar perfis e gates para validar 70 espécies, sem alterar o registro de 151.

## Etapa C — validação final

Ainda pendente: compilação, testes Java, GameTests do servidor, smoke do cliente, validação dos recursos/JAR, confirmação dos 151 registros e publicação do JAR final aprovado. Não iniciar o Lote 8.

## Estado desta etapa

- Testes de recursos locais: passaram para a correção das asas e preservação do Lote 6.
- Execução Gradle local: não disponível porque o wrapper precisa baixar Gradle 7.5.1 e esta sessão não tem conexão de saída para `services.gradle.org`; a validação de build seguirá no GitHub Actions após publicação.
- Próxima etapa: publicar commit específico da Etapa A e acompanhar sua execução do Actions antes de importar o Lote 7.
