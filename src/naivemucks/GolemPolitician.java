package naivemucks;
import battlecode.common.*;

import naivemucks.Util.*;
import naivemucks.Debug.*;

public class GolemPolitician extends Robot {
    static Direction main_direction;
    
    public GolemPolitician(RobotController r) {
        super(r);
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.POL_GOLEM);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Debug.println(Debug.info, "I am a golem politician; current influence: " + rc.getInfluence());
        
        int min_attackable_conviction = (rc.getConviction()-10) / 3;
        int attackable_conviction = 0;
        int distancetoECSemaphor = 2;
        MapLocation currLoc = rc.getLocation();
        int maxEnemyDistSquared = Integer.MIN_VALUE;
        MapLocation farthestEnemy = null;
        boolean enemyECinRadius = false;
        MapLocation enemyLoc = null;
        for (RobotInfo robot : enemyAttackable) {
            attackable_conviction += robot.getConviction();
            int temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp > maxEnemyDistSquared) {
                maxEnemyDistSquared = temp;
                farthestEnemy = robot.getLocation();
            }
        }
        for (RobotInfo robot: enemySensable) {
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                enemyECinRadius = true;
                enemyLoc = robot.getLocation();
            }
        }

        int ECInfluence = 0;
        MapLocation ECPosition = null;
        for (RobotInfo robot: friendlySensable) {
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                ECInfluence = robot.getInfluence();
                ECPosition = robot.getLocation();
                if(rc.canGetFlag(robot.getID())) {
                    int flag = rc.getFlag(robot.getID());
                    if(Comms.getIC(flag) == Comms.InformationCategory.RUSH_EC_GOLEM) {
                        int[] dxdy = Comms.getDxDy(flag);
                        MapLocation spawningLoc = rc.getLocation();
                        MapLocation rushEnemyLoc = new MapLocation(dxdy[0] + spawningLoc.x - Util.dOffset, dxdy[1] + spawningLoc.y - Util.dOffset);
                        changeTo = new RushPolitician(rc, rushEnemyLoc);
                        return;
                    }
                }
            }
        }

        boolean boostBase = false;
        if(ECInfluence !=0 && ECInfluence <= rc.getInfluence() ) {
            if(distancetoECSemaphor == 0) {
                boostBase = true;
            }
            else {
                Direction toMove = rc.getLocation().directionTo(ECPosition);
                int DistancetoECBefore = rc.getLocation().distanceSquaredTo(ECPosition);
                boolean moveSuccesful = tryMoveDest(toMove);
                int DistancetoECAfter = rc.getLocation().distanceSquaredTo(ECPosition);
                if(moveSuccesful && (DistancetoECBefore > DistancetoECAfter)) {
                    distancetoECSemaphor = 2;
                }
                else {
                    distancetoECSemaphor--;
                }

            }
        }

        if ((attackable_conviction >= min_attackable_conviction || boostBase) && rc.canEmpower(maxEnemyDistSquared)) {
            Debug.println(Debug.info, "Empowered with radius: " + maxEnemyDistSquared);
            Debug.setIndicatorLine(Debug.info, rc.getLocation(), farthestEnemy, 255, 150, 50);
            rc.empower(maxEnemyDistSquared);
            return;
        }
        if (enemyECinRadius) {
            changeTo = new RushPolitician(rc, enemyLoc);
            return;
        }
        int distToEC = 500;
        boolean sensesEC = false;
        MapLocation ECLoc = null;
        MapLocation closestGolemLoc = null; 
        int distToClosestGolem = 500;
        for (RobotInfo robot: friendlySensable) {
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                sensesEC = true;
                boolean seenCenter = false;
                for(RobotInfo secondRobot: rc.senseNearbyRobots(actionRadius, rc.getTeam())) {
                    if(secondRobot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                        seenCenter = true;
                        distToEC = rc.getLocation().distanceSquaredTo(robot.getLocation());
                        ECLoc = robot.getLocation();
                    }
                }
                if(!seenCenter) {
                    Direction toMove = rc.getLocation().directionTo(robot.getLocation());
                    tryMoveDest(toMove);
                }
            }
            else if(rc.canGetFlag(robot.getID())) {
                if(rc.getFlag(robot.getID()) == defaultFlag) {
                    int distToCurrGolem = rc.getLocation().distanceSquaredTo(robot.getLocation());
                    if(distToCurrGolem < distToClosestGolem) {
                        distToClosestGolem = distToCurrGolem;
                        closestGolemLoc = robot.getLocation();
                    }
                }
            }
        }
        RobotInfo enemyRobot = null;
        int maxEnemyConviction = min_attackable_conviction - 1;
        for (RobotInfo robot : enemySensable) {
            int enemyConviction = robot.getConviction();
            if(enemyConviction > maxEnemyConviction) {
                enemyRobot = robot;
                maxEnemyConviction = enemyConviction;
            }
        }

        
        if (enemyRobot != null) {
            Direction toMove = rc.getLocation().directionTo(enemyRobot.getLocation());
            tryMoveDest(toMove);
        }
        if(distToEC <= 2) {
            Direction toMove = rc.getLocation().directionTo(ECLoc).opposite();
            tryMoveDest(toMove);
        }
        if(closestGolemLoc != null) {
            Direction toMove = rc.getLocation().directionTo(closestGolemLoc).opposite();
            tryMoveDest(toMove);
        }
        if (!sensesEC) {
            Direction toMove = rc.getLocation().directionTo(home);
            tryMoveDest(toMove);
        }
    }
}