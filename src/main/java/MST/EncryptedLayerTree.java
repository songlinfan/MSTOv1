package MST;

import common.Crypt;
import common.SecretKeyGenerator;

import javax.crypto.SecretKey;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EncryptedLayerTree implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<byte[]> buckets = new ArrayList<byte[]>();
    private int depth = 0;

    public EncryptedLayerTree(LayerTree layerTree)  {
        SecretKey key = SecretKeyGenerator.loadKey();

        Bucket[] buckets = layerTree.getBuckets();
        for(int i = 0; i < buckets.length; i++){
            Bucket bucket = buckets[i];
            this.buckets.add(Crypt.encryptBucket(bucket,key));
        }
        this.depth = layerTree.getDepth();
    }

    public List<byte[]> getBuckets(){
        return this.buckets;
    }

    public Map<Integer,byte[]> getBucketsFromLeafToRoot(int baseLeaf){
        int totalNodes = (int) Math.pow(2, depth + 1) - 1;
        List<Integer> leafToRoot = new ArrayList<>();
        while (baseLeaf >= 0) {
            if (baseLeaf < totalNodes) leafToRoot.add(baseLeaf);
            if (baseLeaf == 0) break;
            baseLeaf = (baseLeaf -1 ) / 2;
        }
        Map<Integer,byte[]> buckets = new HashMap<>();

        for(int i = leafToRoot.size()-1; i >=0; i--){
            int nodeIndex = leafToRoot.get(i);
            buckets.put(nodeIndex,this.buckets.get(nodeIndex));
        }
        return buckets;
    }

    public void updateBucketsFromLeafToRoot(Map<Integer,byte[]> buckets){
        for (Map.Entry<Integer, byte[]> entry : buckets.entrySet()) {
            int nodeIndex = entry.getKey();
            byte[] bucket = entry.getValue();

            this.buckets.set(nodeIndex, bucket);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < this.buckets.size(); i++){
            sb.append(" [");
            byte[] bucketEncryptedContent = this.buckets.get(i);

            sb.append(i).append(":").append(bucketEncryptedContent);
//            for (byte b : bucketEncryptedContent) {
//                sb.append(String.format("%02x", b)); // 保证两位十六进制
//            }
            sb.append("] ");
        }
        return sb.toString();
    }
}
