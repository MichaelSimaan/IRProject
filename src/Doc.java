import java.util.ArrayList;

public class Doc {
    private String main_category;
    private String question;
    private ArrayList<String> nbestanswers;
    private String answer;
    private String id;

    public Doc(String main_category, String question, ArrayList<String> nbestanswers, String answer, String id) {
        this.main_category = main_category;
        this.question = question;
        this.nbestanswers = nbestanswers;
        this.answer = answer;
        this.id = id;
    }

    public String getMain_category() {
        return main_category;
    }

    public void setMain_category(String main_category) {
        this.main_category = main_category;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public ArrayList<String> getNbestanswers() {
        return nbestanswers;
    }
    //test
    public String getNbestanswersString(){
        String listString = "";
        for (String s : nbestanswers)
        {
            listString += s;
        }
        return listString;
    }
    public void setNbestanswers(ArrayList<String> nbestanswers) {
        this.nbestanswers = nbestanswers;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
