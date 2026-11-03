"""Packs icon_<name>_<8..64>.png into one strip icon_<name>_sizes.png (sizes left to right, 2 px apart) and removes them."""
import sys
from pathlib import Path
from PIL import Image

gui = Path(sys.argv[1]); GAP = 2; SIZES = range(8, 65)
names = sorted({p.name[5:-7] for p in gui.glob("icon_*_64.png")})
width = sum(s + GAP for s in SIZES) - GAP
for name in names:
    strip = Image.new("RGBA", (width, 64), (0, 0, 0, 0)); u = 0
    for s in SIZES:
        p = gui / f"icon_{name}_{s}.png"
        im = Image.open(p).convert("RGBA"); assert im.size == (s, s), p
        strip.paste(im, (u, 0)); u += s + GAP
    strip.save(gui / f"icon_{name}_sizes.png", optimize=True)
    (gui / f"icon_{name}_sizes.png.mcmeta").write_text('{"texture":{"blur":true,"clamp":true}}', newline="\n")
    for s in SIZES:
        (gui / f"icon_{name}_{s}.png").unlink(); m = gui / f"icon_{name}_{s}.png.mcmeta"
        if m.exists(): m.unlink()
print(len(names), "icons,", width, "px strips")
