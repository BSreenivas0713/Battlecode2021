package naivebuffs;

import battlecode.common.*;

import naivebuffs.Util.*;
import naivebuffs.Debug.*;

public class Slanderer extends Robot {
    static Direction main_direction;

    static Direction avgEnemyDir;
    static int avgEnemyDirTurn;
    
    public Slanderer(RobotController r) {
        super(r);
        subRobotType = Comms.SubRobotType.SLANDERER;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
    }

    public Slanderer(RobotController r,  MapLocation h, int hID) {
        this(r);
        home = h;
        homeID = hID;
        friendlyECs.add(home, homeID);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        
        if (rc.getType() != RobotType.SLANDERER) {
            changeTo = new LatticeProtector(rc, home, homeID);
            return;
        }

        Debug.println(Debug.info, "I am a slanderer; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        // checkForAvgEnemyDir();

        MapLocation curr = rc.getLocation();

        if(main_direction == null) {
            main_direction = Util.randomDirection();
        }

        RobotInfo robot;
        RobotInfo closestEnemy = null;
        Comms.EnemyType closestEnemyType = null;
        double minDistSquared = Integer.MAX_VALUE;
        double temp;

        for(int i = enemySensable.length - 1; i >= 0; i--) {
            robot = enemySensable[i];
            temp = curr.distanceSquaredTo(robot.getLocation());
            if (temp < minDistSquared) {
                minDistSquared = temp;
                closestEnemy = robot;
                if(robot.getType() == RobotType.MUCKRAKER) {
                    closestEnemyType = Comms.EnemyType.MUC;
                } else {
                    closestEnemyType = Comms.EnemyType.UNKNOWN;
                }
            }
        }

        boolean foundOwnEnemy = closestEnemy != null;
        
        RobotInfo minNeutralRobot = null;
        double minNeutralDistSquared = Integer.MAX_VALUE;
        for(int i = neutralSensable.length - 1; i >= 0; i--) {
            robot = neutralSensable[i];
            temp = curr.distanceSquaredTo(robot.getLocation());
            if (temp < minNeutralDistSquared) {
                minNeutralDistSquared = temp;
                minNeutralRobot = robot;
            }
        }
        
        RobotInfo friendlyEC = null;
        RobotInfo closestSlanderer = null;
        int closestSlandDist = Integer.MAX_VALUE;
        MapLocation spawnKillDude = null;
        Direction otherSlaFleeingDir = null;
        MapLocation loc = null;
        int id;
        int flag;
        int dist;
        for(int i = friendlySensable.length - 1; i >= 0; i--) {
            robot = friendlySensable[i];
            loc = robot.getLocation();
            id = robot.getID();

            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                friendlyEC = robot;
            }

            if (robot.getType() == RobotType.POLITICIAN && home.isAdjacentTo(loc) && rc.getEmpowerFactor(rc.getTeam(),0) > Util.spawnKillThreshold) {
                spawnKillDude = loc;
            }
            
            if(rc.canGetFlag(id)) {
                flag = rc.getFlag(id);
                dist = loc.distanceSquaredTo(curr);
                
                if (Comms.isSubRobotType(flag, subRobotType) && dist < closestSlandDist) {
                    closestSlanderer = robot;
                    closestSlandDist = dist;
                }

                if(Comms.getIC(flag) == Comms.InformationCategory.CLOSEST_ENEMY_OR_FLEEING) {
                    switch(Comms.getSubCEOF(flag)) {
                        case FLEEING:
                            otherSlaFleeingDir = Comms.getDirection(flag);
                            break;
                        default:
                            if(!foundOwnEnemy) {
                                int[] enemyDxDyFromRobot = Comms.getDxDy(flag);
    
                                MapLocation enemyLoc = new MapLocation(enemyDxDyFromRobot[0] + loc.x - Util.dOffset, 
                                                                        enemyDxDyFromRobot[1] + loc.y - Util.dOffset);
    
                                temp = rc.getLocation().distanceSquaredTo(enemyLoc);
                                if (temp < minDistSquared) {
                                    minDistSquared = temp;
                                    closestEnemy = robot;
                                    closestEnemyType = Comms.EnemyType.UNKNOWN;
                                }
                            }
                            break;
                    }
                }
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

        if(closestEnemy != null && curr.isWithinDistanceSquared(closestEnemy.getLocation(), Util.minDistFromEnemy)) {
            main_direction = curr.directionTo(closestEnemy.getLocation()).opposite();
            // flag = Comms.getFlag(Comms.InformationCategory.SLA_FLEEING, main_direction.ordinal());
            flag = Comms.getFlag(Comms.InformationCategory.CLOSEST_ENEMY_OR_FLEEING, 
                                Comms.ClosestEnemyOrFleeing.FLEEING, 
                                0, main_direction.ordinal());
            setFlag(flag);
            Debug.println(Debug.info, "Prioritizing moving away from enemies: " + main_direction);
        }
        else if(otherSlaFleeingDir != null) {
            // Direction[] candidateDirs = {avgEnemyDir.opposite(), avgEnemyDir.opposite().rotateLeft(), avgEnemyDir.opposite().rotateRight()};
            // main_direction = candidateDirs[(int)(Math.random() * candidateDirs.length)];
            main_direction = otherSlaFleeingDir;
            Debug.println(Debug.info, "Prioritizing joining a fleeing slanderer " + main_direction);
        }
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
        tryMoveDest(main_direction);

        Debug.setIndicatorLine(Debug.pathfinding, curr, target, 100, 100, 255);
        
        // if(avgEnemyDir != null && rc.getRoundNum() > avgEnemyDirTurn + Util.turnsEnemyBroadcastValid) {
        //     avgEnemyDir = null;
        //     resetFlagOnNewTurn = true;
        // }

        // This means that the first half of an EC-ID/EC-ID broadcast finished.
        if(needToBroadcastHomeEC && rc.getFlag(rc.getID()) == defaultFlag) { broadcastHomeEC(); }
        else if(broadcastECLocation());
        else if(foundOwnEnemy && broadcastEnemyLocalOrGlobal(closestEnemy.getLocation(), closestEnemyType));
    }
}