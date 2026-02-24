import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        try {
            Socket sock = new Socket("127.0.0.1", 6013);

            BufferedReader bin    = new BufferedReader(
                    new InputStreamReader(sock.getInputStream()));
            PrintWriter    pout   = new PrintWriter(sock.getOutputStream(), true);
            Scanner        scanner = new Scanner(System.in);

            // --- Name registration loop ---
            // Keep responding to server prompts until we get a "Welcome" message
            String serverMsg;
            while ((serverMsg = bin.readLine()) != null) {
                System.out.println("Server: " + serverMsg);

                if (serverMsg.startsWith("Welcome")) {
                    break;  // registration complete
                }

                // Respond to "Enter your name:" or "Name already taken." etc.
                System.out.print("You: ");
                pout.println(scanner.nextLine());
            }

            // --- Message loop ---
            System.out.println("Type messages (type 'exit' to quit):");
            while (true) {
                System.out.print("You: ");
                String msg = scanner.nextLine();

                if (msg.equalsIgnoreCase("exit")) break;

                pout.println(msg);
                System.out.println("Server: " + bin.readLine());
            }

            sock.close();
            System.out.println("Disconnected from server.");

        } catch (IOException ioe) {
            System.err.println("Connection error: " + ioe.getMessage());
        }
    }
}