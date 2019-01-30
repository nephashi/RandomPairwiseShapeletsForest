package edu.bjtu.core.shapelets;

import edu.bjtu.core.timeseries.TimeSeries;
import edu.bjtu.core.timeseries.TimeSeriesDataset;

import java.util.Random;

public class PairShapeletsFinder extends LegacyShapelets{

    private int lengthIntervalSize;
    protected double percentage;
    private int trainSize;
    private int seriesLength;
    protected static Random rand;


    public PairShapeletsFinder(TimeSeriesDataset trainSet, int minLen, int maxLen, int stepSize) {
        super(trainSet, minLen, maxLen, stepSize);
        //+1是因为rand.nextInt不会出现上界的数
        lengthIntervalSize = maxLen - minLen;
        //percentage = Lab.percentage;
        percentage = 0.01;
        trainSize = trainSet.size();
        seriesLength = trainSet.get(0).getTS().length;
        rand = new Random();
    }

    public double nextcount = 0;
    public double calccount = 0;
    @Override
    public ShapeletPair findShapelet() {
        TimeSeries bsfShapelet1 = null;
        TimeSeries[] currCandidate;
        TimeSeries bsfShapelet2 = null;
        TimeSeries t;
        tmpInfo tmpInfo = new tmpInfo();
        int[] bsfSplit = null;
        double bsfGain = Double.NEGATIVE_INFINITY;
        double bsfGap = Double.NEGATIVE_INFINITY;
        int numToInspect = (int)((seriesLength * (maxLen - minLen) - (maxLen + minLen) * (maxLen - minLen) / 2) * trainSize * percentage);
        if(numToInspect == 0) numToInspect = 1;

        for(int i = 0; i < numToInspect; i++) {
            double start = System.currentTimeMillis();
            currCandidate = getNextCandidatePair();
            nextcount += System.currentTimeMillis() - start;
            totalCandidates++;
            info.gain = Double.NEGATIVE_INFINITY;
            start = System.currentTimeMillis();
            double[] tmp = calcGainAndGap(currCandidate, bsfGain, tmpInfo);
            calccount += System.currentTimeMillis() -start;
            double currGain = tmp[0];
            double currGap = tmp[1];
            if(currGain > bsfGain) {
                bsfGain = currGain;
                bsfGap = currGap;
                bsfShapelet1 = currCandidate[0];
                bsfShapelet2 = currCandidate[1];
                bsfSplit = tmpInfo.split;
            } else if(this.nearlyEqual(currGain, bsfGain, 1e-6) && currGap > bsfGap) {
                bsfGain = currGain;
                bsfGap = currGap;
                bsfShapelet1 = currCandidate[0];
                bsfShapelet2 = currCandidate[1];
                bsfSplit = tmpInfo.split;
            }
        }
//        if(test.count != 0) {
//            System.out.println("distance called:" + test.count + " avg length:" + test.length / test.count);
//            System.out.println("next:" + nextcount / 1e3 + " calc:" + calccount / 1e3);
//            System.out.println("d1:" + d1cost / 1e3 + " d2:" + d2cost / 1e3);
//        }
        ShapeletPair best = new ShapeletPair(bsfShapelet1, bsfShapelet2, bsfSplit);
        best.setGain(bsfGain);
        return best;
    }

    protected boolean entropyEarlyPruning(TimeSeriesDataset nearS1, TimeSeriesDataset nearS2, int index, double bsfGain) {
        for(Integer cls : trainSet.getAllClasses()) {
            TimeSeriesDataset optimalNearS1Case1 = new TimeSeriesDataset();
            TimeSeriesDataset optimalNearS2Case1 = new TimeSeriesDataset();
            for (int i = 0; i < nearS1.size(); i++)
                optimalNearS1Case1.add(nearS1.get(i));
            for(int i = 0; i < nearS2.size(); i++)
                optimalNearS2Case1.add(nearS2.get(i));
            for(int i = index + 1; i < trainSize; i++) {
                if(trainSet.get(i).getLabel() == cls)
                    optimalNearS1Case1.add(trainSet.get(i));
                else optimalNearS2Case1.add(trainSet.get(i));
            }
            double start = System.currentTimeMillis();
            double gain1 = this.calcGain(optimalNearS1Case1, optimalNearS2Case1);
            if(gain1 > bsfGain)
                //不应剪枝
                return false;

            TimeSeriesDataset optimalNearS1Case2 = new TimeSeriesDataset();
            TimeSeriesDataset optimalNearS2Case2 = new TimeSeriesDataset();
            for (int i = 0; i < nearS1.size(); i++)
                optimalNearS1Case2.add(nearS1.get(i));
            for(int i = 0; i < nearS2.size(); i++)
                optimalNearS2Case2.add(nearS2.get(i));
            for(int i = index + 1; i < trainSize; i++) {
                if(trainSet.get(i).getLabel() == cls)
                    optimalNearS2Case2.add(trainSet.get(i));
                else optimalNearS1Case2.add(trainSet.get(i));
            }
            start = System.currentTimeMillis();
            double gain2 = this.calcGain(optimalNearS1Case2, optimalNearS2Case2);
            if(gain2 > bsfGain)
                //不应剪枝
                return false;
        }
        return true;
    }

    public double d1cost = 0, d2cost = 0;
    private double[] calcGainAndGap(TimeSeries[] currCandidate, double bsfGain, tmpInfo tmpInfo) {
        boolean pruned = false;
        double gap = 0;
        TimeSeries t;
        double distance1, distance2;

        int[] split = new int[trainSize];
        TimeSeriesDataset nearS1 = new TimeSeriesDataset();
        TimeSeriesDataset nearS2 = new TimeSeriesDataset();
        for(int i = 0; i < trainSize; i++) {
            t = trainSet.get(i);
            double start1 = System.currentTimeMillis();
            double start = System.currentTimeMillis();
            distance1 = subseqDist(t, currCandidate[0]);
            d1cost += (System.currentTimeMillis() - start);
            start = System.currentTimeMillis();
            distance2 = subseqDist(t, currCandidate[1]);
            d2cost += (System.currentTimeMillis() - start);
            if(distance1 < distance2){
                gap += (distance2 - distance1);
                nearS1.add(t);
            } else {
                gap += (distance1 - distance2);
                nearS2.add(t);
                split[i] = 1;
            }
            start = System.currentTimeMillis();
            if(entropyPruningEnabled && entropyEarlyPruning(nearS1, nearS2, i, bsfGain)) {
                pruned = true;
                prunedCandidates++;
                break;
            }
        }
        double[] rst = new double[2];
        double start = System.currentTimeMillis();
        if(!pruned) {
            tmpInfo.split = split;
            rst[0] = calcGain(nearS1, nearS2);
            rst[1] = gap / Math.sqrt(currCandidate[0].size());
        }
        return rst;
    }

    private double calcGain(TimeSeriesDataset nearS1, TimeSeriesDataset nearS2) {
        return trainSet.entropy() - (nearS1.entropy() * nearS1.size() + nearS2.entropy() * nearS2.size()) / trainSize;
    }

    public TimeSeries[] getNextCandidatePair() {
        int length1 = rand.nextInt(lengthIntervalSize) + minLen;
        int length2 = rand.nextInt(lengthIntervalSize) + minLen;
        int startPos1 = rand.nextInt(seriesLength - length1);
        int startPos2 = rand.nextInt(seriesLength - length2);

        int seriesId1 = rand.nextInt(trainSize);
        int seriesId2 = rand.nextInt(trainSize);
        while (trainSet.get(seriesId1).getLabel() == trainSet.get(seriesId2).getLabel())
            seriesId2 = rand.nextInt(trainSize);
        TimeSeries whole1 = trainSet.get(seriesId1);
        TimeSeries whole2 = trainSet.get(seriesId2);

        TimeSeries[] ts = new TimeSeries[2];
        ts[0] = new TimeSeries(whole1, startPos1, startPos1 + length1);
        ts[0].setLabel(whole1.getLabel());
        ts[0].setStartPos(startPos1);
        ts[1] = new TimeSeries(whole2, startPos2, startPos2 + length2);
        ts[1].setLabel(whole2.getLabel());
        ts[1].setStartPos(startPos2);

        return ts;

    }

    public int getBranch(TimeSeries testInst, ShapeletPair pair) {
        double distance1 = subseqDist(testInst, pair.getShapelet());
        double distance2 = subseqDist(testInst, pair.getShapelet2());
        if(distance1 < distance2)
            return 0;
        else
            return 1;
    }
}

class tmpInfo {
    protected int[] split;
}