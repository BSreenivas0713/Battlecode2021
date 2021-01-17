package ecs;

import battlecode.common.*;

import ecs.Util.*;
import ecs.Debug.*;

public class DiagonalSlanderer extends Robot {
    static Direction main_direction;
    
    public DiagonalSlanderer(RobotController r) {
        super(r);
        subRobotType = Comms.SubRobotType.SLANDERER;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
    }

    public DiagonalSlanderer(RobotController r,  MapLocation h) {
        this(r);
        home = h;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        
        if (rc.getType() != RobotType.SLANDERER) {
            changeTo = new LatticeProtector(rc, home);
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
        RobotInfo minRobot = null;
        double minDistSquared = Integer.MAX_VALUE;
        double temp;

        for(int i = enemySensable.length - 1; i >= 0; i--) {
            robot = enemySensable[i];
            temp = curr.distanceSquaredTo(robot.getLocation());
            if (temp < minDistSquared) {
                minDistSquared = temp;
                minRobot = robot;
            }
        }
        
        RobotInfo closestSlanderer = null;
        int closestSlandDist = Integer.MAX_VALUE;
        MapLocation spawnKillDude = null;
        Direction otherSlaFleeingDir = null;
        int flag;
        int dist;
        for(int i = friendlySensable.length - 1; i >= 0; i--) {
            robot = friendlySensable[i];
            if (robot.getType() == RobotType.POLITICIAN && home.isAdjacentTo(robot.getLocation()) && rc.getEmpowerFactor(rc.getTeam(),0) > Util.spawnKillThreshold) {
                spawnKillDude = robot.getLocation();
            }
            
            if(rc.canGetFlag(robot.getID())) {
                flag = rc.getFlag(robot.getID());
                dist = robot.getLocation().distanceSquaredTo(curr);
                
                if (Comms.isSubRobotType(flag, subRobotType) && dist < closestSlandDist) {
                    closestSlanderer = robot;
                    closestSlandDist = dist;
                }
                if(Comms.getIC(flag) == Comms.InformationCategory.SLA_FLEEING) {
                    otherSlaFleeingDir = Comms.getDirection(flag);
                }
            }
        }
        int ecRadius = RobotType.ENLIGHTENMENT_CENTER.sensorRadiusSquared;
        boolean moveBack = false;
        if (!curr.isWithinDistanceSquared(home, ecRadius)) {
            moveBack = true;
        }
        MapLocation latticeLoc;

        if (closestKnownEnemy != null && curr.isWithinDistanceSquared(closestKnownEnemy, Util.minDistFromEnemy)) {
            main_direction = curr.directionTo(closestKnownEnemy).opposite();
            flag = Comms.getFlag(Comms.InformationCategory.SLA_FLEEING, main_direction.ordinal());
            setFlag(flag);
            Debug.println(Debug.info, "Prioritizing moving away from enemies: " + main_direction);
        }
        else if(otherSlaFleeingDir != null) {
            // Direction[] candidateDirs = {avgEnemyDir.opposite(), avgEnemyDir.opposite().rotateLeft(), avgEnemyDir.opposite().rotateRight()};
            // main_direction = candidateDirs[(int)(Math.random() * candidateDirs.length)];
            main_direction = otherSlaFleeingDir;
            Debug.println(Debug.info, "Prioritizing joining a fleeing slanderer " + main_direction);
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
        }
        while (rc.isReady() && !tryMoveDest(main_direction)) {
            main_direction = Util.randomDirection();
        }
        MapLocation target = rc.adjacentLocation(main_direction);

        Debug.setIndicatorLine(Debug.pathfinding, curr, target, 100, 100, 255);
        
        // if(avgEnemyDir != null && rc.getRoundNum() > avgEnemyDirTurn + Util.turnsEnemyBroadcastValid) {
        //     avgEnemyDir = null;
        //     resetFlagOnNewTurn = true;
        // }

        if(broadcastECLocation());
        else if(broadcastEnemyLocalOrGlobal());
    }
}