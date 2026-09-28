# Contribuindo

Obrigado pelo interesse! Algumas orientações:

## Ambiente

- Android Studio (Ladybug ou superior) e JDK 17
- Um `local.properties` com `sdk.dir=/caminho/do/Android/Sdk` (o Android Studio cria sozinho)

```bash
./gradlew lintDebug testDebugUnitTest assembleDebug
```

## Fluxo

1. Abra uma issue descrevendo o bug ou a ideia antes de mudanças grandes.
2. Crie uma branch a partir da `main`.
3. Commits no padrão [Conventional Commits](https://www.conventionalcommits.org/pt-br/) (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `build:`, `chore:`).
4. Garanta que `lintDebug` e `testDebugUnitTest` passam. O CI roda os dois em todo PR.
5. Lógica nova (cálculo de faltas, parsing do SUAP) vem acompanhada de teste em `app/src/test`.

## Regras do projeto

- O app só conversa com `suap.ifrn.edu.br`. Nada de analytics, anúncios ou servidores próprios.
- Nunca logar corpo de requisição/resposta: o login contém a senha.
- Textos visíveis ao usuário em português do Brasil.
