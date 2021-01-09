package musketeerplayer;
import battlecode.common.*;

import musketeerplayer.Util.*;

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

        Util.vPrintln("I am a rush politician; current influence: " + rc.getInfluence());
        Util.vPrintln("current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        Util.vPrintln("target map location: x:" + enemyLocation.x + ", y:" + enemyLocation.y);

        if(main_direction == null){
            main_direction = Util.randomDirection();
        }

        RobotInfo[] neutrals = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        
        for(RobotInfo robot : enemyAttackable) {
            MapLocation loc = robot.getLocation();
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER && 
                enemyLocation.isWithinDistanceSquared(loc, 2) &&
                rc.canEmpower(actionRadius)) {
                //Util.vPrintln("empowering...");
                rc.empower(actionRadius);
                //Util.vPrintln("empowered");
                return;
            }
        }
        
        for(RobotInfo robot : neutrals) {
            MapLocation loc = robot.getLocation();
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER && 
                enemyLocation.isWithinDistanceSquared(loc, 2) &&
                rc.canEmpower(actionRadius)) {
                //Util.vPrintln("empowering...");
                rc.empower(actionRadius);
                //Util.vPrintln("empowered");
                return;
            }
        }

        for (RobotInfo robot : friendlySensable) {
            MapLocation loc = robot.getLocation();
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && enemyLocation.isWithinDistanceSquared(loc, 2) &&
                loc.isWithinDistanceSquared(rc.getLocation(), actionRadius)) {
                changeTo = new DefenderPolitician(rc, dx, dy);
                return;
            }
        }

        main_direction = rc.getLocation().directionTo(enemyLocation);
        if(!rc.getLocation().isWithinDistanceSquared(enemyLocation, 2)){
            tryMoveDest(main_direction);
        }
    }
}