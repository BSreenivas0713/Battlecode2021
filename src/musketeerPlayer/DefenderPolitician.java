package musketeerplayer;
import battlecode.common.*;

import musketeerplayer.Util.*;

public class DefenderPolitician extends Robot {
    static Direction main_direction;
    static boolean hasSeenEnemy = false;
    
    public DefenderPolitician(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        System.out.println("I am a defender politician; current influence: " + rc.getInfluence());
        System.out.println("hasSeenEnemy: " + hasSeenEnemy);
        
        Team enemy = rc.getTeam().opponent();
        int sensingRadius = rc.getType().sensorRadiusSquared;
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        RobotInfo[] sensable = rc.senseNearbyRobots(sensingRadius, enemy);

        if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
            //System.out.println("empowering...");
            rc.empower(actionRadius);
            //System.out.println("empowered");
            return;
        }

        RobotInfo enemyRobot = null;
        int minDistance = Integer.MAX_VALUE;
        for (RobotInfo robot : sensable) {
            int dist = rc.getLocation().distanceSquaredTo(robot.location);
            if(dist < minDistance) {
                enemyRobot = robot;
                minDistance = dist;
                hasSeenEnemy = true;
            }
        }

        if(hasSeenEnemy && sensable.length == 0) {
            changeTo = new ExplorerPolitician(rc, dx, dy);
            return;
        }
        
        if (enemyRobot != null) {
            Direction toMove = rc.getLocation().directionTo(enemyRobot.getLocation());
            tryMoveDest(toMove);
        }
    }
}