#!/usr/bin/env python3
"""
Generates the placeholder media for AboLolo:
  - app/src/main/res/drawable-nodpi/presplash.png   (shown for 3 seconds at start)
  - app/src/main/res/mipmap-*/ic_launcher.png
  - app/src/main/res/raw/sfx_*.wav                  (sound effects)
  - app/src/main/res/raw/endgamelolo.ogg            (ending song)

You can replace any of these files with your own (keep the same file name).
Run:  python3 tools/generate_assets.py     (needs numpy, pillow, ffmpeg)
"""
import math
import os
import random
import subprocess
import wave

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")


def ensure(path):
    os.makedirs(path, exist_ok=True)
    return path


# --------------------------------------------------------------------------
# Image helpers
# --------------------------------------------------------------------------
def glow(img, xy, r, color, strength=1.0, blur=None):
    """Additive soft glow disc."""
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    x, y = xy
    d.ellipse((x - r, y - r, x + r, y + r), fill=color + (int(255 * strength),))
    layer = layer.filter(ImageFilter.GaussianBlur(blur or r * 0.8))
    img.alpha_composite(layer)


def draw_zombie_head(img, cx, cy, s):
    """Big zombie head: black face, green wrap, black agal ring, glowing eyes."""
    d = ImageDraw.Draw(img)
    # rim light so the black silhouette reads on a black background
    glow(img, (cx, cy + 20 * s), 150 * s, (90, 60, 30), 0.35, 120 * s)
    d = ImageDraw.Draw(img)
    # shoulders
    d.rounded_rectangle((cx - 150 * s, cy + 90 * s, cx + 150 * s, cy + 330 * s), 70 * s, fill=(14, 14, 18, 255), outline=(60, 50, 44, 255), width=int(4 * s))
    # head
    d.ellipse((cx - 108 * s, cy - 108 * s, cx + 108 * s, cy + 108 * s), fill=(12, 12, 16, 255), outline=(70, 58, 50, 255), width=int(5 * s))
    # drapes
    green = (46, 160, 80, 255)
    green_d = (30, 120, 60, 255)
    green_l = (110, 210, 130, 255)
    d.polygon([(cx - 128 * s, cy - 34 * s), (cx - 152 * s, cy + 130 * s), (cx - 84 * s, cy + 100 * s), (cx - 92 * s, cy - 20 * s)], fill=green)
    d.polygon([(cx + 128 * s, cy - 34 * s), (cx + 152 * s, cy + 130 * s), (cx + 84 * s, cy + 100 * s), (cx + 92 * s, cy - 20 * s)], fill=green)
    # cap (top half disc)
    d.pieslice((cx - 128 * s, cy - 158 * s, cx + 128 * s, cy + 98 * s), 180, 360, fill=green)
    for i in range(-3, 4):
        d.ellipse((cx + i * 40 * s - 24 * s, cy - 54 * s, cx + i * 40 * s + 24 * s, cy - 6 * s), fill=green)
    # folds
    for a, b in [(-90, -35), (-10, -10), (62, -35), (-45, -75)]:
        d.line((cx + a * s, cy - 36 * s, cx + b * s * 1.1, cy - 150 * s), fill=green_d, width=int(7 * s))
    d.ellipse((cx - 100 * s, cy - 148 * s, cx - 40 * s, cy - 108 * s), fill=green_l)
    # agal rings (black)
    for off in (0, 26):
        d.ellipse((cx - 100 * s, cy - 142 * s + off * s, cx + 100 * s, cy - 92 * s + off * s), outline=(8, 8, 10, 255), width=int(13 * s))
    # eyes
    for ex in (-42, 42):
        glow(img, (cx + ex * s, cy + 14 * s), 40 * s, (255, 210, 60), 0.9, 30 * s)
    d = ImageDraw.Draw(img)
    for ex in (-42, 42):
        d.ellipse((cx + ex * s - 20 * s, cy + 3 * s, cx + ex * s + 20 * s, cy + 25 * s), fill=(255, 236, 120, 255))
        d.ellipse((cx + ex * s - 7 * s, cy + 7 * s, cx + ex * s + 7 * s, cy + 21 * s), fill=(80, 40, 0, 255))
    # teeth
    for i in range(-3, 4):
        x = cx + i * 17 * s
        d.polygon([(x - 8 * s, cy + 62 * s), (x + 8 * s, cy + 62 * s), (x, cy + 84 * s)], fill=(232, 232, 216, 255))


def gradient_text(img, text, center, font, top, bottom):
    w, h = img.size
    mask = Image.new("L", (w, h), 0)
    md = ImageDraw.Draw(mask)
    bbox = md.textbbox((0, 0), text, font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    x = center[0] - tw / 2 - bbox[0]
    y = center[1] - th / 2 - bbox[1]
    md.text((x, y), text, font=font, fill=255)
    grad = Image.new("RGBA", (w, h))
    gd = ImageDraw.Draw(grad)
    y0, y1 = int(center[1] - th / 2), int(center[1] + th / 2)
    for yy in range(max(0, y0), min(h, y1 + 1)):
        t = (yy - y0) / max(1, (y1 - y0))
        c = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)) + (255,)
        gd.line((0, yy, w, yy), fill=c)
    # soft shadow
    shadow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    shadow.paste((0, 0, 0, 200), (6, 8), mask)
    shadow = shadow.filter(ImageFilter.GaussianBlur(8))
    img.alpha_composite(shadow)
    img.paste(grad, (0, 0), mask)


def make_presplash():
    S = 2  # supersample
    W, H = 1080 * S, 1920 * S
    img = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    glow(img, (W // 2, int(H * 0.82)), 700 * S, (120, 60, 20), 0.35, 400 * S)
    glow(img, (W // 2, int(H * 0.40)), 420 * S, (60, 120, 70), 0.18, 300 * S)
    draw_zombie_head(img, W // 2, int(H * 0.37), 1.55 * S)
    # embers
    rnd = random.Random(5)
    d = ImageDraw.Draw(img)
    for _ in range(70):
        x, y = rnd.randint(0, W), rnd.randint(int(H * 0.1), H)
        r = rnd.randint(2, 7) * S // 2
        d.ellipse((x - r, y - r, x + r, y + r), fill=(255, rnd.randint(120, 190), 50, rnd.randint(90, 200)))
    font = ImageFont.truetype("/usr/share/fonts/truetype/google-fonts/Poppins-Bold.ttf", 230 * S)
    gradient_text(img, "AboLolo", (W // 2, int(H * 0.70)), font, (255, 222, 110), (240, 120, 30))
    font2 = ImageFont.truetype("/usr/share/fonts/truetype/google-fonts/Poppins-Bold.ttf", 54 * S)
    txt = "Z O M B I E   I N V A S I O N"
    d = ImageDraw.Draw(img)
    bb = d.textbbox((0, 0), txt, font=font2)
    d.text((W // 2 - (bb[2] - bb[0]) / 2, int(H * 0.78)), txt, font=font2, fill=(190, 190, 200, 255))
    img = img.resize((1080, 1920), Image.LANCZOS).convert("RGB")
    out = os.path.join(ensure(os.path.join(RES, "drawable-nodpi")), "presplash.png")
    img.save(out, optimize=True)
    print("wrote", out)


def make_icons():
    S = 4
    base = 192 * S
    img = Image.new("RGBA", (base, base), (0, 0, 0, 255))
    d = ImageDraw.Draw(img)
    for y in range(base):
        t = y / base
        d.line((0, y, base, y), fill=(int(40 + 200 * t * t), int(22 + 90 * t), int(70 - 40 * t), 255))
    glow(img, (base // 2, int(base * 0.95)), 90 * S, (255, 170, 60), 0.5, 60 * S)
    draw_zombie_head(img, base // 2, int(base * 0.46), 0.52 * S)
    # rounded mask
    mask = Image.new("L", (base, base), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, base - 1, base - 1), 36 * S, fill=255)
    img.putalpha(mask)
    for folder, size in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
        out_dir = ensure(os.path.join(RES, "mipmap-" + folder))
        img.resize((size, size), Image.LANCZOS).save(os.path.join(out_dir, "ic_launcher.png"), optimize=True)
    print("wrote launcher icons")


# --------------------------------------------------------------------------
# Audio
# --------------------------------------------------------------------------
SR = 22050


def save_wav(path, x, sr=SR):
    x = np.clip(x, -1.0, 1.0)
    data = (x * 32767).astype("<i2")
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(sr)
        w.writeframes(data.tobytes())


def tt(dur, sr=SR):
    return np.arange(int(dur * sr)) / sr


def lowpass(x, a):
    """one-pole low-pass, a in (0,1): higher = more smoothing"""
    y = np.zeros_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc = acc * a + v * (1 - a)
        y[i] = acc
    return y


def sfx_sword():
    t = tt(0.30)
    n = np.random.RandomState(1).randn(len(t))
    sweep = np.interp(t, [0, 0.12, 0.3], [0.97, 0.80, 0.93])
    y = np.zeros_like(n)
    acc = 0.0
    for i in range(len(n)):
        acc = acc * sweep[i] + n[i] * (1 - sweep[i])
        y[i] = acc
    env = np.sin(np.clip(t / 0.30, 0, 1) * math.pi) ** 1.5
    ring = np.sin(2 * math.pi * 2200 * t) * np.exp(-t * 14) * 0.15
    out = y * env * 3.2 + ring
    return out / max(1e-6, np.max(np.abs(out))) * 0.7


def sfx_shot():
    t = tt(0.30)
    n = np.random.RandomState(2).randn(len(t))
    crack = lowpass(n, 0.35) * np.exp(-t * 28)
    thump = np.sin(2 * math.pi * np.cumsum(60 + 150 * np.exp(-t * 40)) / SR) * np.exp(-t * 16)
    out = crack * 1.6 + thump * 0.9
    return np.tanh(out * 1.4) * 0.8


def sfx_scream():
    dur = 0.62
    t = tt(dur)
    f = np.interp(t, [0, 0.08, 0.35, dur], [300, 520, 380, 210])
    f = f * (1 + 0.025 * np.sin(2 * math.pi * 24 * t))
    ph = 2 * math.pi * np.cumsum(f) / SR
    saw = sum(np.sin(k * ph) / k for k in range(1, 14))
    # crude formants: emphasize harmonics near 900 and 1700 Hz by AM with fixed sines
    form = 0.6 * np.sin(2 * math.pi * 900 * t) + 0.4 * np.sin(2 * math.pi * 1700 * t)
    rasp = 0.55 + 0.45 * np.sign(np.sin(2 * math.pi * 55 * t))
    noise = np.random.RandomState(3).randn(len(t)) * 0.25
    out = (saw * 0.45 * (0.6 + 0.4 * form) + noise) * rasp
    env = np.minimum(1, t / 0.02) * np.exp(-np.maximum(0, t - 0.25) * 5.5)
    out = np.tanh(out * env * 1.6)
    return out * 0.75


def sfx_hurt():
    t = tt(0.28)
    f = np.interp(t, [0, 0.28], [190, 95])
    ph = 2 * math.pi * np.cumsum(f) / SR
    out = sum(np.sin(k * ph) / k for k in range(1, 8)) * np.exp(-t * 9)
    out += np.random.RandomState(4).randn(len(t)) * 0.12 * np.exp(-t * 20)
    return np.tanh(out * 1.2) * 0.8


def sfx_click():
    t = tt(0.07)
    return np.sin(2 * math.pi * 1100 * t) * np.exp(-t * 60) * 0.6


def sfx_stomp():
    t = tt(0.28)
    thump = np.sin(2 * math.pi * np.cumsum(40 + 120 * np.exp(-t * 35)) / SR) * np.exp(-t * 11)
    dust = lowpass(np.random.RandomState(5).randn(len(t)), 0.6) * np.exp(-t * 22) * 0.6
    return np.tanh((thump + dust) * 1.5) * 0.9


def note(freq, dur, sr=SR, decay=4.0, harm=(1, .5, .25, .12)):
    t = tt(dur, sr)
    y = sum(h * np.sin(2 * math.pi * freq * (i + 1) * t) for i, h in enumerate(harm))
    return y * np.exp(-t * decay) * np.minimum(1, t / 0.005)


def seq(notes, total, sr=SR):
    out = np.zeros(int(total * sr))
    for start, f, d, dec in notes:
        n = note(f, d, sr, dec)
        i = int(start * sr)
        out[i:i + len(n)] += n[: len(out) - i]
    return out / max(1e-6, np.max(np.abs(out))) * 0.7


def sfx_win():
    f = lambda m: 440 * 2 ** ((m - 69) / 12)
    return seq([(0.0, f(72), 0.4, 5), (0.14, f(76), 0.4, 5), (0.28, f(79), 0.4, 5), (0.42, f(84), 0.9, 3.2), (0.42, f(88), 0.9, 3.2)], 1.5)


def sfx_lose():
    f = lambda m: 440 * 2 ** ((m - 69) / 12)
    return seq([(0.0, f(67), 0.5, 4), (0.3, f(63), 0.5, 4), (0.6, f(60), 0.5, 4), (0.9, f(55), 1.0, 2.5)], 2.0)


def sfx_boom():
    t = tt(0.9)
    n = np.random.RandomState(7).randn(len(t))
    body = lowpass(n, 0.93) * np.exp(-t * 4.5)
    thump = np.sin(2 * math.pi * np.cumsum(55 + 90 * np.exp(-t * 9)) / SR) * np.exp(-t * 5)
    out = body * 5.0 + thump * 1.2
    return np.tanh(out) * 0.9


def sfx_cannon():
    t = tt(0.22)
    n = np.random.RandomState(8).randn(len(t))
    crack = lowpass(n, 0.5) * np.exp(-t * 30)
    thump = np.sin(2 * math.pi * np.cumsum(90 + 200 * np.exp(-t * 50)) / SR) * np.exp(-t * 20)
    return np.tanh((crack * 1.8 + thump) * 1.3) * 0.8


def sfx_jet():
    t = tt(1.3)
    n = np.random.RandomState(9).randn(len(t))
    y = lowpass(n, 0.86)
    env = np.sin(np.clip(t / 1.3, 0, 1) * math.pi) ** 1.3
    return y * env * 4.0 * 0.6


def make_sfx_extra():
    out = ensure(os.path.join(RES, "raw"))
    for name, fn in [("sfx_boom", sfx_boom), ("sfx_cannon", sfx_cannon), ("sfx_jet", sfx_jet)]:
        save_wav(os.path.join(out, name + ".wav"), fn())
    print("wrote extra sfx")


def make_sfx():
    make_sfx_extra()
    out = ensure(os.path.join(RES, "raw"))
    for name, fn in [("sfx_sword", sfx_sword), ("sfx_shot", sfx_shot), ("sfx_scream", sfx_scream), ("sfx_hurt", sfx_hurt),
                     ("sfx_click", sfx_click), ("sfx_stomp", sfx_stomp), ("sfx_win", sfx_win), ("sfx_lose", sfx_lose)]:
        save_wav(os.path.join(out, name + ".wav"), fn())
    print("wrote sfx")


# ---------------- ending song: dabke-style dance track (118 BPM) ----------------
MSR = 32000
BPM = 118.0
BEAT = 60.0 / BPM
STEP = BEAT / 2  # eighth note
HIJAZ = [0, 1, 4, 5, 7, 8, 10]


def midi(m):
    return 440.0 * 2 ** ((m - 69) / 12.0)


def degree(root, d):
    return root + HIJAZ[d % 7] + 12 * (d // 7)


def add(buf, start, sig):
    i = int(start * MSR)
    if i >= len(buf):
        return
    n = min(len(sig), len(buf) - i)
    buf[i:i + n] += sig[:n]


def k_kick():
    t = tt(0.32, MSR)
    f = 48 + 120 * np.exp(-t * 30)
    return np.sin(2 * math.pi * np.cumsum(f) / MSR) * np.exp(-t * 9) * 1.1


def k_clap(seed):
    t = tt(0.20, MSR)
    n = np.random.RandomState(seed).randn(len(t))
    n = np.diff(n, prepend=0)
    env = np.exp(-t * 26) + 0.6 * np.exp(-((t - 0.012) ** 2) * 60000)
    return n * env * 0.5


def k_hat(seed):
    t = tt(0.07, MSR)
    n = np.diff(np.random.RandomState(seed).randn(len(t)), prepend=0)
    return n * np.exp(-t * 70) * 0.25


def k_dum():
    t = tt(0.22, MSR)
    return np.sin(2 * math.pi * np.cumsum(95 + 60 * np.exp(-t * 25)) / MSR) * np.exp(-t * 16) * 0.8


def k_tek(seed):
    t = tt(0.10, MSR)
    n = np.diff(np.random.RandomState(seed).randn(len(t)), prepend=0)
    return (np.sin(2 * math.pi * 1000 * t) * 0.6 + n * 0.4) * np.exp(-t * 45) * 0.5


def k_pluck(freq, dur, bright=1.0):
    t = tt(dur, MSR)
    vib = 1 + 0.004 * np.sin(2 * math.pi * 5.5 * t) * np.minimum(1, t * 4)
    ph = 2 * math.pi * freq * t * vib
    y = np.sin(ph) + 0.55 * np.sin(2 * ph) * bright + 0.32 * np.sin(3 * ph) * bright + 0.18 * np.sin(4 * ph) * bright
    return y * np.exp(-t * 5.2) * np.minimum(1, t / 0.004)


def k_bass(freq, dur):
    t = tt(dur, MSR)
    y = np.sin(2 * math.pi * freq * t) + 0.35 * np.sin(4 * math.pi * freq * t) + 0.18 * np.sin(6 * math.pi * freq * t)
    return y * np.exp(-t * 3.2) * np.minimum(1, t / 0.004)


def k_pad(freqs, dur):
    t = tt(dur, MSR)
    y = sum(np.sin(2 * math.pi * f * t) + 0.3 * np.sin(2 * math.pi * f * 2 * t) for f in freqs) / len(freqs)
    att = np.minimum(1, t / 0.6) * np.minimum(1, (dur - t) / 0.4)
    return y * att * 0.35


def make_song():
    bars = 24
    total = bars * 4 * BEAT + 1.5
    buf = np.zeros(int(total * MSR))
    kick, dum = k_kick(), k_dum()
    claps = [k_clap(i) for i in range(3)]
    hats = [k_hat(i) for i in range(3)]
    teks = [k_tek(i) for i in range(3)]

    A = [7, None, 8, 9, None, 8, 7, None, 9, None, 10, 9, None, 8, 7, None]
    B = [7, None, 6, 7, None, 5, 4, None, 5, None, 6, 7, None, None, 4, None]
    A2 = [14, None, 15, 16, 14, 15, 14, 12, 16, None, 17, 16, None, 15, 14, None]
    B2 = [14, None, 13, 14, None, 12, 11, None, 12, None, 13, 14, 16, 15, 14, None]
    roots = [0, 0, 1, 0, 0, 0, -2, 0]

    for bar in range(bars):
        t0 = bar * 4 * BEAT
        # kick on each beat = stomp
        for b in range(4):
            add(buf, t0 + b * BEAT, kick)
        # claps on 2 and 4
        for b in (1, 3):
            add(buf, t0 + b * BEAT, claps[(bar + b) % 3])
        # hats on off-beats
        for s in range(8):
            if s % 2 == 1:
                add(buf, t0 + s * STEP, hats[s % 3])
        # darbuka groove (dum tek . tek dum . tek tek)
        if bar >= 4:
            pat = {0: "d", 1: "t", 3: "t", 4: "d", 6: "t", 7: "t"}
            for s, kind in pat.items():
                if bar % 4 == 3 and s == 7:
                    continue
                add(buf, t0 + s * STEP + 0.0, dum * 0.6 if kind == "d" else teks[s % 3] * 0.9)
        # bass
        if bar >= 4:
            r = 38 + roots[bar % 8]
            for s, ln in [(0, 2), (3, 1), (4, 2), (6, 1)]:
                add(buf, t0 + s * STEP, k_bass(midi(r + (12 if s == 6 else 0)), STEP * ln + 0.1) * 0.55)
        # lead
        if bar >= 8 and bar % 2 == 0:
            phrase_idx = (bar - 8) // 2
            if bar < 16:
                ph = A if phrase_idx % 2 == 0 else B
                bright = 1.0
            else:
                ph = A2 if phrase_idx % 2 == 0 else B2
                bright = 1.3
            for s, d in enumerate(ph):
                if d is not None:
                    ln = 1
                    nxt = s + 1
                    while nxt < len(ph) and ph[nxt] is None:
                        ln += 1
                        nxt += 1
                    add(buf, t0 + s * STEP, k_pluck(midi(degree(62, d)), STEP * ln + 0.15, bright) * 0.38)
        # pad
        if bar >= 16 and bar % 4 == 0:
            ch = [midi(50), midi(57), midi(62), midi(66 if bar % 8 == 0 else 65)]
            add(buf, t0, k_pad(ch, 16 * BEAT))
    # soft limiter + fade out tail
    buf = np.tanh(buf * 0.9)
    buf /= np.max(np.abs(buf))
    buf *= 0.9
    fade = int(1.2 * MSR)
    buf[-fade:] *= np.linspace(1, 0, fade)
    wav_path = os.path.join(ROOT, "tools", "_endgame.wav")
    save_wav(wav_path, buf, MSR)
    out = os.path.join(ensure(os.path.join(RES, "raw")), "endgamelolo.ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav_path, "-c:a", "libvorbis", "-q:a", "4", out], check=True)
    os.remove(wav_path)
    print("wrote", out, "%.1f s" % (len(buf) / MSR))


# ---------------- victory song: bright triumphant dance loop (same 118 BPM so the stomps stay in sync) ----------------
MAJOR = [0, 2, 4, 5, 7, 9, 11]


def mdeg(root, d):
    return root + MAJOR[d % 7] + 12 * (d // 7)


def make_victory_song():
    bars = 12
    total = bars * 4 * BEAT + 1.0
    buf = np.zeros(int(total * MSR))
    kick = k_kick()
    claps = [k_clap(i + 10) for i in range(3)]
    hats = [k_hat(i + 10) for i in range(3)]
    # lead phrases (scale degrees, 16 eighth-notes per bar-pair)
    P1 = [4, None, 4, 5, 7, None, 7, None, 9, None, 7, 5, 4, None, None, None]
    P2 = [5, None, 5, 7, 9, None, 9, None, 11, None, 9, 7, 9, None, None, None]
    P3 = [7, 9, 11, 9, 7, None, 9, None, 11, 12, 11, 9, 7, None, None, None]
    P4 = [9, None, 7, 5, 4, None, 5, 7, 4, None, None, None, 4, None, None, None]
    phrases = [P1, P2, P3, P4]
    roots = [0, 3, 4, 0]  # I IV V I (scale degrees of the bass)
    for bar in range(bars):
        t0 = bar * 4 * BEAT
        for b in range(4):
            add(buf, t0 + b * BEAT, kick)
        for b in (1, 3):
            add(buf, t0 + b * BEAT, claps[(bar + b) % 3])
        for st in range(8):
            if st % 2 == 1:
                add(buf, t0 + st * STEP, hats[st % 3])
        r = mdeg(38, roots[bar % 4])
        for st, ln in [(0, 2), (3, 1), (4, 2), (6, 1)]:
            add(buf, t0 + st * STEP, k_bass(midi(r + (12 if st == 6 else 0)), STEP * ln + 0.1) * 0.55)
        if bar % 2 == 0:
            ph = phrases[(bar // 2) % 4]
            for st, d in enumerate(ph):
                if d is not None:
                    ln, nxt = 1, st + 1
                    while nxt < len(ph) and ph[nxt] is None:
                        ln += 1
                        nxt += 1
                    add(buf, t0 + st * STEP, k_pluck(midi(mdeg(62, d)), STEP * ln + 0.15, 1.25) * 0.4)
        if bar % 4 == 0:
            ch = [midi(50), midi(54), midi(57), midi(62)]
            add(buf, t0, k_pad(ch, 16 * BEAT) * 0.8)
    buf = np.tanh(buf * 0.9)
    buf /= np.max(np.abs(buf))
    buf *= 0.9
    fade = int(0.6 * MSR)
    buf[-fade:] *= np.linspace(1, 0, fade)
    wav_path = os.path.join(ROOT, "tools", "_victory.wav")
    save_wav(wav_path, buf, MSR)
    out = os.path.join(ensure(os.path.join(RES, "raw")), "victory_song.ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav_path, "-c:a", "libvorbis", "-q:a", "4", out], check=True)
    os.remove(wav_path)
    print("wrote", out, "%.1f s" % (len(buf) / MSR))


if __name__ == "__main__":
    import sys
    if "--sfx-only" in sys.argv:
        make_sfx_extra()
    elif "--victory-only" in sys.argv:
        make_victory_song()
    else:
        make_presplash()
        make_icons()
        make_sfx()
        make_song()
        make_victory_song()
