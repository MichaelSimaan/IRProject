/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required byOCP applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import javafx.util.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

public class App {
    public static final String BODY_FIELD = "body";
    public static final String FILTERED_BODY_FIELD = "filteredbody";
    private static int WINDOW = 15;
    public static final FieldType TERM_VECTOR_TYPE;
    static {
        TERM_VECTOR_TYPE = new FieldType(TextField.TYPE_STORED);
        TERM_VECTOR_TYPE.setStoreTermVectors(true);
        TERM_VECTOR_TYPE.setStoreTermVectorPositions(true);
        TERM_VECTOR_TYPE.setStoreTermVectorOffsets(true);
        TERM_VECTOR_TYPE.freeze();
    }
    public static final String corpusFile1 = "src/Corpus1.txt"; //Original answers corpus
    public static final String corpusFile2 = "src/Corpus2.txt"; //Original answers corpus
    public static final String corpusFile3 = "src/Corpus3.txt"; //Original answers corpus
    public static final String corpusFile4 = "src/Corpus4.txt"; //Original answers corpus
    public static final String corpusFile5 = "src/Corpus5.txt"; //Original answers corpus
    private static final String filteredCorpusFile1 = "src/FilteredCorpus1.txt"; //Filtered from the original corpus, removed stop words and frequent words.
    private static final String filteredCorpusFile2 = "src/FilteredCorpus2.txt"; //Filtered from the original corpus, removed stop words and frequent words.
    public static void main(String[] args) throws Exception {

        //read the corpus file and add all the lines to a list.
        List<String> documents = new ArrayList<String>();
        try (BufferedReader br = new BufferedReader(new FileReader(corpusFile1))) {
        	String line;
            while ((line = br.readLine()) != null) {
            	documents.add(line);
            }
        }
        try (BufferedReader br = new BufferedReader(new FileReader(corpusFile2))) {
        	String line;
            while ((line = br.readLine()) != null) {
            	documents.add(line);
            }
        }
        try (BufferedReader br = new BufferedReader(new FileReader(corpusFile3))) {
        	String line;
            while ((line = br.readLine()) != null) {
            	documents.add(line);
            }
        }
        try (BufferedReader br = new BufferedReader(new FileReader(corpusFile4))) {
        	String line;
            while ((line = br.readLine()) != null) {
            	documents.add(line);
            }
        }
        try (BufferedReader br = new BufferedReader(new FileReader(corpusFile5))) {
        	String line;
            while ((line = br.readLine()) != null) {
            	documents.add(line);
            }
        }
        QueryUtils.allDocuments = documents; //storing the list in QueryUtils object.

        //read the Filtered corpus file and add all the lines to a list.
        List<String> filteredDocuments = new ArrayList<String>();
        try (BufferedReader br = new BufferedReader(new FileReader(filteredCorpusFile1))) {
        	String line;
            while ((line = br.readLine()) != null) {
            	filteredDocuments.add(line);
            }
        }
        try (BufferedReader br = new BufferedReader(new FileReader(filteredCorpusFile2))) {
        	String line;
            while ((line = br.readLine()) != null) {
            	filteredDocuments.add(line);
            }
        }
        if (documents.size() != filteredDocuments.size()) {
        	System.out.println("Corpus and FilteredCorpus are not the same size.");
        	System.exit(0);
        }
    	List<String> questions = GetQuestionsList(); //reads the query questions to run and get results
    	int k = 0;
    	for (String question : questions) {
    		System.out.println("\n\nCurrent question is: " + question);
    		QueryUtils qu = new QueryUtils(question);
        	QueryUtils.tagQuery(qu.query);
        	String expandedQuery = qu.filterWords(qu.query); //filter the stop words from the query
        	expandedQuery = qu.spellCorrector(expandedQuery); //spell corrects the query
        	expandedQuery = qu.filterWords(expandedQuery); //refilter after the spell correct
        	expandedQuery = qu.dealWithAbbreviation(expandedQuery); //example U.S -> US
        	WINDOW = WINDOW * Arrays.asList(expandedQuery.split("\\s+")).size();
        	String nonExpandedQuery = expandedQuery;
        	expandedQuery = qu.checkVerbalPhrases(expandedQuery); //Example: calm down -> relax, Get up -> stand
        	String afterSubwords = expandedQuery;
        	expandedQuery = qu.filterWords(expandedQuery);
        	int queryLength = expandedQuery.split("\\s+").length;
        	if (queryLength < 3) {
            	//expandedQuery = qu.checkForSubwords(expandedQuery);
            	expandedQuery = qu.filterWords(expandedQuery);
        	}
        	System.out.println("After filtering stopwords: " + expandedQuery);
        	if (nonExpandedQuery.equals("")) {
        		nonExpandedQuery = qu.originalQuery;
        	}
            if (expandedQuery.equals("")) {
            	expandedQuery = qu.originalQuery;
            }
            qu.nonExpandedQuery = nonExpandedQuery;
            expandedQuery = expandQueryUsingPassages(expandedQuery, qu, filteredDocuments, queryLength); //Query expand
            expandedQuery = QueryUtils.mergeIntoOneQuery(expandedQuery, afterSubwords);
            System.out.println("Query after expansion: " + expandedQuery);
            float k1 = 0f;
            float b = 0f;

            try (Directory dir = newDirectory();
                    Analyzer analyzer = newAnalyzer())
            {
                IndexWriterConfig conf = new IndexWriterConfig(analyzer);
                conf.setSimilarity(new BM25Similarity(k1, b));
                try (IndexWriter writer = new IndexWriter(dir, conf))
                {
                    int i=0;
                    for (String document : documents) {
                    	final Document doc = new Document();
                    	doc.add(new StringField("id", "doc" + i, Store.YES));
                    	doc.add(new Field(BODY_FIELD, document, TERM_VECTOR_TYPE));
                    	doc.add(new Field(FILTERED_BODY_FIELD, filteredDocuments.get(i), TERM_VECTOR_TYPE));
                    	writer.addDocument(doc);
                    	i++;
                    }
                }
                // Search
                try (DirectoryReader corpusReader = DirectoryReader.open(dir))
                {
                    final IndexSearcher searcher = new IndexSearcher(corpusReader);
                    final QueryParser qp = new QueryParser(FILTERED_BODY_FIELD, analyzer);
                    expandedQuery = QueryUtils.boostQuery(nonExpandedQuery, expandedQuery, qu.originalQuery, 4, 8, 10, queryLength);
                    System.out.println("Boosted Query: " + expandedQuery);
                    Query query = qp.parse(expandedQuery);
                    searcher.setSimilarity(new BM25Similarity(k1, b));
                    final TopDocs td = searcher.search(query, 50);

                    List<Pair<Double,Document>> documentScores = new ArrayList<>();
                    for (final ScoreDoc sd : td.scoreDocs) {
                        final Document doc = searcher.doc(sd.doc);
                        double score = sd.score;
                        documentScores.add(new Pair<>(score,doc));
                    }

                    //Rerank results
                    List<Pair<String, Pair<String, Double>>> retrievedDocs = FinalizeRun(query, td, searcher);

                    Collections.sort(documentScores, new Comparator<Pair<Double, Document>>() {
                        @Override
                        public int compare(Pair<Double, Document> o1, Pair<Double, Document> o2) {
                            return (o1.getKey() < o2.getKey()) ? 1 : -1;
                        }
                    });
                    JsonBuilder builder = new JsonBuilder(qu.id, retrievedDocs, searcher, k);
                    builder.createJson();
                    k++;
                }
            }
    	}
    }

    /**
     *  Expands query using each word in the query adds up to 4 words
     * @param query Query to expand
     * @param corpusReader
     * @param qp
     * @return returns
     * @throws IOException
     * @throws ParseException
     */
    private static String expandQueryUsingTerms(String query, DirectoryReader corpusReader, QueryParser qp) throws IOException, ParseException {
        float k1 = 0.6f;
        float b = 0f;
        String finalExpandedQuery = query;
        List<String> terms = Arrays.asList(query.split("\\s+")); //spliting terms
        List<String> expansionTerms = new ArrayList<String>();
        List<String> allTerms = new ArrayList<String>();
        for (String term : terms) {
        	allTerms.add(term);
        	expansionTerms.add(term);
        }
        allTerms.add(query);

        //tagging terms
        for (String term : allTerms) {
        	if (QueryUtils.tagsMap.containsKey(term) && term.length() > 2) {
        		if ((QueryUtils.tagsMap.get(term).equals("V") && term.length() < 3) || QueryUtils.tagsMap.get(term).equals("O")) {
            		finalExpandedQuery = finalExpandedQuery + " " + term;
            		finalExpandedQuery = finalExpandedQuery.trim();
        			continue;
        		}
        	}
        	else if (term.split("\\s+").length >= 2) {
        		
        	}
        	else {
        		finalExpandedQuery = finalExpandedQuery + " " + term;
        		finalExpandedQuery = finalExpandedQuery.trim();
        		continue;
        	}
        	String type = "S";
        	if (QueryUtils.tagsMap.containsKey(term)) {
        		type = QueryUtils.tagsMap.get(term);
        	}
        	String boostedQuery = QueryUtils.boostQuery(term, term, 1, 5); //boosting original query
            System.out.println("Checking the term : " + term);
            Query currentQuery = qp.parse(boostedQuery);
            final IndexSearcher searcher = new IndexSearcher(corpusReader);                
            searcher.setSimilarity(new BM25Similarity(k1, b));
            final TopDocs td = searcher.search(currentQuery, 50);
            final PassageSearcher passageSearcher =
                    new TermVectorsPassageSearcher(searcher, FILTERED_BODY_FIELD, 0.1, PassageScorer.DOC_SCORE_AND_QUERY_TF);

            final List<Passage> retrievedPassages = passageSearcher.search(currentQuery, td, 30, 50); //Re-scoring documents
            List<Pair<String, Pair<String, Double>>> top10Docs = Utils.GetPassagesList(retrievedPassages);
            Collections.sort(top10Docs, new Comparator<Pair<String, Pair<String, Double>>>() { //sorting results by scores
                @Override
                public int compare(Pair<String, Pair<String, Double>> o1, Pair<String, Pair<String, Double>> o2) {
                    return (o1.getValue().getValue() < o2.getValue().getValue()) ? 1 : -1;
                }
            });
            List<String> topPassages = new ArrayList<String>();
            for (Pair<String, Pair<String, Double>> pair : top10Docs) {
            	if (pair.getValue().getKey() != null) {
                	topPassages.add(pair.getValue().getKey());
            	}
            }
            String expandedWords = QueryUtils.expandWithTerms(topPassages, term, false, type, expansionTerms, terms);
            for (String word : expandedWords.split("\\s+")) {
            	if (!expansionTerms.contains(word)) {
                	finalExpandedQuery = finalExpandedQuery + " " + word;
            	}
            	expansionTerms.add(word);
            }
            finalExpandedQuery = finalExpandedQuery.trim();
            System.out.println("After : " + finalExpandedQuery);
        }
       	return finalExpandedQuery;
    }

    /**
     * Expand using passages file
     * @param givenQuery Query
     * @param receivedQuery
     * @param filteredDocs
     * @param queryLength
     * @return
     * @throws IOException
     * @throws ParseException
     */
    private static String expandQueryUsingPassages(String givenQuery, QueryUtils receivedQuery, List<String> filteredDocs, int queryLength) throws IOException, ParseException {
        float k1 = 0.6f;
        float b = 0f;
    	try (Directory dir = newDirectory();
                Analyzer analyzer = newAnalyzer()) 
        {
            IndexWriterConfig conf = new IndexWriterConfig(analyzer);
            conf.setSimilarity(new BM25Similarity(k1, b));

            try (IndexWriter writer = new IndexWriter(dir, conf))
            {
            	int i = 0;
                for (String line : filteredDocs) {
                	final Document doc = new Document();
                    doc.add(new StringField("id", "doc" + i, Store.YES));
                    doc.add(new Field(FILTERED_BODY_FIELD, line, TERM_VECTOR_TYPE));
                    writer.addDocument(doc);
                    i++;
                }
                writer.close();
            }
            // Search
            try (DirectoryReader corpusReader = DirectoryReader.open(dir))
            {
                final QueryParser qp = new QueryParser(FILTERED_BODY_FIELD, analyzer);
                if (receivedQuery.query.equals("")) {
                	receivedQuery.query = receivedQuery.originalQuery;
                }
                String queryString = QueryParser.escape(givenQuery);
        		String expandedQuery = queryString;
        		expandedQuery = QueryUtils.stemQuery(expandedQuery);
        		if (expandedQuery.split("\\s+").length < 3) {
        			return expandQueryUsingTerms(expandedQuery, corpusReader, qp);
        		}
                String boostedQuery = QueryUtils.boostQuery(expandedQuery, expandedQuery, receivedQuery.originalQuery, 3, 7, 10, queryLength);
                System.out.println(boostedQuery);
                Query query = qp.parse(boostedQuery);
                final IndexSearcher searcher = new IndexSearcher(corpusReader);                
                searcher.setSimilarity(new BM25Similarity(k1, b));
                final TopDocs td = searcher.search(query, 50);
                final PassageSearcher passageSearcher =
                        new TermVectorsPassageSearcher(searcher, FILTERED_BODY_FIELD, 0.1, PassageScorer.DOC_SCORE_AND_QUERY_TF);

                final List<Passage> retrievedPassages = passageSearcher.search(query, td, 25, 60);
                List<Pair<String, Pair<String, Double>>> top10Docs = Utils.GetPassagesList(retrievedPassages);
                Collections.sort(top10Docs, new Comparator<Pair<String, Pair<String, Double>>>() {
                    @Override
                    public int compare(Pair<String, Pair<String, Double>> o1, Pair<String, Pair<String, Double>> o2) {
                        return (o1.getValue().getValue() < o2.getValue().getValue()) ? 1 : -1;
                    }
                });
                List<String> topPassages = new ArrayList<String>();
                for (Pair<String, Pair<String, Double>> pair : top10Docs) {
                	if (pair.getValue().getKey() != null) {
                    	topPassages.add(pair.getValue().getKey());
                	}
                }
                String finalExpandedQuery = QueryUtils.expandsWithPassages(topPassages, expandedQuery, true, queryLength);
               	return finalExpandedQuery;
            }
        }
    }

    public static Directory newDirectory() {
        return new RAMDirectory();
    }

    public static Analyzer newAnalyzer() {
        return new EnglishAnalyzer();
    }

    
    static public List<Pair<String, Pair<String, Double>>> FinalizeRun(Query query, TopDocs td, IndexSearcher searcher) throws IOException {
    	final PassageSearcher passageSearcher =
                new TermVectorsPassageSearcher(searcher, FILTERED_BODY_FIELD, 0.1, PassageScorer.DOC_SCORE_AND_QUERY_TF);

        final List<Passage> retrievedPassages = passageSearcher.search(query, td, 25, 50);
//        List<Pair<String, Pair<String, Double>>> topPassages = Utils.GetPassagesList(retrievedPassages);
//    	System.out.println("\ntop documents:");
//    	for (Pair<String, Pair<String, Double>> pair : topPassages) {
//    		final String docID = pair.getKey();
//    		final String text = pair.getValue().getKey();
//    		final double score = pair.getValue().getValue();
//    		System.out.println("{ " + score +", " + docID + ", "  + text + " }");
//    	}

        System.out.println("\nRetrieved documents");
        List<Pair<String, Pair<String, Double>>> retrievedDocs = Utils.CompressPassagesToDocs(retrievedPassages, td, searcher, false, false);
        for (Pair<String, Pair<String, Double>> pair : retrievedDocs) {
        	final String docID = pair.getKey();
        	final String text = pair.getValue().getKey();
        	final double score = pair.getValue().getValue();
        	System.out.println("{ " + score +", " + docID + ", " + text + " }");
        }
        return retrievedDocs;
    }
    
    static public List<String> GetQuestionsList() throws IOException {
    	ArrayList<String> questions = new ArrayList<String>();
    	String pathToQuestions = "./src/Questions.txt";
        try (BufferedReader br = new BufferedReader(new FileReader(pathToQuestions))) {
        	String line;
            while ((line = br.readLine()) != null) {
            	questions.add(line);
            }
        }
		return questions;
    }
    
}