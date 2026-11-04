from __future__ import annotations

import re
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path

import pytest
import yaml

SKILLS_ROOT = Path(__file__).resolve().parents[2]
CATALOG_PATH = SKILLS_ROOT / "process-tinker" / "skill_catalog.yaml"

YAML_BLOCK_RE = re.compile(r"```yaml\n(.*?)```", re.DOTALL)
RULE_ID_RE = re.compile(r"^([A-Z]+)(\d+)$")
RANGE_RE = re.compile(r"^([A-Z]+)(\d+)-([A-Z]+)(\d+)$")


@dataclass(frozen=True)
class CatalogEntry:
    orchestrator: str
    category: str
    id_range: str
    first: int
    last: int


def _catalog_entries_by_prefix() -> dict[str, CatalogEntry]:
    catalog = yaml.safe_load(CATALOG_PATH.read_text())
    entries: dict[str, CatalogEntry] = {}
    for orchestrator, categories in catalog["meta"]["categories"].items():
        for category, id_range in categories.items():
            match = RANGE_RE.match(id_range)
            assert match, f"{orchestrator}/{category}: malformed id_range {id_range!r}"
            start_prefix, start_num, end_prefix, end_num = match.groups()
            assert start_prefix == end_prefix, (
                f"{orchestrator}/{category}: id_range {id_range!r} mixes prefixes"
            )
            assert start_prefix not in entries, (
                f"prefix {start_prefix} claimed by both "
                f"{entries[start_prefix].category if start_prefix in entries else ''} and {category}"
            )
            entries[start_prefix] = CatalogEntry(
                orchestrator=orchestrator,
                category=category,
                id_range=id_range,
                first=int(start_num),
                last=int(end_num),
            )
    return entries


def _rule_numbers_by_prefix_and_file() -> dict[str, dict[Path, list[int]]]:
    found: dict[str, dict[Path, list[int]]] = defaultdict(lambda: defaultdict(list))
    for skill_file in sorted(SKILLS_ROOT.rglob("SKILL.md")):
        for block in YAML_BLOCK_RE.findall(skill_file.read_text()):
            try:
                data = yaml.safe_load(block)
            except yaml.YAMLError as error:
                raise AssertionError(f"{skill_file}: unparsable yaml block: {error}")
            if not isinstance(data, dict) or not isinstance(data.get("rules"), list):
                continue
            for rule in data["rules"]:
                raw_id = rule.get("id")
                match = RULE_ID_RE.match(str(raw_id))
                assert match, f"{skill_file}: malformed rule id {raw_id!r}"
                prefix, number = match.groups()
                found[prefix][skill_file].append(int(number))
    return found


CATALOG = _catalog_entries_by_prefix()
RULES = _rule_numbers_by_prefix_and_file()


def test_every_rule_prefix_is_catalogued() -> None:
    orphans = {
        prefix: sorted(str(path.relative_to(SKILLS_ROOT)) for path in files)
        for prefix, files in RULES.items()
        if prefix not in CATALOG
    }
    assert not orphans, f"rule prefixes missing from skill_catalog.yaml: {orphans}"


PREFIXES = sorted(CATALOG)


@pytest.mark.parametrize(
    "prefix",
    PREFIXES,
    ids=[f"{CATALOG[p].orchestrator}/{CATALOG[p].category}" for p in PREFIXES],
)
def test_catalog_entry_matches_rules(prefix: str) -> None:
    entry = CATALOG[prefix]
    label = f"{entry.orchestrator}/{entry.category} ({entry.id_range})"

    files = RULES.get(prefix)
    assert files, f"{label}: catalogued but no rule with prefix {prefix} exists"
    assert len(files) == 1, (
        f"{label}: prefix {prefix} spread over "
        f"{sorted(str(path.relative_to(SKILLS_ROOT)) for path in files)}"
    )

    skill_file, numbers = next(iter(files.items()))
    relative = skill_file.relative_to(SKILLS_ROOT)
    assert relative.parts[0] == entry.orchestrator, (
        f"{label}: rules live in {relative}, not under {entry.orchestrator}/"
    )

    assert len(set(numbers)) == len(numbers), f"{label}: duplicate ids in {relative}"
    assert numbers == sorted(numbers), f"{label}: ids out of order in {relative}"
    assert numbers == list(range(numbers[0], numbers[0] + len(numbers))), (
        f"{label}: gap in the id sequence in {relative} — got {numbers}"
    )
    assert (numbers[0], numbers[-1]) == (entry.first, entry.last), (
        f"{label}: catalog says {entry.id_range}, {relative} holds "
        f"{prefix}{numbers[0]:02d}-{prefix}{numbers[-1]:02d}"
    )


def test_scopes_name_existing_skills() -> None:
    catalog = yaml.safe_load(CATALOG_PATH.read_text())
    missing = [scope for scope in catalog["scopes"] if not (SKILLS_ROOT / scope).is_dir()]
    assert not missing, f"scopes without a skill directory: {missing}"
