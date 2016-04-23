package experiments;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deeplearning4j.graph.api.IGraph;
import org.deeplearning4j.graph.api.Vertex;
import org.deeplearning4j.graph.models.deepwalk.DeepWalk;
import org.deeplearning4j.graph.models.embeddings.GraphVectorLookupTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.CsvIterator;

public class ClusterGraphTest {
	
	//TODO: write results to file

	protected static final Logger logger = LoggerFactory.getLogger(ClusterSeqTest.class);

	public Map<String, Set<Double>> clusters = new HashMap<String, Set<Double>>();

	public ClusterGraphTest() throws IOException, InterruptedException {
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

	public void checkClusters1(DeepWalk<List<Double>, Integer> deepwalk, int highestId, int k) throws Exception {

		IGraph<List<Double>, Integer> graph = deepwalk.getGraph();
		
		GraphVectorLookupTable table = deepwalk.lookupTable();
		
		int id = 0;
		while(id <= highestId) {
			try {
				table.getVector(id);
				
				Vertex<List<Double>> vertex = graph.getVertex(id);
				Double icd10 = vertex.getValue().get(3);
				
				
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

				int[] knn = deepwalk.verticesNearest(id, k);


				//logger.info(knn.toString());

				int amountInCluster = 0;

				for(int knnId: knn) {
					Vertex<List<Double>> knnVertex = graph.getVertex(knnId);

					Double toCheckIcd10 = knnVertex.getValue().get(3);

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
			catch(Exception e) {
				
			}
			
			id++;
		}	


	}
	
	public void checkClusters2(DeepWalk<List<Double>, Integer> deepwalk, int highestId, int k) throws Exception {

		IGraph<List<Double>, Integer> graph = deepwalk.getGraph();
		
		GraphVectorLookupTable table = deepwalk.lookupTable();
		
		Map<Double, Set<Double>> icdCluster = new HashMap<Double, Set<Double>>();
		Map<Double, Set<Double>> icdClusterRemoved = new HashMap<Double, Set<Double>>();
		
		int id = 0;
		while(id <= highestId) {
			try {
				table.getVector(id);
				
				Vertex<List<Double>> vertex = graph.getVertex(id);
				Double icd10 = vertex.getValue().get(3);
				
				
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

				int initAmount = otherDiags.size();

				int[] knn = deepwalk.verticesNearest(id, k);


				//logger.info(knn.toString());

				int amountInCluster = 0;

				for(int knnId: knn) {
					Vertex<List<Double>> knnVertex = graph.getVertex(knnId);

					Double toCheckIcd10 = knnVertex.getValue().get(3);

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

				for (Map.Entry<Double, Set<Double>> entry : icdCluster.entrySet())
				{
					int originalSize = entry.getValue().size();

					if(originalSize == 0) {
						String info = "ClusterRatio - Nothing initial: " + 0.0;

						//logger.info(info);
						
						continue;
					}

					if(icdClusterRemoved.containsKey(entry.getKey())) {
						int removedSize = icdClusterRemoved.get(entry.getKey()).size();

						double ratio = (double) removedSize / (double) originalSize;

						String info = "ClusterRatio: " + ratio;

						logger.info(info);
					}
					else {
						String info = "ClusterRatio - Nothing removed: " + 0.0;

						logger.info(info);
					}
				}

				
			}
			catch(Exception e) {
				
			}
			
			id++;
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
