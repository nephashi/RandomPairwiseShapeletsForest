package edu.bjtu.core.shapelets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;

import edu.bjtu.core.timeseries.TimeSeries;
import edu.bjtu.core.timeseries.TimeSeriesDataset;

public class LogicalShapelets extends BaseShapelets {
	
	protected static final int MAXREF = 5;
	protected int stepSize = 1;

	public LogicalShapelets(TimeSeriesDataset trainSet, int minLen, int maxLen, int stepSize) {
		super(trainSet, minLen, maxLen, stepSize);
	}

	@Override
	public Shapelet findShapelet() {
        TimeSeries x, y;
        double mux, muy, sgx, sgy, smxy;
        long count = 0, pruneCount = 0;
//        long bhulCount = 0;
        int refCount = 0;
        
        XYMatrix[] xy = new XYMatrix[trainSet.size()];
        for (int i = 0; i < trainSet.size(); i++) {
        	xy[i] = new XYMatrix(trainSet.get(i).size(), trainSet.get(i).size());
        }
        double bsf = 0;
        OrderLine line = new OrderLine(trainSet);
        OrderLine bestSplit = new OrderLine();
        OrderLine[] references = new OrderLine[MAXREF];
        
        for (int k = 0; k < MAXREF; k++) {
            references[k] = new OrderLine();
        }

        for (int j = 0; j < trainSet.size(); j++) {
        	x = trainSet.get(j);
        	for (int k = 0; k < trainSet.size(); k++) {
        		y = trainSet.get(k);
        		xy[k].computeXY(x.getTS(), y.getTS());
        	}
        	for (int len = maxLen; len >= minLen; len -= stepSize) {
        		refCount = 0;
        		// clear H
        		for (int i = 0; i < trainSet.get(j).size() - len + 1; i++) {
        			count++;
                    mux = trainSet.get(j).mean(i,len);
                    sgx = trainSet.get(j).stdv(i,len);
                    if (sgx == 0) {
                    	continue;
                    }
                    int yy = 0;
                    while( refCount >= MAXREF && yy < MAXREF )
                    {
                            int r = references[yy].shapeletPos;
                            muy = trainSet.get(j).mean(r,len);
                            sgy = trainSet.get(j).stdv(r,len);
                            smxy = xy[j].sumXY(i,r,len);
                            if (sgy == 0) {
                                System.exit(1);
                            }
                             double t = smxy - len*mux*muy;
                             t = t / (len*sgx*sgy);
                             t = 2*len*(1-t);
                             t = (t < 0) ? 0.0 : Math.sqrt(t);
                             
                             references[yy].TUB(t);
                             if( references[yy].informationGain < bestSplit.informationGain )
                                     break;
                             yy++;
                    }

                    if (refCount >= MAXREF && yy < MAXREF) {
                        pruneCount++;
                        continue;
                    }
                    line.reset(j,i,len);
	        		// for (w = 1 to |H|)
	        		for (int k = 0; k < trainSet.size(); k++) {
                        bsf = Double.POSITIVE_INFINITY;
                        double t;
                        for (int u = 0; u < trainSet.get(k).size()-len+1; u++) {
							muy = trainSet.get(k).mean(u, len);
							sgy = trainSet.get(k).stdv(u, len);
							if (sgy == 0) {
								sgy = 1;
							}
							smxy = xy[k].sumXY(i, u, len);
							t = smxy - len * mux * muy;
							t = t / (len * sgx * sgy);
							t = 2 * len * (1 - t);
							t = (t < 0) ? 0.0 : Math.sqrt(t);
							if (t < bsf) {
								bsf = t;
							}
						}
                        Projection p = new Projection(k, bsf);
                        line.insert(k , p);
	        		}
                    // calculate bestIG
	        		line.findBestSplit();
                    for( int op = 0 ; refCount >= MAXREF && op < MAXREF ; op++ ) {
                        if (references[op].informationGain < line.informationGain) {
//                            bhulCount++;
                            break;
                        }
                    }
	        		// if updates required update best shapelet, best split, best line
					if (line.informationGain > bestSplit.informationGain) {
						bestSplit.copy(line);
					} else if (line.informationGain == bestSplit.informationGain && bestSplit.gap < line.gap) {
						bestSplit.copy(line);
					}
	        		// if maxGain is not changed add line and shapelet to H
					double minimumgain = 0;
					int minref = refCount % MAXREF;
					for (int er = 0; refCount >= MAXREF && er < MAXREF; er++) {
						if (references[er].tIG > minimumgain) {
							minimumgain = references[er].tIG;
							minref = er;
						}
					}
	                if(line.informationGain < bestSplit.informationGain) {
	                    references[minref].copy(line);
	                    references[minref].tIG = line.informationGain;
	                    refCount = refCount+1;
	                }
        		}
        	}
        }
//        System.out.format("Shpelet ID : %d , Start Position : %d , Shapelet Length : %d\n\n", bestSplit.shapeletID , bestSplit.shapeletPos , bestSplit.shapeletLength );
//        System.out.format("Split informationGain : %f , split position %d , split distance %f and the separation gap %f\n\n",bestSplit.informationGain,bestSplit.splitPos,bestSplit.splitDist,bestSplit.gap);
//        System.out.format("Total candidates : %d , ",count);
//        System.out.format("Number of Pruned candidates : %d\n\n", pruneCount );
        totalCandidates += count;
        prunedCandidates += pruneCount;
//        System.out.format("Time for current shapelet : %f seconds ",(t2-t1)/1e3);
//        System.out.format("and Bound Computation Time %d\n\n\n", boundTime);
//        System.out.format("Bhool Count %d\n\n\n",bhulCount);
        TimeSeries shapeletTS = new TimeSeries(trainSet.get(bestSplit.shapeletID), bestSplit.shapeletPos, bestSplit.shapeletPos + bestSplit.shapeletLength);
        TreeMap<Double, ArrayList<Integer>> obj_hist = new TreeMap<>();
        for (int i = 0; i < trainSet.size(); i++) {
        	this.addToMap(obj_hist, bestSplit.line[i].distance, bestSplit.line[i].tsID);
        }
        Shapelet s = new Shapelet(shapeletTS, bestSplit.splitDist, obj_hist);
		return s;
	}
    
	@Override
	public double getDist(TimeSeries t, TimeSeries s) {
	    int len = s.size();
	    XYMatrix xy = new XYMatrix(t.size(), len);
	    xy.computeXY(t.getTS(), s.getTS());
	    double mux, muy, sgx, sgy, smxy;
	    muy = s.mean(0, len);
	    sgy = s.stdv(0, len);
	    
	    double bsf = Double.POSITIVE_INFINITY, c;
	    for (int u = 0; u < t.size() - len + 1; u++) {
	        mux = t.mean(u, len);
	        sgx = t.stdv(u, len);
	        if (sgx == 0) {
	            sgx = 1;
	        }
	        smxy = xy.sumXY(u, 0, len);
	        c = smxy - len*mux*muy;
	        c = c / (len*sgx*sgy);
	        c = Math.sqrt(2*len*(1-c));
//	        c = (c < 0.0) ? 0 : Math.sqrt(2*len*(1-c));
	        if (c < bsf) {
	            bsf = c;
	        }
	    }
	    
		return bsf;
	}

	class XYMatrix {
	    protected double[][] d;
	    protected int lenX;
	    protected int lenY;
	    
	    public XYMatrix(int lenX, int lenY) {
	        this.lenX = lenX;
	        this.lenY = lenY;
	        this.d = new double[lenX][lenY];
	    }
	    
	    public double sumXY(int indX, int indY, int len) {
	        if (indX > 0 && indY > 0) {
	            return d[indX + len - 1][indY + len - 1] - d[indX - 1][indY - 1];
	        } else {
	            return d[indX + len - 1][indY + len - 1];
	        }
	    }
	    
	    public void computeXY(Double[] tsX, Double[] tsY) {
	        this.lenX = tsX.length;
	        this.lenY = tsY.length;
	        int L = (lenX > lenY) ? lenX : lenY;
	        int i, j, k;
	        for (k = 0; k < L; k++) {
	            if (k < lenX) {
	                d[k][0] = tsX[k] * tsY[0];
	                for (i = k + 1, j = 1; i < lenX && j < lenY; i++, j++)
	                    d[i][j] = d[i - 1][j - 1] + tsX[i] * tsY[j];
	            }
	            if (k < lenY) {
	                d[0][k] = tsX[0] * tsY[k];
	                for (i = 1, j = k + 1; i < lenX && j < lenY; i++, j++)
	                    d[i][j] = d[i - 1][j - 1] + tsX[i] * tsY[j];
	            }
	        }
	    }
	}
	
	class Projection implements Comparable<Projection> {
	    public int tsID;
	    public double distance;
	    public Projection() {
	        tsID = -1;
	        distance = Double.POSITIVE_INFINITY;
	    }
	    public Projection(int tID, double dist) {
	        this.tsID = tID;
	        this.distance = dist;
	    }
	    public int getLabel() {
	    	return trainSet.get(tsID).getLabel();
	    }
	    public int compareTo(Projection p) {
	    	if (this.distance < p.distance ) {
	            return -1;
	    	} else if (this.distance == p.distance ) {
	        	return 0;
	    	} else {
	            return 1;
	    	}
	    }
	}
	
	class OrderLine {
	    
	    public int shapeletID;
	    public int shapeletPos;
	    public int shapeletLength;
	    
	    public int N = 0;
	    public int curN = 0;
	    public int nCls = 0;
	    public double entropy = 0;
	    public HashMap<Integer, Integer> clsHist = null;
	    
	    public HashMap<Integer, Integer> leftClsHist = null;
	    public HashMap<Integer, Integer> rightClsHist = null;
	    
	    public double leftTotal = 0;
	    public double rightTotal = 0;
	    public double leftEntropy = 0;
	    public double rightEntropy = 0;
	    
	    public double informationGain = 0;
	    public double gap = 0;
	    public double splitDist = -1;
	    public int splitPos = -1;
	    
	    public double tIG;
	    
	    public Projection[] line;
	    
	    public OrderLine() {
	    }
	    
	    public OrderLine(TimeSeriesDataset trainSet) {
	        N = trainSet.size();
	        nCls = trainSet.getNumOfClasses();
	        entropy = trainSet.entropy();
	        clsHist = new HashMap<Integer, Integer>(trainSet.getClassHist());
	        leftClsHist = new HashMap<Integer, Integer>();
	        rightClsHist = new HashMap<Integer, Integer>();
	        splitPos = -1;
	        splitDist = -1;
	        curN = 0;
	        
	        line = new Projection[N];
	    }
	    
	    public void reset(int sId, int sPos, int sLen) {
	        shapeletID = sId; shapeletPos = sPos; shapeletLength = sLen;
	        splitPos = -1;
	        splitDist = -1;
	        curN = 0;
	        rightTotal = leftTotal = 0;
	        
	        Projection p = new Projection();
	        
	        for (int i = 0; i < N; i++)
	            line[i] = p;
	            
	        rightClsHist.clear();
	        leftClsHist.clear();
	        informationGain = 0;
	        gap = 0;
	    }
	    
	    public void copy(OrderLine src) {
	        shapeletID = src.shapeletID;
	        shapeletPos = src.shapeletPos;
	        shapeletLength = src.shapeletLength;
	        
	        N = src.N;
	        curN = src.curN;
	        splitPos = src.splitPos;
	        splitDist = src.splitDist;
	        nCls = src.nCls;
	        informationGain = src.informationGain;
	        gap = src.gap;
	        
	        if (line == null)
	            line = new Projection[N];
	            
	        for (int i = 0; i < N; i++) {
	            line[i] = src.line[i];
	        }
	            
	        if (leftClsHist == null)
	            leftClsHist = new HashMap<Integer, Integer>(src.leftClsHist);
	        if (rightClsHist == null)
	            rightClsHist = new HashMap<Integer, Integer>(src.rightClsHist);
	        if (clsHist == null)
	            clsHist = new HashMap<Integer, Integer>(src.clsHist);
	        
	        leftClsHist = new HashMap<Integer, Integer>(src.leftClsHist);
	        rightClsHist = new HashMap<Integer, Integer>(src.rightClsHist);
	        clsHist = new HashMap<Integer, Integer>(src.clsHist);
	        
	        entropy = src.entropy;
	    }
	    
	    public void insert(int i, Projection p) {
	        if (curN == N) {
	            System.out.println("ERROR!!! line is full.\n");
	            System.err.println("ERROR : Memory can't be allocated!!!\n\n");
	        }
	        // ordered insertion
	        int j, k;
	        for (j = 0; j < curN; j++) {
	            if (line[j].distance > p.distance) {
	                for (k = curN - 1; k >= j; k--)
	                    line[k + 1] = line[k];
	                line[j] = p;
	                break;
	            }
	        }
	        if (j == curN)
	            line[j] = p;
	        
	        rightClsHist.put(p.getLabel(), rightClsHist.getOrDefault(p.getLabel(), 0) + 1);
	        rightTotal++;
	        
	        if (curN == i)
	            curN++;
	        else
	            System.out.println("ERROR!!! insertion order missmatch\n");
	    }
	    
	    public double minGap(int j) {
	        double meanLeft = 0, meanRight = 0;
	        for (int i = 0; i <= j; i++)
	            meanLeft += line[i].distance;
	            
	        meanLeft /= (j + 1);
	        
	        for (int i = j + 1; i < N; i++)
	            meanRight += line[i].distance;
	            
	        meanRight /= (N - j);
	        
	        return (meanRight - meanLeft) / Math.sqrt(shapeletLength);
	    }
	    
	    public double gapDist(int j) {
	        if (j < curN) {
	            return (line[j + 1].distance + line[j].distance) / 2.0;
	        } else
	            return 0;
	    }
	    
	    public double shiftEntropy(double shiftAmount) {
	        Projection[] tempLine = line.clone();
	        
	        double maxInf = 0, maxGap = 0, maxDistance = 0;
	        int maxi = -1;
	        
	        for (int q = 0; q < N; q++) {
	            splitPos = q;
	            
	            for (int j = 1; j <= Math.pow(2.0, nCls) - 2; j++) {
	                for (int i = 0; i < N; i++) {
	                    int k = j & (int) Math.pow(2.0, line[i].getLabel());
	                    if (k == 0)
	                        line[i].distance -= shiftAmount;
	                    else
	                        line[i].distance += shiftAmount;
	                }
	                Arrays.sort(line);
	                findEntropies();
	                if (informationGain > maxInf) {
	                    maxi = splitPos;
	                    maxInf = informationGain;
	                    maxGap = gap;
	                    maxDistance = splitDist;
	                }
	                line = tempLine;
	            }
	            
	        }
	        splitPos = maxi;
	        informationGain = maxInf;
	        gap = maxGap;
	        splitDist = maxDistance;
	        tempLine = null;
	        
	        return maxInf;
	    }
	    
	    public double TUB(double shiftAmount) {
	        Projection[] tempLine = line.clone();

	        double maxInf = 0;
	        
	        int i;
	        
	        HashMap<Integer, Integer> lch_tmp = new HashMap<Integer, Integer>();
	        HashMap<Integer, Integer> rch_tmp = new HashMap<Integer, Integer>();
	        int ltot_tmp = 0;
	        int rtot_tmp = N;
	        
	        int val;
	        for (Integer key : clsHist.keySet()) {
	            leftClsHist.put(key, 0);
	            rch_tmp.put(key, 0);
	            lch_tmp.put(key, 0);
	            val = clsHist.get(key);
	            rch_tmp.put(key, val);
	            rightClsHist.put(key, val);
	        }
	        
	        leftTotal = 0;
	        rightTotal = N;
	        HashMap<Integer, Integer> classType = new HashMap<Integer, Integer>();
	        
	        int leftStart = 0, rightStart = 0;
	        
	        int k;
	        for (k = 0; k < N - 1; k++) {
	            int spPos = k;
	            int c = line[k].getLabel();
	            lch_tmp.put(c, lch_tmp.getOrDefault(c, 0) + 1);
	            leftClsHist.put(c, leftClsHist.getOrDefault(c, 0) + 1);
	            ltot_tmp++;
	            leftTotal++;
	            rch_tmp.put(c, rch_tmp.getOrDefault(c, 0) - 1);
	            rightClsHist.put(c, rightClsHist.getOrDefault(c, 0) - 1);
	            rtot_tmp--;
	            rightTotal--;
	           
	            
	            for (Integer key : clsHist.keySet())    // for (i = 0; i < nCls; i++)
	            {
	                if (leftClsHist.get(key) / leftTotal > rightClsHist.get(key) / rightTotal
	                    && (leftClsHist.get(key) + 1) / (leftTotal + 1) > (rightClsHist.get(key) - 1) / (rightTotal - 1))
	                    classType.put(key, -1);
	                else if (leftClsHist.get(key) / leftTotal < rightClsHist.get(key) / rightTotal
	                         && (leftClsHist.get(key) - 1) / (leftTotal - 1) < (rightClsHist.get(key) + 1) / (rightTotal + 1))
	                    classType.put(key, 1);
	                else
	                    classType.put(key, 0);
	            }
	            
	            for (i = leftStart; i <= spPos; i++) {
	                if (Math.abs(line[i].distance - line[spPos].distance) < shiftAmount)
	                    break;
	            }
	                    
	            leftStart = i;
	            
	            for (i = rightStart; i < N; i++) {
	                if (Math.abs(line[i].distance - line[spPos].distance) > shiftAmount)
	                    break;
	            }
	                    
	            rightStart = i;
	            
	            for (i = leftStart; i < rightStart; i++) {
	                int c1 = line[i].getLabel();
	                if (classType.get(c1) == -1 && i > spPos) {
	                    leftClsHist.put(c1, leftClsHist.getOrDefault(c1, 0) + 1);
	                    leftTotal++;
	                    rightClsHist.put(c1, rightClsHist.getOrDefault(c1, 0) - 1);
	                    rightTotal--;
	                } else if (classType.get(c1) == 1 && i <= spPos) {
	                    leftClsHist.put(c1, leftClsHist.getOrDefault(c1, 0) - 1);
	                    leftTotal--;
	                    rightClsHist.put(c1, rightClsHist.getOrDefault(c1, 0) + 1);
	                    rightTotal++;
	                }
	            }
	            informationGain = computeInformationGain();
	            if (informationGain > maxInf) {
	                maxInf = informationGain;
	            }
	            line = tempLine.clone();
	            leftClsHist = new HashMap<Integer, Integer>(lch_tmp);

	            rightClsHist = new HashMap<Integer, Integer>(rch_tmp);
	            leftTotal = ltot_tmp;
	            rightTotal = rtot_tmp;
	            
	        }
	        informationGain = maxInf;
	        return maxInf;
	    }
	    
	    public double findEntropies() {
	        
	        for (Integer i : clsHist.keySet()) {
	            leftClsHist.put(i, 0);
	            rightClsHist.put(i, clsHist.get(i));
	        }
	        leftTotal = 0;
	        rightTotal = N;
	        int i;
	        
	        for (i = 0; i <= splitPos; i++) {
	            int c = line[i].getLabel();
	            leftClsHist.put(c, leftClsHist.getOrDefault(c, 0) + 1);
	            leftTotal++;
	            rightClsHist.put(c, rightClsHist.getOrDefault(c, 0) - 1);
	            rightTotal--;
	        }
	        informationGain = computeInformationGain();
	        
	        return informationGain;
	    }
	    
	    public double computeInformationGain() {
	        double ratio;
	        
	        leftEntropy = 0;
	        for (Integer i : clsHist.keySet()) {
	            ratio = (double) leftClsHist.get(i) / leftTotal;
	            if (ratio > 0)
	                leftEntropy += -(Math.log(ratio) * ratio);
	                
	        }
	        
	        rightEntropy = 0;
	        for (Integer i : clsHist.keySet()) {
	            ratio = (double) rightClsHist.get(i) / rightTotal;
	            if (ratio > 0)
	                rightEntropy += -(Math.log(ratio) * ratio);
	        }
	        
	        return entropy - (leftTotal / N) * leftEntropy
	               - (rightTotal / N) * rightEntropy;
	    }
	    
	    public double findBestSplit() {
	        for (Integer i1 : clsHist.keySet()) {
	            leftClsHist.put(i1, 0);
	            rightClsHist.put(i1, clsHist.get(i1));
	        }
	        leftTotal = 0;
	        rightTotal = N;
	        
	        double maxInf = 0;
	        double maxGap = 0;
	        int maxi = -1;
	        
	        int i;
	        for (i = 0; i < N; i++) {
	            int c = line[i].getLabel();
	            leftClsHist.put(c, leftClsHist.getOrDefault(c, 0) + 1);
	            leftTotal++;
	            rightClsHist.put(c, rightClsHist.getOrDefault(c, 0) - 1);
	            rightTotal--;
	            informationGain = computeInformationGain();
	            double mG = minGap(i);
	            if (informationGain > maxInf) {
	                maxi = i;
	                maxInf = informationGain;
	                maxGap = mG;
	            } else if (informationGain == maxInf && mG > maxGap) {
	                maxi = i;
	                maxInf = informationGain;
	                maxGap = mG;
	            }
	            
	        }
	        gap = maxGap;
	        splitPos = maxi;
	        splitDist = gapDist(splitPos);
	        informationGain = maxInf;
	        
	        return maxInf;
	    }
	}
}
