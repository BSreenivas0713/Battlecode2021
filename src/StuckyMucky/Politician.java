package StuckyMucky;
import battlecode.common.*;

import StuckyMucky.Util.*;

public class Politician extends Robot {
    static Direction main_direction;
    
    public Politician(RobotController r) {
        super(r);
    }

    public Politician(RobotController r, int currDx, int currDy) {
        super(r, currDx, currDy);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        if (Util.verbose) System.out.println("I am a normal politician; current influence: " + rc.getInfluence());
        if (Util.verbose) System.out.println("current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        Team enemy = rc.getTeam().opponent();
        int sensingRadius = rc.getType().sensorRadiusSquared;
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        RobotInfo[] neutrals = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        RobotInfo[] sensable = rc.senseNearbyRobots(sensingRadius, enemy);
        RobotInfo[] within6 = rc.senseNearbyRobots(6, rc.getTeam());
        RobotInfo[] friendlySensable = rc.senseNearbyRobots(sensingRadius, rc.getTeam());

        if(main_direction == null){
            main_direction = Util.randomDirection();
        }
        if ((attackable.length != 0 || neutrals.length != 0) && rc.canEmpower(actionRadius)) {
            //if (Util.verbose) System.out.println("empowering...");
            rc.empower(actionRadius);
            //if (Util.verbose) System.out.println("empowered");
            return;
        }
        RobotInfo powerful = null;
        int max_influence = 0;

        MapLocation ECWithinSensable = null;

        for (RobotInfo robot : sensable) {
            int currInfluence = robot.getInfluence();
            if (robot.getType() == RobotType.MUCKRAKER && currInfluence > max_influence) {
                powerful = robot;
                max_influence = currInfluence;
            }
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                ECWithinSensable = robot.getLocation();
            }
        }
        
        if (powerful != null) {
            Direction toMove = rc.getLocation().directionTo(powerful.getLocation());
            tryMoveDest(toMove);
        }

        boolean within2ofEC = false;
        for (RobotInfo robot : rc.senseNearbyRobots(2, rc.getTeam())) {
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                within2ofEC = true;
            }
        }

        for (RobotInfo robot : within6) {
            if (robot.getType() == RobotType.POLITICIAN && rc.getFlag(robot.getID()) == 1 && !within2ofEC) {
                if (Util.verbose) System.out.println("within radius 6");
                return;
            }
        }

        RobotInfo bestSlanderer = null;
        max_influence = 0;
        for (RobotInfo robot : friendlySensable) {
            if (robot.getType() == RobotType.POLITICIAN && rc.getFlag(robot.getID()) == 1 && robot.getInfluence() > max_influence) {
                if (Util.verbose) System.out.println("within sensing radius but not 6");
                if (ECWithinSensable == null || robot.getLocation().distanceSquaredTo(ECWithinSensable) > robot.getType().sensorRadiusSquared) {
                    max_influence = robot.getInfluence();
                }
                bestSlanderer = robot;
            }
        }

        if (bestSlanderer != null) {
            Direction toMove = rc.getLocation().directionTo(bestSlanderer.getLocation());
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