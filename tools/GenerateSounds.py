"""Synthesizes Vela's Wasted sting (original audio, no samples) and encodes them to OGG Vorbis.

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

# Hit sounds are recorded (Kenney Impact Sounds, CC0), not generated; only the Wasted sting is synthesized.
SOUNDS = {"death/wasted": wasted}

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
