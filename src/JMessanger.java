import java.io.*;
import java.net.*;
import java.util.Scanner;

public class JMessanger {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("Welcome to JMessanger - Made by Luca Ballabio");
        System.out.println("1. START SERVER");
        System.out.println("2. START CLIENT");
        System.out.print("> ");

        int scelta = sc.nextInt();
        sc.nextLine();

        if (scelta == 1) {
            runServer();
        } else if (scelta == 2) {
            runClient();
        } else {
            System.out.println("Invalid, exiting.");
        }
    }

    private static void runServer() {
        try (ServerSocket ss = new ServerSocket(4999)) {
            System.out.println("SERVER ONLINE - Waiting for connection...");
            Socket client = ss.accept();
            String clientIP = client.getInetAddress().getHostAddress();
            String serverIP = InetAddress.getLocalHost().getHostAddress();
            System.out.println(clientIP + " connected.");

            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter out = new PrintWriter(client.getOutputStream(), true);
            Scanner sc = new Scanner(System.in);

            Thread receiver = new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        System.out.println(clientIP + " > " + msg);
                    }
                } catch (IOException e) {
                    System.out.println("Connection closed.");
                }
            });

            receiver.start();

            while (true) {
                String msg = sc.nextLine();
                out.println(msg);
                System.out.println(serverIP + " > " + msg);
            }

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void runClient() {
        try (Scanner sc = new Scanner(System.in)) {
            System.out.println("Connection type:");
            System.out.println("1. Localhost");
            System.out.println("2. Manual IP");
            System.out.print("> ");
            int tipoConnessione = sc.nextInt();
            sc.nextLine();

            String ip;
            if (tipoConnessione == 1) {
                ip = "localhost";
            } else if (tipoConnessione == 2) {
                System.out.print("Server IP: ");
                ip = sc.nextLine();
            } else {
                System.out.println("Invalid, exiting.");
                return;
            }

            try (Socket socket = new Socket(ip, 4999)) {
                String myIP = InetAddress.getLocalHost().getHostAddress();
                String serverIP = socket.getInetAddress().getHostAddress();
                System.out.println("Connected to " + serverIP);

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                Thread receiver = new Thread(() -> {
                    try {
                        String msg;
                        while ((msg = in.readLine()) != null) {
                            System.out.println(serverIP + " > " + msg);
                        }
                    } catch (IOException e) {
                        System.out.println("Connection closed.");
                    }
                });

                receiver.start();

                while (true) {
                    String msg = sc.nextLine();
                    out.println(msg);
                    System.out.println(myIP + " > " + msg);
                }

            }

        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }
}
