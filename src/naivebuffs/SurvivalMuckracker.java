package naivebuffs;
import battlecode.common.*;

import naivebuffs.Util.*;
import naivebuffs.Debug.*;
import naivebuffs.fast.FastIterableLocSet;

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

    public SurvivalMuckracker(RobotController r, MapLocation h, int hID) {
        this(r);
        home = h;
        homeID = hID;
        friendlyECs.add(home, homeID);
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