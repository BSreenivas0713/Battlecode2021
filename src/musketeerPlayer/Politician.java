package musketeerplayer;
import battlecode.common.*;

import musketeerplayer.Util.*;

public class Politician extends Robot {
    static Direction main_direction;
    static boolean   toDetonate = false;

    
    public Politician(RobotController r) {
        super(r);
    }

    public Politician(RobotController r, int currDx, int currDy) {
        super(r, currDx, currDy);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        System.out.println("I am a " + rc.getType() + "; current influence: " + rc.getInfluence());
        System.out.println("current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        Team enemy = rc.getTeam().opponent();
        int sensingRadius = rc.getType().sensorRadiusSquared;
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        RobotInfo[] neutrals = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        RobotInfo[] sensable = rc.senseNearbyRobots(sensingRadius, enemy);
        RobotInfo[] within6 = rc.senseNearbyRobots(6, rc.getTeam());
        RobotInfo[] friendlySensable = rc.senseNearbyRobots(sensingRadius, rc.getTeam());

        if(toDetonate) {
            Direction toMove = Util.randomDirection();
            for(RobotInfo robot: neutrals) {
                if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    if(rc.canEmpower(actionRadius)) {
                        rc.empower(actionRadius);
                    } else {
                        toMove = Util.findDirection(robot.getLocation(), rc.getLocation());
                    }
                }
            }

            tryMoveDest(toMove);
        } else {
            if(main_direction == null){
                main_direction = Util.randomDirection();
            }
            if ((attackable.length != 0 || neutrals.length != 0) && rc.canEmpower(actionRadius)) {
                //System.out.println("empowering...");
                rc.empower(actionRadius);
                //System.out.println("empowered");
                return;
            }
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
                tryMoveDest(toMove);
            }
    
            for (RobotInfo robot : within6) {
                if (robot.getType() == RobotType.POLITICIAN && rc.getFlag(robot.getID()) == 1) {
                    System.out.println("within radius 6");
                    return;
                }
            }

            RobotInfo bestSlanderer = null;
            max_influence = 0;
            for (RobotInfo robot : friendlySensable) {
                if (robot.getType() == RobotType.POLITICIAN && rc.getFlag(robot.getID()) == 1 && robot.getInfluence() > max_influence) {
                    System.out.println("within sensing radius but not 6");
                    max_influence = robot.getInfluence();
                    bestSlanderer = robot;
                }
                
                if(robot.getType() == RobotType.POLITICIAN && Comms.getIC(rc.getFlag(robot.getID())) == Comms.InformationCategory.DETONATE) {
                    for(RobotInfo robot2: neutrals) {
                        if(robot2.getType() == RobotType.ENLIGHTENMENT_CENTER){
                            toDetonate = true;
                            break;
                        }
                    }

                    if(toDetonate)
                        break;
                }
            }
    
            if (bestSlanderer != null) {
                Direction toMove = Util.findDirection(bestSlanderer.getLocation(), rc.getLocation());
                tryMoveDest(toMove);
            }
            
    
            RobotInfo weakest = null;
            int min_influence = 0;
            for (RobotInfo robot : sensable) {
                int currInfluence = robot.getInfluence();
                if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && currInfluence < min_influence) {
                    weakest = robot;
                    min_influence = currInfluence;
                }
            }
            
            if (weakest != null) {
                Direction toMove = Util.findDirection(weakest.getLocation(), rc.getLocation());
                tryMoveDest(toMove);
            }
            
            while (!tryMove(main_direction) && rc.isReady()){
                main_direction = Util.randomDirection();
            }
        }

        broadcastECLocation();
    }
}