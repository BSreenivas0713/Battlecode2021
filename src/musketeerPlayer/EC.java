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
        if (sendTroopsSemaphore > 0) {
            int currFlag = rc.getFlag(rc.getID());
            toBuild = RobotType.POLITICIAN;
            // if (Comms.getIC(currFlag) == Comms.InformationCategory.ENEMY_EC) {
            //     toBuild = RobotType.MUCKRAKER;
            // }
            influence = Math.max(50,rc.getInfluence()/30);
            int i = 0;
            Direction dir = null;
            while (i < 8) {
                dir = Util.randomDirection();
                if (rc.canBuildRobot(toBuild, dir, influence)) {
                    rc.buildRobot(toBuild, dir, influence);
                    robotCounter+=1;
                    break;
                }
                else {
                    i++;
                    break;
                }
            }

            sendTroopsSemaphore--;
            if (sendTroopsSemaphore == 0) {
                resetFlagOnNewTurn = true;
            }
        }
        else if (enemy_near) {
            int num_robots = rc.senseNearbyRobots(15).length;
            int naive_influence = num_robots * max_influence;
            influence = Math.min(naive_influence + 10, (int)(3 * rc.getInfluence()/4));
            int i = 0;
            Direction dir = null;
            while (i < 8) {
                dir = Util.randomDirection();
                if (rc.canBuildRobot(RobotType.POLITICIAN, dir, influence)) {
                    rc.buildRobot(RobotType.POLITICIAN, dir, influence);
                    robotCounter+=1;
                    break;
                }
                else {
                    i++;
                    break;
                }
            }
        }
        else if (rc.getEmpowerFactor(rc.getTeam(),0) > Util.spawnKillThreshold) {
            influence = 6*rc.getInfluence()/8;
            int i = 0;
            Direction dir = null;
            while (i < 8) {
                dir = Util.randomDirection();
                if (rc.canBuildRobot(RobotType.POLITICIAN, dir, influence)) {
                    rc.buildRobot(RobotType.POLITICIAN, dir, influence);
                    robotCounter+=1;
                    break;
                }
                else {
                    i++;
                    break;
                }
            }
        }
        else {
            int slandererInfluence = Math.max(100, rc.getInfluence() / 10);
            int normalInfluence = Math.max(50, rc.getInfluence() / 20);
            if (currRoundNum < 2000) {
                if(robotCounter % 9 == 0 || robotCounter % 9 == 2 || robotCounter % 9 == 4){
                    toBuild = RobotType.SLANDERER;
                    influence = slandererInfluence;
                }
                else if(robotCounter % 9 == 1 || robotCounter % 9 == 3 || robotCounter % 9 == 5){
                    toBuild = RobotType.POLITICIAN;
                    influence = normalInfluence;
                }
                else {
                    toBuild = RobotType.MUCKRAKER;
                    influence = normalInfluence;
                }
            } 
            else {
                toBuild = RobotType.MUCKRAKER;
                influence = normalInfluence;
            }
            int i = 0;
            Direction dir = null;
            while (i < 8) {
                dir = Util.randomDirection();
                if (rc.canBuildRobot(toBuild, dir, influence)) {
                    rc.buildRobot(toBuild, dir, influence);
                    robotCounter+=1;
                    break;
                }
                else {
                    i++;
                    break;
                }
            }
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
            int currFlag = ECflags.remove();
            sendTroopsSemaphore = 6;
            resetFlagOnNewTurn = false;
            setFlag(currFlag);
        }

    }
}