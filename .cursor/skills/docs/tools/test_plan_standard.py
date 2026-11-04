from __future__ import annotations

from pathlib import Path

import pytest
import validate_plan as vp

_FIXTURE_FILENAME = "plan_test_fixture.yaml"

_FIRST_TASK = """\
prd: .claude/prd/demo_stub_prd.yaml
title: "Demo plan for the standard tests"
tasks:
    -   id: S-1
        title: "Create sample file"
        review: "Тестовая задача: создаёт файл sample.py для проверки валидатора."
        introduces:
            -   name: "sample.py"
                kind: "file"
                at: "tools/"
        skills:
            -   path: .cursor/skills/demo/SKILL.md
                rules: [DM01]
        files_to_create:
            -   path: tools/sample.py
                description: >
                    Sample module printing hello.
        constraints:
            - >
                Do not touch anything outside tools/.
        depends_on: []
        acceptance:
            - tools/sample.py exists
"""

_SECOND_TASK_WIRING_DOCS = """\
    -   id: S-2
        title: "Wire sample file into docs"
        review: "Вторая задача: подключает созданный файл к документации."
        skills: []
        files_to_modify:
            -   path: docs/index.md
                changes: >
                    Add a link to tools/sample.py in the tools section.
        depends_on:
            - S-1
        acceptance:
            - docs/index.md links to the sample
"""

_FINAL_PROCESS_STYLIST_TASK = """\
    -   id: S-3
        title: "Bring the branch in line with the project rules"
        review: "Последняя задача: приводит ветку к правилам скиллов и гоняет валидаторы."
        skills: []
        files_to_modify:
            -   path: whole branch diff
                changes: >
                    Follow .cursor/skills/process-stylist/SKILL.md over the whole branch diff;
                    fix only violations citing a rule id, then run the validators once.
        constraints:
            - >
                Change code only for a cited rule id; never change behavior.
        depends_on:
            - S-1
            - S-2
        acceptance:
            - every command in the top-level verify block exits 0
"""

_TOP_LEVEL_VERIFY_BLOCK = """\
verify: |
    test -f tools/sample.py
"""

VALID_PLAN_WITH_THREE_TASKS = (
    _FIRST_TASK + _SECOND_TASK_WIRING_DOCS + _FINAL_PROCESS_STYLIST_TASK + _TOP_LEVEL_VERIFY_BLOCK
)

_DEMO_SKILL_WITH_NUMBERED_RULES = """\
---
name: demo
description: Demo skill with numbered rules.
---

## Rules

```yaml
rules:
    - id: DM01
      rule: Demo rule one
    - id: DM02
      rule: Demo rule two
```
"""

_SKILL_WITHOUT_NUMBERED_RULES = """\
---
name: plain
description: Demo skill without numbered rules.
---

Freeform guidance only.
"""


def _build_fixture_repo(tmp_path: Path) -> Path:
    (tmp_path / ".claude/prd").mkdir(parents=True, exist_ok=True)
    (tmp_path / ".claude/prd/demo_stub_prd.yaml").write_text("pipeline: stub\n")
    (tmp_path / ".cursor/skills/demo").mkdir(parents=True, exist_ok=True)
    (tmp_path / ".cursor/skills/demo/SKILL.md").write_text(_DEMO_SKILL_WITH_NUMBERED_RULES)
    (tmp_path / ".cursor/skills/plain").mkdir(parents=True, exist_ok=True)
    (tmp_path / ".cursor/skills/plain/SKILL.md").write_text(_SKILL_WITHOUT_NUMBERED_RULES)
    return tmp_path


def _write_plan_and_validate(tmp_path: Path, text: str, name: str = _FIXTURE_FILENAME) -> list[str]:
    root = _build_fixture_repo(tmp_path)
    plan_dir = root / ".claude/prd"
    plan_dir.mkdir(parents=True, exist_ok=True)
    path = plan_dir / name
    path.write_text(text, encoding="utf-8")
    return vp.validate_plan(path, repo_root=root)


def _write_plan_file(root: Path, name: str, text: str = VALID_PLAN_WITH_THREE_TASKS) -> Path:
    path = root / ".claude/prd" / name
    path.write_text(text, encoding="utf-8")
    return path


def _assert_violates(errors: list[str], rule_id: str) -> None:
    assert errors, f"expected a {rule_id} violation, got none"
    assert any(rule_id in err for err in errors), f"no {rule_id} in: {errors}"


def test_valid_fixture_passes(tmp_path: Path) -> None:
    assert _write_plan_and_validate(tmp_path, VALID_PLAN_WITH_THREE_TASKS) == []


def test_ip01_filename_shape(tmp_path: Path) -> None:
    errors = _write_plan_and_validate(tmp_path, VALID_PLAN_WITH_THREE_TASKS, name="plan_final.yaml")
    _assert_violates(errors, "IP01")
    errors = _write_plan_and_validate(
        tmp_path, VALID_PLAN_WITH_THREE_TASKS, name="09-0400_test_fixture.yaml"
    )
    _assert_violates(errors, "IP01")


def test_ip02_missing_and_unknown_header_keys(tmp_path: Path) -> None:
    text = VALID_PLAN_WITH_THREE_TASKS.replace("prd: .claude/prd/demo_stub_prd.yaml\n", "")
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP02")
    text = VALID_PLAN_WITH_THREE_TASKS + "extra_key: >\n    surprise\n"
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP02")
    text = "plan_path: /tmp/plan_test_fixture.yaml\n" + VALID_PLAN_WITH_THREE_TASKS
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP02")


def test_ip03_prd_must_exist(tmp_path: Path) -> None:
    text = VALID_PLAN_WITH_THREE_TASKS.replace("demo_stub_prd.yaml", "missing_stub_prd.yaml")
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP03")


def test_ip04_plan_title_one_line_quoted(tmp_path: Path) -> None:
    text = VALID_PLAN_WITH_THREE_TASKS.replace(
        'title: "Demo plan for the standard tests"',
        "title: >\n    Demo plan for the standard tests",
    )
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP04")
    text = VALID_PLAN_WITH_THREE_TASKS.replace(
        'title: "Demo plan for the standard tests"',
        "title: Demo plan for the standard tests",
    )
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP04")


def test_ip05_sequential_ids(tmp_path: Path) -> None:
    text = VALID_PLAN_WITH_THREE_TASKS.replace("id: S-2", "id: S-3")
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP05")


def test_ip06_unknown_key_order_and_empty_optional(tmp_path: Path) -> None:
    text = VALID_PLAN_WITH_THREE_TASKS.replace(
        "        depends_on: []", "        description: >\n            extra\n        depends_on: []"
    )
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP06")
    text = VALID_PLAN_WITH_THREE_TASKS.replace(
        "        depends_on: []\n        acceptance:\n            - tools/sample.py exists",
        "        acceptance:\n            - tools/sample.py exists\n        depends_on: []",
    )
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP06")
    text = VALID_PLAN_WITH_THREE_TASKS.replace(
        "        constraints:\n            - >\n                Do not touch anything outside tools/.\n",
        "        constraints: []\n",
    )
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP06")


def test_ip07_task_title_one_line_quoted(tmp_path: Path) -> None:
    text = VALID_PLAN_WITH_THREE_TASKS.replace(
        'title: "Create sample file"', "title: >\n            Create sample file"
    )
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP07")


def test_ip08_review_quoted_and_russian(tmp_path: Path) -> None:
    text = VALID_PLAN_WITH_THREE_TASKS.replace(
        'review: "Тестовая задача: создаёт файл sample.py для проверки валидатора."',
        "review: >\n            Тестовая задача про файл sample.py.",
    )
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP08")
    text = VALID_PLAN_WITH_THREE_TASKS.replace(
        "Тестовая задача: создаёт файл sample.py для проверки валидатора.",
        "English-only review about sample.py.",
    )
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP08")


def test_ip09_introduces_shape_and_kind(tmp_path: Path) -> None:
    text = VALID_PLAN_WITH_THREE_TASKS.replace('kind: "file"', 'kind: "module"')
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP09")
    text = VALID_PLAN_WITH_THREE_TASKS.replace('                at: "tools/"\n', "")
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP09")


def test_ip10_review_is_strippable(tmp_path: Path) -> None:
    text = VALID_PLAN_WITH_THREE_TASKS.replace(
        "для проверки валидатора.", "и правит secret/config.json по пути."
    )
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP10")


def test_ip11_introduces_name_mirrors_agent_layer(tmp_path: Path) -> None:
    text = VALID_PLAN_WITH_THREE_TASKS.replace('name: "sample.py"', 'name: "ghost.py"')
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP11")


def test_ip12_skill_pins(tmp_path: Path) -> None:
    text = VALID_PLAN_WITH_THREE_TASKS.replace(
        "path: .cursor/skills/demo/SKILL.md", "path: .cursor/skills/nope/SKILL.md"
    )
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP12")
    text = VALID_PLAN_WITH_THREE_TASKS.replace("rules: [DM01]", "rules: []")
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP12")
    text = VALID_PLAN_WITH_THREE_TASKS.replace("rules: [DM01]", "rules: [DM99]")
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP12")
    text = VALID_PLAN_WITH_THREE_TASKS.replace("rules: [DM01]", "rules: [lowercase]")
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP12")


def test_ip12_unnumbered_skill_allows_empty_pin(tmp_path: Path) -> None:
    text = VALID_PLAN_WITH_THREE_TASKS.replace(
        "            -   path: .cursor/skills/demo/SKILL.md\n                rules: [DM01]",
        "            -   path: .cursor/skills/plain/SKILL.md\n                rules: []",
    )
    assert _write_plan_and_validate(tmp_path, text) == []


def test_ip13_file_entry_shapes(tmp_path: Path) -> None:
    text = VALID_PLAN_WITH_THREE_TASKS.replace(
        "            -   path: tools/sample.py\n                description: >\n"
        "                    Sample module printing hello.",
        "            -   path: tools/sample.py",
    )
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP13")


def test_ip14_constraints_is_string_list(tmp_path: Path) -> None:
    text = VALID_PLAN_WITH_THREE_TASKS.replace(
        "        constraints:\n            - >\n                Do not touch anything outside tools/.",
        "        constraints:\n            frozen_zone: >\n                Do not touch tools/.",
    )
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP14")


def test_ip15_depends_on_earlier_only(tmp_path: Path) -> None:
    text = VALID_PLAN_WITH_THREE_TASKS.replace(
        "        depends_on: []", "        depends_on:\n            - S-2"
    )
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP15")


def test_ip16_acceptance_capped(tmp_path: Path) -> None:
    text = VALID_PLAN_WITH_THREE_TASKS.replace(
        "            - tools/sample.py exists\n    -   id: S-2",
        "            - tools/sample.py exists\n            - extra one\n"
        "            - extra two\n            - extra three\n    -   id: S-2",
    )
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP16")


def test_ip17_final_task_must_be_a_process_stylist_pass(tmp_path: Path) -> None:
    single_task = VALID_PLAN_WITH_THREE_TASKS.replace(_SECOND_TASK_WIRING_DOCS, "").replace(
        _FINAL_PROCESS_STYLIST_TASK, ""
    )
    _assert_violates(_write_plan_and_validate(tmp_path, single_task), "IP17")
    missing_pointer = VALID_PLAN_WITH_THREE_TASKS.replace(
        "Follow .cursor/skills/process-stylist/SKILL.md over the whole branch diff;",
        "Audit the whole branch diff;",
    )
    _assert_violates(_write_plan_and_validate(tmp_path, missing_pointer), "IP17")
    no_constraints = VALID_PLAN_WITH_THREE_TASKS.replace(
        "        constraints:\n            - >\n"
        "                Change code only for a cited rule id; never change behavior.\n",
        "",
    )
    _assert_violates(_write_plan_and_validate(tmp_path, no_constraints), "IP17")


def test_ip18_agent_text_never_quoted(tmp_path: Path) -> None:
    text = VALID_PLAN_WITH_THREE_TASKS.replace(
        "                changes: >\n"
        "                    Add a link to tools/sample.py in the tools section.",
        '                changes: "Add a link to tools/sample.py."',
    )
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP18")
    text = VALID_PLAN_WITH_THREE_TASKS.replace('name: "sample.py"', "name: sample.py")
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP18")
    text = VALID_PLAN_WITH_THREE_TASKS.replace(
        "            - tools/sample.py exists", '            - "tools/sample.py exists"'
    )
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP18")


def test_ip18_plain_one_line_agent_prose_is_ok(tmp_path: Path) -> None:
    text = VALID_PLAN_WITH_THREE_TASKS.replace(
        "                changes: >\n"
        "                    Add a link to tools/sample.py in the tools section.",
        "                changes: Add a link to tools/sample.py in the tools section.",
    )
    assert _write_plan_and_validate(tmp_path, text) == []


def test_ip19_agent_fields_english(tmp_path: Path) -> None:
    text = VALID_PLAN_WITH_THREE_TASKS.replace(
        "Add a link to tools/sample.py in the tools section.",
        "Добавить ссылку на tools/sample.py.",
    )
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP19")


def test_ip20_lines_and_keys(tmp_path: Path) -> None:
    text = VALID_PLAN_WITH_THREE_TASKS.replace(
        "            - tools/sample.py exists",
        "            - tools/sample.py exists and " + "x" * 120,
    )
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP20")
    text = VALID_PLAN_WITH_THREE_TASKS.replace("        review:", "        ⚠️ review:")
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP20")


def test_ip21_single_top_level_verify(tmp_path: Path) -> None:
    text = VALID_PLAN_WITH_THREE_TASKS.replace(
        "        acceptance:\n            - tools/sample.py exists\n    -   id: S-2",
        "        acceptance:\n            - tools/sample.py exists\n"
        "        verify: >\n            echo hi\n    -   id: S-2",
    )
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP21")
    text = VALID_PLAN_WITH_THREE_TASKS.replace("verify: |\n    test -f tools/sample.py\n", "")
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP02")
    text = VALID_PLAN_WITH_THREE_TASKS.replace(
        "                changes: >\n", "                changes: |\n"
    )
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP21")


def test_ip21_final_task_acceptance_must_tie_to_verify(tmp_path: Path) -> None:
    text = VALID_PLAN_WITH_THREE_TASKS.replace(
        "            - every command in the top-level verify block exits 0",
        "            - the build is green",
    )
    _assert_violates(_write_plan_and_validate(tmp_path, text), "IP21")


def test_explicit_valid_path_exits_zero(tmp_path: Path, capsys: pytest.CaptureFixture) -> None:
    root = _build_fixture_repo(tmp_path)
    plan = _write_plan_file(root, "plan_demo_plan.yaml")
    exit_code = vp.main([str(plan), "--repo-root", str(root)])
    captured = capsys.readouterr()
    assert exit_code == 0
    assert "ok: 1 plan file(s) validated" in captured.out


def test_broken_plan_exits_nonzero_with_message(tmp_path: Path, capsys: pytest.CaptureFixture) -> None:
    root = _build_fixture_repo(tmp_path)
    broken_text = VALID_PLAN_WITH_THREE_TASKS.replace(
        '        review: "Тестовая задача: создаёт файл sample.py для проверки валидатора."\n', ""
    )
    plan = _write_plan_file(root, "plan_demo_plan.yaml", broken_text)
    exit_code = vp.main([str(plan), "--repo-root", str(root)])
    captured = capsys.readouterr()
    assert exit_code == 1
    assert "plan_demo_plan.yaml" in captured.err


def test_unparseable_yaml_reports_parse_error(tmp_path: Path, capsys: pytest.CaptureFixture) -> None:
    root = _build_fixture_repo(tmp_path)
    plan_path = root / ".claude/prd/plan_demo_plan.yaml"
    plan_path.write_text("tasks: [unclosed\n", encoding="utf-8")
    exit_code = vp.main([str(plan_path), "--repo-root", str(root)])
    captured = capsys.readouterr()
    assert exit_code == 1
    assert "YAML parse error" in captured.err


def test_discovery_finds_written_plan(tmp_path: Path) -> None:
    root = _build_fixture_repo(tmp_path)
    plan = _write_plan_file(root, "plan_demo_plan.yaml")
    discovered_plans = vp.discover_plans(root)
    assert plan in discovered_plans


def test_repo_wide_run_validates_discovered_plans(tmp_path: Path, capsys: pytest.CaptureFixture) -> None:
    root = _build_fixture_repo(tmp_path)
    _write_plan_file(root, "plan_demo_plan.yaml")
    exit_code = vp.main(["--repo-root", str(root)])
    captured = capsys.readouterr()
    assert exit_code == 0
    assert "ok: 1 plan file(s) validated" in captured.out


def test_no_plans_found_is_ok(tmp_path: Path, capsys: pytest.CaptureFixture) -> None:
    root = _build_fixture_repo(tmp_path)
    exit_code = vp.main(["--repo-root", str(root)])
    captured = capsys.readouterr()
    assert exit_code == 0
    assert "none found" in captured.out
