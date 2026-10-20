# Piloto Charmander — Cobblemon 1.8.2 (registro histórico)

> Este documento descreve o commit-piloto `8061ea9` e não o estado atual do Lote 1. A chama em malha sintética e o texto de “offset aprovado” abaixo foram substituídos pela adaptação baseada no pivô/UV do modelo fonte e pelo clip independente `tail_flame`. Consulte [`minemon-gen1-batch1-cobblemon-1.8.2.md`](animation-audit/minemon-gen1-batch1-cobblemon-1.8.2.md) para o estado atual.

## Escopo e preservação

Este piloto altera somente a fonte de recursos selecionada para Charmander (#004), a preparação GeckoLib necessária para esse modelo e os testes/diagnósticos do cliente. Os recursos anteriores de Charmander permanecem como fonte arquivada; os outros 150 registros, modelos, texturas, animações e comportamentos não são substituídos. A branch de referência e `feature/minemon-gen1-modid` permanecem preservadas. Nenhum lote começa neste commit.

## Fonte e compatibilidade

Os novos arquivos foram extraídos do JAR fornecido `Cobblemon-fabric-1.8.2+1.21.1.jar`:

| Recurso | Caminho selecionado | SHA-256 do arquivo-fonte |
|---|---|---|
| Geometria Bedrock | `src/main/resources/assets/minemon/geo/charmander_cobblemon_1_8_2/charmander.geo.json` | `157da3b38b3992916098fc8799ca7a65f91d1831a48b4db4c1c8efb549fc22db` |
| Textura base 64×64 | `src/main/resources/assets/minemon/textures/pokemon/charmander_cobblemon_1_8_2/charmander.png` | `4244b3fad50b08dab613296e54a1b6b80370371979534a2209f02407d3047a68` |
| Clips Bedrock | `src/main/resources/assets/minemon/animations/charmander_cobblemon_1_8_2/charmander.animation.json` | `d4eb2df1a6162710bfded77156074f7d541922409c18566726ec8f5d3d30a484` |

O resolver/poser original seleciona `animation.charmander.ground_idle` e `animation.charmander.ground_walk`. Ambos são loops e animam ossos existentes. A animação usa o alias Bedrock `q.anim_time` e chaves Catmull-Rom com `pre`/`post`; a etapa runtime converte `q.` para `query.` e escolhe o valor `post` de cada quadro (ou `pre` sem `post`) para a estrutura de vetor aceita por GeckoLib 3.1.40. O motor interpola linearmente entre os quadros originais: as tangentes Catmull-Rom do formato Bedrock não têm equivalente direto no GeckoLib 3. A compilação do clip e o movimento efetivo dos ossos são cobertos pelo teste Java e pelo smoke do cliente. O renderizador continua sendo o renderer Forge/GeckoLib existente; código Fabric do Cobblemon não é copiado.

O rig novo possui `tail3 → fire → fire2 → locator_fire`; o pivô de `fire` é `[0, 4.25, 16.5]`. O modelo original representa a chama com planos de largura zero, incompatíveis com a geometria que o Minemon já verificava. O gerador runtime substitui esses planos por duas faces texturizadas de volume finito, ancoradas no osso `fire`, sem mudar o pivô. O centro mantém o deslocamento local aprovado em relação ao pivô. Metadados Bedrock `locators`, que o desserializador Geo do GeckoLib 3 não consome, são retirados somente da cópia runtime; a fonte fica intacta. O atlas runtime acrescenta a chama animável da implementação Minemon; o PNG-fonte Cobblemon permanece intacto.

O JSON Cobblemon não fornece um clip terrestre `ground_run`. O perfil piloto não cria um clone acelerado de `ground_walk` com nome de corrida; o seletor permanece usando o clip `ground_walk` em velocidades mais altas. Corrida própria fica fora do escopo deste piloto.

## Validação

`verify_project.py` e `verify_runtime.py` continuam validando os recursos e os 151 registros. A verificação detalhada do cliente cobre idle inicial/final, caminhada em início/velocidade estável/desaceleração, caminhada articulada nos ossos `leg_left` e `leg_right`, transições de volta ao repouso, presença da textura e seleção dos clips. O smoke mantém as validações gerais de roster, carregamento GeckoLib, mundo e combate.

O fixture Fase 0 não teleporta a entidade e aplica velocidade no mesmo tick. O teste também sincroniza o rótulo de fase pelo servidor antes de amostrar o estado, e os diagnósticos registram fase, clip, velocidade e deslocamento. Assim, um tick atrasado ou uma velocidade que ultrapassa o limiar de corrida não pode ser rotulado como caminhada. As asserções de `WALK_DECEL` permanecem ativas; os limiares de articulação não foram reduzidos.

O workflow CI deve passar build, testes Java, GameTests do servidor, smoke do cliente e inspeção do JAR antes de disponibilizar o artefato. A confirmação da aparência no jogo real ainda depende da avaliação manual do usuário no Minecraft.
