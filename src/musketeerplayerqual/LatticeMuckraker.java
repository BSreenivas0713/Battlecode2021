package musketeerplayerqual;
import battlecode.common.*;

import musketeerplayerqual.Util.*;
import musketeerplayerqual.Debug.*;

public class LatticeMuckraker extends Robot {
    static Direction main_direction;
    static MapLocation enemyLocation;

    public LatticeMuckraker(RobotController r) {
        super(r);
        subRobotType = Comms.SubRobotType.MUC_LATTICE;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
        enemyLocation = null;
    }
    
    public LatticeMuckraker(RobotController r, MapLocation h, int hID) {
        this(r);
        home = h;
        homeID = hID;
        friendlyECs.add(home, homeID);
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
        
        RobotInfo robot;
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

        RobotInfo closest_muk = null;
        int closest_muk_dist = Integer.MAX_VALUE;
        boolean spawnKillRunFromHome = false;
        RobotInfo disperseBot = null;

        for(int i = friendlySensable.length - 1; i >= 0; i--) {
            robot = friendlySensable[i];
            if(rc.canGetFlag(robot.getID())) {
                int flag = rc.getFlag(robot.getID());
                if(Comms.isSubRobotType(flag, Comms.SubRobotType.POL_RUSH) || 
                Comms.isSubRobotType(flag, Comms.SubRobotType.POL_HEAD) ||
                Comms.isSubRobotType(flag, Comms.SubRobotType.POL_SUPPORT)) {
                    Debug.println(Debug.info, "Found a rusher.");
                    disperseBot = robot;
                }
            }

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

        if (powerful != null) {
            if (rc.canExpose(powerful.location)) {
                rc.expose(powerful.location);
            }
        }

        boolean muckraker_Found_EC = false;
        RobotInfo bestSlanderer = null;
        bestInfluence = Integer.MIN_VALUE;
        RobotInfo closestEnemy = null;
        Comms.EnemyType closestEnemyType = null;
        double minDistSquared = Integer.MAX_VALUE;
        
        for(int i = enemySensable.length - 1; i >= 0; i--) {
            robot = enemySensable[i];
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
                closestEnemy = robot;
                if(robot.getType() == RobotType.MUCKRAKER) {
                    closestEnemyType = Comms.EnemyType.MUC;
                } else {
                    closestEnemyType = Comms.EnemyType.UNKNOWN;
                }
            }
            
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER){
                MapLocation tempLoc = robot.getLocation();
                if (currLoc.distanceSquaredTo(tempLoc) <= 2) {
                    muckraker_Found_EC = true;
                } else {
                    enemyLocation = tempLoc;
                }
            }
        }

        // This means that the first half of an EC-ID/EC-ID broadcast finished.
        if(needToBroadcastHomeEC && rc.getFlag(rc.getID()) == defaultFlag) { broadcastHomeEC(); }
        else if(broadcastECLocation());
        else if(bestSlanderer != null && broadcastEnemyFound(bestSlanderer.getLocation(), Comms.EnemyType.SLA));
        else if(closestEnemy != null && broadcastEnemyLocalOrGlobal(closestEnemy.getLocation(), closestEnemyType));
        
        if (bestSlanderer != null) {
            main_direction = currLoc.directionTo(bestSlanderer.getLocation());
        }

        if(!muckraker_Found_EC){
            if (bestSlanderer != null && rc.isReady()) {
                main_direction = currLoc.directionTo(bestSlanderer.getLocation());
                tryMoveDest(main_direction);
            }            
            else if (disperseBot != null && rc.isReady()) {
                main_direction = currLoc.directionTo(disperseBot.getLocation()).opposite();
                tryMoveDest(main_direction);
                Debug.println(Debug.info, "Dispersing to avoid rusher.");
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
                Direction[] orderedDirs = Util.getOrderedDirections(main_direction);
                for(Direction dir : orderedDirs) {
                    tryMove(dir);
                }
            }
        }
    }
}