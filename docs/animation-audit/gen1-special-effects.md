# Auditoria estática de efeitos visuais — Kanto 151

**Base:** `001657b541f4d79599c046e79fe91acb419e5681`  
**Ambiente:** Minecraft 1.19.2 / Forge 43.5.0 / GeckoLib 3.1.40  
**Limite:** inspeção de nomes de ossos, cubos, canais de animação e estrutura do renderizador. Não houve captura visual manual nesta auditoria. A presença/ausência de osso nomeado não demonstra por si só se o efeito foi pintado na textura.

## Achados com geometria explícita

| Grupo / espécies | Evidência estática | Estado no runtime e confiança |
|---|---|---|
| Charmander #004, Charmeleon #005, Charizard #006 | Os três modelos têm osso `fire` com duas geometrias/cubos; animações podem hierarquicamente acompanhar a cauda. | Efeito geométrico presente no modelo. Deve ser preservado sem alterar posicionamento. A leitura visual deste relatório não mede alinhamento. |
| Ponyta #077 | 13 ossos com nomes `fire_mane*` ou `*_fire` (crina, cabeça e pernas); alguns são planos de espessura zero. Canais de idle/walk/run afetam parte do conjunto; parte de canais pode ter sido filtrada quando o osso não existe na geometria selecionada. | Sinal forte de chama modelada. Renderização no smoke não atesta a legibilidade do fogo nem o estado de cada segmento. |
| Rapidash #078 | 19 ossos relacionados a `fire_mane*` e fogo nas pernas; diversas partes da crina são planos. O inventário de conversão registrou coordenadas UV negativas em alguns planos de fogo no recurso fonte. | Geometria e animação de fogo existem; UV e aparência precisam de inspeção em cliente. Não foi declarado como erro visual comprovado. |
| Magmar #126 | Osso `tailfire` contém duas faces/quads de espessura zero; clip fonte `ground_idle` o anima. | Indício de chama geométrica presente. A aparência correta não foi avaliada por captura. |
| Moltres #146 | Não foi encontrado osso nomeado `fire` ou `flame` na geometria selecionada. Ossos das asas são planos e recebem clipes `air_idle`/`air_fly`. | Fogo pode estar incorporado na textura; evidência não permite afirmar ausência visual. Não há camada de partículas própria no renderer comum. |

## Gás, fumaça e aura

| Espécie | Evidência estática | Hipótese / próximo teste |
|---|---|---|
| Gastly #092 | Hierarquia tem raiz `gastly` sem cubos e corpo/torso representado por uma geometria; não há osso dedicado com nome de gás, fumaça ou aura. O walk selecionado anima corpo, sem canal de apêndice reconhecido. | A nuvem pode estar ausente da malha selecionada ou pintada na textura. Renderer não tem camada de gás por espécie. Capturar modelo parado e em movimento; se confirmada ausência, avaliar malha/transparência animada vinculada a osso ou camada dedicada. |
| Koffing #109 | Não há osso nomeado para gás/fumaça/aura; corpo tem movimento de fonte, mas sem osso de apêndice identificado. | Textura pode carregar o aspecto de gás. O modo flying dá navegação aérea, mas não há partícula de gás específica. Conferir textura e render no cliente. |
| Weezing #110 | Não há osso nomeado para gás/fumaça/aura; locomotion fallback usa idle reutilizado. | Efeito pode estar na textura; sem camada especial no renderer. Validar aparência e transparência em jogo. |

## Eletricidade, brilho e partículas

Pikachu #025, Raichu #026, Magnemite #081, Magneton #082, Voltorb #100, Electrode #101, Electabuzz #125 e Zapdos #145 não têm, nas geometrias examinadas, osso cujo nome declare `spark`, `electric`, `lightning`, `aura` ou `particle`. Magnemite/Magneton têm ossos de ímã e canais de movimento, mas isso não equivale a efeito elétrico.

O código do renderer não registra uma camada de efeito elétrico por espécie; as partículas existentes no sistema comum são feedback de interação (por exemplo, domesticação), não efeitos permanentes de ataque/eletricidade. O efeito pode ser baked na textura ou simplesmente não incluído. Confirmar visualmente antes de recomendar sistema novo.

## Outros sinais e cautelas

- A busca por palavras-chave encontrou somente os grupos com ossos listados acima. Isso é uma busca nominal, não detecção semântica de efeitos.
- Eventos de animação foram sanitizados na preparação; não há evidência de que partículas características sejam emitidas pelos JSONs usados.
- Não há base para concluir que toda espécie de fogo/elétrica/gás careça de efeito. Texturas não foram submetidas a classificação visual automatizada nem comparadas com screenshots in-game.
- A chama da linha Charmander é expressamente fora do escopo de correção desta etapa. Preservar modelos, UVs e orientação atuais.

## Critérios para eventual fase de implementação (ainda não autorizada)

1. Captura in-game com fundo claro/escuro e ângulos lateral, traseiro e frontal.
2. Verificar vínculo/hierarquia de chama com o osso da cauda durante idle, walk, run e combate.
3. Medir transparência, z-fighting, UVs e profundidade de planos.
4. Para gases, testar transparência e ordenação de render antes de adicionar partículas.
5. Confirmar custo por entidade e ausência de emissão excessiva para qualquer partícula persistente.
