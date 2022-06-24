package cloud.methods;

import java.util.List;
import java.util.Random;
import cloud.components.*;

public class Methods {
	/** Check whether the PM is suitable for a VM */
    public boolean isSuitPM(VM vm,PM pm) {
    	if(pm.getIdlePes() >= vm.getRequestedPes() && pm.getIdleRam() >= vm.getRequestedRam()) { //Check whether meet the require
			return true;
		}
    	else {
    		return false;
    	}
    }
    	
	/**Poisson Distribution*/
	public static int PoissValue(double Lamda) {
		int value=0;
		double b=1;
		double c=0;
		c=Math.exp(-Lamda); 
		double u=0;
		do {
			u=Math.random();
			b*=u;
			if(b>=c)
				value++;
			}while(b>=c);
		return value;
	}
	
    /**
    * Generator for normal distribution
    * @param average
    * @param deviance
    * @return 
    */
    public static double NormalDistribution(double average,double deviance)
    {
    	Random random = new Random();
    	//double result = average+Math.sqrt(deviance)*random.nextGaussian();
    	double result = average+deviance*random.nextGaussian();
    	return result;
    }
    
    /**Calculate the SLA of a PM before/after assigned a VM*/
    public static double calculateSLA(List<VM> vmList, List<PM> pmList) {
  		double slaVTSum = 0; //The total SLA violation rate
  		for(PM pm : pmList) {
  			int slaVT = 0; //The SLA violation times of each PM
  			for(double cpu : pm.getCpuUtilizeHistory()) {
  				if(cpu == 1) {
  	  				slaVT++;
  	  			}
  			}
  			for(int usedPes : pm.getPesUsageHistory()) {
  				if(usedPes == pm.getTotalPes()) {
  	  				slaVT++;
  	  			}
  			}
  			for(double usedRam : pm.getRamUsageHistory()) {
  				if(usedRam == pm.getTotalRam()) {
  	  				slaVT++;
  	  			}
  			}
  			if(pm.getCpuUtilizeHistory().size() != 0) {
  				slaVTSum += slaVT/pm.getCpuUtilizeHistory().size();
  			}
  			
  		}
  		
  		double slatah= slaVTSum/pmList.size();
  		
  		double totalPDM = 0;
  		for(VM tempVM : vmList) {
			for(int timeNo : tempVM.getMigrateTimeNo()) { //If is the VM has migrated
				totalPDM += tempVM.getCpuUtilize(timeNo)*0.1/tempVM.getRequestedPes();
			}
	  	}
  		
  		double pdm = totalPDM / vmList.size();
  		
  		double slav;
  		if(pdm != 0) { //Consider assigned new VMs when there is no migrate VM
  			slav = slatah * pdm;
  		}
  		else {
  			slav = slatah;
  		}
  		
  		return slav;
  	}

}
