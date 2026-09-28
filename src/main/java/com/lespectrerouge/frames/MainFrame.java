package com.lespectrerouge.frames;

import com.lespectrerouge.utils.Scraper;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Main application window for configuring and starting the Twitter scraper.
 *
 * <p>The frame allows users to select a cookies file, enter a username, start
 * the scraper, and view application logs.</p>
 */
public class MainFrame extends JFrame {

    private static final int MAX_LOG_LINES = 100;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{4,15}$");

    private File cookiesFile;

    private JTextField usernameField;
    private JButton startButton;

    private DefaultListModel<String> logsModel;
    private JList<String> logsList;

    /**
     * Creates and initializes the main application window.
     */
    public MainFrame() {
        super("Twitter Scraper");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setJMenuBar(createMenuBar());
        setContentPane(createMainPanel());
        redirectSystemStreams();
    }

    /**
     * Creates the application menu bar.
     *
     * @return the configured menu bar
     */
    private JMenuBar createMenuBar() {
        final JMenuBar menuBar = new JMenuBar();
        final JMenu fileMenu = new JMenu("File");
        final JMenuItem loadCookies = new JMenuItem("Load Cookies");

        loadCookies.addActionListener(e -> {
            final File file = openCookiesDialog();
            if (file != null) {
                cookiesFile = file;
                addLog("Cookies loaded: " + file.getName());
            }
        });

        fileMenu.add(loadCookies);
        menuBar.add(fileMenu);

        return menuBar;
    }

    /**
     * Creates the main content panel containing the username input controls
     * and the log display.
     *
     * @return the configured main panel
     */
    private JPanel createMainPanel() {
        final JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        final JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        final JLabel usernameLabel = new JLabel("Username:");

        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        usernameField = new JTextField();
        startButton = new JButton("Start");
        startButton.addActionListener(e -> startScraper());

        inputPanel.add(usernameLabel, BorderLayout.WEST);
        inputPanel.add(usernameField, BorderLayout.CENTER);
        inputPanel.add(startButton, BorderLayout.EAST);

        logsModel = new DefaultListModel<>();
        logsList = new JList<>(logsModel);
        logsList.setFocusable(false);

        final JScrollPane logsScrollPane = new JScrollPane(logsList);
        logsScrollPane.setBorder(BorderFactory.createTitledBorder("Logs"));

        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(logsScrollPane, BorderLayout.CENTER);

        return mainPanel;
    }

    /**
     * Validates the input values and starts the scraper in a background thread.
     */
    private void startScraper() {
        final String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            addLog("Please enter a username.");
            return;
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            addLog("Invalid username: " + username);
            return;
        }
        if (cookiesFile == null) {
            addLog("No cookies file loaded.");
            return;
        }
        final File selectedCookies = cookiesFile;
        addLog(String.format("Starting scraper for @%s...", username));
        startButton.setEnabled(false);
        new Thread(() -> {
            try {
                final Scraper scraper = new Scraper(username);
                scraper.loadCookies(selectedCookies);
                scraper.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                SwingUtilities.invokeLater(() -> startButton.setEnabled(true));
            }
        }, "scraper-thread").start();
    }

    /**
     * Adds a message to the log list and keeps the most recent messages
     * visible.
     *
     * <p>If called outside the Swing event-dispatch thread, the update is
     * scheduled on that thread.</p>
     *
     * @param message the message to add to the log
     */
    public void addLog(final String message) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> addLog(message));
            return;
        }
        logsModel.addElement(message);
        while (logsModel.size() > MAX_LOG_LINES) logsModel.remove(0);
        if (!logsModel.isEmpty()) {
            final int lastIndex = logsModel.size() - 1;
            logsList.ensureIndexIsVisible(lastIndex);
            logsList.setSelectedIndex(lastIndex);
            logsList.clearSelection();
        }
    }

    /**
     * Opens a file chooser for selecting a cookies file.
     *
     * @return the selected cookies file, or {@code null} if no file was chosen
     */
    public final File openCookiesDialog() {
        final JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.home")));
        chooser.setFileFilter(new FileNameExtensionFilter("Cookie files (*.cookies)", "cookies"));
        return chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile(): null;
    }

    /**
     * Redirects standard output and standard error streams to the
     * application's log list.
     */
    private void redirectSystemStreams() {
        final PrintStream stdout = new PrintStream(
            new OutputStream() {
                private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                @Override
                public void write(int b) {
                    if (b == '\n') flushBuffer(false); else if (b != '\r') buffer.write(b);
                }

                private void flushBuffer(boolean error) {
                    if (buffer.size() > 0) {
                        final String message = buffer.toString(StandardCharsets.UTF_8);
                        addLog(error ? "[ERROR] " + message : message);
                        buffer.reset();
                    }
                }
            },
            true,
            StandardCharsets.UTF_8
        );

        final PrintStream stderr = new PrintStream(
            new OutputStream() {
                private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                @Override
                public void write(int b) {
                    if (b == '\n') {
                        if (buffer.size() > 0) {
                            final String message = buffer.toString(StandardCharsets.UTF_8);
                            addLog("[ERROR] " + message);
                            buffer.reset();
                        }
                    } else if (b != '\r') buffer.write(b);
                }
            },
            true,
            StandardCharsets.UTF_8
        );

        System.setOut(stdout);
        System.setErr(stderr);
    }

}