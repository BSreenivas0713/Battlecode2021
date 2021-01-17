package musketeerplayersprint2;

import battlecode.common.*;

import musketeerplayersprint2.Util.*;
import musketeerplayersprint2.Debug.*;

public class LatticeRusher extends Robot {
    static MapLocation enemyLocation;
    static Direction main_direction;
    static int moveSemaphore;
    
    public LatticeRusher(RobotController r, MapLocation enemyLoc) {
        super(r);
        enemyLocation = enemyLoc;
        moveSemaphore = 2;
        subRobotType = Comms.SubRobotType.POL_RUSH;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
        Nav.setDest(enemyLoc);
    }
    
    public LatticeRusher(RobotController r, MapLocation enemyLoc, MapLocation h) {
        this(r, enemyLoc);
        home = h;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Debug.println(Debug.info, "I am a Lattice Rusher; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        Debug.println(Debug.info, "target map location: x:" + enemyLocation.x + ", y:" + enemyLocation.y);
        Debug.println(Debug.info, "Semaphore: " + moveSemaphore);
        
        RobotInfo robot;
        MapLocation currLoc = rc.getLocation();
        RobotInfo[] neutrals = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        int minEnemyDistSquared = Integer.MAX_VALUE;
        MapLocation closestEnemy = null;
        boolean baseConverted = false;
        for(int i = enemyAttackable.length - 1; i >= 0; i--) {
            robot = enemyAttackable[i];
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
                    if(closestEnemy == null) {
                        Debug.println(Debug.info, "Base seems to be connverted to our side, baseConverted set to true");
                        baseConverted = true;
                    }
                }
            }
        }
        
        if (rc.canEmpower(minEnemyDistSquared) && (moveSemaphore <= 0 || minEnemyDistSquared <= 1)) {
            int radius = Math.min(actionRadius, minEnemyDistSquared);
            Debug.println(Debug.info, "Empowered with radius: " + radius);
            Debug.setIndicatorLine(Debug.info, rc.getLocation(), closestEnemy, 255, 150, 50);
            rc.empower(radius);
            return;
        }

        if(baseConverted) {
            return;
        }

        if(currLoc.isWithinDistanceSquared(enemyLocation, actionRadius)) {
            Debug.println(Debug.info, "Close to EC; using heuristic for movement");
            main_direction = rc.getLocation().directionTo(enemyLocation);
            if(tryMove(main_direction)) {
                moveSemaphore = 2;
            } else {
                moveSemaphore--;
            }
            tryMove(main_direction.rotateRight());
            tryMove(main_direction.rotateLeft());
        } else {
            Debug.println(Debug.info, "Using gradient descent for movement");
            main_direction = Nav.gradientDescent();
            tryMoveDest(main_direction);
        }

        Debug.setIndicatorLine(Debug.info, rc.getLocation(), enemyLocation, 255, 150, 50);
    }
}