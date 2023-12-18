package ece.cpen502;

public class Experience {
    public State previousState;
    public Action action;
    public double reward;
    public State nextState;

    // Constructor
    public Experience(State currentState, Action action, double reward, State nextState) {
        this.previousState = currentState;
        this.action = action;
        this.reward = reward;
        this.nextState = nextState;
    }
}
