package edu.illinois.cs.cogcomp.saulexamples.data;

/**
 * Created by guest on 7/4/17.
 */

import java.io.IOException;
import java.io.PrintWriter;


public class WriteToFile {
    PrintWriter textWriter;
    public WriteToFile(String filename) throws IOException {
        String path = "data/mSpRL/" + filename + ".txt";
        textWriter = new PrintWriter(path);
    }

    public void WriteText(String s) {
        textWriter.print(s);
    }

    public void WriteTextln(String s) {
        textWriter.println(s);
    }

    public void closeWriter() {
        textWriter.close();
    }
}
