import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Utility {
	
	/*	Leggo il file e ritorno il contenuto sotto forma di stringa.Viene ritornato 'null' in caso di file
	 *	non presenti ovvero quando il server viene avviato per la primissima volta e non ci sono dati registrati	*/
	public static String readFile(String file) {

		try(FileInputStream fis = new FileInputStream(file);){
			byte[] byteArray = fis.readAllBytes();
			String data = new String(byteArray);
			return data;
		}
		catch(FileNotFoundException e) {
			System.out.println("File non presente.Server avviato per la prima volta!");
			return null;
		}
		catch(IOException e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
	// Scrivo la stringa (struttura dati) sul file
	public static void writeFile(String file,String data) {
		try(FileOutputStream fos = new FileOutputStream(file);){
			fos.write(data.getBytes());
		}
		catch(FileNotFoundException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		catch(IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

}
