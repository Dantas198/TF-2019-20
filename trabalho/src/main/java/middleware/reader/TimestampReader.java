package middleware.reader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class TimestampReader {
    private Path timestampPath;

    public TimestampReader(String timestampPath) {
        this.timestampPath = Path.of(timestampPath);
    }


    public void putTimestamp(long timestamp) throws IOException {
        putTimeStamp(Long.toString(timestamp));
    }

    public void putTimeStamp(String timestamp) throws IOException {
        if(!Files.exists(timestampPath))
           Files.createFile(timestampPath);
        Files.write(timestampPath, timestamp.getBytes());
    }

    public long getTimestamp() throws IOException {
        if(!Files.exists(timestampPath)){
            putTimestamp(0);
        }
        return Long.parseLong(Files.readString(timestampPath, StandardCharsets.US_ASCII));
    }
}
