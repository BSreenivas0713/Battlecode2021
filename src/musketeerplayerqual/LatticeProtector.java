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
    static Direction wallDirection = null;

    
    public LatticeProtector(RobotController r) {
        super(r);
        subRobotType = Comms.SubRobotType.POL_PROTECTOR;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
        lastSeenSlanderer = null;
        turnLastSeenSlanderer = 0;
    }

    public LatticeProtector(RobotController r, Direction nearestWall) {
        this(r);
        wallDirection = nearestWall;
    }
    
    public LatticeProtector(RobotController r, MapLocation h, int hID) {
        this(r);
        home = h;
        homeID = hID;
        friendlyECs.add(home, homeID);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Debug.println(Debug.info, "I am a lattice protector politician; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        if (rc.getRoundNum() >= 1495 && rc.canEmpower(actionRadius)) {
            rc.empower(actionRadius);
        }

        MapLocation currLoc = rc.getLocation();

        /* Creating all the variables that we need to do the step by step decision making for later*/

        int distanceToEC = rc.getLocation().distanceSquaredTo(home);
        
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
        int maxMuckrakerAttackableSize = 0;
        RobotInfo closestEnemy = null;
        Comms.EnemyType closestEnemyType = null;
        double minDistSquared = Integer.MAX_VALUE;

        for(int i = enemySensable.length - 1; i >= 0; i--) {
            robot = enemySensable[i];
            MapLocation tempLoc = robot.getLocation();
            int currDistance = tempLoc.distanceSquaredTo(home);
            if (robot.getType() == RobotType.MUCKRAKER) {
                if(currDistance < minMuckrakerDistance) {
                    closestMuckrakerSensable = robot;
                    minMuckrakerDistance = currDistance;
                }
                if(robot.getConviction() > maxMuckrakerAttackableSize) {
                    maxMuckrakerAttackableSize = robot.getConviction();
                }
            }

            int temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp < minDistSquared) {
                minDistSquared = temp;
                closestEnemy = robot;
                if(robot.getType() == RobotType.MUCKRAKER) {
                    closestEnemyType = Comms.EnemyType.MUC;
                } else {
                    closestEnemyType = Comms.EnemyType.UNKNOWN;
                }
            }

            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
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
        boolean protectorNearby = false;
        MapLocation enemyLoc = null;
        boolean turnIntoRusher = false;
        int maxInfSeen = -1;

        MapLocation rusherLoc = null;

        int totalProtectorX = 0;
        int totalProtectorY = 0;
        int numProtectors = 0;

        for(int i = friendlySensable.length - 1; i >= 0; i--) {
            robot = friendlySensable[i];
            if(rc.canGetFlag(robot.getID())) {
                int robotFlag = rc.getFlag(robot.getID());
                Comms.InformationCategory IC = Comms.getIC(robotFlag);
                if(Comms.isSubRobotType(robotFlag, Comms.SubRobotType.POL_PROTECTOR) ||
                    (robot.getType() == RobotType.POLITICIAN && IC == Comms.InformationCategory.FOLLOWING)) {
                    protectorNearby = true;
                    int currDistToProtector = robot.getLocation().distanceSquaredTo(rc.getLocation());
                    if (currDistToProtector < closestProtectorDist) {
                        closestProtectorDist = currDistToProtector;
                        closestProtectorLoc = robot.getLocation();
                    }
                    numProtectors++;
                    // if(rc.getLocation().distanceSquaredTo(robot.getLocation()) < 20) {

                    //     Debug.setIndicatorDot(Debug.info, robot.getLocation(), 100, 100, 100);

                    totalProtectorX += robot.getLocation().x;
                    totalProtectorY += robot.getLocation().y;
                    numProtectors++;
                } else if (Comms.isSubRobotType(robotFlag, Comms.SubRobotType.POL_RUSH)) {
                    rusherLoc = robot.getLocation();
                }
            }
            
            if ((robot.getType() == RobotType.ENLIGHTENMENT_CENTER)) {
                slandererOrECNearby = true;
                ECNearby = true;
            } else {
                MapLocation tempLoc = robot.getLocation();
                int tempDist = currLoc.distanceSquaredTo(tempLoc);
                if(rc.canGetFlag(robot.getID())) {
                    int flag = rc.getFlag(robot.getID());
                    if(Comms.isSubRobotType(flag, Comms.SubRobotType.SLANDERER)) {
                        slandererOrECNearby = true;
                        slandererNearby = true;
                        if (tempDist < nearestSlandyDist && tempLoc.distanceSquaredTo(home) > currLoc.distanceSquaredTo(home)) {
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
                        !slandererNearby && currLoc.distanceSquaredTo(home) >= 4 * RobotType.ENLIGHTENMENT_CENTER.sensorRadiusSquared) {
                        Debug.println(Debug.info, "Joining the rush");
                        turnIntoRusher = true;
                    } else {
                        Debug.println(Debug.info, "I was not included in this rush");
                    }
                    break;
                case TEST:
                    Debug.println(Debug.info, "EC is in between building ");
                    break;
            }
        } else {
            Debug.println(Debug.info, "Can't get home flag: " + homeID);
        }
        
        /* Step by Step decision making*/
        //empower if near 2 enemies or enemy is in sensing radius of our base
        if ((numMuckAttackable > 1 || 
            (numMuckAttackable > 0 && (slandererNearby || minMuckrakerDistance <= RobotType.ENLIGHTENMENT_CENTER.sensorRadiusSquared || maxMuckrakerAttackableSize > 1)))
            && rc.canEmpower(maxEnemyAttackableDistSquared)) {
            Debug.println(Debug.info, "Enemy too close to base. I will empower");
            Debug.setIndicatorLine(Debug.info, rc.getLocation(), farthestEnemyAttackable, 255, 150, 50);
            rc.empower(maxEnemyAttackableDistSquared);
            return;
        }

        if (rusherLoc != null && rc.isReady()) {
            Debug.println(Debug.info, "Running away from a rusher.");
            main_direction = currLoc.directionTo(rusherLoc).opposite();
            tryMoveDest(main_direction);
        }

        //Turns into a rusher if the enemy tower has less than 10 times the amount that the politician will use when empowering
        // if(turnIntoRusher) {
        //     Debug.println(Debug.info, "Changing into a Lattice Rusher");
        //     changeTo = new LatticeRusher(rc, enemyLoc, home, homeID);
        //     return;
        // }

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

        main_direction = Direction.CENTER;
        //moves out of sensor radius of Enlightenment Center
        if(ECNearby) { //this is the old case, changed it to what is below for experimentation
            // if (home != null && currLoc.isWithinDistanceSquared(home, RobotType.ENLIGHTENMENT_CENTER.sensorRadiusSquared * 4)) {
            // Debug.println(Debug.info, "I am moving away from the base");
            // main_direction = Util.rotateInSpinDirection(spinDirection, currLoc.directionTo(home).opposite());

            Direction dirOfMovement = currLoc.directionTo(home).opposite();
            if (wallDirection == null || (dirOfMovement != wallDirection && dirOfMovement != wallDirection.rotateLeft() &&
                dirOfMovement != wallDirection.rotateRight())) { //if wallDir == null or you're not moving towards wall
                Debug.println(Debug.info, "I am moving away from the base");
                main_direction = Util.rotateInSpinDirection(spinDirection, dirOfMovement);
            }
            else {
                Debug.println(Debug.info, "I am moving away from wall direction: " + wallDirection);
                main_direction = Util.rotateInSpinDirection(spinDirection, wallDirection.opposite()); //move away from wall near EC
            }
        }
        //Tries to lattice
        else if(protectorNearby) {
            MapLocation averageProtector = new MapLocation(totalProtectorX / numProtectors, totalProtectorY / numProtectors);
            // main_direction = currLoc.directionTo(averageProtector).opposite();
            main_direction = currLoc.directionTo(closestProtectorLoc).opposite();
            Debug.println(Debug.info, "Latticing away from other protectors");
        }
        //If cannot lattice, go towards nearest slanderer
        // else if(lastSeenSlanderer != null) {
        //     Debug.setIndicatorDot(Debug.pathfinding, lastSeenSlanderer, 200, 0, 255);
        //     Debug.setIndicatorLine(Debug.pathfinding, currLoc, lastSeenSlanderer, 200, 0, 255);
        //     main_direction = currLoc.directionTo(lastSeenSlanderer);
        //     Debug.println(Debug.info, "going towards slanderers");
        // }
        // else rotate towards ec
        else {
            Debug.println(Debug.info, "I see no slanderers, and cannot lattice. Rotating towards ec");
            main_direction = Util.rotateOppositeSpinDirection(spinDirection, currLoc.directionTo(home));
            // Debug.println(Debug.info, "starting to explore");
            // main_direction = Nav.explore();
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
                main_direction = Util.rightOrLeftTurn(spinDirection, home.directionTo(currLoc));
            }
            tryMove +=1;
        }

        if(!setFollowingFlag) {
            // This means that the first half of an EC-ID/EC-ID broadcast finished.
            if(needToBroadcastHomeEC && rc.getFlag(rc.getID()) == defaultFlag) { broadcastHomeEC(); }
            else if(broadcastECLocation());
            else if(closestEnemy != null && broadcastEnemyLocalOrGlobal(closestEnemy.getLocation(), closestEnemyType));
        }
    }
}