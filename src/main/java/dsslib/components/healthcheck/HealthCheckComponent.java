package dsslib.components.healthcheck;

import dsslib.process.IProcess;
import dsslib.components.networking.NetworkModule;
import dsslib.components.networking.OperationPacket;
import dsslib.components.networking.PacketPlane;
import dsslib.components.timers.SimpleTimer;
import dsslib.components.timers.SimpleTimerModule;
import dsslib.events.EventHandler;
import dsslib.events.Execution;
import dsslib.events.Init;
import dsslib.exceptions.MissingModule;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HealthCheckComponent extends HealthCheckModule {

    Map<String,State> uuids = new HashMap<>();
    Map<String,State> hosts = new HashMap<>();

    private class State{
        String host;
        String uuid;
        boolean receivedReply;
        boolean expired;
        int period;
        long sequence;

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getUuid() {
            return uuid;
        }

        public void setPeriod(int period) {
            this.period = period;
        }

        public int getPeriod() {
            return period;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getHost() {
            return host;
        }

        public boolean isExpired() {
            return expired;
        }

        public void setExpired(boolean expired) {
            this.expired = expired;
        }

        public void setReceivedReply(boolean receivedReply) {
            this.receivedReply = receivedReply;
        }

        public long getSequence() {
            return sequence;
        }

        public void setSequence(long sequence) {
            this.sequence = sequence;
        }

        public boolean isReceivedReply() {
            return receivedReply;
        }
    }

    @Override
    public EventHandler<SimpleTimer.Expire> onExpire() {
        return (e)->{
            String uuid = e.getUuid();
            State state = uuids.get(uuid);

            //Not responsible for this expiration. Two reasons:
            // 1. because of Simple Timer
            // 2. Because we called stop and we removed state
            if(state == null)
                return;

            logToScheduler("Timer Expired for host: "+state.getHost());

            if(!state.receivedReply) {
                state.setExpired(true);
                trigger(new HostIsDown(state.getHost()));
            } else {
                //Everything was ok
                state.setExpired(false);
            }

            //Resend healthcheck
            state.receivedReply=false;
            state.expired=false;
            state.sequence=state.sequence++;
            OperationPacket packet = new OperationPacket("HC", PacketPlane.HEALTH_CHECK, "HELLO");
            trigger(new NetworkModule.SendMsg(packet, state.getHost()));
            trigger(new SimpleTimerModule.Start(state.getPeriod(),state.getUuid()));
        };
    }

    @Override
    public EventHandler<NetworkModule.NewMessageReceived> onNewMessage() {
        return (msg)->{

            //Ask If should loose the messages if am not enabled
            if(!this.getProcess().isEnabled())
                return;

            String from = msg.getIpFrom();
            OperationPacket packet = msg.getPacket();

            if(!packet.getOperation().equals("HC"))
                return;

            String operation =  (String) packet.getPayload();

            if(operation.equals("HELLO")){
                trigger(new NetworkModule.SendMsg(new OperationPacket("HC", PacketPlane.HEALTH_CHECK, "HI"),from));
            }
            else
            if(operation.equals("HI")){
                State state = hosts.get(from);
                if(state!=null){
                    if(!state.isExpired()) {
                        logToScheduler("Host: "+from+" is up and fine");
                        trigger(new HostIsUp(from));
                        state.setReceivedReply(true);
                    }else{
                        state.setReceivedReply(true);
                        logToScheduler("Host: "+from+" is up but late");
                        trigger(new HostIsUpButLate(from));

                    }
                }else{
                    logToScheduler("Unknown host");
                }
            }


        };
    }

    public HealthCheckComponent(IProcess process) throws MissingModule {
        super(process);
    }

    @Override
    public EventHandler<Start> onStart() {
        return (e)->{
            State state = new State();
            state.setHost(e.getHost());
            state.setUuid(UUID.randomUUID().toString());
            state.setPeriod(e.getPeriod());
            uuids.put(state.getUuid(),state);
            hosts.put(state.getHost(),state);

            OperationPacket enc = new OperationPacket("HC", PacketPlane.HEALTH_CHECK, "HELLO");

            trigger(new NetworkModule.SendMsg(enc,state.getHost()));
            logToScheduler("Starting a "+state.getPeriod()+"s timer for host:"+state.getHost());
            trigger(new SimpleTimerModule.Start(state.getPeriod(),state.getUuid()));
        };
    }

    @Override
    public EventHandler<Stop> onStop() {
        return (e)->{
            String host = e.getHost();
            State state = hosts.get(host);
            uuids.remove(state.getUuid());
            hosts.remove(host);
            logToScheduler("Stopped healthcheck for "+host);
        };
    }

    @Override
    public Map<String, Object> getState() {
        Map<String,Object> state = new HashMap<>();
        state.put("hosts",hosts.values());
        return state;
    }
    @Override
    public EventHandler<Init> onInit() {
        return (e)->{};
    }

    @Override
    public EventHandler<Execution> onExecution() {
        return (e)->{ };
    }
}
