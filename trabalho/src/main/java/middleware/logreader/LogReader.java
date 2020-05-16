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
        return getQueriesFromString(fileStr).size();
    }

    public Collection<String> getQueries() throws Exception{
        String fileStr = getFileString();
        return getQueriesFromString(fileStr);
    }

    private String getFileString() throws Exception{
        File log = new File(logPath);
        FileInputStream inputStream = new FileInputStream(log);
        return new String(inputStream.readAllBytes());
    }

    private Collection<String> getQueriesFromString(String filestr){
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
        return queries;
    }

    public static void main(String[] args)  throws  Exception{
        LogReader logReader = new LogReader("./testdb.sql.log");
        logReader.size();
    }
}
