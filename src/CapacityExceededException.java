public class CapacityExceededException extends Exception {
    public CapacityExceededException(String message) {
        System.out.println("Capacity Exceeded.");
        super(message);
    }
}