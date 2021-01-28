package musketeerplayerqual;
import battlecode.common.*;

import musketeerplayerqual.Util.*;
import musketeerplayerqual.Debug.*;
import musketeerplayerqual.fast.FastIterableLocSet;

public class ExplorerMuckracker extends Robot {
    static Direction main_direction;
    static MapLocation locGotEnemyLocation;
    static MapLocation enemyLocation;
    static int turnsSinceClosestDistanceDecreased;
    static int closestDistanceToDest;
    static boolean seenEnemyLocation;
    static boolean isEnemyLocEC;
    static int turnFoundEnemyLoc;
    static RotationDirection spinDirection = Util.RotationDirection.COUNTERCLOCKWISE;

    public ExplorerMuckracker(RobotController r) {
        super(r);
        subRobotType = Comms.SubRobotType.MUC_EXPLORER;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
        enemyLocation = null;
        turnsSinceClosestDistanceDecreased = 0;
        closestDistanceToDest = Integer.MAX_VALUE;
        seenEnemyLocation = false;
        isEnemyLocEC = true;
    }

    public ExplorerMuckracker(RobotController r, MapLocation h, int hID) {
        this(r);
        home = h;
        homeID = hID;
        friendlyECs.add(home, homeID);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        boolean inActionRadiusOfFriendly = false;
        int sensingRadius = rc.getType().sensorRadiusSquared;
        MapLocation currLoc = rc.getLocation();

        Debug.println(Debug.info, "I am an explorer mucker; current influence: " + rc.getInfluence() + "; current conviction: " + rc.getConviction());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        if(main_direction == null){
            main_direction = Util.randomDirection();
        }

        if(rc.canGetFlag(homeID)) {
            Debug.println(Debug.info, "Checking home flag");
            int flag = rc.getFlag(homeID);
            Comms.InformationCategory IC = Comms.getIC(flag);
            switch(IC) {
                // case TARGET_ROBOT:
                //     if(Comms.getSubRobotType(flag) == Comms.SubRobotType.POL_SUPPORT && enemyLocation == null) {
                //         int[] dxdy = Comms.getDxDySubRobotType(flag);
                //         MapLocation enemyLoc = new MapLocation(dxdy[0] + home.x - Util.dOffset, dxdy[1] + home.y - Util.dOffset);
                //         Debug.setIndicatorDot(Debug.info, enemyLoc, 255, 0, 0);

                //         int GRmod = (int)(Math.random() * 2);
                //         Debug.println(Debug.info, "EC is sending a rush: Read POL_SUPPORT flag. Mod: " + GRmod);

                //         if(GRmod == rc.getID() % 2) {
                //             Debug.println(Debug.info, "Joining the rush");
                //             locGotEnemyLocation = rc.getLocation();
                //             enemyLocation = enemyLoc;
                //             seenEnemyLocation = false;
                //             isEnemyLocEC = true;
                //             turnsSinceClosestDistanceDecreased = 0;
                //             closestDistanceToDest = Integer.MAX_VALUE;
                //         } else {
                //             Debug.println(Debug.info, "I was not included in this rush");
                //         }
                //     }
                case ENEMY_EC:
                    if(enemyLocation == null) {
                        int[] dxdy = Comms.getDxDy(flag);
                        if(dxdy[0] != 0 && dxdy[1] != 0) {
                            MapLocation enemyLoc = new MapLocation(dxdy[0] + home.x - Util.dOffset, dxdy[1] + home.y - Util.dOffset);
                            Debug.setIndicatorDot(Debug.info, enemyLoc, 255, 0, 0);
    
                            Comms.GroupRushType GRtype = Comms.getRushType(flag);
                            int GRmod = Comms.getRushMod(flag);
                            Debug.println(Debug.info, "EC is sending a rush: Read ENEMY_EC flag. Type: " + GRtype + ", mod: " + GRmod);
    
                            if((GRtype == Comms.GroupRushType.MUC || GRtype == Comms.GroupRushType.MUC_POL) && 
                                GRmod == rc.getID() % 2) {
                                Debug.println(Debug.info, "Joining the rush");
                                locGotEnemyLocation = rc.getLocation();
                                enemyLocation = enemyLoc;
                                seenEnemyLocation = false;
                                isEnemyLocEC = true;
                                turnsSinceClosestDistanceDecreased = 0;
                                closestDistanceToDest = Integer.MAX_VALUE;
                            } else {
                                Debug.println(Debug.info, "I was not included in this rush");
                            }
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
                            locGotEnemyLocation = rc.getLocation();
                            enemyLocation = enemyLoc;
                            seenEnemyLocation = false;
                            isEnemyLocEC = true;
                            turnsSinceClosestDistanceDecreased = 0;
                            closestDistanceToDest = Integer.MAX_VALUE;
                        } else {
                            Debug.println(Debug.info, "I was not included in this call");
                        }
                    }
                    break;
                case DELETE_ENEMY_LOC:
                    if(enemyLocation != null && !seenEnemyLocation) {
                        int[] dxdy = Comms.getDxDy(flag);
                        MapLocation enemyLoc = new MapLocation(dxdy[0] + home.x - Util.dOffset, dxdy[1] + home.y - Util.dOffset);

                        if(enemyLocation.equals(enemyLoc)) {
                            locGotEnemyLocation = null;
                            enemyLocation = null;
                            seenEnemyLocation = false;
                            isEnemyLocEC = true;
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
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                if(rc.getLocation().distanceSquaredTo(robot.getLocation()) <= actionRadius) {
                    inActionRadiusOfFriendly = true;
                }
                if(enemyLocation != null && robot.getLocation().equals(enemyLocation)) {
                    locGotEnemyLocation = null;
                    enemyLocation = null;
                    seenEnemyLocation = false;
                    isEnemyLocEC = true;
                    Debug.println("Base has been captured. EnemyLocation is null");
                }
            }
            if(rc.canGetFlag(robot.getID())) {
                int flag = rc.getFlag(robot.getID());
                // Move out of the way of rush pols
                if(Comms.isRusher(flag)) {
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
        
        if(enemyLocation != null && seenEnemyLocation && !isEnemyLocEC &&
            rc.getRoundNum() > turnFoundEnemyLoc + Util.foundEnemyLocationBoredom) {
            locGotEnemyLocation = null;
            enemyLocation = null;
            seenEnemyLocation = false;
            isEnemyLocEC = true;
            Debug.println("Got bored of following direction past enemyLocation. Deleting enemyLocation.");
        }
        
        if (disperseBot != null) {
            main_direction = currLoc.directionTo(disperseBot.getLocation()).opposite();
            tryMoveDest(main_direction);
            Debug.println(Debug.info, "Dispersing to avoid rusher.");
        }
        else if(!muckraker_Found_EC){
            if (bestSlanderer != null) {
                main_direction = currLoc.directionTo(bestSlanderer.getLocation());
                Debug.setIndicatorLine(Debug.info, rc.getLocation(), bestSlanderer.getLocation(), 255, 150, 50);
                tryMoveDest(main_direction);
                Debug.println(Debug.info, "Moving towards a slanderer");
            }
            else if (spawnKillRunFromHome) {
                main_direction = currLoc.directionTo(home).opposite();
                tryMoveDest(main_direction);
                Debug.println(Debug.info, "Moving away from home");
            }
            else if (enemyLocation != null) {
                if(!seenEnemyLocation && rc.canSenseLocation(enemyLocation)) {
                    RobotInfo supposedToBeAnEC = rc.senseRobotAtLocation(enemyLocation);
                    if(supposedToBeAnEC == null || supposedToBeAnEC.getType() != RobotType.ENLIGHTENMENT_CENTER) {
                        Debug.println(Debug.info, "Enemy EC not found, going in the enemy direction for 20 turns");
                        Debug.setIndicatorDot(Debug.info, enemyLocation, 255, 0, 0);
                        
                        int dx = enemyLocation.x - currLoc.x;
                        int dy = enemyLocation.y - currLoc.y;
        
                        isEnemyLocEC = false;
                    } else {
                        Debug.println("Enemy EC found");
                    }

                    seenEnemyLocation = true;
                    turnFoundEnemyLoc = rc.getRoundNum();
                }

                boolean rotating = false;
                if(seenEnemyLocation) {
                    if(isEnemyLocEC) {
                        if(rc.getLocation().distanceSquaredTo(enemyLocation) <= actionRadius) {
                            main_direction = rc.getLocation().directionTo(enemyLocation).opposite();
                            Debug.println(Debug.info, "I moving away from the enemy location");
                        } else if(closest_muk_dist <= 4 && rc.getInfluence() < Util.smallMuckThreshold) {
                            main_direction = rc.getLocation().directionTo(closest_muk.getLocation()).opposite();
                            Debug.println(Debug.info, "I moving away from another muck");
                        } else {
                            rotating = true;
                            main_direction = Util.rightOrLeftTurn(spinDirection, enemyLocation.directionTo(currLoc)); //Direction if we only want to rotate around the base
                            Debug.println(Debug.info, "I am rotating around the base");
                        }
                    } else {
                        main_direction = locGotEnemyLocation.directionTo(enemyLocation);
                        Debug.println("Enemy location is not an EC: Continuing past enemyLocation for a few turns");
                    }
                } else {
                    main_direction = rc.getLocation().directionTo(enemyLocation);
                    Debug.println(Debug.info, "I am going straight to the enemy location: " + main_direction);
                }

                Direction[] orderedDirs = Nav.greedyDirection(main_direction, rc);
                boolean moved = false;
                for(Direction dir : orderedDirs) {
                    moved = moved || tryMove(dir);
                }

                if(!moved && rotating && rc.isReady()) {
                    Debug.println(Debug.info, "I am switching rotation direction");
                    spinDirection = Util.switchSpinDirection(spinDirection);
                    main_direction = Util.rightOrLeftTurn(spinDirection, enemyLocation.directionTo(currLoc));
                
                    orderedDirs = Nav.greedyDirection(main_direction, rc);
                    if(orderedDirs != null) {
                        for(Direction dir : orderedDirs) {
                            moved = moved || tryMove(dir);
                        }
                    }
                }

                if(!moved && rc.isReady() && inActionRadiusOfFriendly) {
                    tryMoveDest(main_direction);
                }

                if(!moved && rc.isReady()) {
                    Debug.println("Failed to move. Continuing towards/past enemy location");
                    main_direction = locGotEnemyLocation.directionTo(enemyLocation);
                    tryMoveDest(main_direction);
                }

                if(!seenEnemyLocation) {
                    if(enemyLocation.distanceSquaredTo(rc.getLocation()) < closestDistanceToDest) {
                        closestDistanceToDest = enemyLocation.distanceSquaredTo(rc.getLocation());
                        turnsSinceClosestDistanceDecreased = 0;
                    } else {
                        turnsSinceClosestDistanceDecreased++;
                    }
                    
                    if(turnsSinceClosestDistanceDecreased > Util.attackCallBoredom) {
                        setFlag(Comms.getFlag(Comms.InformationCategory.DELETE_ENEMY_LOC,
                                            enemyLocation.x - home.x + Util.dOffset,
                                            enemyLocation.y - home.y + Util.dOffset));
                        enemyLocation = null;
                        seenEnemyLocation = false;
                    }
                }
                
                Debug.println(Debug.info, "Prioritizing hunting enemy at " + enemyLocation + ". Boredom: " + turnsSinceClosestDistanceDecreased);
                Debug.setIndicatorLine(Debug.info, rc.getLocation(), enemyLocation, 255, 150, 50);
            }
            else {
                boolean moved = false;
                Direction[] orderedDirs = Nav.exploreGreedy(rc);
                if(orderedDirs != null) {
                    for(Direction dir : orderedDirs) {
                        tryMove(dir);
                    }
                    orderedDirs = Util.getOrderedDirections(main_direction);
                    for(Direction dir : orderedDirs) {
                        moved = moved || tryMove(dir);
                    }
                }
                if(!moved && rc.isReady() && inActionRadiusOfFriendly) {
                    tryMoveDest(main_direction);
                }
                Debug.println(Debug.info, "Prioritizing exploring: " + Nav.lastExploreDir);
            }
        }

        // if(turnCount > Util.explorerMuckrakerLifetime) {
        //     changeTo = new LatticeMuckraker(rc, home);
        // }
        
        // This means that the first half of an EC-ID/EC-ID broadcast finished.
        if(needToBroadcastHomeEC && rc.getFlag(rc.getID()) == defaultFlag) { broadcastHomeEC(); }
        else if(broadcastECorSlanderers());
        else if(bestSlanderer != null && broadcastEnemyFound(bestSlanderer.getLocation(), Comms.EnemyType.SLA));
        else if(closestEnemy != null && broadcastEnemyLocalOrGlobal(closestEnemy.getLocation(), closestEnemyType));
    }
}