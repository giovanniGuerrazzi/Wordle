import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

// Interfaccia client remoto (RMI)
public interface NotifyEventRmiInterface extends Remote {
	
	// Metodo invocato dal server per notificare il cambiamento della top 3 ai client registrati
	public void notifyNewTopThree(ArrayList<UserRank> TopThree) throws RemoteException;
		
}
