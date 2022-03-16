public class Music {
    private String name;
    private int duration;

    public Music(String name, int duration) {
        this.name = name;
        this.duration = duration;
    }

    public String getName() {
        return this.name;
    }

    public int getDuration() {
        return this.duration;
    }
}