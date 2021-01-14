package musketeerplayersprint2;
import battlecode.common.*;

import musketeerplayersprint2.Util.*;
import musketeerplayersprint2.Debug.*;

public class ExplorerPolitician extends Robot {
    static Direction main_direction;
    static boolean   toDetonate = false;

    //TOCONSIDER: allow for these types to attack neutrals
    public ExplorerPolitician(RobotController r) {
        super(r);
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.POL_EXPLORER);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Debug.println(Debug.info, "I am an explorer politician; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        if(main_direction == null){
            main_direction = Util.randomDirection();
        }
        
        int min_attackable_conviction = (rc.getConviction()-10) / 3;
        int attackable_conviction = 0;
        MapLocation currLoc = rc.getLocation();
        int maxEnemyDistSquared = Integer.MIN_VALUE;
        MapLocation farthestEnemy = null;
        for (RobotInfo robot : enemyAttackable) {
            attackable_conviction += robot.getConviction();
            int temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp > maxEnemyDistSquared) {
                maxEnemyDistSquared = temp;
                farthestEnemy = robot.getLocation();
            }
        }

        if (attackable_conviction >= min_attackable_conviction && rc.canEmpower(maxEnemyDistSquared)) {
            Debug.println(Debug.info, "Empowered with radius: " + maxEnemyDistSquared);
            Debug.setIndicatorLine(Debug.info, rc.getLocation(), farthestEnemy, 255, 150, 50);
            rc.empower(maxEnemyDistSquared);
            return;
        }

        RobotInfo powerful = null;
        int max_influence = (rc.getConviction()-10) / 3;
        RobotInfo minRobot = null;
        double minDistSquared = Integer.MAX_VALUE;
        for (RobotInfo robot : enemySensable) {
            int currInfluence = robot.getConviction();
            if (robot.getType() == RobotType.MUCKRAKER && currInfluence > max_influence) {
                powerful = robot;
                max_influence = currInfluence;
            }
            double temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp < minDistSquared) {
                minDistSquared = temp;
                minRobot = robot;
            }
        }

        if (minRobot != null) {
            broadcastEnemyFound(minRobot.getLocation());
        }
        
        if (powerful != null) {
            Direction toMove = rc.getLocation().directionTo(powerful.getLocation());
            tryMoveDest(toMove);
        }
        
        RobotInfo weakest = null;
        int min_influence = 0;
        for (RobotInfo robot : enemySensable) {
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