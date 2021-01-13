package musketeerplayersprint2;
import battlecode.common.*;

import musketeerplayersprint2.Comms.*;
import musketeerplayersprint2.Util.*;
import musketeerplayersprint2.Debug.*;
import musketeerplayersprint2.fast.FastIterableIntSet;
import java.util.ArrayDeque;
import java.util.PriorityQueue;

public class EC extends Robot {
    static enum State {
        PHASE1,
        PHASE2,
        RUSHING,
        SAVING_FOR_RUSH,
        CLEANUP,
        REMOVING_BLOCKAGE
    };

    static class RushFlag implements Comparable<RushFlag> {
        int requiredInfluence;
        int dx;
        int dy;
        int flag;

        RushFlag(int r, int x, int y, int f) {
            requiredInfluence = r;
            dx = x;
            dy = y;
            flag = f;
        }

        public int compareTo(RushFlag other) {
            return Integer.compare(requiredInfluence, other.requiredInfluence);
        }

        public boolean equals(Object o) { 
            if (o == this) { 
                return true; 
            } 
        
            if (!(o instanceof RushFlag)) { 
                return false; 
            }
            RushFlag other = (RushFlag) o;
            return dx == other.dx && dy == other.dy;
        }
    }

    static int robotCounter;
    static RobotType toBuild;
    static int influence;

    static int cleanUpCount = 0;

    static int currRoundNum;
    static int currInfluence;
    static boolean needToBuild;
    static boolean muckrackerNear;

    //start spawning slanderers in random directions until you find an enemy
    static Direction avgDirectionOfEnemies;

    static FastIterableIntSet idSet;
    static int[] ids;
    static PriorityQueue<RushFlag> ECflags;
    static ArrayDeque<State> stateStack;

    static State currentState;

    static int lastRush = 0;

    public EC(RobotController r) {
        super(r);
        idSet = new FastIterableIntSet(1000);
        ids = idSet.ints;
        ECflags = new PriorityQueue<RushFlag>();
        stateStack = new ArrayDeque<State>();
        currentState = State.PHASE1;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.EC);
        avgDirectionOfEnemies = Util.randomDirection();
    }

    public boolean buildRobot(RobotType toBuild, int influence) throws GameActionException {
        Debug.println(Debug.info, "building robot type: " + toBuild + " influence: " + influence);
        Direction main_direction = Util.randomDirection();
        int num_direction = 8;
        while(num_direction != 0) {
            if (rc.canBuildRobot(toBuild, main_direction, influence)) {
                rc.buildRobot(toBuild, main_direction, influence);
                RobotInfo robot = rc.senseRobotAtLocation(home.add(main_direction));
                if(robot != null) {
                    Debug.println(Debug.info, "built robot: " + robot.getID());
                    idSet.add(robot.getID());
                } else {
                    System.out.println("CRITICAL: build robot didn't find the robot it just built");
                }

                resetFlagOnNewTurn = true;
                robotCounter += 1;
                return true;
            }
            main_direction = main_direction.rotateRight();
            num_direction--;
        }

        return false;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Debug.println(Debug.info, "I am a " + rc.getType() + "; current influence: " + currInfluence);
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        Debug.println(Debug.info, "num of ec's found: " + ECflags.size());
        Debug.println(Debug.info, "num ids found: " + idSet.size);
        Debug.println(Debug.info, "state: " + currentState);
        Debug.println(Debug.info, "avg direction of enemies: " + avgDirectionOfEnemies);

        checkForTowersAndEnemyDirection();
        if (currRoundNum > 500)
            tryStartCleanup();

        //bidding code
        int biddingInfluence = currInfluence / 20;
        if (rc.canBid(biddingInfluence) && currRoundNum > 500) {
            rc.bid(biddingInfluence);
        } else {
            biddingInfluence = Math.max(currInfluence / 100, 1);
            if (rc.canBid(biddingInfluence)) {
                rc.bid(biddingInfluence);
            }
        }

        //updating currInfluence after a bid
        currInfluence = rc.getInfluence();
        muckrackerNear = checkIfMuckrakerNear();
        
        switch(currentState) {
            case PHASE1:
                Debug.println(Debug.info, "Phase1 state");
                if(Util.getBestSlandererInfluence(currInfluence) >= 130 && robotCounter % 5 == 0 && !muckrackerNear) {
                    toBuild = RobotType.SLANDERER;
                    signalSlandererAwayDirection(avgDirectionOfEnemies.opposite());
                    influence = Util.getBestSlandererInfluence(currInfluence);
                } else {
                    toBuild = RobotType.MUCKRAKER;
                    influence = 1;
                }
                
                if(needToBuild) {
                    buildRobot(toBuild, influence);
                }
                tryStartSavingForRush();
                break;
            case PHASE2:
                Debug.println(Debug.info, "Phase2 state");
                if(Util.getBestSlandererInfluence(7 * currInfluence / 8) >= 150 && robotCounter % 5 == 0 && !muckrackerNear) {
                    toBuild = RobotType.SLANDERER;
                    signalSlandererAwayDirection(avgDirectionOfEnemies.opposite());
                    influence = Util.getBestSlandererInfluence(7 * currInfluence / 8);
                } else {
                    toBuild = RobotType.MUCKRAKER;
                    influence = 1;
                }
                
                if(needToBuild) {
                    buildRobot(toBuild, influence);
                }                
                if (tryStartRemovingBlockage()) {}   
                else {tryStartSavingForRush();}
                break;
            case RUSHING:
                trySendARush();
                break;
            case SAVING_FOR_RUSH:
                RushFlag targetEC = ECflags.peek();
                if(targetEC == null) {
                    currentState = stateStack.pop();
                    break;
                }
                int requiredInfluence = targetEC.requiredInfluence;

                MapLocation enemyLocation = home.translate(targetEC.dx - Util.dOffset, targetEC.dy - Util.dOffset);
                Debug.setIndicatorLine(home, enemyLocation, 100, 255, 100);

                if(requiredInfluence < currInfluence) {
                    resetFlagOnNewTurn = false;
                    nextFlag = targetEC.flag;
                    currentState = State.RUSHING;
                    break;
                }
                int[] currDxDy = {targetEC.dx, targetEC.dy};
                toBuild = RobotType.MUCKRAKER;
                influence = 1;
                Debug.println(Debug.info, "Required Influence: " + requiredInfluence + "; DxDy: " + (currDxDy[0] - Util.dOffset) +  ", " + (currDxDy[1] - Util.dOffset));
                if(needToBuild) {
                    buildRobot(toBuild, influence);
                }
                tryStartRemovingBlockage();
                break;
            case CLEANUP:
                if(Util.getBestSlandererInfluence(currInfluence) >= 100 && robotCounter % 2 == 0 && !muckrackerNear) {
                    toBuild = RobotType.SLANDERER;
                    signalSlandererAwayDirection(avgDirectionOfEnemies.opposite());
                    influence = Util.getBestSlandererInfluence(currInfluence / 2);
                }
                else if(currInfluence >= 100) {
                    toBuild = RobotType.POLITICIAN;
                    influence = Util.cleanupPoliticianInfluence;
                    signalRobotType(Comms.SubRobotType.POL_CLEANUP);
                } 
                if(needToBuild && toBuild != null) {
                    buildRobot(toBuild, influence);
                }
                break;
            case REMOVING_BLOCKAGE:
                toBuild = RobotType.POLITICIAN;
                influence = 30;
                signalRobotType(Comms.SubRobotType.POL_DEFENDER);
                if(needToBuild && buildRobot(toBuild, influence)) {
                    currentState = stateStack.pop();
                }
                break;
            default:
                System.out.println("CRITICAL: Maxwell screwed up stateStack");
                break;
        }
    }

    public void initializeGlobals() throws GameActionException {
        super.initializeGlobals();

        toBuild = null;
        influence = 0;
        needToBuild = true;
        currRoundNum = rc.getRoundNum();
        currInfluence = rc.getInfluence();

        if(currentState == State.PHASE1 && turnCount > Util.phaseOne) {
            currentState = State.PHASE2;
        }
    }
    
    public boolean checkIfMuckrakerNear() throws GameActionException {
        for(RobotInfo robot: rc.senseNearbyRobots(RobotType.MUCKRAKER.actionRadiusSquared, enemy)) {
            if(robot.getType() == RobotType.MUCKRAKER) {
                return true;
            }
        }
        return false;
    }

    public void checkForTowersAndEnemyDirection() throws GameActionException {
        idSet.updateIterable();

        int directionTotal = 0;
        int numChildrenThatFoundEnemy = 0;
        int id;
        for(int j = idSet.size - 1; j >= 0; j--) {
            id = ids[j];
            if(rc.canGetFlag(id)) {
                int flag = rc.getFlag(id);
                Comms.InformationCategory flagIC = Comms.getIC(flag);
                if((flagIC == Comms.InformationCategory.NEUTRAL_EC || flagIC == Comms.InformationCategory.ENEMY_EC)) {
                    int neededInf =  (int) Math.exp(Comms.getInf(flag) * Math.log(Comms.INF_LOG_BASE));
                    int currReqInf = (int)  neededInf * 4 + 10;
                    if (neededInf <= Util.maxECRushConviction || rc.getInfluence() >= (currReqInf * 3 / 4)) {
                        // Debug.println(Debug.info, "Current Inluence: " + rc.getInfluence() + ", Tower inf: " + neededInf);
                        int[] currDxDy = Comms.getDxDy(flag);
                        RushFlag rushFlag = new RushFlag(currReqInf, currDxDy[0], currDxDy[1], flag);
                        ECflags.remove(rushFlag);
                        ECflags.add(rushFlag);
                        cleanUpCount = -1;
                        if (currentState == State.CLEANUP) {
                            currentState = stateStack.pop();
                        }
                    }
                } else if(flagIC == Comms.InformationCategory.FRIENDLY_EC) {
                    int[] currDxDy = Comms.getDxDy(flag);
                    RushFlag rushFlag = new RushFlag(0, currDxDy[0], currDxDy[1], 0);
                    ECflags.remove(rushFlag);
                }
                //for calculation of average direction to enemy
                if (flagIC == Comms.InformationCategory.ENEMY_FOUND) {
                    int[] enemyDxDy = Comms.getDxDy(flag);
                    MapLocation spawningLoc = rc.getLocation();
                    MapLocation enemyLoc = new MapLocation(enemyDxDy[0] + spawningLoc.x - Util.dOffset, enemyDxDy[1] + spawningLoc.y - Util.dOffset);
                    Direction dirToEnemy = spawningLoc.directionTo(enemyLoc);
                    directionTotal += dirToEnemy.ordinal();
                    numChildrenThatFoundEnemy++;
                }
            } else {
                idSet.remove(id);
            }
        }
        //for calculation of average direction to enemy
        if (numChildrenThatFoundEnemy != 0) {
            int enemyDirectionAverage = (int) (directionTotal / numChildrenThatFoundEnemy);
            avgDirectionOfEnemies = Direction.values()[enemyDirectionAverage];
        }

        cleanUpCount++;
    }

    public boolean tryStartCleanup() throws GameActionException {
        Debug.println(Debug.info, "Cleanup count: " + cleanUpCount);
        if (cleanUpCount > Util.startCleanupThreshold && currentState != State.CLEANUP) {
            stateStack.push(currentState);
            currentState = State.CLEANUP;
            return true;
        }
        return false;
    }

    public boolean tryStartSavingForRush() throws GameActionException {
        if (!ECflags.isEmpty() && turnCount > lastRush + Util.minTimeBetweenRushes) {
            stateStack.push(currentState);
            currentState = State.SAVING_FOR_RUSH;
            Debug.println(Debug.info, "tryStartSavingForRush is returning true");
            return true;
        }
        return false;
    }

    public boolean tryStartRemovingBlockage() throws GameActionException {
        int num_enemies_near = 0;
        for (Direction dir : Util.directions) {
            MapLocation surroundingLoc = home.add(dir);
            if (rc.onTheMap(surroundingLoc)){
                RobotInfo surroundingBot = rc.senseRobotAtLocation(surroundingLoc);
                if (surroundingBot != null && surroundingBot.getTeam() == enemy) {
                    num_enemies_near++;
                }
            }
        }
        Debug.println(Debug.info, "num enemies surrounding: " + num_enemies_near);
        if (num_enemies_near >= 7) {
            stateStack.push(currentState);
            currentState = State.REMOVING_BLOCKAGE;
            return true;
        }
        return false;
    }

    public boolean trySendARush() throws GameActionException {
        Debug.println(Debug.info, "building rush bots");

        toBuild = RobotType.POLITICIAN;
        RushFlag rushFlag = ECflags.peek();
        influence = rushFlag.requiredInfluence;

        MapLocation enemyLocation = home.translate(rushFlag.dx - Util.dOffset, rushFlag.dy - Util.dOffset);
        Debug.setIndicatorLine(home, enemyLocation, 255, 150, 50);

        if (influence >= currInfluence) {
            currentState = State.SAVING_FOR_RUSH;
            return false;
        }
        
        if(buildRobot(toBuild, influence)) {
            ECflags.remove();
            resetFlagOnNewTurn = true;
            currentState = stateStack.pop();
            lastRush = turnCount;
        }

        return true;
    }

    void signalRobotType(Comms.SubRobotType type) throws GameActionException {
        if (resetFlagOnNewTurn) {
            nextFlag = Comms.getFlag(Comms.InformationCategory.TARGET_ROBOT, type);
            resetFlagOnNewTurn = false;
        }
    }

    void signalSlandererAwayDirection(Direction awayDirection) throws GameActionException {
        if (resetFlagOnNewTurn) {
            nextFlag = Comms.getFlag(Comms.InformationCategory.SPECIFYING_SLANDERER_DIRECTION, awayDirection);
        }
    }
}