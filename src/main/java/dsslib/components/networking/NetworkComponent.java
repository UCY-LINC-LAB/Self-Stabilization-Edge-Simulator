package dsslib.components.networking;

import dsslib.process.IProcess;
import dsslib.events.EventHandler;
import dsslib.events.Execution;
import dsslib.exceptions.MissingModule;
import dsslib.statistics.Statistics;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class NetworkComponent extends NetworkModule {

    private Queue<SendMsg> outputBuffer = new LinkedList<>();
    private Queue<ReceiveMsg> inputBuffer = new LinkedList<>();


    public void registerChannel(IProcess process){
        this.channels.put(process.getUuid(),process);
    }
    @Override
    public EventHandler<SendMsg> onSendMsg() {
        return (msg) -> {
            //logToScheduler("Sending msg(" + msg.getOperationPacket() + ") to " + msg.getIpTo());
            outputBuffer.add(msg);

            HashMap<String, Object> map = new HashMap<String, Object>() {
                {
                    put("from", getProcess().getUuid());
                    put("to", msg.getIpTo());
                    put("packet", msg.getOperationPacket());
                }
            };

            Statistics.getInstance().record("network", map);
        };
    }

    @Override
    public EventHandler<ReceiveMsg> onReceiveMsg() {
        return (msg)->{
            //logToScheduler("Received msg("+msg.getMsg()+") from "+msg.getIpFrom());
            inputBuffer.add(msg);
        };
    }

    @Override
    public EventHandler<Execution> onExecution() {
        return (e)->{


            //logToScheduler("Checking ReceiveBuffer");
            int countReceived = 0;
            while (!inputBuffer.isEmpty() && (countReceived< receiveBandwidth)){
                ReceiveMsg msg = inputBuffer.poll();
                OperationPacket m = (OperationPacket) msg.getMsg();
                String ip = msg.getIpFrom();
                //logToScheduler("Processing a new msg "+m+" from "+ip);
                trigger(new NewMessageReceived(m,ip));
                countReceived++;

            }

            //logToScheduler("Checking OutputBuffer");
            int countSent = 0;
            while (!outputBuffer.isEmpty() && (countSent< transmitBandwidth)){
                SendMsg msg = outputBuffer.poll();
                Object payload = msg.getOperationPacket();
                String ip = msg.getIpTo();


                IProcess channelHost = channels.get(this.getProcess().getUuid()+"_"+ip);
                if(channelHost==null){
                    logToScheduler("No channel for host "+ip);
                }else{
                    //Should trigger ReceiveMsg of the respective channel
                    channelHost.trigger(new NetworkModule.ReceiveMsg(payload, this.getProcess().getUuid()));
                }
            }


        } ;
    }

    @Override
    public Map<String, Object> getState() {
        Map<String,Object> res = new HashMap<>();
        res.put("InputBuffer",inputBuffer);
        res.put("OutputBuffer",outputBuffer);

        return res;
    }

    /**
     *
     * @param process
     * @param props
     */
    public NetworkComponent(IProcess process,Map<String,Object> props) throws MissingModule {
        super(process,props);
    }

}
