package sprintfirstversion;
import battlecode.common.*;

import sprintfirstversion.Util.*;

public class Muckracker extends Robot {
    static Direction main_direction;

    public Muckracker(RobotController r) {
        super(r);
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.MUCKRAKER);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Util.vPrintln("I am a " + rc.getType() + "; current influence: " + rc.getInfluence() + "; current conviction: " + rc.getConviction());
        Util.vPrintln("current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        if(main_direction == null){
            main_direction = Util.randomDirection();
        }
        RobotInfo powerful = null;
        int bestInfluence = Integer.MIN_VALUE;
        for (RobotInfo robot : enemyAttackable) {
            int curr = robot.getInfluence();
            if (curr > bestInfluence && robot.type.canBeExposed()) {
                bestInfluence = curr;
                powerful = robot;
            }
        }
        
        boolean muckraker_Found_EC = false;
        for (RobotInfo robot : rc.senseNearbyRobots(2, enemy)) {
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER){
                muckraker_Found_EC = true;
            }
        }
        // int ECInfluence = Integer.MAX_VALUE;
        // if() {
        // if(rc.senseRobotAtLocation(home) == RobotInfo.ENLIGHTENMENT_CENTER) {
        //     ECInfluence = home.
        // }
        // }

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
                int curr = robot.getConviction();
                if (curr > bestInfluence) {
                    bestInfluence = curr;
                    bestSlanderer = robot;
                }
            }
            // if (robot.getType() == RobotType.POLITICIAN && (robot.getConviction() >= 100 || ))
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