package musketeerplayersprint;

import battlecode.common.*;

public class FastIterableLocSet {
  private int size = 0;
  private StringBuilder keys;

  FastIterableLocSet() {
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
      if ((index = keys.indexOf(key)) != -1) {
          keys.delete(index, index+3);
          size--;
      }
  }

  public boolean contains(MapLocation loc) {
      return keys.indexOf(locToStr(loc)) != -1;
  }

  public void clear() {
      keys = new StringBuilder();
      size = 0;
  }

  public MapLocation[] getKeys() {
      MapLocation[] locs = new MapLocation[size];
      for (int i = 0; i < size; i++) {
          locs[i] = new MapLocation(keys.charAt(i*3+1), keys.charAt(i*3+2));
      }
      return locs;
  }

  public void replace(String newSet) {
      keys.replace(0, keys.length(), newSet);
      size = newSet.length() / 3;
  }
}