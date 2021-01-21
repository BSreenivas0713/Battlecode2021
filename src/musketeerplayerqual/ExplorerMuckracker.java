package musketeerplayerqual;
import battlecode.common.*;

import musketeerplayerqual.Util.*;
import musketeerplayerqual.Debug.*;
import musketeerplayerqual.fast.FastIterableLocSet;

public class ExplorerMuckracker extends Robot {
    static Direction main_direction;
    static MapLocation enemyLocation;
    static boolean seenEnemyLocation;
    static RotationDirection spinDirection = Util.RotationDirection.COUNTERCLOCKWISE;

    public ExplorerMuckracker(RobotController r) {
        super(r);
        subRobotType = Comms.SubRobotType.MUC_EXPLORER;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
        enemyLocation = null;
    }

    public ExplorerMuckracker(RobotController r, MapLocation h, int hID) {
        this(r);
        home = h;
        homeID = hID;
        friendlyECs.add(home, homeID);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        int sensingRadius = rc.getType().sensorRadiusSquared;
        MapLocation currLoc = rc.getLocation();

        Debug.println(Debug.info, "I am an explorer mucker; current influence: " + rc.getInfluence() + "; current conviction: " + rc.getConviction());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        if(enemyLocation != null) {
            Debug.println(Debug.info, "enemy location: " + enemyLocation);
        }
        if(main_direction == null){
            main_direction = Util.randomDirection();
        }

        boolean setChillFlag = false;
        boolean setAttackFlag = false;

        if(enemyLocation != null && rc.canSenseLocation(enemyLocation) ) {
            RobotInfo supposedToBeAnEC = rc.senseRobotAtLocation(enemyLocation);
            if(supposedToBeAnEC == null || supposedToBeAnEC.getType() != RobotType.ENLIGHTENMENT_CENTER) {
                Debug.println(Debug.info, "Enemy EC not found, setting chill flag, reseting enemyLocation");
                Debug.setIndicatorDot(Debug.info, enemyLocation, 255, 0, 0);
                
                int dx = enemyLocation.x - currLoc.x;
                int dy = enemyLocation.y - currLoc.y;
                
                enemyLocation = null;
            }
        }

        if(rc.canGetFlag(homeID)) {
            Debug.println(Debug.info, "Checking home flag");
            int flag = rc.getFlag(homeID);
            Comms.InformationCategory IC = Comms.getIC(flag);
            switch(IC) {
                case ENEMY_EC:
                    if(enemyLocation == null) {
                        int[] dxdy = Comms.getDxDy(flag);
                        MapLocation enemyLoc = new MapLocation(dxdy[0] + home.x - Util.dOffset, dxdy[1] + home.y - Util.dOffset);
                        Debug.setIndicatorDot(Debug.info, enemyLoc, 255, 0, 0);

                        Comms.GroupRushType GRtype = Comms.getRushType(flag);
                        int GRmod = Comms.getRushMod(flag);
                        Debug.println(Debug.info, "EC is sending a rush: Read ENEMY_EC flag. Type: " + GRtype + ", mod: " + GRmod);

                        if((GRtype == Comms.GroupRushType.MUC || GRtype == Comms.GroupRushType.MUC_POL) && 
                            GRmod == rc.getID() % 2) {
                            Debug.println(Debug.info, "Joining the rush");
                            enemyLocation = enemyLoc;
                        } else {
                            Debug.println(Debug.info, "I was not included in this rush");
                        }
                    }
                    break;
                case ENEMY_FOUND:
                    if(enemyLocation == null) {
                        int[] dxdy = Comms.getDxDy(flag);
                        MapLocation enemyLoc = new MapLocation(dxdy[0] + home.x - Util.dOffset, dxdy[1] + home.y - Util.dOffset);
                        Debug.setIndicatorDot(Debug.info, enemyLoc, 255, 0, 0);

                        Debug.println(Debug.info, "EC is reporting slanderer location");

                        Comms.EnemyType enemyType = Comms.getEnemyType(flag);
                        int GRmod = Comms.getRushMod(flag);

                        if(enemyType == Comms.EnemyType.SLA && 
                            GRmod == rc.getID() % 2) {
                            Debug.println(Debug.info, "Following the slanderer");
                            enemyLocation = enemyLoc;
                        } else {
                            Debug.println(Debug.info, "I was not included in this call");
                        }
                    }
                    break;
            }
        } else {
            Debug.println(Debug.info, "Can't get home flag: " + homeID);
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
        
        boolean muckraker_Found_EC = false;

        RobotInfo closest_muk = null;
        int closest_muk_dist = Integer.MAX_VALUE;
        boolean spawnKillRunFromHome = false;
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
            }
        }

        if (powerful != null) {
            if (rc.canExpose(powerful.location)) {
                rc.expose(powerful.location);
            }
        }

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
            if (bestSlanderer != null) {
                main_direction = currLoc.directionTo(bestSlanderer.getLocation());
                Debug.setIndicatorLine(Debug.info, rc.getLocation(), bestSlanderer.getLocation(), 255, 150, 50);
                tryMoveDest(main_direction);
                Debug.println(Debug.info, "Moving towards a slanderer");
            }
            else if (disperseBot != null) {
                main_direction = currLoc.directionTo(disperseBot.getLocation()).opposite();
                tryMoveDest(main_direction);
                Debug.println(Debug.info, "Dispersing to avoid rusher.");
            }
            else if (spawnKillRunFromHome) {
                main_direction = currLoc.directionTo(home).opposite();
                tryMoveDest(main_direction);
                Debug.println(Debug.info, "Moving away from home");

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
                        main_direction = Util.rightOrLeftTurn(spinDirection, enemyLocation.directionTo(currLoc)); //Direction if we only want to rotate around the base
                        Debug.println(Debug.info, "I am rotating around the base");
                    }
                } else {
                    main_direction = rc.getLocation().directionTo(enemyLocation);
                    Debug.println(Debug.info, "I am going straight to the base");
                }

                Direction[] orderedDirs = Nav.greedyDirection(main_direction);
                boolean moved = false;
                for(Direction dir : orderedDirs) {
                    moved = moved || tryMove(dir);
                }

                if(!moved && rotating) {
                    Debug.println(Debug.info, "I am switching rotation direction");
                    spinDirection = Util.switchSpinDirection(spinDirection);
                    main_direction = Util.rightOrLeftTurn(spinDirection, enemyLocation.directionTo(currLoc));
                
                    orderedDirs = Nav.greedyDirection(main_direction);
                    if(orderedDirs != null) {
                        for(Direction dir : orderedDirs) {
                            tryMove(dir);
                        }
                    }
                }
                
                Debug.println(Debug.info, "Prioritizing hunting base at " + enemyLocation);
                Debug.setIndicatorLine(Debug.info, rc.getLocation(), enemyLocation, 255, 150, 50);
            }
            else {
                Direction[] orderedDirs = Nav.exploreGreedy();
                if(orderedDirs != null) {
                    for(Direction dir : orderedDirs) {
                        tryMove(dir);
                    }
                    orderedDirs = Util.getOrderedDirections(main_direction);
                    for(Direction dir : orderedDirs) {
                        tryMove(dir);
                    }
                }
                Debug.println(Debug.info, "Prioritizing exploring: " + Nav.lastExploreDir);
            }
        }

        // if(turnCount > Util.explorerMuckrakerLifetime) {
        //     changeTo = new LatticeMuckraker(rc, home);
        // }
    }
}