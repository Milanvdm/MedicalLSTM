package experiments;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.sequencevectors.SequenceVectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.StateImpl;
import util.Constants;
import util.CsvIterator;
import util.HelpFunctions;

public class ClusterSeqTest {
	
	//TODO: write results to file

	protected static final Logger logger = LoggerFactory.getLogger(ClusterSeqTest.class);

	public Map<String, Set<Double>> clusters = new HashMap<String, Set<Double>>();

	public ClusterSeqTest() throws IOException, InterruptedException {
		readClusters();
	}

	private void readClusters() throws IOException, InterruptedException {
		File file = new File("clusters/clusters.csv");

		//skip first 5 lines
		CsvIterator iterator = new CsvIterator(file);

		iterator.next();
		iterator.next();
		iterator.next();
		iterator.next();
		iterator.next();

		while(iterator.hasNext()) {
			String[] line = iterator.next();

			String clusterName = line[0];
			String diag1 = line[3];
			String diag2 = line[5];
			String diag3 = line[9];
			String diag4 = line[13];

			if(clusters.containsKey(clusterName)) {

				Set<Double> diags = clusters.get(clusterName);

				diags.add(stringToDouble(diag1));
				diags.add(stringToDouble(diag2));
				diags.add(stringToDouble(diag3));
				diags.add(stringToDouble(diag4));
			}
			else {

				clusters.put(clusterName, new HashSet<Double>());

				Set<Double> diags = clusters.get(clusterName);

				diags.add(stringToDouble(diag1));
				diags.add(stringToDouble(diag2));
				diags.add(stringToDouble(diag3));
				diags.add(stringToDouble(diag4));
			}
		}

		//logger.info(clusters.toString());
		//String info = "" + clusters.size();
		//logger.info(info);


	}

	public void checkClusters1(SequenceVectors<StateImpl> sequenceVectors, int k) throws Exception {

		WeightLookupTable<StateImpl> table = sequenceVectors.getLookupTable();

		//String info = "" + table.getVocabCache().numWords();
		//logger.info(info);

		for(StateImpl state: table.getVocabCache().vocabWords()) {
			Double icd10 = (Double) state.getCompleteState().get(Constants.CONDITION_COLUMN);

			Set<Double> otherDiags = new HashSet<Double>();

			for (Map.Entry<String, Set<Double>> entry : clusters.entrySet())
			{
				if(entry.getValue().contains(icd10)) {
					if(!entry.getKey().equals("None")) {
						otherDiags.addAll(entry.getValue());
					}

				}
			}
			//otherDiags.remove(icd10);

			int initAmount = otherDiags.size();

			Collection<String> knn = sequenceVectors.wordsNearest(state.getLabel(), k);


			//logger.info(knn.toString());

			int amountInCluster = 0;

			for(String toConvert: knn) {
				double[] label = HelpFunctions.parse(toConvert);

				Double toCheckIcd10 = new Double(label[3]);

				if(otherDiags.contains(toCheckIcd10)) {
					amountInCluster++;
				}

			}

			double clusterCovered = (double) amountInCluster / (double) k;


			if(amountInCluster != 0) {
				//info = "ClusterPercentage: " + clusterCovered;
				//logger.info(info);
			}


		}



	}

	public void checkClusters2(SequenceVectors<StateImpl> sequenceVectors, int k) throws Exception {

		WeightLookupTable<StateImpl> table = sequenceVectors.getLookupTable();

		String info = "" + table.getVocabCache().numWords();
		logger.info(info);

		Map<Double, Set<Double>> icdCluster = new HashMap<Double, Set<Double>>();
		Map<Double, Set<Double>> icdClusterRemoved = new HashMap<Double, Set<Double>>();

		for(StateImpl state: table.getVocabCache().vocabWords()) {
			Double icd10 = (Double) state.getCompleteState().get(Constants.CONDITION_COLUMN);

			Set<Double> otherDiags = new HashSet<Double>();

			for (Map.Entry<String, Set<Double>> entry : clusters.entrySet())
			{
				if(entry.getValue().contains(icd10)) {
					if(!entry.getKey().equals("None")) {
						otherDiags.addAll(entry.getValue());
					}

				}
			}

			if(icdCluster.containsKey(icd10)) {
				icdCluster.get(icd10).addAll(otherDiags);
			}
			else {
				icdCluster.put(icd10, new HashSet<Double>());
				icdCluster.get(icd10).addAll(otherDiags);
			}


			Collection<String> knn = sequenceVectors.wordsNearest(state.getLabel(), k);


			for(String toConvert: knn) {
				double[] label = HelpFunctions.parse(toConvert);

				Double toCheckIcd10 = new Double(label[3]);

				if(otherDiags.contains(toCheckIcd10)) {
					if(icdClusterRemoved.containsKey(icd10)) {
						icdClusterRemoved.get(icd10).add(toCheckIcd10);
					}
					else {
						icdClusterRemoved.put(icd10, new HashSet<Double>());
						icdClusterRemoved.get(icd10).add(toCheckIcd10);
					}
				}

			}


		}

		for (Map.Entry<Double, Set<Double>> entry : icdCluster.entrySet())
		{
			int originalSize = entry.getValue().size();

			if(originalSize == 0) {
				info = "ClusterRatio - Nothing initial: " + 0.0;

				//logger.info(info);
				
				continue;
			}

			if(icdClusterRemoved.containsKey(entry.getKey())) {
				int removedSize = icdClusterRemoved.get(entry.getKey()).size();

				double ratio = (double) removedSize / (double) originalSize;

				info = "ClusterRatio: " + ratio;

				logger.info(info);
			}
			else {
				info = "ClusterRatio - Nothing removed: " + 0.0;

				logger.info(info);
			}

		}





	}

	private Double stringToDouble(String toConvert) {
		StringBuilder sb = new StringBuilder();
		for (char c : toConvert.toCharArray()) {
			sb.append((int) c);
		}

		Double toReturn = new Double(sb.toString());

		return toReturn;
	}



}
