import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface GameInterface extends Remote {
    String connectPlayer() throws RemoteException;
    boolean makeMove(String playerName, int x1, int y1, int x2, int y2) throws RemoteException;
    String getGameState() throws RemoteException;
    String getCurrentPlayer() throws RemoteException;
    String getWinnerString() throws RemoteException;
    Map<String, String> getSquaresState() throws RemoteException; // Новый метод
}
