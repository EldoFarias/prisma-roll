# Prisma Roll

Jogo casual 3D para Android no estilo "Helix": gire a torre arrastando o dedo e faça a bola quicar
anel por anel até o fundo, sem tocar nas partes vermelhas. Kotlin + Jetpack Compose + OpenGL ES 3.0
(sem engine), monetizado só com AdMob, publicado só na Play Store.

## O que tem

- Fases infinitas e determinísticas pelo número; cada fase só é aceita se um robô na velocidade de
  um dedo humano consegue terminar (`TowerLevels` + `TowerBot`).
- Modo fogo: 3 anéis atravessados seguidos → a bola quebra o próximo anel, até os perigosos.
- Anéis que giram e balançam a partir da fase 12; meta dourada no fim.
- 10 torres (neon, oceano, doces, vulcão, galáxia, ouro, gelo, selva, sakura, cyber) e 13 bolas
  (pérola, rubi, safira, praia, bala, ouro, cromo, neon, arco-íris, disco, planeta, bola 8, lava),
  com padrões feitos no shader e prévia 3D ao vivo na loja.
- Partículas, respingos de tinta, rastro, amassar-e-esticar da bola, anéis voando ao quebrar, MSAA 4×.
- Desafio diário, baú de 7 dias, missões diárias, recordes, sequência de vitórias.
- Anúncios: banner adaptativo, intersticial entre fases (nunca nas 4 primeiras vitórias), premiado
  para continuar e para triplicar moedas; consentimento UMP (GDPR/LGPD).
- 8 idiomas: en, pt, es, fr, de, tr, id, hi.

## APK de teste sem PC

Cada `push` na `main` roda o GitHub Actions, que gera o APK com anúncios de teste e publica em
**Releases → "teste"** (`PrismaRoll-teste.apk`). É só baixar no celular e instalar.

## Antes de publicar

Configure em *Settings → Secrets and variables → Actions*:

| Secret | Para quê |
|---|---|
| `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` | Assinar o AAB de produção |
| `ADMOB_APP_ID`, `ADMOB_BANNER_ID`, `ADMOB_INTERSTITIAL_ID`, `ADMOB_REWARDED_ID` | IDs reais do AdMob |
