import java.rmi.Remote;
import java.rmi.RemoteException;

// Interfaccia server remoto (RMI)
public interface RegistrationRmiInterface extends Remote{
		
	// Servizio per registrare un nuovo utente tramite RMI
	public void addUser(String username,String password) throws RemoteException;
	
}
