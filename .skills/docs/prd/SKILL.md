---
name: docs/prd
description: Canonical YAML standard for product requirement documents (PRD).
---

## Purpose

A PRD is a single YAML file with two layers: a short human layer a reviewer reads first and may
stop at, and a detailed agent layer below it. The agent layer carries enough mechanics,
invariants, and rationale for another agent to pick up the task without the original
conversation — this is where the "why" behind a decision lives, not in readme.yaml (which covers
"how to use it") or in the code. The motivation behind the standard's own requirements is spelled
out in two_layer_prd.yaml's decision.

## Reference example

Read `.skills/docs/prd/two_layer_prd.yaml` — the canonical PRD example.

```yaml
rules:

    -   id: PD01
        rule: First key must be pipeline; value is an English title
        do: |
            pipeline: Analytics Repeat-On-Open Fix

    -   id: PD02
        rule: Status is a required enum — proposed, accepted, deprecated, superseded
        do: |
            status: accepted

    -   id: PD03
        rule: Required top-level keys must be present; others stay optional
        do: |
            pipeline -> status -> summary -> context -> decision -> rules -> history

    -   id: PD04
        rule: summary is the human layer with exactly three keys
        do: |
            summary:
              problem: Что именно сломано или чего не хватает, несколькими предложениями
              solution: Что делаем, чтобы это исправить, несколькими предложениями
              tradeoff: Чем жертвуем ради этого решения, несколькими предложениями

    -   id: PD05
        rule: summary values are double-quoted strings, never blocks
        do: |
            problem: "Одно длинное предложение о проблеме, которое не помещается в одну
              физическую строку и продолжается на следующей."
        dont: |
            problem: >
              Свёрнутый блок в summary запрещён — только двойные кавычки.

    -   id: PD06
        rule: Non-summary values are bare, or folded ">" if long
        details: quotes are summary-only; a value containing a colon can't be bare even if short — fold it instead
        do: |
            pipeline: Signup Currency Defaults
            history:
              v1: краткое описание первой версии решения
        dont: |
            pipeline: "Signup Currency Defaults"
            history:
              v1: "краткое описание первой версии решения"

    -   id: PD07
        rule: context and decision carry full agent-layer mechanics
        details: each is capped at 1500 chars, measured after folding whitespace
        do: |
            context: >
              полная механика проблемы: репродукция, цепочка вызовов, доказательства
            decision: >
              полное решение и обоснование, почему выбрано именно оно

    -   id: PD08
        rule: rules maps forward-looking invariants only, not a decision retelling
        do: |
            rules:
              example_invariant: >
                Инвариант в форме «никогда X — вместо этого Y», который обязан соблюдать будущий код.

    -   id: PD09
        rule: consequences is optional but needs both positive and negative
        do: |
            consequences:
              positive:
                example_benefit: Что становится лучше благодаря этому решению, в одном предложении.
              negative:
                example_cost: Чем приходится пожертвовать ради этого решения, в одном предложении.

    -   id: PD10
        rule: Section-mapping keys (rules, consequences, rejected_alternatives, etc.) are 2-3-word snake_case
        do: |
            rejected_alternatives:
              kept_it_simple: Почему рассмотренный вариант отклонён, в одном предложении.
        dont: |
            rules:
              handles_every_edge_case_manually_here: ...

    -   id: PD11
        rule: flow is an optional ordered list of step strings
        do: |
            flow:
              - example_step — короткое описание, что происходит на этом шаге

    -   id: PD12
        rule: components is an optional mapping of named infra items
        do: |
            components:
              example_component: имя или адрес инфраструктурного элемента, который используется

    -   id: PD13
        rule: paths is an optional list of convention strings
        do: |
            paths:
              - соглашение об именовании файла или переменной, которое стоит знать заранее

    -   id: PD14
        rule: setup is an optional list of prerequisite strings
        do: |
            setup:
              - команда или шаг, который нужно выполнить перед началом работы

    -   id: PD15
        rule: implementation_deltas is optional; records decision-vs-implementation drift after the fact
        do: |
            implementation_deltas:
              example_delta: >
                Чем реализация отличается от того, что было решено изначально.

    -   id: PD16
        rule: discovered_issues is optional; entries require status and problem
        do: |
            discovered_issues:
              example_finding:
                status: resolved
                problem: >
                  Побочная находка, обнаруженная во время проверки решения.
                fix: >
                  Как её исправили.

    -   id: PD17
        rule: rejected_alternatives is optional, placed last before history
        do: |
            rejected_alternatives:
              kept_it_simple: Почему рассмотренный вариант отклонён, в одном предложении.

            history:
              v1: ...

    -   id: PD18
        rule: history is a required one-line changelog; put details elsewhere
        do: |
            history:
              v1: краткое описание первой версии решения
              v2: что изменилось в этой версии по сравнению с первой

    -   id: PD19
        rule: Follow canonical top-level key order
        do: |
            pipeline -> status -> summary -> context -> decision -> consequences -> rules
            -> flow -> components -> paths -> setup -> implementation_deltas
            -> discovered_issues -> rejected_alternatives -> history
        file_reference: .skills/docs/prd/two_layer_prd.yaml

    -   id: PD20
        rule: Separate top-level sections with one blank line
        do: |
            pipeline: Signup Currency Defaults

            status: accepted

    -   id: PD21
        rule: Omit optional sections entirely when empty
        do: |
            no flow key at all when there are no steps to record

    -   id: PD22
        rule: Content is Russian; identifiers and keys stay English verbatim
        do: |
            decision: >
              Текст решения на русском; названия классов, файлов и команд остаются
              на английском без перевода.

    -   id: PD23
        rule: Caps — summary/other 300, decision/context 1500, deltas/issues 600, history 200
        do: |
            a field's raw text is whitespace-normalized before comparing it to the cap above

    -   id: PD24
        rule: Line cap 120 incl. indent; file cap 250 lines
        do: |
            rules:
              example_invariant: >
                Инвариант, сформулированный настолько подробно, что естественным
                образом переносится на несколько строк.

    -   id: PD25
        rule: Each fact lives in exactly one section
        do: |
            decision: >
              Что и как решено — полный текст рассуждения.
            rules:
              example_invariant: >
                Детальное требование, вытекающее из decision, сформулировано иначе, не дословно.
        dont: |
            decision: >
              Guard-поле присваивается последним в if-блоке.
            rules:
              guard_after_work: Guard-поле присваивается последним в if-блоке.

    -   id: PD26
        rule: Never cite a rule or task by its id
        do: |
            краткое описание итогового поведения или решения, без ссылки на номер
        dont: |
            ссылка на номер задачи или правила вместо описания результата (см. S-5)

    -   id: PD27
        rule: File name is exactly three snake_case words
        do: |
            refactoring_document_style.yaml
            two_layer_prd.yaml
        dont: |
            prd.yaml
            rootless.yaml
```

- Parsing requires PyYAML (`python3 -c "import yaml"`) — no extra OS package needed.
