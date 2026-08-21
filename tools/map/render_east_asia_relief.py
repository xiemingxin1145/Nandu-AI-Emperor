#!/usr/bin/env python3
"""Build the mobile East Asia relief from public-domain Natural Earth vectors."""

from __future__ import annotations

import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from scipy.ndimage import gaussian_filter

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "tools/map/east_asia_natural_earth.json"
TARGET = ROOT / "app/src/main/assets/images/map_v2/east_asia_relief.webp"
WIDTH, HEIGHT = 3072, 1920


def main() -> None:
    geography = json.loads(SOURCE.read_text(encoding="utf-8"))
    bounds = geography["bounds"]

    def xy(point: list[float] | tuple[float, float]) -> tuple[int, int]:
        lon, lat = point
        return (
            round((lon - bounds["west"]) / (bounds["east"] - bounds["west"]) * WIDTH),
            round((bounds["north"] - lat) / (bounds["north"] - bounds["south"]) * HEIGHT),
        )

    def polygons(geometry: dict) -> list[list[list[list[float]]]]:
        if geometry["type"] == "Polygon":
            return [geometry["coordinates"]]
        if geometry["type"] == "MultiPolygon":
            return geometry["coordinates"]
        return []

    def lines(geometry: dict) -> list[list[list[float]]]:
        if geometry["type"] == "LineString":
            return [geometry["coordinates"]]
        if geometry["type"] == "MultiLineString":
            return geometry["coordinates"]
        return []

    # One geographic land mask drives land, coast and relief; no decorative circles.
    land = Image.new("L", (WIDTH, HEIGHT), 0)
    land_draw = ImageDraw.Draw(land)
    coast_paths: list[list[tuple[int, int]]] = []
    for feature in geography["land"]:
        for polygon in polygons(feature["geometry"]):
            if not polygon:
                continue
            exterior = [xy(point) for point in polygon[0]]
            land_draw.polygon(exterior, fill=255)
            coast_paths.append(exterior)
            for hole in polygon[1:]:
                land_draw.polygon([xy(point) for point in hole], fill=0)

    land_mask = np.asarray(land, dtype=np.float32) / 255.0
    rows, cols = np.mgrid[0:HEIGHT, 0:WIDTH].astype(np.float32)
    lat_field = bounds["north"] - rows / HEIGHT * (bounds["north"] - bounds["south"])
    lon_field = bounds["west"] + cols / WIDTH * (bounds["east"] - bounds["west"])

    # Near-shore blue makes the actual Korea/China/Japan coastline legible.
    coastal_water = gaussian_filter(land_mask, sigma=27) * (1.0 - land_mask)
    ocean = np.zeros((HEIGHT, WIDTH, 3), dtype=np.float32)
    ocean[:, :, 0] = 13 + coastal_water * 18 + rows / HEIGHT * 2
    ocean[:, :, 1] = 31 + coastal_water * 39 + rows / HEIGHT * 7
    ocean[:, :, 2] = 45 + coastal_water * 49 + rows / HEIGHT * 10

    # Major ridges are geographically anchored approximations, not a claimed DEM.
    ridges = [
        ("秦岭", [(103.8, 33.0), (105.0, 33.4), (106.3, 33.6), (107.6, 33.7), (109.0, 33.8), (110.5, 33.8), (112.0, 33.7)], 46, 0.78),
        ("太行山", [(112.0, 35.0), (112.7, 36.0), (113.2, 37.0), (113.8, 38.1), (114.2, 39.0), (115.4, 40.0)], 42, 0.70),
        ("燕山", [(113.6, 40.4), (115.3, 40.7), (117.0, 40.7), (118.6, 40.5), (120.2, 41.0), (121.3, 41.1)], 37, 0.63),
        ("大巴山", [(105.6, 32.1), (106.8, 32.0), (108.0, 31.8), (109.0, 31.7), (110.1, 31.6)], 38, 0.63),
        ("巫山", [(108.8, 31.3), (109.2, 30.9), (109.6, 30.5), (110.0, 30.0)], 34, 0.62),
        ("横断山", [(99.0, 34.0), (99.4, 32.7), (99.6, 31.2), (100.0, 29.8), (100.2, 28.5), (99.6, 27.0), (99.0, 25.5)], 57, 0.92),
        ("云贵高原", [(100.5, 26.9), (102.3, 26.5), (104.0, 26.2), (105.5, 26.4), (106.8, 26.1)], 63, 0.58),
        ("南岭", [(109.3, 25.3), (110.8, 25.4), (112.5, 25.4), (114.0, 25.2), (115.2, 25.0)], 31, 0.48),
        ("武夷山", [(116.1, 28.4), (117.0, 27.8), (117.7, 27.0), (118.1, 26.2), (118.4, 25.5)], 27, 0.46),
        ("贺兰山", [(105.3, 37.5), (105.7, 38.4), (106.0, 39.1)], 34, 0.56),
        ("祁连山", [(96.0, 38.7), (98.3, 38.8), (100.2, 38.5), (102.0, 37.8)], 52, 0.70),
    ]

    elevation = np.zeros((HEIGHT, WIDTH), dtype=np.float32)
    for _, ridge, width, strength in ridges:
        trace = Image.new("L", (WIDTH, HEIGHT), 0)
        ImageDraw.Draw(trace).line([xy(point) for point in ridge], fill=255, width=width, joint="curve")
        elevation += gaussian_filter(np.asarray(trace, dtype=np.float32) / 255.0, sigma=width * 0.85) * strength

    # Western highlands, southern subtropics and the Sichuan basin add terrain mass.
    western_uplift = np.clip((108.0 - lon_field) / 14.0, 0, 1) * 0.25
    plateau = np.exp(-(((lon_field - 102.8) / 5.0) ** 2 + ((lat_field - 25.5) / 3.1) ** 2)) * 0.20
    sichuan_basin = np.exp(-(((lon_field - 105.5) / 2.8) ** 2 + ((lat_field - 30.7) / 1.9) ** 2)) * 0.33
    elevation = np.clip((elevation + western_uplift + plateau - sichuan_basin) * land_mask, 0, 1)

    rng = np.random.default_rng(1127)
    texture = gaussian_filter(rng.normal(0, 1, (HEIGHT, WIDTH)).astype(np.float32), sigma=5.0)
    texture += gaussian_filter(rng.normal(0, 1, (HEIGHT, WIDTH)).astype(np.float32), sigma=21.0) * 0.8
    grad_y, grad_x = np.gradient(elevation)
    relief_light = np.clip((-grad_x * 0.85 - grad_y * 0.45) * 120, -0.22, 0.22)
    south = np.clip((35.0 - lat_field) / 15.0, 0, 1)

    lowland = np.zeros_like(ocean)
    lowland[:, :, 0] = 119 - south * 29
    lowland[:, :, 1] = 111 + south * 4
    lowland[:, :, 2] = 76 + south * 0
    highland = np.zeros_like(ocean)
    highland[:, :, 0] = 150
    highland[:, :, 1] = 125
    highland[:, :, 2] = 90
    terrain = lowland * (1 - elevation[:, :, None] * 0.68) + highland * elevation[:, :, None] * 0.68
    terrain += relief_light[:, :, None] * np.array([50, 43, 30], dtype=np.float32)
    terrain += texture[:, :, None] * np.array([13, 11, 8], dtype=np.float32)

    image_array = ocean * (1 - land_mask[:, :, None]) + terrain * land_mask[:, :, None]
    image = Image.fromarray(np.clip(image_array, 0, 255).astype(np.uint8), "RGB")
    draw = ImageDraw.Draw(image, "RGBA")

    for coastline in coast_paths:
        draw.line(coastline, fill=(181, 188, 151, 175), width=3, joint="curve")

    lake_names = {"Poyang Hu": 4, "Tai Hu": 4, "Hongze Hu": 3, "Chao Hu": 3, "Hong Hu": 3, "Liangzi Hu": 3, "Qinghai Hu": 2, "Gaoyou Hu": 3}
    for lake in geography["lakes"]:
        for polygon in polygons(lake["geometry"]):
            draw.polygon([xy(point) for point in polygon[0]], fill=(48, 106, 128, 245))
            draw.line([xy(point) for point in polygon[0]], fill=(117, 153, 150, 170), width=lake_names.get(lake["name"], 2))

    # Dongting is absent in the selected Natural Earth resolution; this is an
    # explicitly documented approximate historical shoreline, never fake GIS data.
    dongting = [(112.30, 29.64), (112.67, 29.77), (112.98, 29.67), (113.15, 29.47), (113.11, 29.18), (112.91, 28.96), (112.49, 29.03), (112.23, 29.27)]
    draw.polygon([xy(point) for point in dongting], fill=(47, 107, 130, 225))

    major = {"Chang Jiang", "Jinsha", "Yangtze", "Tongtian", "Huang"}
    for river in geography["rivers"]:
        width = 5 if river["name"] in major else 3
        color = (77, 151, 181, 230) if river["name"] in major else (85, 144, 161, 205)
        for line in lines(river["geometry"]):
            points = [xy(point) for point in line]
            if len(points) > 1:
                draw.line(points, fill=(21, 42, 48, 150), width=width + 3, joint="curve")
                draw.line(points, fill=color, width=width, joint="curve")

    # Natural Earth omits the Huai at 1:50m; these anchors follow its real basin.
    huai = [(112.75, 32.43), (113.55, 32.54), (114.38, 32.29), (115.20, 32.34), (116.21, 32.79), (117.37, 32.94), (118.72, 33.28), (119.30, 33.42)]
    pearl_delta = [(112.53, 23.04), (113.02, 23.11), (113.27, 23.13), (113.55, 22.73), (113.80, 22.45)]
    for river in [huai, pearl_delta]:
        draw.line([xy(point) for point in river], fill=(82, 152, 173, 235), width=4, joint="curve")

    # Restrained vignette keeps focus on the actual landmass rather than UI effects.
    vignette = np.clip(((cols / WIDTH - 0.5) ** 2 * 0.33 + (rows / HEIGHT - 0.51) ** 2 * 0.30), 0, 0.20)
    output = np.asarray(image, dtype=np.float32) * (1 - vignette[:, :, None])
    final = Image.fromarray(np.clip(output, 0, 255).astype(np.uint8), "RGB")
    TARGET.parent.mkdir(parents=True, exist_ok=True)
    final.save(TARGET, "WEBP", quality=91, method=6)

    print(
        json.dumps(
            {
                "target": str(TARGET.relative_to(ROOT)),
                "pixels": [WIDTH, HEIGHT],
                "bytes": TARGET.stat().st_size,
                "land_polygons": len(coast_paths),
                "natural_earth_river_features": len(geography["rivers"]),
                "natural_earth_lake_features": len(geography["lakes"]),
                "terrain_ridges": len(ridges),
            },
            ensure_ascii=False,
        )
    )


if __name__ == "__main__":
    main()
