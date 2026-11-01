"""Promotional phone screenshots (1080x1920) for the Play Store, drawn with the same art as make_art.py.

python tools/make_shots.py <project_root>
Writes store/screenshots/<locale>/01.png ... for pt-BR and en-US (other languages show the pt-BR set).
"""
import math, os, sys
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = sys.argv[1]
OUT = os.path.join(ROOT, "store", "screenshots")

# Reuse the drawing helpers of make_art.py (everything before its main code).
src = open(os.path.join(ROOT, "tools", "make_art.py"), encoding="utf-8").read()
src = src[: src.index("big = icon_scene")].replace("ROOT = sys.argv[1]", "ROOT = sys.argv[1]\n")
A = {"__name__": "make_art"}
exec(compile(src, "make_art.py", "exec"), A)
sky, glow, orb, tower, ring, mountains, lerp, vgrad = (A[k] for k in ("sky", "glow", "orb", "tower", "ring", "mountains", "lerp", "vgrad"))

W, H = 1080, 1920
FONTS = [r"C:\Windows\Fonts\seguibl.ttf", r"C:\Windows\Fonts\arialbd.ttf",
         "/usr/share/fonts/truetype/google-fonts/Poppins-Bold.ttf", "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"]
FONT = next(f for f in FONTS if os.path.exists(f))

CAPTIONS = {
    "pt-BR": [("GIRE A TORRE", "e faça a bola cair!"), ("MODO FOGO", "quebra tudo no caminho"),
              ("TORRES CHEFE", "a cada 10 fases"), ("QUEDA LIVRE", "infinita: bata seu recorde"),
              ("13 TORRES", "23 bolas e 7 rastros"), ("ROLETA", "missões e conquistas")],
    "en-US": [("SPIN THE TOWER", "and drop the ball!"), ("FIRE MODE", "smashes everything"),
              ("BOSS TOWERS", "every 10 levels"), ("FREE FALL", "endless: beat your record"),
              ("13 TOWERS", "23 balls and 7 trails"), ("LUCKY WHEEL", "missions and achievements")],
}


def scenery():
    img = sky(W, H)
    mountains(img, H * 0.93, (58, 40, 110, 255), H * 0.08, 7, 1.3)
    mountains(img, H * 1.0, (30, 20, 70, 255), H * 0.05, 5, 2.4)
    return img


def text_fit(d, txt, max_w, size):
    while size > 30:
        f = ImageFont.truetype(FONT, size)
        if d.textlength(txt, font=f) <= max_w:
            return f
        size -= 4
    return ImageFont.truetype(FONT, size)


def caption(img, title, sub):
    """Big gradient title with glow and a white subtitle at the top."""
    t = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    td = ImageDraw.Draw(t)
    f1 = text_fit(td, title, W - 120, 132)
    tw = td.textlength(title, font=f1)
    td.text(((W - tw) / 2, 110), title, font=f1, fill=(255, 255, 255, 255))
    g = Image.new("RGBA", (W, H), (232, 121, 249, 0))
    g.putalpha(t.split()[3].filter(ImageFilter.GaussianBlur(22)).point(lambda a: int(a * 0.8)))
    img.alpha_composite(g)
    gp = Image.new("RGB", (W, 1))
    for x in range(W):
        gp.putpixel((x, 0), lerp((34, 211, 238), (232, 121, 249), x / W))
    img.alpha_composite(Image.composite(gp.resize((W, H)).convert("RGBA"), Image.new("RGBA", (W, H), (0, 0, 0, 0)), t.split()[3]))
    d = ImageDraw.Draw(img)
    f2 = text_fit(d, sub, W - 140, 64)
    sw = d.textlength(sub, font=f2)
    d.text(((W - sw) / 2, 290), sub, font=f2, fill=(255, 255, 255, 235))


def finger(img, cx, cy):
    """Swipe hint: curved double arrow and a fingertip."""
    d = ImageDraw.Draw(img)
    r = 230
    d.arc([cx - r, cy - 70, cx + r, cy + 70], 20, 160, fill=(255, 255, 255, 220), width=12)
    for sx in (-1, 1):
        x, y = cx + sx * r * 0.94, cy + 24
        d.polygon([(x + sx * 34, y - 10), (x - sx * 18, y - 40), (x - sx * 4, y + 34)], fill=(255, 255, 255, 230))
    glow(img, cx, cy + 70, 70, (255, 255, 255), 0.35)
    d.ellipse([cx - 38, cy + 32, cx + 38, cy + 108], fill=(255, 255, 255, 240))


def shot_spin():
    img = scenery()
    tower(img, W / 2, H * 0.36, 330, rings=5)
    finger(img, W / 2, H * 0.86)
    return img


def fire_ball(img, bx, by, br):
    for k in range(14, 0, -1):
        t = k / 14
        glow(img, bx + math.sin(k * 0.9) * br * 0.15 * t, by - k * br * 0.42, br * (1.15 - t * 0.6),
             lerp((255, 220, 90), (239, 68, 68), t), 0.55 * (1 - t * 0.7))
    glow(img, bx, by, br * 3.2, (251, 146, 60), 0.75)
    orb(img, bx, by, br, (255, 251, 220), (249, 115, 22))


def shards(img, cx, cy, n, spread, colors, seed):
    d = ImageDraw.Draw(img)
    for i in range(n):
        a = (i * 137.5 + seed) % 360
        rr = spread * (0.4 + ((i * 53) % 60) / 100)
        x, y = cx + math.cos(math.radians(a)) * rr, cy + math.sin(math.radians(a)) * rr * 0.6
        s = 18 + (i * 7) % 26
        rot = math.radians(a * 2)
        pts = [(x + math.cos(rot + k * 2.1) * s, y + math.sin(rot + k * 2.1) * s * 0.6) for k in range(3)]
        d.polygon(pts, fill=colors[i % len(colors)] + (235,))


def shot_fire():
    img = scenery()
    unit, top, sp = 330, H * 0.5, 330 * 0.9
    # Pillar + rings below the ball, the top ring exploding.
    pill = ImageDraw.Draw(img)
    pill.rectangle([W / 2 - 66, top - 40, W / 2 + 66, H], fill=(70, 50, 150, 255))
    for i in range(3):
        ring(img, W / 2, top + i * sp, unit, A["RINGS"][(i + 2) % 5], [200, 300, 150][i], danger_at=(330 if i == 1 else None))
    shards(img, W / 2, top - 90, 26, 420, [(244, 114, 182), (251, 146, 60), (192, 132, 252), (255, 255, 255)], 11)
    fire_ball(img, W / 2 + 120, top - 150, 64)
    d = ImageDraw.Draw(img)
    f = ImageFont.truetype(FONT, 110)
    combo = "x5"
    glow(img, W * 0.24, H * 0.27, 120, (251, 146, 60), 0.5)
    d.text((W * 0.24 - d.textlength(combo, font=f) / 2, H * 0.27 - 70), combo, font=f, fill=(255, 237, 160, 255))
    return img


def shot_boss():
    img = sky(W, H)
    glow(img, W / 2, H * 0.55, W * 0.7, (239, 68, 68), 0.35)
    mountains(img, H * 0.95, (70, 22, 50, 255), H * 0.09, 6, 0.7)
    saved = A["RINGS"]
    A["RINGS"] = [(250, 204, 21), (251, 146, 60), (239, 68, 68), (250, 204, 21), (251, 146, 60), (239, 68, 68)]
    tower(img, W / 2, H * 0.45, 245, rings=6)
    A["RINGS"] = saved
    # Crown above the finial.
    d = ImageDraw.Draw(img)
    cx, cy = W / 2, H * 0.45 - 300
    glow(img, cx, cy, 150, (250, 204, 21), 0.55)
    d.polygon([(cx - 110, cy + 50), (cx - 120, cy - 60), (cx - 55, cy - 5), (cx, cy - 90), (cx + 55, cy - 5),
               (cx + 120, cy - 60), (cx + 110, cy + 50)], fill=(250, 204, 21, 255))
    d.rectangle([cx - 110, cy + 38, cx + 110, cy + 62], fill=(217, 119, 6, 255))
    for x in (-70, 0, 70):
        orb(img, cx + x, cy + 8, 14, (255, 255, 255), (239, 68, 68))
    f = ImageFont.truetype(FONT, 96)
    glow(img, W * 0.8, H * 0.27, 110, (250, 204, 21), 0.45)
    d.text((W * 0.8 - d.textlength("x3", font=f) / 2, H * 0.27 - 60), "x3", font=f, fill=(255, 237, 160, 255))
    return img


def shot_fall():
    img = scenery()
    d = ImageDraw.Draw(img)
    d.rectangle([W / 2 - 40, H * 0.2, W / 2 + 40, H], fill=(70, 50, 150, 255))
    # Rings shrinking into the distance (view from above, falling down).
    for i in range(9, -1, -1):
        s = 0.55 ** (i * 0.42)
        ring(img, W / 2, H * 0.47 + (1 - s) * -H * 0.06 + s * H * 0.36 - H * 0.2, 470 * s,
             A["RINGS"][i % 5], (i * 67) % 360, danger_at=((i * 101) % 360 if i % 3 == 1 else None))
    fire_ball(img, W / 2 + 170, H * 0.62, 70)
    f = ImageFont.truetype(FONT, 100)
    num = "1000 m"
    d = ImageDraw.Draw(img)
    d.text((W / 2 - d.textlength(num, font=f) / 2, H * 0.86), num, font=f, fill=(255, 255, 255, 255))
    return img


def shot_skins():
    img = scenery()
    pairs = [((255, 255, 255), (232, 121, 249)), ((255, 251, 220), (249, 115, 22)), ((207, 250, 254), (6, 182, 212)),
             ((220, 252, 231), (22, 163, 74)), ((254, 226, 226), (220, 38, 38)), ((254, 249, 195), (202, 138, 4)),
             ((224, 231, 255), (79, 70, 229)), ((252, 231, 243), (219, 39, 119)), ((241, 245, 249), (71, 85, 105)),
             ((255, 255, 255), (14, 165, 233)), ((236, 252, 203), (101, 163, 13)), ((250, 232, 255), (147, 51, 234))]
    d = ImageDraw.Draw(img)
    cols, cell = 3, 270
    x0, y0 = (W - cols * cell) / 2, H * 0.27
    for i, (lt, dk) in enumerate(pairs):
        cx, cy = x0 + (i % cols + 0.5) * cell, y0 + (i // cols + 0.5) * cell
        d.rounded_rectangle([cx - 115, cy - 115, cx + 115, cy + 115], radius=36, fill=(255, 255, 255, 28),
                            outline=(255, 255, 255, 70) if i else (250, 204, 21, 255), width=4 if i == 0 else 2)
        glow(img, cx, cy, 90, dk, 0.45)
        orb(img, cx, cy, 70, lt, dk)
        d = ImageDraw.Draw(img)
    return img


def shot_wheel():
    img = scenery()
    cx, cy, r = W / 2, H * 0.55, 400
    glow(img, cx, cy, r * 1.25, (192, 132, 252), 0.45)
    d = ImageDraw.Draw(img)
    cols = A["RINGS"] + [(250, 204, 21), (34, 197, 94), (239, 68, 68)]
    n = 8
    for i in range(n):
        d.pieslice([cx - r, cy - r, cx + r, cy + r], i * 360 / n - 90, (i + 1) * 360 / n - 90, fill=cols[i] + (255,))
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=(255, 255, 255, 255), width=18)
    for i in range(n):
        a = math.radians(i * 360 / n - 90 + 180 / n)
        orb(img, cx + math.cos(a) * r * 0.66, cy + math.sin(a) * r * 0.66, 34, (255, 251, 220), (202, 138, 4))
    d = ImageDraw.Draw(img)
    for i in range(16):
        a = math.radians(i * 22.5)
        x, y = cx + math.cos(a) * (r + 2), cy + math.sin(a) * (r + 2)
        d.ellipse([x - 12, y - 12, x + 12, y + 12], fill=(255, 251, 220, 255))
    orb(img, cx, cy, 70, (255, 255, 255), (147, 51, 234))
    d = ImageDraw.Draw(img)
    d.polygon([(cx - 50, cy - r - 70), (cx + 50, cy - r - 70), (cx, cy - r + 40)], fill=(255, 255, 255, 255))
    return img


SHOTS = [shot_spin, shot_fire, shot_boss, shot_fall, shot_skins, shot_wheel]
bases = [f() for f in SHOTS]
for loc, caps in CAPTIONS.items():
    os.makedirs(os.path.join(OUT, loc), exist_ok=True)
    for i, (base, (t, s)) in enumerate(zip(bases, caps)):
        img = base.copy()
        caption(img, t, s)
        img.convert("RGB").save(os.path.join(OUT, loc, f"{i + 1:02d}.png"), optimize=True)
print("done")
