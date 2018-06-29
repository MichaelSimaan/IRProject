import org.apache.lucene.index.Term;

public class TermStats {
    private double idf;
    private String text;

    public TermStats(double idf, String text) {
        this.idf = idf;
        this.text = text;
    }

    public double getIdf() {
        return idf;
    }

    public void setIdf(double idf) {
        this.idf = idf;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
