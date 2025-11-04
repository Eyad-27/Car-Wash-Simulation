import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

public class ServiceStation {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        int waitingAreaCapacity = readIntInRange(scanner,
                "Enter waiting area capacity (1-10): ", 1, 10);

        int numPumps = readIntMin(scanner,
                "Enter number of pumps (>= 1): ", 1);

        System.out.println("\n--- Initializing Service Station ---");
        System.out.println("Waiting area capacity: " + waitingAreaCapacity);
        System.out.println("Number of pumps: " + numPumps);

        // Shared resources
        Queue<Car> waitingQueue = new LinkedList<>();
        Semaphore empty = new Semaphore(waitingAreaCapacity);
        Semaphore full = new Semaphore(0);
        Semaphore mutex = new Semaphore(1);
        Semaphore pumps = new Semaphore(numPumps);

        // Create and start Pump threads
        Pump[] pumpThreads = new Pump[numPumps];
        for (int i = 0; i < numPumps; i++) {
            pumpThreads[i] = new Pump(i + 1, waitingQueue, empty, full, mutex, pumps);
            pumpThreads[i].start();
        }
        System.out.println("Started " + numPumps + " pump thread(s).\n");

        // Create and start Car threads (producer stream)
        System.out.println("--- Spawning arriving cars ---");
        Thread[] carThreads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            Car car = new Car(i + 1, waitingQueue, empty, full, mutex);
            carThreads[i] = car;
            car.start();
            try {
                Thread.sleep(250); // Small delay between arrivals
            } catch (InterruptedException ignored) { }
        }

        // Optional: wait for car threads to finish (they are templates and do nothing yet)
        for (Thread t : carThreads) {
            try {
                t.join();
            } catch (InterruptedException ignored) { }
        }


    }

    private static int readIntInRange(Scanner scanner, String prompt, int min, int max) {
        int val;
        while (true) {
            System.out.print(prompt);
            if (scanner.hasNextInt()) {
                val = scanner.nextInt();
                if (val >= min && val <= max) return val;
                System.out.println("Please enter a number between " + min + " and " + max + ".");
            } else {
                System.out.println("Invalid input. Please enter an integer.");
                scanner.next(); // consume invalid token
            }
        }
    }

    private static int readIntMin(Scanner scanner, String prompt, int min) {
        int val;
        while (true) {
            System.out.print(prompt);
            if (scanner.hasNextInt()) {
                val = scanner.nextInt();
                if (val >= min) return val;
                System.out.println("Please enter a number greater than or equal to " + min + ".");
            } else {
                System.out.println("Invalid input. Please enter an integer.");
                scanner.next(); // consume invalid token
            }
        }
    }
}

// ---------------------------
// Stub classes for colleagues
// ---------------------------

// NOTE: Intentionally left as stubs. Implementations should follow the assignment specs.
class Semaphore {
    // Placeholder state if needed later
    private int value;

    public Semaphore(int initialValue) {
        this.value = initialValue;
    }

    // TODO: Implement per assignment.
    // Required signatures mentioned in the assignment are intentionally omitted here
    // to avoid conflicts with Object.wait(). Add synchronized wait() and signal()
    // with the exact semantics when you implement this class.
}

class Car extends Thread {
    private final int id;
    private final Queue<Car> waitingQueue;
    private final Semaphore empty;
    private final Semaphore full;
    private final Semaphore mutex;

    public Car(int id, Queue<Car> waitingQueue, Semaphore empty, Semaphore full, Semaphore mutex) {
        this.id = id;
        this.waitingQueue = waitingQueue;
        this.empty = empty;
        this.full = full;
        this.mutex = mutex;
    }

    @Override
    public void run() {
        // TEMPLATE (to be implemented by colleagues):
        // System.out.println("Car " + id + " arrived");
        // empty.wait();           // Wait for an empty spot in the queue
        // mutex.wait();           // Acquire lock to access the queue
        // waitingQueue.add(this);
        // System.out.println("Car " + id + " entered the queue");
        // mutex.signal();         // Release the lock
        // full.signal();          // Notify pumps that a car is available
    }
}

class Pump extends Thread {
    private final int id;
    private final Queue<Car> waitingQueue;
    private final Semaphore empty;
    private final Semaphore full;
    private final Semaphore mutex;
    private final Semaphore pumps;

    public Pump(int id, Queue<Car> waitingQueue, Semaphore empty, Semaphore full, Semaphore mutex, Semaphore pumps) {
        this.id = id;
        this.waitingQueue = waitingQueue;
        this.empty = empty;
        this.full = full;
        this.mutex = mutex;
        this.pumps = pumps;
    }

    @Override
    public void run() {
        // TEMPLATE (to be implemented by colleagues):
        // while (true) {
        //     pumps.wait();        // Wait for an available service bay
        //     full.wait();         // Wait for a car to be in the queue
        //     mutex.wait();        // Acquire lock to access the queue
        //     Car car = waitingQueue.poll();
        //     mutex.signal();      // Release the lock
        //     empty.signal();      // Signal that a queue spot is now free
        //
        //     // --- Start Service ---
        //     System.out.println("Pump " + id + ": Car " + (car != null ? car.id : "?") + " Occupied");
        //     System.out.println("Pump " + id + ": begins service at Bay " + id);
        //     try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        //     // --- Finish Service ---
        //     System.out.println("Pump " + id + ": finishes service");
        //     System.out.println("Pump " + id + ": Bay " + id + " is now free");
        //     pumps.signal();      // Release the service bay
        // }
    }
}
