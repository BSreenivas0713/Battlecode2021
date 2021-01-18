package musketeerplayersprint2;

import battlecode.common.*;

public class Comms {
    public static final double INF_LOG_BASE = 1.35;
    public static final double INF_SCALAR = 1;
    static final int BIT_IC_OFFSET = 20;
    static final int BIT_MASK_IC = 0xF << BIT_IC_OFFSET;
    static final int BIT_DX_OFFSET = 7;
    static final int BIT_MASK_COORD = 0x7F;
    static final int BIT_MASK_COORDS = 0x3FFF;
    static final int BIT_INF_OFFSET = 14;
    static final int BIT_TURNCOUNT_OFFSET = 8;
    static final int BIT_SMALL_DX_OFFSET = 4;
    static final int BIT_MASK_SMALL_COORD = 0xF;
    static final int BIT_MASK_DIR = 0xF;
    //Old flag setup: CCCCCIIIIIXXXXXXXYYYYYYY
    //New flag setup: CCCCIIIIIIXXXXXXXYYYYYYY

    public enum InformationCategory {
        EMPTY,                      // UNUSED BUT BREAKS IF REMOVED? TODO: LOOK INTO
        NEUTRAL_EC,
        ENEMY_EC,
        ENEMY_EC_MUK,
        FRIENDLY_EC,
        TARGET_ROBOT,
        ROBOT_TYPE,
        ENEMY_FOUND,
        FOLLOWING,
        ENEMY_EC_ATTACK_CALL,
        ENEMY_EC_CHILL_CALL,
        CLOSEST_ENEMY_OR_FLEEING,
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
        MUC_SCOUT,
        EC,
    }

    public enum GroupRushType {
        MUC,
    }

    public enum ClosestEnemyOrFleeing {
        NOT_SLA,
        SLA,
        FLEEING,
    }

    public static int addCoord(int flag, int dx, int dy) {
        return (flag << BIT_IC_OFFSET) + (dx << BIT_DX_OFFSET) + dy;
    }

    public static int getFlagRush(InformationCategory cat, int idMod, GroupRushType type, int dx, int dy) {
        return getFlag(cat, idMod << 2 + type.ordinal(), dx, dy);
    }

    public static int getFlag(InformationCategory cat, ClosestEnemyOrFleeing CEOF, int dx, int dy) {
        return getFlag(cat, CEOF.ordinal(), dx, dy);
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

    public static int getFlagScout(InformationCategory cat, SubRobotType type, Direction dir) {
        return (cat.ordinal() << BIT_IC_OFFSET) + (dir.ordinal() << BIT_INF_OFFSET) + type.ordinal();
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
        return InformationCategory.values()[(flag >>> BIT_IC_OFFSET)];
    }

    public static int[] getDxDy(int flag) {
        int[] res = new int[2];
        res[0] = (flag >>> BIT_DX_OFFSET) & BIT_MASK_COORD;
        res[1] = flag & BIT_MASK_COORD;
        return res;
    }

    public static int[] getSmallDxDy(int flag) {
        int[] res = new int[2];
        res[0] = (flag >>> BIT_SMALL_DX_OFFSET) & BIT_MASK_SMALL_COORD;
        res[1] = flag & BIT_MASK_SMALL_COORD;
        return res;
    }

    public static int getInf(int flag) {
        int encodedInf = (flag & ~BIT_MASK_IC) >>> BIT_INF_OFFSET;
        int influence = (int) (Math.exp(encodedInf * Math.log(Comms.INF_LOG_BASE)) * INF_SCALAR);
        return influence;
    }

    public static int getTurnCount(int flag) {
        return (flag & ~BIT_MASK_IC) >>> BIT_TURNCOUNT_OFFSET;
    }

    public static Direction getScoutDirection(int flag) {
        return Direction.values()[(flag & ~BIT_MASK_IC) >>> BIT_INF_OFFSET];
    }

    public static ClosestEnemyOrFleeing getSubCEOF(int flag) {
        return ClosestEnemyOrFleeing.values()[(flag & ~BIT_MASK_IC) >>> BIT_INF_OFFSET];
    }

    public static int encodeInf(int inf) {
        return (int) Math.min(63, Math.floor(Math.log(inf / INF_SCALAR) / Math.log(Comms.INF_LOG_BASE)));
    }

    // DO NOT USE WITH OR MUK_SCOUT
    public static SubRobotType getSubRobotType(int flag) {
        return SubRobotType.values()[(flag & BIT_MASK_COORDS)];
    }

    public static SubRobotType getSubRobotTypeScout(int flag) {
        return SubRobotType.values()[(flag & BIT_MASK_COORDS)];
    }

    public static Direction getDirection(int flag) {
        return Direction.values()[(flag & BIT_MASK_DIR)];
    }

    public static GroupRushType getRushType(int flag) {
        return GroupRushType.values()[(flag >>> BIT_INF_OFFSET) & 0x3];
    }

    public static int getRushMod(int flag) {
        return (flag >>> BIT_INF_OFFSET >>> 2) & 0x3;
    }

    public static boolean isSubRobotType(int flag, SubRobotType type) {
        switch(Comms.getIC(flag)) {
            case ROBOT_TYPE:
                return getSubRobotType(flag) == type;
            case CLOSEST_ENEMY_OR_FLEEING:
                return getSubCEOF(flag) != ClosestEnemyOrFleeing.NOT_SLA && SubRobotType.SLANDERER == type;
            default:
                return false;
        }
    }
}