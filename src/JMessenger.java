import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.Scanner;

public class JMessenger {
    private static int currentPort = 5000;
    private static String destIp = null;
    private static ServerThread serverThread = null;
    private static String myIp;

    public static void main(String[] args) {
        myIp = getRealIp();
        Scanner sc = new Scanner(System.in);

        System.out.println("JMessenger CLI");
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
                    System.out.println("Available commands:");
                    System.out.println("ip <address>   - Set recipient IP");
                    System.out.println("port <number>  - Change port and restart server");
                    System.out.println("info           - Show connection info");
                    System.out.println("exit           - Exit program");
                    break;

                case "ip":
                    if (argsStr.isEmpty()) {
                        System.out.println("Usage: ip <address>");
                    } else {
                        destIp = argsStr;
                        System.out.println("Recipient set to " + destIp);
                    }
                    break;

                case "port":
                    if (argsStr.isEmpty()) {
                        System.out.println("Current port: " + currentPort);
                    } else {
                        try {
                            restartServer(Integer.parseInt(argsStr));
                        } catch (Exception e) {
                            System.out.println("Invalid port.");
                        }
                    }
                    break;

                case "info":
                    System.out.println("Local: " + myIp + ":" + currentPort);
                    System.out.println("Recipient: " + (destIp != null ? destIp : "not set"));
                    break;

                case "exit":
                    if (serverThread != null) serverThread.stopServer();
                    return;

                default:
                    if (destIp == null) {
                        System.out.println("Set recipient IP first using 'ip'.");
                    } else {
                        sendMessage(destIp, currentPort, input);
                    }
                    break;
            }
        }
    }

    private static void startServer() {
        serverThread = new ServerThread(currentPort);
        new Thread(serverThread).start();
    }

    private static void restartServer(int newPort) {
        serverThread.stopServer();
        currentPort = newPort;
        startServer();
        System.out.println("Server restarted on port " + currentPort);
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
                if (running) System.out.println("Server error: " + e.getMessage());
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

    private static void sendMessage(String ip, int port, String msg) {
        try (Socket socket = new Socket(ip, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(msg);
            System.out.println("You > " + msg);
        } catch (IOException e) {
            System.out.println("Send error: unreachable host.");
        }
    }
}