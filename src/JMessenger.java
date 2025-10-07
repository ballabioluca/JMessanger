import java.io.*;
import java.net.*;
import java.util.Scanner;

public class JMessenger {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Porta locale per la ricezione: ");
        int localPort = sc.nextInt();
        sc.nextLine();

        // Avvia server in background
        new Thread(() -> startServer(localPort)).start();
        System.out.println("In ascolto sulla porta " + localPort + "...\n");

        while (true) {
            System.out.print("> ");
            String input = sc.nextLine().trim();

            // Scrivendo 0 si esce, senza messaggi aggiuntivi
            if (input.equals("0")) break;

            try {
                System.out.print("Porta destinatario: ");
                int destPort = Integer.parseInt(sc.nextLine().trim());

                System.out.print("Messaggio: ");
                String msg = sc.nextLine();

                // Se l'IP è "localhost", lo accetta come valido
                String destIp = input.equalsIgnoreCase("localhost") ? "127.0.0.1" : input;

                sendMessage(destIp, destPort, msg);
            } catch (Exception e) {
                System.out.println("Errore: " + e.getMessage());
            }
        }

        sc.close();
    }

    // ---------------- SERVER ----------------
    private static void startServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleClient(socket)).start();
            }
        } catch (IOException e) {
            System.out.println("Errore server: " + e.getMessage());
        }
    }

    private static void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String senderIp = socket.getInetAddress().getHostAddress();
            String msg;
            while ((msg = in.readLine()) != null) {
                System.out.println("\n" + senderIp + " > " + msg);
                System.out.print("> ");
            }
        } catch (IOException e) {
            System.out.println("Errore ricezione messaggio: " + e.getMessage());
        }
    }

    // ---------------- CLIENT ----------------
    private static void sendMessage(String destIp, int destPort, String msg) {
        try (Socket socket = new Socket(destIp, destPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(msg);
            System.out.println("Tu > " + msg);
        } catch (IOException e) {
            System.out.println("Errore invio: " + e.getMessage());
        }
    }
}
