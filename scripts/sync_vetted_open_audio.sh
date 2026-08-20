#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BGM="$ROOT/app/src/main/assets/audio/bgm"
UI="$ROOT/app/src/main/assets/audio/ui"
VOICE="$ROOT/app/src/main/assets/audio/voice"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

mkdir -p "$BGM" "$UI" "$VOICE"

# Current demo audio contains generated prompt/voice material that can be mistaken for BGM/SFX.
# Quarantine the high-frequency channels completely before importing vetted replacements.
rm -f "$BGM"/*.ogg "$UI"/*.ogg "$VOICE"/* 2>/dev/null || true

fetch_sha256() {
  local url="$1"
  local sha="$2"
  local out="$3"
  curl -fL --retry 4 --retry-delay 2 "$url" -o "$out"
  printf '%s  %s\n' "$sha" "$out" | sha256sum -c -
}

SB_BASE="https://raw.githubusercontent.com/blancmathis/Super_Bash_Folds/main/public/assets/audio/open"

# Verified CC0 Kenney-derived loops / interface sounds, pinned by SHA-256.
fetch_sha256 "$SB_BASE/music/menu-loop.ogg" \
  "3028433d60fa198ac919a934aef07256e1bf81eeb585f25426fd57f2a76cee7d" \
  "$TMP/menu-loop.ogg"
fetch_sha256 "$SB_BASE/music/battle-loop.ogg" \
  "5323dfe7f172e6b85870790f60027a533bd87bfd233e6c99cab0caf923f68f3f" \
  "$TMP/battle-loop.ogg"
fetch_sha256 "$SB_BASE/sfx/menu-back.ogg" \
  "61581c58194e3f19f531072edabbc344204c7e0a2887b8ededce4357bcf09195" \
  "$TMP/menu-back.ogg"
fetch_sha256 "$SB_BASE/sfx/menu-confirm.ogg" \
  "33b17a9a9a2397c62b285c52c33a907fdffb476909c99e42dde603f6a7a8b12c" \
  "$TMP/menu-confirm.ogg"
fetch_sha256 "$SB_BASE/sfx/menu-move.ogg" \
  "ad09146e4ea33b931b2f5dfb4051a4f1fe4a36f1a48c42c5e9269c292ae21214" \
  "$TMP/menu-move.ogg"

# Safe fallback BGM. These are real instrumental loops, not prompt narration.
cp "$TMP/menu-loop.ogg" "$BGM/bgm_main_menu.ogg"
cp "$TMP/menu-loop.ogg" "$BGM/bgm_court.ogg"
cp "$TMP/menu-loop.ogg" "$BGM/bgm_palace_hall.ogg"
cp "$TMP/menu-loop.ogg" "$BGM/bgm_court_council.ogg"
cp "$TMP/menu-loop.ogg" "$BGM/bgm_map.ogg"
cp "$TMP/menu-loop.ogg" "$BGM/bgm_city.ogg"
cp "$TMP/menu-loop.ogg" "$BGM/bgm_market.ogg"
cp "$TMP/menu-loop.ogg" "$BGM/bgm_diplomacy.ogg"
cp "$TMP/menu-loop.ogg" "$BGM/bgm_ritual.ogg"
cp "$TMP/menu-loop.ogg" "$BGM/bgm_event_sad.ogg"
cp "$TMP/menu-loop.ogg" "$BGM/bgm_victory.ogg"
cp "$TMP/battle-loop.ogg" "$BGM/bgm_military.ogg"
cp "$TMP/battle-loop.ogg" "$BGM/bgm_battle.ogg"
cp "$TMP/battle-loop.ogg" "$BGM/bgm_crisis.ogg"
cp "$TMP/battle-loop.ogg" "$BGM/bgm_defeat.ogg"

# Replace every frequently-triggered UI sound with vetted short CC0 effects.
cp "$TMP/menu-move.ogg" "$UI/ui_click.ogg"
cp "$TMP/menu-move.ogg" "$UI/ui_switch_tab.ogg"
cp "$TMP/menu-move.ogg" "$UI/ui_open_panel.ogg"
cp "$TMP/menu-back.ogg" "$UI/ui_cancel.ogg"
cp "$TMP/menu-back.ogg" "$UI/ui_close_panel.ogg"
cp "$TMP/menu-confirm.ogg" "$UI/ui_confirm.ogg"
cp "$TMP/menu-confirm.ogg" "$UI/ui_select.ogg"
cp "$TMP/menu-confirm.ogg" "$UI/ui_unlock.ogg"
cp "$TMP/menu-confirm.ogg" "$UI/ui_warning.ogg"
cp "$TMP/menu-confirm.ogg" "$UI/ui_stamp_edict.ogg"
cp "$TMP/menu-move.ogg" "$UI/ui_brush_write.ogg"
cp "$TMP/menu-move.ogg" "$UI/ui_scroll_open.ogg"
cp "$TMP/menu-back.ogg" "$UI/ui_scroll_close.ogg"

# Try to improve the peaceful BGM with the CC0 OpenLoFi Asian & Zen collection.
# If GitHub Releases is temporarily unavailable, the verified instrumental fallbacks above remain valid.
OPENLOFI_ZIP="$TMP/openlofi.zip"
if curl -fL --retry 4 --retry-delay 2 \
  "https://github.com/btahir/open-lofi/releases/latest/download/openlofi.zip" \
  -o "$OPENLOFI_ZIP"; then
  mkdir -p "$TMP/openlofi"
  unzip -q "$OPENLOFI_ZIP" -d "$TMP/openlofi"

  convert_track() {
    local filename="$1"
    local output="$2"
    local src
    src="$(find "$TMP/openlofi" -type f -iname "$filename" -print -quit)"
    if [[ -n "$src" ]]; then
      ffmpeg -nostdin -hide_banner -loglevel error -y -i "$src" -vn -c:a libvorbis -q:a 5 "$output"
      echo "OpenLoFi: $filename -> $(basename "$output")"
    else
      echo "OpenLoFi track not found: $filename (keeping safe fallback)" >&2
    fi
  }

  convert_track "temple-at-dawn.mp3" "$BGM/bgm_main_menu.ogg"
  convert_track "bamboo-shadow-waltz.mp3" "$BGM/bgm_court.ogg"
  cp "$BGM/bgm_court.ogg" "$BGM/bgm_palace_hall.ogg"
  convert_track "lanterns-in-slow-motion.mp3" "$BGM/bgm_court_council.ogg"
  cp "$BGM/bgm_court_council.ogg" "$BGM/bgm_diplomacy.ogg"
  convert_track "moon-through-bamboo.mp3" "$BGM/bgm_map.ogg"
  convert_track "teacup-morning-fog.mp3" "$BGM/bgm_city.ogg"
  cp "$BGM/bgm_city.ogg" "$BGM/bgm_market.ogg"
  convert_track "bells-before-sunrise.mp3" "$BGM/bgm_ritual.ogg"
  convert_track "paper-lantern-rain.mp3" "$BGM/bgm_event_sad.ogg"
  convert_track "misty-steam-quiet-dreams.mp3" "$BGM/bgm_victory.ogg"
fi

# Hard invariant for this pass: no packaged voice files until the NPC voice system deliberately calls them.
# This prevents accidental prompt narration from ever being routed as ambient audio.
find "$VOICE" -type f -delete 2>/dev/null || true

# Sanity checks: BGM must be non-trivial audio; no tiny generated placeholders are allowed back in.
for f in "$BGM"/*.ogg; do
  [[ -s "$f" ]] || { echo "empty BGM: $f" >&2; exit 1; }
  bytes=$(wc -c < "$f")
  if (( bytes < 128000 )); then
    echo "BGM too small / suspicious: $f ($bytes bytes)" >&2
    exit 1
  fi
done

# Ensure the old tell-tale prompt words were not accidentally committed as text metadata.
if strings "$BGM"/*.ogg "$UI"/*.ogg 2>/dev/null | grep -Eiq '南渡无悔|古风|温柔典雅|界面切换|切换音乐|报数'; then
  echo "Suspicious prompt text detected in imported audio" >&2
  exit 1
fi

echo "Vetted audio sync complete. Voice channel is intentionally empty."
