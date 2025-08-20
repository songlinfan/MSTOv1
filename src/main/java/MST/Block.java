package MST;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;


public class Block implements Serializable {
    private static final long serialVersionUID = 1L;

    private String keyword;
    private List<UUID> uuids;
    private int counter = 0;

    private int priority = 2;

    public Block() {}

    /**
     * A block is the smallest structural unit of the Multi-layer Stacked Tree.
     * Its format is defined as (keyword, uuids, counter).
     * <p>
     * A Block instance can only be created using this parameterized constructor.
     */
    public Block(String keyword, List<UUID> uuids) {
        this.keyword = keyword;
        this.uuids = uuids;
    }

    public String getKeyword(){
        return keyword;
    }
    public List<UUID> getUuids(){
        return uuids;
    }
    public int getPriority() { return priority; }

    public void updateCounter(){
        counter++;
    }

    public void resetPriority(int priority){
        this.priority = priority;
    }

    /**
     * Returns a string representation of this block.
     * The simple format is: {@code keyword=...||uuids.size=...||counter=...} <br>
     * The complete format is: {@code keyword=...||uuids=[...]||counter=...}
     * You can switch between formats by replacing the function body.
     *
     * @return a string describing the block
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("keyword=").append(keyword).append("||uuids.size=").append(uuids.size()).append("||counter=").append(counter);
        // sb.append("keyword=").append(keyword).append("||uuids=").append(uuids).append("||counter=").append(counter);
        return sb.toString();
    }
}
