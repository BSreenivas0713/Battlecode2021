package musketeerplayersprint;
import battlecode.common.*;

import musketeerplayersprint.Util.*;
public class Politician extends Robot {
    static Direction main_direction;
    static final int slandererFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.SLANDERER);
    
    public Politician(RobotController r) {
        super(r);
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.POL_BODYGUARD);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Util.vPrintln("I am a normal politician; current influence: " + rc.getInfluence());
        Util.vPrintln("current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        RobotInfo[] neutrals = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        RobotInfo[] within6 = rc.senseNearbyRobots(6, rc.getTeam());

        if(main_direction == null){
            main_direction = Util.randomDirection();
        }

        if((enemyAttackable.length >= 1 || neutrals.length != 0) && rc.canEmpower(actionRadius)) {
            rc.empower(actionRadius);
            return;
        }

        RobotInfo powerful = null;
        int max_influence = 0;

        MapLocation ECWithinSensable = null;

        for (RobotInfo robot : enemySensable) {
            int currInfluence = robot.getInfluence();
            if (robot.getType() == RobotType.MUCKRAKER && currInfluence > max_influence) {
                powerful = robot;
                max_influence = currInfluence;
            }
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                ECWithinSensable = robot.getLocation();
            }
        }
        
        if (powerful != null) {
            Direction toMove = rc.getLocation().directionTo(powerful.getLocation());
            tryMoveDest(toMove);
        }

        boolean within2ofEC = false;
        for (RobotInfo robot : rc.senseNearbyRobots(2, rc.getTeam())) {
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                within2ofEC = true;
            }
        }

        for (RobotInfo robot : within6) {
            if (robot.getType() == RobotType.POLITICIAN && rc.getFlag(robot.getID()) == 1 && !within2ofEC) {
                Util.vPrintln("within radius 6");
                return;
            }
        }

        RobotInfo bestSlanderer = null;
        max_influence = 0;
        for (RobotInfo robot : friendlySensable) {
            if (rc.canGetFlag(robot.getID()) && 
                rc.getFlag(robot.getID()) == slandererFlag && 
                robot.getInfluence() > max_influence) {
                Util.vPrintln("within sensing radius but not 6");
                if (ECWithinSensable == null || robot.getLocation().distanceSquaredTo(ECWithinSensable) > robot.getType().sensorRadiusSquared) {
                    max_influence = robot.getInfluence();
                }
                bestSlanderer = robot;
            }
        }

        if (bestSlanderer != null) {
            Direction toMove = rc.getLocation().directionTo(bestSlanderer.getLocation());
            tryMoveDest(toMove);
        }
        
        RobotInfo weakest = null;
        int min_influence = 0;
        for (RobotInfo robot : enemySensable) {
            int currInfluence = robot.getInfluence();
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && currInfluence < min_influence) {
                weakest = robot;
                min_influence = currInfluence;
            }
        }
        
        if (weakest != null) {
            Direction toMove = rc.getLocation().directionTo(weakest.getLocation());
            tryMoveDest(toMove);
        }

        if (!rc.getLocation().isWithinDistanceSquared(home, 2 * sensorRadius)) {
            tryMoveDest(rc.getLocation().directionTo(home));
        }
        
        while (!tryMove(main_direction) && rc.isReady()){
            main_direction = Util.randomDirection();
        }

        broadcastECLocation();
    }
}