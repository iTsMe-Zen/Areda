#!/usr/bin/env python3
"""Small manual i18n audit helper for Areada Kotlin/Compose files."""

from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
KOTLIN_ROOT = ROOT / "app" / "src" / "main" / "java"

STRING_RE = re.compile(r'"([^"\\]*(?:\\.[^"\\]*)*)"')
IGNORED = {
    "",
    " ",
    "\n",
    "content",
    "file",
    "application/pdf",
    "application/epub+zip",
    "text/plain",
    "application/zip",
    "application/x-zip-compressed",
}


def looks_user_facing(value: str) -> bool:
    if value in IGNORED:
        return False
    if "%" in value and len(value) <= 4:
        return False
    if value.startswith("#"):
        return False
    if value.startswith("http"):
        return False
    if "/" in value and " " not in value:
        return False
    if value.isupper() and len(value) <= 5:
        return False
    return any(ch.isalpha() for ch in value) and len(value.strip()) > 1


def main() -> int:
    for path in sorted(KOTLIN_ROOT.rglob("*.kt")):
        text = path.read_text(encoding="utf-8", errors="ignore")
        for line_number, line in enumerate(text.splitlines(), start=1):
            if "stringResource(" in line or "R.string." in line:
                continue
            for match in STRING_RE.finditer(line):
                value = match.group(1)
                if looks_user_facing(value):
                    rel = path.relative_to(ROOT)
                    print(f"{rel}:{line_number}: \"{value}\"")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
