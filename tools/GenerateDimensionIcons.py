"""Draws the Coordinates HUD dimension icons (Earth globe, Nether portal) at 4x and downsamples to 64 px.

Run: python tools/GenerateDimensionIcons.py  (needs Pillow). Output: assets/velaclient/textures/gui/dim_*.png
"""
import math
import os
from PIL import Image, ImageDraw, ImageFilter

S = 256  # working size; saved at 64
OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'velaclient', 'textures', 'gui')


def mask_circle(size, inset):
    m = Image.new('L', (size, size), 0)
    ImageDraw.Draw(m).ellipse((inset, inset, size - inset, size - inset), fill=255)
    return m


def radial(size, inner, outer, cx, cy, radius):
    img = Image.new('RGBA', (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            t = min(1.0, math.hypot(x - cx, y - cy) / radius)
            px[x, y] = tuple(round(inner[i] + (outer[i] - inner[i]) * t) for i in range(3)) + (255,)
    return img


def earth():
    inset = 10
    ocean = radial(S, (88, 170, 255), (30, 86, 190), S * .36, S * .32, S * .78)
    land = Image.new('L', (S, S), 0)
    d = ImageDraw.Draw(land)
    # Stylised continents: the Americas on the left, Africa/Europe on the right, a polar cap at the bottom.
    d.polygon([(70, 52), (118, 46), (128, 78), (104, 100), (112, 126), (96, 150), (104, 196), (86, 214), (72, 172), (58, 132), (46, 96)], fill=255)
    d.polygon([(150, 58), (196, 66), (206, 92), (182, 108), (196, 140), (184, 186), (162, 196), (152, 156), (140, 124), (156, 100), (142, 80)], fill=255)
    d.ellipse((92, 222, 170, 262), fill=255)
    land = land.filter(ImageFilter.GaussianBlur(5)).point(lambda v: 255 if v > 120 else 0)
    green = radial(S, (120, 226, 120), (46, 150, 82), S * .36, S * .3, S * .8)
    globe = Image.composite(green, ocean, land)
    # Soft highlight and darker limb for volume.
    shade = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shade)
    for i in range(24):
        a = round(70 * (i / 24) ** 2)
        sd.ellipse((inset + i, inset + i, S - inset - i, S - inset - i), outline=(10, 24, 60, a), width=2)
    globe.alpha_composite(shade)
    glow = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse((58, 40, 130, 100), fill=(255, 255, 255, 70))
    globe.alpha_composite(glow.filter(ImageFilter.GaussianBlur(14)))
    out = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    out.paste(globe, (0, 0), mask_circle(S, inset))
    return out


def nether():
    out = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    frame = Image.new('L', (S, S), 0)
    fd = ImageDraw.Draw(frame)
    x0, y0, x1, y1 = 46, 16, 210, 240
    fd.rounded_rectangle((x0, y0, x1, y1), radius=26, fill=255)
    obsidian = radial(S, (78, 56, 112), (26, 16, 44), S * .3, S * .2, S * .9)
    out.paste(obsidian, (0, 0), frame)
    # Portal surface: violet gradient with swirling bands.
    inner = Image.new('L', (S, S), 0)
    ImageDraw.Draw(inner).rounded_rectangle((x0 + 26, y0 + 26, x1 - 26, y1 - 26), radius=12, fill=255)
    portal = Image.new('RGBA', (S, S))
    px = portal.load()
    cx, cy = S / 2, S / 2
    for y in range(S):
        for x in range(S):
            r = math.hypot(x - cx, (y - cy) * .7)
            ang = math.atan2(y - cy, x - cx)
            band = .5 + .5 * math.sin(ang * 2 + r / 11)
            t = min(1, r / 110)
            base = (196 - 60 * t, 92 - 40 * t, 255 - 40 * t)
            light = .72 + .28 * band
            px[x, y] = (round(min(255, base[0] * light + 30 * band)), round(base[1] * light), round(min(255, base[2] * light)), 255)
    out.paste(portal, (0, 0), inner)
    glow = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(glow).rounded_rectangle((x0 + 30, y0 + 30, x1 - 30, y1 - 30), radius=12, outline=(236, 190, 255, 120), width=8)
    out.alpha_composite(glow.filter(ImageFilter.GaussianBlur(6)))
    return out


def save(img, name):
    img.resize((64, 64), Image.LANCZOS).save(os.path.join(OUT, name))
    with open(os.path.join(OUT, name + '.mcmeta'), 'w') as f:
        f.write('{"texture":{"blur":true,"clamp":true}}')


save(earth(), 'dim_overworld.png')
save(nether(), 'dim_nether.png')
print('ok')
