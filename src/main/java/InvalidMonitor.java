import mpi.Intracomm;
import mpi.MPI;
import mpi.Status;
import sun.awt.Mutex;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Map;

public class InvalidMonitor {
    boolean allowedToEnterCS;
    int processNumber;
    int worldSize;
    final Intracomm COMM;
    boolean waitingForToken = false;
    boolean isInCriticalSection = false;
    boolean waitForToken = false;
    int waitAt;
    boolean conditionalNotified;
    Map<Integer, ArrayList<Integer>> awaitings;

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

    private void initTest() {
        sendEnterCriticalSectionRequest();
    }

    private void initTokenInFirstProcess() {
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
                //check what status has message
                Status status = COMM.Probe(MPI.ANY_SOURCE, MPI.ANY_TAG);

                //another process requesting token
                if (status.tag == MessageType.REQUEST_TOKEN.getValue()) {
                    COMM.Recv(recv_buf, 0, recv_buf.length, MPI.INT, status.source, MessageType.REQUEST_TOKEN.getValue());
                    Utils.printReceivedData(recv_buf);

                }
                //send whole token
                else if (status.tag == MessageType.TOKEN_QUEUE_SIZE.getValue()) {
                    COMM.Recv(token.queueSize, 0, recv_buf.length, MPI.INT, status.source, MessageType.TOKEN_QUEUE_SIZE.getValue());
                    COMM.Recv(token.LN, 0, recv_buf.length, MPI.INT, status.source, MessageType.TOKEN_LN.getValue());
                    COMM.Recv(token.queue, 0, recv_buf.length, MPI.INT, status.source, MessageType.TOKEN_QUEUE.getValue());

                    if (waitingForToken) {
                        allowedToEnterCS = true;
                        //conditionalVariable.notify_one();
                        //TODO notify conditional variable
                    }
                } else if (true) {

                }
            }
        });
        receiveThread.start();
    }

    public void lock() {
        //std::unique_lock<std::mutex> lock(mutex);
        enterCriticalSection(new Mutex());
    }

    public void unlock() {
        //std::unique_lock<std::mutex> lock(mutex);
        releaseCriticalSection();
    }

    public void wait(int id) {
        //std::unique_lock<std::mutex> lock(mutex);
        broadcastWait(id);
        releaseCriticalSection();
        waitAt = id;
        conditionalNotified = false;
        /*conditionalVariable.wait(lock, [this]() -> bool {
            return conditionalNotified;
        });*/
        enterCriticalSection(new Mutex());
    }

    public void notifyAll(int id) {
        //std::unique_lock<std::mutex> lock(mutex);
        if (awaitings.containsKey(id)) {
            ArrayList<Integer> awaitingsArray = awaitings.get(id);
            for (int i = 0; i < awaitingsArray.size(); i++) {
                COMM.Send(id, 0, 1, MPI.INT, awaitingsArray.get(awaitingsArray.size() - 1), MessageType.NOTIFY.getValue());
                awaitingsArray.remove(awaitingsArray.size() - 1);
            }
            awaitings.remove(id);
        }
        sendClearAwaitings(id);
    }

    private void sendClearAwaitings(int id) {
        for (int i = 0; i < worldSize; i++) {
            if (i != processNumber) {
                COMM.Send(id, 0, 1, MPI.INT, i, MessageType.CLEAR_AWAITINGS.getValue());
            }
        }
    }

    private void enterCriticalSection(Mutex lock) {
        waitForToken = true;
        sendEnterCriticalSectionRequest();

        if (token == null) {
            /*conditionalVariable.wait(lock, [this]() -> bool {
                return allowedToEnterCS;
            });*/
        }

        isInCriticalSection = true;
        waitForToken = false;
    }

    //when process want to enter CS and don't have token, incrementing his RN[own_rank] array
    //and sends a request message containing new sequence number to all processes in the system
    private void sendEnterCriticalSectionRequest() {
        RN[processNumber] += 1;
        for (int i = 0; i < worldSize; i++) {
            if (i != processNumber) {
                int[] tempSending = {RN[processNumber]};
                COMM.Send(tempSending, 0, tempSending.length, MPI.INT, i, MessageType.REQUEST_TOKEN.getValue());
            }
        }
    }

    private void releaseCriticalSection() {
        //when process i, leaves the CS
        //set LN[i] of token equal to RN[i]. what's indicates that its request RN[i] has been executed
        //for every process k, not in the token queue Q, appends k to Q if RN[k] == LN[k]. What's indicates that process k has an outstanding request
        //if the token queue Q is nonempty after this update, it pops a process ID j from Q and sends the token to j
        //otherwise it keeps the token
        token.LN[processNumber] = RN[processNumber];
        for (int k = 0; k < worldSize; k++) {
            if (RN[k] == token.LN[k] + 1) {
                boolean isInQueue = false;
                for (int j = 0; j < token.queueSize[0]; j++) {
                    if (token.queue[j] == k) {
                        isInQueue = true;
                    }
                }
                if (!isInQueue) {
                    int newSize = token.queueSize[0] + 1;
                    int[] newQueue = new int[newSize];
                    newQueue = token.queue;
                    newQueue[newSize - 1] = k;
                    token.queue = newQueue;
                    token.queueSize[0] = newSize;
                }
            }
        }
        if (token.queueSize[0] > 0) {
            int newSize = token.queueSize[0] - 1;
            int newQueue[] = new int[newSize];

            int firstInQueue = token.queue[0];
            if (newSize > 0) {
                newQueue = token.queue;
            }
            token.queue = null;
            token.queue = newQueue;
            token.queueSize[0] = newSize;
            sendToken(firstInQueue);
        }
        isInCriticalSection = false;
    }

    //send wait message to every process except this
    private void broadcastWait(int id) {
        for (int i = 0; i < worldSize; i++) {
            if (i != processNumber) {
                COMM.Send(id, 0, 1, MPI.INT, i, MessageType.WAIT.getValue());
            }
        }
    }

    //send all token parts to another process
    private void sendToken(int destProcessNumber) {
        COMM.Send(token.queueSize, 0, 1, MPI.INT, destProcessNumber, MessageType.TOKEN_QUEUE_SIZE.getValue());
        COMM.Send(token.LN, 0, worldSize, MPI.INT, destProcessNumber, MessageType.TOKEN_LN.getValue());
        COMM.Send(token.queue, 0, token.queueSize[0], MPI.INT, destProcessNumber, MessageType.TOKEN_QUEUE.getValue());
        allowedToEnterCS = false;
        token = null;
    }
}
