package musketeerplayersprint2;
import battlecode.common.*;

import musketeerplayersprint2.Util.*;
import musketeerplayersprint2.Debug.*;

public class CleanupPolitician extends Robot {
    static Direction main_direction;

    //TOCONSIDER: allow for these types to attack neutrals
    public CleanupPolitician(RobotController r) {
        super(r);
        subRobotType = Comms.SubRobotType.POL_EXPLORER;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
    }
    
    public CleanupPolitician(RobotController r, MapLocation h) {
        this(r);
        home = h;
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
        int maxEnemyAttackableDistSquared = Integer.MAX_VALUE;
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
            }
        }
        
        if (powerful != null) {
            Direction toMove = rc.getLocation().directionTo(powerful.getLocation());
        }

        while (!tryMove(main_direction) && rc.isReady()){
            main_direction = Util.randomDirection();
        }

        if(propagateFlags());
        else if(broadcastECLocation());
        else if(closestEnemy != null && broadcastEnemyLocalOrGlobal(closestEnemy.getLocation()));
    }
}