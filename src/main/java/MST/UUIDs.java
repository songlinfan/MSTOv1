package MST;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class UUIDs {
    private List<UUID> uuids;
    private int size;
    private  List<Integer> chunkSizeList = new ArrayList<>();
    private List<List<UUID>> chunks = new ArrayList<>();

    public UUIDs(List<UUID> uuids) {
        this.uuids = uuids;
        this.size = uuids.size();
        InitChunkSizeList(size);
        splitIntoChunks();
    }

    private void InitChunkSizeList(Integer size){
        int chunksize = 0;
        while(size > 0){
            if((size&1)==1){
                chunkSizeList.add(chunksize);
            }
            size>>=1;
            chunksize++;
        }
    }

    private void splitIntoChunks() {
        int start = 0;
        for(int i : chunkSizeList){
            int chunkSize = (int)Math.pow(2,i);
            chunks.add(new ArrayList<>(uuids.subList(start, start + chunkSize)));
            start += chunkSize;
        }
    }

    public int getSize() { return size;}

    public  List<Integer> getchunksizeList(){
        return chunkSizeList;
    }
    public List<List<UUID>> getChunks(){
        return chunks;
    }

    public static void main(String[] args) throws IOException {
        String filePath = "E:/keyword_documents_3.sql"; // 修改为你的 SQL 文件路径
        Map<String, List<UUID>> keywordMap = new HashMap<>();

        // 正则表达式匹配 INSERT 语句中的两个字符串
        Pattern pattern = Pattern.compile("VALUES \\('(.+?)', '(.+?)'\\);");

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String keyword = matcher.group(1);
                    String documentId = matcher.group(2);
                    if(!keywordMap.containsKey(keyword)){
                        keywordMap.put(keyword, new ArrayList<>());
                    }
                    keywordMap.get(keyword).add(UUID.fromString(documentId));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        // 创建输出目录
        File outputDir = new File("inverted_index/dataset3");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT); // pretty print

        int fileIndex = 1;
        for (Map.Entry<String, List<UUID>> entry : keywordMap.entrySet()) {
            Map<String, List<UUID>> singleEntryMap = new HashMap<>();
            singleEntryMap.put(entry.getKey(), entry.getValue());

            File outFile = new File(outputDir, fileIndex + ".json");
            mapper.writeValue(outFile, singleEntryMap);
            System.out.println(fileIndex);
            fileIndex++;

        }

        System.out.println("写入完成！");
    }

}
