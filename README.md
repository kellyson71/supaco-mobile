<div align="center">

# 📚 Supaco Mobile

**App Android não-oficial para o [SUAP](https://suap.ifrn.edu.br) do IFRN**

A pergunta que todo estudante faz — _"posso faltar hoje?"_ — respondida com os dados reais das suas faltas.

<br/>

![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material%203-757575?style=for-the-badge&logo=materialdesign&logoColor=white)
![Android](https://img.shields.io/badge/Android%207.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

</div>

---

## 📱 Telas

<div align="center">
<table>
  <tr>
    <td align="center" width="25%">
      <img src="docs/screenshots/home.png" width="100%"/><br/>
      <b>Início</b><br/>
      <sub>Veredito do dia + semana</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/materias.png" width="100%"/><br/>
      <b>Matérias</b><br/>
      <sub>Faltas e frequência por disciplina</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/perfil.png" width="100%"/><br/>
      <b>Eu, o réu</b><br/>
      <sub>Estatísticas e conquistas</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/configuracoes.png" width="100%"/><br/>
      <b>Configurações</b><br/>
      <sub>Tema, Material You e biometria</sub>
    </td>
  </tr>
</table>
</div>

---

## ✨ Funcionalidades

- 🎲 **"Posso faltar hoje?"** — veredito imediato baseado no seu saldo real de faltas
- 📊 **Cálculo de faltas em tempo real**, com frequência e limite por disciplina
- 🚦 **Semáforo de risco** — verde (folgado), amarelo (no fio), vermelho (sofrendo)
- 🏠 **Dashboard com 4 abas:** Início, Matérias, Horários e Perfil
- 🧩 **3 widgets de tela inicial:** resumo de faltas, veredito rápido e grade da semana
- ⚡ **Quick Settings Tile** — "posso faltar?" direto na barra de notificações
- 🔔 **Alertas inteligentes** via WorkManager quando uma matéria fica perigosa
- 🎨 **Material You** com cores dinâmicas do papel de parede + tema claro/escuro
- 🔐 **Biometria** e armazenamento criptografado de credenciais
- 📴 **Cache offline** com Room — funciona mesmo sem internet
- 🏆 **Conquistas e streaks** para gamificar (ou ironizar) suas faltas
- 🔎 **Busca de servidores** do campus

---

## 🛠️ Stack

| Camada | Tecnologia |
|--------|-----------|
| **UI** | Jetpack Compose + Material 3 |
| **Navegação** | Navigation Compose |
| **DI** | Koin |
| **Rede** | Retrofit + OkHttp |
| **Banco local** | Room |
| **Widgets** | Glance AppWidget |
| **Background** | WorkManager |
| **Segurança** | EncryptedSharedPreferences + Biometric |
| **Serialização** | Kotlin Serialization |

---

## 🗂️ Estrutura do projeto

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

## 🚀 Como rodar

```bash
git clone https://github.com/kellyson71/supaco-mobile.git
```

1. Abra no **Android Studio** (Ladybug ou superior)
2. **Sync** do Gradle
3. Rode em um dispositivo/emulador com **Android 7.0+** (API 24)
4. Faça login com sua **matrícula e senha do SUAP IFRN**

> 💡 O app consome a API pública do SUAP em `suap.ifrn.edu.br`. Não é necessário nenhum token ou chave de terceiros.

---

## 📋 Requisitos

- Android **7.0+** (API 24)
- Conta ativa no **SUAP IFRN**
- Android Studio **Ladybug+** / **JDK 17**

---

## ⚠️ Aviso

Projeto **não-oficial**, sem qualquer vínculo com o IFRN. Suas credenciais ficam **apenas no seu dispositivo**, criptografadas — nada é enviado para servidores de terceiros.

---

<div align="center">

Feito com ☕ e poucas faltas por [**@kellyson71**](https://github.com/kellyson71)

**Licença MIT**

</div>
