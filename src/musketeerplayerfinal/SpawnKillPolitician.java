package musketeerplayerfinal;

import battlecode.common.*;
import musketeerplayerfinal.Debug.*;
import musketeerplayerfinal.Util.*;

public class SpawnKillPolitician extends Robot {
    static Direction main_direction;
    
    public SpawnKillPolitician(RobotController r) {
        super(r);
        subRobotType = Comms.SubRobotType.POL_SPAWNKILL;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
    }
    
    public SpawnKillPolitician(RobotController r, MapLocation h, int hID) {
        this(r);
        home = h;
        homeID = hID;
        friendlyECs.add(home, homeID);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Debug.println(Debug.info, "I am a spawn kill politician; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        RobotInfo robot;
        RobotInfo closestEnemy = null;
        Comms.EnemyType closestEnemyType = null;
        double minDistSquared = Integer.MAX_VALUE;
        MapLocation currLoc = rc.getLocation();
        for(int i = enemySensable.length - 1; i >= 0; i--) {
            robot = enemySensable[i];
            double temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp < minDistSquared) {
                minDistSquared = temp;
                closestEnemy = robot;
                if(robot.getType() == RobotType.MUCKRAKER) {
                    closestEnemyType = Comms.EnemyType.MUC;
                } else if(Util.isSlandererInfluence(robot.getInfluence())) {
                    closestEnemyType = Comms.EnemyType.SLA;   
                } else {
                    closestEnemyType = Comms.EnemyType.UNKNOWN;
                }
            }
        }

        if(rc.getEmpowerFactor(rc.getTeam(), 0) > Util.spawnKillThreshold) {
            int radius = Math.max(1, home.distanceSquaredTo(rc.getLocation()));
            if (rc.canEmpower(radius)) {
                Debug.println(Debug.info, "Empowered with radius: " + radius);
                Debug.setIndicatorLine(Debug.info, rc.getLocation(), home, 255, 150, 50);
                rc.empower(radius);
            }
        } else {
            changeTo = new LatticeProtector(rc, home, homeID);
        }
    }
}