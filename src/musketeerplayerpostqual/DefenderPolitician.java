package musketeerplayerpostqual;
import battlecode.common.*;

import musketeerplayerpostqual.Util.*;
import musketeerplayerpostqual.Debug.*;

public class DefenderPolitician extends Robot {
    static Direction main_direction;
    static boolean hasSeenEnemy = false;
    
    public DefenderPolitician(RobotController r) {
        super(r);
        subRobotType = Comms.SubRobotType.POL_DEFENDER;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
    }
    
    public DefenderPolitician(RobotController r, MapLocation h, int hID) {
        this(r);
        home = h;
        homeID = hID;
        friendlyECs.add(home, homeID);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Debug.println(Debug.info, "I am a defender politician; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "hasSeenEnemy: " + hasSeenEnemy);
        
        RobotInfo robot;
        MapLocation currLoc = rc.getLocation();
        int maxEnemyDistSquared = Integer.MIN_VALUE;
        MapLocation farthestEnemy = null;

        for(int i = enemyAttackable.length - 1; i >= 0; i--) {
            robot = enemyAttackable[i];
            int temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp > maxEnemyDistSquared) {
                maxEnemyDistSquared = temp;
                farthestEnemy = robot.getLocation();
            }
        }

        if (enemyAttackable.length != 0 && rc.canEmpower(maxEnemyDistSquared)) {
            Debug.println(Debug.info, "Empowered with radius: " + maxEnemyDistSquared);
            Debug.setIndicatorLine(Debug.info, rc.getLocation(), farthestEnemy, 255, 150, 50);
            rc.empower(maxEnemyDistSquared);
            return;
        }

        RobotInfo closestEnemy = null;
        Comms.EnemyType closestEnemyType = null;
        int minDistance = Integer.MAX_VALUE;
        for(int i = enemySensable.length - 1; i >= 0; i--) {
            robot = enemySensable[i];
            int dist = rc.getLocation().distanceSquaredTo(robot.location);
            if(dist < minDistance) {
                closestEnemy = robot;
                minDistance = dist;
                hasSeenEnemy = true;
                if(robot.getType() == RobotType.MUCKRAKER) {
                    closestEnemyType = Comms.EnemyType.MUC;
                } else if(Util.isSlandererInfluence(robot.getInfluence())) {
                    closestEnemyType = Comms.EnemyType.SLA;   
                } else {
                    closestEnemyType = Comms.EnemyType.UNKNOWN;
                }
            }
        }

        if(hasSeenEnemy && enemySensable.length == 0) {
            changeTo = new ExplorerPolitician(rc, home, homeID);
            return;
        }
        
        if (closestEnemy != null) {
            Direction toMove = rc.getLocation().directionTo(closestEnemy.getLocation());
            tryMoveDest(toMove);
        }

        int distToClosestDefender = 500;
        MapLocation closestDefenderLoc = null;
        int distToEC = 500;
        MapLocation ECLoc = null;

        for(int i = friendlySensable.length - 1; i >= 0; i--) {
            robot = friendlySensable[i];
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
        
        // This means that the first half of an EC-ID/EC-ID broadcast finished.
        if(needToBroadcastHomeEC && rc.getFlag(rc.getID()) == defaultFlag) { broadcastHomeEC(); }
        else if(broadcastECLocation());
        else if(closestEnemy != null && broadcastEnemyLocalOrGlobal(closestEnemy.getLocation(), closestEnemyType));
    }
}