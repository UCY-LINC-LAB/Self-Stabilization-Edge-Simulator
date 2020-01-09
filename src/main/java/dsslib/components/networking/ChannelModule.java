package dsslib.components.networking;

import dsslib.process.IProcess;
import dsslib.components.AbstractModule;
import dsslib.events.Event;
import dsslib.events.EventHandler;
import dsslib.exceptions.MissingModule;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import static dsslib.components.networking.NetworkModule.*;

public abstract class ChannelModule extends AbstractModule {
    protected final IProcess from;
    protected final IProcess to;
    protected final int bandwidth;

    protected Queue<ReceiveMsg> msgs = new LinkedList<>();

    //public abstract EventHandler<Transmit> onTransmit();
    public abstract EventHandler<ReceiveMsg> onReceiveMsg();

    public static final class Transmit extends Event {
        private final Object msg;
        private final String ipFrom;

        public Transmit(Object msg, String ipFrom){
            this.msg = msg;
            this.ipFrom = ipFrom;
        }

        public String getIpFrom() {
            return ipFrom;
        }

        public Object getMsg() {
            return msg;
        }


        @Override
        public Event clone() {
            return new Transmit(msg, ipFrom);
        }
    }
    public ChannelModule(IProcess process, Map<String,Object> props) throws MissingModule {
        super(process,props);
        this.from = (IProcess) props.get("from");
        this.to = (IProcess) props.get("to");
        this.bandwidth = (int) props.get("bandwidth");
        //All transmit events ipFrom the eventBus of process ipFrom are forwarded
        //bindOn(onTransmit(),Transmit.class,ipFrom.getEventBus(),process.getEventBus());
        //subscribe(onTransmit(),Transmit.class);
        subscribe(onReceiveMsg(), ReceiveMsg.class);
    }

    public String getFromIp() {
        return from.getUuid();
    }

    public String getToIp() {
        return to.getUuid();
    }

    @Override
    public Map<String, Object> getState() {
        Map<String,Object> state  = new HashMap<>();

        state.put("msgs",msgs);

        return state;
    }
}
