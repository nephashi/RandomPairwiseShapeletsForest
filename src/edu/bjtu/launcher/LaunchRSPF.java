package edu.bjtu.launcher;

import edu.bjtu.CommonConfig;
import edu.bjtu.core.btree.DecisionTree;
import edu.bjtu.core.timeseries.TimeSeriesDataset;

import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class LaunchRSPF {
    
	public static void main(String[] args){
    	String dirs[] = new String[]{
//    	        "CinC_ECG_torso_TRAIN50"
//    	        "SyntheticControl"
//  			"Coffee",
//    			"CBF",
//   			"ECGFiveDays",
//    			"DiatomSizeReduction",
//    			"ItalyPowerDemand",
//    			"Adiac",
//    			"Beef",
//    			"FaceFour",
//    			"MoteStrain",
//    			"ShapeletSim",
//    			"SonyAIBORobotSurface1",
//    			"SonyAIBORobotSurface2",
//    			"ToeSegmentation2",
//    			"TwoLeadECG",
//    			"ArrowHead",
//    			"BeetleFly",
//    			"BirdChicken",
//    			"ECG200",
    			"GunPoint",
//    			"Meat",
//    			"OliveOil",
//    			"Plane",
//    			"Symbols",
//    			"ToeSegmentation1",
//    			"Trace",
//    			"Wine",
//    			"SyntheticControl",
//    			"DistalPhalanxTW",
//    			"FacesUCR",
//    			"Car",
//				"DistalPhalanxOutlineAgeGroup",
//				"DistalPhalanxOutlineCorrect",
//				"DistalPhalanxTW",
//				"MiddlePhalanxOutlineAgeGroup",
//				"MiddlePhalanxOutlineCorrect",
//				"MiddlePhalanxTW",
//				"ProximalPhalanxOutlineAgeGroup",
//				"ProximalPhalanxTW",
//                "ProximalPhalanxOutlineCorrect",
//                "Lightning2",
//                "Lightning7",
//                "Ham",
//                "Fish",
//				"Herring",
//                "MedicalImages",
//                "DP_Little",
//                "DP_Middle",
//                "DP_Thumb",
//                "MP_Little",
//                "MP_Middle",
//                "PP_Little",
//                "PP_Middle",
//                "PP_Thumb",
    	};
    	
    	for(String s:dirs){
    	    try {
                for(int i = 0; i < 10; i++) {
                    String params[] = new String[]{
                            // data path
                            "-dd", "D:\\data\\TSC\\TSC_Problems",
                            // dataset name
                            "-dn", s,
                            // method, 2: gRSF, 4: RPSF
                            "-sm", "4",
                            // result file name, written in project_root/results
                            "-rf", "RPSF.csv"
                    };
                    main1(params);
                }
            } catch (Exception e) {
    	        e.printStackTrace();
            }

    	}
    }
	

    public static void main1(String[] args) throws Exception{
	    CommonConfig cc = new CommonConfig(args);

	    int method = cc.getMethod();

        System.out.println("method = " + method);
        TimeSeriesDataset trainSet = new TimeSeriesDataset(cc.getTrainSet()),
                          testSet = new TimeSeriesDataset(cc.getTestSet()),
                          trainSetBagged;
        long start, stop;
        double trainingTime;
        DecisionTree tree;
        ArrayList<DecisionTree> dtList = new ArrayList<>();
        Random rng = new Random();
        IntStream randIntStream;
        ArrayList<Integer> randIndices;
        
        start = System.currentTimeMillis();

        for (int i = 0; i < cc.getEnsembleSize(); i++) {

            System.out.println("building tree " + i);
            trainSetBagged = new TimeSeriesDataset();
            randIntStream = rng.ints(trainSet.size(), 0, trainSet.size());
            randIndices = (ArrayList<Integer>)randIntStream.boxed().collect(Collectors.toList());//有放回的实例抽样过程，抽样个数等于训练集合大小
            for (Integer ind : randIndices) {
                trainSetBagged.add(trainSet.get(ind));
            }
            tree = new DecisionTree.Builder(trainSetBagged, method)
                                   .minLen(cc.getMinLen())
                                   .maxLen(cc.getMaxLen())
                                   .stepSize(cc.getStepSize())
                                   .leafeSize(cc.getLeafSize())
                                   .treeDepth(cc.getTreeDepth())
                                   .build();
            dtList.add(tree);
        }
        stop = System.currentTimeMillis();
        trainingTime = (stop - start) / 1e3;

        cc.saveResults(dtList, trainSet, testSet, trainingTime);
    }
}
