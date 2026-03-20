package com.loginbruteforce;

import com.loginbruteforce.db.DatabaseHelper;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * Universal Bootstrapper for the Unified Security Console.
 * Validates dependencies, downloads them if necessary, initializes databases,
 * and handles all fatal errors gracefully without raw console stack traces.
 */
public class Launcher {

    public static final String APP_DATA_DIR = System.getProperty("user.home") + File.separator + ".unifiedconsole";
    private static final String FIRST_RUN_FLAG = APP_DATA_DIR + File.separator + ".app_initialized";
    private static final String LOG_FILE = APP_DATA_DIR + File.separator + "app_crash.log";
    private static final String LIBS_DIR = APP_DATA_DIR + File.separator + "libs";

    // Required dependencies and their Maven Central URLs
    private static final Dependency[] REQUIRED_LIBS = {
        new Dependency("org.sqlite.JDBC", "sqlite-jdbc.jar", 
                "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.1.0/sqlite-jdbc-3.45.1.0.jar"),
        new Dependency("com.formdev.flatlaf.FlatDarkLaf", "flatlaf.jar", 
                "https://repo1.maven.org/maven2/com/formdev/flatlaf/3.4.1/flatlaf-3.4.1.jar"),
        new Dependency("org.mindrot.jbcrypt.BCrypt", "jbcrypt.jar", 
                "https://repo1.maven.org/maven2/org/mindrot/jbcrypt/0.4/jbcrypt-0.4.jar"),
        new Dependency("org.apache.commons.codec.binary.Base32", "commons-codec.jar", 
                "https://repo1.maven.org/maven2/commons-codec/commons-codec/1.17.0/commons-codec-1.17.0.jar"),
        new Dependency("com.google.zxing.qrcode.QRCodeWriter", "zxing-core.jar", 
                "https://repo1.maven.org/maven2/com/google/zxing/core/3.5.3/core-3.5.3.jar")
    };

    public static void main(String[] args) {
        File dataDir = new File(APP_DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        setupGlobalExceptionHandler();

        // Check if we are in a process that already updated the classpath
        if (checkDependencies(false)) {
            runApp();
        } else {
            // Missing dependencies detected. Show UI and attempt download.
            System.out.println("Missing dependencies detected. Initializing downloader...");
            SwingUtilities.invokeLater(() -> showDownloaderUIAndRestart());
        }
    }

    private static void runApp() {
        handleFirstRun();
        
        // Launch standard app
        MainController.main(new String[0]);
    }

    private static void handleFirstRun() {
        File flagFile = new File(FIRST_RUN_FLAG);
        if (!flagFile.exists()) {
            System.out.println("First run detected. Running setup routine...");
            try {
                // Initialize local database schemas natively
                DatabaseHelper.initializeDatabase();
                
                // Confirm it's working by grabbing a connection briefly
                try (java.sql.Connection c = DatabaseHelper.getConnection()) {
                    if (c == null || c.isClosed()) throw new RuntimeException("Database connection test failed.");
                }

                // Create the flag so this doesn't run again
                flagFile.createNewFile();
                System.out.println("Setup routine complete.");
            } catch (Exception e) {
                showFatalErrorDialog("First Run Setup Failed", 
                    "Could not initialize the local database.", e);
                System.exit(1);
            }
        }
    }

    // --- Dependency Management ---

    static class Dependency {
        String className;
        String jarName;
        String downloadUrl;
        Dependency(String className, String jarName, String downloadUrl) {
            this.className = className;
            this.jarName = jarName;
            this.downloadUrl = downloadUrl;
        }
    }

    private static boolean checkDependencies(boolean checkSilently) {
        for (Dependency dep : REQUIRED_LIBS) {
            try {
                Class.forName(dep.className, false, Launcher.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
        return true;
    }

    private static void showDownloaderUIAndRestart() {
        JFrame frame = new JFrame("Dependency Installer");
        frame.setSize(400, 150);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel label = new JLabel("Missing required libraries. Downloading...");
        JProgressBar progressBar = new JProgressBar();
        progressBar.setStringPainted(true);

        panel.add(label, BorderLayout.NORTH);
        panel.add(progressBar, BorderLayout.CENTER);
        frame.add(panel);
        frame.setVisible(true);

        new Thread(() -> {
            File libsDir = new File(LIBS_DIR);
            if (!libsDir.exists()) libsDir.mkdirs();

            List<Dependency> missing = new ArrayList<>();
            for (Dependency dep : REQUIRED_LIBS) {
                try {
                    Class.forName(dep.className, false, Launcher.class.getClassLoader());
                } catch (ClassNotFoundException e) {
                    missing.add(dep);
                }
            }

            for (int i = 0; i < missing.size(); i++) {
                Dependency dep = missing.get(i);
                final int current = i + 1;
                final int total = missing.size();
                
                SwingUtilities.invokeLater(() -> {
                    label.setText("Downloading " + dep.jarName + " (" + current + "/" + total + ")");
                    progressBar.setValue((int) (((double) (current - 1) / total) * 100));
                });

                try {
                    downloadFile(dep.downloadUrl, new File(libsDir, dep.jarName));
                } catch (IOException ex) {
                    SwingUtilities.invokeLater(() -> {
                        showFatalErrorDialog("Download Failed",
                            "Failed to automatically download dependency: " + dep.jarName + "\n" +
                            "Please ensure you are connected to the internet.", ex);
                        System.exit(1);
                    });
                    return;
                }
            }

            SwingUtilities.invokeLater(() -> {
                progressBar.setValue(100);
                label.setText("Download complete. Restarting...");
            });

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}

            restartAppWithLibs();
            System.exit(0);
        }).start();
    }

    private static void downloadFile(String fileUrl, File destFile) throws IOException {
        URL url = URI.create(fileUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        
        try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
             FileOutputStream fileOutputStream = new FileOutputStream(destFile)) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        }
    }

    private static void restartAppWithLibs() {
        // Spawn a new JVM process including the downloaded JARs in the classpath
        try {
            String javaPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            String classpath = System.getProperty("java.class.path");
            
            // Build the new classpath including the libs folder
            String separator = System.getProperty("path.separator");
            File libsDir = new File(LIBS_DIR);
            if (libsDir.exists() && libsDir.isDirectory()) {
                for (File file : libsDir.listFiles()) {
                    if (file.getName().endsWith(".jar")) {
                        classpath += separator + file.getAbsolutePath();
                    }
                }
            }

            ProcessBuilder pb = new ProcessBuilder(javaPath, "-cp", classpath, "com.loginbruteforce.Launcher");
            pb.inheritIO();
            pb.start();
        } catch (IOException ex) {
            showFatalErrorDialog("Restart Failed", "Could not restart the application.", ex);
        }
    }

    // --- Error Handling ---

    private static void setupGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            logError(throwable);
            showFatalErrorDialog("Unexpected Application Error", 
                "A fatal error occurred and the application needs to close.",
                throwable);
            System.exit(1);
        });
    }

    private static void logError(Throwable throwable) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            writer.println("=== CRASH LOG ===");
            writer.println("Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            throwable.printStackTrace(writer);
            writer.println("=================\n");
        } catch (IOException e) {
            System.err.println("Failed to write to crash log.");
            e.printStackTrace();
        }
    }

    private static void showFatalErrorDialog(String title, String message, Throwable throwable) {
        if (SwingUtilities.isEventDispatchThread()) {
            displayErrorInternal(title, message, throwable);
        } else {
            try {
                SwingUtilities.invokeAndWait(() -> displayErrorInternal(title, message, throwable));
            } catch (Exception ex) {
                ex.printStackTrace(); // Fallback
            }
        }
    }

    private static void displayErrorInternal(String title, String message, Throwable throwable) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        
        JLabel msgLabel = new JLabel("<html><body><p style='width: 300px;'>" +
                message + "<br><br>Details have been saved to <b>" + LOG_FILE + "</b>.</p></body></html>");
        panel.add(msgLabel, BorderLayout.NORTH);

        JTextArea textArea = new JTextArea(10, 40);
        textArea.setText(throwable.toString() + "\nSee log file for full trace.");
        textArea.setEditable(false);
        textArea.setForeground(Color.RED);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(350, 100));
        panel.add(scrollPane, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(null, panel, title, JOptionPane.ERROR_MESSAGE);
    }
}
