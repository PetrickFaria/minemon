# Plano proposto — 16 lotes de locomoção Gen 1

**Status:** Lote 1 (#001–#010) aprovado automaticamente em `2aa0c05`. Em 2026-10-20 o usuário autorizou os Lotes 2 e 3, sequencialmente, sem aguardar avaliação visual. Lotes 2 e 3 aprovados em `8bba5b0` (Actions 39469489652). Em 2026-10-20 o usuário autorizou exclusivamente o Lote 4 (#031–#040); Lote 4 aprovado em `f9cda38`, encerramento `e435ab9` (Actions 39475562845). O Lote 5 (#041–#050) foi aprovado automaticamente em `b43311c` (Actions 39505150691). O usuário autorizou agora exclusivamente o Lote 6 (#051–#060); a avaliação visual manual permanece pendente. O Lote 7 não está autorizado.

## Regras comuns dos lotes

- Cada JAR e cada revisão mantém os 151 Pokémon registrados, disponíveis e ordenados por número na Pokédex.
- Rodar testes gerais dos 151 em cada lote; acrescentar smoke detalhado de idle, walk, run, transições e articulações para os membros do lote atual.
- Concluir todas as correções aplicáveis de caminhada, corrida, comportamento e efeitos especiais dentro do lote antes de avançar. Separar voo e natação da corrida terrestre.
- Testar servidor e cliente; confirmar ossos de articulação e poses reais, não só presença de nomes de clips. Preservar o último JAR aprovado e descartar promoção de artefato se qualquer gate falhar.
- Manter o posicionamento aprovado das chamas de Charmander, Charmeleon e Charizard sem mudanças.
- Um lote só fecha com revisão dos relatórios e confirmação de regressões; falhas não justificam alongar janelas temporais sem evidência do mecanismo.

## Sequência

| Lote | Pokédex | Lote | Pokédex |
|---:|---|---:|---|
| 01 | #001–#010 | 09 | #081–#090 |
| 02 | #011–#020 | 10 | #091–#100 |
| 03 | #021–#030 | 11 | #101–#110 |
| 04 | #031–#040 | 12 | #111–#120 |
| 05 | #041–#050 | 13 | #121–#130 |
| 06 | #051–#060 | 14 | #131–#140 |
| 07 | #061–#070 | 15 | #141–#150 |
| 08 | #071–#080 | 16 | #151 |

## Lote 1 — prioridades #001–#010

1. **Roster/regressão:** conferir dez definições detalhadas e preservar a validação abrangente dos 151.
2. **Gait comprovada:** auditar cada clip, referências/condições do poser, ossos correspondentes e perfil de velocidade. Cobrir walk start, steady e decel; idle return; run start, steady e decel.
3. **Run próprio:** decidir por espécie em evidência visual e de ossos. Na base 1.8.2, #001–002, #004–005 e #010 não têm `ground_run`; #003, #006 e #009 têm run referenciado; #007–008 têm clip no JAR mas não poser Gen 1 nesse pacote. O estado final do Minemon deve ser testado contra seu próprio modelo/runtime.
4. **Espécies e comportamento:** verificar ritmo de Caterpie, quatro patas de starters e transições com velocidades de Minemon; manter modo aquático/voo como fluxo separado.
5. **Chamas congeladas:** qualquer ajuste dos três starters preserva posição/UV/offset das chamas já aprovadas.
6. **Gate:** compilação, testes Java, GameTests de servidor, smoke do cliente, clips verdadeiros e articulações; reter o último JAR aprovado. Efeitos especiais são incluídos quando aplicáveis antes de fechar o lote, sem ampliar ticks sem diagnóstico.

O usuário autorizou neste marco a correção da chama de Charmander seguida imediatamente pelo Lote 1, sem pausa para nova aprovação. O Lote 2 não começa neste marco.

## Autorização e validação dos Lotes 2 e 3

A avaliação visual dos três lotes permanece pendente por decisão do usuário. O Lote 2 deve passar todos os gates automatizados antes de iniciar o Lote 3. Cada importação mantém os registros dos 151 Pokémon, verifica preservação das outras 141 entradas, inclui o Lote 1 nos testes detalhados e preserva seu JAR aprovado. Novos recursos usam o SHA-256 já auditado do Cobblemon Fabric 1.8.2. Não se inventa caminhada para casulos nem se classifica voo como corrida terrestre. O prazo do processo de smoke foi ampliado para 25 minutos para acomodar os perfis cumulativos de até 30 espécies, sem alterar fases, velocidades, limites ou asserções das animações.
