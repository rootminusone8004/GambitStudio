#!/usr/bin/env python3
"""
Update the release version badge in README.md.

Usage: update_readme_badge.py <version>     (e.g. 1.3.2 or v1.3.2)

Rewrites the badge line
    ![release: X.Y.Z](https://img.shields.io/badge/release-X.Y.Z-<color>.svg)
to the given version, keeping its color. Exits non-zero if no badge is found,
so a renamed/removed badge is noticed instead of silently ignored.
"""
import re
import sys
from pathlib import Path

BADGE_PATTERN = re.compile(
    r'!\[release: [^\]]*\]\(https://img\.shields\.io/badge/release-(?:[^-)]|--)+-([A-Za-z0-9]+)\.svg\)'
)


def shields_escape(text: str) -> str:
    # In shields.io static badges, '-' and '_' must be doubled to appear literally
    return text.replace("-", "--").replace("_", "__")


def main() -> int:
    if len(sys.argv) != 2 or not sys.argv[1].strip():
        print("usage: update_readme_badge.py <version>", file=sys.stderr)
        return 2

    version = sys.argv[1].strip().lstrip("v")
    readme = Path(__file__).resolve().parents[2] / "README.md"
    content = readme.read_text(encoding="utf-8")

    def replace(match: re.Match) -> str:
        color = match.group(1)
        return f"![release: {version}](https://img.shields.io/badge/release-{shields_escape(version)}-{color}.svg)"

    updated, count = BADGE_PATTERN.subn(replace, content)
    if count == 0:
        print("::error::No release badge found in README.md", file=sys.stderr)
        return 1

    if updated == content:
        print(f"README badge already at {version}")
        return 0

    readme.write_text(updated, encoding="utf-8")
    print(f"README badge updated to {version}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
