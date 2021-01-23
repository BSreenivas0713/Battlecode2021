package naivesuey;
import battlecode.common.*;

import naivesuey.Util.*;
import naivesuey.Debug.*;

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
        if (home != null) {
            friendlyECs.add(home, homeID);
        }
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
        int numReasonablePols = 0;
        int ecRadius = Integer.MAX_VALUE;
        MapLocation ecLoc = null;

        int maxPolAttackableDistSquared = Integer.MIN_VALUE;
        int maxPoliticianSizeWithinReasonableThreshold = 0;
        int robotConviction = 0;
        // int numRobotsInAttackable = rc.senseNearbyRobots(rc.getType().actionRadiusSquared).length;
        int numFriendlyAttackable = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam()).length;
        int polAttackThreshold = (int) (rc.getConviction() * rc.getEmpowerFactor(rc.getTeam(), 0) * 3 / 4);

        for(int i = enemyAttackable.length - 1; i >= 0; i--) {
            robot = enemyAttackable[i];
            robotConviction = robot.getConviction();
            attackable_conviction += robotConviction;
            int temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp > maxEnemyDistSquared) {
                maxEnemyDistSquared = temp;
                farthestEnemy = robot.getLocation();
            }

            if(robot.getType() == RobotType.POLITICIAN) {
                if (robotConviction > maxPoliticianSizeWithinReasonableThreshold &&
                    robotConviction <= polAttackThreshold) {
                    numReasonablePols++;
                    maxPoliticianSizeWithinReasonableThreshold = robotConviction;
                    if (temp > maxPolAttackableDistSquared) {
                        maxPolAttackableDistSquared = temp;
                    }
                    
                }
                numPoliticians++;
            }

            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                ecConviction = robot.getConviction();
                ecRadius = currLoc.distanceSquaredTo(robot.getLocation());
                ecLoc = robot.getLocation();
            }
        }

        if (ecConviction < 5 * rc.getEmpowerFactor(rc.getTeam(), 0) * rc.getConviction() && rc.canEmpower(ecRadius)) {
            Debug.println(Debug.info, "Empowered near EC with radius: " + ecRadius);
            Debug.setIndicatorLine(Debug.info, rc.getLocation(), ecLoc, 255, 150, 50);
            rc.empower(ecRadius);
            return;
        }

        if(numPoliticians > 3 && rc.getEmpowerFactor(rc.getTeam(), 0) > 3 && rc.canEmpower(maxEnemyDistSquared)) {
            Debug.println(Debug.info, "Empowered because of 3 pols with radius: " + maxEnemyDistSquared);
            Debug.setIndicatorLine(Debug.info, rc.getLocation(), farthestEnemy, 255, 150, 50);
            rc.empower(maxEnemyDistSquared);
            return;
        }

        if (maxPoliticianSizeWithinReasonableThreshold > 0 && enemyAttackable.length >= numFriendlyAttackable && 
            rc.getEmpowerFactor(rc.getTeam(), 0) >= Util.chainEmpowerFactor && rc.canEmpower(maxPolAttackableDistSquared) && numReasonablePols >= 2) {
            Debug.println(Debug.info, "Empowering because of high buff, trying to start a chain");
            rc.empower(maxPolAttackableDistSquared);
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
            double distToBase = (double) Integer.MAX_VALUE;
            if (home != null) {
                distToBase = home.distanceSquaredTo(robot.getLocation());
            }

            if (temp < minDistSquared) {
                minDistSquared = temp;
                closestEnemy = robot;
                if(robot.getType() == RobotType.MUCKRAKER) {
                    closestEnemyType = Comms.EnemyType.MUC;
                } else {
                    closestEnemyType = Comms.EnemyType.UNKNOWN;
                }
            }
            if(home != null && (distToBase <= sensorRadius || (distToBase <= 4 * sensorRadius && robot.getInfluence() > Util.smallMuckThreshold )) && 
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

        if (home != null) {
            // This means that the first half of an EC-ID/EC-ID broadcast finished.
            if(needToBroadcastHomeEC && rc.getFlag(rc.getID()) == defaultFlag) { broadcastHomeEC(); }
            else if(broadcastECLocation());
            else if(closestEnemy != null && broadcastEnemyLocalOrGlobal(closestEnemy.getLocation(), closestEnemyType));   
        }
        
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