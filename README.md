## 🎵 Arquitetura do Player: Ciclo de Vida e Background

### 🚧 Estado Atual: Estratégia "Clean Exit" (Limpeza Total)

Atualmente, o aplicativo adota o comportamento de encerrar o serviço de áudio e remover a
notificação imediatamente quando o usuário encerra o app (swipe/arrastar) da lista de aplicativos
recentes (Overview Screen).

**Comportamento:**

1. Usuário abre a lista de apps recentes e fecha o *Ouvindo a Bíblia*.
2. O sistema chama `onTaskRemoved` no `PlaybackService`.
3. O player para (`stop`), a notificação é removida (`STOP_FOREGROUND_REMOVE`) e o serviço se
   autodestrói (`stopSelf`).

**Por que essa decisão foi tomada?**

* **Prevenção de "Notificações Zumbis":** Evita que a notificação de mídia permaneça ativa após o
  sistema Android matar o processo do app para economizar bateria. Sem persistência de estado,
  clicar no "Play" dessa notificação horas depois não funcionaria, gerando frustração.
* **Simplicidade Inicial:** Foca na estabilidade do streaming sem a complexidade de gerenciar banco
  de dados local para salvar o milissegundo exato de cada faixa.

**Onde está o código responsável?**

* Arquivo: `PlaybackService.kt`
* Método: `onTaskRemoved(rootIntent: Intent?)`

---

### 🎯 Meta Futura: Estratégia "Spotify-like" (Persistência e Retomada)

O objetivo final é permitir que a notificação permaneça ativa e funcional mesmo após o app ser
fechado ou o processo ser morto pelo sistema, permitindo a retomada ("Resumption") a qualquer
momento.

**Requisitos para Implementação (Roadmap):**

1.  [ ] **Persistência de Estado (Room Database):**
    * Criar tabela para salvar: `lastBookId`, `lastChapterIndex`, `currentPositionMs`, `artworkUrl`.
    * Salvar esses dados periodicamente (a cada X segundos ou no `onPause`).

2.  [ ] **Implementar `onPlaybackResumption`:**
    * No `PlaybackService.kt`, substituir o retorno de erro atual por uma lógica que lê o Banco de
      Dados.
    * Reconstruir a `MediaItem` e configurar o `player.seekTo()` com a posição salva.

3.  [ ] **Remover o "Kill-switch":**
    * Remover a chamada `stopSelf()` e `stopForeground()` do método `onTaskRemoved`.
    * Permitir que o serviço rode até que o sistema decida matá-lo, confiando que o
      `onPlaybackResumption` restaurará o estado quando o usuário voltar.

---