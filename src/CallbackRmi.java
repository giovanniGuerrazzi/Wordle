import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class CallbackRmi extends RemoteObject implements CallbackRmiInterface {

	// Default serial version ID
	private static final long serialVersionUID = 1L;
		
	// Lista dei client registrati per l'aggiornamento classifica
	private ArrayList <NotifyEventRmiInterface> Clients;
	
	public CallbackRmi () throws RemoteException {
		super();
		this.Clients	= new ArrayList<NotifyEventRmiInterface>();
	}

	// Funzione che registra il client per la callback
	public synchronized void registerForCallback(NotifyEventRmiInterface clientInterface) throws RemoteException {
		if(!Clients.contains(clientInterface)) {
			Clients.add(clientInterface);
			System.out.println("Client registered");
		}
	}
		
	// Funzione che cancella la registrazione del client per la callback
	public synchronized void unregisterForCallback (NotifyEventRmiInterface clientInterface) throws RemoteException{
		if (Clients.remove(clientInterface)){
			System.out.println("Client unregistered");
		}
		else { 
			System.out.println("Unable to unregister client."); 
		}
	}
		
	// Funzione che notifica ai clienti registrati che c'è stato un cambiamento nelle prime 3 posizioni tramite l'invocazione di 'doCallbacks'
	public void update(ArrayList<UserRank> TopThree) throws RemoteException{
		doCallbacks(TopThree);
	}
		
	// Funzione che notifica ai clienti registrati che c'è stato un cambiamento nelle prime 3 posizioni
	private synchronized void doCallbacks(ArrayList<UserRank> TopThree) throws RemoteException{ 
		System.out.println("Starting callbacks...");
		Iterator<NotifyEventRmiInterface> i = Clients.iterator();
		while (i.hasNext()){
			try {
				NotifyEventRmiInterface client = (NotifyEventRmiInterface) i.next();
				client.notifyNewTopThree(TopThree);
			}
			catch(NoSuchElementException e) {
				System.err.println("Non ci sono piu' elementi da scorrere");
				e.printStackTrace();
			}
		}
		System.out.println("Callbacks complete.");
	}

}
