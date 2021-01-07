package musketeerplayer;
import battlecode.common.*;

import musketeerplayer.Util.*;

public class Slanderer extends Robot {
    static Direction main_direction;
    
    public Slanderer(RobotController r) {
        super(r);
        if (rc.canSetFlag(1)) {
            rc.setFlag(1);
        }
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        System.out.println("I am a " + rc.getType() + "; current influence: " + rc.getInfluence());
        System.out.println("current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        if(main_direction == null) {
            main_direction = Util.randomDirection();
        }
        Team enemy = rc.getTeam().opponent();
        int sensorRadius = rc.getType().sensorRadiusSquared;
        RobotInfo[] enemiesInReach = rc.senseNearbyRobots(sensorRadius, enemy);
        RobotInfo minRobot = null;
        double minDistSquared = Integer.MAX_VALUE;
        MapLocation curr = rc.getLocation();
        for (RobotInfo robot : enemiesInReach) {
            double temp = Util.distanceSquared(curr, robot.getLocation());
            if (temp < minDistSquared) {
                minDistSquared = temp;
                minRobot = robot;
            }
        }
        if (minRobot != null) {
            main_direction = Util.findDirection(curr, minRobot.getLocation());
        }

        MapLocation target = rc.adjacentLocation(main_direction);
        if (rc.onTheMap(target)) {
            while (!tryMove(main_direction) && rc.isReady()) {
                main_direction = Util.randomDirection();
            }
        }
    }
}