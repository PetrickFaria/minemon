# Lote 1 — Cobblemon 1.8.2 (#001–#010)

**Status:** implementação e CI em andamento. Nenhuma alteração do Lote 2.

## Escopo aplicado

O primeiro grupo segue exatamente o plano de lotes: Bulbasaur, Ivysaur, Venusaur, Charmander, Charmeleon, Charizard, Squirtle, Wartortle, Blastoise e Caterpie. Os 151 registros, seus ovos e a ordem da Pokédex permanecem. Os recursos Minemon anteriores foram mantidos em seus caminhos originais; o manifesto runtime seleciona cópias identificadas com `cobblemon_1_8_2` somente para estes dez.

Para Bulbasaur, Ivysaur e Venusaur o runtime usa o rig feminino do pacote 1.8.2, pois Minemon ainda não seleciona modelos por gênero. Para os demais, usa-se a geometria padrão do pacote. Os JSON originais, a textura base e a licença específica por modelo estão preservados sob `src/main/resources/assets/minemon/`.

| # | Espécie | Rig selecionado | Idle | Walk | Run terrestre selecionado |
|---:|---|---|---|---|---|
| 001 | Bulbasaur | feminino | `animation.bulbasaur.ground_idle` | `animation.bulbasaur.ground_walk` | não existe no pacote |
| 002 | Ivysaur | feminino | `animation.ivysaur.ground_idle` | `animation.ivysaur.ground_walk` | não existe no pacote |
| 003 | Venusaur | feminino | `animation.venusaur.ground_idle` | `animation.venusaur.ground_walk` | `animation.venusaur.ground_run` |
| 004 | Charmander | padrão | `animation.charmander.ground_idle` | `animation.charmander.ground_walk` | não existe no pacote |
| 005 | Charmeleon | padrão | `animation.charmeleon.ground_idle` | `animation.charmeleon.ground_walk` | não existe no pacote |
| 006 | Charizard | padrão | `animation.charizard.ground_idle` | `animation.charizard.ground_walk` | `animation.charizard.ground_run` |
| 007 | Squirtle | padrão | `animation.squirtle.ground_idle` | `animation.squirtle.ground_walk` | sem referência no poser Gen 1; não selecionado |
| 008 | Wartortle | padrão | `animation.wartortle.ground_idle` | `animation.wartortle.ground_walk` | sem referência no poser Gen 1; não selecionado |
| 009 | Blastoise | padrão | `animation.blastoise.ground_idle` | `animation.blastoise.ground_walk` | `animation.blastoise.ground_run` |
| 010 | Caterpie | padrão | `animation.caterpie.ground_idle` | `animation.caterpie.ground_walk` | não existe no pacote |

Clips de água, voo, sono, blink, recoil e ataque foram mapeados quando têm correspondência de estado no runtime atual. O voo/nado não é declarado como corrida terrestre. Poses auxiliares Minemon já existentes continuam com seus nomes/adaptações documentados; elas não substituem os clips obrigatórios de idle e walk.

## Chama de Charmander

No rig fonte, a cadeia é `tail3 → fire → fire2 → locator_fire`. O pivô da chama `fire2` é `[0, 4.25, 16.5]`; o plano fonte começa em `[0, 4.2, 14]`, tem altura 8 e profundidade 5. A cópia runtime agora deixa `fire` e `fire2` seguirem os canais originais de idle/walk, remove somente o plano degenerado e coloca duas faces texturizadas no novo osso filho `tail_flame`, pai de `fire2`. A nova geometria conserva os limites e UV da chama fonte e acompanha as transformações da ponta da cauda.

A imagem usada para a malha vem do frame `charmander_flame1.png` do Cobblemon, extraída da região UV original. Um controlador GeckoLib separado reproduz em loop `animation.charmander.minemon_tail_flame` no osso filho, com oscilação contínua de rotação e escala pequena. As frequências adotam a mesma base angular `90*16`/`90*14` usada nos canais de fogo de Charmander no recurso fonte. Molang `math.sin` recebe graus; frequências subgrau eram lentas demais e foram substituídas após o smoke medir pouca variação. O teste Java amostra o ciclo em vários pontos, e o teste cliente mede o movimento desse osso durante idle e walk e verifica o clip selecionado.

Charmeleon e Charizard também recebem malhas de chama compatíveis com GeckoLib, construídas a partir dos respectivos limites, UVs e bones fonte. Os clips originais de chama/cauda desses dois modelos não são zerados nem reescritos. A confirmação visual final da conexão, clipping e aparência ainda depende do teste manual do JAR no Minecraft.

O smoke de cliente também encontrou o clip terrestre de corrida de Charizard com keyframes de braço Z estáticos em ±11,5°, diferente dos vetores Molang usados no caminho de caminhada. A adaptação runtime desloca somente esse eixo para a pose interna de ±55° já usada para o movimento dos iniciais de fogo; mantém os valores X/Y e as variações Z relativas do clip de origem. Outro diagnóstico mostrou que o seletor de corrida podia manter `run` usando deslocamento residual mesmo depois de a velocidade cair; o seletor agora usa velocidade horizontal para escolher `run`, e o smoke exige amostras `RUN_DECEL` tanto acima quanto abaixo do limiar de velocidade.

## Compatibilidade e validação

- Os aliases `q.` são convertidos para `query.` apenas na cópia runtime; keyframes Bedrock `pre`/`post` são reduzidos ao valor de saída por quadro, e o GeckoLib 3 interpola os valores resultantes.
- Locators Bedrock não consumidos pelo GeckoLib 3 são removidos somente dos rigs runtime. Canais que apontam para bones ausentes na variante selecionada são removidos apenas quando explicitamente listados em `stripped_bones` no manifesto.
- Os testes gerais continuam verificando 151 entidades/ovos, ordem, carga das 151 geometrias/animações/texturas, renderização e movimento. Perfis de articulação detalhados cobrem os dez deste lote, com bones próprios por espécie, estados idle/walk e run quando selecionado.
- Os limites de velocidade/ticks existentes não foram ampliados. O cliente também valida os estados de transição já definidos pelo fixture.
- `verify_project.py`, compilação Java, testes Java e GameTests de servidor passaram. Os smoke runs confirmaram as correções de chama e pose de braço; a asserção restante expôs uma transição de corrida enviesada por deslocamento residual, além de não observar de forma consistente o sample rápido no início da desaceleração. A seleção de corrida e a validação de velocidade foram ajustadas com condições explícitas de velocidade; aguardam nova execução completa do GitHub Actions. O build Gradle local não iniciou porque o wrapper precisaria baixar Gradle 7.5.1 de `services.gradle.org`, inacessível nesta rede. O lote continua não aprovado até o smoke do cliente e a verificação do JAR passarem.

## Licenças e origem

Cada modelo/texture mantém a licença per-model que acompanha o JAR de origem; o JAR Minemon inclui também `META-INF/ASSET_LICENSES.md` com crédito a **Cobblemon Assets contributors** e a descrição das adaptações. O software Fabric do Cobblemon não foi copiado. Os recursos convertidos permanecem sujeitos à licença original CC BY-NC 3.0 referenciada no arquivo de atribuição.
