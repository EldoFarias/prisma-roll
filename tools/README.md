# Ferramentas

Scripts que geram os recursos do jogo. Rode da raiz do projeto.

| Script | O que gera |
|---|---|
| `python3 tools/make_art.py .` | ícones do app (todas as densidades) e imagens da loja em `store/` |
| `python3 tools/make_sfx.py app/src/main/res/raw` | efeitos sonoros (.wav) |
| `python3 tools/make_music.py app/src/main/res/raw/music_chill.ogg` | música de fundo (precisa do ffmpeg) |
| `python3 tools/strings.py app/src/main/res` | `strings.xml` dos 8 idiomas (en, pt, es, fr, de, tr, in, hi) |
| `python3 store/listings.py` (dentro de `store/`) | textos da loja em `store/listings/<idioma>/` |

`TowerTest.kt` joga todas as fases (1–400 e 30 desafios diários) com um robô na velocidade de um dedo e confere que todas têm solução (JVM, fora do Android).

Precisa de Python 3 com `numpy` e `Pillow`.
