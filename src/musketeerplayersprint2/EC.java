package musketeerplayersprint2;
import battlecode.common.*;

import musketeerplayersprint2.Comms.*;
import musketeerplayersprint2.Util.*;
import musketeerplayersprint2.Debug.*;
import musketeerplayersprint2.fast.FastIterableIntSet;
import musketeerplayersprint2.fast.FastIterableLocSet;
import musketeerplayersprint2.fast.FastLocIntMap;

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
        BUILDING_SPAWNKILLS,
        RUSHING,
        SAVING_FOR_RUSH,
        CLEANUP,
        REMOVING_BLOCKAGE,
        CHILLING,
        ACCELERATED_SLANDERERS,
        INIT,
        SURVIVAL,
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
            double infDiff = Math.abs(requiredInfluence - other.requiredInfluence);
            if (infDiff < 50 || infDiff / Math.min(requiredInfluence, other.requiredInfluence) < 0.1) {
                return Integer.compare(dx*dx + dy*dy, other.dx*other.dx + other.dy*other.dy);
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

        public String toString() {
            return "Inf: " + requiredInfluence + ", dx: " + dx + ", dy: " + dy;
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
    static State prevState;

    static int chillingCount;
    static boolean savingForSlanderer;
    static boolean readyForSlanderer;

    static boolean goToAcceleratedSlanderersState;
    static int builtInAcceleratedCount;

    static int lastRush;
    static int spawnKillLock;
    static int lastSuccessfulBlockageRemoval;

    static int bigBid;
    static int prevBid;
    static int littleBid;
    static boolean wonLastBid;
    static int lastVoteCount;

    static FastIterableLocSet enemyECsFound;
    static FastLocIntMap rushingECtoTurnMap;

    static int savingForRushSemaphore;

    static MapLocation recentSlanderer;

    public EC(RobotController r) {
        super(r);
        idSet = new FastIterableIntSet(1000);
        ids = idSet.ints;

        protectorIdSet = new FastIterableIntSet(100);
        protectorIds = protectorIdSet.ints;

        ECflags = new PriorityQueue<RushFlag>();
        stateStack = new ArrayDeque<State>();
        currentState = State.INIT;
        prevState = State.INIT;

        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.EC);

        cleanUpCount = 0;
        numProtectors = 0;
        lastRush = Integer.MIN_VALUE;
        canGoBackToBuildingProtectors = true;
        spawnKillLock = 10;
        lastSuccessfulBlockageRemoval = -1;
        littleBid = 0;
        prevBid = 0;
        bigBid = 1;
        wonLastBid = false;
        lastVoteCount = 0;
        chillingCount = 0;
        enemyECsFound = new FastIterableLocSet(20);
        savingForSlanderer = false;
        readyForSlanderer = false;
        goToAcceleratedSlanderersState = true;
        builtInAcceleratedCount = 0;
        noAdjacentEC = true;
        rushingECtoTurnMap = new FastLocIntMap();
        savingForRushSemaphore = 100;
        recentSlanderer = null;

        /*if (rc.getRoundNum() <= 1) {
            int encodedInfForUnknownEC = Comms.encodeInf(200);
            int flagForUnknownEC = Comms.getFlag(Comms.InformationCategory.ENEMY_EC, encodedInfForUnknownEC, Util.dOffset, Util.dOffset);
            RushFlag rfForUnknownEC = new RushFlag(810, 0, 0, flagForUnknownEC, rc.getTeam().opponent());
            ECflags.add(rfForUnknownEC);
            Debug.println(Debug.info, "Added thingy to ECflags.");
        }*/

        for (RobotInfo robot : rc.senseNearbyRobots(sensorRadius, enemy)) {
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                noAdjacentEC = false;
                RushFlag rushFlag;
                MapLocation EnemyECLoc = robot.getLocation();
                int neededInf =  robot.getInfluence();
                int currReqInf = (int)  neededInf * 4 + 10;
                if(currRoundNum <=150) {
                    currReqInf = (int) neededInf * 2 + 10;
                }
                int actualDX = rc.getLocation().x - EnemyECLoc.x;
                int actualDY = rc.getLocation().y - EnemyECLoc.y;
                int encodedInf = Comms.encodeInf(robot.getInfluence());
                int flag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC, encodedInf, actualDX + Util.dOffset, actualDY + Util.dOffset);
                Debug.println(Debug.info, "ADJACENT INFO:: neededInf: " + neededInf + "; actualDX: " + actualDX + "; actualDY: " + actualDY);
                rushFlag = new RushFlag(currReqInf, actualDX, actualDY, flag, rc.getTeam().opponent());
                ECflags.remove(rushFlag);
                if (!enemyECsFound.contains(EnemyECLoc)) {
                    enemyECsFound.add(EnemyECLoc);
                }
                ECflags.add(rushFlag);
            }
        }
    }

    public boolean buildRobot(RobotType toBuild, int influence) throws GameActionException {
        Debug.println(Debug.info, "building robot type: " + toBuild + " influence: " + influence);
        DirectionPreference pref = DirectionPreference.RANDOM;
        if(currentState == State.BUILDING_SPAWNKILLS) {
            pref = DirectionPreference.ORTHOGONAL;
        }
        
        Direction[] orderedDirs = Util.getOrderedDirections(pref);

        for(Direction dir : orderedDirs) {
            boolean isScout = Comms.getIC(nextFlag) == Comms.InformationCategory.TARGET_ROBOT &&
                            Comms.getSubRobotType(nextFlag) == Comms.SubRobotType.MUC_SCOUT;
            boolean scoutCheck = true;
            Direction scoutDirection = Comms.getScoutDirection(nextFlag);
            if(isScout && dir != scoutDirection && rc.onTheMap(rc.getLocation().add(scoutDirection))) { //Try and spawn in the direction that the scout wants to go
                scoutCheck = false;
            } 

            if (rc.canBuildRobot(toBuild, dir, influence) && scoutCheck) {
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
        // if (currentState == State.SAVING_FOR_RUSH) {
        //     Debug.println(Debug.info, "Saving sema: " + savingForRushSemaphore);
        //     if (savingForRushSemaphore == 0) {
        //         currentState = stateStack.pop();
        //     } else {
        //         savingForRushSemaphore--;
        //     }
        // } else if (currentState == State.RUSHING) {
        //     savingForRushSemaphore = 100;
        // } else {
        //     savingForRushSemaphore++;
        // }

        if (currRoundNum > 1250 && rc.getTeamVotes() > 750) {
            currentState = State.SURVIVAL;
        }

        goToAcceleratedSlanderersState = true;
        processChildrenFlags(); //goToAcceleratedSlanderer gets set to false if there is an enemy within 2 sensor radiuses of the base
        if(enemySensable.length > 0 || currInfluence > Util.buildSlandererThreshold) { //also set to false if we sense an enemy in our base or we have enough influence
            goToAcceleratedSlanderersState = false;
        }

        if (currentState != State.SURVIVAL) {
            if(currentState != State.INIT) {
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
                            currentState = State.RUSHING;
                        }
                    } else {
                        // Second priority is removing blockage
                        tryStartRemovingBlockage();
                        // Third priority is building protectors.
                        if (currentState != State.REMOVING_BLOCKAGE) {
                            // Fourth priority is saving for rush
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
                            else if (currentState == State.CHILLING && goToAcceleratedSlanderersState) { //If nothing around, make more slanderers (after you have a defense from the first few rounds)
                                currentState = State.ACCELERATED_SLANDERERS;
                            }             
                            else if (currentState == State.ACCELERATED_SLANDERERS && !goToAcceleratedSlanderersState) {
                                currentState = State.CHILLING;
                                builtInAcceleratedCount = 0;
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
        if (rc.canBid(biddingInfluence) && currentState != State.INIT) {
            rc.bid(biddingInfluence);
        }
        Debug.println(Debug.info, "Amount bid: " + biddingInfluence);

        //updating currInfluence after a bid
        currInfluence = rc.getInfluence();
        muckrackerNear = checkIfMuckrakerNear();
        if(currentState == State.INIT) { //Specific checks in the INIT state
            Debug.println(Debug.info, "Inside INIT checker");
            if(robotCounter >= 30 || muckrackerNear || rc.getInfluence() > Util.buildSlandererThreshold) { //Dont initialize if a muckraker is near or we have a lot of money from buff
                    Debug.println(Debug.info, "set state from INIT to CHILLING");
                    currentState = State.CHILLING;
            }
            if(!noAdjacentEC) {
                Debug.println(Debug.info, "Adjacent EC found");
                if (tryStartSavingForRush()) {
                        stateStack.push(State.CHILLING);
                        currentState = State.SAVING_FOR_RUSH;
                    }
                }
            else if(almostReadyToRush()) {
                stateStack.push(State.CHILLING);
                currentState = State.SAVING_FOR_RUSH;
            }
        }
        

        Debug.println(Debug.info, "I am a " + rc.getType() + "; current influence: " + currInfluence);
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        Debug.println(Debug.info, "num of ec's found: " + ECflags.size());
        Debug.println(Debug.info, "num ids found: " + idSet.size);
        Debug.println(Debug.info, "num protectors currently: " + protectorIdSet.size);
        Debug.println(Debug.info, "State stack size: " + stateStack.size() + ", state: " + currentState);
        Debug.println(Debug.info, "Muckraker near: " + muckrackerNear);

        switch(currentState) {
            case INIT: 
                firstRounds();
                buildRobot(toBuild, influence);
                break;
            case SURVIVAL:
                toBuild = RobotType.MUCKRAKER;
                signalRobotType(SubRobotType.MUC_SURVIVAL);
                influence = Integer.max(1, currInfluence / 500);
                buildRobot(toBuild, influence);
                break;
            case CHILLING: 
                if(savingForSlanderer && Util.getBestSlandererInfluence(currInfluence) > 100) {
                    readyForSlanderer = true;
                }

                if(muckrackerNear || currInfluence > Util.buildSlandererThreshold) { //possibly add income threshold as well
                    readyForSlanderer = false;
                    savingForSlanderer = false;
                }

                if(readyForSlanderer) {
                    toBuild = RobotType.SLANDERER;
                    influence = Util.getBestSlandererInfluence(currInfluence);
                    if(buildRobot(toBuild, influence)) {
                        Debug.println(Debug.info, "building a slanderer");
                        readyForSlanderer = false;
                        savingForSlanderer = false;
                        chillingCount = 0;
                    }

                    if(recentSlanderer != null) {
                        int dx = recentSlanderer.x - home.x;
                        int dy = recentSlanderer.y - home.y;
                        nextFlag = Comms.getFlagRush(Comms.InformationCategory.ENEMY_FOUND, (int)(2 * Math.random()), Comms.EnemyType.SLA, 
                                                dx + Util.dOffset, dy + Util.dOffset);
                    }
                }
                else if (savingForSlanderer) {
                    switch(chillingCount % 3) {
                        case 0: case 2: 
                            toBuild = RobotType.MUCKRAKER;
                            influence = Integer.max(1, currInfluence / 500);
                            makeMuckraker();
                            break;
                        case 1: 
                            toBuild = RobotType.POLITICIAN;
                            influence = Math.max(15, currInfluence / 50);
                            signalRobotType(SubRobotType.POL_PROTECTOR);
                            break;
                    }
                    if(buildRobot(toBuild, influence)) {
                        Debug.println(Debug.info, "saving for slanderers case");
                        chillingCount ++;
                    }
                }
                else {
                    switch(chillingCount % 4) {
                        case 0: case 1:
                            toBuild = RobotType.POLITICIAN;
                            influence = Math.max(15, currInfluence / 50);
                            signalRobotType(SubRobotType.POL_PROTECTOR);
                            if(buildRobot(toBuild, influence)) {
                                Debug.println(Debug.info, "case 1 of the else case of CHILLING");
                                chillingCount ++;
                            }
                            break;
                        case 2: 
                            toBuild = RobotType.MUCKRAKER;
                            influence = Integer.max(1, currInfluence / 500);
                            if(buildRobot(toBuild, influence)) {
                                Debug.println(Debug.info, "case 2 of the else case of CHILLING");
                                chillingCount ++;
                            }
                            break;
                        case 3: 
                            int currBestSlandererInfluence = Util.getBestSlandererInfluence(currInfluence);
                            toBuild = RobotType.MUCKRAKER;
                            influence = Integer.max(1, currInfluence / 500);
                            makeMuckraker();
                            if(buildRobot(toBuild, influence)) {
                                Debug.println(Debug.info, "case 3 of the else case of CHILLING");
                                chillingCount = 0;
                                if(currBestSlandererInfluence > 100) {
                                    readyForSlanderer = true;
                                }
                                else {
                                    savingForSlanderer = true;
                                }
                            }
                            break;
                    }
                }  
                break;
            case ACCELERATED_SLANDERERS:
                switch(builtInAcceleratedCount % 3) {
                    case 0: case 1:
                        toBuild = RobotType.POLITICIAN;
                        influence = Math.max(15, currInfluence / 50);
                        signalRobotType(SubRobotType.POL_PROTECTOR);
                        break;
                    case 2:
                        if(Util.getBestSlandererInfluence(currInfluence ) > 100) {
                            toBuild = RobotType.SLANDERER;
                            influence = Util.getBestSlandererInfluence(currInfluence);
                    
                            if(recentSlanderer != null) {
                                int dx = recentSlanderer.x - home.x;
                                int dy = recentSlanderer.y - home.y;
                                nextFlag = Comms.getFlagRush(Comms.InformationCategory.ENEMY_FOUND, (int)(2 * Math.random()), Comms.EnemyType.SLA, 
                                                        dx + Util.dOffset, dy + Util.dOffset);
                            }
                        }
                        else {
                            toBuild = RobotType.MUCKRAKER;
                            influence = Integer.max(1, currInfluence / 500);
                            makeMuckraker();
                        }
                        break;
                    default:
                        break;
                }
                if(buildRobot(toBuild, influence)) {
                    builtInAcceleratedCount ++;
                }
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
                        nextFlag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC_MUK, currDxDy[0] + Util.dOffset, currDxDy[1] + Util.dOffset);
                        Debug.println(Debug.info, "Making hunter mucker with destination " + currDxDy[0] + ", " + currDxDy[1] + ".");
                    }
                    else {
                        nextFlag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC_MUK);
                        Debug.println(Debug.info, "Making hunter mucker with no desitation");
                    }
                }
                
                toBuild = RobotType.MUCKRAKER;
                influence = Integer.max(1, currInfluence / 500);
                
                // if(robotCounter % 2 == 0 || prevState != currentState) {
                //     toBuild = RobotType.MUCKRAKER;
                //     influence = Integer.max(1, currInfluence / 500);
                // } else {
                //     toBuild = RobotType.POLITICIAN;
                //     influence = Math.max(15, currInfluence / 50);
                //     signalRobotType(SubRobotType.POL_PROTECTOR);
                // }

                // // Signal to troops to lead the rush
                // if(prevState != currentState) {
                //     if(targetEC.team == enemy) {
                //         nextFlag = Comms.getFlagRush(InformationCategory.ENEMY_EC, (int)(4 * Math.random()), Comms.GroupRushType.MUC, 
                //                                     targetEC.dx + Util.dOffset, targetEC.dy + Util.dOffset);
                //     } else { 
                //         nextFlag = Comms.getFlagRush(InformationCategory.NEUTRAL_EC, (int)(4 * Math.random()), Comms.GroupRushType.MUC, 
                //                                     targetEC.dx + Util.dOffset, targetEC.dy + Util.dOffset);
                //     }
                // }

                buildRobot(toBuild, influence);
                break;
            case CLEANUP:
                if(Util.getBestSlandererInfluence(currInfluence) >= 100 && robotCounter % 3 == 1 && !muckrackerNear && currInfluence < Util.buildSlandererThreshold) {
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
                    influence = Integer.max(1, currInfluence / 500);
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

        prevState = currentState;

        Debug.println(Debug.info, "next flag that will be set: " + nextFlag);
    }

    public void initializeGlobals() throws GameActionException {
        super.initializeGlobals();

        toBuild = null;
        influence = 0;
        currRoundNum = rc.getRoundNum();
        currInfluence = rc.getInfluence();

        // Reset slanderer every 3 rounds
        if(rc.getRoundNum() % 3 == 0) {
            recentSlanderer = null;
        }

        MapLocation[] keys = rushingECtoTurnMap.getKeys();
        MapLocation key;
        for(int i = keys.length - 1; i >= 0; i--) {
            key = keys[i];
            if(rc.getRoundNum() > rushingECtoTurnMap.getVal(key) + Util.rushCooldown) {
                rushingECtoTurnMap.remove(key);
            }
        }
    }

    /*public void toggleBuildProtectors() throws GameActionException {
        // Debug.println(Debug.info, "can go back to building protectors: " + canGoBackToBuildingProtectors);
        // Debug.println(Debug.info, "protector id set size: " + protectorIdSet.size);
        // Debug.println(Debug.info, "current state from toggle building protectors: " + currentState);
        // Debug.println (Debug.info, "robot counter from toggle protectors: " + robotCounter);

        if (protectorIdSet.size <= 25 && currentState != State.BUILDING_PROTECTORS && 
            robotCounter > 10 && canGoBackToBuildingProtectors && noAdjacentEC) {
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
    }*/
    public int PoliticianInfluence() throws GameActionException{
        return Math.max(15, currInfluence / 50);
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
        for(RobotInfo robot: rc.senseNearbyRobots(sensorRadius, enemy)) {
            if(robot.getType() == RobotType.MUCKRAKER) {
                return true;
            }
        }
        return false;
    }

    public void processChildrenFlags() throws GameActionException {
        idSet.updateIterable();
        protectorIdSet.updateIterable();

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
                        neededInf = Comms.getInf(flag);
                        currReqInf = (int) neededInf * 2 + 10;
                        // if(currRoundNum <= 150) {
                        //     currReqInf = (int) neededInf * 2 + 10;
                        // }
                        rushFlag = new RushFlag(currReqInf, currDxDy[0] - Util.dOffset, currDxDy[1] - Util.dOffset, flag, Team.NEUTRAL);
                        tempMapLoc = new MapLocation(rc.getLocation().x + rushFlag.dx, rc.getLocation().y + rushFlag.dy);

                        // Only insert if we aren't rushing this EC
                        if(!rushingECtoTurnMap.contains(tempMapLoc)) {
                            ECflags.remove(rushFlag);
                            ECflags.add(rushFlag);
                        }
                        break;
                    case ENEMY_EC:
                        // Debug.println(Debug.info, "Current Inluence: " + rc.getInfluence() + ", Tower inf: " + neededInf);
                        currDxDy = Comms.getDxDy(flag);
                        neededInf =  Comms.getInf(flag);
                        currReqInf = (int) neededInf * 4 + 10;
                        if(currRoundNum <=150) {
                            currReqInf = (int) neededInf * 2 + 10;
                        }
                        rushFlag = new RushFlag(currReqInf, currDxDy[0] - Util.dOffset, currDxDy[1] - Util.dOffset, flag, rc.getTeam().opponent());
                        tempMapLoc = new MapLocation(rc.getLocation().x + rushFlag.dx, rc.getLocation().y + rushFlag.dy);
                        
                        // Only insert if we aren't rushing this EC
                        if(!rushingECtoTurnMap.contains(tempMapLoc)) {
                            ECflags.remove(rushFlag);
                            ECflags.add(rushFlag);
                        }

                        if (!enemyECsFound.contains(tempMapLoc)) {
                            enemyECsFound.add(tempMapLoc);
                        }
                        cleanUpCount = -1;
                        if (currentState == State.CLEANUP) {
                            currentState = stateStack.pop();
                        }
                        break;
                    case FRIENDLY_EC:
                        currDxDy = Comms.getDxDy(flag);
                        rushFlag = new RushFlag(0, currDxDy[0] - Util.dOffset, currDxDy[1] - Util.dOffset, 0, rc.getTeam());
                        tempMapLoc = new MapLocation(rc.getLocation().x + rushFlag.dx, rc.getLocation().y + rushFlag.dy);
                        ECflags.remove(rushFlag);
                        if (enemyECsFound.contains(tempMapLoc)) enemyECsFound.remove(tempMapLoc);
                        break;
                    case ENEMY_FOUND:
                        int[] enemyDxDy = Comms.getDxDy(flag);
                        int enemyLocX = enemyDxDy[0] + home.x - Util.dOffset;
                        int enemyLocY = enemyDxDy[1] + home.y - Util.dOffset;

                        MapLocation enemyLoc = new MapLocation(enemyLocX, enemyLocY);
                        if (rc.getLocation().isWithinDistanceSquared(enemyLoc, rc.getType().sensorRadiusSquared * 4)) {
                            goToAcceleratedSlanderersState = false;
                        }

                        Comms.EnemyType enemyType = Comms.getEnemyType(flag);
                        if(enemyType == Comms.EnemyType.SLA) {
                            recentSlanderer = enemyLoc;
                        }
                        break;
                }
            } else {
                idSet.remove(id);
                protectorIdSet.remove(id);
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
            RushFlag rushFlag = ECflags.peek();
            int flag = rushFlag.flag;
            int distanceSquared = rushFlag.dx * rushFlag.dx + rushFlag.dy * rushFlag.dy;
            int neededInf =  Comms.getInf(flag);
            /*if (rushFlag.dx == 0 && rushFlag.dy == 0 && currRoundNum <= 50) {
                return false;
            }*/
            int currReqInf = rushFlag.requiredInfluence;
            if (neededInf <= Util.maxECRushConviction || rc.getInfluence() >= (currReqInf * 3 / 4) || (distanceSquared < sensorRadius)) {
                // if (savingForRushSemaphore == 100) {
                    Debug.println(Debug.info, "tryStartSavingForRush is returning true");
                    return true;
                // }
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

        MapLocation enemyLocation = home.translate(rushFlag.dx, rushFlag.dy);
        Debug.setIndicatorLine(Debug.info, home, enemyLocation, 255, 150, 50);

        if (influence >= currInfluence) {
            currentState = stateStack.pop();
            return false;
        }
        
        if(buildRobot(toBuild, influence)) {
            ECflags.remove();
            // nextFlag = rushFlag.flag;
            if(rushFlag.team == enemy) {
                nextFlag = Comms.getFlagRush(InformationCategory.ENEMY_EC, (int)(4 * Math.random()), Comms.GroupRushType.MUC_POL, 
                                            rushFlag.dx + Util.dOffset, rushFlag.dy + Util.dOffset);
            } else { 
                nextFlag = Comms.getFlagRush(InformationCategory.NEUTRAL_EC, (int)(4 * Math.random()), Comms.GroupRushType.MUC_POL, 
                                            rushFlag.dx + Util.dOffset, rushFlag.dy + Util.dOffset);
            }
            currentState = stateStack.pop();
            lastRush = turnCount;
            rushingECtoTurnMap.add(enemyLocation, lastRush);
            /*if (currentState != State.BUILDING_SLANDERERS) {
                canGoBackToBuildingProtectors = true;
            }
            return true;*/
        }
        return false;   
    }

    public void makeMuckraker() throws GameActionException {
        RushFlag targetEC = ECflags.peek();
        if (numMucks % 2 == 0) {           
            if (targetEC != null && targetEC.team != Team.NEUTRAL) {
                int[] currDxDy = {targetEC.dx, targetEC.dy};
                nextFlag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC_MUK, targetEC.dx + Util.dOffset, targetEC.dy + Util.dOffset);
                Debug.println(Debug.info, "Making hunter mucker with destination " + targetEC.dx + ", " + targetEC.dy + ".");
            } else {
                nextFlag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC_MUK);
                Debug.println(Debug.info, "Making hunter mucker with no destination.");
            }
        }
        return;
    }

    void signalRobotType(Comms.SubRobotType type) throws GameActionException {
        nextFlag = Comms.getFlag(Comms.InformationCategory.TARGET_ROBOT, type);
    }

    void signalScoutMuckraker(Comms.SubRobotType type, Direction dir) throws GameActionException {
        nextFlag = Comms.getFlagScout(Comms.InformationCategory.TARGET_ROBOT, type, dir);
    }
    
    //rush if we can
    public boolean readyToRush() {
        RushFlag targetEC = ECflags.peek();
        if(targetEC != null) {
            int requiredInfluence = targetEC.requiredInfluence;
            MapLocation enemyLocation = home.translate(targetEC.dx, targetEC.dy);
            Debug.setIndicatorLine(Debug.info, home, enemyLocation, 100, 255, 100);
            if(requiredInfluence < currInfluence) return true;
        }
        return false;
    }

    public boolean almostReadyToRush() {
        RushFlag targetEC = ECflags.peek();
        if(targetEC != null) {
            int requiredInfluence = targetEC.requiredInfluence;
            /*if (targetEC.dx == 0 && targetEC.dy == 0 && currRoundNum <= 50) {
                return false;
            }*/
            MapLocation enemyLocation = home.translate(targetEC.dx, targetEC.dy);
            Debug.setIndicatorLine(Debug.info, home, enemyLocation, 100, 255, 100);
            if(3 * requiredInfluence / 4 < currInfluence) return true;
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
        } else if (currRoundNum > 1300) {
            return currInfluence / 25;
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
        Debug.println(Debug.info, "L: " + littleBid + ", B: " + bigBid);
        if (wonLastBid) {
            res = Integer.min(Integer.max((prevBid + littleBid) / 2, 2), currInfluence / 25);
            bigBid = prevBid;
            prevBid = res;
        } else {
            if (bigBid < currInfluence / 10) bigBid *= 2;
            res = Integer.min(Integer.max((prevBid + bigBid) / 2, 2), currInfluence / 25);
            littleBid = prevBid;
            prevBid = res;
        }
        if (bigBid == 0) bigBid++;
        return res;
    }
    
    public void firstRounds() throws GameActionException {
        switch(robotCounter) {
            case 0: case 12: case 15: case 18: case 21: case 24: case 27: 
                toBuild = RobotType.SLANDERER;
                if(robotCounter == 0) {
                    influence = 130;
                }
                else {
                    influence = Util.getBestSlandererInfluence(currInfluence);
                    if(influence == -1) {
                        Debug.println(Debug.info, "Slanderer cost too low");
                    }
                }
                break;
            case 3: case 4: case 5: case 6: case 7: case 8: case 9: case 10: case 11:
                toBuild = RobotType.MUCKRAKER;
                influence = Integer.max(1, currInfluence / 500);
                if(robotCounter <= 8) {
                    signalScoutMuckraker(Comms.SubRobotType.MUC_SCOUT, Util.directions[robotCounter-1]);
                }
                break;
            case 1: case 2: case 13: case 14: case 16: case 17: case 19: case 20: case 22: case 23: case 25: case 26: case 28: case 29:
                toBuild = RobotType.POLITICIAN;
                influence = 15;
                signalRobotType(SubRobotType.POL_PROTECTOR);
                break;
        }
    }
}