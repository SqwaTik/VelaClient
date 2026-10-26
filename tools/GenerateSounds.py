"""Synthesizes Vela's built-in sounds (original audio, no samples) and encodes them to OGG Vorbis.

Usage: python tools/GenerateSounds.py <output assets/velaclient/sounds dir>   (needs numpy, scipy, ffmpeg on PATH)
"""
import os, subprocess, sys, tempfile
import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, sosfilt

RATE = 44100
rng = np.random.default_rng(26)

def t(seconds): return np.arange(int(RATE * seconds)) / RATE
def env(n, attack, decay):
    x = np.arange(n) / RATE
    return np.minimum(1, x / max(attack, 1e-4)) * np.exp(-x / decay)
def sweep(f0, f1, seconds, curve=1.0):
    x = t(seconds); k = (x / seconds) ** curve
    freq = f0 + (f1 - f0) * k
    return np.sin(2 * np.pi * np.cumsum(freq) / RATE)
def band(sig, low, high, order=4):
    return sosfilt(butter(order, [low, high], btype='band', fs=RATE, output='sos'), sig)
def lowpass(sig, cut, order=4):
    return sosfilt(butter(order, cut, btype='low', fs=RATE, output='sos'), sig)
def noise(seconds): return rng.uniform(-1, 1, len(t(seconds)))
def pad(a, n): return np.pad(a, (0, max(0, n - len(a))))[:n]
def mix(*parts):
    n = max(len(p) for p in parts); return sum(pad(p, n) for p in parts)
def reverb(sig, seconds=1.2, mixing=.25):
    out = np.concatenate([sig, np.zeros(int(RATE * seconds))])
    wet = np.zeros_like(out)
    for delay, gain in ((0.0297, .72), (0.0371, .70), (0.0411, .68), (0.0437, .66)):
        d = int(delay * RATE); buf = out.copy()
        for i in range(d, len(buf)): buf[i] += gain * buf[i - d]
        wet += buf
    wet = lowpass(wet / 4, 5000)
    return out * (1 - mixing) + wet * mixing
def finish(sig, peak=.89, fade=.004):
    n = int(RATE * fade)
    sig = sig - np.mean(sig)
    sig[:n] *= np.linspace(0, 1, n); sig[-n:] *= np.linspace(1, 0, n)
    return sig / (np.max(np.abs(sig)) + 1e-9) * peak

def hit_click():
    tick = np.sin(2 * np.pi * 3100 * t(.05)) * env(len(t(.05)), .0005, .012)
    burst = band(noise(.012), 2500, 9000) * env(len(t(.012)), .0002, .003)
    return finish(mix(tick, burst * .8))
def hit_pulse():
    s = .16; x = t(s); f = 900 * np.exp(-x * 2.5)
    tone = np.sin(2 * np.pi * np.cumsum(f) / RATE) + .35 * np.sin(4 * np.pi * np.cumsum(f) / RATE)
    return finish(tone * env(len(x), .001, .045))
def hit_crystal():
    s = .45; x = t(s)
    parts = sum(a * np.sin(2 * np.pi * f * x) * np.exp(-x / d) for f, a, d in ((1480, 1, .16), (2390, .6, .11), (3870, .35, .07), (5210, .2, .05)))
    return finish(reverb(parts * env(len(x), .0008, 10), .5, .2))
def hit_bass():
    body = sweep(95, 42, .22, .6) * env(len(t(.22)), .001, .07)
    knock = lowpass(noise(.02), 1800) * env(len(t(.02)), .0003, .006)
    return finish(mix(body, knock * .6))
def hit_neon():
    s = .11; x = t(s); freq = 520 + 900 * (x / s) ** .5; ph = 2 * np.pi * np.cumsum(freq) / RATE
    saw = sum(np.sin(k * ph) / k for k in range(1, 6))
    return finish(lowpass(saw, 6000) * env(len(x), .001, .035))
def hit_glass():
    s = .3; x = t(s)
    ping = sum(a * np.sin(2 * np.pi * f * x) * np.exp(-x / d) for f, a, d in ((4180, 1, .08), (6270, .55, .05), (8390, .25, .03)))
    shimmer = band(noise(s), 6000, 12000) * np.exp(-x / .02) * .4
    return finish(reverb(ping + shimmer, .3, .18))
def hit_marker():
    tick = np.sin(2 * np.pi * 1250 * t(.035)) * env(len(t(.035)), .0004, .01)
    snap = band(noise(.025), 1500, 6000) * env(len(t(.025)), .0002, .005)
    return finish(mix(tick, snap * .9))

def wasted():
    s = 2.6; x = t(s)
    boom = (np.sin(2 * np.pi * np.cumsum(58 * np.exp(-x * .45)) / RATE) * np.exp(-x / .9)
            + .5 * lowpass(noise(s), 220) * np.exp(-x / .35))
    cutoff = np.geomspace(3800, 180, len(x))
    whoosh = np.zeros(len(x)); block = 512; raw = noise(s)
    for i in range(0, len(x), block):
        seg = raw[max(0, i - 2048):i + block]
        whoosh[i:i + block] = lowpass(seg, cutoff[i])[-len(whoosh[i:i + block]):]
    whoosh *= np.minimum(1, x / .08) * np.exp(-x / .8) * .55
    drop = np.exp(-x * .06)
    bell = sum(a * np.sin(2 * np.pi * np.cumsum(f * drop) / RATE) * np.exp(-x / d)
               for f, a, d in ((196, .55, 1.4), (294, .35, 1.1), (466, .22, .8), (587, .12, .6)))
    bell *= np.minimum(1, x / .02)
    return finish(reverb(boom * 1.0 + whoosh + bell * .7, 1.6, .3), peak=.92, fade=.02)

SOUNDS = {
    "hit/click": hit_click, "hit/pulse": hit_pulse, "hit/crystal": hit_crystal, "hit/bass": hit_bass,
    "hit/neon": hit_neon, "hit/glass": hit_glass, "hit/marker": hit_marker, "death/wasted": wasted,
}

def main(out_dir):
    with tempfile.TemporaryDirectory() as tmp:
        for name, make in SOUNDS.items():
            data = (make() * 32767).astype(np.int16)
            wav = os.path.join(tmp, name.replace('/', '_') + ".wav")
            wavfile.write(wav, RATE, data)
            target = os.path.join(out_dir, name + ".ogg")
            os.makedirs(os.path.dirname(target), exist_ok=True)
            subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "5", "-ac", "1", target], check=True)
            print(f"{target}: {len(data) / RATE:.2f} s, {os.path.getsize(target)} B")

if __name__ == "__main__":
    main(sys.argv[1])
