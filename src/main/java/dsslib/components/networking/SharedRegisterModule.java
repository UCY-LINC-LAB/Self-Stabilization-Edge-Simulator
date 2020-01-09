package dsslib.components.networking;

import dsslib.process.IProcess;
import dsslib.components.AbstractModule;
import dsslib.events.Event;
import dsslib.events.EventHandler;
import dsslib.events.Execution;
import dsslib.events.Init;
import dsslib.exceptions.MissingModule;
import dsslib.selfstabilization.RegisterInfoModel;
import dsslib.selfstabilization.SeqIdPair;

import java.util.HashMap;
import java.util.Map;

public abstract class SharedRegisterModule extends AbstractModule {

    public Map<String,Object> records = new HashMap<>();


    public abstract EventHandler<Read> onRead();
    public abstract EventHandler<Write> onWrite();
    public abstract EventHandler<WriteArray> onWriteArray();


    public abstract EventHandler<NetworkModule.NewMessageReceived> onMessageReceived();




    Map<String,RegisterInfoModel> fakeRegister = new HashMap<>();
    Map<String,Integer> counts = new HashMap<>();


    public void makeCloudletBelieveThatIsALeaderForNReads(String cloudlet,int n){
        //FAKE Info
        RegisterInfoModel toRead = new RegisterInfoModel();
        toRead.setLeader(new SeqIdPair(1,cloudlet));

        fakeRegister.put(cloudlet,toRead);
        counts.put(cloudlet,n);
    }

    protected Object retrieveFakeOrReal(String cloudlet,String key) {
        if(!key.equals("info")) {
            Object value = records.get(key);
            logToScheduler("READ operation received:" + key + ". Answer: " + value);
            return value;
        }

        RegisterInfoModel fake = fakeRegister.get(cloudlet);
        if(fake==null){
            Object value = records.get(key);
            logToScheduler("READ operation received:" + key + ". Answer: " + value);
            return value;
        }

        RegisterInfoModel real = (RegisterInfoModel) records.getOrDefault(key,new RegisterInfoModel());
        fake.setCloudlets(real.getCloudlets());
        fake.setDevices(real.getDevices());
        fake.setGuards(real.getGuards());
        fake.getGuards().remove(cloudlet);

        logToScheduler("Fake READ operation received:" + key + ". Answer: " + fake);

        int c = counts.get(cloudlet);
        c--;
        if(c<=0){
            counts.remove(cloudlet);
            fakeRegister.remove(cloudlet);
        }else{
            counts.put(cloudlet,c);
        }

        return fake;


    }

    /**
     * Indication
     */
    public static final class ReadResponse extends Event{
        private final String key;
        private final Object value;
        private final String uuid;

        public ReadResponse(String key, Object value, String uuid){
            this.key = key;
            this.value = value;
            this.uuid = uuid;
        }

        public String getUuid() {
            return uuid;
        }

        public String getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }
    }

    public static final class Read extends Event {
        private final String key;
        private final String uuid;

        public Read(String key, String uuid){
            this.key = key;
            this.uuid = uuid;
        }

        public String getUuid() {
            return uuid;
        }

        public String getKey() {
            return key;
        }

    }

    public static final class WriteResponse extends Event{
        private final String key;
        private final Object value;
        private final String uuid;

        public WriteResponse(String key, Object value, String uuid){
            this.key = key;
            this.value = value;
            this.uuid = uuid;
        }

        public String getKey() {
            return key;
        }

        public String getUuid() {
            return uuid;
        }

        public Object getValue() {
            return value;
        }
    }
    public static final class WriteArray extends Event {
        private final String key;
        private final Object value;
        private final String uuid;

        public WriteArray(String key, Object value, String uuid) {
            this.key = key;
            this.value = value;
            this.uuid = uuid;
        }

        public String getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }

        public String getUuid() {
            return uuid;
        }
    }

    public static final class Write extends Event{
        private final String key;
        private final Object value;
        private final String uuid;

        public Write(String key, Object value, String uuid) {
            this.key = key;
            this.value = value;
            this.uuid = uuid;
        }

        public String getUuid() {
            return uuid;
        }

        public String getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }
    }

    @Override
    public EventHandler<Init> onInit() {
        return (e)->{};
    }

    @Override
    public EventHandler<Execution> onExecution() {
        return (e)->{};
    }

    public SharedRegisterModule(IProcess process) throws MissingModule {
        super(process,null);
        require(NetworkModule.class);
        subscribe(onMessageReceived(), NetworkModule.NewMessageReceived.class);
        subscribe(onRead(),Read.class);
        subscribe(onWrite(),Write.class);
        subscribe(onWriteArray(),WriteArray.class);
    }

    public Map<String, Object> getRecords() {
        return records;
    }

    @Override
    public Map<String, Object> getState() {
        return records;
    }
}
