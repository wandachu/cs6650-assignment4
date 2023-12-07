package main.model;

public class ReviewInfo {
    private String likes;
    private String dislikes;

    public ReviewInfo(String likes, String dislikes) {
        this.likes = likes;
        this.dislikes = dislikes;
    }

    public String getLikes() {
        return likes;
    }

    public void setLikes(String likes) {
        this.likes = likes;
    }

    public String getDislikes() {
        return dislikes;
    }

    public void setDislikes(String dislikes) {
        this.dislikes = dislikes;
    }

    @Override
    public String toString() {
        return "ReviewInfo{" +
                "likes='" + likes + '\'' +
                ", dislikes='" + dislikes + '\'' +
                '}';
    }
}
