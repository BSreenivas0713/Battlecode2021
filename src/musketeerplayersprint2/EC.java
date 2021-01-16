package musketeerplayersprint2;
import battlecode.common.*;

import musketeerplayersprint2.Comms.*;
import musketeerplayersprint2.Util.*;
import musketeerplayersprint2.Debug.*;
import musketeerplayersprint2.fast.FastIterableIntSet;
import musketeerplayersprint2.fast.FastIterableLocSet;

import java.util.ArrayDeque;
import java.util.PriorityQueue;
/* 
TODO: 
No 1 priority: within first 100 rounds, currReqInf is 1.4*neutral EC influence (if EC is neutral)
have tower to tower communication(slanderers tell protectors about enemies)
muckraker to muckraker communication(what base to attack, what base has been converted to ours)
have some protectors go in the direction of the slanderers DO NOT FORGET ABOUT THIS
have more muckrakers at the beginning so we explore quicker/dont auto lose on small maps
Muckrakers should report slanderers to the EC

not sure how protectors should figure out who slanderers are since they basically never have the slanderer flag
Right now ECs do not propogate flags
*/
public class EC extends Robot {
    static enum State {
        BUILDING_SLANDERERS,
        BUILDING_PROTECTORS,
        BUILDING_SPAWNKILLS,
        RUSHING,
        SAVING_FOR_RUSH,
        CLEANUP,
        REMOVING_BLOCKAGE,
    };

    static class RushFlag implements Comparable<RushFlag> {
        int requiredInfluence;
        int dx;
        int dy;
        int flag;
        Team team;

        RushFlag(int r, int x, int y, int f, Team t) {
            requiredInfluence = r;
            dx = x;
            dy = y;
            flag = f;
            team = t;
        }

        public int compareTo(RushFlag other) {
            if (Math.abs(requiredInfluence - other.requiredInfluence) < 20) {
                return Integer.compare(dx*dx+dy*dy, other.dx*other.dx+other.dy*other.dy);
            } else {
                return Integer.compare(requiredInfluence, other.requiredInfluence);
            }
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
    static int numMucks = 0;
    static int currInfluence;
    static boolean noAdjacentEC;

    static boolean muckrackerNear;
    
    static FastIterableIntSet idSet;
    static FastIterableIntSet protectorIdSet;
    static int[] ids;
    static int[] protectorIds;
    static PriorityQueue<RushFlag> ECflags;
    static ArrayDeque<State> stateStack;

    static int numProtectors;
    static int protectorsSpawnedInARow;
    static boolean canGoBackToBuildingProtectors;
    static boolean haveSeenEnemy;

    static State currentState;

    static int lastRush;
    static int spawnKillLock;
    static int lastSuccessfulBlockageRemoval;

    static int bigBid;
    static int prevBid;
    static int littleBid;
    static boolean wonLastBid;
    static int lastVoteCount;
    static FastIterableLocSet enemyECsFound;

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

        cleanUpCount = 0;
        numProtectors = 0;
        lastRush = 0;
        canGoBackToBuildingProtectors = true;
        spawnKillLock = 10;
        lastSuccessfulBlockageRemoval = -1;
        littleBid = 0;
        prevBid = 0;
        bigBid = 1;
        wonLastBid = false;
        lastVoteCount = 0;
        enemyECsFound = new FastIterableLocSet(20);
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
                    if(robot.getType() == RobotType.MUCKRAKER) {
                        numMucks ++;
                        Debug.println(Debug.info, "Num Mucks being updated, new value: " + numMucks);
                    }
                    Debug.println(Debug.info, "built robot: " + robot.getID());
                    idSet.add(robot.getID());

                    Comms.InformationCategory IC = Comms.getIC(nextFlag);
                    if ((IC == Comms.InformationCategory.TARGET_ROBOT && 
                        Comms.getSubRobotType(nextFlag) == SubRobotType.POL_PROTECTOR)) {
                        protectorIdSet.add(robot.getID());
                    }
                } else {
                    System.out.println("CRITICAL: EC didn't find the robot it just built");
                }
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

        processChildrenFlags();

        // Override everything for a spawn kill. This is fine, as it only takes 1 turn
        // and at most happens once every 10 turns.
        tryStartBuildingSpawnKill();

        if (currentState != State.BUILDING_SPAWNKILLS) {
            // If we have enough to rush a tower, make that the #1 priority
            if (readyToRush()) {
                if (currentState != State.RUSHING) {
                    if (currentState != State.SAVING_FOR_RUSH) {
                        stateStack.push(currentState);
                    }
                    if (currentState != State.BUILDING_SLANDERERS) {
                        canGoBackToBuildingProtectors = false;
                    }
                    currentState = State.RUSHING;
                }
            } else {
                // Second priority is removing blockage
                tryStartRemovingBlockage();
                // Third priority is building protectors.
                if (currentState != State.REMOVING_BLOCKAGE) {
                    toggleBuildProtectors();
                    // Fourth priority is saving for rush
                    if (currentState != State.BUILDING_PROTECTORS) {
                        if (tryStartSavingForRush()) {
                            if (currentState != State.SAVING_FOR_RUSH) {
                                stateStack.push(currentState);
                                currentState = State.SAVING_FOR_RUSH;
                            }
                        }
                        // If there's nothing to do, clean up
                        else if (currRoundNum > 500 && tryStartCleanup()) {
                            if (currentState != State.CLEANUP) {
                                stateStack.push(currentState);
                                currentState = State.CLEANUP;
                            }
                        }
                    }                
                }
            }
        }

        // At this point, state is either RUSHING, SAVING, BUILDING (spawn kill or protectors),or CLEANUP
        // At most, two things have been pushed to the state stack, the previous state, and whatever protectors overrode.

        //bidding code
        int biddingInfluence = bidBS();
        if (rc.canBid(biddingInfluence)) {
            rc.bid(biddingInfluence);
        }
        Debug.println(Debug.info, "Amount bid: " + biddingInfluence);

        //updating currInfluence after a bid
        currInfluence = rc.getInfluence();
        muckrackerNear = checkIfMuckrakerNear();

        Debug.println(Debug.info, "I am a " + rc.getType() + "; current influence: " + currInfluence);
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        Debug.println(Debug.info, "num of ec's found: " + ECflags.size());
        Debug.println(Debug.info, "num ids found: " + idSet.size);
        Debug.println(Debug.info, "num protectors currently: " + protectorIdSet.size);
        Debug.println(Debug.info, "State stack size: " + stateStack.size() + ", state: " + currentState);
        Debug.println(Debug.info, "protectors built in a row: " + protectorsSpawnedInARow);

        switch(currentState) {
            case BUILDING_SLANDERERS:
                Debug.println(Debug.info, "building slanderers state");            
                if(!muckrackerNear && robotCounter % 2 != 0) {
                    toBuild = RobotType.SLANDERER;
                    influence = Util.getBestSlandererInfluence(currInfluence);
                } else {
                    RushFlag targetEC = ECflags.peek();
                    if (numMucks % 2 == 0) {
                        if (targetEC != null && targetEC.team != Team.NEUTRAL) {
                            nextFlag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC_MUK, targetEC.dx, targetEC.dy);
                            Debug.println(Debug.info, "Making hunter mucker with destination " + targetEC.dx + ", " + targetEC.dy + ".");
                        } else {
                            nextFlag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC_MUK);
                            Debug.println(Debug.info, "Making hunter mucker with no destination.");
                        }
                    }
                    toBuild = RobotType.MUCKRAKER;
                    influence = 1;
                }

                if (robotCounter == 0 && noAdjacentEC) {
                    //the first robot built should be a 107 slanderer, otherwise in this state
                    //we will build 130 slanderers at minimum
                    toBuild = RobotType.SLANDERER;
                    influence = 107;
                }

                boolean built_bot = false;
                if (toBuild != null) built_bot = buildRobot(toBuild, influence);
                if (built_bot && toBuild == RobotType.SLANDERER && !canGoBackToBuildingProtectors) {
                    canGoBackToBuildingProtectors = true;
                    protectorsSpawnedInARow = 0;
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
                    RushFlag targetEC = ECflags.peek();
                    if (numMucks % 2 == 0) {
                        if (targetEC != null && targetEC.team != Team.NEUTRAL) {
                            nextFlag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC_MUK, targetEC.dx, targetEC.dy);
                            Debug.println(Debug.info, "Making hunter mucker with destination " + targetEC.dx + ", " + targetEC.dy + ".");
                        } else {
                            nextFlag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC_MUK);
                            Debug.println(Debug.info, "Making hunter mucker with no destination.");
                        }
                    }
                    toBuild = RobotType.MUCKRAKER;
                    influence = 1;
                }
                
                boolean built_robot = false;
                if (toBuild != null) {
                    built_robot = buildRobot(toBuild, influence);
                }

                if (built_robot && toBuild == RobotType.POLITICIAN) protectorsSpawnedInARow++;
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
                int[] currDxDy = {targetEC.dx, targetEC.dy};
                if (numMucks % 2 == 0) {
                    if(targetEC.team != Team.NEUTRAL) {
                        nextFlag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC_MUK, currDxDy[0], currDxDy[1]);
                        Debug.println(Debug.info, "Making hunter mucker with destination " + currDxDy[0] + ", " + currDxDy[1] + ".");
                    }
                    else {
                        nextFlag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC_MUK);
                        Debug.println(Debug.info, "Making hunter mucker with no desitation");
                    }
                }
                
                toBuild = RobotType.MUCKRAKER;
                influence = 1;
                buildRobot(toBuild, influence);
                break;
            case CLEANUP:
                if(Util.getBestSlandererInfluence(currInfluence) >= 100 && robotCounter % 3 == 1 && !muckrackerNear) {
                    toBuild = RobotType.SLANDERER;
                    influence = Util.getBestSlandererInfluence(currInfluence / 4);
                }
                else if(currInfluence >= 100 && robotCounter % 3 == 0) {
                    toBuild = RobotType.POLITICIAN;
                    influence = Util.cleanupPoliticianInfluence;
                    signalRobotType(Comms.SubRobotType.POL_CLEANUP);
                } else {
                    nextFlag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC_MUK);
                    Debug.println(Debug.info, "Making hunter mucker with no destination.");
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
                    lastSuccessfulBlockageRemoval = currRoundNum;
                    currentState = stateStack.pop();
                }
                break;
            case BUILDING_SPAWNKILLS:
                if(rc.getEmpowerFactor(rc.getTeam(),11) <= Util.spawnKillThreshold) {
                    currentState = stateStack.pop();
                    break;
                }
                Debug.println(Debug.info, "Building spawn kill politician");
                influence = 3*currInfluence/4;
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
        noAdjacentEC = true;
    }

    public void toggleBuildProtectors() throws GameActionException {
        // Debug.println(Debug.info, "can go back to building protectors: " + canGoBackToBuildingProtectors);
        // Debug.println(Debug.info, "protector id set size: " + protectorIdSet.size);
        // Debug.println(Debug.info, "current state from toggle building protectors: " + currentState);
        // Debug.println(Debug.info, "robot counter from toggle protectors: " + robotCounter);

        if (protectorIdSet.size <= 25 && currentState != State.BUILDING_PROTECTORS && 
            robotCounter > 40 && canGoBackToBuildingProtectors && noAdjacentEC) {
            Debug.println(Debug.info, "switching to building protectors");
            stateStack.push(currentState);
            currentState = State.BUILDING_PROTECTORS;
            protectorsSpawnedInARow = 0;
        } else if (protectorIdSet.size > 35 && currentState == State.BUILDING_PROTECTORS) {
            Debug.println(Debug.info, "we have > 35 protectors, switching to whatevers on the state stack");
            currentState = stateStack.pop();
        } else if (protectorsSpawnedInARow >= 10 && currentState == State.BUILDING_PROTECTORS) {
            Debug.println(Debug.info, "just built 10 protectors in a row, going to building a slanderer");
            canGoBackToBuildingProtectors = false;
            currentState = State.BUILDING_SLANDERERS;
        }
    }

    public void tryStartBuildingSpawnKill() throws GameActionException {
        Debug.println(Debug.info, "spawn kill lock: " + spawnKillLock);
        if (rc.getEmpowerFactor(rc.getTeam(),0) > Util.spawnKillThreshold && spawnKillLock >= 10) {
            Debug.println(Debug.info, "Switching to building spawn kills");
            stateStack.push(currentState);
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

        int id;
        MapLocation tempMapLoc;
        int neededInf;
        int currReqInf;
        for(int j = idSet.size - 1; j >= 0; j--) {
            id = ids[j];
            if(rc.canGetFlag(id)) {
                int flag = rc.getFlag(id);
                Comms.InformationCategory flagIC = Comms.getIC(flag);
                int[] currDxDy;
                RushFlag rushFlag;
                switch (flagIC) {
                    case NEUTRAL_EC:
                        // Debug.println(Debug.info, "Current Inluence: " + rc.getInfluence() + ", Tower inf: " + neededInf);
                        currDxDy = Comms.getDxDy(flag);
                        neededInf =  (int) Math.exp(Comms.getInf(flag) * Math.log(Comms.INF_LOG_BASE));
                        currReqInf = (int)  neededInf * 4 + 10;
                        if(currRoundNum <=150) {
                            currReqInf = (int) neededInf * 2 + 10;
                        }
                        rushFlag = new RushFlag(currReqInf, currDxDy[0], currDxDy[1], flag, Team.NEUTRAL);
                        ECflags.remove(rushFlag);
                        tempMapLoc = new MapLocation(rc.getLocation().x + currDxDy[0] - Util.dOffset, rc.getLocation().y + currDxDy[1] - Util.dOffset);
                        ECflags.add(rushFlag);
                        break;
                    case ENEMY_EC:
                        // Debug.println(Debug.info, "Current Inluence: " + rc.getInfluence() + ", Tower inf: " + neededInf);
                        currDxDy = Comms.getDxDy(flag);
                        neededInf =  (int) Math.exp(Comms.getInf(flag) * Math.log(Comms.INF_LOG_BASE));
                        currReqInf = (int)  neededInf * 4 + 10;
                        if(currRoundNum <=150) {
                            currReqInf = (int) neededInf * 2 + 10;
                        }
                        rushFlag = new RushFlag(currReqInf, currDxDy[0], currDxDy[1], flag, rc.getTeam().opponent());
                        ECflags.remove(rushFlag);
                        tempMapLoc = new MapLocation(rc.getLocation().x + currDxDy[0] - Util.dOffset, rc.getLocation().y + currDxDy[1] - Util.dOffset);
                        if (!enemyECsFound.contains(tempMapLoc)) {
                            enemyECsFound.add(tempMapLoc);
                        }
                        ECflags.add(rushFlag);
                        cleanUpCount = -1;
                        if (currentState == State.CLEANUP) {
                            currentState = stateStack.pop();
                        }
                        break;
                    case FRIENDLY_EC:
                        currDxDy = Comms.getDxDy(flag);
                        rushFlag = new RushFlag(0, currDxDy[0], currDxDy[1], 0, rc.getTeam());
                        tempMapLoc = new MapLocation(rc.getLocation().x + currDxDy[0] - Util.dOffset, rc.getLocation().y + currDxDy[1] - Util.dOffset);
                        if (enemyECsFound.contains(tempMapLoc)) enemyECsFound.remove(tempMapLoc);
                        break;
                    case ENEMY_FOUND:
                        haveSeenEnemy = true;
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
                        break;
                }
            } else {
                idSet.remove(id);
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

        cleanUpCount++;
    }

    public boolean tryStartCleanup() throws GameActionException {
        Debug.println(Debug.info, "Cleanup count: " + cleanUpCount);
        if (enemyECsFound.size == 0 && cleanUpCount > Util.startCleanupThreshold) {
            return true;
        }
        return false;
    }

    public boolean tryStartSavingForRush() throws GameActionException {
        if (!ECflags.isEmpty() && turnCount > lastRush + Util.minTimeBetweenRushes) {
            int flag = ECflags.peek().flag;
            int neededInf =  (int) Math.exp(Comms.getInf(flag) * Math.log(Comms.INF_LOG_BASE));
            int currReqInf = (int)  neededInf * 4 + 10;
            if(currRoundNum <=150) {
                currReqInf = (int) neededInf * 2 + 10;
            }
            if (neededInf <= Util.maxECRushConviction || rc.getInfluence() >= (currReqInf * 3 / 4)) {
                Debug.println(Debug.info, "tryStartSavingForRush is returning true");
                return true;
            }
        }
        return false;
    }

    public void tryStartRemovingBlockage() throws GameActionException {
        if (currentState == State.REMOVING_BLOCKAGE) return;
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
        if (num_enemies_near >= 6 && (lastSuccessfulBlockageRemoval == -1 || 
        (lastSuccessfulBlockageRemoval >= 0 && currRoundNum - lastSuccessfulBlockageRemoval > 10))) {
            stateStack.push(currentState);
            currentState = State.REMOVING_BLOCKAGE;
        }
    }

    public boolean trySendARush() throws GameActionException {
        Debug.println(Debug.info, "building rush bots");

        toBuild = RobotType.POLITICIAN;
        RushFlag rushFlag = ECflags.peek();
        if (rushFlag == null) {
            currentState = stateStack.pop();
            return false;
        }
        influence = rushFlag.requiredInfluence;

        MapLocation enemyLocation = home.translate(rushFlag.dx - Util.dOffset, rushFlag.dy - Util.dOffset);
        Debug.setIndicatorLine(Debug.info, home, enemyLocation, 255, 150, 50);

        if (influence >= currInfluence) {
            currentState = stateStack.pop();
            return false;
        }
        
        if(buildRobot(toBuild, influence)) {
            ECflags.remove();
            nextFlag = rushFlag.flag;
            currentState = stateStack.pop();
            lastRush = turnCount;
            if (currentState != State.BUILDING_SLANDERERS) {
                canGoBackToBuildingProtectors = true;
            }
            return true;
        }
        return false;   
    }

    void signalRobotType(Comms.SubRobotType type) throws GameActionException {
        nextFlag = Comms.getFlag(Comms.InformationCategory.TARGET_ROBOT, type);
    }
    
    //rush if we can
    public boolean readyToRush() {
        RushFlag targetEC = ECflags.peek();
        if(targetEC != null) {
            int requiredInfluence = targetEC.requiredInfluence;
            MapLocation enemyLocation = home.translate(targetEC.dx - Util.dOffset, 
                targetEC.dy - Util.dOffset);
            Debug.setIndicatorLine(Debug.info, home, enemyLocation, 100, 255, 100);
            if(requiredInfluence < currInfluence) return true;
        }
        return false;
    }

    /* public void makeBid() throws GameActionException {
        if(!overBidThreshold) {
            if (rc.getTeamVotes() > 750) {
                overBidThreshold = true;
                Debug.println(Debug.info, "Not bidding. Already won, suckers!");
            } else {
                int biddingInfluence;
                if (currRoundNum < 200) {
                    biddingInfluence = Math.max(currInfluence / 100, 2);
                } else {
                    switch (currentState) {
                        case CLEANUP:
                        case BUILDING_SLANDERERS:
                        case BUILDING_PROTECTORS:
                            Debug.println(Debug.info, "Bidding high.");
                            biddingInfluence = currInfluence / 10;
                            break;
                        case SAVING_FOR_RUSH:
                        case BUILDING_SPAWNKILLS:
                        case RUSHING:
                            Debug.println(Debug.info, "Bidding low.");
                            biddingInfluence = currInfluence / 50;
                            break;
                        default:
                            Debug.println(Debug.info, "Bidding medium.");
                            biddingInfluence = currInfluence / 20;
                            break;
                    }
                    if (rc.canBid(biddingInfluence)) {
                        rc.bid(biddingInfluence);
                    }
                }
                Debug.println(Debug.info, "Amount bid: " + biddingInfluence);
            }
        }
    } */
    public int bidBS() throws GameActionException {
        int currVotes = rc.getTeamVotes();
        int res;
        if (currVotes > 750) {
            return 0;
        } else if (currVotes > lastVoteCount) {
            wonLastBid = true;
            bigBid = prevBid;
            Debug.println(Debug.info, "Won last bid.");
            lastVoteCount++;
        } else {
            wonLastBid = false;
            littleBid = prevBid;
            Debug.println(Debug.info, "Lost last bid.");
        }
        Debug.println(Debug.info, "L: " + littleBid + ", P: " + prevBid + ", B: " + bigBid);
        if (wonLastBid) {
            res = Integer.max((prevBid + littleBid) / 2, 2);
            bigBid = prevBid;
            prevBid = res;
        } else {
            if (bigBid < currInfluence / 10) bigBid *= 2;
            res = Integer.min((prevBid + bigBid) / 2, currInfluence / 25);
            littleBid = prevBid;
            prevBid = res;
        }
        return res;
    }
}