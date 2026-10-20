# Lote 5 — progresso e retomada

Base: `e435ab9d4d9706859effb16f0ec2636c91b74c30`, encerramento do Lote 4. Código aprovado: `f9cda38`, [Actions 39475562845](https://github.com/PetrickFaria/minemon/actions/runs/39475562845).
Branch: `feature/minemon-gen1-batch5-cobblemon`.

Escopo exclusivo #041–#050: Zubat, Golbat, Oddish, Gloom, Vileplume, Paras, Parasect, Venonat, Venomoth e Diglett. Lote 6 não autorizado. Avaliação visual manual pendente por decisão do usuário.

## Etapas

- [x] Confirmar base e aprovação; ausência de commits posteriores na branch base.
- [x] Criar branch própria.
- [x] Auditar fonte Cobblemon 1.8.2: modelos, texturas, ossos, posers, expressões e eventos.
- [x] Integrar somente dez espécies; preservar outras 141 entradas e recursos anteriores.
- [x] Compilação, 9 testes Java, 13 GameTests e smoke cumulativo dos 50 e gerais dos 151.
- [x] Inspecionar JAR real; artefato publicado e conferido.

## Recuperação

Consultar commits desta branch e Actions existentes antes de repetir operações. Auditoria/preparação somente com `contents: read`, sem executar código do Cobblemon; publicação pelo conector GitHub. Não reiniciar workflows em andamento. Adaptar posers apenas com evidência dos ossos existentes; preservar poses estáticas. Nenhuma IA, atributo ou mecânica nova. Comparar com a base para proteger os 40 anteriores, chama do Charmander, braços do Charizard e eventos anteriores.

Auditoria: [Actions 39481543733](https://github.com/PetrickFaria/minemon/actions/runs/39481543733). Todos os dez têm locomoção nativa. Zubat/Golbat/Venomoth conservam modo flying, selecionando air_idle/air_fly; voo não é classificado como corrida terrestre. Oddish e Parasect têm ground_run nativo. Demais terrestres não recebem corrida inventada. Variantes padrão masculinas quando a geometria é separada por sexo; não importar formas Alola/Valencian/shiny como funcionalidades.

Zubat: visibilidade de asas dobradas/abertas do poser convertida em escala dos mesmos pais, sem remover ou mudar ossos/pivôs. Parasect: Math.cos normalizado; consultas q.r de clips de montaria conservados recebem entrada neutra zero porque o Minemon não implementa montaria. Clips selecionados de locomoção não dependem dessas consultas. Eventos cry e timelines de wing flap são conservados como evidência; a execução de scripts q.sound do Cobblemon não existe no runtime GeckoLib e não é declarada como áudio validado.

O smoke aprovado do Lote 4 levou 24m07s. O orçamento do processo para 50 espécies será de 35min (projeção proporcional ~30min), mantendo intactos fases, ticks por espécie, velocidades, thresholds e asserções. Sem redução da cobertura dos 151 nem dos perfis anteriores.

Inconsistência adicional identificada: clips Golbat animam wing_left_rotation/right_rotation, mas as geometrias 1.8.2 omitem esses grupos. Para não perder parte substancial da batida das asas, restaurar os dois grupos de transformação nos pivôs existentes das asas, sob wings; reparentear somente wing_left/right a esses grupos. Nenhum pivô, cubo, textura ou canal nativo é deslocado/modificado. Os demais 149 modelos (inclusive os 40 aprovados) permanecem intactos. Vileplume.sleep contém nomes de folhas/baba de outro rig; remover apenas canais ausentes listados na evidência runtime, mantendo os canais válidos; pose de sono requer revisão visual.

Preparação aprovada: [Actions 39482213213](https://github.com/PetrickFaria/minemon/actions/runs/39482213213). Recursos das 151 espécies e preservação das outras 141 entradas aprovados. Publicação retomada a partir dos mesmos 43 arquivos preparados, sem nova importação. Gate verify_batch5 compara com e435ab9; testes Java cobrem todos os clips/expressões novos, grupos de Golbat e visibilidade de Zubat. O smoke conserva os testes anteriores e acrescenta a asserção de visibilidade de Zubat. Nenhum código de entidade/IA/combate alterado. attackTicks de Zubat/Diglett sincroniza somente os clips físicos nativos de 3s.

## Diagnóstico do gate de geometria

Runs 39503957422 e 39504234020 falharam antes da compilação: comparação set/dict no verificador corrigida; depois o gate revelou quatro cubos oculares com dimensões assinadas nativas (Paras eye_left/right; Venomoth left_eye/right). O baker GeoCube da GeckoLib 3 aceita dimensões assinadas e calcula UV com seus sinais; tornar os tamanhos positivos alteraria o atlas (Venomoth usa v=64 com extensões negativas). Preservar exatamente os quatro cubos com origem/tamanho/UV/inflate auditados. O gate rejeita outros casos negativos e alterações desses quatro. Novo teste Java usa GeoCube.createFromPojoCube da dependência 3.1.40 para exigir seis faces, vértices finitos e UVs dentro do atlas. Nenhum recurso foi modificado para contornar o gate.

Referência primária: https://github.com/bernie-g/geckolib/blob/1.19/Forge/src/main/java/software/bernie/geckolib3/geo/render/built/GeoCube.java .

## Compatibilidade Molang das pernas de Paras/Parasect

Run 39504614191: 8 testes Java executados, 7 passaram; o teste abrangente encontrou erro do parser 3 em -(math.sin(...)*1)*0.25. Reescritos somente os 16 canais correspondentes para -1*(math.sin(...)*1)*0.25, sem alterar tempos, valores ou oscilação. Novo teste compara 81 amostras contra -0.25*sin em graus, idle a 360 graus/s e walk a 1440 graus/s. Os testes de cubos oculares assinados, grupos Golbat e visibilidade Zubat passaram nessa execução. Os recursos dos 40 anteriores continuam intactos.


## Validação concluída

- Código validado: `b43311c69b698b622e589920c7a593dcb88e6d56`.
- GitHub Actions: [execução 39505150691](https://github.com/PetrickFaria/minemon/actions/runs/39505150691) — sucesso.
- Compilação/Java: `build` concluído; 9 testes (5 Batch5, 1 Batch3 e 3 Molang), 0 falhas, 0 erros, 0 ignorados.
- GameTests do servidor: 13 testes obrigatórios aprovados.
- Smoke do cliente: aprovado em aproximadamente 30m48s. WORLD, COMBAT, LOCOMOTION e ROSTER passaram; as 151 espécies renderizaram com textura. A chama independente do Charmander mudou em repouso e caminhada; a regressão dos braços dos starters passou.
- Perfis cumulativos: 48 aprovações detalhadas de 50; as duas exceções esperadas são Kakuna e Metapod, que não têm clipe nativo de caminhada e usam idle. Os demais Pokémon continuam cobertos pelos testes gerais de renderização e movimento.
- Gate e JAR: os 151 registros permanecem presentes; exatamente as 50 espécies #001–#050 usam os recursos atualizados; os 40 anteriores e as 101 espécies fora do escopo foram preservados. Conteúdo interno do JAR inspecionado: 1.927 entradas, `Minemon-1.19.2-2.0.0.jar`, 5.550.607 bytes, SHA-256 `1df177a2575fe471a29c2381f74c810a5b5d68f08ab0d74b32edaf659a6b1a7e`.
- Artefato do GitHub Actions: [Minemon-1.19.2-2.0.0-gen1 (ID 12304019220)](https://github.com/PetrickFaria/minemon/actions/runs/39505150691/artifacts/12304019220), contendo um JAR. Expira em 3 de novembro de 2026.

## Pendências e limites visuais

A aprovação automatizada não substitui a inspeção visual manual dentro do Minecraft, que permanece pendente.

- Zubat, Golbat e Venomoth usam seus clips de voo nativos; voo não foi convertido em corrida terrestre. Só Oddish e Parasect têm corrida terrestre nativa. Para as outras espécies terrestres sem esse clip, usa-se a caminhada em movimento acelerado, sem inventar nova animação.
- `Vileplume.sleep` referencia folhas e baba ausentes no rig escolhido; apenas esses canais inexistentes foram omitidos da cópia runtime. A aparência da pose de sono deve ser verificada no jogo.
- Os eventos nativos de choro foram mantidos. As timelines de batida de asas que chamam `q.sound` também foram preservadas, mas o runtime GeckoLib do Minemon não executa esses scripts de som; esse áudio não foi validado.
- A textura emissiva referenciada pelo recurso de Venonat não foi integrada como camada emissiva, pois o Minemon não tem esse caminho visual.
- Nos casos em que Cobblemon separa a geometria por sexo, foi usada a variante masculina padrão; formas regionais e variantes shiny não fazem parte do lote.
- Os quatro cubos oculares assinados de Paras e Venomoth foram mantidos exatamente como na fonte e aprovados pelo baker GeoCube real da GeckoLib 3.1.40 e pelo gate do JAR.

Lote 6 não iniciado. Nenhuma inspeção visual manual foi realizada.
