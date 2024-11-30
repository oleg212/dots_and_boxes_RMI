import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public class GameServer extends UnicastRemoteObject implements GameInterface {
    private static final int SIZE = 3; // Размер игрового поля (3x3 точки)
    private final Set<String> edges; // Ребра между точками
    private final Map<String, String> squares; // Квадраты и их владельцы
    private String currentPlayer; // Текущий игрок
    private final String[] players; // Имена игроков
    private final int[] scores; // Очки игроков
    private int playerCount;
    private String lastWinner = "";

    protected GameServer() throws RemoteException {
        super();
        this.edges = new HashSet<>();
        this.squares = new HashMap<>();
        this.players = new String[2];
        this.scores = new int[2];
        players[0] = "PlayerA";
        players[1] = "PlayerB";
        this.playerCount = 0;
        this.currentPlayer = players[0];
    }

    protected void resetGame() throws RemoteException
    {
        edges.clear();
        squares.clear();
        scores[0] = 0;
        scores[1] = 0;
    }

    @Override
    public synchronized String connectPlayer() throws RemoteException {
        if (playerCount == 0) {
            playerCount++;
            return "PlayerA";
        } else if (playerCount == 1) {
            playerCount++;
            return "PlayerB";
        } else {
            throw new RemoteException("Game is full!");
        }
    }

    public synchronized String getWinnerString() throws RemoteException {
        return lastWinner;
    }

    @Override
    public synchronized Map<String, String> getSquaresState() throws RemoteException {
        return new HashMap<>(squares);
    }


    @Override
    public synchronized boolean makeMove(String playerName, int x1, int y1, int x2, int y2) throws RemoteException {
        if (!playerName.equals(currentPlayer)) return false; // Ход не этого игрока

        if (!isValidMove(x1, y1, x2, y2)) return false; // Невалидный ход

        String edge = encodeEdge(x1, y1, x2, y2);
        if (edges.contains(edge)) return false; // Уже занято

        edges.add(edge);

        boolean scored = checkSquares(x1, y1, x2, y2, playerName);
        if (!scored) switchTurn();

        if (gameFinished())
        {
            resetGame();
            lastWinner = findWinner();
        }

        return true;
    }

    private synchronized boolean gameFinished()
    {
        if (edges.size() == 12) return true;
        if (scores[0] == 3 || scores[1] == 3) return true;
        return false;
    }

    private synchronized String findWinner() throws RemoteException
    {
        if (scores[0] >scores[1])
            return players[0];

        if (scores[1] >scores[0])
            return  players[1];

        return "";
    }


    @Override
    public synchronized String getGameState() throws RemoteException {
        StringBuilder state = new StringBuilder();
        for (String edge : edges) {
            state.append(edge).append(" ");
        }
        return state.toString().trim();
    }

    @Override
    public synchronized String getCurrentPlayer() throws RemoteException {
        return currentPlayer;
    }

    private boolean isValidMove(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2) == 1;
    }

    private String encodeEdge(int x1, int y1, int x2, int y2) {
        // Кодируем ребро в виде "x1,y1-x2,y2" или "x2,y2-x1,y1" для уникальности
        if (x1 > x2 || (x1 == x2 && y1 > y2)) {
            return x2 + "," + y2 + "-" + x1 + "," + y1;
        }
        return x1 + "," + y1 + "-" + x2 + "," + y2;
    }

    private void switchTurn() {
        currentPlayer = currentPlayer.equals(players[0]) ? players[1] : players[0];
    }

    private boolean checkSquares(int x1, int y1, int x2, int y2, String playerName) {
        boolean scored = false;

        // Проверяем все квадраты, для которых (x1, y1) может быть верхним левым углом
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                int topLeftX = x1 - dx;
                int topLeftY = y1 - dy;

                if (topLeftX >= 0 && topLeftY >= 0 && topLeftX + 1 < SIZE && topLeftY + 1 < SIZE) {
                    String squareKey = encodeSquare(topLeftX, topLeftY);

                    if (!squares.containsKey(squareKey) && isSquareCompleted(topLeftX, topLeftY)) {
                        squares.put(squareKey, playerName);
                        scored = true;
                        incrementScore(playerName);
                    }
                }
            }
        }

        return scored;
    }

    private String encodeSquare(int topLeftX, int topLeftY) {
        // Кодируем квадрат в виде "x1y1x2y2x3y3x4y4" (по часовой стрелке)
        return topLeftX + "" + topLeftY +
                (topLeftX + 1) + "" + topLeftY +
                (topLeftX + 1) + "" + (topLeftY + 1) +
                topLeftX + "" + (topLeftY + 1);
    }

    private boolean isSquareCompleted(int topLeftX, int topLeftY) {
        // Проверяем наличие всех четырех сторон квадрата
        return edges.contains(encodeEdge(topLeftX, topLeftY, topLeftX + 1, topLeftY)) &&
                edges.contains(encodeEdge(topLeftX + 1, topLeftY, topLeftX + 1, topLeftY + 1)) &&
                edges.contains(encodeEdge(topLeftX + 1, topLeftY + 1, topLeftX, topLeftY + 1)) &&
                edges.contains(encodeEdge(topLeftX, topLeftY + 1, topLeftX, topLeftY));
    }

    private void incrementScore(String playerName) {
        if (playerName.equals(players[0])) {
            scores[0]++;
        } else if (playerName.equals(players[1])) {
            scores[1]++;
        }
    }

    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("GameServer", new GameServer());
            System.out.println("GameServer is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
