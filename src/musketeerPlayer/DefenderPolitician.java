package musketeerplayer;
import battlecode.common.*;

import musketeerplayer.Util.*;

public class DefenderPolitician extends Robot {
    static Direction main_direction;
    static boolean hasSeenEnemy = false;
    
    public DefenderPolitician(RobotController r) {
        super(r);
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.POL_DEFENDER);
    }
    
    public DefenderPolitician(RobotController r, int dx, int dy) {
        super(r, dx, dy);
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.POL_DEFENDER);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Util.vPrintln("I am a defender politician; current influence: " + rc.getInfluence());
        Util.vPrintln("hasSeenEnemy: " + hasSeenEnemy);
        

        if (enemyAttackable.length != 0 && rc.canEmpower(actionRadius)) {
            //Util.vPrintln("empowering...");
            rc.empower(actionRadius);
            //Util.vPrintln("empowered");
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

        if(hasSeenEnemy && enemySensable.length == 0) {
            changeTo = new ExplorerPolitician(rc, dx, dy);
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
                if(rc.getFlag(robot.getID()) == defaultFlag) {
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
    }
}