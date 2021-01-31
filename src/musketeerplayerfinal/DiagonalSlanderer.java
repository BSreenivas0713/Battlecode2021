package musketeerplayerfinal;

import battlecode.common.*;
import musketeerplayerfinal.Debug.*;
import musketeerplayerfinal.Util.*;

public class DiagonalSlanderer extends Robot {
    static Direction main_direction;
    static int ecRadius = RobotType.ENLIGHTENMENT_CENTER.sensorRadiusSquared;
    
    public DiagonalSlanderer(RobotController r) {
        super(r);
        subRobotType = Comms.SubRobotType.SLANDERER;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
    }

    public DiagonalSlanderer(RobotController r, MapLocation h, int hID) {
        this(r);
        home = h;
        homeID = hID;
        friendlyECs.add(home, homeID);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        
        if (rc.getType() != RobotType.SLANDERER) {
            if(rc.getConviction() > 100) {
                changeTo = new RushPolitician(rc, null, home, homeID);
            } else {
                changeTo = new LatticeProtector(rc, home, homeID);
            }
            return;
        }

        Debug.println(Debug.info, "I am a slanderer; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        // checkForAvgEnemyDir();

        MapLocation curr = rc.getLocation();

        RobotInfo robot;
        RobotInfo closestEnemy = null;
        Comms.EnemyType closestEnemyType = null;
        double minDistSquared = Integer.MAX_VALUE;
        double temp;

        for(int i = enemySensable.length - 1; i >= 0; i--) {
            robot = enemySensable[i];
            temp = curr.distanceSquaredTo(robot.getLocation());
            if (temp < minDistSquared && robot.getType() == RobotType.MUCKRAKER) {
                minDistSquared = temp;
                closestEnemy = robot;
                closestEnemyType = Comms.EnemyType.MUC;
                // if(robot.getType() == RobotType.MUCKRAKER) {
                //     closestEnemyType = Comms.EnemyType.MUC;
                // } else if(Util.isSlandererInfluence(robot.getInfluence())) {
                //     closestEnemyType = Comms.EnemyType.SLA;   
                // } else {
                //     closestEnemyType = Comms.EnemyType.UNKNOWN;
                // }
            }
        }

        boolean foundOwnEnemy = closestEnemy != null;
        
        RobotInfo closestSlanderer = null;
        int closestSlandDist = Integer.MAX_VALUE;
        MapLocation spawnKillDude = null;
        Direction otherSlaFleeingDir = null;
        MapLocation loc = null;
        MapLocation enemyLoc = null;
        int[] enemyDxDyFromRobot;
        int id;
        int flag;
        int dist;
        RobotInfo disperseBot = null;
        for(int i = friendlySensable.length - 1; i >= 0; i--) {
            robot = friendlySensable[i];
            loc = robot.getLocation();
            id = robot.getID();
            
            if(rc.canGetFlag(id)) {
                flag = rc.getFlag(id);
                dist = loc.distanceSquaredTo(curr);
                
                if (Comms.isSubRobotType(flag, subRobotType) && dist < closestSlandDist) {
                    closestSlanderer = robot;
                    closestSlandDist = dist;
                }
                else if (Comms.isSubRobotType(flag, Comms.SubRobotType.POL_SPAWNKILL)) {
                    spawnKillDude = loc;
                } else if(Comms.isSubRobotType(flag, Comms.SubRobotType.POL_HEAD_READY)) {
                    disperseBot = robot;
                }

                if(Comms.getIC(flag) == Comms.InformationCategory.CLOSEST_ENEMY_OR_FLEEING) {
                    switch(Comms.getSubCEOF(flag)) {
                        case FLEEING:
                            otherSlaFleeingDir = Comms.getDirection(flag);
                            break;
                        default:
                            if(!foundOwnEnemy && Comms.getEnemyType(flag) == Comms.EnemyType.MUC) {
                                enemyDxDyFromRobot = Comms.getDxDy(flag);
    
                                enemyLoc = new MapLocation(enemyDxDyFromRobot[0] + loc.x - Util.dOffset, 
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
        if (!curr.isWithinDistanceSquared(home, ecRadius)) {
            moveBack = true;
        }
        MapLocation latticeLoc;

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
        } else if (disperseBot != null && curr.isAdjacentTo(disperseBot.getLocation())) {
            main_direction = curr.directionTo(disperseBot.getLocation()).opposite();
            tryMoveDest(main_direction);
            Debug.println(Debug.info, "Dispersing to avoid rusher.");
        } else if (spawnKillDude != null) {
            main_direction = curr.directionTo(spawnKillDude).opposite();
        } else if (moveBack) {
            main_direction = curr.directionTo(home);
            Debug.println(Debug.info, "Prioritizing moving towards home.");
        } else if (curr.isAdjacentTo(home)) {
            main_direction = curr.directionTo(home).opposite();
        } else if (closestSlanderer != null) {
            main_direction = curr.directionTo(closestSlanderer.getLocation());
            latticeLoc = rc.adjacentLocation(main_direction);
            if (closestSlandDist == 1) {
                main_direction = main_direction.rotateRight().rotateRight();
                latticeLoc = rc.adjacentLocation(main_direction);
                if (rc.isReady() && latticeLoc.distanceSquaredTo(home) > 2 && latticeLoc.distanceSquaredTo(home) <= ecRadius) {
                    tryMove(main_direction);
                }
                main_direction = main_direction.opposite();
                latticeLoc = rc.adjacentLocation(main_direction);
                if (rc.isReady() && latticeLoc.distanceSquaredTo(home) > 2 && latticeLoc.distanceSquaredTo(home) <= ecRadius) {
                    tryMove(main_direction.opposite());
                }
            } else if (closestSlandDist > 2) {
                if (rc.isReady() && latticeLoc.distanceSquaredTo(home) > 2 && latticeLoc.distanceSquaredTo(home) <= ecRadius) {
                    tryMoveDest(main_direction);
                }
            } else {
                return;
            }
        } else {
            main_direction = Direction.CENTER;
        }

        if(main_direction != Direction.CENTER)
            tryMoveDest(main_direction);
        
        MapLocation target = rc.adjacentLocation(main_direction);

        Debug.setIndicatorLine(Debug.pathfinding, curr, target, 100, 100, 255);
        // if(avgEnemyDir != null && rc.getRoundNum() > avgEnemyDirTurn + Util.turnsEnemyBroadcastValid) {
        //     avgEnemyDir = null;
        //     resetFlagOnNewTurn = true;
        // }

        if(foundOwnEnemy && broadcastEnemyLocalOrGlobal(closestEnemy.getLocation(), closestEnemyType));
    }
}