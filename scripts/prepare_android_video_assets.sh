#!/usr/bin/env bash
set -euo pipefail

for tool in ffprobe ffmpeg; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "::error::$tool is required to prepare Android video assets"
    exit 2
  fi
done

roots=("assets/videos" "app/src/main/assets/videos")
files=()
for root in "${roots[@]}"; do
  if [[ -d "$root" ]]; then
    while IFS= read -r -d '' file; do
      files+=("$file")
    done < <(find "$root" -type f -iname '*.mp4' -print0 | sort -z)
  fi
done

converted=0
kept=0

for file in "${files[@]}"; do
  codec=$(ffprobe -v error -select_streams v:0 -show_entries stream=codec_name -of csv=p=0 "$file" | head -1 | tr -d '\r')
  pix_fmt=$(ffprobe -v error -select_streams v:0 -show_entries stream=pix_fmt -of csv=p=0 "$file" | head -1 | tr -d '\r')

  if [[ "$codec" == "h264" && "$pix_fmt" == "yuv420p" ]]; then
    kept=$((kept + 1))
    continue
  fi

  echo "Transcoding for Android compatibility: $file ($codec/$pix_fmt -> h264/yuv420p, visual-only)"
  tmp="${file}.android-compat.tmp.mp4"
  rm -f "$tmp"

  ffmpeg -hide_banner -loglevel error -y \
    -i "$file" \
    -map 0:v:0 \
    -c:v libx264 \
    -preset veryfast \
    -crf 25 \
    -profile:v main \
    -level:v 4.0 \
    -pix_fmt yuv420p \
    -movflags +faststart \
    -an \
    "$tmp"

  mv "$tmp" "$file"
  converted=$((converted + 1))
done

echo "Android video preparation complete: converted=$converted kept=$kept total=${#files[@]}"
