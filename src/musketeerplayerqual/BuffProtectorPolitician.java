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
public class BuffProtectorPolitician extends Robot {
    static RotationDirection spinDirection = Util.RotationDirection.COUNTERCLOCKWISE;
    static Direction main_direction;
    static final int slandererFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.SLANDERER);
    
    public BuffProtectorPolitician(RobotController r) {
        super(r);
        subRobotType = Comms.SubRobotType.POL_BUFF;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
    }
    
    public BuffProtectorPolitician(RobotController r, MapLocation h) {
        this(r);
        home = h;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Debug.println(Debug.info, "I am a buff protector politician; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        Debug.println(Debug.info, "current home: " + home);

        RobotInfo[] neutrals = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        MapLocation currLoc = rc.getLocation();

        if(!rc.canGetFlag(homeID)) {
            changeTo = new RushPolitician(rc, home);
        }

        main_direction = Util.rightOrLeftTurn(spinDirection, home.directionTo(currLoc)); //Direction if we only want to rotate around the base
        /* Creating all the variables that we need to do the step by step decision making for later*/

        int distanceToEC = rc.getLocation().distanceSquaredTo(home);
        
        RobotInfo robot;
        int maxEnemyAttackableDistSquared = Integer.MIN_VALUE;
        MapLocation farthestEnemyAttackable = null;
        int maxAttackableSize = 0;
        RobotInfo closestBuffMuck = null;
        int closestBuffMuckDist = Integer.MAX_VALUE;
        int farthestAttackableEnemyDist = Integer.MIN_VALUE;
        int temp;
        int numMuckAttackable = 0;
        int maxMuckAttackableDistSquared = Integer.MIN_VALUE;
        for(int i = enemyAttackable.length - 1; i >= 0; i--) {
            robot = enemyAttackable[i];
            temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (robot.getConviction() > maxAttackableSize) {
                maxAttackableSize = robot.getConviction();
                maxEnemyAttackableDistSquared = temp;
            }

            if(temp > farthestAttackableEnemyDist) {
                farthestAttackableEnemyDist = temp;
            }

            if(robot.getType() == RobotType.MUCKRAKER) {
                numMuckAttackable++;
                if(temp > maxMuckAttackableDistSquared) {
                    maxMuckAttackableDistSquared = temp;
                }
            }
        }

        RobotInfo closestMuckrakerSensable = null;
        int minMuckrakerDistance = Integer.MAX_VALUE;
        RobotInfo closestEnemy = null;
        Comms.EnemyType closestEnemyType = null;
        double minDistSquared = Integer.MAX_VALUE;

        for(int i = enemySensable.length - 1; i >= 0; i--) {
            robot = enemySensable[i];
            int currDistance = robot.getLocation().distanceSquaredTo(home);
            if (robot.getType() == RobotType.MUCKRAKER && currDistance < minMuckrakerDistance) {
                closestMuckrakerSensable = robot;
                minMuckrakerDistance = currDistance;
            }

            if(robot.getType() == RobotType.MUCKRAKER &&
                closestBuffMuckDist > currLoc.distanceSquaredTo(robot.getLocation())) {
                closestBuffMuckDist = currLoc.distanceSquaredTo(robot.getLocation());
                closestBuffMuck = robot;
            }

            temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp < minDistSquared) {
                minDistSquared = temp;
                closestEnemy = robot;
                if(robot.getType() == RobotType.MUCKRAKER) {
                    closestEnemyType = Comms.EnemyType.MUC;
                } else if(Util.isSlandererInfluence(robot.getInfluence())) {
                    closestEnemyType = Comms.EnemyType.SLA;   
                } else {
                    closestEnemyType = Comms.EnemyType.UNKNOWN;
                }
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

        int mostKilled = Integer.MIN_VALUE;
        int bestRadius = actionRadius;
        int numKilled;
        int damagePerBot;
        for(int i = actionRadius; i >= 0; i--) {
            RobotInfo[] botsInAttack = rc.senseNearbyRobots(i);
            damagePerBot = (int) ((rc.getConviction() - 10) * rc.getEmpowerFactor(rc.getTeam(), 0) / botsInAttack.length);
            numKilled = 0;
            for(int j = botsInAttack.length - 1; j >= 0; j--) {
                robot = botsInAttack[j];
                if(robot.getTeam() == enemy && damagePerBot > robot.getConviction()) {
                    numKilled++;
                }
            }
            if(numKilled > mostKilled) {
                mostKilled = numKilled;
                bestRadius = i;
            }
        }
        
        /* Step by Step decision making*/
        if ((closestBuffMuck != null && closestBuffMuck.getConviction() >= rc.getConviction() / 3)
            && rc.canEmpower(closestBuffMuckDist)) {
            Debug.println(Debug.info, "Big enemy nearby: Empowering with radius: " + closestBuffMuckDist);
            Debug.setIndicatorLine(Debug.info, rc.getLocation(), closestBuffMuck.getLocation(), 255, 150, 50);
            rc.empower(closestBuffMuckDist);
            return;
        }

        if(mostKilled >= 2 && rc.canEmpower(bestRadius)) {
            Debug.println("Can kill at least 2 enemies. Empowering with radius: " + bestRadius);
            rc.empower(bestRadius);
            return;
        }

        if(numMuckAttackable >= 2 && slandererNearby && rc.canEmpower(maxMuckAttackableDistSquared)) {
            Debug.println("Slanderer nearby with mucks. Empowering with radius: " + maxMuckAttackableDistSquared);
            rc.empower(maxMuckAttackableDistSquared);
            return;
        }
        
        if(enemyAttackable.length >= 3 && rc.canEmpower(farthestAttackableEnemyDist)) {
            Debug.println(Debug.info, "Lots of enemies nearby: Empowering with radius: " + farthestAttackableEnemyDist);
            rc.empower(farthestAttackableEnemyDist);
            return;
        }

        if(closestBuffMuck != null) {
            Debug.println("Moving towards a buff muck");
            Direction toMove = currLoc.directionTo(closestBuffMuck.getLocation());
            Direction[] orderedDirs = Nav.greedyDirection(toMove, rc);
            for(Direction dir : orderedDirs) {
                tryMove(dir);
            }
        }
        //moves out of sensor radius of Enlightenment Center
        else if (ECNearby) {
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
        boolean switchedDirection = false;
        Debug.println(Debug.info, "I am rotating around the base");
        Direction[] orderedDirs = {main_direction, main_direction.rotateLeft(), main_direction.rotateRight()};
        for(Direction dir : orderedDirs) {
            MapLocation target = rc.getLocation().add(dir);
            if(!rc.onTheMap(target)) {
                switchedDirection = true;
                break;
            } else {
                tryMove(dir);
            }
        }

        if(switchedDirection && rc.isReady()) {
            Debug.println(Debug.info, "I am switching rotation direction");
            spinDirection = Util.switchSpinDirection(spinDirection);
            main_direction = Util.rightOrLeftTurn(spinDirection, home.directionTo(currLoc));

            orderedDirs = new Direction[]{main_direction, main_direction.rotateRight(), main_direction.rotateLeft()};
            for(Direction dir : orderedDirs) {
                tryMove(dir);
            }
        }

        // This means that the first half of an EC-ID/EC-ID broadcast finished.
        if(closestEnemy != null && broadcastEnemyLocalOrGlobal(closestEnemy.getLocation(), closestEnemyType));
    }
}