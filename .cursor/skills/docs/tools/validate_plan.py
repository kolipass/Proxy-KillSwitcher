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

_CANONICAL_HEADER_KEY_ORDER = ["prd", "title", "tasks", "verify"]

_CANONICAL_TASK_KEY_ORDER = [
    "id",
    "title",
    "review",
    "introduces",
    "skills",
    "files_to_create",
    "files_to_modify",
    "constraints",
    "depends_on",
    "acceptance",
]
_TASK_KEY_POSITION = {key: idx for idx, key in enumerate(_CANONICAL_TASK_KEY_ORDER)}
REQUIRED_TASK_KEYS = {"id", "title", "review", "skills", "depends_on", "acceptance"}
HUMAN_READ_TASK_KEYS = {"review", "introduces"}

REQUIRED_INTRODUCES_ENTRY_KEYS = ("name", "kind", "at")
ALLOWED_INTRODUCES_KIND_VALUES = {"file", "class", "function", "var", "flag", "rule", "endpoint"}

MAX_LINE_CHARS = 120
MAX_ACCEPTANCE_ITEMS = 3

_TASK_ID = re.compile(r"^S-(\d+)$")
_SKILL_RULE_ID = re.compile(r"^[A-Z]{1,4}\d{2}$")
_PLAN_FILENAME = re.compile(r"^plan_[a-z][a-z0-9]*_[a-z][a-z0-9]*\.yaml$")
_CYRILLIC_CHARACTER = re.compile(r"[а-яА-ЯёЁ]")
_KEY_LINE = re.compile(r"^(\s*)(-\s+)?([A-Za-z_][A-Za-z0-9_]*):(\s.*|)$")
_LIST_ITEM_LINE = re.compile(r"^(\s*)-\s*(.*)$")
_PATH_OR_FILE_LOOKING_TOKEN = re.compile(
    r"[\w.~-]*/[\w./~-]+|\b[\w-]+\.(?:py|sh|ya?ml|md|kt|kts|json|xml|toml)\b"
)
_ONE_LINE_DOUBLE_QUOTED_TASK_KEYS = {"title"}
PROCESS_STYLIST_SKILL_POINTER = "process-stylist/skill.md"


def _is_quoted_scalar_closed_on_this_line(value_after_colon: str) -> bool:
    return (
        len(value_after_colon) >= 2
        and value_after_colon.startswith('"')
        and value_after_colon.endswith('"')
        and not value_after_colon.endswith('\\"')
    )


def _serialize_agent_layer_excluding_human_fields(task: dict) -> str:
    agent_layer = {k: v for k, v in task.items() if k not in HUMAN_READ_TASK_KEYS}
    return yaml.dump(agent_layer, allow_unicode=True, default_flow_style=False)


def _all_leaf_strings(value: object) -> list[str]:
    if isinstance(value, dict):
        leaves: list[str] = []
        for child in value.values():
            leaves.extend(_all_leaf_strings(child))
        return leaves
    if isinstance(value, list):
        leaves = []
        for child in value:
            leaves.extend(_all_leaf_strings(child))
        return leaves
    if isinstance(value, str):
        return [value]
    return []


def _check_header_keys_present_unknown_and_ordered(data: dict) -> list[str]:
    errors: list[str] = []
    keys = list(data.keys())
    for key in _CANONICAL_HEADER_KEY_ORDER:
        if key not in keys:
            errors.append(f"missing required top-level key: {key} (IP02)")
    for key in keys:
        if key not in _CANONICAL_HEADER_KEY_ORDER:
            errors.append(f"unknown top-level key: '{key}' (IP02)")
    present_in_file_order = [k for k in keys if k in _CANONICAL_HEADER_KEY_ORDER]
    present_in_canonical_order = [k for k in _CANONICAL_HEADER_KEY_ORDER if k in present_in_file_order]
    if present_in_file_order != present_in_canonical_order:
        errors.append("top-level keys must follow prd -> title -> tasks -> verify (IP02)")
    return errors


def _check_prd_path_exists(data: dict, repo_root: Path) -> list[str]:
    keys = list(data.keys())
    prd = data.get("prd")
    if isinstance(prd, str) and prd.strip():
        prd_path = Path(prd)
        if not prd_path.is_absolute():
            prd_path = repo_root / prd
        if not prd_path.exists():
            return [f"prd file does not exist: {prd} (IP03)"]
        return []
    if "prd" in keys:
        return ["prd must be a non-empty path string (IP03)"]
    return []


def _check_plan_title_is_present(data: dict) -> list[str]:
    keys = list(data.keys())
    title = data.get("title")
    if "title" in keys and (not isinstance(title, str) or not title.strip()):
        return ["plan title must be a non-empty string (IP04)"]
    return []


def _check_top_level_verify_is_present(data: dict) -> list[str]:
    keys = list(data.keys())
    verify = data.get("verify")
    if "verify" in keys and (not isinstance(verify, str) or not verify.strip()):
        return ["verify must be a non-empty command block (IP21)"]
    return []


def _check_task_key_shape_order_and_emptiness(task: dict, idx: int) -> list[str]:
    errors: list[str] = []
    tid = task.get("id", f"tasks[{idx}]")
    keys = list(task.keys())

    for key in keys:
        if key not in _TASK_KEY_POSITION:
            errors.append(f"{tid}: unknown task key: '{key}' (IP06)")
    for key in REQUIRED_TASK_KEYS:
        if key not in keys:
            errors.append(f"{tid}: missing required task key: {key} (IP06)")

    present_in_file_order = [(k, _TASK_KEY_POSITION[k]) for k in keys if k in _TASK_KEY_POSITION]
    for i in range(1, len(present_in_file_order)):
        if present_in_file_order[i][1] < present_in_file_order[i - 1][1]:
            errors.append(
                f"{tid}: key '{present_in_file_order[i][0]}' is out of canonical task order (IP06)"
            )
            break

    for key in keys:
        value = task[key]
        if key == "depends_on" or key == "skills":
            if not isinstance(value, list):
                errors.append(f"{tid}: '{key}' must be a list (IP06)")
        elif value is None or (isinstance(value, (list, dict, str)) and not value):
            errors.append(f"{tid}: key '{key}' is empty — omit it instead (IP06)")

    if "verify" in keys:
        errors.append(f"{tid}: per-task verify is forbidden (IP21)")
    return errors


def _check_review_is_quoted_russian_and_strippable(task: dict) -> list[str]:
    errors: list[str] = []
    tid = task.get("id", "?")
    review = task.get("review")
    if review is None:
        return errors
    if not isinstance(review, str) or not review.strip():
        return [f"{tid}: review must be a non-empty string (IP08)"]
    if not _CYRILLIC_CHARACTER.search(review):
        errors.append(f"{tid}: review must be written in Russian (IP08)")

    agent_layer_text = _serialize_agent_layer_excluding_human_fields(task)
    for token in _PATH_OR_FILE_LOOKING_TOKEN.findall(review):
        if token not in agent_layer_text:
            errors.append(
                f"{tid}: review mentions '{token}' which appears nowhere in the"
                " agent layer — the human layer must be strippable (IP10)"
            )
    return errors


def _check_introduces_shape_kind_and_name_mirroring(task: dict) -> list[str]:
    errors: list[str] = []
    tid = task.get("id", "?")
    introduces = task.get("introduces")
    if introduces is None:
        return errors
    if not isinstance(introduces, list):
        return [f"{tid}: introduces must be a list of name/kind/at entries (IP09)"]
    agent_layer_text = _serialize_agent_layer_excluding_human_fields(task)
    for i, entry in enumerate(introduces):
        label = f"{tid}: introduces[{i}]"
        if not isinstance(entry, dict):
            errors.append(f"{label} must be a mapping with name, kind, at (IP09)")
            continue
        for required_key in REQUIRED_INTRODUCES_ENTRY_KEYS:
            value = entry.get(required_key)
            if not isinstance(value, str) or not value.strip():
                errors.append(f"{label} is missing required key: {required_key} (IP09)")
        for key in entry:
            if key not in REQUIRED_INTRODUCES_ENTRY_KEYS:
                errors.append(f"{label} has unknown key: '{key}' (IP09)")
        kind = entry.get("kind")
        if isinstance(kind, str) and kind not in ALLOWED_INTRODUCES_KIND_VALUES:
            errors.append(
                f"{label} kind '{kind}' is not one of {sorted(ALLOWED_INTRODUCES_KIND_VALUES)} (IP09)"
            )
        name = entry.get("name")
        if isinstance(name, str) and name.strip() and name not in agent_layer_text:
            errors.append(
                f"{label} name '{name}' does not appear verbatim in the task's"
                " agent fields (IP11)"
            )
        for value in entry.values():
            if isinstance(value, str) and _CYRILLIC_CHARACTER.search(value):
                errors.append(f"{label} values must stay verbatim English (IP19)")
                break
    return errors


def _skill_file_has_numbered_rules(skill_file_text: str) -> bool:
    return re.search(r"^\s*-\s+id:\s+[A-Z]{1,4}\d{2}\s*$", skill_file_text, re.MULTILINE) is not None


def _check_skills_paths_exist_and_rules_are_pinned(task: dict, repo_root: Path) -> list[str]:
    errors: list[str] = []
    tid = task.get("id", "?")
    skills = task.get("skills")
    if not isinstance(skills, list):
        return errors
    for i, entry in enumerate(skills):
        label = f"{tid}: skills[{i}]"
        if not isinstance(entry, dict) or set(entry) != {"path", "rules"}:
            errors.append(f"{label} must be a mapping with exactly path and rules (IP12)")
            continue
        skill_path = entry["path"]
        pinned_rules = entry["rules"]
        if not isinstance(skill_path, str) or not skill_path.strip():
            errors.append(f"{label} path must be a non-empty string (IP12)")
            continue
        resolved_skill_path = Path(skill_path)
        if not resolved_skill_path.is_absolute():
            resolved_skill_path = repo_root / skill_path
        if not resolved_skill_path.exists():
            errors.append(f"{label} path does not exist: {skill_path} (IP12)")
            continue
        if not resolved_skill_path.is_file():
            errors.append(f"{label} path is not a file: {skill_path} (IP12)")
            continue
        if not isinstance(pinned_rules, list):
            errors.append(f"{label} rules must be a list of rule ids (IP12)")
            continue
        try:
            skill_file_text = resolved_skill_path.read_text(encoding="utf-8")
        except OSError as exc:
            errors.append(f"{label} cannot read {skill_path}: {exc} (IP12)")
            continue
        if not pinned_rules and _skill_file_has_numbered_rules(skill_file_text):
            errors.append(
                f"{label} pins no rules but {skill_path} has numbered rules (IP12)"
            )
        for rule_id in pinned_rules:
            rule_id = str(rule_id)
            if not _SKILL_RULE_ID.match(rule_id):
                errors.append(f"{label} rule id '{rule_id}' is not like PD06 (IP12)")
            elif f"id: {rule_id}" not in skill_file_text:
                errors.append(f"{label} rule id '{rule_id}' not found in {skill_path} (IP12)")
    return errors


def _check_files_to_create_and_modify_shapes(task: dict) -> list[str]:
    errors: list[str] = []
    tid = task.get("id", "?")
    for section, prose_key in (("files_to_create", "description"), ("files_to_modify", "changes")):
        entries = task.get(section)
        if entries is None:
            continue
        if not isinstance(entries, list):
            errors.append(f"{tid}: {section} must be a list (IP13)")
            continue
        for i, entry in enumerate(entries):
            label = f"{tid}: {section}[{i}]"
            if not isinstance(entry, dict) or set(entry) != {"path", prose_key}:
                errors.append(
                    f"{label} must be a mapping with exactly path and {prose_key} (IP13)"
                )
                continue
            for key in ("path", prose_key):
                if not isinstance(entry[key], str) or not entry[key].strip():
                    errors.append(f"{label} {key} must be a non-empty string (IP13)")
    return errors


def _check_constraints_are_nonempty_strings(task: dict) -> list[str]:
    tid = task.get("id", "?")
    constraints = task.get("constraints")
    if constraints is None:
        return []
    if not isinstance(constraints, list):
        return [f"{tid}: constraints must be a list of invariant strings (IP14)"]
    return [
        f"{tid}: constraints[{i}] must be a non-empty string (IP14)"
        for i, item in enumerate(constraints)
        if not isinstance(item, str) or not item.strip()
    ]


def _check_depends_on_and_acceptance_shape(task: dict, own_task_number: int) -> list[str]:
    errors: list[str] = []
    tid = task.get("id", "?")
    depends_on = task.get("depends_on")
    if isinstance(depends_on, list):
        for dependency in depends_on:
            match = _TASK_ID.match(str(dependency))
            if not match:
                errors.append(f"{tid}: depends_on entry '{dependency}' is not an S-<n> id (IP15)")
            elif int(match.group(1)) >= own_task_number:
                errors.append(
                    f"{tid}: depends_on '{dependency}' must reference an earlier task (IP15)"
                )
    acceptance = task.get("acceptance")
    if acceptance is not None:
        if not isinstance(acceptance, list) or not acceptance:
            errors.append(f"{tid}: acceptance must be a non-empty list (IP16)")
        else:
            if len(acceptance) > MAX_ACCEPTANCE_ITEMS:
                errors.append(
                    f"{tid}: acceptance has {len(acceptance)} items,"
                    f" max {MAX_ACCEPTANCE_ITEMS} (IP16)"
                )
            for i, item in enumerate(acceptance):
                if not isinstance(item, str) or not item.strip():
                    errors.append(f"{tid}: acceptance[{i}] must be a non-empty string (IP16)")
    return errors


def _files_to_modify_changes_text(task: dict) -> str:
    entries = task.get("files_to_modify")
    if not isinstance(entries, list):
        return ""
    return " ".join(
        entry["changes"]
        for entry in entries
        if isinstance(entry, dict) and isinstance(entry.get("changes"), str)
    )


def _check_plan_ends_with_a_process_stylist_pass(tasks: list) -> list[str]:
    errors: list[str] = []
    if len(tasks) < 2:
        errors.append(
            "plan must end with a process-stylist pass — at least two tasks (IP17)"
        )
        return errors

    final_task = tasks[-1]
    if isinstance(final_task, dict):
        fid = final_task.get("id", "?")
        if PROCESS_STYLIST_SKILL_POINTER not in _files_to_modify_changes_text(final_task).lower():
            errors.append(
                f"{fid}: final task changes must point at '{PROCESS_STYLIST_SKILL_POINTER}' (IP17)"
            )
        if not final_task.get("constraints"):
            errors.append(f"{fid}: final task must carry constraints (IP17)")

        acceptance_text = " ".join(_all_leaf_strings(final_task.get("acceptance"))).lower()
        if "verify" not in acceptance_text or "exit" not in acceptance_text:
            errors.append(
                f"{fid}: final task acceptance must tie the verify commands"
                " to exit 0 (IP21)"
            )
    return errors


def _check_agent_fields_have_no_cyrillic(data: dict) -> list[str]:
    errors: list[str] = []
    agent_header = {k: v for k, v in data.items() if k in ("title", "verify", "prd")}
    for value in _all_leaf_strings(agent_header):
        if _CYRILLIC_CHARACTER.search(value):
            errors.append("plan header agent fields must be English (IP19)")
            break
    tasks = data.get("tasks")
    if not isinstance(tasks, list):
        return errors
    for task in tasks:
        if not isinstance(task, dict):
            continue
        tid = task.get("id", "?")
        agent_fields = {k: v for k, v in task.items() if k not in HUMAN_READ_TASK_KEYS}
        for value in _all_leaf_strings(agent_fields):
            if _CYRILLIC_CHARACTER.search(value):
                errors.append(f"{tid}: agent fields must be English (IP19)")
                break
    return errors


def _check_scalar_quoting_and_block_style(text: str) -> list[str]:
    errors: list[str] = []
    lines = text.splitlines()
    open_block_indent: int | None = None
    inside_a_wrapped_quoted_scalar = False
    inside_an_introduces_block = False
    introduces_block_indent = 0
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
            key = key_match.group(3)
            value_after_colon = key_match.group(4).strip()
            is_list_entry = key_match.group(2) is not None
            effective_indent = indent + (len(key_match.group(2)) if is_list_entry else 0)

            if inside_an_introduces_block and effective_indent <= introduces_block_indent:
                inside_an_introduces_block = False
            if key == "introduces":
                inside_an_introduces_block = True
                introduces_block_indent = effective_indent

            if key == "verify" and indent == 0:
                if value_after_colon.startswith(("|", ">")):
                    open_block_indent = indent
                continue
            if value_after_colon.startswith("|"):
                errors.append(
                    f"line {lineno}: '{key}' uses a '|' literal block — allowed only"
                    " for the top-level verify (IP21)"
                )
                open_block_indent = indent
                continue
            if key in _ONE_LINE_DOUBLE_QUOTED_TASK_KEYS:
                rule = "IP04" if indent == 0 else "IP07"
                if value_after_colon.startswith(">"):
                    errors.append(
                        f"line {lineno}: '{key}' must be a one-line double-quoted"
                        f" string, not a block ({rule})"
                    )
                    open_block_indent = indent
                elif not _is_quoted_scalar_closed_on_this_line(value_after_colon):
                    errors.append(
                        f"line {lineno}: '{key}' must be a one-line double-quoted"
                        f" string ({rule})"
                    )
                    if value_after_colon.startswith('"'):
                        inside_a_wrapped_quoted_scalar = True
                continue
            if key == "review":
                if value_after_colon.startswith(">"):
                    errors.append(
                        f"line {lineno}: 'review' must be a double-quoted string,"
                        " not a block (IP08)"
                    )
                    open_block_indent = indent
                elif not value_after_colon.startswith('"'):
                    errors.append(
                        f"line {lineno}: 'review' must be a double-quoted string (IP08)"
                    )
                elif not _is_quoted_scalar_closed_on_this_line(value_after_colon):
                    inside_a_wrapped_quoted_scalar = True
                continue
            if inside_an_introduces_block and key in REQUIRED_INTRODUCES_ENTRY_KEYS:
                if not _is_quoted_scalar_closed_on_this_line(value_after_colon):
                    errors.append(
                        f"line {lineno}: introduces '{key}' must be a double-quoted"
                        " string on the key's line (IP18)"
                    )
                continue
            if value_after_colon.startswith(">"):
                open_block_indent = indent
            elif value_after_colon.startswith('"'):
                errors.append(
                    f"line {lineno}: '{key}' is agent text and must not be"
                    " double-quoted — quotes mark the human layer (IP18)"
                )
                if not _is_quoted_scalar_closed_on_this_line(value_after_colon):
                    inside_a_wrapped_quoted_scalar = True
        elif list_item_match:
            value_after_dash = list_item_match.group(2).strip()
            if value_after_dash.startswith("|"):
                errors.append(
                    f"line {lineno}: list item uses a '|' literal block — allowed only"
                    " for the top-level verify (IP21)"
                )
                open_block_indent = indent
            elif value_after_dash.startswith(">"):
                open_block_indent = indent
            elif value_after_dash.startswith('"'):
                errors.append(
                    f"line {lineno}: list item is agent text and must not be"
                    " double-quoted — quotes mark the human layer (IP18)"
                )
                if not _is_quoted_scalar_closed_on_this_line(value_after_dash):
                    inside_a_wrapped_quoted_scalar = True
        else:
            if ":" in stripped:
                errors.append(
                    f"line {lineno}: key is not plain English snake_case (IP20)"
                )
    return errors


def _check_line_length_cap(text: str) -> list[str]:
    return [
        f"line {i}: {len(line)} chars (max {MAX_LINE_CHARS}) (IP20)"
        for i, line in enumerate(text.splitlines(), start=1)
        if len(line) > MAX_LINE_CHARS
    ]


def _discover_plans_via_git_ls_files(root: Path) -> list[Path]:
    try:
        result = subprocess.run(
            ["git", "ls-files", "--cached", "--others", "--exclude-standard",
             ".claude/prd/plan_*.yaml"],
            cwd=root,
            capture_output=True,
            text=True,
            check=True,
        )
    except (subprocess.CalledProcessError, FileNotFoundError):
        return []
    tracked_and_untracked_paths = [line.strip() for line in result.stdout.splitlines() if line.strip()]
    return sorted({root / p for p in tracked_and_untracked_paths})


def _discover_plans_via_filesystem_walk(root: Path) -> list[Path]:
    prd_dir = root / ".claude" / "prd"
    if not prd_dir.is_dir():
        return []
    return sorted(prd_dir.glob("plan_*.yaml"))


def discover_plans(repo_root: Path | None = None) -> list[Path]:
    root = (repo_root or REPO_ROOT).resolve()
    return _discover_plans_via_git_ls_files(root) or _discover_plans_via_filesystem_walk(root)


def _check_filename_matches_plan_naming(path: Path) -> list[str]:
    if _PLAN_FILENAME.match(path.name):
        return []
    return [
        f"file name '{path.name}' must look like plan_<two_word_name>.yaml,"
        " e.g. plan_launcher_detect.yaml (IP01)"
    ]


def _check_tasks_have_sequential_ids_and_pass_every_task_check(tasks: list, repo_root: Path) -> list[str]:
    errors: list[str] = []
    for idx, task in enumerate(tasks):
        if not isinstance(task, dict):
            errors.append(f"tasks[{idx}] must be a mapping (IP05)")
            continue
        expected_id = f"S-{idx + 1}"
        actual_id = str(task.get("id", ""))
        if actual_id != expected_id:
            errors.append(f"tasks[{idx}] id must be '{expected_id}', got '{actual_id}' (IP05)")
        errors.extend(_check_task_key_shape_order_and_emptiness(task, idx))
        errors.extend(_check_review_is_quoted_russian_and_strippable(task))
        errors.extend(_check_introduces_shape_kind_and_name_mirroring(task))
        errors.extend(_check_skills_paths_exist_and_rules_are_pinned(task, repo_root))
        errors.extend(_check_files_to_create_and_modify_shapes(task))
        errors.extend(_check_constraints_are_nonempty_strings(task))
        errors.extend(_check_depends_on_and_acceptance_shape(task, idx + 1))
    return errors


def validate_plan(path: Path, repo_root: Path | None = None) -> list[str]:
    root = (repo_root or REPO_ROOT).resolve()
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
    if not data:
        return ["file contains no top-level keys"]

    errors: list[str] = []
    errors.extend(_check_filename_matches_plan_naming(path))
    errors.extend(_check_header_keys_present_unknown_and_ordered(data))
    errors.extend(_check_prd_path_exists(data, root))
    errors.extend(_check_plan_title_is_present(data))
    errors.extend(_check_top_level_verify_is_present(data))

    tasks = data.get("tasks")
    if tasks is not None and not isinstance(tasks, list):
        errors.append("'tasks' must be a list (IP05)")
        tasks = None
    if isinstance(tasks, list):
        if not tasks:
            errors.append("'tasks' must be a non-empty list (IP05)")
        errors.extend(_check_tasks_have_sequential_ids_and_pass_every_task_check(tasks, root))
        errors.extend(_check_plan_ends_with_a_process_stylist_pass(tasks))

    errors.extend(_check_agent_fields_have_no_cyrillic(data))
    errors.extend(_check_scalar_quoting_and_block_style(text))
    errors.extend(_check_line_length_cap(text))

    return errors


def _path_relative_to_repo_root(path: Path, root: Path) -> Path:
    try:
        return path.resolve().relative_to(root.resolve())
    except ValueError:
        return path


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Validate Implementation Plan files against the docs/plan standard."
    )
    parser.add_argument(
        "paths",
        nargs="*",
        type=Path,
        help="specific plan files to validate (default: discover all in repo)",
    )
    parser.add_argument(
        "--repo-root",
        type=Path,
        default=REPO_ROOT,
        help="repository root used for discovery and relative path display",
    )
    args = parser.parse_args(argv)

    paths = sorted(set(args.paths)) if args.paths else discover_plans(args.repo_root)
    if not paths:
        print("ok: 0 plan file(s) validated (none found)")
        return 0

    all_ok = True
    for path in paths:
        errors = validate_plan(path, args.repo_root)
        if errors:
            all_ok = False
            relative_path = _path_relative_to_repo_root(path, args.repo_root)
            for error in errors:
                print(f"{relative_path}: {error}", file=sys.stderr)

    if all_ok:
        print(f"ok: {len(paths)} plan file(s) validated")
        return 0
    return 1


if __name__ == "__main__":
    sys.exit(main())
