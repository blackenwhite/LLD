import java.util.concurrent.atomic.AtomicIntegerArray;
// using AtomicIntegerArray

public class Solution implements Q06WebpageVisitCounterInterface {
    private AtomicIntegerArray count;
    private Helper06 helper;

    public void init(int totalPages, Helper06 helper) {
        if (totalPages < 0 || totalPages > 1000) {
            throw new IllegalArgumentException("Invalid totalPages");
        }

        this.helper = helper;
        this.count = new AtomicIntegerArray(totalPages);
    }

    public void incrementVisitCount(int pageIndex) {
        validatePageIndex(pageIndex);
        count.incrementAndGet(pageIndex);
    }

    public int getVisitCount(int pageIndex) {
        validatePageIndex(pageIndex);
        return count.get(pageIndex);
    }

    private void validatePageIndex(int pageIndex) {
        if (count == null) {
            throw new IllegalStateException("Counter not initialized");
        }

        if (pageIndex < 0 || pageIndex >= count.length()) {
            throw new IndexOutOfBoundsException("Invalid page index");
        }
    }
}
