---
name: process-scout
description: |
    Guides a problem discussion from multi-agent research to an approved PRD.
    Trigger: `/process-scout` — always activates this skill.
    Auto-invocation: any request to research a problem or discuss a task for a future PRD.
tools: Read, Write, GitNexus, Grep, Glob, Exa Search.
---

## Role

Scout runs in the main session — never as a background agent — because every phase below
depends on asking the user questions. Sub-agents are used only for parallel research (step 2).
The only artifact of the whole flow is one PRD file conforming to `.cursor/skills/docs/prd/SKILL.md`.
No code, no Implementation Plan, no other files.

## Required Algorithm

```yaml
algorithm:

    -   step: 0
        name: codebase_reconnaissance
        note: >
            mandatory for every task — any task ends in code, so the affected area
            must be known before any solution is compared; an empty result
            ("nothing related found") is itself a finding and goes into the digest
        actions:
            -   id: 1
                action: Explore the problem area with GitNexus MCP and determine the affected area
                note: >
                    what has to come back: the flows and symbols the problem touches, and the
                    blast radius of changing them
            -   id: 2
                action: >
                    If research needs to look at a different project, check it with GitNexus MCP first — list_repos,
                      then every call scoped to that repo — before reaching for Glob/Grep there
                note: Which sibling project to open is decided by "Related repositories" in .claude/CLAUDE.md
            -   id: 3
                action: If GitNexus MCP tools are unavailable — report it to the user and STOP the flow
                note: never fall back to grep; resume only after the user fixes MCP
            -   id: 4
                action: Summarize findings into a reconnaissance digest
                note: the digest goes verbatim into every research brief in step 2

    -   step: 1
        name: pick_research_agents
        note: >
            any agent doing internet research (this step's agents and step 2) uses only the
            Exa Search MCP tools (mcp__exa__web_search_exa / mcp__exa__web_fetch_exa)
        actions:
            -   id: 1
                action: >
                    Present the user a checklist (multi-select) of ALL research agents —
                    never a binary 2-or-4 choice; the full list is always shown
                full_list:
                    agent_best_practice: industry-canonical, battle-tested solution
                    agent_conservative: builds on the current project and its already-applied practices
                    agent_innovator: recent approaches and tools; active web search
                    agent_nihilist: counters the framing, removes the problem instead of solving it
            -   id: 2
                action: >
                    Recommend the optimal subset for THIS specific task and pre-check it in the
                    list, with a one-line reason per recommended agent; still show every agent
                    so the user can tick any combination
            -   id: 3
                action: Launch exactly the agents the user checked — no more, no less

    -   step: 2
        name: parallel_research
        actions:
            -   id: 1
                action: Launch the chosen set of research sub-agents in parallel
                mandates:
                    agent_best_practice: the industry-canonical, battle-tested solution
                    agent_conservative: >
                        builds on the current project and its already-applied practices;
                        reuses the step 0 digest as its primary source
                    agent_innovator: recent approaches and tools; active web search required
                    agent_nihilist: >
                        always counters — attacks the problem framing, eliminates the problem
                        instead of solving it (different architecture level, dropping a component,
                        changing the process)
            -   id: 2
                action: Give every launched agent the same brief
                brief: problem statement + step 0 digest + report_contract
            -   id: 3
                action: Enforce the report contract on every returned report
                report_contract:
                    - approach_essence
                    - strengths
                    - weaknesses
                    - implementation_cost
                    - risks
        constraints:
            - the returned solutions must differ from each other
            - research agents never edit files and never ask the user
            - never launch agents without the step 0 digest in the brief

    -   step: 3
        name: synthesis_through_questions
        actions:
            -   id: 1
                action: Compare the returned reports and identify the strongest elements of each
            -   id: 2
                action: For every disputed element ask the user a question
                note: never decide silently
                question_contract:
                    - name the element being decided — one line, in the project's own terms
                    - >
                        per option: what it actually is, what changes in the code, what it costs
                        to build and to live with
                    - state the trade-off that separates the options — the reason this is a choice at all
                    - give your recommendation and the reason for it
                anti_pattern: >
                    "take X from best_practice or Y from optimal?" — the agent's labels are not
                    the substance; the user cannot choose between names they cannot unpack
            -   id: 3
                action: Record every choice AND the reason the alternatives were rejected
                note: these become decision and rejected_alternatives in the PRD
            -   id: 4
                action: Assemble the answers into one composed approach
                note: approaches are never taken wholesale — only cherry-picked parts

    -   step: 4
        name: discussion_and_approve
        actions:
            -   id: 1
                action: Lay the composed approach out in full before asking for approval
                note: >
                    a summary is the right form, but not two sentences — every decision gets a
                    line or two of substance: what it changes in the code, what it replaces
            -   id: 2
                action: Discuss it until the user explicitly approves
            -   id: 3
                action: No PRD before approve — ask "пишем PRD?" and accept ONLY an explicit "да" to it

    -   step: 5
        name: write_prd
        actions:
            -   id: 1
                action: Re-read .cursor/skills/docs/prd/SKILL.md and two_layer_prd.yaml
                note: immediately before writing — never from memory
            -   id: 2
                action: Scout writes the PRD itself, never delegates it
                note: rejection rationale must reach the file without a lossy handoff
            -   id: 3
                action: Build the target path as .claude/prd/<MMDD>-<branch_number>-<three_words>/<three_words>.yaml
                path_parts:
                    MMDD: today's month and day, zero-padded — 0807 for the 7th of August
                    branch_number: the digits of the current git branch — 31 for AIDEV-31
                    three_words: exactly three snake_case words, naming the package and the file alike (PD27)
                example: .claude/prd/0807-31-three_words_name/three_words_name.yaml
            -   id: 4
                action: Show the assembled path and ask the user to confirm the PRD goes exactly there
                note: no writing before the confirmation — a wrong package is a PRD the planner never finds
            -   id: 5
                action: Run python3 .cursor/skills/docs/tools/validate_prd.py <the exact PRD path>
                note: >
                    direct path, not pytest — discover_prds() misses nested .claude/prd/<slug>/
                    files. On a cap violation, cut exactly (actual − max) chars from that field
                    in one edit, not by guessing.
            -   id: 6
                action: Verify the PRD is self-sufficient
                note: a planner agent without this conversation must be able to produce an Implementation Plan from it
```
