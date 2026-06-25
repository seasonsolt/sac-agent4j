public final class CalculatorTest {
    public static void main(String[] args) {
        assertEquals(5, Calculator.add(2, 3), "2 + 3 should equal 5");
        assertEquals(0, Calculator.add(-1, 1), "-1 + 1 should equal 0");
        System.out.println("CalculatorTest passed");
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + " but got " + actual);
        }
    }
}
