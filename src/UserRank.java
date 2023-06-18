import java.io.Serializable;

// Classe confrontabile che contiene lo username e il wordleScore dell'utente che permette di avere una classifica
public class UserRank implements Serializable,Comparable<UserRank>{
	
	// Default serial version ID
	private static final long serialVersionUID = 1L;
		
	private String username;
	private double wordleScore;
	
	public UserRank (String username,double wordleScore){
		this.username = username;
		this.wordleScore = wordleScore;
	}

	public String getUsername(){
		return username;
	}
	public double getWordleScore() {
		return wordleScore;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	public void setWordleScore(double wordleScore) {
		this.wordleScore = wordleScore;
	}
	
	public String toString() {
		String formatWordleScore = String.format("%.02f",wordleScore);
		return "\nUsername:\t" + username + "\t\tWordleScore: " + formatWordleScore + "\n";
	}
	
	public boolean equals(Object obj) {
		if(!(obj instanceof UserRank))
            return false;
        UserRank userRank = (UserRank) obj;
        return userRank.username.equals(username);
		
	}
	public int compareTo(UserRank user) {
		return wordleScore > user.wordleScore ? 1 : wordleScore < user.wordleScore ? -1 : 0;
	}
}

