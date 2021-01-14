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
    
    public ProtectorPoliticianNew(RobotController r) {
        super(r);
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.POL_PROTECTOR);
    }
    
    public ProtectorPoliticianNew(RobotController r, MapLocation h) {
        super(r);
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.POL_PROTECTOR);
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
        // MapLocation nearestSlandy = null;
        // int nearestSlandyDist = Integer.MAX_VALUE;
        int slandyX = 0;
        int slandyY = 0;
        int numSlandies = 0;
        boolean ECNearby = false;
        int numFollowingClosestMuckraker = 0;
        for (RobotInfo robot : friendlySensable) {
            if ((robot.getType() == RobotType.ENLIGHTENMENT_CENTER)){
                slandererOrECNearby = true;
                ECNearby = true;
            } else {
                MapLocation tempLoc = robot.getLocation();
                // int tempDist = currLoc.distanceSquaredTo(tempLoc);
                /* if (robot.getType() == RobotType.SLANDERER && tempDist < nearestSlandyDist) {
                    nearestSlandy = tempLoc;
                    nearestSlandyDist = tempDist;
                }*/
                if (robot.getType() == RobotType.SLANDERER) {
                    slandyX += tempLoc.x;
                    slandyY += tempLoc.y;
                    numSlandies++;
                }
                if(rc.canGetFlag(robot.getID())) {
                    int flag = rc.getFlag(robot.getID());
                    // Only slanderers and EC's broadcast AVG_ENEMY_DIR so this is valid to check for slanderers
                    if(flag == slandererFlag || Comms.getIC(flag) == Comms.InformationCategory.AVG_ENEMY_DIR) {
                        slandererOrECNearby = true;
                        slandererNearby = true;
                    }
    
                    if(closestMuckrakerSensable != null) {
                        if(flag == Comms.getFlag(Comms.InformationCategory.FOLLOWING, closestMuckrakerSensable.getID())) {
                            numFollowingClosestMuckraker++;
                        }
                    }
                }
            }
        }

        MapLocation avgSlandy = null;
        if (numSlandies != 0) {
            avgSlandy = new MapLocation(slandyX / numSlandies, slandyY / numSlandies);
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
            Direction toMove = Util.rotateInSpinDirection(spinDirection, rc.getLocation().directionTo(home).opposite());
            tryMoveDest(toMove);
            return;
        }

        //if too far close to average slandy, move away it
        if (avgSlandy != null && currLoc.distanceSquaredTo(avgSlandy) < 8) {
            Debug.println(Debug.info, "I am moving towards the slandies");
            Direction toMove = avgSlandy.directionTo(currLoc).rotateLeft();
            tryMoveDest(toMove);
            return;
        }

        if (avgSlandy != null) {
            main_direction = Util.rightOrLeftTurn(spinDirection, avgSlandy.directionTo(currLoc));
        }

        //Rotates around slanderers and then home
        int tryMove = 0;
        Debug.println(Debug.info, "I am rotating around slandies");
        while (!tryMoveDest(main_direction) && rc.isReady() && tryMove <= 1){
            Debug.println(Debug.info, "I am switching rotation direction");
            spinDirection = Util.switchSpinDirection(spinDirection);
            if (avgSlandy != null) {
                main_direction = Util.rightOrLeftTurn(spinDirection, avgSlandy.directionTo(currLoc));
            } else {
                Debug.println(Debug.info, "Couldn't find slandies ): I am rotating around home");
                main_direction = Util.rightOrLeftTurn(spinDirection, home.directionTo(currLoc));
            }
            tryMove +=1;

        }

        broadcastECLocation();
        return;
    }
}