"""Synthesises p1ll's sounds into app/src/main/res/raw. Run: python3 tools/sounds.py

Everything is additive synthesis: a soft mallet (marimba-like inharmonic partials) on a
D major pentatonic, so any sequence of doses taken in a day stays in key.
"""
import os
import wave

import numpy as np

SR = 32000
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")
rng = np.random.default_rng(7)

NOTE = {"D4": 293.66, "A4": 440.0, "D5": 587.33, "E5": 659.25, "F#5": 739.99, "A5": 880.0,
        "B5": 987.77, "D6": 1174.66, "E6": 1318.51, "F#6": 1479.98, "D7": 2349.32}


def lowpass(x, cutoff):
    a = np.exp(-2 * np.pi * cutoff / SR)
    y = np.zeros_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc = (1 - a) * v + a * acc
        y[i] = acc
    return y


def mallet(f, dur, amp=1.0, sustain=1.0):
    t = np.arange(int(SR * dur)) / SR
    partials = [(1.0, 1.0, 4.0 / sustain), (2.0, 0.10, 7.0), (3.93, 0.30, 12.0), (9.2, 0.06, 28.0)]
    y = sum(a * np.sin(2 * np.pi * f * r * t) * np.exp(-d * t) for r, a, d in partials)
    y += 0.16 * np.sin(np.pi * f * t) * np.exp(-5.0 * t)  # warmth an octave down
    y *= np.minimum(1.0, t / 0.0025)
    click = lowpass(rng.standard_normal(len(t)) * np.exp(-t * 300), 3500) * 0.5
    return (y + click) * amp


def pad(freqs, dur, amp):
    t = np.arange(int(SR * dur)) / SR
    env = np.minimum(1.0, t / 0.09) * np.exp(-1.6 * t)
    return amp * env * sum(np.sin(2 * np.pi * f * t) for f in freqs) / len(freqs)


def mix(length, *parts):
    out = np.zeros(int(SR * length))
    for start, sig in parts:
        i = int(SR * start)
        n = min(len(sig), len(out) - i)
        out[i:i + n] += sig[:n]
    return out


def write(name, y, peak=0.7):
    fade = int(SR * 0.04)
    y[-fade:] *= np.linspace(1, 0, fade)
    y = y / np.max(np.abs(y)) * peak
    data = (y * 32767).astype("<i2").tobytes()
    with wave.open(os.path.join(OUT, name + ".wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(data)


os.makedirs(OUT, exist_ok=True)

for i, n in enumerate(["D5", "E5", "F#5", "A5", "B5", "D6"], start=1):
    write(f"take_{i}", mallet(NOTE[n], 1.0), peak=0.62)

arp = ["D5", "F#5", "A5", "D6", "F#6"]
write("complete", mix(
    2.2,
    *[(i * 0.065, mallet(NOTE[n], 1.6, amp=1.0 - i * 0.08, sustain=1.6)) for i, n in enumerate(arp)],
    (0.02, pad([NOTE["D4"], NOTE["A4"], NOTE["D5"]], 2.1, 0.55)),
    (0.30, mallet(NOTE["D7"], 1.2, amp=0.12, sustain=2.0)),
), peak=0.72)

write("undo", mix(0.7, (0.0, mallet(NOTE["B5"], 0.6, 0.7)), (0.075, mallet(NOTE["F#5"], 0.6, 0.6))), peak=0.45)
write("save", mix(0.9, (0.0, mallet(NOTE["A5"], 0.8, 0.8)), (0.085, mallet(NOTE["D6"], 0.8, 1.0))), peak=0.55)
write("reminder", mix(
    1.7,
    (0.0, mallet(NOTE["F#5"], 1.4, 0.9, sustain=1.8)),
    (0.17, mallet(NOTE["A5"], 1.4, 0.9, sustain=1.8)),
    (0.34, mallet(NOTE["D6"], 1.4, 1.0, sustain=2.2)),
), peak=0.8)
print("ok")
