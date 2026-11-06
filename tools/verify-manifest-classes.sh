#!/usr/bin/env bash
# 防这类"启动即崩"：清单里声明的组件类，必须真的在 dex 里
set -euo pipefail
APK=${1:-app/build/outputs/apk/release/app-release.apk}
AAPT2=${AAPT2:-/workspace/.toolchain/android-sdk/build-tools/36.0.0/aapt2}
PKG=$("$AAPT2" dump badging "$APK" | sed -n "s/^package: name='\([^']*\)'.*/\1/p")

fail=0
"$AAPT2" dump xmltree "$APK" --file AndroidManifest.xml \
  | sed -n 's/.*android:name(0x[0-9a-f]*)="\([^"]*\)".*/\1/p' \
  | grep -E '\.' | sort -u | while read -r name; do
      case "$name" in
        *.*.*) cls="${name//./\/}" ;;
        .*)    cls="${PKG//./\/}${name//./\/}" ;;
        *)     cls="${PKG//./\/}/${name//./\/}" ;;
      esac
      count=$(unzip -p "$APK" classes.dex | grep -ac "$cls" || true)
      if [ "$count" -eq 0 ]; then
        echo "✗ 清单声明但 dex 里没有：$name"
        fail=1
      fi
    done
[ "$fail" -eq 0 ] && echo "✓ 清单组件全部存在"
