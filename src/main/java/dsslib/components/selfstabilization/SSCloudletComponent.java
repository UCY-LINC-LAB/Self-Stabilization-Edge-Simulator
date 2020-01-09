package dsslib.components.selfstabilization;

import dsslib.process.IProcess;
import dsslib.components.networking.NetworkModule;
import dsslib.components.networking.OperationPacket;
import dsslib.components.networking.PacketPlane;
import dsslib.components.networking.SharedRegisterModule;
import dsslib.components.timers.PeriodicTimerModule;
import dsslib.scheduler.Scheduler;
import dsslib.events.EventHandler;
import dsslib.events.Execution;
import dsslib.events.Init;
import dsslib.exceptions.MissingModule;
import dsslib.selfstabilization.*;
import dsslib.utilities.Tuple2;
import dsslib.utilities.Tuple3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SSCloudletComponent extends SSCloudletModule{
    private static final Logger logger = LoggerFactory.getLogger(SSCloudletComponent.class);


    @Override
    public EventHandler<Init> onInit() {
        return (e)->{
            uuidForPeriodicTimer = getProcess().getUuid()+"-"+ UUID.randomUUID().toString();
            trigger(new PeriodicTimerModule.PeriodStart(uuidForPeriodicTimer,periodicity));

        };
    }


    @Override
    public EventHandler<PeriodicTimerModule.PeriodExpired> onPeriodTimeout() {

        return (e)->{
            //Execute only when the periodic timer is for me
            String uuid = e.getUuid();
            if(uuidForPeriodicTimer.equals(uuid))
                trigger(new SharedRegisterModule.Read("info",uuidForPeriodicTimer));


        };
    }

    @Override
    public EventHandler<SharedRegisterModule.ReadResponse> onReadResponse() {
        return (ret)->{
            if(!ret.getUuid().equals(uuidForPeriodicTimer))
                return;

            String key = ret.getKey();

            if(!key.equals("info")){
                return;
            }

            Object result = ret.getValue();

            lInfo = (RegisterInfoModel) result;
            if(lInfo==null)
                lInfo= new RegisterInfoModel();

            lDevices  = lInfo.getDevices();
            lCloudlets = lInfo.getCloudlets();
            lLeader = lInfo.getLeader();
            lGuards = lInfo.getGuards();

            String i = this.getProcess().getUuid();
            trigger(new SSCloudletLeaderModule.ResetLeaderShip());

            //Line 48. If I am not in the view of the cloud....
            if(!lCloudlets.containsKey(i)) {
                cloudletInit();
            }else{

                //Line 50: Neither a guard nor a leader
                if (!lGuards.contains(i) && !lLeader.getId().equals(i)) {
                    aggregateInfo = new HashMap<>();
                    MSGc = new HashMap<>();
                    // Trigger Reset leadership
                    trigger(new SSCloudletLeaderModule.ResetLeaderShip());

                }
                if (lLeader.getId().equalsIgnoreCase(i)) {
                    trigger(new SSCloudletLeaderModule.UpdateLeadership(SSCloudletLeaderModule.Type.LEADER, true));
                } else if(lGuards.contains(i)){
                    trigger(new SSCloudletLeaderModule.UpdateLeadership(SSCloudletLeaderModule.Type.GUARD, true));
                }

                //TODO Test the removeIf functionaility
                //Line 51: Update deviceSet, remove the ones not in the ldevices
                deviceSet.entrySet().removeIf(e -> !lDevices.containsKey(e.getKey()));

                //Line 52: Remove the sequence entries of that are not in the device set anymore
                MSGSEQ.entrySet().removeIf(e -> !deviceSet.containsKey(e.getKey()));

                //Line 53
                MSGc.entrySet().removeIf(e -> !lCloudlets.containsKey(e.getKey()));


                //Line 54
                int iotAdd = 0;
                int msgAdd = 0;

                //Line 55: For every IoT in my responsibility
                for (String iot : myIoT(lDevices.keySet(), lCloudlets)) {

                    IoTModelTime iotModel = deviceSet.getOrDefault(iot, new IoTModelTime(iot, -1, null));
                    //TODO Check which model
                    Object m = iotModel.getModel();

                    PriorityQueue<CloudletWithPriority> cloudletList = cloudletList(iot);
                    CloudletList clist = new CloudletList(msgtoiot, m, cloudletList);

                    OperationPacket packet = new OperationPacket("", PacketPlane.CONTROL, clist);
                    OperationPacket enc = new OperationPacket("CLD", PacketPlane.CONTROL, packet);

                    trigger(new NetworkModule.SendMsg(enc, iot));
                    logToScheduler("Sending the cloudletList: " + clist.getCloudlets() + " to iot:" + iot +" with model: "+m);
                    iotAdd = 1;
                }

                //Line 56: For every guard and then the leader....
                Set<Tuple3> aggregate = aggregate(deviceSet);
                for (String guard : lGuards) {
                    OperationPacket packet = new OperationPacket(msgc + "", PacketPlane.DATA, aggregate);
                    trigger(new NetworkModule.SendMsg(packet, guard));
                    logToScheduler("Sending to guard:" + guard + ", aggregate:" + aggregate);
                    msgAdd=1;
                }
                OperationPacket packet = new OperationPacket(msgc + "", PacketPlane.DATA, aggregate);
                if (!lLeader.getId().isEmpty()){
                    trigger(new NetworkModule.SendMsg(packet, lLeader.getId()));
                    msgAdd=1;
                }

                msgtoiot +=  iotAdd;
                msgc += msgAdd;

            }



        };
    }


    @Override
    public EventHandler<MsgFromIoT> onMsgFromIoT() {
        return (e)->{
            //Line 59

            String iot = e.getFrom();
            Object model = e.getIoTMsgToCloudlet().getModel();
            long seq = e.getIoTMsgToCloudlet().getMsgseq();

            if(seq>MSGSEQ.getOrDefault(iot,0L)){

                IoTModelTime record = new IoTModelTime(iot,getProcess().getLocalTime(),model);
                //Replacement here is necessary
                deviceSet.put(iot,record);
                MSGSEQ.put(iot,seq);
                logToScheduler("Received from IoT: "+ record);
            }else{
                logToScheduler("Receive from IoT, but seq# is older than what we know so far");
            }

            //This happens every time we receive msg from iot
            trigger(new NetworkModule.SendMsg(new OperationPacket("IOTSEQ",PacketPlane.CONTROL,MSGSEQ.getOrDefault(iot,0L)),iot));


        };
    }

    @Override
    public EventHandler<MsgFromCloudlet> onMsgFromCloudlet() {
        return (msg)->{
            String z = msg.getFrom();
            String i = this.getProcess().getUuid();
            if(msg.getMsgSeq()>MSGc.getOrDefault(msg.getFrom(),0L)
                    && (lGuards.contains(i) || lLeader.getId().equals(i))){

                logToScheduler("From "+msg.getFrom()+":"+msg.getAggregateInfo());
                aggregateInfo.put(z,new Tuple2(getProcess().getLocalTime(),msg.getAggregateInfo()));


                MSGc.put(msg.getFrom(),msg.getMsgSeq());
            }

            //SEND to cloudlet back
            //This happens every time we receive msg from iot
            trigger(new NetworkModule.SendMsg(new OperationPacket("CLOUDLETSEQ",PacketPlane.CONTROL,MSGc.getOrDefault(z,0L)),z));
        };
    }

    @Override
    public EventHandler<NetworkModule.NewMessageReceived> onNewMessageReceived() {
        return (e)->{

            //Ask If should lose the messages if am not enabled
            if(!this.getProcess().isEnabled())
                return;

            String from = e.getIpFrom();
            OperationPacket packet = e.getPacket();

            if(from.startsWith("iot_")){
                //Line 69
                if(packet.getOperation().equals("IOTSEQ")){
                    long msgseq = (long) packet.getPayload();
                    msgtoiot  = Math.max(msgtoiot, msgseq);
                    logToScheduler("msgtoiot updated to: "+msgtoiot);
                }else {

                    //For line 59
                    trigger(new MsgFromIoT(from, (IoTMsgToCloudlet) packet.getPayload()));
                }

            }else if(from.startsWith("cloudlet_")){

                //Line 78
               if(packet.getOperation().equals("CLOUDLETSEQ")){
                    long msgseq = (long) packet.getPayload();
                    msgc  = Math.max(msgc, msgseq);
                    logToScheduler("msgc updated to: "+msgc);

               }else if(!packet.getOperation().equals("SS")){

                   if(packet.getOperation().equals("MCM")){
                       //Do nothing... It is handled by the SSCloduletLeader component
                   }else if(packet.getOperation().equals("SSL")) {
                       //Do nothing... It is handled by the SSCloduletLeader component

                   }else{
                       //Here it means that the reception is the aggregate
                       long msgseq = Long.parseLong(packet.getOperation());
                       trigger(new MsgFromCloudlet(from, msgseq, packet.getPayload()));
                   }
               }
            }else{ //We ignore other senders. If it was from cloud, it must be a healthcheck
            }
        };
    }

    @Override
    public Set<Tuple3> aggregate(Map<String, IoTModelTime> deviceSet) {
        Set<Tuple3> agg = new LinkedHashSet<>();
        for (IoTModelTime value : deviceSet.values()) {
            agg.add(new Tuple3(value.getIot(),value.getTime(),value.getModel()));
        }
        return agg;
    }

    @Override
    public EventHandler<SSCloudletLeaderModule.Fetch> onFetch() {
        return (e)->{
            //TODO Check if deep copy is needed
            Map<String, Tuple2> aggregateInfo = new HashMap<>(this.aggregateInfo);
            trigger(new SSCloudletLeaderModule.FetchResponse(e.getWhoCalled(), aggregateInfo));

        };
    }
    @Override
    public PriorityQueue<CloudletWithPriority> cloudletList(String iot) {
        return Scheduler.getInstance().getVirtualFogTopolgy().cloudletList(iot);
    }

    @Override
    public Set<String> myIoT(Set<String> localDevices, Map<String, Integer> lCloudlets) {

        Set<String> iots = Scheduler.getInstance().getVirtualFogTopolgy().reachableIoT(this.getProcess());
        //Removes all elements that are not contained
        iots.retainAll(localDevices);
        return iots;
    }

    @Override
    public void cloudletInit() {
        logToScheduler("Sending to cloud for registration");
        trigger(new NetworkModule.SendMsg(new OperationPacket("REGISTER", PacketPlane.CONTROL, "HELLO"),"cloud"));
    }

    @Override
    public Set<String> suspectedCloudlet(Set<String> cloudlets) {
        return Scheduler.getInstance().getVirtualFogTopolgy().cloudletSuspectedCloudlet(new HashSet<>(cloudlets));
    }

    @Override
    public boolean checkCloudlet(String cloudlet) {

        Set<String> set = new LinkedHashSet<>();
        set.add(cloudlet);
        //Return true if the list contains 1 element
        return !Scheduler.getInstance().getVirtualFogTopolgy().cloudletSuspectedCloudlet(set).isEmpty();
    }

    public SSCloudletComponent(IProcess process, Map<String,Object> props) throws MissingModule {
        super(process, props);
    }

    @Override
    public EventHandler<Execution> onExecution() {
        return (e)->{};
    }

    @Override
    public Map<String, Object> getState() {
        Map<String,Object> state  = new HashMap<>();
        state.put("aggregateInfo",aggregateInfo);
        state.put("lLeader",lLeader);
        state.put("deviceSet",deviceSet);
        state.put("lDevices",lDevices);
        state.put("msgc",msgc);
        state.put("msgtoiot",msgtoiot);
        state.put("MSGc",MSGc);
        state.put("MSGseq",MSGSEQ);

        return state;
    }
}
