package musketeerplayerqual;

import battlecode.common.*;

public class Debug {
    static final boolean verbose = false;
    public static final boolean info = true;
    public static final boolean pathfinding = true;
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

    static void setIndicatorDot(boolean cond, MapLocation loc, int r, int g, int b) {
        if(verbose && indicators && cond && loc != null) {
            rc.setIndicatorDot(loc, r, g, b);
        }
    }

    static void setIndicatorLine(boolean cond, MapLocation startLoc, MapLocation endLoc, int r, int g, int b) {
        if(verbose && indicators && cond && startLoc != null && endLoc != null) {
            rc.setIndicatorLine(startLoc, endLoc, r, g, b);
        }
    }
}