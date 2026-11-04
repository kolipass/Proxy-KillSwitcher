#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

try:
    import yaml
except ImportError as exc:  # pragma: no cover
    raise SystemExit(
        "PyYAML is required to run this validator (python3 -c 'import yaml')"
    ) from exc


def _repo_root_five_directories_above_this_script() -> Path:
    tools_directory = Path(__file__).resolve().parent
    return tools_directory.parents[3]


REPO_ROOT = _repo_root_five_directories_above_this_script()

_CANONICAL_TOP_LEVEL_KEY_ORDER = [
    "pipeline",
    "status",
    "summary",
    "context",
    "decision",
    "consequences",
    "rules",
    "flow",
    "components",
    "paths",
    "setup",
    "implementation_deltas",
    "discovered_issues",
    "rejected_alternatives",
    "history",
]
_CANONICAL_KEY_POSITION: dict[str, int] = {
    key: idx for idx, key in enumerate(_CANONICAL_TOP_LEVEL_KEY_ORDER)
}

REQUIRED_TOP_LEVEL_KEYS = {"pipeline", "status", "summary", "context", "decision", "rules", "history"}
ALLOWED_TOP_LEVEL_KEYS = set(_CANONICAL_TOP_LEVEL_KEY_ORDER)
ALLOWED_STATUS_VALUES = {"proposed", "accepted", "deprecated", "superseded"}
REQUIRED_SUMMARY_KEYS = ("problem", "solution", "tradeoff")

KEY_MAPPING_SECTIONS = {"rules", "rejected_alternatives", "implementation_deltas"}
LIST_SECTIONS = {"flow", "paths", "setup"}
SINGLE_MAPPING_SECTIONS = {"components"}

SUMMARY_FIELD_CHAR_CAP = 300
DECISION_CONTEXT_CHAR_CAP_WITH_BUFFER = 1800
IMPLEMENTATION_DELTA_AND_DISCOVERED_ISSUE_CHAR_CAP = 600
HISTORY_ENTRY_CHAR_CAP = 200
DEFAULT_FIELD_CHAR_CAP = 300

MAX_LINE_CHARS = 120
MAX_FILE_LINES = 250

MAX_SECTION_KEY_WORDS_WITH_BUFFER = 4

_SNAKE_CASE_KEY = re.compile(r"^[a-z][a-z0-9]*(_[a-z0-9]+)*$")
_HISTORY_VERSION_KEY = re.compile(r"^v(\d+)$")
_PLAN_TASK_ID = re.compile(r"\bS-\d+\b")
_RULE_ID_LETTERS_DIRECTLY_FOLLOWED_BY_DIGITS = re.compile(r"\b[A-Z]{1,4}\d{2,4}\b")
_KEY_LINE = re.compile(r"^(\s*)([A-Za-z_][A-Za-z0-9_]*):\s*(.*)$")
_LIST_ITEM_LINE = re.compile(r"^(\s*)-\s*(.*)$")
_PRD_FILENAME_THREE_SNAKE_CASE_WORDS = re.compile(r"^[a-z][a-z0-9]*(_[a-z0-9]+){2}\.yaml$")
_PLAN_FILENAME = re.compile(r"^plan_[a-z][a-z0-9]*_[a-z][a-z0-9]*\.yaml$")


def _char_cap_for_top_level_key(key: str) -> int:
    if key == "summary":
        return SUMMARY_FIELD_CHAR_CAP
    if key in ("decision", "context"):
        return DECISION_CONTEXT_CHAR_CAP_WITH_BUFFER
    if key in ("implementation_deltas", "discovered_issues"):
        return IMPLEMENTATION_DELTA_AND_DISCOVERED_ISSUE_CHAR_CAP
    if key == "history":
        return HISTORY_ENTRY_CHAR_CAP
    return DEFAULT_FIELD_CHAR_CAP


def _length_with_whitespace_runs_collapsed(value: str) -> int:
    return len(" ".join(value.split()))


def _is_none_blank_string_or_empty_collection(value: object) -> bool:
    if value is None:
        return True
    if isinstance(value, str) and value.strip() == "":
        return True
    if isinstance(value, (list, dict)) and len(value) == 0:
        return True
    return False


def _walk_string_leaves(value: object, prefix: str = ""):
    if isinstance(value, dict):
        for key, child in value.items():
            path = f"{prefix}.{key}" if prefix else str(key)
            yield from _walk_string_leaves(child, path)
    elif isinstance(value, list):
        for idx, child in enumerate(value):
            yield from _walk_string_leaves(child, f"{prefix}[{idx}]")
    elif isinstance(value, str):
        yield prefix, value


def _find_fields_over_char_cap(value: object, cap: int, prefix: str = "") -> list[tuple[str, int, int]]:
    results: list[tuple[str, int, int]] = []
    for path, leaf in _walk_string_leaves(value, prefix):
        length = _length_with_whitespace_runs_collapsed(leaf)
        if length > cap:
            results.append((path, length, cap))
    return results


def _find_fields_citing_a_rule_or_task_id(value: object, prefix: str = "") -> list[str]:
    return [
        path
        for path, leaf in _walk_string_leaves(value, prefix)
        if _PLAN_TASK_ID.search(leaf) or _RULE_ID_LETTERS_DIRECTLY_FOLLOWED_BY_DIGITS.search(leaf)
    ]


def _check_filename_is_three_snake_case_words(path: Path) -> list[str]:
    if _PRD_FILENAME_THREE_SNAKE_CASE_WORDS.match(path.name):
        return []
    return [
        f"file name '{path.name}' must be exactly three snake_case words,"
        " e.g. refactoring_document_style.yaml (PD27)"
    ]


def _check_first_top_level_key_is_pipeline(keys: list[str]) -> list[str]:
    if keys[0] == "pipeline":
        return []
    return [f"first top-level key must be 'pipeline', got '{keys[0]}'"]


def _check_required_top_level_keys_are_present(keys: list[str]) -> list[str]:
    missing_required_keys = sorted(REQUIRED_TOP_LEVEL_KEYS - set(keys))
    return [f"missing required key: {key}" for key in missing_required_keys]


def _check_no_top_level_key_has_an_empty_value(data: dict, keys: list[str]) -> list[str]:
    return [
        f"key '{key}' has an empty value (PD21 — omit instead)"
        for key in keys
        if _is_none_blank_string_or_empty_collection(data[key])
    ]


def _check_every_field_stays_within_its_char_cap(data: dict, keys: list[str]) -> list[str]:
    errors: list[str] = []
    for key in keys:
        cap = _char_cap_for_top_level_key(key)
        for field_path, length, field_cap in _find_fields_over_char_cap(data[key], cap, prefix=key):
            errors.append(f"field '{field_path}' is {length} chars (max {field_cap}) (PD23)")
    return errors


def _check_status_is_a_valid_enum_value(status: object) -> list[str]:
    if status is None or status in ALLOWED_STATUS_VALUES:
        return []
    return [f"status must be one of {sorted(ALLOWED_STATUS_VALUES)!r}, got {status!r}"]


def _check_no_unknown_top_level_keys(keys: list[str]) -> list[str]:
    return [f"unknown top-level key: '{key}'" for key in keys if key not in ALLOWED_TOP_LEVEL_KEYS]


def _check_present_keys_follow_canonical_order(keys: list[str]) -> list[str]:
    errors: list[str] = []
    present_keys_in_canonical_order = [
        (key, _CANONICAL_KEY_POSITION[key]) for key in keys if key in _CANONICAL_KEY_POSITION
    ]
    for i in range(1, len(present_keys_in_canonical_order)):
        prev_key, prev_pos = present_keys_in_canonical_order[i - 1]
        cur_key, cur_pos = present_keys_in_canonical_order[i]
        if cur_pos < prev_pos:
            errors.append(
                f"out-of-order key '{cur_key}' (expected after '{prev_key}' per canonical order)"
            )
    return errors


def _check_summary_has_exactly_problem_solution_tradeoff(data: dict) -> list[str]:
    errors: list[str] = []
    summary = data.get("summary")
    if summary is None:
        return errors
    if not isinstance(summary, dict):
        return ["'summary' must be a mapping of problem/solution/tradeoff (PD04)"]
    present_keys = [str(k) for k in summary]
    for key in REQUIRED_SUMMARY_KEYS:
        if key not in present_keys:
            errors.append(f"summary is missing required key: {key} (PD04)")
    for key in present_keys:
        if key not in REQUIRED_SUMMARY_KEYS:
            errors.append(f"summary has unknown key: '{key}' (PD04)")
    for key in REQUIRED_SUMMARY_KEYS:
        value = summary.get(key)
        if key in present_keys and not isinstance(value, str):
            errors.append(f"summary.{key} must be a string (PD04)")
    return errors


def _check_consequences_has_nonempty_positive_and_negative(data: dict) -> list[str]:
    errors: list[str] = []
    consequences = data.get("consequences")
    if consequences is None:
        return errors
    if not isinstance(consequences, dict):
        return ["'consequences' must be a mapping with positive and negative (PD09)"]
    for sub_key in ("positive", "negative"):
        value = consequences.get(sub_key)
        if not isinstance(value, dict) or not value:
            errors.append(f"consequences.{sub_key} must be a non-empty mapping (PD09)")
    for key in consequences:
        if key not in ("positive", "negative"):
            errors.append(f"consequences has unknown key: '{key}' (PD09)")
    return errors


def _check_discovered_issues_entries_have_status_and_problem(data: dict) -> list[str]:
    errors: list[str] = []
    discovered_issues = data.get("discovered_issues")
    if discovered_issues is None:
        return errors
    if not isinstance(discovered_issues, dict):
        return ["'discovered_issues' must be a mapping of named issues (PD16)"]
    for issue_name, issue in discovered_issues.items():
        if not isinstance(issue, dict):
            errors.append(
                f"discovered_issues.{issue_name} must be a mapping with status and problem (PD16)"
            )
            continue
        for required_key in ("status", "problem"):
            value = issue.get(required_key)
            if not isinstance(value, str) or not value.strip():
                errors.append(
                    f"discovered_issues.{issue_name} is missing required key: {required_key} (PD16)"
                )
    return errors


def _check_list_and_mapping_sections_have_the_right_type(data: dict) -> list[str]:
    errors: list[str] = []
    for section in LIST_SECTIONS:
        if section in data and not isinstance(data[section], list):
            errors.append(f"'{section}' must be a list (PD11/PD13/PD14)")
    for section in SINGLE_MAPPING_SECTIONS:
        if section in data and not isinstance(data[section], dict):
            errors.append(f"'{section}' must be a mapping (PD12)")
    for section in KEY_MAPPING_SECTIONS:
        if section in data and not isinstance(data[section], dict):
            errors.append(f"'{section}' must be a mapping (PD10)")
    return errors


def _find_key_naming_violations(section_label: str, mapping: dict) -> list[str]:
    errors: list[str] = []
    for key in mapping:
        key = str(key)
        if not _SNAKE_CASE_KEY.match(key):
            errors.append(f"{section_label}.{key}: key must be English lowercase snake_case (PD10)")
            continue
        words = key.split("_")
        if len(words) > MAX_SECTION_KEY_WORDS_WITH_BUFFER:
            errors.append(
                f"{section_label}.{key}: key has {len(words)} words,"
                f" max {MAX_SECTION_KEY_WORDS_WITH_BUFFER} (PD10)"
            )
    return errors


def _check_all_section_mapping_keys_are_well_formed(data: dict) -> list[str]:
    errors: list[str] = []
    for section in KEY_MAPPING_SECTIONS:
        value = data.get(section)
        if isinstance(value, dict):
            errors.extend(_find_key_naming_violations(section, value))
    consequences = data.get("consequences")
    if isinstance(consequences, dict):
        for sub_key in ("positive", "negative"):
            value = consequences.get(sub_key)
            if isinstance(value, dict):
                errors.extend(_find_key_naming_violations(f"consequences.{sub_key}", value))
    discovered_issues = data.get("discovered_issues")
    if isinstance(discovered_issues, dict):
        errors.extend(_find_key_naming_violations("discovered_issues", discovered_issues))
        for issue_name, issue in discovered_issues.items():
            if isinstance(issue, dict):
                errors.extend(_find_key_naming_violations(f"discovered_issues.{issue_name}", issue))
    return errors


def _check_history_keys_are_sequential_versions(data: dict) -> list[str]:
    errors: list[str] = []
    history = data.get("history")
    if history is None:
        return errors
    if not isinstance(history, dict):
        errors.append("'history' must be a mapping of v1, v2, ... to description strings (PD18)")
        return errors
    previous_version_number = 0
    for key in history:
        key = str(key)
        match = _HISTORY_VERSION_KEY.match(key)
        if not match:
            errors.append(f"history.{key}: key must look like v1, v2, ... (PD18)")
            continue
        version_number = int(match.group(1))
        if version_number <= previous_version_number:
            errors.append(f"history.{key}: version keys must strictly increase in order (PD18)")
        previous_version_number = version_number
    return errors


def _check_no_field_cites_a_rule_or_task_id(data: dict) -> list[str]:
    return [
        f"field '{field_path}' cites a rule or task id, e.g. 'S-5' or 'PD23' (PD26)"
        for field_path in _find_fields_citing_a_rule_or_task_id(data)
    ]


def _is_quoted_scalar_closed_on_this_line(value_after_colon: str) -> bool:
    return (
        len(value_after_colon) >= 2
        and value_after_colon.startswith('"')
        and value_after_colon.endswith('"')
        and not value_after_colon.endswith('\\"')
    )


def _next_non_blank_non_comment_line(lines: list[str], start: int) -> str:
    for line in lines[start:]:
        stripped = line.strip()
        if stripped and not stripped.startswith("#"):
            return stripped
    return ""


def _indent_of_next_content_line(lines: list[str], start: int) -> int | None:
    for line in lines[start:]:
        if line.strip() and not line.strip().startswith("#"):
            return len(line) - len(line.lstrip(" "))
    return None


def _check_scalar_quoting_and_block_style(text: str) -> list[str]:
    errors: list[str] = []
    lines = text.splitlines()
    current_top_level_key = ""
    open_block_indent: int | None = None
    inside_a_wrapped_quoted_scalar = False
    for idx, line in enumerate(lines):
        lineno = idx + 1
        stripped = line.strip()
        indent = len(line) - len(line.lstrip(" "))
        if inside_a_wrapped_quoted_scalar:
            if stripped.endswith('"') and not stripped.endswith('\\"'):
                inside_a_wrapped_quoted_scalar = False
            continue
        if open_block_indent is not None:
            if not stripped or indent > open_block_indent:
                continue
            open_block_indent = None
        if not stripped or stripped.startswith("#"):
            continue
        key_match = _KEY_LINE.match(line)
        list_item_match = None if key_match else _LIST_ITEM_LINE.match(line)
        if key_match:
            key, value_after_colon = key_match.group(2), key_match.group(3).strip()
            if indent == 0:
                current_top_level_key = key
                if key == "status":
                    continue
            if current_top_level_key == "summary" and indent > 0:
                if value_after_colon.startswith((">", "|")):
                    errors.append(
                        f"line {lineno}: summary.{key} must be a double-quoted string,"
                        " not a block (PD05)"
                    )
                    open_block_indent = indent
                elif not value_after_colon.startswith('"'):
                    errors.append(
                        f"line {lineno}: summary.{key} must be a double-quoted string (PD05)"
                    )
                elif not _is_quoted_scalar_closed_on_this_line(value_after_colon):
                    inside_a_wrapped_quoted_scalar = True
                continue
            if value_after_colon == "" or value_after_colon.startswith("#"):
                next_line = _next_non_blank_non_comment_line(lines, lineno)
                next_line_opens_a_nested_structure = bool(
                    _KEY_LINE.match(next_line) or _LIST_ITEM_LINE.match(next_line)
                )
                if (
                    next_line
                    and not next_line.startswith((">", "|"))
                    and not next_line_opens_a_nested_structure
                ):
                    errors.append(
                        f"line {lineno}: '{key}' value must not be pushed to the next line (PD06)"
                    )
            elif value_after_colon.startswith("|"):
                errors.append(
                    f"line {lineno}: '{key}' uses '|' literal block — use '>' folded block (PD06)"
                )
                open_block_indent = indent
            elif value_after_colon.startswith(">"):
                open_block_indent = indent
            elif value_after_colon.startswith('"'):
                if not _is_quoted_scalar_closed_on_this_line(value_after_colon):
                    errors.append(
                        f"line {lineno}: '{key}' multiline quoted scalar — use '>' folded block (PD06)"
                    )
                    inside_a_wrapped_quoted_scalar = True
                else:
                    errors.append(
                        f"line {lineno}: '{key}' is quoted — quotes are summary-only, write it"
                        " bare or as a '>' block (PD06)"
                    )
            else:
                spillover_indent = _indent_of_next_content_line(lines, lineno)
                if spillover_indent is not None and spillover_indent > indent:
                    errors.append(
                        f"line {lineno}: '{key}' bare value spills onto the next line —"
                        " use a '>' folded block (PD06)"
                    )
                    open_block_indent = indent
        elif list_item_match:
            value_after_dash = list_item_match.group(2).strip()
            if value_after_dash.startswith("|"):
                errors.append(
                    f"line {lineno}: list item uses '|' literal block — use '- >' folded block (PD06)"
                )
                open_block_indent = indent
            elif value_after_dash.startswith(">"):
                open_block_indent = indent
            elif value_after_dash.startswith('"'):
                if not _is_quoted_scalar_closed_on_this_line(value_after_dash):
                    errors.append(
                        f"line {lineno}: list item multiline quoted scalar — use '- >' block (PD06)"
                    )
                    inside_a_wrapped_quoted_scalar = True
                else:
                    errors.append(
                        f"line {lineno}: list item is quoted — quotes are summary-only, write it"
                        " bare or as a '- >' block (PD06)"
                    )
            elif value_after_dash:
                spillover_indent = _indent_of_next_content_line(lines, lineno)
                if spillover_indent is not None and spillover_indent > indent:
                    errors.append(
                        f"line {lineno}: list item spills onto the next line —"
                        " use a '- >' folded block (PD06)"
                    )
                    open_block_indent = indent
    return errors


def _check_line_and_file_length_caps(text: str) -> list[str]:
    lines = text.splitlines()
    errors = [
        f"line {i}: {len(line)} chars (max {MAX_LINE_CHARS}) (PD24)"
        for i, line in enumerate(lines, start=1)
        if len(line) > MAX_LINE_CHARS
    ]
    if len(lines) > MAX_FILE_LINES:
        errors.append(f"file has {len(lines)} lines (max {MAX_FILE_LINES}) (PD24)")
    return errors


def _is_an_implementation_plan_not_a_prd(path: Path) -> bool:
    if not _PLAN_FILENAME.match(path.name):
        return False
    try:
        top_level_document = yaml.safe_load(path.read_text())
    except (yaml.YAMLError, OSError):
        return False
    return (
        isinstance(top_level_document, dict)
        and "prd" in top_level_document
        and "pipeline" not in top_level_document
    )


def _discover_prds_via_git_ls_files(root: Path) -> list[Path]:
    try:
        result = subprocess.run(
            [
                "git", "ls-files", "--cached", "--others", "--exclude-standard",
                "*prd.yaml", ".claude/prd/*.yaml",
            ],
            cwd=root,
            capture_output=True,
            text=True,
            check=True,
        )
    except (subprocess.CalledProcessError, FileNotFoundError):
        return []
    tracked_and_untracked_paths = [line.strip() for line in result.stdout.splitlines() if line.strip()]
    return sorted(
        {root / p for p in tracked_and_untracked_paths if not _is_an_implementation_plan_not_a_prd(root / p)}
    )


def _discover_prds_via_filesystem_walk(root: Path) -> list[Path]:
    paths_ending_in_prd_yaml = {p for p in root.rglob("*prd.yaml") if ".git" not in p.parts}
    paths_under_a_claude_prd_directory = {
        p for p in (root / ".claude" / "prd").glob("*.yaml")
        if ".git" not in p.parts
    }
    all_candidate_paths = paths_ending_in_prd_yaml | paths_under_a_claude_prd_directory
    return sorted(p for p in all_candidate_paths if not _is_an_implementation_plan_not_a_prd(p))


def discover_prds(repo_root: Path | None = None) -> list[Path]:
    root = (repo_root or REPO_ROOT).resolve()
    return _discover_prds_via_git_ls_files(root) or _discover_prds_via_filesystem_walk(root)


def validate_prd(path: Path) -> list[str]:
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
    errors.extend(_check_filename_is_three_snake_case_words(path))
    errors.extend(_check_first_top_level_key_is_pipeline(keys))
    errors.extend(_check_required_top_level_keys_are_present(keys))
    errors.extend(_check_no_top_level_key_has_an_empty_value(data, keys))
    errors.extend(_check_every_field_stays_within_its_char_cap(data, keys))
    errors.extend(_check_status_is_a_valid_enum_value(data.get("status")))
    errors.extend(_check_no_unknown_top_level_keys(keys))
    errors.extend(_check_present_keys_follow_canonical_order(keys))
    errors.extend(_check_summary_has_exactly_problem_solution_tradeoff(data))
    errors.extend(_check_consequences_has_nonempty_positive_and_negative(data))
    errors.extend(_check_discovered_issues_entries_have_status_and_problem(data))
    errors.extend(_check_list_and_mapping_sections_have_the_right_type(data))
    errors.extend(_check_all_section_mapping_keys_are_well_formed(data))
    errors.extend(_check_history_keys_are_sequential_versions(data))
    errors.extend(_check_no_field_cites_a_rule_or_task_id(data))
    errors.extend(_check_scalar_quoting_and_block_style(text))
    errors.extend(_check_line_and_file_length_caps(text))
    return errors


def _path_relative_to_repo_root(path: Path, root: Path) -> Path:
    try:
        return path.resolve().relative_to(root.resolve())
    except ValueError:
        return path


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Validate PRD files against the docs/prd standard."
    )
    parser.add_argument(
        "paths",
        nargs="*",
        type=Path,
        help="specific PRD files to validate (default: discover all in repo)",
    )
    parser.add_argument(
        "--repo-root",
        type=Path,
        default=REPO_ROOT,
        help="repository root used for discovery and relative path display",
    )
    args = parser.parse_args(argv)

    paths = sorted(set(args.paths)) if args.paths else discover_prds(args.repo_root)
    if not paths:
        print("no PRD files found", file=sys.stderr)
        return 1

    all_ok = True
    for path in paths:
        errors = validate_prd(path)
        if errors:
            all_ok = False
            relative_path = _path_relative_to_repo_root(path, args.repo_root)
            for error in errors:
                print(f"{relative_path}: {error}", file=sys.stderr)

    if all_ok:
        print(f"ok: {len(paths)} PRD file(s) validated")
        return 0
    return 1


if __name__ == "__main__":
    sys.exit(main())
