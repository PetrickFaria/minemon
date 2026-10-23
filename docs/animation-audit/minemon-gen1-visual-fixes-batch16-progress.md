# Minemon — correções visuais e Lote 16

## Checkpoint de retomada — 2026-10-23

- Branch de trabalho: `feature/minemon-gen1-visual-fixes-batch16`.
- Base: checkpoint documental `731dd5aefde687ca92aa6f5a80678f1417179f0d`, contendo o código aprovado `276abc1428c8882a837e770a356f48a8d122bbab`.
- A execução #39963816100 concluiu com sucesso para o commit `276abc1`. O JAR publicado foi baixado e inspecionado: ZIP contém `Minemon-1.19.2-2.0.0.jar` (6.522.319 bytes), `pack.mcmeta`, `META-INF/mods.toml`, 15 classes e os recursos de 151 espécies. Artefato #12510114037, expiração informada pelo GitHub: 2026-11-06 15:00:35 UTC.
- `main` está no commit `b7dc3b09416546bfdd6c4a27cbfc47f6d010a4fc`, linha separada de testes do arena/smoke. Branches `visual-flames` e `visual-loco` são pilotos/auditorias anteriores ao Lote 15; não foram mescladas.

## Correções começadas nesta branch

- O gerador runtime já cria o osso de chama `tail_flame` para Charmander, Charmeleon e Charizard a partir dos planos nativos e seus pivôs. Antes, só Charmander declarava o clip independente gerado; Charmeleon e Charizard ficavam sem a oscilação própria da chama.
- Manifesto agora declara clips de chama para Charmeleon e Charizard usando o mesmo adaptador runtime `minemon_tail_flame`; os canais dos braços do Charizard não foram tocados.
- Smoke do cliente agora mede a chama independente de cada inicial em repouso e caminhada e exige seleção do clip correspondente.
- `verify_runtime.py` valida a chama para os três iniciais. Verificadores cumulativos Lotes 8–15 aceitam somente essas mudanças explícitas no manifesto; a extensão do perfil de corrida de Rapidash acrescenta apenas a articulação de fogo que o clip nativo já anima.
- O perfil atual confirma corrida nativa para Geodude, Graveler e Golem; Graveler/Golem usam `body_roll` no `ground_run`. Geodude usa `ground_run` próprio e não foi alterado.
- Ponyta/Rapidash já selecionam clips nativos de idle/walk e Rapidash tem `ground_run`; os clips têm canais Molang nas articulações de fogo. Ainda falta restaurar/confirmar a visibilidade dos planos. A geometria original contém planos de espessura zero e UVs fora do atlas em algumas partes (especialmente Rapidash). O workflow de auditoria existente foi estendido para recuperar do JAR Cobblemon 1.8.2 as texturas suplementares originais de fogo e os recursos completos de Mew antes de fazer adaptação.

## Validações locais

- Aprovados: verificadores `verify_batch8.py` a `verify_batch15.py`, `verify_runtime.py`, `verify_project.py` e `git diff --check`.
- Java local: Java 17.0.20 presente. `./gradlew --version` não chegou a iniciar porque o wrapper precisa baixar Gradle 7.5.1 e a rede do workspace retorna `Network is unreachable`; teste Java/build/smoke aguardam o Actions.
- Nenhum smoke novo ou Actions desta branch foi iniciado ainda no checkpoint.

## Estado do Lote 16 — Mew (#151)

- Mew já está no manifesto e no render smoke dos 151, mas isso não comprova integração do Lote 16. Os caminhos atuais (`assets/minemon/geo/mew`, `textures/pokemon/mew`, `animations/mew`) remontam ao commit inicial `a6ae739` e não têm fonte Cobblemon 1.8.2 nem licença de modelo do Cobblemon registrada. A definição atual usa fallback estático e não contém `air_idle`/`air_fly`; não deve ser tratada como Lote 16 aprovado.
- A auditoria foi ampliada para procurar modelo, texturas, animação e poser de Mew no arquivo Fabric 1.8.2 verificado pelo SHA-256 já usado pelo projeto, além dos mapas de fogo Ponyta/Rapidash. Próxima etapa: consultar o run dessa auditoria, recuperar os arquivos exatos e só então substituir a integração antiga de Mew, com verificação cumulativa e smoke.

## Correções protegidas e pendências

- Preservar: braços do Charizard; asas de Pidgeotto/Pidgeot; corrida de Graveler/Golem; voo do Moltres; Mewtwo; teste opcional de Kabutops; demais recursos Lotes 1–15.
- Ainda não visualmente aprovadas: chamas das caudas Charmander, Charmeleon e Charizard; chamas do corpo Ponyta e Rapidash.
- Avaliação visual manual permanece distinta da aprovação automatizada.

## Próxima ação

1. Publicar este checkpoint e as alterações da adaptação da chama e auditoria da fonte.
2. Acompanhar uma única execução do workflow de build e do source audit disparados pelo commit.
3. Recuperar os recursos oficiais de Mew e fogo suplementar; corrigir integração/render sem alterar corpos ou braços aprovados.
4. Executar testes direcionados e a validação completa antes de declarar qualquer correção ou Lote 16 concluído.
