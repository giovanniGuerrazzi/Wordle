import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.ArrayList;

// Implementazione dell'interfaccia del client remoto (RMI)
public class NotifyEventRmi extends RemoteObject implements NotifyEventRmiInterface {
	
	// Default serial version ID
	private static final long serialVersionUID = 1L;
	public ArrayList<UserRank> TopThree;

	public NotifyEventRmi() throws RemoteException { 
		super(); 
		this.TopThree = new ArrayList<UserRank>(3);
	}
	
	// Metodo che può essere chiamato dal server per notificare un cambiamento in classifica
	public void notifyNewTopThree(ArrayList<UserRank> TopThree) throws RemoteException {
		String returnMessage = " ( E' cambiata la TOP 3! )";
		this.TopThree = TopThree;
		System.out.print(returnMessage); 
	}
	
}

