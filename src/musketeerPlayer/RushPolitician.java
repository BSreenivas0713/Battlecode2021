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
    }

    public RushPolitician(RobotController r, MapLocation enemyLoc, boolean det) {
        super(r);
        enemyLocation = enemyLoc;
        toDetonate = det;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        if (Util.verbose) System.out.println("I am a rush politician; current influence: " + rc.getInfluence());
        if (Util.verbose) System.out.println("current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        if (Util.verbose) System.out.println("target map location: x:" + enemyLocation.x + ", y:" + enemyLocation.y);

        if(main_direction == null){
            main_direction = Util.randomDirection();
        }

        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        int sensorRadius = rc.getType().sensorRadiusSquared;
        RobotInfo[] friendlies = rc.senseNearbyRobots(sensorRadius, rc.getTeam());
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        RobotInfo[] neutrals = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        
        for(RobotInfo robot : attackable) {
            MapLocation loc = robot.getLocation();
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER && 
                enemyLocation.isWithinDistanceSquared(loc, 2) &&
                rc.canEmpower(actionRadius)) {
                //if (Util.verbose) System.out.println("empowering...");
                rc.empower(actionRadius);
                //if (Util.verbose) System.out.println("empowered");
                return;
            }
        }
        
        for(RobotInfo robot : neutrals) {
            MapLocation loc = robot.getLocation();
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER && 
                enemyLocation.isWithinDistanceSquared(loc, 2) &&
                rc.canEmpower(actionRadius)) {
                //if (Util.verbose) System.out.println("empowering...");
                rc.empower(actionRadius);
                //if (Util.verbose) System.out.println("empowered");
                return;
            }
        }

        for (RobotInfo robot : friendlies) {
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