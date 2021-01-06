package auxmusketeerplayer;
import battlecode.common.*;

import auxmusketeerplayer.Util.*;

public class EC extends Robot {
    static int robotCounter;

    public EC(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        System.out.println("aux AI current influence: " + rc.getInfluence());
        int currRoundNum = rc.getRoundNum();
        int currInfluence = rc.getInfluence();
        int biddingInfluence = currInfluence / 10;
        if (rc.canBid(biddingInfluence) && currRoundNum > 500) {
            rc.bid(biddingInfluence);
        }
        else {
            rc.bid(1);
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
        if(enemy_near) {
            int num_robots = rc.senseNearbyRobots(15).length;
            int naive_influence = num_robots * max_influence;
            influence = Math.min(naive_influence + 10, (int)(3 * rc.getInfluence()/4));
            for (Direction dir : Util.directions) {
                if (rc.canBuildRobot(RobotType.POLITICIAN, dir, influence)) {
                    rc.buildRobot(RobotType.POLITICIAN, dir, influence);
                    robotCounter+=1;
                } else {
                    break;
                }
            }
        }
        else {
            if (currRoundNum < 1000) {
                if(robotCounter % 10 == 0){
                    toBuild = RobotType.SLANDERER;
                    influence = 100;
                }
                else {
                    toBuild = RobotType.MUCKRAKER;
                    influence = 50;
                }
            } else if (currRoundNum < 2000) {
                if (robotCounter % 5 == 0 || robotCounter % 5 == 1) {
                    toBuild = RobotType.MUCKRAKER;
                    influence = 50;
                } else {
                    toBuild = RobotType.SLANDERER;
                    influence = 50;
                }
            } else {
                toBuild = RobotType.SLANDERER;
                influence = 50;
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