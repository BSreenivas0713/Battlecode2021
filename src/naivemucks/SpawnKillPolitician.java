package naivemucks;

import battlecode.common.*;

import naivemucks.Util.*;
import naivemucks.Debug.*;

public class SpawnKillPolitician extends Robot {
    static Direction main_direction;
    
    public SpawnKillPolitician(RobotController r) {
        super(r);
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.POL_SPAWNKILL);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Debug.println(Debug.info, "I am a spawn kill politician; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        RobotInfo minRobot = null;
        double minDistSquared = Integer.MAX_VALUE;
        MapLocation currLoc = rc.getLocation();
        for (RobotInfo robot : enemySensable) {
            double temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp < minDistSquared) {
                minDistSquared = temp;
                minRobot = robot;
            }
        }
        if (minRobot != null) {
            broadcastEnemyFound(minRobot.getLocation());
        }

        if(rc.getEmpowerFactor(rc.getTeam(), 0) > Util.spawnKillThreshold) {
            int radius = Math.max(1, home.distanceSquaredTo(rc.getLocation()));
            if (rc.canEmpower(radius)) {
                Debug.println(Debug.info, "Empowered with radius: " + radius);
                Debug.setIndicatorLine(Debug.info, rc.getLocation(), home, 255, 150, 50);
                rc.empower(radius);
            }
        } else {
            changeTo = new GolemPolitician(rc);
        }
    }
}