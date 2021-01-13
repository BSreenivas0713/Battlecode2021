package musketeerplayersprint2;

import battlecode.common.*;

public class Debug {
    static final boolean verbose = true;
    public static final boolean info = true;
    private static final boolean indicators = true;

    private static RobotController rc;

    static void init(RobotController r) {
        rc = r;
    }

    static void println(boolean cond, String s) {
        if(verbose && cond) {
            System.out.println(s);
        }
    }
    
    static void print(boolean cond, String s) {
        if(verbose && cond) {
            System.out.print(s);
        }
    }

    static void setIndicatorDot(MapLocation loc, int r, int g, int b) {
        if(verbose && indicators) {
            rc.setIndicatorDot(loc, r, g, b);
        }
    }

    static void setIndicatorLine(MapLocation startLoc, MapLocation endLoc, int r, int g, int b) {
        if(verbose && indicators) {
            rc.setIndicatorLine(startLoc, endLoc, r, g, b);
        }
    }
}