package musketeerplayer;
import battlecode.common.*;

public class Comms {
    public static final double INF_LOG_BASE = 1.35;
    static final int BIT_IC_OFFSET = 19;
    static final int BIT_MASK_IC = 0x1F << BIT_IC_OFFSET;
    static final int BIT_DX_OFFSET = 7;
    static final int BIT_MASK_COORD = 0x7F;
    static final int BIT_MASK_COORDS = 0x3FFF;
    static final int BIT_INF_OFFSET = 14;

    public enum InformationCategory {
        EMPTY,
        NEUTRAL_EC,
        ENEMY_EC,
        NEW_ROBOT,
        DETONATE,
        TARGET_ROBOT,
        ATTACKING,
        ROBOT_TYPE,
    }

    public enum SubRobotType {
        POL_DEFENDER,
        POL_EXPLORER,
        POL_BODYGUARD,
        POL_SPAWNKILL,
        POL_RUSH,
        POL_GOLEM,
        SLANDERER,
        MUCKRAKER,
        EC
    }

    public static int addCoord(int flag, int dx, int dy) {
        return (flag << BIT_IC_OFFSET) + (dx << BIT_DX_OFFSET) + dy;
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

    public static int getInf(int flag) {
        return (flag & ~BIT_MASK_IC) >> BIT_INF_OFFSET;
    }

    public static SubRobotType getSubRobotType(int flag) {
        return SubRobotType.values()[(flag & ~BIT_MASK_IC)];
    }
}