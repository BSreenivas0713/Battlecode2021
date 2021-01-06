package musketeerplayer;
import battlecode.common.*;

import musketeerplayer.Util.*;

public class EC extends Robot {
    static int robotCounter;

    public EC(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        System.out.println("AI current influence: " + rc.getInfluence());
        int currRoundNum = rc.getRoundNum();
        int currInfluence = rc.getInfluence();
        int biddingInfluence = currInfluence / 5;
        if (rc.canBid(biddingInfluence) && currRoundNum > 200) {
            rc.bid(biddingInfluence);
        }
        else {
            rc.bid(1);
        }

        RobotType toBuild;
        int influence;
        boolean enemy_near = false;
        int max_influence = 1;
        Team enemy = rc.getTeam().opponent();
        for (RobotInfo robot : rc.senseNearbyRobots(15, enemy)) {
           if (robot.getType() == RobotType.MUCKRAKER){
               enemy_near = true;
               if (robot.getInfluence() > max_influence){
                   max_influence = robot.getInfluence();
               }
           } 
        }
        if(enemy_near){
            int num_robots = rc.senseNearbyRobots(15).length;
            int naive_influence = num_robots * max_influence;
            influence = Math.min(naive_influence, (int)(3 * rc.getInfluence()/4));
            for (Direction dir : Util.directions) {
                if (rc.canBuildRobot(RobotType.POLITICIAN, dir, influence)) {
                    rc.buildRobot(RobotType.POLITICIAN, dir, influence);
                    robotCounter+=1;
                } else {
                    break;
                }
            }
        }
        else{
        if (currRoundNum > 500 && currRoundNum < 1500) {
            if(robotCounter % 4 == 0 || robotCounter % 4 == 1){
                toBuild = RobotType.SLANDERER;
                influence = 50;
            }
            else if (robotCounter % 4 == 3){
                toBuild = RobotType.POLITICIAN;
                influence = currInfluence / 5;
            }
            else{
                toBuild = RobotType.MUCKRAKER;
                influence = 20;
            }
        } else {
            if(robotCounter % 2 == 0){
                toBuild = RobotType.SLANDERER;
                influence = 50;
            }
            else{
                toBuild = RobotType.MUCKRAKER;
                influence = 50;
            }
        }
        for (Direction dir : Util.directions) {
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                rc.buildRobot(toBuild, dir, influence);
                robotCounter+=1;
            } else {
                break;
            }
        }
        }
    }
}