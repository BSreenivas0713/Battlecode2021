package auxMusketeerPlayer;
import battlecode.common.*;

import auxMusketeerPlayer.Util.*;

public class Robot {
    static RobotController rc;
    static int turnCount = 0;

    public Robot(RobotController r) {
        rc = r;
    }

    public void takeTurn() throws GameActionException {
        turnCount += 1;
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        //System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }
}