package dsslib.events;

import dsslib.scenarios.ScenarioDescriptionLoader;
import dsslib.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class EventBus {
    private static final Logger logger = LoggerFactory.getLogger(EventBus.class);


    private static Set<String> traceEvents;
    private Queue<Event> events = new LinkedList<>();

    /** A FIFO Queue for all events **/
    private final List<Event> trace = new LinkedList<>();

    //Keeps set of subscriber topic wise, using set to prevent duplicates
    private Map<String, Set<ISubscriber>> subscribersMap = new HashMap<>();


    public int countRemainingEvents(){
        return events.size();
    }

    public void subscribe(ISubscriber subscriber, Class<? extends Event> type){
        String evType = type.getTypeName();
        Set<ISubscriber> subscribers = subscribersMap.getOrDefault(evType,new LinkedHashSet<>());
        subscribers.add(subscriber);
        subscribersMap.putIfAbsent(evType,subscribers);

    }

    public void trigger(Event event){
       String type = event.getClass().getTypeName();

       event.setGlobalTimeIssued(Scheduler.getInstance().getTime());
       events.add(event);

       //TODO Add to trace
        if(traceEvents==null){
            traceEvents = ScenarioDescriptionLoader.getScenarioDescription().getMode().getTrace_events();
        }
        if(traceEvents!=null && traceEvents.contains(type))
            trace.add(event);
    }

    /**
     * Make sure that the returned events are removed
     * @return
     */
    public String getTrace(){
        StringBuilder sb = new StringBuilder();
        Iterator<Event> traceIterator = trace.iterator();
        if(trace.size()==1){
            Event event = trace.get(0);
            if(event instanceof Execution){
                trace.clear();
                return "";
            }
        }
        while(traceIterator.hasNext()){
            Event event = traceIterator.next();
            if(event.getLocalTimeProcessed()>=0 && event.getGlobalTimeProcessed() >= event.getGlobalTimeIssued()){
                //Flush it
                sb.append(event.toString()+"\n");
                traceIterator.remove();
            }
        }
        //if(sb.length()>1){
        //return sb.deleteCharAt(sb.length()-1).toString();
        //}
        return sb.toString();
    }

    public String getUnprocessedTrace() {
        StringBuilder sb = new StringBuilder();
        Iterator<Event> traceIterator = trace.iterator();
        while(traceIterator.hasNext()){
            Event event = traceIterator.next();
            if(event.getLocalTimeProcessed()==-1 && event.getGlobalTimeProcessed() < event.getGlobalTimeIssued()){
                //Flush it
                sb.append(event.toString()+"\n");
                traceIterator.remove();
            }
        }
        //if(sb.length()>1){
        //return sb.deleteCharAt(sb.length()-1).toString();
        //}
        return sb.toString();


    }
    public void broadcast() {

        if(this.events.isEmpty()){
            logger.trace(hashCode()+": No events to broadcast");
        }else{
            while(!events.isEmpty()){
                Event event = events.poll();
                String type = event.getClass().getTypeName();
                Set<ISubscriber> subscribers = this.subscribersMap.get(type);
                if(subscribers == null) {
                    logger.trace("No subscribers and No forwarding found for event: "+event);
                    continue;
                    //if(!forwarding.containsKey(type))
                    //continue;
                }

                for(ISubscriber subscriber : subscribers){
                    subscriber.forwardEvent(event);
                }


            }


        }
    }

}
