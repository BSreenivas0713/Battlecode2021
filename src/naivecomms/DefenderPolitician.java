package naivecomms;
import battlecode.common.*;

import naivecomms.Util.*;
import naivecomms.Debug.*;

public class DefenderPolitician extends Robot {
    static Direction main_direction;
    static boolean hasSeenEnemy = false;
    
    public DefenderPolitician(RobotController r) {
        super(r);
        subRobotType = Comms.SubRobotType.POL_DEFENDER;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
    }
    
    public DefenderPolitician(RobotController r, MapLocation h) {
        this(r);
        home = h;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Debug.println(Debug.info, "I am a defender politician; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "hasSeenEnemy: " + hasSeenEnemy);
        
        MapLocation currLoc = rc.getLocation();
        int minEnemyDistSquared = Integer.MAX_VALUE;
        MapLocation closestEnemy = null;
        for (RobotInfo robot : enemyAttackable) {
            int temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp < minEnemyDistSquared) {
                minEnemyDistSquared = temp;
                closestEnemy = robot.getLocation();
            }
        }

        if (enemyAttackable.length != 0 && rc.canEmpower(minEnemyDistSquared)) {
            Debug.println(Debug.info, "Empowered with radius: " + minEnemyDistSquared);
            Debug.setIndicatorLine(Debug.info, rc.getLocation(), closestEnemy, 255, 150, 50);
            rc.empower(minEnemyDistSquared);
            return;
        }

        RobotInfo enemyRobot = null;
        int minDistance = Integer.MAX_VALUE;
        for (RobotInfo robot : enemySensable) {
            int dist = rc.getLocation().distanceSquaredTo(robot.location);
            if(dist < minDistance) {
                enemyRobot = robot;
                minDistance = dist;
                hasSeenEnemy = true;
            }
        }
        
        // if (enemyRobot != null) {
        //     broadcastEnemyFound(enemyRobot.getLocation());
        // }

        if(hasSeenEnemy && enemySensable.length == 0) {
            changeTo = new ExplorerPolitician(rc, home);
            return;
        }
        
        if (enemyRobot != null) {
            Direction toMove = rc.getLocation().directionTo(enemyRobot.getLocation());
            tryMoveDest(toMove);
        }

        int distToClosestDefender = 500;
        MapLocation closestDefenderLoc = null;
        int distToEC = 500;
        MapLocation ECLoc = null;
        for (RobotInfo robot: friendlySensable) {
            if(rc.canGetFlag(robot.getID())) {
                int flag = rc.getFlag(robot.getID());
                if(Comms.isSubRobotType(flag, subRobotType)) {
                    int distToCurrDefender = rc.getLocation().distanceSquaredTo(robot.getLocation());
                    if(distToCurrDefender < distToClosestDefender) {
                        distToClosestDefender = distToCurrDefender;
                        closestDefenderLoc = robot.getLocation();
                    }
                }
            }
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
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
        }
        if(distToEC <= 2) {
            Direction toMove = rc.getLocation().directionTo(ECLoc).opposite();
            tryMoveDest(toMove);
        }
        if(closestDefenderLoc != null) {
            Direction toMove = rc.getLocation().directionTo(closestDefenderLoc).opposite();
            tryMoveDest(toMove);
        }
        
        if(broadcastECLocation());
        else if(broadcastEnemyLocalOrGlobal());
    }
}