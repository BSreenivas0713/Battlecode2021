package musketeerplayerqual;

import battlecode.common.*;

import musketeerplayerqual.Util.*;
import musketeerplayerqual.Debug.*;

public class SupportRushPolitician extends Robot {
    static MapLocation enemyLocation;
    static MapLocation ecLoc;
    static MapLocation lastRusherLoc;
    static Direction main_direction;
    static int moveSemaphore;
    static boolean seenRushPol;
    static boolean rusherReady;
    static final int readyRushFlag = Comms.getFlag(Comms.InformationCategory.RUSH_READY);
    
    public SupportRushPolitician(RobotController r, MapLocation enemyLoc) {
        super(r);
        enemyLocation = enemyLoc;
        ecLoc = enemyLoc;
        Nav.setDest(enemyLoc);
        moveSemaphore = 6;
        subRobotType = Comms.SubRobotType.POL_SUPPORT;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
        seenRushPol = false;
        rusherReady = false;
    }
    
    public SupportRushPolitician(RobotController r, MapLocation enemyLoc, MapLocation h, int hID) {
        this(r, enemyLoc);
        home = h;
        homeID = hID;
        friendlyECs.add(home, homeID);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Debug.println(Debug.info, "I am a support rush politician; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        Debug.println(Debug.info, "target map location: x:" + enemyLocation.x + ", y:" + enemyLocation.y);
        Debug.println(Debug.info, "Semaphore: " + moveSemaphore);

        MapLocation currLoc = rc.getLocation();
        RobotInfo robot;
        int minEnemyDistOfRusher = Integer.MAX_VALUE;
        MapLocation closestEnemyToRusher = null;

        boolean seeRushPol = false;
        if (currLoc.isWithinDistanceSquared(ecLoc, actionRadius) || seenRushPol) {
            for(int i = friendlySensable.length - 1; i >= 0; i--) {
                robot = friendlySensable[i];

                if(rc.canGetFlag(robot.getID())) {
                    int flag = rc.getFlag(robot.getID());
                    if(Comms.isSubRobotType(flag, Comms.SubRobotType.POL_HEAD)) {
                        Debug.println("Found rusher. Following him");
                        seenRushPol = true;
                        seeRushPol = true;
                        enemyLocation = robot.getLocation();
                        if(flag == readyRushFlag) {
                            Debug.println("Rusher is ready to empower");
                            rusherReady = true;
                        }
                    }
                }
            }
        }

        if(seenRushPol) {
            RobotInfo[] enemiesNearRushPol = rc.senseNearbyRobots(enemyLocation, enemyLocation.distanceSquaredTo(ecLoc), enemy);
            
            for(int i = enemiesNearRushPol.length - 1; i >= 0; i--) {
                robot = enemiesNearRushPol[i];
                if(robot.getType() != RobotType.ENLIGHTENMENT_CENTER) {
                    MapLocation loc = robot.getLocation();
                    int dist = enemyLocation.distanceSquaredTo(loc);
                    if(dist <= minEnemyDistOfRusher) {
                        minEnemyDistOfRusher = dist;
                        if(closestEnemyToRusher == null ||
                            currLoc.distanceSquaredTo(closestEnemyToRusher) < currLoc.distanceSquaredTo(loc)) {
                            closestEnemyToRusher = loc;
                        }
                    }
                }
            }
        }

        int radius = actionRadius;
        if(closestEnemyToRusher != null) {
            radius = Math.min(currLoc.distanceSquaredTo(closestEnemyToRusher), actionRadius);
        }
        
        Debug.println("Rusher ready: " + rusherReady + ". Seen rusher: " + seenRushPol + ", See rusher: " + seeRushPol);
        if (rc.canEmpower(radius) && 
        (rusherReady || moveSemaphore <= 0 || (seenRushPol && !seeRushPol))) {
            Debug.println(Debug.info, "Empowered with radius: " + radius);
            Debug.setIndicatorLine(Debug.info, rc.getLocation(), closestEnemyToRusher, 255, 150, 50);
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
                if(moved || (seenRushPol && !enemyLocation.equals(lastRusherLoc))) {
                    moveSemaphore = 10;
                } else {
                    moveSemaphore--;
                }
            }
        } else {
            Debug.println(Debug.info, "Using gradient descent for movement");
            main_direction = Nav.gradientDescent();
            tryMoveDest(main_direction);
        }
        lastRusherLoc = enemyLocation;

        Debug.setIndicatorLine(Debug.info, rc.getLocation(), enemyLocation, 255, 150, 50);
    }
}