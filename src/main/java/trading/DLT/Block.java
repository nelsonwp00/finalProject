package trading.DLT;

import java.util.ArrayList;

public class Block {
    byte[] hash;

    byte[] prevHash;

    static int blockSize = 4;

    long timeStamp;

    ArrayList<String> transactions;
}
