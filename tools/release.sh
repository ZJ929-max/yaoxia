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

# 不写死任何一台机器的 Gradle 路径：优先 wrapper，其次环境变量，最后常见位置
if [ -x ./gradlew ]; then
  GRADLE_CMD="./gradlew"
elif [ -n "${GRADLE_HOME:-}" ] && [ -x "$GRADLE_HOME/bin/gradle" ]; then
  GRADLE_CMD="$GRADLE_HOME/bin/gradle"
else
  GRADLE_CMD="$(command -v gradle || true)"
fi
[ -n "${GRADLE_CMD:-}" ] || { echo "找不到 gradle：请先 ./gradlew 或设置 GRADLE_HOME" >&2; exit 1; }
$GRADLE_CMD --no-daemon assembleRelease

OUT="OpenSandbox-${NAME}.apk"
mkdir -p ../builds
cp app/build/outputs/apk/release/app-release.apk "../builds/${OUT}"
echo "已输出：$(cd .. && pwd)/builds/${OUT}  (versionName=${NAME}, versionCode=${CODE})"
