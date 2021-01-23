package naivesuey;

import battlecode.common.*;

import naivesuey.Util.*;
import naivesuey.Comms.*;
import naivesuey.Debug.*;

public strictfp class RobotPlayer {

    static Robot bot;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        Debug.init(rc);
        Nav.init(rc);

        RobotInfo[] sensableWithin2 = rc.senseNearbyRobots(2, rc.getTeam());
        boolean foundHome = false;

        switch (rc.getType()) {
            case ENLIGHTENMENT_CENTER: bot = new EC(rc);          break;
            case POLITICIAN: 
                for (RobotInfo robot : sensableWithin2) {
                    int botFlag = rc.getFlag(robot.getID());
                    Comms.InformationCategory flagIC = Comms.getIC(botFlag);
                    if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && robot.getTeam() == rc.getTeam()) {
                        foundHome = true;
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
                                        Direction closestWall = Comms.getDirectionFromSubRobotTypeFlag(botFlag);
                                        Debug.println(Debug.info, "building protector with closest: " + closestWall);
                                        if (closestWall != Direction.CENTER) {
                                            bot = new LatticeProtector(rc, closestWall);
                                        }
                                        else {
                                            bot = new LatticeProtector(rc);
                                        }
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
                if (foundHome) {
                    bot = new LatticeProtector(rc);
                }
                else {
                    bot = new ExplorerPolitician(rc, null, -1);
                }
                break;
            case SLANDERER:
                bot = new DiagonalSlanderer(rc);
                break;
            case MUCKRAKER: 
                for (RobotInfo robot : sensableWithin2) {
                    int botFlag = rc.getFlag(robot.getID());
                    Comms.InformationCategory flagIC = Comms.getIC(botFlag);
                    if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && robot.getTeam() == rc.getTeam()) {
                        Debug.println(Debug.info, "Flag for creation: " + botFlag);
                        switch (flagIC) {
                            case ENEMY_EC_MUK:
                                int[] dxdy = Comms.getDxDy(botFlag);
                                if (dxdy[0] == 0 && dxdy[1] == 0) {
                                    bot = new HunterMuckracker(rc);
                                } else {
                                    MapLocation spawningLoc = robot.getLocation();
                                    MapLocation enemyLoc = new MapLocation(dxdy[0] + spawningLoc.x - Util.dOffset, dxdy[1] + spawningLoc.y - Util.dOffset);
                                    bot = new HunterMuckracker(rc, enemyLoc);
                                }
                                break;
                            case TARGET_ROBOT:
                                Comms.SubRobotType subType = Comms.getSubRobotTypeScout(botFlag);
                                if (subType == Comms.SubRobotType.MUC_SCOUT) {
                                    Direction dirToMove = Comms.getScoutDirection(botFlag);
                                    bot = new ScoutMuckraker(rc, dirToMove);
                                } else if (subType == Comms.SubRobotType.MUC_SURVIVAL) {
                                    bot = new SurvivalMuckracker(rc);
                                }
                                
                                break;
                            default:
                                break;
                        }
                    }
                    if (bot != null) {
                        break;
                    }
                }
                if(bot != null) {
                    break;
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
                EC.currentState = EC.State.CHILLING;
                EC.ECflags.clear();
                break;
            case POLITICIAN: 
                bot = new LatticeProtector(rc);
                break;
            case SLANDERER:
                bot = new DiagonalSlanderer(rc);
                break;
            case MUCKRAKER:
                bot = new ExplorerMuckracker(rc);
                break;
        }
    }
}
