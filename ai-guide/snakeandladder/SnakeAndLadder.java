import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class SnakeAndLadder {

    enum GameStatus {
        IN_PROGRESS,
        WON
    }

    static final class Snake {
        private final int head;
        private final int tail;

        public Snake(int head, int tail) {
            if (head <= 0 || tail <= 0) {
                throw new IllegalArgumentException("Snake positions must be positive");
            }
            if (head <= tail) {
                throw new IllegalArgumentException("A snake's head must be above its tail");
            }

            this.head = head;
            this.tail = tail;
        }

        public int getHead() {
            return head;
        }

        public int getTail() {
            return tail;
        }
    }

    static final class Ladder {
        private final int bottom;
        private final int top;

        public Ladder(int bottom, int top) {
            if (bottom <= 0 || top <= 0) {
                throw new IllegalArgumentException("Ladder positions must be positive");
            }
            if (bottom >= top) {
                throw new IllegalArgumentException("A ladder's bottom must be below its top");
            }

            this.bottom = bottom;
            this.top = top;
        }

        public int getBottom() {
            return bottom;
        }

        public int getTop() {
            return top;
        }
    }

    interface Dice {
        int roll();
    }

    static final class RandomDice implements Dice {
        private final int numberOfSides;

        public RandomDice(int numberOfSides) {
            if (numberOfSides < 2) {
                throw new IllegalArgumentException("A die must have at least two sides");
            }
            this.numberOfSides = numberOfSides;
        }

        @Override
        public int roll() {
            return ThreadLocalRandom.current().nextInt(1, numberOfSides + 1);
        }
    }

    static final class SequenceDice implements Dice {
        private final Queue<Integer> rolls;

        public SequenceDice(int... rolls) {
            if (rolls == null || rolls.length == 0) {
                throw new IllegalArgumentException("At least one scripted roll is required");
            }

            this.rolls = new ArrayDeque<>();
            for (int roll : rolls) {
                if (roll <= 0) {
                    throw new IllegalArgumentException("Scripted rolls must be positive");
                }
                this.rolls.offer(roll);
            }
        }

        @Override
        public int roll() {
            if (rolls.isEmpty()) {
                throw new IllegalStateException("No scripted rolls remain");
            }
            return rolls.remove();
        }
    }

    static final class Player {
        private final String name;
        private int position;

        public Player(String name) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Player name cannot be blank");
            }

            this.name = name;
            this.position = 0;
        }

        public String getName() {
            return name;
        }

        public int getPosition() {
            return position;
        }

        private void moveTo(int position) {
            if (position < 0) {
                throw new IllegalArgumentException("Player position cannot be negative");
            }
            this.position = position;
        }

        @Override
        public String toString() {
            return name + " at " + position;
        }
    }

    static final class Board {
        private final int size;
        private final Map<Integer, Snake> snakesByHead;
        private final Map<Integer, Ladder> laddersByBottom;

        public Board(int size, List<Snake> snakes, List<Ladder> ladders) {
            if (size < 2) {
                throw new IllegalArgumentException("Board size must be at least 2");
            }
            if (snakes == null || ladders == null) {
                throw new IllegalArgumentException("Snake and ladder collections cannot be null");
            }

            this.size = size;
            this.snakesByHead = new HashMap<>();
            this.laddersByBottom = new HashMap<>();
            initializeSnakes(snakes);
            initializeLadders(ladders);
        }

        private void initializeSnakes(List<Snake> snakes) {
            for (Snake snake : snakes) {
                if (snake == null) {
                    throw new IllegalArgumentException("Snake cannot be null");
                }
                validatePositionWithinBoard(snake.getHead());
                validatePositionWithinBoard(snake.getTail());
                validateTransitionStart(snake.getHead());
                snakesByHead.put(snake.getHead(), snake);
            }
        }

        private void initializeLadders(List<Ladder> ladders) {
            for (Ladder ladder : ladders) {
                if (ladder == null) {
                    throw new IllegalArgumentException("Ladder cannot be null");
                }
                validatePositionWithinBoard(ladder.getBottom());
                validatePositionWithinBoard(ladder.getTop());
                validateTransitionStart(ladder.getBottom());
                laddersByBottom.put(ladder.getBottom(), ladder);
            }
        }

        private void validatePositionWithinBoard(int position) {
            if (position <= 0 || position > size) {
                throw new IllegalArgumentException("Transition position is outside the board");
            }
        }

        private void validateTransitionStart(int position) {
            if (position == size) {
                throw new IllegalArgumentException("A transition cannot start at the winning cell");
            }
            if (snakesByHead.containsKey(position) || laddersByBottom.containsKey(position)) {
                throw new IllegalArgumentException("Only one transition may start at a position");
            }
        }

        public int getSize() {
            return size;
        }

        public boolean isOvershoot(int position) {
            return position > size;
        }

        public boolean isWinningPosition(int position) {
            return position == size;
        }

        public int resolvePosition(int position) {
            validatePositionWithinBoard(position);

            Snake snake = snakesByHead.get(position);
            if (snake != null) {
                return snake.getTail();
            }

            Ladder ladder = laddersByBottom.get(position);
            if (ladder != null) {
                return ladder.getTop();
            }

            return position;
        }
    }

    static final class Game {
        private final Board board;
        private final List<Player> players;
        private final Dice dice;

        private int currentPlayerIndex;
        private Player winner;
        private GameStatus status;

        public Game(Board board, List<Player> players, Dice dice) {
            if (board == null || dice == null) {
                throw new IllegalArgumentException("Board and dice are required");
            }
            validatePlayers(players);

            this.board = board;
            this.players = List.copyOf(players);
            this.dice = dice;
            this.currentPlayerIndex = 0;
            this.status = GameStatus.IN_PROGRESS;
        }

        private void validatePlayers(List<Player> players) {
            if (players == null || players.size() < 2) {
                throw new IllegalArgumentException("At least two players are required");
            }

            Set<String> names = new HashSet<>();
            for (Player player : players) {
                if (player == null) {
                    throw new IllegalArgumentException("Player cannot be null");
                }
                if (!names.add(player.getName())) {
                    throw new IllegalArgumentException("Player names must be unique");
                }
            }
        }

        public void playTurn() {
            if (status != GameStatus.IN_PROGRESS) {
                System.out.println("Game is already over");
                return;
            }

            Player player = getCurrentPlayer();
            int previousPosition = player.getPosition();
            int rolledValue = dice.roll();
            int landedPosition = previousPosition + rolledValue;

            System.out.printf("%s rolled %d", player.getName(), rolledValue);

            if (board.isOvershoot(landedPosition)) {
                System.out.printf(
                        " and stays at %d because the move overshoots%n",
                        previousPosition
                );
                switchPlayer();
                return;
            }

            int finalPosition = board.resolvePosition(landedPosition);
            player.moveTo(finalPosition);

            if (finalPosition < landedPosition) {
                System.out.printf(
                        ": %d -> %d -> %d (snake)%n",
                        previousPosition,
                        landedPosition,
                        finalPosition
                );
            } else if (finalPosition > landedPosition) {
                System.out.printf(
                        ": %d -> %d -> %d (ladder)%n",
                        previousPosition,
                        landedPosition,
                        finalPosition
                );
            } else {
                System.out.printf(": %d -> %d%n", previousPosition, finalPosition);
            }

            if (board.isWinningPosition(player.getPosition())) {
                winner = player;
                status = GameStatus.WON;
                System.out.println(player.getName() + " wins!");
                return;
            }

            switchPlayer();
        }

        private void switchPlayer() {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        }

        public Player getCurrentPlayer() {
            return players.get(currentPlayerIndex);
        }

        public Player getWinner() {
            return winner;
        }

        public GameStatus getStatus() {
            return status;
        }
    }

    public static void main(String[] args) {
        Board board = new Board(
                20,
                List.of(new Snake(14, 7)),
                List.of(new Ladder(3, 11))
        );

        Player alice = new Player("Alice");
        Player bob = new Player("Bob");

        Dice dice = new SequenceDice(3, 4, 3, 6, 6, 4, 6, 6, 1);
        Game game = new Game(board, List.of(alice, bob), dice);

        while (game.getStatus() == GameStatus.IN_PROGRESS) {
            game.playTurn();
        }

        System.out.println("Winner: " + game.getWinner());
        game.playTurn();

        // Use this in a real game: Dice randomDice = new RandomDice(6);
    }
}
