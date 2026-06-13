#!/usr/bin/env python3
"""
Generates mipmap PNGs for all Android density buckets from the FocusFilter
icon source (attached_assets screenshot).

Run from the FocusFilter/ directory:
    python3 generate_icons.py
"""
import sys
import os

try:
    from PIL import Image
except ImportError:
    print("Installing Pillow…")
    os.system(f"{sys.executable} -m pip install Pillow --quiet")
    from PIL import Image

SRC = os.path.join(os.path.dirname(__file__), "..",
                   "attached_assets",
                   "Screenshot_20260604_184332_1780591778751.jpg")

SIZES = {
    "mipmap-hdpi":    72,
    "mipmap-xhdpi":   96,
    "mipmap-xxhdpi":  144,
    "mipmap-xxxhdpi": 192,
}

BASE = os.path.join(os.path.dirname(__file__),
                    "app", "src", "main", "res")

img = Image.open(SRC).convert("RGBA")
# Crop to square if needed
w, h = img.size
side  = min(w, h)
left  = (w - side) // 2
top   = (h - side) // 2
img   = img.crop((left, top, left + side, top + side))

for folder, px in SIZES.items():
    out_dir = os.path.join(BASE, folder)
    os.makedirs(out_dir, exist_ok=True)
    resized = img.resize((px, px), Image.LANCZOS)
    out_path = os.path.join(out_dir, "ic_launcher.png")
    resized.save(out_path, "PNG")
    print(f"  {folder}/ic_launcher.png  ({px}x{px})")

    # Round icon — same image, Android clips it to circle
    round_path = os.path.join(out_dir, "ic_launcher_round.png")
    resized.save(round_path, "PNG")
    print(f"  {folder}/ic_launcher_round.png  ({px}x{px})")

print("Done.")
