"""Frames raw phone screenshots into 9:16 Play Store screenshots with a caption.

Usage:
  python3 playstore/frame_screenshots.py OUT_DIR IMAGE "Caption" [IMAGE "Caption" ...]

Phone screenshots are usually taller than 9:16 (e.g. 1080x2424), so instead of cropping or
stretching, each one is scaled onto a 1080x1920 canvas below its caption.
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

from generate_assets import AMBER, CREAM, NAVY, font

W, H = 1080, 1920
TOP = 300  # caption area height
BOTTOM = 80
RADIUS = 44


def frame(src: Path, caption: str, out: Path) -> None:
    shot = Image.open(src).convert("RGB")
    target_h = H - TOP - BOTTOM
    scale = target_h / shot.height
    shot = shot.resize((round(shot.width * scale), target_h), Image.LANCZOS)
    x, y = (W - shot.width) // 2, TOP

    canvas = Image.new("RGB", (W, H), NAVY)

    # Soft shadow under the screenshot.
    shadow = Image.new("L", (W, H), 0)
    ImageDraw.Draw(shadow).rounded_rectangle(
        [x, y + 12, x + shot.width, y + shot.height + 12], radius=RADIUS, fill=110
    )
    canvas.paste((0x22, 0x33, 0x44), mask=shadow.filter(ImageFilter.GaussianBlur(24)))

    mask = Image.new("L", shot.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, shot.width - 1, shot.height - 1], radius=RADIUS, fill=255)
    canvas.paste(shot, (x, y), mask)

    d = ImageDraw.Draw(canvas)
    title_font = font(68, bold=True)
    lines = caption.split("\n")
    line_h = 84
    start = (TOP - line_h * len(lines)) // 2 - 6
    for i, line in enumerate(lines):
        w = d.textlength(line, font=title_font)
        d.text(((W - w) / 2, start + i * line_h), line, font=title_font, fill=CREAM if i == 0 else AMBER)

    canvas.save(out, optimize=True)


if __name__ == "__main__":
    out_dir = Path(sys.argv[1])
    out_dir.mkdir(parents=True, exist_ok=True)
    pairs = list(zip(sys.argv[2::2], sys.argv[3::2]))
    for n, (src, caption) in enumerate(pairs, start=1):
        out = out_dir / f"screenshot-{n}.png"
        frame(Path(src), caption.replace("\\n", "\n"), out)
        print("Wrote", out)
