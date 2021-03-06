package naivemucks;
import battlecode.common.*;

import naivemucks.Util.*;
import naivemucks.Comms.*;
import naivemucks.Debug.*;

public class Robot {
    static RobotController rc;
    static int turnCount = 0;

    static int defaultFlag;
    static int nextFlag;
    static boolean resetFlagOnNewTurn = true;
    static MapLocation home;
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
        if(rc.getType() == RobotType.ENLIGHTENMENT_CENTER) {
            home = rc.getLocation();
        } else {
            RobotInfo[] sensableWithin2 = rc.senseNearbyRobots(2, rc.getTeam());
            for (RobotInfo robot : sensableWithin2) {
                if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    MapLocation ecLoc = robot.getLocation();
                    home = ecLoc;
                }
            }
        }
        if (home == null) {
            home = rc.getLocation();
        }

        nextFlag = Comms.getFlag(InformationCategory.NEW_ROBOT);
    }

    public void takeTurn() throws GameActionException {
        initializeGlobals();
        turnCount += 1;
        if(rc.getFlag(rc.getID()) != nextFlag) {
            setFlag(nextFlag);
        }
        Debug.println(Debug.info, "Flag set: " + rc.getFlag(rc.getID()));
        Debug.setIndicatorDot(Debug.info, home, 255, 255, 255);

        if(resetFlagOnNewTurn)
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
        //Debug.println(Debug.info, "I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }

    static boolean tryMoveDest(Direction target_dir) throws GameActionException {
        // Debug.println(Debug.info, "Dest direction: " + dir);
        Direction[] dirs = {target_dir, target_dir.rotateRight(), target_dir.rotateLeft(), 
            target_dir.rotateRight().rotateRight(), target_dir.rotateLeft().rotateLeft()};

        for(Direction dir : dirs) {
            if(rc.canMove(dir)) {
                rc.move(dir);  
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

        RobotInfo[] sensable = rc.senseNearbyRobots(sensorRadius);
        for (RobotInfo robot : sensable) {
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                if(robot.getTeam() == rc.getTeam()) {
                    if(!robot.getLocation().equals(home)) {
                        res = true;
    
                        MapLocation ecLoc = robot.getLocation();
        
                        int ecDX = ecLoc.x - home.x + Util.dOffset;
                        int ecDY = ecLoc.y - home.y + Util.dOffset;
    
                        int inf = (int) Math.min(31, Math.ceil(Math.log(robot.getInfluence()) / Math.log(Comms.INF_LOG_BASE)));

                        nextFlag = Comms.getFlag(InformationCategory.FRIENDLY_EC, inf, ecDX, ecDY);
                    }
                } else {
                    res = true;
    
                    MapLocation ecLoc = robot.getLocation();
    
                    int ecDX = ecLoc.x - home.x + Util.dOffset;
                    int ecDY = ecLoc.y - home.y + Util.dOffset;
    
                    int inf = (int) Math.min(31, Math.ceil(Math.log(robot.getInfluence()) / Math.log(Comms.INF_LOG_BASE)));
                    if(robot.getTeam() == enemy) {
                        nextFlag = Comms.getFlag(InformationCategory.ENEMY_EC, inf, ecDX, ecDY);
                    } else {
                        nextFlag = Comms.getFlag(InformationCategory.NEUTRAL_EC, inf, ecDX, ecDY);
                    }
                }
            }
        }
        return res;
    }

    boolean broadcastEnemyFound(MapLocation enemyLoc) {
        int enemyDx = enemyLoc.x - home.x + Util.dOffset;
        int enemyDy = enemyLoc.y - home.y + Util.dOffset;

        nextFlag = Comms.getFlag(InformationCategory.ENEMY_FOUND, enemyDx, enemyDy);
        return true;
    }

    void setFlag(int flag) throws GameActionException {
        if(rc.canSetFlag(flag)) {
            rc.setFlag(flag);
        }
    }
}