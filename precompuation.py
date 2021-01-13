import math

def calculate_profit(x):
    return math.floor(x * (0.02 + 0.03 * math.exp(-0.001 * x))) * 50 - x

def calculate_best_profits():
    length = 950;
    dp = [-100] * length;
    dp[0] = calculate_profit(0);
    for x in range(1, length):
        dp[x] = x if calculate_profit(x) > calculate_profit(dp[x-1]) else dp[x-1]
    
    return dp;

def unroll(i, j, s):
    for x in range(0, i):
        for y in range(0, j):
            print(s.replace("[i]", "[" + str(x) + "]").replace("[j]", "[" + str(y) + "]"))
    
def unroll(i, s):
    for x in range(0, i):
        print(s.replace("[j]", "[" + str(x) + "]").replace("(j)", "(" + str(x) + ")"))

if __name__ == '__main__':
    s = """
        enc++;
        costs[rows - 1][j] = heuristic(map[rows - 1][j], dest);
        q.add(enc);
        settled.add(enc);"""
    
    unroll(5, s);