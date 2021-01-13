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
public class ProtectorPolitician extends Robot {
    static RotationDirection spinDirection = Util.RotationDirection.COUNTERCLOCKWISE;
    static Direction main_direction;
    static final int slandererFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.SLANDERER);
    
    public ProtectorPolitician(RobotController r) {
        super(r);
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.POL_PROTECTOR);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Debug.println(Debug.info, "I am a protector politician; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        RobotInfo[] neutrals = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        MapLocation currLoc = rc.getLocation();

        main_direction = Util.rightOrLeftTurn(spinDirection, home.directionTo(currLoc)); //Direction if we only want to rotate around the base
        /* Creating all the variables that we need to do the step by step decision making for later*/

        boolean slandererOrECNearby = false;
        boolean ECNearby = false;
        for (RobotInfo robot : friendlySensable) {
            if ((robot.getType() == RobotType.ENLIGHTENMENT_CENTER)){
                slandererOrECNearby = true;
                ECNearby = true;
            } 
            
            if((rc.canGetFlag(robot.getID()) && 
                rc.getFlag(robot.getID()) == slandererFlag)) {
                slandererOrECNearby = true;
            }
        }

        int distanceToEC = rc.getLocation().distanceSquaredTo(home);
        
        int maxEnemyAttackableDistSquared = Integer.MIN_VALUE;
        MapLocation farthestEnemyAttackable = null;
        for (RobotInfo robot : enemyAttackable) {
            int temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp > maxEnemyAttackableDistSquared) {
                maxEnemyAttackableDistSquared = temp;
                farthestEnemyAttackable = robot.getLocation();
            }
        }

        RobotInfo closestMuckrakerSensable = null;
        int minDistance = Integer.MAX_VALUE;

        for (RobotInfo robot : enemySensable) {
            int currDistance = robot.getLocation().distanceSquaredTo(home);
            if (robot.getType() == RobotType.MUCKRAKER && currDistance < minDistance) {
                closestMuckrakerSensable = robot;
                minDistance = currDistance;
            }
        }
        
        /* Step by Step decision making*/

        //empower if near 2 enemies or near an enemy while a slanderer or EC is nearby
        if ((enemyAttackable.length > 1 || (enemyAttackable.length >0 && slandererOrECNearby)) && rc.canEmpower(maxEnemyAttackableDistSquared)) {
            Debug.println(Debug.info, "Empowered with radius: " + maxEnemyAttackableDistSquared);
            Debug.setIndicatorLine(rc.getLocation(), farthestEnemyAttackable, 255, 150, 50);
            rc.empower(maxEnemyAttackableDistSquared);
            return;
        }
        //tries to block a muckraker in its path(if the muckraker is within 2 sensing radiuses of the EC)
        if (closestMuckrakerSensable != null && closestMuckrakerSensable.getLocation().distanceSquaredTo(home) <= 2 * sensorRadius ) {
            Debug.println(Debug.info, "I am pushing a muckraker away");
            MapLocation closestMuckrakerSensableLoc = closestMuckrakerSensable.getLocation();
            Direction muckrakerPathtoBase = closestMuckrakerSensableLoc.directionTo(home);
            MapLocation squareToBlock = closestMuckrakerSensableLoc.add(muckrakerPathtoBase);
            Direction toMove = rc.getLocation().directionTo(squareToBlock);
            tryMoveDest(toMove);
            return;
        }
        //moves out of sensor radius of Enlightenment Center
        if (ECNearby) {
            Debug.println(Debug.info, "I am moving away from the base");
            Direction toMove = Util.rotateInSpinDirection(spinDirection, rc.getLocation().directionTo(home).opposite());
            tryMoveDest(toMove);
            return;
        }
        //if too far away from sensor radius of Enlightenment center, move towards Enlgihtenment Center
        if (distanceToEC > (int) (sensorRadius * 2)) {
            Debug.println(Debug.info, "I am moving toward the base");
            Direction toMove = rc.getLocation().directionTo(home);
            tryMoveDest(toMove);
            return;
        }
        //Rotates around the base
        Debug.println(Debug.info, "I am rotating around the base");
        while (!tryMoveDest(main_direction) && rc.isReady()){
            Debug.println(Debug.info, "I am switching rotation direction");
            spinDirection = Util.switchSpinDirection(spinDirection);
        }

        broadcastECLocation();
        return;
    }
}