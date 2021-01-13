package StuckyMucky;
import battlecode.common.*;

public class Comms {
    public static final int MIN_FLAG_MESSAGE = 1000000;

    public enum InformationCategory {
        NEUTRAL_EC,
        ENEMY_EC,
        NEW_ROBOT,
        DETONATE,
        SUB_ROBOT,
        UNKOWN
    }

    public enum SubRobotType {
        POL_DEFENDER,
        POL_EXPLORER,
        POL_BODYGUARD,
        POL_SPAWNKILL
    }

    public static int addCoord(int flag, int dx, int dy) {
        return flag*1000000 + dx*1000 + dy;
    }

    // SUB_ROBOT
    public static int getFlag(InformationCategory cat, SubRobotType type) {
        return getFlag(cat, 0, type.ordinal());
    }

    public static int getFlag(InformationCategory cat) {
        return getFlag(cat, 0, 0);
    }

    public static int getFlag(InformationCategory cat, int dx, int dy) {
        int flag = 0;
        switch (cat) {
            case NEUTRAL_EC:
                flag += 2;
                break;
            case ENEMY_EC:
                flag += 3;
                break;
            case NEW_ROBOT:
                flag += 4;
                break;
            case DETONATE:
                flag += 5;
                break;
            case SUB_ROBOT:
                flag += 6;
                break;
            default:
                break;
        }

        flag = addCoord(flag, dx, dy);
        return flag;
    }

    public static InformationCategory getIC(int flag) {
        switch(flag/1000000) {
            case 2: return InformationCategory.NEUTRAL_EC;
            case 3: return InformationCategory.ENEMY_EC;
            case 4: return InformationCategory.NEW_ROBOT;
            case 5: return InformationCategory.DETONATE;
            case 6: return InformationCategory.SUB_ROBOT;
            default: return InformationCategory.UNKOWN;
        }
    }

    public static int[] getDxDy(int flag) {
        int[] res = new int[2];
        flag %= 1000000;
        res[0] = flag / 1000;
        res[1] = flag % 1000;
        return res;
    }

    public static SubRobotType getSubRobotType(int flag) {
        flag %= 1000000;
        return SubRobotType.values()[flag];
    }
}