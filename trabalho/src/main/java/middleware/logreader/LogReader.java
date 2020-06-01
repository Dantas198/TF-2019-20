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
        lowerBound = Math.max(lowerBound, 0);
        upperBound = Math.min(upperBound, size() - 1);
        for(int i = lowerBound; i < upperBound; i++){
            res.add(queries.get(i));
        }
        return res;
    }

    public Collection<String> getQueries(int lowerBound) throws Exception {
        return getQueries(lowerBound, size());
    }

    private void getQueriesFromString(String filestr){
        if(queries == null){
            queries = new ArrayList<>();
            String splitRegex = "\n(?=DROP|SET|INSERT|DELETE|CREATE|COMMIT)"; //"\n(?=\\d{4}-\\d+-\\d+)";
            Pattern logLine = Pattern.compile("(/\\*.*\\*/)?((.|[\n\r])*)");//"\\d+-\\d+-\\d+ \\d+:\\d+:\\d+[.]\\d+ \\d ((.|\n)*)");
            String[] split = filestr.split(splitRegex);
            for(String log : split){
                Matcher matcher = logLine.matcher(log);
                if(matcher.find()){
                    String query = matcher.group(0);
                    //System.out.println("Query--" + query);
                    queries.add(query);
                } else {
                    System.out.println("Log " + log + " couldn't be parsed");
                }
            }
        }
    }

    public static void main(String[] args)  throws  Exception{
        LogReader logReader = new LogReader("./testdb.log");
        logReader.getQueries(18).forEach(System.out::println);
    }
}
