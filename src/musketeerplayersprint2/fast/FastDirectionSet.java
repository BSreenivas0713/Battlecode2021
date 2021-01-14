package musketeerplayersprint2.fast;

import battlecode.common.*;

public class FastDirectionSet {
    public int set;

    public FastDirectionSet() {
        set = 0;
    }

    public void add(Direction dir) {
        set |= 1 << dir.ordinal();
    }

    public void remove(Direction dir) {
        set &= ~(1 << dir.ordinal());
    }
}
