"""Cuts the static Red Hat instances in app/src/main/res/font from the variable fonts.

Static files render the exact weight everywhere, including Robolectric screenshots, which
ignore variable-font axes. Run: python3 tools/fonts.py <RedHatDisplay[wght].ttf> <RedHatText[wght].ttf>
(both from https://github.com/google/fonts/tree/main/ofl).
"""
import os
import sys

from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "font")

for src, name, weights in [(sys.argv[1], "redhat_display", [500, 650]), (sys.argv[2], "redhat_text", [400, 500, 600])]:
    for w in weights:
        font = instancer.instantiateVariableFont(TTFont(src), {"wght": w}, updateFontNames=False)
        # Every instance gets its own family name, or font caches that key on names hand back the first one.
        family = font["name"].getDebugName(1)
        unique = f"{family} W{w}"
        ps = unique.replace(" ", "")
        names = font["name"]
        for rid in (16, 17, 21, 22, 25):
            names.removeNames(nameID=rid)
        for rid, value in ((1, unique), (2, "Regular"), (3, f"{ps};p1ll"), (4, unique), (6, ps)):
            names.setName(value, rid, 3, 1, 0x409)
            names.setName(value, rid, 1, 0, 0)
        font["OS/2"].usWeightClass = w
        font["head"].fontRevision = float(f"{font['head'].fontRevision:.0f}.{w}")
        font.save(os.path.join(OUT, f"{name}_{w}.ttf"))
print("ok")
