package musketeerplayer;
import battlecode.common.*;

import musketeerplayer.Util.*;

public class Slanderer extends Robot {
    
    public Slanderer(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
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
        Direction toMove = Util.randomDirection();
        if (minRobot != null) {
            toMove = Util.findDirection(curr, minRobot.getLocation());
        }
    
        if (tryMove(toMove));
            // System.out.println("I moved!");
    }
}