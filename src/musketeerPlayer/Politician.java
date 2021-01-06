package musketeerplayer;
import battlecode.common.*;

import musketeerplayer.Util.*;

public class Politician extends Robot {
    static Direction main_direction;
    
    public Politician(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        if(main_direction == null){
            main_direction = Util.randomDirection();
        }
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
            //System.out.println("empowering...");
            rc.empower(actionRadius);
            //System.out.println("empowered");
            return;
        }

        int sensingRadius = rc.getType().sensorRadiusSquared;
        RobotInfo[] sensable = rc.senseNearbyRobots(sensingRadius, enemy);
        RobotInfo powerful = null;
        int max_influence = 0;
        for (RobotInfo robot : sensable) {
            int currInfluence = robot.getInfluence();
            if (robot.getType() == RobotType.MUCKRAKER && currInfluence > max_influence) {
                powerful = robot;
                max_influence = currInfluence;
            }
        }
        
        if (powerful != null) {
            Direction toMove = Util.findDirection(powerful.getLocation(), rc.getLocation());
            tryMove(toMove);
        }

        while (!tryMove(main_direction) && rc.isReady()){
            main_direction = Util.randomDirection();
        }
    }
}