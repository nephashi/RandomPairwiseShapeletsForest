package edu.bjtu;

import org.apache.commons.cli.*;
import edu.bjtu.core.btree.DecisionTree;
import edu.bjtu.core.timeseries.TimeSeries;
import edu.bjtu.core.timeseries.TimeSeriesDataset;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class CommonConfig {
    // The following static String variables are the CLI switches
    protected static String shapeletMethodSw = "sm";
    protected static String shapeletMethodSwL = "method";

    protected static String datasetSw = "dn";
    protected static String datasetSwL = "dataset-name";
    
    protected static String dataPathSw = "dd";
    protected static String dataPathSwL = "data-dir";
    
    protected static String rsltPathSw = "rd";
    protected static String rsltPathSwL = "results-dir";

    protected static String rsltFNameSw = "rf";
    protected static String rsltFNameSwL = "results-file-name";
    
    protected static String paramsPathSw = "pd";
    protected static String paramsPathSwL = "params-dir";
    
    protected static String ensembleSizeSw = "es";
    protected static String ensembleSizeSwL = "ensemble-size";
    
    protected static String minLenFracSw = "mn";
    protected static String minLenFracSwL = "min-len-frac";
    
    protected static String maxLenFracSw = "mx";
    protected static String maxLenFracSwL = "max-len-frac";
    
    protected static String saveIterationResults = "itrslts";
    protected static String saveIterationResultsL = "save-iter-results";
    
    protected String resultsFileName;
    
    protected CommandLine cmdLine;
    protected Options options;
    
    protected ArrayList<TimeSeries> trainSet;
    protected ArrayList<TimeSeries> testSet;
    
    protected int minLen;
    protected int maxLen;
    protected int stepSize;
    protected int leafSize;
    protected int treeDepth;
    
    protected boolean detailedResultsEnabled;
    protected double meanSquaredError;
    protected double bias;
    protected double variance;
    protected double irreducibleError;
    protected HashMap<Integer, Double> trueVec;
    protected HashMap<Integer, Double> predVec;
    protected HashMap<Integer, Double> meanVec;
    
    public CommonConfig(String[] args) {
        this.constructCommandLine(args);
        if (this.cmdLine == null) {
            this.printHelp(true);
            System.exit(1);
        } else {
            resultsFileName = this.getResultsFileName();
            System.out.println("Data set: " + this.getDataSetName() + " - " + resultsFileName);
            this.trainSet = this.loadDataset(this.getDataSetName(), "_TRAIN.txt");
            this.testSet  = this.loadDataset(this.getDataSetName(), "_TEST.txt");
            this.parseCandidateLengthFractions();
            
            Properties props = new Properties();
            File propsFile;
            Path filePath;
            try {
                filePath = Paths.get(this.getParamsPath(), "default.params");
                propsFile = new File(filePath.toUri());
                if (propsFile.exists()) {
                    props.load(new FileInputStream(propsFile));
                }
                filePath = Paths.get(this.getParamsPath(), this.getDataSetName() + ".params");
                propsFile = new File(filePath.toUri());
                if (propsFile.exists()) {
                    props.load(new FileInputStream(propsFile));
                }
            } catch (Exception e) {
                System.err.println("Error opening properties file: " + e);
            }
            
            this.stepSize = 1;
            this.leafSize = 1;
            this.treeDepth = Integer.MAX_VALUE;
            
            if (props.containsKey("stepSize")) {
                this.stepSize = Integer.parseInt(props.getProperty("stepSize"));
            }
            if (props.containsKey("leafSize")) {
                this.leafSize = Integer.parseInt(props.getProperty("leafSize"));
                if (this.leafSize == 0) {
                    HashMap<Integer, Integer> classMap = new HashMap<>();
                    for (TimeSeries ts : trainSet) {
                        classMap.put(ts.getLabel(), classMap.getOrDefault(ts.getLabel(), 0)+1);
                    }
                    this.leafSize = classMap.entrySet()
                                            .stream()
                                            .min((x, y) -> x.getValue() < y.getValue() ? -1 : 1)
                                            .get()
                                            .getValue();
                }
            }
            if (props.containsKey("treeDepth")) {
                this.treeDepth = Integer.parseInt(props.getProperty("treeDepth"));
            }
            
            this.meanSquaredError = 0d;
            this.bias = 0d;
            this.variance = 0d;
            this.irreducibleError = 0d;
            this.trueVec = new HashMap<>();
            this.predVec = new HashMap<>();
            this.meanVec = new HashMap<>();
            this.detailedResultsEnabled = this.getDetailedResultsEnabled();
        }
    }
    
    protected void constructCommandLine(String[] args) {
        this.options = new Options();
        this.options.addOption(Option.builder(shapeletMethodSw)
                    .longOpt(shapeletMethodSwL)
                    .argName("SHAPELET TREE METHOD")
                    .required()
                    .desc("method to build shapelet forest")
                    .numberOfArgs(1)
                    .build())
                    .addOption(Option.builder(datasetSw)
                                     .longOpt(datasetSwL)
                                     .argName("FILE")
                                     .required()
                                     .desc("Data set name to be evaluated")
                                     .numberOfArgs(1)
                                     .build())
                    .addOption(Option.builder(dataPathSw)
                                     .longOpt(dataPathSwL)
                                     .argName("PATH/TO/DATA/DIRECTORY")
                                     .desc("Absolute or relative path to the directory with the data set file")
                                     .numberOfArgs(1)
                                     .build())
                    .addOption(Option.builder(rsltPathSw)
                                     .longOpt(rsltPathSwL)
                                     .argName("PATH/TO/RESULTS/DIRECTORY")
                                     .desc("Absolute or relative path to the directory where results will be saved"
                                           + "\nDefault: 'results' directory in the program directory")
                                     .numberOfArgs(1)
                                     .build())
                    .addOption(Option.builder(rsltFNameSw)
                            .longOpt(rsltFNameSwL)
                            .argName("RESULTS FILE NAME")
                            .required()
                            .desc("result file name")
                            .numberOfArgs(1)
                            .build())
                    .addOption(Option.builder(paramsPathSw)
                                     .longOpt(paramsPathSwL)
                                     .argName("PATH-TO-PARAMS-FOLDER")
                                     .desc("Absolute or relative path to the parameter files directory"
                                           + "\nDefault: 'params' directory in the program directory")
                                     .numberOfArgs(1)
                                     .build())
                    .addOption(Option.builder(ensembleSizeSw)
                                     .longOpt(ensembleSizeSwL)
                                     .argName("ENSEMBLE SIZE")
                                     .desc("The ensemble size\nDefault: 10 members")
                                     .numberOfArgs(1)
                                     .build())
                    .addOption(Option.builder(minLenFracSw)
                                     .longOpt(minLenFracSwL)
                                     .argName("MIN LEN FRACTION")
                                     .desc("Fraction of Time Series Length to use as Minimum Shapelet Length")
                                     .numberOfArgs(1)
                                     .build())
                    .addOption(Option.builder(maxLenFracSw)
                                     .longOpt(maxLenFracSwL)
                                     .argName("MAX LEN FRACTION")
                                     .desc("Fraction of Time Series Length to use as Maximum Shapelet Length")
                                     .numberOfArgs(1)
                                     .build())
                    .addOption(Option.builder(saveIterationResults)
                                     .longOpt(saveIterationResultsL)
                                     .argName("Save detailed iteration results")
                                     .desc("Enables the creation of results file with detailed results for each iteration")
                                     .numberOfArgs(1)
                                     .build());
        
        try {
            CommandLineParser cliParser = new DefaultParser();
            
            this.cmdLine = cliParser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error parsing command line arguments\nInvalid or no arguments provided");
        }
    }
    
    protected ArrayList<TimeSeries> loadDataset(String fileName, String split) {
        ArrayList<TimeSeries> dataset = null;
        TimeSeries currTS;
        ArrayList<Double> ts;
        int tsClass;
        
        String currLine, delimiter = ",";
        StringTokenizer st;
        try (BufferedReader br = Files.newBufferedReader(Paths.get(this.getDataPath(),
                                                                   fileName,
                                                                   fileName + split))) {
            dataset = new ArrayList<TimeSeries>();
            while ((currLine = br.readLine()) != null) {
                if (currLine.matches("\\s*")) {
                    continue;
                } else {
                    currLine = currLine.trim();
                    st = new StringTokenizer(currLine, String.valueOf(delimiter));
                    tsClass = (int) Double.parseDouble(st.nextToken());
                    ts = new ArrayList<Double>();
                    while (st.hasMoreTokens()) {
                        ts.add(Double.parseDouble(st.nextToken()));
                    }
                    currTS = new TimeSeries(ts, tsClass);
                    dataset.add(currTS);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading the data set file: " + e);
        }
        return dataset;
    }
    
    public String getDataSetName() {
        return this.cmdLine.getOptionValue(datasetSw);
    }

    public String getDataPath() {
        return this.cmdLine.getOptionValue(dataPathSw, "../time_series_data/formats/csv_norm");
    }

    public String getResultsPath() {
        return this.cmdLine.getOptionValue(rsltPathSw, "results");
    }

    public String getResultsFileName() {
        return this.cmdLine.getOptionValue(rsltFNameSw, "RPSF.csv");
    }
    
    public String getParamsPath() {
        return this.cmdLine.getOptionValue(paramsPathSw, "params");
    }
    
    public int getEnsembleSize() {
        return Integer.parseInt(this.cmdLine.getOptionValue(ensembleSizeSw, "50"));
    }

    public int getMethod() {
        return Integer.parseInt(this.cmdLine.getOptionValue(shapeletMethodSw, "4"));
    }
    
    public ArrayList<TimeSeries> getTrainSet() {
        return this.trainSet;
    }
    
    public ArrayList<TimeSeries> getTestSet() {
        return this.testSet;
    }
    
    public int getMinLen() {
        return this.minLen;
    }
    
    public int getMaxLen() {
        return this.maxLen;
    }
    
    public int getStepSize() {
        return this.stepSize;
    }
    
    public int getLeafSize() {
        return this.leafSize;
    }
    
    public int getTreeDepth() {
        return this.treeDepth;
    }
    
    public boolean getDetailedResultsEnabled() {
        int temp = Integer.parseInt(this.cmdLine.getOptionValue(saveIterationResults, "0"));
        return !(temp == 0);
    }
    
    /**
     * Print help to provided OutputStream.
     *
     * @param detailed
     */
    public void printHelp(boolean detailed) {
        String cliSyntax = "java ";
        PrintWriter writer = new PrintWriter(System.out);
        HelpFormatter helpFormatter = new HelpFormatter();
        if (detailed) {
            helpFormatter.printHelp(writer, 120, cliSyntax, "", options, 7, 1, "", true);
        } else {
            helpFormatter.printUsage(writer, 120, cliSyntax, options);
        }
        writer.flush();
    }
    
    protected void parseCandidateLengthFractions() {
        double fLow = Double.parseDouble(cmdLine.getOptionValue(minLenFracSw,
                                                                "0.25"));
        double fHigh = Double.parseDouble(cmdLine.getOptionValue(maxLenFracSw,
                                                                 "0.67"));
        String message;
        if (fLow > fHigh) {
            message = "Maximum length can not be less than Minimum length";
            throw new IllegalArgumentException(message);
        }
        if (fLow <= 0 || fLow > 1) {
            message = "Illegal fraction for 'Minimum candidate length'\n"
                      + "Valid range is (0,1] and min_length < max_length";
            throw new IllegalArgumentException(message);
        }
        
        if (fHigh <= 0 || fHigh > 1) {
            message = "Illegal fraction for 'Maximum candidate length'\n"
                    + "Valid range is (0,1] and min_length < max_length";
            throw new IllegalArgumentException(message);
        }
        
        int minLen = Math.max(1, (int) (this.trainSet.get(0).size() * fLow));
        if (minLen < 2) {
            System.err.println("!!! Warning !!!\nMinimum candidate length "
                               + "below 2 units may cause program crashes "
                               + "when using normalization because of 0 "
                               + "variance of the subsequence at length 1");
        }
        this.minLen = minLen;
        
        int maxLen = Math.min(this.trainSet.get(0).size(),
                              (int) (this.trainSet.get(0).size() * fHigh));
        this.maxLen = maxLen;
    }
    
    public double getMinLenFrac() {
        return Double.parseDouble(cmdLine.getOptionValue(minLenFracSw, "0.25"));
    }

    public double getMaxLenFrac() {
        return Double.parseDouble(cmdLine.getOptionValue(maxLenFracSw, "0.67"));
    }
    
    public void saveResults(DecisionTree dt, TimeSeriesDataset trainSet,
                            TimeSeriesDataset testSet, double trainingTime) {
        ArrayList<DecisionTree> dtList = new ArrayList<>();
        dtList.add(dt);
        this.saveResults(dtList, trainSet, testSet, trainingTime, false);
    }
    
    public void saveResults(ArrayList<DecisionTree> dtList,
                            TimeSeriesDataset trainSet,
                            TimeSeriesDataset testSet, double trainingTime) {
        this.saveResults(dtList, trainSet, testSet, trainingTime, true);
    }
    
    protected void saveResults(ArrayList<DecisionTree> dtList,
                               TimeSeriesDataset trainSet,
                               TimeSeriesDataset testSet, double trainingTime,
                               boolean isEnsemble) {
        long start;
        double trainingAccuracy, testingAccuracy, testingTime;
        
        try {
            Files.createDirectories(Paths.get(this.getResultsPath()));
            File resultsFile = new File(Paths.get(this.getResultsPath(),
                                                  this.resultsFileName)
                                             .toString());
            Formatter finalResults = new Formatter();
            if (!resultsFile.exists()) {
                finalResults.format("%s",
                                 "Dataset,TrainingTime,TestingTime,"
                                       + "TrainingAccuracy,TestingAccuracy,"
                                       + "TrainSize,TestSize,TSLen,MinLen,"
                                       + "MaxLen");
                if (isEnsemble) {
                    finalResults.format(",%s", "EnsembleSize");
                }
                finalResults.format("\n");
            }
            
            if (this.detailedResultsEnabled) {
                File iterResultsFile = new File(Paths.get(this.getResultsPath(),
                                                          this.getDataSetName()
                                                          + " - "
                                                          + this.resultsFileName)
                                                     .toString());
                Formatter iterResults = new Formatter();
                iterResults.format("%s\n",
                                   "Iteration,TrAccModelI,TsAccModelI,TrAcc,"
                                   + "TrMSE,TrBias^2,TrVar,TrIrrErr,TsAcc,"
                                   + "TsMSE,TsBias^2,TsVar,TsIrrErr");
                try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(iterResultsFile.getAbsolutePath()),
                                                                 StandardOpenOption.CREATE,
                                                                 StandardOpenOption.TRUNCATE_EXISTING)) {
                    ArrayList<DecisionTree> subList;
                    for (int i = 0; i < dtList.size(); i++) {
                        subList = new ArrayList<DecisionTree>(dtList.subList(i,
                                                                             i + 1));
                        trainingAccuracy = getSplitAccuracy(subList, trainSet);
                        testingAccuracy = getSplitAccuracy(subList, testSet);
                        iterResults.format("%d,%.3f,%.3f", i + 1,
                                           trainingAccuracy, testingAccuracy);
                        subList = new ArrayList<DecisionTree>(dtList.subList(0,
                                                                             i + 1));
                        trainingAccuracy = getSplitAccuracy(subList, trainSet);
                        testingAccuracy = getSplitAccuracy(subList, testSet);
                        this.calculateBiasVariance(subList, trainSet);
                        iterResults.format(",%.4f,%.4f,%.4f,%.4f,%.4f",
                                           trainingAccuracy,
                                           this.meanSquaredError, this.bias,
                                           this.variance,
                                           this.irreducibleError);
                        this.calculateBiasVariance(subList, testSet);
                        iterResults.format(",%.4f,%.4f,%.4f,%.4f,%.4f\n",
                                           testingAccuracy,
                                           this.meanSquaredError, this.bias,
                                           this.variance,
                                           this.irreducibleError);
                    }
                    bw.write(iterResults.toString());
                }
                iterResults.close();
            }
            
            start = System.currentTimeMillis();
            testingAccuracy = this.getSplitAccuracy(dtList, testSet);
            testingTime = (System.currentTimeMillis() - start) / 1e3;
            trainingAccuracy = this.getSplitAccuracy(dtList, trainSet);
            
            try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(resultsFile.getAbsolutePath()),
                                                             StandardOpenOption.CREATE,
                                                             StandardOpenOption.APPEND)) {
                finalResults.format("%s,%.2f,%.4f,%.4f,%.4f,%d,%d,%d,%f,%f",
                                 this.getDataSetName(),
                                 trainingTime,
                                 testingTime,
                                 trainingAccuracy,
                                 testingAccuracy,
                                 this.trainSet.size(),
                                 this.testSet.size(),
                                 this.trainSet.get(0).size(),
                                 this.getMinLenFrac(),
                                 this.getMaxLenFrac());
                if (isEnsemble) {
                    finalResults.format(",%d", dtList.size());
                }
                finalResults.format("\n");
                bw.write(finalResults.toString());
            }
            System.out.println(finalResults.toString());
            finalResults.close();
        } catch (IOException e) {
            System.err.println("Error saving results: " + e);
        }
    }
    
    private double getSplitAccuracy(ArrayList<DecisionTree> dtList,
                                    TimeSeriesDataset split) {
        int predClass, correct = 0, majorityVote;
        HashMap<Integer, Integer> predClassCount = new HashMap<>();
        for (int ind = 0; ind < split.size(); ind++) {
            predClassCount.clear();
            for (int j = 0; j < dtList.size(); j++) {
                predClass = dtList.get(j).checkInstance(split.get(ind));
                predClassCount.put(predClass,
                                   1 + predClassCount.getOrDefault(predClass,
                                                                   0));
            }
            majorityVote = predClassCount.entrySet().stream()
                                         .max((e1, e2) -> ((e1.getValue() > e2.getValue()) ? 1 : -1))
                                         .get()
                                         .getKey();
            if (majorityVote == split.get(ind).getLabel()) {
                correct++;
            }
        }
        return 100.0 * correct / split.size();
    }
    
    public void calculateBiasVariance(ArrayList<DecisionTree> dtList,
                                      TimeSeriesDataset ds) {
        this.meanSquaredError = 0d;
        this.bias = 0d;
        this.variance = 0d;
        double cummErrorForInstI, errorForInstIWithTreeK;
        int trueLabel, predLabel, bk, ck;
        for (int i = 0; i < ds.size(); i++) {
            trueLabel = ds.get(i).getLabel();
            cummErrorForInstI = 0d;
            for (int k = 0; k < dtList.size(); k++) {
                predLabel = dtList.get(k).checkInstance(ds.get(i));
                errorForInstIWithTreeK = 0d;
                for (Integer key : ds.getAllClasses()) {
                    bk = predLabel == key ? 1 : 0;
                    ck = trueLabel == key ? 1 : 0;
                    errorForInstIWithTreeK += (bk - ck) * (bk - ck);
                }
                cummErrorForInstI += errorForInstIWithTreeK;
            }
            cummErrorForInstI /= dtList.size();
            this.meanSquaredError += cummErrorForInstI;
        }
        this.meanSquaredError /= ds.size();
        
        for (int i = 0; i < ds.size(); i++) {
            this.trueVec.clear();
            this.predVec.clear();
            this.meanVec.clear();
            this.trueVec.put(ds.get(i).getLabel(), 1d);
            for (int k = 0; k < dtList.size(); k++) {
                predLabel = dtList.get(k).checkInstance(ds.get(i));
                this.predVec.put(predLabel,
                                 1 + this.predVec.getOrDefault(predLabel, 0d));
                this.meanVec.put(predLabel,
                                 1 + this.meanVec.getOrDefault(predLabel, 0d));
            }
            double max = this.predVec.entrySet().stream()
                                     .max((e1, e2) -> e1.getValue() > e2.getValue() ? 1 : -1)
                                     .get().getValue();
            for (Integer key : ds.getAllClasses()) {
                this.predVec.put(key, 
                                 Math.floor(predVec.getOrDefault(key, 0d) / max));
                this.variance += Math.pow(this.predVec.getOrDefault(key, 0d)
                                          - this.meanVec.getOrDefault(key, 0d)
                                            / dtList.size(),
                                          2)
                                 / dtList.size();
                this.bias += Math.pow(this.trueVec.getOrDefault(key, 0d)
                                      - this.meanVec.getOrDefault(key, 0d)
                                        / dtList.size(),
                                      2);
            }
        }
        this.variance /= ds.size();
        this.bias /= ds.size();
        this.irreducibleError = this.meanSquaredError - this.bias
                                - this.variance;
    }
}
