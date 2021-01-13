package auxMusketeerPlayer;
import battlecode.common.*;

import auxMusketeerPlayer.Util.*;

public class Muckracker extends Robot {
    static Direction main_direction;
    static boolean muckraker_Found_EC;

    public Muckracker(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

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
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER){
                muckraker_Found_EC = true;
            }
        }
        if (powerful != null) {
            if (rc.canExpose(powerful.location)) {
                rc.expose(powerful.location);
            }
        }

        if(!muckraker_Found_EC){
            while (!tryMove(main_direction) && rc.isReady()){
                main_direction = Util.randomDirection();
            }
        }
            //System.out.println("I moved!");
    }
}