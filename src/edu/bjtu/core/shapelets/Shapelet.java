package edu.bjtu.core.shapelets;

import edu.bjtu.core.timeseries.TimeSeries;

import java.util.ArrayList;
import java.util.TreeMap;

public class Shapelet {
    protected TimeSeries shapelet;
    protected double splitDist;
    protected TreeMap<Double, ArrayList<Integer>> dsHist;
    
    public Shapelet() {
        this.shapelet = null;
        this.splitDist = Double.POSITIVE_INFINITY;
        this.dsHist = null;
    }
    
    public Shapelet(TimeSeries shapelet, double splitDist, TreeMap<Double, ArrayList<Integer>> dsHist) {
        this.shapelet = shapelet;
        this.splitDist = splitDist;
        this.dsHist = dsHist;
    }

    public int[] getPairSplit() throws UnsupportedOperationException{
        throw new UnsupportedOperationException();
    }
    
    public TimeSeries getShapelet() {
        return this.shapelet;
    }
    
    public double getSplitDist() {
        return this.splitDist;
    }
    
    public TreeMap<Double, ArrayList<Integer>> getHistMap() {
        return this.dsHist;
    }
}
