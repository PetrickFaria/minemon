# Lotes 2 e 3 — Cobblemon 1.8.2
Base: Lote 1 aprovado em `2aa0c05334900961caa751e78c52634fc1617829`.
Lote 2: `aec7cae8b9956f5c78d5441330f6f4f891192831`, Actions 39464045461 aprovado antes de iniciar a implementação do Lote 3.
Fonte: Cobblemon Fabric 1.8.2; SHA-256 `cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2`.

## Escopo
- Lote 2 (#011–#020): Metapod, Butterfree, Weedle, Kakuna, Beedrill, Pidgey, Pidgeotto, Pidgeot, Rattata, Raticate.
- Lote 3 (#021–#030): Spearow, Fearow, Ekans, Arbok, Pikachu, Raichu, Sandshrew, Sandslash, Nidoran-F, Nidorina.
- Modelos base; variante masculina quando a fonte somente disponibiliza modelos por sexo e o Minemon não seleciona gênero.
- 151 registros mantidos. Cada importação compara integralmente as outras 141 definições e mantém os recursos anteriores. Lote 1 e sua chama não foram reimportados.
- Nenhuma implementação do Lote 4.

## Animações e limitações
Metapod e Kakuna não possuem caminhada nativa: mantêm repouso animado como fallback explícito, sem alegação de marcha própria. As asas dos voadores do Lote 2 são testadas em air_idle/air_fly; clips terrestres disponíveis continuam preservados, separados do voo.
Spearow e Fearow usam q.biped_walk e q.look nos posers originais. GeckoLib 3 não executa esses procedimentos do Cobblemon: a adaptação conserva a pose terrestre e acrescenta às pernas um ciclo oposto de ±30 graus a 720 graus/segundo. É uma adaptação de compatibilidade, não uma reprodução exata do relógio de passos do Cobblemon. A função de olhar usa pitch/yaw limitados apenas nesses dois rigs. O repouso terrestre original é uma pose estática com resposta ao olhar; o teste fornece entrada de olhar, sem alegar oscilação autônoma.
Os modos de comportamento existentes foram preservados. Não foi acrescentada IA de voo a Spearow/Fearow; as poses aéreas presentes na fonte permanecem no arquivo, e a referência aérea ausente no arquivo original do Spearow não é inventada.
Ekans, Arbok, Pikachu, Raichu, Sandshrew, Nidoran-F e Nidorina conservam seus clips nativos de corrida quando disponíveis no modelo selecionado. A corrida em bola do Sandshrew é verificada pelo osso ball, e não pelas pernas recolhidas.
Referências source a bones ausentes ficam discriminadas no manifesto e são removidas somente da cópia runtime. A caminhada procedural nova não herda canais incompatíveis de asas.
Licenças per-model acompanham as cópias atualizadas. Nenhum código Fabric foi incorporado.

## Validação
São mantidos compilação, testes Java, GameTests de servidor, smoke cliente geral dos 151 e perfis detalhados cumulativos dos três lotes. O gate final continua obrigatório para publicar o JAR como aprovado. A coleta de fases aguarda renderização, sem remover asserções ou ampliar limites de velocidade.
O workflow não publica commits: recursos são preparados com permissões read-only e publicados pelo conector GitHub.
A avaliação visual dos três lotes está pendente por autorização do usuário: conferir escala/UV, aparência e clipping, ritmo dos movimentos, poses e chama dentro do Minecraft.
