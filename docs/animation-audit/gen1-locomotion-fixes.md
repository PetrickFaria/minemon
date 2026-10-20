# Registro de correções de locomoção — Minemon Gen 1

## Estado em 2026-10-20

- Branch: `feature/minemon-gen1-locomotion-fixes`
- Commit funcional original preservado: `001657b541f4d79599c046e79fe91acb419e5681`
- Commit de auditoria preservado: `2a9fe6debbdd39bfd3257ef32d96647f9691c533`
- Último commit confirmado antes deste registro: `8baa23cbe8748e2299a6977b3eab23f503f4baa7`
- A branch de correções é separada da branch funcional original.
- Fase 1 ainda não iniciada; nenhuma animação/asset de espécie foi alterada.

## Fase 0 — Instrumentação e resultados

A instrumentação testa nove perfis anatômicos com ossos locomotores nomeados: Bulbasaur, Pikachu, Caterpie, Hitmonlee, Onix, Psyduck, Jolteon, Nidorina e Meowth. O smoke também mede deslocamento separado de transformações locais e valida seleção de idle, walk e run.

### Execuções relevantes

- [Run 103 — commit 0ab7c8b](https://github.com/PetrickFaria/minemon/actions/runs/39346998608): compilação e testes Java passaram; GameTests de servidor passaram; o smoke do cliente falhou em `validateArticulationProfiles`. O relatório `client-validation.txt` contém `PASS MENU`, `PASS CREATIVE ORDER`, `PASS WORLD` e `PASS COMBAT`, mas não contém `PASS LOCOMOTION`. Causa funcional registrada no log: em Jolteon, `IDLE_FINAL` ainda selecionava `animation.jolteon.ground_walk` quando se esperava `animation.jolteon.ground_idle`. Nenhum JAR foi publicado por essa execução.
- [Run 104 — commit 199a5afe](https://github.com/PetrickFaria/minemon/actions/runs/39349258148): tentativa 2 terminou em falha sem iniciar nenhuma etapa do job e sem artefatos. Não valida nem invalida o ajuste do teste.
- [Run 105 — commit 8baa23c](https://github.com/PetrickFaria/minemon/actions/runs/39352945441): tentativa 1 e repetição do job terminaram em poucos segundos, ambas sem etapas ou artefatos. A nova amostragem não foi executada.

### Ajuste pendente de validação

Commit `8baa23cbe8748e2299a6977b3eab23f503f4baa7` ampliou cada janela por espécie de 60 para 80 ticks e adiou a coleta de `IDLE_FINAL` para a fase 68. Isso dá mais tempo ao servidor, à animação e à renderização para estabilizar após cessar o movimento. A alteração apenas ajusta o tempo de amostragem; mantém a exigência de que o clipe de repouso correto seja realmente selecionado.

**Status da Fase 0: pendente.** Não considerar aprovada até uma execução completa do smoke que confirme os clipes de idle/walk/run e as articulações dos perfis, além da compilação, GameTests e ausência de regressões.

## Próxima ação

1. Obter uma execução completa do workflow para o commit atual e examinar `client-validation.txt` e os logs do cliente.
2. Se o smoke repetir o caso de Jolteon, medir a sincronização efetiva entre ticks do servidor e do cliente e corrigir o diagnóstico de estado com cobertura preservada.
3. Só após aprovação comprovada da Fase 0 começar a Fase 1 em lotes de 5–10 espécies, priorizando lacunas de locomoção terrestre comprovadas.
4. Não alterar natação, voo, repouso/sono, efeitos, chamas da linha Charmander, registro, Pokédex, ovos, mod ID ou outras funcionalidades.

## Pendências

- Fase 0: execução completa do cliente no commit `8baa23c`.
- Fase 1: não iniciada.
- JAR novo validado: nenhum produzido pelos runs 103–105.
- Restrições de infraestrutura do Actions: dois runs recentes falharam sem executar etapas; diagnóstico remoto de logs desses runs não está disponível.
