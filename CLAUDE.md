# Prisma Roll — contexto do projeto

Jogo 3D estilo "Going Balls": a bola rola sozinha por uma pista flutuando no céu; o jogador desliza o dedo para
os lados para guiá-la, desviando de obstáculos e pulando vãos até a linha de chegada.
Android, só Play Store. Kotlin + Jetpack Compose (telas) + OpenGL ES 3.0 (`GLSurfaceView`, sem engine).
Feito a partir do motor do Prisma Tower (telas, loja, missões, baú, anúncios, sons e shaders reaproveitados).
Dono: Eldo (EldoFarias). Comunicação em português. **Monetização só AdMob** (banner, intersticial, premiado) —
sem outras redes, sem assinatura, sem compras no app.

## Como compilar e rodar

- Cada push na `main` gera o APK de teste em Releases → `teste` (`PrismaRoll-teste.apk`, debug, anúncios de teste) e,
  quando houver os secrets `KEYSTORE_*`, também o AAB de release (artefato `PrismaRoll-release-aab`).
  Erros de compilação aparecem como anotações do job (`gh api repos/EldoFarias/prisma-roll/check-runs/<job>/annotations`).
- Debug usa sempre IDs de **teste** do AdMob (pacote `com.farias.prismaroll.debug`); release lê os IDs reais dos
  secrets `ADMOB_*` e **falha** se faltar algum ou se for ID de teste. `applicationId` = `com.farias.prismaroll`.
- Teste das fases na JVM (sem Android), da raiz (tempo de geração, duração, obstáculos, se o robô vence):
  ```bash
  P=app/src/main/java/com/farias/prismaroll/game
  CP=$(ls /opt/gradle/lib/*.jar | tr '\n' ':'); java -cp "$CP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -cp "$CP" $P/Roll.kt tools/RollTest.kt -d /tmp/rt && java -cp "$CP:/tmp/rt" RollTestKt
  ```
- Atalhos só no debug: `adb shell am start -n com.farias.prismaroll.debug/com.farias.prismaroll.MainActivity --es screen game --ei level 12`
  (`home`, `chest`, `game`, `daily`, `shop`, `stats`).

## Regras do jogo (`game/Roll.kt`, puro Kotlin, testável na JVM)

- A pista avança pela distância `s`; no mundo 3D o ponto (x, y, s) fica em (x, y, −s). A bola anda sozinha
  (`RollLevel.speed`, 9,5→14 u/s) e o dedo só move o alvo lateral (`RollSim.targetX`); a bola persegue o alvo a no
  máximo `Roll.LAT_MAX`. Gravidade `Roll.GRAVITY`, passos de 1/120 s.
- Pista = lista de `Seg` (trecho reto de `s0` a `s0+len`, centro `x0→x1` (diagonal), altura `h0→h1` (rampa), largura
  `w`, tipo `SegKind`: normal, turbo, trampolim, chegada, pedra). Sem pista embaixo = cai; cair `Roll.FALL_DEPTH`
  abaixo do último chão = perdeu. Bater na lateral de um trecho mais alto também perde.
- Obstáculos (`Obs`, `ObsKind`): bloco parado, bloco deslizante, barra giratória, martelo pendular, muro que sobe e
  desce, moeda. `Obs.gap()` dá a distância da bola ao obstáculo (negativo = bateu). Passar a menos de 0,3 = "RASPOU!"
  (+10 pontos, câmera lenta). Escudo (vídeo) quebra o obstáculo em vez de perder.
- Gerador (`RollLevels.level(n)`, determinístico): monta a fase em pedaços (`Builder.chunk`: reta, diagonal, rampa,
  slalom, vão com rampa, turbo, ponte estreita, muro com buraco, deslizantes, pedras, trampolim, giratória, martelos,
  muros) em volta de uma **linha segura** (`pathS`/`pathX`) que nunca exige mais que `Roll.HUMAN_LAT` de velocidade
  lateral. Obstáculos móveis recebem a fase do movimento que deixa a linha segura livre na hora em que a bola passa
  (`timeTable`, `clearFor`). Depois o `RollBot` (segue a linha segura) joga a fase inteira; só fica a fase que ele
  vence → toda fase tem solução.
- Mecânicas a partir da fase (`RollLevels.*_FROM`): rampas 2, blocos 2, vãos 4, turbo 5, ponte 6, muro 8,
  deslizantes 10, pedras 14, trampolim 18, giratória 22, martelos 26, muros que sobem 32. Aviso na 1ª vez (`notice`).
- Pista chefe a cada 10 fases (`isBoss`): mais longa e difícil, prêmio ×3. `daily(dia)` igual para todos no dia.
  Sem fim (`endless(seed)`): ~2600 u de pedaços de dificuldade crescente; recorde em metros (`Progress.bestEndless`).
- Continuar por vídeo (`RollSim.revive`): volta à linha segura alguns metros antes, com o relógio da fase recuado
  para o horário planejado daquele ponto (os obstáculos móveis ficam como no plano) e espera um toque.
- Contadores herdados do Tower com novo sentido: `ringsPassed` = metros rolados, `ringsSmashed` = saltos,
  `bestCombo` = raspões numa fase, `glassBroken` = raspões, `portalsUsed` = turbos, "perfeita" = todas as moedas.

## Arquitetura

| Arquivo | O que tem |
|---|---|
| `game/Roll.kt` | Regras: `Seg`, `Obs`, `RollLevel`, `RollSim`, `RollLevels` (gerador), `RollBot`. |
| `game/RollGame.kt` | Roda na thread do GL: demo do menu (robô) e jogo, câmera atrás da bola, tremida, câmera lenta, partículas (estilhaços, poeira, turbo, confete), rastro. |
| `game/GameHost.kt` | Ponte GL ↔ Compose: estado da partida, sons, vibração, textos flutuantes, missões, moedas. `steer(dx)` vem do dedo. |
| `gl/RollRenderer.kt` | Desenho: céu (do Tower), pista e obstáculos em **uma** chamada instanciada de caixas (`Shaders.TRACK_VS`, 12 floats por caixa: centro, largura, espessura, comprimento, diagonal, rampa, cor, tipo), barras/martelos com matriz, bola, moedas, sombra, nuvens, brilhos aditivos, partículas. |
| `gl/Shaders.kt` | GLSL ES 3.0. Variante `V_TRACK` do shader iluminado: azulejos e bordas neon da pista, turbo com setas, trampolim, chegada xadrez, obstáculos com listras, metal, colunas. Use `ss()` em vez de `smoothstep`. |
| `gl/Looks.kt` | Os 13 temas de pista (ainda `TowerLook`: céu, cores da pista `ringA/ringB`, perigo `danger`, borda `rim`, cenário) e 23 bolas. |
| `ui/` | Igual ao Tower: Splash, Home (sobre a demo), Game (HUD, deslizar, escudo, avisos, pausa/derrota/vitória), Shop, Map, Extras, Stats, Rewards, Dialogs. |
| `data/`, `ads/`, `audio/` | Progresso, missões, conquistas, rastros; AdMob + UMP; sons, música e vibração (como no Tower). |

## Convenções

- Textos em 8 idiomas (en, pt, es, fr, de, tr, in, hi): editar `tools/strings.py` (bloco `ROLL` sobrescreve os
  textos herdados da torre) e rodar `python3 tools/strings.py app/src/main/res`.
- Sons: `python3 tools/make_sfx.py app/src/main/res/raw`. Músicas: `python3 tools/make_music.py ...`. Ícone e arte da
  loja: `python3 tools/make_art.py .`; textos da loja: `python3 store/listings.py`.
- Gradientes radiais com raio 0 derrubam o app: proteger tamanhos < 1 px.
- **Desempenho é prioridade (o Eldo quer o jogo extremamente leve):** nada de alocar no laço de desenho (arrays de
  rascunho no `RollRenderer`); pista e obstáculos numa única chamada instanciada; céu em 1/3 da resolução a cada 2
  quadros; "Cenário detalhado" e desligamento automático se os quadros ficarem lentos; GL limitado a 1080 px.
- Shaders: validar com glslangValidator antes de enviar (npm `glslang-validator-prebuilt-predownloaded`), inclusive o
  link VS+FS (`glslangValidator -l a.vert b.frag`). Uniform usado no vertex E no fragment precisa da MESMA precisão
  explícita (no Xiaomi do Eldo a diferença derruba o app).

## Forma de trabalho

- Não tirar prints nem rodar no emulador: só publicar o APK (push na `main`); o Eldo testa no celular e manda prints.

## Pendências

1. Eldo testar no celular: sensação do deslizar (`STEER_PER_WIDTH` em `GameScreen.kt`), câmera, velocidade, dificuldade.
2. Ícone, arte e textos da loja próprios do Prisma Roll; prints.
3. Publicação: keystore + secrets `KEYSTORE_*`, app e blocos no AdMob + secrets `ADMOB_*`, Play Console (`store/PUBLICAR.md`).
