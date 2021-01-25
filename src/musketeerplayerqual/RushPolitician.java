package musketeerplayerqual;

import battlecode.common.*;

import musketeerplayerqual.Util.*;
import musketeerplayerqual.Debug.*;

public class RushPolitician extends Robot {
    static MapLocation enemyLocation;
    static Direction main_direction;
    static int moveSemaphore;
    
    public RushPolitician(RobotController r, MapLocation enemyLoc) {
        super(r);
        /*if (enemyLoc.isAdjacentTo(r.getLocation())) { // Comment out the if case to render my changes irrelevant
            enemyLocation = null;
        } else {*/
            enemyLocation = enemyLoc;
            Nav.setDest(enemyLoc);
        //}
        moveSemaphore = 2;
        subRobotType = Comms.SubRobotType.POL_RUSH;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
    }
    
    public RushPolitician(RobotController r, MapLocation enemyLoc, MapLocation h, int hID) {
        this(r, enemyLoc);
        home = h;
        homeID = hID;
        friendlyECs.add(home, homeID);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Debug.println(Debug.info, "I am a rush politician; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        //if (enemyLocation != null) {
            Debug.println(Debug.info, "target map location: x:" + enemyLocation.x + ", y:" + enemyLocation.y);
        /*} else {
            Debug.println(Debug.info, "I have no target. I will explore around...");
        }*/
        Debug.println(Debug.info, "Semaphore: " + moveSemaphore);

        MapLocation currLoc = rc.getLocation();
        RobotInfo[] neutrals = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        RobotInfo robot;
        int minEnemyDistSquared = Integer.MAX_VALUE;
        MapLocation closestEnemy = null;
        /*if (enemyLocation == null) {
            for (int i = enemySensable.length - 1; i >= 0; i--) {
                robot = enemySensable[i];
                MapLocation loc = robot.getLocation();
                if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && robot.getInfluence() <= 600) {
                    enemyLocation = loc;
                    Nav.setDest(loc);
                }
            }
        } else {*/
            for(int i = enemyAttackable.length - 1; i >= 0; i--) {
                robot = enemyAttackable[i];
                MapLocation loc = robot.getLocation();
                if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER && enemyLocation.isWithinDistanceSquared(loc, 8)) {
                    int dist = currLoc.distanceSquaredTo(loc);
                    if(dist < minEnemyDistSquared) {
                        minEnemyDistSquared = dist;
                        closestEnemy = loc;
                    }
                }
            }
            for(int i = neutrals.length - 1; i >= 0; i--) {
                robot = neutrals[i];
                MapLocation loc = robot.getLocation();
                if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER && 
                    enemyLocation.isWithinDistanceSquared(loc, 8)) {
                    int dist = currLoc.distanceSquaredTo(loc);
                    if(dist < minEnemyDistSquared) {
                        minEnemyDistSquared = dist;
                        closestEnemy = loc;
                    }
                }
            }

            if (minEnemyDistSquared == Integer.MAX_VALUE) {
                for(int i = friendlySensable.length - 1; i >= 0; i--) {
                    robot = friendlySensable[i];
                    MapLocation loc = robot.getLocation();
                    if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && enemyLocation.isWithinDistanceSquared(loc, 8)) {
                        int dist = currLoc.distanceSquaredTo(loc);
                        if(dist < minEnemyDistSquared) {
                            minEnemyDistSquared = dist;
                            closestEnemy = loc;
                        }
                    }
                }
            }
        //}
        
        if (rc.canEmpower(minEnemyDistSquared) && (moveSemaphore <= 0 || minEnemyDistSquared <= 1)) {
            int radius = Math.min(actionRadius, minEnemyDistSquared);
            Debug.println(Debug.info, "Empowered with radius: " + radius);
            Debug.setIndicatorLine(Debug.info, rc.getLocation(), closestEnemy, 255, 150, 50);
            rc.empower(radius);
            return;
        }

        if(/*enemyLocation != null && */currLoc.isWithinDistanceSquared(enemyLocation, actionRadius)) {
            Debug.println(Debug.info, "Close to EC; using heuristic for movement");
            main_direction = rc.getLocation().directionTo(enemyLocation);
            if(rc.isReady()) {
                boolean moved = tryMove(main_direction) || tryMove(main_direction.rotateRight()) || tryMove(main_direction.rotateLeft());
                if(moved) {
                    moveSemaphore = 2;
                } else {
                    moveSemaphore--;
                }
            }
        } else /*if (enemyLocation != null) */{
            Debug.println(Debug.info, "Using gradient descent for movement");
            main_direction = Nav.gradientDescent();
            tryMoveDest(main_direction);
        /*} else {
            main_direction = Nav.explore();
            if (main_direction != null) {
                tryMoveDest(main_direction);
            }*/
        }

        Debug.setIndicatorLine(Debug.info, rc.getLocation(), enemyLocation, 255, 150, 50);
    }
}