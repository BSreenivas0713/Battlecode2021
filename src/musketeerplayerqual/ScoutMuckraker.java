package musketeerplayerqual;
import battlecode.common.*;

import musketeerplayerqual.Util.*;
import musketeerplayerqual.Debug.*;
import musketeerplayerqual.fast.FastIterableLocSet;

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
        Debug.println(Debug.info, "Direction of movement: " + main_direction);
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        
        if(main_direction == null){
            main_direction = Util.randomDirection();
        }

        RobotInfo robot;
        RobotInfo closestEnemy = null;
        double minDistSquared = Integer.MAX_VALUE;
        for(int i = enemySensable.length - 1; i >= 0; i--) {
            robot = enemySensable[i];
            double temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp < minDistSquared) {
                minDistSquared = temp;
                closestEnemy = robot;
            }
        }

        RobotInfo powerful = null;
        int bestInfluence = Integer.MIN_VALUE;
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
            Debug.println(Debug.info, "next loc on map. moving in direction: " + main_direction);
            tryMoveDest(main_direction);
        }
        else {
            Debug.println(Debug.info, "new location not on the map. switching to explorer");
            changeTo = new ExplorerMuckracker(rc, home);
        }

        if(broadcastECLocation());
        else if(closestEnemy != null && broadcastEnemyLocalOrGlobal(closestEnemy.getLocation()));
    }
}