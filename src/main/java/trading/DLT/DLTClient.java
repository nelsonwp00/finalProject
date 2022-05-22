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
        System.out.println("Usage : provide args {IP} {Port} {Interval}\n");
        assert (args.length == 3);

        final String HOST = args[0];
        final int PORT = Integer.parseInt(args[1]);
        final long interval = Long.parseLong(args[2]);

        System.out.println("Client started. Connecting to " + HOST + ":" + PORT);

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
