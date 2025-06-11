package cirilo.atc.model;

public class ApiBalloon {
    private String original_text;
    private String translated_text;
    private ApiBoundingBox bounding_box;
    private String balloon_type;

    public ApiBalloon(String original_text, String translated_text, ApiBoundingBox bounding_box, String balloon_type) {
        this.original_text = original_text;
        this.translated_text = translated_text;
        this.bounding_box = bounding_box;
        this.balloon_type = balloon_type;
    }

    // Getters and Setters
    public String getOriginal_text() { return original_text; }
    public void setOriginal_text(String original_text) { this.original_text = original_text; }
    public String getTranslated_text() { return translated_text; }
    public void setTranslated_text(String translated_text) { this.translated_text = translated_text; }
    public ApiBoundingBox getBounding_box() { return bounding_box; }
    public void setBounding_box(ApiBoundingBox bounding_box) { this.bounding_box = bounding_box; }
    public String getBalloon_type() { return balloon_type; }
    public void setBalloon_type(String balloon_type) { this.balloon_type = balloon_type; }
}