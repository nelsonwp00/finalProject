package trading.DLT;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class Block {
    byte[] hash;

    byte[] prevHash;

    static int blockSize = 100;

    LocalDateTime timeStamp;

    ArrayList<String> transactions;
}
