package dsslib.components.selfstabilization;

import dsslib.process.IProcess;
import dsslib.components.networking.NetworkModule;
import dsslib.components.networking.OperationPacket;
import dsslib.components.networking.PacketPlane;
import dsslib.components.timers.PeriodicTimerModule;
import dsslib.scheduler.Scheduler;
import dsslib.events.EventHandler;
import dsslib.events.Execution;
import dsslib.events.Init;
import dsslib.exceptions.MissingModule;
import dsslib.exceptions.MissingScenario;
import dsslib.selfstabilization.CloudletList;
import dsslib.selfstabilization.CloudletWithPriority;
import dsslib.selfstabilization.IoTMsgToCloudlet;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

public class SSIoTComponent extends SSIoTModule {
    String periodUUID = getProcess().getUuid()+"-"+ UUID.randomUUID().toString();
    @Override
    public EventHandler<CloudletModelMsg> onCloudletModelMsg() {
        return (e)->{
            logToScheduler("CloudletModelMsg received");
            String j = e.getCloudlet();

            //Line 36
            long msgJ = MSG.getOrDefault(e.getCloudlet(),0L);
            if(e.getSeq()>msgJ){

                cloudletList = new PriorityQueue<>(e.getCloudletList());
                cloudletModel = e.getCloudletModel();
                lastUpdate = getProcess().getLocalTime();

                logToScheduler("\tNew list: "+cloudletList);
                logToScheduler("\tNew model: "+cloudletModel);

                //Line 31. Make sure that is exactly what it says
                MSG.put(j,e.getSeq());

            }else{
                logToScheduler("The msg received is ignored since sequence "+e.getSeq()+" is lower than "+msgJ);
            }

            //Updating msg seq:
            msgseq  = Math.max(msgseq, e.getSeq());
            logToScheduler("msgseq updated to: "+msgseq);

            trigger(new NetworkModule.SendMsg(
                    new OperationPacket("IOTSEQ", PacketPlane.CONTROL,MSG.getOrDefault(j,0L)),
                    j)
            );


        };
    }

    @Override
    public EventHandler<PeriodicTimerModule.PeriodExpired> onPeriodExpired() {
        return (e)->{

            if(!e.getUuid().equals(periodUUID))
                return;

            if(this.getProcess().getLocalTime()-modelUpdate>modelUpdatePeriodicity) {
                try {
                    model = (int) (Scheduler.getInstance().getScenario().getRandom().nextGaussian()*10+30);
                    Scheduler.getInstance().getVirtualFogTopolgy().updateRealState(getProcess().getUuid(),model);
                } catch (MissingScenario missingScenario) {
                    missingScenario.printStackTrace();
                }
                modelUpdate = getProcess().getLocalTime();
            }

            String cloudID = Scheduler.getInstance().getVirtualFogTopolgy().getCloudIP();
            if(timingFailure()){
                //Initializing
                iotInit();
                logToScheduler("Timing Failure occurred. Sending to cloud to register");
                trigger(new NetworkModule.SendMsg(new OperationPacket("REGISTER",
                        PacketPlane.CONTROL, "I am iot "+getProcess().getUuid()
                        +" at "+getProcess().getLocalTime()), cloudID));

            }else if(update()){
                if(cloudletList==null || cloudletList.isEmpty())
                    logToScheduler("Need for update, but no cloudlets are found");
                else {

                    if(!model.equals(cloudletModel)) {
                        logToScheduler("CloudletModel is different from the current model");
                    }

                    //Send to cloudlets
                    // All messages have the same msgseq.
                    boolean atLeastOneSent=false;
                    for(CloudletWithPriority c: cloudletList) {
                        OperationPacket packet = new OperationPacket("MODEL", PacketPlane.DATA, new IoTMsgToCloudlet(msgseq, model));
                        trigger(new NetworkModule.SendMsg(packet, c.getId()));
                        logToScheduler("Sending model to "+c.getId());
                        atLeastOneSent = true;
                    }
                    if(atLeastOneSent)
                        msgseq++;

                }

            }

        };
    }

    @Override
    public EventHandler<NetworkModule.NewMessageReceived> onNewMessage() {
        return (msg->{
            //Ask If should lose the messages if am not enabled
            if(!this.getProcess().isEnabled())
                return;

            OperationPacket packet = msg.getPacket();

            //Line 33
            if(packet.getOperation().equals("IOTSEQ")){
                long mseq = (long) packet.getPayload();
                msgseq=Math.max(msgseq,mseq);
                logToScheduler("Msg seq, changed to: " +mseq);
                return;
            }


            if(!packet.getOperation().equals("CLD")) {
                logToScheduler("Not a CLD msg");
            }


            //Line 28
            packet = (OperationPacket) packet.getPayload();

            //logToScheduler("Received packet: "+packet);
            CloudletList cloudletList = (CloudletList) packet.getPayload();
            String cloudletID = msg.getIpFrom();
            trigger(new CloudletModelMsg(cloudletID, cloudletList.getSeq(), cloudletList.getModel(), cloudletList.getCloudlets()));

        });
    }

    @Override
    public boolean timingFailure() {
        return (getProcess().getLocalTime() - lastUpdate) >= timeout;

    }

    @Override
    public void iotInit() {
        //model = 0;
        cloudletModel = 0 ;
        cloudletList = new PriorityQueue<>();
        lastUpdate=0;
        msgseq =-1;
    }

    long modelUpdate=0;

    @Override
    public boolean update() {
        return !model.equals(cloudletModel);
    }

    public SSIoTComponent(IProcess process, Map<String, Object> props) throws MissingModule {
        super(process,props);
    }


    @Override
    public EventHandler<Init> onInit() {
        return (e)->{
            trigger(new PeriodicTimerModule.PeriodStart( periodUUID,periodicity));
        };
    }

    @Override
    public EventHandler<Execution> onExecution() {
        return (e)->{

        };
    }
}
