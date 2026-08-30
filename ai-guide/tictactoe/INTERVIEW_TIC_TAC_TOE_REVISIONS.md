# Interview Tic Tac Toe Revisions

This document records the design we arrived at step by step. It is intended for revision before a low-level design interview, not merely as a code summary.

The complete runnable implementation is in `TicTacToe.java` in the same directory.

---

## 1. Problem statement

Design a local Tic Tac Toe game in which:

- exactly two players participate;
- each player chooses a different symbol, either `X` or `O`;
- the game uses an `N x N` board;
- `X` always takes the first turn, regardless of which player chose it;
- the game automatically manages the current player;
- a player wins by filling all `N` positions in one row, one column, the main diagonal, or the anti-diagonal;
- invalid moves are rejected;
- when the board becomes full without a winner, the game ends in a draw;
- no move is allowed after the game has ended.

For the first demonstration, we pass `N = 3`.

---

## 2. Clarifications and decisions

### Why support an `N x N` board?

We do not hardcode the number `3` throughout the classes. Instead, the caller supplies the board size:

```java
Game game = new Game(3, playerOne, playerTwo);
```

This keeps the design flexible while retaining a simple winning rule: on an `N x N` board, `N` symbols are required to win.

The board size and winning length are deliberately the same in Phase 1. Supporting a separate winning length can be added later if required.

### Why exactly two players?

Traditional Tic Tac Toe is a two-player game. Generalizing players introduces turn-order and symbol-allocation complexity without solving a stated requirement.

### Can players choose their symbols?

Yes. A player may choose `X` or `O`, but:

- a player cannot choose `EMPTY`;
- both players cannot choose the same symbol;
- `X` starts the game automatically.

### Who manages turns?

`Game` manages turns. The caller submits only coordinates:

```java
game.playMove(row, column);
```

The caller does not pass a player on each move because the game already knows the current player.

### Does an invalid move consume the turn?

No. The current player changes only after a valid move that does not finish the game.

---

## 3. Core entities

### `Symbol`

An enum representing the only legal board values:

```java
enum Symbol {
    X,
    O,
    EMPTY
}
```

`EMPTY` represents an unoccupied board cell and cannot be assigned to a player.

### `Player`

Represents player identity and the chosen symbol.

Responsibilities:

- store the player's name;
- store the player's symbol;
- reject a blank name;
- reject `null` or `EMPTY` as the player's symbol.

It does not control turns, mutate the board, or determine the winner.

### `Board`

Owns the `N x N` grid.

Responsibilities:

- initialize every cell as `EMPTY`;
- validate coordinates;
- determine whether a cell is empty;
- place a symbol into an empty cell;
- determine whether the last move completed a winning row, column, or diagonal;
- keep row, column, and diagonal traversal private;
- determine whether the board is full;
- print the board.

### `GameStatus`

Represents the lifecycle of a game:

```java
enum GameStatus {
    IN_PROGRESS,
    WON,
    DRAW
}
```

`NOT_STARTED` is unnecessary because the game starts when it is constructed.

### `MoveResult`

Describes what happened when the caller attempted a move:

```java
enum MoveResult {
    MOVE_ACCEPTED,
    PLAYER_WON,
    DRAW,
    OUT_OF_BOUNDS,
    CELL_OCCUPIED,
    GAME_ALREADY_OVER
}
```

Returning a result is more informative than returning only `true` or `false`.

### `Game`

Coordinates the use case.

Responsibilities:

- own the board and two players;
- validate that the players use different symbols;
- choose the `X` player as the starting player;
- track the current player;
- accept and validate moves;
- ask `Board` to place the current player's symbol;
- ask `Board` whether the move completed a winning line;
- track the winner and game status;
- switch players after a valid non-terminal move.

---

## 4. Relationships

```text
Game
 ├── HAS-A Board
 ├── HAS-A Player one
 ├── HAS-A Player two
 ├── references the current Player
 ├── may reference a winning Player
 └── HAS-A GameStatus

Board
 ├── HAS N x N Symbol values
 └── owns board analysis and win detection

Player
 └── HAS-A Symbol
```

These relationships use composition. There is no useful inheritance hierarchy in Phase 1.

---

## 5. Why `placeSymbol` belongs to `Board`

`Board` owns the cell array, so it must control changes to that array.

```java
public boolean placeSymbol(int row, int column, Symbol symbol)
```

This demonstrates the **Information Expert** principle: assign a responsibility to the object that has the information needed to perform it.

The board knows:

- its size;
- whether the coordinates are valid;
- whether the cell is empty;
- where the symbol must be stored.

However, `Game` still controls the workflow:

```text
Game checks game state
  -> Game identifies current player
  -> Board validates and places symbol
    -> Board reports whether that symbol has a winning line
    -> Game records winner/draw
  -> Game switches player
```

Therefore:

- `Board` performs the low-level state mutation;
- `Game` decides when that mutation is allowed in the game flow.

`Player` should not place a symbol directly because a player does not own the board.

### Why win detection also belongs to `Board`

Win detection requires direct knowledge of the grid, its size, and its cells. `Board` already owns all of that information. Moving row, column, and diagonal traversal into `Board` follows the **Information Expert** principle and avoids leaking board internals into `Game`.

The public board-level question is:

```java
board.hasWinningLine(row, column, symbol);
```

The private helpers remain implementation details of `Board`:

```java
hasCompleteRow(...)
hasCompleteColumn(...)
hasCompleteMainDiagonal(...)
hasCompleteAntiDiagonal(...)
```

`Board` should return a boolean rather than a `Player`. The board knows symbols, but it should not know the participating players. `Game` maps the successful symbol back to `currentPlayer` and stores that player as the winner.

---

## 6. Encapsulation and invariants

### What is encapsulation?

Encapsulation means hiding internal state and exposing controlled operations.

The board array is private:

```java
private final Symbol[][] cells;
```

The caller cannot directly overwrite cells. It must use methods such as `placeSymbol` and `getSymbolAt`.

### Important invariants

An invariant is a rule that should always remain true.

Our design protects these invariants:

1. The board size is at least 3.
2. Every cell contains exactly one of `X`, `O`, or `EMPTY`.
3. An occupied cell cannot be overwritten.
4. Both players use different symbols.
5. Neither player uses `EMPTY`.
6. `X` starts.
7. A turn changes only after a valid move.
8. No moves occur after `WON` or `DRAW`.
9. `winner` is set only when the status becomes `WON`.

---

## 7. Why fields are `private final`

Example:

```java
private final int size;
private final Symbol[][] cells;
```

### `private`

Only the owning class can access the field directly. This prevents external code from bypassing validation.

### `final`

The field reference is assigned once in the constructor and cannot later point to a different object.

For an array, `final` does not make every element immutable. It means the `cells` field always references the same array. The `Board` can still modify individual cells through controlled methods.

This is useful because a board should not suddenly replace its grid while a game is running.

---

## 8. Move flow

For `game.playMove(row, column)`, the sequence is:

1. Reject the move if the game is already over.
2. Reject the move if coordinates are outside the board.
3. Reject the move if the cell is occupied.
4. Read the current player's symbol.
5. Ask the board to place that symbol.
6. Ask the board whether the last move completed a winning line.
7. If won, store the winner and set status to `WON`.
8. Otherwise, if the board is full, set status to `DRAW`.
9. Otherwise, switch the current player.
10. Return a descriptive `MoveResult`.

Checking for a win must happen before checking for a draw. The final empty cell could produce a winning move; that result is a win, not a draw.

---

## 9. Win-detection optimization

After a move at `(row, column)`, only lines containing that position can have newly become complete:

- that row;
- that column;
- the main diagonal, only when `row == column`;
- the anti-diagonal, only when `row + column == size - 1`.

Therefore, we do not scan every row and every column after every move.

Conceptually:

```java
board.hasWinningLine(row, column, symbol);
```

Inside `Board`, that method checks the last move's row, column, and applicable diagonals.

For an `N x N` board:

- row check: `O(N)`;
- column check: `O(N)`;
- diagonal check: at most `O(N)`;
- complete win check: `O(N)`;
- board storage: `O(N^2)`.

The constants do not change the final complexity, so checking up to four lines remains `O(N)`.

---

## 10. Draw detection

A simple approach scans all cells after every move, which costs `O(N^2)`.

Our board stores:

```java
private int occupiedCells;
```

It increments this after every successful placement. The board is full when:

```java
occupiedCells == size * size
```

This makes draw/full-board detection `O(1)`.

This is a deliberate space-for-time trade-off: one integer avoids repeatedly scanning the board.

---

## 11. OOP principles used

### Encapsulation

The board array and game state are private. Callers cannot change them directly.

### Abstraction

The caller uses:

```java
game.playMove(row, column);
```

It does not need to know how turns, validation, or win checking are implemented.

### Single Responsibility Principle

- `Player` stores player identity.
- `Board` manages and analyzes board state.
- `Game` orchestrates game flow and lifecycle.
- enums represent fixed domain values and outcomes.

### Composition over inheritance

`Game` contains a `Board` and players. No unnecessary subclass hierarchy is introduced.

### Open/Closed Principle

The configurable board size supports new sizes without modifying `Board` internals. However, we should not claim the entire design is fully open for all rule changes. Different winning policies would require further abstraction.

### Defensive programming

Constructors reject invalid setup, while `playMove` returns clear rejection results for normal user mistakes.

---

## 12. Why no design pattern is forced into Phase 1

Phase 1 does not require Strategy, Factory, Observer, Command, or State patterns.

The code already has clear responsibilities using basic OOP. Adding interfaces merely to name a pattern would create unnecessary complexity.

Potential future uses:

- **Strategy Pattern**: different winning rules or AI move strategies;
- **Command Pattern**: undo, redo, and move replay;
- **Observer Pattern**: notify UI, spectators, or analytics after moves;
- **Factory Pattern**: construct different game modes when creation becomes complex.

Patterns should solve changing requirements, not decorate simple code.

---

## 13. Important edge cases

1. Board size below 3.
2. Null player.
3. Blank player name.
4. Player chooses `EMPTY`.
5. Both players choose `X` or both choose `O`.
6. Negative row or column.
7. Row or column greater than or equal to `N`.
8. Cell already occupied.
9. Invalid move should not switch the player.
10. Win in any row.
11. Win in any column.
12. Win in the main diagonal.
13. Win in the anti-diagonal.
14. Win on the final available cell.
15. Full board with no winner.
16. Move attempted after win.
17. Move attempted after draw.

---

## 14. Interview explanation

A concise answer:

> I model Tic Tac Toe as a small stateful domain. `Board` owns and protects an `N x N` grid and performs board-related analysis, including win detection. `Player` stores identity and a chosen `Symbol`. `Game` is a thin orchestrator for turns and lifecycle. `Game.playMove` rejects an invalid or terminal move, delegates placement to `Board`, asks `Board` whether the last move won in `O(N)`, then records a win, draw, or switches the player. The design uses Information Expert, encapsulation, composition, and single responsibility without forcing a design pattern into Phase 1.

### Follow-up: Why does `Board.placeSymbol` exist if `Game` validates the move?

Both layers protect different responsibilities:

- `Game` validates game workflow, such as whether the game is still running.
- `Board` protects its own grid invariants, such as bounds and occupancy.

This prevents the board from accepting invalid state changes even if it is reused or called incorrectly.

### Follow-up: Why does `X` start instead of player one?

The requirement says players choose symbols and `X` begins. Therefore, the constructor selects whichever player owns `X`:

```java
currentPlayer = playerOne.getSymbol() == Symbol.X ? playerOne : playerTwo;
```

### Follow-up: Why return `MoveResult` instead of throwing exceptions?

Out-of-bounds and occupied-cell attempts are expected gameplay outcomes, not necessarily programming defects. Returning a result lets the caller display an appropriate message. Constructors throw exceptions for invalid object configuration because such configuration is a programming/setup error.

---

## 15. Phase 1 class outline

```text
TicTacToe
 ├── enum Symbol
 ├── enum GameStatus
 ├── enum MoveResult
 ├── class Player
 ├── class Board
 ├── class Game
 └── main demonstration
```

They are nested in one file for learning convenience. In a production project, each top-level type can be moved into a separate file without changing the design.

---

## 16. What to practice

1. Write `Player`, `Board`, and the enums without looking.
2. Explain why `placeSymbol` belongs to `Board`.
3. Explain why win detection belongs to `Board`, but the winning `Player` belongs to `Game`.
4. Write `playMove` in the correct validation order.
5. Write row and column checks inside `Board`.
6. Derive both diagonal formulas:
   - main diagonal: `row == column`;
   - anti-diagonal: `row + column == size - 1`.
7. Explain why an invalid move does not switch turns.
8. Explain `O(N)` win checking and `O(N^2)` board storage.
9. Explain where Information Expert, encapsulation, and SRP appear.
10. Test a row win, column win, both diagonal wins, a draw, and invalid moves.
