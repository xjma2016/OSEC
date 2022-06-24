package cloud.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.distribution.BetaDistribution;

import cloud.components.*;
import cloud.configurations.Parameters;
import cloud.methods.*;
/**W. Guo, P. Kuang, Y. Jiang, et al.,
 *  “SAVE: self-adaptive consolidation of virtual machines for energy efficiency of CPU-intensive applications in the cloud,” 
 *  The Journal of Supercomputing, vol. 75, no. 11, pp. 7076-7100, 2019.
 *  */
public class SAVE implements Scheduler  {
	//Active and used PMs
	private List<PM> activePMList;
	private List<PM> usedPMList;
	
	//Store the PMs that will be migrate the VMs on it
	private List<PM> migratePMList;
	
	//Store migrate VM count of total time
	int migrateCount;
	//Store  migrate VM count of current time
	int migrateCountofCurrent;
	//Store shutdown PM count of current time
	int usedPMCountofCurrent;		
	
	//The current time in the scheduling process
	private double currentTime; 
	//The execution time cycle, equals currentTime/Parameters.cpuChangePeriod
	private int timeNo; 
	//The number of arrived VMs
	private int arrivedVMNum;
	//The number of finished VMs
	private int finishedVMNum;
	
	//The methods and creatComponents used in scheduling
	private Methods methods;
	private CreatComponents creatComponents;
	
	//Record the runtime of scheduling algorithm
	long totalScheduleTime;
		
	public void schedule(List<VM> vmList) {
		//Initialize the parameter
		activePMList= new ArrayList<PM>();
		usedPMList= new ArrayList<PM>();
		migratePMList= new ArrayList<PM>();
		
		migrateCount = 0;
		currentTime=0; 
		timeNo=0; 
		arrivedVMNum = 0;
		finishedVMNum = 0;
		totalScheduleTime = 0;
		
		methods = new Methods();
		creatComponents = new CreatComponents();
		
		//Run the scheduling when there has new arrive task or unfinished tasks
		while(arrivedVMNum < vmList.size() || finishedVMNum < vmList.size()) {
			//Store the arrive VMs  at currentTime
			List<VM> arriveVMList = new ArrayList<VM>();
			
			migrateCountofCurrent=0;
			usedPMCountofCurrent=0;
			timeNo = (int) Math.floor(currentTime/Parameters.cpuChangePeriod); //Calculate the actual time No.
			
			//Get the same arrive time VMs by compare the VMs after i because VMs are ascending by arrival time 
			for(int i=arrivedVMNum;i<vmList.size();i++) { 
				if(vmList.get(i).getArriveTime() == currentTime) {
					arrivedVMNum++;//Record the arrived VM in the vmList
					arriveVMList.add(vmList.get(i));
					System.out.println("SAVE--VM #" + vmList.get(i).getId() + " arriving at time: " + currentTime + " finish time: " + vmList.get(i).getFinishTime());
				}
				else {
					break;//Break 'for i' after get all VMs that have the same arriveTime because the ascending arrival time
				}
			}
			
			long startTime01 = System.currentTimeMillis();
			
			//Schedule the new arriving VMs
			for(int i=0;i<arriveVMList.size();i++) {
				VM newVM = arriveVMList.get(i);
				scheduleVM(newVM);
			}
			
			long endTime01 = System.currentTimeMillis();
			totalScheduleTime = totalScheduleTime + (endTime01 - startTime01);
			
			//Execute each VM at current time
			executeVMOnPM(activePMList);
			 
			long startTime02 = System.currentTimeMillis();
			
			//Migrate the VMs on overload or lowload PMs
			migrateVM(activePMList);
			
			long endTime02 = System.currentTimeMillis();
			totalScheduleTime = totalScheduleTime + (endTime02 - startTime02);
			
			//Update the history CPU utilization,pes and ram usage of each PM in activePMList
			for(int i=0;i<activePMList.size();i++) {
				PM pm = activePMList.get(i);
				if(pm.getCpuUtilize() == 0) {
					pm.setCpuUtilizeHistory(0.1);
				}
				else {
					pm.setCpuUtilizeHistory(pm.getCpuUtilize());
				}
				
				pm.setPesUsageHistory(pm.getTotalPes()-pm.getIdlePes());
				pm.setRamUsageHistory(pm.getTotalRam()-pm.getIdleRam());
			}
			
			//Update the usedPM and migratePM in each current time
			Parameters.activePMPerTimeList.add(activePMList.size());
			Parameters.migrateVMPerTimeList.add(migrateCountofCurrent);
			Parameters.shutdownPMPerTimeList.add(usedPMCountofCurrent);
			
			System.out.println("SAVE--CurrentTime: " +currentTime + " timeNo: " + timeNo + " arrivedVMNum : " + arrivedVMNum+ " finishedVMNum :" + finishedVMNum);
			currentTime++; //Go to next scheduling time
			
		} //End while, the total loop finished when the vmList is empty
		System.out.println("SAVE--All VMs in the vmlist have been scheduled at time: " +currentTime );
		
		//Check implementation 
		Check.checkUnfinishVMAndPM(vmList,activePMList,usedPMList);
		
		//Calculate the experimental results
		ExperimentResult.calculateExperimentResult("SAVE", vmList, usedPMList, migrateCount,totalScheduleTime);
		
		//Clear the lists
		vmList.clear();
		activePMList.clear();
		usedPMList.clear();
		migratePMList.clear();
		
		//Reset the id as 0 when finish the experiment of the algorithm
		PM.resetInnerId(); 
		VM.resetInnerId(); 
	}

	/** Algorithm 1 VMAllocation. */
	private void scheduleVM(VM assignVM) {
		//Find suitable PM from activePMList 
		PM assignedPM = getSuitablePM(assignVM, activePMList);
		
		//Create new PM when there is no suitable PM in active PM list
		if (assignedPM == null) {
			assignedPM = creatComponents.createRandomPM(assignVM,currentTime);
			if (assignedPM != null) {
				activePMList.add(assignedPM);
			} 
			else {
				System.out.println("There is no new PM can be creat for the requirement of VM #" + assignVM.getId());
				System.exit(0);
			}
		}

		//Assign VM on the chosen PM
		if(assignedPM.assignVM(assignVM, assignVM.getCpuUtilize(timeNo))) {
			System.out.println("SAVE--Arriving VM #" + assignVM.getId() + " is assigned to PM #" + assignedPM.getId());
		}
	}

	/** Update the status of VM and PM when executing task at current time, including:
	 *  1.Set the VM as finish when it's finish time is less or equal to current time;
	 *  2.Set the CPU utilization of PM by the current time. */
	private void executeVMOnPM(List<PM> pmList) {
		List<PM> finishPM = new ArrayList<PM>();
		for(int i=0;i<pmList.size();i++) { //Get each PM, cannot remove the finish PM at here
			PM pm = pmList.get(i);
			List<VM> finishVM = new ArrayList<VM>();
			double currentCpuUtilize = 0;
			
			for(int j=0;j<pm.getActiveVMList().size();j++) { //Get each VM on a PM
				
				VM vm = pm.getActiveVMList().get(j);
				//Set the finish VM
				if(vm.getFinishTime()<=currentTime) {
					pm.finishedVM(vm, vm.getCpuUtilize(timeNo));
					finishVM.add(vm);
					finishedVMNum++;
					System.out.println("SAVE--VM #"  + vm.getId() + " with finish time: " + vm.getFinishTime() + " is finined at time: " +currentTime);
				}
				//Calculate the total CPU utilization by add the value of each VM
				currentCpuUtilize  = currentCpuUtilize + vm.getCpuUtilize(timeNo);
			}
			
			//Remove the finished VM in the list
			pm.getActiveVMList().removeAll(finishVM);
			finishVM.clear();
			
			//Update the CPU utilization of PM
			pm.setCpuUtilize(currentCpuUtilize);
			
			//Add the finish PM into list when there is no VM in it's activeVMList
			if(pm.getActiveVMList().size()==0) {
				finishPM.add(pm);
				pm.setFinishTime(currentTime);
				pm.setStatus(false);
			}
		}
		
		//Shutdown the PM after execute each PM
		usedPMList.addAll(finishPM);
		usedPMCountofCurrent = usedPMCountofCurrent + finishPM.size();
		activePMList.removeAll(finishPM);
		for(PM pm : finishPM) {
			System.out.println("SAVE--Idle PM #" + pm.getId() + " is moved from activePMList to userPMList");
		}
		finishPM.clear();
	}
	
	/** Algorithm 2, VMMigration. Traverse all active PMs and migrate the VMs on overload or lowload PMs to the appropriate PM. */
	private void migrateVM(List<PM> pmList) {
		//Remove all PMs in the two group before classify the PMs of pmList at each run time
		System.out.println("*************SAVE start migrate***********");
		migratePMList.clear();
		
		//Move the PM with different CPU utilization to different PM list
		for(int i=0;i<pmList.size();i++){
			PM pm = pmList.get(i);
			double cpuUtilizte = pm.getCpuUtilize();
			if(cpuUtilizte<=Parameters.lowloadofCpu || cpuUtilizte>=Parameters.overloadofCpu) {
				BetaDistribution beta=new BetaDistribution(Parameters.alpha, Parameters.beta);
				double migrateBeta = -1 * beta.density(cpuUtilizte);
				double betaDistribute = (migrateBeta/3)+1;
				
				double random = new Random().nextDouble(); 
				if(random<betaDistribute) {
					if(cpuUtilizte>=Parameters.overloadofCpu){
						migratePMList.add(pm); //Add the overload migrate PM
					}
					
					if(cpuUtilizte<=Parameters.lowloadofCpu && activePMList.size()>1) { //Migrate lowload PM Only activePMList has more than one PM
						migratePMList.add(pm);
					}
				}
			}
		} //End for, check and move each PM in activePMList
		
		//Add the suitable VM into migratePMList
		List<VM> migrateVMList = new ArrayList<VM>(); //Store the migrate VM 
		for(int i=0;i<migratePMList.size();i++){
			PM sourcePM = migratePMList.get(i);
			if(sourcePM.getCpuUtilize()<=Parameters.lowloadofCpu && activePMList.size()>1){
				migrateVMList.addAll(sourcePM.getActiveVMList());
			}
			else {
				//Chose the suitable VM in overloadPM that let the PM has max CPU utilization while meet the upper limit
				double maxCpuUtilizeAfterMigrate = 0;
				VM migrateVM = null;
				for(int k=0;k<sourcePM.getActiveVMList().size();k++){ //Get the suitable VM
					VM vm = sourcePM.getActiveVMList().get(k);
					double tempCpuUtilizeAferMigrate = sourcePM.getCpuUtilize() - vm.getCpuUtilize(timeNo);
					if(tempCpuUtilizeAferMigrate <= Parameters.overloadofCpu && tempCpuUtilizeAferMigrate > maxCpuUtilizeAfterMigrate) {
						maxCpuUtilizeAfterMigrate = tempCpuUtilizeAferMigrate;
						migrateVM = vm;
					}
				}
				if(migrateVM == null) { //Get the max CPU utilization VM if cann't get a suitable VM
					double maxCpuUtilize = 0;
					for(int k=0;k<sourcePM.getActiveVMList().size();k++){
						VM vm = sourcePM.getActiveVMList().get(k);
						double tempCpuUtilize = vm.getCpuUtilize(timeNo);
						if(tempCpuUtilize > maxCpuUtilize) {
							maxCpuUtilize = tempCpuUtilize;
							migrateVM = vm;
						}
					}
				}
				migrateVMList.add(migrateVM);
			}
		} //End add all migrate VM into the list
		
		//Migrate the VM in migrateVMList to suitable PM in the activePMList, or create a new PM
		PM targetPM = null;
		for(int i=0;i<migrateVMList.size();i++) {
			VM migrateVM = migrateVMList.get(i);
			PM sourcePM = migrateVM.getPM();
			//Find a suitable PM
			if(getSuitablePM(migrateVM,activePMList) != null) {
				targetPM = getSuitablePM(migrateVM,activePMList);
			}
			else {
				System.out.println("SAVE--creat a new PM, activePMList size is: "+activePMList.size()+" migratePMList size is: "+migratePMList.size());
				targetPM = creatComponents.createRandomPM(migrateVM,currentTime);
				activePMList.add(targetPM);
			}
			//Migrate the VM
			if(targetPM == null ) {
				System.out.println("There is no active or new PM can be creat for the migrate VM #" 
			+ migrateVM.getId() + "cpu/ram" + migrateVM.getRequestedPes()+"/"+migrateVM.getRequestedRam());
				System.exit(0);
			}
			else {
				if(sourcePM.getId() != targetPM.getId()) {
					sourcePM.migrateVM(migrateVM, migrateVM.getCpuUtilize(timeNo));
					targetPM.assignVM(migrateVM, migrateVM.getCpuUtilize(timeNo));
					
					migrateCount++;
					migrateCountofCurrent++;
					
					System.out.println("SAVE--VM #" + migrateVM.getId() + " is migrated from  PM #" + sourcePM.getId() + " to PM # " + targetPM.getId());
					
					//Shutdown the PM on which VMs are all migrated and remove it from activePMList into usedPMList
					if(sourcePM.getActiveVMList().size() == 0) {
						sourcePM.setFinishTime(currentTime);
						sourcePM.setStatus(false);
						System.out.println("SAVE--Idle PM #" + sourcePM.getId()+" is shutdown with the size of VMList:" + sourcePM.getActiveVMList().size());
						activePMList.remove(sourcePM);
						usedPMList.add(sourcePM);
						usedPMCountofCurrent++;
					}
					else {
						System.out.println("A lowload PM #: " + sourcePM.getId()+"the size of VMList:" + sourcePM.getActiveVMList().size());
					}
				}
				
			}
		}//End migrate the VM in migrateVMList to suitable PM
	}
	
	/** Find a suitable PM from the list by the max probabilistic */
	private PM getSuitablePM(VM vm, List<PM> pmList) {
		// Store the PM that meet vm's require
		List<PM> suitablePMList = new ArrayList<PM>();

		//Find available PMs
		for (int i = 0; i < pmList.size(); i++) {
			PM chosedPM = pmList.get(i);
			double predictUtilize = vm.getCpuUtilize(timeNo) + chosedPM.getCpuUtilize();
			if (methods.isSuitPM(vm, chosedPM) && predictUtilize < Parameters.overloadofCpu) {
				suitablePMList.add(chosedPM);
			} 
		} // End for, get the available PM list

		//Get the PM has the max probabilistic 
		PM assignedPM = null;
		double maxProbability = 0;

		for (PM pm : suitablePMList) {
			double cpuUtilize = pm.getCpuUtilize();
			double tempProbability = (1-Math.exp(-1*cpuUtilize))/(1-Math.exp(-1)); //Calculate by the Eq.1 in the article
			if (vm.getRequestedPes() > pm.getIdlePes() || vm.getRequestedRam() > pm.getIdleRam()) {
				System.out.println("There has a PM in suitablePMList cannot meet the requirement of VM!");
				System.exit(0);
			}
			if (tempProbability > maxProbability) {
				maxProbability = tempProbability;
				assignedPM = pm;
			}
		}

		return assignedPM;
	}
	
	public String getName() {
		return "SAVE";
	}

}
