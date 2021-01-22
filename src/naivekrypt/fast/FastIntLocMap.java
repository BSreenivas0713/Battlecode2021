package naivekrypt.fast;

import battlecode.common.*;

public class FastIntLocMap {
    public StringBuilder keys;
    public int size;
    private int earliestRemoved;

    public FastIntLocMap() {
        keys = new StringBuilder();
    }

    private String intToStr(int val) {
        return "^" + (char)(val + 0x100);
    }

    private String locToStr(MapLocation loc) {
        return "" + (char)(loc.x) + (char)(loc.y);
    }

    public void add(int val, MapLocation loc) {
        String key = intToStr(val);
        if (keys.indexOf(key) == -1) {
            keys.append(key + locToStr(loc));
            size++;
        }
    }

    public void remove(int val) {
        String key = intToStr(val);
        int index;
        if ((index = keys.indexOf(key)) >= 0) {
            keys.delete(index, index + 4);
            size--;
            
            if(earliestRemoved > index)
                earliestRemoved = index;
        }
    }

    public boolean contains(int val) {
        return keys.indexOf(intToStr(val)) >= 0;
    }

    public void clear() {
        size = 0;
        keys = new StringBuilder();
        earliestRemoved = 0;
    }

    public MapLocation getLoc(int val) {
        String key = intToStr(val);
        int idx = keys.indexOf(key);
        if (idx != -1) {
            return new MapLocation((int)keys.charAt(idx + 2), (int)keys.charAt(idx + 3));
        }

        return null;
    }

    public int[] getKeys() {
        int[] locs = new int[size];
        for(int i = 1; i < keys.length(); i += 4) {
            locs[i/4] = (int)keys.charAt(i);
        }
        return locs;
    }
}