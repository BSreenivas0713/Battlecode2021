package musketeerplayerjan8;
import battlecode.common.*;

import musketeerplayerjan8.Util.*;

public class ExplorerPolitician extends Robot {
    static Direction main_direction;
    static boolean   toDetonate = false;

    
    public ExplorerPolitician(RobotController r) {
        super(r);
    }

    public ExplorerPolitician(RobotController r, int currDx, int currDy) {
        super(r, currDx, currDy);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        if (Util.verbose) System.out.println("I am a explorer politician; current influence: " + rc.getInfluence());
        if (Util.verbose) System.out.println("current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        Team enemy = rc.getTeam().opponent();
        int sensingRadius = rc.getType().sensorRadiusSquared;
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        RobotInfo[] neutrals = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        RobotInfo[] sensable = rc.senseNearbyRobots(sensingRadius, enemy);
        RobotInfo[] friendlySensable = rc.senseNearbyRobots(sensingRadius, rc.getTeam());

        if(main_direction == null){
            main_direction = Util.randomDirection();
        }

        
        int min_attackable_conviction = rc.getConviction() / 3;
        int attackable_conviction = 0;
        for (RobotInfo robot : attackable) {
            attackable_conviction += robot.getConviction();
        }

        if (attackable_conviction >= min_attackable_conviction && rc.canEmpower(actionRadius)) {
            //if (Util.verbose) System.out.println("empowering...");
            rc.empower(actionRadius);
            //if (Util.verbose) System.out.println("empowered");
            return;
        }

        RobotInfo powerful = null;
        int max_influence = rc.getConviction() / 3;
        for (RobotInfo robot : sensable) {
            int currInfluence = robot.getConviction();
            if (robot.getType() == RobotType.MUCKRAKER && currInfluence > max_influence) {
                powerful = robot;
                max_influence = currInfluence;
            }
        }
        
        if (powerful != null) {
            Direction toMove = rc.getLocation().directionTo(powerful.getLocation());
            tryMoveDest(toMove);
        }
        
        RobotInfo weakest = null;
        int min_influence = 0;
        for (RobotInfo robot : sensable) {
            int currInfluence = robot.getInfluence();
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && currInfluence < min_influence) {
                weakest = robot;
                min_influence = currInfluence;
            }
        }
        
        if (weakest != null) {
            Direction toMove = rc.getLocation().directionTo(weakest.getLocation());
            tryMoveDest(toMove);
        }

        while (!tryMove(main_direction) && rc.isReady()){
            main_direction = Util.randomDirection();
        }

        broadcastECLocation();
    }
}