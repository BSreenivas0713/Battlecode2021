package musketeerplayerqual;

import battlecode.common.*;

import musketeerplayerqual.Util.*;
import musketeerplayerqual.Debug.*;

public class HeadRushPolitician extends Robot {
    static MapLocation enemyLocation;
    static Direction main_direction;
    static int moveSemaphore;
    static boolean seenSupportPol;
    static boolean supportPolEmpowered;
    static int readySemaphore;
    static boolean readyToEmpower;
    
    public HeadRushPolitician(RobotController r, MapLocation enemyLoc) {
        super(r);
        enemyLocation = enemyLoc;
        Nav.setDest(enemyLoc);
        moveSemaphore = 4;
        subRobotType = Comms.SubRobotType.POL_HEAD;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
        seenSupportPol = false;
        supportPolEmpowered = false;
        readySemaphore = 10;
        readyToEmpower = false;
    }
    
    public HeadRushPolitician(RobotController r, MapLocation enemyLoc, MapLocation h, int hID) {
        this(r, enemyLoc);
        home = h;
        homeID = hID;
        friendlyECs.add(home, homeID);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Debug.println(Debug.info, "I am a head rush politician; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        Debug.println(Debug.info, "target map location: x:" + enemyLocation.x + ", y:" + enemyLocation.y);
        Debug.println(Debug.info, "Semaphore: " + moveSemaphore);

        MapLocation currLoc = rc.getLocation();
        RobotInfo[] neutrals = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        RobotInfo robot;
        RobotInfo ec = null;
        int distToEC = Integer.MAX_VALUE;
        MapLocation closestEnemy = null;

        if (rc.getTeamVotes() < 751 && rc.getRoundNum() >= 1450) changeTo = new CleanupPolitician(rc, home, homeID);

        for(int i = enemyAttackable.length - 1; i >= 0; i--) {
            robot = enemyAttackable[i];
            MapLocation loc = robot.getLocation();
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER && 
                enemyLocation.isWithinDistanceSquared(loc, 8)) {
                int dist = currLoc.distanceSquaredTo(loc);
                if(dist < distToEC) {
                    distToEC = dist;
                    closestEnemy = loc;
                    ec = robot;
                }
            }
        }
        
        for(int i = neutrals.length - 1; i >= 0; i--) {
            robot = neutrals[i];
            MapLocation loc = robot.getLocation();
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER && 
                enemyLocation.isWithinDistanceSquared(loc, 8)) {
                int dist = currLoc.distanceSquaredTo(loc);
                if(dist < distToEC) {
                    distToEC = dist;
                    closestEnemy = loc;
                    ec = robot;
                }
            }
        }

        if (distToEC == Integer.MAX_VALUE) {
            for(int i = friendlySensable.length - 1; i >= 0; i--) {
                robot = friendlySensable[i];
                MapLocation loc = robot.getLocation();
                int dist = currLoc.distanceSquaredTo(loc);
                if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && enemyLocation.isWithinDistanceSquared(loc, 8)) {
                    if (robot.getConviction() > 50) {
                        changeTo = new RushPolitician(rc, null);
                    } else if (dist < distToEC) {
                        distToEC = dist;
                        closestEnemy = loc;
                    }
                }
            }
        }

        boolean canConvertEC = false;
        if(distToEC == Integer.MAX_VALUE) {
            distToEC = currLoc.distanceSquaredTo(enemyLocation);
        } else {
            RobotInfo[] botsInAttack = rc.senseNearbyRobots(distToEC);
            int damagePerBot = (int) ((rc.getConviction() - 10) * rc.getEmpowerFactor(rc.getTeam(), 0) / botsInAttack.length);
            if(ec != null && damagePerBot > ec.getConviction()) {
                canConvertEC = true;
            }
        }

        if((distToEC <= 1 || moveSemaphore <= 0) && rc.isReady()) {
            subRobotType = Comms.SubRobotType.POL_HEAD_READY;
            defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
            nextFlag = defaultFlag;
            setFlag(nextFlag);
            readyToEmpower = true;
        }

        boolean seeSupportPol = false;
        if (currLoc.isWithinDistanceSquared(enemyLocation, actionRadius)) {
            for(int i = friendlySensable.length - 1; i >= 0; i--) {
                robot = friendlySensable[i];
                MapLocation loc = robot.getLocation();
                if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && enemyLocation.isWithinDistanceSquared(loc, 8)) {
                    int dist = currLoc.distanceSquaredTo(loc);
                    if(dist < distToEC) {
                        distToEC = dist;
                        closestEnemy = loc;
                    }
                }

                if(readyToEmpower) {
                    if(rc.canGetFlag(robot.getID())) {
                        int flag = rc.getFlag(robot.getID());
                        if(Comms.isSubRobotType(flag, Comms.SubRobotType.POL_SUPPORT)) {
                            if(!seenSupportPol) {
                                readySemaphore = 10;
                            }
                            seenSupportPol = true;
                            seeSupportPol = true;
                        }
                    }
                }
            }
        }

        if(readyToEmpower) {
            if(rc.isReady()) {
                readySemaphore--;
            }
            Debug.println("In empower position but waiting for support pol. readySemaphore: " + readySemaphore);
            Debug.println("Seen support: " + seenSupportPol + ", See support: " + seeSupportPol);
        } else {
            broadcastSlanderers();
        }
        
        if (rc.canEmpower(distToEC)) {
            if(readyToEmpower && seenSupportPol && !seeSupportPol) {
                Debug.println(Debug.info, "Support pol empowered. Empowered with radius: " + distToEC);
                Debug.setIndicatorLine(Debug.info, rc.getLocation(), enemyLocation, 255, 150, 50);
                rc.empower(distToEC);
                return;
            // } else if(canConvertEC) {
            //     Debug.println(Debug.info, "Can convert EC immediately. Empowered with radius: " + distToEC);
            //     Debug.setIndicatorLine(Debug.info, rc.getLocation(), enemyLocation, 255, 150, 50);
            //     rc.empower(distToEC);
            //     return;
            } else if(rc.senseNearbyRobots(distToEC).length == 1) {
                Debug.println(Debug.info, "Only EC in attack radius. Empowered with radius: " + distToEC);
                Debug.setIndicatorLine(Debug.info, rc.getLocation(), enemyLocation, 255, 150, 50);
                rc.empower(distToEC);
                return;
            } else if(readySemaphore <= 0) {
                Debug.println(Debug.info, "Ready semaphore <= 0. Empowered with radius: " + distToEC);
                Debug.setIndicatorLine(Debug.info, rc.getLocation(), enemyLocation, 255, 150, 50);
                rc.empower(distToEC);
                return;
            }
        }

        if(currLoc.isWithinDistanceSquared(enemyLocation, actionRadius)) {
            Debug.println(Debug.info, "Close to EC; using heuristic for movement");
            main_direction = rc.getLocation().directionTo(enemyLocation);
            if(rc.isReady()) {
                boolean moved = false;
                if(!currLoc.isWithinDistanceSquared(enemyLocation, 1)) {
                    moved = tryMove(main_direction) || tryMove(main_direction.rotateRight()) || tryMove(main_direction.rotateLeft());
                }
                if(moved) {
                    moveSemaphore = 5;
                } else {
                    moveSemaphore--;
                }
            }
        } else {
            Debug.println(Debug.info, "Using gradient descent for movement");
            main_direction = Nav.gradientDescent();
            tryMoveDest(main_direction);
        }

        Debug.setIndicatorDot(Debug.info, enemyLocation, 255, 0, 0);
        Debug.setIndicatorLine(Debug.info, rc.getLocation(), enemyLocation, 255, 150, 50);
    }
}