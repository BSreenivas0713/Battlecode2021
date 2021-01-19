package musketeerplayerqual;
import battlecode.common.*;

import musketeerplayerqual.Util.*;
import musketeerplayerqual.Comms.*;
import musketeerplayerqual.Debug.*;
import musketeerplayerqual.fast.FastIntIntMap;
import musketeerplayerqual.fast.FastLocIntMap;

public class Robot {
    static RobotController rc;
    static int turnCount;

    static int defaultFlag;
    static int nextFlag;
    static boolean resetFlagOnNewTurn = true;
    static MapLocation home;
    static int homeID;

    static int sensorRadius;
    static int actionRadius;
    static Team enemy;
    static RobotInfo[] enemySensable;
    static RobotInfo[] friendlySensable;
    static RobotInfo[] neutralSensable;
    static RobotInfo[] enemyAttackable;

    static Comms.SubRobotType subRobotType;

    static final int parityBroadcastEnemy = (int) (Math.random() * 2);

    static FastIntIntMap ICtoTurnMap;
    static FastLocIntMap friendlyECs;

    public static Robot changeTo = null;

    public Robot(RobotController r) {
        rc = r;
        turnCount = 0;
        enemy = rc.getTeam().opponent();
        sensorRadius = rc.getType().sensorRadiusSquared;
        actionRadius = rc.getType().actionRadiusSquared;
        defaultFlag = 0;
        ICtoTurnMap = new FastIntIntMap();
        friendlyECs = new FastLocIntMap();

        if(rc.getType() == RobotType.ENLIGHTENMENT_CENTER) {
            home = rc.getLocation();
            homeID = rc.getID();
            friendlyECs.add(home, homeID);
        } else {
            RobotInfo[] sensableWithin2 = rc.senseNearbyRobots(2, rc.getTeam());
            for (RobotInfo robot : sensableWithin2) {
                if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    MapLocation ecLoc = robot.getLocation();
                    home = ecLoc;
                    homeID = robot.getID();
                    friendlyECs.add(home, homeID);
                }
            }
        }

        if (home == null) {
            home = rc.getLocation();
            homeID = -1;
        }
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

        RobotInfo robot;
        // Moved beforehand, so we need to recalculate
        RobotInfo[] sensable = rc.senseNearbyRobots(sensorRadius);

        for(int i = sensable.length - 1; i >= 0; i--) {
            robot = sensable[i];
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                if(robot.getTeam() == rc.getTeam()) {
                    if(!robot.getLocation().equals(home)) {
                        res = true;
    
                        MapLocation ecLoc = robot.getLocation();
        
                        int ecDX = ecLoc.x - home.x + Util.dOffset;
                        int ecDY = ecLoc.y - home.y + Util.dOffset;
    
                        int encodedInf = Comms.encodeInf(robot.getInfluence());

                        nextFlag = Comms.getFlag(InformationCategory.FRIENDLY_EC, encodedInf, ecDX, ecDY);
                    }
                } else {
                    Debug.println(Debug.info, "Broadcasting enemy EC location");
                    res = true;
    
                    MapLocation ecLoc = robot.getLocation();
    
                    int ecDX = ecLoc.x - home.x + Util.dOffset;
                    int ecDY = ecLoc.y - home.y + Util.dOffset;
    
                    int encodedInf = Comms.encodeInf(robot.getInfluence());
                    if(robot.getTeam() == enemy) {
                        nextFlag = Comms.getFlag(InformationCategory.ENEMY_EC, encodedInf, ecDX, ecDY);
                    } else {
                        nextFlag = Comms.getFlag(InformationCategory.NEUTRAL_EC, encodedInf, ecDX, ecDY);
                    }
                }
            }
        }

        return res;
    }

    // Returns true if a flag was propagated
    boolean propagateFlags() throws GameActionException {
        // Remove keys that we don't needf
        int[] keys = ICtoTurnMap.getKeys();
        for(int key : keys) {
            if(rc.getRoundNum() > ICtoTurnMap.getVal(key) + Util.flagCooldown) {
                ICtoTurnMap.remove(key);
            }
        }

        // Moved beforehand, so we need to recalculate
        RobotInfo robot;
        RobotInfo[] friendlyNearby = rc.senseNearbyRobots(sensorRadius, rc.getTeam());
        MapLocation currLoc = rc.getLocation();

        int flag;
        InformationCategory IC;
        
        // Only propgatable flags
        MapLocation robotLoc;
        int []DxDyFromRobot;
        MapLocation enemyLoc;
        int dx;
        int dy;
        int encodedInf;
        int newFlag;

        for(int i = friendlyNearby.length - 1; i >= 0; i--) {
            robot = friendlyNearby[i];
            if(rc.canGetFlag(robot.getID())) {
                flag = rc.getFlag(robot.getID());
                IC = Comms.getIC(flag);

                // Do not propagate if we have propagated recently
                if(!ICtoTurnMap.contains(IC.ordinal())) {
                    switch(IC) {
                        case ENEMY_EC_ATTACK_CALL:
                            Debug.println(Debug.info, "Propagating Attack Flag");
                            robotLoc = robot.getLocation();
                            DxDyFromRobot = Comms.getDxDy(flag);
                            
                            enemyLoc = new MapLocation(DxDyFromRobot[0] + robotLoc.x - Util.dOffset, 
                                                        DxDyFromRobot[1] + robotLoc.y - Util.dOffset);
                            Debug.println(Debug.info, "Apparant enemy location: " + enemyLoc);
                            Debug.setIndicatorDot(Debug.info, enemyLoc, 255, 0, 0);
                            Debug.setIndicatorDot(Debug.info, robotLoc, 0, 0, 255);
    
                            dx = enemyLoc.x - currLoc.x;
                            dy = enemyLoc.y - currLoc.y;
                            encodedInf = Comms.encodeInf(robot.getInfluence());

                            newFlag = Comms.getFlag(IC, encodedInf, dx + Util.dOffset, dy + Util.dOffset);
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
                            Debug.setIndicatorDot(Debug.info, robotLoc, 0, 0, 255);
    
                            dx = enemyLoc.x - currLoc.x;
                            dy = enemyLoc.y - currLoc.y;
                            encodedInf = Comms.encodeInf(robot.getInfluence());

                            newFlag = Comms.getFlag(IC, encodedInf, dx + Util.dOffset, dy + Util.dOffset);
                            setFlag(newFlag);
    
                            ICtoTurnMap.add(IC.ordinal(), rc.getRoundNum());
    
                            return true;
                        default:
                            break;
                    }
                }
            }
        }

        return false;
    }

    boolean broadcastEnemyLocal(MapLocation enemyLoc) throws GameActionException {
        if(enemyLoc == null || !rc.canSenseLocation(enemyLoc))
            return false;
        
        MapLocation currLoc = rc.getLocation();
        int dx = enemyLoc.x - currLoc.x;
        int dy = enemyLoc.y - currLoc.y;

        Debug.println(Debug.info, "Broadcasting enemy locally at: dX: " + dx + ", dY: " + dy);

        if(subRobotType == Comms.SubRobotType.SLANDERER) {
            int flag = Comms.getFlag(InformationCategory.CLOSEST_ENEMY_OR_FLEEING, 
                                    Comms.ClosestEnemyOrFleeing.SLA,
                                    dx + Util.dOffset, dy + Util.dOffset);
            setFlag(flag);
        } else {
            int flag = Comms.getFlag(InformationCategory.CLOSEST_ENEMY_OR_FLEEING, 
                                    Comms.ClosestEnemyOrFleeing.NOT_SLA, 
                                    dx + Util.dOffset, dy + Util.dOffset);
            setFlag(flag);
        }

        return true;
    }

    boolean broadcastEnemyLocalOrGlobal(MapLocation enemyLoc) throws GameActionException {
        return broadcastEnemyLocalOrGlobal(enemyLoc, Comms.EnemyType.UNKNOWN);
    }

    boolean broadcastEnemyLocalOrGlobal(MapLocation enemyLoc, Comms.EnemyType type) throws GameActionException {
        if(enemyLoc == null)
            return false;
            
        Debug.setIndicatorDot(Debug.info, enemyLoc, 0, 0, 0);

        if(rc.getRoundNum() % 2 == parityBroadcastEnemy) {
            broadcastEnemyFound(enemyLoc, type);
        } else {
            // Broadcast locally first, and then globally if we can't fit it in the local version.
            if(!broadcastEnemyLocal(enemyLoc)) {
                broadcastEnemyFound(enemyLoc, type);
            }
        }
        return true;
    }

    boolean broadcastEnemyFound(MapLocation enemyLoc, Comms.EnemyType type) throws GameActionException {
        int enemyDx = enemyLoc.x - home.x;
        int enemyDy = enemyLoc.y - home.y;

        MapLocation currLoc = rc.getLocation();
        int dx = enemyLoc.x - currLoc.x;
        int dy = enemyLoc.y - currLoc.y;

        Debug.println(Debug.info, "Broadcasting enemy globally at: dX: " + dx + ", dY: " + dy);

        int flag = Comms.getFlag(InformationCategory.ENEMY_FOUND, type.ordinal(), enemyDx + Util.dOffset, enemyDy + Util.dOffset);
        setFlag(flag);

        return true;
    }

    void setFlag(int flag) throws GameActionException {
        if(rc.canSetFlag(flag)) {
            rc.setFlag(flag);
        }
    }
}