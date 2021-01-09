package stuckymucky;
import battlecode.common.*;

import stuckymucky.Util.*;
import stuckymucky.Comms.*;

public class Robot {
    static RobotController rc;
    static int turnCount = 0;

    public static int dx = Util.dOffset;
    public static int dy = Util.dOffset;
    static int defaultFlag = 0;
    static int nextFlag = 0;
    static boolean resetFlagOnNewTurn = true;

    public static Robot changeTo = null;

    public Robot(RobotController r) {
        rc = r;

        int sensorRadius = rc.getType().sensorRadiusSquared;
        RobotInfo[] sensable = rc.senseNearbyRobots(sensorRadius, rc.getTeam());
        MapLocation currLoc = rc.getLocation();
        for (RobotInfo robot : sensable) {
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                MapLocation ecLoc = robot.getLocation();
                dx += currLoc.x - ecLoc.x;
                dy += currLoc.y - ecLoc.y;
            }
        }

        nextFlag = Comms.getFlag(InformationCategory.NEW_ROBOT);
    }

    public Robot(RobotController r, int currDx, int currDy) {
        rc = r;

        dx = currDx;
        dy = currDy;
    }

    public void takeTurn() throws GameActionException {
        turnCount += 1;
        if (Util.verbose) System.out.println("Flag set: " + rc.getFlag(rc.getID()));
        if(rc.getFlag(rc.getID()) != nextFlag) {
            setFlag(nextFlag);
        }

        if(resetFlagOnNewTurn && turnCount > 2)
            nextFlag = defaultFlag;
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        //if (Util.verbose) System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.canMove(dir)) {
            rc.move(dir);
            dx += dir.getDeltaX();
            dy += dir.getDeltaY();
            return true;
        } else return false;
    }

    static boolean tryMoveDest(Direction target_dir) throws GameActionException {
        // if (Util.verbose) System.out.println("Dest direction: " + dir);
        Direction[] dirs = {target_dir, target_dir.rotateRight(), target_dir.rotateLeft(), 
            target_dir.rotateRight().rotateRight(), target_dir.rotateLeft().rotateLeft()};

        for(Direction dir : dirs) {
            if(rc.canMove(dir)) {
                rc.move(dir);
                
                dx += dir.getDeltaX();
                dy += dir.getDeltaY();
    
                return true;
            }
        }
        
        return false;
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
               robot.getType() == RobotType.ENLIGHTENMENT_CENTER && 
               robot.getConviction() <= Util.minECRushConviction) {
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

                nextFlag = flag;
            }
        }
        return res;
    }

    void setFlag(int flag) {
        try {
            if(rc.canSetFlag(flag)) {
                rc.setFlag(flag);
            }
        } catch (Exception e) {}
    }
}