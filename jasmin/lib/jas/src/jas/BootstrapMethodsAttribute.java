package jas;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class BootstrapMethodsAttribute {
	
    static CP attr = new AsciiCP("BootstrapMethods");
    short attr_length = 0;
    short num = 0;
    ArrayList<Pair<MethodHandleCP,CP[]>> list = new ArrayList<Pair<MethodHandleCP,CP[]>>();
    
    int addEntry(MethodHandleCP bsm, CP[] argCPs) {
    	int i=0;
    	//search for existing equal entry
    	for (Pair<MethodHandleCP,CP[]> pair : list) {
    		MethodHandleCP mh = pair.getO1();
    		CP[] args = pair.getO2();
    		if(mh.uniq.equals(bsm.uniq)) {
    			boolean equal = true;
    			for (int j = 0; j < args.length; j++) {
					CP arg = args[j];
					CP otherArg = argCPs[j];
					if(!arg.uniq.equals(otherArg.uniq)) {
						equal = false;
						break;
					}
				}
    			if(equal) return i;
    		}
    		i++;
		}    	
    	//none found
    	
    	//add to end
    	list.add(new Pair<MethodHandleCP,CP[]>(bsm,argCPs));
    	return list.size()-1;
    }
    
    void resolve(ClassEnv e){
        e.addCPItem(attr); 
    }
    
    void write(ClassEnv e, DataOutputStream out)
            throws IOException, jasError {
        
            out.writeShort(e.getCPIndex(attr)); //u2 name;
            out.writeInt(size()); //u4 size;
            out.writeShort(list.size()); //u2 bootstrap_method_count;
            for (Pair<MethodHandleCP,CP[]> pair : list) {
                out.writeShort(e.getCPIndex(pair.getO1())); //u2 bootstrap_method_ref;
            	CP[] cps = pair.getO2(); 
            	out.writeShort(cps.length);//u2 bootstrap_argument_count;
            	for (CP cp : cps) {
                    out.writeShort(e.getCPIndex(cp)); //u2 bootstrap_arguments[bootstrap_argument_count]; 
				}
            }
    }
    
    int size() {
//    	 u2 bootstrap_method_count;
    	 int size = 2;
    	
    	 for (Pair<MethodHandleCP,CP[]> pair : list) {
//    		   u2 bootstrap_method_ref;  // index to CONSTANT_MethodHandle
//    		   u2 bootstrap_argument_count;
    		   size+=4;
    		   
    		   size+= pair.getO2().length*2; //args
    	 }
    	 return size;
    }


}
