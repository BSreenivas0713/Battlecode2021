package musketeerplayer;
import battlecode.common.*;

import musketeerplayer.Util.*;

public class Slanderer extends Robot {
    
    public Slanderer(RobotController r) {
        super(r);
    }

    static double distanceSquared(MapLocation curr, MapLocation enemy) {
        return Math.pow(Math.abs(enemy.x - curr.x),2) + Math.pow(Math.abs(enemy.y - curr.y),2);
    }
    
    static Direction findDirection(MapLocation curr, MapLocation enemy) {
        int dy = curr.y - enemy.y;
        int dx = curr.x - enemy.x;
        //setting angle
        double angle;
        if (dx == 0 && dy > 0) {
            angle = 90;
        }
        else if (dx < 0 && dy == 0) {
            angle = 180;
        }
        else if (dx == 0 && dy < 0) {
            angle = 270;
        }
        else if (dx > 0 && dy == 0) {
            angle = 360;
        }
        else {
            angle = Math.toDegrees(Math.abs(Math.atan(dy/dx)));
        }
    
        //adding angle offsets
        if (dx < 0 && dy >= 0) {
            angle += 90;
        }
        else if (dx < 0 && dy < 0) {
            angle += 180;
        }
        else if (dx >= 0 && dy < 0) {
            angle += 270;
        }
        angle = angle % 360;
    
        //returning directions
        if (22.5 <= angle && angle < 67.5) {
            return Direction.NORTHEAST;
        }
        else if (67.5 <= angle && angle < 112.5) {
            return Direction.NORTH;
        }
        else if (112.5 <= angle && angle < 157.5) {
            return Direction.NORTHWEST;
        }
        else if (157.5 <= angle && angle < 202.5) {
            return Direction.WEST;
        }
        else if (202.5 <= angle && angle < 247.5) {
            return Direction.SOUTHWEST;
        }
        else if (247.5 <= angle && angle < 292.5) {
            return Direction.SOUTH;
        }
        else if (292.5 <= angle && angle < 337.5) {
            return Direction.SOUTHEAST;
        }
        else {
            return Direction.EAST;
        }
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
            double temp = distanceSquared(curr, robot.getLocation());
            if (temp < minDistSquared) {
                minDistSquared = temp;
                minRobot = robot;
            }
        }
        Direction toMove = Util.randomDirection();
        if (minRobot != null) {
            toMove = findDirection(curr, minRobot.getLocation());
        }
    
        if (tryMove(toMove));
            // System.out.println("I moved!");
    }
}