package musketeerplayersprint;

import battlecode.common.*;

import musketeerplayersprint.Util.*;
import musketeerplayersprint.Debug.*;

public class RushPolitician extends Robot {
    static MapLocation enemyLocation;
    static Direction main_direction;
    static boolean   toDetonate = false;
    
    public RushPolitician(RobotController r, MapLocation enemyLoc) {
        super(r);
        enemyLocation = enemyLoc;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.POL_RUSH);
    }

    public RushPolitician(RobotController r, MapLocation enemyLoc, boolean det) {
        super(r);
        enemyLocation = enemyLoc;
        toDetonate = det;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.POL_RUSH);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Debug.println(Debug.info, "I am a rush politician; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        Debug.println(Debug.info, "target map location: x:" + enemyLocation.x + ", y:" + enemyLocation.y);

        if(main_direction == null){
            main_direction = Util.randomDirection();
        }

        RobotInfo[] neutrals = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        
        for(RobotInfo robot : enemyAttackable) {
            MapLocation loc = robot.getLocation();
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER && 
                enemyLocation.isWithinDistanceSquared(loc, 8) &&
                rc.canEmpower(actionRadius)) {
                rc.empower(actionRadius);
                return;
            }
        }
        
        for(RobotInfo robot : neutrals) {
            MapLocation loc = robot.getLocation();
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER && 
                enemyLocation.isWithinDistanceSquared(loc, 8) &&
                rc.canEmpower(actionRadius)) {
                rc.empower(actionRadius);
                return;
            }
        }

        for (RobotInfo robot : friendlySensable) {
            MapLocation loc = robot.getLocation();
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && enemyLocation.isWithinDistanceSquared(loc, 2) &&
                loc.isWithinDistanceSquared(rc.getLocation(), actionRadius)) {
                changeTo = new GolemPolitician(rc);
                return;
            }
        }

        main_direction = rc.getLocation().directionTo(enemyLocation);
        if(!rc.getLocation().isWithinDistanceSquared(enemyLocation, 2)){
            tryMoveDest(main_direction);
        }
        
        rc.setIndicatorLine(rc.getLocation(), enemyLocation, 255, 150, 50);
    }
}