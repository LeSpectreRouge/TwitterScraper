package com.lespectrerouge;

import com.lespectrerouge.utils.Scraper;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

public class TwitterScraper {

    private static final TwitterScraper INSTANCE = new TwitterScraper();
    private final Pattern usernamePattern = Pattern.compile("^[A-Za-z0-9_]{4,15}$");

    public static void main(String... args) { getInstance().run(args); }

    private void run(String... args) {
        if (args.length != 1) {
            System.err.println(getUsage());
            return;
        }
        final String username = args[0];
        if (!usernamePattern.matcher(username).matches()) {
            System.err.printf("Invalid username %s (must match %s)%n", username, usernamePattern.pattern());
            return;
        }
        final File cookiesFile = openCookiesDialog();
        if (cookiesFile == null || !cookiesFile.isFile()) {
            JOptionPane.showMessageDialog(null, "Invalid cookies file", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        final Scraper scraper = new Scraper(username);
        try {
            scraper.loadCookies(cookiesFile);
            scraper.start();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            scraper.quit();
        }
    }

    public final File openCookiesDialog() {
        final JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.home")));
        chooser.setFileFilter(new FileNameExtensionFilter("Cookie files (*.cookies)", "cookies"));
        return chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
    }

    public String getUsage() { return String.format("java -jar %s <username>", getJarFile().getName()); }

    public static String getJarName() { return getJarFile().getName(); }

    public static File getJarFile() { return new File(getJarPath()); }

    public static String getJarPath() {
        try {
            return new File(TwitterScraper.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static TwitterScraper getInstance() { return INSTANCE; }

}