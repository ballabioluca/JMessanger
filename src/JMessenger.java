import java.io.*;
import java.net.*;
import java.util.Scanner;

public class JMessenger {
    private static final int TCP_PORT = 5000;             // porta TCP
    private static final int MULTICAST_PORT = 5001;       // porta UDP multicast
    private static final String MULTICAST_GROUP = "230.0.0.1";

    private static String destIp = null;
    private static boolean multicastMode = false;
    private static ServerThread tcpServer = null;
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
        System.out.println("TCP port: " + TCP_PORT);
        System.out.println("Broadcast/Multicast group: " + MULTICAST_GROUP + ":" + MULTICAST_PORT);
        System.out.println("Type /help for commands.\n");

        // Avvia server TCP
        tcpServer = new ServerThread(TCP_PORT);
        new Thread(tcpServer).start();

        // Avvia server UDP multicast
        new Thread(new MulticastServerThread(MULTICAST_GROUP, MULTICAST_PORT)).start();

        while (true) {
            System.out.print("> ");
            String input = sc.nextLine().trim();
            if (input.isEmpty()) continue;

            switch (input.toLowerCase()) {

                case "/help":
                    System.out.println("Commands:");
                    System.out.println("/ip           - set recipient IP or 'broadcast'");
                    System.out.println("/info         - show info");
                    System.out.println("/clear        - clear console");
                    System.out.println("/exit         - exit");
                    break;

                case "/clear":
                    clearConsole();
                    break;

                case "/ip":
                    System.out.print("Recipient IP (or 'broadcast'): ");
                    String ipInput = sc.nextLine().trim();

                    if (ipInput.isEmpty()) {
                        System.out.println("IP cannot be empty.");
                        break;
                    }

                    if (ipInput.equalsIgnoreCase("broadcast")) {
                        multicastMode = true;
                        destIp = MULTICAST_GROUP; // usa multicast al posto di broadcast
                        System.out.println("Broadcast mode enabled on group " + MULTICAST_GROUP + ":" + MULTICAST_PORT);
                        break;
                    }

                    try {
                        InetAddress.getByName(ipInput); // validate
                        destIp = ipInput.equalsIgnoreCase("localhost") ? "127.0.0.1" : ipInput;
                        multicastMode = false;
                        System.out.println("Recipient set to " + destIp + ":" + TCP_PORT);
                    } catch (Exception e) {
                        System.out.println("Invalid IP address.");
                    }
                    break;

                case "/info":
                    System.out.println("Your IP: " + myIp);
                    System.out.println("TCP Listening on port: " + TCP_PORT);
                    System.out.println("Broadcast/Multicast group: " + MULTICAST_GROUP + ":" + MULTICAST_PORT);
                    System.out.println("Destination IP: " + (destIp != null ? destIp : "not set"));
                    System.out.println("Mode: " + (multicastMode ? "BROADCAST/MULTICAST" : "TCP"));
                    break;

                case "/exit":
                    System.out.println("Exiting...");
                    if (tcpServer != null) tcpServer.stopServer();
                    sc.close();
                    return;

                default:
                    if (destIp == null) {
                        System.out.println("Recipient not set. Use /ip first.");
                    } else {
                        if (multicastMode) {
                            sendMulticast(input);
                        } else {
                            sendMessage(destIp, TCP_PORT, input);
                        }
                    }
                    break;
            }
        }
    }

    // ================== FUNZIONI ==================
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

    private static void sendMessage(String destIp, int port, String msg) {
        try (Socket socket = new Socket(destIp, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(msg);
            System.out.println(myIp + " > " + msg);
        } catch (IOException e) {
            System.out.println("Send error: " + e.getMessage());
        }
    }

    private static void sendMulticast(String msg) {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] buffer = msg.getBytes();
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, MULTICAST_PORT);
            socket.send(packet);
            System.out.println(myIp + " (broadcast) > " + msg);
        } catch (IOException e) {
            System.out.println("Multicast send error: " + e.getMessage());
        }
    }

    // ================== SERVER TCP ==================
    private static class ServerThread implements Runnable {
        private ServerSocket serverSocket;
        private final int port;
        private boolean running = true;

        public ServerThread(int port) { this.port = port; }

        public void stopServer() {
            running = false;
            try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
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
                        if (running) System.out.println("TCP server error: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.out.println("TCP server could not start: " + e.getMessage());
            }
        }

        private void handleClient(Socket socket) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String senderIp = socket.getInetAddress().getHostAddress();
                String msg;
                while ((msg = in.readLine()) != null) {
                    synchronized (System.out) {
                        System.out.println("\n" + senderIp + " > " + msg);
                        System.out.print("> ");
                    }
                }
            } catch (IOException e) {
                System.out.println("Connection closed by " + socket.getInetAddress().getHostAddress());
            }
        }
    }

    // ================== SERVER MULTICAST ==================
    private static class MulticastServerThread implements Runnable {
        private final String groupAddress;
        private final int port;
        private boolean running = true;

        public MulticastServerThread(String groupAddress, int port) {
            this.groupAddress = groupAddress;
            this.port = port;
        }

        @Override
        public void run() {
            try (MulticastSocket socket = new MulticastSocket(port)) {
                InetAddress group = InetAddress.getByName(groupAddress);
                socket.joinGroup(group);

                byte[] buffer = new byte[1024];
                while (running) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength());
                    String senderIp = packet.getAddress().getHostAddress();

                    synchronized (System.out) {
                        System.out.println("\n" + senderIp + " (broadcast) > " + msg);
                        System.out.print("> ");
                    }
                }
            } catch (IOException e) {
                System.out.println("Multicast server error: " + e.getMessage());
            }
        }
    }

}