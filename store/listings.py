"""Store listing texts and screenshot captions in the 8 languages of Prisma Roll.

Writes store/listings/<play-locale>/{title,short_description,full_description}.txt and checks the
Play Console limits (30 / 80 / 4000 characters). Captions are for the store screenshots.

    python3 store/listings.py
"""
import os

HERE = os.path.dirname(os.path.abspath(__file__))

L = {}

L["pt-BR"] = dict(
    title="Prisma Roll: Torre Helix 3D",
    short="Gire a torre, faça a bola quicar até o fundo e quebre anéis pegando fogo!",
    full="""Gire a torre com o dedo e guie a bola quicando de anel em anel até a meta dourada lá embaixo. Atravesse 3 anéis seguidos e a bola pega fogo, quebrando tudo pelo caminho! Prisma Roll é um jogo 3D rápido, bonito e viciante: fácil de começar, difícil de largar.

★ COMO JOGAR
• Arraste o dedo para girar a torre
• Caia pelos vãos e evite as partes vermelhas
• 3 anéis seguidos sem quicar = modo fogo, que quebra até o vermelho

★ FASES INFINITAS
• Cada fase é diferente, e todas têm solução
• Torre chefe a cada 10 fases: mais alta, mais difícil e com prêmio triplo
• Mapa de mundos para acompanhar seu caminho
• Obstáculos novos: vidro que racha e quebra, anéis que somem, portais que pulam dois andares, anéis que giram e balançam
• Moedas flutuando no caminho

★ QUEDA LIVRE
• Modo infinito: até onde você consegue descer?
• Bata o seu recorde a cada partida

★ TODO DIA TEM NOVIDADE
• Desafio diário igual para todos
• Roleta da sorte diária
• Três missões por dia e baú de 7 dias
• Mais de 20 conquistas com recompensas

★ VISUAL PREMIUM
• 13 torres com cenários próprios: montanhas nevadas, cidade neon, vulcão, espaço, deserto, castelo e mais
• 23 bolas, de pérola e cromo até planeta Terra, galáxia e joia
• Rastros especiais: arco-íris, estrelas, fogo azul e pó de ouro
• Partículas, câmera lenta nos momentos de perigo, música por tema
• Leve e fluido, funciona sem internet

Grátis, com anúncios. Não precisa de conta.
""",
    captions=["Gire a torre e caia!", "Modo fogo quebra tudo", "Torres chefe a cada 10 fases",
              "Queda livre infinita", "13 torres, 23 bolas", "Cenários incríveis", "Roleta, missões e conquistas"],
)

L["en-US"] = dict(
    title="Prisma Roll: Helix Ball 3D",
    short="Spin the tower, bounce the ball down and smash rings on fire!",
    full="""Spin the tower with your finger and guide the bouncing ball ring by ring down to the golden goal. Fall through 3 rings in a row and the ball catches fire, smashing everything in its way! Prisma Roll is a fast, beautiful and addictive 3D game: easy to start, hard to put down.

★ HOW TO PLAY
• Drag to spin the tower
• Fall through the gaps and avoid the red parts
• 3 rings in a row without bouncing = fire mode, which smashes even red

★ ENDLESS LEVELS
• Every level is different, and every one is beatable
• A boss tower every 10 levels: taller, harder, triple reward
• World map to follow your journey
• New obstacles: glass that cracks and shatters, fading rings, portals that skip two floors, spinning and swinging rings
• Coins floating on the way down

★ FREE FALL
• Endless mode: how deep can you go?
• Beat your record every run

★ SOMETHING NEW EVERY DAY
• Daily challenge, the same for everyone
• Daily lucky wheel
• Three daily missions and a 7-day chest
• 20+ achievements with rewards

★ PREMIUM LOOK
• 13 towers with their own worlds: snowy peaks, neon city, volcano, space, desert, castle and more
• 23 balls, from pearl and chrome to planet Earth, galaxy and gem
• Special trails: rainbow, stars, blue fire and gold dust
• Particles, slow motion on close calls, music for each theme
• Light and smooth, works offline

Free to play, with ads. No account needed.
""",
    captions=["Spin the tower and fall!", "Fire mode smashes all", "Boss towers every 10 levels",
              "Endless free fall", "13 towers, 23 balls", "Amazing worlds", "Wheel, missions, achievements"],
)

L["es-ES"] = dict(
    title="Prisma Roll: Helix 3D",
    short="¡Gira la torre, haz rebotar la bola hasta el fondo y rompe anillos en llamas!",
    full="""Gira la torre con el dedo y guía la bola rebotando de anillo en anillo hasta la meta dorada. ¡Atraviesa 3 anillos seguidos y la bola se enciende, rompiendo todo a su paso! Prisma Roll es un juego 3D rápido, bonito y adictivo: fácil de empezar, difícil de dejar.

★ CÓMO JUGAR
• Arrastra para girar la torre
• Cae por los huecos y evita las partes rojas
• 3 anillos seguidos sin rebotar = modo fuego, que rompe hasta el rojo

★ NIVELES INFINITOS
• Cada nivel es diferente, y todos tienen solución
• Torre jefe cada 10 niveles: más alta, más difícil y premio triple
• Mapa de mundos para seguir tu camino
• Nuevos obstáculos: vidrio que se agrieta, anillos que desaparecen, portales que saltan dos pisos, anillos que giran
• Monedas flotando en el camino

★ CAÍDA LIBRE
• Modo infinito: ¿hasta dónde puedes bajar?
• Supera tu récord en cada partida

★ NOVEDADES CADA DÍA
• Desafío diario igual para todos
• Ruleta de la suerte diaria
• Tres misiones diarias y cofre de 7 días
• Más de 20 logros con recompensas

★ ASPECTO PREMIUM
• 13 torres con sus propios mundos: montañas nevadas, ciudad neón, volcán, espacio, desierto, castillo y más
• 23 bolas, de perla y cromo hasta la Tierra, galaxia y gema
• Estelas especiales: arcoíris, estrellas, fuego azul y polvo de oro
• Partículas, cámara lenta en los momentos de peligro, música por tema
• Ligero y fluido, funciona sin conexión

Gratis, con anuncios. No necesitas cuenta.
""",
    captions=["¡Gira la torre y cae!", "El modo fuego lo rompe todo", "Torres jefe cada 10 niveles",
              "Caída libre infinita", "13 torres, 23 bolas", "Mundos increíbles", "Ruleta, misiones y logros"],
)

L["fr-FR"] = dict(
    title="Prisma Roll: Helix 3D",
    short="Tourne la tour, fais rebondir la balle et brise les anneaux en feu !",
    full="""Tourne la tour du doigt et guide la balle qui rebondit d'anneau en anneau jusqu'à l'arrivée dorée. Traverse 3 anneaux d'affilée et la balle prend feu, brisant tout sur son passage ! Prisma Roll est un jeu 3D rapide, beau et addictif : facile à prendre en main, difficile à lâcher.

★ COMMENT JOUER
• Glisse pour tourner la tour
• Tombe par les trous et évite les parties rouges
• 3 anneaux d'affilée sans rebond = mode feu, qui brise même le rouge

★ NIVEAUX INFINIS
• Chaque niveau est différent, et tous sont faisables
• Une tour du boss tous les 10 niveaux : plus haute, plus dure, récompense triple
• Carte des mondes pour suivre ton parcours
• Nouveaux obstacles : verre qui se fissure, anneaux qui disparaissent, portails qui sautent deux étages, anneaux qui tournent
• Des pièces flottent sur le chemin

★ CHUTE LIBRE
• Mode infini : jusqu'où peux-tu descendre ?
• Bats ton record à chaque partie

★ DU NOUVEAU CHAQUE JOUR
• Défi du jour, le même pour tous
• Roue de la chance quotidienne
• Trois missions par jour et un coffre sur 7 jours
• Plus de 20 succès avec récompenses

★ UN LOOK PREMIUM
• 13 tours avec leurs propres mondes : pics enneigés, ville néon, volcan, espace, désert, château et plus
• 23 balles, de la perle et du chrome à la Terre, la galaxie et la gemme
• Traînées spéciales : arc-en-ciel, étoiles, feu bleu et poudre d'or
• Particules, ralenti dans les moments chauds, musique pour chaque thème
• Léger et fluide, fonctionne hors ligne

Gratuit, avec publicités. Aucun compte requis.
""",
    captions=["Tourne la tour et tombe !", "Le mode feu brise tout", "Un boss tous les 10 niveaux",
              "Chute libre infinie", "13 tours, 23 balles", "Des mondes superbes", "Roue, missions et succès"],
)

L["de-DE"] = dict(
    title="Prisma Roll: Helix 3D",
    short="Dreh den Turm, lass den Ball nach unten springen und zerschmettere Ringe!",
    full="""Dreh den Turm mit dem Finger und führe den springenden Ball Ring für Ring hinunter zum goldenen Ziel. Fall durch 3 Ringe in Folge und der Ball fängt Feuer und zerschmettert alles auf seinem Weg! Prisma Roll ist ein schnelles, schönes 3D-Spiel mit Suchtfaktor: leicht zu lernen, schwer aus der Hand zu legen.

★ SO WIRD GESPIELT
• Ziehen, um den Turm zu drehen
• Durch die Lücken fallen und die roten Teile meiden
• 3 Ringe in Folge ohne Aufprall = Feuermodus, der sogar Rot zerschmettert

★ ENDLOSE LEVEL
• Jedes Level ist anders, und jedes ist schaffbar
• Alle 10 Level ein Bossturm: höher, schwerer, dreifache Belohnung
• Weltkarte, um deinem Weg zu folgen
• Neue Hindernisse: Glas, das bricht, verschwindende Ringe, Portale, die zwei Etagen überspringen, drehende Ringe
• Münzen schweben auf dem Weg nach unten

★ FREIER FALL
• Endlosmodus: Wie tief schaffst du es?
• Knack deinen Rekord in jedem Lauf

★ JEDEN TAG ETWAS NEUES
• Tägliche Challenge, für alle gleich
• Tägliches Glücksrad
• Drei tägliche Missionen und eine 7-Tage-Truhe
• Über 20 Erfolge mit Belohnungen

★ PREMIUM-LOOK
• 13 Türme mit eigenen Welten: verschneite Gipfel, Neonstadt, Vulkan, Weltraum, Wüste, Burg und mehr
• 23 Bälle, von Perle und Chrom bis Erde, Galaxie und Juwel
• Besondere Spuren: Regenbogen, Sterne, blaues Feuer und Goldstaub
• Partikel, Zeitlupe bei knappen Momenten, Musik je Thema
• Leicht und flüssig, funktioniert offline

Kostenlos, mit Werbung. Kein Konto nötig.
""",
    captions=["Dreh den Turm und fall!", "Feuermodus zerschmettert alles", "Bossturm alle 10 Level",
              "Endloser freier Fall", "13 Türme, 23 Bälle", "Atemberaubende Welten", "Glücksrad, Missionen, Erfolge"],
)

L["tr-TR"] = dict(
    title="Prisma Roll: Helix 3D",
    short="Kuleyi çevir, topu aşağı zıplat ve alevle halkaları kır!",
    full="""Kuleyi parmağınla çevir ve zıplayan topu halka halka altın hedefe kadar indir. Üst üste 3 halkadan geç, top alev alsın ve yolundaki her şeyi kırsın! Prisma Roll hızlı, güzel ve bağımlılık yapan bir 3D oyun: başlaması kolay, bırakması zor.

★ NASIL OYNANIR
• Kuleyi çevirmek için sürükle
• Boşluklardan düş, kırmızı kısımlardan kaçın
• Sekmeden üst üste 3 halka = alev modu, kırmızıyı bile kırar

★ SONSUZ BÖLÜMLER
• Her bölüm farklı ve hepsi geçilebilir
• Her 10 bölümde bir patron kule: daha yüksek, daha zor, üç kat ödül
• Yolculuğunu takip etmek için dünya haritası
• Yeni engeller: çatlayıp kırılan cam, kaybolan halkalar, iki kat atlatan portallar, dönen halkalar
• Yolda süzülen altınlar

★ SERBEST DÜŞÜŞ
• Sonsuz mod: ne kadar derine inebilirsin?
• Her oyunda rekorunu kır

★ HER GÜN YENİ BİR ŞEY
• Herkes için aynı günlük görev
• Günlük şans çarkı
• Günde üç görev ve 7 günlük sandık
• Ödüllü 20'den fazla başarım

★ PREMIUM GÖRÜNÜM
• Kendi dünyası olan 13 kule: karlı zirveler, neon şehir, volkan, uzay, çöl, kale ve daha fazlası
• İnciden kroma, Dünya'dan galaksiye ve mücevhere 23 top
• Özel izler: gökkuşağı, yıldızlar, mavi ateş ve altın tozu
• Parçacıklar, tehlikeli anlarda ağır çekim, her temaya özel müzik
• Hafif ve akıcı, internetsiz çalışır

Ücretsiz, reklamlı. Hesap gerekmez.
""",
    captions=["Kuleyi çevir ve düş!", "Alev modu her şeyi kırar", "Her 10 bölümde patron",
              "Sonsuz serbest düşüş", "13 kule, 23 top", "Muhteşem dünyalar", "Çark, görevler, başarımlar"],
)

L["id"] = dict(
    title="Prisma Roll: Helix 3D",
    short="Putar menara, pantulkan bola ke bawah, dan hancurkan cincin dengan api!",
    full="""Putar menara dengan jarimu dan pandu bola yang memantul dari cincin ke cincin sampai ke garis finis emas. Lewati 3 cincin berturut-turut dan bola akan terbakar, menghancurkan semua yang dilewatinya! Prisma Roll adalah game 3D yang cepat, indah, dan bikin ketagihan: mudah dimulai, sulit berhenti.

★ CARA BERMAIN
• Geser untuk memutar menara
• Jatuh lewat celah dan hindari bagian merah
• 3 cincin berturut-turut tanpa memantul = mode api, bahkan menghancurkan merah

★ LEVEL TANPA BATAS
• Setiap level berbeda, dan semuanya bisa diselesaikan
• Menara bos setiap 10 level: lebih tinggi, lebih sulit, hadiah tiga kali
• Peta dunia untuk mengikuti perjalananmu
• Rintangan baru: kaca yang retak dan pecah, cincin yang menghilang, portal yang melompati dua lantai, cincin yang berputar
• Koin melayang di sepanjang jalan

★ TERJUN BEBAS
• Mode tanpa akhir: seberapa dalam kamu bisa turun?
• Pecahkan rekormu setiap permainan

★ SETIAP HARI ADA YANG BARU
• Tantangan harian yang sama untuk semua
• Roda keberuntungan harian
• Tiga misi harian dan peti 7 hari
• Lebih dari 20 pencapaian berhadiah

★ TAMPILAN PREMIUM
• 13 menara dengan dunianya sendiri: puncak bersalju, kota neon, gunung api, luar angkasa, gurun, kastil, dan lainnya
• 23 bola, dari mutiara dan krom sampai Bumi, galaksi, dan permata
• Jejak spesial: pelangi, bintang, api biru, dan debu emas
• Partikel, gerak lambat di saat genting, musik untuk setiap tema
• Ringan dan mulus, bisa dimainkan offline

Gratis, dengan iklan. Tanpa akun.
""",
    captions=["Putar menara dan jatuh!", "Mode api hancurkan semua", "Menara bos tiap 10 level",
              "Terjun bebas tanpa akhir", "13 menara, 23 bola", "Dunia yang menakjubkan", "Roda, misi, pencapaian"],
)

L["hi-IN"] = dict(
    title="Prisma Roll: Helix 3D",
    short="टावर घुमाएं, गेंद को नीचे उछालें और आग से रिंग तोड़ें!",
    full="""उंगली से टावर घुमाएं और उछलती गेंद को रिंग-दर-रिंग नीचे सुनहरे लक्ष्य तक ले जाएं। लगातार 3 रिंग पार करें और गेंद में आग लग जाएगी, जो रास्ते की हर चीज़ तोड़ देगी! Prisma Roll एक तेज़, सुंदर और मज़ेदार 3D गेम है: शुरू करना आसान, छोड़ना मुश्किल।

★ कैसे खेलें
• टावर घुमाने के लिए खींचें
• खाली जगहों से नीचे गिरें और लाल हिस्सों से बचें
• बिना उछले लगातार 3 रिंग = आग मोड, जो लाल को भी तोड़ देता है

★ अनगिनत लेवल
• हर लेवल अलग है, और हर लेवल पार किया जा सकता है
• हर 10 लेवल पर बॉस टावर: ज़्यादा ऊँचा, ज़्यादा कठिन, तीन गुना इनाम
• अपनी यात्रा देखने के लिए दुनिया का नक्शा
• नई बाधाएँ: चटकने और टूटने वाला कांच, गायब होने वाली रिंग, दो मंज़िल कुदाने वाले पोर्टल, घूमने वाली रिंग
• रास्ते में तैरते सिक्के

★ फ्री फॉल
• अंतहीन मोड: आप कितना नीचे जा सकते हैं?
• हर बार अपना रिकॉर्ड तोड़ें

★ हर दिन कुछ नया
• सबके लिए एक जैसी दैनिक चुनौती
• रोज़ का किस्मत का पहिया
• रोज़ तीन मिशन और 7 दिन का खज़ाना
• इनाम वाली 20 से ज़्यादा उपलब्धियाँ

★ प्रीमियम लुक
• अपनी दुनिया वाले 13 टावर: बर्फ़ीली चोटियाँ, नियॉन शहर, ज्वालामुखी, अंतरिक्ष, रेगिस्तान, महल और भी बहुत कुछ
• मोती और क्रोम से लेकर पृथ्वी, आकाशगंगा और रत्न तक 23 गेंदें
• खास ट्रेल: इंद्रधनुष, सितारे, नीली आग और सोने की धूल
• पार्टिकल, खतरे के पलों में स्लो मोशन, हर थीम का संगीत
• हल्का और स्मूथ, बिना इंटरनेट के चलता है

मुफ़्त, विज्ञापनों के साथ। किसी अकाउंट की ज़रूरत नहीं।
""",
    captions=["टावर घुमाएं और गिरें!", "आग मोड सब तोड़ देता है", "हर 10 लेवल पर बॉस",
              "अंतहीन फ्री फॉल", "13 टावर, 23 गेंदें", "शानदार दुनिया", "पहिया, मिशन, उपलब्धियाँ"],
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
