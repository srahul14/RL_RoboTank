package ece.cpen502;

import java.io.*;
import robocode.RobocodeFileOutputStream;
import robocode.RobocodeFileWriter;

public class LUT implements LUTInterface{

    private double[][][][][][] lut;
    private int myEnergy;
    private int enemyEnergy;
    private int enemyBearing;
    private int enemyDistance;
    private int distanceToCenter;
    private int actionSpace;
    public LUT(
            int myEnergy,
            int enemyEnergy,
            int enemyBearing,
            int enemyDistance,
            int distanceToCenter,
            int actionSpace) {
        this.myEnergy = myEnergy;
        this.enemyEnergy = enemyEnergy;
        this.enemyBearing = enemyBearing;
        this.enemyDistance = enemyDistance;
        this.distanceToCenter = distanceToCenter;
        this.actionSpace = actionSpace;

        lut = new double[myEnergy][enemyEnergy][enemyBearing][enemyDistance][distanceToCenter][actionSpace];
        this.initialiseLUT();
    }
    @Override
    public double outputFor(double[] X) throws ArrayIndexOutOfBoundsException{
        if (X.length != 6)
            throw new ArrayIndexOutOfBoundsException();
        else {
            int a = (int) X[0];
            int b = (int) X[1];
            int c = (int) X[2];
            int d = (int) X[3];
            int e = (int) X[4];
            int f = (int) X[5];

            return lut[a][b][c][d][e][f];
        }
    }

    @Override
    public double train(double[] X, double argValue) {

        if (X.length != 6)
            throw new ArrayIndexOutOfBoundsException();
        else {
            int a = (int) X[0];
            int b = (int) X[1];
            int c = (int) X[2];
            int d = (int) X[3];
            int e = (int) X[4];
            int f = (int) X[5];

            lut[a][b][c][d][e][f] = argValue;
        }
        return 0;
    }

    @Override
    public void save(File argFile) throws IOException {

        PrintStream saveFile = null;

        try {
            saveFile = new PrintStream(new RobocodeFileOutputStream(argFile));
        } catch (IOException e) {
            System.out.println("*** Could not create output stream for save file.");
        }

        // First line is the number of rows of data
        assert saveFile != null;
        saveFile.println(myEnergy * enemyEnergy * enemyBearing * enemyDistance * distanceToCenter * actionSpace);

        // Second line is the number of dimensions per row
        saveFile.println(6);

        System.out.println("start writing");


        for (int a = 0; a < myEnergy; a++) {
            for (int b = 0; b < enemyEnergy; b++) {
                for (int c = 0; c < enemyBearing; c++) {
                    for (int d = 0; d < enemyDistance; d++) {
                        for (int e = 0; e < distanceToCenter; e++) {
                            for (int f =0; f<actionSpace; f++) {

                                String row = String.format("%d,%d,%d,%d,%d,%d,%2.5f",
                                        a, b, c, d, e, f, lut[a][b][c][d][e][f]
                                );
                                saveFile.println(row);
                            }

                        }
                    }
                }
            }
        }
        saveFile.close();
        System.out.println("finish saving");
    }

    @Override
    public void load(String argFileName) throws IOException {
        FileInputStream inputFile = new FileInputStream(argFileName);
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(inputFile));
        int numExpectedRows = myEnergy * enemyEnergy * enemyBearing * enemyDistance * distanceToCenter * actionSpace;

        // Check the number of rows is compatible
        int numRows = Integer.valueOf(inputReader.readLine());
        // Check the number of dimensions is compatible
        int numDimensions = Integer.valueOf(inputReader.readLine());

        if (numRows != numExpectedRows || numDimensions != 5) {
            System.out.printf(
                    "*** rows/dimensions expected is %s/%s but %s/%s encountered\n",
                    numExpectedRows, 6, numRows, numDimensions
            );
            inputReader.close();
            throw new IOException();
        }

        for (int a = 0; a < myEnergy; a++) {
            for (int b = 0; b < enemyEnergy; b++) {
                for (int c = 0; c < enemyBearing; c++) {
                    for (int d = 0; d < enemyDistance; d++) {
                        for (int e = 0; e < distanceToCenter; e++) {
                            for (int f =0; f<actionSpace; f++) {

                                // Read line formatted like this: <e,d,e2,d2,a,q,visits\n>
                                String line = inputReader.readLine();
                                String[] tokens = line.split(",");
                                int dim1 = Integer.parseInt(tokens[0]);
                                int dim2 = Integer.parseInt(tokens[1]);
                                int dim3 = Integer.parseInt(tokens[2]);
                                int dim4 = Integer.parseInt(tokens[3]);
                                int dim5 = Integer.parseInt(tokens[4]);
                                int dim6 = Integer.parseInt(tokens[5]);// actions
                                double q = Double.parseDouble(tokens[6]);
                                lut[a][b][c][d][e][f] = q;
                            }
                        }
                    }
                }
            }
        }
        inputReader.close();
    }

    @Override
    public void initialiseLUT() {

        for (int a = 0; a < myEnergy; a++) {
            for (int b = 0; b < enemyEnergy; b++) {
                for (int c = 0; c < enemyBearing; c++) {
                    for (int d = 0; d < enemyDistance; d++) {
                        for (int e = 0; e < distanceToCenter; e++) {
                            for (int f =0; f<actionSpace; f++) {

                                lut[a][b][c][d][e][f] = Math.random();
                            }

                        }
                    }
                }
            }
        }

    }

    @Override
    public int indexFor(double[] X) {
        return 0;
    }
}
