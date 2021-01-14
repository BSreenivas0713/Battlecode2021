package musketeerplayersprint2;
import battlecode.common.*;

import musketeerplayersprint2.Util.*;
import musketeerplayersprint2.Comms.*;
import musketeerplayersprint2.Debug.*;

public class Robot {
    static RobotController rc;
    static int turnCount;

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

    static Comms.SubRobotType subRobotType;

    public static Robot changeTo = null;

    public Robot(RobotController r) {
        rc = r;
        turnCount = 0;
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

    boolean broadcastClosestEnemy() throws GameActionException {
        int dx = 0;
        int dy = 0;

        MapLocation currLoc = rc.getLocation();
        MapLocation closestEnemyLoc = null;
        double minDistSquared = Integer.MAX_VALUE;
        for (RobotInfo robot : enemySensable) {
            double temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp < minDistSquared) {
                minDistSquared = temp;
                closestEnemyLoc = robot.getLocation();
            }
        }

        // Did not find its own sensable enemy, propagate other flags
        if(closestEnemyLoc == null) {
            int minEnemyDist = Integer.MAX_VALUE;
            for(RobotInfo robot : friendlySensable) {
                if(rc.canGetFlag(robot.getID())) {
                    int flag = rc.getFlag(robot.getID());
                    switch(Comms.getIC(flag)) {
                        case ROBOT_TYPE_AND_CLOSEST_ENEMY:
                            MapLocation robotLoc = robot.getLocation();
                            int[] enemyDxDyFromRobot = Comms.getDxDy(flag);

                            // Both dOffset means no enemy found.
                            if(enemyDxDyFromRobot[0] != Util.dOffset || enemyDxDyFromRobot[1] != Util.dOffset) {
                                int enemyDx = enemyDxDyFromRobot[0] + robotLoc.x - Util.dOffset;
                                int enemyDy = enemyDxDyFromRobot[1] + robotLoc.y - Util.dOffset;
    
                                int dist = enemyDx * enemyDx + enemyDy + enemyDy;
                                if(dist < minEnemyDist) {
                                    dx = enemyDx;
                                    dy = enemyDy;
                                    minEnemyDist = dist;
                                }
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        } else {
            dx = closestEnemyLoc.x - currLoc.x;
            dy = closestEnemyLoc.y - currLoc.y;
        }

        nextFlag = Comms.getFlag(InformationCategory.ROBOT_TYPE_AND_CLOSEST_ENEMY, 
                                    subRobotType, dx + Util.dOffset, dy + Util.dOffset);
        if(dx == 0 && dy == 0) {
            Debug.println(Debug.info, "No closest enemy found");
            return false;
        } else {
            Debug.println(Debug.info, "Broadcasting closest enemy found at: dX: " + dx + ", dY: " + dy);
            Debug.setIndicatorDot(Debug.info, currLoc.translate(dx, dy), 0, 0, 0);
            return true;
        }
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