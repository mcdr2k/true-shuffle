package trueshuffle.jobs;

import se.michaelthelin.spotify.model_objects.specification.Image;

public class ShuffleJobPlaylistStatus {
    private String name;
    private Image[] images;

    ShuffleJobPlaylistStatus(String name, Image[] images) {
        this.name = name;
        this.images = images;
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public Image[] getImages() {
        return images;
    }

    public void setImages(Image[] images) {
        this.images = images;
    }
}
