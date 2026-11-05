#!/usr/bin/env bash
# 开发构建：只递增 -dev.N，前三位不动
set -euo pipefail
cd "$(dirname "$0")/.."

PROPS=version.properties
NAME=$(sed -n 's/^versionName=//p' "$PROPS")

BASE=${NAME%%-dev.*}
DEV=$(echo "$NAME" | sed -n 's/.*-dev\.\([0-9]*\)$/\1/p')
[ -z "$DEV" ] && DEV=0
NAME="${BASE}-dev.$((DEV + 1))"
sed -i "s/^versionName=.*/versionName=$NAME/" "$PROPS"

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-arm64}"
export ANDROID_HOME="${ANDROID_HOME:-/workspace/.toolchain/android-sdk}"
./gradlew --no-daemon assembleRelease

mkdir -p ../builds
cp app/build/outputs/apk/release/app-release.apk "../builds/Yaohu-${NAME}.apk"
cp app/build/outputs/apk/release/app-release.apk ../builds/Yaohu-dev.apk
echo "已输出：builds/Yaohu-${NAME}.apk（versionCode 恒为 1）"
