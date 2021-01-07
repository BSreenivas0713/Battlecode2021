package musketeerplayer;
import battlecode.common.*;

public class Comms {
    public enum InformationCategory {
        NEUTRAL_EC,
        ENEMY_EC,
        UNKOWN
    }

    public static int addCoord(int flag, int dx, int dy) {
        return flag*1000000 + dx*1000 + dy;
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
}