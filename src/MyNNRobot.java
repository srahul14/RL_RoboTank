package ece.cpen502;


import robocode.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.util.Date;
import java.util.Random;

public class MyNNRobot extends AdvancedRobot {

    private String weightsFilename =  getClass().getSimpleName() + "-weights.txt";
    //private String logFilename =  getClass().getSimpleName() + "-" + new Date().toString() + ".log";
    static int numInputs = 10;
    static int numHidden = 50;
    static double learningRate = 0.001;
    static double momentum = 0.9;
    static double argA = -1.0;
    static double argB = +1.0;
    static boolean bipolar = true;

    public enum enumOptionalMode {scan, performanceAction};

    static private NeuralNet q = new NeuralNet(
            numInputs,
            numHidden,
            learningRate,
            momentum,
            argA,
            argB,
            bipolar
    );

    public static int totalNumRounds = 0;
    public static int numRoundsTo100 = 0;
    public static int numWins = 0;
    private State currentState = new State(100,100,120,500,500);
    private Action currentAction = Action.values()[0];

    private State previousState = new State(100,100,90,200,800);
    private Action previousAction = Action.values()[0];

    // Initialization: operationMode
    private enumOptionalMode myOperationMode= enumOptionalMode.scan;

    // set RL
    private double gamma = 0.99;
    private double alpha = 0.01;
    private final double epsilon_initial = 0.9;
    private double epsilon = epsilon_initial;
    private boolean decayEpsilon = true;
    private int targetNumRounds = 40000;

    //previous, current and max Q
    private double currentQ = 0.0;
    private double previousQ = 0.0;
    private double maxQ = 0.0;

    // Rewards
    private final double goodReward = 1.0;
    private final double badReward = -0.25;
    private final double goodTerminalReward = 2.0;
    private final double badTerminalReward = -0.5;

    private double currentReward = 0.0;

    // Initialize states
    double myX = 0.0;
    double myY = 0.0;
    double myEnergy = 0.0;
    double enemyBearing = 0.0;
    double enemyDistance = 0.0;
    double distanceToCenter = 0.0;
    double enemyEnergy = 0.0;

    double totalReward = 0.0;

    // Whether you take immediate rewards
    public static boolean takeImmediate = true;

    // Whether you take off-policy algorithm
    public static boolean offPolicy = true;

    // Logging
    static String logFilename = "robotNN.log";
    static LogFile log = null;

    // get center of board
    int xMid = 0;
    int yMid = 0;

    static int replayMemorySize = 1;
    int MAX_SAMPLE_SIZE = 1;
    static ReplayMemory<Experience> replayMemory = new ReplayMemory<>(replayMemorySize);

    static private double curr_best_win_rate =0.0;

    /**
     * MyFirstRobot's run method
     */
    public void run() {

        if (getRoundNum() == 0) {
            q.initializeWeights();
            //nn.zeroWeights();
        }

        // get coordinate of the board center
        int xMid = (int) getBattleFieldWidth() / 2;
        int yMid = (int) getBattleFieldHeight() / 2;

        if (log == null) {
            System.out.print(logFilename);
            log = new LogFile(getDataFile(logFilename));
            log.stream.printf("gamma,   %2.2f\n", gamma);
            log.stream.printf("alpha,   %2.2f\n", alpha);
            log.stream.printf("epsilon, %2.2f\n", epsilon);
            log.stream.printf("badInstantReward, %2.2f\n", badReward);
            log.stream.printf("badTerminalReward, %2.2f\n", badTerminalReward);
            log.stream.printf("goodInstantReward, %2.2f\n", goodReward);
            log.stream.printf("goodTerminalReward, %2.2f\n\n", goodTerminalReward);
        }

        while (true) {
            if (totalNumRounds > 40000) {epsilon = 0.0;}
            switch (myOperationMode) {
                case scan: {
                    currentReward = 0.0;
                    turnRadarLeft(90);
                    break;
                }
                case performanceAction: {
                    if (Math.random()<= epsilon){
                        currentAction = selectRandomAction();
                    }
                    else{
                        currentAction = selectBestAction (
                                myEnergy,
                                enemyEnergy,
                                enemyBearing,
                                enemyDistance,
                                distanceToCenter
                        );
                    }

                    switch (currentAction) {
                        case FORWARD: {
                            setAhead(100);
                            execute();
                            break;
                        }

                        case BACK: {
                            setBack(100);
                            execute();
                            break;
                        }

                        case LEFT: {
                            setTurnLeft(90);
                            setAhead(100);
                            execute();
                            break;
                        }

                        case RIGHT: {
                            setTurnRight(90);
                            setAhead(100);
                            execute();
                            break;
                        }

                        case FIRE: {
                            double amountToTurn = getHeading() - getGunHeading() + enemyBearing;
                            if(amountToTurn == 360.0 || amountToTurn == -360.0){amountToTurn=0.0;}
                            turnGunRight(amountToTurn);
                            fire(3);
                            break;
                        }
                    }
                    double[] x = new double[]{
                            previousState.myEnergy,
                            previousState.enemyEnergy,
                            previousState.enemyBearing,
                            previousState.enemyDistance,
                            previousState.distanceToCenter,
                            previousAction.ordinal()};

//                    double [] scaled_x = downScaleVector(oneHotVectorFor(x));
//                    q.train(scaled_x, computeQ(currentReward,previousState,currentState  ));
                    replayMemory.add(new Experience(previousState,previousAction,currentReward,currentState));
                    replayExperience(replayMemory);
                    myOperationMode = enumOptionalMode.scan;
                    execute();
                }
            }
        }

    }

    private double[] downScaleVector(double[] x) {
        double [] scaled_vector = x;
        scaled_vector[0] = x[0]/100;
        scaled_vector[1] = x[1]/100;
        scaled_vector[2] = x[2]/180;
        scaled_vector[3] = x[3]/1000;
        scaled_vector[4] = x[4]/1000;

        return scaled_vector;
    }

    private double[] oneHotVectorFor(double[] x) {
        double [] hotVector = Action.bipolarOneHotVectorOf(Action.values()[(int)x[5]]);

        double mE = x[0];
        double eE = x[1];
        double eB = x[2];
        double eD = x[3];
        double dtc = x[4];
        double a0 = hotVector[0];
        double a1 = hotVector[1];
        double a2 = hotVector[2];
        double a3 = hotVector[3];
        double a4 = hotVector[4];

        return new double[]{mE,eE,eB,eD,dtc,a0,a1,a2,a3,a4};
    }

    private void replayExperience(ReplayMemory rm) {
        int memorySize = rm.sizeOf();
        int requestedSampleSize = (memorySize<MAX_SAMPLE_SIZE) ? memorySize:MAX_SAMPLE_SIZE;


        Object [] sample = rm.randomSample(requestedSampleSize);

        for (Object item: sample){
           Experience exp = (Experience) item;

            double[] x = new double[]{
                    previousState.myEnergy,
                    previousState.enemyEnergy,
                    previousState.enemyBearing,
                    previousState.enemyDistance,
                    previousState.distanceToCenter,
                    previousAction.ordinal()};

            double [] scaled_x = downScaleVector(oneHotVectorFor(x));
            q.train(scaled_x, computeQ(exp.reward, exp.previousState, exp.nextState));

        }
    }

    public double computeQ(double r, State previousState, State currentState) {
        Action maxA = selectBestAction(
                currentState.myEnergy,
                currentState.enemyEnergy,
                currentState.enemyBearing,
                currentState.enemyDistance,
                currentState.distanceToCenter);

        double[] prevStateAction = new double[]{
                previousState.myEnergy,
                previousState.enemyEnergy,
                previousState.enemyBearing,
                previousState.enemyDistance,
                previousState.distanceToCenter,
                previousAction.ordinal()};

        double[] currentStateAction = new double[]{
                currentState.myEnergy,
                currentState.enemyEnergy,
                currentState.enemyBearing,
                currentState.enemyDistance,
                currentState.distanceToCenter,
                currentAction.ordinal()};

        double[] maxStateAction = new double[]{
                currentState.myEnergy,
                currentState.enemyEnergy,
                currentState.enemyBearing,
                currentState.enemyDistance,
                currentState.distanceToCenter,
                maxA.ordinal()};

        double prevQ = outputForWrapper(prevStateAction);
        double maxQ = outputForWrapper(maxStateAction);
        double currentQ = outputForWrapper(currentStateAction);

        return offPolicy ? prevQ + alpha * (r + gamma * maxQ - prevQ):
                prevQ + alpha * (r + gamma * currentQ - prevQ);
    }

    private double outputForWrapper(double[] x) {
        double [] scaled_vector = downScaleVector(oneHotVectorFor(x));
        return q.outputFor(scaled_vector);
    }

    public Action selectRandomAction() {
        Random rand = new Random();
        int r = rand.nextInt(Action.values().length);
        return Action.values()[r];
    }

    public Action selectBestAction(double e, double e2, double b, double d, double dc) {
        double bestQ = -Double.MAX_VALUE;
        Action bestAction = null;

        for (int a = 0; a < Action.values().length; a++) {
            double[] x = new double[]{e, e2, b, d, dc, a};
            if (outputForWrapper(x) > bestQ) {
                bestQ = outputForWrapper(x);
                bestAction = Action.values()[a];
            }
        }
        return bestAction;
    }

//    public enumEnergy enumEnergyOf(double energy) {
//        enumEnergy e = null;
//        if (energy < 20) e = enumEnergy.critical;
//        else if (energy >= 20 && energy < 40) e = enumEnergy.veryLow;
//        else if (energy >= 40 && energy < 60) e = enumEnergy.low;
//        else if (energy >= 60 && energy < 80) e = enumEnergy.medium;
//        else if (energy >=80) e = enumEnergy.high;
//        return e;
//    }
//
//    public enumDistance enumDistanceOf(double distance) {
//        enumDistance d = null;
//        if (distance < 50) d = enumDistance.veryClose;
//        else if (distance >= 50 && distance < 250) d = enumDistance.close;
//        else if (distance >= 250 && distance < 500) d = enumDistance.near;
//        else if (distance >= 500 && distance < 750) d = enumDistance.far;
//        else if (distance >= 750) d = enumDistance.veryFar;
//        return d;
//    }
//
//    public enumDistanceToCenter enumDistanceToCenterOf(double distance) {
//        enumDistanceToCenter d = null;
//        if (distance < 300) d = enumDistanceToCenter.close;
//        else if (distance >= 300 && distance < 600) d = enumDistanceToCenter.medium;
//        else if (distance >= 600 ) d = enumDistanceToCenter.far;
//        return d;
//    }
//
//    public enumEnemyBearing enumBearingOf(double b) {
//        enumEnemyBearing e = null;
//        if (b>=0 && b <=90) e = enumEnemyBearing.q1;
//        else if (b > 90 && b <= 180) e = enumEnemyBearing.q2;
//        else if (b > -90 && b <= 0) e = enumEnemyBearing.q3;
//        else if (b >= -180 && b <= -90) e = enumEnemyBearing.q4;
//        return e;
//    }

    public double dtc(double fromX, double fromY, double toX, double toY) {
        double distance = Math.sqrt(Math.pow((fromX - toX), 2) + Math.pow((fromY - toY), 2));
        return distance;
    }

    private void logger_print() throws IOException {
        log.stream.printf("%d - %d, %2.1f\n", totalNumRounds - 100, totalNumRounds, 100.0 * numWins / numRoundsTo100);
        log.stream.flush();
        pocketWeights(numWins * 1.0/numRoundsTo100);
        numRoundsTo100 = 0;
        numWins = 0;
    }

    private void pocketWeights(double winRate) throws IOException {
        if(winRate>=curr_best_win_rate*1.05 & winRate>0.5){
            q.save(getDataFile(weightsFilename));
            curr_best_win_rate = winRate;
        }

    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        myEnergy = getEnergy();
        myX = getX();
        myY = getY();
        enemyEnergy = e.getEnergy();
        enemyDistance=e.getDistance();
        distanceToCenter = dtc(myX, myY, xMid, yMid);
        enemyBearing= e.getBearing();

        // Update states
        previousState.myEnergy = currentState.myEnergy;
        previousState.enemyEnergy = currentState.enemyEnergy;
        previousState.enemyBearing = currentState.enemyBearing;
        previousState.enemyDistance = currentState.enemyDistance;
        previousState.distanceToCenter = currentState.distanceToCenter;
        previousAction = currentAction;

        currentState.myEnergy = getEnergy();
        currentState.enemyEnergy = e.getEnergy();
        currentState.enemyBearing = e.getBearing();
        currentState.enemyDistance = e.getDistance();
        currentState.distanceToCenter = dtc(getX(), getY(), xMid, yMid);
        myOperationMode = enumOptionalMode.performanceAction;
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        if (takeImmediate){
            currentReward = badReward;
            totalReward += currentReward;
        }
    }

    @Override
    public void onBulletHit(BulletHitEvent e) {
        if (takeImmediate) {
            currentReward = goodReward;
            totalReward += currentReward;
        }
    }

    @Override
    public void onHitWall(HitWallEvent event) {
        if (takeImmediate){
            currentReward = badReward;
            totalReward += currentReward;
        }
    }

    @Override
    public void onBulletMissed(BulletMissedEvent event) {
        if (takeImmediate){
            currentReward = badReward;
            totalReward += currentReward;
        }
    }

    @Override
    public void onDeath(DeathEvent e) {
        currentReward = badTerminalReward;
        totalReward += currentReward;

        double[] x = new double[]{
                previousState.myEnergy,
                previousState.enemyEnergy,
                previousState.enemyBearing,
                previousState.enemyDistance,
                previousState.distanceToCenter,
                previousAction.ordinal()};

//        double [] scaled_vctr = downScaleVector(oneHotVectorFor(x));
        replayMemory.add(new Experience(previousState,previousAction,currentReward,currentState));
        replayExperience(replayMemory);

        if (numRoundsTo100 < 100) {
            numRoundsTo100++;
            totalNumRounds++;
        } else {
            try {
                logger_print();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        totalReward = 0;
        execute();
    }

    @Override
    public void onWin(WinEvent e) {
        currentReward = goodTerminalReward;
        totalReward += currentReward;

        double[] x = new double[]{
                previousState.myEnergy,
                previousState.enemyEnergy,
                previousState.enemyBearing,
                previousState.enemyDistance,
                previousState.distanceToCenter,
                previousAction.ordinal()};

//        double [] scaled_vctr = downScaleVector(oneHotVectorFor(x));
        replayMemory.add(new Experience(previousState,previousAction,currentReward,currentState));
        replayExperience(replayMemory);

        if (numRoundsTo100 < 100) {
            numRoundsTo100++;
            totalNumRounds++;
            numWins++;
        } else {
            try {
                logger_print();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        totalReward = 0;
        execute();
    }

    @Override
    public void onBattleEnded(BattleEndedEvent event) {
        super.onBattleEnded(event);
    }
}