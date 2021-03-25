package imtt;

import java.util.ArrayList;

public class LimitedQueue<K> extends ArrayList<K> {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private int maxSize;

    public LimitedQueue(int maxSize) {
        this.maxSize = maxSize;
    }

    public boolean add(K e) {
        boolean r = super.add(e);

        if (size() > this.maxSize) {
            removeRange(0, size() - maxSize);
        }

        return r;
    }

    public K getYoungest() {
        return get(size() - 1);
    }
}