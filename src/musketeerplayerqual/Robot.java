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

    boolean broadcastWall() throws GameActionException {
        boolean res = false;
        MapLocation currLoc = rc.getLocation();
        Direction[] cardinalDirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        int[][] dirToCoordMap = {{0,0},{0,0},{0,0},{0,0}};
        Direction dir;
        MapLocation temp;
        for (int i = cardinalDirs.length - 1; i >= 0; i--) {
            dir = cardinalDirs[i];
            temp = currLoc;
            for (int j = 4; j >= 0; j--) {
                temp = temp.add(dir);
                if (!rc.onTheMap(temp)) {
                    dirToCoordMap[i][0] = 1;
                    if (i == 0 || i == 2) { //found a wall north or south of home
                        dirToCoordMap[i][1] = temp.y - home.y + Util.dOffset;
                    }
                    else { //found a wall east or west of home
                        dirToCoordMap[i][1] = temp.x - home.x + Util.dOffset;
                    }
                    break;
                }
            }  
        }
        int[] currDirToCoord;
        int wallDx = 0;
        int wallDy = 0;
        for (int i = dirToCoordMap.length - 1; i >= 0; i--) {
            currDirToCoord = dirToCoordMap[i];
            dir = cardinalDirs[i];
            if (currDirToCoord[0] == 1) { //only set wallDx or wallDy if we found a wall in one of the dirs
                if (dir == Direction.NORTH || dir == Direction.SOUTH) {
                    wallDy = currDirToCoord[1];
                }
                else {
                    wallDx = currDirToCoord[1];
                }
            }
        }
        if (wallDx != 0 || wallDy != 0) {
            Debug.println(Debug.info, "broadcasting wall with dx: " + wallDx + ", dy: " + wallDy);
            nextFlag = Comms.getFlag(InformationCategory.REPORTING_WALL, wallDx, wallDy);
            res = true;
        }

        return res;
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