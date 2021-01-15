package musketeerplayersprint2;

import battlecode.common.*;

public class Comms {
    public static final double INF_LOG_BASE = 1.35;
    static final int BIT_IC_OFFSET = 19;
    static final int BIT_MASK_IC = 0x1F << BIT_IC_OFFSET;
    static final int BIT_DX_OFFSET = 7;
    static final int BIT_MASK_COORD = 0x7F;
    static final int BIT_MASK_COORDS = 0x3FFF;
    static final int BIT_INF_OFFSET = 14;
    static final int BIT_TURNCOUNT_OFFSET = 8;
    static final int BIT_SMALL_DX_OFFSET = 4;
    static final int BIT_MASK_SMALL_COORD = 0xF;
    static final int BIT_MASK_DIR = 0xF;

    public enum InformationCategory {
        EMPTY,                      // UNUSED BUT BREAKS IF REMOVED? TODO: LOOK INTO
        NEUTRAL_EC,
        ENEMY_EC,
        ENEMY_EC_MUK,
        FRIENDLY_EC,
        RUSH_EC_GOLEM,
        NEW_ROBOT,
        TARGET_ROBOT,
        ROBOT_TYPE,
        ENEMY_FOUND,
        FOLLOWING,
        AVG_ENEMY_DIR,                // UNUSED? Though might do one for slanderers specifically
        ROBOT_TYPE_AND_CLOSEST_ENEMY, // UNUSED?
        SLA_CLOSEST_ENEMY,
        CLOSEST_ENEMY,
        ENEMY_EC_ATTACK_CALL,
    }

    public enum SubRobotType {
        POL_DEFENDER,
        POL_EXPLORER,
        POL_PROTECTOR,
        POL_SPAWNKILL,
        POL_RUSH,
        POL_GOLEM,
        POL_CLEANUP,
        SLANDERER,
        MUC_HUNTER,
        MUC_EXPLORER,
        MUC_LATTICE,
        EC,
    }

    public enum ECFoundType {
        ENEMY,
        NEUTRAL,
        HUNTER_ADD,
        HUNTER_DELETE,
    }

    public static int addCoord(int flag, int dx, int dy) {
        return (flag << BIT_IC_OFFSET) + (dx << BIT_DX_OFFSET) + dy;
    }

    // dx/dy max 4 bits
    public static int getFlagTurn(InformationCategory cat, int turnCount, int dx, int dy) {
        return (cat.ordinal() << BIT_IC_OFFSET) + (turnCount << BIT_TURNCOUNT_OFFSET) + (dx << BIT_SMALL_DX_OFFSET) + dy;
    }

    public static int getFlag(InformationCategory cat, SubRobotType type, int dx, int dy) {
        return getFlag(cat, type.ordinal(), dx, dy);
    }

    public static int getFlag(InformationCategory cat, int turnCount, Direction avgDirection) {
        return getFlag(cat, turnCount, avgDirection.ordinal());
    }

    public static int getFlag(InformationCategory cat, int n) {
        return getFlag(cat, 0, n);
    }

    //spawning slanderers and telling them what direction is away from average enemy
    public static int getFlag(InformationCategory cat, Direction awayFromEnemy) {
        return getFlag(cat, 0, awayFromEnemy.ordinal());
    }

    // TARGET_ROBOT / ROBOT_TYPE
    public static int getFlag(InformationCategory cat, SubRobotType type) {
        return getFlag(cat, 0, type.ordinal());
    }

    public static int getFlag(InformationCategory cat) {
        return getFlag(cat, 0, 0);
    }

    // NEUTRAL_EC / ENEMY_EC
    public static int getFlag(InformationCategory cat, int inf, int dx, int dy) {
        int flag = cat.ordinal();
        return (flag << BIT_IC_OFFSET) + (inf << BIT_INF_OFFSET) + (dx << BIT_DX_OFFSET) + dy;
    }

    public static int getFlag(InformationCategory cat, int dx, int dy) {
        int flag = cat.ordinal();
        flag = addCoord(flag, dx, dy);
        return flag;
    }

    public static InformationCategory getIC(int flag) {
        return InformationCategory.values()[(flag >> BIT_IC_OFFSET)];
    }

    public static int[] getDxDy(int flag) {
        int[] res = new int[2];
        res[0] = (flag >> BIT_DX_OFFSET) & BIT_MASK_COORD;
        res[1] = flag & BIT_MASK_COORD;
        return res;
    }

    public static int[] getSmallDxDy(int flag) {
        int[] res = new int[2];
        res[0] = (flag >> BIT_SMALL_DX_OFFSET) & BIT_MASK_SMALL_COORD;
        res[1] = flag & BIT_MASK_SMALL_COORD;
        return res;
    }

    public static int getInf(int flag) {
        return (flag & ~BIT_MASK_IC) >> BIT_INF_OFFSET;
    }

    public static int getTurnCount(int flag) {
        return (flag & ~BIT_MASK_IC) >> BIT_TURNCOUNT_OFFSET;
    }

    // DO NOT USE WITH ROBOT_TYPE_AND_CLOSEST_ENEMY
    public static SubRobotType getSubRobotType(int flag) {
        return SubRobotType.values()[(flag & ~BIT_MASK_IC)];
    }

    // USE ONLY WITH ROBOT_TYPE_AND_CLOSEST_ENEMY
    public static SubRobotType getSubRobotTypeClosestEnemy(int flag) {
        return SubRobotType.values()[(flag & ~BIT_MASK_IC) >> BIT_INF_OFFSET];
    }

    public static Direction getAwayDirection(int flag) {
        return Direction.values()[(flag & ~BIT_MASK_IC)];
    }

    public static Direction getDirection(int flag) {
        return Direction.values()[(flag & BIT_MASK_DIR)];
    }

    public static boolean isSubRobotType(int flag, SubRobotType type) {
        switch(Comms.getIC(flag)) {
            case ROBOT_TYPE:
                return Comms.getSubRobotType(flag) == type;
            case ROBOT_TYPE_AND_CLOSEST_ENEMY:
                return Comms.getSubRobotTypeClosestEnemy(flag) == type;
            case SLA_CLOSEST_ENEMY:
                return SubRobotType.SLANDERER == type;
            default:
                return false;
        }
    }
}