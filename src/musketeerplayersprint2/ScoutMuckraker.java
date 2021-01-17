package musketeerplayersprint2;
import battlecode.common.*;

import musketeerplayersprint2.Util.*;
import musketeerplayersprint2.Debug.*;
import musketeerplayersprint2.fast.FastIterableLocSet;

public class ScoutMuckraker extends Robot {
    static Direction main_direction;

    public ScoutMuckraker(RobotController r, Direction dir) {
        super(r);
        main_direction = dir;
        subRobotType = Comms.SubRobotType.MUC_SCOUT;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
    }

    public ScoutMuckraker(RobotController r, Direction dir, MapLocation h) {
        this(r, dir);
        home = h;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        MapLocation currLoc = rc.getLocation();

        Debug.println(Debug.info, "I am a scout mucker; current influence: " + rc.getInfluence() + "; current conviction: " + rc.getConviction());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        
        if(main_direction == null){
            main_direction = Util.randomDirection();
        }

        RobotInfo powerful = null;
        int bestInfluence = Integer.MIN_VALUE;
        RobotInfo robot;
        for(int i = enemyAttackable.length - 1; i >= 0; i--) {
            robot = enemyAttackable[i];
            int curr = robot.getInfluence();
            if (curr > bestInfluence && robot.type.canBeExposed()) {
                bestInfluence = curr;
                powerful = robot;
            }
        }

        if (powerful != null) {
            if (rc.canExpose(powerful.location)) {
                rc.expose(powerful.location);
            }
        }

        if (rc.onTheMap(currLoc.add(main_direction))) {
            tryMoveDest(main_direction);
        }
        else {
            changeTo = new ExplorerMuckracker(rc, home);
        }

        broadcastECLocation();
    }
}