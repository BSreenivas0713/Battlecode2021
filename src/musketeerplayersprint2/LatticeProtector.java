package musketeerplayersprint2;
import battlecode.common.*;

import musketeerplayersprint2.Util.*;
import musketeerplayersprint2.Debug.*;
import musketeerplayersprint2.fast.FastIterableLocSet;
/*1. blow up if around a muckraker if we also sense base/a slanderer
2. blow up if around 2 or 3 muckrakers 
3. push enemy muckrakers away from base/where slanderers are if we are within 2 sensor radiuses of base or we see a slanderer
4. if within sensor radius of base move away
5. if in sensor radius of base make a move that keeps you most closely within sensor radius of base(in one direction)
6. if too far away from base move back towards base
EXTRA CREDIT: move towards muckrakers near slanderers if when slanderers signal that they are in trouble */
public class LatticeProtector extends Robot {
    static RotationDirection spinDirection = Util.RotationDirection.COUNTERCLOCKWISE;
    static Direction main_direction;
    static final int slandererFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.SLANDERER);
    static MapLocation lastSeenSlanderer;
    static int turnLastSeenSlanderer;
    static FastIterableLocSet seenECs;
    static MapLocation currMinEC;
    
    public LatticeProtector(RobotController r) {
        super(r);
        subRobotType = Comms.SubRobotType.POL_PROTECTOR;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
        lastSeenSlanderer = null;
        turnLastSeenSlanderer = 0;
        seenECs = new FastIterableLocSet();
        seenECs.add(home);
        currMinEC = home;
    }
    
    public LatticeProtector(RobotController r, MapLocation h) {
        this(r);
        home = h;
    }

    public MapLocation seenECmin(MapLocation currLoc) {
        int min = Integer.MAX_VALUE;
        MapLocation minLoc = null;
        seenECs.updateIterable();
        for(int j = seenECs.size - 1; j >= 0; j--) {
            MapLocation loc = seenECs.locs[j];
            int tempDist = loc.distanceSquaredTo(currLoc);
            if (tempDist < min) {
                min = tempDist;
                minLoc = loc;
            }
        }

        return minLoc;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Debug.println(Debug.info, "I am a protector politician; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        RobotInfo[] neutrals = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        MapLocation currLoc = rc.getLocation();

        main_direction = Util.rightOrLeftTurn(spinDirection, currMinEC.directionTo(currLoc)); //Direction if we only want to rotate around the base
        /* Creating all the variables that we need to do the step by step decision making for later*/

        int distanceToEC = rc.getLocation().distanceSquaredTo(currMinEC);
        
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
            MapLocation tempLoc = robot.getLocation();
            int currDistance = tempLoc.distanceSquaredTo(currMinEC);
            if (robot.getType() == RobotType.MUCKRAKER && currDistance < minMuckrakerDistance) {
                closestMuckrakerSensable = robot;
                minMuckrakerDistance = currDistance;
            }
            int temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp < minDistSquared) {
                minDistSquared = temp;
                minRobot = robot;
            }
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                if (seenECs.contains(tempLoc)) {
                    seenECs.remove(tempLoc);
                }
                int distToEmpower = currLoc.distanceSquaredTo(robot.getLocation());
                if (rc.canEmpower(distToEmpower)) {
                    rc.empower(distToEmpower);
                }
            }
        }
        
        int closestProtectorDist = Integer.MAX_VALUE;
        MapLocation closestProtectorLoc = null;
        boolean ProtectorNearby = false;
        for(RobotInfo robot: rc.senseNearbyRobots(actionRadius, rc.getTeam())) {
            if(rc.canGetFlag(robot.getID())) {
                int robotFlag = rc.getFlag(robot.getID());
                if(Comms.getIC(robotFlag) == Comms.InformationCategory.ROBOT_TYPE && Comms.getSubRobotType(robotFlag) == Comms.SubRobotType.POL_PROTECTOR) {
                    ProtectorNearby = true;
                    int currDistToProtector = robot.getLocation().distanceSquaredTo(rc.getLocation());
                    if (currDistToProtector < closestProtectorDist) {
                        closestProtectorDist = currDistToProtector;
                        closestProtectorLoc = robot.getLocation();
                    }
                }
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
                if (!seenECs.contains(robot.getLocation())) {
                    seenECs.add(robot.getLocation());
                }
            } else {
                MapLocation tempLoc = robot.getLocation();
                int tempDist = currLoc.distanceSquaredTo(tempLoc);
                if(rc.canGetFlag(robot.getID())) {
                    int flag = rc.getFlag(robot.getID());
                    // Only slanderers and EC's broadcast AVG_ENEMY_DIR so this is valid to check for slanderers
                    if(Comms.isSubRobotType(flag, Comms.SubRobotType.SLANDERER) || Comms.getIC(flag) == Comms.InformationCategory.AVG_ENEMY_DIR) {
                        slandererOrECNearby = true;
                        slandererNearby = true;
                        if (tempDist < nearestSlandyDist && tempLoc.distanceSquaredTo(currMinEC) > currLoc.distanceSquaredTo(currMinEC)) {
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
        if (seenECs.size != 0) {
            Debug.println(Debug.info, "seensECs.locs: " + seenECs.locs + "; currLoc: " + currLoc);
            currMinEC = seenECmin(currLoc);
        } else {
            currMinEC = home;
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
            closestMuckrakerSensable.getLocation().isWithinDistanceSquared(currMinEC, (5 * sensorRadius)) &&
            numFollowingClosestMuckraker < Util.maxFollowingSingleUnit) {
            Debug.println(Debug.info, "I am pushing a muckraker away. ID: " + closestMuckrakerSensable.getID());
            setFlag(Comms.getFlag(Comms.InformationCategory.FOLLOWING, closestMuckrakerSensable.getID()));
            resetFlagOnNewTurn = false;
            Debug.println(Debug.info, "I am pushing a muckraker away");
            MapLocation closestMuckrakerSensableLoc = closestMuckrakerSensable.getLocation();
            Direction muckrakerPathtoBase = closestMuckrakerSensableLoc.directionTo(currMinEC);
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
            main_direction = Util.rotateInSpinDirection(spinDirection, currLoc.directionTo(currMinEC).opposite());
            Debug.setIndicatorLine(Debug.pathfinding, currLoc, lastSeenSlanderer, 200, 0, 255);
        }
        //Tries to lattice
        else if(ProtectorNearby) {
            main_direction = currLoc.directionTo(closestProtectorLoc).opposite();
            Debug.println(Debug.info, "Latticing away from other protectors");
        }
        //If cannot lattice, go towards nearest slanderer
        else if(lastSeenSlanderer != null) {
            Debug.setIndicatorDot(Debug.pathfinding, lastSeenSlanderer, 200, 0, 255);
            Debug.setIndicatorLine(Debug.pathfinding, currLoc, lastSeenSlanderer, 200, 0, 255);
            main_direction = currLoc.directionTo(lastSeenSlanderer);
            Debug.println(Debug.info, "going towards slanderers");
        }
        // else rotate towards ec
        else {
            Debug.println(Debug.info, "I see no slanderers, and cannot lattice. Rotating towards ec");
            main_direction = Util.rotateOppositeSpinDirection(spinDirection, currLoc.directionTo(currMinEC));
        }

        MapLocation target = currLoc.add(main_direction);
        Debug.setIndicatorLine(Debug.pathfinding, currLoc, target, 100, 100, 255);

        MapLocation lookAhead = currLoc.translate(main_direction.getDeltaX() * 2, main_direction.getDeltaY() * 2);
        if(!rc.onTheMap(lookAhead)) {
            Debug.println(Debug.info, "Close to a wall: Switching direction");
            spinDirection = Util.switchSpinDirection(spinDirection);
        }

        //Rotates around slanderers and then ec
        int tryMove = 0;
        while (!tryMoveDest(main_direction) && rc.isReady() && tryMove <= 1){
            Debug.println(Debug.info, "Try move failed: I am switching rotation direction");
            spinDirection = Util.switchSpinDirection(spinDirection);
            if (nearestSlandy != null) {
                main_direction = Util.rightOrLeftTurn(spinDirection, nearestSlandy.directionTo(currLoc));
            } else {
                Debug.println(Debug.info, "Couldn't find slandies ): I am rotating around an ec");
                main_direction = Util.rightOrLeftTurn(spinDirection, currMinEC.directionTo(currLoc));
            }
            tryMove +=1;
        }

        broadcastECLocation();
        return;
    }
}