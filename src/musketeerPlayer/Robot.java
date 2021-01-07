package musketeerplayer;
import battlecode.common.*;

import musketeerplayer.Util.*;
import musketeerplayer.Comms.*;

public class Robot {
    static RobotController rc;
    static int turnCount = 0;

    static final int dOffset = 64;
    static int dx = dOffset;
    static int dy = dOffset;

    public Robot(RobotController r) {
        rc = r;
    }

    public void takeTurn() throws GameActionException {
        turnCount += 1;
        System.out.println("Flag set: " + rc.getFlag(rc.getID()));
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

            switch(dir) {
                case NORTHEAST:
                    dx++;
                    dy++;
                    break;
                case NORTH:
                    dy++;
                    break;
                case NORTHWEST:
                    dx--;
                    dy++;
                    break;
                case EAST:
                    dx++;
                    break;
                case WEST:
                    dx--;
                    break;
                case SOUTHEAST:
                    dx++;
                    dy--;
                    break;
                case SOUTHWEST:
                    dx--;
                    dy--;
                    break;
                case SOUTH:
                    dy--;
                    break;
            }

            return true;
        } else return false;
    }

    /**
     * @return true if found an EC and broadcasted
     */
    boolean broadcastECLocation() {
        boolean res = false;

        int sensingRadius = rc.getType().sensorRadiusSquared;
        Team enemy = rc.getTeam().opponent();
        RobotInfo[] sensable = rc.senseNearbyRobots(sensingRadius);
        for (RobotInfo robot : sensable) {
            if(robot.getTeam() != rc.getTeam() && 
               robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                res = true;

                MapLocation currLoc = rc.getLocation();
                MapLocation ecLoc = robot.getLocation();

                int ecDX = dx + ecLoc.x - currLoc.x;
                int ecDY = dy + ecLoc.y - currLoc.y;

                int flag = 0;
                if(robot.getTeam() == enemy) {
                    flag = Comms.getFlag(InformationCategory.ENEMY_EC, ecDX, ecDY);
                } else {
                    flag = Comms.getFlag(InformationCategory.NEUTRAL_EC, ecDX, ecDY);
                }

                if(rc.canSetFlag(flag)) {
                    try {
                        rc.setFlag(flag);
                    } catch(Exception e) {}
                }
            }
        }

        return res;
    }
}