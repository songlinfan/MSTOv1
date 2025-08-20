package MST;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LayerTree represents a full binary tree where each node is a bucket, implemented using an array.
 * <p>
 * The height of the tree is given by {@code depth}, so the total number of nodes is
 * {@code (int) Math.pow(2, depth + 1) - 1}.
 * <p>
 * The {@code layer} indicates the layer of the LayerTree within the MST.
 * At layer {@code i}, each block in a bucket(tree node) can store {@code (int) Math.pow(2, layer)} UUIDs.
 */
public class LayerTree implements Serializable {
    private static final long serialVersionUID = 1L;
    private Bucket[] buckets;
    private int layer;
    private int depth;
    private int totalNodes;

    /**
     * Constructs a LayerTree that has enough capacity to hold the specified number of blocks.
     *
     * @param totalBlocksNeeded   The number of blocks of size {@code 2^layer} required
     *                            (corresponding to how many UUIDs of size {@code 2^layer} exist in the dataset).
     * @param bucketCapacity      The number of blocks each bucket can store.
     * @param layer               The layer of the LayerTree within the MST.
     */
    public LayerTree(int totalBlocksNeeded, int bucketCapacity, int layer) {
        int totalBucketsNeeded = (int) Math.ceil((double) totalBlocksNeeded / bucketCapacity);

        depth = 0;
        while ((Math.pow(2, depth + 1) - 1) < totalBucketsNeeded) {
            depth++;
        }
        //depth++;

        totalNodes = (int) Math.pow(2, depth + 1) - 1;

        buckets = new Bucket[totalNodes];
        for (int i = 0; i < totalNodes; i++) {
            buckets[i] = new Bucket(layer);
        }
        this.layer = layer;

        System.out.println("Layer " + layer + " tree has been initialized with depth = " + depth + ".");
    }

    public Bucket[] getBuckets(){return this.buckets;}

    public int getLayer(){return this.layer;}
    public int getDepth(){return this.depth;}
    public int getTotalNodes(){return totalNodes;}
    public int getLeafStartIndex() {
        return (int) Math.pow(2, depth) - 1;
    }



    public List<Integer> getLeafToRootPath(int baseLeaf) {
        List<Integer> path = new ArrayList<>();
        while (baseLeaf >= 0) {
            if (baseLeaf < totalNodes) path.add(baseLeaf);
            if (baseLeaf == 0) break;
            baseLeaf = (baseLeaf -1 ) / 2;
        }
        return path;
    }

    public boolean hasEmptyBlockAlongPath(List<Integer> LeafToRootPathOnLayerTree) {
        for(int node_index : LeafToRootPathOnLayerTree){
            if(buckets[node_index].hasFreeBlock()){
                return  true;
            }
        }
        return false;
    }

    public Bucket getBucket(int bucketIndex) {
        return this.buckets[bucketIndex];
    }

    public Map<Integer, Bucket> getBucketsFromLeafToRoot(int baseLeaf) {
        List<Integer> leafToRootPath = getLeafToRootPath(baseLeaf);
        Map<Integer, Bucket> buckets = new HashMap<>();
        for (Integer nodeIndex : leafToRootPath) {
            buckets.put(nodeIndex,this.buckets[nodeIndex]);
        }
        return buckets;
    }

    public void setBucketAt(int index, Bucket bucket) {
        if (index >= 0 && index < buckets.length) {
            buckets[index] = bucket;
        } else {
            throw new IndexOutOfBoundsException("Bucket index " + index + " out of bounds.");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" [depth=").append(depth)
                .append(", totalNodes=").append(totalNodes).append("]\n");

        int level = 0;
        int count = 0;
        int nodesAtLevel = 1;

        for (int i = 0; i < buckets.length; i++) {
            if (count == 0) {
                sb.append("    Height ").append(level).append(": ");
            }

            sb.append("Bucket").append(i).append("{").append(buckets[i]).append("}");

            count++;
            if (count == nodesAtLevel) {
                sb.append("\n");
                level++;
                count = 0;
                nodesAtLevel *= 2;
            } else {
                sb.append("  ");
            }
        }

        return sb.toString();
    }
}
