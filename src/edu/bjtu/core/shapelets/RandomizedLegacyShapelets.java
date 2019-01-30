/**
 * 
 */
package edu.bjtu.core.shapelets;

import edu.bjtu.core.timeseries.TimeSeries;
import edu.bjtu.core.timeseries.TimeSeriesDataset;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.Random;

/**
 * @author razaa
 *
 */
public class RandomizedLegacyShapelets extends LegacyShapelets {
    
    protected static Random rand;
    protected static double percentage;

    
    public RandomizedLegacyShapelets(TimeSeriesDataset trainSet, int minLen, int maxLen, int stepSize) {
        super(trainSet, minLen, maxLen, stepSize);
        try {
            Properties props = new Properties();
            File propsFile = new File(System.getProperty("rs-props", "rs.properties"));
            if (propsFile.exists()) {
                props.load(new FileInputStream(propsFile));
            }
            percentage = Double.parseDouble(props.getProperty("selection_ratio", "10")) / 100;
            if (props.containsKey("rand_seed")) {
                int seed = Integer.parseInt(props.getProperty("rand_seed", "0"));
                rand = new Random(seed);
            } else {
                rand = new Random();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    protected TimeSeries getNextCandidate() {
        // get a first candidate then start skipping randomly
        // until a random number comes up so that its less than
        // the set percentage then check if candidates are still
        // available, if so, create a new candidate at current
        // position or return the already created one
        TimeSeries candidate = super.getNextCandidate();
        while ((rand.nextFloat() > percentage) && this.hasMoreCandidates) {
            this.incrementCandidatePosition();
        }
        if (this.hasMoreCandidates) {
            candidate = super.getNextCandidate();
        }
        return candidate;
    }
    
    public double getSamplingPercentage() {
        return percentage;
    }
}
