package musketeerplayer;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

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

    static int turnCount;
    static Direction main_direction;
    static boolean muckraker_Found_EC;
    static int robotCounter;
    static int politicianCounter;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        turnCount = 0;

        //System.out.println("I'm a " + rc.getType() + " and I just got created!");
        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to freeze
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You may rewrite this into your own control structure if you wish.
                //System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                switch (rc.getType()) {
                    case ENLIGHTENMENT_CENTER: runEnlightenmentCenter(); break;
                    case POLITICIAN:           runPolitician();          break;
                    case SLANDERER:            runSlanderer();           break;
                    case MUCKRAKER:            runMuckraker();           break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                //System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runEnlightenmentCenter() throws GameActionException {
        System.out.println("AI current influence: " + rc.getInfluence());
        int currRoundNum = rc.getRoundNum();
        int currInfluence = rc.getInfluence();
        int biddingInfluence = currInfluence / 10;
        assert rc.canBid(biddingInfluence);
        if (rc.canBid(biddingInfluence) && currRoundNum > 200) {
            rc.bid(biddingInfluence);
        }
        else {
            rc.bid(1);
        }
        //System.out.println("curr influence after bid:" + rc.getInfluence());
        //System.out.println("bidding influence:" + biddingInfluence);
        //System.out.println();
        RobotType toBuild;
        int influence;
        if (currRoundNum > 500) {
            if(robotCounter % 3 == 0){
                toBuild = RobotType.SLANDERER;
                influence = 50;
            }
            else if (robotCounter % 3 == 1){
                toBuild = RobotType.POLITICIAN;
                influence = currInfluence / 10;
            }
            else{
                toBuild = RobotType.MUCKRAKER;
                influence = 1;
            }
        } else {
            if(robotCounter % 2 == 0){
                toBuild = RobotType.SLANDERER;
                influence = 50;
            }
            else{
                toBuild = RobotType.MUCKRAKER;
                influence = 50;
            }
        }
        for (Direction dir : directions) {
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                rc.buildRobot(toBuild, dir, influence);
                robotCounter+=1;
            } else {
                break;
            }
        }
    }

    static void runPolitician() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
            //System.out.println("empowering...");
            rc.empower(actionRadius);
            //System.out.println("empowered");
            return;
        }
        if (tryMove(randomDirection()));
            //System.out.println("I moved!");
    }

    static double distanceSquared(MapLocation curr, MapLocation enemy) {
        return Math.pow(Math.abs(enemy.x - curr.x),2) + Math.pow(Math.abs(enemy.y - curr.y),2);
    }

    static Direction findDirection(MapLocation curr, MapLocation enemy) {
        int dy = curr.y - enemy.y;
        int dx = curr.x - enemy.x;
        //setting angle
        double angle;
        if (dx == 0 && dy > 0) {
            angle = 90;
        }
        else if (dx < 0 && dy == 0) {
            angle = 180;
        }
        else if (dx == 0 && dy < 0) {
            angle = 270;
        }
        else if (dx > 0 && dy == 0) {
            angle = 360;
        }
        else {
            angle = Math.toDegrees(Math.abs(Math.atan(dy/dx)));
        }

        //adding angle offsets
        if (dx < 0 && dy >= 0) {
            angle += 90;
        }
        else if (dx < 0 && dy < 0) {
            angle += 180;
        }
        else if (dx >= 0 && dy < 0) {
            angle += 270;
        }
        angle = angle % 360;

        //returning directions
        if (22.5 <= angle && angle < 67.5) {
            return Direction.NORTHEAST;
        }
        else if (67.5 <= angle && angle < 112.5) {
            return Direction.NORTH;
        }
        else if (112.5 <= angle && angle < 157.5) {
            return Direction.NORTHWEST;
        }
        else if (157.5 <= angle && angle < 202.5) {
            return Direction.WEST;
        }
        else if (202.5 <= angle && angle < 247.5) {
            return Direction.SOUTHWEST;
        }
        else if (247.5 <= angle && angle < 292.5) {
            return Direction.SOUTH;
        }
        else if (292.5 <= angle && angle < 337.5) {
            return Direction.SOUTHEAST;
        }
        else {
            return Direction.EAST;
        }
    }

    static void runSlanderer() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int sensorRadius = rc.getType().sensorRadiusSquared;
        RobotInfo[] enemiesInReach = rc.senseNearbyRobots(sensorRadius, enemy);
        RobotInfo minRobot = null;
        double minDistSquared = Integer.MAX_VALUE;
        MapLocation curr = rc.getLocation();
        for (RobotInfo robot : enemiesInReach) {
            double temp = distanceSquared(curr, robot.getLocation());
            if (temp < minDistSquared) {
                minDistSquared = temp;
                minRobot = robot;
            }
        }
        Direction toMove = randomDirection();
        if (minRobot != null) {
            toMove = findDirection(curr, minRobot.getLocation());
        }

        if (tryMove(toMove));
            // System.out.println("I moved!");
    }

    static void runMuckraker() throws GameActionException {
        if(main_direction == null){
            main_direction = randomDirection();
        }
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if (rc.canExpose(robot.location)) {
                    //System.out.println("e x p o s e d");
                    rc.expose(robot.location);
                    return;
                }
            }
            if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER){
                muckraker_Found_EC = true;
            }
        }
        if(!muckraker_Found_EC){
            while (!tryMove(main_direction)){
                main_direction = randomDirection();
            }
        }
            //System.out.println("I moved!");
    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Returns a random spawnable RobotType
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnableRobotType() {
        return spawnableRobot[(int) (Math.random() * spawnableRobot.length)];
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        //System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }
}
