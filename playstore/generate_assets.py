"""Generates Google Play store graphics from the launcher icon artwork.

Geometry and colours mirror app/src/main/res/drawable/ic_launcher_foreground.xml
(108-unit adaptive-icon canvas). Run:  python3 playstore/generate_assets.py
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).parent
SS = 4  # supersampling factor for smooth edges

NAVY = (0x3E, 0x5C, 0x76)
CREAM = (0xFF, 0xF8, 0xF1)
AMBER = (0xF2, 0xB8, 0x4B)


def draw_notebook(d: ImageDraw.ImageDraw, ox: float, oy: float, s: float) -> None:
    """Draws the notebook using adaptive-icon units; (ox, oy) is where unit (0, 0) lands, s = px per unit."""

    def box(x1, y1, x2, y2):
        return [ox + x1 * s, oy + y1 * s, ox + x2 * s, oy + y2 * s]

    d.rounded_rectangle(box(34, 30, 72, 78), radius=4 * s, fill=CREAM)  # cover
    # Spine, rounded on the left only: a fully rounded box with its right half squared off.
    d.rounded_rectangle(box(34, 30, 42, 78), radius=4 * s, fill=AMBER)
    d.rectangle(box(38, 30, 42, 78), fill=AMBER)
    for y, x2 in ((42, 66), (50, 66), (58, 66), (66, 60)):  # ruled lines
        d.rectangle(box(48, y, x2, y + 2.5), fill=NAVY)


def app_icon() -> None:
    """512x512 full-bleed square; Play applies its own corner mask, so no rounding or shadow."""
    size = 512 * SS
    img = Image.new("RGB", (size, size), NAVY)
    # The launcher shows the inner 72 units (18..90) of the 108-unit canvas; map that to the full icon.
    s = size / 72
    draw_notebook(ImageDraw.Draw(img), -18 * s, -18 * s, s)
    img.resize((512, 512), Image.LANCZOS).save(OUT / "app-icon-512.png", optimize=True)


def font(size: int, bold: bool) -> ImageFont.FreeTypeFont:
    candidates = [
        ("/System/Library/Fonts/Avenir Next.ttc", 2 if bold else 5),  # Bold / Regular
        ("/System/Library/Fonts/HelveticaNeue.ttc", 1 if bold else 0),
        ("/System/Library/Fonts/Helvetica.ttc", 1 if bold else 0),
    ]
    for path, index in candidates:
        if Path(path).exists():
            return ImageFont.truetype(path, size, index=index)
    return ImageFont.load_default(size)


def feature_graphic() -> None:
    """1024x500, no transparency. Content kept away from the edges, since Play may crop it."""
    w, h = 1024 * SS, 500 * SS
    img = Image.new("RGB", (w, h), NAVY)
    d = ImageDraw.Draw(img)

    # Lighter navy circle behind the notebook, echoing the splash screen.
    cx, cy, r = 250 * SS, h / 2, 160 * SS
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(0x4A, 0x6A, 0x86))
    s = 4.8 * SS  # notebook body spans units 34..72 x 30..78; centre it on (cx, cy)
    draw_notebook(d, cx - 53 * s, cy - 54 * s, s)

    tx = 470 * SS
    d.text((tx, 180 * SS), "Daily Journal", font=font(76 * SS, bold=True), fill=CREAM)
    d.text((tx, 282 * SS), "Your days, your words, your photos.", font=font(28 * SS, bold=False),
           fill=(0xD0, 0xE4, 0xFF))
    d.text((tx, 324 * SS), "Private. Offline. No account.", font=font(28 * SS, bold=False), fill=AMBER)

    img.resize((1024, 500), Image.LANCZOS).save(OUT / "feature-graphic-1024x500.png", optimize=True)


if __name__ == "__main__":
    app_icon()
    feature_graphic()
    print("Wrote", ", ".join(p.name for p in sorted(OUT.glob("*.png"))))
