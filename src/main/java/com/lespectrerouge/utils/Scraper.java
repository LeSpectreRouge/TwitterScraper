package com.lespectrerouge.utils;

import com.lespectrerouge.records.TweetData;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scrapes tweet data from an X user profile.
 */
public class Scraper {

    private final String username;
    private final WebDriver driver;
    private final File csvFile;
    private final Random random = new Random(true);
    private final AtomicBoolean running = new AtomicBoolean(true);

    private int maxWaitTime = 10_000;
    private String retryButtonText = "Réessayer";

    private static final By TWEET = By.cssSelector("article[data-testid='tweet']");
    private static final By TIME = By.cssSelector("time");
    private static final By TWEET_TEXT = By.cssSelector("[data-testid='tweetText']");
    private static final Pattern STAT_PATTERN = Pattern.compile("([0-9]+(?:[.,][0-9]+)?)(?:\\s*([KMB]))?", Pattern.CASE_INSENSITIVE);

    /**
     * Creates a scraper for the specified username.
     *
     * @param username the username whose tweets should be scraped
     */
    public Scraper(String username) {
        csvFile = new File(String.format("%s_tweets.csv",(this.username = username)));
        this.driver = new FirefoxDriver();
        driver.get("https://x.com/");
    }

    /**
     * Loads cookies from a tab-separated cookie file.
     *
     * @param file the cookie file to load
     * @return this scraper
     * @throws IOException if the cookie file cannot be read
     */
    public final Scraper loadCookies(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] f = line.split("\t", -1);
                if (f.length < 7) continue;
                String domain = (f[0].startsWith(".") ? f[0].substring(1) : f[0]);
                try {
                    driver.manage().addCookie(new Cookie(f[5], f[6], domain, f[2], null, f[3].equalsIgnoreCase("TRUE"), false));
                } catch (Exception e) {
                    System.err.printf("Unable to load cookie '%s' for '%s': %s%n", f[5], domain, e.getMessage());
                }
            }
        }

        return this;
    }

    /**
     * Starts scraping tweets and writes them to the configured CSV file.
     *
     * @throws IOException if the CSV file cannot be written
     * @throws InterruptedException if the scraper is interrupted while waiting
     */
    public final void start() throws IOException, InterruptedException {
        driver.navigate().to(String.format("https://x.com/%s", username));
        final Set<String> tweets = new HashSet<>();
        try (CsvWriter csv = new CsvWriter(getCsvFile(), "id,date,url,text,replies,reposts,likes,bookmarks,views")) {
            int unchanged = 0;
            while (isRunning()) {
                if (unchanged > 1 && !clickRetry()) {
                    driver.navigate().to(String.format("https://x.com/%s/with_replies", username));
                    unchanged = 0;
                }
                int oldSize = tweets.size();
                for (WebElement tweet : driver.findElements(TWEET)) {
                    try {
                        final TweetData data = data(tweet);
                        if (data == null || !tweets.add(data.id())) continue;
                        csv.write(
                            data.id(),
                            data.date(),
                            data.url(),
                            data.text(),
                            data.replies(),
                            data.reposts(),
                            data.likes(),
                            data.bookmarks(),
                            data.views()
                        );
                        System.out.printf(
                            "[%d] %s | replies=%s reposts=%s likes=%s bookmarks=%s views=%s%n",
                            tweets.size(),
                            data.text().replace("\n", " "),
                            data.replies(),
                            data.reposts(),
                            data.likes(),
                            data.bookmarks(),
                            data.views()
                        );

                    } catch (WebDriverException ignored) {}
                }
                unchanged = (tweets.size() == oldSize ? unchanged + 1 : 0);
                scroll();
                waitForTweets(maxWaitTime);
                System.out.printf("Tweets: %d | unchanged: %d%n", tweets.size(), unchanged);
            }
            System.out.printf("Finished: %d tweets saved to %s%n", tweets.size(), getCsvFile().getName());
        }
    }

    /**
     * Extracts tweet data from a tweet element.
     *
     * @param tweet the tweet element
     * @return the extracted tweet data, or {@code null} if required data is missing
     */
    private TweetData data(WebElement tweet) {
        final List<WebElement> times = tweet.findElements(TIME);
        if (times.isEmpty()) return null;
        final WebElement time = times.getFirst();
        final String date = time.getAttribute("datetime");
        final List<WebElement> links = time.findElements(By.xpath("./ancestor::a[contains(@href, '/status/')]"));
        if (links.isEmpty()) return null;
        final String url = links.getFirst().getAttribute("href");
        final String id = id(url);
        if (id.isBlank()) return null;
        final List<WebElement> text = tweet.findElements(TWEET_TEXT);
        return new TweetData(
            id,
            date,
            url,
            text.isEmpty() ? "" : text.getFirst().getText(),
            stat(tweet, "reply"),
            stat(tweet, "retweet"),
            stat(tweet, "like"),
            stat(tweet, "bookmark"),
            stat(tweet, "view")
        );
    }

    /**
     * Extracts the tweet ID from a tweet URL.
     *
     * @param url the tweet URL
     * @return the tweet ID, or an empty string if no ID is found
     */
    private String id(String url) {
        if (url == null) return "";
        int index = url.indexOf("/status/");
        if (index == -1) return "";
        String id = url.substring(index + 8);
        int end = id.indexOf('?');
        if (end == -1) end = id.indexOf('/');
        return end == -1 ? id : id.substring(0, end);
    }

    /**
     * Extracts a statistic from a tweet element.
     *
     * @param tweet the tweet element
     * @param type the statistic type
     * @return the statistic value, or {@code "0"} if it is unavailable
     */
    private String stat(WebElement tweet, String type) {
        for (WebElement element : tweet.findElements(By.cssSelector(String.format("[data-testid*='%s']", type)))) {
            try {
                String label = element.getAttribute("aria-label");
                if (label == null || label.isBlank()) continue;
                Matcher matcher = STAT_PATTERN.matcher(label);
                if (!matcher.find()) continue;
                String number = matcher.group(1);
                String suffix = matcher.group(2);
                return (suffix == null ? number : number + suffix.toUpperCase());
            } catch (StaleElementReferenceException ignored) {}
        }
        return "0";
    }

    /**
     * Clicks the retry button when it is available.
     *
     * @return {@code true} if the button was clicked; otherwise {@code false}
     */
    protected final boolean clickRetry() {
        try {
            for (WebElement button : driver.findElements(By.xpath(String.format("//button[.//span[normalize-space()='%s']]", retryButtonText)))) {
                try {
                    if (!button.isDisplayed() || !button.isEnabled()) continue;
                    try {
                        button.click();
                    } catch (ElementClickInterceptedException e) {
                        ((JavascriptExecutor)driver).executeScript("arguments[0].click();", button);
                    }
                    System.out.println("Retry button clicked.");
                    return true;
                } catch (StaleElementReferenceException ignored) {}
            }
        } catch (WebDriverException ignored) {}
        return false;
    }

    /**
     * Scrolls to the last visible tweet.
     */
    protected final void scroll() {
        final List<WebElement> tweets = driver.findElements(TWEET);
        if (tweets.isEmpty()) return;
        try {
            ((JavascriptExecutor)driver).executeScript("arguments[0].scrollIntoView({block:'end',behavior:'instant'});", tweets.getLast());
        } catch (StaleElementReferenceException ignored) {}
    }

    /**
     * Waits for additional tweets to load.
     *
     * @param maxTime the maximum wait time in milliseconds
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    protected final void waitForTweets(int maxTime) throws InterruptedException { Thread.sleep(random.randomInt(1000, maxTime)); }

    /**
     * Quits the WebDriver.
     *
     * @return this scraper
     */
    public final Scraper quit() {
        driver.quit();
        return this;
    }

    /**
     * Returns whether scraping is currently running.
     *
     * @return {@code true} if scraping is active
     */
    public final boolean isRunning() { return running.get(); }

    /**
     * Sets whether scraping should continue.
     *
     * @param running whether scraping should be active
     * @return this scraper
     */
    public final Scraper setRunning(boolean running) {
        this.running.set(running);
        return this;
    }

    /**
     * Toggles the running state.
     *
     * @return this scraper
     */
    public final Scraper toggle() { return setRunning(!running.get()); }

    /**
     * Returns the WebDriver used by this scraper.
     *
     * @return the WebDriver
     */
    public final WebDriver getDriver() { return driver; }

    /**
     * Returns the CSV output file.
     *
     * @return the CSV file
     */
    public final File getCsvFile() { return csvFile; }

    /**
     * Returns the scraped username.
     *
     * @return the username
     */
    public final String getUsername() { return username; }

    /**
     * Returns the maximum wait time between scrolls.
     *
     * @return the maximum wait time in milliseconds
     */
    public final int getMaxWaitTime() { return maxWaitTime; }

    /**
     * Sets the maximum wait time between scrolls.
     *
     * @param maxWaitTime the maximum wait time in milliseconds
     * @return this scraper
     * @throws IllegalArgumentException if the wait time is less than 1001 milliseconds
     */
    public final Scraper setMaxWaitTime(int maxWaitTime) {
        if (maxWaitTime < 1001)
            throw new IllegalArgumentException("maxWaitTime cannot be lower than 1001");
        this.maxWaitTime = maxWaitTime;
        return this;
    }

    /**
     * Returns the text used to identify the retry button.
     *
     * @return the retry button text
     */
    public final String getRetryButtonText() { return retryButtonText; }

    /**
     * Sets the text used to identify the retry button.
     *
     * @param retryButtonText the retry button text
     * @return this scraper
     */
    public final Scraper setRetryButtonText(String retryButtonText) {
        this.retryButtonText = retryButtonText;
        return this;
    }

}