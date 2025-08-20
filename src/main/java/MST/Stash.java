package MST;

import common.Config;
import common.Crypt;
import common.SecretKeyGenerator;
import redis.clients.jedis.Jedis;

import javax.crypto.SecretKey;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;


/**
 * A {@code Stash} consists of three collections of {@code Block} objects with different priorities.
 *
 * It serves as a temporary storage for blocks read from the server,
 * and supports evicting them back to the server at a later time.
 */
public class Stash implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Block> stashPri1 = new ArrayList<>();
    private List<Block> stashPri2 = new ArrayList<>();
    private Map<String,List<Block>> stashPri3 = new  HashMap<>();

    private Map<Integer, Map<Integer, Bucket>> bktArr = new HashMap<>(); // layer, (BucketID, bucket)
    public Map<Integer, Map<Integer, Bucket>> getBktArr(){ return bktArr; }

    public Map<Integer, Map<Integer, byte[]>> getEncryptedBktArr() {
        SecretKey key = SecretKeyGenerator.loadKey();
        Map<Integer, Map<Integer, byte[]>> EncryptedBktArr = new HashMap<>();

        for (Map.Entry<Integer, Map<Integer, Bucket>> entry : bktArr.entrySet()) {
            int layer = entry.getKey();
            Map<Integer, byte[]> buckets = new HashMap<>();
            for (Map.Entry<Integer, Bucket> entry2 : entry.getValue().entrySet()) {
                int bucketID = entry2.getKey();
                Bucket bucket = entry2.getValue();
                byte[] EncryptedBucket = Crypt.encryptBucket(bucket,key);
                buckets.put(bucketID,EncryptedBucket);
            }
            EncryptedBktArr.put(layer, buckets);
        }
        return EncryptedBktArr;
    }

    /**
     * Extracts the UUID associated with the search keyword from the response,
     * and moves all blocks in the response into the stash:
     *      If a block's keyword does not match the search keyword, move it to blocks1;
     *      otherwise, move it to blocks2.
     *
     * All UUIDs matching the search query have been written to data/client/SearchResult.txt
     */
    public void parseEncryptedResponse(String keyword, Map<Integer, Map<Integer, byte[]>> response) {
        List<UUID> uuids = new ArrayList<>();
        SecretKey key = SecretKeyGenerator.loadKey();

        for (Map.Entry<Integer, Map<Integer, byte[]>> entry : response.entrySet()) {
            int layer =  entry.getKey();
            Map<Integer, byte[]> bucketMap = entry.getValue();
            Map<Integer, Bucket> clearBkts = new HashMap<>();

            for (Map.Entry<Integer, byte[]> bucketEntry : bucketMap.entrySet()) {
                int bucketId = bucketEntry.getKey();
                Bucket bucket = Crypt.decryptBucket(bucketEntry.getValue(), key);
                List<Block> blocks = bucket.getBlocks();
                for (Block block : blocks) {
                    Object first = block.getUuids().get(0);
                    if(!block.getKeyword().equals("Dummy")) {
                        addBlockToStash(keyword, block);
                        if (keyword.equals(block.getKeyword()))
                            uuids.addAll(block.getUuids());
                    }
                }
                bucket.clear();
                clearBkts.put(bucketId,bucket);
            }
            bktArr.put(layer,clearBkts);
        }
        saveSearchResultToLocal(keyword,uuids,Config.get("CipherSearchResultFilePath"));
    }

    public void parseResponse(String keyword,  Map<Integer, Map<Integer, Bucket>> response) throws IOException {
        List<UUID> uuids = new ArrayList<>();

        for (Map.Entry<Integer, Map<Integer, Bucket>> entry : response.entrySet()) {
            Map<Integer, Bucket> bucketMap = entry.getValue();

            for (Map.Entry<Integer, Bucket> bucketEntry : bucketMap.entrySet()) {
                Bucket bucket = bucketEntry.getValue();
                List<Block> blocks = bucket.getBlocks();
                for (Block block : blocks) {
                    if(!block.getKeyword().equals("Dummy")) {
                        addBlockToStash(keyword,block);
                        if(keyword.equals(block.getKeyword()))
                            uuids.addAll(block.getUuids());
                    }

                }
                bucket.clear();
            }
        }
        bktArr = response;
        saveSearchResultToLocal(keyword,uuids,Config.get("PlainSearchResultFilePath"));
    }

    public void evict(String keyword, KeywordToLeaf keywordToLeaf) throws IOException {
        int oldleaf = keywordToLeaf.getLeafOfKeyword(keyword);
        evict1(keywordToLeaf,oldleaf,2);
        evict1(keywordToLeaf,oldleaf,1);
        evict2(keywordToLeaf,keyword);
        evict3(keywordToLeaf,oldleaf);
        for (Block block : stashPri1) {
            block.resetPriority(1);
        }
    }


    private void evict1(KeywordToLeaf keywordToLeaf, int oldleaf, int priority) throws IOException {
        Iterator<Block> iterator = stashPri1.iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if(block.getPriority()==priority) {
                int layer = Integer.numberOfTrailingZeros(block.getUuids().size());
                int leaf = keywordToLeaf.getLeafOfKeyword(block.getKeyword());
                int intersectionLeaf = getIntersectionPath(oldleaf, leaf);
                if (insertBlock(layer, intersectionLeaf, block)) {
                    iterator.remove();
                } else {
                    block.resetPriority(0);
                    System.out.println("Insert info: no available space along the path for block:(" + block + "), it will remain in the stash for later processing.");
                }
            }
        }
    }


    private void evict2(KeywordToLeaf keywordToLeaf, String searchKeyword) throws IOException {
        int oldLeaf = keywordToLeaf.getLeafOfKeyword(searchKeyword);
        int newLeaf = assignNewLeaf(oldLeaf);
        if(newLeaf == -1){
            System.out.println("Insert info: insufficient space, so all blocks associated with '" + searchKeyword + "' have been stored in the stash(Pri3).");
            removeBlocksToStashPri3(searchKeyword);
            return;
        }

        for (Block block : stashPri2) {
            int layer =  Integer.numberOfTrailingZeros(block.getUuids().size());
            if(!insertBlock(layer, newLeaf, block)){
                System.err.println("error");
            }
        }
        stashPri2.clear();
        keywordToLeaf.setLeafOfKeyword(searchKeyword, newLeaf);
    }

    private void evict3(KeywordToLeaf keywordToLeaf, int oldleaf) throws IOException {
        Iterator<Map.Entry<String, List<Block>>> iterator = stashPri3.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<Block>> entry = iterator.next();
            String keyword = entry.getKey();
            List<Block> blocks = entry.getValue();

            if (isValidLeaf(oldleaf, blocks)) {
                for (Block block : blocks) {
                    int layer = Integer.numberOfTrailingZeros(block.getUuids().size());
                    if (!insertBlock(layer, oldleaf, block)) {
                        System.err.println("error");
                    }
                }
                keywordToLeaf.setLeafOfKeyword(keyword, oldleaf);
                iterator.remove();
            }
        }
    }


    public void addChunksToStash(String keyword, List<List<UUID>> chunks) {
        List<Block> blocks = new ArrayList<>();
        for (List<UUID> chunk : chunks) {
            Block block = new Block(keyword,chunk);
            blocks.add(block);
        }
        stashPri3.put(keyword,blocks);
    }

    private void removeBlocksToStashPri3(String keyword) {
        stashPri3.put(keyword,stashPri2);
        stashPri2.clear();
    }

    private boolean isValidLeaf(int leaf,List<Block> blocks) {
        boolean valid = true;
        for (Block block : blocks) {
            int layer =  Integer.numberOfTrailingZeros(block.getUuids().size());
            Map<Integer, Bucket> buckets = bktArr.get(layer);
            int bucketID = leaf;
            boolean validlLayer = false;

            while(bucketID>=0){
                if(buckets.get(bucketID)!=null){
                    if(buckets.get(bucketID).hasFreeBlock()){
                        validlLayer = true;
                        break;
                    }
                }
                if(bucketID == 0) return false;
                bucketID = (bucketID - 1) / 2;
            }
            valid = valid & validlLayer;
        }
        return valid;
    }


    private int getIntersectionPath(int oldLeaf, int newLeaf) {
        while(oldLeaf != newLeaf){
            if(oldLeaf > newLeaf){
                oldLeaf = (oldLeaf-1)/2;
            }else{
                newLeaf = (newLeaf-1)/2;
            }
        }
        return oldLeaf;
    }

    /**
     * Attempts to assign a new valid leaf.
     * This method either returns a valid leaf or returns -1 if there is not enough space to evict blocks2
     *
     * @param oldLeaf the original leaf index used to determine tree height
     * @return a valid new leaf index, or -1 if no valid leaf is available
     */
    private int assignNewLeaf(int oldLeaf) {
        Random random = new Random();
        int height = getBaseHeight(oldLeaf);
        int lowerBound = (int) (Math.pow(2, height) - 1);
        int upperBound = (int) (Math.pow(2, height + 1) - 2);

        List<Integer> candidates = new ArrayList<>();
        for (int i = lowerBound; i <= upperBound; i++) {
            candidates.add(i);
        }

        Collections.shuffle(candidates, random);

        for (int newLeaf : candidates) {
            if(newLeaf == oldLeaf){
                continue;
            }
            if (isValidLeaf(newLeaf,stashPri2)) {
                return newLeaf;
            }
        }
        return -1;
    }


    /**
     * By using a known leaf, we can infer the height of the base tree,
     * which in turn allows us to compute the valid range of leaf nodes.
     */
    private int getBaseHeight(int oldLeaf){
        int height = 0;
        while(true){
            int lowerBound = (int) (Math.pow(2,height)-1);
            int upperBound = (int) (Math.pow(2,height+1)-2);
            if(oldLeaf >= lowerBound && oldLeaf <= upperBound) {
                return height;
            }
            height++;
        }
    }



    private boolean insertBlock(int layer, int leaf, Block block) {
        Map<Integer, Bucket> buckets = bktArr.get(layer);
        List<Integer> path = new ArrayList<>();
        int bucketID = leaf;
        while (true) {
            if (buckets.containsKey(bucketID)) {
                path.add(bucketID);
            }
            if (bucketID == 0) break;
            bucketID = (bucketID - 1) / 2;
        }

        for (int node : path) {
            Bucket bucket = buckets.get(node);
            if (bucket.hasFreeBlock()) {
                block.updateCounter();
                block.resetPriority(2);
                bucket.insertBlock(block);
                return true;
            }
        }

        return false;
    }

    private void addBlockToStash(String keyword, Block block) {
        if(!block.getKeyword().equals(keyword)){
            stashPri1.add(block);
        }else{
            stashPri2.add(block);
        }
    }

    public void padding(){
        for (Map.Entry<Integer, Map<Integer, Bucket>> entry : bktArr.entrySet()) {
            for(Bucket bucket : entry.getValue().values()){
                bucket.padding();
            }
        }
    }


    private void saveSearchResultToLocal(String keyword, List<UUID> UUIDs, String filename) {
        Path outputPath = Paths.get(filename);
        try {
            Files.createDirectories(outputPath.getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(keyword);
            writer.newLine();
            for (UUID uuid : UUIDs) {
                writer.write(String.valueOf(uuid));
                writer.newLine();
            }
            System.out.println("The search result for keyword '" + keyword + "' contains " + UUIDs.size() + " UUIDs, which have been written to file: " + outputPath.toString());
        } catch (IOException e) {
            System.err.println("Error writing UUIDs to file: " + e.getMessage());
        }
    }

    public void saveToDisk(String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(this);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save Stash object to disk (Please 实例化一个MST first): " + filename, e);
        }
    }

    public static Stash loadFromDisk(String filename) {
        File file = new File(filename);

        if (!file.exists()) {
            throw new RuntimeException("Stash file not found at: " + filename + "\n Please run ");
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (Stash) ois.readObject();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read Stash object from disk: " + filename, e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class definition not found while loading Stash object", e);
        }
    }




    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(size:").append(stashPri1.size()+") stashPri1: ").append(stashPri1).append("\n");
        sb.append("(size:").append(stashPri2.size()+") stashPri2: ").append(stashPri2).append("\n");
        sb.append("(size:").append(stashPri3.size()+") stashPri3: ").append(stashPri3).append("\n");
        return sb.toString();
    }
}
