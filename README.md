# Twitter Scraper

A Java-based Twitter/X scraper built with Selenium and Firefox. It loads authenticated cookies, navigates to a specified X URL, automatically scrolls through the page, and collects tweets along with their engagement statistics.

### Features

* 🦊 Firefox + Selenium WebDriver
* 🍪 Load authentication cookies from `.cookies` files
* 🔗 Scrape any supplied X/Twitter URL
* 📜 Automatically scroll and collect tweets
* 📊 Extract replies, reposts, likes, bookmarks, and views
* 💾 Export results to `<username>_tweets.csv`
* 📦 Buildable as an executable JAR with Maven

### Requirements

* Java 21+
* Maven
* A valid X/Twitter account cookie file in Netscape format with a `.cookies` file extension.

### Build

```bash
mvn clean package
```

### Run

```bash
java -jar target/TwitterScraper-1.0.0.jar
```

The scraper prompts for a `.cookies` file and can then process the X/Twitter URL provided by the user.

### CSV Output

The generated `<username>_tweets.csv` contains:

```text
id,date,url,text,replies,reposts,likes,bookmarks,views
```
