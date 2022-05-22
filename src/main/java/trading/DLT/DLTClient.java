package trading.DLT;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class DLTClient {
    public static void main(String[] args) {
        final String HOST = "127.0.0.1";
        final int PORT = 10000;
        final long interval = 100;

        System.out.println("Client started.");

        try {
            Socket socket = new Socket(HOST, PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner in = new Scanner(socket.getInputStream());

            new Timer().scheduleAtFixedRate(new TimerTask(){
                @Override
                public void run(){
                    String txn = UUID.randomUUID().toString();
                    out.println(txn);
                    System.out.println("Submits Txn : " + txn);

                    if (in.hasNextLine())
                        System.out.println("Received Server message : " + in.nextLine());
                }
            },0, interval);
        }
        catch (IOException e) {
            System.err.println("Error : " + e.getMessage());
        }
    }
}
