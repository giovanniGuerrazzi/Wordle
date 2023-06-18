import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

// Implementazione interfaccia Server per la registrazione dell'utente
public class RegistrationRmi extends RemoteObject implements RegistrationRmiInterface {
	
	// Default serial version ID
	private static final long serialVersionUID = 1L;
	
	/*	DataBase che contiene l'insieme degli utenti registrati al Server con associata la password
	 	Lo username deve essere unico	*/
	private ConcurrentHashMap<String,String> DataBase;
	// Lista di utenti registrati con tutte le loro statistiche
	private ArrayList<User> StatsUsers;
	
	public RegistrationRmi(ConcurrentHashMap<String,String> DataBase,ArrayList<User> StatsUsers) throws RemoteException {
		this.DataBase	= DataBase;
		this.StatsUsers	= StatsUsers;
	}
	
	// Funzione che aggiunge un'utente nuovo se lo username non è gia stato preso
	public void addUser(String username,String password) throws RemoteException {
		// Ritorna un valore diverso da null se 'username' già presente
		if(DataBase.putIfAbsent(username, password) != null) {
			throw new RemoteException();
		}
		else {
			System.out.printf("Correttamente registrato utente: <%s> \n", username,password);
			User utente = new User(username,password);
			StatsUsers.add(utente);
		}
	}
	
}
