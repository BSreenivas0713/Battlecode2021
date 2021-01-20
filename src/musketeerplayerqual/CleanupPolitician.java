package musketeerplayerqual;
import battlecode.common.*;

import musketeerplayerqual.Util.*;
import musketeerplayerqual.Debug.*;

public class CleanupPolitician extends Robot {
    static Direction main_direction;

    //TOCONSIDER: allow for these types to attack neutrals
    public CleanupPolitician(RobotController r) {
        super(r);
        subRobotType = Comms.SubRobotType.POL_EXPLORER;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
    }
    
    public CleanupPolitician(RobotController r, MapLocation h, int hID) {
        this(r);
        home = h;
        homeID = hID;
        friendlyECs.add(home, homeID);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Debug.println(Debug.info, "I am an cleanup politician; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        if(main_direction == null){
            main_direction = Util.randomDirection();
        }
        
        RobotInfo robot;
        MapLocation currLoc = rc.getLocation();
        int maxEnemyAttackableDistSquared = 0;
        MapLocation maxEnemyAtackableLoc = null;

        for(int i = enemyAttackable.length - 1; i >= 0; i--) {
            robot = enemyAttackable[i];
            int temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp > maxEnemyAttackableDistSquared) {
                maxEnemyAttackableDistSquared = temp;
                maxEnemyAtackableLoc = robot.getLocation();
            }
        }

        if (enemyAttackable.length != 0 && rc.canEmpower(maxEnemyAttackableDistSquared)) {
            Debug.println(Debug.info, "Empowered with radius: " + maxEnemyAttackableDistSquared);
            Debug.setIndicatorLine(Debug.info, rc.getLocation(), maxEnemyAtackableLoc, 255, 150, 50);
            rc.empower(maxEnemyAttackableDistSquared);
            return;
        }

        RobotInfo powerful = null;
        int max_influence = (rc.getConviction()-10) / 3;
        RobotInfo closestEnemy = null;
        Comms.EnemyType closestEnemyType = null;
        double minDistSquared = Integer.MAX_VALUE;

        for(int i = enemySensable.length - 1; i >= 0; i--) {
            robot = enemySensable[i];
            int currInfluence = robot.getConviction();
            if (currInfluence > max_influence) {
                powerful = robot;
                max_influence = currInfluence;
            }
            
            double temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp < minDistSquared) {
                minDistSquared = temp;
                closestEnemy = robot;
                if(robot.getType() == RobotType.MUCKRAKER) {
                    closestEnemyType = Comms.EnemyType.MUC;
                } else {
                    closestEnemyType = Comms.EnemyType.UNKNOWN;
                }
            }
        }
        
        if (powerful != null) {
            main_direction = rc.getLocation().directionTo(powerful.getLocation());
        }

        Direction[] orderedDirs = Util.getOrderedDirections(main_direction);
        for(Direction dir : orderedDirs) {
            tryMove(dir);
        }

        // This means that the first half of an EC-ID/EC-ID broadcast finished.
        if(needToBroadcastHomeEC && rc.getFlag(rc.getID()) == defaultFlag) { broadcastHomeEC(); }
        else if(broadcastECLocation());
        else if(closestEnemy != null && broadcastEnemyLocalOrGlobal(closestEnemy.getLocation(), closestEnemyType));
    }
}