package musketeerplayerqual;
import battlecode.common.*;

import musketeerplayerqual.Comms.*;
import musketeerplayerqual.Util.*;
import musketeerplayerqual.Debug.*;
import musketeerplayerqual.fast.FastIterableIntSet;
import musketeerplayerqual.fast.FastIterableLocSet;
import musketeerplayerqual.fast.FastLocIntMap;
import musketeerplayerqual.fast.FastIntLocMap;
import musketeerplayerqual.fast.FastQueue;
import musketeerplayerqual.fast.FastIntIntMap;

import java.util.ArrayDeque;
import java.util.PriorityQueue;
/* 
TODO: 
slandies move away from scout mucks (reason why we lose as blue on circle, their scout finds us way earlier
cuz ours tries to go around a slanderer)

Fix accelerated slanderer mode so that we win on small maps but don't lose on the maps that we started winning because of the change(like snowflake)
My first guess would be try and get out of the mode sooner. I tried to get out of it if we see any enemy but that didn't work. Myabe if we see more than 1 muckraker
in our sensor radius? 

We also don't win decisively on gridlock as blue anymore. Not sure why. Might be worth it to investigate.




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
        STUCKY_MUCKY,
        RUSHING_MUCKS,
        ABOUT_TO_DIE,
        BUILDING_BUFF_POLS,
        BETTER_RUSHING,
    };

    static enum RushingState {
        MUCK,
        SUPPORT1,
        SUPPORT2,
        RUSH,
    }

    static class RushFlag implements Comparable<RushFlag> {
        int requiredInfluence;
        int dx;
        int dy;
        int flag;
        Team team;

        int muckInf;
        int supportInf;
        int rushInf;

        RushFlag(int r, int x, int y, int f, Team t) {
            requiredInfluence = r;
            dx = x;
            dy = y;
            flag = f;
            team = t;
        }

        RushFlag(int r, int x, int y, int f, Team t, int m, int s, int ru) {
            requiredInfluence = r;
            dx = x;
            dy = y;
            flag = f;
            team = t;
            muckInf = m;
            supportInf = s;
            rushInf = ru;
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
    static int maxUsableInfluence;
    static int cleanUpCount;
    static int currRoundNum;
    static int numMucks;
    static int numPols;
    static int currInfluence;
    static boolean noAdjacentEC;
    static boolean builtRobot;

    static boolean muckrakerNear;
    static int buffMuckCooldown; 
    static MapLocation lastSentBufMuck;
    
    static FastIterableIntSet idSet;
    static FastIterableIntSet protectorIdSet;
    static FastIterableIntSet buffPolSet;
    static int[] ids;
    static PriorityQueue<RushFlag> ECflags;
    static ArrayDeque<State> stateStack;
    static FastQueue<Integer> flagQueue;

    static int protectorsSpawnedInARow;
    static boolean canGoBackToBuildingProtectors;
    static boolean haveSeenEnemy;

    static State currentState;
    static State prevState;
    static RushingState rushingState;
    static RushFlag currRushFlag;

    static int chillingCount;
    static boolean savingForSlanderer;
    static boolean readyForSlanderer;

    static boolean firstScoutDeathReported;

    static boolean goToAcceleratedSlanderersState;
    static int builtInAcceleratedCount;
    static int builtInBuffPolsCount;
    static int buffPolLock;

    static int lastRush;
    static int spawnKillLock;
    static MapLocation spawnKillLoc;
    static int lastSuccessfulBlockageRemoval;

    static int bigBid;
    static int prevBid;
    static int littleBid;
    static boolean wonLastBid;
    static int lastVoteCount;
    static boolean biddingOneLess;
    static boolean bidEquilibrium;

    static int initialMucksDirection;
    static Direction closestWall;
    static int[] wallLocations = {0,0,0,0};
    static int flagQueueCooldown;

    static FastIterableLocSet enemyECsFound;
    static FastIterableLocSet nearbyECs;
    static FastLocIntMap rushingECtoTurnMap;
    static FastIntLocMap idToFriendlyECLocMap;
    static FastIntIntMap roundToSlandererID;
    static FastIntIntMap slandererIDToRound;
    static MapLocation nextBufLoc;
    static FastIntLocMap scoutIDToEnemyLocs;

    static MapLocation recentSlanderer;

    static int enemyRushPolInf;
    static int enemyBuffMuckInf;
    static int dyingSemaphore;
    static int dyingSemaphoreDefault;
    static double passabilityOfHome;
    static MapLocation enemyRushPolLoc;

    public EC(RobotController r) throws GameActionException {
        super(r);
        idSet = new FastIterableIntSet(1000);
        ids = idSet.ints;

        protectorIdSet = new FastIterableIntSet(1000);
        buffPolSet = new FastIterableIntSet(100);
        roundToSlandererID = new FastIntIntMap(1000);
        slandererIDToRound = new FastIntIntMap(1000);

        ECflags = new PriorityQueue<RushFlag>();
        stateStack = new ArrayDeque<State>();
        flagQueue = new FastQueue<>(100);
        currentState = getInitialState();
        prevState = getInitialState();
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.EC);

        cleanUpCount = 0;
        lastRush = Integer.MIN_VALUE;
        canGoBackToBuildingProtectors = true;
        spawnKillLock = 10;
        spawnKillLoc = null;
        lastSuccessfulBlockageRemoval = -1;
        littleBid = 0;
        prevBid = 0;
        bigBid = 2;
        wonLastBid = false;
        lastVoteCount = 0;
        biddingOneLess = false;
        bidEquilibrium = false;
        chillingCount = 0;
        numMucks = 0;
        enemyECsFound = new FastIterableLocSet(20);
        savingForSlanderer = false;
        readyForSlanderer = false;
        goToAcceleratedSlanderersState = true;
        builtInAcceleratedCount = 0;
        builtInBuffPolsCount = 0;
        buffPolLock = 10;

        noAdjacentEC = true;
        rushingECtoTurnMap = new FastLocIntMap();
        recentSlanderer = null;
        idToFriendlyECLocMap = new FastIntLocMap();
        firstScoutDeathReported = false;
        buffMuckCooldown = 0;
        lastSentBufMuck = null;

        friendlyECs.remove(rc.getLocation());
        initialMucksDirection = 0;
        closestWall = null;
        flagQueueCooldown = 0;
        nearbyECs = new FastIterableLocSet(12);
        nextBufLoc = null;
        scoutIDToEnemyLocs = new FastIntLocMap();

        enemyRushPolInf = 0;
        enemyBuffMuckInf = 0;
        passabilityOfHome = rc.sensePassability(home);
        dyingSemaphoreDefault = (int) (4.0 * (2.0 / passabilityOfHome));
        dyingSemaphore = dyingSemaphoreDefault;
        enemyRushPolLoc = null;

        /*if (rc.getRoundNum() <= 1) {
            int encodedInfForUnknownEC = Comms.encodeInf(200);
            int flagForUnknownEC = Comms.getFlag(Comms.InformationCategory.ENEMY_EC, encodedInfForUnknownEC, Util.dOffset, Util.dOffset);
            RushFlag rfForUnknownEC = new RushFlag(810, 0, 0, flagForUnknownEC, rc.getTeam().opponent());
            ECflags.add(rfForUnknownEC);
            Debug.println(Debug.info, "Added thingy to ECflags.");
        }*/

        for (RobotInfo robot : rc.senseNearbyRobots(sensorRadius)) {
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                nearbyECs.add(robot.getLocation());
            }
        }
        nearbyECs.updateIterable();
    }

    public State getInitialState() {
        RobotInfo[] enemies = rc.senseNearbyRobots(sensorRadius, enemy);
        RobotInfo[] friendlies = rc.senseNearbyRobots(sensorRadius, rc.getTeam());
        if(enemies.length > friendlies.length && enemies.length >= 5) {
            return State.STUCKY_MUCKY;
        }
        else if (rc.getInfluence() < 100) {
            return State.CHILLING;
        }
        else {
            return State.INIT;
        }
    }

    public boolean buildRobot(RobotType toBuild, int influence) throws GameActionException {
        Debug.println(Debug.info, "Trying to build robot of type: " + toBuild + " influence: " + influence);
        Debug.println("Max usable influence: " + maxUsableInfluence);
        influence = Math.min(maxUsableInfluence, influence);
        DirectionPreference pref = DirectionPreference.RANDOM;
        if(currentState == State.BUILDING_SPAWNKILLS || spawnKillLock < 10) {
            pref = DirectionPreference.ORTHOGONAL;
        }
        
        Direction[] orderedDirs = Util.getOrderedDirections(pref);
        boolean isScout = Comms.getIC(nextFlag) == Comms.InformationCategory.TARGET_ROBOT &&
                        Comms.getSubRobotType(nextFlag) == Comms.SubRobotType.MUC_SCOUT;

        if(isScout) {
            Direction scoutDirection = Comms.getScoutDirection(nextFlag);
            orderedDirs = Util.getOrderedDirections(scoutDirection);            
        } else if(currentState == State.ABOUT_TO_DIE) {
            if(enemyRushPolLoc != null) {
                orderedDirs = Util.getAboutToDieBuildOrder(home.directionTo(enemyRushPolLoc));
            } else {
                orderedDirs = Util.getOrderedDirections(DirectionPreference.ORTHOGONAL);
            }
        }

        if (toBuild == RobotType.POLITICIAN && robotCounter == 1) {
            orderedDirs = Util.getOrderedDirections(Direction.NORTHWEST);
        }
        else if (toBuild == RobotType.POLITICIAN && robotCounter == 2) {
            orderedDirs = Util.getOrderedDirections(Direction.NORTHEAST);
        }

        for(Direction dir : orderedDirs) {
            if (rc.canBuildRobot(toBuild, dir, influence) 
                && (spawnKillLoc == null || !home.add(dir).isAdjacentTo(spawnKillLoc))) {
                rc.buildRobot(toBuild, dir, influence);
                RobotInfo robot = rc.senseRobotAtLocation(home.add(dir));
                if(robot != null) {
                    if (currentState == State.BUILDING_SPAWNKILLS) {
                        spawnKillLoc = robot.getLocation();
                    }
                    switch(robot.getType()) {
                        case MUCKRAKER:
                            numMucks++;
                            Debug.println(Debug.info, "Num Mucks being updated, new value: " + numMucks);
                            if(numMucks <= 12) {
                                scoutIDToEnemyLocs.add(robot.getID(), new MapLocation(0,0));
                                Debug.println("Scout added to Scout ID map");
                            }
                            break;
                        case SLANDERER:
                            roundToSlandererID.add(currRoundNum + Util.slandererLifetime, robot.getID());
                            slandererIDToRound.add(robot.getID(), currRoundNum + Util.slandererLifetime);
                            break;
                        case POLITICIAN:
                            numPols++;
                        default:
                            break;
                    }
                    Debug.println(Debug.info, "Built robot: " + robot.getID());
                    idSet.add(robot.getID());

                    Comms.InformationCategory IC = Comms.getIC(nextFlag);
                    if(IC == Comms.InformationCategory.TARGET_ROBOT) {
                        switch(Comms.getSubRobotType(nextFlag)) {
                            case POL_PROTECTOR:
                                protectorIdSet.add(robot.getID());
                                break;
                            case POL_BUFF:
                                buffPolSet.add(robot.getID());
                                break;
                        }
                    }
                } else {
                    System.out.println("CRITICAL: EC didn't find the robot it just built");
                }
                robotCounter += 1;
                builtRobot = true;
                return builtRobot;
            }
        }

        builtRobot = false;
        return builtRobot;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        if (!noAdjacentEC && currentState != State.SAVING_FOR_RUSH && currentState != State.RUSHING) {
            stateStack.push(State.CHILLING);
            currentState = State.SAVING_FOR_RUSH;
        }

        int[] nearStuff = checkIfMuckrakerNear();
        muckrakerNear = (nearStuff[0] == 1);
        enemyRushPolInf = nearStuff[1];

        processFriendlyECFlags();
        processLocalFlags();
        processChildrenFlags();
        
        if(firstScoutDeathReported && !ECflags.isEmpty()) {
            Debug.println("Updating nextBufLoc");
            RushFlag currentTarget = ECflags.peek();
            if(currentTarget.team == enemy) {
                MapLocation currLoc = rc.getLocation();
                nextBufLoc = new MapLocation(currLoc.x + currentTarget.dx, currLoc.y + currentTarget.dy);
            }
        }
        
        goToAcceleratedSlanderersState = true;
        //goToAcceleratedSlanderer gets set to false if there is an enemy within 2 sensor radiuses of the base
        if(enemySensable.length > 0 || currInfluence > Util.buildSlandererThreshold) { //also set to false if we sense an enemy in our base or we have enough influence
            goToAcceleratedSlanderersState = false;
        }

        if (enemyRushPolInf * rc.getEmpowerFactor(enemy, 0) <= currInfluence) {
            if(currentState == State.STUCKY_MUCKY) {
                currentState = getInitialState();
            }
            else if(currentState != State.INIT) {
                // Override everything for a spawn kill. This is fine, as it only takes 1 turn
                // and at most happens once every 10 turns.
                // tryStartBuildingSpawnKill();
                // tryStartBuildingBuffPols();

                if(currentState != State.BUILDING_BUFF_POLS) {
                    // If we have enough to rush a tower, make that the #1 priority
                    if (readyToRush()) {
                        // if (currentState != State.RUSHING) {
                        //     if (currentState != State.SAVING_FOR_RUSH) {
                        //         stateStack.push(currentState);
                        //     }
                        //     currentState = State.RUSHING;
                        // }
                        if (currentState != State.BETTER_RUSHING) {
                            if (currentState != State.SAVING_FOR_RUSH) {
                                stateStack.push(currentState);
                            }
                            currentState = State.BETTER_RUSHING;
                            rushingState = RushingState.SUPPORT1;
                            currRushFlag = ECflags.peek();
                        }
                    }
                    else if (readyToSendBufMuck()) {
                        if(currentState != State.RUSHING_MUCKS) {
                            stateStack.push(currentState);
                            currentState = State.RUSHING_MUCKS;
                        }
                    }
                    
                    else {
                        // Second priority is removing blockage
                        tryStartRemovingBlockage();
                        // Third priority is building protectors.
                        if (currentState != State.REMOVING_BLOCKAGE) {
                            // If there's nothing to do, clean up
                            if (currRoundNum > 500 && tryStartCleanup()) {
                                if (currentState != State.CLEANUP) {
                                    stateStack.push(currentState);
                                    currentState = State.CLEANUP;
                                }
                            }
                            else if (currentState == State.CHILLING && goToAcceleratedSlanderersState && currInfluence > 107) { //If nothing around, make more slanderers (after you have a defense from the first few rounds)
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
        } else if (currentState != State.ABOUT_TO_DIE) {
            stateStack.push(currentState);
            currentState = State.ABOUT_TO_DIE;
        }
        
        // At this point, state is either RUSHING, SAVING, BUILDING (spawn kill or protectors),or CLEANUP
        // At most, two things have been pushed to the state stack, the previous state, and whatever protectors overrode.

        //bidding code
        int biddingInfluence = bidBS();
        if (rc.canBid(biddingInfluence) && currentState != State.INIT) {
            rc.bid(biddingInfluence);
        }
        Debug.println("Amount bid: " + biddingInfluence);

        //updating currInfluence after a bid
        currInfluence = rc.getInfluence();
        if(currentState == State.INIT) { //Specific checks in the INIT state
            Debug.println(Debug.info, "Inside INIT checker");
            if(robotCounter >= 30 || muckrakerNear || rc.getInfluence() > Util.buildSlandererThreshold) { //Dont initialize if a muckraker is near or we have a lot of money from buff
                Debug.println(Debug.info, "set state from INIT to CHILLING");
                currentState = State.CHILLING;
            }
        }

        maxUsableInfluence = Math.max(1, currInfluence - enemyRushPolInf / 2);

        builtRobot = false;

        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        Debug.println(Debug.info, "num of ec's found: " + ECflags.size());
        Debug.println(Debug.info, "num ids found: " + idSet.size);
        Debug.println(Debug.info, "num protectors currently: " + protectorIdSet.size);
        Debug.println(Debug.info, "num slanderers currently: " + slandererIDToRound.size);
        Debug.println(Debug.info, "State stack size: " + stateStack.size() + ", state: " + currentState);
        Debug.println(Debug.info, "Muckraker near: " + muckrakerNear);
        Debug.println(Debug.info, "Wall locations: north: " + wallLocations[0] + "; east: " + wallLocations[1] + "; south: " + wallLocations[2] + "; west: " + wallLocations[3]);
        Debug.println(Debug.info, "closest wall direction: " + closestWall);
        Debug.println(Debug.info, "buf muck rush waiting time: " + buffMuckCooldown);
        if (!flagQueue.isEmpty()) {
            Debug.println(Debug.info, "IC of first elem in flag queue: " + Comms.getIC(flagQueue.peek()));
        }
        else {
            Debug.println(Debug.info, "flag queue is empty");
        }
        Debug.println("Enemy Rush pol: " + enemyRushPolInf);

        doStateAction();

        // If we didn't build a bot, then we can use nextFlag safely
        if(!flagQueue.isEmpty() && !builtRobot) {
            nextFlag = flagQueue.poll();
        }

        prevState = currentState;

        Debug.println(Debug.info, "next flag that will be set: " + nextFlag);
    }

    public void doStateAction() throws GameActionException {
        switch(currentState) {
            case INIT: 
                firstRounds();
                break;
            case ABOUT_TO_DIE:
                toBuild = RobotType.MUCKRAKER;
                influence = 1;
                dyingSemaphore--;
                buildRobot(toBuild, influence);
                if(rc.isReady()) {
                    Debug.println("Robots in orthogonal directions already, executing the turn for the last state");
                    currentState = stateStack.pop();
                    dyingSemaphore = dyingSemaphoreDefault;
                    doStateAction();
                    break;
                }
                if (dyingSemaphore == 0) {
                    currentState = stateStack.pop();
                    dyingSemaphore = dyingSemaphoreDefault;
                }
                break;
            case CHILLING: 
                if(protectorIdSet.size <= slandererIDToRound.size) {
                    toBuild = RobotType.POLITICIAN;
                    influence = getPoliticianInfluence();
                    makePolitician();
                    buildRobot(toBuild, influence);
                } else {
                    if(savingForSlanderer && Util.getBestSlandererInfluence(currInfluence) > 0 &&
                        protectorIdSet.size > 2 * slandererIDToRound.size) {
                        readyForSlanderer = true;
                    }
    
                    if(muckrakerNear || currInfluence > Util.buildSlandererThreshold) { //possibly add income threshold as well
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
                    }
                    else if (savingForSlanderer) {
                        switch(chillingCount % 3) {
                            /// case 0:
                            case 0: case 2:
                                toBuild = RobotType.MUCKRAKER;
                                influence = getMuckrakerInfluence();
                                makeMuckraker();
                                break;
                            // case 1: case 2:
                            case 1:
                                toBuild = RobotType.POLITICIAN;
                                influence = getPoliticianInfluence();
                                makePolitician();
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
                                influence = getPoliticianInfluence();
                                makePolitician();
                                if(buildRobot(toBuild, influence)) {
                                    Debug.println(Debug.info, "case 1 of the else case of CHILLING");
                                    chillingCount ++;
                                }
                                break;
                            case 2: 
                                toBuild = RobotType.MUCKRAKER;
                                influence = getMuckrakerInfluence();
                                makeMuckraker();
                                if(buildRobot(toBuild, influence)) {
                                    Debug.println(Debug.info, "case 2 of the else case of CHILLING");
                                    chillingCount ++;
                                }
                                break;
                            case 3: 
                                int currBestSlandererInfluence = Util.getBestSlandererInfluence(currInfluence);
                                toBuild = RobotType.MUCKRAKER;
                                influence = getMuckrakerInfluence();
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
                }
                break;
            case ACCELERATED_SLANDERERS:
                if(numPols <= 3 * slandererIDToRound.size / 2) {
                    toBuild = RobotType.POLITICIAN;
                    influence = getPoliticianInfluence();
                    makePolitician();
                    buildRobot(toBuild, influence);
                } else {
                    switch(builtInAcceleratedCount % 5) {
                        case 0: case 2: case 4:
                            toBuild = RobotType.POLITICIAN;
                            influence = getPoliticianInfluence();
                            makePolitician();
                            break;
                        case 1: case 3:
                            if(Util.getBestSlandererInfluence(currInfluence) > 0) {
                                toBuild = RobotType.SLANDERER;
                                influence = Util.getBestSlandererInfluence(currInfluence);
                            }
                            else {
                                toBuild = RobotType.MUCKRAKER;
                                influence = getMuckrakerInfluence();
                                makeMuckraker();
                            }
                            break;
                        default:
                            break;
                    }
                    if(buildRobot(toBuild, influence)) {
                        builtInAcceleratedCount ++;
                    }
                }
                break;
            case RUSHING_MUCKS:
                tryRushMuck();
                break;
            case RUSHING:
                trySendARush();
                break;
            case BETTER_RUSHING:
                MapLocation enemyLocation = home.translate(currRushFlag.dx, currRushFlag.dy);
                switch(rushingState) {
                    case MUCK:
                        toBuild = RobotType.MUCKRAKER;
                        influence = currRushFlag.muckInf;
                        nextFlag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC_MUK, currRushFlag.dx + Util.dOffset, currRushFlag.dy + Util.dOffset);
                        if(buildRobot(toBuild, influence)) {
                            rushingState = RushingState.RUSH;
                        }
                        break;
                    case SUPPORT1:
                        toBuild = RobotType.POLITICIAN;
                        influence = currRushFlag.supportInf;
                        // nextFlag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC_MUK, currRushFlag.dx + Util.dOffset, currRushFlag.dy + Util.dOffset);
                        signalRobotTypeAndDxDy(Comms.SubRobotType.POL_SUPPORT, currRushFlag.dx, currRushFlag.dy);
                        if(buildRobot(toBuild, influence)) {
                            rushingState = RushingState.RUSH;
                        }
                        break;
                    case RUSH:
                        toBuild = RobotType.POLITICIAN;
                        influence = currRushFlag.rushInf;
                        // nextFlag = Comms.getFlagRush(InformationCategory.HEAD_RUSH, (int)(4 * Math.random()), Comms.GroupRushType.MUC_POL, 
                        //                             currRushFlag.dx + Util.dOffset, currRushFlag.dy + Util.dOffset);
                        signalRobotTypeAndDxDy(Comms.SubRobotType.POL_HEAD, currRushFlag.dx, currRushFlag.dy);
                        if(buildRobot(toBuild, influence)) {
                            Debug.println("ECflags size: " + ECflags.size());
                            ECflags.remove(currRushFlag);
                            Debug.println("ECflags size after removing: " + ECflags.size());
                            currentState = stateStack.pop();
                            lastRush = rc.getRoundNum();
                            int cooldown = Math.min(Util.maxRushCooldown, Util.baseRushCooldown + Math.max(Math.abs(currRushFlag.dx), Math.abs(currRushFlag.dy)));
                            Debug.println("Cooldown for " + enemyLocation + " : " + cooldown);
                            
                            rushingECtoTurnMap.add(enemyLocation, lastRush + cooldown);
                        }
                        break;
                    // case SUPPORT2:
                    //     toBuild = RobotType.POLITICIAN;
                    //     influence = targetEC1.supportInf;
                    //     nextFlag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC_MUK, targetEC1.dx + Util.dOffset, targetEC1.dy + Util.dOffset);
                    //     if(buildRobot(toBuild, influence)) {
                    //         rushingState = RushingState.RUSH;
                    //     }
                    //     break;
                }
                break;
            case SAVING_FOR_RUSH:
                RushFlag targetEC = ECflags.peek();
                if(targetEC == null) {
                    currentState = stateStack.pop();
                    break;
                }
                int[] currDxDy = {targetEC.dx, targetEC.dy};
                
                toBuild = RobotType.MUCKRAKER;
                influence = getMuckrakerInfluence();
                makeMuckraker();
                
                // if(robotCounter % 2 == 0 || prevState != currentState) {
                //     toBuild = RobotType.MUCKRAKER;
                //     influence = Integer.max(1, currInfluence / 500);
                // } else {
                //     toBuild = RobotType.POLITICIAN;
                //     influence = Math.max(15, currInfluence / 50);
                //     signalRobotAndDirection(SubRobotType.POL_PROTECTOR, closestWall);
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
                if(Util.getBestSlandererInfluence(currInfluence) >= 100 && robotCounter % 3 == 1 && !muckrakerNear && currInfluence < Util.buildSlandererThreshold) {
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
                    influence = getMuckrakerInfluence();
                    makeMuckraker();
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
            case STUCKY_MUCKY:
                switch(robotCounter % 3) {
                    case 0: case 1:
                        toBuild = RobotType.MUCKRAKER;
                        influence = getMuckrakerInfluence();
                        makeMuckraker();
                        break;
                    case 2:
                        toBuild = RobotType.POLITICIAN;
                        influence = getPoliticianInfluence();
                        makePolitician();
                        break;
                }
                buildRobot(toBuild, influence);
                break;
            case BUILDING_BUFF_POLS:
                influence = Math.max(50, currInfluence / 10);
                toBuild = RobotType.POLITICIAN;
                signalRobotType(Comms.SubRobotType.POL_BUFF);
                if(buildRobot(toBuild, influence)) {
                    builtInBuffPolsCount++;
                    buffPolLock = 0;
                    if(builtInBuffPolsCount >= Util.maxBuffPolsInARow || enemyBuffMuckInf <= 50 * buffPolSet.size) {
                        currentState = stateStack.pop();
                    }
                }
                break;
            default:
                currentState = State.CHILLING;
                System.out.println("CRITICAL: Maxwell screwed up stateStack");
                break;
        }
    }

    public Direction findClosestWall() throws GameActionException {
        Direction firstWall = null;
        Direction secondWall = null;
        int firstClosest = Integer.MAX_VALUE;
        int secondClosest = Integer.MAX_VALUE;
        Direction[] cardinalDirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        int currWallDist;
        Direction dir;
        for (int i = cardinalDirs.length - 1; i >= 0; i--) {
            dir = cardinalDirs[i];
            currWallDist = Math.abs(wallLocations[i]);
            if (currWallDist != 0) {
                if (currWallDist < firstClosest) {
                    secondClosest = firstClosest;
                    secondWall = firstWall;
                    firstClosest = currWallDist;
                    firstWall = dir;
                }
                else if (currWallDist <= secondClosest) {
                    secondClosest = currWallDist;
                    secondWall = dir;
                }
            }
        }
        Debug.println(Debug.info, "closest wall: " + firstWall + " at location: " + firstClosest + "; second closest wall: " + secondWall + " at location: " + secondClosest);
        if ((firstClosest == Integer.MAX_VALUE && secondClosest == Integer.MAX_VALUE) || 
            firstClosest >= 20) {
            return null;
        }
        Debug.println(Debug.info, "First closest wall: " + firstClosest + "; 2nd closest: " + secondClosest);
        if (Math.abs(firstClosest - secondClosest) <= 15) {
            if ((firstWall == Direction.NORTH && secondWall == Direction.WEST) || 
                (secondWall == Direction.NORTH && firstWall == Direction.WEST)) {
                return Direction.NORTHWEST;
            }
            else if ((firstWall == Direction.NORTH && secondWall == Direction.EAST) || 
                     (secondWall == Direction.NORTH && firstWall == Direction.EAST)) {
                return Direction.NORTHEAST;
            }
            else if ((firstWall == Direction.SOUTH && secondWall == Direction.WEST) || 
                    (secondWall == Direction.SOUTH && firstWall == Direction.WEST)) {
                return Direction.SOUTHWEST;
            }
            else if ((firstWall == Direction.SOUTH && secondWall == Direction.EAST) || 
                    (secondWall == Direction.SOUTH && firstWall == Direction.EAST)) {
                return Direction.SOUTHEAST;
            }
        }
        return firstWall;
    } 

    public void initializeGlobals() throws GameActionException {
        enemySensable = rc.senseNearbyRobots(sensorRadius, enemy);
        friendlySensable = rc.senseNearbyRobots(sensorRadius, rc.getTeam());

        toBuild = null;
        influence = 0;
        currRoundNum = rc.getRoundNum();
        currInfluence = rc.getInfluence();
        closestWall = findClosestWall();
        enemyRushPolInf = 0;
        enemyBuffMuckInf = 0;

        // Reset slanderer every 3 rounds
        if(currRoundNum % 3 == 0) {
            recentSlanderer = null;
        }

        if (spawnKillLock < 10) {
            spawnKillLock++;
        } else {
            spawnKillLoc = null;
        }

        if(buffPolLock < 10) {
            buffPolLock++;
        }

        MapLocation[] keys = rushingECtoTurnMap.getKeys();
        MapLocation key;
        for(int i = keys.length - 1; i >= 0; i--) {
            key = keys[i];
            if(currRoundNum >= rushingECtoTurnMap.getVal(key)) {
                rushingECtoTurnMap.remove(key);
            }
        }
        noAdjacentEC = true;
        for (int i = nearbyECs.size - 1; i >= 0; i--) {
            MapLocation locOfNearby = nearbyECs.locs[i];
            RobotInfo robot;
            if (rc.canSenseLocation(locOfNearby)) {
                robot = rc.senseRobotAtLocation(locOfNearby);
            } else {
                continue;
            }
            if (robot.getTeam() == enemy) {
                noAdjacentEC = false;
                RushFlag rushFlag;
                MapLocation EnemyECLoc = robot.getLocation();
                int neededInf =  robot.getInfluence();
                int currReqInf = (int)  neededInf * 4 + 10;
                if(currRoundNum <= 150) {
                    currReqInf = (int) neededInf * 2 + 10;
                }
                int actualDX = EnemyECLoc.x - rc.getLocation().x;
                int actualDY = EnemyECLoc.y - rc.getLocation().y;
                int encodedInf = Comms.encodeInf(robot.getInfluence());
                int flag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC, encodedInf, actualDX + Util.dOffset, actualDY + Util.dOffset);
                rushFlag = new RushFlag(currReqInf, actualDX, actualDY, flag, rc.getTeam().opponent());
                Debug.println(Debug.info, "ADJACENT INFO:: neededInf: " + neededInf + "; actualDX: " + actualDX + "; actualDY: " + actualDY);
                
                ECflags.remove(rushFlag);
                if (!enemyECsFound.contains(EnemyECLoc)) {
                    enemyECsFound.add(EnemyECLoc);
                }
                ECflags.add(rushFlag);
            } else if (robot.getTeam() == Team.NEUTRAL) {
                RushFlag rushFlag;
                MapLocation NeutralECLoc = robot.getLocation();
                int neededInf =  robot.getInfluence();
                int currReqInf = (int) neededInf * 2 + 10;
                int actualDX = NeutralECLoc.x - rc.getLocation().x;
                int actualDY = NeutralECLoc.y - rc.getLocation().y;
                int encodedInf = Comms.encodeInf(robot.getInfluence());
                int flag = Comms.getFlag(Comms.InformationCategory.NEUTRAL_EC, encodedInf, actualDX + Util.dOffset, actualDY + Util.dOffset);
                rushFlag = new RushFlag(currReqInf, actualDX, actualDY, flag, Team.NEUTRAL);
                Debug.println(Debug.info, "ADJACENT INFO:: neededInf: " + neededInf + "; actualDX: " + actualDX + "; actualDY: " + actualDY);
                
                ECflags.remove(rushFlag);
                ECflags.add(rushFlag);
            }
        }
    }

    public int getPoliticianInfluence() throws GameActionException {
        // if(numPols > 10 && numPols % Util.buffPolFrequency == 0 && 
        //     buffPolSet.size < Util.maxBuffPolNum && protectorIdSet.size > 2 * buffPolSet.size) {
        //     System.out.println("Giving buff pol influence");
        //     return Math.max(50, currInfluence / 10);
        // } else {
        //     return Math.max(20, currInfluence / 50);
        // }
        return Math.max(20, currInfluence / 50);
    }

    public void makePolitician() throws GameActionException {
        // if(numPols > 10 && numPols % Util.buffPolFrequency == 0 && 
        //     buffPolSet.size < Util.maxBuffPolNum && protectorIdSet.size > 2 * buffPolSet.size) {
        //     System.out.println("Making buff pol");
        //     signalRobotType(Comms.SubRobotType.POL_BUFF);
        // } else if(numPols % Util.explorerPolFrequency == 0) {
        //     signalRobotType(Comms.SubRobotType.POL_EXPLORER);
        // } else {
        //     signalRobotAndDirection(SubRobotType.POL_PROTECTOR, closestWall);
        // }
        if(numPols % Util.explorerPolFrequency == 0) {
            signalRobotType(Comms.SubRobotType.POL_EXPLORER);
        } else {
            signalRobotAndDirection(SubRobotType.POL_PROTECTOR, closestWall);
        }
    }

    public int getMuckrakerInfluence() throws GameActionException {
        if (numMucks > 50 && numMucks % Util.buffMukFrequency == 0 && noAdjacentEC) {
            return Math.max(50, Math.min(currInfluence / 5, 400));
        } else {
            return Math.max(1, currInfluence / 50);
        }
    }

    public void makeMuckraker() throws GameActionException {
        RushFlag targetEC = ECflags.peek();
        if (numMucks % 2 == 0 || influence > 1) {
            if (targetEC != null && targetEC.team != Team.NEUTRAL) {
                int[] currDxDy = {targetEC.dx, targetEC.dy};
                nextFlag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC_MUK, targetEC.dx + Util.dOffset, targetEC.dy + Util.dOffset);
                if (influence > 1) {
                    Debug.println(Debug.info, "This is a buff muck of influence: " + influence + "; destination " + targetEC.dx + ", " + targetEC.dy + ".");
                }
                else {
                    Debug.println(Debug.info, "Making hunter mucker with destination " + targetEC.dx + ", " + targetEC.dy + ".");
                }
            } else {
                nextFlag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC_MUK);
                Debug.println(Debug.info, "Making hunter mucker with no destination.");
            }
        }
    }

    public void tryStartBuildingBuffPols() throws GameActionException {
        if(enemyBuffMuckInf >= 50 && enemyBuffMuckInf >= 50 * buffPolSet.size && 
            buffPolLock >= 10 && currentState != State.BUILDING_BUFF_POLS) {
            stateStack.push(currentState);
            currentState = State.BUILDING_BUFF_POLS;
            builtInBuffPolsCount = 0;
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
    
    public int[] checkIfMuckrakerNear() throws GameActionException {
        RobotInfo robot;
        int[] res = {0, 0};
        int polMaxInf = 0;
        for(int i = enemySensable.length - 1; i >= 0; i--) {
            robot = enemySensable[i];
            if (robot.getType() == RobotType.MUCKRAKER) {
                res[0] = 1;
            } else if (robot.getType() == RobotType.POLITICIAN) {
                if (robot.getConviction() > polMaxInf) {
                    polMaxInf = robot.getConviction();
                    enemyRushPolLoc = robot.getLocation();
                }
            }
        }
        res[1] = polMaxInf;
        return res;
    }

    public void processChildrenFlags() throws GameActionException {
        idSet.updateIterable();
        // protectorIdSet.updateIterable();

        int id, slandererID;
        MapLocation tempMapLoc;
        int neededInf;
        int currReqInf;
        int friendId;
        int flag;
        int[] currDxDy;
        int[] enemyDxDy;
        int[] wallDxDy;
        int dx, dy;
        int enemyLocX, enemyLocY;
        int wallDx, wallDy;
        RushFlag rushFlag;
        int roundNum;
        int muckInf;
        int supportInf;
        int rushInf;
        if(buffMuckCooldown != 0) {
            buffMuckCooldown --;
        }
        if(roundToSlandererID.contains(currRoundNum)) {
            slandererID = roundToSlandererID.getVal(currRoundNum);
            roundToSlandererID.remove(currRoundNum);
            slandererIDToRound.remove(slandererID);
            protectorIdSet.add(slandererID);
        }

        for(int j = idSet.size - 1; j >= 0; j--) {
            if(Clock.getBytecodesLeft() < 3000) {
                Debug.println(Debug.info, "Bytecode limit close, Breaking from loop");
                break;
            }
            id = ids[j];
            if(rc.canGetFlag(id)) {
                flag = rc.getFlag(id);
                switch (Comms.getIC(flag)) {
                    case NEUTRAL_EC:
                        currDxDy = Comms.getDxDy(flag);

                        dx = currDxDy[0] - Util.dOffset;
                        dy = currDxDy[1] - Util.dOffset;
                        tempMapLoc = new MapLocation(home.x + dx, home.y + dy);

                        // Only insert if we aren't rushing this EC
                        if(!rushingECtoTurnMap.contains(tempMapLoc)) {
                            neededInf = Comms.getInf(flag);
                            // currReqInf = neededInf * 2 + 10;
                            // rushFlag = new RushFlag(currReqInf, dx, dy, flag, Team.NEUTRAL);
                            muckInf = 0;
                            supportInf = Math.min(100, Math.max(20, neededInf / 5));
                            rushInf = neededInf * 2 + 10;
                            currReqInf = rushInf + supportInf + muckInf;
                            rushFlag = new RushFlag(currReqInf, dx, dy, flag, Team.NEUTRAL,
                                                    muckInf, supportInf, rushInf);
                            ECflags.remove(rushFlag);
                            ECflags.add(rushFlag);
                        }

                        if(tempMapLoc.isWithinDistanceSquared(home, 2 * sensorRadius) && !nearbyECs.contains(tempMapLoc)) {
                            nearbyECs.add(tempMapLoc);
                            nearbyECs.updateIterable();
                        }

                        Debug.println(Debug.info, "Found neutral EC at : " + tempMapLoc);
                        break;
                    case ENEMY_EC:
                        currDxDy = Comms.getDxDy(flag);

                        dx = currDxDy[0] - Util.dOffset;
                        dy = currDxDy[1] - Util.dOffset;
                        tempMapLoc = new MapLocation(home.x + dx, home.y + dy);
                        
                        // Only insert if we aren't rushing this EC
                        if(!rushingECtoTurnMap.contains(tempMapLoc)) {
                            neededInf =  Comms.getInf(flag);
                            // currReqInf = neededInf * 4 + 10;
                            // if(currRoundNum <= 150) {
                            //     currReqInf = neededInf * 2 + 10;
                            // }
                            // rushFlag = new RushFlag(currReqInf, dx, dy, flag, rc.getTeam().opponent());
                            // muckInf = Math.max(20, neededInf / 4);
                            muckInf = 0;
                            supportInf = Math.min(100, Math.max(20, neededInf / 5));
                            rushInf = neededInf * 2 + 10;
                            currReqInf = rushInf + supportInf + muckInf;
                            rushFlag = new RushFlag(currReqInf, dx, dy, flag, rc.getTeam().opponent(),
                                                    muckInf, supportInf, rushInf);
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

                        if(tempMapLoc.isWithinDistanceSquared(home, 2 * sensorRadius) && !nearbyECs.contains(tempMapLoc)) {
                            nearbyECs.add(tempMapLoc);
                            nearbyECs.updateIterable();
                        }

                        Debug.println(Debug.info, "Found enemy EC at : " + tempMapLoc);
                        friendlyECs.remove(tempMapLoc);
                        break;
                    case FRIENDLY_EC:
                        switch(Comms.getFriendlyECType(flag)) {
                            case HOME_READ_LOC:
                                currDxDy = Comms.getFriendlyDxDy(flag);
                                rushFlag = new RushFlag(0, currDxDy[0] - Util.dOffset, currDxDy[1] - Util.dOffset, 0, rc.getTeam());
                                tempMapLoc = new MapLocation(home.x + rushFlag.dx, home.y + rushFlag.dy);
                                ECflags.remove(rushFlag);
                                if (enemyECsFound.contains(tempMapLoc)) enemyECsFound.remove(tempMapLoc);

                                idToFriendlyECLocMap.remove(id);
                                idToFriendlyECLocMap.add(id, tempMapLoc);
                                break;
                            case HOME_READ_ID:
                                if(idToFriendlyECLocMap.contains(id)) {
                                    friendId = Comms.getFriendlyID(flag);
                                    tempMapLoc = idToFriendlyECLocMap.getLoc(id);
                                    // Uhh todo. Why do I need this check?? Shouldn't be reporting home to itself.
                                    if(!tempMapLoc.equals(home)) {
                                        friendlyECs.add(tempMapLoc, friendId);
                                    }
                                    idToFriendlyECLocMap.remove(id);

                                    Debug.println(Debug.info, "Found friendly EC at : " + tempMapLoc + ", id: " + friendId);
                                }
                                break;
                        }
                        break;
                    case ENEMY_FOUND:
                        enemyDxDy = Comms.getDxDy(flag);
                        enemyLocX = enemyDxDy[0] + home.x - Util.dOffset;
                        enemyLocY = enemyDxDy[1] + home.y - Util.dOffset;
                        
                        tempMapLoc = new MapLocation(enemyLocX, enemyLocY);
                        if(scoutIDToEnemyLocs.contains(id)) {
                            Debug.println("Changing scoutID Map because scout has seen an enemy, Scout ID: " + id);
                            scoutIDToEnemyLocs.remove(id);
                            scoutIDToEnemyLocs.add(id, tempMapLoc);
                        }
                        if (rc.getLocation().isWithinDistanceSquared(tempMapLoc, rc.getType().sensorRadiusSquared * 4)) {
                            goToAcceleratedSlanderersState = false;
                        }

                        switch(Comms.getEnemyType(flag)) {
                            case SLA:
                                recentSlanderer = tempMapLoc;
                                Debug.setIndicatorDot(Debug.info, tempMapLoc, 50, 150, 50);
                                break;
                            case MUC:
                                if (rc.getLocation().isWithinDistanceSquared(tempMapLoc, sensorRadius * 4)) {
                                    muckrakerNear = true;
                                }
                                Debug.setIndicatorDot(Debug.info, tempMapLoc, 200, 50, 50);
                                break;
                            default:
                                Debug.setIndicatorDot(Debug.info, tempMapLoc, 0, 0, 0);
                                break;
                        }
                        break;
                    case REPORTING_WALL:
                        wallDxDy = Comms.getDxDy(flag);
                        wallDx = wallDxDy[0] != 0 ? wallDxDy[0] - Util.dOffset : 0;
                        wallDy = wallDxDy[1] != 0 ? wallDxDy[1] - Util.dOffset : 0;
                        if (wallDx < 0) { 
                            Debug.println(Debug.info, "scout with id: " + id + " found wall to the west at dx: " + wallDx);
                            wallLocations[3] = wallDx;
                        } 
                        else if (wallDx > 0) {
                            Debug.println(Debug.info, "scout with id: " + id + " found wall to the east at dx: " + wallDx);
                            wallLocations[1] = wallDx;
                        }

                        if (wallDy < 0) {
                            Debug.println(Debug.info, "scout with id: " + id + " found wall to the south at dx: " + wallDy);
                            wallLocations[2] = wallDy;
                        } 
                        else if (wallDy > 0) {
                            Debug.println(Debug.info, "scout with id: " + id + " found wall to the north at dx: " + wallDy);
                            wallLocations[0] = wallDy;
                        }
                        break;
                    case DELETE_ENEMY_LOC:
                        if (flagQueueCooldown >= 10) {
                            flagQueueCooldown = 0;
                            flagQueue.add(flag);
                        }
                        break;
                    // case BUFF_MUCK:
                    //     enemyBuffMuckInf = Math.max(enemyBuffMuckInf, Comms.getInf(flag));
                    //     break;
                }
            } else {
                idSet.remove(id);
                protectorIdSet.remove(id);
                buffPolSet.remove(id);
                if(slandererIDToRound.contains(id)) {
                    roundNum = slandererIDToRound.getVal(id);
                    slandererIDToRound.remove(id);
                    roundToSlandererID.remove(roundNum);
                }
                if (nextBufLoc == null && scoutIDToEnemyLocs.contains(id) && !firstScoutDeathReported) {
                    if(scoutIDToEnemyLocs.getLoc(id).equals(new MapLocation(0,0))) {
                        Debug.println("scout dead after not finding anything");
                    }
                    else {
                        Debug.println("Scout actually found an enemy");
                        nextBufLoc = scoutIDToEnemyLocs.getLoc(id);
                        Debug.println("first Scout Death reported: " + nextBufLoc + "; id: " + id);
                    }
                }
            }
        }
        

        if(recentSlanderer != null && currRoundNum % 3 == 0) {
            dx = recentSlanderer.x - home.x;
            dy = recentSlanderer.y - home.y;
            flagQueue.add(Comms.getFlagRush(Comms.InformationCategory.ENEMY_FOUND, (int)(2 * Math.random()), Comms.EnemyType.SLA, 
                                            dx + Util.dOffset, dy + Util.dOffset));
        }

        cleanUpCount++;
    }

    public void processLocalFlags() throws GameActionException {
        Debug.println(Debug.info, "Process local flags");
        RobotInfo robot;
        int id, friendId;;
        MapLocation tempMapLoc;
        int[] currDxDy;
        int flag;
        for(int i = friendlySensable.length - 1; i >= 0; i--) {
            robot = friendlySensable[i];
            id = robot.getID();
            if(!idSet.contains(id) && rc.canGetFlag(id)) {
                flag = rc.getFlag(id);
                switch(Comms.getIC(flag)) {
                    case FRIENDLY_EC:
                        switch(Comms.getFriendlyECType(flag)) {
                            case OTHER_READ_LOC:
                                currDxDy = Comms.getFriendlyDxDy(flag);
                                tempMapLoc = new MapLocation(robot.getLocation().x + currDxDy[0] - Util.dOffset,
                                                            robot.getLocation().y + currDxDy[1] - Util.dOffset);
                                if(!friendlyECs.contains(tempMapLoc)) {
                                    idToFriendlyECLocMap.remove(id);
                                    idToFriendlyECLocMap.add(id, tempMapLoc);
                                }
                                break;
                            case OTHER_READ_ID:
                                if(idToFriendlyECLocMap.contains(id)) {
                                    friendId = Comms.getFriendlyID(flag);
                                    tempMapLoc = idToFriendlyECLocMap.getLoc(id);
                                    friendlyECs.add(tempMapLoc, friendId);
                                    idToFriendlyECLocMap.remove(id);

                                    Debug.println(Debug.info, "Found friendly EC at : " + tempMapLoc + ", id: " + friendId);
                                }
                                break;
                        }
                        break;
                }
            }
        }
    }

    public void processFriendlyECFlags() throws GameActionException{
        MapLocation[] keys = friendlyECs.getKeys();
        MapLocation key;
        int id;
        int ECflag;
        MapLocation rushLoc;
        int[] friendlyDxDy;
        int rushDx, rushDy;
        for(int i = keys.length - 1; i >= 0; i--) {
            key = keys[i];
            id = friendlyECs.getVal(key);

            if(rc.canGetFlag(id)) {
                ECflag = rc.getFlag(id);
                switch (Comms.getIC(ECflag)) {
                    case ENEMY_EC:
                        friendlyDxDy = Comms.getDxDy(ECflag);
                        rushLoc = new MapLocation(friendlyDxDy[0] + key.x - Util.dOffset, friendlyDxDy[1] + key.y - Util.dOffset);
                        rushDx = rushLoc.x - home.x + Util.dOffset;
                        rushDy = rushLoc.y - home.y + Util.dOffset;
                        if (flagQueueCooldown >= 10) {
                            flagQueueCooldown = 0;
                            flagQueue.add(Comms.getFlagRush(InformationCategory.ENEMY_EC, (int)(4 * Math.random()), Comms.GroupRushType.MUC_POL, 
                                            rushDx, rushDy));
                        }
                        break;
                    default:
                        break;
                }
                Debug.setIndicatorDot(Debug.info, key, 200, 200, 200);
                Debug.println(Debug.info, "EC at: " + key + ", id: " + id);
            } else {
                friendlyECs.remove(key);
            }
        }
        flagQueueCooldown++;
    }

    public boolean tryStartCleanup() throws GameActionException {
        Debug.println(Debug.info, "Cleanup count: " + cleanUpCount);
        if (enemyECsFound.size == 0 && cleanUpCount > Util.startCleanupThreshold) {
            return true;
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

    public boolean tryRushMuck() throws GameActionException {
        Debug.println("building buff Muck");

        toBuild = RobotType.MUCKRAKER;
        influence = Math.max(Util.scoutBuffMuckSize, currInfluence / 5);
        if(influence >= currInfluence) {
            currentState = stateStack.pop();
            return false;
        }
        if(nextBufLoc != null && buildRobot(toBuild, influence)) { //short circuit if firstScoutDeath == null
            nextFlag = Comms.getFlag(Comms.InformationCategory.ENEMY_EC_MUK, nextBufLoc.x - home.x + Util.dOffset, nextBufLoc.y - home.y + Util.dOffset);
            firstScoutDeathReported = true;
            buffMuckCooldown = Util.bufMuckCooldownThreshold;
            lastSentBufMuck = nextBufLoc;
            nextBufLoc = null;
            currentState = stateStack.pop();
            return true;
        }
        return false;
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
            lastRush = rc.getRoundNum();
            int cooldown = Math.min(Util.maxRushCooldown, Util.baseRushCooldown + Math.max(Math.abs(currRushFlag.dx), Math.abs(currRushFlag.dy)));
            
            rushingECtoTurnMap.add(enemyLocation, lastRush + cooldown);
            /*if (currentState != State.BUILDING_SLANDERERS) {
                canGoBackToBuildingProtectors = true;
            }
            return true;*/
        }
        return false;
    }

    void signalRobotType(Comms.SubRobotType type) throws GameActionException {
        nextFlag = Comms.getFlag(Comms.InformationCategory.TARGET_ROBOT, type);
    }

    void signalRobotAndDirection(Comms.SubRobotType type, Direction dir) throws GameActionException {
        if (dir == null) {
            nextFlag = Comms.getFlagScout(Comms.InformationCategory.TARGET_ROBOT, type, Direction.CENTER);
        }
        else {
            nextFlag = Comms.getFlagScout(Comms.InformationCategory.TARGET_ROBOT, type, dir);
        }
    }

    void signalRobotTypeAndDxDy(Comms.SubRobotType type, int dx, int dy) {
        nextFlag = Comms.getFlag(Comms.InformationCategory.TARGET_ROBOT, type, dx + Util.dOffset, dy + Util.dOffset);
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

    public boolean readyToSendBufMuck() {
        if(nextBufLoc != null && currInfluence > Util.scoutBuffMuckSize && 
            buffMuckCooldown  == 0 && !nextBufLoc.equals(lastSentBufMuck) &&
            noAdjacentEC) {
            Debug.setIndicatorLine(Debug.info, home, nextBufLoc,128,0,128);
            return true;
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

    public int bidBS() throws GameActionException {
        int currVotes = rc.getTeamVotes();
        int res;
        // Handle the edge cases where we've won or are close to the end or beginning.
        if (currentState == State.INIT || currVotes > 750) {
            return 0;
        } else if (currRoundNum > 1300) {
            return currInfluence / 25;
        }

        // Check if we won the last bid and increment accordingly.
        if (currVotes > lastVoteCount) {
            lastVoteCount++;
            wonLastBid = true;
            Debug.println("Won last bid.");
        } else {
            wonLastBid = false;
            Debug.println("Lost last bid.");
        }

        // Check if we are at an equilibrium.
        if (biddingOneLess) {
            biddingOneLess = false;
            if (!wonLastBid) {
                bidEquilibrium = true;
                res = ++prevBid;
                Debug.println("At equilibrium.");
                return res;
            }
        } else if (bidEquilibrium) {
            if (wonLastBid && prevBid < currInfluence / 25) {
                return prevBid;
            } else bidEquilibrium = false;
        }

        // Change the big or little depending on if you won.
        if (wonLastBid) bigBid = prevBid;
        else littleBid = prevBid;

        // Otherwise use a binary search to find the new bid.
        if (wonLastBid) {
            res = Integer.min(Integer.max((prevBid + littleBid) / 2, 2), currInfluence / 25);
            if (res == littleBid && res == bigBid) littleBid /= 2;
        } else {
            if (bigBid < currInfluence / 10) bigBid *= 2;
            res = Integer.min(Integer.max((prevBid + bigBid) / 2, 2), currInfluence / 25);
        }
        // Handle the edge cases where the big bid is too small or little bid is too big.
        if (bigBid < 2) bigBid = 2;
        if (res < littleBid) littleBid = res / 2;
        Debug.println("L: " + littleBid + ", B: " + bigBid);

        // Check to see if we're ready to go into equilibrium.
        if (res == prevBid - 1) {
            biddingOneLess = true;
        }
        prevBid = res;
        return res;
    }
    
    public void firstRounds() throws GameActionException {
        switch(robotCounter) {
            case 0: case 5: case 9: case 13: case 15: case 20: case 22: case 25: case 28:
                toBuild = RobotType.SLANDERER;
                influence = Math.min(130, Util.getBestSlandererInfluence(currInfluence));
                break;
            case 3: case 4: case 6: case 7: case 8: case 10: case 11: case 12: 
                toBuild = RobotType.MUCKRAKER;
                influence = Integer.max(1, currInfluence / 500);
                if(numMucks < 8) {
                    signalRobotAndDirection(Comms.SubRobotType.MUC_SCOUT, Util.scoutDirs[numMucks]);
                }
                break;
            case 2: case 14: case 17: case 18: case 19: case 21: case 23: case 24: case 26: case 27: case 29:
                toBuild = RobotType.POLITICIAN;
                influence = 15;
                signalRobotAndDirection(SubRobotType.POL_PROTECTOR, closestWall);
                break;
            case 1: case 16:
                toBuild = RobotType.POLITICIAN;
                influence = 1;
                signalRobotType(SubRobotType.POL_EXPLORER);
                break;
        }

        if (!buildRobot(toBuild, influence)) {
            toBuild = RobotType.MUCKRAKER;
            influence = 1;
            buildRobot(toBuild, influence);
        }
    }
}