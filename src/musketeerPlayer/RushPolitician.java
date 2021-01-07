package musketeerplayer;
import battlecode.common.*;

import musketeerplayer.Util.*;

public class RushPolitician extends Robot {
    static MapLocation enemyLocation;
    static Direction main_direction;
    
    public RushPolitician(RobotController r, MapLocation enemyLoc) {
        super(r);
        enemyLocation = enemyLoc;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        System.out.println("I am a rush politician; current influence: " + rc.getInfluence());
        System.out.println("current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        if(main_direction == null){
            main_direction = Util.randomDirection();
        }
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        RobotInfo[] neutrals = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        for(RobotInfo robot: neutrals) {
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER && rc.canEmpower(actionRadius)){
            //System.out.println("empowering...");
            rc.empower(actionRadius);
            //System.out.println("empowered");
            return;
            }
        }
        for(RobotInfo robot: attackable) {
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER && rc.canEmpower(actionRadius)){
            //System.out.println("empowering...");
            rc.empower(actionRadius);
            //System.out.println("empowered");
            return;
            }
        }
        main_direction = Util.findDirection(enemyLocation, rc.getLocation());
        tryMove(main_direction);
        broadcastECLocation();
    }
}