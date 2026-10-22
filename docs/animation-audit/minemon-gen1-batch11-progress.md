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

## Pokémon implementados / pendentes

- Implementados no Lote 11: nenhum.
- Pendentes: #101–#110.

## Correções e recursos protegidos

- Preservar: corridas de Graveler (#075) e Golem (#076), rigs de asas de Pidgeotto (#017) e Pidgeot (#018), braços de Charizard (#006).
- Não alterar as chamas de Charmander (#004), Charmeleon (#005) ou Charizard (#006). Permanecem pendências visuais não bloqueantes; Charmander teve melhora anterior que deve ser preservada.
- Preservar os primeiros 100 Pokémon, os registros das 151 espécies e recursos não atualizados.

## Validações e próxima etapa

- Aprovação base do Lote 10 confirmada no Actions e no JAR do artefato.
- Validações do Lote 11: ainda não executadas.
- Próximo passo: obter/confirmar os arquivos da fonte Cobblemon 1.8.2 para os dez alvos Kanto; comparar modelos, texturas, poses e clips com o inventário; depois implementar #101–#105 e executar verificações direcionadas antes do commit intermediário.
- GitHub Actions do Lote 11: ainda não iniciado.
- Avaliação visual manual dos novos Pokémon: pendente após entrega técnica.
