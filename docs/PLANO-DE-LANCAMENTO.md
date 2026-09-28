# Plano de lançamento — Supaco Mobile

> Auditoria feita em 27/09/2026 sobre o commit `25e616c` (v1.8). Fontes: leitura de todo o código em `app/src`, `./gradlew testDebugUnitTest lintDebug`, inspeção do APK `docs/supaco-1.8.apk` e comparação com a especificação oficial `https://suap.ifrn.edu.br/api/openapi.json`.

## Status (branch `release/2.0`)

Distribuição decidida: **só APK** (GitHub Releases). Itens exclusivos da Play Store (AAB, Data safety, teste fechado) ficam para depois.

| Item | Status |
|---|---|
| 1.1–1.8 Bloqueadores | ✅ Corrigidos. Nova assinatura: gere o keystore da 2.0 (ver README) |
| 2.1–2.4, 2.6–2.9 | ✅ Corrigidos |
| 2.5 Validar a regra dos 25% com boletins reais | ⏳ **Manual**: comparar com `percentual_carga_horaria_frequentada` |
| 3.1, 3.3, 3.4, 3.8 | ✅ (política em `docs/PRIVACIDADE.md`, backup sem dados sensíveis, HTTPS obrigatório, backoff no Worker) |
| 3.7 Crash reporting | ⏸ Adiado: exige serviço externo; hoje há "Reportar problema" (issue no GitHub) e mapping do R8 em cada release |
| 4.1, 4.6, 4.7 parcial | ✅ R8 (3,5 MB), dependência morta removida, catálogo de versões |
| 4.2, 4.3, 4.5 | ⏸ P2: ícones enxutos, baseline profile, upgrade de dependências (Dependabot configurado) |
| 5.2, 5.5, 5.6 | ✅ Modo sério, "atualizado há X", seção Sobre |
| 5.1 Onboarding | ◑ Parcial: aviso de não-oficial no login e pedido de notificação com explicação |
| 5.3 Acessibilidade (TalkBack/contraste) | ⏳ Revisão manual pendente |
| 6.1–6.4 Repositório, CI, testes | ✅ LICENSE, CHANGELOG, SECURITY, CONTRIBUTING, templates, CI, release por tag, 23 testes |
| 6.5 Arquitetura | ⏸ P2 |
| 7.x Novos endpoints | ⏸ Roadmap pós-lançamento |

## Resumo

O app tem boa base (Compose + M3, cache offline, widgets, tile, temas) e os 7 testes unitários passam. Mesmo assim, **não está pronto para lançamento público**. Há problemas que:

- **vazam a senha do aluno** (log HTTP com o corpo da requisição em release);
- **derrubam o app no Android 7.x** (lint falha com 4 erros `NewApi`);
- **dão a resposta errada à pergunta principal do app** ("posso faltar hoje?" ignora quantas aulas o aluno perde no dia);
- **impedem a publicação na Play Store** (`applicationId = com.example.*`).

Prioridades:

| Nível | Significado |
|---|---|
| **P0** | Bloqueia o lançamento. Corrigir antes de qualquer divulgação. |
| **P1** | Bug real ou risco alto. Corrigir antes da v2.0 pública. |
| **P2** | Qualidade/polimento. Ideal no lançamento, aceitável logo depois. |
| **P3** | Evolução/roadmap pós-lançamento. |

---

## 1. Bloqueadores (P0)

### 1.1 Senha do aluno aparece no logcat
`app/src/main/java/com/example/supacomobile/di/AppModule.kt:54` — `HttpLoggingInterceptor.Level.BODY` vale também no build de release. O `POST /api/token/pair` registra `{"username": "...", "password": "..."}` em texto puro, além de todos os tokens e do boletim.
**Correção:** ativar `buildConfig = true`, só adicionar o interceptor com `if (BuildConfig.DEBUG)` e usar `redactHeader("Authorization")`. Nunca logar corpo no endpoint de token.

### 1.2 Crash no Android 7.0/7.1 (minSdk 24)
`notifications/FaltasNotifier.kt:31-38` — `NotificationChannel` só existe a partir da API 26. O lint já aponta isso como **erro**. Qualquer sync com matéria perigosa em Android 7 dispara `NoClassDefFoundError`.
**Correção:** envolver em `if (Build.VERSION.SDK_INT >= O)` ou usar `NotificationChannelCompat` + `NotificationManagerCompat`. Alternativa: subir o `minSdk` para 26 (Android 8), o que hoje cobre >97% dos aparelhos e simplifica o código.

### 1.3 "Posso faltar hoje?" dá resposta errada
- `ui/dashboard/AbsenceLogic.kt:46-56` (`calcStatus`) avalia "sobra 1 falta = LAST", mas **faltar um dia custa N faltas**, uma por aula do bloco (ex.: `3V1234` = 4 aulas = 4 faltas). Com 2 faltas restantes e 4 aulas hoje, o app diz "CALMA AÍ, dá pra faltar" e o aluno reprova.
- `data/ScheduleData.kt:46` (`parseSuapHorario`) junta os blocos num intervalo e **descarta a quantidade de aulas**.
- `ui/dashboard/tabs/HomeTab.kt:49` pega só a **primeira** matéria do dia, não a pior. Sem aula no dia (fim de semana), pega `materias.firstOrNull()` e mostra como "Sua matéria mais crítica", o que não é verdade.
- `widget/SupacoVerdictWidget.kt:53` usa a pior matéria **geral**, então o widget e o app podem dar vereditos diferentes.

**Correção:** guardar `qtdAulas` em `ScheduleEntry`/`HorarioEntity` (é a quantidade de dígitos do token). Criar uma função pura `vereditoDoDia(materiasDeHoje)` que, para cada matéria, calcula `restantes - aulasHoje` e retorna a pior. Usar essa mesma função no Home, no overlay, no widget e no tile. Cobrir com testes.

### 1.4 Horário pessoal hardcoded vaza para outros usuários
`data/ScheduleData.kt:15-20` — `BY_SIGLA` tem a grade (e nomes de professores) de uma turma específica como "fallback". Se a API de turmas falhar, **qualquer aluno com as siglas TEC.0017/0012/0005/0035 vê horário, sala e professor errados**. Isso é usado em `AbsenceLogic.kt:91`, `AcademicRepository.kt:98` e `HorariosWidget.kt:120`.
**Correção:** remover o fallback. Sem grade, mostrar "Horário indisponível" e seguir sem ele.

### 1.5 `applicationId = com.example.supacomobile`
`app/build.gradle.kts:10,13` — a Play Store **rejeita** o prefixo `com.example`, e trocar o ID depois do lançamento cria "outro app".
**Correção:** definir o ID final agora (ex.: `io.github.kellyson71.supaco` ou `br.dev.kellyson.supaco`) e renomear também o pacote Kotlin.
⚠️ Quem instalou pelo APK (v1.x) **não recebe atualização** para um ID novo: o Android trata como outro app. Avise no README/release que é preciso desinstalar a versão antiga.

### 1.6 Assinatura de release com senha padrão no repositório público
`app/build.gradle.kts:25-27` — fallback `"supaco123"` para a senha do keystore e da chave. Se o `release.keystore` real usa essa senha, basta o arquivo vazar para qualquer pessoa assinar "atualizações".
**Correção:** remover os fallbacks e falhar o build de release se as variáveis de ambiente não existirem. Ler de `keystore.properties` (já está no `.gitignore`) ou de secrets do CI. Se a chave atual usa `supaco123`, gerar uma nova junto com a troca de `applicationId`. Na Play Store, usar **Play App Signing** e guardar a *upload key* fora do repositório, com backup.

### 1.7 Logout não apaga os dados do usuário anterior
- `data/repository/AuthRepository.kt:25` apaga só os tokens e o Room.
- Ficam para trás: `supaco_settings` (cache de períodos `cached_periodos_letivos`), `faltas_history` (streak), `faltas_notified` (dedupe), o arquivo de fundo, os **widgets** (continuam mostrando o boletim antigo até o próximo update) e o `FaltasWorker` agendado.
- `MainActivity.kt:180` ("Entrar com matrícula e senha" na tela bloqueada) chama só `tokenManager.clear()`, **sem limpar o banco**. Outra pessoa logando no mesmo aparelho vê dados misturados.

**Correção:** um único `SessionManager.logout()` que limpa tudo, cancela o Worker e chama `updateAll()` nos 3 widgets. Os dois caminhos devem usar esse método.

### 1.8 Sessão expirada não leva ao login
`data/remote/TokenAuthenticator.kt` limpa os tokens quando o refresh falha, mas a UI continua no dashboard (o perfil vem do cache) mostrando "Erro ao buscar boletim". Além disso:
- `data/model/Auth.kt:14` — `access` é `String` não nulo, mas o spec (`TokenRefreshOutputSchema`) diz que `access` pode ser **null**, o que faria a desserialização quebrar;
- `AuthRepository.kt:18` salva refresh `""` quando ele não vem;
- `TokenAuthenticator.kt:32` cria um `OkHttpClient()` novo a cada 401, sem timeout e sem sincronização. Duas requisições paralelas disparam dois refresh (corrida que pode invalidar o refresh token);
- não há limite de tentativas (`responseCount`), então o risco de loop fica com o OkHttp.

**Correção:** expor um `StateFlow<SessionState>` (Autenticado/Expirado) observado pela `MainActivity` para navegar ao login com a mensagem "Sua sessão expirou". Refresh dentro de `synchronized`/`Mutex`, conferindo se outro thread já renovou. Usar um cliente dedicado (sem o authenticator) injetado via Koin.

---

## 2. Correções importantes (P1)

### 2.1 Notificações em segundo plano não funcionam de verdade
`notifications/FaltasWorker.kt:23` só relê o Room local, **sem buscar dados no SUAP**. Se o aluno não abre o app, as faltas novas nunca geram alerta. A feature "alertas inteligentes" do README hoje é enganosa.
**Correção:** o Worker deve chamar `AcademicRepository` para o período corrente, com `Constraints(NetworkType.CONNECTED)` e backoff exponencial. Retornar `Result.retry()` em erro de rede e não fazer nada se não houver sessão. Frequência de 12h está ok; considerar 6h em dias úteis.

### 2.2 Cache do boletim não sabe de qual período é
`data/repository/AcademicRepository.kt:73-90` — a tabela `boletim` não guarda ano/período. Na virada de semestre o app mostra o boletim **antigo** como atual até o refresh em background terminar. E esse refresh (`CoroutineScope(Dispatchers.IO).launch`, linha 79) **não atualiza a UI**: o usuário vê dado velho até reabrir o app. O mesmo padrão está em `ProfileRepository.kt:23`.
**Correção:** adicionar `anoLetivo`/`periodoLetivo` na entidade. Expor `Flow` do Room (`observeBoletim()` já existe e não é usado) e deixar o ViewModel observar. Nada de escopos soltos: usar `viewModelScope` ou um `applicationScope` injetado.

### 2.3 Paginação ignorada
Todos os endpoints de lista retornam `next`/`previous` (`PagedBoletimSchema`, `PagedMatriculaPeriodoSchema`, `PagedMeuPeriodoSchema`, `PagedServidorSchema`), mas `SuapApi.kt` só lê a primeira página. A busca de servidores é o caso mais visível (resultados cortados); períodos e turmas podem ser cortados em cursos longos.
**Correção:** helper `suspend fun <T> fetchAll(page: suspend (Int) -> Paged<T>)` para boletim/períodos/turmas, e "carregar mais" na busca de servidores.

### 2.4 Modelos divergentes do spec
| Campo | Spec | App | Risco |
|---|---|---|---|
| `media_final_disciplina` | `string \| null` | `Double?` (`Academic.kt:46`) | Um valor não numérico (ex.: `"-"`) **quebra o parse do boletim inteiro** |
| `nota_avaliacao_final` | presente | ignorado | NAF real não aparece no simulador |
| `carga_horaria_cumprida` | presente | ignorado | Dá para mostrar "aulas restantes no semestre" |
| `segundo_semestre` | presente | ignorado | Útil para diários anuais/semestrais |
| `TokenRefreshOutput.access` | `string \| null` | `String` | Ver 1.8 |
| `Profile.curso` | vem de `/api/ensino/meus-dados-aluno/` | recebe `campus` (`Profile.kt:43`) | Curso exibido errado |

**Correção:** tornar os campos tolerantes (`String?` + conversão segura) e adicionar testes de desserialização com JSONs reais anonimizados em `app/src/test/resources/`.

### 2.5 Validar a regra dos 25% com dados reais
`calcStatus` usa `floor(carga_horaria * 0.25)` e compara com `numero_faltas`. O SUAP já devolve `percentual_carga_horaria_frequentada`. Antes do lançamento, **compare o cálculo do app com o percentual do SUAP** em 3 ou 4 boletins reais (técnico integrado, subsequente e superior). Se `carga_horaria` estiver em horas-relógio e as faltas em hora-aula (45 min), o limite sai errado. Considerar também `total_abonos` de `/api/ensino/frequencia-periodo-letivo/{ano}/{periodo}/`, já que faltas abonadas não contam.

### 2.6 Usuários que não são alunos
Servidores e professores também conseguem logar (`tipo_usuario`). Hoje eles caem em "Erro ao buscar períodos letivos". **Correção:** checar `tipo_usuario` no login e mostrar "O Supaco é só para alunos" (ou esconder as abas acadêmicas).

### 2.7 Login
- `ui/auth/AuthViewModel.kt:41` mostra `exception.message` cru ("HTTP 401 ", "Unable to resolve host…"). Mapear para: credenciais inválidas (401), sem internet (`IOException`), SUAP fora do ar (5xx), muitas tentativas (429).
- `ui/auth/LoginScreen.kt:52,137` — o switch **"Lembrar de mim" não faz nada**.
- `LoginScreen.kt:204` — o botão **"Entrar com o SUAP" faz exatamente o mesmo que "Entrar"**. Isso engana o usuário e pode ser lido como imitação de login oficial na revisão da loja. Remover (ou implementar OAuth de verdade, ver 7.3).
- Falta suporte a autofill/gerenciador de senhas (`Modifier.semantics { contentType = ContentType.Username/Password }`) e ação do teclado (`ImeAction.Next`/`Done`).

### 2.8 Idioma inglês que não existe
`ui/settings/SettingsScreen.kt:176-225` + `res/xml/locales_config.xml` oferecem "English (United States)", mas **não existe `values-en/`** e todos os textos estão hardcoded em PT nos composables. Trocar o idioma não muda nada. **Correção:** remover a opção agora e internacionalizar depois (P3).

### 2.9 Menores
- `ui/dashboard/DashboardViewModel.kt:114` — `sync()` mostra "Boletim sincronizado com o SUAP." **mesmo quando falha**, porque `fetchData` roda num `launch` separado e o `delay(800)` é fixo.
- Erros só aparecem na aba Início. Nas abas Matérias/Horários, uma falha mostra lista vazia sem explicação.
- `MainActivity.kt:123` — `BitmapFactory.decodeFile` roda na thread principal, sem `inSampleSize`. Foto de 12 MP → ~48 MB de bitmap → risco de OOM/ANR. Reduzir na hora de salvar (`SettingsManager.saveBackgroundFromUri`) e decodificar fora da main thread.
- `MateriaDetailSheet.kt:197` divide por `materia.total`. Com `carga_horaria = 0` o resultado é `NaN`.
- `di/AppModule.kt:38` — `fallbackToDestructiveMigration()` está ok para cache, mas ative `exportSchema = true` e escreva migrações a partir da v2.0, quando começar a haver dados locais do usuário.
- Biometria só é pedida no `onCreate`. Voltar do background não bloqueia de novo. Considerar travar após X minutos em background.
- `security-crypto 1.1.0-alpha06` está **deprecado** pelo Google e tem crashes conhecidos de Keystore em alguns aparelhos. Envolver `TokenManager` em `try/catch` que recria as prefs (forçando novo login) em vez de crashar. Médio prazo: DataStore + Tink.

---

## 3. Segurança, privacidade e conformidade

| # | Item | Prioridade |
|---|---|---|
| 3.1 | **Política de privacidade** publicada (GitHub Pages serve) e acessível no app. A Play Store exige para apps que tratam dados pessoais e credenciais. | P0 (se for para a Play Store) |
| 3.2 | Preencher o **Data safety** da Play Store: coleta credenciais (só para autenticar, não armazena a senha), dados acadêmicos em cache local, sem compartilhamento. | P0 (Play Store) |
| 3.3 | Corrigir o README: "Suas credenciais ficam guardadas com EncryptedSharedPreferences" está impreciso. A **senha não é guardada**, só os tokens. O banco Room (notas/faltas) **não é criptografado** e entra no backup do Google (`backup_rules.xml`). Ou se exclui o banco do backup, ou se documenta isso. | P1 |
| 3.4 | `network_security_config.xml` com `cleartextTrafficPermitted="false"`. Pinning é opcional; se usar, precisa de plano de rotação. | P2 |
| 3.5 | Deixar claro na loja e no app: **"não-oficial, sem vínculo com o IFRN"**. Evitar logo do IFRN/SUAP no ícone e nas screenshots para não cair em política de *impersonation*. | P0 (Play Store) |
| 3.6 | Considerar avisar a COTIC/DIGTI do IFRN antes da divulgação ampla (uso da API por muitos alunos). Reduz o risco de bloqueio e pode abrir OAuth oficial. | P2 |
| 3.7 | Crash reporting **opt-in** (Sentry, ou ACRA com endpoint próprio), já que o README promete "sem analytics". Sem isso, você lança às cegas. Documentar na política. | P1 |
| 3.8 | Tratar HTTP 429/5xx com backoff. Com centenas de alunos, sync agressivo pode fazer o SUAP limitar o app. | P1 |

---

## 4. Build, performance e tamanho

| # | Item | Prioridade |
|---|---|---|
| 4.1 | `isMinifyEnabled = false` (`app/build.gradle.kts:34`). O APK tem **17 MB, com ~54 MB de dex** sem compressão. Ativar R8 + `isShrinkResources = true` deve levar para ~4–6 MB. **O arquivo `proguard-rules.pro` não existe**: criar com regras para kotlinx.serialization, Retrofit e Koin, e testar o release inteiro (login, sync, widgets, tile). | P0 |
| 4.2 | `material-icons-extended` inteiro (milhares de ícones) é o maior peso. Com R8 isso cai, mas melhor copiar só os ícones usados. | P2 |
| 4.3 | Baseline Profile (`profileinstaller` já está nas dependências, mas não há profile gerado) para acelerar o cold start. | P2 |
| 4.4 | Publicar **AAB** na Play Store. No GitHub, manter o APK universal. | P0 (Play Store) |
| 4.5 | Atualizar as dependências apontadas pelo lint (47 avisos `GradleDependency`), principalmente Kotlin 2.1.0, Room 2.6.1, Koin 3.5.3, `navigation-compose 2.8.0`, `kotlinx-serialization-json 1.6.3` (hardcoded fora do catálogo) e `work-runtime 2.10.0` (fora do `libs.versions.toml`). | P2 |
| 4.6 | Remover dependências não usadas: `lifecycle-viewmodel-navigation3` (só o código morto usa). | P2 |
| 4.7 | Definir `versionCode` automaticamente (ex.: a partir da tag git ou do número do build no CI). | P2 |

---

## 5. UX e polimento

| # | Item | Prioridade |
|---|---|---|
| 5.1 | **Onboarding curto** (2 ou 3 telas): o que o app faz, que é não-oficial, como o limite é calculado. Só depois pedir biometria e notificações. Hoje a permissão de notificação não tem contexto. | P1 |
| 5.2 | Tom "ácido" (`LINES_ACIDO`, "Vagabundo Sênior", "F.", "💀"). Divertido, mas num lançamento amplo pode pegar mal com aluno em situação difícil ou menor de idade. Sugestão: um toggle **"Modo zoeira" / "Modo sério"**, com o sério como padrão ou escolhido no onboarding. | P1 |
| 5.3 | Acessibilidade: 52 `Icon(..., null)`. Revisar quais transmitem informação (status, ações) e dar `contentDescription`. Semáforo verde/amarelo/vermelho **não pode depender só de cor**: o texto já ajuda, mas confira contraste e os widgets. Testar com TalkBack e fonte 200%. | P1 |
| 5.4 | Estados vazios e de erro por aba (ver 2.9), com pull-to-refresh padrão em vez de depender só do FAB. | P2 |
| 5.5 | Indicar **"atualizado há X min"** e o modo offline, para o aluno saber se o dado é fresco. | P1 |
| 5.6 | Seção "Sobre": link para o repositório, política de privacidade, licenças de terceiros (`oss-licenses` ou AboutLibraries), versão e "reportar problema" (abre issue/e-mail). | P1 |
| 5.7 | Tablets/landscape e telas pequenas (360dp). Conferir a sobreposição do FAB (`padding(bottom = 120.dp)` fixo) com a navbar em gestos e em 3 botões. | P2 |
| 5.8 | Ícone e nome na loja: "Supaco Mobile" → "Supaco". Revisar o ícone adaptativo com *themed icon* (monocromático, Android 13+). | P2 |

---

## 6. Repositório e engenharia

### 6.1 Limpeza (P1)
- Apagar o **código morto do template**: `ui/main/MainScreen.kt`, `ui/main/MainScreenViewModel.kt`, `data/DataRepository.kt`, `Navigation.kt`, `NavigationKeys.kt`, `test/.../MainScreenViewModelTest.kt`, `androidTest/.../MainScreenTest.kt`. `FaltasVault` está registrado no Koin (`AppModule.kt:28`) mas **nunca é usado**: implementar o "Faltei/Eu fui" ou remover, junto com a entrada no `backup_rules.xml`.
- **Tirar o APK do git** (`docs/supaco-1.8.apk`, 17 MB). Binário não vai para o repositório: publique em **GitHub Releases**. O `.git` já tem 18 MB. Considere `git filter-repo` para limpar o histórico **antes** de divulgar (reescreve o histórico, então combine antes se houver colaboradores).
- Pasta `tmp/` (`tmp/codex-homes`) na raiz: adicionar ao `.gitignore`.
- Substituir FQNs inline (`com.example.supacomobile.data.model.Servidor`, `androidx.compose.ui.platform.LocalContext`) por imports.
- Adotar `ktlint` ou `detekt` + `.editorconfig`. O código mistura indentação de 2 e 4 espaços (`build.gradle.kts`, `MainScreen.kt`).

### 6.2 Arquivos que faltam (P1)
- `LICENSE`: o README diz MIT, mas **não há arquivo de licença** (o GitHub mostra `licenseInfo: null`). Sem ele, legalmente o código não é MIT.
- `CHANGELOG.md` (Keep a Changelog), `CONTRIBUTING.md`, `SECURITY.md` (como reportar vulnerabilidade, importante porque o app lida com credenciais), `CODE_OF_CONDUCT.md`.
- `.github/ISSUE_TEMPLATE/` (bug com versão do app/Android, feature request) e `PULL_REQUEST_TEMPLATE.md`.
- `docs/privacy-policy.md` (servido via GitHub Pages).
- Topics no GitHub: `android`, `jetpack-compose`, `ifrn`, `suap`, `material-you`, `kotlin`.

### 6.3 CI/CD com GitHub Actions (P1)
1. **`ci.yml`** (em PR e push): `./gradlew lint testDebugUnitTest assembleRelease` com cache do Gradle. Falha se o lint tiver erro. Isso teria pego o bug 1.2.
2. **`release.yml`** (em tag `v*`): monta AAB + APK assinados com secrets (`SUPACO_KEYSTORE_BASE64`, senhas), cria o GitHub Release com changelog e checksums SHA-256, e opcionalmente envia para a trilha interna da Play Store (`r0adkll/upload-google-play` ou Gradle Play Publisher).
3. Dependabot/Renovate para dependências e para as actions.

### 6.4 Testes (P1)
Hoje são só 5 testes reais (`AbsenceLogicTest`). Mínimo para lançar:
- `vereditoDoDia` (1.3) com casos de 1, 2 e 4 aulas no dia, fim de semana e matéria sem horário;
- `ScheduleData.parseSuapHorario` / `limpaSala` / `limpaNome` com strings reais do SUAP (`"2M34 / 4V12"`, turnos noturnos, tokens inválidos);
- desserialização dos DTOs com JSON real anonimizado (2.4);
- `TokenAuthenticator` com MockWebServer (refresh ok, refresh 401, 401 concorrentes);
- `DashboardViewModel` com repositórios fake (sucesso, offline com cache, sessão expirada);
- um teste de UI do fluxo login → dashboard.

### 6.5 Arquitetura (P2, fazer aos poucos)
- Extrair a lógica de domínio (`AbsenceLogic`, que está em `ui/dashboard/`) para `domain/`, porque é usada por widget, notificação e UI.
- `DashboardViewModel` tem busca de servidores, sync e navegação de sheets. Separar `ServidoresViewModel`.
- Interfaces para os repositórios (facilita fakes nos testes).
- README: atualizar o badge (diz **v1.7**, a versão é 1.8), a estrutura de pastas e a seção de build (JDK 17, `local.properties`/`ANDROID_HOME`, como gerar release).

---

## 7. Aproveitar melhor a API (P3, roadmap)

Endpoints do spec que o app ainda não usa e que agregam muito:

| Endpoint | Feature |
|---|---|
| `GET /api/ensino/minhas-aulas/{ano}/{mes}/` | **Histórico real de faltas por data e por aula** (`faltas`, `qtd_aulas`, `conteudo`). Substitui o streak heurístico de `FaltasHistory` ("dias sem falta nova" calculado só nos dias em que o app sincroniza) por dados exatos, e permite "em que dia eu faltei?". |
| `GET /api/ensino/minhas-proximas-avaliacoes/` | **Próximas provas** na Home e no widget, com alerta "prova amanhã, não falte". Casa perfeitamente com o veredito. |
| `GET /api/ensino/meu-calendario-academico/{ano}/{periodo}/` | Feriados e fim do semestre: "hoje é feriado", "faltam X semanas", e projeção de quantas aulas ainda vão acontecer. |
| `GET /api/ensino/frequencia-periodo-letivo/{ano}/{periodo}/` | Frequência global e **abonos** (2.5). |
| `GET /api/ensino/meus-dados-aluno/` | Curso, IRA, situação da matrícula (corrige `Profile.curso`). |
| `GET /api/ensino/mensagens/entrada/{status}/` | Caixa de mensagens do SUAP com notificação de mensagem nova. |
| `GET /api/ensino/minha-turma-virtual/{pk}/` | Professores reais, materiais de aula e datas de início/fim por diário. |
| `GET /api/ensino/requisitos-conclusao/` | Progresso no curso. |
| `POST /api/token/verify` | Validar a sessão ao abrir, sem gastar uma chamada de dados. |

### 7.3 Login via OAuth do SUAP (investigar)
O ideal para um app de terceiros é **não receber a senha do aluno**. O SUAP tem OAuth2 para aplicações. Vale verificar se os escopos disponíveis cobrem os endpoints de ensino. Se cobrirem, é o maior ganho de confiança possível ("você entra pela página oficial do SUAP").

### 7.4 Outras ideias
- Widget "próxima aula" com contagem regressiva e notificação 10 min antes (opcional).
- Exportar a grade para o Google Agenda (`.ics`).
- Wear OS tile com o veredito.
- Modo responsável (`/api/ensino/meus-dependentes/`).

---

## 8. Canal de distribuição

**Recomendação: Play Store como canal principal, GitHub Releases como secundário.**

- A maioria dos alunos não instala APK de fonte desconhecida, e a loja traz atualização automática.
- **Contas pessoais novas na Play Console exigem teste fechado com pelo menos 12 testadores por 14 dias** antes da produção (confira a regra vigente na Play Console). Comece o teste fechado **o quanto antes**, com colegas de turma. Isso também serve de beta.
- Custo: taxa única de US$ 25 da conta de desenvolvedor, mais verificação de identidade.
- F-Droid é opcional e combina com o discurso open source, mas exige build reproduzível e sem dependências proprietárias (hoje o app já não tem nenhuma, o que é um ponto a favor).

---

## 9. Cronograma sugerido

| Fase | Duração | Conteúdo | Saída |
|---|---|---|---|
| **Fase 0: Decisões** | 1 dia | `applicationId` final, nova chave de assinatura, canal (Play/GitHub), tom padrão (zoeira/sério), minSdk 24 ou 26 | Decisões registradas no README/issue |
| **Fase 1: Bloqueadores** | ~1 semana | Todo o item 1 (1.1–1.8) + 4.1 (R8) + limpeza de código morto (6.1) | v1.9 interna |
| **Fase 2: Qualidade** | ~1–2 semanas | Itens 2.x, testes (6.4), CI (6.3), LICENSE/SECURITY/CHANGELOG, validação dos 25% com boletins reais (2.5) | v2.0-beta no teste fechado da Play Store |
| **Fase 3: Loja** | 14+ dias (em paralelo com a fase 2) | Teste fechado com mais de 12 pessoas, política de privacidade, Data safety, screenshots, descrição, onboarding (5.1), "Sobre" (5.6), crash reporting (3.7) | Aprovação para produção |
| **Fase 4: Lançamento** | 1 dia | v2.0 na Play Store + GitHub Release + README atualizado + aviso para os usuários do APK antigo | 🚀 |
| **Fase 5: Pós-lançamento** | contínuo | Monitorar crashes e avaliações, hotfixes, roadmap do item 7 (próximas avaliações e histórico real de faltas primeiro) | v2.1+ |

## 10. Checklist final (dia do lançamento)

- [ ] `./gradlew lint testDebugUnitTest` verde no CI
- [ ] Build **release com R8** testado manualmente: login, sync, troca de período, detalhe da matéria, simulador, widgets (3), tile, notificação, biometria, logout → login com outra conta
- [ ] Testado em Android 7 ou 8 (emulador), Android 14/15/16 e em um aparelho com pouca RAM
- [ ] Sem logs HTTP em release (`adb logcat` durante o login não mostra a senha)
- [ ] Cálculo de faltas conferido contra o percentual do SUAP em pelo menos 3 boletins reais
- [ ] Política de privacidade no ar e linkada no app e na loja
- [ ] LICENSE presente, README com a versão certa, CHANGELOG com a v2.0
- [ ] Keystore/upload key com backup offline em 2 lugares
- [ ] APK removido do repositório; release com checksum SHA-256
