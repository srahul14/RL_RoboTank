package ece.cpen502;

import java.io.File;
import java.io.IOException;

/**
 * This interface is common to both the Neural Net and LUT interfaces.
 * The idea is that you should be able to easily switch the LUT
 * for the Neural Net since the interfaces are identical.
 * @date 20 June 2012
 * @author sarbjit
 */
public interface CommonInterface {

    /**
     * @param X The input vector. An array of doubles.
     * @return The value returned by the LUT or NN for this input vector
     */
    double outputFor(double[] X);

    /**
     * This method will tell the NN or the LUT the output
     * value that should be mapped to the given input vector. I.e.
     * the desired correct output value for an input.
     * @param X The input vector
     * @param argValue The new value to learn
     * @return The error in the output for that input vector
     */
    double train(double[] X, double argValue);

    /**
     * A method to write either a LUT or weights of a neural net to a file.
     * @param argFile of type File.
     */
    void save(File argFile) throws IOException;

    /**
     * Loads the LUT or neural net weights from a file. The load must, of course,
     * have knowledge of how the data was written out by the save method.
     * You should raise an error in the case that an attempt is being
     * made to load data into an LUT or neural net whose structure does not match
     * the data in the file (e.g., the wrong number of hidden neurons).
     * @param argFileName The name of the file to load.
     * @throws IOException If there is an issue with file input/output.
     */
    void load(String argFileName) throws IOException;
}
