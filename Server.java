import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.time.*;
import java.time.format.*;

public class Server {

    // Stores connected client info mapped by unique name
    static ConcurrentHashMap<String, ClientInfo> clientMap = new ConcurrentHashMap<>();

    // Holds detailed information about each connected client
    static class ClientInfo {
        String name;
        String ipAddress;
        String connectedAt;

        ClientInfo(String name, String ipAddress) {
            this.name        = name;
            this.ipAddress   = ipAddress;
            this.connectedAt = LocalDateTime.now()
                                   .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        @Override
        public String toString() {
            return String.format("[Name: %s | IP: %s | Connected At: %s]",
                                 name, ipAddress, connectedAt);
        }
    }

    // Prints the list of connected clients to the server terminal
    static void printConnectedClients() {
        if (clientMap.isEmpty()) {
            System.out.println("No clients currently connected.");
        } else {
            System.out.println("Connected clients (" + clientMap.size() + "):");
            for (ClientInfo info : clientMap.values()) {
                System.out.println("  " + info);
            }
        }
    }

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(6013);
            System.out.println("Server started. Waiting for clients...");
            System.out.println("Type '!list' in this terminal to see connected clients.");

            // Background thread: reads commands typed directly in the server terminal
            Thread consoleThread = new Thread(() -> {
                Scanner scanner = new Scanner(System.in);
                while (scanner.hasNextLine()) {
                    String cmd = scanner.nextLine().trim();
                    if (cmd.equalsIgnoreCase("!list")) {
                        printConnectedClients();
                    }
                }
            });
            consoleThread.setDaemon(true);
            consoleThread.start();

            while (true) {
                Socket client = serverSocket.accept();
                Thread t = new Thread(new ClientHandler(client));
                t.start();
            }
        } catch (IOException ioe) {
            System.err.println(ioe);
        }
    }

    static class ClientHandler implements Runnable {
        private Socket client;

        public ClientHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                BufferedReader bin  = new BufferedReader(
                        new InputStreamReader(client.getInputStream()));
                PrintWriter    pout = new PrintWriter(client.getOutputStream(), true);

                String ipAddress = client.getInetAddress().getHostAddress();
                String name = "";

                // --- Name registration loop ---
                while (true) {
                    pout.println("Enter your name:");
                    name = bin.readLine();

                    if (name == null) return;   // client disconnected early
                    name = name.trim();

                    if (name.isEmpty()) {
                        pout.println("Name cannot be empty. Please try again.");
                        continue;
                    }

                    // putIfAbsent atomically checks + inserts — no race condition
                    ClientInfo newClient = new ClientInfo(name, ipAddress);
                    ClientInfo existing  = clientMap.putIfAbsent(name, newClient);

                    if (existing != null) {
                        pout.println("Name already taken. Try another.");
                    } else {
                        pout.println("Welcome, " + name + "! You can now send messages.");
                        System.out.println(name + " has connected. " + newClient);
                        break;
                    }
                }

                // --- Message loop ---
                String message;
                while ((message = bin.readLine()) != null) {
                    System.out.println("[" + name + "]: " + message);
                    pout.println("Message received.");
                }

                // --- Cleanup on disconnect ---
                clientMap.remove(name);
                System.out.println(name + " has disconnected.");
                client.close();

            } catch (IOException ioe) {
                System.err.println(ioe);
            }
        }
    }
}
