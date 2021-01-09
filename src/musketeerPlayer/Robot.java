package musketeerplayer;
import battlecode.common.*;

import musketeerplayer.Util.*;
import musketeerplayer.Comms.*;

public class Robot {
    static RobotController rc;
    static int turnCount = 0;

    public static int dx = Util.dOffset;
    public static int dy = Util.dOffset;
    static int defaultFlag = 0;
    static int nextFlag = 0;
    static boolean resetFlagOnNewTurn = true;
    static int sensorRadius;
    static int actionRadius;
    static Team enemy;
    static RobotInfo[] enemySensable;
    static RobotInfo[] friendlySensable;
    static RobotInfo[] neutralSensable;
    static RobotInfo[] enemyAttackable;

    public static Robot changeTo = null;

    public Robot(RobotController r) {
        rc = r;
        enemy = rc.getTeam().opponent();
        sensorRadius = rc.getType().sensorRadiusSquared;
        actionRadius = rc.getType().actionRadiusSquared;
        defaultFlag = 0;
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
        enemy = rc.getTeam().opponent();
        sensorRadius = rc.getType().sensorRadiusSquared;
        actionRadius = rc.getType().actionRadiusSquared;
        rc = r;
        defaultFlag = 0;
        dx = currDx;
        dy = currDy;
    }

    public void takeTurn() throws GameActionException {
        initializeGlobals();
        turnCount += 1;
        if(rc.getFlag(rc.getID()) != nextFlag) {
            setFlag(nextFlag);
        }
        if (Util.verbose) System.out.println("Flag set: " + Integer.toBinaryString(rc.getFlag(rc.getID())));

        if(resetFlagOnNewTurn && turnCount > 2)
            nextFlag = defaultFlag;
    }

    public void initializeGlobals() throws GameActionException {
        enemySensable = rc.senseNearbyRobots(sensorRadius, enemy);
        friendlySensable = rc.senseNearbyRobots(sensorRadius, rc.getTeam());
        neutralSensable = rc.senseNearbyRobots(sensorRadius, Team.NEUTRAL);
        enemyAttackable  = rc.senseNearbyRobots(actionRadius, enemy);
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
                int inf = (int) Math.min(31, Math.ceil(Math.log(robot.getInfluence()) / Math.log(Comms.INF_LOG_BASE)));
                if(robot.getTeam() == enemy) {
                    flag = Comms.getFlag(InformationCategory.ENEMY_EC, inf, ecDX, ecDY);
                } else {
                    flag = Comms.getFlag(InformationCategory.NEUTRAL_EC, inf, ecDX, ecDY);
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