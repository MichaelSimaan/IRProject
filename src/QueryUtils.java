import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import javafx.util.Pair;

public class QueryUtils {
	static final String stopWordsFile = "src/StopWords.txt";
	static final String phrasesJsonFile = "src/Phrases.json";
	String query;
	String originalQuery;
	String nonExpandedQuery;
	static List<String> stopWords;
	String origQueryTerms;
	String expandedQueryTerms;
	String id;
	static List<String> allDocuments;
	static List<String> filteredPassages;
	static List<Pair<Double, String>> changePercentage;
	static Map<String, String> tagsMap;
	
	QueryUtils(String origQuery) {
		initStopWords();
		origQuery = origQuery.trim();
		while (origQuery.endsWith("?") || origQuery.endsWith(".")) {
			origQuery = origQuery.substring(0, origQuery.length() - 1);
		}
		
		query = origQuery.trim().replace("-", " ").replace("\"", "").replace("\\", "").replace("(", " ").replace(")", " ")
				.replace(":", " ").replace("/", " ").replace(",", " ").replace("?", " ").replace("*", " ");
		ArrayList<String> queryList = new ArrayList<String>(Arrays.asList(query.split("\\s+")));
		id = queryList.get(0);
		Boolean numeric = true;
        try {
            @SuppressWarnings("unused")
			Double num = Double.parseDouble(id);
        } catch (NumberFormatException e) {
            numeric = false;
            id = "No id given";
        }
        if (numeric) {
    		queryList.remove(0);
        }
        query = String.join(" ", queryList);
		originalQuery = query;
	}
	
	static String mergeIntoOneQuery(String expandedQuery, String beforeSubwords) {
		beforeSubwords = stemQuery(beforeSubwords);
		expandedQuery = stemQuery(expandedQuery);
		List<String> subwordsQuery = Arrays.asList(beforeSubwords.split("\\s+"));
		String[] expandedQueryWords = expandedQuery.split("\\s+");
		for (String subword : subwordsQuery) {
			boolean found = false;
			for (String term : expandedQueryWords) {
				term = term.trim().toLowerCase();
				if (subword.toLowerCase().equals(term)) {
					found = true;
				}
			}
			if (!found) {
				expandedQuery = expandedQuery + " " + subword;
			}
		}
		return expandedQuery;
	}

	static String getQueryId(String sentQuery) {
		ArrayList<String> queryList = new ArrayList<String>(Arrays.asList(sentQuery.split("\\s+")));
		String currentId = queryList.get(0);
		Boolean numeric = true;
        try {
            @SuppressWarnings("unused")
			Double num = Double.parseDouble(currentId);
        } catch (NumberFormatException e) {
            numeric = false;
        }
        if (numeric) {
        	return "-1";
        }
        return currentId;
	}
	static String initQuery(String sentQuery) {
		if (sentQuery.endsWith("?")) {
			sentQuery = sentQuery.substring(0, sentQuery.length() - 1);
		}
		sentQuery = sentQuery.trim().toLowerCase();
		ArrayList<String> queryList = new ArrayList<String>(Arrays.asList(sentQuery.split("\\s+")));
		String currentId = queryList.get(0);
		Boolean numeric = true;
        try {
            @SuppressWarnings("unused")
			Double num = Double.parseDouble(currentId);
        } catch (NumberFormatException e) {
            numeric = false;
        }
        if (numeric) {
    		queryList.remove(0);
        }
        sentQuery = String.join(" ", queryList);
        return sentQuery;
	}

	static public Map<String,Integer> lemmatizeMap(Map<String, Integer> map) throws IOException {
		try {
			String dir = System.getProperty("user.dir");
			String filePath = dir + "\\python\\QueryExpander.py";
			Gson gson = new Gson(); 
			String mapString = gson.toJson(map);
			mapString = mapString.replace("\"", "\\\"");
			ProcessBuilder pb = new ProcessBuilder("python", filePath, "-m", mapString);
			pb.redirectErrorStream(true);
			Process process = pb.start();
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String input = null;
			List<String> temp = new ArrayList<String>();
			while ((input = stdInput.readLine()) != null) {
				temp.add(input);
			}
			String output = temp.get(0);
			output = output.replace("\'", "\"");
			Map<String, Integer> son = new Gson().fromJson(output, new TypeToken<Map<String, Integer>>(){}.getType());
			return son;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return map;
	}

	public String expandQueryWithMostSimilar(int wordsCount) throws  IOException{
		String expandedQuery = Utils.most_similar(query,wordsCount);
		query = expandedQuery;
	    return expandedQuery;
	    
    }
	
	public void initOriginalTermsQuery()
	{
		List<String> querySplit = Arrays.asList(query.split("\\s+"));
		List<String> origQuerySplit = Arrays.asList(originalQuery.split("\\s+"));
		List<String> queryTerms = new ArrayList<String>();
		List<String> expandedQueryTermsTemp = new ArrayList<String>();
		for (String term : querySplit) {
			Boolean inserted = false;
			for (String origTerm : origQuerySplit) {
				if (term.toLowerCase().equals(origTerm.toLowerCase())) {
					queryTerms.add(term.toLowerCase());
					inserted = true;
				}
			}
			if (!inserted) {
				expandedQueryTermsTemp.add(term.toLowerCase());
			}
		}
		origQueryTerms = String.join("+", queryTerms);
		expandedQueryTerms = String.join(" ", expandedQueryTermsTemp);
	}

	public void initStopWords()
	{
		if (stopWords != null) {
			return;
		}
		stopWords = new ArrayList<String>();
		try (BufferedReader br = new BufferedReader(new FileReader(stopWordsFile))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		    	stopWords.add(line.trim().toLowerCase());
		    }
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static public String stemQuery(String expandedQuery)
	{
		try {
			String dir = System.getProperty("user.dir");
			String filePath = dir + "\\python\\StemQuery.py";
			ProcessBuilder pb = new ProcessBuilder("python", filePath, "-q", expandedQuery);
			Process process = pb.start();
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String input;
			List<String> temp = new ArrayList<String>();
			while ((input = stdInput.readLine()) != null) {
				temp.add(input);
			}
			expandedQuery = temp.get(0);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return expandedQuery;
	}
	
	static public void tagQuery(String expandedQuery)
	{
		try {
			String dir = System.getProperty("user.dir");
			String filePath = dir + "\\python\\TagQuery.py";
			ProcessBuilder pb = new ProcessBuilder("python", filePath, "-q", expandedQuery);
			Process process = pb.start();
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String input;
			List<String> temp = new ArrayList<String>();
			while ((input = stdInput.readLine()) != null) {
				temp.add(input);
			}
			String output = temp.get(0);
			output = output.replace("\'", "\"");
			Map<String, String> son = new Gson().fromJson(output, new TypeToken<Map<String, String>>(){}.getType());
			tagsMap = son;
			System.out.println(son);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public String spellCorrector(String expandedQuery)
	{
		try {
			String dir = System.getProperty("user.dir");
			String filePath = dir + "\\python\\SpellCorrector.py";
			ProcessBuilder pb = new ProcessBuilder("python", filePath, "-q", expandedQuery);
			Process process = pb.start();
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String input;
			List<String> temp = new ArrayList<String>();
			while ((input = stdInput.readLine()) != null) {
				temp.add(input);
			}
			expandedQuery = temp.get(0);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return expandedQuery;
	}

	public String checkForSubwords(String expandedQuery)
	{
		try {
			String dir = System.getProperty("user.dir");
			String filePath = dir + "\\python\\CheckForSubwords.py";
			ProcessBuilder pb = new ProcessBuilder("python", filePath, "-q", expandedQuery);
			Process process = pb.start();
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String input;
			List<String> temp = new ArrayList<String>();
			while ((input = stdInput.readLine()) != null) {
				temp.add(input);
			}
			expandedQuery = temp.get(0);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		query = expandedQuery;
		return expandedQuery;
	}

	static public String filterStopWords(String questions)
	{
		String [] queryTerms = questions.split("\\s+");
		String editedQuery = "";
		for (int i = 0; i < queryTerms.length ; i++) {
			String lowerCaseTerm = queryTerms[i].toLowerCase();
			lowerCaseTerm = lowerCaseTerm.replace("'", "");
			Boolean found = false;
			for (int j = 0; j < stopWords.size() ; j++) {
				if (lowerCaseTerm.equals(stopWords.get(j))) {
					found = true;
				}
			}
			if (!found) {
				editedQuery += queryTerms[i].replace("'", "") + ' ';
			}
		}
		questions = editedQuery.replace(",", " ").trim();
		return questions;
	}
	
	public String filterWords(String givenQuery)
	{
		String [] queryTerms = givenQuery.split("\\s+");
		String editedQuery = "";
		for (int i = 0; i < queryTerms.length ; i++) {
			String lowerCaseTerm = queryTerms[i].toLowerCase();
			lowerCaseTerm = lowerCaseTerm.replace("'", "");
			Boolean found = false;
			for (int j = 0; j < stopWords.size() ; j++) {
				if (lowerCaseTerm.equals(stopWords.get(j))) {
					found = true;
				}
			}
			if (!found) {
				editedQuery += queryTerms[i].replace("'", "") + ' ';
			}
		}
		query = editedQuery.replace(",", " ").trim();
		return query;
	}
	
	public String dealWithAbbreviation(String givenQuery)
	{
		String [] queryTerms = givenQuery.split("\\s+");
		List<String> fixedQuery = new ArrayList<String>();
		for (int i = 0; i < queryTerms.length ; i++) {
			String term = queryTerms[i];
			term = term.replace("'", "");
			if (term.contains(".")) {
				List<String> split = Arrays.asList(term.split("."));
				Boolean abbrev = true;
				for (String letter : split) {
					if (!letter.matches("[a-zA-Z ]+")) {
						abbrev = false;
						break;
					}
				}
				if (abbrev) {
					fixedQuery.add(term.replace(".", ""));
				}
			}
			fixedQuery.add(term);
		}
		query = String.join(" " , fixedQuery);
		return query;
	}

	static public String filterPassage(String passage)
	{
		passage = passage.trim();
		if (passage.endsWith(".")) {
			passage = passage.substring(0, passage.length() - 1);
		}
		passage = passage.replace(",", " ");
		String [] queryTerms = passage.split("\\s+");
		String editedQuery = "";
		for (int i = 0; i < queryTerms.length ; i++) {
			String lowerCaseTerm = queryTerms[i].toLowerCase();
			Boolean found = false;
			for (int j = 0; j < stopWords.size() ; j++) {
				if (lowerCaseTerm.equals(stopWords.get(j))) {
					found = true;
				}
			}
			if (!found) {
				editedQuery += queryTerms[i] + ' ';
			}
		}
		passage = editedQuery.trim();
		return passage;
	}
	
	static public String compressPassages(String passage) {
		List<String> queryTerms = Arrays.asList(passage.split("\\s+"));
		List<String> finalTerms = new ArrayList<String>();
		for (String term : queryTerms) {
			term = term.trim();
			if (!finalTerms.contains(term)) {
				finalTerms.add(term);
			}
		}
		return String.join(" ", finalTerms);
	}
	
	static public String expandsWithPassages(List<String> topPassages, String currentQuery, Boolean compressSameLineTerms, int queryLength) throws IOException, ParseException
	{	
		String expandedQuery = stemQuery(currentQuery);
		List<String> filteredPassages = new ArrayList<String>();
		for (String s : topPassages) {
			String filtered = filterPassage(s);
			if (compressSameLineTerms) {
				filtered = compressPassages(s);
			}
			filteredPassages.add(filtered);
		}
		Map<String, Integer> wordAppearances = new HashMap<String, Integer>();
		for (String k : filteredPassages) {
			List<String> passageWords = Arrays.asList(k.split("\\s+"));
			for (String word : passageWords) {
				word = word.replaceAll("[\"\'(){}-]", " ").replace("<", " ").replace(">", " ").trim().toLowerCase();
				if (word.equals("")) {
					continue;
				}
            	Boolean exists = wordAppearances.containsKey(word);
            	if (exists) {
            		int times = wordAppearances.get(word);
            		wordAppearances.put(word, times + 1);
            	}
            	else {
            		wordAppearances.put(word, 1);
            	}
			}
		}
        ArrayList<String> queryWords = new ArrayList<String>(Arrays.asList(expandedQuery.trim().split("\\s+")));
        for (String word : queryWords) {
        	Boolean exists = wordAppearances.containsKey(word);
        	if (exists) {
            	wordAppearances.remove(word.toLowerCase());
        	}
    	}
        wordAppearances = lemmatizeMap(wordAppearances);
        ValueComparator bvc = new ValueComparator(wordAppearances);
        TreeMap<String, Integer> sortedPassageMap = new TreeMap<String, Integer>(bvc);
        sortedPassageMap.putAll(wordAppearances);
        System.out.println(sortedPassageMap);
        int i = 0;
        int expandNum = queryLength;
        if (expandNum >= 7) {
        	expandNum = 0;
        }
        else if (expandNum > 5) {
        	expandNum = 2;
        }
        else if (expandNum >= 3) {
        	expandNum = 3;
        }
        else {
        	expandNum = 4;
        }

		for (Map.Entry<String, Integer> entry : sortedPassageMap.entrySet()) {
			if (i == expandNum) {
				break;
			}
			String key = entry.getKey();
			int value = entry.getValue();
			Boolean similar = false;
			for (String word : queryWords) {
				if (ifSimilar(key, word)) {
					similar = true;
				}
			}
			if (!similar && value >= 2 && key.length() >= 3 && !QueryReranker.IsNumeric(key) && key.matches("[a-zA-Z]+")
					&& !stopWords.contains(key)) {
				i++;
				expandedQuery = expandedQuery + " " + key;
				queryWords.add(key);
				System.out.println("Word appended : " + key);
			}
		}
		expandedQuery = filterStopWords(expandedQuery);
		expandedQuery = stemQuery(expandedQuery);
		expandedQuery = expandedQuery.trim();
		return expandedQuery;
	}
	
	static public String expandWithTerms(List<String> topPassages, String term, Boolean compressSameLineTerms, String type, List<String> expansionTerms, List<String> queryTerms) throws IOException, ParseException
	{	
		String givenTerm = stemQuery(term);
		List<String> filteredPassages = new ArrayList<String>();
		for (String s : topPassages) {
			String filtered = filterPassage(s);
			if (compressSameLineTerms) {
				filtered = compressPassages(s);
			}
			filteredPassages.add(filtered);
		}
		Map<String, Integer> wordAppearances = new HashMap<String, Integer>();
		for (String k : filteredPassages) {
			List<String> passageWords = Arrays.asList(k.split("\\s+"));
			for (String word : passageWords) {
				word = word.replaceAll("[\"\'(){}-]", " ").replace("<", " ").replace(">", " ").trim().toLowerCase();
				if (word.equals("")) {
					continue;
				}
            	Boolean exists = wordAppearances.containsKey(word);
            	if (exists) {
            		int times = wordAppearances.get(word);
            		wordAppearances.put(word, times + 1);
            	}
            	else {
            		wordAppearances.put(word, 1);
            	}
			}
		}
    	Boolean exists = wordAppearances.containsKey(givenTerm.toLowerCase());
    	if (exists) {
        	wordAppearances.remove(givenTerm.toLowerCase());
    	}
        wordAppearances = lemmatizeMap(wordAppearances);
        ValueComparator bvc = new ValueComparator(wordAppearances);
        TreeMap<String, Integer> sortedPassageMap = new TreeMap<String, Integer>(bvc);
        sortedPassageMap.putAll(wordAppearances);
        System.out.println(sortedPassageMap);
        int i = 0;
        int expandNum = 1;
        if (type.equals("S")) {
        	expandNum = 3;
        }
        else if (IsAllNouns(queryTerms) && queryTerms.size() > 1) {
        	expandNum = 1;
        }
        else if (type.equals("N")) {
        	expandNum = 1;
        }
        else if (type.equals("V") || type.equals("J")) {
        	expandNum = 0;
        }
		String expandedQuery = givenTerm;
		for (Map.Entry<String, Integer> entry : sortedPassageMap.entrySet()) {
			if (i == expandNum) {
				break;
			}
			String key = entry.getKey();
			int value = entry.getValue();
			Boolean similar = false;
			if (ifSimilar(key, term)) {
				similar = true;
			}
			if (!similar && value >= 2 && key.length() >= 3 && !QueryReranker.IsNumeric(key) && key.matches("[a-zA-Z]+")
					&& !expansionTerms.contains(key) && !queryTerms.contains(key) && !stopWords.contains(key)) {
				i++;
				System.out.println("Word appended : " + key);
				expandedQuery = expandedQuery + " " + key;
			}
		}
		expandedQuery = filterStopWords(expandedQuery);
		expandedQuery = stemQuery(expandedQuery);
		expandedQuery = expandedQuery.trim();
		return expandedQuery;
	}

	static public Boolean IsAllNouns(List<String> queryTerms) {
		Boolean nouns = true;
		for (String term : queryTerms) {
			if (tagsMap.containsKey(term) && !tagsMap.get(term).equals("N")) {
				nouns = false;
			}
		}
		return nouns;
	}


	public Boolean checkChangePercentage(List<List<Pair<String, Double>>> topDocs, String givenQuery, String newWord) {
			List<Pair<String, Double>> docsWithoutExpansion = topDocs.get(0);
			List<Pair<String, Double>> docsWithExpansion = topDocs.get(1);
			List<String> textWithoutExpansion = new ArrayList<String>();
			List<String> textWithExpansion = new ArrayList<String>();

			for (Pair<String, Double> doc : docsWithoutExpansion) {
				textWithoutExpansion.add(doc.getKey());
			}
			
			for (Pair<String, Double> doc : docsWithExpansion) {
				textWithExpansion.add(doc.getKey());
			}
			textWithoutExpansion.removeAll(textWithExpansion);
			double differnece = textWithoutExpansion.size()/100.0;
			double allowedChange = 0.7 - Arrays.asList(givenQuery.split("\\s+")).size() * 0.04;
			if (allowedChange <= 0.05) {
				allowedChange = 0.1;
			}
			System.out.println("Adding the word \"" + newWord + "\" changes the top results with " + differnece * 100 + "%");
			if (differnece > allowedChange) {
				if (changePercentage == null) {
					changePercentage = new ArrayList<Pair<Double,String>>();
				}
				changePercentage.add(new Pair<Double, String>(differnece, newWord));
				return false;
			}
			return true;
	}


	static Boolean ifSimilar(String word1, String word2) {
		double count = 0;
		String shorter = "";
		String longer = "";
		if (word1.length() < word2.length()) {
			shorter = word1.toLowerCase();
			longer = word2.toLowerCase();
		}
		else {
			shorter = word2.toLowerCase();
			longer = word1.toLowerCase();
		}
		for(int i = 0; i < word1.length() && i < word2.length(); i++) {
		    if(word1.charAt(i) == word2.charAt(i)){
		        count++;
		    }
		}
		double maxLength = Math.max(word1.length(), word2.length());
		double minLength = Math.min(word1.length(), word2.length());

		if (longer.toLowerCase().contains(shorter) && longer.length() > 5 && shorter.length() > 2 && count / maxLength <= 0.6) {
			return false;
		}
		if (longer.toLowerCase().startsWith(shorter)) {
			return true;
		}
		if (count / minLength > 0.8) {
			return true;
		}
		return false;
	}
	
	static String boostQuery(String nonExpandedQuery, String expandedQuery, int nonExpandBoost, int expandBoost) {
		nonExpandedQuery = stemQuery(nonExpandedQuery);
		List<String> nonExpandedWords = Arrays.asList(nonExpandedQuery.split("\\s+"));
		List<String> expandedWords = Arrays.asList(expandedQuery.split("\\s+"));
		List<String> finalQuery = new ArrayList<String>();
		for (String expandedWord : expandedWords) {
			Boolean origQueryWord = false;
			for (String nonExpandedWord : nonExpandedWords) {
				if (ifSimilar(expandedWord.replace("\\", ""), nonExpandedWord)) {
					origQueryWord = true;
				}
			}
			if (origQueryWord) {
				int boost = expandBoost;
				
				finalQuery.add(expandedWord + "^" + boost);
			}
			else {
				finalQuery.add(expandedWord + "^" + nonExpandBoost);
			}
		}
		String queryFinal = String.join(" ", finalQuery);
		return queryFinal;
	}

	/**
	 * boosts query
	 * @param nonExpandedQuery query before expansion
	 * @param expandedQuery query after  expansion
	 * @param originalQuery non filtered query
	 * @param nonExpandBoost boost value for the nonExpandedQuery terms
	 * @param expandBoost boost value for the expandedQuery terms
	 * @param originalOrderBoost boost value for the originalOrder terms
	 * @param queryLength
	 * @return
	 */
	static String boostQuery(String nonExpandedQuery, String expandedQuery, String originalQuery, int nonExpandBoost, int expandBoost, int originalOrderBoost, int queryLength) {
		nonExpandedQuery = stemQuery(nonExpandedQuery);
		List<String> nonExpandedWords = Arrays.asList(nonExpandedQuery.split("\\s+"));
		List<String> expandedWords = Arrays.asList(expandedQuery.split("\\s+"));
		List<String> finalQuery = new ArrayList<String>();
		for (String expandedWord : expandedWords) {
			Boolean origQueryWord = false;
			for (String nonExpandedWord : nonExpandedWords) {
				if (ifSimilar(expandedWord.replace("\\", ""), nonExpandedWord)) {
					origQueryWord = true;
				}
			}
			if (origQueryWord) {
				int boost = expandBoost;
				if (tagsMap.containsKey(expandedWord)) {
					if (tagsMap.get(expandedWord).equals("J")) {
						boost = boost + 2;
					}
					else if (tagsMap.get(expandedWord).equals("N")) {
						boost = boost + 1;
					}
				}
				if (queryLength == 1) {
					boost += 5;
				}
				finalQuery.add(expandedWord + "^" + boost);
			}
			else {
				finalQuery.add(expandedWord + "^" + nonExpandBoost);
			}
		}
		originalQuery = stemQuery(originalQuery);
		List<String> originalQueryWords = Arrays.asList(originalQuery.split("\\s+"));
		for (int i = 0 ; i < nonExpandedWords.size() - 1; i++) {
			for (int j = 0 ; j < originalQueryWords.size() - 1; j++) {
				String firstWord = nonExpandedWords.get(i).toLowerCase();
				String secondWord = nonExpandedWords.get(i + 1).toLowerCase();
				Boolean firstEqual = firstWord.toLowerCase().equals(originalQueryWords.get(j).toLowerCase());
				Boolean secondEqual = secondWord.toLowerCase().equals(originalQueryWords.get(j + 1).toLowerCase());
				if (firstEqual && secondEqual) {
					if (tagsMap.containsKey(firstWord) && tagsMap.get(firstWord).equals("J") && tagsMap.containsKey(secondWord) &&
							tagsMap.get(secondWord).equals("N")){
								finalQuery.add("\"" + nonExpandedWords.get(i).toLowerCase() + " " + nonExpandedWords.get(i + 1).toLowerCase() + "\"^" + originalOrderBoost);
							}
				}
			}
		}
		String queryFinal = String.join(" ", finalQuery);
		return queryFinal;
	}

	
	String checkVerbalPhrases(String givenQuery) throws FileNotFoundException {
        JsonReader jsonReader = new JsonReader(new FileReader(phrasesJsonFile));
		Map<String, String> verbalPhrasesMap = new Gson().fromJson(jsonReader, new TypeToken<Map<String, String>>(){}.getType());
		List<String> queryWords = Arrays.asList(givenQuery.split("\\s+"));
		String finalQuery = queryWords.get(0);
		for (int i = 0 ; i < queryWords.size() - 1 ; i++) {
			String possibleVerbalPhrase = queryWords.get(i).toLowerCase() + " " + queryWords.get(i + 1).toLowerCase();
			if (verbalPhrasesMap.get(possibleVerbalPhrase) != null) {
				finalQuery += " " + queryWords.get(i + 1) + " " + verbalPhrasesMap.get(possibleVerbalPhrase).replace("-", " ");
			}
			else {
				finalQuery += " " + queryWords.get(i + 1);
			}
		}
		return finalQuery;
	}
}

class ValueComparator implements Comparator<String> {
    Map<String, Integer> base;

    public ValueComparator(Map<String, Integer> base) {
        this.base = base;
    }

    // Note: this comparator imposes orderings that are inconsistent with
    // equals.
    public int compare(String a, String b) {
        if (base.get(a) >= base.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }
}
