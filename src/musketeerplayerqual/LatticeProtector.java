package musketeerplayerqual;
import battlecode.common.*;

import musketeerplayerqual.Util.*;
import musketeerplayerqual.Debug.*;
import musketeerplayerqual.fast.FastIterableLocSet;
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

        Debug.println(Debug.info, "I am a lattice protector politician; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        MapLocation currLoc = rc.getLocation();

        main_direction = Util.rightOrLeftTurn(spinDirection, currMinEC.directionTo(currLoc)); //Direction if we only want to rotate around the base
        /* Creating all the variables that we need to do the step by step decision making for later*/

        int distanceToEC = rc.getLocation().distanceSquaredTo(currMinEC);
        
        RobotInfo robot;
        int maxEnemyAttackableDistSquared = Integer.MIN_VALUE;
        MapLocation farthestEnemyAttackable = null;
        int maxPoliticianSize = 0;
        int numMuckAttackable = 0;
        
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
            if(robot.getType() == RobotType.MUCKRAKER) {
                numMuckAttackable++;
            }
        }

        RobotInfo closestMuckrakerSensable = null;
        int minMuckrakerDistance = Integer.MAX_VALUE;
        RobotInfo closestEnemy = null;
        double minDistSquared = Integer.MAX_VALUE;

        for(int i = enemySensable.length - 1; i >= 0; i--) {
            robot = enemySensable[i];
            MapLocation tempLoc = robot.getLocation();
            int currDistance = tempLoc.distanceSquaredTo(currMinEC);
            if (robot.getType() == RobotType.MUCKRAKER && currDistance < minMuckrakerDistance) {
                closestMuckrakerSensable = robot;
                minMuckrakerDistance = currDistance;
            }

            int temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp < minDistSquared) {
                minDistSquared = temp;
                closestEnemy = robot;
            }

            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                if (seenECs.contains(tempLoc)) {
                    seenECs.remove(tempLoc);
                }
                int distToEmpower = currLoc.distanceSquaredTo(robot.getLocation());
                if (distToEmpower <= 2 && rc.canEmpower(distToEmpower)) {
                    rc.empower(distToEmpower);
                }
            }
        }

        boolean slandererOrECNearby = false;
        boolean slandererNearby = false;
        MapLocation nearestSlandy = null;
        int nearestSlandyDist = Integer.MAX_VALUE;
        boolean ECNearby = false;
        int numFollowingClosestMuckraker = 0;

        int closestProtectorDist = Integer.MAX_VALUE;
        MapLocation closestProtectorLoc = null;
        boolean ProtectorNearby = false;
        MapLocation enemyLoc = null;
        boolean turnIntoRusher = false;
        int maxInfSeen = -1;

        int totalProtectorX = 0;
        int totalProtectorY = 0;
        int numProtectors = 0;

        for(int i = friendlySensable.length - 1; i >= 0; i--) {
            robot = friendlySensable[i];
            if(rc.canGetFlag(robot.getID())) {
                int robotFlag = rc.getFlag(robot.getID());
                if(Comms.isSubRobotType(robotFlag, Comms.SubRobotType.POL_PROTECTOR)) {
                    ProtectorNearby = true;
                    int currDistToProtector = robot.getLocation().distanceSquaredTo(rc.getLocation());
                    if (currDistToProtector < closestProtectorDist) {
                        closestProtectorDist = currDistToProtector;
                        closestProtectorLoc = robot.getLocation();
                    }

                    Debug.setIndicatorDot(Debug.info, robot.getLocation(), 100, 100, 100);

                    totalProtectorX += robot.getLocation().x;
                    totalProtectorY += robot.getLocation().y;
                    numProtectors++;
                }
            }
            
            if ((robot.getType() == RobotType.ENLIGHTENMENT_CENTER)) {
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
                    if(Comms.isSubRobotType(flag, Comms.SubRobotType.SLANDERER)) {
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
        
        if(rc.canGetFlag(homeID)) {
            Debug.println(Debug.info, "Checking home flag");
            int flag = rc.getFlag(homeID);
            Comms.InformationCategory IC = Comms.getIC(flag);
            switch(IC) {
                case ENEMY_EC:
                    int[] dxdy = Comms.getDxDy(flag);
                    enemyLoc = new MapLocation(dxdy[0] + home.x - Util.dOffset, dxdy[1] + home.y - Util.dOffset);
                    Debug.setIndicatorDot(Debug.info, enemyLoc, 255, 0, 0);

                    Comms.GroupRushType GRtype = Comms.getRushType(flag);
                    int GRmod = Comms.getRushMod(flag);
                    Debug.println(Debug.info, "EC is sending a rush: Read ENEMY_EC flag. Type: " + GRtype + ", mod: " + GRmod);

                    if(GRtype == Comms.GroupRushType.MUC_POL && GRmod == rc.getID() % 4 && 
                        !slandererNearby && currLoc.distanceSquaredTo(home) >= 2 * RobotType.ENLIGHTENMENT_CENTER.sensorRadiusSquared) {
                        Debug.println(Debug.info, "Joining the rush");
                        turnIntoRusher = true;
                    } else {
                        Debug.println(Debug.info, "I was not included in this rush");
                    }
            }
        } else {
            Debug.println(Debug.info, "Can't get home flag: " + homeID);
        }
        
        /* Step by Step decision making*/
        //empower if near 2 enemies or enemy is in sensing radius of our base
        if (((maxPoliticianSize > 0 && maxPoliticianSize <= 1.5 * rc.getInfluence()) || 
            (numMuckAttackable > 1 || 
            (numMuckAttackable > 0 && (slandererNearby || minMuckrakerDistance <= RobotType.ENLIGHTENMENT_CENTER.sensorRadiusSquared))))
            && rc.canEmpower(maxEnemyAttackableDistSquared)) {
            Debug.println(Debug.info, "Enemy too close to base. I will empower");
            Debug.setIndicatorLine(Debug.info, rc.getLocation(), farthestEnemyAttackable, 255, 150, 50);
            rc.empower(maxEnemyAttackableDistSquared);
            return;
        }
        //Turns into a rusher if the enemy tower has less than 10 times the amount that the politician will use when empowering
        if(turnIntoRusher) {
            Debug.println(Debug.info, "Changing into a Lattice Rusher");
            changeTo = new LatticeRusher(rc, enemyLoc, home);
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

        main_direction = Direction.CENTER;
        //moves out of sensor radius of Enlightenment Center
        if (ECNearby) {
            Debug.println(Debug.info, "I am moving away from the base");
            main_direction = Util.rotateInSpinDirection(spinDirection, currLoc.directionTo(currMinEC).opposite());
        }
        //Tries to lattice
        else if(ProtectorNearby) {
            MapLocation averageProtector = new MapLocation(totalProtectorX / numProtectors, totalProtectorY / numProtectors);
            // main_direction = currLoc.directionTo(averageProtector).opposite();
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
            // Debug.println(Debug.info, "I see nobody, chilling out");
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
        while (main_direction != Direction.CENTER && !tryMoveDest(main_direction) && rc.isReady() && tryMove <= 1){
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

        if(broadcastECLocation());
        else if(closestEnemy != null && broadcastEnemyLocalOrGlobal(closestEnemy.getLocation()));
    }
}