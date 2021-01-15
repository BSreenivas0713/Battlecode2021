package musketeerplayersprint2;

import battlecode.common.*;

public class Util {
    static enum RotationDirection {
        CLOCKWISE,
        COUNTERCLOCKWISE
    };

    static enum DirectionPreference {
        RANDOM,
        ORTHOGONAL,
        DIAGONAL,
    }

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

    static final Direction[] orthogonalDirs = {
        Direction.NORTH,
        Direction.SOUTH,
        Direction.WEST,
        Direction.EAST,
    };

    static final Direction[] diagonalDirs = {
        Direction.NORTH,
        Direction.SOUTH,
        Direction.WEST,
        Direction.EAST,
    };

    static final int spawnKillThreshold = 5;
    static final int dOffset = 64;
    static final int dSmallOffset = 8;

    static final int phaseOne = 30;
    static final int phaseTwo = 2000;

    static final int numDefenders = 4;
    static final int timeBeforeDefenders = 50;

    static final int minRushInfluence = 200;
    static final int maxECRushConviction = 150;
    static final int minTimeBetweenRushes = 0;
    
    static final int startCleanupThreshold = 100;
    static final int cleanupPoliticianInfluence = 34;
    
    static final int maxSlandererInfluence = 949;
    static final int[] bestSlandererCosts = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 41, 41, 41, 41, 41, 41, 
        41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 85, 85, 85, 85, 
        85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 107, 107, 107, 107, 107, 107, 107, 107, 107, 107, 107, 107, 107, 107, 107, 107, 107, 107, 107, 107, 107, 107, 107, 130, 130, 130, 130, 130, 130, 130, 130, 130, 130, 130, 130, 130, 130, 130, 130, 130, 130, 130, 130, 130, 130, 130, 130, 154, 154, 154, 154, 154, 154, 154, 154, 154, 154, 154, 154, 154, 154, 154, 154, 154, 154, 154, 154, 154, 154, 154, 154, 178, 178, 178, 178, 178, 178, 178, 178, 178, 178, 178, 178, 178, 178, 178, 178, 178, 178, 178, 178, 178, 178, 178, 178, 178, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 
        203, 228, 228, 228, 228, 228, 228, 228, 228, 228, 228, 228, 228, 228, 228, 228, 228, 228, 228, 228, 228, 228, 228, 228, 228, 228, 228, 228, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 282, 282, 282, 282, 282, 282, 282, 282, 282, 282, 282, 282, 282, 282, 282, 282, 282, 282, 282, 282, 282, 282, 282, 282, 282, 282, 282, 282, 310, 310, 310, 310, 310, 310, 310, 310, 310, 310, 310, 310, 310, 310, 310, 310, 310, 310, 310, 310, 310, 310, 310, 310, 310, 310, 310, 310, 310, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 368, 368, 368, 368, 368, 368, 368, 368, 368, 368, 368, 368, 368, 368, 368, 368, 368, 368, 368, 368, 368, 368, 368, 368, 368, 368, 368, 
        368, 368, 368, 368, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 431, 431, 431, 431, 431, 431, 431, 431, 431, 431, 431, 431, 431, 431, 431, 431, 431, 431, 431, 431, 431, 431, 431, 431, 431, 431, 431, 431, 431, 431, 431, 431, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 497, 532, 532, 532, 532, 532, 532, 532, 532, 532, 532, 532, 532, 532, 532, 532, 532, 532, 532, 532, 532, 532, 532, 532, 532, 532, 532, 532, 532, 532, 532, 532, 
        532, 532, 532, 532, 532, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 568, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 605, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 643, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 683, 724, 724, 724, 724, 724, 724, 724, 
        724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 724, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 766, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 810, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 855, 
        855, 855, 855, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 902, 949};

    static final int maxFollowingSingleUnit = 4;

    static final int turnsBetweenEnemyBroadcast = 5;
    static final int turnsEnemyBroadcastValid = 5;

    static final int explorerMuckrakerLifetime = 200;

    static final int turnsSlandererLocValid = 5;
    static final int minRotationRadius = 15;
    static final int maxRotationRadius = 30;

    static final int minDistFromEnemy = 100;
    
    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    static Direction randomOrthogonalDirection() {
        return orthogonalDirs[(int) (Math.random() * orthogonalDirs.length)];
    }

    static Direction randomDiagonalDirection() {
        return orthogonalDirs[(int) (Math.random() * orthogonalDirs.length)];
    }

    static Direction[] getOrderedDirections(DirectionPreference pref) {
        Direction dir;
        switch(pref) {
            case ORTHOGONAL:
                dir = randomOrthogonalDirection();
                return new Direction[]{dir, dir.rotateLeft(), dir.rotateLeft().rotateLeft(), dir.opposite().rotateRight(), dir.opposite(),
                        dir.opposite().rotateLeft(), dir.rotateRight().rotateRight(), dir.rotateRight()};
            case DIAGONAL:
                dir = randomDiagonalDirection();
                return new Direction[]{dir, dir.rotateLeft(), dir.rotateLeft().rotateLeft(), dir.opposite().rotateRight(), dir.opposite(),
                        dir.opposite().rotateLeft(), dir.rotateRight().rotateRight(), dir.rotateRight()};
            case RANDOM:
            default:
                dir = randomDirection();
                return new Direction[]{dir, dir.rotateLeft(), dir.rotateLeft().rotateLeft(), dir.opposite().rotateRight(), dir.opposite(),
                        dir.opposite().rotateLeft(), dir.rotateRight().rotateRight(), dir.rotateRight()};
        }
    }

    static Direction rotateInSpinDirection(RotationDirection Rot, Direction dir) {
        switch(Rot) {
            case COUNTERCLOCKWISE:
                return dir.rotateLeft();
            case CLOCKWISE:
                return dir.rotateRight();
            default:
                return null;
        }
    }

    static Direction rotateOppositeSpinDirection(RotationDirection Rot, Direction dir) {
        switch(Rot) {
            case COUNTERCLOCKWISE:
                return dir.rotateRight();
            case CLOCKWISE:
                return dir.rotateLeft();
            default:
                return null;
        }
    }

    static RotationDirection switchSpinDirection(RotationDirection Rot) {
        switch(Rot) {
            case COUNTERCLOCKWISE:
                return RotationDirection.CLOCKWISE;
            case CLOCKWISE: 
                return RotationDirection.COUNTERCLOCKWISE;
            default:
                return null;
        }
    }
    
    static Direction turnLeft90(Direction dir) {
        return dir.rotateLeft().rotateLeft();
    }

    static Direction turnRight90(Direction dir) {
        return dir.rotateRight().rotateRight();
    }

    static Direction rightOrLeftTurn(RotationDirection Rot, Direction dir) {
        switch(Rot) {
            case COUNTERCLOCKWISE:
                return turnLeft90(dir);
            case CLOCKWISE: 
                return turnRight90(dir);
            default:
                return null;
        }

    }

    /**
     * Returns a random spawnable RobotType
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnableRobotType() {
        return spawnableRobot[(int) (Math.random() * spawnableRobot.length)];
    }

    static int getBestSlandererInfluence(int upperBound) {
        if(upperBound > maxSlandererInfluence) return maxSlandererInfluence;
        return bestSlandererCosts[upperBound];
    }
}