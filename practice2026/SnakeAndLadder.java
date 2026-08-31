package practice2026;
/*
Requirements -> 
Snake and Ladder Game 
1. N players can play this game
2. There is a board which has snakes and ladder
3. Snake will have head and tail
4. Ladder will have start and end
5. Players will rol dice 

Core entities 
Player - name, position
Dice - roll()
Snake - head, tail
Ladder - bottom, top
Board - Map<Snakes>, Map<LAdders>, int size;
Game - board, players, currentPlayer, playGame,
*/

class Snake {
    private final int head;
    private final int tail;

    public Snake(int head, int tail) {
        this.head = head;
        this.tail = tail;
    }
}

class Ladder {
    private final int bottom;
    private final int top;

    public Ladder(int bottom, int top) {
        this.bottom = bottom;
        this.top = top;
    }

}

class Dice {
    int size;

    public Dice(int size) {
        this.size = size;
    }
    public int rollDice() {
        // return random number;
        return 2;
    }
}

class Player {
    private final String name;
    private int position;

    public Player(String name, int position) {
        this.name = name;
        this.position = position;
    }
}



public class SnakeAndLadder {
    
}
