package musketeerplayer;
import battlecode.common.*;

import musketeerplayer.Util.*;

public class Slanderer extends Robot {
    static Direction main_direction;
    
    public Slanderer(RobotController r) {
        super(r);
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.SLANDERER);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        
        if (rc.getType() != RobotType.SLANDERER) {
            if(turnCount % 2 == 0) {
                changeTo = new Politician(rc, dx, dy);
            } else {
                changeTo = new ExplorerPolitician(rc, dx, dy);
            }
            return;
        }

        Util.vPrintln("I am a slanderer; current influence: " + rc.getInfluence());
        Util.vPrintln("current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        Util.vPrintln("current flag: " + rc.getFlag(rc.getID()));

        if(main_direction == null) {
            main_direction = Util.randomDirection();
        }

        RobotInfo[] neutralECs = rc.senseNearbyRobots(sensorRadius, Team.NEUTRAL);
        RobotInfo minRobot = null;
        double minDistSquared = Integer.MAX_VALUE;
        MapLocation curr = rc.getLocation();
        for (RobotInfo robot : enemySensable) {
            double temp = curr.distanceSquaredTo(robot.getLocation());
            if (temp < minDistSquared) {
                minDistSquared = temp;
                minRobot = robot;
            }
        }
        
        RobotInfo minNeutralRobot = null;
        double minNeutralDistSquared = Integer.MAX_VALUE;
        for (RobotInfo robot: neutralECs) {
            double temp = curr.distanceSquaredTo(robot.getLocation());
            if (temp < minNeutralDistSquared) {
                minNeutralDistSquared = temp;
                minNeutralRobot = robot;
            }
        }
        
        RobotInfo friendlyEC = null;
        for (RobotInfo robot: friendlySensable) {
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                friendlyEC = robot;
            }
        }

        if (minRobot != null) {
            main_direction = curr.directionTo(minRobot.getLocation()).opposite();
        }
        else if (minNeutralRobot != null) {
            main_direction = curr.directionTo(minNeutralRobot.getLocation()).opposite(); 
        }
        else if (friendlyEC != null) {
            main_direction = curr.directionTo(friendlyEC.getLocation()).opposite(); 
        }

        MapLocation target = rc.adjacentLocation(main_direction);
        if (rc.onTheMap(target)) {
            while (!tryMove(main_direction) && rc.isReady()) {
                main_direction = Util.randomDirection();
            }
        }

        broadcastECLocation();
    }
}