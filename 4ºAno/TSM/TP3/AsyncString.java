public class AsyncString {
    private byte[] bytes;

    public AsyncString() {
    }

    public synchronized void setBytes(byte[] bytes) {
        this.bytes = bytes;
        notifyAll();
    }

    public synchronized byte[] getBytes() throws InterruptedException {
        wait();
        return this.bytes;
    }
}
