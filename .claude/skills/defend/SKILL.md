---
name: defend
description: Oral-exam prep. Explain and justify a class, interface or design decision in this codebase using SOLID, Booch's metrics and the quality attributes, then list likely examiner questions with answers. Use when the student asks why something is designed a certain way or wants to rehearse for the oral exam.
---

Help the student defend **$ARGUMENTS** in the D7032E oral exam.

1. Read the relevant code, its tests and any entry in `docs/design-notes.md`. Base everything on the actual code, not on the target architecture in CLAUDE.md if the two differ. If they differ, point that out.
2. Explain in plain language:
   - **Responsibility:** what the class or decision does, in one sentence.
   - **Principles:** which SOLID principles it supports, with concrete evidence from the code (e.g. "adding a new card = a new `CardEffect` class; `GameEngine` is unchanged → OCP").
   - **Booch's metrics:** coupling, cohesion, sufficiency, completeness and primitiveness. Be honest about weak spots.
   - **Quality attributes:** how it supports modifiability, extensibility (rules 14–17) and testability (req 19), and concurrency (req 20) if relevant.
   - **Patterns:** name any pattern used (Strategy, Observer, Factory, etc.) and why it fits better than the simpler alternative.
   - **Comparison to legacy:** what problem in `legacy/ExplodingKittens.java` this solves, if any.
3. List 3–5 likely examiner questions, including critical ones ("why not just use X?", "what would you change to add expansion Y?"), each with a short, honest answer.
4. If the design is hard to defend, say so and suggest a simpler alternative instead of rationalizing it.

Do not modify any files.
