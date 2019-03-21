# Random Pairwise Shapelets Forest

This is implementation for Random Pairwise Shapelets Forest, a time series classification scheme.

## Abstract
Shapelet is a discriminative subsequence of time series. An advanced time series classification method is to integrate shapelet with random forest. However, it shows several limitations. First, random shapelet forest requires a large training cost for split threshold searching. Second, a single shapelet provides limited information for only one branch of the decision tree, resulting in insufficient accuracy and interpretability. Third, randomized ensemble causes interpretability declining. For that, this paper presents Random Pairwise Shapelets Forest (RPSF). RPSF combines a pair of shapelets from different classes to construct random forest. It is more efficient due to omit of threshold search, and more effective due to including of additional information from different classes. Moreover, a discriminability metric, Decomposed Mean Decrease Impurity (DMDI), is proposed to identify influential region for every class. Extensive experiments show that RPSF improves the accuracy and training speed of shapelet forest. Case studies demonstrate the interpretability of our method.

## Instruction
step 1. Download the [UCR dataset archive](http://www.timeseriesclassification.com/dataset.php)

step 2. Specify dataset path in edu.bjtu.launcher.LaunchRPSF and run it as main class.

## Acknowledgement
This implementation is based on paper: Ensembles of Randomized Time Series Shapelets Provide Improved Accuracy while Reducing Computational Costs, Raza et al. 2017. Thanks for their strong baseline.
