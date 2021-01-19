package musketeerplayerqual;
import battlecode.common.*;

import musketeerplayerqual.Util.*;
import musketeerplayerqual.Debug.*;
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
        subRobotType = Comms.SubRobotType.POL_PROTECTOR;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
    }
    
    public ProtectorPolitician(RobotController r, MapLocation h, int hID) {
        this(r);
        home = h;
        homeID = hID;
        friendlyECs.add(home, homeID);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Debug.println(Debug.info, "I am a protector politician; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        Debug.println(Debug.info, "current home: " + home);

        RobotInfo[] neutrals = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        MapLocation currLoc = rc.getLocation();

        main_direction = Util.rightOrLeftTurn(spinDirection, home.directionTo(currLoc)); //Direction if we only want to rotate around the base
        /* Creating all the variables that we need to do the step by step decision making for later*/

        int distanceToEC = rc.getLocation().distanceSquaredTo(home);
        
        RobotInfo robot;
        int maxEnemyAttackableDistSquared = Integer.MIN_VALUE;
        MapLocation farthestEnemyAttackable = null;
        int maxPoliticianSize = 0;
        for(int i = enemyAttackable.length - 1; i >= 0; i--) {
            robot = enemyAttackable[i];
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
        RobotInfo closestEnemy = null;
        double minDistSquared = Integer.MAX_VALUE;

        for(int i = enemySensable.length - 1; i >= 0; i--) {
            robot = enemySensable[i];
            int currDistance = robot.getLocation().distanceSquaredTo(home);
            if (robot.getType() == RobotType.MUCKRAKER && currDistance < minMuckrakerDistance) {
                closestMuckrakerSensable = robot;
                minMuckrakerDistance = currDistance;
            }

            int temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp < minDistSquared) {
                minDistSquared = temp;
                closestEnemy = robot;
            }
        }
        
        boolean slandererOrECNearby = false;
        boolean slandererNearby = false;
        boolean ECNearby = false;
        int numFollowingClosestMuckraker = 0;
        for(int i = friendlySensable.length - 1; i >= 0; i--) {
            robot = friendlySensable[i];
            if ((robot.getType() == RobotType.ENLIGHTENMENT_CENTER)){
                slandererOrECNearby = true;
                ECNearby = true;
            } else {
                if(rc.canGetFlag(robot.getID())) {
                    int flag = rc.getFlag(robot.getID());
                    if(Comms.isSubRobotType(flag, Comms.SubRobotType.SLANDERER)) {
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

        boolean setFollowingFlag = false;

        //tries to block a muckraker in its path(if the muckraker is within 2 sensing radiuses of the EC)
        if (closestMuckrakerSensable != null && 
            closestMuckrakerSensable.getLocation().isWithinDistanceSquared(home, (5 * sensorRadius)) &&
            numFollowingClosestMuckraker < Util.maxFollowingSingleUnit) {
            Debug.println(Debug.info, "I am pushing a muckraker away. ID: " + closestMuckrakerSensable.getID());
            setFlag(Comms.getFlag(Comms.InformationCategory.FOLLOWING, closestMuckrakerSensable.getID()));
            setFollowingFlag = true;
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
        }

        //if too far away from sensor radius of Enlightenment center, move towards Enlgihtenment Center
        else if (distanceToEC > (int) (sensorRadius * 2)) {
            Debug.println(Debug.info, "I am moving toward the base");
            Direction toMove = rc.getLocation().directionTo(home);
            tryMoveDest(toMove);
        }

        //Rotates around the base
        int tryMove = 0;
        Debug.println(Debug.info, "I am rotating around the base");
        while (!tryMoveDest(main_direction) && rc.isReady() && tryMove <= 1){
            Debug.println(Debug.info, "I am switching rotation direction");
            spinDirection = Util.switchSpinDirection(spinDirection);
            main_direction = Util.rightOrLeftTurn(spinDirection, home.directionTo(currLoc));
            tryMove +=1;
        }

        if(!setFollowingFlag) {
            // This means that the first half of an EC-ID/EC-ID broadcast finished.
            if(needToBroadcastHomeEC && rc.getFlag(rc.getID()) == defaultFlag) { broadcastHomeEC(); }
            else if(broadcastECLocation());
            else if(closestEnemy != null && broadcastEnemyLocalOrGlobal(closestEnemy.getLocation()));
        }
    }
}