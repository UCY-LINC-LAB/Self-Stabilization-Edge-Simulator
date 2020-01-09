package dsslib.components.networking;

import dsslib.process.IProcess;
import dsslib.events.EventHandler;
import dsslib.exceptions.MissingModule;
import dsslib.utilities.Tuple2;
import dsslib.utilities.Tuple3;

import java.util.HashMap;
import java.util.Map;

public class SharedRegisterComponent extends SharedRegisterModule{

    public SharedRegisterComponent(IProcess process) throws MissingModule {
        super(process);
    }

    @Override
    public EventHandler<Read> onRead() {
        return (e)->{
            String key = e.getKey();
            String uuid = e.getUuid();
            logToScheduler("Read initiated for key:"+key);
            Tuple2 keyAndUUID = new Tuple2(key,uuid);
            OperationPacket p = new OperationPacket("READ", PacketPlane.SHARED_REGISTER_READ, keyAndUUID);
            OperationPacket encapsulation = new OperationPacket("SR", PacketPlane.SHARED_REGISTER_READ, p);
            trigger(new NetworkModule.SendMsg(encapsulation,"sharedMemory"));

        };
    }

    @Override
    public EventHandler<WriteArray> onWriteArray() {
        return (e)->{
            String key = e.getKey();
            String uuid = e.getUuid();
            logToScheduler("Write initiated for key:"+key);
            Object value = e.getValue();
            Tuple3 tuple3 = new Tuple3(key,value,uuid);
            OperationPacket p = new OperationPacket("WRITE_ARRAY", PacketPlane.SHARED_REGISTER_WRITE, tuple3);
            OperationPacket encapsulation = new OperationPacket("SR", PacketPlane.SHARED_REGISTER_WRITE, p );
            trigger(new NetworkModule.SendMsg(encapsulation ,"sharedMemory"));

        };
    }

    @Override
    public EventHandler<Write> onWrite() {
        return (e)->{
            String key = e.getKey();
            String uuid = e.getUuid();
            logToScheduler("Write initiated for key:"+key);
            Object value = e.getValue();
            Tuple3 tuple3 = new Tuple3(key,value,uuid);
            OperationPacket p = new OperationPacket("WRITE", PacketPlane.SHARED_REGISTER_WRITE, tuple3);
            OperationPacket encapsulation = new OperationPacket("SR", PacketPlane.SHARED_REGISTER_WRITE, p );
            trigger(new NetworkModule.SendMsg(encapsulation ,"sharedMemory"));
        };
    }

    @Override
    public EventHandler<NetworkModule.NewMessageReceived> onMessageReceived() {
        return (msg)-> {

            //Ask If should lose the messages if am not enabled
            if(!this.getProcess().isEnabled())
                return;

            OperationPacket packet = msg.getPacket();

            //We ignore any other SR
            if(!packet.getOperation().equals("SR"))
                return;

            //Decapsulate
            packet = (OperationPacket) packet.getPayload();

            //
            if (packet.getOperation().equals("READ")) {
                Tuple2 keyUuid = (Tuple2) packet.getPayload();
                String key = (String) keyUuid.getR1();
                String uuid = (String) keyUuid.getR2();


                //Here we check if we should fake
                Object value;
                if(msg.getIpFrom().startsWith("cloudlet_")){

                    value = retrieveFakeOrReal(msg.getIpFrom(),key);

                }else{
                    value = records.get(key);
                    logToScheduler("READ operation received:"+key+". Answer: "+value);

                }




                //TODO xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                Tuple3 tuple3 = new Tuple3(key,value,uuid);
                OperationPacket p = new OperationPacket("READ_RESULT", PacketPlane.SHARED_REGISTER_READ, tuple3);
                OperationPacket encapsulation = new OperationPacket("SR", PacketPlane.SHARED_REGISTER_READ, p);
                trigger(new NetworkModule.SendMsg(encapsulation, msg.getIpFrom()));
            }
            else
            if (packet.getOperation().equals("WRITE")) {
                Tuple3 tuple3 = (Tuple3) packet.getPayload();
                String key = (String) tuple3.getR1();
                Object value = tuple3.getR2();
                records.put(key,value);

                OperationPacket p = new OperationPacket("WRITE_RESULT", PacketPlane.SHARED_REGISTER_WRITE, tuple3);
                trigger(new NetworkModule.SendMsg(new OperationPacket("SR", PacketPlane.SHARED_REGISTER_WRITE, p), msg.getIpFrom()));
            }else
            if(packet.getOperation().equals("WRITE_ARRAY")){
                Tuple3 tuple3 = (Tuple3) packet.getPayload();
                String key = (String) tuple3.getR1();
                Object value = tuple3.getR2();
                Map<String,Object> lcal = (Map<String, Object>) records.getOrDefault(key, new HashMap<String, Object>());
                records.putIfAbsent(key,lcal);
                lcal.put(msg.getIpFrom(),value);

                OperationPacket p = new OperationPacket("WRITE_RESULT", PacketPlane.SHARED_REGISTER_WRITE, tuple3);
                trigger(new NetworkModule.SendMsg(new OperationPacket("SR", PacketPlane.SHARED_REGISTER_WRITE, p), msg.getIpFrom()));

            }
            else
            if (packet.getOperation().equals("READ_RESULT")) {
                logToScheduler("READ_RESULT operation received");
                Tuple3 pair = (Tuple3) packet.getPayload();
                String key = (String) pair.getR1();
                Object value = pair.getR2();
                String uuid = (String) pair.getR3();

                trigger(new ReadResponse(key,value, uuid));

            }
            else
            if (packet.getOperation().equals("WRITE_RESULT")) {
                logToScheduler("WRITE_RESULT operation received");
                Tuple3 pair = (Tuple3) packet.getPayload();
                trigger(new WriteResponse((String) pair.getR1(), pair.getR2(), (String) pair.getR3()));
            } else {
                logToScheduler("Unknown operation: " + packet.getOperation()+" ,from"+msg.getIpFrom());

            }
        };
    }


}
