package musketeerPlayer;
import battlecode.common.*;

import musketeerPlayer.Util.*;

public class GolemPolitician extends Robot {
    static Direction main_direction;
    
    public GolemPolitician(RobotController r) {
        super(r);
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.POL_GOLEM);
    }
    
    public GolemPolitician(RobotController r, int dx, int dy) {
        super(r);
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.POL_GOLEM);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Util.vPrintln("I am a golem politician; current influence: " + rc.getInfluence());
        Util.vPrintln("Golem extra line");
        int min_attackable_conviction = (rc.getConviction()-10) / 3;
        int attackable_conviction = 0;
        for (RobotInfo robot : enemyAttackable) {
            attackable_conviction += robot.getConviction();
        }

        if (attackable_conviction >= min_attackable_conviction && rc.canEmpower(actionRadius)) {
            // Util.vPrintln("empowering...");
            rc.empower(actionRadius);
            Util.vPrintln("empowered");
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
            changeTo = new ExplorerPolitician(rc, dx, dy);
            return;
        }
    }
}