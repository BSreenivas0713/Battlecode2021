package musketeerplayerfinal;
import battlecode.common.*;

import musketeerplayerfinal.Util.*;
import musketeerplayerfinal.Debug.*;
import musketeerplayerfinal.fast.FastIterableLocSet;
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
    static boolean explorer = false;

    
    public LatticeProtector(RobotController r) {
        super(r);
        subRobotType = Comms.SubRobotType.POL_PROTECTOR;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
        lastSeenSlanderer = null;
        turnLastSeenSlanderer = 0;
        explorer = false;
    }

    public LatticeProtector(RobotController r, Direction nearestWall) {
        this(r);
        wallDirection = nearestWall;
        explorer = false;
    }
    
    public LatticeProtector(RobotController r, MapLocation h, int hID) {
        this(r);
        home = h;
        homeID = hID;
        friendlyECs.add(home, homeID);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        if (rc.getTeamVotes() < 751 && rc.getRoundNum() >= 1450) changeTo = new CleanupPolitician(rc, home, homeID);

        Debug.println(Debug.info, "I am a lattice protector politician; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        MapLocation currLoc = rc.getLocation();

        /* Creating all the variables that we need to do the step by step decision making for later*/

        int distanceToEC = rc.getLocation().distanceSquaredTo(home);
        
        RobotInfo robot;
        int maxEnemyAttackableDistSquared = Integer.MIN_VALUE;
        int maxPolAttackableDistSquared = Integer.MIN_VALUE;
        int maxMuckAttackableDistSquared = Integer.MIN_VALUE;
        int minMuckAttackableDistSquared = Integer.MAX_VALUE;
        MapLocation farthestEnemyAttackable = null;
        MapLocation farthestMuckAttackable = null;
        MapLocation closestMuckAttackable = null;
        int maxPoliticianSizeWithinReasonableThreshold = 0;
        int numMuckAttackable = 0;
        int robotConviction = 0;
        int numReasonablePols = 0;
        // int numRobotsInAttackable = rc.senseNearbyRobots(rc.getType().actionRadiusSquared).length;
        int numFriendlyAttackable = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam()).length;
        int polAttackThreshold = (int) (rc.getConviction() * rc.getEmpowerFactor(rc.getTeam(), 0) * 3 / 4);
        
        for(int i = enemyAttackable.length - 1; i >= 0; i--) {
            robot = enemyAttackable[i];
            robotConviction = robot.getConviction();
            int temp = currLoc.distanceSquaredTo(robot.getLocation());
            if (temp > maxEnemyAttackableDistSquared) {
                maxEnemyAttackableDistSquared = temp;
                farthestEnemyAttackable = robot.getLocation();
            }
            if (robot.getType() == RobotType.POLITICIAN && robotConviction > maxPoliticianSizeWithinReasonableThreshold &&
                robotConviction <= polAttackThreshold) {
                numReasonablePols++;
                maxPoliticianSizeWithinReasonableThreshold = robotConviction;
                if (temp > maxPolAttackableDistSquared) {
                    maxPolAttackableDistSquared = temp;
                }
            }
            if(robot.getType() == RobotType.MUCKRAKER) {
                numMuckAttackable++;
                if(temp > maxMuckAttackableDistSquared) {
                    maxMuckAttackableDistSquared = temp;
                    farthestMuckAttackable = robot.getLocation();
                } else if(temp < minMuckAttackableDistSquared) {
                    minMuckAttackableDistSquared = temp;
                    closestMuckAttackable = robot.getLocation();
                }
            }
        }

        RobotInfo closestMuckToHome = null;
        int closestMuckToHomeDist = Integer.MAX_VALUE;
        RobotInfo closestMuck = null;
        int closestMuckDist = Integer.MAX_VALUE;
        int maxMuckrakerAttackableSize = 0;
        RobotInfo closestEnemy = null;
        Comms.EnemyType closestEnemyType = null;
        double minDistSquared = Integer.MAX_VALUE;

        for(int i = enemySensable.length - 1; i >= 0; i--) {
            robot = enemySensable[i];
            MapLocation tempLoc = robot.getLocation();
            int currDistance = tempLoc.distanceSquaredTo(home);
            if (robot.getType() == RobotType.MUCKRAKER) {
                if(currDistance < closestMuckToHomeDist) {
                    closestMuckToHome = robot;
                    closestMuckToHomeDist = currDistance;
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
                } else if(Util.isSlandererInfluence(robot.getInfluence())) {
                    closestEnemyType = Comms.EnemyType.SLA;   
                } else {
                    closestEnemyType = Comms.EnemyType.UNKNOWN;
                }
            }

            if(robot.getType() == RobotType.MUCKRAKER && temp < closestMuckDist) {
                closestMuckDist = temp;
                closestMuck = robot;
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

        boolean amClosestPolToClosestMuck = true;
        int totalProtectorX = 0;
        int totalProtectorY = 0;
        int numProtectors = 0;
        MapLocation tempLoc;
        int tempDist;

        for(int i = friendlySensable.length - 1; i >= 0; i--) {
            robot = friendlySensable[i];
            tempLoc = robot.getLocation();
            tempDist = currLoc.distanceSquaredTo(tempLoc);
            if(rc.canGetFlag(robot.getID())) {
                int flag = rc.getFlag(robot.getID());
                Comms.InformationCategory IC = Comms.getIC(flag);
                if(Comms.isSubRobotType(flag, Comms.SubRobotType.POL_PROTECTOR) ||
                    (robot.getType() == RobotType.POLITICIAN && IC == Comms.InformationCategory.FOLLOWING)) {
                    protectorNearby = true;
                    int currDistToProtector = tempLoc.distanceSquaredTo(currLoc);
                    if (currDistToProtector < closestProtectorDist) {
                        closestProtectorDist = currDistToProtector;
                        closestProtectorLoc = tempLoc;
                    }
                    // if(rc.getLocation().distanceSquaredTo(robot.getLocation()) < 20) {

                    //     Debug.setIndicatorDot(Debug.info, robot.getLocation(), 100, 100, 100);

                    totalProtectorX += robot.getLocation().x;
                    totalProtectorY += robot.getLocation().y;
                    numProtectors++;

                    if(closestMuck != null && 
                        closestMuckDist > tempLoc.distanceSquaredTo(closestMuck.getLocation())) {
                        Debug.setIndicatorDot(Debug.info, tempLoc, 255, 255, 255);
                        Debug.setIndicatorLine(Debug.info, currLoc, tempLoc, 255, 255, 255);
                        Debug.println("Found a pol closer to my closest muck");
                        amClosestPolToClosestMuck = false;
                    }
                } else if (Comms.isSubRobotType(flag, Comms.SubRobotType.POL_RUSH)) {
                    rusherLoc = robot.getLocation();
                }

                if(Comms.isSubRobotType(flag, Comms.SubRobotType.SLANDERER)) {
                    slandererOrECNearby = true;
                    slandererNearby = true;
                    if (tempDist < nearestSlandyDist) {
                        nearestSlandy = tempLoc;
                        nearestSlandyDist = tempDist;
                    }
                }

                if(closestMuckToHome != null) {
                    if(flag == Comms.getFlag(Comms.InformationCategory.FOLLOWING, closestMuckToHome.getID())) {
                        numFollowingClosestMuckraker++;
                    }
                }
            }
            
            if ((robot.getType() == RobotType.ENLIGHTENMENT_CENTER)) {
                slandererOrECNearby = true;
                ECNearby = true;
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
            }
        } else {
            Debug.println(Debug.info, "Can't get home flag: " + homeID);
        }
        
        /* Step by Step decision making*/
        //empower if near 2 enemies or enemy is in sensing radius of our base
        if ((numMuckAttackable > 1 || 
            (numMuckAttackable > 0 && (slandererNearby || closestMuckToHomeDist <= 2 * RobotType.ENLIGHTENMENT_CENTER.sensorRadiusSquared || maxMuckrakerAttackableSize > 1) &&
                amClosestPolToClosestMuck))
            && rc.canEmpower(maxMuckAttackableDistSquared)) {
            Debug.println(Debug.info, "Enemy too close to base. I will empower with radius: " + maxMuckAttackableDistSquared);
            Debug.setIndicatorLine(Debug.info, rc.getLocation(), farthestMuckAttackable, 255, 150, 50);
            rc.empower(maxMuckAttackableDistSquared);
            return;
        }

        if (maxPoliticianSizeWithinReasonableThreshold > 0 && enemyAttackable.length >= numFriendlyAttackable && 
            rc.getEmpowerFactor(rc.getTeam(), 0) >= Util.chainEmpowerFactor && rc.canEmpower(maxPolAttackableDistSquared) && numReasonablePols >= 2) {
            Debug.println(Debug.info, "Empowering because of high buff, trying to start a chain");
            rc.empower(maxPolAttackableDistSquared);
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

        // This means that the first half of an EC-ID/EC-ID broadcast finished.
        if(needToBroadcastHomeEC && rc.getFlag(rc.getID()) == defaultFlag) { broadcastHomeEC(); }
        else if(broadcastECLocation());
        else if(closestEnemy != null && broadcastEnemyLocalOrGlobal(closestEnemy.getLocation(), closestEnemyType));
        
        main_direction = Direction.CENTER;

        // if(closestMuck != null) {
        //     Debug.println("AmClosestPol: " + amClosestPolToClosestMuck);
        //     Debug.setIndicatorLine(Debug.info, rc.getLocation(), closestMuck.getLocation(), 255, 200, 100);
        // }

        //tries to block a muckraker in its path(if the muckraker is within 2 sensing radiuses of the EC)
        if (closestMuckToHome != null && 
            closestMuckToHome.getLocation().isWithinDistanceSquared(home, (5 * sensorRadius)) &&
            numFollowingClosestMuckraker < Util.maxFollowingSingleUnit &&
            amClosestPolToClosestMuck) {
            Debug.println(Debug.info, "I am pushing a muckraker away. ID: " + closestMuckToHome.getID());
            setFlag(Comms.getFlag(Comms.InformationCategory.FOLLOWING, closestMuckToHome.getID()));
            Debug.println(Debug.info, "I am pushing a muckraker away");
            MapLocation closestMuckrakerSensableLoc = closestMuckToHome.getLocation();
            Direction muckrakerPathtoBase = closestMuckrakerSensableLoc.directionTo(home);
            MapLocation squareToBlock = closestMuckrakerSensableLoc.add(muckrakerPathtoBase);
            main_direction = currLoc.directionTo(squareToBlock);
        }
        // else if(closestMuck != null && 
        //         numFollowingClosestMuckraker < Util.maxFollowingSingleUnit &&
        //         amClosestPolToClosestMuck) {
        //     main_direction = currLoc.directionTo(closestMuck.getLocation());
        //     setFlag(Comms.getFlag(Comms.InformationCategory.FOLLOWING, closestMuck.getID()));
        //     Debug.println("I am following a muckraker. ID: " + closestMuck.getID());
        //     Debug.setIndicatorLine(Debug.info, rc.getLocation(), closestMuck.getLocation(), 255, 200, 100);
        // }
        //moves out of sensor radius of Enlightenment Center
        else if(ECNearby) { //this is the old case, changed it to what is below for experimentation
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
            if(!explorer) {
                Debug.println(Debug.info, "I see no slanderers, and cannot lattice. Rotating towards ec");
                main_direction = Util.rotateOppositeSpinDirection(spinDirection, currLoc.directionTo(home));
            }
            else {
                main_direction = Nav.explore();
                Debug.println(Debug.info, "I see nobody, chilling out");
            }
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
    }
}