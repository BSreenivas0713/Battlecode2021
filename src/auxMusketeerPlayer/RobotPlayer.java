package auxMusketeerPlayer;
import battlecode.common.*;

public strictfp class RobotPlayer {

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        Robot bot = null;
        switch (rc.getType()) {
            case ENLIGHTENMENT_CENTER: bot = new EC(rc);          break;
            case POLITICIAN:           bot = new Politician(rc);  break;
            case SLANDERER:            bot = new Slanderer(rc);   break;
            case MUCKRAKER:            bot = new Muckracker(rc);  break;
        }
        RobotType prev = rc.getType();

        while (true) {
            try {
                RobotType curr = rc.getType();
                if (prev != curr) {
                    if (prev == RobotType.SLANDERER && curr == RobotType.POLITICIAN) {
                        bot = new Politician(rc);
                    }
                    prev = curr;
                }
                bot.takeTurn();

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }
}
