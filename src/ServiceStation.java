import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;


public class ServiceStation extends JFrame {

    // --- GUI Components ---
    private JTextField queueCapacityField;
    private JTextField numPumpsField;
    private JTextField numCarsField;
    private JButton startButton;
    private JButton stopButton;
    private JTextPane logPane;
    private JPanel pumpStatusContainer;
    private JPanel queueVisualizationPanel;
    private JLabel queueCountLabel;
    private final List<PumpPanel> pumpPanels;

    private int waitingAreaCapacity;
    private int numPumps;
    private int totalCars;
    private WaitingArea waitingArea; 
    private Semaphore pumps; 

    private Thread[] pumpThreads;
    private Thread[] carThreads;
    private volatile boolean simulationRunning = false;
    private int carsServedCount = 0;

    //--- Colors---
    private static final Color COLOR_IDLE = new Color(0x10, 0xB9, 0x81);
    private static final Color COLOR_BUSY = new Color(0xF9, 0x73, 0x16);
    private static final Color COLOR_OFFLINE = new Color(0xE5, 0xE7, 0xEB);
    private static final Color COLOR_BG = new Color(0xF9, 0xFA, 0xFB);

    private static final Color EVENT_ARRIVED = new Color(0xF59E0B);
    private static final Color EVENT_WAITING = new Color(0xFBBF24);
    private static final Color EVENT_SERVICE = new Color(0x3B82F6);
    private static final Color EVENT_COMPLETE = new Color(0x10B981);
    private static final Color EVENT_SYSTEM = new Color(0x6B7280);

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Could not set system look and feel.");
        }

        SwingUtilities.invokeLater(() -> {
            ServiceStation station = new ServiceStation();
            station.setVisible(true);
        });
    }

    public ServiceStation() {
        setTitle("Service Station Simulator (Semaphore)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(COLOR_BG);

        pumpPanels = new ArrayList<>();

        JPanel mainContainer = new JPanel(new BorderLayout(15, 15));
        mainContainer.setBackground(COLOR_BG);
        mainContainer.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel inputPanel = createInputPanel();
        mainContainer.add(inputPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 15, 15));
        centerPanel.setBackground(COLOR_BG);

        JPanel queuePanel = createQueuePanel();
        centerPanel.add(queuePanel);

        JPanel pumpPanel = createPumpPanel();
        centerPanel.add(pumpPanel);

        mainContainer.add(centerPanel, BorderLayout.CENTER);

        JPanel logPanel = createLogPanel();
        mainContainer.add(logPanel, BorderLayout.SOUTH);

        add(mainContainer);

        startButton.addActionListener(new StartButtonListener());
        stopButton.addActionListener(new StopButtonListener());
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5, 0xE7, 0xEB), 1),
                new EmptyBorder(15, 15, 15, 15)
        ));

        Font labelFont = new Font("Dialog", Font.BOLD, 13);

        JLabel titleLabel = new JLabel("Simulation Controls");
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 16));
        panel.add(titleLabel);
        panel.add(Box.createHorizontalStrut(20));

        JLabel qLabel = new JLabel("Waiting Area Capacity (1-10):");
        qLabel.setFont(labelFont);
        panel.add(qLabel);
        queueCapacityField = new JTextField("5", 4);
        queueCapacityField.setFont(new Font("Dialog", Font.PLAIN, 13));
        panel.add(queueCapacityField);

        panel.add(Box.createHorizontalStrut(10));

        JLabel pLabel = new JLabel("Pumps (>= 1):");
        pLabel.setFont(labelFont);
        panel.add(pLabel);
        numPumpsField = new JTextField("3", 4);
        numPumpsField.setFont(new Font("Dialog", Font.PLAIN, 13));
        panel.add(numPumpsField);

        panel.add(Box.createHorizontalStrut(10));

        JLabel cLabel = new JLabel("Total Cars (>= 1):");
        cLabel.setFont(labelFont);
        panel.add(cLabel);
        numCarsField = new JTextField("15", 4);
        numCarsField.setFont(new Font("Dialog", Font.PLAIN, 13));
        panel.add(numCarsField);

        panel.add(Box.createHorizontalStrut(20));

        startButton = new JButton("Start");
        startButton.setFont(new Font("Dialog", Font.BOLD, 13));
        startButton.setBackground(new Color(0x10, 0xB9, 0x81));
        startButton.setForeground(Color.WHITE);
        startButton.setFocusPainted(false);
        startButton.setBorderPainted(false);
        startButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        panel.add(startButton);

        stopButton = new JButton("Stop");
        stopButton.setFont(new Font("Dialog", Font.BOLD, 13));
        stopButton.setBackground(new Color(0xEF, 0x44, 0x44));
        stopButton.setForeground(Color.WHITE);
        stopButton.setFocusPainted(false);
        stopButton.setBorderPainted(false);
        stopButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        stopButton.setEnabled(false);
        panel.add(stopButton);

        return panel;
    }

    private JPanel createQueuePanel() {
        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.setBackground(Color.WHITE);
        container.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5, 0xE7, 0xEB), 1),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLabel = new JLabel("Waiting Queue");
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 16));
        container.add(titleLabel, BorderLayout.NORTH);

        queueVisualizationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        queueVisualizationPanel.setBackground(Color.WHITE);

        JPanel queueContainer = new JPanel(new BorderLayout());
        queueContainer.setBackground(Color.WHITE);
        queueContainer.add(queueVisualizationPanel, BorderLayout.CENTER);

        queueCountLabel = new JLabel("Current: 0/5");
        queueCountLabel.setFont(new Font("Dialog", Font.ITALIC, 13));
        queueCountLabel.setForeground(new Color(0x6B, 0x72, 0x80));
        queueContainer.add(queueCountLabel, BorderLayout.SOUTH);

        container.add(queueContainer, BorderLayout.CENTER);
        return container;
    }

    private JPanel createPumpPanel() {
        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.setBackground(Color.WHITE);
        container.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5, 0xE7, 0xEB), 1),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLabel = new JLabel("Service Bays (Pumps)");
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 16));
        container.add(titleLabel, BorderLayout.NORTH);

        pumpStatusContainer = new JPanel();
        pumpStatusContainer.setBackground(Color.WHITE);
        container.add(pumpStatusContainer, BorderLayout.CENTER);

        return container;
    }

    private JPanel createLogPanel() {
        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.setBackground(Color.WHITE);
        container.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5, 0xE7, 0xEB), 1),
                new EmptyBorder(15, 15, 15, 15)
        ));
        container.setPreferredSize(new Dimension(0, 250));

        JLabel titleLabel = new JLabel("Event Timeline");
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 16));
        container.add(titleLabel, BorderLayout.NORTH);

        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setFont(new Font("Dialog", Font.PLAIN, 13));
        logPane.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(logPane);
        scrollPane.setBorder(null);
        container.add(scrollPane, BorderLayout.CENTER);

        return container;
    }

    private void appendLog(String message, Color barColor) {
        if (!simulationRunning && !barColor.equals(EVENT_SYSTEM)) return;

        SwingUtilities.invokeLater(() -> {
            try {
                StyledDocument doc = logPane.getStyledDocument();

                Style barStyle = logPane.addStyle("BarStyle", null);
                StyleConstants.setForeground(barStyle, barColor);
                StyleConstants.setBold(barStyle, true);

                Style textStyle = logPane.addStyle("TextStyle", null);
                StyleConstants.setForeground(textStyle, new Color(0x1F, 0x29, 0x37));

                doc.insertString(doc.getLength(), "|", barStyle);
                doc.insertString(doc.getLength(), " " + message + "\n", textStyle);

                logPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    private void updateQueueVisualization() {
        SwingUtilities.invokeLater(() -> {
            queueVisualizationPanel.removeAll();
            queueVisualizationPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));

            int index = waitingArea.getOut();
            int count = waitingArea.getCount();
            int[] store = waitingArea.getStore();

            for (int i = 0; i < count; i++) {
                int carId = store[index];
                index = (index + 1) % waitingAreaCapacity;

                JLabel carLabel = new JLabel("C" + carId);
                carLabel.setFont(new Font("Dialog", Font.BOLD, 20));
                carLabel.setForeground(Color.WHITE);
                carLabel.setOpaque(true);
                carLabel.setBackground(EVENT_WAITING);
                carLabel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.DARK_GRAY, 1),
                        new EmptyBorder(5, 10, 5, 10)
                ));
                carLabel.setToolTipText("Car #" + carId);
                queueVisualizationPanel.add(carLabel);
            }
            queueCountLabel.setText("Current: " + count + "/" + waitingAreaCapacity);


            queueVisualizationPanel.revalidate();
            queueVisualizationPanel.repaint();
        });
    }

    private class StartButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (simulationRunning) return;

            try {
                waitingAreaCapacity = Integer.parseInt(queueCapacityField.getText());
                numPumps = Integer.parseInt(numPumpsField.getText());
                totalCars = Integer.parseInt(numCarsField.getText());

                if (waitingAreaCapacity < 1 || waitingAreaCapacity > 10) {
                    JOptionPane.showMessageDialog(ServiceStation.this,
                            "Queue capacity must be between 1 and 10.",
                            "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (numPumps < 1) {
                    JOptionPane.showMessageDialog(ServiceStation.this,
                            "Must have at least 1 pump.",
                            "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (totalCars < 1) {
                    JOptionPane.showMessageDialog(ServiceStation.this,
                            "Must simulate at least 1 car.",
                            "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                logPane.setText("");
                carsServedCount = 0;
                simulationRunning = true;

                appendLog("--- Initializing Service Station ---", EVENT_SYSTEM);
                appendLog("Waiting area capacity: " + waitingAreaCapacity, EVENT_SYSTEM);
                appendLog("Number of pumps: " + numPumps, EVENT_SYSTEM);
                appendLog("Number of cars: " + totalCars, EVENT_SYSTEM);

                waitingArea = new WaitingArea(waitingAreaCapacity); 
                pumps = new Semaphore(numPumps); 


                startButton.setEnabled(false);
                stopButton.setEnabled(true);

                pumpStatusContainer.removeAll();
                pumpStatusContainer.setLayout(new GridLayout(1, numPumps, 15, 10));
                pumpPanels.clear();

                for (int i = 1; i <= numPumps; i++) {
                    PumpPanel panel = new PumpPanel("Pump " + i);
                    pumpPanels.add(panel);
                    pumpStatusContainer.add(panel);
                }
                pumpStatusContainer.revalidate();
                pumpStatusContainer.repaint();

                updateQueueVisualization();

                // Start pump threads
                pumpThreads = new Thread[numPumps];
                for (int i = 0; i < numPumps; i++) {
                    Pump pump = new Pump(i + 1, pumpPanels.get(i));
                    pumpThreads[i] = pump;
                    pump.setDaemon(true);
                    pump.start();
                }
                appendLog("Started " + numPumps + " pump thread(s).", EVENT_SYSTEM);

                appendLog("\n--- Spawning arriving cars ---", EVENT_SYSTEM);

                // Start car threads
                carThreads = new Thread[totalCars];
                new Thread(() -> {
                    for (int i = 0; i < totalCars; i++) {
                        if (!simulationRunning) break;
                        Car car = new Car(i + 1);
                        carThreads[i] = car;
                        car.start();
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ignored) {
                        }
                    }

                    // Monitor for completion
                    for (Thread t : carThreads) {
                        try {
                            if (t != null) t.join();
                        } catch (InterruptedException ignored) {
                        }
                    }

                    try {
                        // Check if simulation is still running and cars are in queue or being serviced
                        while (simulationRunning && (waitingArea.full.getValue() > 0 || pumps.getValue() < numPumps)) {
                            Thread.sleep(100);
                        }
                    } catch (InterruptedException ignored) {
                    }

                    if (simulationRunning) {
                        appendLog("\nAll cars processed; simulation ends", EVENT_SYSTEM);
                        SwingUtilities.invokeLater(() -> {
                            stopSimulation();
                        });
                    }
                }).start();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(ServiceStation.this,
                        "Please enter valid numbers for all fields.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class StopButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            stopSimulation();
        }
    }

    private void stopSimulation() {
        if (!simulationRunning) return;

        simulationRunning = false;
        appendLog("SIMULATION STOPPING...", EVENT_SYSTEM);

        if (pumpThreads != null) {
            for (Thread t : pumpThreads) {
                if (t != null) t.interrupt();
            }
        }
        if (carThreads != null) {
            for (Thread t : carThreads) {
                if (t != null) t.interrupt();
            }
        }

        for (PumpPanel panel : pumpPanels) {
            panel.setStatus("Offline", COLOR_OFFLINE);
        }

        startButton.setEnabled(true);
        stopButton.setEnabled(false);
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


    class WaitingArea {
        int size;
        private int[] store;
        private int in = 0;
        private int out = 0;
        private int count = 0; 

        Semaphore empty;
        Semaphore full;
        Semaphore mutex;

        WaitingArea(int size) {
            this.size = size;
            this.store = new int[size];
            this.empty = new Semaphore(size);
            this.full = new Semaphore(0);
            this.mutex = new Semaphore(1);
        }

        public void produce(int v) throws InterruptedException {
            empty.acquire();
            mutex.acquire();

            store[in] = v;
            in = (in + 1) % size;
            count++;

            appendLog("Car #" + v + " entered the queue.", EVENT_WAITING);
            updateQueueVisualization();

            mutex.release();
            full.release();
        }


        public int consume(int pumpId) throws InterruptedException {
            int v;
            mutex.acquire();
            v = store[out];
            out = (out + 1) % size;
            count--;

            appendLog("Pump " + pumpId + ": Car " + v + " Occupied", EVENT_SERVICE);
            appendLog("Pump " + pumpId + ": begins service at Bay " + pumpId, EVENT_SERVICE);
            updateQueueVisualization();

            mutex.release();
            return v;
        }

        public int getOut() {
            return out;
        }

        public int getCount() {
            return count;
        }

        public int[] getStore() {
            return store;
        }
    }

    class Car extends Thread {
        private final int id;

        public Car(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            if (!simulationRunning) return;

            appendLog("Car #" + id + " arrived.", EVENT_ARRIVED);
            try {
                waitingArea.produce(id);
            } catch (InterruptedException ignored) {
            }
        }
    }

    class Pump extends Thread {
        private final int id;
        private final PumpPanel panel;

        public Pump(int id, PumpPanel panel) {
            this.id = id;
            this.panel = panel;
        }

        @Override
        public void run() {
            while (simulationRunning) {
                try {
                    panel.setStatus("Idle (Waiting for Car)", COLOR_IDLE);

                    waitingArea.full.acquire();
                    pumps.acquire();
                    int carId = waitingArea.consume(this.id);

                    waitingArea.empty.release();

                    panel.setStatus("Serving C" + carId, COLOR_BUSY);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                        pumps.release();
                        Thread.currentThread().interrupt();
                        break;
                    }

                    pumps.release();

                    appendLog("Pump " + id + ": finishes service for Car " + carId, EVENT_COMPLETE);
                    appendLog("Pump " + id + ": ends service at Bay " + id, EVENT_COMPLETE);

                    synchronized (this) {
                        carsServedCount++;
                    }

                } catch (InterruptedException e) {
                    panel.setStatus("Offline", COLOR_OFFLINE);
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    //PumpPanel Class
    class PumpPanel extends JPanel {
        private final JLabel nameLabel;
        private final JLabel statusLabel;

        public PumpPanel(String pumpName) {
            setLayout(new BorderLayout(5, 5));
            setBackground(COLOR_OFFLINE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xD1, 0xD5, 0xDB), 2),
                    new EmptyBorder(15, 15, 15, 15)
            ));

            nameLabel = new JLabel(pumpName, SwingConstants.CENTER);
            nameLabel.setFont(new Font("Dialog", Font.BOLD, 14));
            nameLabel.setForeground(new Color(0x1F, 0x29, 0x37));
            add(nameLabel, BorderLayout.NORTH);

            statusLabel = new JLabel("Initializing...", SwingConstants.CENTER);
            statusLabel.setFont(new Font("Dialog", Font.PLAIN, 13));
            statusLabel.setForeground(Color.WHITE);
            add(statusLabel, BorderLayout.CENTER);
        }

        public void setStatus(String text, Color color) {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText(text);
                setBackground(color);

                if (color.equals(COLOR_OFFLINE)) {
                    statusLabel.setForeground(new Color(0x6B, 0x72, 0x80));
                } else {
                    statusLabel.setForeground(Color.WHITE);
                }
            });
        }
    }
}
