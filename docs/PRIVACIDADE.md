# Política de privacidade — Supaco

_Última atualização: 27 de setembro de 2026_

O Supaco é um aplicativo **não-oficial**, de código aberto, sem vínculo com o IFRN. Ele existe para mostrar suas faltas e notas do SUAP de um jeito mais prático.

## Resumo

- O app conversa **apenas com o SUAP** (`suap.ifrn.edu.br`), por HTTPS.
- **Não há servidor do Supaco.** Nenhum dado seu é enviado para o desenvolvedor ou para terceiros.
- **Sem anúncios, sem analytics, sem rastreadores.**

## O que o app acessa

| Dado | Para quê | Onde fica |
|---|---|---|
| Matrícula e senha do SUAP | Obter o token de acesso no login | **A senha não é guardada.** Ela é enviada uma única vez ao SUAP no login. |
| Token de acesso do SUAP | Consultar seus dados sem pedir a senha de novo | No aparelho, criptografado (Android Keystore) |
| Perfil (nome, matrícula, campus, curso, foto) | Exibir no app | Cache local no aparelho |
| Boletim (faltas, notas, situação) e horários | Calcular o "posso faltar?", widgets e alertas | Cache local no aparelho |
| Busca de servidores | Consultar contatos de servidores do campus | Não é armazenada |

## Segundo plano e notificações

Se você permitir notificações, o app consulta seu boletim no SUAP algumas vezes por dia para avisar quando uma matéria estiver perto do limite de faltas. Você pode desativar isso nas configurações do app.

## Backup

Somente as preferências visuais (tema, cores) entram no backup automático do Android. Tokens, boletim e horários **não** entram no backup.

## Como apagar seus dados

Toque em **Sair** no app: tokens, cache, histórico e notificações são apagados. Desinstalar o app também remove tudo.

## Contato

Dúvidas ou problemas: abra uma issue em <https://github.com/kellyson71/supaco-mobile/issues>.
