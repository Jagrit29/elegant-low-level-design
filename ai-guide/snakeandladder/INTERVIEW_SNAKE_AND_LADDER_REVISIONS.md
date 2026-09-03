# Interview Snake and Ladder Revisions

This document records an interview-ready low-level design for Snake and Ladder. It focuses on requirement clarification, responsibility placement, invariants, testability, complexity, and follow-up discussions.

The complete runnable implementation is in `SnakeAndLadder.java` in the same directory.

---

## 1. Problem statement

Design a local Snake and Ladder game in which:

- at least two players participate;
- players take turns in insertion order;
- every player begins at position `0`;
- the board has a configurable final cell;
- snakes move players downward;
- ladders move players upward;
- a configurable die produces the movement value;
- a player must land exactly on the final cell to win;
- an overshooting roll leaves the player at the current position;
- landing on the start of a snake or ladder applies one transition;
- the first player to reach the final cell wins;
- no turn is played after the game has ended.

The demonstration uses a small board and scripted dice so that its output is deterministic. A real game can use `RandomDice`.

---

## 2. Clarifications and decisions

### Is the board always 100 cells?

No. The board size is configurable:

```java
Board board = new Board(size, snakes, ladders);
```

A traditional game can pass `100`. A smaller board is useful for demonstrations and tests.

### How many players are supported?

The game accepts `N` players but requires at least two. Turn rotation uses:

```java
currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
```

### Is an exact roll required to win?

Yes. If the proposed position exceeds the board size, the player does not move and the turn passes.

### Does rolling six grant another turn?

No in Phase 1. This is a rule variation and can be added later if requested.

### Can multiple players occupy one cell?

Yes. There is no collision or capture rule in Phase 1.

### Are transitions chained?

No. A turn applies at most one snake or ladder. If a ladder ends at a snake's head, the snake is not applied during the same turn.

This rule keeps the core implementation simple and prevents accidental cycles. Chained transitions can be introduced as an explicit extension with cycle validation.

### Is player order random?

No. Players take turns in the order supplied to `Game`. Randomizing the initial order is independent of the board and can be done before game construction.

---

## 3. Core entities

### `Snake`

Represents a downward board transition.

State:

- `head`: the entry position;
- `tail`: the destination.

Invariant:

```text
head > tail > 0
```

A snake is immutable because its endpoints should not change during a game.

### `Ladder`

Represents an upward board transition.

State:

- `bottom`: the entry position;
- `top`: the destination.

Invariant:

```text
0 < bottom < top
```

`Snake` and `Ladder` remain separate. They have a similar data shape, but different domain language and invariants. Inheritance would not provide useful polymorphic behavior in Phase 1.

### `Dice`

Defines the randomness boundary:

```java
interface Dice {
    int roll();
}
```

Implementations:

- `RandomDice`: generates a real random value;
- `SequenceDice`: returns predefined values for demonstrations and tests.

### `Player`

Stores:

- immutable player name;
- mutable current position.

Every player begins at position `0`. Position mutation is controlled by the game rather than exposed as a general public setter.

### `Board`

Owns the board configuration.

Responsibilities:

- store board size;
- store snakes by head position;
- store ladders by bottom position;
- reject invalid transition configuration;
- determine whether a proposed position overshoots;
- determine whether a position is the winning cell;
- resolve a landing position through one snake or ladder.

### `GameStatus`

Represents lifecycle state:

```java
enum GameStatus {
    IN_PROGRESS,
    WON
}
```

A draw is not possible under the current rules, so there is no `DRAW` state.

### `Game`

Orchestrates gameplay.

Responsibilities:

- own the board, players, and dice;
- validate players;
- track the current player;
- execute one turn;
- update player position;
- detect the winner;
- rotate turns;
- prevent turns after completion.

---

## 4. Relationships

```text
Game
 ├── HAS-A Board
 ├── HAS N Players
 ├── HAS-A Dice
 ├── tracks current player index
 ├── may reference a winning Player
 └── HAS-A GameStatus

Board
 ├── HAS snakes indexed by head
 ├── HAS ladders indexed by bottom
 └── resolves a landing position

Player
 └── HAS-A current position

RandomDice and SequenceDice
 └── IMPLEMENT Dice
```

The main relationships use composition. The only polymorphism is at the useful variability point: dice behavior.

---

## 5. Responsibility placement

### Why transition resolution belongs to `Board`

`Board` owns:

- board size;
- snake locations;
- ladder locations;
- transition destinations.

Therefore, it is the Information Expert for this operation:

```java
int finalPosition = board.resolvePosition(landedPosition);
```

`Game` should not iterate through snake and ladder collections because that would expose board internals and mix configuration lookup with turn orchestration.

### Why turn flow belongs to `Game`

`Game` knows:

- whose turn it is;
- which dice is being used;
- whether the game is still active;
- which player won;
- when to rotate turns.

Therefore, `Game.playTurn()` coordinates the use case while delegating board-specific questions to `Board`.

### Why position belongs to `Player` in this interview version

A player's position changes during a game and is naturally queried as:

```java
player.getPosition();
```

For a production platform where one user participates in multiple games, user identity and game-specific position should be separated. For a local interview model, keeping position in `Player` is simpler and sufficient.

---

## 6. Encapsulation and invariants

The design protects these invariants:

1. A snake always moves downward.
2. A ladder always moves upward.
3. Transition positions are positive.
4. Every transition endpoint lies within the board.
5. No transition begins at the winning cell.
6. Only one transition begins at any position.
7. A die has at least two sides.
8. A player has a nonblank name.
9. Every player begins at position `0`.
10. At least two players participate.
11. Player names are unique within the game.
12. A player never moves beyond the winning cell.
13. Winner is set only when status becomes `WON`.
14. No gameplay state changes after the game is won.

Constructors reject invalid setup because setup errors are programming/configuration defects. Expected gameplay outcomes, such as overshooting, are handled as normal branches inside `playTurn()`.

---

## 7. Why `Snake` and `Ladder` do not use inheritance

A possible abstraction is a generic `Jump(start, end)`. It reduces duplicate fields, but it weakens domain language and still requires identifying whether the jump is upward or downward.

In Phase 1:

- snakes and ladders have different names;
- snakes and ladders enforce opposite invariants;
- neither exposes shared behavior beyond storing endpoints;
- the board treats their transitions differently when reporting results.

Therefore, keeping them separate is clearer than forcing an inheritance hierarchy.

If future requirements introduce many board transition types with genuinely shared behavior, a common abstraction may become justified.

---

## 8. Why `Dice` is an interface

Randomness makes tests nondeterministic. If `Game` constructs a random die internally, tests cannot reliably arrange an overshoot, snake, ladder, or winning roll.

`Game` instead depends on this abstraction:

```java
interface Dice {
    int roll();
}
```

Production-style play uses:

```java
Dice dice = new RandomDice(6);
```

Deterministic verification uses:

```java
Dice dice = new SequenceDice(3, 4, 6);
```

This demonstrates Dependency Inversion at a meaningful change point rather than creating interfaces for every class.

For a die with `S` sides, random generation uses:

```java
ThreadLocalRandom.current().nextInt(1, numberOfSides + 1);
```

The lower bound is inclusive and the upper bound is exclusive, so results are in `[1, S]`.

---

## 9. Board data structure

The board stores:

```java
Map<Integer, Snake> snakesByHead;
Map<Integer, Ladder> laddersByBottom;
```

The key is the position where the transition begins.

This provides average `O(1)` transition lookup. A list-based design would require scanning snakes and ladders on every valid move, costing `O(S + L)`, where `S` and `L` are the counts of snakes and ladders.

Separate maps preserve explicit domain modeling while still providing efficient lookup.

---

## 10. Turn flow

For `game.playTurn()`, the sequence is:

1. Read the current player.
2. If the game is over, print a message and return without rolling.
3. Roll the injected dice.
4. Add the roll to the player's current position.
5. If the proposed position exceeds the board size:
   - keep the player at the old position;
   - rotate the turn;
    - return from the method.
6. Ask `Board` to resolve one snake or ladder.
7. Move the player to the resolved position.
8. If that position is the final cell:
   - record the player as winner;
   - set status to `WON`;
    - return from the method.
9. Otherwise rotate the turn.

```text
Game.playTurn
  -> Dice.roll
  -> calculate landed position
  -> Board.isOvershoot
  -> Board.resolvePosition
  -> Player.moveTo
  -> Board.isWinningPosition
    -> win or switch player
```

The winning player is not switched away because the game ends immediately.

---

## 11. Why `playTurn()` returns `void`

For the interview-focused Phase 1 solution, `playTurn()` executes one complete turn and prints a concise explanation. This avoids introducing `TurnResult`, `TurnOutcome`, and `PositionResolution` before they are required.

This is a deliberate simplicity trade-off:

- advantage: fewer types and less code to write under interview time constraints;
- disadvantage: game logic is coupled to console presentation;
- extension: return a structured `TurnResult` when building a web API, UI, move history, or automated event pipeline.

The board remains simple and returns only the resolved integer position:

```java
int finalPosition = board.resolvePosition(landedPosition);
```

An interview solution should start with the smallest design that satisfies the agreed requirements and explain where richer abstractions would be introduced.

---

## 12. Complexity analysis

Let:

- `P` be the number of players;
- `S` be the number of snakes;
- `L` be the number of ladders.

### Initialization

- player validation: `O(P)`;
- snake indexing: `O(S)`;
- ladder indexing: `O(L)`;
- total setup: `O(P + S + L)`.

### Each turn

- dice roll: `O(1)`;
- overshoot check: `O(1)`;
- snake lookup: average `O(1)`;
- ladder lookup: average `O(1)`;
- win check: `O(1)`;
- player rotation: `O(1)`.

Therefore, each turn is average `O(1)`.

### Space

- players: `O(P)`;
- transitions: `O(S + L)`;
- total: `O(P + S + L)`.

The board does not need an array of every cell because only transition starts and the final cell affect game logic.

---

## 13. OOP and design principles used

### Encapsulation

Fields are private. Player movement and board configuration cannot be modified arbitrarily from outside their owners.

### Abstraction

The caller uses:

```java
game.playTurn();
```

It does not need to know transition lookup, overshoot rules, or player-index arithmetic.

### Single Responsibility Principle

- `Snake` and `Ladder` represent validated transitions.
- `Dice` supplies movement values.
- `Player` stores participant state.
- `Board` owns board rules and transition lookup.
- `Game` orchestrates turns and lifecycle.

### Information Expert

- `Board` resolves positions because it owns transition data.
- `Game` manages turns because it owns game lifecycle.

### Dependency Inversion

`Game` depends on `Dice`, not directly on `RandomDice`.

### Composition over inheritance

`Game` contains a board, players, and dice. `Snake` and `Ladder` are not forced into an unnecessary parent class.

### Defensive copying

`Game` uses:

```java
this.players = List.copyOf(players);
```

The caller cannot later add, remove, or reorder players through the original list.

---

## 14. Why no additional patterns are forced into Phase 1

The useful Strategy-like abstraction is `Dice`, because random behavior must be replaceable for testing.

The core solution does not need factories, repositories, observers, state classes, or commands. These patterns become useful only if requirements evolve:

- **Strategy**: configurable overshoot, extra-turn, or transition policies;
- **Factory/Builder**: complex board creation from configuration;
- **Observer**: notify UI, analytics, or spectators after turns;
- **Command**: replay, undo, or audit turn history;
- **Repository**: persist and restore games;
- **State**: complex lifecycle with paused, abandoned, or timed-out states.

Patterns should solve concrete variation, not inflate an interview solution.

---

## 15. Important edge cases

1. Board size below two.
2. Null snake or ladder collection.
3. Null snake or ladder item.
4. Snake head not above its tail.
5. Ladder bottom not below its top.
6. Transition endpoint outside the board.
7. Transition beginning at the winning cell.
8. Two snakes beginning at the same position.
9. Two ladders beginning at the same position.
10. Snake and ladder beginning at the same position.
11. Die with fewer than two sides.
12. Random roll must remain within `[1, numberOfSides]`.
13. Null or blank player name.
14. Fewer than two players.
15. Null player.
16. Duplicate player names.
17. Landing on a snake.
18. Landing on a ladder.
19. Landing on an ordinary cell.
20. Exact roll reaches the final cell.
21. Overshooting leaves position unchanged.
22. Overshoot still rotates the turn.
23. Turn order wraps from the last player to the first.
24. No turn is executed after a winner exists.
25. Scripted dice has no remaining values.

---

## 16. Interview explanation

A concise answer:

> I model Snake and Ladder with immutable `Snake` and `Ladder` value objects, a `Player` containing current position, a `Board` that owns transition configuration, and a `Game` that orchestrates turns and lifecycle. `Board` indexes snake heads and ladder bottoms in maps, giving average `O(1)` resolution per move. `Game` depends on a `Dice` abstraction, allowing `RandomDice` in real play and deterministic `SequenceDice` in tests. `playTurn` handles game state, roll, overshoot, one board transition, victory, and player rotation. For the first interview version it prints the result directly; a structured result object can be added when an external UI or API requires it.

### Follow-up: Why not store all board cells?

Most cells contain no special data. The game only needs:

- final board size;
- snake starts and destinations;
- ladder starts and destinations.

Maps provide direct lookup without allocating an object for every cell.

### Follow-up: Why does the board validate endpoints if Snake and Ladder validate themselves?

They protect different invariants:

- `Snake` validates that a snake moves downward.
- `Ladder` validates that a ladder moves upward.
- `Board` validates that those endpoints belong to this particular board and do not conflict with other transitions.

### Follow-up: Why not throw an exception for overshoot?

Overshoot is expected gameplay behavior, not invalid program configuration. It is handled as a normal branch. Constructors throw for invalid setup because those errors indicate a defective game configuration.

### Follow-up: How would chained transitions work?

Repeatedly look up a transition from the current resolved position until none exists. To make this safe, either:

- validate the entire transition graph for cycles during board construction; or
- track visited positions while resolving and fail if one repeats.

The complexity becomes proportional to the number of transitions followed.

### Follow-up: How would extra turns on six work?

Introduce a rule in `Game`: switch players only when the roll does not grant an extra turn. If several rule variations are expected, extract a `TurnPolicy` rather than hardcoding multiple conditions.

### Follow-up: How would multiple dice work?

Create another `Dice` implementation that owns several dice and returns their sum. `Game` remains unchanged because it depends only on `roll()`.

### Follow-up: Is `Game` thread-safe?

No. The local interview model assumes one caller executes turns sequentially. For online play, commands should include game/player identity and version information, and the server should serialize or atomically validate updates.

---

## 17. Class outline

```text
SnakeAndLadder
 ├── enum GameStatus
 ├── class Snake
 ├── class Ladder
 ├── interface Dice
 │    ├── RandomDice
 │    └── SequenceDice
 ├── class Player
 ├── class Board
 ├── class Game
 └── main demonstration
```

The types are nested in one source file for revision and interview convenience. In a production project, they can be placed in separate files and packages without changing their responsibilities.

---

## 18. What to practice

1. Clarify exact-win, extra-turn, collision, and chained-transition rules.
2. Identify `Player`, `Dice`, `Snake`, `Ladder`, `Board`, and `Game`.
3. Explain why Snake and Ladder remain separate.
4. Write constructor invariants for `Snake` and `Ladder`.
5. Explain why `Dice` is the useful abstraction boundary.
6. Implement inclusive random rolling correctly.
7. Build board maps and reject duplicate starting positions.
8. Write `playTurn()` in the correct order.
9. Explain why overshoot rotates the turn but does not move the player.
10. Explain why transition lookup belongs to `Board`.
11. Explain why lifecycle and turn rotation belong to `Game`.
12. Explain average `O(1)` turn complexity.
13. Use scripted dice to test ladder, snake, overshoot, and win scenarios.
14. Discuss chained transitions, extra turns, persistence, and concurrency only as extensions.

---

## 19. Compile and run

From this directory:

```text
javac SnakeAndLadder.java
java SnakeAndLadder
```

From the repository root:

```text
javac ai-guide/snakeandladder/SnakeAndLadder.java
java -cp ai-guide/snakeandladder SnakeAndLadder
```
