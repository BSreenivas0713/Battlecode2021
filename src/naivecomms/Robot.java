package naivecomms;
import battlecode.common.*;

import naivecomms.Util.*;
import naivecomms.Comms.*;
import naivecomms.Debug.*;

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

    static MapLocation closestKnownEnemy;
    static int turnClosestKnownEnemy;
    static final int parityBroadcastEnemy = (int) (Math.random() * 2);

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
        
        findClosestEnemyGlobal();
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

    MapLocation findClosestEnemyGlobal() throws GameActionException {
        int dx = 0;
        int dy = 0;
        int turn = -1;

        MapLocation currLoc = rc.getLocation();
        MapLocation closestEnemyLoc = null;
        int minDistSquared = Integer.MAX_VALUE;
        for (RobotInfo robot : enemySensable) {
            int temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp < minDistSquared) {
                minDistSquared = temp;
                closestEnemyLoc = robot.getLocation();
                turn = rc.getRoundNum();
            }
        }

        // Did not find its own sensable enemy, propagate other flags
        if(closestEnemyLoc == null) {
            for(RobotInfo robot : rc.senseNearbyRobots(sensorRadius, rc.getTeam())) {
                if(rc.canGetFlag(robot.getID())) {
                    int flag = rc.getFlag(robot.getID());
                    switch(Comms.getIC(flag)) {
                        case CLOSEST_ENEMY:
                        case SLA_CLOSEST_ENEMY:
                            MapLocation robotLoc = robot.getLocation();
                            int[] enemyDxDyFromRobot = Comms.getSmallDxDy(flag);

                            // Both dSmallOffset means no enemy found.
                            if(enemyDxDyFromRobot[0] != Util.dSmallOffset || enemyDxDyFromRobot[1] != Util.dSmallOffset) {
                                MapLocation enemyLoc = new MapLocation(enemyDxDyFromRobot[0] + robotLoc.x - Util.dSmallOffset, 
                                                                        enemyDxDyFromRobot[1] + robotLoc.y - Util.dSmallOffset);

                                int temp = currLoc.distanceSquaredTo(enemyLoc);
                                if (temp < minDistSquared) {
                                    minDistSquared = temp;
                                    closestEnemyLoc = enemyLoc;
                                    turn = Comms.getTurnCount(flag);
                                }
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        if(closestEnemyLoc != null) {
            dx = closestEnemyLoc.x - currLoc.x;
            dy = closestEnemyLoc.y - currLoc.y;

            closestKnownEnemy = closestEnemyLoc;
            turnClosestKnownEnemy = turn;
        }
        
        // Reset the enemy location after a number of turns
        if(rc.getRoundNum() > turnClosestKnownEnemy + Util.turnsEnemyBroadcastValid) {
            closestKnownEnemy = null;
            turnClosestKnownEnemy = -1;
        }

        // Reset the enemy location if we don't see an enemy there.
        // if(closestKnownEnemy != null && rc.canSenseLocation(closestKnownEnemy)) {
        //     RobotInfo oldEnemy = rc.senseRobotAtLocation(closestKnownEnemy);
        //     if(oldEnemy == null || oldEnemy.team != enemy) {
        //         closestKnownEnemy = null;
        //         turnClosestKnownEnemy = -1;
        //     }
        // }

        // nextFlag = Comms.getFlag(InformationCategory.ROBOT_TYPE_AND_CLOSEST_ENEMY, 
        //                             subRobotType, dx + Util.dOffset, dy + Util.dOffset);
        if(closestKnownEnemy == null) {
            Debug.println(Debug.info, "No closest enemy found");
        } else {
            Debug.println(Debug.info, "Found closest enemy on turn: " + turnClosestKnownEnemy + " at: dX: " + dx + ", dY: " + dy);
            Debug.setIndicatorDot(Debug.info, closestEnemyLoc, 0, 0, 0);
        }

        return closestEnemyLoc;
    }

    boolean broadcastClosestEnemy() throws GameActionException {
        if(closestKnownEnemy == null)
            return false;
        
        MapLocation currLoc = rc.getLocation();
        int dx = closestKnownEnemy.x - currLoc.x;
        int dy = closestKnownEnemy.y - currLoc.y;

        if(Math.abs(dx) < Util.dSmallOffset && Math.abs(dy) < Util.dSmallOffset) {
            Debug.println(Debug.info, "Broadcasting closest enemy from turn: " + turnClosestKnownEnemy + " at: dX: " + dx + ", dY: " + dy);
            if(subRobotType == Comms.SubRobotType.SLANDERER) {
                nextFlag = Comms.getFlagTurn(InformationCategory.SLA_CLOSEST_ENEMY, 
                                            turnClosestKnownEnemy, dx + Util.dSmallOffset, dy + Util.dSmallOffset);
            } else {
                nextFlag = Comms.getFlagTurn(InformationCategory.CLOSEST_ENEMY, 
                                            turnClosestKnownEnemy, dx + Util.dSmallOffset, dy + Util.dSmallOffset);
            }

            return true;
        }

        return false;
    }

    boolean broadcastEnemyLocalOrGlobal() throws GameActionException {
        if(closestKnownEnemy == null)
            return false;
        
        if(rc.getRoundNum() % 2 == parityBroadcastEnemy) {
            broadcastEnemyFound(closestKnownEnemy);
        } else {
            // Broadcast locally first, and then globally if we can't fit it in the local version.
            if(!broadcastClosestEnemy()) {
                broadcastEnemyFound(closestKnownEnemy);
            }
        }
        return true;
    }

    boolean broadcastEnemyFound(MapLocation enemyLoc) {
        int enemyDx = enemyLoc.x - home.x;
        int enemyDy = enemyLoc.y - home.y;

        nextFlag = Comms.getFlag(InformationCategory.ENEMY_FOUND, enemyDx + Util.dOffset, enemyDy + Util.dOffset);
        return true;
    }

    void setFlag(int flag) throws GameActionException {
        if(rc.canSetFlag(flag)) {
            rc.setFlag(flag);
        }
    }
}