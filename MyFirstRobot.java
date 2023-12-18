//package ece.cpen502;
//
//
//import robocode.*;
//
//import java.awt.*;
//import java.awt.geom.Point2D;
//import java.io.*;
//import java.util.Random;
//
//public class MyFirstRobot extends AdvancedRobot {
//
//    private String lutFilename =  getClass().getSimpleName() + ".txt";
//
//    public enum enumEnergy {critical, veryLow, low, medium, high};
//    public enum enumDistance {veryClose, close, near, far, veryFar};
//    public enum enumDistanceToCenter { close, medium, far};
//    public enum enumEnemyBearing {q1, q2, q3, q4};
//    public enum enumAction {forward, back, left, right,fire};
//
//    public enum enumOptionalMode {scan, performanceAction};
//
//    static private LUT q = new LUT(
//            enumEnergy.values().length,
//            enumEnergy.values().length,
//            enumEnemyBearing.values().length,
//            enumDistance.values().length,
//            enumDistanceToCenter.values().length,
//            enumAction.values().length
//    );
//
//    public static int totalNumRounds = 0;
//   public static int numRoundsTo100 = 0;
//    public static int numWins = 0;
//    public static double sumTotalReward = 0;
//
//    private enumEnergy currentMyEnergy = enumEnergy.high;
//    private enumEnergy currentEnemyEnergy = enumEnergy.high;
//    private enumEnemyBearing currentEnemyBearing = enumEnemyBearing.q1;
//    private enumDistance currentEnemyDistance = enumDistance.near;
//    private enumDistanceToCenter currentDistanceToCenter = enumDistanceToCenter.close;
//    private enumAction currentAction = enumAction.forward;
//
//    private enumEnergy previousMyEnergy = enumEnergy.high;
//    private enumEnergy previousEnemyEnergy = enumEnergy.high;
//    private enumEnemyBearing previousEnemyBearing = enumEnemyBearing.q4;
//    private enumDistance previousEnemyDistance = enumDistance.near;
//    private enumDistanceToCenter previousDistanceToCenter = enumDistanceToCenter.close;
//    private enumAction previousAction = enumAction.forward;
//
//    // Initialization: operationMode
//    private enumOptionalMode myOperationMode= enumOptionalMode.scan;
//
//    // set RL
//    private double gamma = 0.9;
//    private double alpha = 0.01;
//    private final double epsilon_initial = 0.9;
//    private double epsilon = epsilon_initial;
//    private boolean decayEpsilon = false;
//
//    //previous, current and max Q
//    private double currentQ = 0.0;
//    private double previousQ = 0.0;
//    private double maxQ = 0.0;
//
//    // Rewards
//    private final double goodReward = 1.0;
//    private final double badReward = -0.25;
//    private final double goodTerminalReward = 2.0;
//    private final double badTerminalReward = -0.5;
//
//    private double currentReward = 0.0;
//
//    // Initialize states
//    double myX = 0.0;
//    double myY = 0.0;
//    double myEnergy = 0.0;
//    double enemyBearing = 0.0;
//    double enemyDistance = 0.0;
//    double distanceToCenter = 0.0;
//    double enemyEnergy = 0.0;
//
//    double totalReward = 0.0;
//
//    // Whether you take immediate rewards
//    public static boolean takeImmediate = true;
//
//    // Whether you take off-policy algorithm
//    public static boolean offPolicy = true;
//
//    // Logging
//    static String logFilename = "robotLUT.log";
//    static LogFile log = null;
//
//    // get center of board
//    int xMid = 0;
//    int yMid = 0;
//
//
//    /**
//     * MyFirstRobot's run method
//     */
//    public void run() {
//
//        // get coordinate of the board center
//        int xMid = (int) getBattleFieldWidth() / 2;
//        int yMid = (int) getBattleFieldHeight() / 2;
//
//        if (log == null) {
//            System.out.print(logFilename);
//            log = new LogFile(getDataFile(logFilename));
//            log.stream.printf("gamma,   %2.2f\n", gamma);
//            log.stream.printf("alpha,   %2.2f\n", alpha);
//            log.stream.printf("epsilon, %2.2f\n", epsilon);
//            log.stream.printf("badInstantReward, %2.2f\n", badReward);
//            log.stream.printf("badTerminalReward, %2.2f\n", badTerminalReward);
//            log.stream.printf("goodInstantReward, %2.2f\n", goodReward);
//            log.stream.printf("goodTerminalReward, %2.2f\n\n", goodTerminalReward);
//        }
//
//        while (true) {
//            if (totalNumRounds > 40000) {epsilon = 0.0;}
//            switch (myOperationMode) {
//                case scan: {
//                    currentReward = 0.0;
//                    turnRadarLeft(90);
//                    break;
//                }
//                case performanceAction: {
//                    if (Math.random()<= epsilon){
//                        currentAction = selectRandomAction();
//                    }
//                    else{
//                        currentAction = selectBestAction (
//                                myEnergy,
//                                enemyEnergy,
//                                enemyBearing,
//                                enemyDistance,
//                                distanceToCenter
//                        );
//                    }
//
//                    switch (currentAction) {
//                        case forward: {
//                            setAhead(100);
//                            execute();
//                            break;
//                        }
//
//                        case back: {
//                            setBack(100);
//                            execute();
//                            break;
//                        }
//
//                        case left: {
//                            setTurnLeft(30);
//                            execute();
//                            break;
//                        }
//
//                        case right: {
//                            setTurnRight(30);
//                            execute();
//                            break;
//                        }
//
//                        case fire: {
//                            turnGunRight(getHeading() - getGunHeading() + enemyBearing);
//                            fire(3);
//                            break;
//                        }
//                    }
//                    if (getGunHeat() == 0) {execute();}
//                    double[] x = new double[]{
//                            previousMyEnergy.ordinal(),
//                            previousEnemyEnergy.ordinal(),
//                            previousEnemyBearing.ordinal(),
//                            previousEnemyDistance.ordinal(),
//                            previousDistanceToCenter.ordinal(),
//                            previousAction.ordinal()};
//
//                    q.train(x, computeQ(currentReward));
//                    myOperationMode = enumOptionalMode.scan;
//                    execute();
//                }
//            }
//        }
//
//    }
//
//    public double computeQ(double r) {
//        enumAction maxA = selectBestAction(
//                currentMyEnergy.ordinal(),
//                currentEnemyEnergy.ordinal(),
//                currentEnemyBearing.ordinal(),
//                currentEnemyDistance.ordinal(),
//                currentDistanceToCenter.ordinal());
//
//        double[] prevStateAction = new double[]{
//                previousMyEnergy.ordinal(),
//                previousEnemyEnergy.ordinal(),
//                previousEnemyBearing.ordinal(),
//                previousEnemyDistance.ordinal(),
//                previousDistanceToCenter.ordinal(),
//                previousAction.ordinal()};
//
//        double[] currentStateAction = new double[]{
//                currentMyEnergy.ordinal(),
//                currentEnemyEnergy.ordinal(),
//                currentEnemyBearing.ordinal(),
//                currentEnemyDistance.ordinal(),
//                currentDistanceToCenter.ordinal(),
//                currentAction.ordinal()};
//
//        double[] maxStateAction = new double[]{
//                currentMyEnergy.ordinal(),
//                currentEnemyEnergy.ordinal(),
//                currentEnemyBearing.ordinal(),
//                currentEnemyDistance.ordinal(),
//                currentDistanceToCenter.ordinal(),
//                maxA.ordinal()};
//
//        double prevQ = q.outputFor(prevStateAction);
//        double maxQ = q.outputFor(maxStateAction);
//        double currentQ = q.outputFor(currentStateAction);
//
//        return offPolicy ? prevQ + alpha * (r + gamma * maxQ - prevQ):
//        prevQ + alpha * (r + gamma * currentQ - prevQ);
//    }
//
//    public enumAction selectRandomAction() {
//        Random rand = new Random();
//        int r = rand.nextInt(enumAction.values().length);
//        return enumAction.values()[r];
//    }
//
//    public enumAction selectBestAction(double e, double e2, double b, double d, double dc) {
//        int energy = enumEnergyOf(e).ordinal();
//        int enemyEnergy = enumEnergyOf(e2).ordinal();
//        int enemyBearing = enumBearingOf(b).ordinal();
//        int enemyDistance = enumDistanceOf(d).ordinal();
//        int distanceToCenter = enumDistanceToCenterOf(dc).ordinal();
//        double bestQ = -Double.MAX_VALUE;
//        enumAction bestAction = null;
//
//        for (int a = enumAction.forward.ordinal(); a < enumAction.values().length; a++) {
//            double[] x = new double[]{energy, enemyEnergy, enemyBearing, enemyDistance, distanceToCenter, a};
//            if (q.outputFor(x) > bestQ) {
//                bestQ = q.outputFor(x);
//                bestAction = enumAction.values()[a];
//            }
//        }
//        return bestAction;
//    }
//
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
//
//    public double dtc(double fromX, double fromY, double toX, double toY) {
//        double distance = Math.sqrt(Math.pow((fromX - toX), 2) + Math.pow((fromY - toY), 2));
//        return distance;
//    }
//
//    private void logger_print() {
//        log.stream.printf("%d - %d, %2.1f\n", totalNumRounds - 100, totalNumRounds, 100.0 * numWins / numRoundsTo100);
//        log.stream.flush();
//        numRoundsTo100 = 0;
//        numWins = 0;
//        sumTotalReward = 0;
//    }
//
//    @Override
//    public void onScannedRobot(ScannedRobotEvent e) {
//        super.onScannedRobot(e);
//        myEnergy = getEnergy();
//        myX = getX();
//        myY = getY();
//        enemyEnergy = e.getEnergy();
//        enemyDistance=e.getDistance();
//        distanceToCenter = dtc(myX, myY, xMid, yMid);
//        enemyBearing= e.getBearing();
//
//        // Update states
//        previousMyEnergy = currentMyEnergy;
//        previousEnemyEnergy = currentEnemyEnergy;
//        previousEnemyBearing = currentEnemyBearing;
//        previousEnemyDistance = currentEnemyDistance;
//        previousDistanceToCenter = currentDistanceToCenter;
//        previousAction = currentAction;
//
//        currentMyEnergy = enumEnergyOf(myEnergy);
//        currentEnemyEnergy = enumEnergyOf(enemyEnergy);
//        currentEnemyBearing = enumBearingOf(enemyBearing);
//        currentEnemyDistance = enumDistanceOf(enemyDistance);
//        currentDistanceToCenter = enumDistanceToCenterOf(distanceToCenter);
//        myOperationMode = enumOptionalMode.performanceAction;
//    }
//
//    @Override
//    public void onHitByBullet(HitByBulletEvent e) {
//        if (takeImmediate){
//            currentReward = badReward;
//            totalReward += currentReward;
//        }
//    }
//
//    @Override
//    public void onBulletHit(BulletHitEvent e) {
//        if (takeImmediate) {
//            currentReward = goodReward;
//            totalReward += currentReward;
//        }
//    }
//
//    @Override
//    public void onHitWall(HitWallEvent event) {
//        if (takeImmediate){
//            currentReward = badReward;
//            totalReward += currentReward;
//        }
//    }
//
//    @Override
//    public void onDeath(DeathEvent e) {
//        currentReward = badTerminalReward;
//        totalReward += currentReward;
//
//        double[] x = new double[]{
//                previousMyEnergy.ordinal(),
//                previousEnemyEnergy.ordinal(),
//                previousEnemyBearing.ordinal(),
//                previousEnemyDistance.ordinal(),
//                previousDistanceToCenter.ordinal(),
//                previousAction.ordinal()};
//
//        q.train(x, computeQ(currentReward));
//
//        if (numRoundsTo100 < 100) {
//            numRoundsTo100++;
//            totalNumRounds++;
//            sumTotalReward += totalReward;
//        } else {
//            logger_print();
//        }
//
//        totalReward = 0;
//
//        try {
//            q.save(getDataFile(lutFilename));
//        } catch (IOException ex) {
//            throw new RuntimeException(ex);
//        }
//        execute();
//    }
//
//    @Override
//    public void onWin(WinEvent e) {
//        currentReward = goodTerminalReward;
//        totalReward += currentReward;
//
//        double[] x = new double[]{
//                previousMyEnergy.ordinal(),
//                previousEnemyEnergy.ordinal(),
//                previousEnemyBearing.ordinal(),
//                previousEnemyDistance.ordinal(),
//                previousDistanceToCenter.ordinal(),
//                previousAction.ordinal()};
//
//        q.train(x, computeQ(currentReward));
//
//        if (numRoundsTo100 < 100) {
//            numRoundsTo100++;
//            totalNumRounds++;
//            numWins++;
//            sumTotalReward += totalReward;
//        } else {
//            logger_print();
//        }
//
//        totalReward = 0;
//
//        try {
//            q.save(getDataFile(lutFilename));
//        } catch (IOException ex) {
//            throw new RuntimeException(ex);
//        }
//
//        execute();
//    }
//
//    @Override
//    public void onBattleEnded(BattleEndedEvent event) {
//        super.onBattleEnded(event);
//    }
//}