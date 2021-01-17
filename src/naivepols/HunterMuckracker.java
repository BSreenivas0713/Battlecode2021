package naivepols;
import battlecode.common.*;

import naivepols.Util.*;
import naivepols.Debug.*;
import naivepols.fast.FastIterableLocSet;

public class HunterMuckracker extends Robot {
    static Direction main_direction;
    static MapLocation enemyLocation;
    static int baseCrowdedSemaphor;
    static int distSquaredToBase;
    static FastIterableLocSet lastAttacked;
    static int numRoundsSinceLastAttacked;
    static boolean seenEnemyLocation;
    static RotationDirection spinDirection = Util.RotationDirection.COUNTERCLOCKWISE;

    public HunterMuckracker(RobotController r, MapLocation enemyLoc) {
        super(r);
        subRobotType = Comms.SubRobotType.MUC_HUNTER;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
        enemyLocation = enemyLoc;
        baseCrowdedSemaphor = 4;
        if(enemyLocation != null) {
            distSquaredToBase = rc.getLocation().distanceSquaredTo(enemyLocation);
        }
        else {
            distSquaredToBase = -1;
        }
        lastAttacked = new FastIterableLocSet();
        numRoundsSinceLastAttacked = 0;
        seenEnemyLocation = false;
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
            Debug.println(Debug.info, "enemy location: " + enemyLocation + ";semaphor value: " + baseCrowdedSemaphor);
        }
        else {
            Debug.println(Debug.info, "no enemy location, reseting baseCrowdedSemaphor");
            baseCrowdedSemaphor = 5;
        }



        // if(lastAttacked.locs.length != 0) {
        //     Debug.println(Debug.info, "last attacked: " + lastAttacked.locs);
        // }
        // else {
        //     Debug.println(Debug.info, "last attacked list empty");
        // }

        // if(lastAttacked.locs.length != 0) {
        //     if(numRoundsSinceLastAttacked >= Util.MuckAttackCooldown) {
        //         numRoundsSinceLastAttacked = 0;
        //     }
        //     else {
        //         numRoundsSinceLastAttacked++;
        //     }
        // }

        if(main_direction == null){
            main_direction = Util.randomDirection();
        }

        if(enemyLocation != null && rc.canSenseLocation(enemyLocation) ) {
            RobotInfo supposedToBeAnEC = rc.senseRobotAtLocation(enemyLocation);
            if(supposedToBeAnEC == null || supposedToBeAnEC.getType() != RobotType.ENLIGHTENMENT_CENTER) {
                Debug.println(Debug.info, "reset the EC flag as it was at a wrong location");
                lastAttacked.add(enemyLocation);
                numRoundsSinceLastAttacked = 0;
                enemyLocation = null;
                baseCrowdedSemaphor = 5;
            }
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


        boolean setAttackCall = false;
        boolean muckraker_Found_EC = false;

        RobotInfo bestSlanderer = null;
        bestInfluence = Integer.MIN_VALUE;
        double minDistSquared = Integer.MAX_VALUE;
        int totalEnemyX = 0;
        int totalEnemyY = 0;
        int enemiesFound = 0;
        RobotInfo closestEnemy = null;
        int closestEnemyDist = Integer.MAX_VALUE;
        int temp;
        for(int i = enemySensable.length - 1; i >= 0; i--) {
            robot = enemySensable[i];
            MapLocation tempLoc = robot.getLocation();
            totalEnemyX += tempLoc.x;
            totalEnemyY += tempLoc.y;
            enemiesFound++;

            temp = currLoc.distanceSquaredTo(tempLoc);
            if (robot.getType() != RobotType.MUCKRAKER && temp < closestEnemyDist) {
                closestEnemyDist = temp;
                closestEnemy = robot;
            }

            if (robot.getType() == RobotType.SLANDERER) {
                int curr = robot.getConviction();
                if (curr > bestInfluence) {
                    bestInfluence = curr;
                    bestSlanderer = robot;
                }
            }
            
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER){
                if (currLoc.distanceSquaredTo(tempLoc) <= 2) {
                    muckraker_Found_EC = true;
                } else {
                    if(!lastAttacked.contains(tempLoc)) {
                        enemyLocation = tempLoc;
                        distSquaredToBase = rc.getLocation().distanceSquaredTo(enemyLocation);
                        if(!ICtoTurnMap.contains(Comms.InformationCategory.ENEMY_EC_ATTACK_CALL.ordinal())) {
                            Debug.println(Debug.info, "Found Enemy EC, Generating Attack call");
                            Debug.setIndicatorDot(Debug.info, enemyLocation, 255, 0, 0);
                            
                            int dx = enemyLocation.x - rc.getLocation().x;
                            int dy = enemyLocation.y - rc.getLocation().y;

                            int newFlag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC_ATTACK_CALL, dx + Util.dOffset, dy + Util.dOffset);
                            setFlag(newFlag);
                            setAttackCall = true;
                        }
                    }
                }
            }
        }

        RobotInfo closest_muk = null;
        int closest_muk_dist = Integer.MAX_VALUE;

        boolean awayFromBase = false;
        RobotInfo friendlyBase = null;
        boolean setChillFlag = false;
        int numFollowingClosestEnemy = 0;
        RobotInfo disperseBot = null;

        MapLocation robotLoc;
        int []DxDyFromRobot;
        MapLocation enemyLoc;

        for(int i = friendlySensable.length - 1; i >= 0; i--) {
            robot = friendlySensable[i];
            if(rc.canGetFlag(robot.getID())) {
                int flag = rc.getFlag(robot.getID());
                if(Comms.isSubRobotType(flag, Comms.SubRobotType.POL_RUSH)) {
                    Debug.println(Debug.info, "Found a rusher.");
                    disperseBot = robot;
                }

                // React to attack calls
                switch(Comms.getIC(flag)) {
                case ENEMY_EC_ATTACK_CALL:
                    if(enemyLocation == null) {
                        Debug.println(Debug.info, "Found Propogated flag(Attack). Acting on it. ");
                        robotLoc = robot.getLocation();
                        DxDyFromRobot = Comms.getDxDy(flag);
                        enemyLoc = new MapLocation(DxDyFromRobot[0] + robotLoc.x - Util.dOffset, DxDyFromRobot[1] + robotLoc.y - Util.dOffset);
                        Debug.setIndicatorDot(Debug.info, enemyLoc, 255, 0, 0);
                        // if(enemyLocation != null && !enemyLoc.equals(enemyLocation)) {
                        //     baseCrowdedSemaphor = 5;
                        //     Debug.println(Debug.info, "reset semaphor because of changed enemy location");
                        // }
                        enemyLocation = enemyLoc;
                        distSquaredToBase = rc.getLocation().distanceSquaredTo(enemyLocation);
                    }
                    break;
                case ENEMY_EC_CHILL_CALL:
                    if(enemyLocation != null) {
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
                    }
                    break;
                default: 
                    break;
                }
                
                if(closestEnemy != null) {
                    if(flag == Comms.getFlag(Comms.InformationCategory.FOLLOWING, closestEnemy.getID())) {
                        numFollowingClosestEnemy++;
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
            int botFlag;
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
                        lastAttacked.add(enemyLocation);
                        numRoundsSinceLastAttacked = 0;
                        enemyLocation = null;
                        distSquaredToBase = -1;
                }
                if(rc.canGetFlag(robot.getID())) {
                    botFlag = rc.getFlag(robot.getID());
                    Comms.InformationCategory flagIC = Comms.getIC(botFlag);
                    if (flagIC == Comms.InformationCategory.ENEMY_EC) {
                        int[] dxdy = Comms.getDxDy(botFlag);
                        enemyLocation = new MapLocation(dxdy[0] + tempLoc.x - Util.dOffset, dxdy[1] + tempLoc.y - Util.dOffset);
                        break;
                    }
                }
            }
        }

        if(closestEnemy != null) {
            Debug.println(Debug.info, "Num Following Closest Enemy: " + numFollowingClosestEnemy + "; closest Enemy at position: " + closestEnemy.getLocation());
        }

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
            if (bestSlanderer != null) {
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
            else if (enemyLocation != null) {
                if(!seenEnemyLocation) {
                    seenEnemyLocation = rc.canSenseLocation(enemyLocation);
                }

                boolean rotating = false;
                if(seenEnemyLocation) {
                    if(rc.getLocation().distanceSquaredTo(enemyLocation) <= actionRadius) {
                        main_direction = rc.getLocation().directionTo(enemyLocation).opposite();
                        Debug.println(Debug.info, "I moving away from the enemy location");
                    } else if(closest_muk_dist <= 4) {
                        main_direction = rc.getLocation().directionTo(closest_muk.getLocation()).opposite();
                        Debug.println(Debug.info, "I moving away from another muck");
                    } else {
                        rotating = true;
                        main_direction = Util.rightOrLeftTurn(spinDirection, home.directionTo(currLoc)); //Direction if we only want to rotate around the base
                        Debug.println(Debug.info, "I am rotating around the base");
                    }
                } else {
                    main_direction = rc.getLocation().directionTo(enemyLocation);
                    Debug.println(Debug.info, "I am going straight to the base");
                }

                int tryMove = 0;
                while (!tryMoveDest(main_direction) && rc.isReady() && tryMove <= 1 && rotating){
                    Debug.println(Debug.info, "I am switching rotation direction");
                    spinDirection = Util.switchSpinDirection(spinDirection);
                    main_direction = Util.rightOrLeftTurn(spinDirection, home.directionTo(currLoc));
                    tryMove +=1;
                }

                // tryMoveDest(currLoc.directionTo(enemyLocation));
                // if(rc.getLocation().distanceSquaredTo(enemyLocation) < distSquaredToBase) {
                //     baseCrowdedSemaphor = 5;
                //     Debug.println(Debug.info, "got closer to enemy base, resetting semaphor");
                // }
                // else {
                //     baseCrowdedSemaphor--;
                //     Debug.println(Debug.info, "did not get closer to enemy base, semaphor getting lower: " + baseCrowdedSemaphor);
                // }
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
            if(baseCrowdedSemaphor == 0) {
                lastAttacked.add(enemyLocation);
                numRoundsSinceLastAttacked = 0;
                enemyLocation = null;
            }
        }
         
        if(propagateFlags());
        else if(broadcastECLocation());
        else if(broadcastEnemyLocalOrGlobal());
    }
}