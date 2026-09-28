# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/). Versões seguem [SemVer](https://semver.org/lang/pt-BR/).

## [2.0] — não lançada

> ⚠️ **Novo identificador do app** (`io.github.kellyson71.supaco`) e nova assinatura. Desinstale a versão 1.x antes de instalar a 2.0.

### Corrigido
- **"Posso faltar hoje?" agora conta todas as aulas do dia**: faltar um bloco de 4 aulas custa 4 faltas. Antes, o app podia dizer "pode faltar" quando isso reprovava. O widget de veredito segue a mesma regra.
- A senha deixou de aparecer nos logs do sistema (o log HTTP agora só existe em builds de debug e nunca inclui o corpo).
- Crash no Android 7.x ao criar o canal de notificação.
- Horário fixo de uma turma específica aparecia para outros alunos quando a grade do SUAP falhava.
- Logout e "entrar com outra conta" agora apagam tudo: cache, widgets, notificações, histórico e tarefas agendadas.
- Sessão expirada leva de volta ao login com aviso, em vez de mostrar erros genéricos.
- Várias requisições simultâneas não disparam mais vários refresh de token.
- Alertas de faltas em segundo plano agora consultam o SUAP (antes só reliam dados antigos).
- Na virada de semestre, o boletim antigo não aparece mais como atual.
- Listas longas do SUAP (boletim, períodos, turmas, servidores) são lidas por completo.
- Médias em formato inesperado não quebram mais o boletim inteiro.
- Curso exibido no perfil vinha errado (mostrava o campus).
- "Sincronizado" não aparece mais quando a sincronização falha.
- Imagem de fundo grande podia travar o app (agora é reduzida ao salvar).

### Adicionado
- Modo sério: troca as piadas por mensagens neutras.
- Pedido da permissão de notificação com explicação (Android 13+).
- "Atualizado há X min" e aviso quando os dados são do cache.
- Mensagens de erro claras (senha incorreta, sem internet, SUAP fora do ar...).
- Biometria volta a ser pedida após 5 minutos com o app em segundo plano.
- Contas de servidor recebem aviso de que o app é só para alunos.
- Busca de servidores com "carregar mais".
- Links para código-fonte, política de privacidade e reportar problema.
- Suporte a preenchimento automático de senha no login.

### Removido
- Botão "Entrar com o SUAP" (fazia o mesmo que "Entrar") e o switch "Lembrar de mim" (não fazia nada).
- Opção de idioma inglês (não havia tradução).

### Interno
- R8 ativado: APK de ~17 MB para ~3,5 MB.
- Assinatura de release sem senhas no código (`keystore.properties` ou variáveis de ambiente).
- CI no GitHub Actions (lint, testes, build) e release automatizado por tag.
- 23 testes unitários (veredito do dia, parser de horários, JSON do SUAP, refresh de token).

## [1.8] — 2026-06-17
- Temas, cor personalizada, preto puro (OLED) e fundo de tela.

## [1.7] — 2026-06-10
- Primeira versão pública em APK.
