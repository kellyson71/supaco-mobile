# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/). Versões seguem [SemVer](https://semver.org/lang/pt-BR/).

## [2.0] — 2026-09-28

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

### Movimento e interação
- Sistema de movimento com três personalidades (viva, calma, firme): quanto pior o status, mais lento e pesado.
- Vocabulário de vibração consistente (tique, alegre, pesado, confirmar, rejeitar).
- **Formas vivas**: as formas orgânicas respiram, giram devagar, murcham conforme o risco e se transformam umas nas outras.
- **Veredito em três atos**: o dado para na face certa (6 = pode faltar, 1 = nem pense), a cor se espalha a partir dele, a forma vira a do status e a barra de "gasto do dia" mostra as aulas de hoje consumindo as faltas livres. O suspense só acontece na primeira consulta do dia; tocar pula.
- Home: frase do dia, pull-to-refresh temático, skeleton no formato real, contagem até a próxima aula, aviso de faltas novas e semana com indicador deslizante e swipe.
- Matérias: filtros e busca sem "pulos", troca de período com direção, selo "falta nova" e barras que enchem na primeira vez.
- Detalhe: anel de frequência animado; no simulador, as pétalas murcham a cada falta e a flor vira pedra ao passar do limite; a média simulada pula ao cruzar 60.
- Horários: timeline acompanha o relógio (aula atual pulsando, trecho vivido preenchido, aulas passadas esmaecidas).
- Perfil: anel de frequência geral, chama do streak que cresce, rank com efeito de carimbo.
- Conquistas: anúncio quando uma nova é desbloqueada, títulos ocultos embaralhados, giro ao tocar.
- **Resumo do semestre** em telas estilo stories, com imagem final para compartilhar.
- Login com entrada em cascata, flor girando enquanto valida e campos que chacoalham na senha errada.
- Troca de tema com transição suave de cores; fundo de tela entra com fade.
- Tile com o veredito do dia no subtítulo; widget de veredito abre direto o veredito; notificação com botão "Ver matéria".
- Opção **Reduzir animações** (também segue a configuração do sistema).

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
