import java.io.*;
import java.net.*;
import java.util.Scanner;

public class JMessenger {
    private static String destIp = null;
    private static int destPort = -1;
    private static ServerThread serverThread = null;
    private static String myIp;

    public static void main(String[] args) {
        try {
            myIp = InetAddress.getLocalHost().getHostAddress();
        } catch (IOException e) {
            myIp = "127.0.0.1"; // fallback
        }

        Scanner sc = new Scanner(System.in);
        System.out.println("============== JMessenger CLI ==============");
        System.out.println("=== Luca Ballabio, OpenAI ChatGTP-5 mini ===");
        System.out.println("============================================");
        System.out.println("Type /help for available commands.\n");

        while (true) {
            System.out.print("> ");
            String input = sc.nextLine().trim();
            if (input.isEmpty()) continue;

            switch (input.toLowerCase()) {
                case "/help":
                    System.out.println("Available commands:");
                    System.out.println("/p      - Set local listening port");
                    System.out.println("/ip     - Set recipient IP and port");
                    System.out.println("/info   - Show local port and current recipient info");
                    System.out.println("/exit   - Exit the program");
                    System.out.println("/help   - Show this help");
                    System.out.println("/clear  - Clear the console");
                    break;

                case "/clear":
                    clearConsole();
                    break;

                case "/p":
                    System.out.print("Enter local port: ");
                    int newPort = readPort(sc);
                    if (serverThread != null) serverThread.stopServer();
                    serverThread = new ServerThread(newPort);
                    new Thread(serverThread).start();
                    System.out.println("Server listening on port " + newPort + "...");
                    break;

                case "/ip":
                    System.out.print("Recipient IP: ");
                    String ipInput = sc.nextLine().trim();
                    if (ipInput.isEmpty()) {
                        System.out.println("IP cannot be empty.");
                        break;
                    }
                    destIp = ipInput.equalsIgnoreCase("localhost") ? "127.0.0.1" : ipInput;

                    System.out.print("Recipient Port: ");
                    destPort = readPort(sc);
                    System.out.println("Recipient set to " + destIp + ":" + destPort);
                    break;

                case "/info":
                    System.out.println("Local listening port: " + (serverThread != null ? serverThread.getPort() : "not set"));
                    System.out.println("Destination IP: " + (destIp != null ? destIp : "not set"));
                    System.out.println("Destination Port: " + (destPort != -1 ? destPort : "not set"));
                    break;

                case "/exit":
                    System.out.println("Exiting...");
                    if (serverThread != null) serverThread.stopServer();
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

    private static int readPort(Scanner sc) {
        while (true) {
            String line = sc.nextLine().trim();
            if (line.isEmpty()) {
                System.out.print("Port cannot be empty. Try again: ");
                continue;
            }
            try {
                int port = Integer.parseInt(line);
                if (port > 0 && port <= 65535) return port;
                System.out.print("Invalid port range (1-65535). Try again: ");
            } catch (NumberFormatException e) {
                System.out.print("Invalid number. Try again: ");
            }
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

        public int getPort() {
            return this.port;
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
    }

    private static void sendMessage(String destIp, int destPort, String msg) {
        try (Socket socket = new Socket(destIp, destPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(msg);
            System.out.println(myIp + " > " + msg);
        } catch (IOException e) {
            System.out.println("Send error: " + e.getMessage());
        }
    }
}
