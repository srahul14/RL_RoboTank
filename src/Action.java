package ece.cpen502;

public enum Action {

    FORWARD, BACK, LEFT, RIGHT, FIRE;

    static public double[] bipolarOneHotVectorOf(Action action){
        double [] hotVector = new double[]{-1,-1,-1,-1,-1};
        hotVector[action.ordinal()]=+1;
        return hotVector;
    }
}
