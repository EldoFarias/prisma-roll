"""Synthesizes the game's sound effects as 16-bit mono WAV files."""
import os, sys, wave
import numpy as np

OUT = sys.argv[1]
SR = 44100
rng = np.random.default_rng(3)


def t(dur):
    return np.arange(int(SR * dur)) / SR


def env(n, attack=0.004, decay=8.0):
    x = np.arange(n) / SR
    a = np.clip(x / attack, 0, 1)
    return a * np.exp(-x * decay)


def tone(freq, dur, decay=8.0, harmonics=((1, 1.0),), attack=0.004):
    x = t(dur)
    sig = sum(amp * np.sin(2 * np.pi * freq * h * x) for h, amp in harmonics)
    return sig * env(len(x), attack, decay)


def mix(parts, total):
    out = np.zeros(int(SR * total))
    for start, sig in parts:
        i = int(SR * start)
        j = min(len(out), i + len(sig))
        out[i:j] += sig[: j - i]
    return out


def save(name, sig, peak=0.75):
    sig = sig / (np.max(np.abs(sig)) + 1e-9) * peak
    fade = min(len(sig), int(SR * 0.01))
    sig[-fade:] *= np.linspace(1, 0, fade)
    data = (sig * 32767).astype(np.int16)
    with wave.open(os.path.join(OUT, name + ".wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(data.tobytes())


bell = ((1, 1.0), (2, 0.35), (3, 0.12), (4.2, 0.06))

# pick: soft glassy tick
save("sfx_pick", tone(1800, 0.06, decay=60, harmonics=((1, 1), (2.5, 0.3))), 0.45)

# place: woody "thock" with a pitch drop
x = t(0.14)
f = 220 * np.exp(-x * 9) + 90
phase = 2 * np.pi * np.cumsum(f) / SR
thock = (np.sin(phase) + 0.3 * np.sin(2 * phase)) * env(len(x), 0.002, 30)
click = rng.normal(0, 1, len(x)) * env(len(x), 0.0005, 300) * 0.25
save("sfx_place", thock + click, 0.8)

# clear: sparkly major arpeggio
notes = [1046.5, 1318.5, 1568.0, 2093.0]
parts = [(i * 0.055, tone(n, 0.5, decay=7, harmonics=bell)) for i, n in enumerate(notes)]
shimmer = rng.normal(0, 1, int(SR * 0.4)) * env(int(SR * 0.4), 0.01, 9) * 0.05
parts.append((0.0, np.convolve(shimmer, np.ones(8) / 8, mode="same")))
save("sfx_clear", mix(parts, 0.7))

# combo: bigger, faster, higher arpeggio
notes = [1046.5, 1318.5, 1568.0, 2093.0, 2637.0, 3136.0]
parts = [(i * 0.045, tone(n, 0.6, decay=6, harmonics=bell)) for i, n in enumerate(notes)]
parts.append((0.0, tone(523.25, 0.7, decay=4, harmonics=((1, 0.6), (2, 0.3)))))
save("sfx_combo", mix(parts, 0.9))

# invalid: soft low buzz
x = t(0.16)
buzz = np.sign(np.sin(2 * np.pi * 140 * x)) * 0.4 + np.sin(2 * np.pi * 140 * x)
buzz = np.convolve(buzz, np.ones(30) / 30, mode="same") * env(len(x), 0.005, 18)
save("sfx_invalid", buzz, 0.4)

# game over: gentle descending phrase
notes = [784.0, 659.3, 523.3, 392.0]
parts = [(i * 0.18, tone(n, 0.8, decay=4, harmonics=((1, 1), (2, 0.25), (3, 0.08)))) for i, n in enumerate(notes)]
save("sfx_gameover", mix(parts, 1.5), 0.6)

# record: short fanfare + held chord
notes = [523.3, 659.3, 784.0]
parts = [(i * 0.1, tone(n, 0.3, decay=10, harmonics=bell)) for i, n in enumerate(notes)]
for n in (1046.5, 1318.5, 1568.0):
    parts.append((0.32, tone(n, 1.0, decay=3, harmonics=bell, attack=0.01)))
save("sfx_record", mix(parts, 1.4))

# revive: rising whoosh + chime
x = t(0.45)
f = 300 + 1500 * (x / x[-1]) ** 2
phase = 2 * np.pi * np.cumsum(f) / SR
whoosh = np.sin(phase) * np.sin(np.pi * x / x[-1]) * 0.5
noise = np.convolve(rng.normal(0, 1, len(x)), np.ones(20) / 20, mode="same") * np.sin(np.pi * x / x[-1]) * 0.4
parts = [(0.0, whoosh + noise), (0.4, tone(2093.0, 0.6, decay=6, harmonics=bell))]
save("sfx_revive", mix(parts, 1.0))
# bomb: deep boom with crackle
x = t(0.7)
f = 120 * np.exp(-x * 5) + 40
phase = 2 * np.pi * np.cumsum(f) / SR
boom = np.sin(phase) * env(len(x), 0.003, 5)
noise = rng.normal(0, 1, len(x))
noise = np.convolve(noise, np.ones(12) / 12, mode="same") * env(len(x), 0.002, 9) * 0.9
save("sfx_bomb", boom + noise, 0.85)

# coin: two quick bright pings
parts = [(0.0, tone(1975.5, 0.25, decay=14, harmonics=bell)), (0.07, tone(2637.0, 0.35, decay=10, harmonics=bell))]
save("sfx_coin", mix(parts, 0.45), 0.6)

# whoosh: soft swipe for screen transitions / splash
x = t(0.35)
w = np.convolve(rng.normal(0, 1, len(x)), np.ones(40) / 40, mode="same") * np.sin(np.pi * x / x[-1]) ** 2
save("sfx_whoosh", w, 0.45)

print("ok", sorted(os.listdir(OUT)))

# glass: a crisp crack followed by a shower of tiny glass tinkles (block bursting through its gate)
g_rng = np.random.default_rng(11)
total = 0.9
n = int(SR * 0.05)
crack = g_rng.normal(0, 1, n)
crack = np.diff(np.concatenate([[0], crack]))  # brighter (high-passed) noise
crack *= env(n, 0.0005, 70)
body = tone(2600, 0.12, decay=38, harmonics=((1, 1), (1.47, 0.6), (2.3, 0.35), (3.1, 0.2)))
parts = [(0, crack * 1.4), (0.002, body * 0.5)]
for i in range(26):
    start = 0.01 + (g_rng.random() ** 1.8) * 0.65
    f0 = 2400 + g_rng.random() * 5200
    d = 0.05 + g_rng.random() * 0.12
    amp = 0.25 + 0.5 * g_rng.random() * (1 - start / 0.7)
    ping = tone(f0, d, decay=30 + g_rng.random() * 40, harmonics=((1, 1), (1.53, 0.4), (2.71, 0.2)), attack=0.0008)
    parts.append((start, ping * amp))
save("sfx_glass", mix(parts, total), 0.8)

# ---- Prisma Roll: jelly bounce and ring pass
x = t(0.22)
f = 230 * np.exp(-x * 9) + 120          # pitch falls: a soft "boing"
phase = 2 * np.pi * np.cumsum(f) / SR
bounce = (np.sin(phase) + 0.25 * np.sin(2 * phase)) * env(len(x), 0.002, 16)
click = rng.standard_normal(int(SR * 0.012)) * np.exp(-np.arange(int(SR * 0.012)) / SR * 300) * 0.4
save("sfx_bounce", mix([(0, bounce), (0, click)], 0.22), peak=0.7)

n = int(SR * 0.32)
noise = rng.standard_normal(n)
sweep = np.convolve(noise, np.ones(30) / 30, mode="same") * np.sin(np.linspace(0, np.pi, n)) ** 2
chime = tone(1318, 0.3, decay=10, harmonics=((1, 1.0), (2, 0.3), (3, 0.12))) * 0.35
save("sfx_pass", mix([(0, sweep), (0.05, chime)], 0.34), peak=0.6)
