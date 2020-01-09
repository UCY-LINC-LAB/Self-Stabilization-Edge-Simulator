package dsslib.components.networking;


import dsslib.process.IProcess;
import dsslib.events.EventHandler;
import dsslib.events.Execution;
import dsslib.events.Init;
import dsslib.exceptions.MissingModule;

import java.util.Map;

/**
 * PortA
 *   onReceive: Should transmit to PortB
 * PortB
 *   onReceive: Should transmit to PortA
 *
 */
public class ChannelComponent extends ChannelModule {


    @Override
    public EventHandler<NetworkModule.ReceiveMsg> onReceiveMsg() {
        return (msg)->{
            //logToScheduler("ReceiveMsg from: "+msg.getIpFrom());
            msgs.add(msg);
        };
    }

    public ChannelComponent(IProcess process, Map<String,Object> props) throws MissingModule {
        super(process, props);
    }

    @Override
    public EventHandler<Init> onInit() {
        return (e)->{};
    }

    @Override
    public EventHandler<Execution> onExecution() {
        return (e)->{
            int count =0;
            while(!msgs.isEmpty() && count < bandwidth){
                NetworkModule.ReceiveMsg msg = msgs.poll();
                String fromIp = msg.getIpFrom();

                if(fromIp.equals(from.getUuid())){
                    logToScheduler("Triggering a receiveMsg to: "+fromIp);
                    //Last change
                    if(to.isEnabled())
                        to.trigger(new NetworkModule.ReceiveMsg(msg.getMsg(),msg.getIpFrom()));
                }
            }

        };
    }
}
