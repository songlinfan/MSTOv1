package common;

import MST.EncryptedMultilayerStackedTree;
import MST.MultilayerStackedTree;
import MST.UUIDs;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class InitDataHelper {
    private static String invertedIndex_inputDirPath = Config.get("PathOfDatasetUsed");
    private static int numOfKeywords = Integer.parseInt(Config.get("numOfKeywords"));
    private static int numOfUUIDs = 0;

    public static int getNumOfUUIDs(){
        return numOfUUIDs;
    }

    public static int getNumOfAllJSON(){
        File directory = new File(invertedIndex_inputDirPath);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                return files.length;
            }
        }
        System.err.println("Dataset directory Not Found");
        return 0;
    }

    public static int getNumOfKeywords(){
        if(numOfKeywords == -1){
            numOfKeywords = getNumOfAllJSON();
            return numOfKeywords;
        }
        if(numOfKeywords < 0 || numOfKeywords > getNumOfAllJSON()){
            return -1;
        }else{
            return numOfKeywords;
        }
    }

    public static Map<String, List<UUID>> loadInvertedIndexById(int maxKeywordCount) {
        ObjectMapper mapper = new ObjectMapper();
        String filePath = invertedIndex_inputDirPath + "/" + maxKeywordCount + ".json";
        File file = new File(filePath);

        try {
            Map<String, List<String>> raw = mapper.readValue(file, new TypeReference<Map<String, List<String>>>() {});
            Map<String, List<UUID>> result = new HashMap<>();

            for (Map.Entry<String, List<String>> entry : raw.entrySet()) {
                List<UUID> uuidList = entry.getValue().stream()
                        .map(UUID::fromString)
                        .collect(Collectors.toList());
                result.put(entry.getKey(), uuidList);
            }

            return result;

        } catch (IOException e) {
            throw new RuntimeException("Failed to read or parse the file: " + file.getName(), e);
        }
    }


    public static Map<Integer, Integer> getChunkCounts(int maxKeywordCount) {
        Map<Integer, Integer> chunkCounts = new HashMap<>();

        for (int i = 1; i <= maxKeywordCount; i++) {
            Map<String,List<UUID>> keywordToUUIDs = loadInvertedIndexById(i);
            Map.Entry<String, List<UUID>> entry = keywordToUUIDs.entrySet().iterator().next();

            UUIDs uuids = new UUIDs(entry.getValue());
            numOfUUIDs += uuids.getSize();
            List<Integer> chunksizeList =  uuids.getchunksizeList();

            for (int chunksize : chunksizeList) {
                chunkCounts.put(chunksize, chunkCounts.getOrDefault(chunksize,0) + 1);
            }
        }
        return chunkCounts;
    }



    public static EncryptedMultilayerStackedTree loadEMST(String FILE_NAME) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_NAME))) {
            EncryptedMultilayerStackedTree emst = (EncryptedMultilayerStackedTree) ois.readObject();
            System.out.println("EMST instance successfully loaded from " + FILE_NAME);
            return emst;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Failed to load MST instance: " + e.getMessage());
            return null;
        }
    }

    public static MultilayerStackedTree loadMLT(String FILE_NAME) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_NAME))) {
            MultilayerStackedTree mst = (MultilayerStackedTree) ois.readObject();
            System.out.println("MST instance successfully loaded from " + FILE_NAME);
            return mst;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Failed to load MST instance: " + e.getMessage());
            return null;
        }
    }

    public static double getSerializedSizeInMB(Object obj) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
            oos.flush();
            long bytes = baos.size();
            return bytes / (1024.0 * 1024.0); // 转换为 MB
        }
    }


    public static void appendLog(String LOG_FILE, String logContent) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            bw.write(timeStamp + " - " + logContent);
            bw.newLine();
        } catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        int numOfKeywords = InitDataHelper.getNumOfKeywords();
        if (numOfKeywords == -1) {
            System.err.println("Error: Invalid number of keywords. The value is either less than 0 or exceeds the number of keywords in the dataset.");
            System.exit(1);
        }
        System.out.println("numOfKeywords = " + numOfKeywords);
    }

}
