package musketeerplayersprint2;

import battlecode.common.*;

import musketeerplayersprint2.Util.*;
import musketeerplayersprint2.Debug.*;

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
            int radius = Math.max(1, home.distanceSquaredTo(rc.getLocation()));
            if (rc.canEmpower(radius)) {
                Debug.println(Debug.info, "Empowered with radius: " + radius);
                Debug.setIndicatorLine(rc.getLocation(), home, 255, 150, 50);
                rc.empower(radius);
            } else {
                changeTo = new ProtectorPolitician(rc);
            }
        } else {
            changeTo = new ProtectorPolitician(rc);
        }
    }
}