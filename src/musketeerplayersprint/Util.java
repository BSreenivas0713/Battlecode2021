package musketeerplayersprint;

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

    static final Direction[] defenderDirs = {
        Direction.NORTHEAST,
        Direction.SOUTHEAST,
        Direction.SOUTHWEST,
        Direction.NORTHWEST,
    };

    static final int spawnKillThreshold = 3;
    static final int dOffset = 64;
    static final int phaseOne = 50;
    static final int phaseTwo = 2000;
    static final int defenderPoliticianFrequency = 5;
    static final int minRushInfluence = 200;
    static final int minECRushConviction = 150;
    static final int timeBeforeDefenders = 50;
    static final int startCleanupThreshold = 100;
    static final int cleanupPoliticianInfluence = 34;
    static final int numDefenders = 4;
    static final int minTimeBetweenRushes = 0;

    static final boolean verbose = true;

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
}