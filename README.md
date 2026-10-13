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
```

## Test

Build first, then:

```bash
javac --release 21 -d out/test-classes -cp out/classes:lib/junit-platform-console-standalone.jar \
  $(find src/test/java -name '*.java')
java -jar lib/junit-platform-console-standalone.jar execute \
  --class-path out/classes:out/test-classes --scan-class-path
```

To run one test class, replace `--scan-class-path` with e.g.
`--select-class ek.rules.RulesComplianceTest`.

Tests named `ruleNN...` cover the matching rule of the exam specification.

## Run

Host a game (you play at this console):

```bash
java -cp out/classes ek.app.Main server <numPlayers> <numBots>
```

- `numPlayers`: human players, including you. The others join as clients.
- `numBots`: computer players.
- There must be 2–5 players in total. Example, you against one bot:

```bash
java -cp out/classes ek.app.Main server 1 1
```

Network game, e.g. you plus two remote players and one bot. The server waits on TCP port 2048
until both clients have joined:

```bash
java -cp out/classes ek.app.Main server 3 1       # on the host
java -cp out/classes ek.app.Main client <host-ip>  # on each other player's machine
```

Use `127.0.0.1` as the IP to try it on one machine in two terminals.

### How to play

On your turn, enter the numbers of the cards to play, then press Enter:

- `3`: play card 3.
- `2 5`: play cards 2 and 5 together. Two or three cards with the same name make a combo.
- `d`: draw a card, which ends your turn (a pass).

When another player plays a card and you hold a Nope, you have 5 seconds to press Enter to Nope it. If you
don't want to, type `n` and Enter so the game goes on at once (otherwise the offer runs out after 5 seconds).

The screen shows a dashboard (the deck size, the other players' hand sizes, the latest events
and your hand) that is redrawn in place whenever something happens, so the terminal never scrolls.
To turn colours and redrawing off and get a plain scrolling log, set `NO_COLOR=1` (e.g. `NO_COLOR=1 java -cp out/classes ek.app.Main server 1 1`).
