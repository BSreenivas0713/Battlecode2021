package musketeerplayerqual;

import battlecode.common.*;

import musketeerplayerqual.Util.*;
import musketeerplayerqual.Debug.*;

public class RushPolitician extends Robot {
    static MapLocation enemyLocation;
    static Direction main_direction;
    static int moveSemaphore;
    static boolean seenEnemyLocation;
    static int turnsSinceClosestDistanceDecreased;
    static int closestDistanceToDest;
    
    public RushPolitician(RobotController r, MapLocation enemyLoc) {
        super(r);
        enemyLocation = enemyLoc;
        seenEnemyLocation = false;
        if (enemyLocation != null) {
            Nav.setDest(enemyLocation);
            subRobotType = Comms.SubRobotType.POL_ACTIVE_RUSH;
        } else {
            subRobotType = Comms.SubRobotType.POL_DORMANT_RUSH;
        }
        moveSemaphore = 2;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
    }
    
    public RushPolitician(RobotController r, MapLocation enemyLoc, MapLocation h, int hID) {
        this(r, enemyLoc);
        home = h;
        homeID = hID;
        friendlyECs.add(home, homeID);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Debug.println(Debug.info, "I am a rush politician; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        if (enemyLocation != null) {
            Debug.println(Debug.info, "target map location: x:" + enemyLocation.x + ", y:" + enemyLocation.y);
        } else {
            Debug.println(Debug.info, "I have no target. I will explore around...");
        }
        Debug.println(Debug.info, "Semaphore: " + moveSemaphore);

        MapLocation currLoc = rc.getLocation();
        RobotInfo[] neutrals = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        RobotInfo robot;
        int minEnemyDistSquared = Integer.MAX_VALUE;
        MapLocation closestEnemy = null;
        if (enemyLocation == null) {
            for (int i = enemySensable.length - 1; i >= 0; i--) {
                robot = enemySensable[i];
                MapLocation loc = robot.getLocation();
                if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && robot.getInfluence() <= 600) {
                    Debug.println("Nearby enemy EC: Setting enemyLocation");
                    enemyLocation = loc;
                    subRobotType = Comms.SubRobotType.POL_ACTIVE_RUSH;
                    defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
                    nextFlag = defaultFlag;
                    setFlag(nextFlag);
                    Nav.setDest(loc);
                }
            }

            if(homeID != -1 && rc.canGetFlag(homeID)) {
                Debug.println(Debug.info, "Checking home flag");
                int flag = rc.getFlag(homeID);
                Comms.InformationCategory IC = Comms.getIC(flag);
                switch(IC) {
                    case ENEMY_EC:
                        if(enemyLocation == null) {
                            int[] dxdy = Comms.getDxDy(flag);
                            if(dxdy[0] != 0 && dxdy[1] != 0) {
                                MapLocation enemyLoc = new MapLocation(dxdy[0] + home.x - Util.dOffset, dxdy[1] + home.y - Util.dOffset);
                                Debug.setIndicatorDot(Debug.info, enemyLoc, 255, 0, 0);
                                Debug.println("dx: " + dxdy[0] + ", dy: " + dxdy[1]);
    
                                Comms.GroupRushType GRtype = Comms.getRushType(flag);
                                int GRmod = Comms.getRushMod(flag);
                                Debug.println(Debug.info, "EC is sending a rush: Read ENEMY_EC flag. Type: " + GRtype + ", mod: " + GRmod);
    
                                if((rc.getID() % 4 == GRmod && GRtype == Comms.GroupRushType.MUC_POL)) {
                                    Debug.println(Debug.info, "Joining the rush");
                                    enemyLocation = enemyLoc;
                                    seenEnemyLocation = false;
                                    subRobotType = Comms.SubRobotType.POL_ACTIVE_RUSH;
                                    defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
                                    nextFlag = defaultFlag;
                                    setFlag(nextFlag);
                                    Nav.setDest(enemyLocation);
                                } else {
                                    Debug.println(Debug.info, "I was not included in this rush");
                                }
                            }
                        }
                        break;
                    case DELETE_ENEMY_LOC:
                        if(enemyLocation != null && !seenEnemyLocation) {
                            Debug.println("EC signaled to delete enemy location");
                            int[] dxdy = Comms.getDxDy(flag);
                            MapLocation enemyLoc = new MapLocation(dxdy[0] + home.x - Util.dOffset, dxdy[1] + home.y - Util.dOffset);
    
                            if(enemyLocation.equals(enemyLoc)) {
                                enemyLocation = null;
                                seenEnemyLocation = false;
                                subRobotType = Comms.SubRobotType.POL_DORMANT_RUSH;
                                defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
                                nextFlag = defaultFlag;
                                setFlag(nextFlag);
                            }
                        }
                        break;
                }
            } else {
                Debug.println("Home was taken. Rushing home");
                enemyLocation = home;
                homeID = -1;
                Nav.setDest(enemyLocation);
            }
            
            int maxMuckAttackableDistSquared = Integer.MIN_VALUE;
            int minMuckAttackableDistSquared = Integer.MAX_VALUE;
            MapLocation farthestMuckAttackable = null;
            MapLocation closestMuckAttackable = null;
            int numMuckAttackable = 0;
            int maxMuckrakerAttackableSize = 0;
            int closestBuffMuckDist = Integer.MAX_VALUE;
            RobotInfo closestBuffMuck = null;
            int numFriendlyAttackable = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam()).length;
            
            for(int i = enemyAttackable.length - 1; i >= 0; i--) {
                robot = enemyAttackable[i];
                int temp = currLoc.distanceSquaredTo(robot.getLocation());
                if(robot.getType() == RobotType.MUCKRAKER) {
                    numMuckAttackable++;
                    if(temp > maxMuckAttackableDistSquared) {
                        maxMuckAttackableDistSquared = temp;
                        farthestMuckAttackable = robot.getLocation();
                    } else if(temp < minMuckAttackableDistSquared) {
                        minMuckAttackableDistSquared = temp;
                        closestMuckAttackable = robot.getLocation();
                    }
                    if(robot.getConviction() > maxMuckrakerAttackableSize) {
                        maxMuckrakerAttackableSize = robot.getConviction();
                    }
                }

                if(robot.getType() == RobotType.MUCKRAKER &&
                    closestBuffMuckDist > currLoc.distanceSquaredTo(robot.getLocation())) {
                    closestBuffMuckDist = currLoc.distanceSquaredTo(robot.getLocation());
                    closestBuffMuck = robot;
                }
            }
    
            boolean amClosestPolToClosestMuck = true;
            MapLocation tempLoc;
            int tempDist;
            boolean slandererNearby = false;

            for(int i = friendlySensable.length - 1; i >= 0; i--) {
                robot = friendlySensable[i];
                tempLoc = robot.getLocation();
                tempDist = currLoc.distanceSquaredTo(tempLoc);
                if(rc.canGetFlag(robot.getID())) {
                    int flag = rc.getFlag(robot.getID());
                    Comms.InformationCategory IC = Comms.getIC(flag);
                    if(Comms.isSubRobotType(flag, Comms.SubRobotType.POL_PROTECTOR) ||
                        (robot.getType() == RobotType.POLITICIAN && IC == Comms.InformationCategory.FOLLOWING)) {
                        if(closestMuckAttackable != null && 
                            minMuckAttackableDistSquared > tempLoc.distanceSquaredTo(closestMuckAttackable)) {
                            Debug.setIndicatorDot(Debug.info, tempLoc, 255, 255, 255);
                            Debug.setIndicatorLine(Debug.info, currLoc, tempLoc, 255, 255, 255);
                            Debug.println("Found a pol closer to my closest muck");
                            amClosestPolToClosestMuck = false;
                        }
                    }

                    if(Comms.isSubRobotType(flag, Comms.SubRobotType.SLANDERER)) {
                        slandererNearby = true;
                    }
                }
            }
            
            if ((closestBuffMuck != null && closestBuffMuck.getConviction() >= rc.getConviction() / 3)
                && rc.canEmpower(closestBuffMuckDist)) {
                Debug.println(Debug.info, "Big enemy nearby: Empowering with radius: " + closestBuffMuckDist);
                Debug.setIndicatorLine(Debug.info, rc.getLocation(), closestBuffMuck.getLocation(), 255, 150, 50);
                rc.empower(closestBuffMuckDist);
                return;
            }
            
            if ((numMuckAttackable > 3 || 
            (numMuckAttackable > 1 && slandererNearby && amClosestPolToClosestMuck)) &&
                rc.canEmpower(maxMuckAttackableDistSquared)) {
                Debug.println(Debug.info, "Enemy too close to base. I will empower with radius: " + maxMuckAttackableDistSquared);
                Debug.setIndicatorLine(Debug.info, rc.getLocation(), farthestMuckAttackable, 255, 150, 50);
                rc.empower(maxMuckAttackableDistSquared);
                return;
            }
        } else {
            for(int i = enemyAttackable.length - 1; i >= 0; i--) {
                robot = enemyAttackable[i];
                MapLocation loc = robot.getLocation();
                if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER && enemyLocation.isWithinDistanceSquared(loc, 8)) {
                    int dist = currLoc.distanceSquaredTo(loc);
                    if(dist < minEnemyDistSquared) {
                        minEnemyDistSquared = dist;
                        closestEnemy = loc;
                    }
                }
            }
            for(int i = neutrals.length - 1; i >= 0; i--) {
                robot = neutrals[i];
                MapLocation loc = robot.getLocation();
                if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER && 
                    enemyLocation.isWithinDistanceSquared(loc, 8)) {
                    int dist = currLoc.distanceSquaredTo(loc);
                    if(dist < minEnemyDistSquared) {
                        minEnemyDistSquared = dist;
                        closestEnemy = loc;
                    }
                }
            }

            if (minEnemyDistSquared == Integer.MAX_VALUE) {
                for(int i = friendlySensable.length - 1; i >= 0; i--) {
                    robot = friendlySensable[i];
                    MapLocation loc = robot.getLocation();
                    int dist = currLoc.distanceSquaredTo(loc);
                    if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && enemyLocation.isWithinDistanceSquared(loc, 8)) {
                        if (robot.getConviction() > 50) {
                            setFlag(Comms.getFlag(Comms.InformationCategory.DELETE_ENEMY_LOC,
                                                enemyLocation.x - home.x + Util.dOffset,
                                                enemyLocation.y - home.y + Util.dOffset));
                            enemyLocation = null;
                            seenEnemyLocation = false;
                            subRobotType = Comms.SubRobotType.POL_DORMANT_RUSH;
                            defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
                            nextFlag = defaultFlag;
                            setFlag(nextFlag);
                            if(homeID == -1) {
                                homeID = robot.getID();
                                home = robot.getLocation();
                            }
                        } else if (dist < minEnemyDistSquared) {
                            minEnemyDistSquared = dist;
                            closestEnemy = loc;
                        }
                    }

                    if(rc.canGetFlag(robot.getID())) {
                        int flag = rc.getFlag(robot.getID());
                        if(Comms.isSubRobotType(flag, Comms.SubRobotType.POL_HEAD) && rc.getConviction() <= robot.getConviction() / 2) {
                            Debug.println("Found rusher. Following him");
                            changeTo = new SupportRushPolitician(rc, enemyLocation);
                        }
                    }
                }
            }
        }
        
        if (rc.canEmpower(minEnemyDistSquared) && (moveSemaphore <= 0 || minEnemyDistSquared <= 1)) {
            int radius = Math.min(actionRadius, minEnemyDistSquared);
            Debug.println(Debug.info, "Empowered with radius: " + radius);
            Debug.setIndicatorLine(Debug.info, rc.getLocation(), closestEnemy, 255, 150, 50);
            rc.empower(radius);
            return;
        }

        if(enemyLocation != null && currLoc.isWithinDistanceSquared(enemyLocation, actionRadius)) {
            Debug.println(Debug.info, "Close to EC; using heuristic for movement");
            main_direction = rc.getLocation().directionTo(enemyLocation);
            if(rc.isReady()) {
                boolean moved = tryMove(main_direction) || tryMove(main_direction.rotateRight()) || tryMove(main_direction.rotateLeft());
                if(moved) {
                    moveSemaphore = 2;
                } else {
                    moveSemaphore--;
                }
            }
            broadcastSlanderers();
        } else if (enemyLocation != null) {
            Debug.println(Debug.info, "Using gradient descent for movement");
            main_direction = Nav.gradientDescent();
            tryMoveDest(main_direction);
            
            if(!seenEnemyLocation && rc.canSenseLocation(enemyLocation)) {
                seenEnemyLocation = true;
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
                    subRobotType = Comms.SubRobotType.POL_DORMANT_RUSH;
                    defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
                    nextFlag = defaultFlag;
                    setFlag(nextFlag);
                    Debug.println("Got bored of hunting location. Signlaing deletion");
                }
            }
        } else {
            Direction[] orderedDirs = Nav.exploreGreedy(rc);
            boolean moved = false;
            if(orderedDirs != null) {
                for(Direction dir : orderedDirs) {
                    moved = moved || tryMove(dir);
                }
                orderedDirs = Util.getOrderedDirections(Nav.lastExploreDir);
                for(Direction dir : orderedDirs) {
                    moved = moved || tryMove(dir);
                }
            }
            
            // This means that the first half of an EC-ID/EC-ID broadcast finished.
            if(needToBroadcastHomeEC && rc.getFlag(rc.getID()) == defaultFlag) { broadcastHomeEC(); }
            else if(broadcastECorSlanderers());
        }

        Debug.setIndicatorLine(Debug.info, rc.getLocation(), enemyLocation, 255, 150, 50);
    }
}