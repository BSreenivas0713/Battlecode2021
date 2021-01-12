package musketeerplayersprint;

import battlecode.common.*;

import musketeerplayersprint.Util.*;
import musketeerplayersprint.Debug.*;

public class RushPolitician extends Robot {
    static MapLocation enemyLocation;
    static Direction main_direction;
    static boolean   toDetonate = false;
    static int moveSemaphore;
    
    public RushPolitician(RobotController r, MapLocation enemyLoc) {
        super(r);
        enemyLocation = enemyLoc;
        moveSemaphore = 5;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.POL_RUSH);
    }

    public RushPolitician(RobotController r, MapLocation enemyLoc, boolean det) {
        super(r);
        enemyLocation = enemyLoc;
        toDetonate = det;
        moveSemaphore = 5;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.POL_RUSH);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Debug.println(Debug.info, "I am a rush politician; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        Debug.println(Debug.info, "target map location: x:" + enemyLocation.x + ", y:" + enemyLocation.y);
        
        MapLocation currLoc = rc.getLocation();
        RobotInfo[] neutrals = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        boolean targetFound = false;
        
        for(RobotInfo robot : enemyAttackable) {
            MapLocation loc = robot.getLocation();
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER && 
                enemyLocation.isWithinDistanceSquared(loc, 8)) {
                targetFound = true;
                int dist = currLoc.distanceSquaredTo(loc);
                if (rc.canEmpower(dist) && (moveSemaphore == 0 || currLoc.isAdjacentTo(loc))) {
                    rc.empower(dist);
                }
                return;
            }
        }
        
        for(RobotInfo robot : neutrals) {
            MapLocation loc = robot.getLocation();
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER && 
                enemyLocation.isWithinDistanceSquared(loc, 8)) {
                targetFound = true;
                int dist = currLoc.distanceSquaredTo(loc);
                if (rc.canEmpower(dist) && (moveSemaphore == 0 || currLoc.isAdjacentTo(loc))) {
                    rc.empower(dist);
                }
                return;
            }
        }

        if (!targetFound) {
            for (RobotInfo robot : friendlySensable) {
                MapLocation loc = robot.getLocation();
                if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && enemyLocation.isWithinDistanceSquared(loc, 8) &&
                    loc.isWithinDistanceSquared(currLoc, actionRadius)) {
                    changeTo = new GolemPolitician(rc);
                    return;
                }
            }
        }

        main_direction = rc.getLocation().directionTo(enemyLocation);
        if (currLoc.isAdjacentTo(enemyLocation)) {
            moveSemaphore = 0;
        } else if (currLoc.distanceSquaredTo(enemyLocation) <= actionRadius) {
            if (tryMove(main_direction)) {
                moveSemaphore = 5;
            } else {
                moveSemaphore--;
            }
        } else {
            tryMoveDest(main_direction);
        }
        
        Debug.setIndicatorLine(rc.getLocation(), enemyLocation, 255, 150, 50);
    }
}