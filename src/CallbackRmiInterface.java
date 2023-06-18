import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

//Interfaccia server remoto (RMI)
public interface CallbackRmiInterface extends Remote {
	
	// Servizio per registrare il client al sistema di notifica di cambiamento della classifica
	public void registerForCallback(NotifyEventRmiInterface clientInterface) throws RemoteException;
		
	// Servizio per rimuovere la registrazione dal sistema di notifica di cambiamento della classifica
	public void unregisterForCallback(NotifyEventRmiInterface clientInterface) throws RemoteException;
	
	// Funzione che notifica ai clienti registrati che c'è stato un cambiamento nelle prime 3 posizioni
	public void update(ArrayList<UserRank> TopThree) throws RemoteException;
	
}
