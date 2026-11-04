from __future__ import annotations

from pathlib import Path

import pytest
import validate_readme as vr

README_PATHS = vr.discover_readmes()


@pytest.mark.parametrize(
    "path",
    README_PATHS,
    ids=[str(p.relative_to(vr.REPO_ROOT)) for p in README_PATHS],
)
def test_readme_standard(path: Path) -> None:
    errors = vr.validate_readme(path)
    assert errors == [], "\n".join(errors)


def test_at_least_one_readme_discovered() -> None:
    assert README_PATHS, "no readme.yaml files discovered"
