import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 4999);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Scrivi un messaggio e premi INVIO:");
            String linea = userInput.readLine();   // legge una riga da tastiera
            out.println(linea);                   // invia la riga al main
            System.out.println("Messaggio inviato: " + linea);

        } catch (IOException e) {
            System.out.println("Errore");
        }
    }
}
