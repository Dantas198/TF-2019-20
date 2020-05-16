package middleware.logreader;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogReader {

    private String logPath;
    private List<String> queries;

    public LogReader(String logPath){
        this.logPath = logPath;
        this.queries = null;
    }

    public int size() throws Exception{
        String fileStr = getFileString();
        getQueriesFromString(fileStr);
        return queries.size();
    }

    public Collection<String> getQueries() throws Exception {
        String fileStr = getFileString();
        getQueriesFromString(fileStr);
        return queries;
    }

    private String getFileString() throws Exception{
        File log = new File(logPath);
        FileInputStream inputStream = new FileInputStream(log);
        return new String(inputStream.readAllBytes());
    }

    public Collection<String> getQueries(int lowerBound, int upperBound) throws Exception{
        List<String> res = new ArrayList<>(upperBound-lowerBound);
        lowerBound = lowerBound < 0 ? 0 : lowerBound;
        upperBound = upperBound > size()-1 ? size()-1 : upperBound;
        for(int i = lowerBound; i < upperBound; i++){
            res.add(queries.get(i));
        }
        return res;
    }

    public Collection<String> getQueries(int lowerBound) throws Exception {
        return getQueries(lowerBound, size()-1);
    }

    private void getQueriesFromString(String filestr){
        if(queries == null){
            queries = new ArrayList<>();
            String splitRegex = "\n(?=\\d{4}-\\d+-\\d+)";
            Pattern logLine = Pattern.compile("\\d+-\\d+-\\d+ \\d+:\\d+:\\d+[.]\\d+ \\d ((.|\n)*)");
            String[] split = filestr.split(splitRegex);
            for(String log : split){
                Matcher matcher = logLine.matcher(log);
                if(matcher.find()){
                    String query = matcher.group(1);
                    queries.add(query);
                } else {
                    System.out.println("Log " + log + " couldn't be parsed");
                }
            }
        }
    }

    public static void main(String[] args)  throws  Exception{
        LogReader logReader = new LogReader("./testdb.sql.log");
        logReader.size();
    }
}
