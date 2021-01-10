package musketeerplayer;
import battlecode.common.*;

import musketeerplayer.Comms.*;
import musketeerplayer.Util.*;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.PriorityQueue;
import java.util.stream.Stream;


public class EC extends Robot {
    static enum State {
        PHASE1,
        PHASE2,
        RUSHING,
        SAVING_FOR_RUSH,
        CLEANUP,
        MAKING_GOLEM, // TOCONSIDER
        MAKING_DEFENDERS,
        REMOVING_BLOCKAGE
    };

    static int robotCounter;
    static int sendTroopsSemaphore = 0;
    static RobotType toBuild;
    static int influence;

    static int cleanUpCount = 0;

    static int currRoundNum;
    static int currInfluence;
    static boolean needToBuild;
    static boolean muckrackerNear;

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

    static ArrayList<Integer> ids; // TODO USE HASHSET
    static PriorityQueue<RushFlag> ECflags;
    static ArrayDeque<State> stateStack;

    static State currentState;
    static State prevState;

    static boolean needToMakeBodyguard = false;
    static int lastRush = 0;

    // TODO: Better slanderer pathfinding.
    // TODO: implement spawn killing if it is worth it, 
    // TODO: better money management strategy(I.E don't just making 1000 slanderers quicker and quicker in late game)
    // TODO: Muckrakers need to go towards enemy towers
    // TODO: change the new player to only build defenders if 6 our of 8 surrounding squares are taken by enemy
    public EC(RobotController r) {
        super(r);
        ids = new ArrayList<Integer>();
        ECflags = new PriorityQueue<RushFlag>();
        stateStack = new ArrayDeque<State>();
        currentState = State.PHASE1;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.EC);
    }

    public boolean buildRobot(RobotType toBuild, int influence) throws GameActionException {
        Util.vPrintln("building robot type: " + toBuild + " influence: " + influence);
        Direction main_direction = Util.randomDirection();
        int num_direction = 8;
        while(num_direction != 0) {
            if (rc.canBuildRobot(toBuild, main_direction, influence)) {
                Util.vPrintln("built robot");
                rc.buildRobot(toBuild, main_direction, influence);

                if(needToMakeBodyguard) {
                    needToMakeBodyguard = false;
                }
                if(toBuild == RobotType.SLANDERER) {
                    needToMakeBodyguard = true;
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

        Util.vPrintln("I am a " + rc.getType() + "; current influence: " + currInfluence);
        Util.vPrintln("current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        Util.vPrintln("num of ec's found: " + ECflags.size());
        Util.vPrintln("state: " + currentState);

        findNearIds();
        checkForTowers();
        if (currRoundNum > 500)
            tryStartCleanup();

        int biddingInfluence = currInfluence / 20;
        if (rc.canBid(biddingInfluence) && currRoundNum > 500) {
            rc.bid(biddingInfluence);
        } else {
            biddingInfluence = Math.max(currInfluence / 100, 1);
            if (rc.canBid(biddingInfluence)) {
                rc.bid(biddingInfluence);
            }
        }
        muckrackerNear = checkIfMuckrakerNear();
        // if (rc.getEmpowerFactor(rc.getTeam(),0) > Util.spawnKillThreshold) {
        //     Util.vPrintln("spawn killing politicians");
        //     influence = 6*rc.getInfluence()/8;
        //     toBuild = RobotType.POLITICIAN;
        // }
        switch(currentState) {
            case PHASE1:
                Util.vPrintln("Phase1 state");
                if(currInfluence >= 150 && robotCounter % 5 == 0 && !muckrackerNear) {
                    toBuild = RobotType.SLANDERER;
                    influence = Math.min(1000, currInfluence);
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
                Util.vPrintln("Phase2 state");
                if(needToMakeBodyguard) {
                    toBuild = RobotType.POLITICIAN;
                    influence = currInfluence;
                    signalRobotType(Comms.SubRobotType.POL_BODYGUARD);
                } else if(7 * currInfluence / 8 >= 150 && robotCounter % 5 == 0 && !muckrackerNear) {
                    toBuild = RobotType.SLANDERER;
                    influence = Math.min(1000, 7 * currInfluence / 8);
                } else {
                    toBuild = RobotType.MUCKRAKER;
                    influence = 1;
                }
                
                if(needToBuild) {
                    buildRobot(toBuild, influence);
                }             
                if (tryStartRemovingBlockage()) {}   
                else if (!tryStartMakingDefenders()) {tryStartSavingForRush();}
                break;
            case RUSHING:
                trySendARush();
                break;
            case SAVING_FOR_RUSH:
                RushFlag targetEC = ECflags.peek();
                int requiredInfluence = targetEC.requiredInfluence;
                int[] currDxDy = {targetEC.dx, targetEC.dy};
                toBuild = RobotType.MUCKRAKER;
                influence = 1;
                Util.vPrintln("Required Influence: " + requiredInfluence + "; DxDy: " + (currDxDy[0] - Util.dOffset) +  ", " + (currDxDy[1] - Util.dOffset));
                if(needToBuild) {
                    buildRobot(toBuild, influence);
                }

                if(requiredInfluence < currInfluence && rc.isReady()) {
                    sendTroopsSemaphore = 1;
                    resetFlagOnNewTurn = false;
                    nextFlag = targetEC.flag;
                    currentState = State.RUSHING;
                }
                tryStartRemovingBlockage();
                tryStartMakingDefenders();
                break;
            case MAKING_DEFENDERS:
                // Consider staggering making defenders
                boolean built = false;
                toBuild = RobotType.POLITICIAN;
                influence = 20;

                if(needToBuild) {
                    signalRobotType(Comms.SubRobotType.POL_DEFENDER);
                    built = buildRobot(toBuild, influence);
                }

                if(built && checkNumDefenders() + 2 >= Util.numDefenders) {
                    currentState = stateStack.pop();
                }
                break;
            case CLEANUP:
                if(robotCounter % 2 == 0) {
                    toBuild = RobotType.POLITICIAN;
                    influence = Util.cleanupPoliticianInfluence;
                    signalRobotType(Comms.SubRobotType.POL_CLEANUP);
                } else {
                    toBuild = RobotType.SLANDERER;
                    influence = Math.min(100, currInfluence / 2);
                }
                if(needToBuild) {
                    buildRobot(toBuild, influence);
                }
                break;
            case REMOVING_BLOCKAGE:
                toBuild = RobotType.POLITICIAN;
                influence = 20;
                signalRobotType(Comms.SubRobotType.POL_DEFENDER);
                if(needToBuild && buildRobot(toBuild, influence)) {
                    currentState = stateStack.pop();
                }
                break;
            default:
                Util.vPrintln("Maxwell screwed up");
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
    
    public void findNearIds() throws GameActionException {
        // Util.vPrintln("Finding nearby robots");
        int sensorRadius = rc.getType().sensorRadiusSquared;
        RobotInfo[] sensable = rc.senseNearbyRobots(sensorRadius, rc.getTeam());
        for(RobotInfo robot : sensable) {
            int id = robot.getID();
            if(rc.canGetFlag(id)) {
                int flag = rc.getFlag(id);
                if(Comms.getIC(flag) == InformationCategory.NEW_ROBOT && !ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        Util.vPrintln("num id's found: " + ids.size());
    }

    public void checkForTowers() throws GameActionException {
        ids.removeIf(robotID -> !rc.canGetFlag(robotID));
        // int i = 0;
        for(int id : ids) {
            if(rc.canGetFlag(id)) {
                int flag = rc.getFlag(id);
                int dxdy = flag & Comms.BIT_MASK_COORDS;
                Comms.InformationCategory flagIC = Comms.getIC(flag);
                
                if((flagIC == Comms.InformationCategory.NEUTRAL_EC || flagIC == Comms.InformationCategory.ENEMY_EC)) {
                    int currReqInf = (int)  Math.exp(Comms.getInf(flag) * Math.log(Comms.INF_LOG_BASE)) * 4;
                    int[] currDxDy = Comms.getDxDy(dxdy);
                    RushFlag rushFlag = new RushFlag(currReqInf, currDxDy[0], currDxDy[1], flag);

                    ECflags.remove(rushFlag);
                    ECflags.add(rushFlag);

                    cleanUpCount = -1;
                    if (currentState == State.CLEANUP) {
                        currentState = stateStack.pop();
                    }
                } else if(flagIC == Comms.InformationCategory.FRIENDLY_EC) {
                    int[] currDxDy = Comms.getDxDy(dxdy);
                    RushFlag rushFlag = new RushFlag(0, currDxDy[0], currDxDy[1], 0);
                    ECflags.remove(rushFlag);
                }
            }
            // i++;
            // if (i >= 100) {
            //     break;
            // }
        }
        cleanUpCount++;
    }

    public boolean tryStartCleanup() throws GameActionException {
        Util.vPrintln("Cleanup count: " + cleanUpCount);
        if (cleanUpCount > Util.startCleanupThreshold && currentState != State.CLEANUP) {
            stateStack.push(currentState);
            currentState = State.CLEANUP;
            return true;
        }
        return false;
    }

    public boolean tryStartSavingForRush() throws GameActionException {
        if (!ECflags.isEmpty() && sendTroopsSemaphore == 0 && turnCount > lastRush + Util.minTimeBetweenRushes) {
            stateStack.push(currentState);
            currentState = State.SAVING_FOR_RUSH;
            return true;
        }
        return false;
    }

    public boolean tryStartMakingDefenders() throws GameActionException {
        return false;
        // if(checkNumDefenders() < Util.numDefenders) {
        //     stateStack.push(currentState);
        //     currentState = State.MAKING_DEFENDERS;
        //     return true;
        // }
        // return false;
    }

    public boolean tryStartRemovingBlockage() throws GameActionException {
        int num_enemies_near = 0;
        MapLocation currLoc = rc.getLocation();
        for (Direction dir : Util.directions) {
            MapLocation surroundingLoc = currLoc.add(dir);
            if (rc.onTheMap(surroundingLoc)){
                RobotInfo surroundingBot = rc.senseRobotAtLocation(surroundingLoc);
                if (surroundingBot != null && surroundingBot.getTeam() == enemy) {
                    num_enemies_near++;
                }
            }
        }
        Util.vPrintln("num enemies surrounding: " + num_enemies_near);
        if (num_enemies_near >= 4) {
            stateStack.push(currentState);
            currentState = State.REMOVING_BLOCKAGE;
            return true;
        }
        return false;
    }

    public boolean trySendARush() throws GameActionException {
        Util.vPrintln("building rush bots");
        int currFlag = rc.getFlag(rc.getID());
        toBuild = RobotType.POLITICIAN;
        // if (Comms.getIC(currFlag) == Comms.InformationCategory.ENEMY_EC) {
        //     toBuild = RobotType.MUCKRAKER;
        // }
        if(sendTroopsSemaphore != 1) {
            influence = 20;
        } else {
            influence = ECflags.peek().requiredInfluence;
        }

        if (influence >= currInfluence) {
            currentState = State.SAVING_FOR_RUSH;
            return false;
        }
        
        if(buildRobot(toBuild, influence)) {
            sendTroopsSemaphore--;
        }

        if (sendTroopsSemaphore == 0) {
            ECflags.remove();
            resetFlagOnNewTurn = true;
            currentState = stateStack.pop();
            lastRush = turnCount;
        }
        return true;
    }

    public boolean tryDefendARush() throws GameActionException {
        boolean enemy_near = false;
        int max_influence = 0;
        Team enemy = rc.getTeam().opponent();
        RobotInfo[] botsIn15 = rc.senseNearbyRobots(15, enemy);
        for (RobotInfo robot : botsIn15) {
           if (robot.getType() == RobotType.MUCKRAKER){
               enemy_near = true;
               if (robot.getInfluence() > max_influence){
                   max_influence = robot.getInfluence();
               }
           } 
        } 
        if (enemy_near) {
            Util.vPrintln("defending a rush");
            int num_robots = rc.senseNearbyRobots(15).length;
            int naive_influence = num_robots * max_influence;
            influence = Math.min(naive_influence + 10, 50);
            toBuild = RobotType.POLITICIAN;
            signalRobotType(Comms.SubRobotType.POL_EXPLORER);
            return true;
        }
        return false;
    }

    public void doMainStrategy() throws GameActionException {
        int slandererInfluence = Math.min(Math.max(100, currInfluence / 10), 1000);
        int normalInfluence = Math.max(50, currInfluence / 20);
        if (currRoundNum < Util.phaseOne) {
            Util.vPrintln("phase 1 default build troop behavior");
            switch(robotCounter % 9) {
                case 5:
                    toBuild = RobotType.MUCKRAKER;
                    influence = normalInfluence;
                    break;
                case 3: case 4: case 6: 
                    signalRobotType(Comms.SubRobotType.POL_EXPLORER);
                    toBuild = RobotType.POLITICIAN;
                    influence = normalInfluence;
                    break;
                case 8:
                    signalRobotType(Comms.SubRobotType.POL_BODYGUARD);
                    toBuild = RobotType.POLITICIAN;
                    influence = normalInfluence;
                    break;
                default:
                    toBuild = RobotType.SLANDERER;
                    influence = slandererInfluence;
                    break;
            }
        }
        else if(currRoundNum < Util.phaseTwo) {
            Util.vPrintln("phase 2 default build troop behavior");
            switch(robotCounter % 9) {
                case 0: case 2: case 6:
                    toBuild = RobotType.MUCKRAKER;
                    influence = normalInfluence;
                    break;
                case 1: case 3: case 5:
                    toBuild = RobotType.POLITICIAN;
                    influence = normalInfluence;
                    break;
                default:
                    toBuild = RobotType.SLANDERER;
                    influence = slandererInfluence;
                    break;
            }
        }
        else {
            Util.vPrintln("build troop behavior after 2000 rounds");
            toBuild = RobotType.MUCKRAKER;
            influence = normalInfluence;
        }
    }

    int checkNumDefenders() throws GameActionException {
        int count = 0;
        for(RobotInfo robot : friendlySensable) {
            if(rc.canGetFlag(robot.getID())) {
                int flag = rc.getFlag(robot.getID());
                Comms.InformationCategory flagIC = Comms.getIC(flag);
                if(flagIC == Comms.InformationCategory.ROBOT_TYPE) {
                    if(Comms.getSubRobotType(flag) == Comms.SubRobotType.POL_DEFENDER) {
                        count++;
                    }
                }
            }
        }

        Util.vPrintln("Defenders around: " + count);
        return count;
    }

    void signalRobotType(Comms.SubRobotType type) throws GameActionException {
        if (resetFlagOnNewTurn) {
            nextFlag = Comms.getFlag(Comms.InformationCategory.TARGET_ROBOT, type);
            resetFlagOnNewTurn = false;
        }
    }
}