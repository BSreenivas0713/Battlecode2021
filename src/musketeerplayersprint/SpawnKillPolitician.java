package musketeerplayersprint;

import battlecode.common.*;

import musketeerplayersprint.Util.*;
import musketeerplayersprint.Debug.*;

public class SpawnKillPolitician extends Robot {
    static Direction main_direction;
    
    public SpawnKillPolitician(RobotController r) {
        super(r);
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, Comms.SubRobotType.POL_SPAWNKILL);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        Debug.println(Debug.info, "I am a spawn kill politician; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        if(rc.getEmpowerFactor(rc.getTeam(), 0) > Util.spawnKillThreshold) {
            if (rc.canEmpower(actionRadius)) {
                rc.empower(actionRadius);
            }
        } else {
            changeTo = new Politician(rc);
        }
    }
}