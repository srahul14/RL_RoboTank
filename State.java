package ece.cpen502;

public class State {
    public double myEnergy;
    public double enemyEnergy;
    public double enemyBearing;
    public double enemyDistance;
    public double distanceToCenter;
    public State(double myE, double eE, double eB, double eD, double dtc){
        this.myEnergy=myE;
        this.enemyEnergy=eE;
        this.enemyBearing=eB;
        this.enemyDistance=eD;
        this.distanceToCenter=dtc;
    }
}
