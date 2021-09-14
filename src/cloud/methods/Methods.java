package cloud.methods;

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
    public static int  PoissValue(double lamda) {
    	int x = 0;
    	double y = Math.random(), cdf = getPossionProbability(x, lamda);
    	while (cdf < y) {
    		x++;
    		cdf += getPossionProbability(x, lamda);
    	}
    	return x;
    }
     
    public static double getPossionProbability(int k, double lamda) {
    	double c = Math.exp(-lamda), sum = 1;
    	for (int i = 1; i <= k; i++) {
    		sum *= lamda / i;
    	}
    	return sum * c;
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
    	double result = average+deviance*random.nextGaussian();
    	return result;
    }

}
