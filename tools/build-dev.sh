#!/usr/bin/env bash
# 开发期构建：不改版本号，产物固定覆盖 builds/OpenSandbox-dev.apk
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-arm64}"
export ANDROID_HOME="${ANDROID_HOME:-/workspace/.toolchain/android-sdk}"
./gradlew --no-daemon assembleRelease
mkdir -p ../builds
cp app/build/outputs/apk/release/app-release.apk ../builds/OpenSandbox-dev.apk
echo "已输出：$(cd .. && pwd)/builds/OpenSandbox-dev.apk（版本未变）"
