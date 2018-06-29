import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.lucene.search.IndexSearcher;

import com.google.gson.stream.JsonWriter;

import javafx.util.Pair;

public class JsonBuilder {
	List<Pair<String, Pair<String, Double>>> topDocs;
	String queryId;
	IndexSearcher searcher;
	int k;

	/**
	 *
	 * @param id Query  ID
	 * @param td topDocs query resaults
	 * @param indexSearcher index Searcher to retrieve the original documents
	 * @param num Number of answers to print in the file 
	 */
	JsonBuilder(String id, List<Pair<String, Pair<String, Double>>> td, IndexSearcher indexSearcher, int num)
	{
		queryId = id;
		topDocs = td;
		searcher = indexSearcher;
		k = num;
	}

	/**
	 * Creates JSON file on the running machine desktop with the results.
	 */
	public void createJson()
	{
		String jsonFilePath = "./src/OutPut/" + k+ ".json";
		File f = new File(jsonFilePath);
		if(f.exists() && !f.isDirectory()) { 
		    f.delete();
		}
	    try {
	        JsonWriter writer = new JsonWriter(new FileWriter(jsonFilePath));
	        writer.setIndent("  ");
	        writer.beginArray();
	        writer.beginObject();
	        writer.name("id").value(queryId);
	        writer.name("answers");
	        writer.beginArray();
	        int i = 0;
	        for (Pair<String, Pair<String, Double>> pair : topDocs) {
	        	if (i >= 5) {
	        		break;
	        	}
	        	Pair<String, Double> textAndScore = pair.getValue();
		        writer.beginObject();
		        writer.name("answer").value(textAndScore.getKey());
		        writer.name("score").value(textAndScore.getValue());
		        writer.endObject();
		        i++;
	        }
	        writer.endArray();
	        writer.endObject();
	        writer.endArray();
	        writer.close();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }

	}
}
