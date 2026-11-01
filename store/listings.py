"""Store listing texts and screenshot captions in the 8 languages of Prisma Roll.

Writes store/listings/<play-locale>/{title,short_description,full_description}.txt and checks the
Play Console limits (30 / 80 / 4000 characters). Captions are for the store screenshots.

    python3 store/listings.py
"""
import os

HERE = os.path.dirname(os.path.abspath(__file__))

L = {}

L["pt-BR"] = dict(
    title='Prisma Roll: Bola na Pista 3D',
    short='Guie a bola por pistas no céu, desvie de obstáculos e chegue ao fim sem cair!',
    full="""Deslize o dedo e guie a bola por pistas flutuando no céu. Desvie de blocos, martelos e barras giratórias, pule vãos com turbo e trampolim e chegue à linha de chegada sem cair! Prisma Roll é um jogo 3D rápido, bonito e viciante: fácil de começar, difícil de largar.

★ COMO JOGAR
• Toque para rolar e deslize para os lados para guiar a bola
• Não caia da pista e não bata nos obstáculos
• Passe raspando para ganhar pontos extras em câmera lenta

★ FASES INFINITAS
• Cada fase é diferente, e todas têm solução
• Pista chefe a cada 10 fases: mais longa, mais rápida e com prêmio triplo
• Obstáculos novos a cada mundo: rampas, vãos, pontes estreitas, pedras soltas, blocos deslizantes, martelos, barras que giram e muros que sobem e descem
• Turbos e trampolins para voar pela pista
• Moedas pelo caminho

★ MODO SEM FIM
• Uma pista que nunca acaba: até onde você consegue rolar?
• Bata o seu recorde a cada partida

★ TODO DIA TEM NOVIDADE
• Desafio diário igual para todos
• Roleta da sorte diária
• Três missões por dia e baú de 7 dias
• Mais de 20 conquistas com recompensas

★ VISUAL PREMIUM
• 13 temas de pista com cenários próprios: cidade neon, oceano, vulcão, espaço, deserto, castelo e mais
• 23 bolas, de pérola e cromo até planeta Terra, galáxia e joia
• Rastros especiais: arco-íris, estrelas, fogo azul e pó de ouro
• Estilhaços, faíscas, câmera lenta e música por tema
• Leve e fluido, funciona sem internet

Grátis, com anúncios. Não precisa de conta.
""",
    captions=['Guie a bola pelo céu!', 'Desvie dos obstáculos', 'Pule vãos com turbo', 'Pistas chefe a cada 10 fases', '13 temas, 23 bolas', 'Modo Sem fim', 'Roleta, missões e conquistas'],
)

L["en-US"] = dict(
    title='Prisma Roll: Sky Ball Run 3D',
    short='Steer the ball along sky tracks, dodge obstacles and finish without falling!',
    full="""Slide your finger and steer the ball along tracks floating in the sky. Dodge blocks, hammers and spinning bars, jump gaps with speed pads and launchers, and reach the finish line without falling! Prisma Roll is a fast, beautiful and addictive 3D game: easy to start, hard to put down.

★ HOW TO PLAY
• Touch to roll and slide left and right to steer
• Don't fall off and don't hit the obstacles
• Pass by a hair for bonus points in slow motion

★ ENDLESS LEVELS
• Every level is different, and every level can be beaten
• A boss track every 10 levels: longer, faster, triple reward
• New obstacles in every world: ramps, gaps, narrow bridges, stepping stones, sliding blocks, hammers, spinning bars and rising walls
• Speed pads and jump pads to fly along the track
• Coins along the way

★ ENDLESS MODE
• A track that never ends: how far can you roll?
• Beat your best every run

★ SOMETHING NEW EVERY DAY
• Daily challenge, the same for everyone
• Daily lucky wheel
• Three missions a day and a 7-day chest
• 20+ achievements with rewards

★ PREMIUM LOOK
• 13 track themes with their own worlds: neon city, ocean, volcano, space, desert, castle and more
• 23 balls, from pearl and chrome to planet Earth, galaxy and gem
• Special trails: rainbow, stars, blue fire and gold dust
• Shards, sparks, slow motion and music for each theme
• Light and smooth, plays offline

Free, with ads. No account needed.
""",
    captions=['Steer the ball through the sky!', 'Dodge the obstacles', 'Jump gaps with speed', 'Boss tracks every 10 levels', '13 themes, 23 balls', 'Endless mode', 'Wheel, missions, achievements'],
)

L["es-ES"] = dict(
    title='Prisma Roll: Bola en el Cielo',
    short='¡Guía la bola por pistas en el cielo, esquiva obstáculos y llega sin caer!',
    full="""Desliza el dedo y guía la bola por pistas que flotan en el cielo. Esquiva bloques, martillos y barras giratorias, salta huecos con turbos y trampolines, ¡y llega a la meta sin caer! Prisma Roll es un juego 3D rápido, bonito y adictivo: fácil de empezar, difícil de dejar.

★ CÓMO JUGAR
• Toca para rodar y desliza a los lados para guiar la bola
• No te caigas de la pista ni choques con los obstáculos
• Pasa rozando para ganar puntos extra a cámara lenta

★ NIVELES INFINITOS
• Cada nivel es distinto, y todos tienen solución
• Pista jefe cada 10 niveles: más larga, más rápida y premio triple
• Obstáculos nuevos en cada mundo: rampas, huecos, puentes estrechos, piedras sueltas, bloques deslizantes, martillos, barras giratorias y muros que suben y bajan
• Turbos y trampolines para volar por la pista
• Monedas por el camino

★ MODO SIN FIN
• Una pista que nunca termina: ¿hasta dónde puedes rodar?
• Supera tu récord en cada partida

★ NOVEDADES CADA DÍA
• Desafío diario igual para todos
• Ruleta de la suerte diaria
• Tres misiones al día y cofre de 7 días
• Más de 20 logros con recompensas

★ ASPECTO PREMIUM
• 13 temas de pista con mundos propios: ciudad neón, océano, volcán, espacio, desierto, castillo y más
• 23 bolas, de perla y cromo a planeta Tierra, galaxia y gema
• Estelas especiales: arcoíris, estrellas, fuego azul y polvo de oro
• Esquirlas, chispas, cámara lenta y música por tema
• Ligero y fluido, funciona sin internet

Gratis, con anuncios. No necesitas cuenta.
""",
    captions=['¡Guía la bola por el cielo!', 'Esquiva los obstáculos', 'Salta huecos con turbo', 'Pistas jefe cada 10 niveles', '13 temas, 23 bolas', 'Modo sin fin', 'Ruleta, misiones y logros'],
)

L["fr-FR"] = dict(
    title='Prisma Roll : Balle du Ciel',
    short='Guide la balle sur des pistes dans le ciel, évite les obstacles sans tomber !',
    full="""Glisse le doigt et guide la balle sur des pistes qui flottent dans le ciel. Évite les blocs, les marteaux et les barres qui tournent, saute les trous avec les turbos et les tremplins, et atteins l'arrivée sans tomber ! Prisma Roll est un jeu 3D rapide, beau et addictif : facile à prendre en main, difficile à lâcher.

★ COMMENT JOUER
• Touche pour rouler et glisse à gauche et à droite pour diriger
• Ne tombe pas de la piste et ne touche pas les obstacles
• Passe de justesse pour des points bonus au ralenti

★ NIVEAUX INFINIS
• Chaque niveau est différent, et tous ont une solution
• Une piste du boss tous les 10 niveaux : plus longue, plus rapide, récompense triple
• De nouveaux obstacles à chaque monde : rampes, trous, ponts étroits, pierres de gué, blocs qui glissent, marteaux, barres qui tournent et murs qui montent
• Turbos et tremplins pour voler sur la piste
• Des pièces sur le chemin

★ MODE SANS FIN
• Une piste qui ne finit jamais : jusqu'où peux-tu rouler ?
• Bats ton record à chaque partie

★ DU NOUVEAU CHAQUE JOUR
• Défi du jour, le même pour tous
• Roue de la chance quotidienne
• Trois missions par jour et coffre de 7 jours
• Plus de 20 succès avec récompenses

★ UN LOOK PREMIUM
• 13 thèmes de piste avec leurs mondes : ville néon, océan, volcan, espace, désert, château et plus
• 23 balles, de la perle et du chrome à la planète Terre, la galaxie et la gemme
• Traînées spéciales : arc-en-ciel, étoiles, feu bleu et poudre d'or
• Éclats, étincelles, ralenti et musique pour chaque thème
• Léger et fluide, fonctionne hors ligne

Gratuit, avec publicités. Aucun compte nécessaire.
""",
    captions=['Guide la balle dans le ciel !', 'Évite les obstacles', 'Saute les trous en turbo', 'Pistes du boss tous les 10 niveaux', '13 thèmes, 23 balles', 'Mode sans fin', 'Roue, missions et succès'],
)

L["de-DE"] = dict(
    title='Prisma Roll: Himmelsball 3D',
    short='Lenk den Ball über Strecken im Himmel, weich Hindernissen aus und fall nicht!',
    full="""Wisch mit dem Finger und lenk den Ball über Strecken, die im Himmel schweben. Weich Blöcken, Hämmern und drehenden Stangen aus, spring mit Turbos und Sprungfeldern über Lücken und erreich das Ziel, ohne zu fallen! Prisma Roll ist ein schnelles, schönes 3D-Spiel mit Suchtfaktor: leicht zu lernen, schwer aus der Hand zu legen.

★ SO WIRD GESPIELT
• Tippen zum Rollen, nach links und rechts wischen zum Lenken
• Nicht von der Strecke fallen und kein Hindernis berühren
• Knapp vorbei bringt Bonuspunkte in Zeitlupe

★ ENDLOSE LEVEL
• Jedes Level ist anders, und jedes ist lösbar
• Alle 10 Level eine Boss-Strecke: länger, schneller, dreifache Belohnung
• Neue Hindernisse in jeder Welt: Rampen, Lücken, schmale Brücken, Trittsteine, gleitende Blöcke, Hämmer, drehende Stangen und Wände, die sich heben
• Turbos und Sprungfelder zum Fliegen
• Münzen auf dem Weg

★ ENDLOS-MODUS
• Eine Strecke ohne Ende: wie weit rollst du?
• Schlag deinen Rekord in jeder Runde

★ JEDEN TAG ETWAS NEUES
• Tägliche Challenge, für alle gleich
• Tägliches Glücksrad
• Drei Missionen pro Tag und eine 7-Tage-Truhe
• Über 20 Erfolge mit Belohnungen

★ PREMIUM-LOOK
• 13 Strecken-Themen mit eigenen Welten: Neonstadt, Ozean, Vulkan, Weltall, Wüste, Burg und mehr
• 23 Bälle, von Perle und Chrom bis Planet Erde, Galaxie und Juwel
• Besondere Spuren: Regenbogen, Sterne, blaues Feuer und Goldstaub
• Splitter, Funken, Zeitlupe und Musik für jedes Thema
• Leicht und flüssig, spielbar offline

Kostenlos, mit Werbung. Kein Konto nötig.
""",
    captions=['Lenk den Ball durch den Himmel!', 'Weich Hindernissen aus', 'Spring mit Turbo über Lücken', 'Boss-Strecken alle 10 Level', '13 Themen, 23 Bälle', 'Endlos-Modus', 'Rad, Missionen, Erfolge'],
)

L["tr-TR"] = dict(
    title='Prisma Roll: Gökyüzü Topu 3D',
    short='Topu gökyüzündeki pistlerde yönlendir, engellerden kaç ve düşmeden bitir!',
    full="""Parmağını kaydır ve topu gökyüzünde süzülen pistlerde yönlendir. Bloklardan, çekiçlerden ve dönen çubuklardan kaç, turbolar ve zıplama pedleriyle boşlukların üstünden atla ve düşmeden bitiş çizgisine ulaş! Prisma Roll hızlı, güzel ve bağımlılık yapan bir 3D oyun: başlaması kolay, bırakması zor.

★ NASIL OYNANIR
• Yuvarlanmak için dokun, yönlendirmek için sağa sola kaydır
• Pistten düşme ve engellere çarpma
• Kıl payı geç, ağır çekimde ekstra puan kazan

★ SONSUZ BÖLÜM
• Her bölüm farklı ve hepsinin çözümü var
• Her 10 bölümde bir patron pist: daha uzun, daha hızlı, üç kat ödül
• Her dünyada yeni engeller: rampalar, boşluklar, dar köprüler, basamak taşları, kayan bloklar, çekiçler, dönen çubuklar ve inip kalkan duvarlar
• Pistte uçmak için turbolar ve zıplama pedleri
• Yol boyunca altınlar

★ SONSUZ MOD
• Hiç bitmeyen bir pist: ne kadar uzağa yuvarlanabilirsin?
• Her oyunda rekorunu kır

★ HER GÜN YENİLİK
• Herkes için aynı günlük görev
• Günlük şans çarkı
• Günde üç görev ve 7 günlük sandık
• Ödüllü 20'den fazla başarım

★ PREMIUM GÖRÜNÜM
• Kendi dünyası olan 13 pist teması: neon şehir, okyanus, volkan, uzay, çöl, kale ve daha fazlası
• İnci ve kromdan Dünya gezegeni, galaksi ve mücevhere 23 top
• Özel izler: gökkuşağı, yıldızlar, mavi ateş ve altın tozu
• Kıymıklar, kıvılcımlar, ağır çekim ve temaya özel müzik
• Hafif ve akıcı, internetsiz çalışır

Ücretsiz, reklamlı. Hesap gerekmez.
""",
    captions=['Topu gökyüzünde yönlendir!', 'Engellerden kaç', 'Turboyla boşluk atla', 'Her 10 bölümde patron pist', '13 tema, 23 top', 'Sonsuz mod', 'Çark, görevler, başarımlar'],
)

L["id"] = dict(
    title='Prisma Roll: Bola Langit 3D',
    short='Arahkan bola di lintasan langit, hindari rintangan dan sampai tanpa jatuh!',
    full="""Geser jarimu dan arahkan bola di lintasan yang melayang di langit. Hindari balok, palu, dan batang yang berputar, lompati celah dengan turbo dan pad lompat, lalu capai garis finis tanpa jatuh! Prisma Roll adalah game 3D yang cepat, indah, dan bikin ketagihan: mudah dimulai, sulit dilepas.

★ CARA BERMAIN
• Sentuh untuk menggelinding, geser kiri dan kanan untuk mengarahkan
• Jangan jatuh dari lintasan dan jangan menabrak rintangan
• Lewat nyaris untuk poin bonus dalam gerak lambat

★ LEVEL TANPA AKHIR
• Setiap level berbeda, dan semuanya bisa diselesaikan
• Lintasan bos setiap 10 level: lebih panjang, lebih cepat, hadiah tiga kali
• Rintangan baru di setiap dunia: tanjakan, celah, jembatan sempit, batu loncatan, balok geser, palu, batang berputar, dan dinding naik turun
• Turbo dan pad lompat untuk terbang di lintasan
• Koin di sepanjang jalan

★ MODE TANPA AKHIR
• Lintasan yang tak pernah habis: seberapa jauh kamu bisa menggelinding?
• Pecahkan rekormu di setiap permainan

★ ADA YANG BARU SETIAP HARI
• Tantangan harian yang sama untuk semua
• Roda keberuntungan harian
• Tiga misi sehari dan peti 7 hari
• Lebih dari 20 pencapaian berhadiah

★ TAMPILAN PREMIUM
• 13 tema lintasan dengan dunia sendiri: kota neon, samudra, gunung api, luar angkasa, gurun, kastil, dan lainnya
• 23 bola, dari mutiara dan krom sampai planet Bumi, galaksi, dan permata
• Jejak spesial: pelangi, bintang, api biru, dan debu emas
• Serpihan, percikan, gerak lambat, dan musik untuk tiap tema
• Ringan dan mulus, bisa dimainkan offline

Gratis, dengan iklan. Tidak perlu akun.
""",
    captions=['Arahkan bola di langit!', 'Hindari rintangan', 'Lompati celah dengan turbo', 'Lintasan bos tiap 10 level', '13 tema, 23 bola', 'Mode tanpa akhir', 'Roda, misi, pencapaian'],
)

L["hi-IN"] = dict(
    title='Prisma Roll: आसमानी गेंद 3D',
    short='आसमान के ट्रैक पर गेंद चलाएँ, रुकावटों से बचें और बिना गिरे पहुँचें!',
    full="""उंगली सरकाएँ और आसमान में तैरते ट्रैक पर गेंद को चलाएँ। ब्लॉक, हथौड़ों और घूमते डंडों से बचें, टर्बो और जंप पैड से गड्ढों के ऊपर कूदें और बिना गिरे फ़िनिश लाइन तक पहुँचें! Prisma Roll एक तेज़, सुंदर और लत लगाने वाला 3D गेम है: शुरू करना आसान, छोड़ना मुश्किल।

★ कैसे खेलें
• लुढ़कने के लिए छुएँ, दिशा देने के लिए बाएँ-दाएँ सरकाएँ
• ट्रैक से न गिरें और रुकावटों से न टकराएँ
• बाल-बाल बचकर स्लो मोशन में बोनस अंक पाएँ

★ अनगिनत लेवल
• हर लेवल अलग है, और हर लेवल जीता जा सकता है
• हर 10 लेवल पर बॉस ट्रैक: ज़्यादा लंबा, ज़्यादा तेज़, तीन गुना इनाम
• हर दुनिया में नई रुकावटें: ढलान, गड्ढे, पतले पुल, पत्थर, सरकते ब्लॉक, हथौड़े, घूमते डंडे और ऊपर-नीचे होती दीवारें
• ट्रैक पर उड़ने के लिए टर्बो और जंप पैड
• रास्ते में सिक्के

★ अंतहीन मोड
• कभी न ख़त्म होने वाला ट्रैक: आप कितनी दूर लुढ़क सकते हैं?
• हर बार अपना रिकॉर्ड तोड़ें

★ हर दिन कुछ नया
• सबके लिए एक जैसी दैनिक चुनौती
• रोज़ किस्मत का पहिया
• रोज़ तीन मिशन और 7 दिन का खज़ाना
• इनाम वाली 20 से ज़्यादा उपलब्धियाँ

★ प्रीमियम लुक
• अपनी-अपनी दुनिया वाले 13 ट्रैक थीम: नियॉन शहर, समुद्र, ज्वालामुखी, अंतरिक्ष, रेगिस्तान, महल और भी
• मोती और क्रोम से लेकर पृथ्वी, आकाशगंगा और रत्न तक 23 गेंदें
• ख़ास ट्रेल: इंद्रधनुष, सितारे, नीली आग और सोने की धूल
• टुकड़े, चिंगारियाँ, स्लो मोशन और हर थीम का संगीत
• हल्का और स्मूद, बिना इंटरनेट चलता है

मुफ़्त, विज्ञापनों के साथ। अकाउंट की ज़रूरत नहीं।
""",
    captions=['आसमान में गेंद चलाएँ!', 'रुकावटों से बचें', 'टर्बो से गड्ढे पार करें', 'हर 10 लेवल पर बॉस ट्रैक', '13 थीम, 23 गेंदें', 'अंतहीन मोड', 'पहिया, मिशन, उपलब्धियाँ'],
)

if __name__ == "__main__":
    for loc, t in L.items():
        for key, limit in (("title", 30), ("short", 80), ("full", 4000)):
            n = len(t[key].strip())
            assert n <= limit, (loc, key, n, limit)
        assert len(t["captions"]) == 7, loc
        d = os.path.join(HERE, "listings", loc)
        os.makedirs(d, exist_ok=True)
        for key, name in (("title", "title"), ("short", "short_description"), ("full", "full_description")):
            with open(os.path.join(d, name + ".txt"), "w", encoding="utf-8", newline="\n") as f:
                f.write(t[key].strip() + "\n")
        print(f"{loc:6} title={len(t['title']):2} short={len(t['short']):2} full={len(t['full'].strip()):4}")
