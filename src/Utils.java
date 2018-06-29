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


import javafx.util.Pair;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.document.Document;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public interface Utils {

    static String format(String pattern, Object... args) {
        return String.format(Locale.ENGLISH, pattern, args);
    }
    static double n_similarity(String query, String answer) throws IOException {
        double res = 1;
    	try {
			String dir = System.getProperty("user.dir");
			String filePath = dir + "\\python\\n_similarity.py";
	        final String model = dir + "\\python\\dataset-py";
			ProcessBuilder pb = new ProcessBuilder("python", filePath, "-model", model, "-q", query, "-p", answer);
			Process process = pb.start();
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String input;
			List<String> temp = new ArrayList<String>();
			while ((input = stdInput.readLine()) != null) {
				temp.add(input);
			}
			input = temp.get(0);
            res = Double.parseDouble(input);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
        return res;
    }

    /**
     * expand query with most similar terms using word2vec
     * @param query query to expand
     * @param queryLength
     * @return expanded query
     * @throws IOException
     */
    static String most_similar(String query,int queryLength) throws IOException {
        int count = queryLength;
        if (count >= 7) {
        	count = 1;
        }
        else if (count > 5) {
        	count = 2;
        }
        else if (count >= 3) {
        	count = 3;
        }

        final String dir = System.getProperty("user.dir");
        final String filePath = dir + "\\python\\ExpandUsingWord2vec.py";
        final String model = dir + "\\python\\dataset-py";
        Process p = Runtime.getRuntime().exec("python \"" + filePath + "\" \""+ model +"\" \"" + query + "\" " + count);
        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(p.getInputStream()));
        String s, res = "";
        while ((s = stdInput.readLine()) != null) {
           res = s;
        }
        return res;
    }

    /**
     * Reranks using max similarity between query and document
     * @param td results documents
     * @param corpusReader indexreader
     * @param avgsl average sentence length
     * @param query
     * @param b
     * @param k1
     * @return
     * @throws IOException
     */
    static ArrayList<Pair<Integer,Double>> ReRank_max_sem(TopDocs td, DirectoryReader corpusReader, double avgsl, String query,float b,float k1) throws IOException {
        int numDocs = corpusReader.numDocs();
        ClassicSimilarity similarity = new ClassicSimilarity();
        ArrayList<Pair<Integer, Double>> result = new ArrayList<>();
        int length = query.split("\\s+").length;
        for (final ScoreDoc sd : td.scoreDocs) {
            ArrayList<TermStats> termStats = new ArrayList<>();
            double score = 0;
            Terms terms = corpusReader.getTermVector(sd.doc, App.FILTERED_BODY_FIELD);
            TermsEnum termsEnum = terms.iterator();
            StringBuilder stringBuilder = new StringBuilder();
            int i = 0;
            while (termsEnum.next() != null) {
                double idf = similarity.idf(termsEnum.docFreq(), numDocs); // idf(w)
                termStats.add(i, new TermStats(idf, termsEnum.term().utf8ToString()));
                i++;
                stringBuilder.append(termsEnum.term().utf8ToString() + " ");
            }
            double[] Semresults;
            Semresults = max_sem(stringBuilder.toString(), query);

            /*if(stringBuilder.toString().length() > query.length()) {
                Semresults = max_sem(stringBuilder.toString(), query);
            }else{
                Semresults = max_sem(query,stringBuilder.toString());
            }*/
            for (int j = 0; j < termStats.size(); j++) {
                double res = termStats.get(j).getIdf() * ((Semresults[j] * (k1 + 1)) / (Semresults[j] + k1 * (1 - b + b * (length / avgsl))));
                if (Double.isNaN(res))
                    res = 0;
                score += res;
            }
            result.add(new Pair<>(sd.doc, score));

        }
        return result;
    }

    static ArrayList<Pair<Integer,Double>> ReRank_max_sem(TopDocs td, DirectoryReader corpusReader, String query) throws IOException {
        int numDocs = corpusReader.numDocs();
        ClassicSimilarity similarity = new ClassicSimilarity();
        ArrayList<Pair<Integer, Double>> result = new ArrayList<>();
        for (final ScoreDoc sd : td.scoreDocs) {
            ArrayList<TermStats> termStats = new ArrayList<>();
            double score = 0;
            Terms terms = corpusReader.getTermVector(sd.doc, App.FILTERED_BODY_FIELD);
            TermsEnum termsEnum = terms.iterator();
            StringBuilder stringBuilder = new StringBuilder();
            int i = 0;
            while (termsEnum.next() != null) {
                double idf = similarity.idf(termsEnum.docFreq(), numDocs); // idf(w)
                termStats.add(i, new TermStats(idf, termsEnum.term().utf8ToString()));
                i++;
                stringBuilder.append(termsEnum.term().utf8ToString() + " ");
            }
            double[] Semresults;
            Semresults = max_sem(stringBuilder.toString(), query);

            /*if(stringBuilder.toString().length() > query.length()) {
                Semresults = max_sem(stringBuilder.toString(), query);
            }else{
                Semresults = max_sem(query,stringBuilder.toString());
            }*/
            for (int j = 0; j < termStats.size(); j++) {
                double res = termStats.get(j).getIdf() * (Semresults[j]);
                if (Double.isNaN(res))
                    res = 0;
                score += res;
            }
            result.add(new Pair<>(sd.doc, score));

        }
        return result;
    }

    /**
     * executes max_sem file
     * @param sl long sentence
     * @param ss short sentence
     * @return returns max double array, array[i] = max similarity between sl[i] and all ss
     * @throws IOException
     */
    static double[] max_sem(String sl,String ss) throws IOException {
        double[] res = new double[ sl.split("\\s+").length];
        final String dir = System.getProperty("user.dir");
        final String filePath = dir + "\\python\\max_sem.py";
        final String model = dir + "\\python\\dataset-py";
        Process p = Runtime.getRuntime().exec("python \"" + filePath + "\" -model \""+ model +"\" -sl \"" + sl + "\" -ss \"" + ss + "\"");
        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(p.getInputStream()));
        String s;
        int i=0;
        while ((s = stdInput.readLine()) != null) {
            res[i++] = Double.parseDouble(s);
            if(i >= res.length)
                break;
        }
        return res;
    }
    
    public static List<Pair<String, Pair<String, Double>>> GetPassagesList(List<Passage> passages) throws IOException {
    	List<Pair<String, Pair<String, Double>>> rerankedPassages = new ArrayList<Pair<String, Pair<String, Double>>>();
    	for (Passage passage :  passages) {
        	rerankedPassages.add(new Pair<String, Pair<String, Double>>(passage.getDocID(), new Pair<String, Double>(passage.getText(), passage.getScore())));
    	}
    	return rerankedPassages;
    }

    public static List<Pair<String, Pair<String, Double>>> CompressPassagesToDocs(List<Passage> passages, TopDocs topDocs, IndexSearcher searcher, Boolean filtered, Boolean sameDocWeight) throws IOException {
    	List<Pair<String, Pair<String, Double>>> rerankedPassages = new ArrayList<Pair<String, Pair<String, Double>>>();
    	for (Passage passage :  passages) {
    		for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                String docId = doc.get("id");
                String body = App.BODY_FIELD;
                if (filtered) {
                	body = App.FILTERED_BODY_FIELD;
                }
                String text = doc.get(body);
                if (docId.equals(passage.getDocID())) {
                	rerankedPassages.add(new Pair<String, Pair<String, Double>>(docId, new Pair<String, Double>(text, scoreDoc.score * passage.getScore())));
                }
    		}
    	}
    	if (sameDocWeight) {
    		return NormalizeScoresWithWeights(rerankedPassages);
    	}
    	return NormalizeScores(rerankedPassages);
    }
    
    public static List<Pair<String, Pair<String, Double>>> NormalizeScoresWithWeights(List<Pair<String, Pair<String, Double>>> passagesData){
        Collections.sort(passagesData, new Comparator<Pair<String, Pair<String, Double>>>() {
            @Override
            public int compare(Pair<String, Pair<String, Double>> o1, Pair<String, Pair<String, Double>> o2) {
                return (o1.getValue().getValue() < o2.getValue().getValue()) ? 1 : -1;
            }
        });
        List<Pair<String, Pair<String, Double>>> rerankedPassages = new ArrayList<Pair<String, Pair<String, Double>>>();
        double maximum = passagesData.get(0).getValue().getValue();
        for (Pair<String, Pair<String, Double>> pair : passagesData) {
        	final String docID = pair.getKey();
        	final String text = pair.getValue().getKey();
        	final double score = 100 * pair.getValue().getValue() / maximum;
        	rerankedPassages.add(new Pair<String, Pair<String, Double>>(docID, new Pair<String, Double>(text, score)));
        }
        
        Collections.sort(rerankedPassages, new Comparator<Pair<String, Pair<String, Double>>>() {
            @Override
            public int compare(Pair<String, Pair<String, Double>> o1, Pair<String, Pair<String, Double>> o2) {
                return (o1.getValue().getValue() < o2.getValue().getValue()) ? 1 : -1;
            }
        });
        List<Pair<String, Pair<String, Double>>> filteredPassages = new ArrayList<Pair<String, Pair<String, Double>>>();
        List<String> added = new ArrayList<String>();
        for (Pair<String, Pair<String, Double>> pair : rerankedPassages) {
        	final String docID = pair.getKey();
        	final String text = pair.getValue().getKey();
        	final double score = pair.getValue().getValue();
        	if (!added.contains(docID)) {
        		added.add(docID);
        		filteredPassages.add(new Pair<String, Pair<String, Double>>(docID, new Pair<String, Double>(text, score)));
        	}
        	else {
        		Pair<String, Pair<String, Double>> changedData = new Pair<String, Pair<String, Double>>("", new Pair<String, Double>("" , 0.0));
        		int i = 0;
        		for (Pair<String, Pair<String, Double>> docData : filteredPassages) {
        			if (docData.getKey().equals(docID)) {
        				changedData = docData;
        				break;
        			}
        			i++;
        		}
        		filteredPassages.remove(i);
        		double newScore = score + changedData.getValue().getValue();
        		filteredPassages.add(new Pair<String, Pair<String, Double>>(docID, new Pair<String, Double>(text, newScore)));
        	}
        }
        Collections.sort(filteredPassages, new Comparator<Pair<String, Pair<String, Double>>>() {
            @Override
            public int compare(Pair<String, Pair<String, Double>> o1, Pair<String, Pair<String, Double>> o2) {
                return (o1.getValue().getValue() < o2.getValue().getValue()) ? 1 : -1;
            }
        });
        List<Pair<String, Pair<String, Double>>> top5 = new ArrayList<Pair<String, Pair<String, Double>>>();
        int i = 0;
        for (Pair<String, Pair<String, Double>> pair : filteredPassages) {
        	if (i == 20) {
        		break;
        	}
        	top5.add(pair);
        	i++;
        }
        return top5;
    }

    public static List<Pair<String, Pair<String, Double>>> NormalizeScores(List<Pair<String, Pair<String, Double>>> passagesData) {
        Collections.sort(passagesData, new Comparator<Pair<String, Pair<String, Double>>>() {
            @Override
            public int compare(Pair<String, Pair<String, Double>> o1, Pair<String, Pair<String, Double>> o2) {
                return (o1.getValue().getValue() < o2.getValue().getValue()) ? 1 : -1;
            }
        });

    	List<Pair<String, Pair<String, Double>>> rerankedPassages = new ArrayList<Pair<String, Pair<String, Double>>>();
    	if (passagesData.size() == 0) {
    		return passagesData;
    	}
        double maximum = passagesData.get(0).getValue().getValue();
        for (Pair<String, Pair<String, Double>> pair : passagesData) {
        	final String docID = pair.getKey();
        	final String text = pair.getValue().getKey();
        	final double score = 100 * pair.getValue().getValue() / maximum;
        	rerankedPassages.add(new Pair<String, Pair<String, Double>>(docID, new Pair<String, Double>(text, score)));
        }
        
        Collections.sort(rerankedPassages, new Comparator<Pair<String, Pair<String, Double>>>() {
            @Override
            public int compare(Pair<String, Pair<String, Double>> o1, Pair<String, Pair<String, Double>> o2) {
                return (o1.getValue().getValue() < o2.getValue().getValue()) ? 1 : -1;
            }
        });
    	List<Pair<String, Pair<String, Double>>> filteredPassages = new ArrayList<Pair<String, Pair<String, Double>>>();
        List<String> added = new ArrayList<String>();
        for (Pair<String, Pair<String, Double>> pair : rerankedPassages) {
        	final String docID = pair.getKey();
        	final String text = pair.getValue().getKey();
        	final double score = pair.getValue().getValue();
        	if (!added.contains(docID)) {
        		added.add(docID);
        		filteredPassages.add(new Pair<String, Pair<String, Double>>(docID, new Pair<String, Double>(text, score)));
        	}
        }
        return filteredPassages;
    }

}
