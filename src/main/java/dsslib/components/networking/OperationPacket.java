package dsslib.components.networking;

import dsslib.selfstabilization.CloudletList;
import dsslib.selfstabilization.cloudlets.Replica;
import dsslib.scenarios.ScenarioDescriptionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class OperationPacket {
    private static final Logger logger = LoggerFactory.getLogger(OperationPacket.class);
    private final String operation;
    private final PacketPlane plane;
    private final Object payload;
    private int size;

    public OperationPacket(String operation, PacketPlane plane, Object payload) {
        this.operation = operation;
        this.plane = plane;
        this.payload = payload;
    }

    public int getSize() {
        //TODO: Calculate Size:
        switch (plane){
            case CONTROL:
                int size = 40; //header
                if(operation.equals("CLD")) {
                    OperationPacket p = (OperationPacket) payload;
                    if (p.getPayload() instanceof CloudletList) {
                        CloudletList s = (CloudletList) p.getPayload();
                        size += s.getCloudlets().size() * 10;
                    }
                }else if(operation.equals("REGISTER")) {
                    size += payload.toString().length();
                }else if(operation.equals("SSL")){
                    if(payload instanceof Replica){
                        Replica r = (Replica) payload;
                        return 40+r.calculateSize();
                    }else{
                        logger.info("Shouldn't happen, Replica not found");
                    }
                }else{
                    //SEQ ACKs
                    size+= payload.toString().length();
                }
                return size;
            case SHARED_REGISTER_WRITE:
                return 40+payload.toString().length();
            case SHARED_REGISTER_READ:
                return 40+payload.toString().length();
            case HEALTH_CHECK:
                return 40+ payload.toString().length();
            case DATA:
                //Actually also aggregate  should be the same....
                if(payload instanceof Set){
                    return ScenarioDescriptionLoader.getScenarioDescription().getAggregateSize();
                }
                int update = ScenarioDescriptionLoader.getScenarioDescription().getIotUpdate();
                if(update<1000)
                    return ScenarioDescriptionLoader.getScenarioDescription().getAggregateSize();
                return (int) (ScenarioDescriptionLoader.getScenarioDescription().getAggregateSize()*(update/1000.0));
        }


        return size;
    }

    public String getOperation() {
        return operation;
    }

    public PacketPlane getPlane() {
        return plane;
    }

    public Object getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "{'operation':'" + operation + '\''
                + ", 'plane':'" + plane + '\''
                + ", 'payload':'" + payload + "\'}";
    }
}
