import java.io.*;
import java.net.*;
import java.util.*;

public class JMessenger {
    private static int currentPort = 5000;
    private static String singleDestIp = null;
    private static final Set<String> groupList = new LinkedHashSet<>();
    private static ServerThread serverThread = null;
    private static String myIp;

    public static void main(String[] args) {
        myIp = getRealIp();
        Scanner sc = new Scanner(System.in);

        System.out.println("JMessenger Hybrid [P2P + Group]");
        System.out.println("Your IP: " + myIp + " | Port: " + currentPort);
        System.out.println("Type 'help' for commands.\n");

        startServer();

        while (true) {
            System.out.print("> ");
            String input = sc.nextLine().trim();
            if (input.isEmpty()) continue;

            String[] parts = input.split("\\s+", 2);
            String cmd = parts[0].toLowerCase();
            String argsStr = (parts.length > 1) ? parts[1] : "";

            switch (cmd) {
                case "help":
                    printHelp();
                    break;

                // --- SINGLE P2P COMMANDS ---
                case "ip":
                    if (!argsStr.isEmpty()) {
                        singleDestIp = argsStr;
                        System.out.println("Single recipient set to: " + singleDestIp);
                    } else {
                        System.out.println("Usage: ip <address>");
                    }
                    break;

                // --- GROUP COMMANDS ---
                case "add":
                    if (!argsStr.isEmpty()) {
                        groupList.add(argsStr);
                        System.out.println("Added to group: " + argsStr);
                    }
                    break;

                case "list":
                    System.out.println("Group contacts: " + groupList);
                    break;

                case "clearlist":
                    groupList.clear();
                    System.out.println("Group list cleared.");
                    break;

                case "info":
                    System.out.println("Local IP: " + myIp);
                    System.out.println("Single Target: " + (singleDestIp != null ? singleDestIp : "None"));
                    System.out.println("Group Size: " + groupList.size());
                    break;

                case "exit":
                    if (serverThread != null) serverThread.stopServer();
                    return;

                default:
                    handleMessaging(input);
                    break;
            }
        }
    }

    private static void printHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("ip <address>    - Set a single recipient for 1-to-1 chat");
        System.out.println("add <address>   - Add an IP to the group list");
        System.out.println("list            - Show all group members");
        System.out.println("clearlist       - Remove everyone from the group");
        System.out.println("info            - Show connection status");
        System.out.println("exit            - Quit");
    }

    private static void handleMessaging(String msg) {
        // Se la lista di gruppo non è vuota, invia a tutti (incluso il singleDestIp se presente)
        if (!groupList.isEmpty()) {
            for (String ip : groupList) {
                sendUnicast(ip, msg, false);
            }
            System.out.println("You (Group) > " + msg);
        }
        // Altrimenti, se è impostato solo un IP singolo, invia solo a quello
        else if (singleDestIp != null) {
            sendUnicast(singleDestIp, msg, true);
        }
        else {
            System.out.println("Error: No recipient or group list set. Use 'ip' or 'add'.");
        }
    }

    private static void sendUnicast(String ip, String msg, boolean printLabel) {
        new Thread(() -> {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ip, currentPort), 1000);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(msg);
                if (printLabel) {
                    synchronized (System.out) {
                        System.out.println("You > " + msg);
                    }
                }
            } catch (IOException e) {
                System.err.println("\n[System] Failed to reach: " + ip);
            }
        }).start();
    }

    private static void startServer() {
        serverThread = new ServerThread(currentPort);
        new Thread(serverThread).start();
    }

    private static String getRealIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.getDisplayName().toLowerCase().contains("virtual")) continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) return addr.getHostAddress();
                }
            }
        } catch (Exception e) { return "127.0.0.1"; }
        return "127.0.0.1";
    }

    private static class ServerThread implements Runnable {
        private ServerSocket serverSocket;
        private final int port;
        private volatile boolean running = true;

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
                    Socket client = ss.accept();
                    new Thread(() -> handleClient(client)).start();
                }
            } catch (IOException e) {
                if (running) System.out.println("Server error.");
            }
        }

        private void handleClient(Socket socket) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String msg = in.readLine();
                if (msg != null) {
                    synchronized (System.out) {
                        System.out.println("\n" + socket.getInetAddress().getHostAddress() + " > " + msg);
                        System.out.print("> ");
                    }
                }
            } catch (IOException ignored) {}
        }
    }
}