import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QueryReranker {
	static Double Reranker(String query, String sentence, List<String> topPassages) {
		List<Integer> boostList = CreateBoostList(query);
		List<String> queryTerms = CreateBoostLessQueryTerms(query);
		query = String.join(" ", queryTerms);
		double score = 0;
		int i = 0;
		for (String term : queryTerms) {
			score += ScoreTerm(term, query, sentence, topPassages, boostList.get(i));
			i++;
		}
		return score;
	}
	
	static Double ScoreTerm(String term, String query, String sentence, List<String> topPassages, double boost) {
		double termFreqInQuery = CalcTermFreq(term, query);
		double termFreqInSentence = CalcTermFreq(term, sentence);
		double numOfPassages = topPassages.size();
		double sentenceFreq = CalcSentenceFreq(term, topPassages);
		// There are 3 multiplied logs - 1. log(TermQueryFreq + 1) 2. log(TermSentenceFreq + 1) 3. log((n + 1) / (0.5 + sentenceFreq))
		// Also, we multiply the boost
		double log1 = Math.log(termFreqInQuery + 1);
		double log2 = Math.log(termFreqInSentence + 1);
		double log3 = Math.log((numOfPassages + 1) / (0.5 + sentenceFreq));
		return log1 * log2 * log3 * boost;
	}
	
	static int CalcSentenceFreq(String term, List<String> topPassages) {
		int freq = 0;
		for (String passage : topPassages) {
			if (passage.toLowerCase().contains(term.toLowerCase())) {
				freq++;
			}
		}
		return freq;
	}
	
	static int CalcTermFreq(String term, String sentence) {
		int freq = 0;
		List<String> sentenceTerms = Arrays.asList(sentence.split("\\s+"));
		for (String sentenceTerm : sentenceTerms) {
			if (sentenceTerm.toLowerCase().equals(term.toLowerCase())) {
				freq++;
			}
		}
		return freq;
	}
	
	static public List<Integer> CreateBoostList(String query){
		List<String> boostedQueryTerms = Arrays.asList(query.split("\\s+"));
		List<Integer> boostList = new ArrayList<Integer>();
		for (String term : boostedQueryTerms) {
			if (term.contains("^")) {
				int index = term.lastIndexOf("^");
				if (term.length() > index + 1 && IsNumeric(term.substring(index + 1))) {
					int boost = Integer.parseInt(term.substring(index + 1));
					boostList.add(boost);
				}
			}
			else {
				boostList.add(1);
			}
		}
		if (boostedQueryTerms.size() != boostList.size()) {
			System.out.println("Didn't create boost for all terms");
			System.exit(0);
		}
		return boostList;
	}

	static public List<String> CreateBoostLessQueryTerms(String query){
		List<String> boostedQueryTerms = Arrays.asList(query.split("\\s+"));
		List<String> queryTerms = new ArrayList<String>();
		for (String term : boostedQueryTerms) {
			if (term.contains("^")) {
				int index = term.lastIndexOf("^");
				if (term.length() > index + 1 && IsNumeric(term.substring(index + 1))) {
					term = term.substring(0, index);
				}
			}
			queryTerms.add(term);
		}
		return queryTerms;
	}

	static public boolean IsNumeric(String str)  
	{  
		try {
			int d = Integer.parseInt(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}
}
