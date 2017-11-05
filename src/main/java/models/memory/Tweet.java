package models.memory;

import java.util.ArrayList;

public class Tweet {

    private int tweetId;
    private long pageId;
    private Human author;
    private int parentId;
    private String content;
    private long time;
    private long nodeId;
    private long likesCount;
    private boolean likedByMe;
    private ArrayList<Tweet> topComments;

    public long getPageId() {
        return pageId;
    }

    public void setPageId(long pageId) {
        this.pageId = pageId;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public int getTweetId() {
        return tweetId;
    }

    public void setTweetId(int tweetId) {
        this.tweetId = tweetId;
    }

    public Human getAuthor() {
        return author;
    }

    public void setAuthor(Human author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getNodeId() {
        return nodeId;
    }

    public void setNodeId(long nodeId) {
        this.nodeId = nodeId;
    }

    public long getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(long likesCount) {
        this.likesCount = likesCount;
    }

    public boolean isLikedByMe() {
        return likedByMe;
    }

    public void setLikedByMe(boolean likedByMe) {
        this.likedByMe = likedByMe;
    }


    public ArrayList<Tweet> getTopComments() {
        return topComments;
    }

    public void setTopComments(ArrayList<Tweet> topComments) {
        this.topComments = topComments;
    }
}