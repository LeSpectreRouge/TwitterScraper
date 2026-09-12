package com.lespectrerouge.utils;

import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Selenium-based scraper for extracting tweets from a specified X (formerly Twitter) user's
 * "with_replies" timeline. It loads cookies for authentication, scrolls through the timeline,
 * and writes the collected tweet data to a CSV file.
 */
public class Scraper {

    private final String username;
    private final WebDriver driver;
    private static final By TWEET = By.cssSelector("article[data-testid='tweet']");
    private static final By TIME = By.cssSelector("time");
    private static final By TWEET_TEXT = By.cssSelector("[data-testid='tweetText']");
    private static final Pattern STAT_PATTERN = Pattern.compile("([0-9]+(?:[.,][0-9]+)?)(?:\\s*([KMB]))?", Pattern.CASE_INSENSITIVE);

    /**
     * Constructs a new Scraper for the given username and initializes a Firefox driver
     * pointing to the X home page.
     *
     * @param username the X username whose tweets will be scraped
     */
    public Scraper(String username) {
        this.username = username;
        this.driver = new FirefoxDriver();
        this.driver.get("https://x.com/");
    }

    /**
     * Loads cookies from a Netscape-style cookie file into the current WebDriver session.
     * Lines that are blank, start with '#', or contain fewer than 7 tab-separated fields
     * are skipped. Individual cookie loading failures are logged but do not abort the process.
     *
     * @param file the cookie file to read
     * @return this Scraper instance for method chaining
     * @throws IOException if the file cannot be read
     */
    public final Scraper loadCookies(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                final String[] fields = line.split("\t", -1);
                if (fields.length < 7) continue;
                final String domain = (fields[0].startsWith(".") ? fields[0].substring(1) : fields[0]);
                final String path = fields[2];
                final boolean secure = fields[3].equalsIgnoreCase("TRUE");
                final String name = fields[5];
                final String value = fields[6];
                final Cookie cookie = new Cookie(name, value, domain, path, null, secure, false );
                try {
                    driver.manage().addCookie(cookie);
                } catch (Exception e) {
                    System.err.printf("Unable to load cookie '%s' for '%s': %s%n", name, domain, e.getMessage());
                }
            }
        }
        return this;
    }

    /**
     * Navigates to the target user's "with_replies" timeline, scrolls through it, and writes
     * each newly discovered tweet to a CSV file named "{@code <username>_tweets.csv}".
     * <p>
     * The scraping loop stops once the number of collected tweets has remained unchanged for
     * 8 consecutive iterations. Each tweet row contains: id, date, url, text, replies, reposts,
     * likes, bookmarks, and views. The CSV file is flushed after every row.
     *
     * @return this Scraper instance for method chaining
     * @throws IOException          if the output CSV file cannot be created or written
     * @throws InterruptedException if the thread is interrupted while sleeping between scrolls
     */
    public final Scraper start() throws IOException, InterruptedException {
        final File file = new File(String.format("%s_tweets.csv", username));
        driver.navigate().to(String.format("https://x.com/%s/with_replies", username));
        final Set<String> tweets = new HashSet<>();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("id,date,url,text,replies,reposts,likes,bookmarks,views");
            writer.newLine();
            int unchanged = 0;
            while (unchanged < 8) {
                final int previousSize = tweets.size();
                final List<WebElement> elements = driver.findElements(TWEET);
                for (int i = 0; i < elements.size(); i++) {
                    try {
                        final List<WebElement> current = driver.findElements(TWEET);
                        if (i >= current.size()) break;
                        final WebElement element = current.get(i);
                        final List<WebElement> times = element.findElements(TIME);
                        if (times.isEmpty()) continue;
                        final WebElement time = times.getFirst();
                        final String date = time.getAttribute("datetime");
                        final List<WebElement> links = time.findElements(By.xpath("./ancestor::a[contains(@href, '/status/')]"));
                        if (links.isEmpty()) continue;
                        final String url = links.getFirst().getAttribute("href");
                        final int statusIndex = url.indexOf("/status/");
                        if (statusIndex == -1) continue;
                        final String id = url.substring(statusIndex + 8);
                        if (!tweets.add(id)) continue;
                        final List<WebElement> text = element.findElements(TWEET_TEXT);
                        final String content = (text.isEmpty() ? "" : text.getFirst().getText());
                        final String replies = stat(element, "reply");
                        final String reposts = stat(element, "retweet");
                        final String likes = stat(element, "like");
                        final String bookmarks = stat(element, "bookmark");
                        final String views = stat(element, "view");
                        writer.write(csv(id));
                        writer.write(",");
                        writer.write(csv(date));
                        writer.write(",");
                        writer.write(csv(url));
                        writer.write(",");
                        writer.write(csv(content));
                        writer.write(",");
                        writer.write(csv(replies));
                        writer.write(",");
                        writer.write(csv(reposts));
                        writer.write(",");
                        writer.write(csv(likes));
                        writer.write(",");
                        writer.write(csv(bookmarks));
                        writer.write(",");
                        writer.write(csv(views));
                        writer.newLine();
                        writer.flush();
                        System.out.printf(
                                "[%d] %s | replies=%s reposts=%s likes=%s bookmarks=%s views=%s%n",
                                tweets.size(),
                                content.replace("\n", " "),
                                replies,
                                reposts,
                                likes,
                                bookmarks,
                                views
                        );
                    } catch (WebDriverException ignored) {}
                }
                if (tweets.size() == previousSize) unchanged++; else unchanged = 0;
                final List<WebElement> current = driver.findElements(TWEET);
                if (!current.isEmpty()) {
                    try {
                        ((JavascriptExecutor)driver).executeScript(
                                "arguments[0].scrollIntoView({ block: 'end', behavior: 'instant'});",
                                current.getLast()
                        );
                    } catch (StaleElementReferenceException ignored) {}
                }
                Thread.sleep(1200);
                System.out.printf("Tweets: %d | unchanged: %d/8%n", tweets.size(), unchanged);
            }
            System.out.printf("Finished: %d tweets saved to %s%n", tweets.size(), file.getName());
        }
        return this;
    }

    /**
     * Extracts a single statistic (e.g. replies, reposts, likes, bookmarks, views) from a tweet
     * element by reading the {@code aria-label} attribute of the matching child element and
     * parsing the leading numeric value (with optional K/M/B suffix).
     *
     * @param tweet the tweet WebElement to inspect
     * @param type  the statistic type keyword used in the {@code data-testid} selector
     *              (e.g. "reply", "retweet", "like", "bookmark", "view")
     * @return the parsed statistic as a string (e.g. "1.2K"), or "0" if none is found
     */
    private String stat(WebElement tweet, String type) {
        final List<WebElement> elements = tweet.findElements(By.cssSelector("[data-testid*='" + type + "']"));
        for (WebElement element : elements) {
            try {
                final String label = element.getAttribute("aria-label");
                if (label == null || label.isBlank()) continue;
                final Matcher matcher = STAT_PATTERN.matcher(label);
                if (!matcher.find()) continue;
                final String number = matcher.group(1);
                final String suffix = matcher.group(2);
                return (suffix == null ? number : number + suffix.toUpperCase());
            } catch (StaleElementReferenceException ignored) {}
        }
        return "0";
    }

    /**
     * Escapes a value for safe inclusion in a CSV field by wrapping it in double quotes and
     * doubling any internal double quotes. Newlines and carriage returns are replaced with
     * spaces. A {@code null} value is written as an empty quoted field.
     *
     * @param value the raw value to escape
     * @return the CSV-escaped representation of the value
     */
    private String csv(String value) {
        if (value == null) return "\"\"";
        return ("\"" + value.replace("\"", "\"\"").replace("\r", "").replace("\n", " ") + "\"");
    }

    /**
     * Quits the underlying WebDriver, closing the browser and ending the session.
     *
     * @return this Scraper instance for method chaining
     */
    public final Scraper quit() {
        driver.quit();
        return this;
    }

    /**
     * Returns the underlying WebDriver instance.
     *
     * @return the WebDriver used by this scraper
     */
    public final WebDriver getDriver() { return driver; }

    /**
     * Returns the username associated with this scraper.
     *
     * @return the target X username
     */
    public final String getUsername() { return username; }

}