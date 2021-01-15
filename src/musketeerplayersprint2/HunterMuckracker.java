package musketeerplayersprint2;
import battlecode.common.*;

import musketeerplayersprint2.Util.*;
import musketeerplayersprint2.Debug.*;

public class HunterMuckracker extends Robot {
    static Direction main_direction;
    static MapLocation enemyLocation;

    public HunterMuckracker(RobotController r, MapLocation enemyLoc) {
        super(r);
        subRobotType = Comms.SubRobotType.MUC_HUNTER;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
        enemyLocation = enemyLoc;
    }

    public HunterMuckracker(RobotController r) {
        this(r, null);
    }

    public HunterMuckracker(RobotController r, MapLocation enemyLoc, MapLocation h) {
        this(r, enemyLoc);
        home = h;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        MapLocation currLoc = rc.getLocation();

        Debug.println(Debug.info, "I am a hunter Mucker; current influence: " + rc.getInfluence() + "; current conviction: " + rc.getConviction());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        if(main_direction == null){
            main_direction = Util.randomDirection();
        }
        RobotInfo powerful = null;
        int bestInfluence = Integer.MIN_VALUE;
        for (RobotInfo robot : enemyAttackable) {
            int curr = robot.getInfluence();
            if (curr > bestInfluence && robot.type.canBeExposed()) {
                bestInfluence = curr;
                powerful = robot;
            }
        }
        
        boolean muckraker_Found_EC = false;
        for (RobotInfo robot : rc.senseNearbyRobots(sensorRadius, enemy)) {
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER){
                MapLocation tempLoc = robot.getLocation();
                if (currLoc.distanceSquaredTo(tempLoc) <= 2) {
                    muckraker_Found_EC = true;
                } else {
                    enemyLocation = tempLoc;
                }
            }
        }

        boolean awayFromBase = false;
        RobotInfo friendlyBase = null;
        for (RobotInfo robot : friendlySensable) {
            MapLocation tempLoc = robot.getLocation();
            int dist = currLoc.distanceSquaredTo(tempLoc);
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                awayFromBase = true;
                friendlyBase = robot;
                Debug.println(Debug.info, "friendly base at " + friendlyBase.getLocation() + "; enemy Location is " + enemyLocation);
                if (tempLoc.equals(enemyLocation)) {
                    Debug.println(Debug.info, "reset enemy location");
                    enemyLocation = null;
                }
                int botFlag = rc.getFlag(robot.getID());
                Comms.InformationCategory flagIC = Comms.getIC(botFlag);
                if (flagIC == Comms.InformationCategory.ENEMY_EC) {
                    int[] dxdy = Comms.getDxDy(botFlag);
                    enemyLocation = new MapLocation(dxdy[0] + tempLoc.x - Util.dOffset, dxdy[1] + tempLoc.y - Util.dOffset);
                    break;
                }
            }
        }
        for (RobotInfo robot : rc.senseNearbyRobots(sensorRadius, Team.NEUTRAL)) {
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER){
                if (awayFromBase == false) {
                    awayFromBase = true;
                    friendlyBase = robot;
                }
            }
        }
        // int ECInfluence = Integer.MAX_VALUE;
        // if() {
        // if(rc.senseRobotAtLocation(home) == RobotInfo.ENLIGHTENMENT_CENTER) {
        //     ECInfluence = home.
        // }
        // }

        if (powerful != null) {
            if (rc.canExpose(powerful.location)) {
                rc.expose(powerful.location);
            }
        }
        RobotInfo bestSlanderer = null;
        bestInfluence = Integer.MIN_VALUE;
        RobotInfo minRobot = null;
        double minDistSquared = Integer.MAX_VALUE;
        int totalEnemyX = 0;
        int totalEnemyY = 0;
        int enemiesFound = 0;
        for (RobotInfo robot : rc.senseNearbyRobots(sensorRadius, enemy)) {
            MapLocation tempLoc = robot.getLocation();
            totalEnemyX += tempLoc.x;
            totalEnemyY += tempLoc.y;
            enemiesFound++;
            if (robot.getType() == RobotType.SLANDERER) {
                int curr = robot.getConviction();
                if (curr > bestInfluence) {
                    bestInfluence = curr;
                    bestSlanderer = robot;
                }
            }
            double temp = currLoc.distanceSquaredTo(tempLoc);
            if (temp < minDistSquared) {
                minDistSquared = temp;
                minRobot = robot;
            }
            // if (robot.getType() == RobotType.POLITICIAN && (robot.getConviction() >= 100 || ))
        }
        if (bestSlanderer != null) {
            main_direction = currLoc.directionTo(bestSlanderer.getLocation());
        }
        // if (minRobot != null) {
        //     broadcastEnemyFound(minRobot.getLocation());
        // }

        if(!muckraker_Found_EC){
            if (bestSlanderer != null && rc.isReady()) {
                main_direction = currLoc.directionTo(bestSlanderer.getLocation());
                tryMoveDest(main_direction);
                Debug.println(Debug.info, "Prioritizing killing slandies.");
                Debug.setIndicatorLine(Debug.info, rc.getLocation(), bestSlanderer.getLocation(), 255, 150, 50);
            }
            else if (enemyLocation != null && rc.isReady() && currLoc.distanceSquaredTo(enemyLocation) > sensorRadius) {
                tryMoveDest(currLoc.directionTo(enemyLocation));
                Debug.println(Debug.info, "Prioritizing hunting base at " + enemyLocation);
                Debug.setIndicatorLine(Debug.info, rc.getLocation(), enemyLocation, 255, 150, 50);
            }
            else if (awayFromBase) {
                tryMoveDest(currLoc.directionTo(friendlyBase.getLocation()).opposite());
                Debug.println(Debug.info, "Prioritizing moving away from friendly/neutral bases.");
            }
            else if (enemiesFound != 0) {
                MapLocation hunterLoc = new MapLocation(totalEnemyX / enemiesFound, totalEnemyY / enemiesFound);
                tryMoveDest(currLoc.directionTo(hunterLoc));
                Debug.println(Debug.info, "Prioritizing going towards average enemy at " + hunterLoc);
                Debug.setIndicatorLine(Debug.info, rc.getLocation(), hunterLoc, 255, 150, 50);
            }
            else {
                main_direction = Nav.explore();
                if(main_direction != null) {
                    tryMoveDest(main_direction);
                }
                Debug.println(Debug.info, "Prioritizing exploring: " + Nav.lastExploreDir);
            }
        }
         
        if(broadcastECLocation());
        else if(broadcastEnemyLocalOrGlobal());
    }
}