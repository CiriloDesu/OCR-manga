package cirilo.atc.model;

public class ApiDebugInfo {
    private ApiDimensions originalDimensions;
    private ApiDimensions processedDimensions;
    private ApiScaleFactors scaleFactors;
    private ApiDimensions originalSize; // Redundant with originalDimensions but matches Node.js output
    private ApiDimensions displaySize;


    public ApiDebugInfo(ApiDimensions originalDimensions, ApiDimensions processedDimensions, ApiScaleFactors scaleFactors, ApiDimensions displaySize) {
        this.originalDimensions = originalDimensions;
        this.processedDimensions = processedDimensions;
        this.scaleFactors = scaleFactors;
        this.originalSize = originalDimensions; // Match Node.js structure
        this.displaySize = displaySize;
    }

    // Getters and Setters
    public ApiDimensions getOriginalDimensions() { return originalDimensions; }
    public void setOriginalDimensions(ApiDimensions originalDimensions) { this.originalDimensions = originalDimensions; }
    public ApiDimensions getProcessedDimensions() { return processedDimensions; }
    public void setProcessedDimensions(ApiDimensions processedDimensions) { this.processedDimensions = processedDimensions; }
    public ApiScaleFactors getScaleFactors() { return scaleFactors; }
    public void setScaleFactors(ApiScaleFactors scaleFactors) { this.scaleFactors = scaleFactors; }
    public ApiDimensions getOriginalSize() { return originalSize; }
    public void setOriginalSize(ApiDimensions originalSize) { this.originalSize = originalSize; }
    public ApiDimensions getDisplaySize() { return displaySize; }
    public void setDisplaySize(ApiDimensions displaySize) { this.displaySize = displaySize; }
}