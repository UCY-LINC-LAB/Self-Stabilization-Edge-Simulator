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
import dsslib.selfstabilization.RegisterInfoModel;
import dsslib.selfstabilization.SeqIdPair;
import dsslib.selfstabilization.cloudlets.CloudletStatus;
import dsslib.selfstabilization.cloudlets.CloudletView;
import dsslib.selfstabilization.cloudlets.DataForSharedRegister;
import dsslib.selfstabilization.cloudlets.Replica;
import dsslib.statistics.Statistics;
import dsslib.utilities.Tuple2;

import java.util.*;

public class SSCloudletLeaderComponent extends SSCloudletLeaderModule{


    @Override
    public EventHandler<Init> onInit() {

        return (e)->{
            uuidForPeriodicTimer = getProcess().getUuid()+"-"+ UUID.randomUUID().toString();
            trigger(new PeriodicTimerModule.PeriodStart(uuidForPeriodicTimer,periodicity));
            healthCheck.put(this.getProcess().getUuid(),0);

        };
    }

    @Override
    public EventHandler<Execution> onExecution() {
        return (e)->{

        };
    }

    @Override
    public EventHandler<PeriodicTimerModule.PeriodExpired> onPeriodTimeout() {
        return (e)->{

            if(amIleader && amIGuard){
                amIleader=true;
                amIGuard = false;
                //TODO Prefer leader
            }

            //Execute only when the periodic timer is for me
            String uuid = e.getUuid();
            if(!uuidForPeriodicTimer.equals(uuid))
                return;

            if(!amIleader && !amIGuard)
                return;


            if(amIleader)
                logToScheduler("Taking step as a leader");
            else
                logToScheduler("Taking step as a guard");



            //Line 95
            FD = failureDetector();
            logToScheduler("FD is read:"+ FD);
            trigger(new SharedRegisterModule.Read("info", uuid));
        };
    }

    @Override
    public EventHandler<ResetLeaderShip> onResetLeaderShip() {
        return (e)->{
            /**
            if(amIGuard) {
                Statistics.getInstance().record("selfStabilization", new HashMap<String, Object>() {{
                    put("event", Statistics.Event.NO_GUARD_ANYMORE);
                    put("process", getProcess().getUuid());
                }});
            }
            if(amIleader) {
                Statistics.getInstance().record("selfStabilization", new HashMap<String, Object>() {{
                    put("event", Statistics.Event.NO_LEADER_ANYMORE);
                    put("process", getProcess().getUuid());
                }});
            }**/
            amIGuard = false;
            amIleader = false;
        };
    }

    @Override
    public EventHandler<SharedRegisterModule.ReadResponse> onReadResponse() {
        return (e)->{

            if(!e.getUuid().equals(uuidForPeriodicTimer))
                return;

            String key = e.getKey();

            if(!key.equals("info")){
                return;
            }

            Object result = e.getValue();
            RegisterInfoModel info = (RegisterInfoModel) result;
            if(info==null) info = new RegisterInfoModel();

            String i = this.getProcess().getUuid();
            //lDevices  = info.getDevices(); //It is not used...
            lCloudlets = info.getCloudlets();
            lLeader = info.getLeader();
            lGuards = info.getGuards();
            //LINE: 88
            logToScheduler("Read register. Local Leader is "+lLeader);


            Replica repI = this.rep.getOrDefault(i, new Replica());
            rep.putIfAbsent(i,repI);


            //Line 89-90 : If I am a leader now but not a leader before
            if(lLeader.getId().equals(i) && !myLeader.equals(i)){
                logToScheduler("I am a leader now");
                repI.setStatus(CloudletStatus.PROPOSE);
                CloudletView propV = new CloudletView();
                propV.setID(new SeqIdPair(lLeader.getSeq(),lLeader.getId()));
                propV.setCnt(0);
                Set<String> FD = this.FD;
                Set<String> propVSet = new HashSet<>(FD);
                Set<String> guardsLleader = new HashSet<>(lGuards);
                guardsLleader.add(lLeader.getId());
                //Intersection
                propVSet.retainAll(guardsLleader);
                propV.setSet(propVSet);
                repI.setPropV(propV);
                myLeader = lLeader.getId();
                logToScheduler("Updated status to: "+repI.getStatus());
                logToScheduler("Updated propV to: "+repI.getPropV());
                logToScheduler("Updated myLeader to: "+myLeader);
                Statistics.getInstance().record("selfStabilization",new HashMap<String,Object>(){{
                    put("event",Statistics.Event.START_PROPOSE_PHASE);
                    put("reason","I was not a leader before. STATE now is PROPOSE");
                    put("process",getProcess().getUuid());
                    put("zone",getProcess().getZone());
                }});

            }

            //For lines 91-92

            //For both Clauses of line 91
            Set<String> viewSet = new HashSet<>(this.FD);
            Set<String> guardsLleader = new HashSet<>(lGuards);
            guardsLleader.add(lLeader.getId());
            viewSet.retainAll(guardsLleader);

            //Line 91
            if(lLeader.getId().equals(i)
                    && myLeader.equals(i)
                    && (
                    ( repI.getStatus()==CloudletStatus.MULTICAST && !viewSet.equals(repI.getView().getSet()) )
                            || (repI.getStatus()!=CloudletStatus.MULTICAST && !viewSet.equals(repI.getPropV().getSet())))
            ){
                //Line 92
                logToScheduler("I am still a leader but the view has changed");
                repI.setStatus(CloudletStatus.PROPOSE);
                CloudletView propV = new CloudletView();
                propV.setID(new SeqIdPair(lLeader.getSeq(),lLeader.getId()));
                //TODO Ask if the cnt is from proposed view
                propV.setCnt(propV.getCnt()+1);
                propV.setSet(viewSet);
                repI.setPropV(propV);
                Statistics.getInstance().record("selfStabilization",new HashMap<String,Object>(){{
                    put("event",Statistics.Event.START_PROPOSE_PHASE);
                    put("reason","View has changed");
                    put("process",getProcess().getUuid());
                    put("zone",getProcess().getZone());
                }});

            }
            //Line 94-96
            if(!lLeader.getId().equals(i) && lGuards.contains(i) && FD.contains(lLeader.getId())) {
                myLeader = lLeader.getId();
                Replica leader = rep.getOrDefault(myLeader,new Replica());
                rep.putIfAbsent(myLeader,leader);
                repI.setStatus(leader.getStatus());
            }

            //Line 97
            boolean alternatePathTaken = false;
            CloudletStatus myStatus = repI.getStatus();
            if(lLeader.getId().equals(i) && roundProceedReady()){

                switch (myStatus){

                    case MULTICAST:
                        alternatePathTaken =true;
                        coordinateMcastRnd();
                        break;
                    case INSTALL:
                        coordinateInstall();
                        break;
                    case PROPOSE:
                        coordinatePropose();
                        break;
                }

            }else //Line 101
                if(!lLeader.getId().equals(i) && lGuards.contains(i) && this.FD.contains(lLeader.getId())  && roundReadyToFollow() ){
                    switch (myStatus){
                        case MULTICAST:
                            alternatePathTaken =true;
                            followMcastRnd();
                            break;
                        case INSTALL:
                            followInstall();
                            break;
                        case PROPOSE:
                            followPropose();
                            break;
                    }
            }

            if(!alternatePathTaken)
                lines_105_109();
        };
    }



    @Override
    public EventHandler<NetworkModule.NewMessageReceived> onNewMessageReceived() {
        return (msg)->{

            //Ask If should lose the messages if am not enabled
            if(!this.getProcess().isEnabled())
                return;

            //Ignore
            if(!amIGuard && !amIleader)
                return;

            OperationPacket packet = msg.getPacket();
            String op = packet.getOperation();
            if(msg.getIpFrom().startsWith("cloudlet_"))
                updateHealthCheck(msg.getIpFrom());

            if(!op.equals("SSL")) {
                return;
            }


            if(packet.getOperation().equals("SSL")){
                logToScheduler("Msg from "+msg.getIpFrom()+": "+packet);
                Replica rep = (Replica) packet.getPayload();
                this.rep.put(msg.getIpFrom(),rep);



                return;
            }

            //THINK THAT THIS IS NOT USED ANYMORE
            /**
            if(packet.getOperation().equals("SS")){
                logToScheduler("Msg received: "+packet);
                Replica rep = (Replica) packet.getPayload();
                rep = rep.copy();
                //rep.setInput(null);
                this.rep.put(msg.getIpFrom(),rep);
                return;
            }
             **/

            /**
             * Ignore it for noww
            if(packet.getOperation().equals("MCM")){
                logToScheduler("Msg received: "+packet);
                Replica rep = (Replica) packet.getPayload();
                //rep = rep.copy();
                //rep.setInput(null);
                this.rep.put(msg.getIpFrom(),rep);
            }
             **/

        };
    }

    private void updateHealthCheck(String from) {
        healthCheck.put(from,0);
        healthCheck.entrySet().forEach(entry->{
            if(!from.equals(entry.getKey()) && !entry.getKey().equals(this.getProcess().getUuid()))
                entry.setValue(entry.getValue()+1);
        });

    }


    @Override
    public EventHandler<SSCloudletLeaderModule.FetchResponse> onFetchResponse() {
        return (e)->{

            String i = getProcess().getUuid();
            Replica repI = rep.getOrDefault(i,new Replica());

            if(e.getWhoCalled().equals("followMcastRnd")){

                Map<String, Tuple2> aggregate = e.getAggregate();
                repI = rep.getOrDefault(i,new Replica());
                rep.putIfAbsent(i,repI);
                repI.setInput(aggregate);

                lines_105_109();

            }else if(e.getWhoCalled().equals("coordinateMcastRnd")){

                Map<String,Tuple2> aggregated = e.getAggregate();
                rep.putIfAbsent(i,repI);
                repI.setInput(aggregated);

                Set<String> allCloudlets = new HashSet<>(Scheduler.getInstance().getVirtualFogTopolgy().getCloudlets().keySet());

                Set<String> myViewSet= repI.getView().getSet();
                for(String j : allCloudlets){

                    Replica repJ = rep.getOrDefault(j,new Replica());
                    rep.putIfAbsent(j,repJ);

                    //Line 91
                    if(myViewSet.contains(j)){
                        repI.getMsg().put(j,repJ.getInput());
                    }else{
                        repI.getMsg().put(j,null);
                    }

                }
                //Write to sharedRegister
                //Line 92
                int rnd = repI.getRnd();
                Object state  = repI.getState();
                DataForSharedRegister data = new DataForSharedRegister(i,lLeader,rnd,state);
                trigger(new SharedRegisterModule.Write("data",data,"line84Response"));
            }



        };
    }

    @Override
    public EventHandler<SharedRegisterModule.WriteResponse> onWriteResponse() {
        return (e->{

            if(!e.getKey().equals("data"))
                return;

            String i = getProcess().getUuid();
            Replica repI = rep.getOrDefault(i, new Replica());

            if(e.getUuid().equals("line84Response")) {


                //The following should happen where Write Response Comes back
                repI.setRnd(repI.getRnd() + 1);
                Statistics.getInstance().record("selfStabilization", new HashMap<String, Object>() {{
                    put("event", Statistics.Event.WRITE_DATA);
                    put("reason", "Written data to SR by leader. Round updated to " + repI.getRnd());
                    put("repSize", repI.calculateSize());
                    put("process", getProcess().getUuid());
                    put("zone",getProcess().getZone());
                }});


                //REPEAT LINES 116-119
                lines_105_109();

            }else if(e.getUuid().equals("line107Response")){

                Statistics.getInstance().record("selfStabilization", new HashMap<String, Object>() {{
                    put("event", Statistics.Event.WRITE_DATA);
                    put("reason", "Written data to SR by guard. Round updated to " + repI.getRnd());
                    put("repSize", repI.calculateSize());
                    put("process", getProcess().getUuid());
                    put("zone",getProcess().getZone());
                }});

                //Lines 108 and 19
                lines_108_109();

            }


        });
    }



    @Override
    public EventHandler<UpdateLeadership> onUpdateLeaderShip() {
        return (e)->{
            Type type = e.getType();
            boolean value = e.getValue();
            switch (type){
                case LEADER:

                    if(amIleader && value!=amIleader)
                        logToScheduler("I am not a leader anymore");
                    else if( amIleader)
                        logToScheduler("I am still a leader");
                    else if(value != amIleader)
                        logToScheduler("I am a leader now");
                    else
                        logToScheduler("Still I am not a leader");

                    amIleader=value;

                    break;

                case GUARD:

                    if(amIGuard && value!=amIGuard)
                        logToScheduler("I am not a guard anymore");
                    else if( amIGuard)
                        logToScheduler("I am still a guard");
                    else if(value != amIGuard)
                        logToScheduler("I a  guard now");
                    else
                        logToScheduler("Still I am not a guard");

                    amIGuard=value;

                    break;
            }
        };
    }

    public SSCloudletLeaderComponent(IProcess process, Map<String, Object> props) throws MissingModule {
        super(process, props);
    }

    @Override
    public Set<String> failureDetector() {
        //TODO
        maxBeat=0;
        maxBeatFrom="";
        Set<String> result = new LinkedHashSet<>();
        Set<String> allCloudlets = Scheduler.getInstance().getVirtualFogTopolgy().getCloudlets().keySet();
        for(String cl : allCloudlets){
            int beat = healthCheck.getOrDefault(cl,0);
            healthCheck.putIfAbsent(cl,0);
            if(maxBeat<beat){
                maxBeat= beat;
                maxBeatFrom = cl;
            }
            if(beat<healthCheckThreshold){
                result.add(cl);
            }
        }


        if(maxBeat>healthCheckThreshold)
            Statistics.getInstance().record("selfStabilization", new HashMap<String, Object>() {{
                put("event", Statistics.Event.REACHED_HEALTH_CHECK_THRESHOLD);
                put("maxBeat", maxBeat);
                put("maxBeatFrom", maxBeatFrom);
                put("process", getProcess().getUuid());
                put("zone",getProcess().getZone());
            }});
        //return Scheduler.getInstance().getVirtualFogTopolgy().failureDetectorAboutCloudlets();
        return  result;
    }

    @Override
    public boolean roundProceedReady() {

        Replica repI = rep.getOrDefault(getProcess().getUuid(), new Replica());
        rep.putIfAbsent(getProcess().getUuid(),repI);

        boolean clause1 = true;

        //Is it ViewSet or ProposedView Set
        //???????? ASKKKKKK
        for(String j : repI.getView().getSet()){
        //for(String j : repI.getPropV().getSet()){

            Replica repJ = rep.getOrDefault(j,new Replica());
            rep.putIfAbsent(j,repJ);

            if(!repI.getStatus().equals(repJ.getStatus())) {
                clause1=false;
                break;
            }

            if(!repI.getView().equals(repJ.getView())) {
                clause1 = false;
                break;
            }

            if(repI.getRnd() != repJ.getRnd()) {
                clause1 = false;
                break;
            }
        }

        if(clause1) {
                return true;
        }


        //Check second clause
        // It must be != to multicast
        if(repI.getStatus()==CloudletStatus.MULTICAST)
           return false;


        //Now this must be true...
        boolean x = true;
        for(String j : repI.getPropV().getSet()){

            Replica repJ = rep.getOrDefault(j,new Replica());
            rep.putIfAbsent(j,repJ);

            if(repJ.getStatus()!=(CloudletStatus.PROPOSE))
                x= false;

            if(!repJ.getPropV().equals(repI.getPropV()))
                x= false;
        }
        if(x)
            return true;

        //Now this must be true...
        for(String j : repI.getPropV().getSet()){

            Replica repJ = rep.getOrDefault(j,new Replica());
            rep.putIfAbsent(j,repJ);

            if(repJ.getStatus()!=(CloudletStatus.INSTALL))
                return false;

            if(!repJ.getPropV().equals(repI.getPropV()))
                return false;
        }
        return true;
    }


    @Override
    public void coordinateMcastRnd() {

        String i = getProcess().getUuid();
        Replica repI = rep.getOrDefault(i, new Replica());
        rep.putIfAbsent(i,repI);

        //Apply Msgs....
        apply(repI,repI.getMsg(),FD);

        trigger(new Fetch("coordinateMcastRnd"));



    }

    @Override
    public void coordinateInstall() {

        String i = getProcess().getUuid();
        Replica repI = rep.getOrDefault(i, new Replica());
        rep.putIfAbsent(i,repI);

        repI.setView(repI.getPropV());
        repI.setStatus(CloudletStatus.MULTICAST);
        repI.setRnd(0);

        Statistics.getInstance().record("selfStabilization",new HashMap<String,Object>(){{
            put("event",Statistics.Event.START_MULTICAST_PHASE_FOR_LEADER);
            put("reason","Change to MULTICAST. Rnd is "+repI.getRnd());
            put("process",getProcess().getUuid());
            put("zone",getProcess().getZone());
        }});
    }

    @Override
    public void coordinatePropose() {
        String i = getProcess().getUuid();
        Replica repI = rep.getOrDefault(i, new Replica());
        rep.putIfAbsent(i,repI);
        repI.setState(synchState(repI));
        repI.setMsg(synchMsgs(repI));
        repI.setStatus(CloudletStatus.INSTALL);
        //repI.setRnd(0);
        //repI.setView(repI.getView());

        Statistics.getInstance().record("selfStabilization",new HashMap<String,Object>(){{
            put("event",Statistics.Event.COORDINATE_PROPOSE);
            put("reason","Set state now to INSTALL");
            put("process",getProcess().getUuid());
            put("zone",getProcess().getZone());
        }});
    }

    @Override
    public boolean roundReadyToFollow() {
        Replica leadersRep = rep.getOrDefault(myLeader, new Replica());
        rep.putIfAbsent(myLeader,leadersRep);

        if(leadersRep.getRnd() == 0)
            return true;


        String i = getProcess().getUuid();
        Replica repI = rep.getOrDefault(i, new Replica());
        rep.putIfAbsent(i,repI);

        if( repI.getRnd()< leadersRep.getRnd())
            return true;

        if( !leadersRep.getView().equals(leadersRep.getPropV()))
            return true;

        return false;
    }

    private void lines_105_109(){
        String i = getProcess().getUuid();
        Replica repI = rep.get(i);
        // Line 105
        if(!lLeader.getId().equals(i) && lGuards.contains(i) && !FD.contains(lLeader.getId()) ){
            myLeader = "";
            logToScheduler("Set myLeader to BOTTOM");
            int rnd = repI.getRnd();
            Object state = repI.getState();
            DataForSharedRegister data = new DataForSharedRegister(i,lLeader,rnd,state);
            trigger(new SharedRegisterModule.Write("data",data,"line107Response"));

        }else{
            lines_108_109();
        }

    }

    private void lines_108_109() {
        Replica repI = rep.get(getProcess().getUuid());

        /**TODO This should be a sent msg **/
        if(amIGuard){

            //If I don't have a leader I don't send anything
            if(myLeader==null || myLeader.isEmpty())
                return;


            //Maybe sent only if change has changed
            //TODO Check this
            // trigger(new MulticastModule.MultiCastMsg(Collections.singletonList(myLeader),repI.getState()));

            OperationPacket packet = new OperationPacket("SSL", PacketPlane.CONTROL,repI.copy());
            trigger(new NetworkModule.SendMsg(packet,myLeader));

        }else if(amIleader){

            Set<String> allCloudlets = new HashSet<>(lGuards);
            allCloudlets.retainAll(FD);

            //Maybe sent only if change has changed
            //TODO Check this
            //trigger(new MulticastModule.MultiCastMsg(allCloudlets,repI.getState()));

            for(String cloudlet:allCloudlets) {
                OperationPacket packet = new OperationPacket("SSL", PacketPlane.CONTROL, repI.copy());
                trigger(new NetworkModule.SendMsg(packet, cloudlet));
            }

        }

    }
    @Override
    public void followMcastRnd() {

        String i = getProcess().getUuid();
        Replica repI = rep.getOrDefault(i,new Replica());
        Replica repLeader = rep.getOrDefault(myLeader,new Replica()).copy();

        Statistics.getInstance().record("selfStabilization", new HashMap<String, Object>() {{
            put("event", Statistics.Event.FOLLOWING_MCAST_RND);
            put("reason", "My status is:"+repI.getStatus());
            put("process", getProcess().getUuid());
            put("zone",getProcess().getZone());
        }});
        rep.put(i,repLeader);

        apply(repLeader,repLeader.getMsg(),FD);

        trigger(new Fetch("followMcastRnd"));
    }

    @Override
    public void followPropose() {
        String i = getProcess().getUuid();
        Replica repI = rep.getOrDefault(i,new Replica());
        rep.putIfAbsent(i,repI);

        Replica repLeader = rep.getOrDefault(myLeader,new Replica());
        rep.putIfAbsent(myLeader,repLeader);

        repI.setStatus(repLeader.getStatus());
        repI.setPropV(repLeader.getPropV().copy());
        Statistics.getInstance().record("selfStabilization", new HashMap<String, Object>() {{
            put("event", Statistics.Event.FOLLOW_PROPOSE);
            put("reason", "Change my view to my leaders proposed view. My status now is:"+repI.getStatus());
            put("process", getProcess().getUuid());
            put("zone",getProcess().getZone());
        }});
    }

    @Override
    public void followInstall() {
        String i = getProcess().getUuid();
        Replica repI;
        if(rep.containsKey(myLeader))
            repI =  rep.get(myLeader).copy();
        else
            repI = new Replica();
        rep.put(i,repI);

        Statistics.getInstance().record("selfStabilization", new HashMap<String, Object>() {{
            put("event", Statistics.Event.FOLLOW_INSTALL);
            put("reason", "Following Install. My status is:"+repI.getStatus());
            put("process", getProcess().getUuid());
            put("zone",getProcess().getZone());
        }});
    }


    @Override
    public Object apply(Replica rep, Map<String, Map<String,Tuple2>> msg,Set<String> active) {
        Object ans = Scheduler.getInstance().getVirtualFogTopolgy().apply(rep,msg,active);
        //Here we should measure the exact time....

        //Statustics Update real state
        //Scheduler.getInstance().getVirtualFogTopolgy().updateRealState(getProcess().getUuid(), model);

        //msg.clear();
        //Msgs are applied so clean is needed
        return ans;
    }

    @Override
    public Object synchState(Replica replica) {
        //TODO Ask what exactly is synchState
        return replica.getState();
    }


    @Override
    public Map<String, Map<String,Tuple2>> synchMsgs(Replica replicas) {
        Map<String, Map<String, Tuple2>> msg = replicas.getMsg();
        // TODO Check if a copy is needed
        replicas.getMsg().entrySet().removeIf(e->!FD.contains(e.getKey()));


        return msg;
    }

    @Override
    public Map<String, Object> getState() {
        HashMap<String, Object> res = new HashMap<>();
        res.put("amIleader", amIleader);
        res.put("amIGuard", amIGuard);
        if (amIleader || amIGuard) {
            res.put("myLeader", this.myLeader);
            res.put("rep", rep);
            res.put("lLeader", lLeader);
            res.put("lGuards", lGuards);
            res.put("healthCheck", healthCheck);
            res.put("lCloudlets", lCloudlets);

        }
        return res;
    }
}
