import java.io.*;
import java.net.*;
import java.util.Scanner;

public class JMessenger {
    private static String destIp = null;
    private static int destPort = -1;
    private static int localPort = -1;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("=== JMessenger P2P CLI ===");
        System.out.println("Type /help for available commands.\n");

        while (true) {
            System.out.print("> ");
            String input = sc.nextLine().trim();

            switch (input.toLowerCase()) {
                case "/help":
                    System.out.println("Available commands:");
                    System.out.println("/p      - Set local listening port");
                    System.out.println("/ip     - Set recipient IP and port");
                    System.out.println("/exit   - Exit the program");
                    System.out.println("/help   - Show this help");
                    System.out.println("/clear  - Clear the console");
                    break;

                case "/clear":
                    clearConsole();
                    break;

                case "/p":
                    System.out.print("Enter local port: ");
                    localPort = Integer.parseInt(sc.nextLine().trim());
                    new Thread(() -> startServer(localPort)).start();
                    System.out.println("Server listening on port " + localPort + "...");
                    break;

                case "/ip":
                    System.out.print("Recipient IP: ");
                    destIp = sc.nextLine().trim();
                    if (destIp.equalsIgnoreCase("localhost")) destIp = "127.0.0.1";

                    System.out.print("Recipient Port: ");
                    destPort = Integer.parseInt(sc.nextLine().trim());
                    System.out.println("Recipient set to " + destIp + ":" + destPort);
                    break;

                case "/exit":
                    System.out.println("Exiting...");
                    sc.close();
                    return;

                default:
                    if (destIp == null || destPort == -1) {
                        System.out.println("Recipient not set. Use /ip to set recipient.");
                    } else {
                        sendMessage(destIp, destPort, input);
                    }
                    break;
            }
        }
    }

    // ---------------- CONSOLE CLEAR ----------------
    private static void clearConsole() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // Fallback: print blank lines
            for (int i = 0; i < 50; i++) System.out.println();
        }
    }

    // ---------------- SERVER ----------------
    private static void startServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleClient(socket)).start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
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
            System.out.println("Connection closed by " + socket.getInetAddress().getHostAddress());
        }
    }

    // ---------------- CLIENT ----------------
    private static void sendMessage(String destIp, int destPort, String msg) {
        try (Socket socket = new Socket(destIp, destPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(msg);
            String myIp = InetAddress.getLocalHost().getHostAddress();
            System.out.println(myIp + " > " + msg);
        } catch (IOException e) {
            System.out.println("Send error: " + e.getMessage());
        }
    }
}
