import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class JMessenger {
    private static int currentPort = 5000;
    private static String singleDestIp = null;
    private static final Set<String> groupList = new LinkedHashSet<>();
    private static ServerThread serverThread = null;
    private static String myIp;

    public static void main(String[] args) {
        myIp = getRealIp();
        Scanner sc = new Scanner(System.in);

        System.out.println("JMessenger CLI [Version 2.8]");
        System.out.println("Copyright (c) 202X. Free use.");
        System.out.println("Local IP: " + myIp + " | Port: " + currentPort);
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

                case "scan":
                    scanNetwork();
                    break;

                case "autogroup":
                    autogroupNetwork();
                    break;

                case "ip":
                    if (!argsStr.isEmpty()) {
                        singleDestIp = argsStr;
                        System.out.println("Single recipient set to: " + singleDestIp);
                    } else {
                        System.out.println("Usage: ip <address>");
                    }
                    break;

                case "add":
                    if (!argsStr.isEmpty()) {
                        groupList.add(argsStr);
                        System.out.println("Added to group: " + argsStr);
                    } else {
                        System.out.println("Usage: add <address>");
                    }
                    break;

                case "remove":
                    if (!argsStr.isEmpty()) {
                        if (groupList.remove(argsStr)) {
                            System.out.println("Removed from group: " + argsStr);
                        } else {
                            System.out.println("IP not found in group: " + argsStr);
                        }
                    } else {
                        System.out.println("Usage: remove <address>");
                    }
                    break;

                case "showgroup":
                    System.out.println("Group contacts: " + groupList);
                    break;

                case "cleargroup":
                    groupList.clear();
                    System.out.println("Group list cleared.");
                    break;

                case "info":
                    System.out.println("Local: " + myIp + ":" + currentPort);
                    System.out.println("Target: " + (singleDestIp != null ? singleDestIp : "None"));
                    System.out.println("Group List: " + groupList);
                    break;

                case "exit":
                    if (serverThread != null) serverThread.stopServer();
                    System.out.println("Exiting...");
                    System.exit(0);
                    return;

                default:
                    handleMessaging(input);
                    break;
            }
            System.out.println();
        }
    }

    private static void printHelp() {
        System.out.println("\nCommands:");
        System.out.println("scan             - Search for JMessenger users on local network");
        System.out.println("ip <address>     - Set single recipient for 1-to-1");
        System.out.println("add <address>    - Add IP to group");
        System.out.println("remove <address> - Remove IP to group");
        System.out.println("showgroup            - Show all group members");
        System.out.println("autogroup        - Scan for users and automatically add them to group");
        System.out.println("cleargroup       - Remove all users from group");
        System.out.println("info / exit      - System info and quit");
    }

    private static void autogroupNetwork() {
        System.out.println("Scanning and grouping...");
        List<String> foundIps = performNetworkScan();

        if (foundIps.isEmpty()) {
            System.out.println("No new users found to add.");
        } else {
            int newUsers = 0;
            for (String ip : foundIps) {
                if (groupList.add(ip)) {
                    newUsers++;
                }
            }
            if (newUsers > 0) {
                System.out.println("Found and added " + newUsers + " new user(s).");
                System.out.println("Updated Group contacts: " + groupList);
            } else {
                System.out.println("No new users found. Group list is already up to date.");
            }
        }
    }

    private static void scanNetwork() {
        System.out.println("Scanning subnet " + myIp.substring(0, myIp.lastIndexOf('.') + 1) + "0/24...");
        List<String> foundIps = performNetworkScan();

        if (foundIps.isEmpty()) {
            System.out.println("No users found.");
        } else {
            System.out.println("Found " + foundIps.size() + " user(s): " + foundIps);
        }
    }

    private static List<String> performNetworkScan() {
        String subnet = myIp.substring(0, myIp.lastIndexOf('.') + 1);
        ExecutorService executor = Executors.newFixedThreadPool(30);
        List<String> foundIps = Collections.synchronizedList(new ArrayList<>());

        for (int i = 1; i < 255; i++) {
            final String targetIp = subnet + i;
            if (targetIp.equals(myIp)) continue;

            executor.submit(() -> {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(targetIp, currentPort), 200);
                    foundIps.add(targetIp);
                } catch (IOException ignored) {}
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}
        return foundIps;
    }

    private static void handleMessaging(String msg) {
        if (!groupList.isEmpty()) {
            for (String ip : groupList) sendUnicast(ip, msg, false);
            System.out.println("You (Group) > " + msg);
        } else if (singleDestIp != null) {
            sendUnicast(singleDestIp, msg, true);
        } else {
            System.out.println("Error: Set an IP first (ip <addr> or add <addr>).");
        }
    }

    private static void sendUnicast(String ip, String msg, boolean printLabel) {
        new Thread(() -> {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ip, currentPort), 1000);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(msg);
                if (printLabel) {
                    synchronized (System.out) { System.out.println("You > " + msg); }
                }
            } catch (IOException e) {
                System.err.println("\n[System] Offline: " + ip);
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
            } catch (IOException e) { if (running) System.out.println("Server error."); }
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