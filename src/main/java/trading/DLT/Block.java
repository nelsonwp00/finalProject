package trading.DLT;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class Block {
    byte[] hash; // SHA-256 Hash

    byte[] prevHash; // SHA-256 Hash

    static int blockSize = 100;

    LocalDateTime timeStamp;

    ArrayList<String> transactions;
}
