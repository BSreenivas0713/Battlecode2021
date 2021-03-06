package auxExplorerPlayer;
import battlecode.common.*;

public class Util {
    static final RobotType[] spawnableRobot = {
        RobotType.POLITICIAN,
        RobotType.SLANDERER,
        RobotType.MUCKRAKER,
    };

    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    static final int spawnKillThreshold = 2;

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Returns a random spawnable RobotType
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnableRobotType() {
        return spawnableRobot[(int) (Math.random() * spawnableRobot.length)];
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
}