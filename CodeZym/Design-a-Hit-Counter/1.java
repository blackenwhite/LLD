// ****** It's better to write code in your local code editor and paste it back here *********
// uses Semaphore
import java.util.*;
import java.util.concurrent.Semaphore;

// use helper.print("") or helper.println("") for printing logs else logs will not be visible.
public class Solution implements Q06WebpageVisitCounterInterface {
    private Helper06 helper;

    private int[] count;
    private int total;
    private Semaphore[] semaphores;

    public Solution(){}

    public void init(int totalPages, Helper06 helper){
        if(totalPages<0 || totalPages>1000) {
            throw new IllegalArgumentException("Invalid param: totalPages");
        }
        this.helper=helper;
        // helper.println("restaurant rating module initialized");
        count = new int[totalPages];
        total = totalPages;
        semaphores = new Semaphore[total];
        for(int i=0;i<total;i++) {
            semaphores[i] = new Semaphore(1);
        }
    }

    // increment visit count for pageIndex by 1
    public void incrementVisitCount(int pageIndex) {
        try{
            semaphores[pageIndex].acquire(1);
            count[pageIndex]++;
        } catch (Exception e) {
            // log or throw exception however is suited
        } finally {
            semaphores[pageIndex].release();
        }
    }

    // return total visit count for a given page
    public int getVisitCount(int pageIndex) {
        // allowing dirty read
        return count[pageIndex];
    }
}

// uncomment below code in case you are using your local ide like intellij, eclipse etc and
// comment it back again back when you are pasting completed solution in the online CodeZym editor.
// if you don't comment it back, you will get "java.lang.AssertionError: java.lang.LinkageError"
// This will help avoid unwanted compilation errors and get method autocomplete in your local code editor.
/**
interface Q06WebpageVisitCounterInterface {
    void init(int totalPages, Helper06 helper);
    void incrementVisitCount(int pageIndex);
    int getVisitCount(int pageIndex);
}

class Helper06 {
    void print(String s){System.out.print(s);}
    void println(String s){System.out.println(s);}
}
*/
