package musketeerplayer;
import battlecode.common.*;

import musketeerplayer.Comms.*;
import musketeerplayer.Util.*;
import java.util.ArrayList;
import java.util.ArrayDeque;


public class EC extends Robot {
    static int robotCounter;
    static int sendTroopsSemaphore = 0;

    static ArrayList<Integer> ids;
    static ArrayDeque<Integer> ECflags;

    public EC(RobotController r) {
        super(r);
        ids = new ArrayList<Integer>();
        ECflags = new ArrayDeque<Integer>();
    }

    public void buildRobot(RobotType toBuild, int influence) throws GameActionException {
        Direction main_direction = Util.randomDirection();
        int num_direction = 8;
        while(num_direction != 0) {
            if (rc.canBuildRobot(toBuild, main_direction, influence)) {
                rc.buildRobot(toBuild, main_direction, influence);
                robotCounter += 1;
                break;
            }
            main_direction = main_direction.rotateRight();
            num_direction--;
        }
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        System.out.println("I am a " + rc.getType() + "; current influence: " + rc.getInfluence());
        System.out.println("current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));
        System.out.println("num of ec's found: " + ECflags.size());
        
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
        System.out.println("num id's found: " + ids.size());

        int currRoundNum = rc.getRoundNum();
        int currInfluence = rc.getInfluence();
        int biddingInfluence = currInfluence / 10;
        if (rc.canBid(biddingInfluence) && currRoundNum > 500) {
            rc.bid(biddingInfluence);
        }
        else if (rc.canBid(currInfluence / 30)) {
            rc.bid(currInfluence / 30);
        }

        RobotType toBuild;
        int influence;
        boolean needToBuild = true;
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

        // if (rc.getEmpowerFactor(rc.getTeam(),0) > Util.spawnKillThreshold) {
        //     System.out.println("spawn killing politicians");
        //     influence = 6*rc.getInfluence()/8;
        //     toBuild = RobotType.POLITICIAN;
        // }
        Direction missingDefenderDirection = checkMissingDefender();
        if(robotCounter % Util.defenderPoliticianFrequency == 4 && 
            missingDefenderDirection != null) {
            System.out.println("building defender politician");
            toBuild = RobotType.POLITICIAN;
            influence = Math.max(50, rc.getInfluence() / 20);
            
            if (rc.canBuildRobot(toBuild, missingDefenderDirection, influence)) {
                signalRobotType(Comms.SubRobotType.POL_DEFENDER);
                rc.buildRobot(toBuild, missingDefenderDirection, influence);
                robotCounter += 1;
                needToBuild = false;
            }
        }
        else if (enemy_near) {
            System.out.println("defending a rush");
            int num_robots = rc.senseNearbyRobots(15).length;
            int naive_influence = num_robots * max_influence;
            influence = Math.min(naive_influence + 10, 50);
            toBuild = RobotType.POLITICIAN;
            signalRobotType(Comms.SubRobotType.POL_DEFENDER);
        }
        else if (sendTroopsSemaphore > 0) {
            System.out.println("building rush bots");
            int currFlag = rc.getFlag(rc.getID());
            toBuild = RobotType.POLITICIAN;
            // if (Comms.getIC(currFlag) == Comms.InformationCategory.ENEMY_EC) {
            //     toBuild = RobotType.MUCKRAKER;
            // }
            influence = Math.max(50,rc.getInfluence()/30);

            sendTroopsSemaphore--;
            if (sendTroopsSemaphore == 0) {
                ECflags.remove();
                resetFlagOnNewTurn = true;
            }
        }
        else {
            int slandererInfluence = Math.min(Math.max(100, rc.getInfluence() / 10), 1000);
            int normalInfluence = Math.max(50, rc.getInfluence() / 20); 
            if (currRoundNum < Util.phaseOne) {
                System.out.println("phase 1 default build troop behavior");
                switch(robotCounter % 9) {
                    // case 0:
                    //     toBuild = RobotType.MUCKRAKER;
                    //     influence = normalInfluence;
                    //     break;
                    case 3: case 4: case 5: case 6: 
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
                System.out.println("phase 2 default build troop behavior");
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
                System.out.println("build troop behavior after 2000 rounds");
                toBuild = RobotType.MUCKRAKER;
                influence = normalInfluence;
            }
        }
        
        if(needToBuild) {
            buildRobot(toBuild, influence);
        }

        for(int id : ids) {
            if(rc.canGetFlag(id)) {
                int flag = rc.getFlag(id);
                Comms.InformationCategory flagIC = Comms.getIC(flag);
                if(flag > Comms.MIN_FLAG_MESSAGE && !ECflags.contains(flag) && 
                (flagIC == Comms.InformationCategory.NEUTRAL_EC || flagIC == Comms.InformationCategory.ENEMY_EC)) {
                    ECflags.add(flag);
                }
            }
        }

        if (!ECflags.isEmpty() && sendTroopsSemaphore == 0) {
            int currFlag = ECflags.peek();
            sendTroopsSemaphore = 6;
            resetFlagOnNewTurn = false;
            nextFlag = currFlag;
            // setFlag(currFlag);
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
        nextFlag = Comms.getFlag(Comms.InformationCategory.SUB_ROBOT, type);
    }
}