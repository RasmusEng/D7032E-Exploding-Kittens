# Design notes

One short entry per decision: what, and why.

## Model layer (`ek.model`)

- **`CardType` is an interface, `BaseCardType` an enum implementing it.**
  Enums give type-safe card names (no magic strings), but a single enum is closed:
  every expansion would have to edit it. With an interface, an expansion adds its own
  enum (e.g. `ImplodingCardType`) and the base game is untouched — Open/Closed (req 14, 18).

- **`Card` is an immutable record.** Cards carry no state besides their type, so value
  equality is enough and they can be shared safely (Booch: primitiveness).

- **`Deck`, `Hand`, `DiscardPile` wrap their list and expose behaviour only**
  (`draw`, `peek`, `insertAt`, `remove(type)`, ...), never the mutable list.
  Invariants (no nulls, valid insert index, draw from empty fails) are enforced in one
  place — high cohesion, encapsulation. Read access is via unmodifiable views.

- **Deck index 0 is the top.** `insertAt(0, …)` puts a defused kitten on top, `insertAt(size(), …)`
  on the bottom (R10a). `peek(n)` returns fewer than `n` cards if the deck is short (R11e).

- **Randomness is injected (`ek.util.Randomizer`).** `Deck.shuffle(Randomizer)` runs
  Fisher–Yates with the injected source, so tests use a lambda (`bound -> 0`) and get a
  known order — Dependency Inversion, testability (req 19). `JavaRandomizer` is the
  production implementation, created only in the composition root.

- **`Player` holds no I/O.** It is a name, a hand and an alive flag; decisions come from a
  `PlayerController` (added later), so human, remote and bot players share one model — SRP.
  `explode()` returns the discarded hand so the caller decides where it goes (the model
  doesn't know about the discard pile — low coupling).

## Game setup, rules 1–7 (`ek.expansion`, `ek.rules`)

- **`Expansion` interface: "which cards go in the deck for N players".** `BaseGame` is just
  one implementation. A new expansion (req 14) is a new class added to the list given to
  `DeckFactory`, with no edits to existing code — Open/Closed. It returns a type→count map
  instead of a list of `Card`s, so counts are easy to read and test against rule 3.

- **`DeckFactory` only builds the unshuffled deck from the expansions.** It doesn't shuffle,
  deal or add kittens — SRP, and it can be tested without any randomness.

- **`GameSetup` (in `rules`) runs rules 1–7 in the spec's order**, one line per rule:
  validate player count, give each player a Defuse, build deck, shuffle, deal 7, insert
  players − 1 kittens, shuffle. It gets `DeckFactory` and `Randomizer` through the constructor
  (Dependency Injection), so tests control both. The players' Defuses are created new, so
  they come on top of the 2 (or 1) in the deck, as rule 3a says.

- **Shuffles are tested by recording the randomizer's calls.** Fisher–Yates on n cards calls
  `nextInt(n)` first, so the recorded bounds prove *when* each shuffle ran and on how many
  cards (48 before dealing, 35 after kittens for 2 players). A randomizer returning
  `bound - 1` swaps nothing, giving a known deck order in other tests.

## Turn engine, rules 8, 9a, 10, 13 (`ek.engine`, `ek.player`, `ek.rules`)

- **`PlayerController` is the only way the engine asks a player anything.** Console,
  network and bot players will implement it, and the engine can't tell them apart —
  Dependency Inversion + Liskov (req 16). It only has the methods needed so far
  (Booch: sufficiency); later rules add methods as they need them.

- **`TurnAction`, `GameView` and `PlayerController` live together in `ek.player`.**
  That package depends only on `ek.model`. The engine depends on `ek.player`, never the
  other way round, so there is no package cycle.

- **`TurnAction` is a sealed interface of records** (`Pass` now; `Play` comes with rule 11).
  The engine `switch`es over it and the compiler reports any action that isn't handled.
  No strings or keywords (req 17).

- **`GameView` is a read-only snapshot**, not the live state. Controllers can't change the
  game or see other players' cards — encapsulation. It's also what the network layer will send.

- **`TurnState` holds only "whose turn, how many turns left".** Moving clockwise, skipping
  exploded players and (later) Attack stacking are tested here on their own (SRP).

- **Events + `GameEventListener` (Observer).** The engine publishes `TurnStarted`,
  `CardDrawn`, `KittenDefused`, `PlayerExploded` and `Winner`, and never prints. A console
  view or the network layer just registers as a listener (CLAUDE.md §2.4). Rule 13
  ("announced to all players") is a `Winner` event sent to every listener.
  `CardDrawn` leaves out the card so other players can't see it.

- **Invalid kitten position → ask again.** Checking a position the player chose is input
  validation, so the engine keeps asking the controller rather than crashing the game.
  A controller that never gives a valid answer would loop forever; real controllers
  (console/bot) are responsible for valid input.

- **Test fakes instead of mocks:** `ScriptedController` (pre-programmed answers),
  `RecordingListener` (collects events) and `GameStateBuilder` (exact hands, deck order and
  start player) put the game in any state with no randomness or real input.

## Playing cards, rules 9b and 11 (`ek.engine`, `ek.cards`, `ek.combos`)

- **`CardEffect` is in `engine`, its implementations in `cards` and `combos`.** Effects need
  the game state (engine), and the engine needs to call effects. If the interface lived in
  `cards`, the two packages would depend on each other. With the interface owned by the
  high-level package, the low-level ones depend on it — Dependency Inversion, no cycle.

- **One class per effect** (`AttackEffect`, `FavorEffect`, …, `TwoOfAKind`, `ThreeOfAKind`).
  A new card is a new class registered by its expansion; the engine has no `switch` on
  card types — Open/Closed, and each class is small and testable alone (SRP).

- **`Expansion.cardEffects()` (default: none)** lets an expansion bring its effects along
  with its cards. `BaseGame` maps Attack, Favor, Shuffle, Skip and See The Future.

- **`PlayResolver` decides what a play means:** 1 card → its effect, 2 or 3 of the same
  type → the combo, anything else → `InvalidMoveException`. Combos ignore the cards' own
  effects because the resolver picks the combo by count, not by card type. Cards
  without a solo effect (cats, Defuse, Nope) are rejected when played alone.

- **`InvalidMoveException` is checked**, so the engine has to handle it: it publishes
  `MoveRejected` with the reason and asks the same player again. Nothing changes on a
  rejected move (cards are only removed after validation).

- **`GameContext` is what an effect may use**: state, controllers, randomizer, publish, the
  player's view, and "ask until valid" helpers. Effects get one object instead of the
  whole engine (low coupling), and tests build one with `GameStateBuilder.buildContext()`.

- **A turn ends when `TurnState.turnId()` changes.** The engine keeps asking the current
  player for actions until that happens. Pass, Skip and Attack all end the turn by changing
  `TurnState`, so the engine doesn't need to know which cards end turns.

- **Attack stacking:** `TurnState` remembers whether the current player is under attack.
  Attack gives the next player 2 turns plus, if the attacker was a victim, their remaining
  turns (2 → 4 → 6 when attacking on the first turn; 3 if on the last of 2). This follows
  the rulebook's "remaining turns plus 2". Skip only ends one turn.

- **Private information stays private:** See The Future goes only to the playing player's
  controller (`seeTheFuture`), and `CardTaken` events don't say which card moved.

## Nope window, rule 12 + concurrency bonus (`ek.engine.NopeResolver`)

- **All concurrency is in one class.** `NopeResolver` (with its private `Window`) is the only
  code that deals with threads. The rest of the engine stays single-threaded and simple.

- **Only the engine thread changes game state.** Controllers answer from any thread by
  calling `NopeOffer.playNope()`. That only *claims* the window (a synchronized method on
  `Window`). The engine thread then removes the Nope card, discards it and publishes the
  event. Hands, deck and discard pile never need locks (thread confinement).

- **Offers go out through an injected `Executor`**, so players decide in parallel and a
  slow or blocked player (console read, network round-trip) can't stop the others from
  answering. Tests inject `Runnable::run` (same thread, deterministic); production will use
  `Executors.newVirtualThreadPerTaskExecutor()`. The `GameView` for each offer is built on
  the engine thread first, and it's immutable, so passing it to another thread is safe.

- **First claim wins, late claims are refused.** `Window.claim` returns `false` once
  someone was first or the window has closed. A `CountDownLatch` wakes the engine as soon
  as someone claims; a Nope is the only thing that ends a window early.

- **Every window runs its full length and can't be skipped.** After every Nope-able play a
  window opens, *even if nobody holds a Nope*, and lasts exactly 5 s unless someone Nopes.
  There is no way to decline. Reasons:
  - *Rule 12, literally:* "a Nope can be played within 5 seconds" gives everyone the whole
    5 s, not "until everyone has said no".
  - *Timing never leaks hands.* Before, a window opened only if someone else held a Nope and
    closed once all had declined. So how long the game paused told you who held one;
    with 2 players, any pause meant "they have a Nope". Now every window lasts exactly the same
    unless a Nope is played, and players without a Nope see the same countdown.
  - *Trade-off:* every Nope-able play now costs 5 s, so games are slower. Accepted
    deliberately: correctness and hidden information over speed.

- **Who may Nope is decided on the engine thread when the window opens:**
  `NopeOffer.canNope()` is "holds a Nope" at that moment, and `playNope()` is refused without
  it. The hands can't change while the engine waits, so the decision stays true. Every alive
  player except the one who played last gets an offer (`offerNope`); the last player gets
  `watchNopeWindow` (a read-only `NopeWindow`, default no-op) so they can see the countdown on
  their own play. `NopeOffer extends NopeWindow` (ISP: watchers can't claim).

- **Window length and waiting are injected:** `Duration` (default `DEFAULT_WINDOW` = 5 s) and
  a `Waiter` (`ek.util`, real one: `CountDownLatch.await(timeout)`). Tests pass a waiter that
  records how long the engine asked to wait and checks every offer is still open at that
  moment, then lets the time run out at once. That proves "not closed early" without
  waiting 5 s. One test uses real threads and a 1-hour first window to prove a Nope from
  another thread wakes the engine immediately.

- **Nope chains: a Nope resets the window.** Each Nope opens a new *full* window for everyone
  except the player who just Noped, so the original player (or anyone) can Yup, and the chain
  goes on until a window runs out. The play is cancelled when the number of Nopes is odd
  (rule 12a). Noped cards stay on the discard pile.

- **Bots never shorten a window.** A bot that wants to Nope (half the time, as before) does
  it at a random moment from 1 s after the window opens to 0.5 s before it ends; otherwise it
  just returns. Returning changes nothing, because the window runs its length anyway.

- **Interrupts:** if the engine thread is interrupted while waiting, the window is closed
  as if time ran out and the interrupt flag is restored — not swallowed (CLAUDE.md §7).

- **Kitten draws and Defuse are never offered for Nope**: the resolver only runs after a
  `Play` action, and Nope alone is not a legal play (`PlayResolver` rejects it).

## Runnable game: console, bots, `Main` (`ek.ui`, `ek.player`, `ek.app`)

- **`Main` is the composition root.** It is the only class that creates services
  (`JavaRandomizer`, `GameSetup`, resolvers, controllers, view) and wires them together.
  Everything else gets its dependencies through constructors, so tests can wire fakes
  instead (Dependency Injection, req 19).

- **`ConsoleController` is just another `PlayerController`.** The engine doesn't know a
  human is playing. All parsing and range checks happen here, at the boundary, and bad
  input is asked again. The engine still validates moves, so a faulty controller can't break it.
  Input and output streams are injected, so tests use a `StringReader` instead of a terminal.

- **Console Nope prompt and threads.** `offerNope` runs on a Nope-window thread while the
  engine waits. It only reads input while the offer is open (`NopeOffer.isOpen()`), and all
  reads share one lock, so after the window closes the next prompt gets the next line —
  input is never read by two prompts. It checks for input every 50 ms instead of blocking on
  `readLine`, so it can stop when time is up.

- **`ConsoleView` is a `GameEventListener`** that turns each event into a line of text with
  an exhaustive `switch` over the sealed `GameEvent` type: a new event type is a compile
  error here until it is rendered. Drawn and stolen cards are never printed.

- **`BotController` makes random legal moves** (decision in CLAUDE.md §9). It picks
  uniformly among Pass and every legal play (single cards with an effect, pairs, triples),
  so it never makes a move the engine rejects. Randomness is injected, so a seeded bot
  game is reproducible. Bots join through the same interface as humans (Liskov, req 16).

## Network play, rule 17 (`ek.net`)

- **Proxy on the server, plain console on the client.** `RemoteController` implements
  `PlayerController` and `GameEventListener` and turns every call into a message. The
  engine can't tell a remote player from a local one (Liskov), so no game code changed.
  The client (`GameClient`) passes each message to an ordinary `ConsoleController` and
  `ConsoleView`, so a remote player gets exactly the same console UI as the host.

- **The host plays at its own console** (CLAUDE.md §9). `server N B` = the host + N−1
  clients + B bots, so `server 1 B` is still a single-player game with no sockets.

- **Typed messages, no keywords (req 17).** `Message` is a sealed interface of records,
  split into `ToClient` and `ToServer`. Most messages mirror one `PlayerController`
  method or its answer. The client `switch`es over them, so a new message is a compile
  error until it is handled. The client doesn't know any card names: the server sends the
  cards that can be named in its `Welcome`, so an expansion needs no client change.

- **Transport behind `MessageChannel`.** The rest of `ek.net` only sees send/receive.
  `ObjectStreamChannel` uses Java serialization (no extra dependency). The wire format
  could change to e.g. JSON by writing another channel. Tests use an in-memory
  `LoopbackChannel` instead of sockets.

- **Received data is checked.** An `ObjectInputFilter` only lets game types and basic
  JDK types be read, so a hostile client can't make the server create other classes.
  Records are rebuilt through their constructors, so their checks (no nulls, copied lists)
  also run on received data. The engine still validates every move, as for local players.

- **Events carry player names, not `Player` objects.** An event is now a pure value that
  can be sent as it is. Before, a listener could reach any player's live hand through
  `event.player().hand()`. Convenience constructors take a `Player`, so the engine's code
  didn't change.

- **Remote Nope (concurrency).** `offerNope` sends `OfferNope` with an id, and the client's
  `PlayNope(id)` is handled on the connection's reader thread, which claims the engine's
  window exactly like a local player would. The engine thread is still the only one that
  changes game state.

- **Message order.** All sends to a client hold one lock. Before any other message,
  `RemoteController` sends `NopeClosed` for windows that have closed, and an offer is
  only sent while its window is open. A `NopeResult` is sent while the claim is made, so
  it arrives before the engine's `NopePlayed`. The client therefore never answers a turn
  prompt while an old Nope prompt is still reading input.

- **Disconnects end the game.** If a client disconnects, the next request to it throws
  `UncheckedIOException` and `Main` prints "Game aborted". Reconnecting or replacing the
  player with a bot is outside rules 1–17.

## Console dashboard and Nope prompt (`ek.ui`)

- **With ANSI, the screen is one frame redrawn in place; nothing scrolls.** `ConsoleOutput`
  holds the frame (board, turn heading, and the question being asked: its lines and prompt)
  and rewrites all of it from the top left after every change: an event goes into the
  board's *Recent* box instead of being appended, and an answered question is removed, so
  lines never pile up under the board. The open prompt is always the last
  row, so an event arriving during a Nope prompt can't push it away. The board is stored as
  a function of the recent lines, so it is redrawn with each new event without
  `ConsoleOutput` knowing about `GameView`. Every decision (`chooseAction`, the Nope
  offer, Favor, ...) replaces the board with one built from its fresh `GameView`. Between
  decisions, deck and hand counts are as of the last decision; keeping them live would need
  the engine to send views with events, which the UI alone shouldn't change. The trade-off: a
  redraw while you are typing hides the typed text, though Enter still sends it.
  Plain style keeps appending lines, so tests and dumb terminals see a simple log.

- **The dashboard is shown as soon as the cards are dealt.** The engine calls
  `PlayerController.gameStarted(view)` for every player before the first turn, so a player
  sees their hand even when someone else starts. It is a controller callback, not a
  `GameEvent`, because the board needs that player's private `GameView` (their hand), which
  events don't carry. It has a no-op default like `handChanged`, so bots need no change (OCP);
  over the network it is the `GameStarted` message.

- **`ConsoleOutput` keeps the last 8 event lines**, and the dashboard's *Recent* section
  shows them. Private lines ("You drew X", See The Future, "Not allowed") go there too, so
  redrawing never hides something you just learned. The history lives in `ConsoleOutput`
  because that is already the single, synchronized object the view and the controller share,
  so no extra wiring is needed in `Main` or `GameClient`.

- **`Dashboard` is pure text** (like `HandMenu`). They are tested
  without I/O, and the controller only decides *when* to print them (SRP, testability).

- **Colour is a `Style` injected through `ConsoleOutput`.** `Style.PLAIN` (used by tests,
  and when `NO_COLOR` is set or `TERM=dumb`) prints no escape codes; `Style.ANSI` colours
  lines and can clear the screen and rewrite a line. `Main` makes the choice once. Rows are
  padded *before* they are painted, so escape codes never upset the box's width.

- **Every human sees the Nope window as a countdown.** With a Nope: "Nope Bob's Attack?
  Enter = Nope… 4". Without one: "Nope window for Bob's Attack… 4", where Enter only prints a
  private "You have no Nope." On your own play: "Waiting for Nopes on your Attack… 4". All
  three look and last the same, so the screen gives nothing away. The countdown is only for
  display: `isOpen()` (the engine, or the server's `NopeClosed` for a remote player) decides
  when it ends, so clock differences between machines don't matter. A remote client counts
  down from `OfferNope.windowMillis` (a plain `long`, so the deserialization filter needs no
  `java.time`); the window on a client's own play arrives as `WatchNopeWindow`.

- **Only the countdown line is redrawn.** Once a second `ConsoleOutput.updatePrompt` writes
  save-cursor, carriage return, the new text, restore-cursor (`Style.rewriteLine`), instead of
  redrawing the whole frame. So anything the player has started typing isn't wiped every
  second. The text keeps the same width, and later full redraws show the current second.
  Plain consoles can't rewrite a line, so they print the prompt once with the window's length
  ("(5 s)") and no ticks. The ticks go through the injected `Sleeper`, so tests run the
  whole countdown instantly.

## Extension points for expansions: rule 6 and drawing (`ek.expansion`, `ek.engine`)

- **Rule 6 comes from the expansions (`Expansion.cardsAfterDeal`).** Before, `GameSetup`
  inserted `players − 1` Exploding Kittens in a hard-coded loop, so an expansion that changes
  the kittens (Imploding Kittens replaces one, Streaking Kittens adds one) would have had to
  edit it. Now `BaseGame` returns the count, and `DeckFactory.addAfterDeal` asks the expansions
  *in order*, each getting the counts so far, so a later expansion can adjust an earlier one's.
  `GameSetup` keeps one line per rule — Open/Closed, and rule 6 stays unchanged and tested.

- **What a drawn card does is a `DrawEffect` (Strategy), like `CardEffect` for played cards.**
  The defuse-or-explode logic moved out of `GameEngine` into `cards.ExplodingKittenDraw`;
  `BaseGame` supplies it through `Expansion.drawEffects()`. Cards without one go into the hand
  (`DrawEffect.KEEP`). The engine only moves the turn on: `SURVIVED` ends the turn, `EXPLODED`
  passes play on. A new kind of kitten is then a new class, not an engine change — OCP, and
  `GameEngine` loses a second responsibility (SRP). `GameContext.explode` and
  `chooseDeckPosition` are shared so every kitten reuses them.

- **Nameable cards come from the expansions** (`DeckFactory.cardTypes`), not
  `BaseCardType.values()`, so an expansion's cards can be named for three of a kind.

- **Known limits, left until an expansion needs them** (no speculative abstractions): turn
  order (`TurnState` is clockwise only), combos (`PlayResolver` only matches identical cards),
  new player decisions (each is a new `PlayerController` method and `Message` record), and
  face-up cards (`Card` has no visibility state).

## Setup menu, expansions and variants, requirement 15 (`ek.app`, `ek.expansion`, `ek.rules`, `ek.ui`)

- **`GameConfig` (record, `ek.rules`) is the host's choices as one validated value:** humans,
  bots, expansions (base game always first) and variants. Its constructor checks at least 1
  human, no negative bots, and a total inside `PlayerRange.of(expansions)`. Both ways of
  starting a host (menu and `server <humans> <bots>`) build one, so the two can't disagree.
  `GameConfig.of` puts `BaseGame` first and sorts the rest, so callers can't get the order
  wrong. Value object, SRP; no I/O, so it is tested directly.

- **`PlayerRange.of(expansions)` is the only place the player limit is defined.** Before, the
  2–5 limit was a pair of constants in `GameSetup`, also read by `Main`. Now `GameConfig`, the
  menu's hint and `GameSetup`'s rule-1 guard (through `DeckFactory.playerRange()`) all ask the
  same method. It already takes the expansions, so when an expansion allows more players
  only this method changes. Single source of truth (Booch: cohesion, sufficiency).

- **`GameOption`: name, description and order, as `default` methods.** `Expansion` and
  `GameVariant` extend it, so the menu shows what an option says about itself (no magic
  strings, nothing to edit in the menu for a new option). Defaults keep `Expansion` a
  functional interface, so the existing lambdas in tests still compile (OCP for the
  interface's users). `GameOption.BY_ORDER` sorts by `order()`, then name, then class name.
  That order is total, so the result is the same however the options were found.

- **`ExpansionRegistry` finds expansions and variants with `java.util.ServiceLoader`.** A new
  expansion is a jar with its classes plus `META-INF/services/ek.expansion.Expansion`; put
  it on the classpath and it appears in the menu with no edit to any existing class. That
  covers the menu too, which is the point of requirement 15 for extensibility.
  - *Service Locator, kept at the edge.* ServiceLoader is a Service Locator, which hides
    dependencies if used deep in the code. Here only `ExpansionRegistry` calls it, and only
    `Main` (the composition root) calls the registry. Everything below `Main` gets plain
    `List<Expansion>`/`List<GameVariant>` through constructors (`GameConfig` →
    `GameAssembler` → `DeckFactory`, `GameSetup`), which is ordinary dependency injection. So
    the locator does the *discovery* and DI does the *wiring*. Tests never need the registry
    to build a game, only lists.
  - *Explicit order.* `DeckFactory.afterDeal` chains the expansions (a later one adjusts an
    earlier one's kitten count), so the order changes the game. ServiceLoader's iteration
    order depends on the classpath, so it is never used. `BaseGame` is not looked up, and is
    always first. The rest are sorted by `order()` in the registry and again in
    `GameConfig.of`.
  - *Errors.* Each `hasNext()`/`next()` is wrapped. A `ServiceConfigurationError` (class
    missing, wrong type, constructor throws) becomes one line in `problems()` naming the
    provider and the cause, and the loop goes on (the JDK iterator moves past the broken
    entry). The menu shows the problems under the expansion list, so nothing fails
    silently and one bad jar doesn't stop the game. A cap on failures in a row guards
    against a classpath that keeps failing. The registry does no I/O itself.

- **`ConsoleMenu` (`ek.ui`) is I/O only.** It gets a `BufferedReader` and `ConsoleOutput`
  injected (like `ConsoleController`), asks for expansions, then variants, then humans and
  bots, and asks again on bad input. It checks only the *form* of the input (numbers in the
  list, each once; whole numbers ≥ 0). Whether the counts make a valid game is
  `GameConfig`'s decision: the menu catches its exception, prints the message and asks
  again. So there is no second copy of the limit (SRP, DRY). Tested with a `StringReader`.

- **`GameAssembler` (`ek.app`) moves the wiring that was inline in `Main.runServer`:**
  expansions → card effects, draw effects, `DeckFactory`, nameable cards, `GameSetup` with the
  variants, and the `GameEngine`. It takes a `GameConfig` and a `Randomizer` and does no I/O,
  so the assembly is tested without sockets or `System.in` (SRP, testability, req 19).
  `Main` stays the composition root: it creates the console, the controllers and the
  sockets, and runs what the assembler built.

- **`CommandLine` (`ek.app`) parses the arguments into a sealed result** (`OpenMenu`,
  `HostBaseGame`, `Join`, `Invalid`), so parsing is tested without `System.exit`.
  `server <humans> <bots>` is deliberately **base game only**: it never looks at the
  registry and takes no expansion or variant arguments. The shortcut is then a fixed,
  reproducible game (handy for testing and the examiner), and choosing options has one
  interface, the menu, instead of a second syntax to keep in step with the menu.

- **Variants: `GameVariant.afterDeal(deck, randomizer)`, one hook in `GameSetup`** (Strategy).
  It runs between dealing (rule 5) and adding the kittens (rule 6), which is where the
  official "Faster game" would remove a third of the deck. No variant is implemented yet
  (not needed now); the interface, the hook, discovery and the menu are in place, and a
  test-only variant shows they work. Rule 6 still runs after the hook, so the kitten count
  is unchanged. Adding the hook was a one-time edit to
  `GameSetup`; a new variant is now a new class plus a line in
  `META-INF/services/ek.expansion.GameVariant`. *Trade-off:* the hook only allows changing
  the deck between dealing and the kittens. That covers the deck-size variants, but not
  variants that change turn rules, which would need an engine hook. That hook is left out
  until one is needed (no speculative framework). Variants as `Expansion`s were rejected:
  `cardsAfterDeal` only edits counts and has no randomizer, so a deck-trimming variant
  couldn't be written as one.

## Extension points for Imploding Kittens (`ek.expansion`, `ek.engine`, `ek.player`)

Each mechanism below is generic and comes from an Imploding Kittens card or rule. None of
the expansion itself is in `src/main`; a test-only fake (`ek.fakeimploding.FakeImploding`,
registered with one services line) proves each one.

- **Player limit and deck counts come from the expansions (block 1).** `Expansion.maxPlayers()`
  defaults to the base game's 5, and `PlayerRange.of` takes the largest among the chosen
  expansions (Imploding Kittens allows 6). The minimum stays 2. `deckCards` is now chained
  like `cardsAfterDeal`: a default `deckCards(soFar, playerCount)` adds the expansion's own
  `deckCards(playerCount)` to the counts so far. An expansion overrides it to change another's
  counts (6 players: all 6 Defuse are dealt, so none go into the deck). One pattern for both
  deck phases (consistency, least surprise). Default methods keep every existing expansion
  and lambda compiling (OCP). `PlayerRange.of` stays the single source of truth (Booch:
  cohesion), so the menu, `GameConfig` and `GameSetup` show and check the new range unchanged.
