package dsslib.components.networking;


import dsslib.process.IProcess;
import dsslib.components.AbstractModule;
import dsslib.events.Event;
import dsslib.events.EventHandler;
import dsslib.events.Init;
import dsslib.exceptions.MissingModule;

import java.util.*;

public abstract class NetworkModule extends AbstractModule {

    /**
     * Request to send a new message
     * @return
     */
    public abstract EventHandler<SendMsg> onSendMsg();

    /**
     * Catches a signal that a msg has been sent to me
     * @return
     */
    public abstract EventHandler<ReceiveMsg> onReceiveMsg();

    public abstract void registerChannel(IProcess process);

    protected final int receiveBandwidth;
    protected final int transmitBandwidth;

    Map<String,IProcess> channels = new HashMap<>();
    public Set<String> connectedProcesses(){
        Set<String> res = new LinkedHashSet<>();
        Collection<IProcess> values = channels.values();
        for(IProcess p : values){
            try {
                ChannelModule c = (ChannelModule) p.getModule(ChannelModule.class);
                res.add(c.to.getUuid());
            } catch (MissingModule missingModule) {
                missingModule.printStackTrace();
            }
        }

        return res;
    }

    @Override
    public EventHandler<Init> onInit() {
        return (e)->{};
    }

    /**
     * REQUEST EVENT
     * Used for Sending a msg to a channel
     */
    public static final class SendMsg extends Event {
        private final OperationPacket operationPacket;
        private String ipTo;

        public SendMsg(OperationPacket payload, String ipTo) {
            this.operationPacket = payload;
            this.ipTo = ipTo;
        }


        public OperationPacket getOperationPacket() {
            return operationPacket;
        }

        public String getIpTo() {
            return ipTo;
        }

    }

    /**
     * REQUEST EVENT
     * A signal caught from the underlying layers indicating that I have a message
     */
    public static final class ReceiveMsg extends Event {
        private Object msg;
        private String ipFrom;

        public ReceiveMsg(Object msg, String ipFrom) {
            this.msg = msg;
            this.ipFrom = ipFrom;
        }

        public Object getMsg() {
            return msg;
        }

        public String getIpFrom() {
            return ipFrom;
        }

    }

    /**
     * A signal that is sent to upper layers for notifying that a msg has been received
     */
    public static class NewMessageReceived extends Event {
        private final OperationPacket packet;
        private final String ipFrom;

        public NewMessageReceived(OperationPacket packet, String from) {
            this.packet = packet;
            this.ipFrom = from;
        }

        public OperationPacket getPacket() {
            return packet;
        }

        public String getIpFrom() {
            return ipFrom;
        }
    }

    public NetworkModule(IProcess process, Map<String,Object> props) throws MissingModule {
        super(process,props);
        this.receiveBandwidth = (int) props.get("receiveBandwidth");
        this.transmitBandwidth = (int) props.get("transmitBandwidth");
        subscribe(onSendMsg(),SendMsg.class);
        subscribe(onReceiveMsg(),ReceiveMsg.class);
    }

    @Override
    public Map<String, Object> getState() {
        Map<String,Object> state  = new HashMap<>();
        return state;
    }

    public Map<String, IProcess> getChannels() {
        return channels;
    }
}
