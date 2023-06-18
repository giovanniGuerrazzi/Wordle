import java.io.Serializable;

// Classe che contiene tutte le statistiche dell'utente
public class User implements Serializable {
	
	// Default serial version ID
	private static final long serialVersionUID = 1L;
	
	// 				Statistiche utente			//
	//				Identità 					//
	private String username;
	private String password;
	//				Punteggio					//
	private double wordleScore;
	//  		 	Statistiche utente 			//
	private int gamesPlayed;
	private int gamesWon;
	private int gamesLost;
	private double winRate;
	private int winStreak;
	private int maxWinStreak;
	private int[] guessDistribution;
	//				Ultima partita giocata		//
	private String lastWordWordle;
	
	public User(String username, String password) {
		this.username = username;
		this.password = password;
		this.lastWordWordle = "----------";
		this.gamesPlayed = 0;
		this.winRate = 0.0;
		this.winStreak = 0;
		this.maxWinStreak = 0;
		this.guessDistribution = new int[12];// Tutti gli elementi dell' array sono inizializzati a 0;
		this.gamesWon = 0;
		this.gamesLost = 0;
		this.wordleScore = 0.0;
		
	}
	//		---------------------------------------	GETTERS	---------------------------------------		//
	
	public String getUsername() {
		return username;
	}
	
	public double getWordleScore() {
		return wordleScore;
	}
	
	public int getGamesPlayed() {
		return gamesPlayed;
	}
	
	public int getGamesWon() {
		return gamesWon;
	}
		
	public int getWinStreak() {
		return winStreak;
	}
	
	public int getMaxWinStreak() {
		return maxWinStreak;
	}
	
	public int[] getGuessDistribution() {
		return guessDistribution;
	}
	
	public String getLastWordWordle() {
		return lastWordWordle;
	}
	
	//	---------------------------------------	SETTERS	---------------------------------------		//
	
	public void setWordleScore(double wordleScore) {
		this.wordleScore = wordleScore;
	}
	
	public void setWinRate(double winRate) {
		this.winRate = winRate;
	}

	public void setWinStreak(int winStreak) {
		this.winStreak = winStreak;
	}

	public void setMaxWinStreak(int maxWinStreak) {
		this.maxWinStreak = maxWinStreak;
	}
	
	public void setLastWordWordle(String lastWordWordle) {
		this.lastWordWordle = lastWordWordle;
	}
	
	//	---------------------------------------	UTILITY	---------------------------------------		//
	
	public void incrGamesPayed() {
		gamesPlayed++;
	}
	
	public void incrGamesWon() {
		gamesWon++;
	}
	
	public void incrGamesLost() {
		gamesLost++;
	}
	
	public void incrWinStreak() {
		winStreak++;
	}
	
	public String toString() {
		String formatWordleScore = String.format("%.02f",wordleScore);
		String formatWinRate = String.format("%.02f",winRate);
		return 	"Username:		"+ username + "\nPassword:		" + password + "\nWordleScore: 		" + formatWordleScore + 
				"\nGamesPlayed:		" + gamesPlayed + "\nGamesWon: 		" + gamesWon + "\nGamesLost: 		" + gamesLost +
				"\nWinRate: 		" + formatWinRate + " %" + "\nWinStreak: 		" + winStreak + "\nMaxWinStreak: 		" + maxWinStreak +
				"\nGuessDistribution:	" + printGuessDistribution(guessDistribution) + "\nLastWordWordle:		" + lastWordWordle; 
	}

	// Funzione per la stampa della guessDistribution dell'utente
	private String printGuessDistribution(int[] guessDistribution) {
		StringBuilder guessDistributionString = new StringBuilder();
		guessDistributionString.append("{ ");
		for(int i = 0; i < 12; i++) {
			guessDistributionString.append(guessDistribution[i]);
			if(i != 11)
				guessDistributionString.append(", ");
		}
		guessDistributionString.append(" }");
		return guessDistributionString.toString();
	}
}
