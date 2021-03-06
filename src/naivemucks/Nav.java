package naivemucks;

import battlecode.common.*;

import naivemucks.fast.FasterQueue;
import naivemucks.fast.FastIterableIntSet;

public class Nav {
    static RobotController rc;

    static double baseCooldown;
    static FastIterableIntSet settled;
    static FasterQueue<Integer> q;

    static final int MAX = Integer.MAX_VALUE;
    static final int rows = 5;
    static final int cols = 5;
    static final int halfRows = rows / 2;
    static final int halfCols = cols / 2;

    static MapLocation[][] map;
    static double[][] costs;
    static double[][] cooldownPenalties;

    static MapLocation dest;
    static int closestDistanceToDest;
    static int turnsSinceClosestDistanceDecreased;

    static Direction lastExploreDir;
    static final int EXPLORE_BOREDOM = 5;
    static int boredom;

    static void init(RobotController r) {
        rc = r;
        baseCooldown = rc.getType().actionCooldown;
        settled = new FastIterableIntSet(25);
        q = new FasterQueue<Integer>(25);
        costs = new double[rows][cols];
        cooldownPenalties = new double[rows][cols];
        map = new MapLocation[rows][cols];
        dest = null;
        closestDistanceToDest = Integer.MAX_VALUE;
        turnsSinceClosestDistanceDecreased = 0;
        lastExploreDir = null;
    }

    static void setDest(MapLocation d) {
        dest = d;
        closestDistanceToDest = Integer.MAX_VALUE;
        turnsSinceClosestDistanceDecreased = 0;
    }

    static void updateLocalMap() throws GameActionException {
        MapLocation currLoc = rc.getLocation();
        MapLocation loc;
        for(int i = rows - 1; i >= 0; i--) {
            int dy = halfRows - i;
            for(int j = cols - 1; j >= 0; j--) {
                int dx = j - halfCols;
                loc = currLoc.translate(dx, dy);
                if(rc.canSenseLocation(loc) && rc.senseRobotAtLocation(loc) == null) {
                    map[i][j] = loc;
                    // Debug.setIndicatorDot(Debug.pathfinding, loc, 0, 100, 255);
                } else {
                    map[i][j] = null;
                }
            }
        }
    }

    static double heuristic(MapLocation loc1, MapLocation loc2) {
        if(loc1 == null || loc2 == null)
            return MAX;
        int dx = Math.abs(loc1.x - loc2.x);
        int dy = Math.abs(loc1.y - loc2.y);
        return Math.max(dx, dy);
    }

    // Clean, rolled up version of gradientDescent
    // static Direction gradientDescent(MapLocation dest) throws GameActionException {
    //     System.out.println("Bytecode at beginning: " + Clock.getBytecodeNum());
    //     rc.setIndicatorDot(dest, 255, 255, 255);
        
    //     updateLocalMap();
    //     MapLocation src = rc.getLocation();
    //     System.out.println("Bytecode after updateLocalMap: " + Clock.getBytecodeNum());
    //     int rows = map.length;
    //     int cols = map[0].length;

    //     for(int i = 0; i < rows; i++) {
    //         for(int j = 0; j < cols; j++) {
    //             costs[i][j] = MAX;
    //             MapLocation loc = map[i][j];
    //             if(loc != null) {
    //                 cooldownPenalties[i][j] = baseCooldown / rc.sensePassability(loc);
    //             }
    //         }
    //     }
    //     System.out.println("Bytecode after initializing cost array: " + Clock.getBytecodeNum());

    //     settled.clear();
    //     q.clear();

    //     System.out.println("Heuristic call before: " + Clock.getBytecodeNum());
    //     heuristic(map[0][0], dest);
    //     System.out.println("Heuristic call after: " + Clock.getBytecodeNum());


    //     // Init borders. TODO: unroll loops if bytecode heavy
    //     for(int i = 0; i < rows; i++) {
    //         int enc = i * rows;
    //         costs[i][0] = heuristic(map[i][0], dest);
    //         q.add(enc);
    //         settled.add(enc);
    //     }
    //     for(int i = 0; i < rows; i++) {
    //         int enc = i * rows + cols - 1;
    //         costs[i][cols - 1] = heuristic(map[i][cols - 1], dest);
    //         q.add(enc);
    //         settled.add(enc);
    //     }
    //     for(int j = 1; j < cols - 1; j++) {
    //         costs[0][j] = heuristic(map[0][j], dest);
    //         q.add(j);
    //         settled.add(j);
    //     }
    //     for(int j = 1; j < cols - 1; j++) {
    //         int enc = rows * (rows - 1) + j;
    //         costs[rows - 1][j] = heuristic(map[rows - 1][j], dest);
    //         q.add(enc);
    //         settled.add(enc);
    //     }
        
    //     System.out.println("Bytecode after initializing heuristics in cost array: " + Clock.getBytecodeNum());
        

    //     System.out.println("Heuristic Costs");
    //     StringBuilder s = new StringBuilder();
    //     for(int i = 0; i < rows; i++) {
    //         s.append("[");
    //         for(int j = 0; j < cols; j++) {
    //             s.append(String.format("%.2f", costs[i][j])).append(", ");
    //         }
    //         s.append("]\n");
    //     }
    //     System.out.println(s.toString());

    //     System.out.println("Bytecode before enetering while loop" + Clock.getBytecodeNum());
    //     while(!q.isEmpty()) {
    //         System.out.println("Bytecode at loop iter entry" + Clock.getBytecodeNum());
    //         System.out.println("Settled size: " + settled.size);
    //         System.out.println("Q Size: " + q.size());
    //         int temp = q.poll();
    //         int i = temp / rows;
    //         int j = temp % rows;
    //         if(map[i][j] == null)
    //             continue;
            
    //         for(int dx = -1; dx <= 1; dx++) {
    //             for(int dy = -1; dy <= 1; dy++) {
    //                 if(dx == 0 && dy == 0)
    //                     continue;

    //                 int i2 = i + dx;
    //                 int j2 = j + dy;
    //                 if(0 <= i2 && i2 < rows && 0 <= j2 && j2 < cols) {
    //                     if(map[i2][j2] != null) {
    //                         costs[i2][j2] = Math.min(costs[i][j] + cooldownPenalties[i2][j2], costs[i2][j2]);

    //                         int enc = i2 * rows + j2;
    //                         if(!settled.contains(enc)) {
    //                             settled.add(enc);
    //                             q.add(enc);
    //                         }
    //                     }
    //                 }
    //             }
    //         }
    //         System.out.println("Bytecode at loop iter exit" + Clock.getBytecodeNum());
    //     }
    //     System.out.println("Bytecode after while loop" + Clock.getBytecodeNum());


    //     System.out.println("Final Costs");
    //     s = new StringBuilder();
    //     for(int i = 0; i < rows; i++) {
    //         s.append("[");
    //         for(int j = 0; j < cols; j++) {
    //             s.append(String.format("%.2f", costs[i][j])).append(", ");
    //         }
    //         s.append("]\n");
    //     }
    //     System.out.println(s.toString());

    //     System.out.println("Bytecode at find min entry" + Clock.getBytecodeNum());
    //     MapLocation minLoc = src;
    //     double minCost = MAX;
    //     for(int i = 1; i < 4; i++) {
    //         for(int j = 1; j < 4; j++) {
    //             if(costs[i][j] < minCost) {
    //                 minCost = costs[i][j];
    //                 minLoc = map[i][j];
    //             }
    //         }
    //     }
    //     System.out.println("Bytecode at find min exit" + Clock.getBytecodeNum());

    //     return src.directionTo(minLoc);
    // }

    // Note: Takes 12500 BC
    // Unrolled version of gradientDescent
    static Direction gradientDescent() throws GameActionException {
        // System.out.println("Bytecode at beginning: " + Clock.getBytecodeNum());
        updateLocalMap();
        MapLocation src = rc.getLocation();

        int dist = src.distanceSquaredTo(dest);
        if(dist < closestDistanceToDest) {
            closestDistanceToDest = dist;
            turnsSinceClosestDistanceDecreased = 0;
        } else {
            turnsSinceClosestDistanceDecreased++;
        }

        if(turnsSinceClosestDistanceDecreased >= 2) {
            Debug.println(Debug.pathfinding, "Gradient descent failed to get closer in two turns: Falling back to directionTo");
            return src.directionTo(dest);
        } else {
            Debug.println(Debug.pathfinding, "Doing gradient descent normally");
        }

        // System.out.println("Bytecode after updateLocalMap: " + Clock.getBytecodeNum());
        int i;
        int j;
        MapLocation loc;
        for(i = rows - 1; i >= 0; i--) {
            for(j = cols - 1; j >= 0; j--) {
                costs[i][j] = MAX;
                loc = map[i][j];
                if(loc != null) {
                    cooldownPenalties[i][j] = baseCooldown / rc.sensePassability(loc);
                }
            }
        }

        // System.out.println("Bytecode after initializing cost array: " + Clock.getBytecodeNum());

        settled.clear();
        q.clear();

        // Init borders
        int enc;
        for(i = rows - 1; i >= 0; i--) {
            enc = i * rows;
            costs[i][0] = heuristic(map[i][0], dest);
            q.add(enc);
            settled.add(enc);
        }
        for(i = rows - 1; i >= 0; i--) {
            enc = i * rows + cols - 1;
            costs[i][cols - 1] = heuristic(map[i][cols - 1], dest);
            q.add(enc);
            settled.add(enc);
        }
        for(j = 1; j < cols - 1; j++) {
            costs[0][j] = heuristic(map[0][j], dest);
            q.add(j);
            settled.add(j);
        }
        for(j = 1; j < cols - 1; j++) {
            enc = rows * (rows - 1) + j;
            costs[rows - 1][j] = heuristic(map[rows - 1][j], dest);
            q.add(enc);
            settled.add(enc);
        }

        // System.out.println("Bytecode after initializing heuristics in cost array: " + Clock.getBytecodeNum());
        

        // System.out.println("Heuristic Costs");
        // StringBuilder s = new StringBuilder();
        // for(int i = 0; i < rows; i++) {
        //     s.append("[");
        //     for(int j = 0; j < cols; j++) {
        //         s.append(String.format("%.2f", costs[i][j])).append(", ");
        //     }
        //     s.append("]\n");
        // }
        // System.out.println(s.toString());

        // System.out.println("Bytecode before enetering while loop" + Clock.getBytecodeNum());
        
        int i2;
        int j2;
        int temp;
        while(!q.isEmpty()) {
            // System.out.println("Bytecode at loop iter entry" + Clock.getBytecodeNum());
            // System.out.println("Settled size: " + settled.size);
            // System.out.println("Q Size: " + q.size());
            temp = q.poll();
            i = temp / rows;
            j = temp % rows;
            if(map[i][j] == null)
                continue;
            i2 = i - 1;
            if(0 <= i2) {
                j2 = j - 1;
                if(0 <= j2) {
                    if(map[i2][j2] != null) {
                        costs[i2][j2] = Math.min(costs[i][j] + cooldownPenalties[i2][j2], costs[i2][j2]);
    
                        enc = i2 * rows + j2;
                        if(!settled.contains(enc)) {
                            settled.add(enc);
                            q.add(enc);
                        }
                    }
                }
                
                j2 = j;
                if(map[i2][j2] != null) {
                    costs[i2][j2] = Math.min(costs[i][j] + cooldownPenalties[i2][j2], costs[i2][j2]);

                    enc = i2 * rows + j2;
                    if(!settled.contains(enc)) {
                        settled.add(enc);
                        q.add(enc);
                    }
                }

                j2 = j + 1;
                if(j2 < cols) {
                    if(map[i2][j2] != null) {
                        costs[i2][j2] = Math.min(costs[i][j] + cooldownPenalties[i2][j2], costs[i2][j2]);
    
                        enc = i2 * rows + j2;
                        if(!settled.contains(enc)) {
                            settled.add(enc);
                            q.add(enc);
                        }
                    }
                }
            }
            
            
            i2 = i + 1;
            if(i2 < rows) {
                j2 = j - 1;
                if(0 <= j2) {
                    if(map[i2][j2] != null) {
                        costs[i2][j2] = Math.min(costs[i][j] + cooldownPenalties[i2][j2], costs[i2][j2]);
    
                        enc = i2 * rows + j2;
                        if(!settled.contains(enc)) {
                            settled.add(enc);
                            q.add(enc);
                        }
                    }
                }
                
                j2 = j;
                if(map[i2][j2] != null) {
                    costs[i2][j2] = Math.min(costs[i][j] + cooldownPenalties[i2][j2], costs[i2][j2]);

                    enc = i2 * rows + j2;
                    if(!settled.contains(enc)) {
                        settled.add(enc);
                        q.add(enc);
                    }
                }

                j2 = j + 1;
                if(j2 < cols) {
                    if(map[i2][j2] != null) {
                        costs[i2][j2] = Math.min(costs[i][j] + cooldownPenalties[i2][j2], costs[i2][j2]);
    
                        enc = i2 * rows + j2;
                        if(!settled.contains(enc)) {
                            settled.add(enc);
                            q.add(enc);
                        }
                    }
                }
            }
            
            i2 = i;
            j2 = j + 1;
            if(j2 < cols) {
                if(map[i2][j2] != null) {
                    costs[i2][j2] = Math.min(costs[i][j] + cooldownPenalties[i2][j2], costs[i2][j2]);

                    enc = i2 * rows + j2;
                    if(!settled.contains(enc)) {
                        settled.add(enc);
                        q.add(enc);
                    }
                }
            }
            
            j2 = j - 1;
            if(0 <= j2) {
                if(map[i2][j2] != null) {
                    costs[i2][j2] = Math.min(costs[i][j] + cooldownPenalties[i2][j2], costs[i2][j2]);

                    enc = i2 * rows + j2;
                    if(!settled.contains(enc)) {
                        settled.add(enc);
                        q.add(enc);
                    }
                }
            }
            
            // System.out.println("Bytecode at loop iter exit" + Clock.getBytecodeNum());
        }
        // System.out.println("Bytecode after while loop" + Clock.getBytecodeNum());


        // System.out.println("Final Costs");
        // s = new StringBuilder();
        // for(int i = 0; i < rows; i++) {
        //     s.append("[");
        //     for(int j = 0; j < cols; j++) {
        //         s.append(String.format("%.2f", costs[i][j])).append(", ");
        //     }
        //     s.append("]\n");
        // }
        // System.out.println(s.toString());

        // System.out.println("Bytecode at find min entry" + Clock.getBytecodeNum());
        MapLocation minLoc = src;
        double minCost = MAX;
        for(i = 1; i < 4; i++) {
            for(j = 1; j < 4; j++) {
                if(costs[i][j] < minCost) {
                    minCost = costs[i][j];
                    minLoc = map[i][j];
                }
            }
        }
        // System.out.println("Bytecode at find min exit" + Clock.getBytecodeNum());

        return src.directionTo(minLoc);
    }
    
	public static void explore() throws GameActionException {
		Debug.println(Debug.pathfinding, "Exploring");
		if(lastExploreDir == null) {
			lastExploreDir = Util.randomDirection();
			boredom = 0;
		}
		if(boredom >= EXPLORE_BOREDOM) {
            boredom = 0;
            Direction[] newDirChoices = {
                lastExploreDir.rotateLeft().rotateLeft(),
                lastExploreDir.rotateLeft(),
                lastExploreDir,
                lastExploreDir.rotateRight(),
                lastExploreDir.rotateRight().rotateRight()};
            // Direction[] newDirChoices = {
            //     lastExploreDir.rotateLeft(),
            //     lastExploreDir,
            //     lastExploreDir.rotateRight(),};
			lastExploreDir = newDirChoices[(int) (Math.random() * newDirChoices.length)];
		}
        boredom++;
        
		if(!rc.onTheMap(rc.getLocation().add(lastExploreDir))) {
            // lastExploreDir = lastExploreDir.opposite();
            lastExploreDir = Util.randomDirection();
        }
        
		if (rc.canMove(lastExploreDir)) {
			rc.move(lastExploreDir);
			return;
        }
        
        Direction[] orderedDirs = {lastExploreDir.rotateLeft(), lastExploreDir.rotateRight(), 
                                    lastExploreDir.rotateLeft().rotateLeft(), lastExploreDir.rotateRight().rotateRight()};
		for (Direction dir : orderedDirs) {
			if (rc.canMove(dir)) {
				rc.move(dir);
				return;
			}
		}
	}
}