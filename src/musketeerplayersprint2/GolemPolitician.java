package musketeerplayersprint2;
import battlecode.common.*;

import musketeerplayersprint2.Util.*;
import musketeerplayersprint2.Debug.*;

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
        MapLocation currLoc = rc.getLocation();
        int maxEnemyDistSquared = Integer.MIN_VALUE;
        MapLocation farthestEnemy = null;
        for (RobotInfo robot : enemyAttackable) {
            attackable_conviction += robot.getConviction();
            int temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp > maxEnemyDistSquared) {
                maxEnemyDistSquared = temp;
                farthestEnemy = robot.getLocation();
            }
        }

        if ((attackable_conviction >= min_attackable_conviction || enemyAttackable.length >= 3) && rc.canEmpower(maxEnemyDistSquared)) {
            Debug.println(Debug.info, "Empowered with radius: " + maxEnemyDistSquared);
            Debug.setIndicatorLine(Debug.info, rc.getLocation(), farthestEnemy, 255, 150, 50);
            rc.empower(maxEnemyDistSquared);
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
        RobotInfo minRobot = null;
        double minDistSquared = Integer.MAX_VALUE;
        for (RobotInfo robot : enemySensable) {
            int enemyConviction = robot.getConviction();
            if(enemyConviction > maxEnemyConviction) {
                enemyRobot = robot;
                maxEnemyConviction = enemyConviction;
            }
            double temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp < minDistSquared) {
                minDistSquared = temp;
                minRobot = robot;
            }
        }
        if (minRobot != null) {
            broadcastEnemyFound(minRobot.getLocation());
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
            changeTo = new ExplorerPolitician(rc);
            return;
        }
    }
}