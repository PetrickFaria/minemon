# Lote 4 — progresso e retomada

Base aprovada: `8bba5b0563d2170a77c1546ea81e32e717569af1`, Actions [39469489652](https://github.com/PetrickFaria/minemon/actions/runs/39469489652).
Branch: `feature/minemon-gen1-batch4-cobblemon`.

Escopo exclusivo #031–#040: Nidoqueen, Nidoran♂, Nidorino, Nidoking, Clefairy, Clefable, Vulpix, Ninetales, Jigglypuff e Wigglytuff. Lote 5 não autorizado.

## Etapas

- [x] Confirmar base aprovada e ausência de commits posteriores na branch base.
- [x] Criar branch própria preservando os três lotes anteriores.
- [x] Auditar modelos, variantes, texturas, ossos, clips e posers Cobblemon 1.8.2.
- [ ] Importar exclusivamente dez espécies, preservando outras 141 entradas.
- [ ] Compilar, testes Java, GameTests servidor, smoke cliente cumulativo e inspeção real do JAR.
- [ ] Publicar links de commit, execução aprovada e artefato.

## Recuperação

Consultar primeiro os commits desta branch e suas execuções Actions. Não reiniciar execuções em andamento. A auditoria e preparação de recursos têm somente `contents: read`; a publicação é feita pelo conector GitHub. Nenhum código do arquivo Cobblemon é executado, apenas recursos são lidos com SHA-256 fixado. As correções de Charmander e Charizard, os primeiros 30 recursos, registros/atributos/IA e mecânicas permanecem fora das alterações.

Avaliação visual manual pendente por decisão do usuário. Aprovação automatizada não substitui inspeção no Minecraft.

Auditoria fonte: Actions 39473590254, 39473802916 e 39473935826. Nidoqueen/Nidorino usam posers Kotlin compilados, inspecionados com javap sem execução. Ninetales/Jigglypuff/Wigglytuff usam posers JSON procedurais. Adaptação preserva a pose nativa e acrescenta ciclo simples nos membros declarados; relógio adaptado ao Minemon, sem alegar equivalência exata ao limb swing Cobblemon. Nidoqueen, Ninetales, Jigglypuff e Wigglytuff têm idle estático nativo: manter e validar estabilidade. Demais seis conservam idle animado. Nidoran♂, Nidoking e Vulpix têm ground_run nativo; demais reutilizam caminhada em alta velocidade, sem inventar clip de corrida.
