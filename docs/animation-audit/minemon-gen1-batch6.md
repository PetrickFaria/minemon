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
- [ ] Concluir gate cumulativo, testes Java, GameTests, smoke do cliente, inspeção do JAR, commit e publicação.

## Adaptações e limites conhecidos

- Meowth e Persian mantêm o idle nativo como pose durante movimento; não há caminhada/poser no pacote de referência 1.8.2.
- Growlithe não tem clipe nativo de `ground_walk`; sua caminhada procedural do poser foi traduzida para os membros presentes no modelo. O idle da fonte é uma pose estática.
- O clip de corrida terrestre nativo de Lote 6 existe somente para Arcanine. Os demais não recebem corrida inventada.
- Psyduck e Golduck preservam animações aquáticas da fonte, mas o runtime atual do Minemon não alterna para natação.
- Os recursos de Poliwag estão marcados `placeholder` na fonte e são preservados como tais; a qualidade visual desses clips requer inspeção manual.
- As expressões `q.r.yaw_change` e `q.r.velocity_y` dos clips montados de Arcanine recebem entrada neutra zero; Minemon não implementa montaria. Os eventos de choro da fonte são conservados.
- A inspeção visual dentro do Minecraft permanece pendente; os testes automatizados não substituem essa revisão.
