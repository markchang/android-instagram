package org.acmelab.andgram;

/**
 * Created by IntelliJ IDEA.
 * User: mchang
 * Date: 3/25/11
 * Time: 11:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class InstagramImage {
    private String url;
    private String username;
    private String comments;
    private String caption;


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }
}
