#!/usr/bin/env python3
"""架构体检：行数、重复声明、死 CompositionLocal、分层越界、TODO。
退出码非 0 表示有硬性违规（行数超限 / 分层越界）。"""
import re, sys, pathlib, collections

ROOT = pathlib.Path(__file__).resolve().parent.parent
SRC = list((ROOT/"app/src").rglob("*.kt")) + list((ROOT/"logic/src").rglob("*.kt"))
MAX_LINES, SOFT_LINES = 280, 200

LAYERS = {"core", "platform", "delegation", "channels", "apps", "provision", "ui", "di"}
ALLOWED = {  # 谁可以依赖谁（只列允许的反向边）
    "core": set(),
    "platform": {"core"},
    "delegation": {"core", "channels", "platform"},
    "channels": {"core", "platform", "delegation"},
    "apps": {"core", "channels", "platform"},
    "provision": {"core", "channels", "platform"},
    "ui": {"core", "platform", "delegation", "channels", "apps", "provision", "di"},
    "di": {"core", "platform", "delegation", "channels", "apps", "provision", "ui"},  # 组合根
}

def layer_of(p: pathlib.Path):
    for part in p.parts:
        if part in LAYERS: return part
    return None

hard, soft = [], []
decl = collections.defaultdict(list)

for f in SRC:
    text = f.read_text(encoding="utf-8", errors="ignore")
    lines = text.count("\n") + 1
    rel = f.relative_to(ROOT)
    if lines > MAX_LINES: hard.append(f"{rel}: {lines} 行（上限 {MAX_LINES}）")
    elif lines > SOFT_LINES: soft.append(f"{rel}: {lines} 行（建议 ≤{SOFT_LINES}）")

    me = layer_of(f)
    for m in re.finditer(r"^import com\.opensandbox\.dhizuku\.(\w+)", text, re.M):
        dep = m.group(1)
        if me and dep in LAYERS and dep != me and dep not in ALLOWED[me]:
            hard.append(f"{rel}: 分层越界 {me} → {dep}")

    # 名字带上接收者：`fun Modifier.foo` 和 `fun Modifier.bar` 是两个不同的声明，
    # 早期只取 \w+ 会都记成 "Modifier"，产生假的重名告警
    for m in re.finditer(r"^(?:internal |private )?(?:val|fun|class|object) ((?:\w+\.)*\w+)", text, re.M):
        decl[m.group(1)].append(str(rel))

    for kw in ("TODO(", "FIXME(", "HACK("):
        if kw in text: soft.append(f"{rel}: 含 {kw}")

dups = {k: v for k, v in decl.items() if len(v) > 1 and not k.startswith("_")}
for k, v in dups.items():
    hard.append(f"重复顶层声明 {k}: {', '.join(v)}")

# 死局部：定义了但只在定义处出现一次
for f in SRC:
    text = f.read_text(encoding="utf-8", errors="ignore")
    for m in re.finditer(r"(?:internal |private )?val (Local\w+) = staticCompositionLocalOf", text):
        name = m.group(1)
        uses = sum(1 for g in SRC if name in g.read_text(encoding="utf-8", errors="ignore"))
        if uses <= 1: soft.append(f"{f.name}: {name} 仅 {uses} 处引用（疑似死代码）")

print(f"扫描 {len(SRC)} 个文件")
if hard:
    print("\n❌ 硬性违规：")
    for h in hard: print("  -", h)
if soft:
    print("\n⚠️ 建议：")
    for s in soft: print("  -", s)
if not hard and not soft:
    print("\n✅ 无问题")
sys.exit(1 if hard else 0)
