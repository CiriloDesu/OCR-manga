package cirilo.atc.model;

import java.util.List;

public class ApiProcessedPageData {
    private List<ApiBalloon> balloons;

    public ApiProcessedPageData(List<ApiBalloon> balloons) {
        this.balloons = balloons;
    }

    // Getters and Setters
    public List<ApiBalloon> getBalloons() { return balloons; }
    public void setBalloons(List<ApiBalloon> balloons) { this.balloons = balloons; }
}