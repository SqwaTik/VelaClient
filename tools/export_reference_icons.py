"""Raster export of the supplied Figma Make icons, preserving their paths."""
from pathlib import Path
import re
import shutil
import cairosvg

work = Path(__file__).resolve().parents[2]
source = (work / "vela-design-reference-20260906/src/App.tsx").read_text(encoding="utf-8")
out = work / "vela-client-26.2/src/main/resources/assets/velaclient/textures/gui"
out.mkdir(parents=True, exist_ok=True)
for name in ["visuals", "hud", "utilities", "markers", "friends", "configs", "settings", "search", "arrow", "back", "x", "plus", "check", "trash", "copy", "folder", "key", "star"]:
    match = re.search(r"\b" + name + r":\s*(<svg\b.*?</svg>)", source)
    if not match:
        raise ValueError(f"Missing reference icon {name}")
    svg = match[1].replace("currentColor", "white")
    svg = svg.replace("<svg ", '<svg xmlns="http://www.w3.org/2000/svg" ')
    for before, after in [("strokeWidth", "stroke-width"), ("strokeLinecap", "stroke-linecap"), ("strokeLinejoin", "stroke-linejoin")]:
        svg = svg.replace(before, after)
    path = out / f"icon_{name}.png"
    cairosvg.svg2png(bytestring=svg.encode(), write_to=str(path), output_width=64, output_height=64)
    path.with_suffix(".png.mcmeta").write_text('{"texture":{"blur":true,"clamp":true}}\n')
shutil.copyfile(work / "vela-launcher/src/renderer/src/assets/images/vela-logo.png", out / "logo.png")
(out / "logo.png.mcmeta").write_text('{"texture":{"blur":true,"clamp":true}}\n')
print("Exported 13 Figma PNG icons and original Vela logo")
