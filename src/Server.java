import java.net.*;
import java.io.*;

public class Server {
    public static void main(String[] args) {
        System.out.println("SERVER ONLINE");
        try (ServerSocket ss = new ServerSocket(4999)) {
            for (int i = 0; i < 10; i++) {
                try (Socket s = ss.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                     PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {

                    System.out.println("CLIENT --> CONNECTED");

                    String str = in.readLine();
                    System.out.println("CLIENT: " + str);

                    // Risposta al client
                    out.println("INCOMING MSG: " + str);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
