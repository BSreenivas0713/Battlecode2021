package musketeerplayer;
import battlecode.common.*;

import musketeerplayer.Util.*;

public class Muckracker extends Robot {
    static Direction main_direction;
    static boolean muckraker_Found_EC;

    public Muckracker(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        System.out.println("I am a " + rc.getType() + "; current influence: " + rc.getInfluence() + "; current conviction: " + rc.getConviction());
        System.out.println("current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        if(main_direction == null){
            main_direction = Util.randomDirection();
        }
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo powerful = null;
        int bestInfluence = Integer.MIN_VALUE;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            int curr = robot.getInfluence();
            if (curr > bestInfluence && robot.type.canBeExposed()) {
                bestInfluence = curr;
                powerful = robot;
            }
        }
        
        for (RobotInfo robot : rc.senseNearbyRobots(2, enemy)) {
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER){
                muckraker_Found_EC = true;
            }
        }

        if (powerful != null) {
            if (rc.canExpose(powerful.location)) {
                rc.expose(powerful.location);
            }
        }
        int sensingRadius = rc.getType().sensorRadiusSquared;
        RobotInfo bestSlanderer = null;
        bestInfluence = Integer.MIN_VALUE;
        for (RobotInfo robot : rc.senseNearbyRobots(sensingRadius, enemy)) {
            if (robot.getType() == RobotType.SLANDERER) {
                int curr = robot.getInfluence();
                if (curr > bestInfluence) {
                    bestInfluence = curr;
                    bestSlanderer = robot;
                }
            }
        }
        if (bestSlanderer != null) {
            main_direction = rc.getLocation().directionTo(bestSlanderer.getLocation());
        }

        if(!muckraker_Found_EC){
            while (!tryMove(main_direction) && rc.isReady()){
                main_direction = Util.randomDirection();
            }
        }
         
        broadcastECLocation();
    }
}