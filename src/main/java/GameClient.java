import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameClient {
    private static String playerName;
    private static GameInterface game;
    private static JFrame frame;
    private static JPanel boardPanel;
    private static int[][] points;
    private static List<Point> selectedPoints = new ArrayList<>();
    private static String currentPlayer = "";

    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            game = (GameInterface) registry.lookup("GameServer");
            connectToServer();

            //SwingUtilities.invokeLater(() -> createAndShowGUI());
            createAndShowGUI();

            new Thread(GameClient::updateGameLoop).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void connectToServer() throws Exception {
        try
        {
            playerName = game.connectPlayer();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("Connected as " + playerName);
    }

    private static void createAndShowGUI() {
        frame = new JFrame("Dots and Boxes " + playerName);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);

        boardPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawBoard(g);
            }
        };
        boardPanel.setPreferredSize(new Dimension(300, 300));
        boardPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e);
            }
        });

        frame.add(boardPanel);
        frame.pack();
        frame.setVisible(true);
    }

    private static void drawBoard(Graphics g) {

        g.clearRect(0, 0, boardPanel.getWidth(), boardPanel.getHeight());
        // Рисуем 9 точек в виде сетки 3x3
        int margin = 50, spacing = 80;
        points = new int[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int x = margin + j * spacing;
                int y = margin + i * spacing;
                g.fillOval(x - 5, y - 5, 5, 5);
                points[i][j] = x + y;
            }
        }

        try {
            // Получаем текущее состояние игры и рисуем линии
            String gameState = game.getGameState();
            String[] edges = gameState.split(" ");
            g.setColor(Color.BLUE);

            for (String edge : edges) {
                if (edge.isEmpty()) continue;
                String[] parts = edge.split("-");
                String[] p1 = parts[0].split(",");
                String[] p2 = parts[1].split(",");
                int x1 = Integer.parseInt(p1[0]);
                int y1 = Integer.parseInt(p1[1]);
                int x2 = Integer.parseInt(p2[0]);
                int y2 = Integer.parseInt(p2[1]);

                g.drawLine(50 + x1 * 80, 50 + y1 * 80, 50 + x2 * 80, 50 + y2 * 80);
            }

            // Получаем информацию о квадратах и отображаем имена игроков
            Map<String, String> squaresState = game.getSquaresState();
            g.setColor(Color.RED);

            for (Map.Entry<String, String> entry : squaresState.entrySet()) {
                String squareKey = entry.getKey();
                String playerName = entry.getValue();

                // Декодируем квадрат и вычисляем его центр
                int x1 = Character.getNumericValue(squareKey.charAt(0));
                int y1 = Character.getNumericValue(squareKey.charAt(1));
                int xCenter = 50 + x1 * 80 + 40; // Центр квадрата по x
                int yCenter = 50 + y1 * 80 + 40; // Центр квадрата по y

                g.drawString(playerName, xCenter - 10, yCenter + 5);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Показываем текущего игрока
        g.setColor(Color.BLACK);
        g.drawString("Current Player: " + currentPlayer, 10, 10);
        try {
            g.drawString(game.getWinnerString(), 10, 30);
        } catch (RemoteException e) {

        }

    }


    private static void handleMouseClick(MouseEvent e) {
        int margin = 50, spacing = 80;
        int x = (e.getX() - margin + spacing / 2) / spacing;
        int y = (e.getY() - margin + spacing / 2) / spacing;


        //if (Math.abs(clickX - centerX) <= regionSize && Math.abs(clickY - centerY) <= regionSize) не работает...

        if (x < 0 || x >= 3 || y < 0 || y >= 3) return; // Клик вне поля
        selectedPoints.add(new Point(x, y));


        if (selectedPoints.size() == 2) {
            Point p1 = selectedPoints.get(0);
            Point p2 = selectedPoints.get(1);

            try {
                if (game.makeMove(playerName, p1.x, p1.y, p2.x, p2.y)) {
                    selectedPoints.clear();
                    boardPanel.repaint();
                } else {
                    JOptionPane.showMessageDialog(frame, "Invalid move or not your turn!");
                    selectedPoints.clear();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }


    private static void updateGameLoop() {
        while (true) {
            try {
                String newCurrentPlayer = game.getCurrentPlayer();
                if (!newCurrentPlayer.equals(currentPlayer)) {
                    currentPlayer = newCurrentPlayer;
                }
                boardPanel.repaint();
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
