# Minemon Gen 1 — Lote 11 (#101–#110)

## Escopo

Implementar exclusivamente as espécies Kanto #101–#110 com modelos, texturas, rigs e animações do Cobblemon Fabric 1.8.2 adaptados ao Minemon/GeckoLib 3.1.40. Preservar o histórico aprovado dos Lotes 1–10, os 151 registros e as funcionalidades existentes. Não iniciar o Lote 12.

## Branch e base

- Branch: `feature/minemon-gen1-batch11-cobblemon`.
- Commit-base íntegro: `b7b81592f4e2e0097a32f9342e3890f7257182e5` — aprovação do Lote 10.
- Actions do Lote 10: [Build Minemon #39815820789](https://github.com/PetrickFaria/minemon/actions/runs/39815820789), sucesso; artefato #12443109947.
- Artefato do Lote 10 contém `Minemon-1.19.2-2.0.0.jar`; expira em 2026-11-05.
- Commit documental inicial do Lote 11 será registrado nesta branch. Nenhum recurso dos lotes anteriores será reimportado nem modificado sem causa técnica comprovada.

## Checkpoint 1 — preparação e auditoria da fonte

Estado inicial: todos os Pokémon do Lote 11 estão pendentes de integração. A auditoria de animação já registrada no projeto aponta que os dados nativos e os clips ativos diferem por espécie; a adaptação deve seguir o poser/clip realmente selecionado pelo runtime e documentar fallbacks.

| # | Espécie | Evidência de animação da auditoria existente |
|---:|---|---|
| 101 | Electrode | Clips terrestres de idle e walk existem no pacote, mas não estavam referenciados por poser; run ausente. Não exigir membros. |
| 102 | Exeggcute | Clips de idle e walk existem no pacote, sem referência ativa; run ausente. Preservar o conjunto e os pivôs individuais. |
| 103 | Exeggutor | Clips de idle e walk existem; forma/variante pode reutilizar outro alvo. Sem run. Confirmar o alvo Kanto e o poser efetivo. |
| 104 | Cubone | A auditoria indica ausência de clips idle/walk/run. Confirmar a fonte e documentar fallback; não inventar movimento. |
| 105 | Marowak | Idle referencia clip nomeado Alolan, mas a auditoria não encontrou trilha de osso; walk/run ausentes. Investigar o alvo Kanto antes de adaptar. |
| 106 | Hitmonlee | Clips idle e walk existentes, mas sem referência ativa; run ausente. Preservar os movimentos característicos existentes. |
| 107 | Hitmonchan | Clips idle e walk existentes, mas sem referência ativa; run ausente. Preservar movimentos de braços disponíveis. |
| 108 | Lickitung | Clips idle, walk e run existentes, sem referência ativa. Conferir pivô/hierarquia da língua na fonte. |
| 109 | Koffing | Idle existe sem referência; walk/run ausentes; fonte aceita hover/flutuação, separado de corrida terrestre. |
| 110 | Weezing | Idle nativo ativo; walk/run ausentes; fonte aceita hover/flutuação, separado de corrida. Preservar as duas cabeças Kanto. |

A tabela acima é inventário preliminar do repositório e não substitui a inspeção dos arquivos do Cobblemon 1.8.2. Corrigir o inventário se a fonte efetiva divergir.

## Checkpoint 2/3 — integração #101–#110

- Importados os dez modelos, texturas, licenças e arquivos de animação do JAR Cobblemon Fabric 1.8.2 com SHA-256 `cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2`.
- Selecionadas as variantes Kanto canônicas (`<slug>.geo.json`); nenhum arquivo Alolan, Galarian ou Hisuian foi usado como modelo principal.
- Atualizadas somente as definições #101–#110 e os perfis correspondentes. Verificador estrutural confirmou preservação das definições #001–#100 e dos demais registros.
- #101 Electrode: `ground_idle` e `ground_walk` nativos; sem corrida nativa.
- #102 Exeggcute: preservados os seis ovos e clips nativos; sem corrida nativa.
- #103 Exeggutor: forma Kanto com três cabeças e clips terrestres nativos; sem corrida nativa.
- #104 Cubone e #105 Marowak: clip `idle` é uma pose estática; movimento usa fallback para o próprio idle e está classificado como tal nos perfis. Não foi inventada caminhada ou corrida.
- #106 Hitmonlee: idle/walk nativos; sem corrida nativa.
- #107 Hitmonchan: idle/walk nativos; clips `ground_run_WIP` e `physical_attack_WIP` preservados mas não selecionados.
- #108 Lickitung: idle/walk/run nativos; perfil inclui os segmentos articulados da língua.
- #109 Koffing e #110 Weezing: comportamento da espécie no Cobblemon define voo e ausência de caminhada terrestre; clips `air_fly` usados no estado de movimento. Weezing segue `flying`, corrigindo o modo legado `ground`.
- Clips opcionais de sentar/dormir/ataque só são referenciados quando a fonte contém clip correspondente. O importador não cria substitutos `.minemon_*` para o Lote 11.
- Adaptados aliases Molang Bedrock `q.` → `query.` e namespace `Math.` → `math.` para os clips importados.

Pokémon implementados no checkout: #101–#110. A verificação estrutural preliminar passou. Ainda faltam os testes Java direcionados, a suíte completa, GameTests do servidor, smoke do cliente, compilação/JAR e Actions final.

## Correções e recursos protegidos

- Preservar: corridas de Graveler (#075) e Golem (#076), rigs de asas de Pidgeotto (#017) e Pidgeot (#018), braços de Charizard (#006).
- Não alterar as chamas de Charmander (#004), Charmeleon (#005) ou Charizard (#006). Permanecem pendências visuais não bloqueantes; Charmander teve melhora anterior que deve ser preservada.
- Preservar os primeiros 100 Pokémon, os registros das 151 espécies e recursos não atualizados.

## Validações e próxima etapa

- Aprovação base do Lote 10 confirmada no Actions e no JAR do artefato.
- `tools/verify_batch11.py`: PASS local — hierarquias, ossos, pivôs/valores finitos, textura/UV, clips/events, modo e fallbacks; preservação dos primeiros 100 recursos conferida contra a base aprovada.
- `tools/verify_molang_compatibility.py`: PASS dos clips ativos existentes (reporta somente funções de montaria incompatíveis em clips não selecionados já conhecidos; sem alteração desse escopo).
- Ainda não executados: testes Java, GameTests, smoke completo do cliente, build final e inspeção do JAR do Lote 11.
- Próximo passo: executar testes Molang e de compatibilidade do Batch 11 direcionados; corrigir apenas erros demonstrados antes da suíte inteira.
- GitHub Actions do Lote 11: ainda não iniciado; evitar push/workflow até os testes locais direcionados aprovarem.
- Avaliação visual manual dos novos Pokémon: pendente após entrega técnica.
