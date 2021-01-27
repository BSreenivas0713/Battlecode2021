package musketeerplayerpostqual;

import battlecode.common.*;

import musketeerplayerpostqual.Util.*;
import musketeerplayerpostqual.Debug.*;

public class RushPolitician extends Robot {
    static MapLocation enemyLocation;
    static Direction main_direction;
    static int moveSemaphore;
    
    public RushPolitician(RobotController r, MapLocation enemyLoc) {
        super(r);
        enemyLocation = enemyLoc;
        if (enemyLocation != null) {
            Nav.setDest(enemyLocation);
        }
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
        if (enemyLocation != null) {
            Debug.println(Debug.info, "target map location: x:" + enemyLocation.x + ", y:" + enemyLocation.y);
        } else {
            Debug.println(Debug.info, "I have no target. I will explore around...");
        }
        Debug.println(Debug.info, "Semaphore: " + moveSemaphore);

        MapLocation currLoc = rc.getLocation();
        RobotInfo[] neutrals = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        RobotInfo robot;
        int minEnemyDistSquared = Integer.MAX_VALUE;
        MapLocation closestEnemy = null;
        if (enemyLocation == null) {
            for (int i = enemySensable.length - 1; i >= 0; i--) {
                robot = enemySensable[i];
                MapLocation loc = robot.getLocation();
                if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && robot.getInfluence() <= 600) {
                    enemyLocation = loc;
                    Nav.setDest(loc);
                }
            }
            if(rc.canGetFlag(homeID)) {
                Debug.println(Debug.info, "Checking home flag");
                int flag = rc.getFlag(homeID);
                Comms.InformationCategory IC = Comms.getIC(flag);
                switch(IC) {
                    case ENEMY_EC:
                        int[] dxdy = Comms.getDxDy(flag);
                        if(dxdy[0] != 0 && dxdy[1] != 0) {
                            MapLocation enemyLoc = new MapLocation(dxdy[0] + home.x - Util.dOffset, dxdy[1] + home.y - Util.dOffset);
                            Debug.setIndicatorDot(Debug.info, enemyLoc, 255, 0, 0);

                            Comms.GroupRushType GRtype = Comms.getRushType(flag);
                            int GRmod = Comms.getRushMod(flag);
                            Debug.println(Debug.info, "EC is sending a rush: Read ENEMY_EC flag. Type: " + GRtype + ", mod: " + GRmod);

                            if((GRtype == Comms.GroupRushType.MUC || GRtype == Comms.GroupRushType.MUC_POL)) {
                                Debug.println(Debug.info, "Joining the rush");
                                enemyLocation = enemyLoc;
                                Nav.setDest(enemyLocation);
                            } else {
                                Debug.println(Debug.info, "I was not included in this rush");
                            }
                        }
                        break;
                }
            }
        } else {
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
                    int dist = currLoc.distanceSquaredTo(loc);
                    if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && enemyLocation.isWithinDistanceSquared(loc, 8)) {
                        if (robot.getConviction() > 50) {
                            enemyLocation = null;
                        } else if (dist < minEnemyDistSquared) {
                            minEnemyDistSquared = dist;
                            closestEnemy = loc;
                        }
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

        if(enemyLocation != null && currLoc.isWithinDistanceSquared(enemyLocation, actionRadius)) {
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
        } else if (enemyLocation != null) {
            Debug.println(Debug.info, "Using gradient descent for movement");
            main_direction = Nav.gradientDescent();
            tryMoveDest(main_direction);
        } else {
            Direction[] orderedDirs = Nav.exploreGreedy(rc);
            boolean moved = false;
            if(orderedDirs != null) {
                for(Direction dir : orderedDirs) {
                    moved = moved || tryMove(dir);
                }
                tryMoveDest(Nav.lastExploreDir);
            }
        }

        Debug.setIndicatorLine(Debug.info, rc.getLocation(), enemyLocation, 255, 150, 50);
    }
}