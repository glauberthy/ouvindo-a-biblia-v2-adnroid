# Texto ATUALIZADO para a seção "privacy" do mais.json (servidor)

> **Handoff para o dono (2026-07-18).** A seção "Política de privacidade" exibida DENTRO do app
> (sheet da tela Mais) vem do `mais.json` (seção `id: "privacy"`, `type: long_text`) e está
> DESALINHADA da política oficial publicada (`politica.html`, espelhada em
> `docs/POLITICA_DE_PRIVACIDADE.md`): falta responsável/contato, o item do IP/Cloudflare,
> permissões, crianças e HTTPS.
>
> **Como aplicar:** (1) substituir o `content.text` da seção `privacy` pelo texto abaixo
> (é uma string única; as quebras de parágrafo são linhas em branco); (2) **bumpar o `version`
> da raiz do `mais.json`** (hoje `1.0.13` → ex.: `1.0.14`) — sem o bump o app NÃO re-sincroniza
> (version-gating da ISSUE 6.E); (3) abrir a tela Mais no app para conferir.

---

**Política de privacidade**

No **Ouvindo a Bíblia**, a privacidade do usuário é tratada com simplicidade, respeito e transparência. **Não criamos conta, não pedimos login e não coletamos dados pessoais** para nossos próprios fins. O app não contém anúncios nem ferramentas de rastreamento/analytics.

**Uso sem cadastro**

O aplicativo pode ser utilizado sem criação de conta, sem login e sem envio de dados pessoais para acesso aos conteúdos disponibilizados. Não usamos identificador de publicidade (Advertising ID) nem SDKs de anúncios, analytics ou rastreamento de terceiros.

**Dados processados para o funcionamento**

Para baixar o conteúdo e reproduzir os áudios, o aplicativo faz requisições pela internet ao servidor do próprio app. Como em qualquer acesso à internet, essas requisições transmitem automaticamente o endereço IP do dispositivo e um identificador técnico do app. **Nós não coletamos, não armazenamos e não acessamos** esses dados; o provedor de infraestrutura (Cloudflare) pode processá-los automaticamente para entregar o conteúdo e para as próprias medidas de segurança da rede, conforme a política de privacidade desse provedor.

**Informações mantidas no próprio dispositivo**

Favoritos, posição de reprodução ("continuar ouvindo") e preferências técnicas de sincronização ficam armazenados **apenas no seu aparelho**, no espaço privado do app, e são apagados ao desinstalá-lo.

**Permissões e por quê**

Internet (baixar conteúdo e reproduzir em streaming); serviço em primeiro plano de mídia (manter o áudio tocando com a tela desligada); notificações (controles de reprodução na notificação e tela de bloqueio — se você negar, o áudio segue funcionando, apenas sem esses controles); wake lock (não interromper a reprodução com a tela apagada).

**Compartilhamento com terceiros**

Não vendemos nem compartilhamos dados pessoais. O app se comunica apenas com o servidor de conteúdo do próprio aplicativo.

**Crianças**

O aplicativo é destinado ao público geral e não é direcionado especificamente a crianças. Não coletamos conscientemente dados de crianças.

**Segurança**

As comunicações com o servidor usam conexão criptografada (HTTPS).

**Política completa e contato**

A versão completa e vigente desta política está publicada em https://ouvindo-a-biblia.ide.app.br/politica.html (responsável: IDETECH LTDA). Dúvidas sobre privacidade: contato@ide.app.br.
