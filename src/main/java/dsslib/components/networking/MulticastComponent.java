package dsslib.components.networking;

import dsslib.process.IProcess;
import dsslib.events.EventHandler;
import dsslib.events.Execution;
import dsslib.events.Init;
import dsslib.exceptions.MissingModule;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class MulticastComponent extends MulticastModule {

    private Queue<Msg> msgs = new LinkedList<>();

    @Override
    public EventHandler<Init> onInit() {
        return (e)->{

        };
    }

    @Override
    public EventHandler<Execution> onExecution() {
        return (e)->{

        };
    }

    @Override
    protected EventHandler<Fetch> fetch() {
        return (e)->{
            //When the fetch event is triggered, we make a request to fetch the next input message.
            logToScheduler("Received request to fetch the next available msg");

            FetchResponse fetchResponse ;
            if(!msgs.isEmpty()){
                Msg resp = msgs.poll();
                fetchResponse= new FetchResponse(e.getWhoCalled(), resp.getSrc(),resp.getPacket());
            }else{
                fetchResponse = new FetchResponse(e.getWhoCalled(), "",null);
            }

            trigger(fetchResponse);

        };
    }

    @Override
    protected EventHandler<MultiCastMsg> onSendMulticastMsg() {
        return (e)->{
            Collection<String> addresses = e.getAddresses();
            for(String addr: addresses){

                OperationPacket packet = new OperationPacket("MCM",PacketPlane.DATA,e.getPayload());
                trigger(new NetworkModule.SendMsg(packet,addr));
            }
        };
    }

    @Override
    protected EventHandler<NetworkModule.NewMessageReceived> onNewMessage() {
        return (e)->{

            //Ask If should loose the messages if am not enabled
            if(!this.getProcess().isEnabled())
                return;

            if(e.getPacket().getOperation().equals("MCM")){
                msgs.add(new Msg(e.getIpFrom(),e.getPacket()));
            }
        };
    }

    public MulticastComponent(IProcess process) throws MissingModule {
        super(process, new HashMap<>());
    }
}
