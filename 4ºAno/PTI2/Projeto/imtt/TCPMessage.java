package imtt;

public class TCPMessage {
    private String message;
    private byte[] bytes;

    public TCPMessage() {
        message = "";
    }

    public synchronized void setMessage(String message) {
        this.message = message;
        notifyAll();
    }

    public synchronized void setBytes(byte[] bytes) {
        this.bytes = bytes;
        notifyAll();
    }

    public synchronized String getMessage() throws InterruptedException {
        wait();
        return this.message;
    }

    public synchronized byte[] getBytes() throws InterruptedException {
        wait();
        return this.bytes;
    }
}
