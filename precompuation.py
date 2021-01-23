import math

def calculate_profit(x):
    return math.floor(x * (0.02 + 0.03 * math.exp(-0.001 * x))) * 50 - x

def calculate_best_profits():
    length = 950
    dp = [-100] * length
    dp[0] = calculate_profit(0)
    for x in range(1, length):
        dp[x] = x if calculate_profit(x) > calculate_profit(dp[x-1]) else dp[x-1]
    
    return dp;

def get_best_profit_switch():
    dp = calculate_best_profits()
    ret = "switch(influence) {\n"
    for x in range(0, len(dp)):
        ret += "    case " + str(x) + ": return " + str(dp[x]) + ";\n"
    ret += "}"
    return ret

def get_slanderer_influences():
    dp = calculate_best_profits()
    length = 950
    ret = [False] * length
    for x in dp:
        ret[x] = True
    
    return ret

def get_slanderer_influence_switch():
    ret = "switch(influence) {\n"
    dp = get_slanderer_influences()
    for x in range(0, len(dp)):
        if dp[x]:
            ret += "    case " + str(x) + ": return true;\n"
    ret += "}"

    return ret

def unroll(i, j, s):
    for x in range(0, i):
        for y in range(0, j):
            print(s.replace("[i]", "[" + str(x) + "]").replace("[j]", "[" + str(y) + "]"))
    
def unroll(i, s):
    for x in range(0, i):
        print(s.replace("[j]", "[" + str(x) + "]").replace("(j)", "(" + str(x) + ")"))

if __name__ == '__main__':
    s = get_best_profit_switch()
    print(s)