package musketeerplayer;
import battlecode.common.*;

import musketeerplayer.Util.*;

public class GolemPolitician extends Robot {
    static Direction main_direction;
    
    public GolemPolitician(RobotController r) {
        super(r);
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.POL_GOLEM);
    }
    
    public GolemPolitician(RobotController r, int dx, int dy) {
        super(r);
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.POL_GOLEM);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Util.vPrintln("I am a golem politician; current influence: " + rc.getInfluence());
        Util.vPrintln("Golem extra line");
        int min_attackable_conviction = (rc.getConviction()-10) / 3;
        int attackable_conviction = 0;
        for (RobotInfo robot : enemyAttackable) {
            attackable_conviction += robot.getConviction();
        }

        if (attackable_conviction >= min_attackable_conviction && rc.canEmpower(actionRadius)) {
            // Util.vPrintln("empowering...");
            rc.empower(actionRadius);
            Util.vPrintln("empowered");
            return;
        }

        boolean sensesEC = false;
        for (RobotInfo robot: friendlySensable) {
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                sensesEC = true;
                boolean seenCenter = false;
                for(RobotInfo secondRobot: friendlySensable) {
                    if(secondRobot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                        seenCenter = true;
                    }
                }
                if(!seenCenter) {
                    Direction toMove = rc.getLocation().directionTo(robot.getLocation());
                    tryMoveDest(toMove);
                }
            }
        }
        Util.vPrintln("Do I see an EC: " + sensesEC);
        RobotInfo enemyRobot = null;
        int maxEnemyConviction = min_attackable_conviction - 1;
        for (RobotInfo robot : enemySensable) {
            int enemyConviction = robot.getConviction();
            if(enemyConviction > maxEnemyConviction) {
                enemyRobot = robot;
                maxEnemyConviction = enemyConviction;
            }
        }
        if (enemyRobot != null) {
            Direction toMove = rc.getLocation().directionTo(enemyRobot.getLocation());
            tryMoveDest(toMove);
        }

        if (!sensesEC) {
            changeTo = new ExplorerPolitician(rc, dx, dy);
            return;
        }
    }
}