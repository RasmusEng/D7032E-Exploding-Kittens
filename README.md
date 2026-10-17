# Exploding Kittens — D7032E Home Exam

A terminal version of the card game *Exploding Kittens* in Java 21.
Design decisions and their motivation are in [`docs/design-notes.md`](docs/design-notes.md).

## Requirements

- JDK 21 or newer (the code is compiled with `--release 21`).
- No build tool. The only dependency is JUnit 5, already included as `lib/junit-platform-console-standalone.jar`.

## Build

From the project root:

```bash
rm -rf out
javac --release 21 -d out/classes $(find src/main -name '*.java')
cp -R src/main/resources/. out/classes/
```

The `cp` line copies `META-INF/services`, where expansions and variants are registered (see
[Expansions and variants](#expansions-and-variants)).

## Test

Build first, then:

```bash
javac --release 21 -d out/test-classes -cp out/classes:lib/junit-platform-console-standalone.jar \
  $(find src/test/java -name '*.java')
cp -R src/test/resources/. out/test-classes/
java -jar lib/junit-platform-console-standalone.jar execute \
  --class-path out/classes:out/test-classes --scan-class-path
```

To run one test class, replace `--scan-class-path` with e.g.
`--select-class ek.rules.RulesComplianceTest`.

Tests named `ruleNN...` cover the matching rule of the exam specification.

## Run

Host a game and set it up in a menu (you play at this console):

```bash
java -cp out/classes ek.app.Main server
```

The menu asks, in this order, for:

1. the expansions to add (the base game is always included),
2. the game variants,
3. the number of human players (including you; the others join as clients) and bots.

Choose expansions and variants by number, separated by spaces, or press Enter for none.
There must be 2–5 players in total (an expansion may allow more; the menu shows the range
for the chosen expansions); anything else is explained and asked again.

Shortcut for the **base game only** (no menu; installed expansions and variants are not
used, and can only be chosen in the menu):

```bash
java -cp out/classes ek.app.Main server <numPlayers> <numBots>
java -cp out/classes ek.app.Main server 1 1       # you against one bot
```

The `run` script builds first and then starts the same way: `./run` (menu),
`./run <numPlayers> <numBots>` (shortcut) or `./run client <ip>`. `./test` does a clean
build and runs all tests.

Network game, e.g. you plus two remote players and one bot. The server waits on TCP port 2048
until both clients have joined:

```bash
java -cp out/classes ek.app.Main server 3 1       # on the host (or use the menu)
java -cp out/classes ek.app.Main client <host-ip>  # on each other player's machine
```

Use `127.0.0.1` as the IP to try it on one machine in two terminals.

### How to play

On your turn, enter the numbers of the cards to play, then press Enter:

- `3`: play card 3.
- `2 5`: play cards 2 and 5 together. Two or three cards with the same name make a combo.
- `d`: draw a card, which ends your turn (a pass).

After every card play that can be Noped, everyone gets a 5-second Nope window with a countdown
("Nope Bob's Attack? Enter = Nope… 4"). If you hold a Nope, press Enter to play it. The window
can't be skipped: it always runs the full 5 seconds, also when nobody holds a Nope, so the
waiting never gives away who has one. A Nope starts a new 5-second window, in which the others
(including the original player) can Nope back (Yup). Without a Nope you see the same countdown,
and Enter only tells you that you have none. On your own play you see "Waiting for Nopes… 4".

The screen shows a dashboard (the deck size, the other players' hand sizes, the latest events
and your hand) that is redrawn in place whenever something happens, so the terminal never scrolls.
To turn colours and redrawing off and get a plain scrolling log, set `NO_COLOR=1` (e.g. `NO_COLOR=1 java -cp out/classes ek.app.Main server 1 1`).

## Expansions and variants

Expansions and variants are found when the game starts with Java's `ServiceLoader`, so a
new one needs no change to this code. None ship with the game yet: the menu shows
"No expansions installed" until one is added. An expansion is a class implementing
`ek.expansion.Expansion` (a variant: `ek.expansion.GameVariant`) with a public no-argument
constructor, registered by its full class name in
`META-INF/services/ek.expansion.Expansion` (or `...GameVariant`). The base game is always
included and is not registered. Expansions are applied in the order of their `order()`
value, never in the order they are found.

A separately compiled expansion jar is picked up by putting it on the classpath:

```bash
# compile the expansion against the game, add its registration, and package it
javac --release 21 -cp out/classes -d my-expansion/classes $(find my-expansion/src -name '*.java')
mkdir -p my-expansion/classes/META-INF/services
echo com.example.MyExpansion > my-expansion/classes/META-INF/services/ek.expansion.Expansion
mkdir -p expansions
jar --create --file expansions/my-expansion.jar -C my-expansion/classes .

# start the menu with every jar in expansions/ on the classpath
java -cp "out/classes:expansions/*" ek.app.Main server
```

The menu then lists it by the `name()` and `description()` it gives itself. Clients don't
need the jar for the menu, but they must have it on their classpath for its card types to
reach them over the network. A provider that can't be loaded is skipped, and the menu shows
which one and why.

