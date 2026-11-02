#!/usr/bin/env python3
"""
Generates AETHER's two signature sounds into androidApp/src/main/res/raw/.

  aether_chime.wav  ~1.6 s   notification: three soft glass-bell notes rising
                             through a D major pentatonic (D5 -> A5 -> F#6)
  aether_alarm.wav  ~9 s     alarm loop: the same motif as a swelling
                             arpeggio, repeating with growing intensity

Bells are additive synthesis with inharmonic partials (the ratios of a real
struck bell), a soft attack, per-partial exponential decay and a short
synthetic room reverb. Tweak NOTES / PARTIALS / the reverb and re-run:

    python3 tools/make_sounds.py
"""
import os
import wave

import numpy as np

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "androidApp", "src", "main", "res", "raw")

# (frequency ratio, relative level, decay seconds)
PARTIALS = [(1.0, 1.00, 1.30), (2.0, 0.42, 0.90), (2.76, 0.30, 0.55), (5.4, 0.16, 0.30), (8.93, 0.07, 0.16)]


def bell(freq, dur, level=1.0):
    t = np.arange(int(SR * dur)) / SR
    out = np.zeros_like(t)
    for ratio, amp, decay in PARTIALS:
        f = freq * ratio
        if f > 9000:
            continue
        out += amp * np.sin(2 * np.pi * f * t) * np.exp(-t / decay)
    attack = np.minimum(1.0, t / 0.006)  # 6 ms: removes the click, keeps the "tink"
    return level * out * attack


def place(track, sound, start):
    i = int(start * SR)
    end = min(len(track), i + len(sound))
    track[i:end] += sound[: end - i]


def reverb(x, seconds=0.9, wet=0.28):
    n = int(SR * seconds)
    rng = np.random.default_rng(7)
    ir = rng.standard_normal(n) * np.exp(-np.arange(n) / (SR * seconds / 4.5))
    ir[0] = 0
    size = len(x) + n
    fft = 1 << (size - 1).bit_length()
    wet_sig = np.fft.irfft(np.fft.rfft(x, fft) * np.fft.rfft(ir, fft), fft)[: len(x)]
    wet_sig *= np.max(np.abs(x)) / (np.max(np.abs(wet_sig)) + 1e-9)
    return (1 - wet) * x + wet * wet_sig


def finish(x, name, peak=0.89):
    x = x / (np.max(np.abs(x)) + 1e-9) * peak
    fade = int(SR * 0.04)
    x[-fade:] *= np.linspace(1, 0, fade)  # never end on a click
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, name)
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((x * 32767).astype("<i2").tobytes())
    print(f"{name}: {len(x) / SR:.2f}s, {os.path.getsize(path) / 1024:.0f} KB")


D5, A5, FS6, D6, E5, B5 = 587.33, 880.00, 1479.98, 1174.66, 659.25, 987.77

# Notification chime: three rising notes, the last one rings out.
chime = np.zeros(int(SR * 1.8))
for start, f, lv in [(0.00, D5, 0.8), (0.13, A5, 0.9), (0.27, FS6, 1.0)]:
    place(chime, bell(f, 1.4, lv), start)
finish(reverb(chime), "aether_chime.wav")

# Alarm loop: arpeggio phrases that swell. Gentle enough to wake to, insistent enough to work.
alarm = np.zeros(int(SR * 9.0))
phrase = [D5, A5, FS6, D6, FS6, A5]
for rep, base in enumerate([0.0, 3.0, 6.0]):
    gain = 0.55 + 0.22 * rep
    for k, f in enumerate(phrase):
        place(alarm, bell(f, 1.5, gain * (0.9 + 0.1 * (k % 2))), base + k * 0.32)
    place(alarm, bell(D5 / 2, 2.2, gain * 0.6), base)  # low root underneath each phrase
finish(reverb(alarm, 1.1, 0.32), "aether_alarm.wav")
