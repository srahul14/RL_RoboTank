package ece.cpen502;

import robocode.RobocodeFileOutputStream;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

public class NeuralNet implements NeuralNetInterface {
    // Implement the methods specified in NeuralNetInterface

    // Neural network architecture
    private int argNumInputs;
    private int argNumHidden;
    private int argNumOutputs;
    private final double argLearningRate;
    private final double argMomentumTerm;
    private final double argA;
    private final double argB;
    private final boolean bipolar;

    /*
     * Weights between the layers
     */
    private double[][] inputHiddenWeights;
    private double[][] hiddenOutputWeights;

    // bias value
    final double bias;

    /*
     * Layers of the network
     */
    private double[] inputLayer;
    private double[] hiddenLayer;
    private double[] outputLayer;

    /*
     * Backpropagation update arrays
     */
    private double[][] lastWeightChangeInputToHidden; // Used to implement the momentum term during weight change
    private double[][] lastWeightChangeHiddenToOutput; // Used to implement the momentum term during weight change

    // Output and hidden layer's error signals
    private double[] hiddenErrorSignal;
    private double[] outputErrorSignal;

    // Constructor to initialize the neural network architecture
    public NeuralNet(
            int argNumInputs,
            int argNumHidden,
            double argLearningRate,
            double argMomentumTerm,
            double argA,
            double argB,
            boolean bipolar) {
        this.argNumInputs = argNumInputs;
        this.argNumHidden = argNumHidden;
        this.argNumOutputs = 1;
        this.argLearningRate = argLearningRate;
        this.argMomentumTerm = argMomentumTerm;
        this.argA = argA;
        this.argB = argB;
        this.bipolar = bipolar;
        this.bias = 1.0;
        this.inputHiddenWeights = new double[argNumInputs + 1][argNumHidden];
        this.hiddenOutputWeights = new double[argNumHidden + 1][argNumOutputs];
        this.inputLayer = new double[argNumInputs + 1];
        this.hiddenLayer = new double[argNumHidden + 1];
        this.outputLayer = new double[argNumOutputs];
        this.lastWeightChangeInputToHidden = new double[argNumInputs + 1][argNumHidden];
        this.lastWeightChangeHiddenToOutput = new double[argNumHidden + 1][argNumOutputs];
        this.hiddenErrorSignal = new double[argNumHidden + 1];
        this.outputErrorSignal = new double[argNumOutputs];
    }

    /**
     * Return a bipolar sigmoid of the input X
     * 
     * @param x The input
     * @return f(x) = 2 / (1+e(-x)) - 1
     */
    @Override
    public double sigmoid(double x) {
        // Implement the sigmoid activation function
        return -1 + 2.0 / (1.0 + Math.exp(-x));
    }

    /**
     * This method implements a general sigmoid with asymptotes bounded by (a, b)
     * 
     * @param x The input
     * @return f(x) = b_minus_a / (1 + e(-x)) - minus_a
     */
    @Override
    public double customSigmoid(double x) {
        if (bipolar) {
            return sigmoid(x);
        }
        return argA + ((argB - argA) / (1.0 + Math.exp(-x)));
    }

    /**
     * Return a derivative of sigmoid of the activated input y.
     * 
     * @param y The activated input.
     * @return f'(S) = y * (1 - y) if binary else f'(S) = 0.5 * (1 + y) * (1 - y).
     */
    public double sigmoidDerivative(double y) {
        if (!bipolar) {
            return y * (1.0 - y);
        }
        return 0.5 * (1 + y) * (1 - y);
    }

    /**
     * Initialize the weights to random values.
     * For say 2 inputs, the input vector is [0] & [1]. We add [2] for the bias.
     * Like wise for hidden units. For say 2 hidden units which are stored in an
     * array.
     * [0] & [1] are the hidden & [2] the bias.
     * We also initialize the last weight change arrays. This is to implement the
     * alpha term.
     */
    @Override
    public void initializeWeights() {
        // Initialize weights with random values

        // Initialize input-hidden weights
        for (int i = 0; i < argNumInputs + 1; i++) {
            for (int j = 0; j < argNumHidden; j++) {
                inputHiddenWeights[i][j] = Math.random() - 0.5;
            }
        }

        // Initialize hidden-output weights
        for (int i = 0; i < argNumHidden + 1; i++) {
            for (int j = 0; j < argNumOutputs; j++) {
                hiddenOutputWeights[i][j] = Math.random() - 0.5;
            }
        }
    }

    /**
     * Initialize the weights to 0.
     */
    @Override
    public void zeroWeights() {
        for (int i = 0; i < argNumInputs + 1; i++) {
            for (int j = 0; j < argNumHidden; j++) {
                inputHiddenWeights[i][j] = 0.0;
            }
        }
        for (int i = 0; i < argNumHidden + 1; i++) {
            for (int j = 0; j < argNumOutputs; j++) {
                hiddenOutputWeights[i][j] = 0.0;
            }
        }
    }

    /**
     * @param X The input vector. An array of doubles.
     * @return The value returned by the LUT or NN for this input vector
     */
    @Override
    public double outputFor(double[] X) {
        if (X.length != argNumInputs) {
            throw new IllegalArgumentException("Input size does not match the network architecture.");
        }

        for (int i = 0; i < argNumInputs; i++) {
            inputLayer[i] = X[i];
        }
        // add bias node to input
        inputLayer[argNumInputs] = bias;

        // Perform forward propagation to compute the final output
        forwardPropagation(inputLayer);

        // Return the final output (assuming one output neuron)
        return outputLayer[0];
    }

    // Method for forward propagation to set the output layer
    private void forwardPropagation(double[] input) {
        // Compute the activations of the hidden layer
        for (int j = 0; j < argNumHidden; j++) {
            double weightedSum = 0.0;
            for (int i = 0; i < argNumInputs + 1; i++) {
                weightedSum += input[i] * inputHiddenWeights[i][j];
            }
            // Apply the activation function (e.g., sigmoid or custom sigmoid)
            hiddenLayer[j] = customSigmoid(weightedSum);
        }
        // add bias node to hidden layer
        hiddenLayer[argNumHidden] = bias;

        // Compute the final output layer activation
        for (int j = 0; j < argNumOutputs; j++) {
            double weightedSum = 0.0;
            for (int i = 0; i < argNumHidden + 1; i++) {
                weightedSum += hiddenLayer[i] * hiddenOutputWeights[i][j];
            }
            // Apply the activation function for the output layer
            outputLayer[j] = customSigmoid(weightedSum);
        }
    }

    /**
     * This method will tell the NN or the LUT the output
     * value that should be mapped to the given input vector. I.e.
     * the desired correct output value for an input.
     * 
     * @param X        The input vector
     * @param argValue The new value to learn
     * @return The error in the output for that input vector
     */
    @Override
    public double train(double[] X, double argValue) {
        double error = 0.0;
        // Perform forward propagation to compute the actual output
        double actualOutput = outputFor(X);

        // Calculate error
        error += 0.5 * Math.pow((actualOutput - argValue), 2);

        // Backpropagation to update weights
        backPropagation(actualOutput, argValue);

        return error;
    }

    // Method for back propagation to update the weights
    private void backPropagation(double actualOutput, double argValue) {

        // Calculate the error for the output layer
        for (int i = 0; i < argNumOutputs; i++) {
            double derivative = sigmoidDerivative(actualOutput);
            outputErrorSignal[i] = (argValue - actualOutput) * derivative;
        }

        // Update hidden-to-output weights
        for (int j = 0; j < argNumOutputs; j++) {
            for (int i = 0; i < argNumHidden + 1; i++) {
                double deltaWeight = argMomentumTerm * lastWeightChangeHiddenToOutput[i][j]
                        + argLearningRate * outputErrorSignal[j] * hiddenLayer[i];
                hiddenOutputWeights[i][j] += deltaWeight;
                lastWeightChangeHiddenToOutput[i][j] = deltaWeight;
            }
        }

        // Calculate the error for the hidden layer
        for (int i = 0; i < argNumHidden; i++) {
            for (int j = 0; j < argNumOutputs; j++) {
                hiddenErrorSignal[i] = hiddenOutputWeights[i][j] * outputErrorSignal[j]
                        * sigmoidDerivative(hiddenLayer[i]);
            }
        }

        // Update input-to-hidden weights
        for (int i = 0; i < argNumInputs + 1; i++) {
            for (int j = 0; j < argNumHidden; j++) {
                double deltaWeight = argMomentumTerm * lastWeightChangeInputToHidden[i][j]
                        + argLearningRate * hiddenErrorSignal[j] * inputLayer[i];
                inputHiddenWeights[i][j] += deltaWeight;
                lastWeightChangeInputToHidden[i][j] = deltaWeight;
            }
        }
    }

    /**
     * A method to write either a LUT or weights of a neural net to a file.
     * 
     * @param argFile of type File.
     * @throws IOException If there is an issue with file input/output.
     */
    @Override
    public void save(File argFile) throws IOException {
        PrintStream savefile = null;
        try  {
            savefile = new PrintStream(new RobocodeFileOutputStream(argFile));
        } catch (IOException e) {
            // Handle any exceptions that may occur during file I/O
            System.err.println("Error saving neural network: " + e.getMessage());
            throw e;
        }


//        savefile.println(argNumInputs);
//        savefile.println(argNumHidden);

        // Save the architecture and weights of the neural network
        savefile.println("Architecture: " + argNumInputs + "-" + argNumHidden + "-" + argNumOutputs + "\n");

        // Save input-hidden weights
        savefile.println("Input-Hidden Weights:\n");
        for (int i = 0; i < argNumInputs + 1; i++) {
            for (int j = 0; j < argNumHidden; j++) {
                savefile.println(inputHiddenWeights[i][j] + " ");
            }
            savefile.println("\n");
        }

        // Save hidden-output weights
        savefile.println("Hidden-Output Weights:\n");
        for (int i = 0; i < argNumHidden + 1; i++) {
            for (int j = 0; j < argNumOutputs; j++) {
                savefile.println(hiddenOutputWeights[i][j] + " ");
            }
            savefile.println("\n");
        }

        System.out.println("Neural network saved successfully to: " + argFile.getAbsolutePath());
    }

    /**
     * Loads the LUT or neural net weights from a file. The load must, of course,
     * have knowledge of how the data was written out by the save method.
     * You should raise an error in the case that an attempt is being
     * made to load data into an LUT or neural net whose structure does not match
     * the data in the file (e.g., the wrong number of hidden neurons).
     * 
     * @param argFileName The name of the file to load.
     * @throws IOException If there is an issue with file input/output.
     */
    @Override
    public void load(String argFileName) throws IOException {
        try (Scanner scanner = new Scanner(new File(argFileName))) {
            // Read the architecture line
            String architectureLine = scanner.nextLine();
            String[] architectureParts = architectureLine.split(":");
            String[] architectureValues = architectureParts[1].trim().split("-");

            int loadedNumInputs = Integer.parseInt(architectureValues[0]);
            int loadedNumHidden = Integer.parseInt(architectureValues[1]);

            // Check if the loaded architecture matches the current network
            if (loadedNumInputs != argNumInputs || loadedNumHidden != argNumHidden) {
                throw new IllegalArgumentException("Loaded architecture does not match the current network.");
            }

            // Read and set the input-hidden weights
            for (int i = 0; i < argNumInputs + 1; i++) {
                String weightsLine = scanner.nextLine();
                String[] weightsValues = weightsLine.trim().split(" ");
                for (int j = 0; j < argNumHidden; j++) {
                    inputHiddenWeights[i][j] = Double.parseDouble(weightsValues[j]);
                }
            }

            // Read and set the hidden-output weights
            for (int i = 0; i < argNumHidden + 1; i++) {
                String weightsLine = scanner.nextLine();
                String[] weightsValues = weightsLine.trim().split(" ");
                for (int j = 0; j < argNumOutputs; j++) {
                    hiddenOutputWeights[i][j] = Double.parseDouble(weightsValues[j]);
                }
            }

            System.out.println("Neural network loaded successfully from: " + argFileName);
        } catch (IOException e) {
            // Handle any exceptions that may occur during file I/O
            System.err.println("Error loading neural network: " + e.getMessage());
            throw e;
        }

    }
}
