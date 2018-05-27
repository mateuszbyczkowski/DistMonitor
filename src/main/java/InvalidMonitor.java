import mpi.Intracomm;
import mpi.MPI;
import mpi.Status;

public class InvalidMonitor {
    boolean allowedToEnterCS;
    int processNumber;
    int worldSize;
    final Intracomm COMM;
    boolean waitingForToken = false;

    int[] RN;
    int[] recv_buf = {0};
    Token token;

    public InvalidMonitor(String[] args) {
        MPI.Init(args);
        this.COMM = MPI.COMM_WORLD;
        this.processNumber = COMM.Rank();
        this.worldSize = COMM.Size();
        this.RN = new int[worldSize];

        for (int i = 0; i < worldSize; i++) {
            RN[i] = 0;
        }

        initTokenInFirstProcess();
        runReceivingThread();
        this.initTest();
    }

    public void initTest() {
        sendEnterCriticalSectionRequest();
    }

    void initTokenInFirstProcess() {
        if (processNumber == 0) {
            token = new Token();
            token.queueSize[0] = 0;
            token.queue = new int[0];
            token.LN = new int[worldSize];
            allowedToEnterCS = true;
        }
    }

    private void runReceivingThread() {
        //receiving messages from MPI in separate thread
        Thread receiveThread = new Thread(() -> {
            while (true) {
                Status status = COMM.Probe(MPI.ANY_SOURCE, MPI.ANY_TAG);

                //another process requesting token
                if (status.tag == MessageType.REQUEST_TOKEN.getValue()) {
                    COMM.Recv(recv_buf, 0, recv_buf.length, MPI.INT, status.source, MessageType.REQUEST_TOKEN.getValue());
                    Utils.printReceivedData(recv_buf);

                } else if (status.tag == MessageType.TOKEN_QUEUE_SIZE.getValue()) {
                    COMM.Recv(token.queueSize, 0, recv_buf.length, MPI.INT, status.source, MessageType.TOKEN_QUEUE_SIZE.getValue());
                    COMM.Recv(token.LN, 0, recv_buf.length, MPI.INT, status.source, MessageType.TOKEN_LN.getValue());
                    COMM.Recv(token.queue, 0, recv_buf.length, MPI.INT, status.source, MessageType.TOKEN_QUEUE.getValue());

                    if (waitingForToken) {
                        allowedToEnterCS = true;
                        //TODO notify conditional variable
                    }
                } else if (true) {

                }
            }
        });
        receiveThread.start();
    }

    //when process want to enter CS and don't have token, incrementing his RN[own_rank] array
    //and sends a request message containing new sequence number to all processes in the system
    public void sendEnterCriticalSectionRequest() {
        RN[processNumber] += 1;
        for (int i = 0; i < worldSize; i++) {
            if (i != processNumber) {
                int[] tempSending = {RN[processNumber]};
                COMM.Send(tempSending, 0, tempSending.length, MPI.INT, i, MessageType.REQUEST_TOKEN.getValue());
            }
        }
    }

    public void releaseCriticalSection() {

    }

    public void sendToken(int destProcessNumber) {
        COMM.Send(token.queueSize, 0, 1, MPI.INT, destProcessNumber, MessageType.TOKEN_QUEUE_SIZE.getValue());
        COMM.Send(token.LN, 0, worldSize, MPI.INT, destProcessNumber, MessageType.TOKEN_LN.getValue());
        COMM.Send(token.queue, 0, token.queueSize[0], MPI.INT, destProcessNumber, MessageType.TOKEN_QUEUE.getValue());
        allowedToEnterCS = false;
        token = null;
    }


}
