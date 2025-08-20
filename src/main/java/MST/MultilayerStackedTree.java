package MST;

import com.fasterxml.jackson.databind.ObjectMapper;
import common.Config;
import common.InitDataHelper;
import redis.clients.jedis.Jedis;

import java.io.*;
import java.util.*;


/**
 * A {@code MultilayerStackedTree} consists of multiple {@code LayerTree} instances.
 *
 * We use a {@code Map<Integer, LayerTree>} to implement multiple {@code LayerTree} instances
 * because it allows efficient retrieval of a specific {@code LayerTree} based on the given layer index.
 *
 * The base tree refers to the {@code LayerTree} with the highest depth among all layers.
 */
public class MultilayerStackedTree implements Serializable {
    private static final long serialVersionUID = 1L;
    static int bucketCapacity = Integer.parseInt(Config.get("bucketCapacity"));

    private Map<Integer, LayerTree> layerTrees;
    private int baseTreeLayer = -1;
    private int baseTreeDepth = -1;

    private int errorInsertCount = 0;

    public MultilayerStackedTree() {}

    /**
     * Constructs a {@code MultilayerStackedTree} based on the number of chunks of different sizes extracted from the dataset.
     * <p>
     * One block in the layer-th {@code LayerTree} can store a single chunk with {@code chunkSize = layer},
     * which contains {@code 2^layer} UUIDs.
     * The {@code chunkCount} specifies the number of such chunks of a {@code chunkSize},
     * which is also the number of blocks needed for that layer.
     */
    public MultilayerStackedTree(Map<Integer, Integer> chunkCounts) {
        layerTrees  = new HashMap<>();

        for(Map.Entry<Integer,Integer> entry :chunkCounts.entrySet()){
            int chunkSize = entry.getKey();
            int chunkCount = entry.getValue();

            LayerTree layerTree = new LayerTree(chunkCount+1, bucketCapacity, chunkSize);
            layerTrees.put(chunkSize,layerTree);

            if (layerTree.getDepth() > baseTreeDepth) {
                baseTreeDepth = layerTree.getDepth();
                baseTreeLayer = chunkSize;
            }
        }
    }



    public int getErrorInsertCount() {
        return errorInsertCount;
    }
    public Map<Integer, LayerTree> getLayerTrees() { return layerTrees;}
    public int getBaseTreeDepth() { return baseTreeDepth; }
    public int getBaseTreeLayer() { return baseTreeLayer; }

    public List<Integer> getLeafToRootPathOnBaseTree(int leafIndex) {
        LayerTree baseTree = layerTrees.get(baseTreeLayer);
        int leaf = baseTree.getLeafStartIndex() + leafIndex;

        List<Integer> LeafToRootPathOnBaseTree = new ArrayList<>();
        while (leaf >= 0) {
            LeafToRootPathOnBaseTree.add(leaf);
            if (leaf == 0) break;
            leaf = (leaf - 1) / 2;
        }
        return LeafToRootPathOnBaseTree;
    }


    public boolean hasEnoughEmptyBlockAlongLeafToRoot(List<Integer> chunkSizeList, int leafIndex) {
        LayerTree baseTree = layerTrees.get(baseTreeLayer);
        int leaf = baseTree.getLeafStartIndex() + leafIndex;

        boolean empty = true;
        for(int layer : chunkSizeList){
            LayerTree layerTree = layerTrees.get(layer);
            List<Integer> leafToRootPathOnLayerTree = layerTree.getLeafToRootPath(leaf);
            empty = layerTree.hasEmptyBlockAlongPath(leafToRootPathOnLayerTree) & empty;
        }
        return empty;
    }

    public int AssignLeafIndex (List<Integer> chunkSizeList){
        Random rand = new Random();
        Set<Integer> InvalidLeafIndex = new HashSet<>();
        while(true){
            int leafIndex= rand.nextInt((int) Math.pow(2, baseTreeDepth));
            if(hasEnoughEmptyBlockAlongLeafToRoot(chunkSizeList,leafIndex)){
                return leafIndex;
            }else{
                InvalidLeafIndex.add(leafIndex);
                if(InvalidLeafIndex.size() == (int) Math.pow(2, baseTreeDepth)){
                    return -1;
                }
            }
        }
    }

    public void insertKeyword(String keyword, int leaf ,List<Integer> chunkSizeList, List<List<UUID>> chunks ) {
        for (int i=0; i<chunkSizeList.size(); i++) {
            int chunksize = chunkSizeList.get(i);
            LayerTree layerTree = layerTrees.get(chunksize);
            List<UUID> chunk = chunks.get(i);

            List<Integer> LeafToRootPathOnLayerTree = layerTree.getLeafToRootPath(leaf);
            Bucket[] buckets = layerTree.getBuckets();
            for (int nodeIndex : LeafToRootPathOnLayerTree) {
                Bucket bucket = buckets[nodeIndex];
                if(bucket.hasFreeBlock()){
                    bucket.insertBlock(keyword,chunk);
                    break;
                }
            }
        }
    }

    public void insertDataSetIntoMST(int numOfKeywords,boolean isEncrypted){
        Stash stash = new Stash();
        KeywordToLeaf keywordToLeaf = new KeywordToLeaf();

        for (int i = 1; i <= numOfKeywords; i++) {
            Map<String, List<UUID>> keywordToUUIDs = InitDataHelper.loadInvertedIndexById(i);
            Map.Entry<String, List<UUID>> entry = keywordToUUIDs.entrySet().iterator().next();

            String keyword = entry.getKey();
            UUIDs uuids = new UUIDs(entry.getValue());

            List<Integer> chunksizeList = uuids.getchunksizeList();
            List<List<UUID>> chunks = uuids.getChunks();
            int leafIndex = AssignLeafIndex(chunksizeList);

            if (leafIndex == -1) {
                System.err.println("There is no empty block available for keyword: " + keyword);
                System.err.println("It should be loaded into one of these layers: " + chunksizeList);
                errorInsertCount++;
                stash.addChunksToStash(keyword, chunks);
            } else {
                LayerTree baseTree = layerTrees.get(baseTreeLayer);
                int leaf = baseTree.getLeafStartIndex() + leafIndex;

                insertKeyword(keyword, leaf, chunksizeList, chunks);
                keywordToLeaf.setLeafOfKeyword(keyword, leaf);
            }
        }

        if(isEncrypted){
            stash.saveToDisk(Config.get("CipherStashFilePath"));
            keywordToLeaf.saveToDisk(Config.get("CipherKeywordToLeafFilePath"));
        }else{
            stash.saveToDisk(Config.get("PlainStashFilePath"));
            keywordToLeaf.saveToDisk(Config.get("PlainKeywordToLeafFilePath"));
        }

        if (errorInsertCount > 0) {
            System.out.println(errorInsertCount + " keywords could not be inserted; their blocks have been placed in the stash. All other keywords and their UUIDs have been successfully inserted.");
        } else {
            System.out.println("All keywords and their UUIDs in the dataset have been successfully inserted into the MST.");
        }
    }

    public Map<Integer,Map<Integer, Bucket>> ResponseToSearchQuery(int leaf){
        Map<Integer,Map<Integer, Bucket>> response = new HashMap<>();
        for(Map.Entry<Integer, LayerTree> entry: layerTrees.entrySet()) {
            LayerTree layerTree = entry.getValue();
            Integer layer = entry.getKey();

            Map<Integer, Bucket> bucketsAlongLeafToRoot = layerTree.getBucketsFromLeafToRoot(leaf);
            response.put(layer, bucketsAlongLeafToRoot);
        }
        return response;
    }

    public void Replace(Map<Integer, Map<Integer, Bucket>> updatedResponse) {
        for (Map.Entry<Integer, Map<Integer, Bucket>> layerEntry : updatedResponse.entrySet()) {
            Integer layer = layerEntry.getKey();
            Map<Integer, Bucket> updatedBuckets = layerEntry.getValue();

            LayerTree layerTree = layerTrees.get(layer);
            if (layerTree == null) continue;

            for (Map.Entry<Integer, Bucket> bucketEntry : updatedBuckets.entrySet()) {
                Integer index = bucketEntry.getKey();
                Bucket updatedBucket = bucketEntry.getValue();

                layerTree.setBucketAt(index,updatedBucket);
            }
        }
    }

    public void padding(){
        for(Map.Entry<Integer, LayerTree> layerTrees: layerTrees.entrySet()){
            LayerTree layerTree = layerTrees.getValue();
            Bucket[] buckets = layerTree.getBuckets();
            for(Bucket bucket : buckets){
                bucket.padding();
            }
        }
    }


    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for(Map.Entry<Integer, LayerTree> entry : layerTrees.entrySet()){
            LayerTree tree = entry.getValue();
            stringBuilder.append("tree layer = " + tree.getLayer() +  ": ");
            stringBuilder.append(tree);
        }
        return stringBuilder.toString();
    }

}
