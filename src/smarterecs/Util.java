package smarterecs;

import battlecode.common.*;

public class Util {
    static enum RotationDirection {
        CLOCKWISE,
        COUNTERCLOCKWISE
    };

    static enum DirectionPreference {
        RANDOM,
        ORTHOGONAL,
        DIAGONAL,
    }

    static final RobotType[] spawnableRobot = {
        RobotType.POLITICIAN,
        RobotType.SLANDERER,
        RobotType.MUCKRAKER,
    };

    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    static final Direction[] orthogonalDirs = {
        Direction.NORTH,
        Direction.SOUTH,
        Direction.WEST,
        Direction.EAST,
    };

    static final Direction[] diagonalDirs = {
        Direction.NORTHWEST,
        Direction.NORTHEAST,
        Direction.SOUTHEAST,
        Direction.SOUTHWEST,
    };

    static final Direction[] scoutDirs = {
        Direction.NORTH,
        Direction.SOUTH,
        Direction.WEST,
        Direction.EAST,
        Direction.NORTHWEST,
        Direction.SOUTHEAST,
        Direction.NORTHEAST,
        Direction.SOUTHWEST,
    };

    static final double spawnKillThreshold = 5;
    static final int dOffset = 64;
    static final int dSmallOffset = 8;

    static final int phaseOne = 30;
    static final int phaseTwo = 2000;

    static final int numDefenders = 4;
    static final int timeBeforeDefenders = 50;

    static final int minRushInfluence = 200;
    static final int maxECRushConviction = 150;
    static final int minTimeBetweenRushes = 0;
    
    static final int startCleanupThreshold = 100;
    static final int cleanupPoliticianInfluence = 34;
    
    static final int buildSlandererThreshold = 20000;

    static final int scoutBuffMuckSize = 150;

    static final int smallMuckThreshold = 5;
    static final int chainEmpowerFactor = 3;
    
    static final int maxSlandererInfluence = 949;
    static final int pathfindingDistanceMult = 3;

    static final int PoliticianSensorRadius = 5;

    static final int maxFollowingSingleUnit = 1;

    static final int turnsBetweenEnemyBroadcast = 5;
    static final int turnsEnemyBroadcastValid = 5;
    static final int flagCooldown = 10;

    static final int bufMuckCooldownThreshold = 10;

    static final int explorerMuckrakerLifetime = 200;
    static final int slandererLifetime = 300;

    static final int turnsSlandererLocValid = 5;
    static final int minRotationRadius = 15;
    static final int maxRotationRadius = 30;

    static final int minDistFromEnemy = 100;
    
    static final int MuckAttackCooldown = 50;

    static final int baseRushCooldown = 15;
    static final int maxRushCooldown = 30;

    static final int buffMukFrequency = 20;
    static final int maxBuffMuk = 400;

    static final int explorerPolFrequency = 7;
    static final int buffPolFrequency = 5;
    static final int maxBuffPolNum = 8;
    static final int maxBuffPolsInARow = 4;

    static final int attackCallBoredom = 30;

    static final int foundEnemyLocationBoredom = 10;

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    static Direction randomOrthogonalDirection() {
        return orthogonalDirs[(int) (Math.random() * orthogonalDirs.length)];
    }

    static Direction randomDiagonalDirection() {
        return diagonalDirs[(int) (Math.random() * diagonalDirs.length)];
    }

    static Direction[] getOrderedDirections(DirectionPreference pref) {
        Direction dir;
        switch(pref) {
            case ORTHOGONAL:
                dir = randomOrthogonalDirection();
                return new Direction[]{dir, dir.rotateLeft(), dir.rotateLeft().rotateLeft(), dir.opposite().rotateRight(), dir.opposite(),
                        dir.opposite().rotateLeft(), dir.rotateRight().rotateRight(), dir.rotateRight()};
            case DIAGONAL:
                dir = randomDiagonalDirection();
                return new Direction[]{dir, dir.rotateLeft(), dir.rotateLeft().rotateLeft(), dir.opposite().rotateRight(), dir.opposite(),
                        dir.opposite().rotateLeft(), dir.rotateRight().rotateRight(), dir.rotateRight()};
            case RANDOM:
            default:
                dir = randomDirection();
                return new Direction[]{dir, dir.rotateLeft(), dir.rotateLeft().rotateLeft(), dir.opposite().rotateRight(), dir.opposite(),
                        dir.opposite().rotateLeft(), dir.rotateRight().rotateRight(), dir.rotateRight()};
        }
    }

    static Direction[] getOrderedDirections(Direction dir) {
        return new Direction[]{dir, dir.rotateLeft(), dir.rotateLeft().rotateLeft(), dir.opposite().rotateRight(), dir.opposite(),
                dir.opposite().rotateLeft(), dir.rotateRight().rotateRight(), dir.rotateRight()};
    }

    static Direction rotateInSpinDirection(RotationDirection Rot, Direction dir) {
        switch(Rot) {
            case COUNTERCLOCKWISE:
                return dir.rotateLeft();
            case CLOCKWISE:
                return dir.rotateRight();
            default:
                return null;
        }
    }

    static Direction rotateOppositeSpinDirection(RotationDirection Rot, Direction dir) {
        switch(Rot) {
            case COUNTERCLOCKWISE:
                return dir.rotateRight();
            case CLOCKWISE:
                return dir.rotateLeft();
            default:
                return null;
        }
    }

    static RotationDirection switchSpinDirection(RotationDirection Rot) {
        switch(Rot) {
            case COUNTERCLOCKWISE:
                return RotationDirection.CLOCKWISE;
            case CLOCKWISE: 
                return RotationDirection.COUNTERCLOCKWISE;
            default:
                return null;
        }
    }
    
    static Direction turnLeft90(Direction dir) {
        return dir.rotateLeft().rotateLeft();
    }

    static Direction turnRight90(Direction dir) {
        return dir.rotateRight().rotateRight();
    }

    static Direction rightOrLeftTurn(RotationDirection Rot, Direction dir) {
        switch(Rot) {
            case COUNTERCLOCKWISE:
                return turnLeft90(dir);
            case CLOCKWISE: 
                return turnRight90(dir);
            default:
                return null;
        }

    }

    static Direction[] getAboutToDieBuildOrder(Direction dir) {
        switch(dir) {
            case NORTH: case SOUTH:
            case EAST: case WEST:
                return new Direction[]{dir, dir.rotateLeft().rotateLeft(), dir.rotateRight().rotateRight(), dir.opposite()};
            default:
                return new Direction[]{dir.rotateLeft(), dir.rotateRight(), dir.rotateLeft().opposite(), dir.rotateRight().opposite()};
        }
    }

    /**
     * Returns a random spawnable RobotType
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnableRobotType() {
        return spawnableRobot[(int) (Math.random() * spawnableRobot.length)];
    }

    static int getBestSlandererInfluence(int influence) {
        switch(influence) {
            case 0: return -1;
            case 1: return -1;
            case 2: return -1;
            case 3: return -1;
            case 4: return -1;
            case 5: return -1;
            case 6: return -1;
            case 7: return -1;
            case 8: return -1;
            case 9: return -1;
            case 10: return -1;
            case 11: return -1;
            case 12: return -1;
            case 13: return -1;
            case 14: return -1;
            case 15: return -1;
            case 16: return -1;
            case 17: return -1;
            case 18: return -1;
            case 19: return -1;
            case 20: return -1;
            case 21: return 21;
            case 22: return 21;
            case 23: return 21;
            case 24: return 21;
            case 25: return 21;
            case 26: return 21;
            case 27: return 21;
            case 28: return 21;
            case 29: return 21;
            case 30: return 21;
            case 31: return 21;
            case 32: return 21;
            case 33: return 21;
            case 34: return 21;
            case 35: return 21;
            case 36: return 21;
            case 37: return 21;
            case 38: return 21;
            case 39: return 21;
            case 40: return 21;
            case 41: return 41;
            case 42: return 41;
            case 43: return 41;
            case 44: return 41;
            case 45: return 41;
            case 46: return 41;
            case 47: return 41;
            case 48: return 41;
            case 49: return 41;
            case 50: return 41;
            case 51: return 41;
            case 52: return 41;
            case 53: return 41;
            case 54: return 41;
            case 55: return 41;
            case 56: return 41;
            case 57: return 41;
            case 58: return 41;
            case 59: return 41;
            case 60: return 41;
            case 61: return 41;
            case 62: return 41;
            case 63: return 63;
            case 64: return 63;
            case 65: return 63;
            case 66: return 63;
            case 67: return 63;
            case 68: return 63;
            case 69: return 63;
            case 70: return 63;
            case 71: return 63;
            case 72: return 63;
            case 73: return 63;
            case 74: return 63;
            case 75: return 63;
            case 76: return 63;
            case 77: return 63;
            case 78: return 63;
            case 79: return 63;
            case 80: return 63;
            case 81: return 63;
            case 82: return 63;
            case 83: return 63;
            case 84: return 63;
            case 85: return 85;
            case 86: return 85;
            case 87: return 85;
            case 88: return 85;
            case 89: return 85;
            case 90: return 85;
            case 91: return 85;
            case 92: return 85;
            case 93: return 85;
            case 94: return 85;
            case 95: return 85;
            case 96: return 85;
            case 97: return 85;
            case 98: return 85;
            case 99: return 85;
            case 100: return 85;
            case 101: return 85;
            case 102: return 85;
            case 103: return 85;
            case 104: return 85;
            case 105: return 85;
            case 106: return 85;
            case 107: return 107;
            case 108: return 107;
            case 109: return 107;
            case 110: return 107;
            case 111: return 107;
            case 112: return 107;
            case 113: return 107;
            case 114: return 107;
            case 115: return 107;
            case 116: return 107;
            case 117: return 107;
            case 118: return 107;
            case 119: return 107;
            case 120: return 107;
            case 121: return 107;
            case 122: return 107;
            case 123: return 107;
            case 124: return 107;
            case 125: return 107;
            case 126: return 107;
            case 127: return 107;
            case 128: return 107;
            case 129: return 107;
            case 130: return 130;
            case 131: return 130;
            case 132: return 130;
            case 133: return 130;
            case 134: return 130;
            case 135: return 130;
            case 136: return 130;
            case 137: return 130;
            case 138: return 130;
            case 139: return 130;
            case 140: return 130;
            case 141: return 130;
            case 142: return 130;
            case 143: return 130;
            case 144: return 130;
            case 145: return 130;
            case 146: return 130;
            case 147: return 130;
            case 148: return 130;
            case 149: return 130;
            case 150: return 130;
            case 151: return 130;
            case 152: return 130;
            case 153: return 130;
            case 154: return 154;
            case 155: return 154;
            case 156: return 154;
            case 157: return 154;
            case 158: return 154;
            case 159: return 154;
            case 160: return 154;
            case 161: return 154;
            case 162: return 154;
            case 163: return 154;
            case 164: return 154;
            case 165: return 154;
            case 166: return 154;
            case 167: return 154;
            case 168: return 154;
            case 169: return 154;
            case 170: return 154;
            case 171: return 154;
            case 172: return 154;
            case 173: return 154;
            case 174: return 154;
            case 175: return 154;
            case 176: return 154;
            case 177: return 154;
            case 178: return 178;
            case 179: return 178;
            case 180: return 178;
            case 181: return 178;
            case 182: return 178;
            case 183: return 178;
            case 184: return 178;
            case 185: return 178;
            case 186: return 178;
            case 187: return 178;
            case 188: return 178;
            case 189: return 178;
            case 190: return 178;
            case 191: return 178;
            case 192: return 178;
            case 193: return 178;
            case 194: return 178;
            case 195: return 178;
            case 196: return 178;
            case 197: return 178;
            case 198: return 178;
            case 199: return 178;
            case 200: return 178;
            case 201: return 178;
            case 202: return 178;
            case 203: return 203;
            case 204: return 203;
            case 205: return 203;
            case 206: return 203;
            case 207: return 203;
            case 208: return 203;
            case 209: return 203;
            case 210: return 203;
            case 211: return 203;
            case 212: return 203;
            case 213: return 203;
            case 214: return 203;
            case 215: return 203;
            case 216: return 203;
            case 217: return 203;
            case 218: return 203;
            case 219: return 203;
            case 220: return 203;
            case 221: return 203;
            case 222: return 203;
            case 223: return 203;
            case 224: return 203;
            case 225: return 203;
            case 226: return 203;
            case 227: return 203;
            case 228: return 228;
            case 229: return 228;
            case 230: return 228;
            case 231: return 228;
            case 232: return 228;
            case 233: return 228;
            case 234: return 228;
            case 235: return 228;
            case 236: return 228;
            case 237: return 228;
            case 238: return 228;
            case 239: return 228;
            case 240: return 228;
            case 241: return 228;
            case 242: return 228;
            case 243: return 228;
            case 244: return 228;
            case 245: return 228;
            case 246: return 228;
            case 247: return 228;
            case 248: return 228;
            case 249: return 228;
            case 250: return 228;
            case 251: return 228;
            case 252: return 228;
            case 253: return 228;
            case 254: return 228;
            case 255: return 255;
            case 256: return 255;
            case 257: return 255;
            case 258: return 255;
            case 259: return 255;
            case 260: return 255;
            case 261: return 255;
            case 262: return 255;
            case 263: return 255;
            case 264: return 255;
            case 265: return 255;
            case 266: return 255;
            case 267: return 255;
            case 268: return 255;
            case 269: return 255;
            case 270: return 255;
            case 271: return 255;
            case 272: return 255;
            case 273: return 255;
            case 274: return 255;
            case 275: return 255;
            case 276: return 255;
            case 277: return 255;
            case 278: return 255;
            case 279: return 255;
            case 280: return 255;
            case 281: return 255;
            case 282: return 282;
            case 283: return 282;
            case 284: return 282;
            case 285: return 282;
            case 286: return 282;
            case 287: return 282;
            case 288: return 282;
            case 289: return 282;
            case 290: return 282;
            case 291: return 282;
            case 292: return 282;
            case 293: return 282;
            case 294: return 282;
            case 295: return 282;
            case 296: return 282;
            case 297: return 282;
            case 298: return 282;
            case 299: return 282;
            case 300: return 282;
            case 301: return 282;
            case 302: return 282;
            case 303: return 282;
            case 304: return 282;
            case 305: return 282;
            case 306: return 282;
            case 307: return 282;
            case 308: return 282;
            case 309: return 282;
            case 310: return 310;
            case 311: return 310;
            case 312: return 310;
            case 313: return 310;
            case 314: return 310;
            case 315: return 310;
            case 316: return 310;
            case 317: return 310;
            case 318: return 310;
            case 319: return 310;
            case 320: return 310;
            case 321: return 310;
            case 322: return 310;
            case 323: return 310;
            case 324: return 310;
            case 325: return 310;
            case 326: return 310;
            case 327: return 310;
            case 328: return 310;
            case 329: return 310;
            case 330: return 310;
            case 331: return 310;
            case 332: return 310;
            case 333: return 310;
            case 334: return 310;
            case 335: return 310;
            case 336: return 310;
            case 337: return 310;
            case 338: return 310;
            case 339: return 339;
            case 340: return 339;
            case 341: return 339;
            case 342: return 339;
            case 343: return 339;
            case 344: return 339;
            case 345: return 339;
            case 346: return 339;
            case 347: return 339;
            case 348: return 339;
            case 349: return 339;
            case 350: return 339;
            case 351: return 339;
            case 352: return 339;
            case 353: return 339;
            case 354: return 339;
            case 355: return 339;
            case 356: return 339;
            case 357: return 339;
            case 358: return 339;
            case 359: return 339;
            case 360: return 339;
            case 361: return 339;
            case 362: return 339;
            case 363: return 339;
            case 364: return 339;
            case 365: return 339;
            case 366: return 339;
            case 367: return 339;
            case 368: return 368;
            case 369: return 368;
            case 370: return 368;
            case 371: return 368;
            case 372: return 368;
            case 373: return 368;
            case 374: return 368;
            case 375: return 368;
            case 376: return 368;
            case 377: return 368;
            case 378: return 368;
            case 379: return 368;
            case 380: return 368;
            case 381: return 368;
            case 382: return 368;
            case 383: return 368;
            case 384: return 368;
            case 385: return 368;
            case 386: return 368;
            case 387: return 368;
            case 388: return 368;
            case 389: return 368;
            case 390: return 368;
            case 391: return 368;
            case 392: return 368;
            case 393: return 368;
            case 394: return 368;
            case 395: return 368;
            case 396: return 368;
            case 397: return 368;
            case 398: return 368;
            case 399: return 399;
            case 400: return 399;
            case 401: return 399;
            case 402: return 399;
            case 403: return 399;
            case 404: return 399;
            case 405: return 399;
            case 406: return 399;
            case 407: return 399;
            case 408: return 399;
            case 409: return 399;
            case 410: return 399;
            case 411: return 399;
            case 412: return 399;
            case 413: return 399;
            case 414: return 399;
            case 415: return 399;
            case 416: return 399;
            case 417: return 399;
            case 418: return 399;
            case 419: return 399;
            case 420: return 399;
            case 421: return 399;
            case 422: return 399;
            case 423: return 399;
            case 424: return 399;
            case 425: return 399;
            case 426: return 399;
            case 427: return 399;
            case 428: return 399;
            case 429: return 399;
            case 430: return 399;
            case 431: return 431;
            case 432: return 431;
            case 433: return 431;
            case 434: return 431;
            case 435: return 431;
            case 436: return 431;
            case 437: return 431;
            case 438: return 431;
            case 439: return 431;
            case 440: return 431;
            case 441: return 431;
            case 442: return 431;
            case 443: return 431;
            case 444: return 431;
            case 445: return 431;
            case 446: return 431;
            case 447: return 431;
            case 448: return 431;
            case 449: return 431;
            case 450: return 431;
            case 451: return 431;
            case 452: return 431;
            case 453: return 431;
            case 454: return 431;
            case 455: return 431;
            case 456: return 431;
            case 457: return 431;
            case 458: return 431;
            case 459: return 431;
            case 460: return 431;
            case 461: return 431;
            case 462: return 431;
            case 463: return 463;
            case 464: return 463;
            case 465: return 463;
            case 466: return 463;
            case 467: return 463;
            case 468: return 463;
            case 469: return 463;
            case 470: return 463;
            case 471: return 463;
            case 472: return 463;
            case 473: return 463;
            case 474: return 463;
            case 475: return 463;
            case 476: return 463;
            case 477: return 463;
            case 478: return 463;
            case 479: return 463;
            case 480: return 463;
            case 481: return 463;
            case 482: return 463;
            case 483: return 463;
            case 484: return 463;
            case 485: return 463;
            case 486: return 463;
            case 487: return 463;
            case 488: return 463;
            case 489: return 463;
            case 490: return 463;
            case 491: return 463;
            case 492: return 463;
            case 493: return 463;
            case 494: return 463;
            case 495: return 463;
            case 496: return 463;
            case 497: return 497;
            case 498: return 497;
            case 499: return 497;
            case 500: return 497;
            case 501: return 497;
            case 502: return 497;
            case 503: return 497;
            case 504: return 497;
            case 505: return 497;
            case 506: return 497;
            case 507: return 497;
            case 508: return 497;
            case 509: return 497;
            case 510: return 497;
            case 511: return 497;
            case 512: return 497;
            case 513: return 497;
            case 514: return 497;
            case 515: return 497;
            case 516: return 497;
            case 517: return 497;
            case 518: return 497;
            case 519: return 497;
            case 520: return 497;
            case 521: return 497;
            case 522: return 497;
            case 523: return 497;
            case 524: return 497;
            case 525: return 497;
            case 526: return 497;
            case 527: return 497;
            case 528: return 497;
            case 529: return 497;
            case 530: return 497;
            case 531: return 497;
            case 532: return 532;
            case 533: return 532;
            case 534: return 532;
            case 535: return 532;
            case 536: return 532;
            case 537: return 532;
            case 538: return 532;
            case 539: return 532;
            case 540: return 532;
            case 541: return 532;
            case 542: return 532;
            case 543: return 532;
            case 544: return 532;
            case 545: return 532;
            case 546: return 532;
            case 547: return 532;
            case 548: return 532;
            case 549: return 532;
            case 550: return 532;
            case 551: return 532;
            case 552: return 532;
            case 553: return 532;
            case 554: return 532;
            case 555: return 532;
            case 556: return 532;
            case 557: return 532;
            case 558: return 532;
            case 559: return 532;
            case 560: return 532;
            case 561: return 532;
            case 562: return 532;
            case 563: return 532;
            case 564: return 532;
            case 565: return 532;
            case 566: return 532;
            case 567: return 532;
            case 568: return 568;
            case 569: return 568;
            case 570: return 568;
            case 571: return 568;
            case 572: return 568;
            case 573: return 568;
            case 574: return 568;
            case 575: return 568;
            case 576: return 568;
            case 577: return 568;
            case 578: return 568;
            case 579: return 568;
            case 580: return 568;
            case 581: return 568;
            case 582: return 568;
            case 583: return 568;
            case 584: return 568;
            case 585: return 568;
            case 586: return 568;
            case 587: return 568;
            case 588: return 568;
            case 589: return 568;
            case 590: return 568;
            case 591: return 568;
            case 592: return 568;
            case 593: return 568;
            case 594: return 568;
            case 595: return 568;
            case 596: return 568;
            case 597: return 568;
            case 598: return 568;
            case 599: return 568;
            case 600: return 568;
            case 601: return 568;
            case 602: return 568;
            case 603: return 568;
            case 604: return 568;
            case 605: return 605;
            case 606: return 605;
            case 607: return 605;
            case 608: return 605;
            case 609: return 605;
            case 610: return 605;
            case 611: return 605;
            case 612: return 605;
            case 613: return 605;
            case 614: return 605;
            case 615: return 605;
            case 616: return 605;
            case 617: return 605;
            case 618: return 605;
            case 619: return 605;
            case 620: return 605;
            case 621: return 605;
            case 622: return 605;
            case 623: return 605;
            case 624: return 605;
            case 625: return 605;
            case 626: return 605;
            case 627: return 605;
            case 628: return 605;
            case 629: return 605;
            case 630: return 605;
            case 631: return 605;
            case 632: return 605;
            case 633: return 605;
            case 634: return 605;
            case 635: return 605;
            case 636: return 605;
            case 637: return 605;
            case 638: return 605;
            case 639: return 605;
            case 640: return 605;
            case 641: return 605;
            case 642: return 605;
            case 643: return 643;
            case 644: return 643;
            case 645: return 643;
            case 646: return 643;
            case 647: return 643;
            case 648: return 643;
            case 649: return 643;
            case 650: return 643;
            case 651: return 643;
            case 652: return 643;
            case 653: return 643;
            case 654: return 643;
            case 655: return 643;
            case 656: return 643;
            case 657: return 643;
            case 658: return 643;
            case 659: return 643;
            case 660: return 643;
            case 661: return 643;
            case 662: return 643;
            case 663: return 643;
            case 664: return 643;
            case 665: return 643;
            case 666: return 643;
            case 667: return 643;
            case 668: return 643;
            case 669: return 643;
            case 670: return 643;
            case 671: return 643;
            case 672: return 643;
            case 673: return 643;
            case 674: return 643;
            case 675: return 643;
            case 676: return 643;
            case 677: return 643;
            case 678: return 643;
            case 679: return 643;
            case 680: return 643;
            case 681: return 643;
            case 682: return 643;
            case 683: return 683;
            case 684: return 683;
            case 685: return 683;
            case 686: return 683;
            case 687: return 683;
            case 688: return 683;
            case 689: return 683;
            case 690: return 683;
            case 691: return 683;
            case 692: return 683;
            case 693: return 683;
            case 694: return 683;
            case 695: return 683;
            case 696: return 683;
            case 697: return 683;
            case 698: return 683;
            case 699: return 683;
            case 700: return 683;
            case 701: return 683;
            case 702: return 683;
            case 703: return 683;
            case 704: return 683;
            case 705: return 683;
            case 706: return 683;
            case 707: return 683;
            case 708: return 683;
            case 709: return 683;
            case 710: return 683;
            case 711: return 683;
            case 712: return 683;
            case 713: return 683;
            case 714: return 683;
            case 715: return 683;
            case 716: return 683;
            case 717: return 683;
            case 718: return 683;
            case 719: return 683;
            case 720: return 683;
            case 721: return 683;
            case 722: return 683;
            case 723: return 683;
            case 724: return 724;
            case 725: return 724;
            case 726: return 724;
            case 727: return 724;
            case 728: return 724;
            case 729: return 724;
            case 730: return 724;
            case 731: return 724;
            case 732: return 724;
            case 733: return 724;
            case 734: return 724;
            case 735: return 724;
            case 736: return 724;
            case 737: return 724;
            case 738: return 724;
            case 739: return 724;
            case 740: return 724;
            case 741: return 724;
            case 742: return 724;
            case 743: return 724;
            case 744: return 724;
            case 745: return 724;
            case 746: return 724;
            case 747: return 724;
            case 748: return 724;
            case 749: return 724;
            case 750: return 724;
            case 751: return 724;
            case 752: return 724;
            case 753: return 724;
            case 754: return 724;
            case 755: return 724;
            case 756: return 724;
            case 757: return 724;
            case 758: return 724;
            case 759: return 724;
            case 760: return 724;
            case 761: return 724;
            case 762: return 724;
            case 763: return 724;
            case 764: return 724;
            case 765: return 724;
            case 766: return 766;
            case 767: return 766;
            case 768: return 766;
            case 769: return 766;
            case 770: return 766;
            case 771: return 766;
            case 772: return 766;
            case 773: return 766;
            case 774: return 766;
            case 775: return 766;
            case 776: return 766;
            case 777: return 766;
            case 778: return 766;
            case 779: return 766;
            case 780: return 766;
            case 781: return 766;
            case 782: return 766;
            case 783: return 766;
            case 784: return 766;
            case 785: return 766;
            case 786: return 766;
            case 787: return 766;
            case 788: return 766;
            case 789: return 766;
            case 790: return 766;
            case 791: return 766;
            case 792: return 766;
            case 793: return 766;
            case 794: return 766;
            case 795: return 766;
            case 796: return 766;
            case 797: return 766;
            case 798: return 766;
            case 799: return 766;
            case 800: return 766;
            case 801: return 766;
            case 802: return 766;
            case 803: return 766;
            case 804: return 766;
            case 805: return 766;
            case 806: return 766;
            case 807: return 766;
            case 808: return 766;
            case 809: return 766;
            case 810: return 810;
            case 811: return 810;
            case 812: return 810;
            case 813: return 810;
            case 814: return 810;
            case 815: return 810;
            case 816: return 810;
            case 817: return 810;
            case 818: return 810;
            case 819: return 810;
            case 820: return 810;
            case 821: return 810;
            case 822: return 810;
            case 823: return 810;
            case 824: return 810;
            case 825: return 810;
            case 826: return 810;
            case 827: return 810;
            case 828: return 810;
            case 829: return 810;
            case 830: return 810;
            case 831: return 810;
            case 832: return 810;
            case 833: return 810;
            case 834: return 810;
            case 835: return 810;
            case 836: return 810;
            case 837: return 810;
            case 838: return 810;
            case 839: return 810;
            case 840: return 810;
            case 841: return 810;
            case 842: return 810;
            case 843: return 810;
            case 844: return 810;
            case 845: return 810;
            case 846: return 810;
            case 847: return 810;
            case 848: return 810;
            case 849: return 810;
            case 850: return 810;
            case 851: return 810;
            case 852: return 810;
            case 853: return 810;
            case 854: return 810;
            case 855: return 855;
            case 856: return 855;
            case 857: return 855;
            case 858: return 855;
            case 859: return 855;
            case 860: return 855;
            case 861: return 855;
            case 862: return 855;
            case 863: return 855;
            case 864: return 855;
            case 865: return 855;
            case 866: return 855;
            case 867: return 855;
            case 868: return 855;
            case 869: return 855;
            case 870: return 855;
            case 871: return 855;
            case 872: return 855;
            case 873: return 855;
            case 874: return 855;
            case 875: return 855;
            case 876: return 855;
            case 877: return 855;
            case 878: return 855;
            case 879: return 855;
            case 880: return 855;
            case 881: return 855;
            case 882: return 855;
            case 883: return 855;
            case 884: return 855;
            case 885: return 855;
            case 886: return 855;
            case 887: return 855;
            case 888: return 855;
            case 889: return 855;
            case 890: return 855;
            case 891: return 855;
            case 892: return 855;
            case 893: return 855;
            case 894: return 855;
            case 895: return 855;
            case 896: return 855;
            case 897: return 855;
            case 898: return 855;
            case 899: return 855;
            case 900: return 855;
            case 901: return 855;
            case 902: return 902;
            case 903: return 902;
            case 904: return 902;
            case 905: return 902;
            case 906: return 902;
            case 907: return 902;
            case 908: return 902;
            case 909: return 902;
            case 910: return 902;
            case 911: return 902;
            case 912: return 902;
            case 913: return 902;
            case 914: return 902;
            case 915: return 902;
            case 916: return 902;
            case 917: return 902;
            case 918: return 902;
            case 919: return 902;
            case 920: return 902;
            case 921: return 902;
            case 922: return 902;
            case 923: return 902;
            case 924: return 902;
            case 925: return 902;
            case 926: return 902;
            case 927: return 902;
            case 928: return 902;
            case 929: return 902;
            case 930: return 902;
            case 931: return 902;
            case 932: return 902;
            case 933: return 902;
            case 934: return 902;
            case 935: return 902;
            case 936: return 902;
            case 937: return 902;
            case 938: return 902;
            case 939: return 902;
            case 940: return 902;
            case 941: return 902;
            case 942: return 902;
            case 943: return 902;
            case 944: return 902;
            case 945: return 902;
            case 946: return 902;
            case 947: return 902;
            case 948: return 902;
            case 949: return 949;
            default: return 949;
        }
    }

    static boolean isSlandererInfluence(int influence) {
        switch(influence) {
            case 21: return true;
            case 41: return true;
            case 63: return true;
            case 85: return true;
            case 107: return true;
            case 130: return true;
            case 154: return true;
            case 178: return true;
            case 203: return true;
            case 228: return true;
            case 255: return true;
            case 282: return true;
            case 310: return true;
            case 339: return true;
            case 368: return true;
            case 399: return true;
            case 431: return true;
            case 463: return true;
            case 497: return true;
            case 532: return true;
            case 568: return true;
            case 605: return true;
            case 643: return true;
            case 683: return true;
            case 724: return true;
            case 766: return true;
            case 810: return true;
            case 855: return true;
            case 902: return true;
            case 949: return true;
            default: return false;
        }
    }
}