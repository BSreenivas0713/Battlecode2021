package musketeerplayerpostqual.fast;

import battlecode.common.*;

public class FastIntIntMap {
    public StringBuilder keyVals;
    public int maxlen;
    public int size;
    private int earliestRemoved;

    public FastIntIntMap() {
        this(100);
    }

    public FastIntIntMap(int len) {
        keyVals = new StringBuilder();
        maxlen = len;
    }

    private String keyToStr(int key) {
        return "^" + (char)(key + 0x100);
    }

    private String keyValToStr(int key, int val) {
        return "^" + (char)(key + 0x100) + (char)(val + 0x100);
    }

    public void add(int key, int val) {
        String keyTemp = keyToStr(key);
        if (keyVals.indexOf(keyTemp) == -1) {
            String keyVal = keyValToStr(key, val);
            keyVals.append(keyVal);
            size++;
        }
    }

    public void remove(int key) {
        String keyTemp = keyToStr(key);
        int index;
        if ((index = keyVals.indexOf(keyTemp)) >= 0) {
            keyVals.delete(index, index + 3);
            size--;
        }
    }

    public boolean contains(int key) {
        return keyVals.indexOf(keyToStr(key)) >= 0;
    }

    public void clear() {
        size = 0;
        keyVals = new StringBuilder();
        earliestRemoved = 0;
    }
    
    public int getVal(int key) {
        String keyTemp = keyToStr(key);
        int idx = keyVals.indexOf(keyTemp);
        if (idx != -1) {
            return (int)keyVals.charAt(idx + 2) - 0x100;
        }

        // Could not find element
        return -1;
    }

    public int[] getKeys() {
        int[] keys = new int[size];
        for(int i = 1; i < keyVals.length(); i += 3) {
            keys[i/3] = (int)keyVals.charAt(i) - 0x100;
        }
        return keys;
    }
}