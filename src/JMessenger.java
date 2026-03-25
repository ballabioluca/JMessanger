import java.io.*;
import java.net.*;
import java.util.Scanner;

public class JMessenger {
    private static final int DEFAULT_PORT = 5000;

    private static String destIp = null;
    private static ServerThread serverThread = null;
    private static String myIp;

    public static void main(String[] args) {
        try {
            myIp = InetAddress.getLocalHost().getHostAddress();
        } catch (IOException e) {
            myIp = "127.0.0.1";
        }

        Scanner sc = new Scanner(System.in);

        System.out.println("=================== JMessenger CLI ===================");
        System.out.println("=== Luca Ballabio assisted by OpenAI ChatGPT ===");
        System.out.println("======================================================");
        System.out.println("Server auto-starts on port " + DEFAULT_PORT);
        System.out.println("Type /help for available commands.\n");

        // ✅ Start server automatically
        serverThread = new ServerThread(DEFAULT_PORT);
        new Thread(serverThread).start();

        while (true) {
            System.out.print("> ");
            String input = sc.nextLine().trim();
            if (input.isEmpty()) continue;

            switch (input.toLowerCase()) {

                case "/help":
                    System.out.println("Available commands:");
                    System.out.println("/ip     - Set recipient IP");
                    System.out.println("/info   - Show local and recipient info");
                    System.out.println("/clear  - Clear console");
                    System.out.println("/exit   - Exit program");
                    break;

                case "/clear":
                    clearConsole();
                    break;

                case "/ip":
                    System.out.print("Recipient IP: ");
                    String ipInput = sc.nextLine().trim();

                    if (ipInput.isEmpty()) {
                        System.out.println("IP cannot be empty.");
                        break;
                    }

                    try {
                        InetAddress.getByName(ipInput); // validate
                        destIp = ipInput.equalsIgnoreCase("localhost") ? "127.0.0.1" : ipInput;
                        System.out.println("Recipient set to " + destIp + ":" + DEFAULT_PORT);
                    } catch (Exception e) {
                        System.out.println("Invalid IP address.");
                    }
                    break;

                case "/info":
                    System.out.println("Your IP: " + myIp);
                    System.out.println("Listening on port: " + DEFAULT_PORT);
                    System.out.println("Destination IP: " + (destIp != null ? destIp : "not set"));
                    System.out.println("Destination Port: " + (destIp != null ? DEFAULT_PORT : "not set"));
                    break;

                case "/exit":
                    System.out.println("Exiting...");
                    if (serverThread != null) serverThread.stopServer();
                    sc.close();
                    return;

                default:
                    if (destIp == null) {
                        System.out.println("Recipient not set. Use /ip first.");
                    } else {
                        sendMessage(destIp, DEFAULT_PORT, input);
                    }
                    break;
            }
            System.out.println(); // Riga vuota dopo ogni comando
        }
    }

    private static void clearConsole() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            for (int i = 0; i < 50; i++) System.out.println();
        }
    }

    private static class ServerThread implements Runnable {
        private ServerSocket serverSocket;
        private final int port;
        private boolean running = true;

        public ServerThread(int port) {
            this.port = port;
        }

        public void stopServer() {
            running = false;
            try {
                if (serverSocket != null) serverSocket.close();
            } catch (IOException ignored) {}
        }
        @Override
        public void run() {
            try (ServerSocket ss = new ServerSocket(port)) {
                this.serverSocket = ss;

                while (running) {
                    try {
                        Socket client = ss.accept();
                        new Thread(() -> handleClient(client)).start();
                    } catch (IOException e) {
                        if (running) System.out.println("Server error: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.out.println("Server could not start: " + e.getMessage());
            }
        }
        private void handleClient(Socket socket) {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()))) {

                String senderIp = socket.getInetAddress().getHostAddress();
                String msg;

                while ((msg = in.readLine()) != null) {
                    synchronized (System.out) {
                        System.out.println("\n" + senderIp + " > " + msg);
                        System.out.print("> ");
                    }
                }

            } catch (IOException e) {
                System.out.println("Connection closed by " +
                        socket.getInetAddress().getHostAddress());
            }
        }
    }

    private static void sendMessage(String destIp, int port, String msg) {
        try (Socket socket = new Socket(destIp, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println(msg);
            System.out.println(myIp + " > " + msg);

        } catch (IOException e) {
            System.out.println("Send error: " + e.getMessage());
        }
    }
}