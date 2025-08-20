package MST;

import common.Config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A bucket represents a node in the Multi-layer Stacked Tree (MST).
 * Each bucket holds a collection of blocks, containing up to {@code freeBlockCount} blocks.
 * The {@code layer} field indicates the tree layer this bucket belongs to.
 * <p>
 * In a bucket at layer {@code i}, each block stores {@code 2^i} UUIDs.
 */

public class Bucket implements Serializable {
    private static final long serialVersionUID = 1L;

    private int layer;
    private List<Block> blocks = new ArrayList<>();
    private int freeBlockCount = Integer.parseInt(Config.get("bucketCapacity"));


    public Bucket( ) {}
    public Bucket(int layer) {
        this.layer = layer;
    }

    public int getLayer(){return layer;}
    public List<Block> getBlocks(){
        return blocks;
    }

    public boolean hasFreeBlock() {
        return freeBlockCount > 0;
    }


    /**
     * Clears all blocks within the bucket and resets {@code freeBlockCount} accordingly.
     * <p>
     * This method is used to update (initialize) a path that has been read from the server.
     */
    public void clear() {
        blocks.clear();
        freeBlockCount = Integer.parseInt(Config.get("bucketCapacity"));
    }
    public void padding(){
        String fakeKeyword = "Dummy";
        UUID fakeUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
        List<UUID> fakeChunk = new ArrayList<>();
        for(int i =0; i<(int)Math.pow(2, layer); i++){
            fakeChunk.add(fakeUuid);
        }
        while(hasFreeBlock()){
            insertBlock(fakeKeyword,fakeChunk);
        }
    }
    public void insertBlock(String keyword, List<UUID> chunk) {
        if(chunk.size() != (int)Math.pow(2, layer)) {
            throw new IllegalArgumentException(
                    "Invalid chunk size: expected " + (int)Math.pow(2, layer) + ", but got " + chunk.size());
        }else{
            Block block = new Block(keyword,chunk);
            blocks.add(block);
            freeBlockCount--;
        }
    }

    public void insertBlock(Block block) {
        if(block.getUuids().size() != (int)Math.pow(2, layer)) {
            throw new IllegalArgumentException(
                    "Invalid chunk size: expected " + (int)Math.pow(2, layer) + ", but got " + block.getUuids().size());
        }else{
            blocks.add(block);
            freeBlockCount--;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (Block block : blocks) {
            sb.append("(").append(block.toString());
            if(block != blocks.get(blocks.size()-1)) {
                sb.append("),");
            }else{
                sb.append(")");
            }
        }
        sb.append("],");
        return sb + "freeBlockCount=" + freeBlockCount;
    }
}