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

    videos = sorted(V3_ROOT.rglob("*.mp4"))
    require(len(videos) == EXPECTED_V3_VIDEOS, f"expected 51 source V3 videos, found {len(videos)}")
    voices = sorted((APP_ASSETS / "audio/voice/prologue").glob("prologue_act*.m4a"))
    require(len(voices) == EXPECTED_PROLOGUE_VOICES, f"expected six prologue narrations, found {len(voices)}")

    audio_registry = (JAVA_ROOT / "com/xiemingxin/nandu/game/AudioResourceRegistry.kt").read_text(encoding="utf-8")
    approved_bgm = sorted(set(re.findall(r'"\$BASE/bgm/(bgm_[^"\n]+\.ogg)"', audio_registry)))
    missing_bgm = [name for name in approved_bgm if not (APP_ASSETS / "audio/bgm" / name).is_file()]

    print(f"COURT_ASSETS: {EXPECTED_COURT_IMAGES}/{EXPECTED_COURT_IMAGES} present, READY, SHA-256 verified")
    print("YINGTIAN: state-aligned capital registry and runtime image verified")
    print("FORMAL_VIDEO: one Media3 implementation; story CG shares AssetVideoSurface; VideoView=0")
    print(f"V3_SOURCE_VIDEOS: {len(videos)}")
    print(f"PROLOGUE_NARRATION: {len(voices)}/{EXPECTED_PROLOGUE_VOICES}")
    if missing_bgm:
        print("::warning::Approved BGM binaries are absent from the repository and CI APK: " + ", ".join(missing_bgm))
    else:
        print(f"APPROVED_BGM: {len(approved_bgm)}/{len(approved_bgm)} present")


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
        if not approved_bgm:
            print("::warning::APK contains zero approved BGM tracks; the eight externally injected V1.6.1 tracks remain a release blocker")
        else:
            print(f"APK_APPROVED_BGM: {len(approved_bgm)}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, help="audit the built APK in addition to source assets")
    args = parser.parse_args()
    try:
        source_audit()
        if args.apk is not None:
            apk_audit(args.apk.resolve())
    except (AuditFailure, OSError, subprocess.CalledProcessError, ValueError, zipfile.BadZipFile) as exc:
        print(f"::error::V1.6.2 preacceptance audit failed: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
