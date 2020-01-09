package dsslib.components.selfstabilization;

import dsslib.process.IProcess;
import dsslib.components.healthcheck.HealthCheckModule;
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
import dsslib.exceptions.MissingScenario;
import dsslib.exceptions.SimulationException;
import dsslib.selfstabilization.IoTModelTime;
import dsslib.selfstabilization.RegisterInfoModel;
import dsslib.selfstabilization.SeqIdPair;

import java.util.*;

public class SSCloudComponent extends SSCloudModule {

    protected Map<String,Integer> knownCloudlets = new HashMap<>();

    private String periodUUID = getProcess().getUuid()+"-"+ UUID.randomUUID().toString();

    @Override
    /**
     * On initialization step
     */
    public EventHandler<Init> onInit() {
        return (e)->{
            trigger(new PeriodicTimerModule.PeriodStart(periodUUID ,periodicity));
        };
    }

    @Override
    public EventHandler<Execution> onExecution() {
        return (e)->{ };
    }

    @Override
    public EventHandler<PeriodicTimerModule.PeriodExpired> onPeriodExpired() {
        return (e) -> {

            if(e.getUuid().equals(periodUUID)) {
                //Read sharedMemory
                trigger(new SharedRegisterModule.Read("info", periodUUID));
            }

        };
    }

    @Override
    public EventHandler<SharedRegisterModule.ReadResponse> onReadResponse() {
        return (ret)->{

            //Not destined for me
            if(!periodUUID.equals(ret.getUuid()))
                return;

            String key = ret.getKey();
            Object result = ret.getValue();

            if(!key.equals("info"))
                return;

            logToScheduler("Read Response received for key: "+key);

            lInfo = (RegisterInfoModel) result;
            if(lInfo==null)
                lInfo = new RegisterInfoModel();


            //Line 8: Add lDevices and new IoT. Remove the ones suspected
            //As cloud appends we need to copy
            lDevices = lInfo.getDevices();
            logToScheduler("lDevices:" + lDevices);
            for (Map.Entry<String, IoTModelTime> iotEntry : newIot.entrySet()) {
                lDevices.put(iotEntry.getKey(), iotEntry.getValue());
            }
            newIot.clear();
            //Remove suspected ones
            for (String suspected : suspectedIot(lDevices.keySet())) {
                logToScheduler("Removed device:  "+suspected+" as it is suspected to be faulty");
                lDevices.remove(suspected);
            }


            //Line 9: Add Local cloudlets and new Cloudlets
            lCloudlets = lInfo.getCloudletsCopy();
            logToScheduler("lCloudlets:" + lCloudlets);
            for (Map.Entry<String, Integer> cloudlet : newCloudlet.entrySet()) {
                lCloudlets.put(cloudlet.getKey(), cloudlet.getValue());
            }
            newCloudlet.clear();
            //Remove suspected ones
            for (String suspected : suspectedCloudlet(lCloudlets.keySet())) {
                logToScheduler("Removed cloudlet:  "+suspected+" as it is suspected to be faulty");
                lCloudlets.remove(suspected);
            }

            //Line 10
            lLeader = lInfo.getLeader();

            //If leader is not in the current view of cloudlets we elect new one
            if (!lCloudlets.containsKey(lLeader.getId())) {
                sequence++;
                try {
                    lLeader = new SeqIdPair(sequence, electLeader(lCloudlets.keySet()));
                    logToScheduler("New leader is elected: " + lLeader);
                    //Update Fault Tolerance Mechanism
                    updateFaultToleranceMechanism(Collections.singletonList(lLeader.getId()));

                } catch (SimulationException e) {
                    logToScheduler(e.getLocalizedMessage());
                    System.err.println(e.getLocalizedMessage());
                }
                if (sequence == Integer.MAX_VALUE) {
                    trigger(new SharedRegisterModule.Write("info",null,periodUUID));
                }
            }

            //Line 12 pre-code
            //From all lGuards remove lCloudlets. If the resulted set is not empty select
            //new guards from the lCloudlets set except the lLeader
            Set<String> guardsFromCloudlets = new HashSet<>(lCloudlets.keySet());
            lguards = new HashSet<>(lInfo.getGuards());
            guardsFromCloudlets.retainAll(lguards);

            //Line 12
            if (guardsFromCloudlets.isEmpty()) {
                Set<String> candidates = new HashSet<>(lCloudlets.keySet());
                candidates.remove(lLeader.getId());
                try {
                    lguards = selectGuards(candidates,lguards);
                    if(lguards.isEmpty())
                        logToScheduler("No guards chosen");
                    else {
                        logToScheduler("New Guards: " + lguards);
                        updateFaultToleranceMechanism(lguards);
                    }

                } catch (MissingScenario e) {
                    logToScheduler(e.getLocalizedMessage());
                    System.err.println(e.getLocalizedMessage());
                    System.exit(1);

                }

            }

            //Update info
            lInfo.setDevices(lDevices);
            lInfo.setCloudlets(lCloudlets);
            lInfo.setLeader(lLeader);
            lInfo.setGuards(lguards);
            trigger(new SharedRegisterModule.Write("info", lInfo, periodUUID));

        };
    }

    @Override
    public EventHandler<HealthCheckModule.HostIsUp> onHostIsUp() {
        return (e)->{
            logToScheduler(e.getHost()+" is up and running!");
        };
    }

    @Override
    public EventHandler<HealthCheckModule.HostIsDown> onHostIsDown() {
        return (e)->{
            logToScheduler(e.getHost()+" is down. Adding to suspected list");
            Scheduler.getInstance().getVirtualFogTopolgy().addHostToSuspectedList(e.getHost());
            trigger(new HealthCheckModule.Stop(e.getHost()));
            knownCloudlets.remove(e.getHost());
        };
    }

    @Override
    public EventHandler<HealthCheckModule.HostIsUpButLate> onHostIsUpButLate() {
        return (e)->{
            logToScheduler(e.getHost()+" is up but late");
        };
    }

    private void updateFaultToleranceMechanism(Collection<String> set) {
        for(String s:set) {
            trigger(new HealthCheckModule.Stop(s));
            trigger(new HealthCheckModule.Start(s, checkPeriodForLeader));
            lCloudlets.put(s, checkPeriodForLeader);
        }

    }

    @Override
    /**
     * Return the set of suspected to be faulty IoT devices
     */
    public Set<String> suspectedIot(Set<String> iots) {
        return Scheduler.getInstance().getVirtualFogTopolgy().cloudSuspectedIots(new HashSet<>(iots));
    }

    @Override
    public Set<String> suspectedCloudlet(Set<String> cloudlets) {
        return Scheduler.getInstance().getVirtualFogTopolgy().cloudSuspectedCloudlet(new HashSet<>(cloudlets));
    }

    @Override
    public String electLeader(Set<String> cloudlets) throws SimulationException {
        try {
            return Scheduler.getInstance().getVirtualFogTopolgy().electLeader(new HashSet<>(cloudlets));
        } catch (MissingScenario missingScenario) {
            throw new SimulationException(missingScenario.getLocalizedMessage());
        }
    }

    @Override
    public Set<String> selectGuards(Set<String> set,Set<String>guards) throws MissingScenario {
        return Scheduler.getInstance().getVirtualFogTopolgy().selectGuards(set,guards);
    }


    @Override
    public EventHandler<MsgFromIoT> onMsgFromIoT() {
        return (e)->{
            String iot = e.getFrom();
            OperationPacket pckt = e.getPckt();
            if(pckt.getOperation().equals("REGISTER")){
                newIot.put(iot,null);
                logToScheduler("Registered IoT: "+ iot);

            }else if(pckt.getOperation().equals("RESET")){

                //This is async...
                trigger(new SharedRegisterModule.Write("info",null,periodUUID));

            }else{
                logToScheduler("Unknown operation received by IoT. Op=["+ pckt.getOperation()+"]");
            }
            //IoTModelTime record = new IoTModelTime(iot,getProcess().getLocalTime(),model);
            //newIot.put(iot,record);
            //logToScheduler("Receive from IoT: "+ record);

        };
    }

    @Override
    public EventHandler<MsgFromCloudlet> onMsgFromCloudlet() {
        return (e)->{
            String cloudlet = e.getFrom();
            OperationPacket pckt = e.getPckt();

            if(pckt.getOperation().equals("INIT")){
                OperationPacket pp = new OperationPacket("INIT_ACK", PacketPlane.HEALTH_CHECK,"");
                trigger(new NetworkModule.SendMsg(pp,cloudlet));
            }

            //If it is in the suspected cloudlets we remove it
            Scheduler.getInstance().getVirtualFogTopolgy().removeHostFromSuspectedList(cloudlet);

            //TODO If a cloudlet has been elected as a leader or guard, we need to update the healthcheck
            if(lguards.contains(cloudlet)||lLeader.getId().equals(cloudlet)) {
                newCloudlet.putIfAbsent(cloudlet, checkPeriodForLeader);
            }else{
                newCloudlet.putIfAbsent(cloudlet, checkPeriodForCloudlets);

            }
            //logToScheduler("Received from cloudlet:"+cloudlet);

            if(knownCloudlets.containsKey(cloudlet))
                return;

            logToScheduler("Received a msg from '"+cloudlet+"'. Starting HealthCheck");
            if(lguards.contains(cloudlet)||lLeader.getId().equals(cloudlet)){

                knownCloudlets.put(cloudlet, checkPeriodForLeader);
                trigger(new HealthCheckModule.Start(cloudlet, checkPeriodForLeader));
            }else {
                knownCloudlets.put(cloudlet, checkPeriodForCloudlets);
                trigger(new HealthCheckModule.Start(cloudlet, checkPeriodForCloudlets));
            }
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
                trigger(new MsgFromIoT(from,packet));
            }else if(from.startsWith("cloudlet_")){
                trigger(new MsgFromCloudlet(from,packet));
            }else{
                //We ignore other senders
            }
        };
    }


    public SSCloudComponent(IProcess process, Map<String,Object> props) throws MissingModule {
        super(process, props);
    }

}
