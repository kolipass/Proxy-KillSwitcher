---
name: tester
description: |
    Writes, modifies, and debugs unit and integration tests.
    Auto-invocation: any work with *Test.kt files or */test/* directories.
---

## Sub-skills

Orchestrator only — no rules here; each rule lives in a sub-skill below.
Before any testing task, read every sub-skill whose `when` matches and follow its rules.

```yaml
stack:
    unit: JUnit4, MockK, Turbine, kotlinx-coroutines-test, orbit-test
    instrumentation: not yet set up in this project — no androidTest source set or instrumentation deps exist yet

sub_skills:
  - name: tester/naming
    path: .skills/tester/naming/SKILL.md
    when: naming sut, actual, expected, setUp, test names

  - name: tester/class_structure
    path: .skills/tester/class_structure/SKILL.md
    when: field order, blank lines, method order, nested types

  - name: tester/mockk
    path: .skills/tester/mockk/SKILL.md
    when: every/coEvery, just runs, any, slot, verify, mockkObject

  - name: tester/coroutines
    path: .skills/tester/coroutines/SKILL.md
    when: suspend tests, TestDispatcher, TestScope, advanceUntilIdle, verifyOrder

  - name: tester/assertions
    path: .skills/tester/assertions/SKILL.md
    when: assertTrue, assertEquals, assertNull, assertInstanceOf, assertThrows

  - name: tester/generators
    path: .skills/tester/generators/SKILL.md
    when: test data with three or more fields or enum lists

  - name: tester/helper_functions
    path: .skills/tester/helper_functions/SKILL.md
    when: enableAllConditions, grouping three or more stubs into helper

  - name: tester/companion_object
    path: .skills/tester/companion_object/SKILL.md
    when: test constants, private const val, numeric type suffixes

  - name: tester/orbit
    path: .skills/tester/orbit/SKILL.md
    when: ViewModel test, runActionAndCheckResultState, expectState, Orbit MVI

  - name: tester/flow_testing
    path: .skills/tester/flow_testing/SKILL.md
    when: Flow, StateFlow, flowOf, first, stateFlow value, collect in launch

  - name: tester/cleanup
    path: .skills/tester/cleanup/SKILL.md
    when: tearDown, unmockkAll, cleanup order

  - name: tester/parameterized
    path: .skills/tester/parameterized/SKILL.md
    when: ParameterizedTest, MethodSource, List of Arguments

  - name: tester/lazy_init
    path: .skills/tester/lazy_init/SKILL.md
    when: lazy for sut, lateinit var, sut depends on setUp stubs
```
