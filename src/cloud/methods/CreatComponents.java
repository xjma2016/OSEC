package cloud.methods;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cloud.components.*;
import cloud.configurations.Parameters;

public class CreatComponents {

	/** Use the random tasks from the workflow template to get the VM list, each task bind to a VM. */
	 public  void createVMList() throws IOException, ClassNotFoundException{
		// Creates a container to store VM
		List<VM> vmList = new ArrayList<VM>();

		//Get tasks list
		List<Task> taskList = new ArrayList<Task>(); //Store the tasks band to VMs
		TaskGenerator taskGenerator = new TaskGenerator();
		taskList = taskGenerator.generateTask(Parameters.vmNum); //The number of task and VM is equal because of only one task is assigned on one VM
				
		//VM parameters
		int arriveTime = 0; //The arrive time of each workflow
		double gtCpuUtilize = 0;
		while (vmList.size()<Parameters.vmNum){
			int gtNum = Methods.PoissValue(Parameters.arrivalLamda); //Get the number of VMs generated in this arrive time
			if(gtNum == 0) {
				arriveTime++; //Go to next second  arriveTime++
				continue;
			}
			else {
				for(int i=0;i<gtNum;i++) { //Bind gtNum tasks to VMs
					if(taskList.size()>0) { //Ensure the taskList is not empty
						Task bindTask = taskList.get(0);
						Check.checkTask(bindTask);
						int randVMType =   new Random().nextInt(Parameters.vmTypeNumber); //Random choose a VM type 
						VM newVM = new VM(Parameters.vmPes[randVMType],Parameters.vmRam[randVMType],Parameters.vmPrice[randVMType]);
						//Get actual CPU utilization into the cpuUtilize list when execute at each time
						for(int j=0;j<Parameters.assumedExTime;j++) { //The assumedExTime is preliminary assumed by the number of VMs
							gtCpuUtilize = Methods.NormalDistribution(Parameters.maxAgCpuUtilize, Parameters.dvCpuUtilize); 
							if(gtCpuUtilize<0) {
								gtCpuUtilize = Parameters.maxAgCpuUtilize;
							}
							newVM.setCpuUtilize(j, gtCpuUtilize/100); //Set the CPU utilization percentage at different execution intervals
						}
						newVM.setTask(bindTask);
						newVM.setArriveTime(arriveTime);
						newVM.setFinishTime(arriveTime+bindTask.getBaseExecuteTime());
						vmList.add(newVM);
						taskList.remove(bindTask);
					}
				} //End for, bind gtNum tasks to VMs
			}
		} //End while
		
		taskList.clear();
		
		//Write the VM templates into txt file, cannot directly write to memory object
		FileOutputStream fos = new FileOutputStream(Parameters.vmTemplateFile);
		ObjectOutputStream os = new ObjectOutputStream(fos);
		try {
			for(int i=0; i<vmList.size(); i++) {
				os.writeObject(vmList.get(i));
				}
			os.close();
			fos.close();
			}
		catch(IOException e) {
			System.out.println(e.getMessage());
			}
		vmList.clear();
		System.gc();
	}
	 
	 /** Random create a new PM that meet the require of a VM. */
	 public  PM createRandomPM(VM vm, double startTime){
		int pmType = Parameters.pmPes.length;
		int randNum = new Random().nextInt(pmType);
		int count = 0;
		//Get whether meet the require
		while(Parameters.pmPes[randNum] < vm.getRequestedPes() || Parameters.pmRam[randNum] < vm.getRequestedRam()) { 
			randNum = new Random().nextInt(pmType);
			count++;
			if(count>100) { //Avoid fail to meet the PM that meet conditions 
				return null;	
			}
		}
		
		int pmPes = Parameters.pmPes[randNum];
		double pmRam = Parameters.pmRam[randNum];  //host memory (MB)
		double pmMIPS = Parameters.pmMIPS[randNum];  //host memory (MB)
		double maxPower = Parameters.maxPower[randNum]; 
		double idlePower = Parameters.idlePower[randNum]; 
		double ratioPower = Parameters.ratioPower[randNum]; 
		PM newPM = new PM(pmPes,pmRam,pmMIPS,maxPower,idlePower,ratioPower,startTime);
		return newPM;
	 }
	 
	 /** Create a min power new PM that meet the require of a VM. */
	 public  PM createMinPowerPM(VM vm, double startTime,double cpuUtilize){
		int pmType = Parameters.pmPes.length;
		int pmPes = -1;
		double pmRam = -1;  //host memory (MB)
		double pmMIPS = -1;
		double maxPower = -1; 
		double idlePower = -1; 
		double ratioPower = -1; 
		
		double minPower = Double.MAX_VALUE;
		int chosedNum = -1;
		for(int i=0;i<pmType;i++){//Check each PM type
			pmPes = Parameters.pmPes[i];
			pmRam = Parameters.pmRam[i];  //host memory (MB)
			pmMIPS = Parameters.pmMIPS[i];
			maxPower = Parameters.maxPower[i]; 
			idlePower = Parameters.idlePower[i]; 
			ratioPower = Parameters.ratioPower[i]; 
			double powerConsumpofPM = idlePower + (maxPower-idlePower)*cpuUtilize;
			
			if(pmPes >= vm.getRequestedPes() && pmRam >= vm.getRequestedRam()) { //Check whether meet the require
				if(powerConsumpofPM<minPower) {
					chosedNum = i;
				}
			}
		}
		if(chosedNum == -1) {
			return null;
		}
		else {
			PM newPM = new PM(pmPes,pmRam,pmMIPS,maxPower,idlePower,ratioPower,startTime);
			System.out.println("A new PM is creat  "+newPM.getId());
			return newPM;
		}
	 }
	 
	 /** Create a new PM that has max resources. */
	 public  PM createMaxPM(double startTime){
		int pmType = Parameters.pmPes.length-1;
		int pmPes = Parameters.pmPes[pmType];
		double pmRam = Parameters.pmRam[pmType];  //host memory (MB)
		double pmMIPS = Parameters.pmMIPS[pmType];
		double maxPower = Parameters.maxPower[pmType]; 
		double idlePower = Parameters.idlePower[pmType]; 
		double ratioPower = Parameters.ratioPower[pmType]; 
		PM newPM = new PM(pmPes,pmRam,pmMIPS,maxPower,idlePower,ratioPower,startTime);
		return newPM;
	 }
}
