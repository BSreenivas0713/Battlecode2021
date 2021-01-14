package musketeerplayersprint2;

import battlecode.common.*;

import musketeerplayersprint2.Util.*;
import musketeerplayersprint2.Comms.*;
import musketeerplayersprint2.Debug.*;

public strictfp class RobotPlayer {

    static Robot bot;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        Debug.init(rc);
        Nav.init(rc);

        RobotInfo[] sensableWithin2 = rc.senseNearbyRobots(2, rc.getTeam());

        switch (rc.getType()) {
            case ENLIGHTENMENT_CENTER: bot = new EC(rc);          break;
            case POLITICIAN: 
                for (RobotInfo robot : sensableWithin2) {
                    int botFlag = rc.getFlag(robot.getID());
                    Comms.InformationCategory flagIC = Comms.getIC(botFlag);
                    if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && robot.getTeam() == rc.getTeam()) {
                        Debug.println(Debug.info, "Flag for creation: " + botFlag);
                        switch(flagIC) {
                            case NEUTRAL_EC:
                            case ENEMY_EC:
                                int[] dxdy = Comms.getDxDy(botFlag);
                                MapLocation spawningLoc = robot.getLocation();
                                MapLocation enemyLoc = new MapLocation(dxdy[0] + spawningLoc.x - Util.dOffset, dxdy[1] + spawningLoc.y - Util.dOffset);
                                
                                bot = new RushPolitician(rc, enemyLoc);
                                break;
                            case TARGET_ROBOT:
                                Comms.SubRobotType type = Comms.getSubRobotType(botFlag);
                                switch(type) {
                                    case POL_EXPLORER:
                                        bot = new ExplorerPolitician(rc);
                                        break;
                                    case POL_CLEANUP:
                                        bot = new CleanupPolitician(rc);
                                        break;
                                    case POL_PROTECTOR:
                                        bot = new ProtectorPolitician(rc);
                                        break;
                                    case POL_DEFENDER:
                                        bot = new DefenderPolitician(rc);
                                        break;
                                    case POL_SPAWNKILL:
                                        if (rc.getEmpowerFactor(rc.getTeam(), 10) > Util.spawnKillThreshold) {
                                            bot = new SpawnKillPolitician(rc);
                                        } else {
                                            bot = new ExplorerPolitician(rc);
                                        }
                                        break;
                                }
                                break;
                            default:
                                break;
                        }
                    }

                    if(bot != null)
                        break;
                }

                if(bot != null)
                    break;
                //TODO: write rush muckraker.
                System.out.println("CRITICAL: Did not find flag directing type");
                bot = new ProtectorPolitician(rc);
                break;
            case SLANDERER:
                for (RobotInfo robot : sensableWithin2) { 
                    int botFlag = rc.getFlag(robot.getID());
                    Comms.InformationCategory flagIC = Comms.getIC(botFlag);
                    if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && robot.getTeam() == rc.getTeam()) {
                        Debug.println(Debug.info, "Flag for creation: " + botFlag);
                        if (flagIC == Comms.InformationCategory.SPECIFYING_SLANDERER_DIRECTION) {
                            Direction awayDirection = Comms.getAwayDirection(botFlag);
                            bot = new Slanderer(rc, awayDirection);
                            break;
                        }
                    }
                    if(bot != null)
                        break;
                }

                if(bot != null)
                    break;

                System.out.println("CRITICAL: Did not find flag directing type");
                bot = new Slanderer(rc, Util.randomDirection());
                break;
            case MUCKRAKER: 
                for (RobotInfo robot : sensableWithin2) {
                    int botFlag = rc.getFlag(robot.getID());
                    Comms.InformationCategory flagIC = Comms.getIC(botFlag);
                    if (flagIC == Comms.InformationCategory.ENEMY_EC_MUK) {
                        int[] dxdy = Comms.getDxDy(botFlag);
                        if (dxdy[0] == 0 && dxdy[0] == 0) {
                            Debug.println(Debug.info, "Making hunter mucker with no destination.");
                            bot = new HunterMuckracker(rc);
                        } else {
                            MapLocation spawningLoc = robot.getLocation();
                            MapLocation enemyLoc = new MapLocation(dxdy[0] + spawningLoc.x - Util.dOffset, dxdy[1] + spawningLoc.y - Util.dOffset);
                            Debug.println(Debug.info, "Making hunter mucker with destination " + enemyLoc + ".");
                            bot = new HunterMuckracker(rc, enemyLoc);
                        }
                        break;
                    }
                }    
                bot = new ExplorerMuckracker(rc);  
                break;
        }

        while (true) {
            try {
                bot.takeTurn();

                if (bot.changeTo != null) {
                    bot = bot.changeTo;
                }
                // Debug.println(Debug.info, "BC left at end: " + Clock.getBytecodesLeft());

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

                reset(rc);
            }
        }
    }

    // Last resort if a bot errors out in deployed code
    // Certain static variables might need to be cleared to ensure 
    // a successful return to execution.
    public static void reset(RobotController rc) throws GameActionException {
        switch (rc.getType()) {
            case ENLIGHTENMENT_CENTER:
                bot = new EC(rc);
                EC.currentState = EC.State.BUILDING_SLANDERERS;
                EC.ECflags.clear();
                break;
            case POLITICIAN: 
                bot = new ProtectorPolitician(rc);
                break;
            case SLANDERER:
                bot = new Slanderer(rc);
                break;
            case MUCKRAKER:
                bot = new ExplorerMuckracker(rc);
                break;
        }
    }
}
