#!/usr/bin/env python3
"""Resolve Minemon's pinned publication version or fall back to immutable tags.

A checked-in tools/minemon_release_version.txt pin takes precedence. Update that
file deliberately for the next release; this prevents commits and retry runs from
silently incrementing versions. Without a pin, the historical tag algorithm remains.
"""

from __future__ import annotations

import re
import subprocess
from decimal import Decimal
from pathlib import Path

TAG_PREFIX = "minemon-v"
BOOTSTRAP_LAST_RELEASE = Decimal("1.75")
VERSION_PATTERN = re.compile(r"^(\d+)\.(\d{2})$")


def git(*args: str) -> str:
    return subprocess.check_output(["git", *args], text=True).strip()


def parse_tag(tag: str) -> Decimal | None:
    if not tag.startswith(TAG_PREFIX):
        return None
    value = tag[len(TAG_PREFIX) :]
    if not VERSION_PATTERN.fullmatch(value):
        raise SystemExit(f"Malformed Minemon release tag: {tag}")
    return Decimal(value)


def resolve_version() -> str:
    pin_file = Path(__file__).with_name("minemon_release_version.txt")
    if pin_file.exists():
        pinned = pin_file.read_text(encoding="utf-8").strip()
        if not VERSION_PATTERN.fullmatch(pinned):
            raise SystemExit(f"Invalid pinned Minemon version: {pinned!r}")
        return pinned

    head = git("rev-parse", "HEAD")
    releases: list[tuple[str, Decimal, str]] = []

    for tag in git("tag", "--list", f"{TAG_PREFIX}*").splitlines():
        version = parse_tag(tag)
        if version is None:
            continue
        commit = git("rev-parse", f"{tag}^{{commit}}")
        releases.append((tag, version, commit))

    on_this_commit = [version for _, version, commit in releases if commit == head]
    if on_this_commit:
        return f"{max(on_this_commit):.2f}"

    latest = max((version for _, version, _ in releases), default=BOOTSTRAP_LAST_RELEASE)
    return f"{latest + Decimal('0.01'):.2f}"


if __name__ == "__main__":
    print(resolve_version())
