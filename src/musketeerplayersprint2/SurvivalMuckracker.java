package musketeerplayersprint2;
import battlecode.common.*;

import musketeerplayersprint2.Util.*;
import musketeerplayersprint2.Debug.*;
import musketeerplayersprint2.fast.FastIterableLocSet;

public class SurvivalMuckracker extends Robot {
    static Direction main_direction;
    static boolean seenEnemyLocation;
    static RotationDirection spinDirection = Util.RotationDirection.COUNTERCLOCKWISE;

    public SurvivalMuckracker(RobotController r) {
        super(r);
        subRobotType = Comms.SubRobotType.MUC_SURVIVAL;
        defaultFlag = Comms.getFlag(Comms.InformationCategory.ROBOT_TYPE, subRobotType);
        seenEnemyLocation = false;
    }

    public SurvivalMuckracker(RobotController r, MapLocation h) {
        this(r);
        home = h;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        MapLocation currLoc = rc.getLocation();

        Debug.println(Debug.info, "I am a survival Mucker; current influence: " + rc.getInfluence());
        Debug.println(Debug.info, "current buff: " + rc.getEmpowerFactor(rc.getTeam(),0));

        if(main_direction == null){
            main_direction = Util.randomDirection();
        }

        if (rc.isReady() && currLoc.distanceSquaredTo(home) > sensorRadius) {
            main_direction = currLoc.directionTo(home);
            tryMoveDest(main_direction);
        } else if (rc.isReady()) {
            tryMoveDest(main_direction);
        }
    }
}