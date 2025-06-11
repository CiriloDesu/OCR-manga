package cirilo.atc.model;

public class ApiSuccessResponse {
    private boolean success;
    private ApiProcessedPageData data;
    private ApiDebugInfo debug_info;
    private long processingTime;

    public ApiSuccessResponse(boolean success, ApiProcessedPageData data, ApiDebugInfo debug_info, long processingTime) {
        this.success = success;
        this.data = data;
        this.debug_info = debug_info;
        this.processingTime = processingTime;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public ApiProcessedPageData getData() { return data; }
    public void setData(ApiProcessedPageData data) { this.data = data; }
    public ApiDebugInfo getDebug_info() { return debug_info; }
    public void setDebug_info(ApiDebugInfo debug_info) { this.debug_info = debug_info; }
    public long getProcessingTime() { return processingTime; }
    public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }
}