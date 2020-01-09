package dsslib.utilities;

import dsslib.process.Process;
import dsslib.process.IProcess;
import dsslib.components.networking.ChannelComponent;
import dsslib.components.networking.NetworkModule;
import dsslib.scheduler.Scheduler;
import dsslib.exceptions.MissingModule;
import dsslib.exceptions.NetworkModuleNotFound;

import java.util.LinkedHashMap;

public class Helpers {

    public static void createDuplexLink(IProcess p1, IProcess p2, float p1p2Speed, int p1p2Bandwidth, float p2p1Speed, int p2p1Bandwidth) throws NetworkModuleNotFound, MissingModule {

        IProcess p1top2 = new Process(p1.getUuid()+"_"+p2.getUuid(),p1p2Speed);
        NetworkModule networkP1 = p1.getNetworkModule();
        networkP1.registerChannel(p1top2);
        p1top2.registerComponent(new ChannelComponent(p1top2,new LinkedHashMap<String,Object>(){{
            put("from",p1);
            put("to",p2);
            put("bandwidth",p1p2Bandwidth);
        }}));
        Scheduler.getInstance().register(p1top2,false);

        IProcess p2top1 = new Process(p2.getUuid()+"_"+p1.getUuid(),p2p1Speed);
        NetworkModule networkP2 = p2.getNetworkModule();
        networkP2.registerChannel(p2top1);
        p2top1.registerComponent(new ChannelComponent(p2top1,new LinkedHashMap<String,Object>(){{
            put("from",p2);
            put("to",p1);
            put("bandwidth",p2p1Bandwidth);
        }}));
        Scheduler.getInstance().register(p2top1,false);

    }
}
