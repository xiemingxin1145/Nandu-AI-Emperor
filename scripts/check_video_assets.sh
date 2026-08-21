#!/usr/bin/env bash
set -euo pipefail

if ! command -v ffprobe >/dev/null 2>&1; then
  echo "::error::ffprobe is required for video asset QA"
  exit 2
fi

roots=("assets/videos" "app/src/main/assets/videos")
files=()
for root in "${roots[@]}"; do
  if [[ -d "$root" ]]; then
    while IFS= read -r -d '' file; do
      files+=("$file")
    done < <(find "$root" -type f -iname '*.mp4' -print0 | sort -z)
  fi
done

if [[ ${#files[@]} -eq 0 ]]; then
  echo "::error::No MP4 video assets found"
  exit 1
fi

failures=0
checked=0

fail() {
  local file="$1"
  local reason="$2"
  echo "::error file=$file::$reason"
  failures=$((failures + 1))
}

for file in "${files[@]}"; do
  checked=$((checked + 1))

  if ! ffprobe -v error "$file" >/dev/null 2>&1; then
    fail "$file" "ffprobe cannot parse this MP4/container"
    continue
  fi

  codec=$(ffprobe -v error -select_streams v:0 -show_entries stream=codec_name -of csv=p=0 "$file" | head -1 | tr -d '\r')
  pix_fmt=$(ffprobe -v error -select_streams v:0 -show_entries stream=pix_fmt -of csv=p=0 "$file" | head -1 | tr -d '\r')
  width=$(ffprobe -v error -select_streams v:0 -show_entries stream=width -of csv=p=0 "$file" | head -1 | tr -d '\r')
  height=$(ffprobe -v error -select_streams v:0 -show_entries stream=height -of csv=p=0 "$file" | head -1 | tr -d '\r')
  duration=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$file" | head -1 | tr -d '\r')
  audio_codecs=$(ffprobe -v error -select_streams a -show_entries stream=codec_name -of csv=p=0 "$file" | tr -d '\r' | sort -u)

  if [[ -z "$codec" ]]; then
    fail "$file" "no video stream"
    continue
  fi

  [[ "$codec" == "h264" ]] || fail "$file" "video codec must be H.264/AVC, found: $codec"
  [[ "$pix_fmt" == "yuv420p" ]] || fail "$file" "pixel format must be yuv420p for broad Android hardware decode, found: ${pix_fmt:-unknown}"

  if [[ ! "$width" =~ ^[0-9]+$ || ! "$height" =~ ^[0-9]+$ || "$width" -le 0 || "$height" -le 0 ]]; then
    fail "$file" "invalid video dimensions: ${width:-?}x${height:-?}"
  else
    long_side=$(( width > height ? width : height ))
    short_side=$(( width > height ? height : width ))
    if (( long_side > 1920 || short_side > 1080 )); then
      fail "$file" "resolution exceeds V1.6.2 compatibility budget: ${width}x${height}"
    fi
  fi

  if ! awk -v d="$duration" 'BEGIN { exit !(d+0 > 0) }'; then
    fail "$file" "duration is missing or zero: ${duration:-unknown}"
  fi

  if [[ -n "$audio_codecs" ]]; then
    while IFS= read -r audio_codec; do
      [[ -z "$audio_codec" || "$audio_codec" == "aac" ]] || fail "$file" "embedded audio codec must be AAC if present, found: $audio_codec"
    done <<< "$audio_codecs"
  fi

done

echo "Video QA checked $checked MP4 assets."
if (( failures > 0 )); then
  echo "::error::$failures video compatibility problem(s) found"
  exit 1
fi

echo "All MP4 assets satisfy the V1.6.2 Android compatibility baseline (H.264/yuv420p, <=1080p-class, AAC-or-silent)."
