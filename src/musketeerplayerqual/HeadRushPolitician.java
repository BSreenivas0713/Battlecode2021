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
        readySemaphore = 5;
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
        int distToEC = Integer.MAX_VALUE;
        MapLocation closestEnemy = null;

        for(int i = enemyAttackable.length - 1; i >= 0; i--) {
            robot = enemyAttackable[i];
            MapLocation loc = robot.getLocation();
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER && 
                enemyLocation.isWithinDistanceSquared(loc, 8)) {
                int dist = currLoc.distanceSquaredTo(loc);
                if(dist < distToEC) {
                    distToEC = dist;
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
                if(dist < distToEC) {
                    distToEC = dist;
                    closestEnemy = loc;
                }
            }
        }

        if(distToEC == Integer.MAX_VALUE) {
            distToEC = currLoc.distanceSquaredTo(enemyLocation);
        }

        if((distToEC <= 1 || moveSemaphore <= 0) && rc.isReady()) {
            defaultFlag = Comms.getFlag(Comms.InformationCategory.RUSH_READY);
            nextFlag = defaultFlag;
            setFlag(defaultFlag);
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
        }
        
        if (rc.canEmpower(distToEC) && 
        ((readyToEmpower && seenSupportPol && !seeSupportPol) || 
        (readySemaphore <= 0) ||
        (rc.senseNearbyRobots(distToEC).length == 1))) {
            int radius = Math.min(actionRadius, distToEC);
            Debug.println(Debug.info, "Empowered with radius: " + radius);
            Debug.setIndicatorLine(Debug.info, rc.getLocation(), enemyLocation, 255, 150, 50);
            rc.empower(radius);
            return;
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
                    moveSemaphore = 4;
                } else {
                    moveSemaphore--;
                }
            }
        } else {
            Debug.println(Debug.info, "Using gradient descent for movement");
            main_direction = Nav.gradientDescent();
            tryMoveDest(main_direction);
        }

        Debug.setIndicatorLine(Debug.info, rc.getLocation(), enemyLocation, 255, 150, 50);
    }
}