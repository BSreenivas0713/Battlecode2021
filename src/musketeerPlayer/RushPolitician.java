package musketeerplayer;
import battlecode.common.*;

import musketeerplayer.Util.*;

public class RushPolitician extends Robot {
    static MapLocation enemy;
    static Direction main_direction;
    
    public RushPolitician(RobotController r, MapLocation enemyLoc) {
        super(r);
        enemy = enemyLoc;
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
        if ((attackable.length != 0 || neutrals.length != 0) && rc.canEmpower(actionRadius)) {
            //System.out.println("empowering...");
            rc.empower(actionRadius);
            //System.out.println("empowered");
            return;
        }

        while (!tryMove(main_direction) && rc.isReady()){
            main_direction = Util.randomDirection();
        }

        broadcastECLocation();
    }
}