package naivemucks;
import battlecode.common.*;

import naivemucks.Comms.*;
import naivemucks.Util.*;
import naivemucks.Debug.*;
import naivemucks.fast.FastIterableIntSet;
import naivemucks.fast.FasterQueue;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.PriorityQueue;
/* 
TODO: 
have tower to tower communication(slanderers communicate best direction, tell protectors about enemies)
have some muckrakers explore while others just attack enemy bases
have a better system for making protectors(we should not make 35 protectors for one muckraker)
*/
public class EC extends Robot {
    static enum State {
        BUILDING_SLANDERERS,
        BUILDING_PROTECTORS,
        BUILDING_SPAWNKILLS,
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
    static int cleanUpCount;
    static int currRoundNum;
    static int currInfluence;
    static int totalGolemConviction;
    static boolean noAdjacentEC;

    static boolean muckrackerNear;
    //start spawning slanderers in random directions until you find an enemy
    static Direction avgDirectionOfEnemies;
    
    static FastIterableIntSet idSet;
    static FastIterableIntSet protectorIdSet;
    static int[] ids;
    static int[] protectorIds;
    static PriorityQueue<RushFlag> ECflags;
    static ArrayDeque<State> stateStack;

    static int numProtectors;
    static int protectorsSpawnedInARow;
    static boolean canGoBackToBuildingProtectors;

    static State currentState;

    static int lastRush;
    static int spawnKillLock;

    public EC(RobotController r) {
        super(r);
        idSet = new FastIterableIntSet(1000);
        ids = idSet.ints;

        protectorIdSet = new FastIterableIntSet(100);
        protectorIds = protectorIdSet.ints;

        ECflags = new PriorityQueue<RushFlag>();
        stateStack = new ArrayDeque<State>();
        currentState = State.BUILDING_SLANDERERS;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.EC);
        avgDirectionOfEnemies = Util.randomDirection();
        cleanUpCount = 0;
        numProtectors = 0;
        lastRush = 0;
        canGoBackToBuildingProtectors = true;
        spawnKillLock = 10;
    }

    public boolean buildRobot(RobotType toBuild, int influence) throws GameActionException {
        Debug.println(Debug.info, "building robot type: " + toBuild + " influence: " + influence);
        DirectionPreference pref = DirectionPreference.RANDOM;
        if(currentState == State.BUILDING_SPAWNKILLS) {
            pref = DirectionPreference.ORTHOGONAL;
        }
        
        Direction[] orderedDirs = Util.getOrderedDirections(pref);

        for(Direction dir : orderedDirs) {
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                rc.buildRobot(toBuild, dir, influence);
                RobotInfo robot = rc.senseRobotAtLocation(home.add(dir));
                if(robot != null) {
                    Debug.println(Debug.info, "built robot: " + robot.getID());
                    idSet.add(robot.getID());

                    if (Comms.getIC(nextFlag) == Comms.InformationCategory.TARGET_ROBOT && 
                        Comms.getSubRobotType(nextFlag) == SubRobotType.POL_PROTECTOR) {
                        protectorIdSet.add(robot.getID());
                    }
                } else {
                    System.out.println("CRITICAL: EC didn't find the robot it just built");
                }

                resetFlagOnNewTurn = true;
                robotCounter += 1;
                return true;
            }
        }

        return false;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if (spawnKillLock < 10) {
            spawnKillLock++;
        }

        for (RobotInfo robot : enemySensable) {
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                noAdjacentEC = false;
            }
            
        }

        Debug.println(Debug.info, "I am a " + rc.getType() + "; current influence: " + currInfluence);
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        Debug.println(Debug.info, "num of ec's found: " + ECflags.size());
        Debug.println(Debug.info, "num ids found: " + idSet.size);
        Debug.println(Debug.info, "num protectors currently: " + protectorIdSet.size);
        Debug.println(Debug.info, "state: " + currentState);
        Debug.println(Debug.info, "avg direction of enemies: " + avgDirectionOfEnemies);
        Debug.println(Debug.info, "protectors built in a row: " + protectorsSpawnedInARow);
        Debug.println(Debug.info, "can reset flag on next turn: " + resetFlagOnNewTurn);


        processChildrenFlags();
        Debug.println(Debug.info, "total Golem Conviction: " + totalGolemConviction);

        if (currRoundNum > 250)
            tryStartCleanup();

        toggleBuildProtectors();
        tryStartBuildingSpawnKill();

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
        Debug.println(Debug.info, "Amount bid: " + biddingInfluence);

        //updating currInfluence after a bid
        currInfluence = rc.getInfluence();
        muckrackerNear = checkIfMuckrakerNear();

        switch(currentState) {
            case BUILDING_SLANDERERS:
                Debug.println(Debug.info, "building slanderers state");

                //rush if we can
                RushFlag targetECFromSlanderers = ECflags.peek();
                if(targetECFromSlanderers != null) {
                    int requiredInfluence = targetECFromSlanderers.requiredInfluence;

                    MapLocation enemyLocation = home.translate(targetECFromSlanderers.dx - Util.dOffset, targetECFromSlanderers.dy - Util.dOffset);
                    Debug.setIndicatorLine(Debug.info, home, enemyLocation, 100, 255, 100);
                    if(requiredInfluence < totalGolemConviction) {
                        resetFlagOnNewTurn = false;
                        nextFlag = Comms.getFlag(Comms.InformationCategory.RUSH_EC_GOLEM, targetECFromSlanderers.dx, targetECFromSlanderers.dy);
                        break;
                    }
                    if(requiredInfluence < currInfluence) {
                        resetFlagOnNewTurn = false;
                        nextFlag = targetECFromSlanderers.flag;
                        stateStack.push(currentState);
                        currentState = State.RUSHING;
                        break;
                    }  
                }
            
                if(!muckrackerNear && robotCounter % 2 != 0) {
                    toBuild = RobotType.SLANDERER;
                    signalSlandererAwayDirection(avgDirectionOfEnemies.opposite());
                    influence = Util.getBestSlandererInfluence(currInfluence);
                } else {
                    toBuild = RobotType.MUCKRAKER;
                    influence = 1;
                }

                if (robotCounter == 0 && noAdjacentEC) {
                    //the first robot built should be a 107 slanderer, otherwise in this state
                    //we will build 130 slanderers at minimum
                    toBuild = RobotType.SLANDERER;
                    signalSlandererAwayDirection(avgDirectionOfEnemies.opposite());
                    influence = 107;
                    stateStack.push(currentState);
                    currentState = State.BUILDING_PROTECTORS;
                }

                boolean built_bot = false;
                if (toBuild != null) built_bot = buildRobot(toBuild, influence);

                if (!canGoBackToBuildingProtectors) {
                    if (built_bot && toBuild == RobotType.SLANDERER) {
                        canGoBackToBuildingProtectors = true;
                        currentState = State.BUILDING_PROTECTORS;
                        protectorsSpawnedInARow = 0;
                    }
                }
                else {
                    if (tryStartRemovingBlockage()) {} 
                    else {tryStartSavingForRush();}
                }

                break;
            case BUILDING_PROTECTORS:
                Debug.println(Debug.info, "building protectors state");
                if (robotCounter % 4 != 0) {
                    toBuild = RobotType.POLITICIAN;
                    influence = 18;
                    signalRobotType(SubRobotType.POL_PROTECTOR);
                }
                else {
                    toBuild = RobotType.MUCKRAKER;
                    influence = 1;
                }
                
                boolean built_robot = false;
                if (toBuild != null) {
                    built_robot = buildRobot(toBuild, influence);
                }

                if (built_robot && toBuild == RobotType.POLITICIAN) protectorsSpawnedInARow++;

                if (robotCounter == 2 && built_robot && toBuild == RobotType.POLITICIAN) {
                    Debug.println(Debug.info, "built a protector and on first round");
                    currentState = stateStack.pop();
                    Debug.println(Debug.info, "switching back to state: " + currentState);
                }

                tryStartRemovingBlockage();
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
                Debug.setIndicatorLine(Debug.info, home, enemyLocation, 100, 255, 100);
                if(requiredInfluence < totalGolemConviction) {
                    resetFlagOnNewTurn = false;
                    nextFlag = Comms.getFlag(Comms.InformationCategory.RUSH_EC_GOLEM, targetEC.dx, targetEC.dy);
                    currentState = stateStack.pop();
                    break;
                }
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
                
                buildRobot(toBuild, influence);
                
                tryStartRemovingBlockage();
                break;
            case CLEANUP:
                if(Util.getBestSlandererInfluence(currInfluence) >= 100 && robotCounter % 3 == 1 && !muckrackerNear) {
                    toBuild = RobotType.SLANDERER;
                    signalSlandererAwayDirection(avgDirectionOfEnemies.opposite());
                    influence = Util.getBestSlandererInfluence(currInfluence / 2);
                }
                else if(currInfluence >= 100 && robotCounter % 3 == 0) {
                    toBuild = RobotType.POLITICIAN;
                    influence = Util.cleanupPoliticianInfluence;
                    signalRobotType(Comms.SubRobotType.POL_CLEANUP);
                } else {
                    toBuild = RobotType.MUCKRAKER;
                    influence = 1;
                }

                if(toBuild != null) {
                    buildRobot(toBuild, influence);
                }
                
                break;
            case REMOVING_BLOCKAGE:
                toBuild = RobotType.POLITICIAN;
                influence = 30;
                signalRobotType(Comms.SubRobotType.POL_DEFENDER);
                if(buildRobot(toBuild, influence)) {
                    currentState = stateStack.pop();
                }
                break;
            case BUILDING_SPAWNKILLS:
                if(rc.getEmpowerFactor(rc.getTeam(),11) <= Util.spawnKillThreshold) {
                    currentState = stateStack.pop();
                    break;
                }
                Debug.println(Debug.info, "Building spawn kill politician");
                influence = 6*rc.getInfluence()/8;
                toBuild = RobotType.POLITICIAN;
                signalRobotType(Comms.SubRobotType.POL_SPAWNKILL);
                if(buildRobot(toBuild, influence)) {
                    spawnKillLock = 0;
                    currentState = stateStack.pop();
                }
                break;
            default:
                System.out.println("CRITICAL: Maxwell screwed up stateStack");
                break;
        }
        Debug.println(Debug.info, "next flag that will be set: " + nextFlag);
    }

    public void initializeGlobals() throws GameActionException {
        super.initializeGlobals();

        toBuild = null;
        influence = 0;
        currRoundNum = rc.getRoundNum();
        currInfluence = rc.getInfluence();
        totalGolemConviction = 0;
        noAdjacentEC = true;

    }

    public void toggleBuildProtectors() throws GameActionException {
        // Debug.println(Debug.info, "can go back to building protectors: " + canGoBackToBuildingProtectors);
        // Debug.println(Debug.info, "protector id set size: " + protectorIdSet.size);
        // Debug.println(Debug.info, "current state from toggle building protectors: " + currentState);
        // Debug.println(Debug.info, "robot counter from toggle protectors: " + robotCounter);

        if (protectorIdSet.size <= 25 && currentState != State.BUILDING_PROTECTORS && resetFlagOnNewTurn && 
            robotCounter > 40 && canGoBackToBuildingProtectors && noAdjacentEC) {
            Debug.println(Debug.info, "switching to building protectors");
            stateStack.push(currentState);
            currentState = State.BUILDING_PROTECTORS;
            protectorsSpawnedInARow = 0;
        } else if (protectorIdSet.size > 35 && currentState == State.BUILDING_PROTECTORS) {
            currentState = stateStack.pop();
        } else if (protectorsSpawnedInARow >= 10 && currentState == State.BUILDING_PROTECTORS) {
            canGoBackToBuildingProtectors = false;
            currentState = State.BUILDING_SLANDERERS;
        }
    }

    public void tryStartBuildingSpawnKill() throws GameActionException {
        Debug.println(Debug.info, "spawn kill lock: " + spawnKillLock);
        if (rc.getEmpowerFactor(rc.getTeam(),0) > Util.spawnKillThreshold && spawnKillLock >= 10) {
            Debug.println(Debug.info, "Switching to building spawn kills");
            stateStack.push(currentState);
            resetFlagOnNewTurn = true;
            currentState = State.BUILDING_SPAWNKILLS;
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

    public void processChildrenFlags() throws GameActionException {
        idSet.updateIterable();
        protectorIdSet.updateIterable();

        int totalEnemyX = 0;
        int totalEnemyY = 0;
        int numChildrenFoundEnemies = 0;

        int id;
        for(int j = idSet.size - 1; j >= 0; j--) {
            id = ids[j];
            if(rc.canGetFlag(id)) {
                int flag = rc.getFlag(id);
                Comms.InformationCategory flagIC = Comms.getIC(flag);
                switch (flagIC) {
                    case NEUTRAL_EC:
                    case ENEMY_EC:
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
                        break;
                    case FRIENDLY_EC:
                        int[] currDxDy = Comms.getDxDy(flag);
                        RushFlag rushFlag = new RushFlag(0, currDxDy[0], currDxDy[1], 0);
                        ECflags.remove(rushFlag);
                        break;
                    case ENEMY_FOUND:

                        int[] enemyDxDy = Comms.getDxDy(flag);
                        int enemyLocX = enemyDxDy[0] + home.x - Util.dOffset;
                        int enemyLocY = enemyDxDy[1] + home.y - Util.dOffset;
                        totalEnemyX += enemyLocX;
                        totalEnemyY += enemyLocY;

                        MapLocation enemyLoc = new MapLocation(enemyLocX, enemyLocY);
                        if (rc.getLocation().isWithinDistanceSquared(enemyLoc, rc.getType().sensorRadiusSquared * 4) &&
                            currentState != State.BUILDING_PROTECTORS && protectorIdSet.size <= 25 && canGoBackToBuildingProtectors && noAdjacentEC) {
                            stateStack.push(currentState);
                            currentState = State.BUILDING_PROTECTORS;
                            protectorsSpawnedInARow = 0;
                        }

                        numChildrenFoundEnemies++;
                        
                        break;
                }
            } else {
                idSet.remove(id);
            }


        }

        //find the combined sizes of the golems
        for(RobotInfo robot: friendlySensable) {
            if (rc.canGetFlag(robot.getID())) {
                int currFlag = rc.getFlag(robot.getID());
                if (Comms.getIC(currFlag) == Comms.InformationCategory.ROBOT_TYPE && Comms.getSubRobotType(currFlag) == Comms.SubRobotType.POL_GOLEM) {
                    totalGolemConviction += robot.getConviction();
                    Debug.println(Debug.info, "Total Golem Conviction Updated: " + totalGolemConviction);
                }
            }
        }
        
        //remove protector ids if dead
        int protectorID;
        for (int i = protectorIdSet.size - 1; i >= 0; i--) {
            protectorID = protectorIds[i];
            if (!rc.canGetFlag(protectorID)) {
                protectorIdSet.remove(protectorID);
            }
        }

        //for calculation of average direction to enemy
        if (numChildrenFoundEnemies != 0) {
            MapLocation enemyTotalDirection = new MapLocation(totalEnemyX / numChildrenFoundEnemies, totalEnemyY / numChildrenFoundEnemies);
            avgDirectionOfEnemies = home.directionTo(enemyTotalDirection);
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
        if (rushFlag == null) {
            resetFlagOnNewTurn = true;
            currentState = stateStack.pop();
            return false;
        }
        influence = rushFlag.requiredInfluence;

        MapLocation enemyLocation = home.translate(rushFlag.dx - Util.dOffset, rushFlag.dy - Util.dOffset);
        Debug.setIndicatorLine(Debug.info, home, enemyLocation, 255, 150, 50);

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