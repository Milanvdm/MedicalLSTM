package deepwalk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.deeplearning4j.graph.api.Edge;
import org.deeplearning4j.graph.api.Vertex;
import org.deeplearning4j.models.sequencevectors.interfaces.SequenceIterator;
import org.deeplearning4j.models.sequencevectors.sequence.Sequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.StateImpl;

public class GraphGenerator {

	private SequenceIterator<StateImpl> iterator;

	private Map<String, StateVertex> vertexMap = new HashMap<String, StateVertex>();
	private List<StateVertex> vertices = new ArrayList<StateVertex>(); //TODO: make hashmaps for fast lookup
	private Map<String, StateEdge> edgeMap = new HashMap<String, StateEdge>();
	private List<StateEdge> edges = new ArrayList<StateEdge>(); //TODO: make hashmaps for fast lookup
	
	protected static final Logger logger = LoggerFactory.getLogger(GraphGenerator.class);

	public GraphGenerator(SequenceIterator<StateImpl> iterator) {
		this.iterator = iterator; 
	}

	public StateGraph createGraph() {
		
		logger.info("Making Vertices and Edges from SequenceIterator");

		createVerticesAndEdges();
		
		logger.info("Making StateGraph");
		
		StateVertex dummy = new StateVertex(vertices.size(), new StateImpl(Arrays.asList(-1.0, -1.0, -1.0, -1.0, -1.0, -1.0).toString()));
		vertices.add(dummy);
		
		StateGraph graph = new StateGraph(vertices.size());


		for(StateVertex vertex: vertices) {
			Vertex<StateImpl> toAdd = new Vertex<StateImpl>(vertex.getIdx(), vertex.getValue());
			graph.addVertex(toAdd);
		}
		
		

		int total = 0;
		for(StateEdge edge: edges) {
			Edge<Integer> toAdd = new Edge<Integer>(edge.getFrom(), edge.getTo(), edge.getWeight(), true);

			graph.addEdge(toAdd);
			
			total = total + edge.getWeight();
		}
		
		graph.removeZeroDegrees();

		return graph;



	}

	public int getHighestId() {
		return vertices.size() - 1;
	}

	private void createVerticesAndEdges() {
		StateVertex previousVertex = null;

		iterator.reset();

		int count = 0;
		while(iterator.hasMoreSequences()) {
			Sequence<StateImpl> sequence = iterator.nextSequence();

			if(sequence.getElements().size() == 1 || sequence.getElements().size() == 0) {
				continue;
			}
			
			int i = 0;
			while(i <  sequence.getElements().size()) {

				StateImpl state = sequence.getElements().get(i);
				String label = state.getLabel();
				
				int dummyId = -1;
				if(vertexMap.containsKey(label)) {
					dummyId = vertexMap.get(label).getIdx();
				}

				StateVertex dummy = new StateVertex(-1, state);
				 

				if(dummyId == -1) {
					dummy.setIdx(vertices.size());
					dummyId = vertices.size();
					vertices.add(dummy); 
					vertexMap.put(label, dummy);

				}
				else {
					dummy.setIdx(dummyId);
				}
				
				if(previousVertex != null) {
					addEdge(previousVertex, vertices.get(dummyId));
				}

				previousVertex = dummy;

				i++;
			}

			previousVertex = null;

			
			if(count % 100000 == 0) {
				logger.info("Amount of states done: " + count);
			}

			count++;
		}

	}

	private void addEdge(StateVertex previousVertex, StateVertex currentVertex) {
		int from = previousVertex.getIdx();
		
		int to = currentVertex.getIdx();
		
		String key = from + "-" + to;

		StateEdge dummy = new StateEdge(from, to, 1);

		if(edgeMap.containsKey(key)) {
			edgeMap.get(key).increaseWeight();
		}
		else {
			edges.add(dummy);
			edgeMap.put(key, dummy);
		}

	}


}
