import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

public class WordleServerMain {
	
	private static String 			serviceRegistrationName;		//	"WORDLE-REGISTRATION"
	private static String 			serviceCallbackName;			//	"WORDLE-CALLBACK"
	private static int 				RMIport;						//	9999 	
	private static int 				TCPport; 						//	8888
	private static int 				UDPport; 						//	7777
	private static int 				multicastPort;					//	9898
	private static String 			multicastAddress;				//	239.250.250.255
	private static String 			host;							//	"localhost"
	private static String 			storageFileDataBase;			//	"./wordle3punto0/Storage/DataBase.json"
	private static String 			storageFileStatsUsers;			//	"./wordle3punto0/Storage/StatsUsers.json"
	private static String 			storageFileRank;				//	"./wordle3punto0/Storage/Rank.json"
	private static String 			vocabolarioWordle;				//	"./wordle3punto0/VocabolarioWordle/words.txt"
	private static int				newWordTime; 					//	180000 ms ovvero 3 minuti
	
	// Lock per accesso a strutture dati condivise in mutua esclusione 
	private static ReentrantLock 	StatsUsersLock		= new ReentrantLock();
	private static ReentrantLock 	RankLock 			= new ReentrantLock();
	
	private static ReentrantLock 	DataBaseFileLock	= new ReentrantLock();
	private static ReentrantLock 	StatsUsersFileLock	= new ReentrantLock();
	private static ReentrantLock 	RankFileLock		= new ReentrantLock();
	
	// Parola da indovinare. Ogni 'newWordTime' millisecondi cambia
	private static String								SECRETWORDWORDLE;
    private static final int							vocabolaryDim 		= 30824;
    
    private static final int							saveTime 			= 10000;
    private static boolean 								end					= false;	
    public static final int 							terminationDelay	= 60000;
    private static boolean 								top3Change			= false;
    
    // Strutture dati 
    private static ArrayList<String>					Vocabolary;
    private static ArrayList<String>					SecretsWords 		= new ArrayList<String>();
    //	------	PRINCIPALI	------	//
    private static ConcurrentHashMap<String,String> 	DataBase;
	private static ArrayList<User>						StatsUsers;
	private static LinkedList<UserRank>					Rank;
	//	------	PRINCIPALI	------	//
	// Conterrà tutti i risultati inerenti alle partite condivisi dagli utenti
	private static ArrayList<String> 					AllResult			= new ArrayList<String>();
	    
    // Thread per l'estrazione di una nuova parola da indovinare per WORDLE
    private static Thread 	threadGenNewWord;
    // Thread per la ricezione di messaggi UDP da parte dei client
    private static Thread	threadListener;
    // Thread per il salvataggio delle strutture
    private static Thread 	threadSaver;
    // Thread per la terminazione del server
    private static Thread 	threadEnder;
    
    // ThreadPool dinamico: maggiore sarà il numero di task, maggiore saranno i thread attivi e viceversa
 	public static ExecutorService pool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),new ThreadPoolExecutor.AbortPolicy());
    // Istruzione deprecata: socketMulticast.joinGroup(group);
	@SuppressWarnings("deprecation")
	//	---------------------------------------------- start MAIN ----------------------------------------------	//
	public static void main(String[] args) {
       
		// Vengono letti tutti i parametri necessari al server per funzionare dall'apposito file di configurazione
		loadServerConfiguration();
		
		// Viene ripristinato lo stato del server
		loadServerStatus();
		
		// Viene costruito il vocabolario di parole che costituira il gioco WORDLE
		loadVocabolary(vocabolarioWordle);
		
		// Genera la nuova parola da indovinare
		generateNewSecretWord(Vocabolary,SecretsWords);	
		
        try {
            // Vengono creati e inizializzati gli oggetti remoti
            RegistrationRmi registrationServer = new RegistrationRmi(DataBase,StatsUsers);
            CallbackRmi callbackServer = new CallbackRmi();
            // Vengono esportati dinamicamente gli oggetti, ottenendo lo 'stub' corrispondente.
            RegistrationRmiInterface registrationServerStub = (RegistrationRmiInterface) UnicastRemoteObject.exportObject(registrationServer, 0);
            CallbackRmiInterface callbackServerStub = (CallbackRmiInterface) UnicastRemoteObject.exportObject(callbackServer, 0);
            // Creazione di un registry sulla porta specificata.
            LocateRegistry.createRegistry(RMIport);
            // Viene preso il registro appena creato
            Registry r = LocateRegistry.getRegistry(RMIport);
            // Pubblicazione degli stub nel registry.
            r.rebind(serviceRegistrationName, registrationServerStub);
            r.rebind(serviceCallbackName, callbackServerStub);
                     	
            System.out.println("Wordle Server registry is running...");
            // Il thread in attesa di invocazione di metodi remoti rimane attivo...
        
            // Server si unisce al gruppo di multicast con indirizzo 239.250.250.255
            try (MulticastSocket socketMulticast = new MulticastSocket(multicastPort)) {
	            // Ottengo l'indirizzo del gruppo e ne controllo la validità.
	            InetAddress group = InetAddress.getByName(multicastAddress);
	            if (!group.isMulticastAddress()) {
	                throw new IllegalArgumentException("Indirizzo multicast non valido: " + group.getHostAddress());
	            }
	            socketMulticast.joinGroup(group);
			
				// Creazione socket per la ricezione dei messaggi UDP da parte dei client su porta SPECIFICA
				try(DatagramSocket ds = new DatagramSocket(UDPport);){
					
					/* 	Viene creato e avviato il thread 't_newWord'	che estrae una nuovo parola da indovinare
					 * 	Viene creato e avviato il thread 't_share' 		che memorizza i messaggi UDP inviati dai client
					 * 	Viene creato e avviato il thread 't_save' 		che salva le strutture dati su dei file .json appositi
					 *  Viene creato e avviato il thread 't_end'		che serve per la terminazione del server	*/
					threadGenNewWord 	= new Thread(new GenerateNewWord());
					threadListener 		= new Thread(new ListenUDPpacket(ds,socketMulticast,group));
					threadSaver 		= new Thread(new SaveServer());
					threadEnder 		= new Thread(new EndServer(ds));
					
					threadGenNewWord.start();
					threadListener.start();
					threadSaver.start();
					threadEnder.start();
					
			        try(ServerSocket listener = new ServerSocket(TCPport)) {
			        	System.out.println("Wordle Server is running...");
			        	while(!end) {
			        		Socket sk = listener.accept();
			        		pool.execute(new Worker(sk,callbackServer));
			        	}
			        }	
			        catch(IOException e){
			        	e.printStackTrace();
			        }
			        catch(NullPointerException e) {
			        	System.out.println("Argomento passato al threadPool ha valore nullo");
			        }
			        catch(IllegalArgumentException e) {
			        	System.out.println("Errore: non è stata inserita una porta che sia compresa nel range 0 - 65535 estremi inclusi");
			        }
			        catch(RejectedExecutionException e) {
			        	//System.out.println("Rifiutato nuovo client");
			        	System.err.println(e.getMessage());
			        	e.printStackTrace();
			        }
				}
				catch(IOException e) {
					e.printStackTrace();
				}
				catch(IllegalArgumentException e) {
					System.out.println("Errore: non è stata inserita una porta che sia compresa nel range 0 - 65535 estremi inclusi");
				}
			}
			catch(UnknownHostException e) {
				System.out.println("Indirizzo IP dell'host non trovato");
			}
			catch(IOException e) {
				e.printStackTrace();
			}
			catch(IllegalArgumentException e) {
				System.out.println("Errore: non è stata inserita una porta che sia compresa nel range 0 - 65535 estremi inclusi");
			}
			catch(SecurityException e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
        }
        catch (RemoteException e) {
            System.err.println("Esportazione oggetto remoto fallita");
        }
        catch (NullPointerException e) {
            System.err.println("Sono stati passati valori 'nulli' al registro");
        }
        
      }
	
	//	---------------------------------------------- end MAIN ----------------------------------------------	//

	// Funzione che ricostruisce lo stato del server leggendo da vari file
	private static void loadServerStatus() {
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		try {
			// Ricostruisco il database
			Type type = new TypeToken<ConcurrentHashMap<String,String>>(){}.getType();
			DataBase = gson.fromJson(Utility.readFile(storageFileDataBase),type);
			/*	Se 'DataBase == null' significa che è la primissima volta che viene lanciato il server
				e quindi la struttura dati risulta essere vuota in quanto non ci sono ancora utenti registrati	*/
			if(DataBase == null) {
				// Viene inizializzato il DataBase
				DataBase = new ConcurrentHashMap<String,String>();
			}
		}
		catch(JsonSyntaxException e) {
			e.printStackTrace();
		}
		try {
			// Ricostruisco il file contenente le statistiche degli utenti
			Type type = new TypeToken<ArrayList<User>>(){}.getType();
			StatsUsers = gson.fromJson(Utility.readFile(storageFileStatsUsers),type);
			/*	Se 'StatsUsers == null' significa che è la primissima volta che viene lanciato il server
				e quindi la struttura dati risulta essere vuota in quanto non ci sono ancora utenti registrati	*/
			if(StatsUsers == null) {
				// Viene inizializzato StatsUsers
				StatsUsers = new ArrayList<User>();
			}
		}
		catch(JsonSyntaxException e) {
			e.printStackTrace();
		}
		try {
			// Ricostruisco la classifica degli utenti
			Type type = new TypeToken<LinkedList<UserRank>>(){}.getType();
			Rank = gson.fromJson(Utility.readFile(storageFileRank),type);
			/*	Se 'Rank == null' significa che è la primissima volta che viene lanciato il server
				e quindi la struttura dati risulta essere vuota in quanto non ci sono ancora utenti registrati	*/
			if(Rank == null) {
				// Viene inizializzato Rank
				Rank = new LinkedList<UserRank>();
			}
		}
		catch(JsonSyntaxException e) {
			e.printStackTrace();
		}
		
		if(!DataBase.isEmpty() && !StatsUsers.isEmpty() && !Rank.isEmpty()) {
			System.out.println("Stato server ricorstruito");
		}
		
	}	

	/*	Funzione che legge automaticamente tutti i parametri di configurazione del server
	 *	da un file apposito */
	private static void loadServerConfiguration() {
		Properties properties = new Properties();
		// File da cui leggere i parametri di configurazione
		String filename = "./ServerConfiguration.properties";
				
		try(FileInputStream fis = new FileInputStream(filename)){
			properties.load(fis);
			RMIport = Integer.parseInt(properties.getProperty("RMIport"));
			TCPport = Integer.parseInt(properties.getProperty("TCPport"));
			UDPport = Integer.parseInt(properties.getProperty("UDPport"));
			newWordTime = Integer.parseInt(properties.getProperty("NewWordTime"));
			serviceRegistrationName = properties.getProperty("ServiceRegistrationName");
			serviceCallbackName = properties.getProperty("ServiceCallbackName");
			host = properties.getProperty("Host");
			multicastPort = Integer.parseInt(properties.getProperty("MulticastPort"));
			multicastAddress = properties.getProperty("MulticastAddress");
			vocabolarioWordle = properties.getProperty("VocabolarioWordle");
			storageFileDataBase = properties.getProperty("StorageFileDataBase");
			storageFileStatsUsers = properties.getProperty("StorageFileStatsUsers");
			storageFileRank = properties.getProperty("StorageFileRank");
		}
		catch (FileNotFoundException e) {
			System.err.println("File di configurazione del client non trovato");
		}
		catch (NumberFormatException e) {
			System.err.println("Errore nella lettura dei parametri di configurazione del client");
		}	
		catch (NullPointerException e) {
			System.err.println("Stream nullo");
		}
		catch(IllegalArgumentException e){
			System.err.println("Argomento illegale per lo stream di input");
		}
		catch (IOException e) {
			System.err.println("Errore leggendo dallo stream di input");
		} 
	}
	
	// Funzione che carica il vocabolario di Wordle
	private static void loadVocabolary(String vocabolarioWordle) {
		// Creo un nuovo vocabolario avente dimensione 30824 (numero di parole presenti nel file words.txt)
		Vocabolary = new ArrayList<String>(vocabolaryDim);
		File fileIn = new File(vocabolarioWordle);
		// Leggo tutto il file riga per riga
		try(BufferedReader buffR = new BufferedReader(new FileReader(fileIn));) {	
			String line;
            while ((line = buffR.readLine()) != null) {
            	// Rimuovo eventuali spazi
            	line.trim();
            	// Aggiungo la parola al dizionario
            	Vocabolary.add(line);
            }
        /*	Finito di creare il vocabolario di Wordle.NOTA BENE:alla posizione 'x' del vocabolario
         * 	nonchè dell'ArrayList ci sarà la parola presente alla riga 'x + 1' del file words.txt 
         *	visto che l'indice dell'array parte da 0 mentre la riga di testo del file parte da 1	*/
		}
		catch(FileNotFoundException e) {
			System.err.println("Vocabolario words.txt non trovato");
		} 
		catch (IOException e) {
			System.err.println("Errore leggendo dal vocabolario words.txt");
		} 
	}
		
	// Classe per la generazione di una nuova parola da indovinare
	private static class GenerateNewWord implements Runnable {
		
		public void run() {
			while (true) {
				try {
					/*	Ogni 'newWordTime' millisecondi viene chiamata la funzione 'generateNewSecretWord' per 
					 * 	l'estrazione di una nuova parola	*/
					Thread.sleep(newWordTime);
				}
				catch(InterruptedException e) {
				}
				if(end)
					return;
				generateNewSecretWord(Vocabolary,SecretsWords);
			}
		}
	}
	
	// Funzione che genera una nuova parola da indovinare da una lista di parole predefinite
	private static void generateNewSecretWord(ArrayList<String> Vocabolary,ArrayList<String> SecretsWords) {
		while(true){
			try {
				// Generazione casuale dell'indeice della parola da estrarre
				int indexOfSW = ThreadLocalRandom.current().nextInt(0,30824);
				// Estrazione della parola segreta
				String secretWord = Vocabolary.get(indexOfSW);
				// Si controlla se la parola è già uscita precedentemente 
				if(!SecretsWords.contains(secretWord)) {
					SecretsWords.add(secretWord);
					SECRETWORDWORDLE = secretWord;
					System.out.printf("Parola segreta estratta: %s\n", SECRETWORDWORDLE);
					break;
				}
				// Si estrae un'altra parola che non è precedentemente uscita
				else
					continue;
			}
			catch(IllegalArgumentException e) {
				e.printStackTrace();
			}
			catch(IndexOutOfBoundsException e) {
				e.printStackTrace();
			}
		}
	}
	
	// Classe per la ricezione dei pacchetti inviati sul gruppo di multicast
	private static class ListenUDPpacket implements Runnable {
			DatagramSocket ds;
			MulticastSocket ms;
			InetAddress group;
			
			public ListenUDPpacket(DatagramSocket ds,MulticastSocket ms,InetAddress group) {
				this.ds = ds;
				this.ms = ms;
				this.group = group;
			}
			public void run() {
				try {
					while(true) {
						byte[] buffer = new byte[8194];
						if(end)
							return;
						DatagramPacket dpReceive = new DatagramPacket(buffer,buffer.length);
						/*	Vengono ricevuti i risultati delle partite inviati da parte dei client i quali hanno intenzione di 
						 * condividerli sul gruppo di multicast sottoforma di pacchetto UDP. Potrebbe anche essere che il pacchetto 
						 * contiene la stringa 'termina' la quale informa il thread Listener di terminare	*/
						ds.receive(dpReceive);
						String received = new String(dpReceive.getData());
						if(received.equals("termina"))
							return;
						// Altrimenti il pacchetto contiene il risultato della partita di un client che viene aggiunto nella struttura
						AllResult.add(received);
						// Viene inviato il risultato al gruppo di multicast
						DatagramPacket dpm = new DatagramPacket(buffer,buffer.length,group,multicastPort);
						ms.send(dpm);
					}
				}
				catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}
		}
	
	// Classe per il salvataggio delle strutture dati del server
	private static class SaveServer implements Runnable {
		
		public void run() {
			while(true) {
				try {
					// Ogni 'saveTime' millsecondi viene chiamata la funzione 'save' per il salvataggio
					Thread.sleep(saveTime);
				}
				catch(InterruptedException e) {
				}
				if(end)
					return;
				save();
			}
		}
		
	}
	
	// Funzione per il salvataggio delle strutture dati sottoforma di file .json
	public static void save() {
			
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		// Memorizzo le strutture dati su file esterni
		DataBaseFileLock.lock();
		Utility.writeFile(storageFileDataBase, gson.toJson(DataBase));
		DataBaseFileLock.unlock();
		
		StatsUsersFileLock.lock();
		Utility.writeFile(storageFileStatsUsers, gson.toJson(StatsUsers));
		StatsUsersFileLock.unlock();
		
		RankFileLock.lock();
		Utility.writeFile(storageFileRank, gson.toJson(Rank));
		RankFileLock.unlock();
		
	}
		
	// Classe per la terminazione del server
	private static class EndServer implements Runnable {
		DatagramSocket ds;
		
		// Socket per la ricezione dei messaggi inviati sul gruppo di multicast
		public EndServer (DatagramSocket ds) {
			this.ds = ds;	
		}
		
		public void run() {
			while(true) {
				try {
					Scanner keyboard = new Scanner(System.in);
					System.out.println("Per la terminazione del server digitare <end>");
					String command = keyboard.nextLine();
					command.trim();
					if(command.equalsIgnoreCase("end")) {
						end = true;
					}
					else 
						continue;
					//Inizia la terminazione del thread pool
					System.out.println("Terminazione del server in corso...");
					keyboard.close();
					/*	Terminazione graduale: nessun nuovo task viene accettato,quelli in esecuzione vengono eseguiti fintanto
					 *	che non terminano la loro esecuzione. Gli viene dato un tempo limite di 60 secondi per terminare il
					 *	il loro operato	*/ 
					pool.shutdown();
					pool.awaitTermination(terminationDelay,TimeUnit.MILLISECONDS);
					// Viene terminato il thread che genere la parole segrete
					threadGenNewWord.interrupt();
					threadGenNewWord.join();
					// Viene inviato un pacchetto per far terminare il thread che sta in ascolto sul gruppo di multicast
					try {
						InetAddress UDPserverAddress = InetAddress.getByName(host);
						String messageEnd = "termina";
						byte[] packetEnd = messageEnd.getBytes();
						DatagramPacket dp = new DatagramPacket(packetEnd,packetEnd.length,UDPserverAddress,UDPport);
						ds.send(dp);
					}
					catch(IOException e) {
						e.printStackTrace();
					}
					threadListener.interrupt();
					threadListener.join();
					// Viene terminato il thread che salva lo stato del server periodicamente
					threadSaver.interrupt();
					threadSaver.join();
				}
				catch(NoSuchElementException e) {
					e.printStackTrace();
				}
				catch(IllegalStateException e) {
					e.printStackTrace();
				}
				catch(InterruptedException e) {
					e.printStackTrace();
					System.out.println("Qualcosa è andato storto nella chiusura");
				}
				// Viene chiamata la funzione save() un ultima volta prima che il server termini
				save();
				System.out.println("Server terminato con successo");
				System.exit(0);
			}
		}
	}
	
	// Thread del thread pool che gestisce interamente una connessione con un client fino al logout di quest'ultimo
	private static class Worker implements Runnable{
		
		private Socket socket;
		private CallbackRmi callbackServer;
				
		public Worker(Socket socket,CallbackRmi callbackServer) {
			this.socket = socket;
			this.callbackServer = callbackServer;
		}
		
		public void run() {
			// Nuovo client che ha effettuato il login che viene gestito dal server
			System.out.printf("Worker %s is running...",Thread.currentThread().getName());
			System.out.println("Connected with a new client");
			User 		utente = null;
			UserRank	utenteRank = null;
			boolean loginStatus = false;
			try {
				// Per leggere dal server
				InputStream ins = socket.getInputStream();
				ObjectInputStream in = new ObjectInputStream(ins);
					
				// Per scrivere al server
				OutputStream outs = socket.getOutputStream();
				ObjectOutputStream out = new ObjectOutputStream(outs);
				
				// {1} Viene letta la stringa 'login' inviata dal client
				String clientCommand = in.readUTF();
					
				if(clientCommand.equals("login")){
					// {2} Viene letto lo username dell'utente inviatogli dal client
					String clientUsername = in.readUTF();
					// {3} Viene letto la password dell'utente inviatogli dal client
					String clientPassword = in.readUTF();
					
					// Viene controllato che lo username appartenga ad un utente registrato
					if(DataBase.containsKey(clientUsername)) {
						String passwd = DataBase.get(clientUsername);
						// Viene controllato se la password associata allo username è corretta
						if(passwd.equals(clientPassword)) {
							loginStatus = true; 
							System.out.println("Login client effettuato con successo!");
							// [1] Viene mandato al client un messaggio di login effettuato con successo
							out.writeUTF("login success");
							out.flush();
						}
						else {
							// [1] Viene mandato al client un messaggio di login fallito causa password errata
							out.writeUTF("login fail (password)");
							out.flush();
						}	
					}
					else {
						// [1] Viene mandato al client un messaggio di login fallito causa username errato
						out.writeUTF("login fail (username)");
						out.flush();
					}
					// Fintanto che il client non esegue il logout
					while(loginStatus) {
						// Vengono recuperate le statistiche dell'utente dall'oggetto 'User'
						Iterator<User> it1 = StatsUsers.iterator();
						while(it1.hasNext()) {
							User us = it1.next();
							// Username univoco
							if(us.getUsername().equals(clientUsername)) {
								// Utente recuperato
								utente = us;
								break;
							}
						}

						// {4} Il worker legge cosa il client ha intenzione di fare ( play || logout || sendMeStatistics || showMeRanking )
						clientCommand = in.readUTF();
						// L'utente ha intenzione di iniziare una nuova partita
						if(clientCommand.equals("play")) {
							// [2] Viene mandato al client il suo oggetto 'User'
							out.writeObject(utente);
							out.flush();
							// Viene controllato se l'utente può giocare o meno a seconda della sua ultima parola giocata
							if(utente.getLastWordWordle().equals(SECRETWORDWORDLE)) {
								// [3] Il worker risponde al client che NON PUO' giocare
								out.writeUTF("cant play");
								out.flush();
								continue;
							}
							else {
								// [3] Il worker risponde al client che PUO' giocare
								out.writeUTF("can play");
								out.flush();
							}
									
							// {5} Il worker si informa se è la prima partita dell'utente o meno
							String newGames = in.readUTF();
							if(newGames.equals("first time")) {
								//Viene costruito il nuovo oggetto 'UserRank' associato all'utente
								UserRank usrank = new UserRank(utente.getUsername(),0.0);
								utenteRank = usrank;
							}
							if(newGames.equals("not first time")) {
								// Viene recuperato l'oggetto 'UserRank' associato all'utente
								Iterator<UserRank> it = Rank.iterator();
								while(it.hasNext()) {
									UserRank usrank = it.next();
									// Username univoco
									if(usrank.getUsername().equals(clientUsername)) {
										utenteRank = usrank;
										//[4] Il worker manda l'oggetto 'UserRank' al client
										out.writeObject(utenteRank);
										out.flush();
										break;
									}
								}
							}
							/* Il client può giocare a WORDLE: vengono inizializzati i tentativi a <0> e la variabile
							 * che ci dice se l'utente ha indovinato la secretWord a <false>	*/
							boolean guess = false;
							int attempt = 0;
							// Copia locale della parola da indovinare
							String secretWordWordleLocal = SECRETWORDWORDLE;	
										
							// Finchè l'utente non esaurisce tutti e 12 i tentativi o indovina la secretWord
							while(!guess && attempt < 12) {
								int i = 0;
								// Stringa che verrà visualizzata dal client contenente indizi inerenti alla parola da indovinare
								String outputWord = "----------";
								// Viene costruito l'insieme contenente le lettere della secret word
								StringBuilder SW = new StringBuilder(secretWordWordleLocal);
								Set<Character> set = new HashSet<Character>();
								while(i < 10) {
									Character character = Character.valueOf(SW.charAt(i));
									set.add(character);
									i++;
								}
								// {6} Lettura della guessWord inviata dal client
								String guessWord = in.readUTF();
								// Se la parola non è presente all'interno del vocabolario il tentativo non viene contato come valido
								if(!Vocabolary.contains(guessWord)) {
									// [5] Tentativo non valido
									out.writeUTF("Nullo");
									out.flush();
									continue;
								}
								// Se la parola è presente all'interno del vocabolario il tentativo viene contato come valido
								else {
									// [5] Tentativo valido
									out.writeUTF("Valido");
									out.flush();
									attempt++;
								}
								StringBuilder GW = new StringBuilder(guessWord);
								StringBuilder OW = new StringBuilder(outputWord);
											
								// Viene costruita la stringa che darà gli indizi al client sulla parola segreta da indovinare
								int j = 0;
								while(j < 10) {
									if(GW.charAt(j) == SW.charAt(j))
										OW.setCharAt(j,'+');
									else if(set.contains(GW.charAt(j)))
										OW.setCharAt(j,'?');
									else
										OW.setCharAt(j,'x');
									j++;	
								}
								// Risposte per il client
								// [5.1] guessWord
								out.writeUTF(GW.toString());
								out.flush();
								// [5.2] outputWord
								out.writeUTF(OW.toString());
								out.flush();
										
								// Viene controllato se l'utente ha indovinato la secret word
								if(GW.toString().equals(secretWordWordleLocal)) {
									//[6] L'utente ha indovinato
									out.writeUTF("Indovinato");
									out.flush();
									guess = true;
								}
								else {
									//[6] L'utente non ha indovinato
									out.writeUTF("Non indovinato");
									out.flush();
								}
							}
											
							if(!guess) {
								// [7] Viene comunicato all'utente che ha perso la partita
								out.writeUTF("Lose");
								out.flush();
							}
							else {
								// [7] Viene comunicato all'utente che ha vinto la partita
								out.writeUTF("Win");
								out.flush();
							}
							// [8] Viene rivelata a fine partita quale era la parola da indovinare 
							out.writeUTF(secretWordWordleLocal);
							out.flush();
										
							/*	Il server deve fornire al client post game la traduzione della parola segreta ottenuta accedendo al 
							 * 	servizio presente alla seguente URL: "https://mymemory.translated.net/doc/spec.php" tramite una 
							 * 	chiamata GET	*/
							String secretWordTranslate = HttpRequestTranslate(secretWordWordleLocal);
							// [9] Viene inviata al client la traduzione della parola segreta
							out.writeUTF(secretWordTranslate);
							out.flush();
									
							// {7} Il worker legge l'oggetto 'User' aggiornato dopo la partita dell'utente
							utente = (User) in.readObject();
							// Viene aggiornata la StatsUsers
							StatsUsersLock.lock();
							updateStatsUsers(utente,clientUsername);
							StatsUsersLock.unlock();
							
							// {8} Il worker legge l'oggetto 'UserRank' aggiornato dopo la partita dell'utente
							utenteRank = (UserRank) in.readObject();
							// Viene aggiornata la classifica
							RankLock.lock();
							updateRank(utenteRank);
							RankLock.unlock();
						}
						// L'utente vuole disconnettersi
						else if(clientCommand.equals("logout")) {
							socket.close();
							loginStatus = false;
						}
						// L'utente vuole visualizzare la classifica
						else if(clientCommand.equals("sendMeStatistics")) {
							// [2] Manda al client il proprio oggetto 'User'
							out.writeObject(utente);
							out.flush();
						}
						// L'utente vuole visualizzare la classifica
						else if(clientCommand.equals("showMeRanking")) {
							// [2] Manda al client la classifica
							out.writeUTF(Rank.toString());
							out.flush();
						}
						// Errore comando non riconosciuto
						else {
							System.out.println("comando non riconosciuto");
						}
					}
				}
				else 
					System.out.println("not login command ");
			}
			catch(IOException e) {
				e.printStackTrace();
			}
			catch(ClassNotFoundException e) {
				System.err.println("Non è stato possibile trovare la classe dell'oggetto serializzato");
			}
			catch(NullPointerException e) {
				System.err.println("Errore: passato valore nullo");
			}
			System.out.printf("Worker %s is ending...",Thread.currentThread().getName());
			System.out.println("Disconnetted from the client");
		}
		
		// Funzione che aggiorna le statistiche degli utenti
		public void updateStatsUsers(User utente,String username) {
			try {
				int index = -1;
				Iterator<User> it = StatsUsers.iterator();
				while(it.hasNext()) {
					User us = it.next();
					// Username univoco
					if(us.getUsername().equals(username)) {
						index = StatsUsers.indexOf(us);
						break;
					}
				}
				StatsUsers.set(index, utente);
				System.out.printf("Stats utente <%s> aggiornate!\n",utente.getUsername());
			}
			catch(IndexOutOfBoundsException e) {
				System.err.println("Indice non valido");
			}
		}
		
		// Funzione che aggiorna la classifica degli utenti
		@SuppressWarnings("unchecked")
		public void updateRank(UserRank utente) {
			
			// Si resetta il valore
			top3Change = false;
			
			LinkedList<UserRank> RankCopy = new LinkedList<UserRank>();
			// Si copia la classifica
			RankCopy = (LinkedList<UserRank>) Rank.clone();
			
			/*	Viene prima aggiornata la classifica 
			 *	Se l'utente non è presente in classifica significa che è la prima partita che ha disputato,quindi viene aggiunto	*/
			if(!Rank.contains(utente)) {
				Rank.add(utente);
				Collections.sort(Rank);
			}
			// Utente già presente in classifica. La sua posizione viene aggiornata dopo la nuova partita giocata
			else {
				int index = Rank.indexOf(utente);
				UserRank ur = Rank.get(index);
				ur.setWordleScore(utente.getWordleScore());
				Collections.sort(Rank);
			}
			
			// Viene verificato se è cambiata la top 3
			Iterator<UserRank> it = Rank.iterator();
			Iterator<UserRank> itC = RankCopy.iterator();
			// Siamo nel caso un cui viene aggiunto il primissimo utente
			if(RankCopy.size() == 0 && Rank.size() == 1)
				top3Change = true;
			// Siamo nel caso un cui viene aggiunto il secondo utente
			else if(RankCopy.size() == 1 && Rank.size() == 2)
				top3Change = true;
			// Siamo nel caso un cui viene aggiunto il terzo utente
			else if(RankCopy.size() == 2 && Rank.size() == 3)
				top3Change = true;
			
			int i = 0;
			ArrayList<UserRank> TopThree = new ArrayList<UserRank>(3);
			while(it.hasNext() && itC.hasNext() && i < 3) {
				UserRank usR = it.next();
				UserRank usRC = itC.next();
				TopThree.add(usR);
				if(!usR.equals(usRC))
					top3Change = true;
				i++;
			}
			
			// Se cambia la top 3 il worker deve aggiornare tutti i client che si sono registrati al sistema di notifica 
			if(top3Change) {
				System.out.println("Top 3 cambiata!");
				try {
					callbackServer.update(TopThree);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}	
		}
		
		//Fuznione che tramite una richiesta http al server MyMemory esegue la traduzione della parola da indovinare
		private static String HttpRequestTranslate(String secretWord) {
			// HTTP request per il server MyMemory
			String urlMyMemory;
			urlMyMemory = "https://api.mymemory.translated.net/get?q="+ secretWord + "&langpair=en|it";
			try{
				URL url = new URL(urlMyMemory);
				try (InputStream uins = url.openStream();
						InputStreamReader uinsr= new InputStreamReader(uins);
						BufferedReader in = new BufferedReader(uinsr);
						JsonReader reader = new JsonReader(uinsr);){
					
					/*	url.openStream() == url.openConnection().getInputStream();
					 * 	Con questa chiamata si scarica il contenuto della risorsa identificata dalla URL url	*/ 
					
					String translation = null;
				
					/*	Esempio interazione server MyMemory:
					 * 
					 * 	Richiesta: (call example)
					 *		"https://api.mymemory.translated.net/get?q=Hello World!&langpair=en|it"
					 *
					 *	Risposta del server MyMemory
					 *	{"responseData":{"translatedText":"Ciao Mondo!","match":1},"quotaFinished":false,"mtLangSupported":null,"responseDetails"
					 *	:"","responseStatus":200,"responderId":null,"exception_code":null,"matches":[{"id":"720659022","segment":"Hello World!",
					 *	"translation":"Ciao Mondo!","source":"en-GB","target":"it-IT","quality":74,"reference":null,"usage-count":2,"subject":
					 *	"","created-by":"MateCat","last-updated-by":"MateCat","create-date":"2023-05-14 15:15:02","last-update-date":
					 *	"2023-05-14 15:15:02","match":1}]}	*/
					
					/*	Viene eseguito il parsing dell'oggetto json
					 *  Viene letto come prima cosa l'intero oggetto	*/
					reader.beginObject();
					while (reader.hasNext()) {
						String name = reader.nextName();
						// Successivamente viene letto l'oggetto 'responseData'
						if (name.equals("responseData")) {
							reader.beginObject();
							while (reader.hasNext()) {
								name = reader.nextName();
								if (name.equals("translatedText"))
									// Viene letta la traduzione della parola richiesta
									translation = reader.nextString();
								else
									reader.skipValue();
							}
							reader.endObject();
						} 
						else
							reader.skipValue();
					}
					reader.endObject();

					if (translation == null) {
						System.out.println("C'e' stato un errore nel leggere la risposta dal server MyMemory: traduzione non trovata");
						return "";
					}
					
					return translation;
				}
				catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}
			catch (MalformedURLException e) {
				System.err.println("Malformed URL");
				return null;
			}
		}
		
	}

}

