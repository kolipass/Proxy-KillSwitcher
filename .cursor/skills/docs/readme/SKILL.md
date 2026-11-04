---
name: docs/readme
description: Canonical readme.yaml standard for project documentation.
---

## Purpose

readme.yaml is the single file a developer should read to understand the whole feature: why it exists, how to
install it, how to update it (if needed), and how to start using it. It's a short, plain-language description
with a concrete sequence of commands — no need to read the feature's code.

```yaml
rules:

    - id: RM01
      rule: First key must be pipeline title
      do: |
        pipeline: "Полная автоматизация обработки задач"

    - id: RM02
      rule: Optional version key may follow pipeline
      do: |
        pipeline: "Voice Dictation (Inline)"
        version: "3.1"

    - id: RM03
      rule: Never use a top-level trigger key — invocation belongs in usage
      do: |
        usage:
          step_01: "Нажать ctrl+alt+i — откроется терминал с пайплайном"
      dont: |
        hotkey: "ctrl+alt+i"
        # или command: "sync.sh" — такого поля больше нет, переносить в usage

    - id: RM04
      rule: Separate top-level sections with dashed line
      do: |
        pipeline: "..."
        # -------------------------------------------------------------------
        what_it_does:

    - id: RM05
      rule: Follow canonical section order
      do: |
        pipeline -> version? -> what_it_does -> components -> files -> setup
        -> pipeline_steps -> *_detection -> start -> usage
        -> rejected_alternatives -> troubleshooting

    - id: RM06
      rule: what_it_does is required bullet list
      do: |
        what_it_does:
          - "Читает план задач"
          - "Запускает связанные сервисы"

    - id: RM07
      rule: components splits docker and host tools
      do: |
        components:
          docker_services: [{name: n8n, role: "оркестрация"}]
          host_tools: ["bash  # выполняет скрипты"]

    - id: RM08
      rule: setup and pipeline_steps are optional ordered maps
      do: |
        setup:
          once_per_machine:
            command: "sudo ln -s /home/user/aisystem/scripts/sync.sh /usr/local/bin/sync-skills"
            note: "добавляет sync-skills в PATH"
        pipeline_steps:
          1_fetch: "Получить данные"

    - id: RM09
      rule: Detection sections use *_detection name
      do: |
        project_detection: "Ищет файл project.json"
        plan_file_detection: "Ищет файл плана через путь, скопированный в CLIPBOARD"

    - id: RM10
      rule: start section describes bring-up and health checks
      do: |
        start:
          docker:
            command: "docker compose up -d"
            check: "docker compose ps"

    - id: RM11
      rule: usage is a required ordered step_NN map
      do: |
        usage:
          step_01: "Открыть терминал"
          step_02: "Выполнить команду"

    - id: RM12
      rule: troubleshooting entries use symptom diagnose fix note
      do: |
        troubleshooting:
          server_not_responding:
            symptom: "Connection refused на localhost:8080"
            diagnose:
              - "curl -sf http://localhost:8080/health"
              - "docker logs aisystem-whisper-1"
            fix:
              - "cd ~/aisystem && docker compose up -d"
              - "curl -sf http://localhost:8080/health"
            note: "диагноз и фикс могут быть строкой или списком"

    - id: RM13
      rule: files is required label to path map
      do: |
        files:
          config: "n8n/full_automation/config.json"
          script: "n8n/full_automation/run.sh"

    - id: RM14
      rule: Russian prose, English keys/paths/commands
      do: |
        what_it_does:
          - "Читает план задач"                 # содержимое — русский
        files:
          script: "scripts/run.sh"              # пути, идентификаторы — английский
        start:
          command: "docker compose up -d"       # команды shell — дословно

    - id: RM15
      rule: Never reference PRD task IDs (S-N)
      do: |
        "ручной override без изменений"
      dont: |
        "ручной override (см. S-5)"
        # a PRD is a one-off planning artifact, not a durable
        # cross-reference target — describe the resulting behavior instead

    - id: RM16
      rule: No physical line exceeds 120 characters, counted from column 0
      do: |
        files:
          script: "scripts/run.sh"  # короткая строка укладывается в лимит
        # reflow longer prose at word boundaries onto a continuation line
```
