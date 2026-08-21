#!/usr/bin/env python3
"""Audit merged court art, formal video routes, and the actual packaged APK."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as xml
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "docs/art/COURT_NPC_ASSET_MANIFEST_CLEAN.json"
JAVA_ROOT = ROOT / "app/src/main/java"
APP_ASSETS = ROOT / "app/src/main/assets"
V3_ROOT = ROOT / "assets/videos"
EXPECTED_COURT_IMAGES = 54
EXPECTED_V3_VIDEOS = 51
EXPECTED_PROLOGUE_VOICES = 6
EXPECTED_APPROVED_BGM = 8
EXPECTED_SMOKE_ROWS = 49
EXPECTED_MAP_ICONS = 16
EXPECTED_MAP_ICON_REFERENCES = 24
EXPECTED_MAP_DECORATIONS = 10
EXPECTED_ACTIVE_MAP_DECORATIONS = 5
EXPECTED_MAP_BACKGROUNDS = 6
EXPECTED_REGISTERED_CITY_BACKGROUNDS = 31
EXPECTED_SEASONAL_PRESENTATIONS = 4
BGM_CANDIDATE_MANIFEST = ROOT / "docs/audio/BGM_V162_DEVICE_CANDIDATE_SHA256.txt"


class AuditFailure(RuntimeError):
    pass


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AuditFailure(message)


def sha256_bytes(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def read_manifest() -> list[dict[str, object]]:
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    records = manifest["records"]
    require(manifest["image_count"] == EXPECTED_COURT_IMAGES, "court manifest count changed")
    require(len(records) == EXPECTED_COURT_IMAGES, "court manifest does not contain 54 records")
    require(len({record["path"] for record in records}) == EXPECTED_COURT_IMAGES, "duplicate court asset paths")
    return records


def source_audit() -> None:
    records = read_manifest()
    for record in records:
        path = ROOT / str(record["path"])
        require(path.is_file(), f"missing court asset: {path.relative_to(ROOT)}")
        require(record["status"] == "READY", f"unapproved court asset: {path.relative_to(ROOT)}")
        require(sha256_bytes(path.read_bytes()) == record["sha256"], f"court asset checksum changed: {path.relative_to(ROOT)}")

    court_files = [
        *APP_ASSETS.joinpath("images/characters/court_crowd_v1").glob("*.webp"),
        *APP_ASSETS.joinpath("images/characters/npc_court_v1").glob("*.webp"),
    ]
    require(len(court_files) == EXPECTED_COURT_IMAGES, f"expected 54 court images, found {len(court_files)}")

    yingtian = APP_ASSETS / "images/city/yingtianfu.webp"
    require(yingtian.is_file(), "Yingtian city image missing from the runtime asset directory")
    city_registry = (JAVA_ROOT / "com/xiemingxin/nandu/game/CityVisualRegistry.kt").read_text(encoding="utf-8")
    art_registry = (JAVA_ROOT / "com/xiemingxin/nandu/game/ArtResourceRegistry.kt").read_text(encoding="utf-8")
    map_data = (JAVA_ROOT / "com/xiemingxin/nandu/game/MapData.kt").read_text(encoding="utf-8")
    require('"yingtianfu", "应天府", CityVisualTier.CAPITAL' in city_registry, "Yingtian is not visually marked as capital")
    require('"images/city/yingtianfu.webp"' in city_registry, "Yingtian runtime image is not registered")
    require('"yingtianfu" to city("yingtianfu", "应天府", "yingtianfu.webp")' in art_registry, "Yingtian art registry mapping missing")
    require(re.search(r'MapNode\("yingtianfu",[^\n]+isCapital\s*=\s*true', map_data) is not None, "Yingtian map node is not the opening capital")
    require(re.search(r'MapNode\("linan",[^\n]+isCapital\s*=\s*true', map_data) is None, "Hangzhou is incorrectly marked as opening capital")

    icon_files = sorted((APP_ASSETS / "images/map/icons").glob("*.webp"))
    decoration_files = sorted((APP_ASSETS / "images/map/decorations").glob("*.webp"))
    background_files = sorted((APP_ASSETS / "images/map").glob("*.webp"))
    require(len(icon_files) == EXPECTED_MAP_ICONS, f"expected 16 real map icon files, found {len(icon_files)}")
    require(len(decoration_files) == EXPECTED_MAP_DECORATIONS, f"expected 10 existing map decorations, found {len(decoration_files)}")
    require(len(background_files) == EXPECTED_MAP_BACKGROUNDS, f"expected six existing map backgrounds, found {len(background_files)}")

    icon_registry_block = art_registry.split("val mapIconImages:", 1)[1].split("\n    )", 1)[0]
    icon_references = re.findall(r'"([^"\n]+)"\s+to\s+icon\([^\n]+?"(city_[^"\n]+\.webp)"\)', icon_registry_block)
    require(len(icon_references) == EXPECTED_MAP_ICON_REFERENCES, f"expected 24 formal icon aliases, found {len(icon_references)}")
    for alias, filename in icon_references:
        require((APP_ASSETS / "images/map/icons" / filename).is_file(), f"map icon alias has no real asset: {alias} -> {filename}")
    require(len({filename for _, filename in icon_references}) == EXPECTED_MAP_ICONS, "map icon aliases do not cover all 16 real files")

    important_icons = re.findall(r'"(images/map/icons/city_[^"\n]+\.webp)"', city_registry)
    require(len(important_icons) == 15, f"expected 15 corrected important-city icon paths, found {len(important_icons)}")
    for path in important_icons:
        require((APP_ASSETS / path).is_file(), f"important city map icon does not exist: {path}")
    require(re.search(r'"images/map/city_[^"\n]+\.webp"', city_registry) is None, "city map icon path is missing the icons/ directory")
    require("mapIconPath = ArtResourceRegistry.mapIcon(iconKey)" in city_registry, "dynamic map icon fallback does not resolve through real registered assets")
    require("panelBackgroundPath = ArtResourceRegistry.cityBackground(id)" in city_registry, "dynamic city background bypasses the actual image registry")

    city_background_block = art_registry.split("val cityBackgrounds:", 1)[1].split("\n    )", 1)[0]
    city_backgrounds = re.findall(r'"([^"\n]+)"\s+to\s+city\([^\n]+?"([^"\n]+\.webp)"\)', city_background_block)
    require(len(city_backgrounds) == EXPECTED_REGISTERED_CITY_BACKGROUNDS, f"expected 31 registered city backgrounds, found {len(city_backgrounds)}")
    for city_id, filename in city_backgrounds:
        require((APP_ASSETS / "images/city" / filename).is_file(), f"registered city background is missing: {city_id} -> {filename}")

    decoration_block = city_registry.split("object MapDecorationRegistry", 1)[1].split("object CityVisualRegistry", 1)[0]
    active_decorations = re.findall(r'const val\s+(\w+)\s*=\s*"\$BASE/([^"\n]+\.webp)"', decoration_block)
    require(len(active_decorations) == EXPECTED_ACTIVE_MAP_DECORATIONS, f"expected five safely wired map decorations, found {len(active_decorations)}")
    map_screen = (JAVA_ROOT / "com/xiemingxin/nandu/ui/screens/MapScreen.kt").read_text(encoding="utf-8")
    require("path = visual.mapIconPath" in map_screen, "formal map UI does not consume CityVisualRegistry.mapIconPath")
    for constant, filename in active_decorations:
        require((APP_ASSETS / "images/map/decorations" / filename).is_file(), f"active map decoration is missing: {filename}")
        if constant in {"songArmyBanner", "jinArmyBanner"}:
            require("MapDecorationRegistry::armyBannerFor" in map_screen, "map army banners are not attached to actual armies")
        else:
            require(f"MapDecorationRegistry.{constant}" in map_screen, f"map decoration is registered but not used: {constant}")

    video_view_paths: list[str] = []
    builders: list[str] = []
    for source in JAVA_ROOT.rglob("*.kt"):
        content = source.read_text(encoding="utf-8")
        if re.search(r"^\s*import\s+android\.widget\.VideoView\b|\bVideoView\s*\(", content, re.MULTILINE):
            video_view_paths.append(str(source.relative_to(ROOT)))
        if "ExoPlayer.Builder(" in content:
            builders.append(str(source.relative_to(ROOT)))
    require(not video_view_paths, "formal video implementation still imports/constructs VideoView: " + ", ".join(video_view_paths))
    require(builders == ["app/src/main/java/com/xiemingxin/nandu/ui/components/AssetVideo.kt"], "expected exactly one formal Media3 player implementation")
    cg_dialog = (JAVA_ROOT / "com/xiemingxin/nandu/ui/components/CgVideoDialog.kt").read_text(encoding="utf-8")
    require("AssetVideoSurface(" in cg_dialog, "story CG dialog bypasses the shared Media3 asset player")
    require("ArtResourceRegistry.Fallback.event" in cg_dialog, "story CG dialog has no static-image fallback")

    seasonal_videos = {
        "spring": "V04_season_spring.mp4",
        "summer": "V05_season_summer.mp4",
        "autumn": "V06_season_autumn.mp4",
        "winter": "V07_season_winter.mp4",
    }
    for season, filename in seasonal_videos.items():
        require((V3_ROOT / "seasons" / filename).is_file(), f"seasonal video is missing: {filename}")
        require((ROOT / "assets/ui_textures" / f"season_{season}_bg.webp").is_file(), f"seasonal static CG is missing: {season}")
    world_overlay = (JAVA_ROOT / "com/xiemingxin/nandu/ui/components/WorldTurnReplayOverlay.kt").read_text(encoding="utf-8")
    world_policy = (JAVA_ROOT / "com/xiemingxin/nandu/game/WorldPresentationPolicy.kt").read_text(encoding="utf-8")
    court_screen = (JAVA_ROOT / "com/xiemingxin/nandu/ui/screens/EmperorMainScreen.kt").read_text(encoding="utf-8")
    view_model = (JAVA_ROOT / "com/xiemingxin/nandu/ui/EmperorViewModel.kt").read_text(encoding="utf-8")
    require("AssetVideoSurface(" in world_overlay and "fallbackPath =" in world_overlay, "seasonal presentation does not preserve Media3 and static CG fallback")
    require("if (before.season == after.season) return null" in world_policy, "seasonal video can replay without an actual season transition")
    require("season = nextCalendar.season()" in view_model, "world turn does not synchronize actual season with its calendar")
    require(re.search(r"decision\.canExecute\(result(?:,\s*mandate\s*!=\s*null)?\)", court_screen) is not None and
            re.search(r"current\.imperialDecision\.canExecute\(edictResult(?:,\s*mandate\s*!=\s*null)?\)", view_model) is not None,
            "court decision has no UI and execution-level approval guard")
    require("综合诸议" in court_screen and "朱批准行" in court_screen and "补充圣意" in court_screen, "formal court decision actions are missing")
    require("WorldPresentationPolicy.commandDescription(state, cmd)" in court_screen, "court command preview may expose raw internal identifiers")
    require("WorldTurnReplayOverlay(" in map_screen and "SeasonalTransitionOverlay(" in map_screen, "formal map does not show actual turn replay and season transition")
    mandate_policy = (JAVA_ROOT / "com/xiemingxin/nandu/game/ImperialMandatePolicy.kt").read_text(encoding="utf-8")
    mandate_system = (JAVA_ROOT / "com/xiemingxin/nandu/game/ImperialMandate.kt").read_text(encoding="utf-8")
    world_executor = (JAVA_ROOT / "com/xiemingxin/nandu/game/WorldAiTurnExecutor.kt").read_text(encoding="utf-8")
    save_codec = (JAVA_ROOT / "com/xiemingxin/nandu/game/GameSaveCodec.kt").read_text(encoding="utf-8")
    require("ImperialMandatePolicy.draft(" in view_model and "ImperialMandateSystem.issue(" in view_model,
            "approved continuing imperial orders do not create actual mandates")
    require("收回授权" in court_screen and "revokeImperialMandate" in view_model,
            "the emperor cannot inspect or revoke existing imperial authority")
    require("prioritizeManualCommands" in mandate_policy and "prioritizeManualCommands" in view_model,
            "a direct imperial order does not override conflicting automatic authority")
    require("mandateExecutionLog = newState.mandateExecutionLog + record" in world_executor,
            "delegated world actions are not written into the authoritative execution log")
    require('put("imperialMandates"' in save_codec and 'put("mandateExecutionLog"' in save_codec,
            "imperial mandates and accountable execution records are not saved")
    require("scheduledTurn = null" in mandate_system,
            "an explicit imperial mandate does not override an obsolete scripted officer teleport")
    require("WorldTurnActionKind.RECRUIT" in world_policy and "WorldTurnActionKind.REPAIR_DEFENSE" in world_policy,
            "actual delegated recruitment and defense repairs do not enter world replay")

    main_activity = (JAVA_ROOT / "com/xiemingxin/nandu/MainActivity.kt").read_text(encoding="utf-8")
    main_menu = (JAVA_ROOT / "com/xiemingxin/nandu/ui/screens/MainMenuScreen.kt").read_text(encoding="utf-8")
    navigation = (JAVA_ROOT / "com/xiemingxin/nandu/ui/AppNavigationPolicy.kt").read_text(encoding="utf-8")
    victory = (JAVA_ROOT / "com/xiemingxin/nandu/game/VictoryJudge.kt").read_text(encoding="utf-8")
    require("showIntro && !showPrologue && !showSettings" in navigation, "main-menu settings can be hidden by the intro route")
    require("AppNavigationPolicy.shouldShowMainMenu(showIntro, showPrologue, showSettings)" in main_activity, "main menu does not use the tested route policy")
    require("BackHandler(enabled = uiState.ending == GameEnding.ONGOING && backTarget != null)" in main_activity, "formal routes do not handle Android system back")
    require("BackHandler(enabled = showGallery)" in main_menu, "video gallery does not handle Android system back")
    require(re.search(r"MilitaryScreenV4\([\s\S]*?onBack\s*=\s*\{\s*\}", main_activity) is None, "military screen has an empty return callback")
    require(re.search(r'text\s*=\s*"[^"\n]*(?:Demo|测试版)[^"\n]*"', main_menu, re.IGNORECASE) is None, "formal main menu exposes Demo copy")
    require("capitalCityId" in victory and "city.isCapital" in victory, "defeat is not derived from the real faction and capital state")
    require('it.id == "linan"' not in victory, "defeat still hardcodes Hangzhou as the capital")
    require("临安陷落" not in victory, "capital-lost ending still names a fixed future capital")

    geography_source = JAVA_ROOT / "com/xiemingxin/nandu/game/EastAsiaGeography.kt"
    geography_map = JAVA_ROOT / "com/xiemingxin/nandu/ui/screens/GeoMapScreenV2.kt"
    geography_basemap = APP_ASSETS / "images/map_v2/east_asia_relief.webp"
    if geography_source.exists() or geography_map.exists() or geography_basemap.exists():
        require(geography_source.is_file() and geography_map.is_file() and geography_basemap.is_file(),
                "real East Asian map V2 is missing its geographic source, screen or relief asset")
        geographic_code = geography_source.read_text(encoding="utf-8")
        map_v2_code = geography_map.read_text(encoding="utf-8")
        locations = re.findall(r'"([^"\n]+)"\s+to\s+GeoLocation\(', geographic_code)
        require(len(locations) == 79 and len(set(locations)) == 79,
                f"real geographic map does not locate all 79 existing nodes: {len(locations)}")
        require('2 -> GeoMapScreenV2(' in main_activity, "formal 山河 entry does not open the geographic map")
        require('"寰宇"' in map_v2_code and '"山河"' in map_v2_code and '"近览"' in map_v2_code,
                "far and close views do not share the geographic map camera")
        require('"隐藏城点"' in map_v2_code, "the real basemap cannot be verified with city nodes hidden")
        require('WorldTurnReplayOverlay(' in map_v2_code and 'SeasonalTransitionOverlay(' in map_v2_code,
                "real geographic map does not preserve world replay and seasonal playback")
        image_streams = ffprobe(geography_basemap).get("streams", [])
        require(len(image_streams) == 1 and image_streams[0].get("codec_name") == "webp",
                "real geographic basemap is not a valid WEBP")
        require(image_streams[0].get("width") >= 2400 and image_streams[0].get("height") >= 1500,
                "real geographic basemap resolution is insufficient for mobile zoom")

    videos = sorted(V3_ROOT.rglob("*.mp4"))
    require(len(videos) == EXPECTED_V3_VIDEOS, f"expected 51 source V3 videos, found {len(videos)}")
    voices = sorted((APP_ASSETS / "audio/voice/prologue").glob("prologue_act*.m4a"))
    require(len(voices) == EXPECTED_PROLOGUE_VOICES, f"expected six prologue narrations, found {len(voices)}")

    audio_registry = (JAVA_ROOT / "com/xiemingxin/nandu/game/AudioResourceRegistry.kt").read_text(encoding="utf-8")
    approved_bgm = sorted(set(re.findall(r'"\$BASE/bgm/(bgm_[^"\n]+\.ogg)"', audio_registry)))
    require(len(approved_bgm) == EXPECTED_APPROVED_BGM, f"expected eight approved BGM registry slots, found {len(approved_bgm)}")
    missing_bgm = [name for name in approved_bgm if not (APP_ASSETS / "audio/bgm" / name).is_file()]
    present_bgm = [name for name in approved_bgm if name not in missing_bgm]
    for name in present_bgm:
        path = APP_ASSETS / "audio/bgm" / name
        require(path.stat().st_size >= 128 * 1024, f"approved BGM is empty or an invalid placeholder: {name}")
        streams = ffprobe(path).get("streams", [])
        audio_streams = [stream for stream in streams if stream.get("codec_type") == "audio"]
        require(len(audio_streams) == 1, f"approved BGM must have exactly one audio stream: {name}")
        require(audio_streams[0].get("codec_name") == "vorbis", f"approved BGM is not OGG Vorbis: {name}")
        require(str(audio_streams[0].get("sample_rate")) == "48000", f"approved BGM is not 48 kHz: {name}")
        require(audio_streams[0].get("channels") == 2, f"approved BGM is not stereo: {name}")

    if len(present_bgm) == EXPECTED_APPROVED_BGM and BGM_CANDIDATE_MANIFEST.is_file():
        candidate_rows = {
            filename: digest
            for digest, filename in re.findall(
                r"^([a-f0-9]{64})\s+(bgm_[^\s]+\.ogg)$",
                BGM_CANDIDATE_MANIFEST.read_text(encoding="utf-8"),
                re.MULTILINE,
            )
        }
        require(set(candidate_rows) == set(approved_bgm), "BGM candidate SHA-256 manifest does not cover all eight registered slots")
        for filename, digest in candidate_rows.items():
            require(sha256_bytes((APP_ASSETS / "audio/bgm" / filename).read_bytes()) == digest, f"BGM candidate checksum changed: {filename}")

    matrix = (ROOT / "docs/V162_SMOKE_TEST_MATRIX.md").read_text(encoding="utf-8")
    smoke_rows = [line for line in matrix.splitlines() if re.match(r"^\| (?:MENU|PRO|COURT|MAP|PEOPLE|GOV|MIL|HIST|MEDIA|AI|SAVE|NAV)-\d+ \|", line)]
    require(len(smoke_rows) == EXPECTED_SMOKE_ROWS, f"expected 49 formal smoke-test rows, found {len(smoke_rows)}")
    smoke_counts = {status: 0 for status in ("PASS", "BLOCKED", "DEVICE_REQUIRED")}
    for row in smoke_rows:
        columns = [part.strip() for part in re.split(r"(?<!\\)\|", row)[1:-1]]
        require(len(columns) == 10, f"smoke-test row has an invalid column count: {columns[0]}")
        status = columns[-1].strip("`")
        require(status in smoke_counts, f"smoke-test row has no formal acceptance status: {columns[0]}")
        smoke_counts[status] += 1
    bgm_row = next(row for row in smoke_rows if row.startswith("| MEDIA-03 |"))
    if missing_bgm:
        require(bgm_row.rstrip().endswith("`BLOCKED` |"), "missing approved BGM is not marked BLOCKED in the smoke matrix")

    print(f"COURT_ASSETS: {EXPECTED_COURT_IMAGES}/{EXPECTED_COURT_IMAGES} present, READY, SHA-256 verified")
    print("YINGTIAN: state-aligned capital registry and runtime image verified")
    print(f"MAP_ICONS: files={len(icon_files)}/{EXPECTED_MAP_ICONS}; references={len(icon_references)}; important_city_paths={len(important_icons)}; dynamic_fallback=registered")
    print(f"MAP_DECORATIONS: available={len(decoration_files)}/{EXPECTED_MAP_DECORATIONS}; safely_active={len(active_decorations)}")
    print(f"MAP_BACKGROUNDS: {len(background_files)}/{EXPECTED_MAP_BACKGROUNDS}; CITY_BACKGROUNDS: {len(city_backgrounds)}/{EXPECTED_REGISTERED_CITY_BACKGROUNDS}")
    print("FORMAL_VIDEO: one Media3 implementation; story CG shares AssetVideoSurface; VideoView=0")
    print(f"WORLD_PRESENTATION: council_selection=guarded; state_backed_replay=enabled; seasonal_video={len(seasonal_videos)}/{EXPECTED_SEASONAL_PRESENTATIONS}; seasonal_cg={len(seasonal_videos)}/{EXPECTED_SEASONAL_PRESENTATIONS}")
    print("IMPERIAL_MANDATES: issued_from_court=1; revocable=1; persistent=1; execution_log=1; state_backed_replay=1")
    print("FORMAL_ROUTES: main-menu settings, military return, system back and state-derived capital verified")
    print(f"V3_SOURCE_VIDEOS: {len(videos)}")
    print(f"PROLOGUE_NARRATION: {len(voices)}/{EXPECTED_PROLOGUE_VOICES}")
    print("SMOKE_MATRIX: " + "; ".join(f"{status}={count}" for status, count in smoke_counts.items()))
    print(f"BGM_REGISTRY_ASSETS: {len(present_bgm)}/{EXPECTED_APPROVED_BGM}; approval=DEVICE_REQUIRED")
    if geography_basemap.is_file():
        print(f"GEO_MAP_V2: 79/79 real locations; relief={image_streams[0]['width']}x{image_streams[0]['height']}; bytes={geography_basemap.stat().st_size}; unified_zoom=1")
    if missing_bgm:
        print("::warning::Approved BGM binaries are absent from the repository and CI APK: " + ", ".join(missing_bgm))


def ffprobe(path: Path) -> dict[str, object]:
    completed = subprocess.run(
        ["ffprobe", "-v", "error", "-show_streams", "-of", "json", str(path)],
        check=True,
        capture_output=True,
        text=True,
    )
    return json.loads(completed.stdout)


def apk_audit(apk_path: Path) -> None:
    require(apk_path.is_file(), f"APK does not exist: {apk_path}")
    records = read_manifest()
    with zipfile.ZipFile(apk_path) as apk:
        members = set(apk.namelist())
        for record in records:
            relative = Path(str(record["path"])).relative_to("app/src/main/assets")
            member = "assets/" + relative.as_posix()
            require(member in members, f"court art missing from APK: {member}")
            require(sha256_bytes(apk.read(member)) == record["sha256"], f"court art checksum mismatch in APK: {member}")

        require("assets/images/city/yingtianfu.webp" in members, "Yingtian runtime city image missing from APK")
        require("assets/video/VID-CZ-001-PREWAR-V01.mp4" in members, "legacy story CG asset missing from APK")
        geography_basemap = APP_ASSETS / "images/map_v2/east_asia_relief.webp"
        if geography_basemap.is_file():
            geography_member = "assets/images/map_v2/east_asia_relief.webp"
            require(geography_member in members, "real East Asian relief map missing from APK")
            require(sha256_bytes(apk.read(geography_member)) == sha256_bytes(geography_basemap.read_bytes()),
                    "real East Asian relief map checksum mismatch in APK")
        apk_map_icons = sorted(member for member in members if re.fullmatch(r"assets/images/map/icons/[^/]+\.webp", member))
        apk_map_decorations = sorted(member for member in members if re.fullmatch(r"assets/images/map/decorations/[^/]+\.webp", member))
        apk_map_backgrounds = sorted(member for member in members if re.fullmatch(r"assets/images/map/[^/]+\.webp", member))
        require(len(apk_map_icons) == EXPECTED_MAP_ICONS, f"APK contains {len(apk_map_icons)} map icons instead of 16")
        require(len(apk_map_decorations) == EXPECTED_MAP_DECORATIONS, f"APK contains {len(apk_map_decorations)} map decorations instead of 10")
        require(len(apk_map_backgrounds) == EXPECTED_MAP_BACKGROUNDS, f"APK contains {len(apk_map_backgrounds)} map backgrounds instead of six")
        for path in (APP_ASSETS / "images/map/icons").glob("*.webp"):
            require(f"assets/images/map/icons/{path.name}" in members, f"map icon missing from APK: {path.name}")
        for path in (APP_ASSETS / "images/map/decorations").glob("*.webp"):
            require(f"assets/images/map/decorations/{path.name}" in members, f"map decoration missing from APK: {path.name}")

        voices = sorted(member for member in members if re.fullmatch(r"assets/audio/voice/prologue/prologue_act[^/]+\.m4a", member))
        require(len(voices) == EXPECTED_PROLOGUE_VOICES, f"APK contains {len(voices)} prologue narration files instead of six")

        videos = sorted(member for member in members if member.startswith("assets/videos/") and member.endswith(".mp4"))
        require(len(videos) == EXPECTED_V3_VIDEOS, f"APK contains {len(videos)} V3 videos instead of 51")
        seasonal_video_members = sorted(member for member in videos if member.startswith("assets/videos/seasons/"))
        seasonal_cg_members = sorted(member for member in members if re.fullmatch(r"assets/ui_textures/season_(?:spring|summer|autumn|winter)_bg\.webp", member))
        require(len(seasonal_video_members) == EXPECTED_SEASONAL_PRESENTATIONS, f"APK contains {len(seasonal_video_members)} seasonal videos instead of four")
        require(len(seasonal_cg_members) == EXPECTED_SEASONAL_PRESENTATIONS, f"APK contains {len(seasonal_cg_members)} seasonal CG backgrounds instead of four")
        with tempfile.TemporaryDirectory(prefix="nandu-v162-video-audit-") as scratch:
            probe_path = Path(scratch) / "video.mp4"
            for member in videos:
                probe_path.write_bytes(apk.read(member))
                streams = ffprobe(probe_path).get("streams", [])
                video_streams = [stream for stream in streams if stream.get("codec_type") == "video"]
                audio_streams = [stream for stream in streams if stream.get("codec_type") == "audio"]
                require(len(video_streams) == 1, f"APK video must have one video stream: {member}")
                require(video_streams[0].get("codec_name") == "h264", f"APK video is not H.264: {member}")
                require(video_streams[0].get("pix_fmt") == "yuv420p", f"APK video is not yuv420p: {member}")
                require(not audio_streams, f"V3 generated video unexpectedly includes embedded audio: {member}")

        approved_bgm = sorted(member for member in members if re.fullmatch(r"assets/audio/bgm/bgm_[^/]+\.ogg", member))
        print(f"APK_COURT_ASSETS: {EXPECTED_COURT_IMAGES}/{EXPECTED_COURT_IMAGES} present and SHA-256 verified")
        print("APK_YINGTIAN_IMAGE: assets/images/city/yingtianfu.webp present")
        print(f"APK_MAP_ICONS: {len(apk_map_icons)}/{EXPECTED_MAP_ICONS}")
        print(f"APK_MAP_DECORATIONS: {len(apk_map_decorations)}/{EXPECTED_MAP_DECORATIONS}; active={EXPECTED_ACTIVE_MAP_DECORATIONS}")
        print(f"APK_MAP_BACKGROUNDS: {len(apk_map_backgrounds)}/{EXPECTED_MAP_BACKGROUNDS}")
        if geography_basemap.is_file():
            print(f"APK_GEO_MAP_V2: 1/1; sha256=verified; bytes={geography_basemap.stat().st_size}")
        print(f"APK_V3_VIDEO: h264={len(videos)}/{EXPECTED_V3_VIDEOS}; yuv420p={len(videos)}/{EXPECTED_V3_VIDEOS}; embedded_audio=0")
        print(f"APK_SEASONAL_VIDEO: {len(seasonal_video_members)}/{EXPECTED_SEASONAL_PRESENTATIONS}; APK_SEASONAL_CG: {len(seasonal_cg_members)}/{EXPECTED_SEASONAL_PRESENTATIONS}")
        print(f"APK_PROLOGUE_NARRATION: {len(voices)}/{EXPECTED_PROLOGUE_VOICES}")
        print(f"APK_BGM_REGISTRY_ASSETS: {len(approved_bgm)}/{EXPECTED_APPROVED_BGM}; approval=DEVICE_REQUIRED")
        if len(approved_bgm) != EXPECTED_APPROVED_BGM:
            print(f"::warning::APK contains {len(approved_bgm)}/{EXPECTED_APPROVED_BGM} approved BGM tracks; missing original audio remains a release blocker")


def unit_test_audit(results_path: Path) -> None:
    require(results_path.is_dir(), f"JVM test result directory does not exist: {results_path}")
    reports = sorted(results_path.glob("TEST-*.xml"))
    require(bool(reports), "no JVM unit test XML reports were produced")
    totals = {name: 0 for name in ("tests", "failures", "errors", "skipped")}
    for report in reports:
        suite = xml.parse(report).getroot()
        for name in totals:
            totals[name] += int(suite.attrib.get(name, "0"))
    declared = sum(
        len(re.findall(r"@Test\b", source.read_text(encoding="utf-8")))
        for source in (ROOT / "app/src/test/java").rglob("*.kt")
    )
    require(totals["tests"] == declared, f"JVM test execution mismatch: ran {totals['tests']} of {declared} declared tests")
    require(totals["failures"] == 0 and totals["errors"] == 0, "JVM unit tests contain failures or errors")
    require(totals["skipped"] == 0, "JVM unit tests were skipped")
    print("UNIT_TESTS: " + "; ".join(f"{name}={count}" for name, count in totals.items()))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, help="audit the built APK in addition to source assets")
    parser.add_argument("--test-results", type=Path, help="verify every declared JVM test actually ran")
    args = parser.parse_args()
    try:
        source_audit()
        if args.test_results is not None:
            unit_test_audit(args.test_results.resolve())
        if args.apk is not None:
            apk_audit(args.apk.resolve())
    except (AuditFailure, OSError, subprocess.CalledProcessError, ValueError, xml.ParseError, zipfile.BadZipFile) as exc:
        print(f"::error::V1.6.2 preacceptance audit failed: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
