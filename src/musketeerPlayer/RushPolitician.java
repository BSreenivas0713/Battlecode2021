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
        System.out.println("target map location: x:" + enemyLocation.x + ", y:" + enemyLocation.y);

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
        int sensorRadius = rc.getType().sensorRadiusSquared;
        RobotInfo[] sensable = rc.senseNearbyRobots(sensorRadius, rc.getTeam());
        for (RobotInfo robot : sensable) {
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && enemyLocation.equals(robot.getLocation())) {
                changeTo = new ExplorerPolitician(rc, dx, dy);
                if (rc.getRoundNum() % 3 == 0) {
                    changeTo = new Politician(rc, dx, dy);
                }
                return;
            }
        }

        main_direction = Util.findDirection(enemyLocation, rc.getLocation());
        System.out.println("returned direction from util: " + main_direction);
        int num_direction = 8;
        while(num_direction != 0) {
            System.out.println("target main direction: " + main_direction);
            if(tryMove(main_direction) || !rc.isReady()) {
                break;
            }
            main_direction = Util.pathFinder.get(main_direction);
            System.out.println("new main direction: " + main_direction);
            num_direction--;
        }
    }
}