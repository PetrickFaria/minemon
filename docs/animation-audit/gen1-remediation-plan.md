# Plano de correção recomendado — Minemon Gen 1

**Este documento propõe etapas; não implementa mudanças.** Base de referência: `001657b541f4d79599c046e79fe91acb419e5681`. A branch funcional original não foi alterada.

## Diagnóstico transversal

1. O seletor de estado vive em `MinemonCreature.java:272-312`; walk/run usam movimento predominantemente horizontal. Não há ramo de estado aquático.
2. `MinemonFlyingCreature.java:10-13` usa navegação aérea e gravidade desligada em cada tick, sem transição aérea/terrestre nem pose inclinada derivada da velocidade.
3. A seleção do manifesto (`SpeciesAssetIndex.java:11`, `SpeciesDefinitions.java:14-15`) escolhe clips por chave; possuir clip no JSON não garante que o controlador o usa.
4. Teste atual do cliente verifica render/textura, deslocamento da entidade e, em determinados casos, alteração de algum osso. Não prova gait de membros. Só os três Pokémon da linha Charmander têm teste próprio de braço walk/run.
5. 47 espécies estão em fallback estático de caminhada, sete reutilizam idle; 144 ataques usam pose de fallback sintética; 28 têm swim clip, mas zero desses estados é selecionado pelo controlador atual.
6. O smoke comprovou 151 modelos/texturas em cena e comportamento básico do roster na execução #39316062513 tentativa 2. Gait visual, água, voo, efeitos e transições completos continuam não verificados.

## Fases propostas

| Fase | Problema e escopo | Arquivos/componentes prováveis | Espécies beneficiadas | Complexidade | Risco | Testes e critério objetivo |
|---|---|---|---|---|---|---|
| 0 — Instrumentar a validação | Teste não mede membros, asa, cauda ou articulação adequada. | `ClientSmokeTest.java`, fixtures/assets de teste, screenshots. | Todas; cobertura segmentada por anatomia. | Média | Falsos positivos se escolher osso errado; teste frágil por timing. | Para cada classe anatômica, medir transform dos membros/segmentos em idle→walk→run e reversão; distinguir deslocamento de corpo. Repetir teste para confirmar determinismo. |
| 1 — Walk/run terrestre | 47 fallback estático + 7 idle reutilizado; 11 candidatas sem canal detectável em apêndice; run é gerado de walk em 111 casos. Não aplicar ciclo quadrúpede a anatomias inadequadas. | `MinemonCreature.java`, `runtime_species.json`, preparação de assets, `ClientSmokeTest.java`. | Priorizar terrestres com pernas/braços: lista precisa derivar de geometria e animação; depois serpentiformes/segmentados e formas sem membros isoladamente. | Alta | Regressão de poses/velocidades ou interferência com blink. | Validar membro correto por espécie, walk/run ligado à velocidade e idle restaurado ao parar; nenhuma referência a osso ausente; screenshots para amostra e teste automatizado para cada modelo alterado. |
| 2 — Água e habitat | 28 têm clips de swim; nenhum ramo seleciona água e não há navegação aquática dedicada no fluxo analisado. Nem todo tipo Água deve ser preso à água. | `MinemonCreature.java`, goals/navigation, predicado de animação, definições por espécie, Client/GameTests. | Prioridade anatômica: Horsea, Seadra, Goldeen, Seaking, Magikarp; em seguida semiaquáticos e Dragonite separadamente. | Alta | Entidades encalhadas, duplicação de goals, conflito seguir dono/água. | Matriz de casos água rasa/profunda/terra, ordem do dono, alvo, recarga; espécies aquáticas permanecem/nadam e semiaquáticas transitam; animação water/surface correta. |
| 3 — Voo/flutuação | 22 flying usam voo/gravity off sempre; 2 não têm air_idle, 3 não têm air_fly, e 7 têm movimento estático. Predicado horizontal não garante acionamento em ascensão vertical. | `MinemonFlyingCreature.java`, `MinemonCreature.java`, controladores e propriedades de espécie. | Pidgey-line, Spearow-line, morcegos, Butterfree/Beedrill, flutuantes, aerodáctilo, lendários, Dragonite/Mewtwo/Mew — cada grupo com regra própria. | Alta | Movimento incessante, altura instável, pose/asas desalinhadas, regressões de pathfinding. | GameTest de estados no chão/ar/queda; direção e velocidade; asas mudam em voo e repouso; teste sem gravidade contínua; sem travar domesticação/sentar. |
| 4 — Efeitos visuais | Sem camada renderer de efeito por espécie; sinais de chama existem em algumas malhas, mas gás/eletricidade e Moltres são incertos. | `MinemonRenderer.java`, modelos/geo, texturas, possivelmente camada de render/partícula. | Gastly/Koffing/Weezing; Ponyta/Rapidash/Magmar/Moltres; elétrica; preservar Charmander-line sem ajuste nesta entrega. | Média-alta | Transparência, ordenação de render, custo de partículas, efeito separado do osso. | Screenshots multiângulo/estados; efeito vinculado ao osso; perfil com múltiplas entidades; sem spam de partículas nem alteração de arquivos fora do grupo. |
| 5 — Ataque, sono, piscada e individualização | 144/151 ataques são poses sintéticas; sleep é fonte em 78, blink em 115. Overlap de canais pode competir entre controllers; não foi comprovado visualmente. | `MinemonCreature.java`, assets por espécie, testes cliente. | Somente espécies onde evidência visual apontar falha, priorizando blink/movement overlap e clips sintetizados. | Média | Prioridade/conflicto de controllers e estados persistentes. | Confirmar ataque efetivo aciona pose uma vez; blink não sobrepõe locomoção; sentar/dormir/combate retornam a idle; persistência e smoke de 151 continuam aprovados. |

## Backlog técnico estático priorizado

### Prioridade A — Lacuna demonstrada pelo mapeamento
- 47 fallback estático: ausência de movimento selecionado no manifesto runtime.
- 7 fallback de idle: clipe dedicado de caminhada não selecionado.
- 28 grupos/species com swim clip sem seleção em água.
- 7 flying com fallback estático.
- Testes de membro faltam para o roster inteiro.

### Prioridade B — Requer teste visual antes de editar
- 11 walk clips sem canal reconhecido por heurística como membro/apêndice.
- Espécies com clips filtrados/canais removidos: conferir se o que sobrou atende a pose desejada.
- 144 ataques gerados: verificar se a pose é anatômica aceitável.
- Chama modelada de Rapidash: validar UV/visibilidade; não alterar Charmander line.
- Efeitos de Gastly, gases e lendários: revisar textura e captura antes de selecionar geometria/partículas.

### Ordem segura de execução
1. Autorizar instrumentação e aprovar uma matriz de espécies/ossos de teste.
2. Corrigir em grupos pequenos com commits que compilem.
3. Validar server + cliente e reter último JAR bom por fase.
4. Fazer fluxo água/voo como alterações de comportamento isoladas.
5. Implementar efeitos somente após confirmação em screenshots.
6. Fechar com teste de regressão de 151 ovos, renderizações e persistência.

Nenhuma fase é aprovada para execução por este relatório. A auditoria não alterou código, recursos, manifesto, ID ou comportamento.
