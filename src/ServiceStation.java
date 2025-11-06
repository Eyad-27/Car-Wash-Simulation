import java.util.Scanner;

public class ServiceStation {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        int waitingAreaCapacity = readIntInRange(scanner,
                "Enter waiting area capacity (1-10): ", 1, 10);

        int numPumps = readIntMin(scanner,
                "Enter number of pumps (>= 1): ", 1);

        int numCars = readIntMin(scanner,
                "Enter number of cars to simulate (>= 1): ", 1);

        System.out.println("\n--- Initializing Service Station ---");
        System.out.println("Waiting area capacity: " + waitingAreaCapacity);
        System.out.println("Number of pumps: " + numPumps);
        System.out.println("Number of cars: " + numCars);

        WaitingArea waitingArea = new WaitingArea(waitingAreaCapacity, numPumps);

        Pump[] pumpThreads = new Pump[numPumps];
        for (int i = 0; i < numPumps; i++) {
            pumpThreads[i] = new Pump(i + 1, waitingArea);
            pumpThreads[i].setDaemon(true);
            pumpThreads[i].start();
        }
        System.out.println("Started " + numPumps + " pump thread(s).\n");

        System.out.println("--- Spawning arriving cars ---");
        Thread[] carThreads = new Thread[numCars];
        for (int i = 0; i < numCars; i++) {
            Car car = new Car(i + 1, waitingArea);
            carThreads[i] = car;
            car.start();
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) { }
        }

        for (Thread t : carThreads) {
            try {
                t.join();
            } catch (InterruptedException ignored) { }
        }

        try {
            while (waitingArea.full.getValue() > 0 || waitingArea.pumps.getValue() < numPumps) {
                Thread.sleep(100);
            }
        } catch (InterruptedException ignored) { }

        System.out.println("All cars processed; simulation ends");
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
                scanner.next();
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
                scanner.next();
            }
        }
    }
}


class Semaphore {
    private int value;

    Semaphore(int v) {
        value = v;
    }

    synchronized void acquire() throws InterruptedException {
        while (value == 0) wait();
        value--;
    }

    synchronized void release() {
        value++;
        notify();
    }

    synchronized int getValue() {
        return value;
    }
}

// Buffer Class
class WaitingArea {
    int size;
    private int[] store;
    private int in = 0;
    private int out = 0;

    Semaphore empty;
    Semaphore full;
    Semaphore mutex;
    Semaphore pumps;


    WaitingArea(int size, int pump_size) {
        this.size = size;
        this.store = new int[size];
        this.empty = new Semaphore(size);
        this.full = new Semaphore(0);
        this.mutex = new Semaphore(1);
        this.pumps = new Semaphore(pump_size);
    }

    public void produce(int v) throws InterruptedException {
        empty.acquire();
        mutex.acquire();
        store[in] = v;
        in = (in + 1) % size;
        mutex.release();
        full.release();
    }
    public int consume() throws InterruptedException {
        int v;
        full.acquire();
        mutex.acquire();
        v = store[out];
        out = (out + 1) % size;
        mutex.release();
        empty.release();
        return v;
    }

}

class Car extends Thread {
    private final int id;
    private final WaitingArea queue;

    public Car(int id, WaitingArea queue) {
        this.id = id;
        this.queue = queue;
    }

    @Override
    public void run() {
        System.out.println("Car #" + id + " arrived.");
        try{
            queue.produce(id);
            System.out.println("Car #" + id + " entered the queue.");
        }catch (InterruptedException ignored) { }

    }
}

class Pump extends Thread {
    private final int id;
    private final WaitingArea queue;


    public Pump(int id, WaitingArea queue) {
        this.id =  id;
        this.queue = queue;
    }

    @Override
    public void run() {
        while(true) {
            try {
                int carId = queue.consume();
                System.out.println("Pump " + id + ": Car " + carId + " Occupied");
                System.out.println("Pump " + id + ": begins service at Bay " + id);
                queue.pumps.acquire();
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                queue.pumps.release();
                System.out.println("Pump " + id + ": finishes service for Car " + carId);
                System.out.println("Pump " + id + ": ends service at Bay " + id);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
