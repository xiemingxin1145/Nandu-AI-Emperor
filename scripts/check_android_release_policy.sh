#!/usr/bin/env bash
set -euo pipefail

gradle_file="app/build.gradle.kts"
key_file="tools/signing/nandu-dev-debug.keystore"
branch="${GITHUB_HEAD_REF:-${GITHUB_REF_NAME:-local}}"

current_code=$(sed -nE 's/.*versionCode[[:space:]]*=[[:space:]]*([0-9]+).*/\1/p' "$gradle_file" | head -1)
current_name=$(sed -nE 's/.*versionName[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' "$gradle_file" | head -1)

if [[ -z "$current_code" || -z "$current_name" ]]; then
  echo "::error::无法读取 Android versionCode/versionName；发布前必须明确版本。"
  exit 1
fi

if [[ ! -f "$key_file" ]]; then
  echo "::error::稳定开发签名缺失：$key_file。不要让 CI 临时生成新签名。"
  exit 1
fi

if ! grep -q 'tools/signing/nandu-dev-debug.keystore' "$gradle_file"; then
  echo "::error::Gradle 未绑定稳定开发签名。升级 APK 必须保持同一 debug 签名。"
  exit 1
fi

if [[ "$branch" == release/* ]]; then
  git fetch --no-tags --depth=1 origin main >/dev/null 2>&1 || true
  if git show origin/main:"$gradle_file" >/tmp/nandu_main_gradle 2>/dev/null; then
    base_code=$(sed -nE 's/.*versionCode[[:space:]]*=[[:space:]]*([0-9]+).*/\1/p' /tmp/nandu_main_gradle | head -1)
    if [[ -n "$base_code" ]] && (( current_code <= base_code )); then
      echo "::error::release 分支 versionCode=$current_code 没有高于 main=$base_code。先升级版本号再构建。"
      exit 1
    fi
  fi
fi

echo "Release policy OK: branch=$branch version=$current_name code=$current_code stable-debug-signing=present"
