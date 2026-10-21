# Minemon Gen 1 — Lote 8

## Objetivo

Integrar exclusivamente os recursos Cobblemon Fabric 1.8.2 dos Pokémon #071–#080, preservando os Lotes 1–7, os 151 registros e todas as funcionalidades existentes. A validação visual manual no Minecraft permanece pendente. Não iniciar o Lote 9.

## Base e branch

- Branch: `feature/minemon-gen1-batch8-cobblemon`
- Base aprovada: `feature/minemon-gen1-batch7-cobblemon` @ `b77eac9c38aa0d6a1d35c3cfab36a707ac6cab24`
- GitHub Actions base aprovado: [39543680758](https://github.com/PetrickFaria/minemon/actions/runs/39543680758)
- Planejamento confirmado: `docs/animation-audit/minemon-gen1-16-batch-plan.md`, Lote 8 = #071–#080.
- Não havia branch do Lote 8 nem commits posteriores na branch do Lote 7 durante a preparação.
- A branch foi criada a partir do SHA aprovado acima. Nenhum recurso de jogo foi modificado neste checkpoint.

## Escopo exato

1. #071 Victreebel
2. #072 Tentacool
3. #073 Tentacruel
4. #074 Geodude
5. #075 Graveler
6. #076 Golem
7. #077 Ponyta
8. #078 Rapidash
9. #079 Slowpoke
10. #080 Slowbro

## Progresso

### Concluído — checkpoint de preparação

- Confirmada a base aprovada do Lote 7 e a ausência de commits posteriores.
- Confirmado no plano original o escopo #071–#080.
- Verificadas as execuções recentes: a execução base 39543680758 está concluída com sucesso; nenhuma execução do Lote 8 está em andamento.
- Criada esta branch sobre o commit aprovado.
- Criado este documento de continuidade.
- Baixado e verificado o pacote de referência Cobblemon Fabric 1.8.2: SHA-256 `cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2`.
- Confirmados no pacote modelo, textura, arquivo de animação e poser quando existente para #071–#080.
- Confirmados clips de água para Tentacool, Tentacruel, Slowpoke e Slowbro; serão preservados, sem introduzir seletor de natação.
- Identificados clips terrestres `ground_run` para Geodude, Graveler, Golem e Rapidash. Ponyta fornece `ground_runold`; a seleção como corrida depende de validação do clip contra modelo/testes e registro de que o poser não o seleciona, se aplicável.
- A fonte não fornece `ground_run` para Victreebel, Tentacool, Tentacruel, Slowpoke e Slowbro. Nesses casos, manter fallback existente em vez de inventar corrida.
- A inspeção do primeiro grupo confirmou animações que movimentam folhas/caule (Victreebel), os tentáculos e flaps (Tentacool/Tentacruel) e membros (Geodude/Graveler).

### Pendente

- Auditar no pacote-fonte Cobblemon 1.8.2 os modelos, texturas, clips, poser/controladores, expressões Molang e eventos para as dez espécies.
- Registrar as adaptações específicas e validar compatibilidade com GeckoLib 3.1.40.
- Implementar o primeiro grupo (#071–#075), validar recursos e publicar checkpoint.
- Implementar o segundo grupo (#076–#080), validar recursos e publicar checkpoint.
- Rodar os gates cumulativos até 80 espécies, regressões, build, testes Java, GameTests, smoke do cliente e inspeção do JAR.
- Iniciar/consultar Actions somente após o commit de implementação; nunca duplicar execução em andamento.
- Após CI, registrar resultado, link/ID, artefato e expiração. A avaliação visual manual segue pendente até o usuário avaliar no Minecraft.

## Preservação e gates

- Comparar as alterações com o commit-base aprovado.
- O gate cumulativo deverá exigir #071–#080 como únicas novas definições, 80 espécies atualizadas, 151 registros preservados, recursos e perfis válidos no JAR.
- Preservar recursos anteriores, incluindo a chama do Charmander, braços do Charizard e estado de ossos/renderização das asas de Pidgeotto/Pidgeot.
- Não enfraquecer testes nem introduzir IA, combate, captura ou locomoção novos.
- Não iniciar o Lote 9.

## Testes e erros

- Testes da preparação: leitura da branch/commit e comparação do planejamento; download do JAR fonte e validação do SHA-256; inspeção dos caminhos/nomes de clips/posers. Nenhum teste de código nesta etapa.
- Erros identificados até agora: nenhum na preparação.
- Não há execução do Lote 8 associada a esta branch ainda.

## Próxima ação

Auditar os dez conjuntos de recursos do Cobblemon 1.8.2 e os validadores/fluxos de importação existentes. Atualizar esta seção antes do checkpoint de implementação #071–#075.
