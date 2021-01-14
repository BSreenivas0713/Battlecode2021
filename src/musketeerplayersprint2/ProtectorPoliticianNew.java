package musketeerplayersprint2;
import battlecode.common.*;

import musketeerplayersprint2.Util.*;
import musketeerplayersprint2.Debug.*;
/*1. blow up if around a muckraker if we also sense base/a slanderer
2. blow up if around 2 or 3 muckrakers 
3. push enemy muckrakers away from base/where slanderers are if we are within 2 sensor radiuses of base or we see a slanderer
4. if within sensor radius of base move away
5. if in sensor radius of base make a move that keeps you most closely within sensor radius of base(in one direction)
6. if too far away from base move back towards base
EXTRA CREDIT: move towards muckrakers near slanderers if when slanderers signal that they are in trouble */
public class ProtectorPoliticianNew extends Robot {
    static RotationDirection spinDirection = Util.RotationDirection.COUNTERCLOCKWISE;
    static Direction main_direction;
    static final int slandererFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.SLANDERER);
    static MapLocation lastSeenSlanderer;
    static int turnLastSeenSlanderer;
    
    public ProtectorPoliticianNew(RobotController r) {
        super(r);
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.POL_PROTECTOR);
        lastSeenSlanderer = null;
        turnLastSeenSlanderer = 0;
    }
    
    public ProtectorPoliticianNew(RobotController r, MapLocation h) {
        super(r);
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.POL_PROTECTOR);
        lastSeenSlanderer = null;
        turnLastSeenSlanderer = 0;
        home = h;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Debug.println(Debug.info, "I am a protector politician; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        RobotInfo[] neutrals = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        MapLocation currLoc = rc.getLocation();

        main_direction = Util.rightOrLeftTurn(spinDirection, home.directionTo(currLoc)); //Direction if we only want to rotate around the base
        /* Creating all the variables that we need to do the step by step decision making for later*/

        int distanceToEC = rc.getLocation().distanceSquaredTo(home);
        
        int maxEnemyAttackableDistSquared = Integer.MIN_VALUE;
        MapLocation farthestEnemyAttackable = null;
        int maxPoliticianSize = 0;
        for (RobotInfo robot : enemyAttackable) {
            int temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp > maxEnemyAttackableDistSquared) {
                maxEnemyAttackableDistSquared = temp;
                farthestEnemyAttackable = robot.getLocation();
            }
            if (robot.getType() == RobotType.POLITICIAN && robot.getConviction() > maxPoliticianSize) {
                maxPoliticianSize = robot.getConviction();
            }
        }

        RobotInfo closestMuckrakerSensable = null;
        int minMuckrakerDistance = Integer.MAX_VALUE;
        RobotInfo minRobot = null;
        double minDistSquared = Integer.MAX_VALUE;

        for (RobotInfo robot : enemySensable) {
            int currDistance = robot.getLocation().distanceSquaredTo(home);
            if (robot.getType() == RobotType.MUCKRAKER && currDistance < minMuckrakerDistance) {
                closestMuckrakerSensable = robot;
                minMuckrakerDistance = currDistance;
            }
            int temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp < minDistSquared) {
                minDistSquared = temp;
                minRobot = robot;
            }
        }
        
        boolean slandererOrECNearby = false;
        boolean slandererNearby = false;
        MapLocation nearestSlandy = null;
        int nearestSlandyDist = Integer.MAX_VALUE;
        boolean ECNearby = false;
        int numFollowingClosestMuckraker = 0;
        for (RobotInfo robot : friendlySensable) {
            if ((robot.getType() == RobotType.ENLIGHTENMENT_CENTER)){
                slandererOrECNearby = true;
                ECNearby = true;
            } else {
                MapLocation tempLoc = robot.getLocation();
                int tempDist = currLoc.distanceSquaredTo(tempLoc);
                if(rc.canGetFlag(robot.getID())) {
                    int flag = rc.getFlag(robot.getID());
                    // Only slanderers and EC's broadcast AVG_ENEMY_DIR so this is valid to check for slanderers
                    if(flag == slandererFlag || Comms.getIC(flag) == Comms.InformationCategory.AVG_ENEMY_DIR) {
                        slandererOrECNearby = true;
                        slandererNearby = true;
                        if (tempDist < nearestSlandyDist) {
                            nearestSlandy = tempLoc;
                            nearestSlandyDist = tempDist;
                        }
                    }
    
                    if(closestMuckrakerSensable != null) {
                        if(flag == Comms.getFlag(Comms.InformationCategory.FOLLOWING, closestMuckrakerSensable.getID())) {
                            numFollowingClosestMuckraker++;
                        }
                    }
                }
            }
        }

        if(nearestSlandy != null) {
            lastSeenSlanderer = nearestSlandy;
            turnLastSeenSlanderer = rc.getRoundNum();
        }
        
        if(rc.getRoundNum() > turnLastSeenSlanderer + Util.turnsSlandererLocValid) {
            lastSeenSlanderer = null;
        }

        if (minRobot != null) {
            broadcastEnemyFound(minRobot.getLocation());
        }
        
        /* Step by Step decision making*/
        //empower if near 2 enemies or enemy is in sensing radius of our base
        if (((maxPoliticianSize > 0 && maxPoliticianSize <= 1.5 * rc.getInfluence()) || 
            (enemyAttackable.length > 1 || 
            (enemyAttackable.length > 0 && (slandererNearby || minMuckrakerDistance <= RobotType.ENLIGHTENMENT_CENTER.sensorRadiusSquared))))
            && rc.canEmpower(maxEnemyAttackableDistSquared)) {
            Debug.println(Debug.info, "Enemy too close to base. I will empower");
            Debug.setIndicatorLine(Debug.info, rc.getLocation(), farthestEnemyAttackable, 255, 150, 50);
            rc.empower(maxEnemyAttackableDistSquared);
            return;
        }

        //tries to block a muckraker in its path(if the muckraker is within 2 sensing radiuses of the EC)
        if (closestMuckrakerSensable != null && 
            closestMuckrakerSensable.getLocation().isWithinDistanceSquared(home, (5 * sensorRadius)) &&
            numFollowingClosestMuckraker < Util.maxFollowingSingleUnit) {
            Debug.println(Debug.info, "I am pushing a muckraker away. ID: " + closestMuckrakerSensable.getID());
            setFlag(Comms.getFlag(Comms.InformationCategory.FOLLOWING, closestMuckrakerSensable.getID()));
            resetFlagOnNewTurn = false;
            Debug.println(Debug.info, "I am pushing a muckraker away");
            MapLocation closestMuckrakerSensableLoc = closestMuckrakerSensable.getLocation();
            Direction muckrakerPathtoBase = closestMuckrakerSensableLoc.directionTo(home);
            MapLocation squareToBlock = closestMuckrakerSensableLoc.add(muckrakerPathtoBase);
            Direction toMove = rc.getLocation().directionTo(squareToBlock);
            tryMoveDest(toMove);
            return;
        } else {
            resetFlagOnNewTurn = true;
        }

        //moves out of sensor radius of Enlightenment Center
        if (ECNearby) {
            Debug.println(Debug.info, "I am moving away from the base");
            main_direction = Util.rotateInSpinDirection(spinDirection, currLoc.directionTo(home).opposite());
            Debug.setIndicatorLine(Debug.pathfinding, currLoc, lastSeenSlanderer, 200, 0, 255);
        }
        else if(lastSeenSlanderer != null) {
            Debug.setIndicatorDot(Debug.pathfinding, lastSeenSlanderer, 200, 0, 255);
            Debug.setIndicatorLine(Debug.pathfinding, currLoc, lastSeenSlanderer, 200, 0, 255);

            int distToSlandy = currLoc.distanceSquaredTo(lastSeenSlanderer);
            int distSlandyToHome = lastSeenSlanderer.distanceSquaredTo(home); // TODO: Make this distSlandyToNearestEC
            int distToHome = currLoc.distanceSquaredTo(home);                 // TODO: Make this distToNearestEC
            if(distSlandyToHome >= distToHome + 2) {
                Debug.println(Debug.info, "I am moving to the outside of the slanderer");
                main_direction = currLoc.directionTo(lastSeenSlanderer);
            } else {
                Debug.println(Debug.info, "I am rotating around last seen slanderer");
                if(distToSlandy > Util.maxRotationRadius) {
                    main_direction = Util.rotateOppositeSpinDirection(spinDirection, currLoc.directionTo(nearestSlandy));
                    Debug.println(Debug.info, "Rotating TOWARDS");
                } else if(distToSlandy < Util.minRotationRadius) {
                    main_direction = Util.rotateInSpinDirection(spinDirection, currLoc.directionTo(nearestSlandy).opposite());
                    Debug.println(Debug.info, "Rotating AWAY");
                } else {
                    main_direction = Util.rightOrLeftTurn(spinDirection, lastSeenSlanderer.directionTo(currLoc));
                    Debug.println(Debug.info, "Rotating EXACTLY");
                }
            }

        }
        // else rotate towards home
        else {
            Debug.println(Debug.info, "I see no slanderers. Rotating towards home");
            main_direction = Util.rotateOppositeSpinDirection(spinDirection, currLoc.directionTo(home));
        }

        MapLocation target = currLoc.add(main_direction);
        Debug.setIndicatorLine(Debug.pathfinding, currLoc, target, 100, 100, 255);

        MapLocation lookAhead = currLoc.translate(main_direction.getDeltaX() * 2, main_direction.getDeltaY() * 2);
        if(!rc.onTheMap(lookAhead)) {
            Debug.println(Debug.info, "Close to a wall: Switching direction");
            spinDirection = Util.switchSpinDirection(spinDirection);
        }

        //Rotates around slanderers and then home
        int tryMove = 0;
        while (!tryMoveDest(main_direction) && rc.isReady() && tryMove <= 1){
            Debug.println(Debug.info, "Try move failed: I am switching rotation direction");
            spinDirection = Util.switchSpinDirection(spinDirection);
            if (nearestSlandy != null) {
                main_direction = Util.rightOrLeftTurn(spinDirection, nearestSlandy.directionTo(currLoc));
            } else {
                main_direction = Util.rightOrLeftTurn(spinDirection, home.directionTo(currLoc));
            }
            tryMove +=1;
        }

        broadcastECLocation();
        return;
    }
}