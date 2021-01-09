package musketeerplayer;
import battlecode.common.*;

import musketeerplayer.Util.*;

public class SpawnKillPolitician extends Robot {
    static Direction main_direction;
    
    public SpawnKillPolitician(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        if (Util.verbose) System.out.println("I am a spawn kill politician; current influence: " + rc.getInfluence());
        if (Util.verbose) System.out.println("current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        if(rc.getEmpowerFactor(rc.getTeam(), 0) > Util.spawnKillThreshold) {
            if (rc.canEmpower(actionRadius)) {
                rc.empower(actionRadius);
            }
        } else {
            changeTo = new Politician(rc);
        }
    }
}