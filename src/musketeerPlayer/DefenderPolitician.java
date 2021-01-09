package musketeerplayer;
import battlecode.common.*;

import musketeerplayer.Util.*;

public class DefenderPolitician extends Robot {
    static Direction main_direction;
    static boolean hasSeenEnemy = false;
    
    public DefenderPolitician(RobotController r) {
        super(r);
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.POL_DEFENDER);
    }
    
    public DefenderPolitician(RobotController r, int dx, int dy) {
        super(r, dx, dy);
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.POL_DEFENDER);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Util.vPrintln("I am a defender politician; current influence: " + rc.getInfluence());
        Util.vPrintln("hasSeenEnemy: " + hasSeenEnemy);
        

        if (enemyAttackable.length != 0 && rc.canEmpower(actionRadius)) {
            //Util.vPrintln("empowering...");
            rc.empower(actionRadius);
            //Util.vPrintln("empowered");
            return;
        }

        RobotInfo enemyRobot = null;
        int minDistance = Integer.MAX_VALUE;
        for (RobotInfo robot : enemySensable) {
            int dist = rc.getLocation().distanceSquaredTo(robot.location);
            if(dist < minDistance) {
                enemyRobot = robot;
                minDistance = dist;
                hasSeenEnemy = true;
            }
        }

        if(hasSeenEnemy && enemySensable.length == 0) {
            changeTo = new ExplorerPolitician(rc, dx, dy);
            return;
        }
        
        if (enemyRobot != null) {
            Direction toMove = rc.getLocation().directionTo(enemyRobot.getLocation());
            tryMoveDest(toMove);
        }
    }
}