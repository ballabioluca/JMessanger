import java.io.*;
import java.net.*;
import java.util.Scanner;

public class JMessanger {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("Benvenuto in JMessenger!");
        System.out.println("1. Avvia Server");
        System.out.println("2. Avvia Client");
        System.out.print("Scegli un'opzione: ");

        int scelta = sc.nextInt();
        sc.nextLine(); // pulisce buffer

        if (scelta == 1) {
            runServer();
        } else if (scelta == 2) {
            runClient();
        } else {
            System.out.println("Scelta non valida, esco.");
        }
    }

    // Metodo server
    private static void runServer() {
        System.out.println("SERVER ONLINE");
        try (ServerSocket ss = new ServerSocket(4999)) {
            System.out.println("In attesa di connessioni...");
            Socket client = ss.accept();
            System.out.println("CLIENT --> CONNECTED");

            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter out = new PrintWriter(client.getOutputStream(), true);

            String msg;
            while ((msg = in.readLine()) != null) {
                System.out.println("CLIENT: " + msg);
                out.println("SERVER REPLY: " + msg);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Metodo client
    private static void runClient() {
        try (Scanner sc = new Scanner(System.in)) {
            System.out.print("Inserisci IP server: ");
            String ip = sc.nextLine();

            try (Socket socket = new Socket(ip, 4999);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                System.out.println("Connesso al server. Scrivi messaggi:");

                String msg;
                while ((msg = sc.nextLine()) != null) {
                    out.println(msg);
                    System.out.println("SERVER: " + in.readLine());
                }

            }

        } catch (IOException e) {
            System.out.println("Errore di connessione: " + e.getMessage());
        }
    }
}

