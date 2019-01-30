package edu.bjtu.core.shapelets;

import edu.bjtu.core.timeseries.TimeSeries;

public class ShapeletPair extends Shapelet{

    protected TimeSeries shapelet2;
    protected int[] split;
    protected double gain;

    public ShapeletPair() {
        this.shapelet = null;
        this.shapelet2 = null;
        split = null;
    }

    public ShapeletPair(TimeSeries shapelet, TimeSeries shapelet2, int[] split) {
        this.shapelet = shapelet;
        this.shapelet2 = shapelet2;
        this.split = split;
    }

    public TimeSeries getShapelet2() {
        return shapelet2;
    }

    public void setGain(double gain) {
        this.gain = gain;
    }

    public double getGain() {
        return gain;
    }

    @Override
    public int[] getPairSplit() { return split;}
}
