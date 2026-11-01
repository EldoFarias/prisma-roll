# Publicar o Prisma Roll na Play Store

Roteiro do Play Console com as respostas que valem para este app. Tudo que é texto ou imagem está nesta pasta.

## O que está pronto

| Item | Onde |
|---|---|
| AAB assinado (release, IDs reais do AdMob) | GitHub Actions → *Build Android* → artefato `PrismaRoll-release-aab` (`gh workflow run android.yml`) |
| Ícone 512x512 | `store/icon-512.png` |
| Gráfico de destaque 1024x500 | `store/feature-graphic-1024x500.png` |
| Capturas de tela | O Eldo tira no celular (mín. 2, ideal 5–8): jogo com a bola em fogo, torre chefe, Queda livre, loja, mapa, roleta. Legendas sugeridas em `store/listings.py` (`captions`) |
| Nome, descrição curta e completa (8 idiomas) | `store/listings/<idioma>/` |
| Política de privacidade | https://eldofarias.github.io/prisma-tower/privacy.html |
| app-ads.txt | https://eldofarias.github.io/app-ads.txt |
| Chave de upload | Gerar no PC (passo 0 abaixo) e guardar em `Desktop/eldo/PrismaRoll-Keystore` |

**Política de privacidade:** o arquivo está em `docs/privacy.html`. Para ela ficar no ar, ative uma vez:
GitHub → repositório **prisma-tower** → *Settings → Pages* → *Source: Deploy from a branch* → Branch **main**, pasta **/docs** → Save.
Em 1–2 minutos o link acima abre.

## 0. Antes de tudo (no PC)

**Chave de assinatura** (uma vez só; guarde a pasta, sem ela não dá para atualizar o app):
```bash
keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 upload.jks > upload.jks.b64     # no Windows: certutil -encode upload.jks upload.jks.b64
```
GitHub → prisma-tower → *Settings → Secrets and variables → Actions* → criar:
`KEYSTORE_BASE64` (conteúdo do .b64), `KEYSTORE_PASSWORD`, `KEY_ALIAS` (= upload), `KEY_PASSWORD`.

**AdMob:** Apps → Adicionar app → Android → "Ainda não está publicado" → nome **Prisma Roll**.
Crie 3 blocos: **Banner**, **Intersticial**, **Premiado**. Depois crie os secrets
`ADMOB_APP_ID` (`ca-app-pub-…~…`), `ADMOB_BANNER_ID`, `ADMOB_INTERSTITIAL_ID`, `ADMOB_REWARDED_ID` (`ca-app-pub-…/…`).
Em *Privacidade e mensagens → GDPR*, inclua o app na mensagem de consentimento e publique.

## 1. Criar o app

Play Console → **Criar app**
- Nome: **Prisma Roll**
- Idioma padrão: **Português (Brasil) – pt-BR**
- App ou jogo: **Jogo** · Gratuito ou pago: **Gratuito**
- Aceitar as declarações (políticas e leis de exportação dos EUA).

> **Conta pessoal criada depois de nov/2023:** o Google exige um **teste fechado com pelo menos 12 testadores
> ativos por 14 dias seguidos** antes de liberar a produção. Nesse caso, comece pelo passo 6 (teste fechado)
> e chame amigos/família (contas Google) para instalar pelo link de teste.

## 2. Configurar o app (painel → "Configure seu app")

**Acesso ao app:** Todas as funcionalidades estão disponíveis sem acesso especial.

**Anúncios:** Sim, meu app contém anúncios.

**Classificação do conteúdo** (questionário IARC)
- E-mail: eldofarias81@gmail.com · Categoria: **Jogo**
- Violência, medo, sexo, linguagem, drogas, apostas/jogos de azar simulados: **Não** em tudo
  (baú, roleta da sorte e prêmio surpresa só dão moedas/skins do jogo; não há aposta, dinheiro nem compra).
- Interação entre usuários / compartilha localização / compras digitais: **Não**
- Resultado esperado: **Livre / PEGI 3 / Everyone**.

**Público-alvo e conteúdo**
- Faixas etárias: **13–15, 16–17 e 18+** (não marcar menores de 13: o app tem anúncios do AdMob
  sem a configuração de família, e a política diz que não é direcionado a crianças).
- O app atrai crianças sem querer? **Não.**

**App de notícias:** Não · **Apps governamentais:** Não · **Recursos financeiros:** Nenhum · **Saúde:** Não

**ID de publicidade:** Sim, o app usa o ID de publicidade → finalidade **Publicidade ou marketing**
(a permissão `AD_ID` já entra pelo SDK do AdMob).

**Segurança dos dados**
- O app coleta ou compartilha algum dos tipos de dados obrigatórios? **Sim**
- Todos os dados são criptografados em trânsito? **Sim**
- Oferece forma de solicitar exclusão dos dados? **Não** (o jogo não tem conta; o progresso fica no aparelho
  e no backup do Android do próprio usuário, que ele controla; o ID de publicidade é redefinido nas configurações do Android)
- Tipos de dados (todos coletados **e** compartilhados pelo SDK do Google Mobile Ads, não obrigatórios
  para o jogo funcionar? → marcar **coleta obrigatória**, porque o usuário não desliga):
  - **Local → Local aproximado** (derivado do IP): Publicidade ou marketing, Análise, Prevenção de fraudes
  - **Atividade no app → Interações com o app**: Publicidade ou marketing, Análise
  - **Informações e desempenho do app → Registros de falhas e Diagnóstico**: Análise
  - **Identificadores do dispositivo ou outros → ID do dispositivo/ID de publicidade**: Publicidade ou marketing, Análise, Prevenção de fraudes
  - Dados processados de forma temporária? **Não**
- URL da política de privacidade: https://eldofarias.github.io/prisma-tower/privacy.html

## 3. Presença na loja

**Categoria:** Jogo → **Casual** · Tags: Casual, Arcade, Ação, 3D, Offline
**Contato:** e-mail eldofarias81@gmail.com · site **https://eldofarias.github.io** (necessário para o app-ads.txt)

**Página principal da loja** (idioma padrão pt-BR, depois *Gerenciar traduções → Adicionar tradução própria*
para en-US, es-ES, fr-FR, de-DE, tr-TR, id, hi-IN):
- Nome do app, descrição curta e completa: copiar de `store/listings/<idioma>/`
- Ícone: `store/icon-512.png` · Gráfico de destaque: `store/feature-graphic-1024x500.png`
- Capturas de tela do telefone: os prints do celular (a mesma série serve para todos os idiomas)
- Tablet: pode usar as mesmas capturas (opcional).

## 4. Países e preço
Produção → Países/regiões → **Adicionar todos**. Preço: gratuito.

## 5. Gerar o AAB

```bash
gh workflow run android.yml        # ou GitHub → Actions → Build Android → Run workflow
```
Ao terminar, baixe o artefato **PrismaRoll-release-aab** (zip com `app-release.aab`).
O `versionCode` é o número do build no GitHub, então cada novo envio precisa de um build novo.

## 6. Enviar a versão

1. **Teste interno** (recomendado primeiro, sai em minutos): Testes → Teste interno → Criar versão →
   enviar o AAB → aceitar a **Assinatura de apps do Google Play** (padrão) → notas da versão → lançar.
   Adicione seu e-mail como testador e instale pelo link.
2. **Teste fechado** (obrigatório para contas pessoais novas): mesma coisa em Testes → Teste fechado,
   com pelo menos 12 testadores por 14 dias.
3. **Produção**: Produção → Criar versão → promover a versão testada → enviar para revisão
   (a primeira revisão costuma levar de 1 a 7 dias).

Notas da versão sugeridas (pt-BR):
```
Primeira versão do Prisma Roll! Gire a torre, quebre anéis pegando fogo, vença torres chefe, desça na
Queda livre e colecione 13 torres, 23 bolas e 7 rastros. Desafio diário, roleta, missões e conquistas.
```

## 7. Depois de publicar
- AdMob → Apps → vincular o app à loja (o AdMob encontra o app publicado e verifica o app-ads.txt).
- No seu celular com o app da loja: AdMob → Configurações → **Dispositivos de teste** → adicionar,
  para nunca clicar nos próprios anúncios reais.
