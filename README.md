# Supaco Mobile

App Android não-oficial para o [SUAP](https://suap.ifrn.edu.br) do IFRN, construído com Jetpack Compose.

A pergunta central do app: **"Posso faltar hoje?"** — e ele responde com dados reais das suas faltas.

---

## Funcionalidades

- **Login via SUAP** com autenticação JWT e renovação automática de token
- **Dashboard com 4 abas:** Home, Horários, Matérias e Perfil
- **Cálculo de faltas em tempo real** com limite por disciplina e alerta de risco
- **Veredito "Posso faltar?"** — resposta imediata baseada no seu saldo de faltas
- **3 widgets para a tela inicial:**
  - Resumo geral de faltas
  - Veredito rápido
  - Grade de horários da semana
- **Quick Settings Tile** ("Posso faltar hoje?") na barra de notificações
- **Notificações inteligentes** via WorkManager quando uma matéria fica no limite
- **Busca de servidores** do campus
- **Conquistas** desbloqueáveis
- **Cache offline** com Room Database
- **Biometria** para acesso rápido
- Suporte a **splash screen** e **atalhos de app**

---

## Stack

| Camada | Tecnologia |
|--------|-----------|
| UI | Jetpack Compose + Material 3 |
| Navegação | Navigation Compose |
| DI | Koin |
| Rede | Retrofit + OkHttp |
| Banco local | Room |
| Widgets | Glance AppWidget |
| Background | WorkManager |
| Segurança | EncryptedSharedPreferences + Biometric |
| Serialização | Kotlin Serialization |

---

## Estrutura do projeto

```
app/src/main/java/com/example/supacomobile/
├── data/
│   ├── model/          # Data classes (Auth, Academic, Profile)
│   ├── remote/         # Retrofit API + interceptors
│   ├── local/          # Room DB, DAOs, EncryptedPrefs
│   └── repository/     # Repositórios (Auth, Academic, Profile)
├── di/                 # Módulos Koin
├── ui/
│   ├── auth/           # Tela de login
│   ├── dashboard/      # Dashboard principal + abas
│   ├── conquistas/     # Tela de conquistas
│   ├── settings/       # Tela de configurações
│   ├── main/           # MainScreen (entry point pós-login)
│   └── components/     # Componentes reutilizáveis
├── widget/             # Glance widgets
├── tile/               # Quick Settings Tile
├── notifications/      # WorkManager + Notifier
└── theme/              # Material Theme (Color, Type, Shape)
```

---

## Como rodar

1. Clone o repositório
2. Abra no Android Studio (Ladybug ou superior)
3. Sync Gradle
4. Rode em um dispositivo/emulador com Android 7.0+ (API 24)
5. Faça login com sua matrícula e senha do SUAP IFRN

> O app consome a API pública do SUAP em `suap.ifrn.edu.br`. Não é necessário nenhum token ou chave de terceiros.

---

## Requisitos

- Android 7.0+ (API 24)
- Conta ativa no SUAP IFRN
- Android Studio Ladybug+ / JDK 17

---

## Licença

MIT
