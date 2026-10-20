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


## Diagnóstico do bloqueio do GitHub Actions — 2026-10-20

Branch HEAD confirmado no momento do diagnóstico: `d2126733333ecec67bec18236a5a7db804d80b8b`.

### Evidências dos runs 104–106

Foram examinadas as cinco tentativas de job registradas nos runs 104–106:

- Run 104: tentativas 1 e 2, job IDs `118389292671` e `118389694214`.
- Run 105: tentativas 1 e 2, job IDs `118401135186` e `118401258851`.
- Run 106: tentativa 1, job ID `118401520473`.

Todas terminaram em aproximadamente 3 segundos, com `runner_id=0`, nome do runner vazio e zero etapas. Nenhum artefato foi produzido. O endpoint de logs retorna 404; o conector GitHub disponível não oferece as anotações do check run, consulta aos runners nem dados de cobrança. Assim, não há mensagem de erro de inicialização acessível para atribuir a falha a uma cota ou bloqueio específico.

### Diferenciação de causas

- **Workflow:** `.github/workflows/build.yml` usa `ubuntu-latest` hospedado pelo GitHub e tem um único job `build`. O mesmo workflow executou normalmente no run 103: as etapas de compilação/testes Java e GameTests passaram e o job chegou ao smoke do cliente. Não há evidência que justifique alterar sua configuração.
- **Falha real de teste:** no run 103, o cliente falhou em `validateArticulationProfiles`: Jolteon selecionou `animation.jolteon.ground_walk` em `IDLE_FINAL`, quando o esperado era `animation.jolteon.ground_idle`. Essa execução chegou ao runner e não é a falha de inicialização investigada aqui.
- **Inicialização dos jobs:** nos runs 104–106 não houve runner atribuído nem etapa executada. Esse padrão é anterior ao workflow e compatível com indisponibilidade/recusa de runner, mas o motivo exato não aparece nos metadados acessíveis.
- **Status público:** o GitHub Status registra um incidente de disponibilidade de Actions em 19/10, resolvido às 16:56 UTC. Os jobs examinados começaram em 20/10, após o status público voltar a operacional; não há evidência de que o incidente seja a causa desses failures. O status em 20/10 às 00:09 UTC era “All Systems Operational”.
- **Permissões:** a conta conectada é `PetrickFaria` e o GitHub confirma permissão `admin` no repositório. A permissão para alterar arquivos está presente; isso não revela o estado de billing ou política de Actions da conta.
- **Cotas/restrições:** não foi possível confirmar nem descartar limite de minutos, orçamento de Actions, cobrança ou bloqueio de runners porque esses dados ficam na interface de billing/Actions da conta e não são expostos pelo conector nesta sessão. Não se deve tratar isso como confirmado.

### Ação controlada e próxima verificação

A única reexecução controlada feita foi a tentativa 2 do run 105; ela repetiu a falha sem iniciar etapa alguma. Não se repetiram novas execuções depois disso.

Antes de solicitar nova tentativa, abrir o job do [run 106](https://github.com/PetrickFaria/minemon/actions/runs/39353067243) no GitHub e ler a mensagem de falha/anotações exibidas na página do job. Se a mensagem mencionar billing/minutos/limite, conferir **Settings → Billing & licensing → Usage** na conta pessoal e resolver o aviso de orçamento/cobrança indicado. Se mencionar Actions desativado ou runner, conferir **Repository → Settings → Actions → General** e a disponibilidade/política de runners. Se não houver explicação ou indicação de bloqueio, encaminhar o run/job ID ao GitHub Support; a execução terminou sem logs que permitam diagnóstico por API.

O commit `8baa23cbe8748e2299a6977b3eab23f503f4baa7` (janela de 80 ticks) ainda não foi executado com sucesso: runs 105–106 falharam antes das etapas. Não alterar a janela de amostragem novamente, não iniciar a Fase 1 e preservar o último JAR validado até haver uma execução completa.
