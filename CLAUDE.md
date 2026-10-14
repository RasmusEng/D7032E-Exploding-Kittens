# CLAUDE.md — Exploding Kittens (D7032E Home Exam 2026)


## 1. Project context

This is a Java implementation of the card game *Exploding Kittens*, playable in the
terminal and over a network. The goal is a **clean, well-structured implementation** that:

- implements rules 1–13 of the exam specification correctly,
- is structured for future change (rules 14–17: expansions, menu, bots, decoupled network),
- satisfies the quality attributes **modifiability, extensibility, testability** (req 18–19)
  and, as a bonus, **concurrency** (req 20, the Nope interrupt window),
- follows **SOLID** principles and **Booch's metrics** (high cohesion, low coupling,
  sufficiency, completeness, primitiveness).

## 2. Ground rules for the agent

1. **The student must be able to defend every design decision in an oral exam.**
   When you introduce a class, interface or pattern, state *why* in one or two sentences
   (in the PR/commit message or in `docs/design-notes.md`). Prefer the simplest design
   that meets the requirements — no speculative abstractions.
2. **Do not add features beyond rules 1–17** without asking. Expansions (14), menu (15)
   and bots (16) must be *possible to add* without modifying existing classes; they do
   not all need to be implemented.
3. **Every rule 1–13 gets at least one unit test**, named after the requirement
   (e.g. `rule03a_twoPlayers_deckHasTwoDefuse()`).
4. **No game logic in I/O classes, no I/O in game logic.** The engine never calls
   `System.out`, `Scanner` or sockets directly.
5. **Determinism in tests**: randomness, time and player input are always injected.
6. Small, focused changes. Run the full test suite before considering a task done.
7. Only dependency allowed besides the JDK: **JUnit 5** (test scope).

## 3. Tech stack & commands

- **Java 21** (LTS). No build tool: plain `javac` + JUnit 5 console launcher.
- Layout: production code in `src/main/java/ek/...`, tests in `src/test/java/ek/...`,
  ServiceLoader registrations in `src/main/resources` and `src/test/resources`,
  JUnit jar at `lib/junit-platform-console-standalone.jar` (committed). Output goes to `out/` (gitignored).
- The local JDK is newer than 21, so **always compile with `--release 21`**. That rejects
  newer APIs and syntax such as `void main()` and `IO.println`. Use a classic
  `public static void main(String[] args)`.

Commands (bash, from the project root):

```bash
# Build (the cp copies META-INF/services, which registers expansions and variants)
javac --release 21 -d out/classes $(find src/main -name '*.java')
cp -R src/main/resources/. out/classes/

# Compile tests (after Build)
javac --release 21 -d out/test-classes -cp out/classes:lib/junit-platform-console-standalone.jar \
  $(find src/test/java -name '*.java')
cp -R src/test/resources/. out/test-classes/

# Run all tests
java -jar lib/junit-platform-console-standalone.jar execute \
  --class-path out/classes:out/test-classes --scan-class-path

# Single test class / single method: replace --scan-class-path with
#   --select-class ek.expansion.DeckFactoryTest
#   --select-method 'ek.rules.RulesComplianceTest#rule03a_twoPlayers_deckHasTwoDefuse'

# Run (numPlayers = human players incl. the host, the rest join as clients; 2–5 players in total)
java -cp out/classes ek.app.Main server                          # setup menu: expansions, variants, players
java -cp out/classes ek.app.Main server <numPlayers> <numBots>   # base game only, no menu
java -cp out/classes ek.app.Main client <ip>      # joins a server on TCP port 2048
java -cp "out/classes:expansions/*" ek.app.Main server           # menu with expansion jars from expansions/

# Scripts: ./test = clean build + all tests; ./run [<numPlayers> <numBots> | client <ip>]
```

"Run the full test suite" (rule 6 in §2) means all three steps: build, compile tests, run tests.
A stale `out/` can hide compile errors, so after deleting or renaming a class, clean with `rm -rf out` first.

Keep this section in sync with the README — the examiner must be able to compile,
run and test from written instructions alone.

## 4. Target architecture (base — refine as the design evolves)

```
ek/
├── app/          Main (composition root, the only place with `new` for services and the only ExpansionRegistry user),
│                 CommandLine (argument parsing), GameAssembler (GameConfig → deck, effects, engine)
├── model/        Card, CardType, Deck, Hand, Player, DiscardPile          — pure data + invariants
├── rules/        GameSetup, TurnState, GameConfig                          — rule logic, no I/O
├── cards/        one CardEffect implementation per card                    — Attack, Skip, Favor, ...
├── combos/       ComboEffect: TwoOfAKind, ThreeOfAKind
├── expansion/    GameOption, Expansion, GameVariant (interfaces), BaseGame, FasterGame, DeckFactory,
│                 PlayerRange, ExpansionRegistry (ServiceLoader)            — card sets, variants + deck setup
├── engine/       GameEngine, GameState, GameContext, CardEffect (interface), PlayResolver, NopeResolver, GameEvent — orchestrates a game
├── player/       PlayerController (interface), BotController, GameView, TurnAction (ConsoleController in ui/, RemoteController in net/)
├── net/          Message (typed protocol), MessageChannel, ObjectStreamChannel, RemoteController, GameClient, GameServer — transport only
├── ui/           ConsoleView, ConsoleController, ConsoleMenu, Dashboard    — rendering & input parsing
└── util/         Randomizer, Clock/Timer abstractions (injectable)
```

### Key abstractions

| Abstraction | Responsibility | Why (principle) |
|---|---|---|
| `CardEffect` | `apply(GameContext ctx, Player actor)` for one card type or combo; interface in `engine/`, implementations in `cards/`/`combos/` | OCP: new cards = new classes, engine unchanged; DIP: no engine↔cards cycle |
| `Expansion` | Provides card types, counts, effects, setup tweaks | OCP/extensibility: req 14 without touching core |
| `DeckFactory` | Builds deck per rules 3–7 from active expansions | SRP, testable setup |
| `GameConfig` | Validated host choices (humans, bots, expansions, variants); player limit from `PlayerRange.of(expansions)` only | One validation for menu and command line |
| `ExpansionRegistry` | Finds optional expansions and variants via `ServiceLoader`, sorted by `order()`, broken providers reported | Req 14/15 without code changes; Service Locator confined to `Main` |
| `GameVariant` | `afterDeal(deck, randomizer)`, the single variant hook in `GameSetup` | Strategy, OCP |
| `PlayerController` | `chooseAction(view)`, `chooseCardToGive(...)`, `wantsToNope(...)` | DIP + LSP: human, remote, bot interchangeable (req 16) |
| `NopeResolver` (`engine/`) | Runs the timed Nope window, handles Nope/Yup chains | Isolates concurrency (req 20) |
| `GameEventListener` | Engine publishes events (`CardPlayed`, `PlayerExploded`, `Winner`) | Observer: views/network decoupled from rules |
| `Message` types | Typed protocol (`ChooseAction`, `OfferNope`, `Event`, ...), sent over a `MessageChannel` | Req 17: typed messages instead of keyword matching; transport swappable |
| `Randomizer`, `Clock` | Shuffling, random start player, timeouts | Testability (req 19) via DI |

### Communication flow (one turn)

1. `TurnLoop` asks current `PlayerController` for an action (given a read-only `GameView`).
2. Action validated by `GameRules`; card moved to `DiscardPile`.
3. `NopeResolver` opens the Nope window for all *other* eligible controllers (parallel, time-boxed).
4. If the effect is not cancelled (even number of Nopes), `CardEffect.apply(...)` runs.
5. Engine publishes events → console view / network layer render them.
6. Pass → draw; ExplodingKitten → Defuse or explode; turn counter updated (Attack stacking).

## 5. Game rules (exam specification)

This is the authoritative specification. When it conflicts with anything else in this
file, the specification wins.

### Rules (must be implemented and tested)

1. There can be between 2 and 5 players.
2. Each player should be given 1 Defuse card.
3. The deck should consist of the following cards before the first hands are dealt:
    - a. 2 Defuse cards for 2–4 players, 1 Defuse card for 5 players
    - b. 4 Attack
    - c. 4 Favor
    - d. 5 Nope
    - e. 4 Shuffle
    - f. 4 Skip
    - g. 5 SeeTheFuture
    - h. 4 HairyPotatoCat
    - i. 4 Cattermelon
    - j. 4 RainbowRalphingCat
    - k. 4 TacoCat
    - l. 4 OverweightBikiniCat
4. Shuffle the deck before the first hands are dealt.
5. Deal 7 cards from the deck to each player.
6. Insert enough ExplodingKitten cards into the deck so there is 1 fewer than the number of players.
7. Shuffle the deck before the game starts.
8. A random player goes first.
9. On a player's turn the player can either Pass or Play one or several cards.
    - a. Pass ends the turn and the player draws a card.
    - b. A player can play as many or as few cards as desired on their turn.
10. If an ExplodingKitten is drawn, the player must play a Defuse card if one is on hand, or explode.
    - a. A defused ExplodingKitten is inserted back into the deck at the location chosen by the surviving player.
    - b. An exploded player discards their hand, including the ExplodingKitten, and takes no further turns.
11. Playing a card:
    - a. **Attack** ends the turn without drawing. The next player takes 2 turns in a row, unless
      that player plays another Attack, in which case the next player must take an additional
      2 turns (e.g. 4 turns, then 6, etc.).
    - b. **Favor** forces another player to give you 1 card of *their* choice.
    - c. **Shuffle** shuffles the deck.
    - d. **Skip** ends your turn without drawing (only 1 turn if you have multiple turns to take).
    - e. **SeeTheFuture** lets the player view the top 3 cards of the deck.
    - f. **Two of a kind** (any two cards with the same name) lets the player steal a random card from another player.
    - g. **Three of a kind** (any three cards with the same name) lets the player name a card to receive from another player (if they have it).
12. A **Nope** can be played within 5 seconds of any card(s) defined in rules 11 and 12 to stop their action.
    - a. A Nope played on another Nope alternates its meaning (Nope ↔ Yup).
13. When all but one player have exploded, the remaining player is announced as the winner to all players.

### Clarifications from the official rulebook

Use these where the specification is silent; don't contradict the specification.

- Nope cannot stop an ExplodingKitten or a Defuse. It can be played on combos, and by any
  player, even when it is not their turn. Noped cards stay in the discard pile.
- Combos ignore the cards' own effects; only the combo action happens.
- Three of a kind: if the target doesn't have the named card, the player gets nothing.
- Attack: the victim takes each turn normally (play-or-pass, then draw). Stacked Attacks
  pass the victim's *remaining* turns plus 2 to the next player.
- Skip used against an Attack ends only one of the turns; two Skips end both.
- SeeTheFuture is private to the player and does not reorder the deck.
- Defuse: the player may put the kitten anywhere in the deck, including on top.
- There is no maximum or minimum hand size; an empty hand still plays on.
- Play continues clockwise (in seat order).

### Future modifications (design for, implement only on request)

14. Support expansions such as Imploding Kittens, Streaking Kittens and Barking Kittens
    (excluding physical gimmicks like the cone of shame or the tower of power crown).
15. Support a menu for selecting expansions or other game variants.
16. Support bots that can join the game instead of players.
17. Decouple network functionality from game functionality so they can vary independently;
    the client must not rely on hard-coded keywords.

### Additional requirements

18. The architecture must be **modifiable and extensible**, so 14–17 are easy to add later.
19. The software must be **testable** (and to some extent debuggable): game rules and
    individual game phases must be testable via dependency injection.
20. *Bonus:* manage **concurrency** and interrupts (the timed Nope window).

## 6. Rules checklist (spec rules 1–13)

Each must be implemented in `rules/`, `cards/`, `expansion/` or `engine/` and covered by tests.

- [x] R1  2–5 players (validate, reject otherwise)
- [x] R2  Each player gets 1 Defuse
- [x] R3  Base deck composition (3a: 2 Defuse for 2–4 players, 1 for 5)
- [x] R4  Shuffle before dealing
- [x] R5  Deal 7 cards each
- [x] R6  Insert (players − 1) ExplodingKittens
- [x] R7  Shuffle before game starts
- [x] R8  Random start player
- [x] R9  Play any number of cards, or Pass (= draw, end turn)
- [x] R10 Draw kitten → Defuse (reinsert at chosen, validated index) or explode (discard hand, out of game)
- [x] R11 a Attack (stacking +2), b Favor, c Shuffle, d Skip (ends one turn only), e SeeTheFuture (handle < 3 cards), f Two-of-a-kind (uniform random steal), g Three-of-a-kind
- [x] R12 Nope within 5 s of any R11 action; Nope on Nope toggles (Nope/Yup)
- [x] R13 Last player alive is announced as winner to everyone

When a rule's behaviour is unclear or a bug is suspected, write a failing test that
captures the spec first, then fix the code until it passes.

## 6. Testing conventions

- Location: `src/test/java/ek/...`, mirroring the main package.
- One test class per production class; rule tests may live in `RulesComplianceTest`.
- Use **fakes, not mocks**: `ScriptedController` (pre-programmed answers),
  `FixedRandomizer`, `ManualClock`. No real sockets or real time in unit tests.
- Tests must be able to put the game in an arbitrary state (custom deck order, hands,
  turn counter) via builders, e.g. `GameStateBuilder`.
- Expansions must be testable in isolation before being registered.

## 7. Coding conventions

- Classes ≤ ~200 lines, methods ≤ ~30 lines; if larger, split by responsibility.
- Interfaces named by role (`PlayerController`), not `IPlayer`.
- No `public static` mutable state. No magic strings for protocol or card names — use enums/types.
- Model objects expose behaviour, not raw mutable lists (`hand.remove(type)`, not `hand.getCards().remove(...)`).
- Exceptions: validate input at the boundary (`ui`, `net`), throw domain exceptions
  (`InvalidMoveException`) from rules, never swallow exceptions silently.
- Javadoc on every public interface and on non-obvious public methods.
- Concurrency confined to `NopeResolver`, `ConsoleController` (Nope prompt) and `net/`; document thread-safety on those classes.

## 8. Documentation to keep current

- `README.md` — build, run, test instructions.
- `docs/design-notes.md` — decisions + motivation (SOLID/Booch/pattern), one short entry per decision.
- `docs/diagrams/` — class diagram and turn sequence diagram (update when structure changes).

## 9. Open decisions (fill in as they are made)

- [x] Build tool: plain `javac` + JUnit 5 console launcher, Java 21 (see §3)
- [x] Server-hosted local player kept: the host plays at its console, the other humans join as clients (`ek.net`)
- [ ] Which expansion (if any) to implement as proof of extensibility
- [x] Req 15 menu: `server` opens `ui.ConsoleMenu` (expansions → variants → humans/bots); `server <p> <b>`
      is a base-game-only shortcut that never uses the registry. Both produce a `rules.GameConfig`
- [x] Expansion discovery: `ServiceLoader` inside `expansion.ExpansionRegistry` only, called only by `Main`;
      `BaseGame` is never looked up and always first, the rest are sorted by `GameOption.order()`;
      broken providers are skipped and shown in the menu. Registrations live in `src/*/resources/META-INF/services`
- [x] Player limit: defined once in `expansion.PlayerRange.of(expansions)` (2–5 today)
- [x] Variants: `GameVariant.afterDeal` is the only hook (after dealing, before the kittens); `FasterGame` implemented
- [x] Assembly: `app.GameAssembler` builds deck/effects/engine from a `GameConfig`; `Main` only does I/O wiring
- [x] Bot strategy scope: random legal moves (`player/BotController`, injected `Randomizer`)