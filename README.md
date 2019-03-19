# Random Pairwise Shapelets Forest

This is implementation for Random Pairwise Shapelets Forest, a time series classification scheme.

## Abstract
Shapelet is a discriminative subsequence of time series. An advanced time series classification method is to integrate shapelet with random forest. However, it shows several limitations. First, random shapelet forest requires a large training cost for split threshold searching. Second, a single shapelet provides limited information for only one branch of the decision tree, resulting in insufficient accuracy and interpretability. Third, randomized ensemble causes interpretability declining. For that, this paper presents Random Pairwise Shapelets Forest (RPSF). RPSF combines a pair of shapelets from different classes to construct random forest. It is more efficient due to omit of threshold search, and more effective due to including of additional information from different classes. Moreover, a discriminability metric, Decomposed Mean Decrease Impurity (DMDI), is proposed to identify influential region for every class. Extensive experiments show that RPSF improves the accuracy and training speed of shapelet forest. Case studies demonstrate the interpretability of our method.

## Instruction
step 1. Download the [UCR dataset archive](http://www.timeseriesclassification.com/dataset.php)

step 2. Specify dataset path in edu.bjtu.launcher.LaunchRPSF and run it as main class.

## Publication
```
@inproceedings{DBLP:conf/pakdd/ShiWYL18,
  author    = {Mohan Shi and
               Zhihai Wang and
               Jidong Yuan and
               Haiyang Liu},
  title     = {Random Pairwise Shapelets Forest},
  booktitle = {Advances in Knowledge Discovery and Data Mining - 22nd Pacific-Asia
               Conference, {PAKDD} 2018, Melbourne, VIC, Australia, June 3-6, 2018,
               Proceedings, Part {I}},
  pages     = {68--80},
  year      = {2018},
  crossref  = {DBLP:conf/pakdd/2018-1},
  url       = {https://doi.org/10.1007/978-3-319-93034-3\_6},
  doi       = {10.1007/978-3-319-93034-3\_6},
  timestamp = {Tue, 19 Jun 2018 16:43:17 +0200},
  biburl    = {https://dblp.org/rec/bib/conf/pakdd/ShiWYL18},
  bibsource = {dblp computer science bibliography, https://dblp.org}
}
```
Please cite our paper if this code is useful to you.

## Acknowledgement
This implementation is based on paper: Ensembles of Randomized Time Series Shapelets Provide Improved Accuracy while Reducing Computational Costs, Raza et al. 2017. Thanks for their strong baseline.
