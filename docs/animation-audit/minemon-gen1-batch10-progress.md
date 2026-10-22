# Minemon Gen 1 — Lote 10 (#091–#100)

## Objetivo e escopo

Integrar exclusivamente os dez Pokémon #091–#100 do Cobblemon Fabric 1.8.2, preservando o estado aprovado dos Lotes 1–9, os 151 registros e as funcionalidades existentes. Não iniciar o Lote 11.

O planejamento original de 16 lotes confirma que o Lote 10 é #091–#100: Cloyster, Gastly, Haunter, Gengar, Onix, Drowzee, Hypno, Krabby, Kingler e Voltorb.

## Branch e commits

- Branch de trabalho: `feature/minemon-gen1-batch10-cobblemon`.
- Base: checkpoint documental mais recente do Lote 9, `eeb3054e6b461dbd713d28029a717f31fa098492` (pai do código aprovado).
- Commit de código aprovado do Lote 9: `637afd0a572e16f175a04b4614bd7369805530f0`.
- Actions aprovado do Lote 9: [39779248077](https://github.com/PetrickFaria/minemon/actions/runs/39779248077), artefato #12428477962.
- Commit publicado do Lote 10: pendente; a branch foi criada a partir do checkpoint correto.

## Fonte e auditoria

- Fonte Cobblemon Fabric 1.8.2 conferida contra o SHA-256 já auditado: `cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2`.
- O importador usa modelos, texturas, rigs e clips nativos da fonte; aplica somente as adaptações de namespace Molang já usadas nos lotes anteriores (`q.` → `query.`, `Math.` → `math.`) e a normalização necessária para clips ativos.
- Os verificadores estruturais confirmam dimensões de textura, hierarquia/pivôs, referências dos ossos e eventos nativos.

## Checkpoint 1 — preparação e auditoria

| # | Pokémon | Estado da fonte/adaptação |
|---:|---|---|
| 091 | Cloyster | Idle e caminhada nativos; preservados clips de concha, flutuação e nado. Sem corrida terrestre nativa. |
| 092 | Gastly | Idle e caminhada nativos; preservados corpo, língua, extensão e expressões. Sem corrida terrestre nativa. |
| 093 | Haunter | O poser usa o `ground_idle` também para caminhar; mantido modo flutuante e fallback explícito `idle`, sem gait inventado. Perfil de idle estático validado contra a pose nativa. |
| 094 | Gengar | `air_idle` e `air_fly` nativos; braços, pernas e corpo incluídos nos perfis corretos de voo. |
| 095 | Onix | Idle e caminhada nativos; os 14 segmentos, pivôs e clips de rotação dos segmentos preservados. Sem corrida terrestre nativa. |
| 096 | Drowzee | Idle, caminhada e corrida nativos; braços e pernas articulados. |
| 097 | Hypno | Idle, caminhada e corrida nativos; clipes preservam pêndulo, fios, peso e articulações de braços/pernas. |
| 098 | Krabby | Idle e caminhada nativos; patas e pinças articuladas; `snip_left`/`snip_right` preservados. Sem corrida terrestre nativa. |
| 099 | Kingler | Idle e caminhada nativos; patas, braços e pinças preservados. Sem corrida terrestre nativa. |
| 100 | Voltorb | Idle e caminhada nativos movimentam apenas o corpo esférico; não há membros ou corrida inventados. |

### Verificações executadas no checkpoint

- `tools/verify_batch10.py`: aprovado localmente.
- `tools/verify_batch9.py`: aprovado; recursos e definições do Lote 9 preservados.
- `tools/verify_batch8.py`: aprovado; recursos e definições do Lote 8 preservados.
- `tools/verify_pidge_wing_visibility.py`: aprovado.
- `tools/verify_project.py`: aprovado; 151 registros e referências a recursos presentes.
- `tools/verify_molang_compatibility.py`: aprovado para clips ativos; continua reportando queries de montaria somente em clips retidos não selecionados, sem suprimir ou reescrever esses casos.
- O teste Gradle direcionado ainda não executou: a distribuição Gradle 7.5.1 não está em cache local e o host recusou a conexão de rede. Não foram iniciados GameTests nem Minecraft.

## Pokémon implementados

Os dez recursos do Lote 10 foram importados na árvore de trabalho e passaram as verificações estruturais. Os primeiros 90 manifestos, perfis, modelos, texturas e animações foram comparados com a base `eeb3054` e permaneceram inalterados.

O documento de evidência da fonte é `docs/animation-audit/minemon-gen1-batch10-source-evidence.json`.

## Correções aprovadas preservadas

- Golem e Graveler: animações de corrida aprovadas.
- Pidgeotto e Pidgeot: asas extras removidas.
- Charizard: animação dos braços aprovada.
- Recursos e poses atuais das chamas de Charmander, Charmeleon e Charizard não foram alterados.

## Pendências visuais

- Charmander: chama da cauda melhorou, mas ainda requer refinamento.
- Charmeleon e Charizard: chamas da cauda ainda não estão visualmente satisfatórias.
- A avaliação visual manual dos dez Pokémon novos continua pendente. Nenhuma dessas pendências impede o gate técnico.

## Próxima etapa

Publicar o checkpoint de implementação no branch do Lote 10 e validar em GitHub Actions. Ordem: testes direcionados Molang/espécies/Lote 10; suíte Java completa; GameTests do servidor; um smoke completo do cliente; inspeção do JAR e upload do artefato. Se a suíte Java falhar, corrigir a causa antes de iniciar qualquer cliente.

## Estado atual

- Pokémon implementados na árvore de trabalho: #091–#100.
- Commit final publicado: não há ainda.
- Workflow do Lote 10: não iniciado.
- Validação automatizada total e JAR do Lote 10: pendentes.
