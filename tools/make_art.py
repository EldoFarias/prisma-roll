"""Prisma Roll art: launcher icons (all densities + adaptive), Play Store icon and feature graphic.

python3 tools/make_art.py <project_root>
Everything is drawn here (a glowing track in perspective with the ball), supersampled for smooth edges.
"""
import math, os, sys
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = sys.argv[1]
RES = os.path.join(ROOT, "app/src/main/res")
STORE = os.path.join(ROOT, "store")
os.makedirs(STORE, exist_ok=True)

RINGS = [(34, 211, 238), (129, 140, 248), (192, 132, 252), (244, 114, 182), (251, 146, 60)]
DANGER = (244, 63, 94)
FONT = "/usr/share/fonts/truetype/google-fonts/Poppins-Bold.ttf"


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


def vgrad(w, h, stops):
    """Vertical gradient through [(pos, rgb), ...]."""
    col = Image.new("RGB", (1, h))
    for y in range(h):
        t = y / max(1, h - 1)
        for i in range(len(stops) - 1):
            (p0, c0), (p1, c1) = stops[i], stops[i + 1]
            if p0 <= t <= p1:
                col.putpixel((0, y), lerp(c0, c1, (t - p0) / max(1e-6, p1 - p0)))
                break
    return col.resize((w, h))


def glow(img, cx, cy, r, color, alpha):
    if r < 2:
        return
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(layer).ellipse([cx - r, cy - r, cx + r, cy + r], fill=color + (int(255 * alpha),))
    img.alpha_composite(layer.filter(ImageFilter.GaussianBlur(r * 0.45)))


def sky(w, h):
    bg = vgrad(w, h, [(0, (12, 8, 38)), (0.55, (33, 15, 74)), (1, (60, 18, 84))]).convert("RGBA")
    glow(bg, w * 0.2, h * 0.15, w * 0.6, (124, 58, 237), 0.55)
    glow(bg, w * 0.95, h * 0.6, w * 0.5, (6, 182, 212), 0.35)
    glow(bg, w * 0.1, h * 1.0, w * 0.55, (219, 39, 119), 0.4)
    return bg


def mountains(img, horizon, color, amp, freq, seed):
    w, h = img.size
    d = ImageDraw.Draw(img)
    pts = [(0, h)]
    for x in range(0, w + 8, 8):
        u = x / w
        y = horizon - amp * (0.55 * abs(math.sin(u * freq + seed)) + 0.3 * abs(math.sin(u * freq * 2.3 + seed * 2)) + 0.15 * abs(math.sin(u * freq * 5.1 + seed)))
        pts.append((x, y))
    pts.append((w, h))
    d.polygon(pts, fill=color)


def annulus_mask(size, cx, cy, rx, ry, irx, iry, start, sweep):
    """Ring sector (degrees, 0 = right, clockwise like PIL)."""
    m = Image.new("L", size, 0)
    d = ImageDraw.Draw(m)
    d.pieslice([cx - rx, cy - ry, cx + rx, cy + ry], start, start + sweep, fill=255)
    d.ellipse([cx - irx, cy - iry, cx + irx, cy + iry], fill=0)
    return m


def ring(img, cx, cy, rx, color, gap_at, danger_at=None):
    ry = rx * 0.27
    th = ry * 0.5
    irx, iry = rx * 0.24, ry * 0.24
    gap = 46
    start, sweep = gap_at + gap, 360 - gap
    w, h = img.size
    # side (thickness)
    side = Image.new("RGBA", img.size, lerp(color, (0, 0, 0), 0.5) + (255,))
    img.paste(side, (0, 0), annulus_mask(img.size, cx, cy + th, rx, ry, irx, iry, start, sweep))
    # top face gradient
    top = vgrad(w, h, [(0, (255, 255, 255)), (max(0.0, (cy - ry) / h), lerp(color, (255, 255, 255), 0.45)), (min(1.0, (cy + ry) / h), color), (1, color)]).convert("RGBA")
    img.paste(top, (0, 0), annulus_mask(img.size, cx, cy, rx, ry, irx, iry, start, sweep))
    if danger_at is not None:
        dang = Image.new("RGBA", img.size, DANGER + (255,))
        img.paste(dang, (0, 0), annulus_mask(img.size, cx, cy, rx, ry, irx, iry, danger_at, 44))
    # front rim highlight
    d = ImageDraw.Draw(img)
    d.arc([cx - rx, cy - ry, cx + rx, cy + ry], 20, 160, fill=(255, 255, 255, 120), width=max(1, int(rx * 0.02)))


def tower(img, cx, top_y, unit, rings=4, ball=True):
    """Pillar with stacked rings and a glossy ball bouncing on the top ring."""
    rx = unit
    spacing = unit * 0.9
    pw = unit * 0.2
    bottom = top_y + spacing * (rings - 0.2)
    # pillar
    pill = Image.new("RGBA", img.size, (0, 0, 0, 0))
    pd = ImageDraw.Draw(pill)
    for i in range(int(pw * 2)):
        t = i / (pw * 2)
        c = lerp((43, 27, 94), (124, 105, 222), 1 - abs(t - 0.45) * 2)
        pd.line([(cx - pw + i, top_y - spacing * 0.8), (cx - pw + i, bottom)], fill=c + (255,))
    for k in range(rings * 2 + 1):
        y = top_y - spacing * 0.6 + k * spacing * 0.5
        pd.line([(cx - pw, y), (cx + pw, y)], fill=(34, 211, 238, 200), width=max(1, int(unit * 0.03)))
    img.alpha_composite(pill)
    # finial orb
    oy = top_y - spacing * 0.95
    glow(img, cx, oy, pw * 2.2, (232, 121, 249), 0.6)
    orb(img, cx, oy, pw * 0.95, (233, 213, 255), (147, 51, 234))
    # rings (top to bottom)
    gaps = [210, 300, 160, 250, 200]
    for i in range(rings):
        ring(img, cx, top_y + i * spacing, rx, RINGS[i % len(RINGS)], gaps[i % 5], danger_at=(330 if i == 1 else 40 if i == 3 else None))
    if ball:
        br = unit * 0.17
        bx = cx + rx * 0.5
        by = top_y - br * 0.9
        # trail
        for k in range(8, 0, -1):
            glow(img, bx, by - k * br * 0.55, br * (1 - k / 11), (244, 114, 182), 0.35 * (1 - k / 9))
        glow(img, bx, by, br * 3, (232, 121, 249), 0.55)
        orb(img, bx, by, br, (255, 255, 255), (232, 121, 249))
        # paint splat under the ball
        d = ImageDraw.Draw(img)
        d.ellipse([bx - br * 1.2, top_y + br * 0.05, bx + br * 1.2, top_y + br * 0.45], fill=(244, 114, 182, 180))


def orb(img, cx, cy, r, light, dark):
    if r < 1:
        return
    size = int(r * 2) + 4
    o = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    od = ImageDraw.Draw(o)
    steps = max(8, int(r))
    for i in range(steps, 0, -1):
        t = i / steps
        rr = r * t
        c = lerp(light, dark, t ** 0.8)
        ox = size / 2 - (1 - t) * r * 0.35
        oy = size / 2 - (1 - t) * r * 0.35
        od.ellipse([ox - rr, oy - rr, ox + rr, oy + rr], fill=c + (255,))
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse([size / 2 - r, size / 2 - r, size / 2 + r, size / 2 + r], fill=255)
    hl = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ImageDraw.Draw(hl).ellipse([size / 2 - r * 0.62, size / 2 - r * 0.62, size / 2 - r * 0.15, size / 2 - r * 0.2], fill=(255, 255, 255, 200))
    o.alpha_composite(hl.filter(ImageFilter.GaussianBlur(r * 0.08)))
    img.paste(o, (int(cx - size / 2), int(cy - size / 2)), mask)


TRACK_A = (34, 211, 238)
TRACK_B = (129, 140, 248)
RIM = (232, 121, 249)


def track(img, cx, top_y, bottom_y, half_bottom, half_top, ball=True, curve=0.0):
    """A track in perspective: tiles from the bottom (near) to the top (far), neon edges, the ball near."""
    w, h = img.size
    d = ImageDraw.Draw(img)
    n = 14
    def row(t):
        # t = 0 near, 1 far; perspective spacing.
        k = (1 - t) ** 2.2
        y = top_y + (bottom_y - top_y) * k
        half = half_top + (half_bottom - half_top) * k
        x = cx + curve * (t ** 2) * half_bottom * 1.6
        return x, y, half
    # Underside glow
    glow(img, cx, bottom_y, half_bottom * 1.1, RIM, 0.35)
    for i in range(n):
        x0, y0, h0 = row(i / n)
        x1, y1, h1 = row((i + 1) / n)
        col = TRACK_A if i % 2 == 0 else TRACK_B
        shade = 0.85 + 0.15 * (1 - i / n)
        c = tuple(int(v * shade) for v in col)
        d.polygon([(x0 - h0, y0), (x0 + h0, y0), (x1 + h1, y1), (x1 - h1, y1)], fill=c + (255,))
        # side faces (thickness)
        th = max(1.0, (y0 - y1) * 0.35 + h0 * 0.06)
        side = lerp(col, (10, 6, 30), 0.55)
        d.polygon([(x0 - h0, y0), (x1 - h1, y1), (x1 - h1, y1 + th * 0.6), (x0 - h0, y0 + th)], fill=side + (255,))
        d.polygon([(x0 + h0, y0), (x1 + h1, y1), (x1 + h1, y1 + th * 0.6), (x0 + h0, y0 + th)], fill=side + (255,))
        # glossy band
        d.line([(x0 - h0 * 0.7, y0 - (y0 - y1) * 0.25), (x0 + h0 * 0.7, y0 - (y0 - y1) * 0.25)], fill=(255, 255, 255, 60), width=max(1, int((y0 - y1) * 0.1)))
    # Neon edges
    for side in (-1, 1):
        pts = [(row(i / n)[0] + side * row(i / n)[2], row(i / n)[1]) for i in range(n + 1)]
        lay = Image.new("RGBA", img.size, (0, 0, 0, 0))
        ImageDraw.Draw(lay).line(pts, fill=RIM + (255,), width=max(2, int(half_bottom * 0.06)))
        img.alpha_composite(lay.filter(ImageFilter.GaussianBlur(half_bottom * 0.03)))
        ImageDraw.Draw(img).line(pts, fill=(255, 235, 255, 255), width=max(1, int(half_bottom * 0.025)))
    # A red block and coins further on
    bx, by, bh = row(0.55)
    bw = bh * 0.55
    d = ImageDraw.Draw(img)
    d.rectangle([bx + bh * 0.2, by - bw * 0.9, bx + bh * 0.2 + bw, by], fill=(244, 63, 94, 255))
    d.rectangle([bx + bh * 0.2, by - bw * 0.9, bx + bh * 0.2 + bw, by - bw * 0.75], fill=(255, 140, 160, 255))
    for k, t in enumerate((0.3, 0.4, 0.48)):
        x, y, hh = row(t)
        r = hh * 0.13
        glow(img, x - hh * 0.35, y - r * 1.6, r * 2.2, (255, 210, 80), 0.5)
        ImageDraw.Draw(img).ellipse([x - hh * 0.35 - r * 0.6, y - r * 2.6, x - hh * 0.35 + r * 0.6, y - r * 0.6], fill=(255, 205, 60, 255))
    if ball:
        x, y, hh = row(0.06)
        br = half_bottom * 0.32
        by = y - br * 0.95
        for k in range(8, 0, -1):
            glow(img, x, by + k * br * 0.32, br * (1 - k / 11), (244, 114, 182), 0.3 * (1 - k / 9))
        glow(img, x, by, br * 2.6, RIM, 0.5)
        ImageDraw.Draw(img).ellipse([x - br * 1.1, y - br * 0.2, x + br * 1.1, y + br * 0.25], fill=(0, 0, 0, 55))
        orb(img, x, by, br, (255, 255, 255), (232, 121, 249))


def icon_scene(S, fg_only=False):
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0)) if fg_only else sky(S, S)
    # Adaptive icons crop to the inner ~66%: keep the drawing smaller there.
    k = 0.72 if fg_only else 1.0
    cy = S * 0.5
    track(img, S / 2, cy - S * 0.3 * k, cy + S * 0.36 * k, S * 0.3 * k, S * 0.04 * k)
    return img


def rounded(img, size, round_mask):
    img = img.resize((size, size), Image.LANCZOS)
    mask = Image.new("L", (size, size), 0)
    md = ImageDraw.Draw(mask)
    if round_mask:
        md.ellipse([0, 0, size - 1, size - 1], fill=255)
    else:
        md.rounded_rectangle([0, 0, size - 1, size - 1], radius=size * 0.22, fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


big = icon_scene(2048)
fg = icon_scene(2048, fg_only=True)
for name, f in {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}.items():
    d = os.path.join(RES, f"mipmap-{name}")
    os.makedirs(d, exist_ok=True)
    rounded(big, int(48 * f), False).save(os.path.join(d, "ic_launcher.png"))
    rounded(big, int(48 * f), True).save(os.path.join(d, "ic_launcher_round.png"))
    fg.resize((int(108 * f), int(108 * f)), Image.LANCZOS).save(os.path.join(d, "ic_launcher_foreground.png"))
    sky(int(108 * f), int(108 * f)).save(os.path.join(d, "ic_launcher_background.png"))
big.convert("RGB").resize((512, 512), Image.LANCZOS).save(os.path.join(STORE, "icon-512.png"))

# Feature graphic 1024x500: scenery, the tower on the right, the title on the left.
W, H = 2048, 1000
fgx = sky(W, H)
mountains(fgx, H * 0.78, (58, 40, 110, 255), H * 0.22, 9, 1.0)
cloud = Image.new("RGBA", (W, H), (0, 0, 0, 0))
cd = ImageDraw.Draw(cloud)
for i in range(18):
    x = (i * 137) % W
    cd.ellipse([x - 160, H * 0.8 - 60, x + 160, H * 0.8 + 70], fill=(235, 225, 255, 150))
fgx.alpha_composite(cloud.filter(ImageFilter.GaussianBlur(30)))
mountains(fgx, H * 0.97, (30, 20, 70, 255), H * 0.14, 6, 2.2)
track(fgx, W * 0.74, H * 0.18, H * 1.02, H * 0.42, H * 0.05, curve=-0.25)
t = Image.new("RGBA", (W, H), (0, 0, 0, 0))
td = ImageDraw.Draw(t)
f1 = ImageFont.truetype(FONT, 210)
f2 = ImageFont.truetype(FONT, 120)
td.text((110, 250), "PRISMA", font=f1, fill=(255, 255, 255, 255))
td.text((118, 500), "R O L L", font=f2, fill=(255, 255, 255, 255))
gp = Image.new("RGB", (W, 1))
for x in range(W):
    gp.putpixel((x, 0), lerp((34, 211, 238), (232, 121, 249), min(1, x / (W * 0.45))))
grad = gp.resize((W, H)).convert("RGBA")
g = Image.new("RGBA", (W, H), (232, 121, 249, 0))
g.putalpha(t.split()[3].filter(ImageFilter.GaussianBlur(26)).point(lambda a: int(a * 0.75)))
fgx.alpha_composite(g)
fgx.alpha_composite(Image.composite(grad, Image.new("RGBA", (W, H), (0, 0, 0, 0)), t.split()[3]))
fgx.convert("RGB").resize((1024, 500), Image.LANCZOS).save(os.path.join(STORE, "feature-graphic-1024x500.png"))
print("done")
