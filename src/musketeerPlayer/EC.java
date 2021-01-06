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
        int biddingInfluence = currInfluence / 10;
        if (rc.canBid(biddingInfluence) && currRoundNum > 200) {
            rc.bid(biddingInfluence);
        }
        else {
            rc.bid(1);
        }

        RobotType toBuild;
        int influence;
        if (currRoundNum > 500) {
            if(robotCounter % 3 == 0){
                toBuild = RobotType.SLANDERER;
                influence = 50;
            }
            else if (robotCounter % 3 == 1){
                toBuild = RobotType.POLITICIAN;
                influence = currInfluence / 10;
            }
            else{
                toBuild = RobotType.MUCKRAKER;
                influence = 1;
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