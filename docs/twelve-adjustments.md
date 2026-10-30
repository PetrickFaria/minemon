# Doze ajustes do Minemon

Base aprovada: `bd9756308b08cb8f01911d3ebecccafbb6b31341`, branch `feature/minemon-natural-behaviors`.
Compatibilidade alvo: Minecraft 1.19.2, Forge 43.5.0, Java 17.

1. **Sentar voadores:** reutiliza a busca de pouso seguro do descanso noturno. A ordem permanece ativa durante o trajeto e as tentativas têm intervalo e duração limitada. As espécies que repousam em flutuação não recebem pouso forçado.
2. **Sono aquático:** os perfis exclusivamente aquáticos mantêm a profundidade escolhida no início do sono; na superfície, o destino fica dois blocos abaixo. Gyarados e visitantes terrestres/aquáticos repousam no fundo seguro. Lapras mantém a posição de superfície. A navegação diurna permanece independente destas decisões.
3. **PokéC:** enquadramento calculado uma vez por espécie na tela, com margem esférica para todas as rotações; centro e escala ficam fixos. Mantém controles, limites do painel e arraste horizontal/vertical.
4. **Ovos:** ovos novos de acasalamento recebem 6.000 ticks. Cada atualização efetiva é salva e a eclosão ocorre no tick 6.000, quando há espaço seguro. Falta de espaço adia a eclosão. O tempo restante de ovos antigos é preservado.
5. **Acompanhamento:** inicia caminhada acima de cinco blocos, para em 4,5; inicia corrida acima de dez, retorna à caminhada abaixo de 9,5. Corrida passa de 1,55 para 1,9375 (25%); caminhada continua 0,8. Combate domesticado limita-se a cinco blocos. Creepers começam desmarcados, preservando escolhas explícitas existentes.
6. **BlackLists:** três filtros visuais disjuntos: Pacíficos, Neutros, Agressivos. Aranhas e aranhas das cavernas ficam em Neutros, assim como Endermen e outros mobs condicionais. A chave persistente de cada seleção continua sendo o ID do registro.
7. **Combate:** quatro golpes corpo a corpo bem-sucedidos, seguidos de projétil elemental. Intervalo mínimo de 50 ticks; falhas não avançam a sequência. Projétil usa o primeiro tipo, colisões nativas e dano básico atual, sem fogo, explosão, dano em área ou alteração de blocos. Protege treinador e companheiros do mesmo dono.
8. **Derrota:** a entidade domesticada original permanece viva para o mecanismo de persistência, com vida zero, sem executar a morte normal. O estado sincronizado/NBT impede IA, combate, XP, reprodução, empurrões e regeneração. Usa postura de sono e fumaça discreta. Petiscos compatíveis do treinador utilizam `TREAT_HEAL_AMOUNT` existente; somente vida cheia libera o estado. Nível, XP, dono e UUID são conservados. Evolução fica indisponível durante a derrota.
9. **Pokébolas:** a liberação aceita volumes de água com espaço para a hitbox; mantém colisões sólidas, rejeição de perigos, NBT e controle de token contra duplicação. O apontamento pode atingir o fundo submerso ou a água.
10. **Petiscos:** 18 receitas de bancada, cada uma com osso central e quatro almas do mesmo tipo em cruz, produzindo uma unidade. Sprites e obtenção das almas permanecem intactos.
11. **Geodude:** banda fixa de um bloco, considerando o fundo visível do modelo, sem animação de rolamento enquanto flutua. Demais espécies mantêm suas alturas.
12. **Gastly:** fase baixa de 6.000 ticks a um bloco e fase variável de 3.600 ticks entre dois e cinco blocos. Estado/timer persistem e pausam durante estados incompatíveis com movimento. Fumaça em todos os estados, 10 partículas/s (antes 20), tamanho 2,5 vezes o anterior, 75% de opacidade e raio reduzido pela metade. A velocidade interna é controlada para manter a fumaça junto ao corpo, preservando a duração de 16 ticks.

## Preservação e validação

O catálogo de raridades e `PokemonProgression.java` não foram alterados. Permanecem 13 comuns, 130 incomuns, três raros, três lendários e dois míticos. Modelos, texturas dos Pokémon, sons e animações-fonte foram preservados.

As verificações automatizadas incluem as suítes existentes, persistência da derrota/recuperação e do ciclo do Gastly, sequência de ataques, dano/colisão do projétil, receitas reais, eclosão e recarga do ovo, sono por categoria, pouso comandado e liberação submersa. O teste de cliente das BlackLists clica nas três abas, verifica a partição completa, rolagem, seleções e padrão de Creepers.

Os resultados devem ser consultados na execução final do GitHub Actions. Não considere uma execução pendente ou com falha como aprovação.

Revisão manual ainda necessária: aparência do pouso para todas as anatomias, alinhamento visual em ângulos extremos do PokéC, aparência/colorização do projétil, postura dos derrotados e densidade/transparência da aura do Gastly. Os testes automáticos não substituem essa avaliação estética.
