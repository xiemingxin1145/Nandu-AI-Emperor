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
    print("FORMAL_VIDEO: one Media3 implementation; story CG shares AssetVideoSurface; VideoView=0")
    print("FORMAL_ROUTES: main-menu settings, military return, system back and state-derived capital verified")
    print(f"V3_SOURCE_VIDEOS: {len(videos)}")
    print(f"PROLOGUE_NARRATION: {len(voices)}/{EXPECTED_PROLOGUE_VOICES}")
    print("SMOKE_MATRIX: " + "; ".join(f"{status}={count}" for status, count in smoke_counts.items()))
    print(f"APPROVED_BGM: {len(present_bgm)}/{EXPECTED_APPROVED_BGM}")
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
        voices = sorted(member for member in members if re.fullmatch(r"assets/audio/voice/prologue/prologue_act[^/]+\.m4a", member))
        require(len(voices) == EXPECTED_PROLOGUE_VOICES, f"APK contains {len(voices)} prologue narration files instead of six")

        videos = sorted(member for member in members if member.startswith("assets/videos/") and member.endswith(".mp4"))
        require(len(videos) == EXPECTED_V3_VIDEOS, f"APK contains {len(videos)} V3 videos instead of 51")
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
        print(f"APK_V3_VIDEO: h264={len(videos)}/{EXPECTED_V3_VIDEOS}; yuv420p={len(videos)}/{EXPECTED_V3_VIDEOS}; embedded_audio=0")
        print(f"APK_PROLOGUE_NARRATION: {len(voices)}/{EXPECTED_PROLOGUE_VOICES}")
        print(f"APK_APPROVED_BGM: {len(approved_bgm)}/{EXPECTED_APPROVED_BGM}")
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
