---
name: implement-rule
description: Implement one exam rule (R1–R13 from CLAUDE.md §5) test-first, then update the checklist and design notes. Use when asked to implement or fix a specific rule.
disable-model-invocation: true
---

Implement rule **$ARGUMENTS** of the Exploding Kittens exam spec.

1. **Locate the rule.** Read the rule text in CLAUDE.md §5. If `legacy/ExplodingKittens.java` exists, read how the legacy code handles it (reference only, never edit or copy its structure) and note any rule violations.
2. **Write the failing test first.** Put it in `src/test/java/ek/...`, mirroring the production package, or in `RulesComplianceTest`. Name it after the requirement, e.g. `rule03a_twoPlayers_deckHasTwoDefuse()`. Inject randomness, time and input with fakes (`FixedRandomizer`, `ManualClock`, `ScriptedController`, `GameStateBuilder`), not mocks. Use no real sockets or sleeps. Cover edge cases the rule implies (e.g. R11e with fewer than 3 cards, R1 rejecting 1 or 6 players).
3. **Run it and confirm it fails** for the right reason: build, compile tests, then run with `--select-class <TestClass>` (commands in CLAUDE.md §3).
4. **Implement the smallest change** in the right package (`rules/`, `cards/`, `combos/`, `expansion/`, `engine/`). Follow CLAUDE.md §2 and §7: no I/O in game logic, keep classes and methods small, use enums and no magic strings, and add a new `CardEffect` class instead of growing a switch.
5. **Run the full suite** using the build → compile tests → run all tests commands in CLAUDE.md §3. All tests must pass.
6. **Update the docs:**
   - Tick the rule's box in CLAUDE.md §5.
   - If you introduced a class, interface or pattern, add one short entry to `docs/design-notes.md` saying what it is and why, citing the SOLID or Booch principle or the pattern.
7. **Report briefly:** what changed, which tests cover the rule, and any design decision the student must be able to defend.
