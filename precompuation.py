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

if __name__ == '__main__':
    dp = calculate_best_profits()
    print(dp)