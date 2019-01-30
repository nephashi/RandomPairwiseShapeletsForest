/**
 * 
 */
package edu.bjtu.core.timeseries;

import java.util.ArrayList;

/**
 * @author Atif Raza
 */
public class TimeSeries {
    protected Double[] ts;
    protected int tsClass;
    protected Double[] sumX;
    protected Double[] sumX2;
    protected int startPos;
    
    public TimeSeries(ArrayList<Double> timeSeries, int classLabel) {
        this.tsClass = classLabel;
        this.ts = new Double[timeSeries.size()];
        this.sumX = new Double[timeSeries.size()];
        this.sumX2 = new Double[timeSeries.size()];
        double val;
        for (int i = 0; i < timeSeries.size(); i++) {
            val = timeSeries.get(i);
            this.ts[i] = val;
            if (i == 0) {
                this.sumX[i] = val;
                this.sumX2[i] = val * val;
            } else {
                this.sumX[i] = this.sumX[i - 1] + val;
                this.sumX2[i] = this.sumX2[i - 1] + val * val;
            }
        }
    }
    
    public TimeSeries(TimeSeries s, int start, int end) {
        this.ts = new Double[end - start];
        this.sumX = new Double[end - start];
        this.sumX2 = new Double[end - start];
        double val;
        for (int indSrc = start, indDest = 0; indSrc < end; indSrc++, indDest++) {
            val = s.get(indSrc);
            this.ts[indDest] = val;
            if (indDest == 0) {
                this.sumX[indDest] = val;
                this.sumX2[indDest] = val * val;
            } else {
                this.sumX[indDest] = this.sumX[indDest - 1] + val;
                this.sumX2[indDest] = this.sumX2[indDest - 1] + val * val;
            }
        }
    }
    
    public void setLabel(int classLabel) {
        this.tsClass = classLabel;
    }
    
    public int getLabel() {
        return this.tsClass;
    }
    
    public double get(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index can not be negative");
        } else if (index > this.ts.length) {
            throw new IndexOutOfBoundsException("The index is larger than the TimeSeries length");
        } else {
            return this.ts[index].doubleValue();
        }
    }
    
    public int size() {
        return this.ts.length;
    }
    
    @Override
    public String toString() {
        StringBuilder tsString = new StringBuilder();
        tsString.append("Class: " + this.tsClass + "\tSeries: ");
        for (int i = 0; i < this.ts.length; i++) {
            tsString.append(String.format("%10.4f ", this.ts[i].doubleValue()));
        }
        return tsString.toString();
    }
    
    public Double[] getTS() {
        return this.ts;
    }
    
    public double mean(int i, int len) {
        return (ts[i] + sumX[i + len - 1] - sumX[i]) / len;
    }
    
    public double stdv(int i, int len) {
        double mu = mean(i, len);
        double s2 = ((ts[i] * ts[i] + sumX2[i + len - 1] - sumX2[i]) / len)
                    - mu * mu;
        if (s2 <= 0)
            return 0;
        else
            return Math.sqrt(s2);
    }

    public void setStartPos(int startPos) {
        this.startPos = startPos;
    }

    public int getStartPos() {
        return this.startPos;
    }
}
