package state2vec;

import java.io.File;

import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable;
import org.deeplearning4j.models.embeddings.loader.VectorsConfiguration;
import org.deeplearning4j.models.sequencevectors.SequenceVectors;
import org.deeplearning4j.models.word2vec.wordstore.VocabConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.StateImpl;
import datahandler.word2vec.DataNormaliser;
import datahandler.word2vec.MedicalSequenceIterator;

public class State2Vec {

	//TODO: With next release: make distributed
	//TODO: Safe model to file

	protected static final Logger logger = LoggerFactory.getLogger(State2Vec.class);

	private SequenceVectors<StateImpl> trainedVectors;

	public void trainSequenceVectors(File file) throws Exception {

		logger.info("Started State2Vec");

		/*
	        	Make a cache for the states
		 */
		StateCache<StateImpl> stateCache = new StateCache<StateImpl>();


		/*
	            Make a sequence iterator
		 */
		MedicalSequenceIterator<StateImpl> sequenceIteratorNorm = new MedicalSequenceIterator<>(file, false);
		DataNormaliser normaliser = new DataNormaliser(sequenceIteratorNorm);
		MedicalSequenceIterator<StateImpl> sequenceIterator = new MedicalSequenceIterator<>(file, true);
		sequenceIterator.setNormaliser(normaliser);

		/*
	        Now we should build vocabulary out of sequence iterator.
	        We can skip this phase, and just set AbstractVectors.resetModel(TRUE), and vocabulary will be mastered internally
		 */
		logger.info("Building Vocab");

		VocabConstructor<StateImpl> constructor = new VocabConstructor.Builder<StateImpl>()
				.addSource(sequenceIterator, 10)
				.setTargetVocabCache(stateCache)
				.build();

		constructor.buildJointVocabulary(false, true);

		/*
	            Time to build WeightLookupTable instance for our new model
		 */

		logger.info("Building LookupTable");
		WeightLookupTable<StateImpl> lookupTable = new InMemoryLookupTable.Builder<StateImpl>()
				.lr(0.025)
				.vectorLength(50) // Equals layersize if automatically managed -> duplicated code, removed in next version
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
				.windowSize(5)
				
				

				// batchSize is the number of sequences being processed by 1 thread at once
				// this value actually matters if you have iterations > 1
				.batchSize(250)

				// number of iterations over batch -> same as epochs if no shuffle is used!
				.iterations(1)

				// number of iterations over whole training corpus
				.epochs(1)

				// if set to true, vocabulary will be built from scratches internally
				// otherwise externally provided vocab will be used
				.resetModel(false)


				/*
	                    These two methods define our training goals. At least one goal should be set to TRUE.
				 */
				.trainElementsRepresentation(true)
				.trainSequencesRepresentation(false)

				.learningRate(0.025)


				.build();

		/*
	            Now, after all options are set, we just call fit()
		 */

		logger.info("Fitting State2Vec");
		vectors.fit();

		trainedVectors = vectors;

	}

	public void plotState2Vec() {
		/*
        As soon as fit() exits, model considered built, and we can test it.
        Please note: all similarity context is handled via SequenceElement's labels, so if you're using AbstractVectors to build models for complex
        objects/relations please take care of Labels uniqueness and meaning for yourself.
		 */

		logger.info("Plotting State2Vec");
		trainedVectors.getLookupTable().plotVocab(); // BUGGED AT THE MOMENT IN DL4J
	}
	
	public SequenceVectors<StateImpl> getTrainedModel() {
		return trainedVectors;
	}


}
