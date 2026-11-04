#!/usr/bin/env python3
from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

try:
    import yaml
except ImportError as exc:  # pragma: no cover
    raise SystemExit("PyYAML is required to run this validator") from exc


def _repo_root_five_directories_above_this_script() -> Path:
    tools_directory = Path(__file__).resolve().parent
    return tools_directory.parents[3]


REPO_ROOT = _repo_root_five_directories_above_this_script()

_CANONICAL_TOP_LEVEL_KEY_ORDER = [
    "pipeline",
    "version",
    "what_it_does",
    "components",
    "files",
    "setup",
    "pipeline_steps",
    "start",
    "usage",
    "rejected_alternatives",
    "troubleshooting",
]

_DETECTION_KEY_SLOT_RIGHT_AFTER_PIPELINE_STEPS = _CANONICAL_TOP_LEVEL_KEY_ORDER.index("pipeline_steps") + 1

_CANONICAL_KEY_POSITION: dict[str, int] = {
    key: idx for idx, key in enumerate(_CANONICAL_TOP_LEVEL_KEY_ORDER)
}

REQUIRED_TOP_LEVEL_KEYS = {"pipeline", "what_it_does", "usage", "files"}
FORBIDDEN_TOP_LEVEL_TRIGGER_KEYS = {"hotkey", "command"}
_ALLOWED_TOP_LEVEL_KEYS = set(_CANONICAL_TOP_LEVEL_KEY_ORDER)
MAX_LINE_CHARS = 120


def _is_a_detection_key(key: str) -> bool:
    return key.endswith("_detection")


def _canonical_position_of_key(key: str) -> int | None:
    if _is_a_detection_key(key):
        return _DETECTION_KEY_SLOT_RIGHT_AFTER_PIPELINE_STEPS
    return _CANONICAL_KEY_POSITION.get(key)


def _check_line_length_cap(text: str) -> list[str]:
    return [
        f"line {i}: {len(line)} chars (max {MAX_LINE_CHARS})"
        for i, line in enumerate(text.splitlines(), start=1)
        if len(line) > MAX_LINE_CHARS
    ]


def _discover_readmes_via_git_ls_files(root: Path) -> list[Path]:
    try:
        result = subprocess.run(
            ["git", "ls-files", "--cached", "--others", "--exclude-standard", "*readme.yaml"],
            cwd=root,
            capture_output=True,
            text=True,
            check=True,
        )
    except (subprocess.CalledProcessError, FileNotFoundError):
        return []
    tracked_paths = [line.strip() for line in result.stdout.splitlines() if line.strip()]
    return sorted(root / p for p in tracked_paths)


def _discover_readmes_via_filesystem_walk(root: Path) -> list[Path]:
    return sorted(p for p in root.rglob("*readme.yaml") if ".git" not in p.parts)


def discover_readmes(repo_root: Path | None = None) -> list[Path]:
    root = (repo_root or REPO_ROOT).resolve()
    return _discover_readmes_via_git_ls_files(root) or _discover_readmes_via_filesystem_walk(root)


def _check_first_top_level_key_is_pipeline(keys: list[str]) -> list[str]:
    if keys[0] == "pipeline":
        return []
    return [f"first top-level key must be 'pipeline', got '{keys[0]}'"]


def _check_required_top_level_keys_are_present(keys: list[str]) -> list[str]:
    missing_required_keys = sorted(REQUIRED_TOP_LEVEL_KEYS - set(keys))
    return [f"missing required key: {key}" for key in missing_required_keys]


def _check_no_forbidden_trigger_keys(keys: list[str]) -> list[str]:
    return [
        f"top-level key '{key}' is forbidden (RM03) — move invocation info into usage"
        for key in keys
        if key in FORBIDDEN_TOP_LEVEL_TRIGGER_KEYS
    ]


def _check_no_unknown_top_level_keys(keys: list[str]) -> list[str]:
    errors: list[str] = []
    for key in keys:
        if key in _ALLOWED_TOP_LEVEL_KEYS or _is_a_detection_key(key) or key in FORBIDDEN_TOP_LEVEL_TRIGGER_KEYS:
            continue
        errors.append(f"unknown top-level key: '{key}'")
    return errors


def _check_present_keys_follow_canonical_order(keys: list[str]) -> list[str]:
    errors: list[str] = []
    present_keys_in_canonical_order: list[tuple[str, int]] = []
    for key in keys:
        pos = _canonical_position_of_key(key)
        if pos is not None:
            present_keys_in_canonical_order.append((key, pos))

    for i in range(1, len(present_keys_in_canonical_order)):
        prev_key, prev_pos = present_keys_in_canonical_order[i - 1]
        cur_key, cur_pos = present_keys_in_canonical_order[i]
        if cur_pos < prev_pos:
            errors.append(
                f"out-of-order key '{cur_key}' (expected after '{prev_key}' per canonical order)"
            )
    return errors


def validate_readme(path: Path) -> list[str]:
    try:
        text = path.read_text(encoding="utf-8")
    except OSError as exc:
        return [f"cannot read file: {exc}"]

    try:
        data = yaml.safe_load(text)
    except yaml.YAMLError as exc:
        return [f"YAML parse error: {exc}"]

    if not isinstance(data, dict):
        return ["top-level value must be a YAML mapping"]

    keys = list(data.keys())
    if not keys:
        return ["file contains no top-level keys"]

    errors: list[str] = []
    errors.extend(_check_first_top_level_key_is_pipeline(keys))
    errors.extend(_check_required_top_level_keys_are_present(keys))
    errors.extend(_check_no_forbidden_trigger_keys(keys))
    errors.extend(_check_no_unknown_top_level_keys(keys))
    errors.extend(_check_present_keys_follow_canonical_order(keys))
    errors.extend(_check_line_length_cap(text))
    return errors


def _path_relative_to_repo_root(path: Path, root: Path) -> Path:
    try:
        return path.resolve().relative_to(root.resolve())
    except ValueError:
        return path


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Validate readme.yaml files against the docs/readme standard."
    )
    parser.add_argument(
        "paths",
        nargs="*",
        type=Path,
        help="specific readme.yaml files to validate (default: discover all in repo)",
    )
    parser.add_argument(
        "--repo-root",
        type=Path,
        default=REPO_ROOT,
        help="repository root used for discovery and relative path display",
    )
    args = parser.parse_args(argv)

    paths = sorted(set(args.paths)) if args.paths else discover_readmes(args.repo_root)
    if not paths:
        print("no readme.yaml files found", file=sys.stderr)
        return 1

    all_ok = True
    for path in paths:
        errors = validate_readme(path)
        if errors:
            all_ok = False
            relative_path = _path_relative_to_repo_root(path, args.repo_root)
            for error in errors:
                print(f"{relative_path}: {error}", file=sys.stderr)

    if all_ok:
        print(f"ok: {len(paths)} readme.yaml file(s) validated")
        return 0
    return 1


if __name__ == "__main__":
    sys.exit(main())
