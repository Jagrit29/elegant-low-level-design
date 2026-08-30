package practice2026;

///
enum Symbol {
    X,
    O, 
    EMPTY
}

class Player {
    private final String name;
    private final Symbol symbol;

    public Player(String name, Symbol symbol) {
        this.name = name;
        this.symbol = symbol;
    }

}

class Board {
    private final int size;
    private final Symbol[][] cells;

    public Board(int size) {
        this.size = size;
        this.cells = new Symbol[size][size];
    }

}

enum GameStatus {
    IN_PROGRESS,
    DRAW,
    WON
}

class Game {
    private final Board board;
    private final Player playerOne;
    private final Player playerTwo;

    private Player currentPlayer;
    private GameStatus gameStatus;
}



public class TicTacToe {
    public static void main(String args[]) {
        System.out.println("hello");
    }
}

/* Questions
1. Why private final?

*/