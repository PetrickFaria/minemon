# Lote 5 — progresso e retomada

Base: `e435ab9d4d9706859effb16f0ec2636c91b74c30`, encerramento do Lote 4. Código aprovado: `f9cda38`, [Actions 39475562845](https://github.com/PetrickFaria/minemon/actions/runs/39475562845).
Branch: `feature/minemon-gen1-batch5-cobblemon`.

Escopo exclusivo #041–#050: Zubat, Golbat, Oddish, Gloom, Vileplume, Paras, Parasect, Venonat, Venomoth e Diglett. Lote 6 não autorizado. Avaliação visual manual pendente por decisão do usuário.

## Etapas

- [x] Confirmar base e aprovação; ausência de commits posteriores na branch base.
- [x] Criar branch própria.
- [x] Auditar fonte Cobblemon 1.8.2: modelos, texturas, ossos, posers, expressões e eventos.
- [x] Integrar somente dez espécies; preservar outras 141 entradas e recursos anteriores.
- [ ] Compilação, Java, GameTests, smoke cumulativo dos 50 e gerais dos 151.
- [ ] Inspecionar JAR real, publicar artefato e resultado.

## Recuperação

Consultar commits desta branch e Actions existentes antes de repetir operações. Auditoria/preparação somente com `contents: read`, sem executar código do Cobblemon; publicação pelo conector GitHub. Não reiniciar workflows em andamento. Adaptar posers apenas com evidência dos ossos existentes; preservar poses estáticas. Nenhuma IA, atributo ou mecânica nova. Comparar com a base para proteger os 40 anteriores, chama do Charmander, braços do Charizard e eventos anteriores.

Auditoria: [Actions 39481543733](https://github.com/PetrickFaria/minemon/actions/runs/39481543733). Todos os dez têm locomoção nativa. Zubat/Golbat/Venomoth conservam modo flying, selecionando air_idle/air_fly; voo não é classificado como corrida terrestre. Oddish e Parasect têm ground_run nativo. Demais terrestres não recebem corrida inventada. Variantes padrão masculinas quando a geometria é separada por sexo; não importar formas Alola/Valencian/shiny como funcionalidades.

Zubat: visibilidade de asas dobradas/abertas do poser convertida em escala dos mesmos pais, sem remover ou mudar ossos/pivôs. Parasect: Math.cos normalizado; consultas q.r de clips de montaria conservados recebem entrada neutra zero porque o Minemon não implementa montaria. Clips selecionados de locomoção não dependem dessas consultas. Eventos cry e timelines de wing flap são conservados como evidência; a execução de scripts q.sound do Cobblemon não existe no runtime GeckoLib e não é declarada como áudio validado.

O smoke aprovado do Lote 4 levou 24m07s. O orçamento do processo para 50 espécies será de 35min (projeção proporcional ~30min), mantendo intactos fases, ticks por espécie, velocidades, thresholds e asserções. Sem redução da cobertura dos 151 nem dos perfis anteriores.

Inconsistência adicional identificada: clips Golbat animam wing_left_rotation/right_rotation, mas as geometrias 1.8.2 omitem esses grupos. Para não perder parte substancial da batida das asas, restaurar os dois grupos de transformação nos pivôs existentes das asas, sob wings; reparentear somente wing_left/right a esses grupos. Nenhum pivô, cubo, textura ou canal nativo é deslocado/modificado. Os demais 149 modelos (inclusive os 40 aprovados) permanecem intactos. Vileplume.sleep contém nomes de folhas/baba de outro rig; remover apenas canais ausentes listados na evidência runtime, mantendo os canais válidos; pose de sono requer revisão visual.

Preparação aprovada: [Actions 39482213213](https://github.com/PetrickFaria/minemon/actions/runs/39482213213). Recursos das 151 espécies e preservação das outras 141 entradas aprovados. Publicação retomada a partir dos mesmos 43 arquivos preparados, sem nova importação. Gate verify_batch5 compara com e435ab9; testes Java cobrem todos os clips/expressões novos, grupos de Golbat e visibilidade de Zubat. O smoke conserva os testes anteriores e acrescenta a asserção de visibilidade de Zubat. Nenhum código de entidade/IA/combate alterado. attackTicks de Zubat/Diglett sincroniza somente os clips físicos nativos de 3s.
