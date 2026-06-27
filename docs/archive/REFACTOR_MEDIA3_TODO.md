# TODO: Refatoração do Motor de Áudio (Media3 + Clean Architecture)

**Status:** ⏸️ Em Backlog (Aguardando conclusão da feature de Temas)
**Epic:** Infraestrutura de Player & Persistência

## 📝 Contexto e Motivação

A implementação atual do `BiblePlaybackService` e do `BibleMediaLibrarySessionCallback` foi baseada
no projeto de demonstração do Google. Isso gerou dois problemas arquiteturais graves para um app de
produção:

1. **God Object em Memória:** O `BibleMediaItemTree` carrega a árvore inteira da Bíblia (1.189
   capítulos) na RAM usando um Singleton estático.
2. **Acoplamento de Lógica:** O `BibleMediaLibrarySessionCallback` está misturando a
   responsabilidade de "rotear eventos do player" com "consultar/montar playlists e metadados".

**Objetivo:** Eliminar o Singleton em memória, adotar o padrão Clean Architecture com Hilt, usar o
Room como "Fonte da Verdade" e delegar a lógica de negócios para UseCases.

---

## ✅ Checklist de Implementação

### Fase 1: Camada de Dados (Room)

- [ ] Criar `BookEntity` e `ChapterEntity` no Room para mapear a estrutura do `bible_index.json`.
- [ ] Criar o `BibleDao` com queries para buscar capítulos por ID de livro (
  `SELECT * FROM chapters WHERE book_id = :bookId`).
- [ ] Configurar a inicialização/população do banco de dados na primeira abertura do app.

### Fase 2: Repositório e Mappers

- [ ] Criar a interface `BibleRepository` e sua implementação `BibleRepositoryImpl` para isolar o
  acesso ao DAO.
- [ ] Criar o `MediaItemMapper` (funções de extensão puras) para converter entidades de domínio em
  instâncias de `MediaItem`.
    - *Exemplo:* `fun ChapterEntity.toMediaItem(): MediaItem` configurando URIs, capa (`artworkUri`)
      e metadados.

### Fase 3: Casos de Uso (Domain Layer)

- [ ] Criar o `ResolveMediaItemsUseCase`.
    - **Responsabilidade:** Receber uma lista de requisições (`List<MediaItem>`), identificar se o
      usuário pediu um livro inteiro (pasta) ou um capítulo avulso, consultar o `BibleRepository` e
      devolver a lista expandida e hidratada com as URLs corretas do formato OGG Vorbis.

### Fase 4: Refatoração do Media3 (Serviço e Callback)

- [ ] Atualizar o módulo Hilt para prover as dependências de Repositório e UseCases.
- [ ] Injetar o `ResolveMediaItemsUseCase` dentro do `BibleMediaLibrarySessionCallback`.
- [ ] Refatorar a função `onSetMediaItems` para chamar o UseCase ao invés de usar a árvore estática.
- [ ] Refatorar a função `onAddMediaItems` para usar a mesma lógica do UseCase.
- [ ] Implementar a ponte assíncrona segura entre Coroutines (do Room/UseCase) e o Guava
  `ListenableFuture` exigido pelo Media3 usando `kotlinx-coroutines-guava`.
- [ ] **Deletar/Limpar** o `BibleMediaItemTree` antigo.

---

## ⚖️ Trade-offs Registrados (Para Reuniões de Arquitetura)

* **Ganhamos:** Economia massiva de memória RAM, fim de vazamentos de memória ligados ao Singleton,
  arquitetura testável e preparada para features de longo prazo (Histórico, Favoritos, "Continue
  Ouvindo").
* **Pagamos:** Custo de latência de disco (I/O) ao consultar o Room SQLite (alguns milissegundos)
  comparado à leitura instantânea na RAM, o que é imperceptível para o usuário final, mas exige
  chamadas assíncronas estritas.