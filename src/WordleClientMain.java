import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.SocketException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.PatternSyntaxException;

public class WordleClientMain {

	// Strutture dati
	// Struttura dati che permette all'utente di memorizzare i risultati della propria ultima partita
	private static ArrayList<String>	OwnResult 		= new ArrayList<String>(12);
	// Struttura dati che permette all'utente di memorizzare tutti i risultati degli utenti che hanno deciso di condividerli
	private static ArrayList<String> 	AllResult		= new ArrayList<String>();
	
	private static boolean				registerDone	= false;
	private static boolean 				gameDone 		= false;
	private static boolean				gameShare		= false;
	private static int 					index;
	
	private static String 				serviceRegistrationName;	//	"WORDLE-REGISTRATION"
	private static String 				serviceCallbackName;		//	"WORDLE-CALLBACK"
	private static int 					RMIport;					//	9999 	
	private static String 				host; 						//	"localhost"
	private static int 					TCPport; 					//	8888
	private static int					UDPport;					//	7777
	private static int 					multicastPort;				//	9898
	private static String 				multicastAddress;			//	239.250.250.255
	
	// Input da tastiera
	private static Scanner				keyboard 		= new Scanner(System.in);
	
	private static User					utente;
	private static UserRank				utenteRank;
	// Stringa di 'aiuto' che viene stampata a schermo contenente i consigli (+,?,x)
	private static String 				OutputWord;
	
	// Thread per la ricezione di messaggi sul gruppo di multicast
	private static Thread 				threadShare;
	
	//	---------------------------------------------- start MAIN ----------------------------------------------	//
	public static void main(String[] args) {
		
		// Leggo tutti i parametri di configurazione dal file apposito
		loadClientConfiguration();
		
		// Stampa su CLI la start interface
		printStartInterface();
		
        while(true) {
        	
        	System.out.print("Inserire comando >> ");
        	try {
        		String line = keyboard.nextLine();
        		// Si rimuovono eventuali spazi aggiuntivi
        		String command = line.trim();
        		// Utente desidera registrarsi (REGISTER)
        		if (command.equalsIgnoreCase("register")) {
        			if(!registerDone) {
        				while(true) {
        					System.out.println("[ In fase di registrazione... (digitare <back> se si desidera tornare indietro) ]");
        					System.out.println("[ Inserire <username> <password> ]");
        					System.out.print(">> ");
        					line = keyboard.nextLine();
        					String[] parts = line.split(" ");
        					// Se è stato inserito un solo comando si controlla se è il comando <back> 
        					if(parts.length == 1) {
        						String innerCommand = line.trim();
        						// Si controlla se l'utente desidera tornare indietro
        						if (innerCommand.equalsIgnoreCase("back"))
        							break;
        						// Errore nell'inserimento
        						else {
        							System.out.println("[ Errore! input non valido: inserire <username> <password> ]");
        							continue;
        						}
        					}
        					// Si controlla se l'utente ha inserito un numero sbagliato di argomenti
        					else if(parts.length > 2) {
        						System.out.println("[ Errore! input non valido: inserire <username> <password> ]");
        						continue;
        					}
        					// L'utente ha inserito un'ipotetico username e password per registrarsi
        					String username = parts[0];
        					String password = parts[1];
        					// Si registra l'utente con password se non risulta già presente nel database
        					int retVal = register(username,password);
        					/*	Se retVal == -1 significa che c'è stato un errore nell'inserimento dati in fase di registrazione
    							e l'utente può riprovare a registrarsi	*/
        					if(retVal == -1)
        						continue;
        					// Registrazione riuscita
        					else {
        						registerDone = true;
        						break;
        					}
        				}
        			}
        			else{
        				System.out.println("[ Registrazione gia effettuata! ]");
        			}
        		}
        		// Utente desidera effettuare il login (LOGIN)
        		else if (command.equalsIgnoreCase("login")) {
        			while(true) {
        				System.out.println("[ In fase di login... (digitare <back> se si desidera tornare indietro) ]");
        				System.out.println("[ Inserire <username> <password> ]");
        				System.out.print(">> ");
        				line = keyboard.nextLine();
        				String[] parts = line.split(" ");
        				// Se è stato inserito un solo comando si controlla se è il comando <back> 
        				if(parts.length == 1) {
        					String innerCommand = line.trim();
        					// Si controlla se l'utente desidera tornare indietro
        					if (innerCommand.equalsIgnoreCase("back"))
        						break;
        					// Errore nell'inserimento
        					else {
        						System.out.println("[ Errore! input non valido: inserire <username> <password> ]");
        						continue;
        					}
        				}
        				// Si controlla se l'utente ha inserito un numero sbagliato di argomenti
        				else if(parts.length > 2) {
        					System.out.println("[ Errore! input non valido: inserire <username> <password> ]");
        					continue;
        				}
        				// L'utente ha inserito un'ipotetico username e password per effettuare il login
        				String username = parts[0];
        				String password = parts[1];
        				int retVal = login(username,password);
        				/*	Se retVal == -1 significa che c'è stato un errore nell'inserimento dati in fase di login
        					e l'utente può riprovare con l'autenticazione	*/
        				if(retVal == -1)
        					continue;
        				// Autenticazione riuscita,utente si è loggato; successivamente post logout torna alla schermata principale
        				else {
        					break;
        				}
        			}
        		}
        		// Utente desidera visualizzare la lista di comandi disponibile
        		else if (command.equalsIgnoreCase("help")){
        			printHelp();
        		}
        		// Utente desidera uscire
        		else if (command.equalsIgnoreCase("exit")) {
        			break;
        		}
        		// Utente ha inserito un comando non valido 
        		else 
        			System.err.println("[ Errore: comando non valido ]");
        	}
        	catch(NoSuchElementException e) {
        		System.err.println("No input avaible for nextLine()");
        		System.exit(-1);
        	}
        	catch(IllegalStateException e) {
        		System.err.println("Scanner per l'input chiuso");
        	}
        	catch(PatternSyntaxException e) {
        		System.err.println("Sintassi invalida");
        	}	
        }
        keyboard.close();
        System.exit(1);
   }
	//	---------------------------------------------- end MAIN ----------------------------------------------	//
	
	/*	Funzione che legge automaticamente tutti i parametri di configurazione del client
	 	da un file apposito	*/
	private static void loadClientConfiguration() {
		Properties properties = new Properties();
		// File da cui leggere i parametri di configurazione
		String filename = "./ClientConfiguration.properties";
			
		try(FileInputStream fis = new FileInputStream(filename)){
			properties.load(fis);
			RMIport = Integer.parseInt(properties.getProperty("RMIport"));
			TCPport = Integer.parseInt(properties.getProperty("TCPport"));
			UDPport = Integer.parseInt(properties.getProperty("UDPport"));
			host = properties.getProperty("Host");
			serviceRegistrationName = properties.getProperty("ServiceRegistrationName");
			serviceCallbackName = properties.getProperty("ServiceCallbackName");
			multicastPort = Integer.parseInt(properties.getProperty("MulticastPort"));
			multicastAddress = properties.getProperty("MulticastAddress");
				
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
	
	//	-------------------------------------------- start REGISTER --------------------------------------------	//
	
	/*	Funzione per la registrazione di un nuovo utente sul Server la quale
	 	ritorna 0 se la registrazione ha avuto successo -1 altrimenti	*/
	private static int register(String username,String password) {
		try {
			// Si Ottiene un riferimento al registry.
			Registry r = LocateRegistry.getRegistry(RMIport);
			// Si ottiene un riferimento all'oggetto remoto.
			RegistrationRmiInterface registrationServerStub = (RegistrationRmiInterface) r.lookup(serviceRegistrationName);
			registrationServerStub.addUser(username,password);
			System.out.println("[ Registrazione effettuata con successo: ]");
			System.out.println("[ Ora puoi effettuare il login con le seguenti credenziali: ]");
			System.out.printf("[ Username: <%s> Password: <%s> ]\n",username,password);
			// Registrazione avvenuta con successo
			return 0;

		}
		catch (NotBoundException e) {
			System.err.println("Nome servizio non legato");
	    }
	    catch (RemoteException e) {
	    	System.err.println("[ Registrazione fallita! Username gia' preso ]");
	    }
	    catch (NullPointerException e) {
	        System.err.println("Sono stati passati valori nulli al registro");
	    }
		// Registrazione fallita
		return -1;
	}
	//	-------------------------------------------- end REGISTER --------------------------------------------	//
  
	//	-------------------------------------------- start LOGIN --------------------------------------------	//
	
	/*	Funzione per effettuare il login la quale
		ritorna 0 se il login ha successo; -1 altrimenti	*/
	// Istruzione deprecata: socketMulticast.joinGroup(group);
	@SuppressWarnings({ "deprecation"})
	private static int login(String username,String password) {
		//Viene instaurata una connessione TCP col server
		try (Socket TCPsocket = new Socket(host,TCPport);){
			// Per scrivere al server	
			OutputStream outs = TCPsocket.getOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(outs);
			
			// Per leggere dal server
			InputStream ins	= TCPsocket.getInputStream();
			ObjectInputStream in = new ObjectInputStream(ins);
			
			// [1] Client vuol notificare al server che vuole effettuare il login
			out.writeUTF("login");
			out.flush();
			// [2] Client invia lo username
			out.writeUTF(username);
			out.flush();
			// [3] Client invia la password
			out.writeUTF(password);
			out.flush();
			
			// {1} Viene letta la risposta del server riguardo al login (se ha avuto successo o meno)
			String responseLogin = in.readUTF();
			// Login ha avuto successo
			if(responseLogin.equals("login success")) {
				// Client ha effettuato il login con successo
				printLoginSuccessInterface(username);
				try {	
					
					// Post login il client si registra al sistema di notifiche cambiamento della top 3 del server
					Registry registry = LocateRegistry.getRegistry(RMIport);
					CallbackRmiInterface callbackServerStub = (CallbackRmiInterface) registry.lookup(serviceCallbackName);	
					// Viene creato e inizializzato l'oggetto remoto
					NotifyEventRmiInterface callbackClient = new NotifyEventRmi();
					// Viene esportato dinamicamente l'oggetto, ottenendo lo 'stub' corrispondente.
					NotifyEventRmiInterface callbackClientStub = (NotifyEventRmiInterface) UnicastRemoteObject.exportObject(callbackClient,0);
					callbackServerStub.registerForCallback(callbackClientStub);
								
					MulticastSocket socketMulticast = new MulticastSocket(multicastPort);
					// Si ottiene l'indirizzo del gruppo e ne viene controllata la validità.
					InetAddress group = InetAddress.getByName(multicastAddress);
					if (!group.isMulticastAddress()) {
							TCPsocket.close();
							socketMulticast.close();
							throw new IllegalArgumentException("Indirizzo multicast non valido: " + group.getHostAddress());
					}
					// Client si unisce al gruppo di multicast.
					socketMulticast.joinGroup(group);
					System.out.println("Client unito al gruppo di multicast");
					
					/*	Viene inizializzato e avviato il thread che riceverà in maniera asincrona i pacchetti UDP
					 *	che saranno inviati sul gruppo di multicast	*/
					threadShare = new Thread(new Multicast(socketMulticast));
					threadShare.start();
					
					InetAddress UDPserverAddress = InetAddress.getByName(host);
					// Socket per l'invio di pacchetti sul gruppo di multicast.Porta 0 equivale a scegliere una porta effimera libera
					try(DatagramSocket UDPsocket = new DatagramSocket(0)) {
						// Interazione principale client-server
						while(true) {
							
							System.out.printf("[ Autenticato come <%s> ]\n",username);
							System.out.print("Inserire comando  >> ");
									
							String line = keyboard.nextLine();
							// Vengono rimossi eventuali spazi aggiunti per sbaglio
							String command = line.trim();
							// L'utente ha intenzione di iniziare una nuova partita
							if(command.equalsIgnoreCase("play")) {
								// [4] Il client notifica al server che vuole giocare
								out.writeUTF("play");
								out.flush();
								// {2} Il client recupera le statistiche generali dell'utente grazie al proprio oggetto 'User'
								utente = (User) in.readObject();
								// Il client vuole iniziare il gioco.Se non ha ancora giocato per indovinare la parola può iniziare
								if(playWORDLE(TCPsocket,in,out)) {
									// Viene controllato se è la prima volta che l'utente gioca in ASSOLUTO
									if(utente.getGamesPlayed() == 0) {
										System.out.println("[ Prima volta che giochi viene creata scheda rank user! ]");
										UserRank usrank = new UserRank(utente.getUsername(),0.0);
										utenteRank = usrank;
										// [5] Viene indicato al server che è la prima partita in assoluto dell'utente
										out.writeUTF("first time");
										out.flush();
									}
									// Non è la prima volta che l'utente gioca in ASSOLUTO. Viene recuperato il suo oggetto 'UserRank'
									else {
										System.out.println("[ Non e' la prima volta che giochi in assoluto! ]");
										// [5] Viene indicato al server che non è la prima partita in assoluto dell'utente
										out.writeUTF("not first time");
										out.flush();
										// {4} Client recupera le informazioni oggetto 'UserRank' proprie
										utenteRank = (UserRank) in.readObject();
									}
									
									System.out.println("[ La parola da indovinare e' di 10 caratteri ]");
									System.out.println("[ Legenda simboli: ]");
									System.out.println("+:	->	carattere presente,	posizione giusta");
									System.out.println("?:	->	carattere presente,	posizione sbagliata");
									System.out.println("x: 	->	carattere assente,	posizione sbagliata");
									
									boolean guess = false;
									int attempt = 1;
									index = 0;
									// Il client inizia una sessione di gioco
									while(!guess && attempt < 13) {
										System.out.printf("[ Tentativo numero: (%d): Tentativi rimasti: (%d) ]\n",attempt ,13 - attempt);
										System.out.print("Inserire guessWord >> ");
										String guessWord = keyboard.nextLine();
										// Vengono rimossi eventuali spazi aggiuntivi
										guessWord.trim();
										// Viene verificato se la parola inserita non è di 10 caratteri
										if(guessWord.length() != 10) {
											System.out.println("[ Errore! Inserire una parola di 10 caratteri ]");
											continue;
										}
										int res = sendWord(TCPsocket,guessWord,in,out);
										OwnResult.add(index, OutputWord);
										// La variabile 'index' è incrementata solo se il tentativo è considerato valido
										if(res == 1)
											index++;
										/*	Il '3' è il valore di ritorno restituito dalla funzione 'sendWord' che indica che è stata 
										 * indovinata la parola segreta	*/
										if(res == 3) {
											index++;
											guess = true;
											break;
										}
										attempt += res;									
									}
									
									// {7} Il client viene informato dal server se ha vinto o perso
									String resultGame = in.readUTF();
									// {8} Il client riceve la parola segreta
									String wordWordle = in.readUTF();
									// {9} Il client riceve la traduzione in italiano della parola segreta
									String secretWordTranslate = in.readUTF();
									
									System.out.printf("[ %s! ]\n",resultGame);
									if(resultGame.equals("Win"))
										System.out.printf("[ La parola segreta e' stata indovinata al <%d> tentativo! ]\n", attempt);
									System.out.printf("[ La parola segreta era: <%s> ]\n", wordWordle);
									System.out.printf("[ La sua traduzione e' la seguente: <%s> ]\n", secretWordTranslate);
									
									// Vengono aggiornate le statistiche utente post partita
									updateStatsUser(utente,attempt,guess,wordWordle);
									double worldeScore = utente.getWordleScore();
									utenteRank.setWordleScore(worldeScore);
									// [7] Viene comunicato al server l'oggetto 'User' del client aggiornato
									out.writeObject(utente);
									out.flush();
									// [8] Viene comunicato al server l'oggetto 'UserRank' del client aggiornato
									out.writeObject(utenteRank);
									out.flush();
									
									gameDone = true;
									// Possibile condividere la partita appena disputata
									gameShare = false;
								}
								//Il client ha già giocato.Deve aspettare che esca una nuova parola segreta se vuole giocare nuovamente
								else {
									System.out.println("[ Gia' giocato! ]");
									System.out.println("[ Aspetta una che esca una nuova word ]");
								}
							}
							// L'utente ha intenzione di disconnettersi
							else if(command.equalsIgnoreCase("logout")) {
								// [4] Il client notifica al server che vuole disconnettersi
								out.writeUTF("logout");
								out.flush();
								
								/*	Quando viene effettuato il logout il client toglie la registrazione al sistema di notifiche 
								 *	cambiamento classifica del server	*/
								callbackServerStub.unregisterForCallback(callbackClientStub);
								
								System.out.println("[ Uscendo... ]");
								socketMulticast.leaveGroup(group);
								System.out.println("Client uscito dal gruppo di multicast");
								socketMulticast.close();
								TCPsocket.close();
								
								printStartInterface();
								
								// Viene interrotto il thread che stava in ascolto dei pacchetti UDP mandati sul gruppo di multicast
								if(threadShare.isAlive()) {
									try {
										threadShare.interrupt();
										threadShare.join();
									}
									catch(InterruptedException e) {
										e.printStackTrace();
									}
								}
								break;
							}
							// L'utente vuole visualizzare le proprie statistiche
							else if(command.equalsIgnoreCase("sendMeStatistics")) {
								// [4] Il client notifica al server che vuole giocare
								out.writeUTF("sendMeStatistics");
								out.flush();
								// {2} Il client recupera le statistiche generali dell'utente grazie al proprio oggetto 'User'
								utente = (User) in.readObject();
								sendMeStatistics(utente);
							}
							// L'utente vuole condividere i risultati della sua ultima partita
							else if(command.equalsIgnoreCase("share")) {
								share(username,UDPserverAddress,UDPsocket);
							}
							// L'utente vuole visualizzare i risultati condivisi dagli altri utenti 
							else if(command.equalsIgnoreCase("showMeSharing")) {
								if(AllResult.size() > 0) {
									System.out.println("[ Visualizzando i risultati degli altri utenti... ]");
									System.out.println(AllResult);
								}
								else
									System.out.println("[ Non e' ancora stato condiviso alcun risultato! ]");
							}
							// L'utente vuole visualizzare la classifica
							else if(command.equalsIgnoreCase("showMeRanking")) {
								
								// [4] Il client notifica il server che vuole visualizzare la classifica
								out.writeUTF("showMeRanking");
								out.flush();
								// {2} Il client riceve dal server la classifica sottoforma di stringa
								String Rank = in.readUTF();
								if(!Rank.equals("[]")) {
									System.out.println("\t[ Visualizzando la classifica... ]");										
									System.out.println();
									System.out.println(Rank);
									System.out.println();
								}
								else
									System.out.println("[ Classifica vuota! Ancora nessun utente ha giocato ]");
							}
							// L'utente vuole visualizzare i comandi che può digitare
							else if(command.equalsIgnoreCase("help"))
								printHelpLogin();
							// L'utente ha inserito un comando errato
							else{
								System.out.println("[ Errore: comando non valido ]");
							}
						}
									
					}
					catch(SocketException e) {
						System.err.println("Errore nella creazione del DatagramSocket");
					}
					catch(IOException e) {
						e.printStackTrace();	
					}
					catch(ClassNotFoundException e) {
						e.printStackTrace();
					}
					catch(IllegalStateException e) {
						e.printStackTrace();
					}
					catch(IllegalArgumentException e) {
						e.printStackTrace();
					}
					catch(NoSuchElementException e) {
						e.printStackTrace();
					}
				}
				catch(RemoteException e) {
					e.printStackTrace();
				}
				catch(NotBoundException e) {
					e.printStackTrace();
				}
				catch(NullPointerException e) {
					System.err.println("Passato valore nullo (non inizializzato)");
				}
				catch(IllegalArgumentException e) {
					e.printStackTrace();
				}
										
				return 0;
							
			}
			// Login fallito causa username scorretto
			if(responseLogin.equals("login fail (username)")) {
				System.out.println("[ Login fallito! Username non esistente ]");
				return -1;
			}
			// Login fallito causa password scorretta
			if(responseLogin.equals("login fail (password)")) {
				System.out.println("[ Login fallito! Password non corretta ]");
				return -1;
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		catch(IllegalArgumentException e) {
			System.err.println("Passato un numero di porta che non è compreso tra 0 e 65535 (inclusi)");
		}
		catch(NullPointerException e) {
			System.err.println("Passato valore nullo (non inizializzato)");
		}
	
		return 0;
	}
	//	-------------------------------------------- end LOGIN --------------------------------------------	//
		
	// Funzione che stampa a CLI le azioni che possono essere disputate dall'utente
	private static void printHelp() {
			
		System.out.println();
		System.out.println("<login>		---> 	permette di autenticarsi con le proprie credenziali di accesso");
	    System.out.println("<register>	---> 	permette di registrarsi creandosi un nuovo profilo");
	    System.out.println("<exit>		---> 	permette di uscire da Wordle");
	    System.out.println("<help>		---> 	permette di visualizzare la lista di comandi disponibili");
	    System.out.println();
	        
	}
	
	// Funzione che stampa a CLI le azioni che possono essere disputate dall'utente
	private static void printHelpLogin() {
				
		System.out.println();
		System.out.println("<play>			---> 	permette di iniziare una nuova sessione di gioco (se possibile)");
		System.out.println("<logout>		---> 	permette di disconnettersi dal proprio account");
		System.out.println("<sendMeStatistics>	---> 	permette di visualizzare le proprie statistiche");
		System.out.println("<share>			---> 	permette di condividere i risultati delle proprie partite disputate");
		System.out.println("<showMeSharing>		---> 	permette di visualizzare i risultati delle partite disputate dagli altri utenti");
		System.out.println("<showMeRanking>		--->	permette di visualizzare la classifica degli utenti che hanno giocato a WORDLE");
		System.out.println("<help>			---> 	permette di visualizzare la lista di comandi disponibili");
		System.out.println();
		        
	}
		
	// Funzione che stampa la schermata iniziale (CLI)
	private static void printStartInterface() {
			
		// Varie stampe a CLI 
		System.out.print("---------------------------------------------------------------------------------------------- \n");
	    System.out.println("WORDLE: un gioco di parole 3.0");
	    System.out.println("Gia' registrato? Effettua il login tramite il comando: \t\t <login>");
	    System.out.println("Nuovo utente? Registrati subito con il comando: \t\t <register>");
	    System.out.println("Per uscire da Wordle digitare il comando: \t\t\t <exit>");
	    System.out.println("Per visualizzare la lista di comandi disponibili digitare: \t <help>");
	    System.out.print("---------------------------------------------------------------------------------------------- \n");
			
	}
		
	// Funzione che stampa la schermata di login (CLI)
	private static void printLoginSuccessInterface(String username) {
			
		// Varie stampe a CLI
		System.out.println("[ Login effettuto con successo! ]");
		System.out.print("---------------------------------------------------------------------------------------------- \n");
		System.out.printf("[ Benvenuto <%s> a WORDLE un gioco di parole 3.0 ]\n",username);
		System.out.println("Cosa desidera fare:");
		System.out.println("Giocare una nuova partita: \t\t\t\t digitare 	<play>");
		System.out.println("Disconnettersi: \t\t\t\t\t digitare 	<logout>");
		System.out.println("Mostrare le proprie statistiche: \t\t\t digitare 	<sendMeStatistics>");
		System.out.println("Condividere i propri risultati: \t\t\t digitare 	<share>");
		System.out.println("Vedere i risultati degli altri utenti: \t\t\t digitare 	<showMeSharing>");
		System.out.println("Vedere la classifica degli utenti WORDLE: \t\t digitare 	<showMeRanking>");
		System.out.println("Visualizzare la lista di comandi disponibili: \t\t digitare 	<help>");
		System.out.print("---------------------------------------------------------------------------------------------- \n");
			
	}
	
	// Funzione che stampa le statistiche dell'utente aggiornate all'ultima partita giocata
	private static void sendMeStatistics(User utente) {
		System.out.println();
		System.out.println(utente);
		System.out.println();
	}
	
	// Funzione che condivide i risultati dellultima partita disputata dal giocatore sul gruppo di multicast
	private static void share(String username,InetAddress UDPserverAddress,DatagramSocket UDPsocket) throws IOException {
		if(gameDone) {
			if(!gameShare) {
				StringBuilder stringBuilderPacket = new StringBuilder();
				String stringPacket = new String();
				byte[] buffer = new byte[8194];
				System.out.println("[ Inviando al server i dati da condividere... ]");	
				// Costruisco il pacchetto da spedire
				stringBuilderPacket.append("\t WORDLE <" + username + ">: " + index + "/12 \n");
				for(int i = 0; i < index; i++) {
					stringBuilderPacket.append("\t     " + OwnResult.get(i) + "\n");
				}
				stringPacket = stringBuilderPacket.toString();
				buffer = stringPacket.getBytes();
				
				// Il client invia al SERVER il pacchetto UDP contenente i risultati della sua partita 
				DatagramPacket dp = new DatagramPacket(buffer,buffer.length,UDPserverAddress,UDPport);
				UDPsocket.send(dp);
				gameShare = true;
			}
			else
				System.out.println("[ Risultati del gioco gia' condivisi ]");
		}
		else {
			System.out.println("[ Non e' possibile condividere i risultati in quanto non e' stata disputata ancora una partita in questa sessione ]");
		}
	}
	
	/*	Funzione che dice al client se gli è permesso di iniziare una partita
	 * 	Ritrorna:	<false> se il client non può giocare
	 *				<true>	se il client può giocare	*/	
	private static boolean playWORDLE(Socket socket,ObjectInputStream in,ObjectOutputStream out) {
	//private static boolean playWORDLE(Socket socket,BufferedReader buffIn,PrintWriter pw) {
		/*	Il client manda sulla connessione tcp la richiesta di voler iniziare una partita
		 *	il server gli rispondera sempre sulla stessa connessione tcp se può giocare o meno	*/
							
		try {
			String response = in.readUTF();//{3} risposta server se può il client giocare
	
			// Se l'autenticazione ha avuto successo l'utente può continuare
			if(response.equals("can play")) {
				System.out.printf("[ Puoi giocare ]\n");
				return true;
			}
				
			// Altrimenti l'utente dovrà provare ad autenticarsi nuovamente
			else if(response.equals("cant play")) { 
				System.out.println("[ Hai gia' giocato, riprovare piu' tardi quando una nuova parola sara' estratta ]");
				return false;
			}
			else
				return false;
		}
		catch(IOException e) {
			e.printStackTrace();
			return false;
		}
	}
		
	/*	Funzione che serve per inviare al server UNA guessWord
	 *	Ritorna: 	<0> in caso di tentativo non valido	
	 *				<1> in caso di tentativo valido ma guessWord != secretWord
	 *				<3> in caso di parola indovinata	*/
	//private static int sendWord(Socket socket,String guessWord,BufferedReader buffIn,PrintWriter pw) {
	private static int sendWord(Socket socket,String guessWord,ObjectInputStream in,ObjectOutputStream out) {
		// Il client invia una guessword al server e aspetta un riscontro
		try {	
			// Viene inviata al server la guessWord
			out.writeUTF(guessWord);//[6] guessWord inviata dal client
			out.flush();
			
			// Viene letta la risposta del server se il tentativo è 'valido' o 'nullo'
			String response = in.readUTF();//{5} tentativo o valido o no
			// Viene verificato se il tentativo di inserimento è valido (guessword appartiene al Dictionary)
			if(response.equals("Valido")) {
				System.out.println("[ Tentativo valido ]");
				String GW = in.readUTF();//{5.1}
				OutputWord = in.readUTF();//{5.2}
				System.out.printf("[ GuessWord inserita dall'utente:		< %s > ]\n", GW);
				System.out.printf("[ Risultato (tips per la secretWord):		< %s > ]\n", OutputWord);
				String iguessit = in.readUTF();//{6} client guess it?
				System.out.printf("[ %s ]\n",iguessit);
				if(iguessit.equals("Indovinato"))
					return 3;
			}
			// Viene verificato se il tentativo di inserimento non è valido (guessword non appartiene al Dictionary)
			else if(response.equals("Nullo")) {
				// Il client deve rimandare una nuova guess word
				System.out.println("[ Tentativo non valido ]");
				return 0;
			}
			else {
				System.out.println("[ Errore ]");
				return 0;
			}
			
			return 1;
			
		}catch(IOException e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	// Funzione che aggiorna le statistiche dell'utente post partita
	private static void updateStatsUser(User user,int attempt,boolean guess,String wordWordle) {
		user.incrGamesPayed();
		// Se l'utente ha indovinato
		if(guess) {
			user.incrGamesWon();
			user.incrWinStreak();
			int winStreak = user.getWinStreak();
			if(winStreak > user.getMaxWinStreak())
				user.setMaxWinStreak(winStreak);
			int [] guessDistribuition = user.getGuessDistribution();
			guessDistribuition[attempt-1]++;
		}
		else {
			user.incrGamesLost();
			user.setWinStreak(0);
		}
		user.setLastWordWordle(wordWordle);
		int gamesWon = user.getGamesWon();
		int gamesPlayed = user.getGamesPlayed();
		double winRate = ((double)gamesWon/(double)gamesPlayed) * 100;
		user.setWinRate(winRate);
		
		double wordleScore = computeScore(user.getGamesPlayed(),user.getGuessDistribution());
		user.setWordleScore(wordleScore);
		
	}
	
	/**
     * Calcola il Wordle Average Score (WAS) dell'utente.
     * @param numPlayed numero totale di partite giocate
     * @param guessDist guess distribution dell'utente
     * @return il Wordle Average Score dell'utente
     */
    public static double computeScore(int numPlayed, int[] guessDist) {
    	final int maxAttempts = 12;
        int sum = 0, numGuessed = 0;
        for (int i = 0; i < guessDist.length; i++) {
            sum += (i + 1) * guessDist[i];
            numGuessed += guessDist[i];
        }
        /*	Per le parole non indovinate (partite perse), 
         *	considero un numero di tentativi pari a 13 (ovvero maxAttempts + 1)	*/
        sum += (maxAttempts + 1) * (numPlayed - numGuessed);
        return ((double) sum / (double) numPlayed);
        
    }
    
	//Thread che serve per memorizzare i risultati degli utenti inviati sul gruppo di multicast tramite pacchetti UDP
	private static class Multicast implements Runnable {
		private MulticastSocket ms;
		
		public Multicast(MulticastSocket ms) {
			this.ms = ms;
		}
		
		public void run() {
			byte[] buffer = new byte[8194];
			try {
				while(true) {
					DatagramPacket dp = new DatagramPacket(buffer,8194);
					ms.receive(dp);
					String received = new String(dp.getData());
					AllResult.add(received);
				}
			}
			catch (IOException e) {
				return;
			}
		}
	}
		
}

