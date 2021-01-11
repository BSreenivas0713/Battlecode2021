package sprintfirstversion;
import battlecode.common.*;

import sprintfirstversion.Util.*;

public class CleanupPolitician extends Robot {
    static Direction main_direction;
    static boolean   toDetonate = false;

    //TOCONSIDER: allow for these types to attack neutrals
    public CleanupPolitician(RobotController r) {
        super(r);
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.POL_EXPLORER);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Util.vPrintln("I am an cleanup politician; current influence: " + rc.getInfluence());
        Util.vPrintln("current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        if(main_direction == null){
            main_direction = Util.randomDirection();
        }

        if (enemyAttackable.length != 0 && rc.canEmpower(actionRadius)) {
            //Util.vPrintln("empowering...");
            rc.empower(actionRadius);
            //Util.vPrintln("empowered");
            return;
        }

        RobotInfo powerful = null;
        int max_influence = (rc.getConviction()-10) / 3;
        for (RobotInfo robot : enemySensable) {
            int currInfluence = robot.getConviction();
            if (currInfluence > max_influence) {
                powerful = robot;
                max_influence = currInfluence;
            }
        }
        
        if (powerful != null) {
            Direction toMove = rc.getLocation().directionTo(powerful.getLocation());
            tryMoveDest(toMove);
        }

        while (!tryMove(main_direction) && rc.isReady()){
            main_direction = Util.randomDirection();
        }
    }
}