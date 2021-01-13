package auxExplorerPlayer;
import battlecode.common.*;

import auxExplorerPlayer.Util.*;

public class Politician extends Robot {
    static Direction main_direction;
    
    public Politician(RobotController r) {
        super(r);
        try {
            if (rc.canSetFlag(0)) {
                rc.setFlag(0);
            }
        }
        catch (Exception e) {
            System.out.println(rc.getType() + " Exception");
            e.printStackTrace();
        }
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        System.out.println("I am a " + rc.getType() + "; current influence: " + rc.getInfluence());
        System.out.println("current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        if(main_direction == null){
            main_direction = Util.randomDirection();
        }
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        RobotInfo[] neutrals = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        if ((attackable.length != 0 || neutrals.length != 0) && rc.canEmpower(actionRadius)) {
            //System.out.println("empowering...");
            rc.empower(actionRadius);
            //System.out.println("empowered");
            return;
        }

        int sensingRadius = rc.getType().sensorRadiusSquared;
        RobotInfo[] sensable = rc.senseNearbyRobots(sensingRadius, enemy);
        RobotInfo powerful = null;
        int max_influence = 0;
        for (RobotInfo robot : sensable) {
            int currInfluence = robot.getInfluence();
            if (robot.getType() == RobotType.MUCKRAKER && currInfluence > max_influence) {
                powerful = robot;
                max_influence = currInfluence;
            }
        }
        
        if (powerful != null) {
            Direction toMove = Util.findDirection(powerful.getLocation(), rc.getLocation());
            tryMove(toMove);
        }

        RobotInfo[] within6 = rc.senseNearbyRobots(6, rc.getTeam());
        for (RobotInfo robot : within6) {
            if (robot.getType() == RobotType.POLITICIAN && rc.getFlag(robot.getID()) == 1) {
                System.out.println("within radius 6");
                return;
            }
        }
        RobotInfo[] friendlySensable = rc.senseNearbyRobots(sensingRadius, rc.getTeam());
        RobotInfo bestSlanderer = null;
        max_influence = 0;
        for (RobotInfo robot : friendlySensable) {
            if (robot.getType() == RobotType.POLITICIAN && rc.getFlag(robot.getID()) == 1 && robot.getInfluence() > max_influence) {
                System.out.println("within sensing radius but not 6");
                max_influence = robot.getInfluence();
                bestSlanderer = robot;
            }
        }

        if (bestSlanderer != null) {
            Direction toMove = Util.findDirection(bestSlanderer.getLocation(), rc.getLocation());
            tryMove(toMove);
        }

        while (!tryMove(main_direction) && rc.isReady()){
            main_direction = Util.randomDirection();
        }
    }
}