import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.ThreadLocalRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class Philosopher extends Thread {
    private int id;
    private Lock leftFork, rightFork;
    private static final int THINKING_TIME = 10000; // Max thinking time (10 seconds)
    private static final int EATING_TIME = 5000; // Max eating time (5 seconds)
    private Table table;

    public Philosopher(int id, Lock leftFork, Lock rightFork, Table table) {
        this.id = id;
        this.leftFork = leftFork;
        this.rightFork = rightFork;
        this.table = table;
    }

    private void think() throws InterruptedException {
        System.out.println("Philosopher " + id + " is thinking.");
        Thread.sleep(ThreadLocalRandom.current().nextInt(THINKING_TIME)); // Random thinking time
    }

    private void eat() throws InterruptedException {
        System.out.println("Philosopher " + id + " is eating.");
        Thread.sleep(ThreadLocalRandom.current().nextInt(EATING_TIME)); // Random eating time
    }

    @Override
    public void run() {
        try {
            while (!table.isSixthTableDeadlocked()) {
                think();
                
                // Try to acquire the left fork
                leftFork.lock();
                System.out.println("Philosopher " + id + " picked up left fork.");
                
                Thread.sleep(4000); // Wait 4 seconds before picking the right fork

                // Try to acquire the right fork
                if (rightFork.tryLock()) {
                    try {
                        System.out.println("Philosopher " + id + " picked up right fork.");
                        eat();
                    } finally {
                        rightFork.unlock();
                        System.out.println("Philosopher " + id + " put down right fork.");
                    }
                }
                
                // Put down the left fork
                leftFork.unlock();
                System.out.println("Philosopher " + id + " put down left fork.");

                // If deadlock detected at table, philosopher moves to the sixth table
                if (table.checkForDeadlock()) {
                    table.movePhilosopherToSixthTable(this);
                    break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public int getIdNumber() {
        return id;
    }
}

class Table {
    private Lock[] forks;
    private List<Philosopher> sixthTablePhilosophers = new ArrayList<>();
    private boolean sixthTableDeadlocked = false;
    private int lastMovedPhilosopherId = -1;
    
    public Table(int philosopherCount) {
        forks = new ReentrantLock[philosopherCount];
        for (int i = 0; i < philosopherCount; i++) {
            forks[i] = new ReentrantLock();
        }
    }

    public Lock getLeftFork(int philosopherIndex) {
        return forks[philosopherIndex];
    }

    public Lock getRightFork(int philosopherIndex) {
        return forks[(philosopherIndex + 1) % forks.length];
    }

    // Check if deadlock occurred by checking if all philosophers are holding one fork and waiting for another
    public synchronized boolean checkForDeadlock() {
        int lockedForks = 0;
        for (Lock fork : forks) {
            if (fork.tryLock()) {
                fork.unlock();
            } else {
                lockedForks++;
            }
        }
        return lockedForks == forks.length; // All forks are locked (i.e., all philosophers are holding one fork)
    }

    // Move a philosopher to the sixth table when deadlock occurs
    public synchronized void movePhilosopherToSixthTable(Philosopher philosopher) {
        System.out.println("Philosopher " + philosopher.getIdNumber() + " moves to the sixth table.");
        sixthTablePhilosophers.add(philosopher);
        lastMovedPhilosopherId = philosopher.getIdNumber();
        if (sixthTablePhilosophers.size() == 5) {
            sixthTableDeadlocked = true;
        }
    }

    public boolean isSixthTableDeadlocked() {
        return sixthTableDeadlocked;
    }

    public int getLastMovedPhilosopherId() {
        return lastMovedPhilosopherId;
    }
}

public class DiningPhilosophers {
    private static final int PHILOSOPHER_COUNT = 5;

    public static void main(String[] args) throws InterruptedException {
        Table[] tables = new Table[6];
        
        // Create 5 tables with 5 philosophers each
        for (int i = 0; i < 6; i++) {
            tables[i] = new Table(PHILOSOPHER_COUNT);
        }
        
        // Start philosophers at each table
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < PHILOSOPHER_COUNT; j++) {
                Lock leftFork = tables[i].getLeftFork(j);
                Lock rightFork = tables[i].getRightFork(j);
                Philosopher philosopher = new Philosopher(j + i * PHILOSOPHER_COUNT, leftFork, rightFork, tables[i]);
                philosopher.start();
            }
        }

        // Monitor deadlock at the sixth table
        while (!tables[5].isSixthTableDeadlocked()) {
            Thread.sleep(1000); // Polling interval to check deadlock
        }
        
        System.out.println("Sixth table is deadlocked. Last philosopher moved: Philosopher " + tables[5].getLastMovedPhilosopherId());
    }
}
