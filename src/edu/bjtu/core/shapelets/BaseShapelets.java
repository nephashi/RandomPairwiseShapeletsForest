package edu.bjtu.core.shapelets;

import edu.bjtu.core.timeseries.TimeSeries;
import edu.bjtu.core.timeseries.TimeSeriesDataset;

import java.util.ArrayList;
import java.util.TreeMap;

public abstract class BaseShapelets {
    
    protected static long totalCandidates = 0;
    protected static long prunedCandidates = 0;
    protected TimeSeriesDataset trainSet;
    protected int minLen;
    protected int maxLen;
    protected int stepSize;
    protected Info info;
    
    public BaseShapelets(TimeSeriesDataset trainSet, int minLen, int maxLen, int stepSize) {
        this.trainSet = trainSet;
        this.minLen = minLen;
        this.maxLen = maxLen;
        this.stepSize = stepSize;
        this.info = new Info();
    }
    
    public long getTotalCandidates() {
        return totalCandidates;
    }
    
    public long getPrunedCandidates() {
        return prunedCandidates;
    }
    
    public abstract Shapelet findShapelet();
    
    public abstract double getDist(TimeSeries t, TimeSeries s);
    
    protected void addToMap(TreeMap<Double, ArrayList<Integer>> container, double key, Integer value) {
        ArrayList<Integer> values = container.getOrDefault(key, new ArrayList<Integer>());
        values.add(value);
        container.put(key, values);
    }

    public TimeSeriesDataset[] splitDataset(int[] split) {
        TimeSeriesDataset[] splits = new TimeSeriesDataset[2];
        splits[0] = new TimeSeriesDataset();
        splits[1] = new TimeSeriesDataset();
        for (int i = 0; i < trainSet.size(); i++) {
            if(split[i] == 0) {
                splits[0].add(trainSet.get(i));
            } else {
                splits[1].add(trainSet.get(i));
            }
        }
        return splits;
    }
    
    public TimeSeriesDataset[] splitDataset(TreeMap<Double, ArrayList<Integer>> obj_hist, double split_dist) {
        final int numSplits = 2;
        final boolean usingWeights = this.trainSet.isUsingWeights();
        
        TimeSeriesDataset[] splits = new TimeSeriesDataset[numSplits];
        double[] sums = new double[numSplits];
        ArrayList<ArrayList<Double>> weights = new ArrayList<>();
        
        for (int i = 0; i < numSplits; i++) {
            splits[i] = new TimeSeriesDataset();
            sums[i] = 0;
            weights.add(new ArrayList<>());
        }
        
        for (Double d : obj_hist.keySet()) {
            if (d.doubleValue() < split_dist) {
                for (Integer index : obj_hist.get(d)) {
                    splits[0].add(this.trainSet.get(index));
                    if (usingWeights) {
                        weights.get(0).add(this.trainSet.getWeight(index));
                        sums[0] += this.trainSet.getWeight(index);
                    }
                }
            } else {
                for (Integer index : obj_hist.get(d)) {
                    splits[1].add(this.trainSet.get(index));
                    if (usingWeights) {
                        weights.get(1).add(this.trainSet.getWeight(index));
                        sums[1] += this.trainSet.getWeight(index);
                    }
                }
            }
        }
        if (usingWeights) {
            for (int i = 0; i < numSplits; i++) {
                for (int j = 0; j < weights.get(i).size(); j++) {
                    weights.get(i).set(j, weights.get(i).get(j) / sums[i]);
                }
                splits[i].setWeights(weights.get(i));
            }
        }
        return splits;
    }
    
    public long getNumOfCandidatesToProcess() {
        long total = 0L;
        long temp;
        for (int cL = this.minLen; cL <= this.maxLen; cL += this.stepSize) {
            temp = 0L;
            for (int cPiI = 0; (cPiI + cL) <= this.trainSet.get(0).size(); cPiI++) {
                temp++;
            }
            total += temp * this.trainSet.size();
        }
        return total;
    }
    
    protected boolean nearlyEqual(double a, double b, double epsilon) {
        final double absA = Math.abs(a);
        final double absB = Math.abs(b);
        final double diff = Math.abs(a - b);
        
        if (a == b) {                       // shortcut, handles infinities
            return true;
        } else if (a == 0 || b == 0 || diff < Double.MIN_NORMAL) {
            // a or b is zero or both are extremely close to it
            // relative error is less meaningful here
            return diff < (epsilon * Double.MIN_NORMAL);
        } else { // use relative error
            return diff / Math.min((absA + absB), Double.MAX_VALUE) < epsilon;
        }
    }
    
    protected class Info {
        public double gain;
        public double splitDist;
        public double splitGap;
    }
}
