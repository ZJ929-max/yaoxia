#!/usr/bin/env bash
# 版本策略：默认只涨"修订号"（第三位），中/大版本留给里程碑。
#   tools/release.sh            # 不改版本名，直接构建
#   tools/release.sh patch      # 0.5.2 → 0.5.3
#   tools/release.sh minor      # 0.5.3 → 0.6.0（里程碑才用）
#   tools/release.sh 0.7.0      # 指定版本名
set -euo pipefail
cd "$(dirname "$0")/.."

PROPS=version.properties
CODE=$(sed -n 's/^versionCode=//p' "$PROPS")
NAME=$(sed -n 's/^versionName=//p' "$PROPS")

case "${1:-}" in
  "") ;;
  patch)
    MAJOR=$(echo "$NAME" | cut -d. -f1)
    MINOR=$(echo "$NAME" | cut -d. -f2)
    PATCH=$(echo "$NAME" | cut -d. -f3)
    NAME="$MAJOR.$MINOR.$((PATCH + 1))"
    ;;
  minor)
    MAJOR=$(echo "$NAME" | cut -d. -f1)
    MINOR=$(echo "$NAME" | cut -d. -f2)
    NAME="$MAJOR.$((MINOR + 1)).0"
    ;;
  *)
    NAME="$1"
    ;;
esac

if [ "$CODE" != "1" ]; then
  echo "警告：versionCode 已不是 1（当前 $CODE），覆盖安装的保证失效" >&2
fi

sed -i "s/^versionName=.*/versionName=$NAME/" "$PROPS"

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-arm64}"
export ANDROID_HOME="${ANDROID_HOME:-/workspace/.toolchain/android-sdk}"
[ -x ./gradlew ] && GRADLE_CMD=./gradlew || GRADLE_CMD="${GRADLE_HOME:-/opt/gradle-9.8.2}/bin/gradle"
$GRADLE_CMD --no-daemon assembleRelease

OUT="OpenSandbox-${NAME}.apk"
mkdir -p ../builds
cp app/build/outputs/apk/release/app-release.apk "../builds/${OUT}"
echo "已输出：$(cd .. && pwd)/builds/${OUT}  (versionName=${NAME}, versionCode=${CODE})"
