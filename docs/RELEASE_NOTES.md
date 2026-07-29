# Notas de versão

Dois públicos, dois textos — não misture:

- **Console (usuário):** o campo "O que há de novo" do Play Console aceita **500 caracteres por
  idioma**, e quem lê é o ouvinte, não o desenvolvedor. Refatoração interna não entra; o EFEITO
  dela entra ("menos falhas ao trocar de faixa", não "backoff de 429 no LoadErrorHandlingPolicy").
- **Changelog completo:** a lista técnica por versão, abaixo, para rastreabilidade. É daqui que se
  extrai o texto do Console, nunca o contrário.

---

## 1.2 (versionCode 3) — AAB gerado em 2026-07-29

> Este release foi montado antes como **1.1 / vc2** e o vc2 **nunca chegou ao Console** (a última
> versão ativa lá era o vc1/1.0). O vc3/1.2 tem o mesmo conteúdo funcional — entre os dois só
> entraram o commit de documentação do README e o próprio bump. Ou seja: não existe um "1.1" do
> ponto de vista do usuário, e o texto abaixo é o do 1.2. Pular o vc2 é permitido; o Play só exige
> `versionCode` crescente.
>
> **O vc3 também não chegou a ser enviado** (confirmado pelo dono em 2026-07-29), então o
> `versionCode` foi mantido e o AAB foi **regerado** para incluir a barra inferior (FASE 11) e as
> correções de lint/recomposição. Um AAB de 28/07 que ficou em `app/build/outputs/` ficou obsoleto
> — o `make release` limpa antes de gerar, justamente para não subir sobra.

### Texto para o Console — copiar como está (498/500 caracteres)

```text
Tema escuro: o app acompanha o tema do sistema.

Barra inferior nova: ícones redesenhados, indicador deslizante e a aba "Início" virou "Livros".

Notificação melhor: mostra em que capítulo ou aula você está, com botões de voltar 10s e avançar 30s.

Correções:
• O botão "anterior" do fone/Bluetooth volta a faixa, não reinicia
• Menos falhas ao trocar de faixa rápido, com aviso claro quando a rede falha
• Fechar o app pelos recentes encerra o áudio
• Áreas de toque maiores em Favoritos e Estudos
```

> **O limite de 500 é por idioma e é apertado.** Este texto está a 2 caracteres do teto, então
> qualquer item novo obriga a encurtar outro — foi o que aconteceu aqui (a barra inferior entrou e
> três linhas foram enxugadas). Se o Console reclamar do tamanho, corte da lista de correções, de
> baixo para cima: o que o usuário nota primeiro está no topo.

### O que ficou de fora do texto do Console, e por quê

| Mudança | Por que não entra |
|---|---|
| Scrim único da status bar (ISSUE 9.G/10.x) | Ajuste fino de contraste; o usuário não sabe que existia faixa dupla |
| ExoPlayer deixou de ser `@Singleton` | Interno; o efeito (não travar ao retomar) já é invisível de tão básico |
| Migrações Room explícitas | Interno; o efeito é NÃO perder favoritos — só apareceria se quebrasse |
| `MediaContentId`, `Resource<T>`, tokens de cor | Refatoração; zero superfície para o usuário |
| `offset` por lambda na pílula (`4aa1794`) | Interno; o efeito (barra fluida) já era o esperado |
| Lint destravado (`85136d4`) | Ferramenta de desenvolvimento; nenhuma superfície no app |
| Atualizações de `CLAUDE.md` / plano | Documentação |

### Changelog completo (23 commits desde `dcbee84`, o vc1)

**Barra inferior (FASE 11, entrou depois do AAB de 27/07)**
- `2fd316c` — ISSUE 11.A: par contorno/preenchido em cada aba (padrão Material 3); Bíblia
  desenhada à mão para "Livros" (o Material Symbols não tem o glifo); Temas passa a lótus (`Spa`)
  e Estudos a livro aberto (`MenuBook`); aba "Início" renomeada para **"Livros"** — era o único
  rótulo que nomeava posição em vez de conteúdo, e a ROTA não mudou; indicador que **desliza**
  entre as abas em vez de sumir e reaparecer.
- `4090469` — ajuste do desenho do ícone a pedido do dono: lombada removida (a 26dp virava borrão),
  cruz recentrada, capa menos magra, cantos arredondados.
- `4aa1794` — a pílula do indicador usa o `offset` por lambda: a barra deixa de recompor a cada
  quadro do deslize (`mutableFloatStateOf` nas coordenadas medidas, de passagem).
- `85136d4` — **o `:app:lint` voltou a rodar**: o detector do navigation-compose 2.8.5 lançava
  `NoClassDefFoundError` e abortava o driver inteiro, ou seja o lint estava cego. Uma regra
  desligada, cobertura perdida zero (ver comentário em `app/build.gradle.kts`).

**Conteúdo e aparência**
- `eb87882`, `c95a3f5`, `6949bcf` — tema escuro completo, com contrastes medidos (WCAG) nos dois
  temas; corrige selo de número e conteúdo sobre o dourado, que ficavam ilegíveis no escuro.
- `e2ca9be` — scrim ÚNICO da status bar, calibrado no limite de legibilidade: eram dois véus
  empilhados (14,5:1 medidos onde 4,5:1 basta), o que virava faixa clara sobre foto escura.
- `ea62b71`, `7afdd4a` — capa de estudo em retângulo arredondado; título com elipse em 2 linhas.
- `d3f8704` — nome do livro em uma linha na Home; ripple restrito à capa.
- `f34bf4f` — aba "Mais": versão correta do app no rodapé e agrupamento dos itens jurídicos.
- `c511462` — cores por token M3 em vez de hex no call site.
- `6ea6925` — alvos de toque abaixo de 48dp corrigidos em Favoritos e Estudos (a11y).

**Player e notificação**
- `1fc434b` — ISSUES 10.A/10.B: a 2ª linha da notificação passa a dizer o contexto real
  (`Capítulo 1 de 50`, `Aula 2 de 28 · <título>`, `<momento> · <referência>`) em vez do nome do
  app; botões −10s/+30s nas superfícies de mídia (sombra, tela de bloqueio, Auto).
- `d68b1f3` — botão "anterior" de Bluetooth/fone reiniciava o capítulo em vez de voltar faixa.
- `01029a6` — swipe nos recentes encerra o playback (antes o áudio continuava sem controle).
- `f020a86` — backoff de 429 no áudio e mensagem de erro por causa (rede x servidor x conteúdo).

**Rede e sincronização**
- `bc77e60` — retry de 429 no cliente JSON e mensagem humana quando o sync falha.

**Documentação (sem efeito no app)**
- `562b692`, `52f94f7`, `42a3586` — `CLAUDE.md` auditado e corrigido; FASE 10 registrada no plano.

### Como gerar e enviar

```bash
make release                 # limpa, roda testes+lint, gera o AAB assinado e confere a chave
make verify                  # só a conferência da assinatura
```

O `verify` compara a impressão digital do assinador com a chave de upload travada no
`Makefile` (`EXPECTED_SHA256`, começa em `84:2D:3A:33`) e **falha** se não bater — assinar com a
chave errada é um erro que só apareceria no upload, depois de todo o build.

Depois, no Console: **Teste interno → Criar nova versão**, subir
`app/build/outputs/bundle/release/app-release.aab` e colar o texto acima em "O que há de novo".

**Declaração de foreground service:** já enviada no Console na versão anterior (PUB-23) e **não
precisa ser reenviada** — a declaração é do app, não da versão, e vale enquanto os tipos
declarados não mudarem. Conferido para este release: o `AndroidManifest.xml` está byte-idêntico
ao do vc1, com `mediaPlayback` como único tipo. Se um dia entrar um segundo tipo de FGS, aí sim a
declaração tem de ser atualizada.

Pendências de publicação que **não** são resolvidas por este release (ver FASE 8 do
`PLANO_FASES_E_ISSUES.md`): placeholders de Estudos no servidor (+ recaptura do screenshot
`04_estudos`) e os testes no release PUB-11/12/13/16.
