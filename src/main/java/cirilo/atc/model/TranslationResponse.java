package cirilo.atc.model;

import java.util.List;

public class TranslationResponse {

    private List<Balloon> balloons;

    public TranslationResponse(List<Balloon> balloons){
        this.balloons = balloons;
    }

    public List<Balloon> getBalloons() {
        return balloons;
    }

    public void setBalloons(List<Balloon> balloons) {
        this.balloons = balloons;
    }
}
