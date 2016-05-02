package deepwalk;

import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable;
import org.deeplearning4j.models.embeddings.loader.VectorsConfiguration;
import org.deeplearning4j.models.sequencevectors.SequenceVectors;
import org.deeplearning4j.models.word2vec.wordstore.VocabConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.StateImpl;
import deepwalk.graph.enums.WalkDirection;
import deepwalk.graph.walkers.impl.WeightedWalker;
import deepwalk.transformers.impl.GraphTransformer;
import state2vec.StateCache;

public class StateDeepWalk {
	
	protected static final Logger logger = LoggerFactory.getLogger(StateDeepWalk.class);

	private SequenceVectors<StateImpl> trainedVectors;



	public SequenceVectors<StateImpl> getTrainedModel() {
		return trainedVectors;
	}

	public void trainDeepWalk(StateGraph graph, int windowSize, double lr, int vectorLength, int walkLength, int batchSize, int epochs, int minWordFreq) {
		
		logger.info("Making RandomWalker");
		
		WeightedWalker<StateImpl> weightedWalker = new WeightedWalker.Builder<StateImpl>(graph)
				.setRestartProbability(0.05)
				.setWalkLength(walkLength)
				.setWalkDirection(WalkDirection.RANDOM)
				.build();
		
		logger.info("Transforming graph");
		
		GraphTransformer<StateImpl> transformer = new GraphTransformer.Builder<StateImpl>(graph)
				.setGraphWalker(weightedWalker)
				.build();
		
		GraphSequenceIterator sequenceIterator = new GraphSequenceIterator(transformer);
		/*
    	Make a cache for the states
		 */
		StateCache<StateImpl> stateCache = new StateCache<StateImpl>();

		/*
	        Now we should build vocabulary out of sequence iterator.
	        We can skip this phase, and just set AbstractVectors.resetModel(TRUE), and vocabulary will be mastered internally
		 */
		logger.info("Building Vocab");

		VocabConstructor<StateImpl> constructor = new VocabConstructor.Builder<StateImpl>()
				.addSource(sequenceIterator, minWordFreq)
				.setTargetVocabCache(stateCache)
				.build();

		constructor.buildJointVocabulary(false, true);

		/*
	            Time to build WeightLookupTable instance for our new model
		 */

		logger.info("Building LookupTable");
		WeightLookupTable<StateImpl> lookupTable = new InMemoryLookupTable.Builder<StateImpl>()
				.vectorLength(vectorLength) // Equals layersize if automatically managed -> duplicated code, removed in next version
				.useAdaGrad(true) // In short: prevents overfitting
				.cache(stateCache)
				.build();

		/*
	             reset model is viable only if you're setting AbstractVectors.resetModel() to false
	             if set to True - it will be called internally
		 */
		lookupTable.resetWeights(true);

		/*
	            Now we can build AbstractVectors model, that suits our needs
		 */
		logger.info("Building SequenceVectors");
		SequenceVectors<StateImpl> vectors = new SequenceVectors.Builder<StateImpl>(new VectorsConfiguration())
				// minimum number of occurencies for each element in training corpus. All elements below this value will be ignored
				// Please note: this value has effect only if resetModel() set to TRUE, for internal model building. Otherwise it'll be ignored, and actual vocabulary content will be used

				// WeightLookupTable
				.lookupTable(lookupTable)

				// abstract iterator that covers training corpus
				.iterate(sequenceIterator)

				// vocabulary built prior to modelling
				.vocabCache(stateCache)

				// default value is 5 (for SkipGram)
				.windowSize(windowSize)



				// batchSize is the number of sequences being processed by 1 thread at once
				// this value actually matters if you have iterations > 1
				.batchSize(batchSize)

				// number of iterations over batch -> same as epochs if no shuffle is used!
				.iterations(epochs)

				// number of iterations over whole training corpus
				.epochs(epochs)

				// if set to true, vocabulary will be built from scratches internally
				// otherwise externally provided vocab will be used
				.resetModel(false)


				/*
	                    These two methods define our training goals. At least one goal should be set to TRUE.
				 */
				.trainElementsRepresentation(true)
				.trainSequencesRepresentation(false)

				.learningRate(lr)


				.build();

		/*
	            Now, after all options are set, we just call fit()
		 */

		logger.info("Fitting State2Vec");
		vectors.fit();

		trainedVectors = vectors;
		
		
	}

}
