# Lote 6 — progresso e retomada

Branch: `feature/minemon-gen1-batch6-cobblemon`  
Base publicada: `eae973354e05a49dc2cd4522006c07585dbd82af` (ponta da branch do Lote 5; código testado em `b43311c69b698b622e589920c7a593dcb88e6d56`, Actions 39505150691 aprovado).  
Escopo: somente #051–#060, conforme o plano de 16 lotes. O Lote 7 não foi iniciado.

## Pokémon autorizados

| # | Pokémon | Recursos do Cobblemon 1.8.2 |
|---:|---|---|
| 051 | Dugtrio | idle e caminhada nativos |
| 052 | Meowth | idle nativo; o pacote não contém caminhada nem poser |
| 053 | Persian | idle nativo; o pacote não contém caminhada nem poser |
| 054 | Psyduck | idle e caminhada terrestres nativos; clipes aquáticos preservados |
| 055 | Golduck | idle e caminhada terrestres nativos; clipes aquáticos preservados |
| 056 | Mankey | idle e caminhada nativos |
| 057 | Primeape | idle e caminhada nativos |
| 058 | Growlithe | idle de pose nativo; caminhada procedural do poser adaptada aos quatro ossos de perna reais |
| 059 | Arcanine | idle, caminhada e corrida nativos |
| 060 | Poliwag | clipes de solo e água marcados como placeholders no pacote; mantidos sem inventar movimento |

## Etapas

- [x] Confirmar HEAD do Lote 5, o plano original e a aprovação de Actions antes de editar.
- [x] Auditar o arquivo Cobblemon Fabric 1.8.2 por SHA-256; conferir modelos, texturas, esqueletos, clipes, posers e eventos de animação.
- [x] Integrar localmente os dez recursos e seus arquivos de licença; manter os 151 registros e os recursos anteriores sem alteração.
- [x] Adaptar a caminhada procedural de Growlithe a partir de `q.quadruped_walk(0.66, 1.4)`, usando pares diagonais de pernas no ciclo de GeckoLib 3.
- [x] Marcar poses nativas estáticas sem adicionar oscilação artificial; Meowth e Persian usam idle como fallback de movimento porque a fonte não oferece caminhada.
- [x] Passar gate de recursos: 151 registros ordenados, exatamente as espécies 051–060 atualizadas, os 50 anteriores preservados e os 91 registros restantes inalterados.
- [x] Passar verificação do projeto e preparar uma cópia dos 151 modelos/clipes para validar as transformações de runtime GeckoLib 3.
- [x] Investigar a falha de `Batch6AnimationCompatibilityTest` na execução `39523351214`: o parser GeckoLib 3 rejeitou o operador unário aplicado a uma expressão parentetizada no movimento da pata esquerda de Psyduck.
- [x] Reescrever por identidades algébricas as expressões iniciadas por `-(...)` nos clips importados de Psyduck, Golduck, Mankey, Primeape e Arcanine; manter valores, fases e amplitudes originais em formas aceitas pelo parser GeckoLib 3.
- [x] Identificar que a execução `39524128879` falhou no verificador de JSON antes da compilação: os três arquivos de animação maiores foram truncados durante a publicação via API.
- [ ] Republicar e validar os arquivos completos por SHA; depois completar compilação/testes Java, GameTests, smoke do cliente e inspeção do JAR.

O `./gradlew clean test build` não pôde ser executado neste ambiente local: o Gradle Wrapper 7.5.1 tentou baixar sua distribuição e a conexão de saída foi recusada. A validação completa será feita no GitHub Actions antes da entrega.

A janela máxima do smoke do cliente foi ampliada de 35 para 45 minutos com as asserções, velocidades e fases intactas. O smoke cumulativo do Lote 5 levou cerca de 31 minutos para 50 perfis detalhados; a janela maior comporta a cobertura de 60 sem encurtar os testes.

A execução `39523351214` passou os dois gates de recursos e todos os testes Java anteriores; um dos quatro testes do Lote 6 falhou ao analisar uma expressão Molang do clip `water_swim` de Psyduck. A execução `39523772540` encontrou a mesma forma em expressões de Golduck, levando à adaptação algébrica dos casos equivalentes nos arquivos do Lote 6. Na execução `39524128879`, o verificador estrutural identificou JSON inválido antes da compilação; a publicação via API havia truncado os arquivos grandes de Arcanine, Mankey e Primeape. Os arquivos locais completos serão publicados com verificação de SHA. Os GameTests, o smoke do cliente e o JAR seguem pendentes.

## Adaptações e limites conhecidos

- Meowth e Persian mantêm o idle nativo como pose durante movimento; não há caminhada/poser no pacote de referência 1.8.2.
- Growlithe não tem clipe nativo de `ground_walk`; sua caminhada procedural do poser foi traduzida para os membros presentes no modelo. O idle da fonte é uma pose estática.
- O clip de corrida terrestre nativo de Lote 6 existe somente para Arcanine. Os demais não recebem corrida inventada.
- Psyduck e Golduck preservam animações aquáticas da fonte, mas o runtime atual do Minemon não alterna para natação.
- Os recursos de Poliwag estão marcados `placeholder` na fonte e são preservados como tais; a qualidade visual desses clips requer inspeção manual.
- As expressões `q.r.yaw_change` e `q.r.velocity_y` dos clips montados de Arcanine recebem entrada neutra zero; Minemon não implementa montaria. Os eventos de choro da fonte são conservados.
- A inspeção visual dentro do Minecraft permanece pendente; os testes automatizados não substituem essa revisão.
