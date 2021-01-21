package naivebuffs.fast;

// Unsafe version of a queue that 
// assumes you never try to add more items than maxlen
// For use ONLY in Nav.java gradientDescent
@SuppressWarnings("unchecked")
public class FasterQueue<T> {

	private T[] buf;
	private int l;
	private int r;
	private int ln;

	public FasterQueue() {
		this(10000);
	}

	public FasterQueue(int maxlen) {
		ln = maxlen + 5;
		buf = (T[])new Object[ln];
		l = 0;
		r = 0;
	}

	public boolean isEmpty() {
		return l == r;
	}

	public void clear() {
        l = 0;
        r = 0;
	}

	public int size() {
		return r - l;
	}

	public boolean add(T e) {
		buf[r] = e;
		r++;
		return true;
	}

	public T peek() {
		if (l == r) return null;
		return buf[l];
	}

	public T poll() {
		T v = buf[l];
		l++;
		return v;
	}
}