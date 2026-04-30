from __future__ import annotations

import struct
import zlib
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
TEXTURE_DIR = ROOT / "src" / "main" / "resources" / "assets" / "juststoragepanel" / "textures" / "block"


def rgba(hex_color: str, alpha: int = 255) -> tuple[int, int, int, int]:
    hex_color = hex_color.lstrip("#")
    return int(hex_color[0:2], 16), int(hex_color[2:4], 16), int(hex_color[4:6], 16), alpha


def new_canvas(color: tuple[int, int, int, int]) -> list[list[tuple[int, int, int, int]]]:
    return [[color for _ in range(16)] for _ in range(16)]


def set_px(canvas: list[list[tuple[int, int, int, int]]], x: int, y: int, color: tuple[int, int, int, int]) -> None:
    if 0 <= x < 16 and 0 <= y < 16:
        canvas[y][x] = color


def fill_rect(canvas: list[list[tuple[int, int, int, int]]], x0: int, y0: int, x1: int, y1: int, color: tuple[int, int, int, int]) -> None:
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            set_px(canvas, x, y, color)


def stroke_rect(canvas: list[list[tuple[int, int, int, int]]], x0: int, y0: int, x1: int, y1: int, color: tuple[int, int, int, int]) -> None:
    for x in range(x0, x1 + 1):
        set_px(canvas, x, y0, color)
        set_px(canvas, x, y1, color)
    for y in range(y0, y1 + 1):
        set_px(canvas, x0, y, color)
        set_px(canvas, x1, y, color)


def hline(canvas: list[list[tuple[int, int, int, int]]], y: int, x0: int, x1: int, color: tuple[int, int, int, int]) -> None:
    for x in range(x0, x1 + 1):
        set_px(canvas, x, y, color)


def vline(canvas: list[list[tuple[int, int, int, int]]], x: int, y0: int, y1: int, color: tuple[int, int, int, int]) -> None:
    for y in range(y0, y1 + 1):
        set_px(canvas, x, y, color)


def add_noise(canvas: list[list[tuple[int, int, int, int]]], light: tuple[int, int, int, int], dark: tuple[int, int, int, int], step: int = 3) -> None:
    for y in range(16):
        for x in range(16):
            value = (x * 17 + y * 29 + x * y * 7) % 11
            if value == 0:
                set_px(canvas, x, y, light)
            elif value % step == 0:
                set_px(canvas, x, y, dark)


def shade_edges(canvas: list[list[tuple[int, int, int, int]]], highlight: tuple[int, int, int, int], shadow: tuple[int, int, int, int]) -> None:
    hline(canvas, 0, 0, 15, highlight)
    vline(canvas, 0, 0, 15, highlight)
    hline(canvas, 15, 0, 15, shadow)
    vline(canvas, 15, 0, 15, shadow)


def bolt(canvas: list[list[tuple[int, int, int, int]]], x: int, y: int, rim: tuple[int, int, int, int], core: tuple[int, int, int, int]) -> None:
    fill_rect(canvas, x, y, x + 1, y + 1, rim)
    set_px(canvas, x, y, core)
    set_px(canvas, x + 1, y + 1, core)


def hazard_band(
    canvas: list[list[tuple[int, int, int, int]]],
    x0: int,
    y0: int,
    x1: int,
    y1: int,
    base: tuple[int, int, int, int],
    stripe: tuple[int, int, int, int],
    shadow: tuple[int, int, int, int],
) -> None:
    fill_rect(canvas, x0, y0, x1, y1, base)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if (x + y) % 4 in (0, 1):
                set_px(canvas, x, y, stripe)
    stroke_rect(canvas, x0, y0, x1, y1, shadow)


def metal_panel(base: str, mid: str, light: str, dark: str) -> list[list[tuple[int, int, int, int]]]:
    canvas = new_canvas(rgba(base))
    fill_rect(canvas, 1, 1, 14, 14, rgba(mid))
    add_noise(canvas, rgba(light), rgba(dark), step=4)
    stroke_rect(canvas, 1, 1, 14, 14, rgba(dark))
    shade_edges(canvas, rgba(light), rgba(dark))
    return canvas


def access_top() -> list[list[tuple[int, int, int, int]]]:
    canvas = metal_panel("#323A42", "#47545F", "#7E919E", "#1E252B")
    fill_rect(canvas, 2, 2, 13, 13, rgba("#556470"))
    stroke_rect(canvas, 2, 2, 13, 13, rgba("#20272D"))
    for y in (4, 7, 10):
        hline(canvas, y, 4, 11, rgba("#1F272D"))
        hline(canvas, y + 1, 4, 11, rgba("#7C909C"))
    vline(canvas, 7, 3, 12, rgba("#263038"))
    vline(canvas, 8, 3, 12, rgba("#8AA0AE"))
    for x, y in ((3, 3), (11, 3), (3, 11), (11, 11)):
        bolt(canvas, x, y, rgba("#9FB1BB"), rgba("#334049"))
    return canvas


def access_side() -> list[list[tuple[int, int, int, int]]]:
    canvas = metal_panel("#2E363D", "#434F58", "#748995", "#1C2227")
    fill_rect(canvas, 2, 2, 13, 13, rgba("#52616C"))
    stroke_rect(canvas, 2, 2, 13, 13, rgba("#1E252B"))
    vline(canvas, 5, 3, 12, rgba("#2A3238"))
    vline(canvas, 10, 3, 12, rgba("#2A3238"))
    hline(canvas, 7, 3, 12, rgba("#7B909C"))
    hline(canvas, 8, 3, 12, rgba("#273038"))
    for x, y in ((3, 3), (11, 3), (3, 11), (11, 11)):
        bolt(canvas, x, y, rgba("#A7B5BF"), rgba("#3A4750"))
    return canvas


def access_front() -> list[list[tuple[int, int, int, int]]]:
    canvas = access_side()
    fill_rect(canvas, 3, 3, 12, 8, rgba("#121A1F"))
    stroke_rect(canvas, 3, 3, 12, 8, rgba("#8499A6"))
    fill_rect(canvas, 4, 4, 11, 7, rgba("#17343B"))
    hline(canvas, 5, 4, 11, rgba("#8EFFF3"))
    hline(canvas, 6, 4, 11, rgba("#3AD7C7"))
    fill_rect(canvas, 4, 10, 8, 12, rgba("#2A3238"))
    for x in (5, 7):
        vline(canvas, x, 10, 12, rgba("#869AA5"))
    fill_rect(canvas, 10, 10, 12, 12, rgba("#1C242A"))
    set_px(canvas, 10, 10, rgba("#FF6565"))
    set_px(canvas, 11, 10, rgba("#F2C94C"))
    set_px(canvas, 12, 10, rgba("#59D98E"))
    set_px(canvas, 10, 12, rgba("#71DFFF"))
    set_px(canvas, 11, 12, rgba("#71DFFF"))
    set_px(canvas, 12, 12, rgba("#71DFFF"))
    return canvas


def access_bottom() -> list[list[tuple[int, int, int, int]]]:
    canvas = metal_panel("#262E35", "#39444C", "#647681", "#171E23")
    for x in (3, 6, 9, 12):
        vline(canvas, x, 3, 12, rgba("#71848F"))
    hline(canvas, 5, 3, 12, rgba("#20292F"))
    hline(canvas, 10, 3, 12, rgba("#20292F"))
    return canvas


def machine_base() -> list[list[tuple[int, int, int, int]]]:
    canvas = metal_panel("#35363B", "#4A4C53", "#7D818A", "#23252A")
    for y in (4, 8, 12):
        hline(canvas, y, 2, 13, rgba("#26292E"))
    for x in (4, 8, 12):
        vline(canvas, x, 2, 13, rgba("#666B75"))
    return canvas


def crafting_top() -> list[list[tuple[int, int, int, int]]]:
    canvas = machine_base()
    fill_rect(canvas, 3, 3, 12, 12, rgba("#B7BBC4"))
    stroke_rect(canvas, 3, 3, 12, 12, rgba("#2A2D33"))
    for pos in (6, 9):
        vline(canvas, pos, 4, 11, rgba("#636872"))
        hline(canvas, pos, 4, 11, rgba("#636872"))
    fill_rect(canvas, 6, 6, 9, 9, rgba("#EDF4FF"))
    hazard_band(canvas, 1, 1, 4, 2, rgba("#D68B1C"), rgba("#F2D24D"), rgba("#4F390A"))
    hazard_band(canvas, 11, 13, 14, 14, rgba("#D68B1C"), rgba("#F2D24D"), rgba("#4F390A"))
    return canvas


def crafting_side() -> list[list[tuple[int, int, int, int]]]:
    canvas = machine_base()
    fill_rect(canvas, 2, 2, 13, 13, rgba("#50545D"))
    stroke_rect(canvas, 2, 2, 13, 13, rgba("#23262C"))
    fill_rect(canvas, 4, 4, 11, 11, rgba("#3A3F47"))
    stroke_rect(canvas, 4, 4, 11, 11, rgba("#6E7480"))
    for x, y in ((3, 3), (11, 3), (3, 11), (11, 11)):
        bolt(canvas, x, y, rgba("#AAB0BA"), rgba("#3A414A"))
    return canvas


def crafting_front() -> list[list[tuple[int, int, int, int]]]:
    canvas = crafting_side()
    hazard_band(canvas, 3, 2, 12, 4, rgba("#C98315"), rgba("#F0D348"), rgba("#563B08"))
    fill_rect(canvas, 3, 6, 12, 9, rgba("#1A2329"))
    stroke_rect(canvas, 3, 6, 12, 9, rgba("#8E96A3"))
    for x in (4, 6, 8, 10):
        for y in (7, 8):
            set_px(canvas, x, y, rgba("#65D9FF"))
    fill_rect(canvas, 4, 11, 11, 12, rgba("#2A3137"))
    hline(canvas, 11, 5, 10, rgba("#C7D7E5"))
    for x in (5, 7, 9, 11):
        set_px(canvas, x, 12, rgba("#72F0A2"))
    return canvas


def crafting_bottom() -> list[list[tuple[int, int, int, int]]]:
    canvas = machine_base()
    fill_rect(canvas, 3, 3, 12, 12, rgba("#3B4048"))
    stroke_rect(canvas, 3, 3, 12, 12, rgba("#22262C"))
    for y in (5, 8, 11):
        hline(canvas, y, 4, 11, rgba("#76808B"))
    return canvas


def logic_cable() -> list[list[tuple[int, int, int, int]]]:
    canvas = new_canvas((0, 0, 0, 0))
    fill_rect(canvas, 0, 6, 15, 9, rgba("#4F545D"))
    fill_rect(canvas, 6, 0, 9, 15, rgba("#4F545D"))
    for x in (0, 15):
        vline(canvas, x, 6, 9, rgba("#22262C"))
    for y in (0, 15):
        hline(canvas, y, 6, 9, rgba("#22262C"))
    fill_rect(canvas, 1, 7, 14, 8, rgba("#878E99"))
    fill_rect(canvas, 7, 1, 8, 14, rgba("#878E99"))
    fill_rect(canvas, 2, 6, 13, 9, rgba("#26353A"))
    fill_rect(canvas, 6, 2, 9, 13, rgba("#26353A"))
    fill_rect(canvas, 3, 7, 12, 8, rgba("#5BE7F2"))
    fill_rect(canvas, 7, 3, 8, 12, rgba("#5BE7F2"))
    fill_rect(canvas, 6, 6, 9, 9, rgba("#E2FCFF"))
    for x in (2, 5, 10, 13):
        set_px(canvas, x, 6, rgba("#A4ACB6"))
        set_px(canvas, x, 9, rgba("#A4ACB6"))
    for y in (2, 5, 10, 13):
        set_px(canvas, 6, y, rgba("#A4ACB6"))
        set_px(canvas, 9, y, rgba("#A4ACB6"))
    stroke_rect(canvas, 0, 6, 15, 9, rgba("#1B1F24"))
    stroke_rect(canvas, 6, 0, 9, 15, rgba("#1B1F24"))
    return canvas


def write_png(path: Path, canvas: list[list[tuple[int, int, int, int]]]) -> None:
    raw = bytearray()
    for row in canvas:
        raw.append(0)
        for red, green, blue, alpha in row:
            raw.extend((red, green, blue, alpha))

    def chunk(tag: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = bytearray(b"\x89PNG\r\n\x1a\n")
    png.extend(chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0)))
    png.extend(chunk(b"IDAT", zlib.compress(bytes(raw), level=9)))
    png.extend(chunk(b"IEND", b""))
    path.write_bytes(png)


def main() -> None:
    TEXTURE_DIR.mkdir(parents=True, exist_ok=True)
    textures = {
        "access_panel_top.png": access_top(),
        "access_panel_side.png": access_side(),
        "access_panel_front.png": access_front(),
        "access_panel_bottom.png": access_bottom(),
        "crafting_panel_top.png": crafting_top(),
        "crafting_panel_side.png": crafting_side(),
        "crafting_panel_front.png": crafting_front(),
        "crafting_panel_bottom.png": crafting_bottom(),
        "logic_cable.png": logic_cable(),
    }
    for name, canvas in textures.items():
        write_png(TEXTURE_DIR / name, canvas)


if __name__ == "__main__":
    main()