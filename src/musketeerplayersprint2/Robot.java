package musketeerplayersprint2;
import battlecode.common.*;

import musketeerplayersprint2.Util.*;
import musketeerplayersprint2.Comms.*;
import musketeerplayersprint2.Debug.*;
import musketeerplayersprint2.fast.FastIntIntMap;

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

    static FastIntIntMap ICtoTurnMap;

    public static Robot changeTo = null;

    public Robot(RobotController r) {
        rc = r;
        turnCount = 0;
        enemy = rc.getTeam().opponent();
        sensorRadius = rc.getType().sensorRadiusSquared;
        actionRadius = rc.getType().actionRadiusSquared;
        defaultFlag = 0;
        ICtoTurnMap = new FastIntIntMap();
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

    // Returns true if a flag was propagated
    boolean propagateFlags() throws GameActionException {
        // Remove keys that we don't need
        int[] keys = ICtoTurnMap.getKeys();
        for(int key : keys) {
            if(rc.getRoundNum() > ICtoTurnMap.getVal(key) + Util.flagCooldown) {
                ICtoTurnMap.remove(key);
            }
        }

        MapLocation currLoc = rc.getLocation();
        for(RobotInfo robot : friendlySensable) {
            if(rc.canGetFlag(robot.getID())) {
                int flag = rc.getFlag(robot.getID());
                InformationCategory IC = Comms.getIC(flag);

                // Do not propagate if we have propagated recently
                if(!ICtoTurnMap.contains(IC.ordinal())) {
                    // Only propgatable flags
                    MapLocation robotLoc;
                    int []DxDyFromRobot;
                    MapLocation enemyLoc;
                    int dx;
                    int dy;
                    int newFlag;
                    switch(IC) {
                        case ENEMY_EC_ATTACK_CALL:
                            Debug.println(Debug.info, "Propagating Attack Flag");
                            robotLoc = robot.getLocation();
                            DxDyFromRobot = Comms.getDxDy(flag);
                            
                            enemyLoc = new MapLocation(DxDyFromRobot[0] + robotLoc.x - Util.dOffset, 
                                                        DxDyFromRobot[1] + robotLoc.y - Util.dOffset);
                            Debug.println(Debug.info, "Apparant enemy location: " + enemyLoc);
                            Debug.setIndicatorDot(Debug.info, enemyLoc, 255, 0, 0);
    
                            dx = enemyLoc.x - currLoc.x;
                            dy = enemyLoc.y - currLoc.y;
    
                            newFlag = Comms.getFlag(IC, dx + Util.dOffset, dy + Util.dOffset);
                            setFlag(newFlag);
    
                            ICtoTurnMap.add(IC.ordinal(), rc.getRoundNum());
    
                            return true;
                        case ENEMY_EC_CHILL_CALL:
                            Debug.println(Debug.info, "Propagating Chill Flag");
                            robotLoc = robot.getLocation();
                            DxDyFromRobot = Comms.getDxDy(flag);
                            
                            enemyLoc = new MapLocation(DxDyFromRobot[0] + robotLoc.x - Util.dOffset, 
                                                        DxDyFromRobot[1] + robotLoc.y - Util.dOffset);
                            Debug.println(Debug.info, "Apparant enemy location: " + enemyLoc);
                            Debug.setIndicatorDot(Debug.info, enemyLoc, 255, 0, 0);
    
                            dx = enemyLoc.x - currLoc.x;
                            dy = enemyLoc.y - currLoc.y;
    
                            newFlag = Comms.getFlag(IC, dx + Util.dOffset, dy + Util.dOffset);
                            setFlag(newFlag);
    
                            ICtoTurnMap.add(IC.ordinal(), rc.getRoundNum());
                        default:
                            break;
                    }
                }
            }
        }

        return false;
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
                int flag = Comms.getFlagTurn(InformationCategory.SLA_CLOSEST_ENEMY, 
                                            turnClosestKnownEnemy, dx + Util.dSmallOffset, dy + Util.dSmallOffset);
                setFlag(flag);
            } else {
                int flag = Comms.getFlagTurn(InformationCategory.CLOSEST_ENEMY, 
                                            turnClosestKnownEnemy, dx + Util.dSmallOffset, dy + Util.dSmallOffset);
                setFlag(flag);
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