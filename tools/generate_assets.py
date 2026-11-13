#!/usr/bin/env python3
"""Pembuat aset Susun Kata (Hanz Dev Official): latar, ikon, efek suara.
Semua aset dibuat prosedural dari kode ini (tanpa gambar/suara pihak ketiga).
Pakai:  python3 tools/generate_assets.py   (butuh Pillow dan numpy)
"""
import math, wave, random
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
FONT = "/usr/share/fonts/opentype/inter/Inter-Black.otf"
W, H = 720, 1280


# ---------- latar belakang ----------
def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def sky(top, mid, bot, horizon=0.55):
    y = np.linspace(0, 1, H)[:, None]
    img = np.zeros((H, W, 3), np.float32)
    for i in range(3):
        a = np.where(y < horizon, top[i] + (mid[i] - top[i]) * (y / horizon), mid[i] + (bot[i] - mid[i]) * ((y - horizon) / (1 - horizon)))
        img[:, :, i] = a
    return Image.fromarray(img.astype(np.uint8))


def glow(img, cx, cy, r, color, strength=0.9):
    yy, xx = np.mgrid[0:H, 0:W]
    d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2) / r
    a = np.clip(1 - d, 0, 1) ** 2 * strength
    arr = np.asarray(img).astype(np.float32)
    for i in range(3):
        arr[:, :, i] = arr[:, :, i] * (1 - a) + color[i] * a
    return Image.fromarray(arr.clip(0, 255).astype(np.uint8))


def clouds(img, rng, n, ytop, ybot, color=(255, 255, 255), alpha=150):
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for _ in range(n):
        cx, cy = rng.randint(-60, W + 60), rng.randint(ytop, ybot)
        for _ in range(rng.randint(5, 9)):
            rx, ry = rng.randint(50, 120), rng.randint(18, 40)
            ox, oy = rng.randint(-100, 100), rng.randint(-14, 14)
            d.ellipse([cx + ox - rx, cy + oy - ry, cx + ox + rx, cy + oy + ry], fill=color + (alpha,))
    layer = layer.filter(ImageFilter.GaussianBlur(14))
    return Image.alpha_composite(img.convert("RGBA"), layer).convert("RGB")


def ridge_y(rng, base, amp, k=4):
    x = np.arange(W)
    y = np.full(W, float(base))
    for i in range(k):
        f = rng.uniform(0.004, 0.012) * (1.9 ** i)
        y += amp / (1.7 ** i) * np.sin(x * f * 2 * math.pi / 6 + rng.uniform(0, 6.28))
    return y


def fill_ridge(img, ys, color, haze=0.0, haze_color=(255, 255, 255), terrace=None):
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    pts = [(x, float(ys[x])) for x in range(W)] + [(W, H), (0, H)]
    col = lerp(color, haze_color, haze)
    d.polygon(pts, fill=col + (255,))
    if terrace:  # garis teras sawah
        light = lerp(color, (230, 255, 120), 0.35)
        dark = lerp(color, (10, 60, 20), 0.35)
        for k in range(0, H, terrace):
            off = [int(math.sin((x + k * 3) * 0.012) * 10) for x in range(0, W, 8)]
            pl = [(x, k + off[i]) for i, x in enumerate(range(0, W, 8))]
            d.line(pl, fill=light + (130,), width=3)
            d.line([(x, y + 4) for x, y in pl], fill=dark + (110,), width=3)
        mask = Image.new("L", (W, H), 0)
        ImageDraw.Draw(mask).polygon(pts, fill=255)
        layer.putalpha(Image.fromarray(np.minimum(np.asarray(layer.split()[3]), np.asarray(mask))))
    return Image.alpha_composite(img.convert("RGBA"), layer).convert("RGB")


def finish(img, rng, name):
    arr = np.asarray(img).astype(np.float32)
    yy, xx = np.mgrid[0:H, 0:W]
    v = 1 - 0.28 * (((xx - W / 2) / (W / 2)) ** 2 + ((yy - H / 2) / (H / 2)) ** 2) / 2
    arr *= v[:, :, None]
    arr += np.random.default_rng(rng.randint(0, 9999)).normal(0, 1.6, arr.shape)
    Image.fromarray(arr.clip(0, 255).astype(np.uint8)).save(RES / "drawable-nodpi" / f"bg_{name}.jpg", quality=84, optimize=True)


def bg_sawah(rng):
    img = sky((92, 150, 214), (170, 205, 235), (210, 228, 240))
    img = clouds(img, rng, 7, 100, 420)
    far = (70, 120, 110)
    for i, (b, a, c, hz) in enumerate([(560, 70, far, 0.55), (650, 55, (60, 125, 85), 0.35), (760, 40, (70, 150, 60), 0.15)]):
        img = fill_ridge(img, ridge_y(rng, b, a), c, hz, (200, 225, 235))
    img = fill_ridge(img, ridge_y(rng, 860, 26, 3), (96, 168, 40), 0.0, terrace=34)
    img = fill_ridge(img, ridge_y(rng, 1060, 30, 3), (60, 140, 36), 0.0, terrace=44)
    img = fill_ridge(img, ridge_y(rng, 1200, 20, 3), (34, 100, 30), 0.0)
    finish(img, rng, "1")


def bg_pantai(rng):
    img = sky((255, 150, 90), (255, 205, 140), (255, 235, 190), 0.5)
    img = glow(img, 360, 640, 360, (255, 245, 200), 0.95)
    img = clouds(img, rng, 5, 160, 460, (255, 225, 200), 130)
    arr = np.asarray(img).copy()
    sea = np.zeros((H - 640, W, 3), np.float32)
    t = np.linspace(0, 1, H - 640)[:, None]
    for i, (a, b) in enumerate(zip((40, 150, 190), (20, 100, 150))):
        sea[:, :, i] = a + (b - a) * t
    arr[640:, :, :] = sea.astype(np.uint8)
    img = Image.fromarray(arr)
    d = ImageDraw.Draw(img, "RGBA")
    for _ in range(140):
        y = rng.randint(650, 1000)
        x = rng.randint(0, W)
        ln = rng.randint(20, 90) * (0.4 + (y - 640) / 400)
        d.line([(x, y), (x + ln, y)], fill=(255, 240, 210, rng.randint(40, 110)), width=2)
    img = fill_ridge(img, ridge_y(rng, 1010, 22, 3), (232, 200, 140), 0.0)
    img = fill_ridge(img, ridge_y(rng, 1130, 18, 3), (214, 176, 112), 0.0)
    for px in (90, 620):  # palem
        d = ImageDraw.Draw(img)
        d.line([(px, 1280), (px - 20, 900)], fill=(70, 45, 30), width=18)
        for ang in range(-80, 100, 32):
            ex, ey = px - 20 + 150 * math.sin(math.radians(ang)), 900 - 40 + 70 * abs(math.cos(math.radians(ang))) - 70
            d.line([(px - 20, 900), (ex, ey + 60)], fill=(30, 110, 50), width=10)
    finish(img, rng, "2")


def bg_gunung(rng):
    img = sky((120, 150, 200), (200, 215, 235), (235, 240, 245), 0.5)
    img = clouds(img, rng, 8, 120, 520, (255, 255, 255), 170)
    cols = [(150, 168, 200), (118, 140, 176), (86, 112, 140), (52, 84, 100), (34, 66, 66)]
    for i, c in enumerate(cols):
        img = fill_ridge(img, ridge_y(rng, 470 + i * 150, 110 - i * 14, 5), c, max(0, 0.5 - i * 0.12), (225, 232, 245))
    finish(img, rng, "3")


def bg_hutan(rng):
    img = sky((30, 90, 70), (80, 150, 100), (150, 200, 120), 0.45)
    img = glow(img, 520, 220, 380, (255, 255, 190), 0.55)
    img = clouds(img, rng, 4, 80, 300, (230, 255, 230), 90)
    cols = [(60, 120, 90), (40, 100, 70), (28, 82, 54), (18, 62, 40), (10, 44, 28)]
    for i, c in enumerate(cols):
        ys = ridge_y(rng, 520 + i * 170, 60, 6)
        img = fill_ridge(img, ys, c, max(0, 0.4 - i * 0.1), (170, 220, 170))
    d = ImageDraw.Draw(img, "RGBA")
    for _ in range(26):  # pohon
        x, y = rng.randint(0, W), rng.randint(760, 1240)
        s = 0.6 + (y - 760) / 480
        d.rectangle([x - 4 * s, y - 40 * s, x + 4 * s, y], fill=(40, 28, 18, 255))
        for k in range(3):
            d.polygon([(x - (50 - k * 10) * s, y - (30 + k * 38) * s), (x + (50 - k * 10) * s, y - (30 + k * 38) * s), (x, y - (90 + k * 38) * s)], fill=(14, 70 + k * 12, 38, 255))
    finish(img, rng, "4")


def bg_senja(rng):
    img = sky((70, 40, 110), (230, 100, 110), (255, 175, 90), 0.52)
    img = glow(img, 360, 700, 330, (255, 210, 120), 0.95)
    img = clouds(img, rng, 7, 160, 560, (255, 170, 150), 150)
    for i, c in enumerate([(120, 60, 110), (84, 40, 92), (52, 28, 70), (30, 18, 48)]):
        img = fill_ridge(img, ridge_y(rng, 760 + i * 130, 60 - i * 8, 5), c, max(0, 0.35 - i * 0.1), (255, 160, 120))
    finish(img, rng, "5")


def bg_malam(rng):
    img = sky((6, 10, 40), (22, 36, 86), (60, 70, 120), 0.6)
    img = glow(img, 540, 250, 260, (210, 225, 255), 0.6)
    d = ImageDraw.Draw(img, "RGBA")
    for _ in range(220):
        x, y = rng.randint(0, W), rng.randint(0, 800)
        r = rng.choice([1, 1, 1, 2])
        d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255, rng.randint(120, 255)))
    d.ellipse([500, 210, 580, 290], fill=(250, 248, 230, 255))
    d.ellipse([522, 200, 600, 280], fill=(26, 40, 90, 255))
    for i, c in enumerate([(40, 56, 110), (26, 40, 84), (16, 26, 58), (8, 14, 36)]):
        img = fill_ridge(img, ridge_y(rng, 720 + i * 150, 70 - i * 10, 5), c, 0.0)
    finish(img, rng, "6")


# ---------- ikon ----------
def rr_gradient(size, top, bot, radius):
    g = Image.new("RGB", (size, size))
    px = np.zeros((size, size, 3), np.uint8)
    t = np.linspace(0, 1, size)[:, None]
    for i in range(3):
        px[:, :, i] = (top[i] + (bot[i] - top[i]) * t).astype(np.uint8)
    g = Image.fromarray(px)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(g, (0, 0), mask)
    return out


def tiles(size, pad):
    """Empat ubin huruf K A T A (2x2) di area aman."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    inner = size - 2 * pad
    gap = inner * 0.06
    t = (inner - gap) / 2
    font = ImageFont.truetype(FONT, int(t * 0.78))
    for idx, ch in enumerate("KATA"):
        r, c = divmod(idx, 2)
        x0, y0 = pad + c * (t + gap), pad + r * (t + gap)
        sh = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        ImageDraw.Draw(sh).rounded_rectangle([x0 + 4, y0 + 8, x0 + t + 4, y0 + t + 8], radius=t * 0.2, fill=(0, 0, 0, 90))
        img = Image.alpha_composite(img, sh.filter(ImageFilter.GaussianBlur(size * 0.01)))
        d = ImageDraw.Draw(img)
        col = (255, 244, 214)
        d.rounded_rectangle([x0, y0, x0 + t, y0 + t], radius=t * 0.2, fill=col)
        tx = d.textbbox((0, 0), ch, font=font)
        d.text((x0 + t / 2 - (tx[0] + tx[2]) / 2, y0 + t / 2 - (tx[1] + tx[3]) / 2), ch, font=font, fill=(24, 120, 36))
    return img


def make_icons():
    master = 1024
    bg = rr_gradient(master, (92, 205, 70), (16, 120, 60), int(master * 0.22))
    icon = Image.alpha_composite(bg, tiles(master, int(master * 0.2)))
    sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for dpi, s in sizes.items():
        icon.resize((s, s), Image.LANCZOS).save(RES / f"mipmap-{dpi}" / "ic_launcher.png")
        rnd = Image.new("L", (master, master), 0)
        ImageDraw.Draw(rnd).ellipse([0, 0, master - 1, master - 1], fill=255)
        rimg = Image.new("RGBA", (master, master), (0, 0, 0, 0))
        rimg.paste(icon, (0, 0), rnd)
        rimg.resize((s, s), Image.LANCZOS).save(RES / f"mipmap-{dpi}" / "ic_launcher_round.png")
    # adaptif 432x432: latar penuh + ubin di zona aman (tengah 66%)
    full = Image.new("RGB", (432, 432))
    px = np.zeros((432, 432, 3), np.uint8)
    t = np.linspace(0, 1, 432)[:, None]
    for i, (a, b) in enumerate(zip((92, 205, 70), (16, 120, 60))):
        px[:, :, i] = (a + (b - a) * t).astype(np.uint8)
    Image.fromarray(px).save(RES / "drawable-nodpi" / "ic_launcher_bg.png")
    tiles(432, 92).save(RES / "drawable-nodpi" / "ic_launcher_fg.png")
    (RES / "mipmap-anydpi-v26").mkdir(exist_ok=True)
    xml = '<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n    <background android:drawable="@drawable/ic_launcher_bg"/>\n    <foreground android:drawable="@drawable/ic_launcher_fg"/>\n</adaptive-icon>\n'
    (RES / "mipmap-anydpi-v26" / "ic_launcher.xml").write_text(xml)
    (RES / "mipmap-anydpi-v26" / "ic_launcher_round.xml").write_text(xml)
    icon.resize((512, 512), Image.LANCZOS).save(ROOT / "tools" / "icon_preview_512.png")


# ---------- suara ----------
SR = 22050


def tone(freq, dur, vol=0.6, decay=5.0, harm=(1.0, 0.35, 0.12), attack=0.004):
    n = int(SR * dur)
    t = np.arange(n) / SR
    s = sum(a * np.sin(2 * math.pi * freq * (i + 1) * t) for i, a in enumerate(harm))
    env = np.exp(-decay * t / dur) * np.minimum(1, t / attack)
    return (s * env * vol / sum(harm)).astype(np.float32)


def seq(notes, gap=0.0):
    parts = []
    for f, d, v in notes:
        parts.append(tone(f, d, v))
        if gap:
            parts.append(np.zeros(int(SR * gap), np.float32))
    return np.concatenate(parts)


def mix(*tracks):
    n = max(len(t) for t in tracks)
    out = np.zeros(n, np.float32)
    for t in tracks:
        out[: len(t)] += t
    return out


def write_wav(name, data):
    data = np.clip(data, -1, 1)
    peak = np.max(np.abs(data)) or 1
    data = data / max(peak, 1) * 0.89  # sisakan headroom sekitar -1 dBFS
    pcm = (data * 32767).astype(np.int16)
    with wave.open(str(RES / "raw" / f"{name}.wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())


def make_sounds():
    (RES / "raw").mkdir(exist_ok=True)
    penta = [261.63, 293.66, 329.63, 392.0, 440.0, 523.25, 587.33, 659.25]
    for i, f in enumerate(penta):
        write_wav(f"snd_tap{i}", tone(f, 0.14, 0.7, 7))
    write_wav("snd_ok", seq([(523.25, 0.11, 0.7), (659.25, 0.11, 0.7), (783.99, 0.22, 0.8)]))
    write_wav("snd_bonus", seq([(880, 0.07, 0.6), (1108.7, 0.07, 0.6), (1318.5, 0.07, 0.6), (1760, 0.22, 0.7)]))
    n = int(SR * 0.22)
    t = np.arange(n) / SR
    buzz = 0.5 * np.sign(np.sin(2 * math.pi * (190 - 80 * t / 0.22) * t)) * np.exp(-6 * t / 0.22)
    write_wav("snd_salah", (buzz * 0.5).astype(np.float32))
    fan = [(523.25, 0.14, 0.7), (659.25, 0.14, 0.7), (783.99, 0.14, 0.7), (1046.5, 0.5, 0.85)]
    arp = seq(fan)
    chord = mix(tone(523.25, 0.9, 0.35, 3), tone(659.25, 0.9, 0.3, 3), tone(783.99, 0.9, 0.3, 3))
    pad = np.concatenate([np.zeros(int(SR * 0.42), np.float32), chord])
    write_wav("snd_menang", mix(arp, pad))
    write_wav("snd_koin", seq([(1318.5, 0.06, 0.6), (1760, 0.18, 0.7)]))
    n = int(SR * 0.12)
    t = np.arange(n) / SR
    pop = np.sin(2 * math.pi * (300 + 900 * t / 0.12) * t) * np.exp(-9 * t / 0.12) * 0.7
    write_wav("snd_petunjuk", pop.astype(np.float32))
    write_wav("snd_klik", tone(660, 0.05, 0.5, 9, harm=(1.0, 0.2)))
    write_wav("snd_acak", mix(tone(440, 0.1, 0.4, 6), np.concatenate([np.zeros(int(SR * 0.05), np.float32), tone(554.37, 0.1, 0.4, 6)])))


if __name__ == "__main__":
    rng = random.Random(7)
    for fn in (bg_sawah, bg_pantai, bg_gunung, bg_hutan, bg_senja, bg_malam):
        fn(rng)
        print("latar", fn.__name__)
    make_icons()
    make_sounds()
    print("selesai")
