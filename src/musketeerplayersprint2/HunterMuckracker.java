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
        if(enemyLocation != null) {
            Debug.println(Debug.info, "enemy location: " + enemyLocation);
        }
        else {
            Debug.println(Debug.info, "no enemy location");
        }

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
        

        for(RobotInfo robot: friendlySensable) {
            if(rc.canGetFlag(robot.getID())) {
                int flag = rc.getFlag(robot.getID());
                MapLocation robotLoc;
                int []DxDyFromRobot;
                MapLocation enemyLoc;
                int dx;
                int dy;
                int newFlag; //These are here so I don't have to make different variable names in the different cases
                switch(Comms.getIC(flag)) {
                case ENEMY_EC_ATTACK_CALL:
                    Debug.println(Debug.info, "Found Propogated flag(Attack). Acting on it. ");
                    robotLoc = robot.getLocation();
                    DxDyFromRobot = Comms.getDxDy(flag);
                    enemyLoc = new MapLocation(DxDyFromRobot[0] + robotLoc.x - Util.dOffset, DxDyFromRobot[1] + robotLoc.y - Util.dOffset);
                    Debug.setIndicatorDot(Debug.info, enemyLoc, 255, 0, 0);
                    enemyLocation = enemyLoc;
                    break;
                case ENEMY_EC_CHILL_CALL:
                    Debug.println(Debug.info, "Found Propogated flag(Chill). Acting on it. ");
                    robotLoc = robot.getLocation();
                    DxDyFromRobot = Comms.getDxDy(flag);
                    enemyLoc = new MapLocation(DxDyFromRobot[0] + robotLoc.x - Util.dOffset, DxDyFromRobot[1] + robotLoc.y - Util.dOffset);
                    Debug.setIndicatorDot(Debug.info, enemyLoc, 255, 0, 0);
                    if(enemyLoc.equals(enemyLocation)) {
                        enemyLocation = null;
                        Debug.println(Debug.info, "Reset enemy location as a result of the chill flag");
                    }
                    break;
                default: 
                    break;
                }
            }
        }

        boolean setAttackCall = false;
        boolean muckraker_Found_EC = false;
        for (RobotInfo robot : enemySensable) {
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER){
                MapLocation tempLoc = robot.getLocation();

                if (currLoc.distanceSquaredTo(tempLoc) <= 2) {
                    muckraker_Found_EC = true;
                } else {
                    enemyLocation = tempLoc;
                    if(!ICtoTurnMap.contains(Comms.InformationCategory.ENEMY_EC_ATTACK_CALL.ordinal())) {
                        Debug.println(Debug.info, "Found Enemy EC, Generating Attack call");
                        Debug.setIndicatorDot(Debug.info, enemyLocation, 255, 0, 0);
                        
                        int dx = enemyLocation.x - currLoc.x;
                        int dy = enemyLocation.y - currLoc.y;

                        int newFlag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC_ATTACK_CALL, dx + Util.dOffset, dy + Util.dOffset);
                        setFlag(newFlag);
                        setAttackCall = true;
                    }
                }
            }
        }

        RobotInfo bestSlanderer = null;
        bestInfluence = Integer.MIN_VALUE;
        RobotInfo minRobot = null;
        double minDistSquared = Integer.MAX_VALUE;
        int totalEnemyX = 0;
        int totalEnemyY = 0;
        int enemiesFound = 0;
        RobotInfo closestEnemy = null;
        int closestEnemyDist = Integer.MAX_VALUE;
        for (RobotInfo robot : enemySensable) {
            MapLocation tempLoc = robot.getLocation();
            totalEnemyX += tempLoc.x;
            totalEnemyY += tempLoc.y;
            enemiesFound++;
            if (currLoc.distanceSquaredTo(tempLoc) < closestEnemyDist) {
                closestEnemyDist = currLoc.distanceSquaredTo(tempLoc);
                closestEnemy = robot;
            }
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

        boolean awayFromBase = false;
        RobotInfo friendlyBase = null;
        boolean setChillFlag = false;
        int numFollowingClosestEnemy = 0;
        RobotInfo disperseBot = null;
        for (RobotInfo robot : friendlySensable) {
            if(rc.canGetFlag(robot.getID())) {
                int flag = rc.getFlag(robot.getID());
                if(Comms.isSubRobotType(flag, Comms.SubRobotType.POL_RUSH)) {
                    Debug.println(Debug.info, "Found a rusher.");
                    disperseBot = robot;
                }
            }
            MapLocation tempLoc = robot.getLocation();
            int dist = currLoc.distanceSquaredTo(tempLoc);
            int botFlag = rc.getFlag(robot.getID());
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                awayFromBase = true;
                friendlyBase = robot;
                Debug.println(Debug.info, "friendly base at " + friendlyBase.getLocation() + "; enemy Location is " + enemyLocation);
                if (tempLoc.equals(enemyLocation)) {
                        Debug.println(Debug.info, "Enemy EC overtaken, setting chill flag, reseting enemyLocation");
                        Debug.setIndicatorDot(Debug.info, enemyLocation, 255, 0, 0);
                        
                        int dx = enemyLocation.x - currLoc.x;
                        int dy = enemyLocation.y - currLoc.y;

                        int newFlag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC_CHILL_CALL, dx + Util.dOffset, dy + Util.dOffset);
                        setFlag(newFlag);
                        setChillFlag = true;
                        enemyLocation = null;
                }
                Comms.InformationCategory flagIC = Comms.getIC(botFlag);
                if (flagIC == Comms.InformationCategory.ENEMY_EC) {
                    int[] dxdy = Comms.getDxDy(botFlag);
                    enemyLocation = new MapLocation(dxdy[0] + tempLoc.x - Util.dOffset, dxdy[1] + tempLoc.y - Util.dOffset);
                    break;
                }
            }
            if(enemiesFound != 0) {
                if(botFlag == Comms.getFlag(Comms.InformationCategory.FOLLOWING, closestEnemy.getID())) {
                    numFollowingClosestEnemy++;
                }
            }
        }
        if(closestEnemy != null) {
            Debug.println(Debug.info, "Num Following Closest Enemy: " + numFollowingClosestEnemy + "; closest Enemy at position: " + closestEnemy.getLocation());
        }
        if(enemyLocation != null) {
            boolean canSenseAroundBase = true;
            if(!rc.canSenseLocation(enemyLocation)) {
                canSenseAroundBase = false;
            }
            for(int x = 0; x < Util.directions.length; x++) {
                MapLocation newLocation = enemyLocation.add(Util.directions[x]);
                if(!rc.canSenseLocation(newLocation)) {
                    canSenseAroundBase = false;
                    break;
                }
            }
            boolean baseCrowded = true;
            if(canSenseAroundBase) {
                Debug.println(Debug.info, "Hunter able to look at base and 8 surrounding squares");
                RobotInfo enemyBase = rc.senseRobotAtLocation(enemyLocation);
                if(enemyBase.getTeam() != enemy) {
                    baseCrowded = false;
                    Debug.println(Debug.info, "Base not held by opponent");
                }
                for(int x = 0; x < Util.directions.length; x++) {
                    MapLocation newLocation = enemyLocation.add(Util.directions[x]);
                    if(rc.onTheMap(newLocation)) {
                        RobotInfo possibleMuckraker = rc.senseRobotAtLocation(newLocation);
                        if(possibleMuckraker.getType() != RobotType.MUCKRAKER && possibleMuckraker.getTeam() == rc.getTeam()) {
                            baseCrowded = false;
                            Debug.println(Debug.info, "No muckraker " + Util.directions[x] + " of enemy base");
                            break;
                        }
                    }
                }
                if(baseCrowded) {
                    Debug.println(Debug.info, "reset enemy location");
                    enemyLocation = null;
                    muckraker_Found_EC = false;
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


        if (bestSlanderer != null) {
            main_direction = currLoc.directionTo(bestSlanderer.getLocation());
        }
        
        //reset flag next turn unless chasing down an enemy
        resetFlagOnNewTurn = true;

        if(!muckraker_Found_EC){
            if (bestSlanderer != null && rc.isReady()) {
                main_direction = currLoc.directionTo(bestSlanderer.getLocation());
                tryMoveDest(main_direction);
                Debug.println(Debug.info, "Prioritizing killing slandies.");
                Debug.setIndicatorLine(Debug.info, rc.getLocation(), bestSlanderer.getLocation(), 255, 150, 50);
            }
            else if (disperseBot != null) {
                main_direction = currLoc.directionTo(disperseBot.getLocation()).opposite();
                tryMoveDest(main_direction);
                Debug.println(Debug.info, "Dispersing to avoid rusher.");
            }
            else if (enemyLocation != null && rc.isReady()) {
                tryMoveDest(currLoc.directionTo(enemyLocation));
                Debug.println(Debug.info, "Prioritizing hunting base at " + enemyLocation);
                Debug.setIndicatorLine(Debug.info, rc.getLocation(), enemyLocation, 255, 150, 50);
            }
            else if (awayFromBase) {
                tryMoveDest(currLoc.directionTo(friendlyBase.getLocation()).opposite());
                Debug.println(Debug.info, "Prioritizing moving away from friendly.");
            }
            else if (enemiesFound != 0 && numFollowingClosestEnemy < Util.maxFollowingSingleUnit) {
                MapLocation hunterLoc = new MapLocation(totalEnemyX / enemiesFound, totalEnemyY / enemiesFound);
                if(!setAttackCall && !setChillFlag) {
                    setFlag(Comms.getFlag(Comms.InformationCategory.FOLLOWING, closestEnemy.getID()));
                    resetFlagOnNewTurn = false;
                }
                tryMoveDest(currLoc.directionTo(hunterLoc));
                if(rc.isReady()) {
                    Debug.println(Debug.info, "Prioritizing going towards average enemy at " + hunterLoc);
                }
                else {
                    Debug.println(Debug.info, "Sending info about average enemy location(if attack call/chill call not set)");
                }
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
         
        if(propagateFlags());
        else if(broadcastECLocation());
        else if(broadcastEnemyLocalOrGlobal());
    }
}