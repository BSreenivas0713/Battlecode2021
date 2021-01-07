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

        System.out.println("I am a spawn kill politician; current influence: " + rc.getInfluence());
        System.out.println("current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        int actionRadius = rc.getType().actionRadiusSquared;
        if (turnCount == 1 && rc.canEmpower(actionRadius)) {
            rc.empower(actionRadius);
        }
    }
}