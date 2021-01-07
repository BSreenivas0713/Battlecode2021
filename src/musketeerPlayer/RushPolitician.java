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
        int sensorRadius = rc.getType().sensorRadiusSquared;
        RobotInfo[] friendlies = rc.senseNearbyRobots(sensorRadius, rc.getTeam());
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        RobotInfo[] neutrals = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        
        if(toDetonate) {
            for(RobotInfo robot: neutrals) {
                if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER && rc.canEmpower(actionRadius)){
                    rc.empower(actionRadius);
                }
            }

        }
        else {
            for(RobotInfo robot: neutrals) {
                if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER){
                    int ECHealth = (int)(robot.getConviction() * 1.25);
                    for(RobotInfo friendlyRobot: friendlies) {
                        if(friendlyRobot.getType() == RobotType.POLITICIAN && Util.distanceSquared(friendlyRobot.getLocation(), robot.getLocation()) <= sensorRadius) {
                            ECHealth -= friendlyRobot.getInfluence();
                            ECHealth += 10;
                        }
                    }
                    if(ECHealth <= 0) {
                        setFlag(Comms.getFlag(Comms.InformationCategory.DETONATE, 0, 0));
                    }
                }
            }

            for(RobotInfo robot: friendlies) {
                if(robot.getType() == RobotType.POLITICIAN && Comms.getIC(rc.getFlag(robot.getID())) == Comms.InformationCategory.DETONATE) {
                    toDetonate = true;
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
        }

        for (RobotInfo robot : friendlies) {
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