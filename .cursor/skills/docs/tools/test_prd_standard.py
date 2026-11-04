from __future__ import annotations

from pathlib import Path

import validate_prd as va
import pytest

PRD_PATHS = va.discover_prds()

VALID_PRD_WITH_ALL_SECTIONS = """pipeline: Test Pipeline

status: accepted

summary:
  problem: "Some problem in one line."
  solution: "Some solution in one line."
  tradeoff: "Some price paid for the solution."

context: >
  Some problem statement with full mechanics
  spanning multiple lines.

decision: >
  Some decision text with rationale
  spanning multiple lines.

consequences:
  positive:
    do_one_thing: Helps with X.
  negative:
    costs_something: Costs Y.

rules:
  keep_it_simple: >
    Always do Y, never Z.

flow:
  - step_one — does a thing

components:
  runner: runner-claude-full

paths:
  - PLAN_FILE points at this PRD's plan file

setup:
  - docker exec ... claude auth login

implementation_deltas:
  small_rename: Renamed A to B during implementation.

discovered_issues:
  side_bug:
    status: resolved
    problem: >
      Found a side bug while verifying the change.
    fix: Patched it in the same change.

rejected_alternatives:
  some_other_way: Rejected because reasons.

history:
  v1: initial decision
  v2: second revision
"""


def _write_and_validate(tmp_path: Path, text: str, name: str = "standard_test_fixture.yaml") -> list[str]:
    p = tmp_path / name
    p.write_text(text, encoding="utf-8")
    return va.validate_prd(p)


def _decision_block_of_word_groups(word_group_count: int) -> str:
    return "decision: >\n" + "".join(f"  {'word ' * 20}\n" for _ in range(word_group_count))


def _move_rejected_alternatives_before_rules(prd_text: str) -> str:
    text_with_duplicated_section = prd_text.replace(
        "rules:\n  keep_it_simple: >\n    Always do Y, never Z.\n",
        "rejected_alternatives:\n  some_other_way: Rejected because reasons.\n\n"
        "rules:\n  keep_it_simple: >\n    Always do Y, never Z.\n",
        1,
    )
    return text_with_duplicated_section.replace(
        "\nrejected_alternatives:\n  some_other_way: Rejected because reasons.\n\nhistory:",
        "\nhistory:",
        1,
    )


@pytest.mark.parametrize(
    "path",
    PRD_PATHS,
    ids=[str(p.relative_to(va.REPO_ROOT)) for p in PRD_PATHS],
)
def test_prd_standard(path: Path) -> None:
    errors = va.validate_prd(path)
    assert errors == [], "\n".join(errors)


def test_at_least_one_prd_discovered() -> None:
    assert PRD_PATHS, "no PRD files discovered"


def test_valid_fixture_passes(tmp_path: Path) -> None:
    assert _write_and_validate(tmp_path, VALID_PRD_WITH_ALL_SECTIONS) == []


def test_pd01_first_key_must_be_pipeline(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace("pipeline: Test Pipeline\n\n", "", 1) + "pipeline: Test Pipeline\n"
    errors = _write_and_validate(tmp_path, text)
    assert any("first top-level key must be 'pipeline'" in e for e in errors)


def test_pd02_status_enum(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace("status: accepted", "status: maybe")
    errors = _write_and_validate(tmp_path, text)
    assert any("status must be one of" in e for e in errors)


def test_pd02_superseded_is_valid(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace("status: accepted", "status: superseded")
    errors = _write_and_validate(tmp_path, text)
    assert not any("status must be one of" in e for e in errors)


def test_pd03_missing_required_summary(tmp_path: Path) -> None:
    lines = VALID_PRD_WITH_ALL_SECTIONS.splitlines()
    summary_line = lines.index("summary:")
    text = "\n".join(lines[:summary_line] + lines[summary_line + 4 :])
    errors = _write_and_validate(tmp_path, text)
    assert any("missing required key: summary" in e for e in errors)


def test_pd03_missing_required_context(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace(
        "context: >\n  Some problem statement with full mechanics\n  spanning multiple lines.\n\n", ""
    )
    errors = _write_and_validate(tmp_path, text)
    assert any("missing required key: context" in e for e in errors)


def test_pd03_missing_required_status(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace("status: accepted\n\n", "")
    errors = _write_and_validate(tmp_path, text)
    assert any("missing required key: status" in e for e in errors)


def test_pd04_summary_missing_tradeoff(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace('  tradeoff: "Some price paid for the solution."\n', "")
    errors = _write_and_validate(tmp_path, text)
    assert any("summary is missing required key: tradeoff" in e for e in errors)


def test_pd04_summary_unknown_key(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace(
        '  tradeoff: "Some price paid for the solution."',
        '  tradeoff: "Some price paid for the solution."\n  bonus: "Not allowed here."',
    )
    errors = _write_and_validate(tmp_path, text)
    assert any("summary has unknown key: 'bonus'" in e for e in errors)


def test_pd05_summary_rejects_folded_block(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace(
        '  problem: "Some problem in one line."',
        "  problem: >\n    Some problem in a folded block.",
    )
    errors = _write_and_validate(tmp_path, text)
    assert any("summary.problem must be a double-quoted string, not a block (PD05)" in e for e in errors)


def test_pd05_summary_rejects_unquoted_value(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace('  problem: "Some problem in one line."', "  problem: Some bare text.")
    errors = _write_and_validate(tmp_path, text)
    assert any("summary.problem must be a double-quoted string (PD05)" in e for e in errors)


def test_pd05_summary_wrapped_quoted_value_is_valid(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace(
        '  problem: "Some problem in one line."',
        '  problem: "Some problem that wraps\n    onto the next physical line."',
    )
    assert _write_and_validate(tmp_path, text) == []


def test_pd05_summary_cap_is_300(tmp_path: Path) -> None:
    problem_over_300_chars = "a" * 301
    text = VALID_PRD_WITH_ALL_SECTIONS.replace("Some problem in one line.", problem_over_300_chars)
    errors = _write_and_validate(tmp_path, text)
    assert any("summary.problem" in e and "(max 300)" in e for e in errors)


def test_pd06_rejects_literal_block(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace("context: >", "context: |")
    errors = _write_and_validate(tmp_path, text)
    assert any("'context' uses '|' literal block" in e for e in errors)


def test_pd06_rejects_multiline_quoted_scalar(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace(
        "decision: >\n  Some decision text with rationale\n  spanning multiple lines.",
        'decision: "Some decision text with rationale\n  spanning multiple lines."',
    )
    errors = _write_and_validate(tmp_path, text)
    assert any("'decision' multiline quoted scalar" in e for e in errors)


def test_pd06_bare_scalar_closed_on_the_keys_line_is_valid(tmp_path: Path) -> None:
    assert _write_and_validate(tmp_path, VALID_PRD_WITH_ALL_SECTIONS) == []


def test_pd06_rejects_closed_quoted_scalar_outside_summary(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace("  runner: runner-claude-full", '  runner: "runner-claude-full"')
    errors = _write_and_validate(tmp_path, text)
    assert any("'runner' is quoted" in e and "(PD06)" in e for e in errors)


def test_pd06_rejects_quoted_list_item_outside_summary(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace(
        "  - PLAN_FILE points at this PRD's plan file", '  - "PLAN_FILE points at this PRD\'s plan file"'
    )
    errors = _write_and_validate(tmp_path, text)
    assert any("list item is quoted" in e and "(PD06)" in e for e in errors)


def test_pd06_rejects_bare_scalar_pushed_to_next_line(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace("  runner: runner-claude-full", "  runner:\n    runner-claude-full")
    errors = _write_and_validate(tmp_path, text)
    assert any("'runner' value must not be pushed to the next line" in e for e in errors)


def test_pd06_rejects_bare_scalar_spilling_onto_next_line(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace(
        "  runner: runner-claude-full", "  runner: runner-claude-full\n    still part of it"
    )
    errors = _write_and_validate(tmp_path, text)
    assert any("'runner' bare value spills onto the next line" in e for e in errors)


def test_pd06_nested_section_header_is_not_a_value_pushed_to_next_line(tmp_path: Path) -> None:
    assert _write_and_validate(tmp_path, VALID_PRD_WITH_ALL_SECTIONS) == []


def test_pd06_rejects_value_on_next_line(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace(
        "decision: >\n  Some decision text with rationale\n  spanning multiple lines.",
        'decision:\n  "Some decision text."',
    )
    errors = _write_and_validate(tmp_path, text)
    assert any("'decision' value must not be pushed to the next line (PD06)" in e for e in errors)


def test_pd23_decision_context_cap_is_1800(tmp_path: Path) -> None:
    decision_over_the_cap = _decision_block_of_word_groups(word_group_count=19)
    text = VALID_PRD_WITH_ALL_SECTIONS.replace(
        "decision: >\n  Some decision text with rationale\n  spanning multiple lines.\n", decision_over_the_cap
    )
    errors = _write_and_validate(tmp_path, text)
    assert any("field 'decision'" in e and "(max 1800)" in e for e in errors), errors


def test_pd23_decision_within_the_documented_target_and_the_cap_is_allowed(tmp_path: Path) -> None:
    decision_past_the_documented_target_but_within_the_cap = _decision_block_of_word_groups(word_group_count=17)
    text = VALID_PRD_WITH_ALL_SECTIONS.replace(
        "decision: >\n  Some decision text with rationale\n  spanning multiple lines.\n",
        decision_past_the_documented_target_but_within_the_cap,
    )
    errors = _write_and_validate(tmp_path, text)
    assert not any("field 'decision'" in e and "max" in e for e in errors), errors


def test_pd09_consequences_requires_negative(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace("  negative:\n    costs_something: Costs Y.\n", "")
    errors = _write_and_validate(tmp_path, text)
    assert any("consequences.negative must be a non-empty mapping (PD09)" in e for e in errors)


def test_pd10_key_too_many_words(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace("do_one_thing", "this_key_has_too_many_words")
    errors = _write_and_validate(tmp_path, text)
    assert any("max 4" in e and "consequences.positive" in e for e in errors)


def test_pd10_key_within_the_documented_target_and_the_word_cap_is_allowed(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace("do_one_thing", "this_key_four_words")
    errors = _write_and_validate(tmp_path, text)
    assert not any("consequences.positive" in e and "max 4" in e for e in errors), errors


def test_pd10_key_must_be_english_snake_case(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace("keep_it_simple", "keepItSimple")
    errors = _write_and_validate(tmp_path, text)
    assert any("English lowercase snake_case" in e for e in errors)


def test_pd11_flow_must_be_a_list(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace("flow:\n  - step_one — does a thing", "flow:\n  step_one: does a thing")
    errors = _write_and_validate(tmp_path, text)
    assert any("'flow' must be a list" in e for e in errors)


def test_pd12_components_must_be_a_mapping(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace(
        "components:\n  runner: runner-claude-full", "components:\n  - runner-claude-full"
    )
    errors = _write_and_validate(tmp_path, text)
    assert any("'components' must be a mapping" in e for e in errors)


def test_pd16_discovered_issue_requires_status(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace("    status: resolved\n", "")
    errors = _write_and_validate(tmp_path, text)
    assert any("discovered_issues.side_bug is missing required key: status" in e for e in errors)


def test_pd17_rejected_alternatives_must_be_last_before_history(tmp_path: Path) -> None:
    text = _move_rejected_alternatives_before_rules(VALID_PRD_WITH_ALL_SECTIONS)
    errors = _write_and_validate(tmp_path, text)
    assert any("out-of-order key 'rules'" in e for e in errors)


def test_pd18_history_keys_must_increase(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace(
        "history:\n  v1: initial decision\n  v2: second revision\n",
        "history:\n  v2: second revision\n  v1: initial decision\n",
    )
    errors = _write_and_validate(tmp_path, text)
    assert any("must strictly increase in order" in e for e in errors)


def test_pd18_history_key_must_look_like_vn(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace("v1: initial decision", "first: initial decision")
    errors = _write_and_validate(tmp_path, text)
    assert any("key must look like v1, v2" in e for e in errors)


def test_pd18_history_entry_cap_is_200(tmp_path: Path) -> None:
    entry_over_200_chars = "a" * 201
    text = VALID_PRD_WITH_ALL_SECTIONS.replace("initial decision", entry_over_200_chars)
    errors = _write_and_validate(tmp_path, text)
    assert any("history.v1" in e and "(max 200)" in e for e in errors)


def test_pd19_unknown_top_level_key_rejected(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS + "\nbogus_section: not a real section\n"
    errors = _write_and_validate(tmp_path, text)
    assert any("unknown top-level key: 'bogus_section'" in e for e in errors)


def test_pd21_empty_optional_section_rejected(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace("flow:\n  - step_one — does a thing\n\n", "flow: []\n\n")
    errors = _write_and_validate(tmp_path, text)
    assert any("key 'flow' has an empty value" in e for e in errors)


def test_pd21_required_section_emptied_is_also_rejected(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace(
        "rules:\n  keep_it_simple: >\n    Always do Y, never Z.\n", "rules: {}\n"
    )
    errors = _write_and_validate(tmp_path, text)
    assert any("key 'rules' has an empty value" in e for e in errors)


def test_pd23_other_field_cap_is_300(tmp_path: Path) -> None:
    invariant_over_300_chars = "a" * 301
    text = VALID_PRD_WITH_ALL_SECTIONS.replace("Always do Y, never Z.", invariant_over_300_chars)
    errors = _write_and_validate(tmp_path, text)
    assert any("rules.keep_it_simple" in e and "301 chars (max 300)" in e for e in errors)


def test_pd23_discovered_issue_cap_is_600(tmp_path: Path) -> None:
    problem_over_600_chars = "a" * 601
    text = VALID_PRD_WITH_ALL_SECTIONS.replace("Found a side bug while verifying the change.", problem_over_600_chars)
    errors = _write_and_validate(tmp_path, text)
    assert any("discovered_issues.side_bug.problem" in e and "(max 600)" in e for e in errors)


def test_pd23_implementation_delta_cap_is_also_600(tmp_path: Path) -> None:
    rename_note_over_600_chars = "a" * 601
    text = VALID_PRD_WITH_ALL_SECTIONS.replace("Renamed A to B during implementation.", rename_note_over_600_chars)
    errors = _write_and_validate(tmp_path, text)
    assert any("implementation_deltas.small_rename" in e and "(max 600)" in e for e in errors)


def test_pd23_folded_block_whitespace_is_normalized(tmp_path: Path) -> None:
    first_line_of_150_chars = "a" * 150
    second_line_of_149_chars = "a" * 149
    payload_folding_to_299_chars = f"rules:\n  keep_it_simple: >\n    {first_line_of_150_chars}\n    {second_line_of_149_chars}\n"
    text = VALID_PRD_WITH_ALL_SECTIONS.replace(
        "rules:\n  keep_it_simple: >\n    Always do Y, never Z.\n", payload_folding_to_299_chars
    )
    errors = _write_and_validate(tmp_path, text)
    assert not any("rules.keep_it_simple" in e and "max 300" in e for e in errors), errors


def test_pd24_line_over_120_chars_rejected(tmp_path: Path) -> None:
    line_of_130_chars = "a" * 130
    text = VALID_PRD_WITH_ALL_SECTIONS.replace("Some problem statement with full mechanics", line_of_130_chars)
    errors = _write_and_validate(tmp_path, text)
    assert any("chars (max 120) (PD24)" in e for e in errors)


def test_pd24_file_over_250_lines_rejected(tmp_path: Path) -> None:
    history_entries_past_the_line_cap = "".join(f"  v{i}: revision number {i}\n" for i in range(3, 220))
    text = VALID_PRD_WITH_ALL_SECTIONS + history_entries_past_the_line_cap
    errors = _write_and_validate(tmp_path, text)
    assert any("lines (max 250) (PD24)" in e for e in errors)


def test_pd26_plan_task_id_reference_rejected(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace("Always do Y, never Z.", "see S-5 for details")
    errors = _write_and_validate(tmp_path, text)
    assert any("PD26" in e for e in errors)


def test_pd26_skill_rule_id_reference_rejected(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace("Always do Y, never Z.", "per rule PD23 of the standard")
    errors = _write_and_validate(tmp_path, text)
    assert any("PD26" in e for e in errors)


def test_pd26_dash_joined_ticket_reference_is_allowed(tmp_path: Path) -> None:
    text = VALID_PRD_WITH_ALL_SECTIONS.replace("Always do Y, never Z.", "see PRODUCT-42094 for context")
    errors = _write_and_validate(tmp_path, text)
    assert not any("PD26" in e for e in errors), errors


def test_pd27_filename_must_be_three_snake_words(tmp_path: Path) -> None:
    errors = _write_and_validate(tmp_path, VALID_PRD_WITH_ALL_SECTIONS, name="prd.yaml")
    assert any("three snake_case words" in e and "(PD27)" in e for e in errors)


def test_pd27_four_word_filename_rejected(tmp_path: Path) -> None:
    errors = _write_and_validate(tmp_path, VALID_PRD_WITH_ALL_SECTIONS, name="one_two_three_four.yaml")
    assert any("three snake_case words" in e and "(PD27)" in e for e in errors)
