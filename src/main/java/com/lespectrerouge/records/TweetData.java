package com.lespectrerouge.records;

/**
 * Represents the data associated with a tweet.
 *
 * @param id        the unique identifier of the tweet
 * @param date      the date the tweet was published
 * @param url       the URL of the tweet
 * @param text      the text content of the tweet
 * @param replies   the number of replies to the tweet
 * @param reposts   the number of times the tweet was reposted
 * @param likes     the number of likes received by the tweet
 * @param bookmarks the number of times the tweet was bookmarked
 * @param views     the number of views of the tweet
 */
public record TweetData(
    String id,
    String date,
    String url,
    String text,
    String replies,
    String reposts,
    String likes,
    String bookmarks,
    String views
) {}