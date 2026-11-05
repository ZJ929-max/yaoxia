#!/usr/bin/env python3
"""防'启动即崩'：清单里声明的组件类必须真的在 dex 里。"""
import re, subprocess, sys, zipfile

apk = sys.argv[1] if len(sys.argv) > 1 else "app/build/outputs/apk/release/app-release.apk"
aapt2 = "/workspace/.toolchain/android-sdk/build-tools/36.0.0/aapt2"

tree = subprocess.run([aapt2, "dump", "xmltree", apk, "--file", "AndroidManifest.xml"],
                      capture_output=True, text=True).stdout

pkg = re.search(r"package: name='([^']+)'",
                subprocess.run([aapt2, "dump", "badging", apk], capture_output=True, text=True).stdout
                ).group(1)

with zipfile.ZipFile(apk) as z:
    dex = z.read("classes.dex")

components, current = [], None
for line in tree.splitlines():
    m = re.match(r"\s*E: (\w+)", line)
    if m:
        current = m.group(1)
        continue
    if current in {"activity", "service", "receiver", "provider", "application"}:
        m = re.search(r':name\(0x[0-9a-f]+\)="([^"]+)"', line)
        if m:
            name = m.group(1)
            if name.startswith("."):
                name = pkg + name
            elif "." not in name:
                name = pkg + "." + name
            components.append(name)

missing = [c for c in components if (c.replace(".", "/").encode() not in dex)]
for c in components:
    print(("✓ " if c not in missing else "✗ ") + c)
sys.exit(1 if missing else 0)
