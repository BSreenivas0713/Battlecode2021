package musketeerplayerfinal;

import battlecode.common.*;

public class Comms {
    public static final double INF_LOG_BASE = 1.16;
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
    static final int BIT_FRIEND_OFFSET = 2;
    static final int BIT_MASK_SUBROBOTTYPE = 0x1F;
    static final int BIT_WITH_SUBROBOTTYPE_OFFSET = 5;
    //Old flag setup: CCCCCIIIIIXXXXXXXYYYYYYY
    //New flag setup: CCCCIIIIIIXXXXXXXYYYYYYY

    static final int INF_BOUND_1 = 100;
    static final int INF_BOUND_2 = 400;
    static final int INF_BOUND_3 = 1000;
    static final int INF_BOUND_4 = 2000;
    static final int INF_BOUND_5 = 3500;
    static final int INF_BOUND_6 = 5600;
    static final int INF_BOUND_7 = 8000;

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
        CLOSEST_ENEMY_OR_FLEEING,
        REPORTING_WALL,
        MY_LOC,
        DELETE_ENEMY_LOC,
        BUFF_MUCK,
    }

    public enum SubRobotType {
        POL_DEFENDER,
        POL_EXPLORER,
        POL_PROTECTOR,
        POL_SPAWNKILL,
        POL_DORMANT_RUSH,
        POL_ACTIVE_RUSH,
        POL_GOLEM,
        POL_CLEANUP,
        POL_BUFF,
        POL_SUPPORT,
        POL_HEAD,
        POL_HEAD_READY,
        POL_FAT,
        SLANDERER,
        MUC_HUNTER,
        MUC_EXPLORER,
        MUC_LATTICE,
        MUC_SCOUT,
        MUC_SURVIVAL,
        EC,
    }

    public enum GroupRushType {
        MUC,
        MUC_POL,
    }

    public enum ClosestEnemyOrFleeing {
        POL_PROTECTOR,
        SLA,
        OTHER,
        FLEEING,
    }

    public enum EnemyType {
        UNKNOWN,
        SLA,
        MUC,
    }

    public enum IsSla {
        NO,
        YES,
    }

    public enum FriendlyECType {
        HOME_READ_LOC,
        HOME_READ_ID,
        OTHER_READ_LOC,
        OTHER_READ_ID,
    }

    public static int addCoord(int flag, int dx, int dy) {
        return (flag << BIT_IC_OFFSET) + (dx << BIT_DX_OFFSET) + dy;
    }

    public static int getFlag(InformationCategory cat, FriendlyECType type, int dx, int dy) {
        return (cat.ordinal() << BIT_IC_OFFSET) + (dx << (BIT_DX_OFFSET + BIT_FRIEND_OFFSET)) + (dy << BIT_FRIEND_OFFSET) + type.ordinal();
    }

    public static int getFlagRush(InformationCategory cat, int idMod, GroupRushType type, int dx, int dy) {
        return getFlag(cat, (idMod << 2) + type.ordinal(), dx, dy);
    }

    public static int getFlagRush(InformationCategory cat, int idMod, EnemyType type, int dx, int dy) {
        return getFlag(cat, (idMod << 2) + type.ordinal(), dx, dy);
    }

    public static int getFlagEnemyFound(InformationCategory cat, IsSla sla, EnemyType type, int dx, int dy) {
        return getFlag(cat, (sla.ordinal() << 2) + type.ordinal(), dx, dy);
    }

    public static int getFlag(InformationCategory cat, ClosestEnemyOrFleeing CEOF, int dx, int dy) {
        return getFlag(cat, (CEOF.ordinal() << 2), dx, dy);
    }

    public static int getFlag(InformationCategory cat, ClosestEnemyOrFleeing CEOF, EnemyType type, int dx, int dy) {
        return getFlag(cat, (CEOF.ordinal() << 2) + type.ordinal(), dx, dy);
    }

    // dx/dy max 4 bits
    public static int getFlagTurn(InformationCategory cat, int turnCount, int dx, int dy) {
        return (cat.ordinal() << BIT_IC_OFFSET) + (turnCount << BIT_TURNCOUNT_OFFSET) + (dx << BIT_SMALL_DX_OFFSET) + dy;
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

    public static int getFlag(InformationCategory cat, SubRobotType type, int dx, int dy) {
        return (cat.ordinal() << BIT_IC_OFFSET) + (dx << BIT_WITH_SUBROBOTTYPE_OFFSET << BIT_DX_OFFSET) +
                (dy << BIT_WITH_SUBROBOTTYPE_OFFSET) + type.ordinal();
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

    public static int[] getDxDySubRobotType(int flag) {
        int[] res = new int[2];
        res[0] = (flag >>> BIT_DX_OFFSET >>> BIT_WITH_SUBROBOTTYPE_OFFSET) & BIT_MASK_COORD;
        res[1] = (flag >>> BIT_WITH_SUBROBOTTYPE_OFFSET) & BIT_MASK_COORD;
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
        return getDirectionFromSubRobotTypeFlag(flag);
    }

    public static ClosestEnemyOrFleeing getSubCEOF(int flag) {
        return ClosestEnemyOrFleeing.values()[(flag >>> BIT_INF_OFFSET >>> 2) & 0x3];
    }

    public static int encodeInf(int inf) {
        return (int) Math.min(63, Math.ceil(Math.log(inf / INF_SCALAR) / Math.log(Comms.INF_LOG_BASE)));
    }

    // public static int getInf(int flag) {
    //     int encodedInf = (flag & ~BIT_MASK_IC) >>> BIT_INF_OFFSET;
        
    //     if(encodedInf < 10) {
    //         return encodedInf * INF_BOUND_1 / 10;
    //     } else if(encodedInf < 20) {
    //         return (encodedInf - 10) * ((INF_BOUND_2 - INF_BOUND_1) / 10) + INF_BOUND_1;
    //     } else if(encodedInf < 30) {
    //         return (encodedInf - 20) * ((INF_BOUND_3 - INF_BOUND_2) / 10) + INF_BOUND_2;
    //     } else if(encodedInf < 40) {
    //         return (encodedInf - 30) * ((INF_BOUND_4 - INF_BOUND_3) / 10) + INF_BOUND_3;
    //     } else if(encodedInf < 50) {
    //         return (encodedInf - 40) * ((INF_BOUND_5 - INF_BOUND_4) / 10) + INF_BOUND_4;
    //     } else if(encodedInf < 60) {
    //         return (encodedInf - 50) * ((INF_BOUND_6 - INF_BOUND_5) / 10) + INF_BOUND_5;
    //     } else {
    //         return (encodedInf - 60) * ((INF_BOUND_7 - INF_BOUND_6) / 4) + INF_BOUND_6;
    //     }
    // }

    // public static int encodeInf(int inf) {
    //     if(inf < INF_BOUND_1) {
    //         return inf / 10;
    //     } else if(inf < INF_BOUND_2) {
    //         return (inf - INF_BOUND_1) / ((INF_BOUND_2 - INF_BOUND_1) / 10) + 10;
    //     } else if(inf < INF_BOUND_3) {
    //         return (inf - INF_BOUND_2) / ((INF_BOUND_3 - INF_BOUND_2) / 10) + 20;
    //     } else if(inf < INF_BOUND_4) {
    //         return (inf - INF_BOUND_3) / ((INF_BOUND_4 - INF_BOUND_3) / 10) + 30;
    //     } else if(inf < INF_BOUND_5) {
    //         return (inf - INF_BOUND_4) / ((INF_BOUND_5 - INF_BOUND_4) / 10) + 40;
    //     } else if(inf < INF_BOUND_6) {
    //         return (inf - INF_BOUND_5) / ((INF_BOUND_6 - INF_BOUND_5) / 10) + 50;
    //     } else {
    //         return Math.max(63, (inf - INF_BOUND_6) / ((INF_BOUND_7 - INF_BOUND_6) / 4) + 60);
    //     }
    // }

    // DO NOT USE WITH OR MUK_SCOUT
    public static SubRobotType getSubRobotType(int flag) {
        return SubRobotType.values()[(flag & BIT_MASK_SUBROBOTTYPE)];
    }

    public static SubRobotType getSubRobotTypeScout(int flag) {
        return SubRobotType.values()[(flag & BIT_MASK_SUBROBOTTYPE)];
    }

    public static Direction getDirection(int flag) {
        return Direction.values()[(flag & BIT_MASK_DIR)];
    }

    public static Direction getDirectionFromSubRobotTypeFlag(int flag) {
        return Direction.values()[(flag & ~BIT_MASK_IC) >>> BIT_INF_OFFSET];
    }

    public static GroupRushType getRushType(int flag) {
        return GroupRushType.values()[(flag >>> BIT_INF_OFFSET) & 0x3];
    }

    public static EnemyType getEnemyType(int flag) {
        return EnemyType.values()[(flag >>> BIT_INF_OFFSET) & 0x3];
    }

    public static IsSla getIsSla(int flag) {
        return IsSla.values()[(flag >>> BIT_INF_OFFSET >>> 2) & 0x1];
    }

    public static FriendlyECType getFriendlyECType(int flag) {
        return FriendlyECType.values()[flag & 0x3];
    }

    public static int getFriendlyID(int flag) {
        return (flag & ~BIT_MASK_IC) >>> BIT_FRIEND_OFFSET;
    }

    public static int[] getFriendlyDxDy(int flag) {
        int[] res = new int[2];
        res[0] = (flag >>> (BIT_DX_OFFSET + BIT_FRIEND_OFFSET)) & BIT_MASK_COORD;
        res[1] = (flag >>> BIT_FRIEND_OFFSET) & BIT_MASK_COORD;
        return res;
    }

    public static int getRushMod(int flag) {
        return (flag >>> BIT_INF_OFFSET >>> 2) & 0x3;
    }

    public static boolean isSubRobotType(int flag, SubRobotType type) {
        switch(Comms.getIC(flag)) {
            case ROBOT_TYPE:
                return getSubRobotType(flag) == type;
            case CLOSEST_ENEMY_OR_FLEEING:
                switch(getSubCEOF(flag)) {
                    case POL_PROTECTOR:
                        return type == SubRobotType.POL_PROTECTOR;
                    case SLA:
                    case FLEEING:
                        return type == SubRobotType.SLANDERER;
                    case OTHER:
                        return false;
                }
            case ENEMY_FOUND:
                return getIsSla(flag) == IsSla.YES && type == SubRobotType.SLANDERER;
            default:
                return false;
        }
    }

    public static boolean isRusher(int flag) {
        if(Comms.getIC(flag) == Comms.InformationCategory.ROBOT_TYPE) {
            switch(Comms.getSubRobotType(flag)) {
                case POL_HEAD:
                case POL_HEAD_READY:
                case POL_ACTIVE_RUSH:
                case POL_SUPPORT:
                    return true;
            }
        }
        return false;
    }
}