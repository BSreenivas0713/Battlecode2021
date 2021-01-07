package musketeerplayer;
import battlecode.common.*;
import musketeerplayer.Util.*;
import musketeerplayer.Comms.*;

public strictfp class RobotPlayer {

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        Robot bot = null;

        switch (rc.getType()) {
            case ENLIGHTENMENT_CENTER: bot = new EC(rc);          break;
            case POLITICIAN: 
                int sensorRadius = rc.getType().sensorRadiusSquared;
                RobotInfo[] sensable = rc.senseNearbyRobots(sensorRadius, rc.getTeam());
                boolean willRush = false;
                for (RobotInfo robot : sensable) {
                    int botFlag = rc.getFlag(robot.getID());
                    if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && botFlag > 1000000) {
                        int[] dxdy = Comms.getDxDy(botFlag);
                        MapLocation spawningLoc = robot.getLocation();
                        MapLocation enemyLoc = new MapLocation(dxdy[0] + spawningLoc.x - Util.dOffset, dxdy[1] + spawningLoc.y - Util.dOffset);
                        
                        bot = new RushPolitician(rc, enemyLoc);
                        willRush = true;
                        break;
                    }
                    //TODO: get rush politician to work, write rush muckraker.
                    //make sure when a flag is set, spawning rush politicians is not the ONLY thing we do (soln: dont set flag always)
                }
                if (willRush) {
                    break;
                }

                if (rc.getEmpowerFactor(rc.getTeam(), 0) > Util.spawnKillThreshold) {
                    bot = new SpawnKillPolitician(rc);
                    break;
                }
                else {
                    boolean isExplorer = rc.getRoundNum() % 3 != 0;
                    if (isExplorer) {
                        bot = new ExplorerPolitician(rc);
                        break;
                    }
                    else {
                        bot = new Politician(rc);
                        break;
                    }
                }
            case SLANDERER:            bot = new Slanderer(rc);   break;
            case MUCKRAKER:            bot = new Muckracker(rc);  break;
        }
        RobotType prev = rc.getType();

        while (true) {
            try {
                RobotType curr = rc.getType();
                if (prev != curr) {
                    if (prev == RobotType.SLANDERER && curr == RobotType.POLITICIAN) {
                        bot = new Politician(rc, bot.dx, bot.dy);
                    }
                    prev = curr;
                }
                bot.takeTurn();

                if (bot.changeTo != null) {
                    bot = bot.changeTo;
                }
                // System.out.println("BC left at end: " + Clock.getBytecodesLeft());

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }
}
