package musketeerplayersprint;

public class Debug {
    static final boolean verbose = true;
    public static final boolean critical = true;
    public static final boolean info = true;

    static void println(boolean cond, String s) {
        if(verbose && cond) {
            System.out.println(s);
        }
    }
    
    static void print(boolean cond, String s) {
        if(verbose && cond) {
            System.out.print(s);
        }
    }
}