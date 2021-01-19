package musketeerplayerqual.fast;

// Kinda buggy. Don't use.
public class FastMinQueue<T extends Comparable<? super T>> {
    private T[] buf;
    private int l;
    private int r;
    private int ln;

    private int frontIndex;
    private boolean needRecalculate;

    @SuppressWarnings("unchecked")
    public FastMinQueue() {
        ln = 1000;
        buf = (T[])new Comparable[ln];
        l = 0;
        r = 0;
        needRecalculate = false;
    }
    
    @SuppressWarnings("unchecked")
    public FastMinQueue(int maxlen) {
        ln = maxlen + 5;
        buf = (T[])new Comparable[ln];
        l = 0;
        r = 0;
        needRecalculate = false;
    }
    
    public boolean isEmpty() {
        return l == r;
    }

    public void clear() {
        l = r;
    }

    public int size() {
        return (r - l + ln) % ln;
    }
    
    public boolean isValidIndex(int i) {
        // return ((l < r && l <= i && i < r) || (r < l && (l <= i || l < r)));
        return l != r && i != r;
    }
    
    public boolean replaceOrAdd(T e) {
        frontIndex = l;
        boolean replaced = false;

        for(int i = (l + 1) % ln; l != r && i != r; i = (i + 1) % ln) {
        // for(int i = (l + 1) ; l < r; i = i + 1) {
            if(!replaced && buf[i].equals(e)) {
                buf[i] = e;
                replaced = true;
            }
            
            if(buf[frontIndex].compareTo(buf[i]) > 0) {
                frontIndex = i;
            }
        }

        if(!replaced) {
            if ((r + 1) % ln == l) return false; // Should not happen if the queue never fills up completely
            buf[r] = e;
            if(buf[frontIndex].compareTo(buf[r]) > 0) {
                frontIndex = r;
            }
            r++;  
            r %= ln;
        }

        needRecalculate = false;
        return true;
    }

    public boolean add(T e) {
        if ((r + 1) % ln == l) return false;
        buf[r] = e;
        r++;  
        r %= ln;
        needRecalculate = true;
        
        return true;
    }

    public boolean remove(T e) {
        boolean removed = false;
        needRecalculate = false;
        frontIndex = l;
        for(int i = (l + 1) % ln; l != r && i != r; i = (i + 1) % ln) {
        // for(int i = (l + 1) ; l < r; i = i + 1) {
            if(!needRecalculate && buf[frontIndex].compareTo(buf[i]) > 0) {
                frontIndex = i;
            }

            if(!removed && buf[i].equals(e)) {
                buf[i] = buf[l];
                removed = true;
                l++;
                l %= ln;
                if(frontIndex == i) {
                    needRecalculate = true;
                }
            }
        }
        
        return removed;
    }

    public T peek() {
        if (l == r) return null;
        
        if(needRecalculate) {
            frontIndex = l;
            for(int i = (l + 1) % ln; l != r && i != r; i = (i + 1) % ln) {
            // for(int i = (l + 1) ; l < r; i = i + 1) {
                if(buf[frontIndex].compareTo(buf[i]) > 0) {
                    frontIndex = i;
                }
            }
            needRecalculate = false;
        }
        
        return buf[frontIndex];
    }

    public T poll() {
        if (l == r) return null;
        
        if(needRecalculate) {
            frontIndex = l;
            for(int i = (l + 1) % ln; l != r && i != r; i = (i + 1) % ln) {
            // for(int i = (l + 1) ; l < r; i = i + 1) {
                if(buf[frontIndex].compareTo(buf[i]) > 0) {
                    frontIndex = i;
                }
            }
        }

        T v = buf[frontIndex];
        buf[frontIndex] = buf[l];
        l++;
        l %= ln;
        needRecalculate = true;
        return v;
    }
    
    public void printAll() {
        for(int i = l; isValidIndex(i); i = (i + 1) % ln) {
            System.out.print(buf[i] + ", ");
        }
        System.out.println("");
    }

    // Test code
    /*
    public static void main(String[] args) {
        FastMinQueue<Integer> q = new FastMinQueue<Integer>(0);
        
        q.replaceOrAdd(23);
        q.replaceOrAdd(4);
        q.replaceOrAdd(15);
        System.out.println("Size: " + q.size());
        System.out.println("Top: " + q.peek());
        System.out.println("");

        q.replaceOrAdd(3);
        q.replaceOrAdd(15);
        q.printAll();
        System.out.println("Size: " + q.size());
        System.out.println("Top: " + q.peek());
        System.out.println("");
        
        q.remove(3);
        q.remove(10);
        System.out.println("Size: " + q.size());
        System.out.println("Top: " + q.peek());
        System.out.println("");
        
        q.clear();
        q.replaceOrAdd(5);
        q.replaceOrAdd(2);
        q.replaceOrAdd(10);
        q.printAll();
        System.out.println("Size: " + q.size());
        System.out.println("Poll: " + q.poll());
        System.out.println("Poll: " + q.poll());
        System.out.println("Poll: " + q.poll());
        System.out.println("Size: " + q.size());
        System.out.println("");
    }
    */
}