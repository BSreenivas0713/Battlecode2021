package musketeerplayersprint2;
import battlecode.common.*;

import musketeerplayersprint2.Util.*;
import musketeerplayersprint2.Debug.*;
import musketeerplayersprint2.fast.FastIterableLocSet;

public class ExplorerMuckracker extends Robot {
    static Direction main_direction;
    static MapLocation enemyLocation;
    static int baseCrowdedSemaphor;
    static int distSquaredToBase;
    static FastIterableLocSet lastAttacked;
    static int numRoundsSinceLastAttacked = 0;

    public ExplorerMuckracker(RobotController r) {
        super(r);
        subRobotType = Comms.SubRobotType.MUC_EXPLORER;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
        enemyLocation = null;
        baseCrowdedSemaphor = 4;
        distSquaredToBase = -1;
        lastAttacked = new FastIterableLocSet();


    }

    public ExplorerMuckracker(RobotController r, MapLocation h) {
        this(r);
        home = h;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        int sensingRadius = rc.getType().sensorRadiusSquared;
        MapLocation currLoc = rc.getLocation();

        Debug.println(Debug.info, "I am an explorer mucker; current influence: " + rc.getInfluence() + "; current conviction: " + rc.getConviction());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        if(enemyLocation != null) {
            Debug.println(Debug.info, "enemy location: " + enemyLocation + ";semaphor value: " + baseCrowdedSemaphor);
        }
        else {
            Debug.println(Debug.info, "no enemy location, resetting base crowded semaphor");
            baseCrowdedSemaphor = 5;
        }

        if(lastAttacked.locs.length != 0) {
            Debug.println(Debug.info, "last attacked: " + lastAttacked.locs);
        }
        else {
            Debug.println(Debug.info, "last attacked list empty");
        }
        if(main_direction == null){
            main_direction = Util.randomDirection();
        }


        if(lastAttacked.locs.length != 0) {
            if(numRoundsSinceLastAttacked >= Util.MuckAttackCooldown) {
                numRoundsSinceLastAttacked = 0;
            }
            else {
                numRoundsSinceLastAttacked++;
            }
        }


        if(enemyLocation != null && rc.canSenseLocation(enemyLocation) ) {
            RobotInfo supposedToBeAnEC = rc.senseRobotAtLocation(enemyLocation);
            if(supposedToBeAnEC == null || supposedToBeAnEC.getType() != RobotType.ENLIGHTENMENT_CENTER) {
                lastAttacked.add(enemyLocation);
                enemyLocation = null;
                baseCrowdedSemaphor = 5;
            }
        }

        RobotInfo powerful = null;
        int bestInfluence = Integer.MIN_VALUE;
        RobotInfo robot;
        for(int i = enemyAttackable.length - 1; i >= 0; i--) {
            robot = enemyAttackable[i];
            int curr = robot.getInfluence();
            if (curr > bestInfluence && robot.type.canBeExposed()) {
                bestInfluence = curr;
                powerful = robot;
            }
        }

        for(int i = friendlySensable.length - 1; i >= 0; i--) {
            robot = friendlySensable[i];
        }
        
        boolean muckraker_Found_EC = false;

        RobotInfo closest_muk = null;
        int closest_muk_dist = Integer.MAX_VALUE;
        boolean spawnKillRunFromHome = false;
        boolean setChillFlag = false;
        RobotInfo disperseBot = null;

        MapLocation robotLoc;
        int []DxDyFromRobot;
        MapLocation enemyLoc;

        for(int i = friendlySensable.length - 1; i >= 0; i--) {
            robot = friendlySensable[i];
            if(rc.canGetFlag(robot.getID())) {
                int flag = rc.getFlag(robot.getID());
                // Move out of the way of rush pols
                if(Comms.isSubRobotType(flag, Comms.SubRobotType.POL_RUSH)) {
                    Debug.println(Debug.info, "Found a rusher.");
                    disperseBot = robot;
                }
                
                // React to attack calls
                if(enemyLocation == null) {
                    switch(Comms.getIC(flag)) {
                    case ENEMY_EC_ATTACK_CALL:
                        Debug.println(Debug.info, "Found Propogated flag(Attack). Acting on it. ");
                        robotLoc = robot.getLocation();
                        DxDyFromRobot = Comms.getDxDy(flag);
                        enemyLoc = new MapLocation(DxDyFromRobot[0] + robotLoc.x - Util.dOffset, DxDyFromRobot[1] + robotLoc.y - Util.dOffset);
                        Debug.setIndicatorDot(Debug.info, enemyLoc, 255, 0, 0);
                        if(enemyLocation != null && !enemyLoc.equals(enemyLocation)) {
                            baseCrowdedSemaphor = 5;
                            Debug.println(Debug.info, "reset semaphor because of changed enemy location");
                        }
                        enemyLocation = enemyLoc;
                        distSquaredToBase = rc.getLocation().distanceSquaredTo(enemyLocation);
                        break;
                    case ENEMY_EC_CHILL_CALL:
                        Debug.println(Debug.info, "Found Propogated flag(Chill). Acting on it. ");
                        robotLoc = robot.getLocation();
                        DxDyFromRobot = Comms.getDxDy(flag);
                        enemyLoc = new MapLocation(DxDyFromRobot[0] + robotLoc.x - Util.dOffset, DxDyFromRobot[1] + robotLoc.y - Util.dOffset);
                        Debug.setIndicatorDot(Debug.info, enemyLoc, 255, 0, 0);
                        if(enemyLoc.equals(enemyLocation)) {
                            enemyLocation = null;
                            distSquaredToBase = -1;
                            Debug.println(Debug.info, "Reset enemy location as a result of the chill flag");
                        }
                        break;
                    default: 
                        break;
                    }
                }
            }

            // Send chill flag
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
                if (tempLoc.equals(enemyLocation)) {
                    Debug.println(Debug.info, "Enemy EC overtaken, setting chill flag, reseting enemyLocation");
                        Debug.setIndicatorDot(Debug.info, enemyLocation, 255, 0, 0);
                        
                        int dx = enemyLocation.x - rc.getLocation().x;
                        int dy = enemyLocation.y - rc.getLocation().y;

                        int newFlag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC_CHILL_CALL, dx + Util.dOffset, dy + Util.dOffset);
                        setFlag(newFlag);
                        setChillFlag = true;
                        lastAttacked.add(enemyLocation);
                        enemyLocation = null;
                        distSquaredToBase = -1;
                }
                int botFlag = rc.getFlag(robot.getID());
                Comms.InformationCategory flagIC = Comms.getIC(botFlag);
                if (flagIC == Comms.InformationCategory.ENEMY_EC) {
                    int[] dxdy = Comms.getDxDy(botFlag);
                    MapLocation ecLoc = robot.getLocation();
                    enemyLocation = new MapLocation(dxdy[0] + ecLoc.x - Util.dOffset, dxdy[1] + ecLoc.y - Util.dOffset);
                    distSquaredToBase = rc.getLocation().distanceSquaredTo(enemyLocation);
                }
            }
        }

        if (powerful != null) {
            if (rc.canExpose(powerful.location)) {
                rc.expose(powerful.location);
            }
        }

        RobotInfo bestSlanderer = null;
        bestInfluence = Integer.MIN_VALUE;
        RobotInfo minRobot = null;
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
                minRobot = robot;
            }
            
            // Attack call
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER){
                MapLocation tempLoc = robot.getLocation();
                if (currLoc.distanceSquaredTo(tempLoc) <= 2) {
                    muckraker_Found_EC = true;
                } else {
                    if(!lastAttacked.contains(tempLoc)) {
                        enemyLocation = tempLoc;
                        distSquaredToBase = rc.getLocation().distanceSquaredTo(enemyLocation);
                        if(!ICtoTurnMap.contains(Comms.InformationCategory.ENEMY_EC_ATTACK_CALL.ordinal())) {
                            Debug.println(Debug.info, "Found Enemy EC, Generating Attack call");
                            Debug.setIndicatorDot(Debug.info, enemyLocation, 255, 0, 0);
                            
                            int dx = enemyLocation.x - currLoc.x;
                            int dy = enemyLocation.y - currLoc.y;

                            int newFlag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC_ATTACK_CALL, dx + Util.dOffset, dy + Util.dOffset);
                            setFlag(newFlag);
                        }
                    }
                }
            }
            // if (robot.getType() == RobotType.POLITICIAN && (robot.getConviction() >= 100 || ))
        }
        
        if (bestSlanderer != null) {
            main_direction = currLoc.directionTo(bestSlanderer.getLocation());
        }

        if(!muckraker_Found_EC){
            if (bestSlanderer != null && rc.isReady()) {
                main_direction = currLoc.directionTo(bestSlanderer.getLocation());
                Debug.setIndicatorLine(Debug.info, rc.getLocation(), bestSlanderer.getLocation(), 255, 150, 50);
                tryMoveDest(main_direction);
                Debug.println(Debug.info, "Moving towards a slanderer");
            }
            else if (disperseBot != null && rc.isReady()) {
                main_direction = currLoc.directionTo(disperseBot.getLocation()).opposite();
                tryMoveDest(main_direction);
                Debug.println(Debug.info, "Dispersing to avoid rusher.");
            }
            else if (spawnKillRunFromHome) {
                main_direction = currLoc.directionTo(home).opposite();
                tryMoveDest(main_direction);
                Debug.println(Debug.info, "Moving away from home");

            }
            else if (enemyLocation != null && rc.isReady() && baseCrowdedSemaphor != 0) {
                tryMoveDest(currLoc.directionTo(enemyLocation));
                if(rc.getLocation().distanceSquaredTo(enemyLocation) < distSquaredToBase) {
                    baseCrowdedSemaphor = 5;
                    Debug.println(Debug.info, "got closer to enemy base, resetting semaphor");
                }
                else {
                    baseCrowdedSemaphor--;
                    Debug.println(Debug.info, "did not get closer to enemy base, semaphor getting lower: " + baseCrowdedSemaphor);
                }
                Debug.println(Debug.info, "Prioritizing hunting base at " + enemyLocation);
                Debug.setIndicatorLine(Debug.info, rc.getLocation(), enemyLocation, 255, 150, 50);
            }
            else {
                main_direction = Nav.explore();
                if(main_direction != null) {
                    tryMoveDest(main_direction);
                }
                Debug.println(Debug.info, "Prioritizing exploring: " + Nav.lastExploreDir);
            }
            if(baseCrowdedSemaphor == 0) {
                lastAttacked.add(enemyLocation);
                enemyLocation = null;
            }
        }

        // if(turnCount > Util.explorerMuckrakerLifetime) {
        //     changeTo = new LatticeMuckraker(rc, home);
        // }
         
        if(propagateFlags());
        else if(broadcastECLocation());
        else if(broadcastEnemyLocalOrGlobal());
    }
}