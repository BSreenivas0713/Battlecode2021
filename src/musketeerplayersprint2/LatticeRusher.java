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
        boolean tooSmall = false;
        boolean needToChill = false;
        MapLocation possibleNewHome = null;
        boolean slandererNearby = false;
        RobotInfo closestMuckrakerSensable = null;
        int minMuckrakerDistance = Integer.MAX_VALUE;
        int maxEnemyAttackableDistSquared = Integer.MIN_VALUE;
        int numMuckAttackable = 0;

        for(int i = enemyAttackable.length - 1; i >= 0; i--) {
            robot = enemyAttackable[i];
            MapLocation loc = robot.getLocation();
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER && 
                enemyLocation.isWithinDistanceSquared(loc, 8)) {
                int dist = currLoc.distanceSquaredTo(loc);
                if(dist < minEnemyDistSquared) {
                    minEnemyDistSquared = dist;
                    closestEnemy = loc;
                    if ((int) (robot.getInfluence() / 75) > (rc.getInfluence() - 10)) {
                        tooSmall = true;
                        Debug.println(Debug.info, "Base seems to be too big to overtake, telling other troops to chill");
                        int dx = enemyLocation.x - currLoc.x;
                        int dy = enemyLocation.y - currLoc.y;
                        int encodedInf = Comms.encodeInf(robot.getInfluence());

                        int newFlag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC_CHILL_CALL, encodedInf, dx + Util.dOffset, dy + Util.dOffset);
                        setFlag(newFlag);
                        needToChill = true;
                        closestEnemy = loc;
                        minEnemyDistSquared = dist;
                        possibleNewHome = loc;
                    }
                }
            } else {
                int temp = currLoc.distanceSquaredTo(robot.getLocation());
                if (temp > maxEnemyAttackableDistSquared) {
                    maxEnemyAttackableDistSquared = temp;
                }
                if(robot.getType() == RobotType.MUCKRAKER) {
                    numMuckAttackable++;
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
                    if(closestEnemy == null && dist < minEnemyDistSquared) {
                        Debug.println(Debug.info, "Base seems to be converted to our side, baseConverted set to true, telling other troops to chill");
                        int dx = enemyLocation.x - currLoc.x;
                        int dy = enemyLocation.y - currLoc.y;

                        int newFlag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC_CHILL_CALL, dx + Util.dOffset, dy + Util.dOffset);
                        setFlag(newFlag);
                        needToChill = true;
                        baseConverted = true;
                        closestEnemy = loc;
                        minEnemyDistSquared = dist;
                        possibleNewHome = loc;
                    }
                }

                if(rc.canGetFlag(robot.getID())) {
                    int flag = rc.getFlag(robot.getID());
                    if (Comms.getIC(flag) == Comms.InformationCategory.ENEMY_EC_CHILL_CALL) {
                        int possibleBaseInfluence = Comms.getInf(flag);
                        if((rc.getInfluence() - 10) < (int) (possibleBaseInfluence / 20)) {
                            needToChill = true;
                            Debug.println(Debug.info, "Recieved chill call, needToChill is getting set to true.");
                        }
                    }

                    if(Comms.isSubRobotType(flag, Comms.SubRobotType.SLANDERER)) {
                        slandererNearby = true;
                    }
                }
            }
        }

        //empower if near 2 enemies or enemy is in sensing radius of our base
        if (((numMuckAttackable > 1 || 
            (numMuckAttackable > 0 && slandererNearby)))
            && rc.canEmpower(maxEnemyAttackableDistSquared)) {
            Debug.println(Debug.info, "Enemy too close to base. I will empower");
            rc.empower(maxEnemyAttackableDistSquared);
            return;
        }

        if (rc.canEmpower(minEnemyDistSquared) && !tooSmall && (moveSemaphore <= 0 || minEnemyDistSquared <= 1)) {
            int radius = Math.min(actionRadius, minEnemyDistSquared);
            Debug.println(Debug.info, "Empowered with radius: " + radius);
            Debug.setIndicatorLine(Debug.info, rc.getLocation(), closestEnemy, 255, 150, 50);
            rc.empower(radius);
            return;
        }

        if(baseConverted) {
            Debug.println(Debug.info, "Becoming a lattice Protector for this new base that has been overtaken");
            changeTo = new LatticeProtector(rc, possibleNewHome);
            return;
        }
        if(needToChill || tooSmall) {
            Debug.println(Debug.info, "New base overtaken/too big to attack, becoming protector for old base");
            changeTo = new LatticeProtector(rc, home);
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