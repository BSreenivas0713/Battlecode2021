package naivecomms;
import battlecode.common.*;

import naivecomms.Util.*;
import naivecomms.Debug.*;

public class LatticeMuckraker extends Robot {
    static Direction main_direction;
    static MapLocation enemyLocation;

    public LatticeMuckraker(RobotController r) {
        super(r);
        subRobotType = Comms.SubRobotType.MUC_LATTICE;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
        enemyLocation = null;
    }
    
    public LatticeMuckraker(RobotController r, MapLocation h) {
        this(r);
        home = h;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        int sensingRadius = rc.getType().sensorRadiusSquared;
        MapLocation currLoc = rc.getLocation();

        Debug.println(Debug.info, "I am an lattice mucker; current influence: " + rc.getInfluence() + "; current conviction: " + rc.getConviction());
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
        for (RobotInfo robot : rc.senseNearbyRobots(sensingRadius, enemy)) {
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER){
                MapLocation tempLoc = robot.getLocation();
                if (currLoc.distanceSquaredTo(tempLoc) <= 2) {
                    muckraker_Found_EC = true;
                } else {
                    enemyLocation = tempLoc;
                }
            }
        }

        RobotInfo closest_muk = null;
        int closest_muk_dist = Integer.MAX_VALUE;
        boolean spawnKillRunFromHome = false;
        for (RobotInfo robot : friendlySensable) {
            MapLocation tempLoc = robot.getLocation();
            int dist = currLoc.distanceSquaredTo(tempLoc);
            if (robot.getType() == RobotType.MUCKRAKER && dist < closest_muk_dist) {
                closest_muk = robot;
                closest_muk_dist = dist;
            }
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                if (rc.getEmpowerFactor(rc.getTeam(),0) > Util.spawnKillThreshold && home.equals(robot.getLocation())) {
                    spawnKillRunFromHome = true;
                }
                int botFlag = rc.getFlag(robot.getID());
                Comms.InformationCategory flagIC = Comms.getIC(botFlag);
                if (flagIC == Comms.InformationCategory.ENEMY_EC) {
                    int[] dxdy = Comms.getDxDy(botFlag);
                    MapLocation ecLoc = robot.getLocation();
                    enemyLocation = new MapLocation(dxdy[0] + ecLoc.x - Util.dOffset, dxdy[1] + ecLoc.y - Util.dOffset);
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
        for (RobotInfo robot : rc.senseNearbyRobots(sensingRadius, enemy)) {
            if (robot.getType() == RobotType.SLANDERER) {
                int curr = robot.getConviction();
                if (curr > bestInfluence) {
                    bestInfluence = curr;
                    bestSlanderer = robot;
                }
            }
            double temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp < minDistSquared) {
                minDistSquared = temp;
                minRobot = robot;
            }
            // if (robot.getType() == RobotType.POLITICIAN && (robot.getConviction() >= 100 || ))
        }
        if (bestSlanderer != null) {
            main_direction = currLoc.directionTo(bestSlanderer.getLocation());
        }
        if (minRobot != null) {
            broadcastEnemyFound(minRobot.getLocation());
        }

        if(!muckraker_Found_EC){
            if (bestSlanderer != null && rc.isReady()) {
                main_direction = currLoc.directionTo(bestSlanderer.getLocation());
                tryMoveDest(main_direction);
            }
            else if (spawnKillRunFromHome) {
                main_direction = currLoc.directionTo(home).opposite();
                tryMoveDest(main_direction);
            }
            else if (closest_muk != null && rc.isReady()){
                main_direction = currLoc.directionTo(closest_muk.getLocation()).opposite();
                tryMoveDest(main_direction);
            }
            else if (enemyLocation != null && rc.isReady()) {
                tryMoveDest(currLoc.directionTo(enemyLocation));
            }
            else {
                main_direction = currLoc.directionTo(home).opposite();
                while (!tryMove(main_direction) && rc.isReady()){
                    main_direction = Util.randomDirection();
                }
            }
        }
         
        if(broadcastECLocation());
        else if(broadcastEnemyLocalOrGlobal());
    }
}