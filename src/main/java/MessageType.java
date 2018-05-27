public enum MessageType {
    TOKEN_QUEUE(0),
    TOKEN_LN(1),
    TOKEN_QUEUE_SIZE(2),

    DATA(3),

    WAIT(4),
    NOTIFY(5),
    REQUEST_TOKEN(6);

    MessageType(int value) {
        this.value = value;
    }

    private int value;

    public int getValue() {
        return value;
    }
}
