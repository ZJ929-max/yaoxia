#!/usr/bin/env bash
#   tools/release.sh            # 构建当前版本
#   tools/release.sh 0.2.0      # 同时改 versionName（versionCode 锁死为 1，不再变动）
set -euo pipefail
cd "$(dirname "$0")/.."

PROPS=version.properties
CODE=$(sed -n 's/^versionCode=//p' "$PROPS")
NAME=$(sed -n 's/^versionName=//p' "$PROPS")

if [ "$CODE" != "1" ]; then
  echo "警告：versionCode 已不是 1（当前 $CODE），覆盖安装的保证失效" >&2
fi
[ $# -ge 1 ] && { NAME="$1"; sed -i "s/^versionName=.*/versionName=$NAME/" "$PROPS"; }

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-arm64}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
/opt/gradle-8.7/bin/gradle --no-daemon assembleRelease

OUT="OpenSandbox-${NAME}.apk"
cp app/build/outputs/apk/release/app-release.apk "../${OUT}"
echo "已输出：$(cd .. && pwd)/${OUT}  (versionName=${NAME}, versionCode=${CODE})"
