# Política de Privacidade — Ouvindo a Bíblia

> **RASCUNHO (PUB-20).** Redigido a partir do comportamento real do app (auditoria em
> `docs/archive/AUDITORIA_04_PLAYSTORE.md`). Antes de publicar: (1) preencha os campos `[...]`;
> (2) confirme o item "Logs do servidor" com quem administra `ouvindo-a-biblia.ide.app.br`;
> (3) hospede numa URL pública e informe-a no Play Console (App content → Privacy policy).
> Isto é um modelo, não aconselhamento jurídico.

**Última atualização:** [DATA — ex.: 16 de julho de 2026]
**Aplicativo:** Ouvindo a Bíblia (Android)
**Desenvolvedor/Responsável:** [SEU NOME OU NOME DA ORGANIZAÇÃO]
**Contato:** [E-MAIL DE CONTATO]

## 1. Resumo

O aplicativo **Ouvindo a Bíblia** é um player de áudio (Bíblia falada, momentos temáticos e
estudos). **Não criamos conta, não pedimos login e não coletamos dados pessoais** para nossos
próprios fins. O app não contém anúncios nem ferramentas de rastreamento/analytics.

## 2. Dados que o aplicativo NÃO coleta

- Não solicitamos nome, e-mail, telefone, localização, contatos, fotos, microfone ou câmera.
- Não há cadastro, login ou perfil de usuário.
- Não usamos SDKs de anúncios, analytics ou rastreamento de terceiros.
- Não usamos identificador de publicidade (Advertising ID).

## 3. Dados processados para o funcionamento

Para baixar o conteúdo (textos e listas em formato JSON) e reproduzir os áudios, o aplicativo faz
requisições pela internet ao servidor `https://ouvindo-a-biblia.ide.app.br/` e aos endereços de
áudio indicados por ele. Como em qualquer acesso à internet, essas requisições transmitem
automaticamente o **endereço IP** do dispositivo e um identificador técnico do app
(cabeçalho *User-Agent*). Esses dados são usados **exclusivamente** para atender à requisição
(entregar o conteúdo/áudio) e **não são vinculados** à sua identidade.

> **[CONFIRMAR — Logs do servidor]** Se o servidor `ouvindo-a-biblia.ide.app.br` **não** registra
> logs de acesso, mantenha a redação acima. Se **registra** logs (IP/data/hora, como é comum em
> servidores web/CDN), acrescente: *"Nosso servidor pode manter registros de acesso (endereço IP,
> data e hora) por [PERÍODO] para fins de segurança e operação, sem vinculá-los à identidade do
> usuário."* — e reflita isso no formulário Data Safety.

## 4. Dados armazenados no seu dispositivo (não enviados)

O app guarda **apenas localmente** (não sai do aparelho):

- Seus **favoritos** (capítulos e lições marcados);
- A **posição de reprodução** ("continuar ouvindo");
- Preferências técnicas de sincronização de conteúdo.

Esses dados ficam no armazenamento privado do app e são apagados ao desinstalar o aplicativo.

## 5. Permissões e por quê

- **Internet / Estado da rede:** baixar o conteúdo e reproduzir o áudio em streaming.
- **Serviço em primeiro plano (reprodução de mídia):** manter o áudio tocando com a tela desligada
  ou o app em segundo plano.
- **Notificações:** exibir os controles de reprodução (tocar/pausar/próximo) na notificação e na
  tela de bloqueio. Se você negar, o áudio continua funcionando, apenas sem esses controles.
- **Wake lock:** evitar que o dispositivo interrompa a reprodução com a tela apagada.

## 6. Compartilhamento com terceiros

Não vendemos nem compartilhamos dados pessoais. O app se comunica apenas com o servidor de conteúdo
do próprio aplicativo, para a finalidade descrita no item 3.

## 7. Crianças

O aplicativo é destinado ao público geral e não é direcionado especificamente a crianças. Não
coletamos conscientemente dados de crianças.

## 8. Segurança

As comunicações com o servidor usam conexão criptografada (HTTPS).

## 9. Alterações nesta política

Podemos atualizar esta política; a data de "Última atualização" no topo indicará a versão vigente.

## 10. Contato

Dúvidas sobre privacidade: [E-MAIL DE CONTATO].
