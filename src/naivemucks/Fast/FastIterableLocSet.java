package naivemucks.fast;

import battlecode.common.*;

public class FastIterableLocSet {
    public StringBuilder keys;
    public int maxlen;
    public int[] ints;
    public int size;
    private int earliestRemoved;

    public FastIterableLocSet() {
        keys = new StringBuilder();
    }

    private String locToStr(MapLocation loc) {
        return "^" + (char)(loc.x) + (char)(loc.y);
    }

    public void add(MapLocation loc) {
        String key = locToStr(loc);
        if (keys.indexOf(key) == -1) {
            keys.append(key);
            size++;
        }
    }

    public void remove(MapLocation loc) {
        String key = locToStr(loc);
        int index;
        if ((index = keys.indexOf(key)) >= 0) {
            keys.deleteCharAt(index);
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
        earliestRemoved = size;
    }

    public void updateIterable() {
        for (int i = earliestRemoved; i < size; i++) {
            ints[i] = keys.charAt(i);
        }
        earliestRemoved = size;
    }

    public void replace(String newSet) {
        keys.replace(0, keys.length(), newSet);
        size = newSet.length() / 3;
    }
}