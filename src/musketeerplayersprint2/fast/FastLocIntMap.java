package musketeerplayersprint2.fast;

import battlecode.common.*;

public class FastLocIntMap {
    public StringBuilder keys;
    public int size;
    private int earliestRemoved;

    public FastLocIntMap() {
        keys = new StringBuilder();
    }

    private String locToStr(MapLocation loc) {
        return "^" + (char)(loc.x) + (char)(loc.y);
    }

    public void add(MapLocation loc, int val) {
        String key = locToStr(loc);
        if (keys.indexOf(key) == -1) {
            keys.append(key + (char)(val + 0x100));
            size++;
        }
    }

    public void remove(MapLocation loc) {
        String key = locToStr(loc);
        int index;
        if ((index = keys.indexOf(key)) >= 0) {
            keys.delete(index, index + 4);
            size--;
            
            if(earliestRemoved > index)
                earliestRemoved = index;
        }
    }

    public boolean contains(MapLocation loc) {
        return keys.indexOf(locToStr(loc)) >= 0;
    }

    public void clear() {
        size = 0;
        keys = new StringBuilder();
        earliestRemoved = 0;
    }

    public int getVal(MapLocation loc) {
        String key = locToStr(loc);
        int idx = keys.indexOf(key);
        if (idx != -1) {
            return (int)keys.charAt(idx + 3) - 0x100;
        }

        return -1;
    }

    public MapLocation[] getKeys() {
        MapLocation[] locs = new MapLocation[size];
        for(int i = 1; i < keys.length(); i += 4) {
            locs[i/4] = new MapLocation((int)keys.charAt(i), (int)keys.charAt(i+1));
        }
        return locs;
    }
}