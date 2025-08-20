package MST;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;

public class KeywordToLeaf {
    Map<String, Integer> keywordMap = new HashMap<String, Integer>();

    public KeywordToLeaf() {

    }

    public KeywordToLeaf(String filename) {
        LoadFromDisk(filename);
    }

    public void LoadFromDisk(String filename) {
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(filename);
        try {
            keywordMap = mapper.readValue(file, Map.class);
        } catch (IOException e) {
            System.err.println("Error reading JSON file: " + e.getMessage());
        }
    }

    public void setLeafOfKeyword(String keyword, int leaf) {
        keywordMap.put(keyword, leaf);
    }

    public int getLeafOfKeyword(String keyword) {
        if (keywordMap.containsKey(keyword)) {
            return keywordMap.get(keyword);
        }else{
            System.err.println("The queried keyword does not exist in KeywordToLeaf. Please try again with a different keyword.");
            System.exit(-1);
            return  -1;
        }
    }

    public void saveToDisk(String filename) {
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(filename);
        file.getParentFile().mkdirs();
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, keywordMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void modifyKeywordToLeaf(String filename,String keyword, int leaf) throws IOException {
        keywordMap.put(keyword, leaf);
        saveToDisk(filename);
    }
}
