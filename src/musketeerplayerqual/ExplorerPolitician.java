package musketeerplayerqual;
import battlecode.common.*;

import musketeerplayerqual.Util.*;
import musketeerplayerqual.Debug.*;

public class ExplorerPolitician extends Robot {
    static Direction main_direction;
    static boolean   toDetonate = false;

    //TOCONSIDER: allow for these types to attack neutrals
    public ExplorerPolitician(RobotController r) {
        super(r);
        subRobotType = Comms.SubRobotType.POL_EXPLORER;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
    }
    
    public ExplorerPolitician(RobotController r, MapLocation h, int hID) {
        this(r);
        home = h;
        homeID = hID;
        friendlyECs.add(home, homeID);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Debug.println(Debug.info, "I am an explorer politician; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        if(main_direction == null){
            main_direction = Util.randomDirection();
        }
        
        RobotInfo robot;
        int min_attackable_conviction = (rc.getConviction() - 10) / 3;
        int attackable_conviction = 0;
        MapLocation currLoc = rc.getLocation();
        int maxEnemyDistSquared = Integer.MIN_VALUE;
        MapLocation farthestEnemy = null;
        boolean inActionRadiusOfFriendly = false;

        int ecConviction = Integer.MAX_VALUE;
        int numPoliticians = 0;
        int ecRadius = Integer.MAX_VALUE;
        MapLocation ecLoc = null;

        for(int i = enemyAttackable.length - 1; i >= 0; i--) {
            robot = enemyAttackable[i];
            attackable_conviction += robot.getConviction();
            int temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp > maxEnemyDistSquared) {
                maxEnemyDistSquared = temp;
                farthestEnemy = robot.getLocation();
            }

            if(robot.getType() == RobotType.POLITICIAN) {
                numPoliticians++;
            }

            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                ecConviction = robot.getConviction();
                ecRadius = currLoc.distanceSquaredTo(robot.getLocation());
                ecLoc = robot.getLocation();
            }
        }

        if (ecConviction < 10 * rc.getEmpowerFactor(rc.getTeam(), 0) * rc.getConviction() && rc.canEmpower(ecRadius)) {
            Debug.println(Debug.info, "Empowered with radius: " + ecRadius);
            Debug.setIndicatorLine(Debug.info, rc.getLocation(), ecLoc, 255, 150, 50);
            rc.empower(ecRadius);
            return;
        }

        if(numPoliticians > 3 && rc.getEmpowerFactor(rc.getTeam(), 0) > 3 && rc.canEmpower(maxEnemyDistSquared)) {
            Debug.println(Debug.info, "Empowered with radius: " + maxEnemyDistSquared);
            Debug.setIndicatorLine(Debug.info, rc.getLocation(), farthestEnemy, 255, 150, 50);
            rc.empower(maxEnemyDistSquared);
            return;
        }

        RobotInfo powerful = null;
        int max_influence = (rc.getConviction()-10) / 3;
        RobotInfo closestEnemy = null;
        Comms.EnemyType closestEnemyType = null;
        double minDistSquared = Integer.MAX_VALUE;
        
        RobotInfo EC = null;
        int min_influence = 0;
        RobotInfo farthestMukCloseToBase = null;
        int MukminDistSquared = Integer.MAX_VALUE;
        MapLocation enemyLoc = null;
        boolean enemyNearBase = false;

        for(int i = enemySensable.length - 1; i >= 0; i--) {
            robot = enemySensable[i];
            int currInfluence = robot.getConviction();
            double temp = currLoc.distanceSquaredTo(robot.getLocation());
            double distToBase = home.distanceSquaredTo(robot.getLocation());
            if (temp < minDistSquared) {
                minDistSquared = temp;
                closestEnemy = robot;
                if(robot.getType() == RobotType.MUCKRAKER) {
                    closestEnemyType = Comms.EnemyType.MUC;
                } else {
                    closestEnemyType = Comms.EnemyType.UNKNOWN;
                }
            }
            if((distToBase <= sensorRadius || (distToBase <= 4 * sensorRadius && robot.getInfluence() > Util.smallMuckThreshold )) && 
                robot.getType() == RobotType.MUCKRAKER) {
                if (temp < MukminDistSquared) {
                    MukminDistSquared = (int) temp;
                    enemyNearBase = true;
                    enemyLoc = robot.getLocation();

                }
            }
            
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && currInfluence < min_influence) {
                EC = robot;
                min_influence = currInfluence;
            }
        }
        for(int i = friendlySensable.length - 1; i >=0; i --) {
            robot = friendlySensable[i];
            int enemyDist = rc.getLocation().distanceSquaredTo(robot.getLocation());
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER && enemyDist  <= actionRadius) {
                inActionRadiusOfFriendly = true;
            }
        }

        // This means that the first half of an EC-ID/EC-ID broadcast finished.
        if(needToBroadcastHomeEC && rc.getFlag(rc.getID()) == defaultFlag) { broadcastHomeEC(); }
        else if(broadcastECLocation());
        else if(closestEnemy != null && broadcastEnemyLocalOrGlobal(closestEnemy.getLocation(), closestEnemyType));   
        
        if(enemyNearBase) {
            if(rc.canEmpower(MukminDistSquared)) {
                Debug.println("enemy too close to base. Even though I am an explorerer, I will empower");
                rc.empower(MukminDistSquared);
            }
            else {
                Debug.println("moving towards an enemy too close to base even though I am an explorer");
                Direction toMove = rc.getLocation().directionTo(enemyLoc);
                tryMoveDest(toMove);
            }
        }
        if (EC != null) {
            Direction toMove = rc.getLocation().directionTo(EC.getLocation());
            tryMoveDest(toMove);
        }

        Direction[] orderedDirs = Nav.exploreGreedy(rc);
        boolean moved = false;
        if(orderedDirs != null) {
            for(Direction dir : orderedDirs) {
                moved = moved || tryMove(dir);
            }
            orderedDirs = Util.getOrderedDirections(main_direction);
            for(Direction dir : orderedDirs) {
                moved = moved || tryMove(dir);
            }
            if(!moved && rc.isReady() && inActionRadiusOfFriendly) {
                    tryMoveDest(main_direction);
            }
        }
    }
}