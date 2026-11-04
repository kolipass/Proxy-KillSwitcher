---
name: process-delivery
description: |
    Publishes changes via Pull Request / Merge Request.
    Trigger: `/process-delivery` or auto-invoked upon completion of code or documentation changes.
    Creates a pull request with a mandatory concise title and clear structured description of incoming changes.
tools: Read, Edit, Write, Bash.
---

## Role

Delivery owns the final step of publishing changes:
- Ensures all work is committed to a dedicated feature branch (`feature/<name>`, `fix/<name>`).
- Direct commits and pushes to `main` are strictly forbidden.
- Pushes the branch to remote `origin`.
- Creates a Pull Request targeting `main` with:
  1. A concise, clear title (e.g. `feat(tunnel): add fallback dns resolution`).
  2. A structured description explaining what came in the PR:
     - **Что изменилось (Summary)**: bullet points describing what changed.
     - **Зачем (Rationale)**: context/motivation for the change.
     - **Проверка (Verification)**: list of checks performed (`./gradlew ktlintCheck detekt test`).

## Required Algorithm

```yaml
algorithm:

    -   step: 0
        name: verify_branch
        actions:
            -   id: 1
                action: Check current branch (`git branch --show-current`)
                rule: MUST NOT be `main` or `master`. If on `main`, checkout a dedicated branch before committing.
            -   id: 2
                action: Run stack verification (`./gradlew ktlintCheck detekt test`) and pre-commit checks

    -   step: 1
        name: commit_and_push
        actions:
            -   id: 1
                action: Stage and commit changes with a concise descriptive commit message
            -   id: 2
                action: Push branch to origin (`git push -u origin <branch-name>`)

    -   step: 2
        name: create_pull_request
        actions:
            -   id: 1
                action: Compose a concise PR title
                format: "<type>(<scope>): <short imperative description>"
                example: "feat(killswitch): add auto reconnect after network restore"
            -   id: 2
                action: Compose structured PR description
                sections:
                    - "### Что изменилось" (bullet points of incoming changes)
                    - "### Зачем" (motivation or linked issue/PRD)
                    - "### Проверка" (verification commands and status)
            -   id: 3
                action: Create Pull Request
                execution: >
                    If GitHub CLI (`gh`) or API token is available, create PR directly via `gh pr create --title "..." --body "..."`.
                    Otherwise, output a direct pre-filled compare/PR URL:
                    `https://github.com/<owner>/<repo>/compare/main...<branch>?expand=1&title=<encoded_title>&body=<encoded_body>`.
            -   id: 4
                action: Present the PR URL and incoming changes summary in the final response to the user
```
