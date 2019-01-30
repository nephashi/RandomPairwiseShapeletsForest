/**
 * 
 */
package edu.bjtu.core.timeseries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * @author Atif Raza
 */
public class TimeSeriesDataset {
    protected ArrayList<TimeSeries> dataset;
    protected HashMap<Integer, Integer> instsPerClass;
    protected boolean isClassHistUpdated;
    protected ArrayList<Double> weights;
    protected boolean useWeights;
    
    public TimeSeriesDataset() {
        this.dataset = new ArrayList<TimeSeries>();
        this.instsPerClass = new HashMap<Integer, Integer>();
        this.isClassHistUpdated = true;
        this.weights = new ArrayList<>();
        this.useWeights = false;
    }
    
    public TimeSeriesDataset(ArrayList<TimeSeries> dataset) {
        this.dataset = dataset;
        this.instsPerClass = new HashMap<Integer, Integer>();
        this.updateClassHist();
    }
    
    public TimeSeries get(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index can not be negative");
        } else if (index > this.dataset.size()) {
            throw new IndexOutOfBoundsException("The index is larger than the TimeSeries objects in dataset");
        } else {
            return this.dataset.get(index);
        }
    }
    
    public void add(TimeSeries t) {
        this.dataset.add(t);
        this.isClassHistUpdated = false;
    }
    
    public void add(ArrayList<TimeSeries> insts) {
        for (TimeSeries t : insts) {
            this.add(t);
        }
        this.isClassHistUpdated = false;
    }
    
    public int size() {
        return this.dataset.size();
    }
    
    public double entropy() {
        double frac = 0, entropy = 0;
        int N = this.dataset.size();
        if (!this.isClassHistUpdated) {
            this.updateClassHist();
        }
        
        for (Integer cls : this.getAllClasses()) {
            if (this.useWeights) {
                frac = 0;
                for (int i = 0; i < N; i++) {
                    if (this.dataset.get(i).getLabel() == cls) {
                        frac += this.weights.get(i);
                    }
                }
            } else {
                frac = (double) this.instsPerClass.get(cls) / N;
            }
            entropy += (frac > 0.0) ? -1 * (frac) * Math.log(frac) : 0.0;
        }
        return entropy;
    }
    
    private void updateClassHist() {
        int currCount;
        Integer clsLabel;
        for (TimeSeries t : this.dataset) {
            clsLabel = t.getLabel();
            currCount = this.instsPerClass.getOrDefault(clsLabel, 0);
            this.instsPerClass.put(t.getLabel(), currCount + 1);
        }
        this.isClassHistUpdated = true;
    }
    
    public Set<Integer> getAllClasses() {
        return this.instsPerClass.keySet();
    }
    
    public int getNumOfClasses() {
        return this.instsPerClass.keySet().size();
    }
    
    public int getInstCount(int instClass) {
        return this.instsPerClass.getOrDefault(instClass, 0);
    }
    
    public HashMap<Integer, Integer> getClassHist() {
        if (!this.isClassHistUpdated) {
            this.updateClassHist();
        }
        return this.instsPerClass;
    }
    
    public void setWeights(ArrayList<Double> weights) {
        this.weights = weights;
        this.useWeights = true;
    }
    
    public double getWeight(int index) {
        return this.weights.get(index);
    }
    
    public boolean isUsingWeights() {
        return this.useWeights;
    }
}
