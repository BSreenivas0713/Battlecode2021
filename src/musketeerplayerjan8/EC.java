package musketeerplayerjan8;
import battlecode.common.*;

import musketeerplayerjan8.Comms.*;
import musketeerplayerjan8.Util.*;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.stream.Stream;


public class EC extends Robot {
    static enum State {
        PHASE1,
        PHASE2,
        RUSHING,
        SAVING_FOR_RUSH
    };

    static int robotCounter;
    static int sendTroopsSemaphore = 0;
    static RobotType toBuild;
    static int influence;

    static int currRoundNum;
    static int currInfluence;
    static boolean needToBuild;

    static ArrayList<Integer> ids; // TODO USE HASHSET
    static ArrayDeque<Integer> ECflags;
    static ArrayDeque<Integer> ECdxdys;

    static State currentState;
    static int requiredInfluence;

    static boolean needToMakeBodyguard = false;

    // TODO: Better slanderer pathfinding. Wallscraping muckrakers. Better defense
    public EC(RobotController r) {
        super(r);
        ids = new ArrayList<Integer>();
        ECflags = new ArrayDeque<Integer>();
        ECdxdys = new ArrayDeque<Integer>();
        currentState = State.PHASE1;
    }

    public boolean buildRobot(RobotType toBuild, int influence) throws GameActionException {
        if(Util.verbose) System.out.println("building robot type: " + toBuild + " influence: " + influence);
        Direction main_direction = Util.randomDirection();
        int num_direction = 8;
        while(num_direction != 0) {
            if (rc.canBuildRobot(toBuild, main_direction, influence)) {
                if(Util.verbose) System.out.println("built robot");
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
        
        initializeGlobals();

        if (Util.verbose) System.out.println("I am a " + rc.getType() + "; current influence: " + currInfluence);
        if (Util.verbose) System.out.println("current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        if (Util.verbose) System.out.println("num of ec's found: " + ECflags.size());
        if (Util.verbose) System.out.println("state: " + currentState);

        findNearIds();
        checkForTowers();

        int biddingInfluence = currInfluence / 20;
        if (rc.canBid(biddingInfluence) && currRoundNum > 1000) {
            rc.bid(biddingInfluence);
        }

        // if (rc.getEmpowerFactor(rc.getTeam(),0) > Util.spawnKillThreshold) {
        //     if (Util.verbose) System.out.println("spawn killing politicians");
        //     influence = 6*rc.getInfluence()/8;
        //     toBuild = RobotType.POLITICIAN;
        // }
        switch(currentState) {
            case PHASE1:
                // System.out.println("Phase1 state");
                if(currInfluence >= 150 && robotCounter % 5 == 0) {
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
                // System.out.println("Phase2 state");
                if(needToMakeBodyguard) {
                    toBuild = RobotType.POLITICIAN;
                    influence = currInfluence;
                    signalRobotType(Comms.SubRobotType.POL_BODYGUARD);
                } else if(7 * currInfluence / 8 >= 150 && robotCounter % 5 == 0) {
                    toBuild = RobotType.SLANDERER;
                    influence = Math.min(1000, 7 * currInfluence / 8);
                } else {
                    toBuild = RobotType.MUCKRAKER;
                    influence = 1;
                }
                
                if(needToBuild) {
                    buildRobot(toBuild, influence);
                }

                // if(createDefenderIfNeeded()) {} 
                // else if(tryDefendARush()) {}
                // else {doMainStrategy();}
                tryStartSavingForRush();
                break;
            case RUSHING:
                trySendARush();
                break;
            case SAVING_FOR_RUSH:
                toBuild = RobotType.MUCKRAKER;
                influence = 1;
                
                if(needToBuild) {
                    buildRobot(toBuild, influence);
                }

                if(requiredInfluence < currInfluence) {
                    sendTroopsSemaphore = 1;
                    resetFlagOnNewTurn = false;
                    nextFlag = ECflags.peek();
                    currentState = State.RUSHING;
                }
                break;
        }
    }

    public void initializeGlobals() throws GameActionException {
        toBuild = null;
        influence = 0;
        needToBuild = true;
        currRoundNum = rc.getRoundNum();
        currInfluence = rc.getInfluence();

        if(currentState == State.PHASE1 && turnCount > Util.phaseOne) {
            currentState = State.PHASE2;
        }
    }

    public void findNearIds() throws GameActionException {
        if(Util.verbose) System.out.println("Finding nearby robots");
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
        if (Util.verbose) System.out.println("num id's found: " + ids.size());
    }

    public void checkForTowers() throws GameActionException {
        ids.removeIf(robotID -> !rc.canGetFlag(robotID));
        for(int id : ids) {
            if(rc.canGetFlag(id)) {
                int flag = rc.getFlag(id);
                int dxdy = flag & Comms.BIT_MASK_COORDS;
                Comms.InformationCategory flagIC = Comms.getIC(flag);
                if((flagIC == Comms.InformationCategory.NEUTRAL_EC || flagIC == Comms.InformationCategory.ENEMY_EC) &&
                    !ECdxdys.contains(dxdy)) {
                    ECflags.add(flag);
                    ECdxdys.add(dxdy);
                }

                if(!ECdxdys.isEmpty() && ECdxdys.peek() == dxdy) {
                    ECflags.remove();
                    ECflags.offerFirst(flag);
                }
            }
        }
    }

    public void tryStartSavingForRush() throws GameActionException {
        if (!ECflags.isEmpty() && sendTroopsSemaphore == 0) {
            int currFlag = ECflags.peek();
            requiredInfluence = (int)  Math.exp(Comms.getInf(currFlag) * Math.log(Comms.INF_LOG_BASE)) * 4 + 100;
            currentState = State.SAVING_FOR_RUSH;
        }
    }

    public boolean trySendARush() throws GameActionException {
        if (Util.verbose) System.out.println("building rush bots");
        int currFlag = rc.getFlag(rc.getID());
        toBuild = RobotType.POLITICIAN;
        // if (Comms.getIC(currFlag) == Comms.InformationCategory.ENEMY_EC) {
        //     toBuild = RobotType.MUCKRAKER;
        // }
        if(sendTroopsSemaphore != 1) {
            influence = 20;
        } else {
            influence = (int) Math.ceil(Math.exp(Comms.getInf(currFlag) * Math.log(Comms.INF_LOG_BASE)) * 4);
        }

        
        if(buildRobot(toBuild, influence)) {
            sendTroopsSemaphore--;
        }

        if (sendTroopsSemaphore == 0) {
            ECflags.remove();
            ECdxdys.remove();
            resetFlagOnNewTurn = true;
            currentState = State.PHASE2;
        }
        return true;
    }

    public boolean createDefenderIfNeeded() throws GameActionException {
        Direction missingDefenderDirection = checkMissingDefender();
        if(turnCount >=Util.timeBeforeDefenders && robotCounter % Util.defenderPoliticianFrequency == 4 && 
            missingDefenderDirection != null) {
            if (Util.verbose) System.out.println("building defender politician in direction: " + missingDefenderDirection);
            toBuild = RobotType.POLITICIAN;
            influence = Math.max(50, currInfluence / 20);
            
            if (rc.canBuildRobot(toBuild, missingDefenderDirection, influence)) {
                signalRobotType(Comms.SubRobotType.POL_DEFENDER);
                rc.buildRobot(toBuild, missingDefenderDirection, influence);
                robotCounter += 1;
                needToBuild = false;
            }
            return true;
        }
        return false;
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
            if (Util.verbose) System.out.println("defending a rush");
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
            if (Util.verbose) System.out.println("phase 1 default build troop behavior");
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
            if (Util.verbose) System.out.println("phase 2 default build troop behavior");
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
            if (Util.verbose) System.out.println("build troop behavior after 2000 rounds");
            toBuild = RobotType.MUCKRAKER;
            influence = normalInfluence;
        }
    }

    Direction checkMissingDefender() throws GameActionException {
        for(Direction dir : Util.defenderDirs) {
            MapLocation loc = rc.getLocation().add(dir);
            if(rc.onTheMap(loc)) {
                RobotInfo robot = rc.senseRobotAtLocation(loc);
                if(robot == null)
                    return dir;
            }
        }

        return null;
    }

    void signalRobotType(Comms.SubRobotType type) throws GameActionException {
        if (resetFlagOnNewTurn) {
            nextFlag = Comms.getFlag(Comms.InformationCategory.SUB_ROBOT, type);
            resetFlagOnNewTurn = false;
        }
    }
}