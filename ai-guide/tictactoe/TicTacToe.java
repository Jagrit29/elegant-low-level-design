public class TicTacToe {

    enum Symbol {
        X,
        O,
        EMPTY
    }

    enum GameStatus {
        IN_PROGRESS,
        WON,
        DRAW
    }

    enum MoveResult {
        MOVE_ACCEPTED,
        PLAYER_WON,
        DRAW,
        OUT_OF_BOUNDS,
        CELL_OCCUPIED,
        GAME_ALREADY_OVER
    }

    static final class Player {
        private final String name;
        private final Symbol symbol;

        public Player(String name, Symbol symbol) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Player name cannot be blank");
            }
            if (symbol == null || symbol == Symbol.EMPTY) {
                throw new IllegalArgumentException("A player must choose X or O");
            }

            this.name = name;
            this.symbol = symbol;
        }

        public String getName() {
            return name;
        }

        public Symbol getSymbol() {
            return symbol;
        }

        @Override
        public String toString() {
            return name + " (" + symbol + ")";
        }
    }

    static final class Board {
        private final int size;
        private final Symbol[][] cells;
        private int occupiedCells;

        public Board(int size) {
            if (size < 3) {
                throw new IllegalArgumentException("Board size must be at least 3");
            }

            this.size = size;
            this.cells = new Symbol[size][size];
            this.occupiedCells = 0;

            for (int row = 0; row < size; row++) {
                for (int column = 0; column < size; column++) {
                    cells[row][column] = Symbol.EMPTY;
                }
            }
        }

        public int getSize() {
            return size;
        }

        public boolean isWithinBounds(int row, int column) {
            return row >= 0 && row < size && column >= 0 && column < size;
        }

        public boolean isCellEmpty(int row, int column) {
            return isWithinBounds(row, column) && cells[row][column] == Symbol.EMPTY;
        }

        public boolean placeSymbol(int row, int column, Symbol symbol) {
            if (symbol == null || symbol == Symbol.EMPTY) {
                return false;
            }
            if (!isCellEmpty(row, column)) {
                return false;
            }

            cells[row][column] = symbol;
            occupiedCells++;
            return true;
        }

        public Symbol getSymbolAt(int row, int column) {
            if (!isWithinBounds(row, column)) {
                throw new IllegalArgumentException("Position is outside the board");
            }
            return cells[row][column];
        }

        public boolean isFull() {
            return occupiedCells == size * size;
        }

        public boolean hasWinningLine(int lastRow, int lastColumn, Symbol symbol) {
            if (!isWithinBounds(lastRow, lastColumn)
                    || symbol == null
                    || symbol == Symbol.EMPTY
                    || cells[lastRow][lastColumn] != symbol) {
                return false;
            }

            return hasCompleteRow(lastRow, symbol)
                    || hasCompleteColumn(lastColumn, symbol)
                    || (lastRow == lastColumn && hasCompleteMainDiagonal(symbol))
                    || (lastRow + lastColumn == size - 1
                        && hasCompleteAntiDiagonal(symbol));
        }

        private boolean hasCompleteRow(int row, Symbol symbol) {
            for (int column = 0; column < size; column++) {
                if (cells[row][column] != symbol) {
                    return false;
                }
            }
            return true;
        }

        private boolean hasCompleteColumn(int column, Symbol symbol) {
            for (int row = 0; row < size; row++) {
                if (cells[row][column] != symbol) {
                    return false;
                }
            }
            return true;
        }

        private boolean hasCompleteMainDiagonal(Symbol symbol) {
            for (int index = 0; index < size; index++) {
                if (cells[index][index] != symbol) {
                    return false;
                }
            }
            return true;
        }

        private boolean hasCompleteAntiDiagonal(Symbol symbol) {
            for (int row = 0; row < size; row++) {
                int column = size - 1 - row;
                if (cells[row][column] != symbol) {
                    return false;
                }
            }
            return true;
        }

        public void print() {
            System.out.println();
            for (int row = 0; row < size; row++) {
                for (int column = 0; column < size; column++) {
                    Symbol symbol = cells[row][column];
                    String value = symbol == Symbol.EMPTY ? " " : symbol.name();
                    System.out.print(" " + value + " ");
                    if (column < size - 1) {
                        System.out.print("|");
                    }
                }
                System.out.println();

                if (row < size - 1) {
                    System.out.println("---+".repeat(size - 1) + "---");
                }
            }
            System.out.println();
        }
    }

    static final class Game {
        private final Board board;
        private final Player playerOne;
        private final Player playerTwo;

        private Player currentPlayer;
        private Player winner;
        private GameStatus status;

        public Game(int boardSize, Player playerOne, Player playerTwo) {
            validatePlayers(playerOne, playerTwo);

            this.board = new Board(boardSize);
            this.playerOne = playerOne;
            this.playerTwo = playerTwo;
            this.currentPlayer = playerOne.getSymbol() == Symbol.X ? playerOne : playerTwo;
            this.status = GameStatus.IN_PROGRESS;
        }

        private void validatePlayers(Player first, Player second) {
            if (first == null || second == null) {
                throw new IllegalArgumentException("Exactly two players are required");
            }
            if (first.getSymbol() == second.getSymbol()) {
                throw new IllegalArgumentException("Players must choose different symbols");
            }
        }

        public MoveResult playMove(int row, int column) {
            if (status != GameStatus.IN_PROGRESS) {
                return MoveResult.GAME_ALREADY_OVER;
            }
            if (!board.isWithinBounds(row, column)) {
                return MoveResult.OUT_OF_BOUNDS;
            }
            if (!board.isCellEmpty(row, column)) {
                return MoveResult.CELL_OCCUPIED;
            }

            Symbol symbol = currentPlayer.getSymbol();
            board.placeSymbol(row, column, symbol);

            if (board.hasWinningLine(row, column, symbol)) {
                winner = currentPlayer;
                status = GameStatus.WON;
                return MoveResult.PLAYER_WON;
            }

            if (board.isFull()) {
                status = GameStatus.DRAW;
                return MoveResult.DRAW;
            }

            switchPlayer();
            return MoveResult.MOVE_ACCEPTED;
        }

        private void switchPlayer() {
            currentPlayer = currentPlayer == playerOne ? playerTwo : playerOne;
        }

        public Board getBoard() {
            return board;
        }

        public Player getCurrentPlayer() {
            return currentPlayer;
        }

        public Player getWinner() {
            return winner;
        }

        public GameStatus getStatus() {
            return status;
        }
    }

    private static void makeMove(Game game, int row, int column) {
        Player player = game.getCurrentPlayer();
        MoveResult result = game.playMove(row, column);

        System.out.printf("%s chooses (%d, %d): %s%n", player, row, column, result);
        game.getBoard().print();
    }

    public static void main(String[] args) {
        Player alice = new Player("Alice", Symbol.O);
        Player bob = new Player("Bob", Symbol.X);
        Game game = new Game(3, alice, bob);

        System.out.println("X starts automatically: " + game.getCurrentPlayer());
        game.getBoard().print();

        makeMove(game, 0, 0); // Bob (X)
        makeMove(game, 1, 0); // Alice (O)
        makeMove(game, 0, 1); // Bob (X)
        makeMove(game, 1, 1); // Alice (O)
        makeMove(game, 0, 2); // Bob (X) wins
        makeMove(game, 2, 2); // Rejected because the game is over

        System.out.println("Final status: " + game.getStatus());
        if (game.getWinner() != null) {
            System.out.println("Winner: " + game.getWinner());
        }
    }
}
