package musketeerplayersprint;

import battlecode.common.*;

import musketeerplayersprint.Util.*;
import musketeerplayersprint.Comms.*;
import musketeerplayersprint.Debug.*;

public strictfp class RobotPlayer {

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        Robot bot = null;

        switch (rc.getType()) {
            case ENLIGHTENMENT_CENTER: bot = new EC(rc);          break;
            case POLITICIAN: 
                // if (rc.getEmpowerFactor(rc.getTeam(), 0) > Util.spawnKillThreshold) {
                //     bot = new SpawnKillPolitician(rc);
                //     break;
                // }

                int sensorRadius = rc.getType().sensorRadiusSquared;
                RobotInfo[] sensableWithin2 = rc.senseNearbyRobots(2, rc.getTeam());
                boolean botCreated = false;
                for (RobotInfo robot : sensableWithin2) {
                    int botFlag = rc.getFlag(robot.getID());
                    Comms.InformationCategory flagIC = Comms.getIC(botFlag);
                    if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
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
                                    case POL_DEFENDER:
                                        bot = new DefenderPolitician(rc);
                                        break;
                                    case POL_EXPLORER:
                                        bot = new ExplorerPolitician(rc);
                                        break;
                                    case POL_BODYGUARD:
                                        bot = new Politician(rc);
                                        break;
                                    case POL_CLEANUP:
                                        bot = new CleanupPolitician(rc);
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
                Debug.println(Debug.critical, "LOGICAL ERROR: Did not find flag directing type");
                bot = new Politician(rc);
                break;
            case SLANDERER:            bot = new Slanderer(rc);   break;
            case MUCKRAKER:            bot = new Muckracker(rc);  break;
        }
        RobotType prev = rc.getType();

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
            }
        }
    }
}
