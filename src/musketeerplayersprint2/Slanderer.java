package musketeerplayersprint2;

import battlecode.common.*;

import musketeerplayersprint2.Util.*;
import musketeerplayersprint2.Debug.*;

public class Slanderer extends Robot {
    static Direction main_direction;
    static Direction awayDirection;
    
    public Slanderer(RobotController r) {
        super(r);
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.SLANDERER);
        awayDirection = null;
    }

    public Slanderer(RobotController r, Direction away) {
        super(r);
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.SLANDERER);
        awayDirection = away;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        
        if (rc.getType() != RobotType.SLANDERER) {
            if(turnCount % 2 == 0) {
                changeTo = new Politician(rc);
            } else {
                changeTo = new ExplorerPolitician(rc);
            }
            return;
        }

        Debug.println(Debug.info, "I am a slanderer; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        MapLocation curr = rc.getLocation();

        if (turnCount <= 50) {
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
            if (minRobot != null) {
                broadcastEnemyFound(minRobot.getLocation());
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

            boolean moveBack = false;
            if (!curr.isWithinDistanceSquared(home, 2 * sensorRadius)) {
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

            if (minRobot != null) {
                main_direction = curr.directionTo(minRobot.getLocation()).opposite();
            }
            else if (minNeutralRobot != null) {
                main_direction = curr.directionTo(minNeutralRobot.getLocation()).opposite(); 
            }
            else if (friendlyEC != null) {
                main_direction = curr.directionTo(friendlyEC.getLocation()).opposite(); 
            } else if (moveBack == true) {
                main_direction = curr.directionTo(home);
            } else if (awayDirection != null) {
                main_direction = awayDirection;
            } else if (closestSlanderer != null) {
                main_direction = curr.directionTo(closestSlanderer.getLocation());
            }

            MapLocation target = rc.adjacentLocation(main_direction);
            if (rc.onTheMap(target)) {
                while (!tryMove(main_direction) && rc.isReady()) {
                    main_direction = Util.randomDirection();
                }
            }

            broadcastECLocation();
        } else {
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
            } else if (curr.isWithinDistanceSquared(home, 2 * sensorRadius)) {
                main_direction = curr.directionTo(home).opposite();
            } else if (awayDirection != null) {
                main_direction = awayDirection;
            } else if (closestSlanderer != null) {
                main_direction = curr.directionTo(closestSlanderer.getLocation());
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
}