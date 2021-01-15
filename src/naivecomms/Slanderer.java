package naivecomms;

import battlecode.common.*;

import naivecomms.Util.*;
import naivecomms.Debug.*;

public class Slanderer extends Robot {
    static Direction main_direction;

    static Direction avgEnemyDir;
    static int avgEnemyDirTurn;
    
    public Slanderer(RobotController r) {
        super(r);
        subRobotType = Comms.SubRobotType.SLANDERER;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
    }

    public Slanderer(RobotController r,  MapLocation h) {
        this(r);
        home = h;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        
        if (rc.getType() != RobotType.SLANDERER) {
            changeTo = new ProtectorPoliticianNew(rc, home);
            return;
        }

        Debug.println(Debug.info, "I am a slanderer; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        // checkForAvgEnemyDir();

        MapLocation curr = rc.getLocation();

        if(main_direction == null) {
            main_direction = Util.randomDirection();
        }

        RobotInfo[] neutralECs = rc.senseNearbyRobots(sensorRadius, Team.NEUTRAL);
        RobotInfo minRobot = null;
        double minDistSquared = Integer.MAX_VALUE;
        for (RobotInfo robot : enemySensable) {
            double temp = curr.distanceSquaredTo(robot.getLocation());
            if (temp < minDistSquared) {
                minDistSquared = temp;
                minRobot = robot;
            }
        }
        // if (minRobot != null) {
        //     broadcastEnemyFound(minRobot.getLocation());
        // }
        
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
        RobotInfo closestSlanderer = null;
        int closestSlandDist = Integer.MAX_VALUE;
        MapLocation spawnKillDude = null;
        for (RobotInfo robot: friendlySensable) {
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                friendlyEC = robot;
            }
            int dist = robot.getLocation().distanceSquaredTo(curr);
            if (robot.getType() == RobotType.SLANDERER && dist < closestSlandDist) {
                closestSlanderer = robot;
                closestSlandDist = dist;
            }
            if (robot.getType() == RobotType.POLITICIAN && home.isAdjacentTo(robot.getLocation()) && rc.getEmpowerFactor(rc.getTeam(),0) > Util.spawnKillThreshold) {
                spawnKillDude = robot.getLocation();
            }
        }

        boolean moveBack = false;
        if (!curr.isWithinDistanceSquared(home, sensorRadius)) {
            moveBack = true;
        }

        double maxPass = 0;
        Direction maxDir = null;
        Direction tempDir = Util.randomDirection();
        double currPass;
        int i = 0;
        while (i < 8) {
            if (rc.onTheMap(curr.add(tempDir))) {
                currPass = rc.sensePassability(curr.add(tempDir));
                if (currPass > maxPass) {
                    maxPass = currPass;
                    maxDir = tempDir;
                }
            }
            i++;
            tempDir = tempDir.rotateRight();
        }
        main_direction = maxDir;

        // findClosestEnemyGlobal();

        if(closestKnownEnemy != null && curr.isWithinDistanceSquared(closestKnownEnemy, Util.minDistFromEnemy)) {
            main_direction = curr.directionTo(closestKnownEnemy).opposite();
            Debug.println(Debug.info, "Prioritizing moving away from enemies: " + main_direction);
        }
        // if (minRobot != null) {
        //     avgEnemyDir = curr.directionTo(minRobot.getLocation());
        //     avgEnemyDirTurn = rc.getRoundNum();
        //     int flag = Comms.getFlag(Comms.InformationCategory.AVG_ENEMY_DIR, avgEnemyDirTurn, avgEnemyDir);
        //     // Rebroadcast the flag right away
        //     if(rc.canSetFlag(flag)) {
        //         rc.setFlag(flag);
        //     }
            
        //     resetFlagOnNewTurn = false;
        //     main_direction = avgEnemyDir.opposite();
        //     Debug.println(Debug.info, "Prioritizing moving away from enemies: " + main_direction);
        // }
        // else if(avgEnemyDir != null) {
        //     // Direction[] candidateDirs = {avgEnemyDir.opposite(), avgEnemyDir.opposite().rotateLeft(), avgEnemyDir.opposite().rotateRight()};
        //     // main_direction = candidateDirs[(int)(Math.random() * candidateDirs.length)];
        //     main_direction = avgEnemyDir.opposite();
        //     Debug.println(Debug.info, "Prioritizing moving away from average enemy location: " + main_direction);
        // }
        else if (spawnKillDude != null) {
            main_direction = curr.directionTo(spawnKillDude).opposite();
        }
        else if (minNeutralRobot != null) {
            main_direction = curr.directionTo(minNeutralRobot.getLocation()).opposite(); 
            Debug.println(Debug.info, "Prioritizing moving away from neutrals.");
        }
        else if (friendlyEC != null) {
            main_direction = curr.directionTo(friendlyEC.getLocation()).opposite(); 
            Debug.println(Debug.info, "Prioritizing moving away from friendly ECs.");
        } else if (moveBack) {
            main_direction = curr.directionTo(home);
            Debug.println(Debug.info, "Prioritizing moving towards home.");
        } else if (closestSlanderer != null) {
            main_direction = curr.directionTo(closestSlanderer.getLocation());
            Debug.println(Debug.info, "Prioritizing moving towards slanderers.");
        } else {
            Debug.println(Debug.info, "Prioritizing passability.");
        }

        MapLocation target = rc.adjacentLocation(main_direction);
        if (rc.onTheMap(target)) {
            while (!tryMoveDest(main_direction) && rc.isReady()) {
                main_direction = Util.randomDirection();
            }
        }

        Debug.setIndicatorLine(Debug.pathfinding, curr, target, 100, 100, 255);
        
        // if(avgEnemyDir != null && rc.getRoundNum() > avgEnemyDirTurn + Util.turnsEnemyBroadcastValid) {
        //     avgEnemyDir = null;
        //     resetFlagOnNewTurn = true;
        // }

        if(broadcastECLocation());
        else if(broadcastEnemyLocalOrGlobal());
        // Old 50-300 turn stuff
        /* else {
            if(main_direction == null) {
                main_direction = Util.randomDirection();
            }

            RobotInfo minRobot = null;
            double minDistSquared = Integer.MAX_VALUE;
            for (RobotInfo robot : enemySensable) {
                double temp = curr.distanceSquaredTo(robot.getLocation());
                if (temp < minDistSquared) {
                    minDistSquared = temp;
                    minRobot = robot;
                }
            }
            if (minRobot != null) {
                broadcastEnemyFound(minRobot.getLocation());
            }

            RobotInfo friendlyEC = null;
            RobotInfo closestSlanderer = null;
            int closestSlandDist = Integer.MAX_VALUE;
            for (RobotInfo robot: friendlySensable) {
                if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    friendlyEC = robot;
                }
                int dist = robot.getLocation().distanceSquaredTo(curr);
                if (robot.getType() == RobotType.SLANDERER && dist < closestSlandDist) {
                    closestSlanderer = robot;
                    closestSlandDist = dist;
                }
            }

            double maxPass = 0;
            Direction maxDir = null;
            Direction tempDir = Util.randomDirection();
            double currPass;
            int i = 0;
            while (i < 8) {
                if (rc.onTheMap(curr.add(tempDir))) {
                    currPass = rc.sensePassability(curr.add(tempDir));
                    if (currPass > maxPass) {
                        maxPass = currPass;
                        maxDir = tempDir;
                    }
                }
                i++;
                tempDir = tempDir.rotateRight();
            }
            main_direction = maxDir;

            if (minRobot != null) {
                main_direction = curr.directionTo(minRobot.getLocation()).opposite();
                Debug.println(Debug.info, "Prioritizing moving away from enemies.");
            } else if (curr.isWithinDistanceSquared(home, 2 * sensorRadius)) {
                main_direction = curr.directionTo(home).opposite();
                Debug.println(Debug.info, "Prioritizing moving away from home.");
            } else if (awayDirection != null) {
                main_direction = awayDirection;
                Debug.println(Debug.info, "Prioritizing moving away using EC signal.");
            } else if (closestSlanderer != null) {
                main_direction = curr.directionTo(closestSlanderer.getLocation());
                Debug.println(Debug.info, "Prioritizing moving towards slanderers.");
            } else {
                Debug.println(Debug.info, "Prioritizing passability.");
            }

            MapLocation target = rc.adjacentLocation(main_direction);
            if (rc.onTheMap(target)) {
                while (!tryMove(main_direction) && rc.isReady()) {
                    main_direction = Util.randomDirection();
                }
            }

            broadcastECLocation();
        }*/
    }

    static void checkForAvgEnemyDir() throws GameActionException {
        for(RobotInfo robot : friendlySensable) {
            if(rc.canGetFlag(robot.getID())) {
                int flag = rc.getFlag(robot.getID());
                if(Comms.getIC(flag) == Comms.InformationCategory.AVG_ENEMY_DIR) {
                    avgEnemyDirTurn = Comms.getTurnCount(flag);
                    if(rc.getRoundNum() <= avgEnemyDirTurn + Util.turnsEnemyBroadcastValid) {
                        // Rebroadcast the flag right away
                        if(rc.canSetFlag(flag)) {
                            rc.setFlag(flag);
                        }
                        resetFlagOnNewTurn = false;
                        avgEnemyDir = Comms.getDirection(flag);
                    }
                }
            }
        }
    }
}