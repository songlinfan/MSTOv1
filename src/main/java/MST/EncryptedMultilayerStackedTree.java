package MST;

import common.Config;
import common.InitDataHelper;
import common.Crypt;
import common.SecretKeyGenerator;

import javax.crypto.SecretKey;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EncryptedMultilayerStackedTree implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<Integer,EncryptedLayerTree> EncryptedLayerTrees = new HashMap<>();
    public EncryptedMultilayerStackedTree() { }
    public EncryptedMultilayerStackedTree(MultilayerStackedTree mst)  {
        Map<Integer, LayerTree> layerTrees = mst.getLayerTrees();
        for (Map.Entry<Integer, LayerTree> entry : layerTrees.entrySet()) {
            int layer = entry.getKey();
            LayerTree layerTree = entry.getValue();
            EncryptedLayerTree encryptedLayerTree = new EncryptedLayerTree(layerTree);
            EncryptedLayerTrees.put(layer, encryptedLayerTree);
        }
    }

    public Map<Integer,Map<Integer,byte[]>> getBucketsAcrossAllLayersFromLeafToRoot(int baseLeaf){
        Map<Integer,Map<Integer,byte[]>> response = new HashMap<>();
        for (Map.Entry<Integer, EncryptedLayerTree> entry : EncryptedLayerTrees.entrySet()) {
            int layer = entry.getKey();
            EncryptedLayerTree encryptedLayerTree = entry.getValue();
            Map<Integer,byte[]> buckets = encryptedLayerTree.getBucketsFromLeafToRoot(baseLeaf);
            response.put(layer, buckets);
        }
        return response;
    }

    public void addEncryptedLayerTree(int layer, EncryptedLayerTree layerTree){
        EncryptedLayerTrees.put(layer, layerTree);
    }

    public void updateBucketsAcrossAllLayersFromLeafToRoot(Map<Integer,Map<Integer,byte[]>> updatedResponse){
        for (Map.Entry<Integer, Map<Integer,byte[]>> entry : updatedResponse.entrySet()) {
            int layer = entry.getKey();
            Map<Integer,byte[]> buckets = entry.getValue();
            EncryptedLayerTree encryptedLayerTree = EncryptedLayerTrees.get(layer);
            encryptedLayerTree.updateBucketsFromLeafToRoot(buckets);
        }
    }

    public void Decrypt(SecretKey key) {
        EncryptedLayerTree encryptedLayerTree = EncryptedLayerTrees.get(0);
        List<byte[]> buckets = encryptedLayerTree.getBuckets();
        for (byte[] bucket : buckets) {
            Bucket b =Crypt.decryptBucket(bucket,key);
            System.out.println(b);
        }

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<Integer, EncryptedLayerTree> entry : EncryptedLayerTrees.entrySet()){
            int layer = entry.getKey();
            EncryptedLayerTree encryptedLayerTree = entry.getValue();
            sb.append("layer:" + layer).append(" | {").append(encryptedLayerTree).append("} \n");
        }
        return sb.toString();
    }

}
