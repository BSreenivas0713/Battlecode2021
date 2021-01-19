package musketeerplayerqual;
import battlecode.common.*;

import musketeerplayerqual.Util.*;
import musketeerplayerqual.Debug.*;
import musketeerplayerqual.fast.FastIterableLocSet;

public class HunterMuckracker extends Robot {
    static Direction main_direction;
    static MapLocation enemyLocation;
    static boolean seenEnemyLocation;
    static RotationDirection spinDirection = Util.RotationDirection.COUNTERCLOCKWISE;

    public HunterMuckracker(RobotController r, MapLocation enemyLoc) {
        super(r);
        subRobotType = Comms.SubRobotType.MUC_HUNTER;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
        enemyLocation = enemyLoc;
        seenEnemyLocation = false;
    }

    public HunterMuckracker(RobotController r) {
        this(r, null);
    }

    public HunterMuckracker(RobotController r, MapLocation enemyLoc, MapLocation h, int hID) {
        this(r, enemyLoc);
        home = h;
        homeID = hID;
        friendlyECs.add(home, homeID);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        MapLocation currLoc = rc.getLocation();

        Debug.println(Debug.info, "I am a hunter Mucker; current influence: " + rc.getInfluence() + "; current conviction: " + rc.getConviction());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        if(enemyLocation != null) {
            Debug.println(Debug.info, "enemy location: " + enemyLocation);
        }

        if(main_direction == null){
            main_direction = Util.randomDirection();
        }

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


        boolean muckraker_Found_EC = false;

        RobotInfo bestSlanderer = null;
        bestInfluence = Integer.MIN_VALUE;
        double minDistSquared = Integer.MAX_VALUE;
        int totalEnemyX = 0;
        int totalEnemyY = 0;
        int enemiesFound = 0;
        RobotInfo closestEnemy = null;
        Comms.EnemyType closestEnemyType = null;
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
                if(robot.getType() == RobotType.MUCKRAKER) {
                    closestEnemyType = Comms.EnemyType.MUC;
                } else {
                    closestEnemyType = Comms.EnemyType.UNKNOWN;
                }
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
                }
            }
        }

        RobotInfo closest_muk = null;
        int closest_muk_dist = Integer.MAX_VALUE;

        boolean awayFromBase = false;
        RobotInfo friendlyBase = null;
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

                if(closestEnemy != null) {
                    if(flag == Comms.getFlag(Comms.InformationCategory.FOLLOWING, closestEnemy.getID())) {
                        numFollowingClosestEnemy++;
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
        
        boolean setFollowingFlag = false;

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

                Debug.println(Debug.info, "Prioritizing hunting base at " + enemyLocation);
                Debug.setIndicatorLine(Debug.info, rc.getLocation(), enemyLocation, 255, 150, 50);
            }
            else if (awayFromBase) {
                tryMoveDest(currLoc.directionTo(friendlyBase.getLocation()).opposite());
                Debug.println(Debug.info, "Prioritizing moving away from friendly.");
            }
            else if (enemiesFound != 0 && numFollowingClosestEnemy < Util.maxFollowingSingleUnit) {
                MapLocation hunterLoc = new MapLocation(totalEnemyX / enemiesFound, totalEnemyY / enemiesFound);
                setFlag(Comms.getFlag(Comms.InformationCategory.FOLLOWING, closestEnemy.getID()));
                setFollowingFlag = true;
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
        
        if(!setFollowingFlag) {
            // This means that the first half of an EC-ID/EC-ID broadcast finished.
            if(needToBroadcastHomeEC && rc.getFlag(rc.getID()) == defaultFlag) { broadcastHomeEC(); }
            else if(broadcastECLocation());
            else if(bestSlanderer != null && broadcastEnemyFound(bestSlanderer.getLocation(), Comms.EnemyType.SLA));
            else if(closestEnemy != null && broadcastEnemyLocalOrGlobal(closestEnemy.getLocation(), closestEnemyType));
        }
    }
}