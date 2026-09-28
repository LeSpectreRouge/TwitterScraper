package com.lespectrerouge;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.DarculaTheme;
import com.github.weisj.darklaf.theme.IntelliJTheme;
import com.lespectrerouge.frames.MainFrame;
import com.lespectrerouge.utils.Scraper;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

public class TwitterScraper {

    private static final TwitterScraper INSTANCE = new TwitterScraper();
    private final MainFrame mainFrame = new MainFrame();

    public static void main(String... args) { getInstance().run(args); }

    private void run(String... args) {
        LafManager.install(new DarculaTheme());
        SwingUtilities.invokeLater(() -> mainFrame.setVisible(true));
    }

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