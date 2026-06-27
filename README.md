# Ouvindo a Bíblia

App Android nativo de áudio (Bíblia falada, momentos temáticos e estudos) — Jetpack Compose, Media3/ExoPlayer, Hilt, Room, multi-módulo Gradle. Código e textos em pt-BR.

## Documentação viva

A referência atual do projeto está nestes arquivos (os planos antigos foram arquivados em `docs/archive/`):

- **`CLAUDE.md`** — visão geral de arquitetura, módulos e comandos de build.
- **`DIAGNOSTICO_01_ARQUITETURA.md`** — auditoria de build e integridade arquitetural.
- **`DIAGNOSTICO_02_PLAYER.md`** — auditoria detalhada da camada de reprodução.
- **`ROADMAP.md`** — trabalho restante, priorizado, derivado dos diagnósticos.

## Player: ciclo de vida e background (estado atual)

O `PlaybackService` (`MediaLibraryService`) roda como **serviço persistente** no modelo "tipo Spotify":

- O `ExoPlayer` **pertence ao serviço** (não é mais `@Singleton`); é liberado uma única vez no `onDestroy` real.
- Ao começar a tocar, o serviço é promovido a **started + foreground** (`startForegroundService`), então **sobrevive** ao app ir para segundo plano ou à `MainActivity` ser destruída — o áudio continua e a notificação permanece.
- `onTaskRemoved` apenas **persiste o estado** (não para nem libera o player).
- A posição de reprodução é salva no Room (`PlaybackStateEntity`) e restaurada no `onCreate` **sem auto-play**.

Detalhes e itens ainda abertos do player (Cast, save periódico, notificação no estado pausado, `onPlaybackResumption`) estão em `DIAGNOSTICO_02_PLAYER.md` e `ROADMAP.md`.

> Nota: a estratégia anterior de "Clean Exit" (encerrar o serviço no swipe) foi **substituída** pelo modelo persistente acima.
