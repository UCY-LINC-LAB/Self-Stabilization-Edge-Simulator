package dsslib.utilities;


import dsslib.process.IProcess;

import java.util.*;

public class UtilRand {

    public static List<IProcess> getRandom(Random rnd, List <IProcess> processList, int count){
        processList.sort(Comparator.comparing(IProcess::getUuid));
        Set<Integer> rands = new TreeSet<>();
        List<IProcess> p = new ArrayList<>(processList);
        List<IProcess> res = new LinkedList<>();
        int i=0;
        if(count>=p.size()){
            return p;
        }
        while(i<count){
            int r = rnd.nextInt(p.size());
            if(rands.add(r)){
                i++;
                res.add(p.get(r));
            }
        }

        return res;
    }
}
