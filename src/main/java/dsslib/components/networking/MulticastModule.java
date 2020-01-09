package dsslib.components.networking;

import dsslib.process.IProcess;
import dsslib.components.AbstractModule;
import dsslib.events.Event;
import dsslib.events.EventHandler;
import dsslib.exceptions.MissingModule;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class MulticastModule extends AbstractModule {


    protected abstract EventHandler<Fetch> fetch();
    //protected abstract EventHandler<FetchEvent> onFetchResponse();

    protected abstract EventHandler<MultiCastMsg> onSendMulticastMsg();

    protected abstract EventHandler<NetworkModule.NewMessageReceived> onNewMessage();


    public static final class Fetch extends Event {
        private String whoCalled;
        public Fetch(String whoCalled){

            this.whoCalled = whoCalled;
        }

        public String getWhoCalled() {
            return whoCalled;
        }
    }
    public static final class FetchResponse extends Event{
        private String whoCalled;
        private String src;
        private OperationPacket packet;

        public FetchResponse(String whoCalled, String src, OperationPacket packet) {
            this.whoCalled = whoCalled;
            this.src = src;
            this.packet = packet;
        }

        public String getWhoCalled() {
            return whoCalled;
        }

        public String getSrc() {
            return src;
        }

        public void setSrc(String src) {
            this.src = src;
        }

        public OperationPacket getPacket() {
            return packet;
        }

        public void setPacket(OperationPacket packet) {
            this.packet = packet;
        }
    }

    public static final class MultiCastMsg extends Event{
        private Collection<String> addresses;
        private Object payload;

        public MultiCastMsg(Collection<String> addresses, Object payload) {
            this.addresses = addresses;
            this.payload = payload;
        }

        public Collection<String> getAddresses() {
            return addresses;
        }

        public Object getPayload() {
            return payload;
        }
    }

    protected static final class Msg{
        private String src;
        OperationPacket packet;

        public Msg(String src, OperationPacket packet) {
            this.src = src;
            this.packet = packet;
        }

        public String getSrc() {
            return src;
        }

        public void setSrc(String src) {
            this.src = src;
        }

        public OperationPacket getPacket() {
            return packet;
        }

        public void setPacket(OperationPacket packet) {
            this.packet = packet;
        }
    }


    public MulticastModule(IProcess process, Map<String, Object> props) throws MissingModule {
        super(process, props);
        require(NetworkModule.class);
        subscribe(onNewMessage(),NetworkModule.NewMessageReceived.class);
        subscribe(fetch(), Fetch.class);
        subscribe(onSendMulticastMsg(), MultiCastMsg.class);

    }

    @Override
    public Map<String, Object> getState() {
        Map<String,Object> res = new HashMap<>();
        return res;
    }
}
