package cloud.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cloud.components.*;
import cloud.configurations.Parameters;
import cloud.methods.*;
/** C. Mastroianni, M. Meo, and G. Papuzzo, 
 * “Probabilistic Consolidation of Virtual Machines in Self-Organizing Cloud Data Centers,” 
 * IEEE Transactions on Cloud Computing, vol. 1, no. 2, pp. 215-228, 2013
 * */
public  class EcoCloud  implements Scheduler  {
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
		while(arrivedVMNum<vmList.size() || finishedVMNum<vmList.size()) {
			//Store the arrive VMs at currentTime
			List<VM> arriveVMList = new ArrayList<VM>();
			
			migrateCountofCurrent=0;
			usedPMCountofCurrent=0;
			timeNo = (int) Math.floor(currentTime/Parameters.cpuChangePeriod); //Calculate the actual time No.
			
			//Get the same arrive time VMs by compare the VMs after i because VMs are ascending by arrival time 
			for(int i=arrivedVMNum;i<vmList.size();i++) { 
				if(vmList.get(i).getArriveTime() == currentTime) {
					arrivedVMNum++;//Record the arrived VM in the vmList
					arriveVMList.add(vmList.get(i));
					System.out.println("ecoCloud--VM #" + vmList.get(i).getId() + " arriving at time: " + currentTime + " finish time: " + vmList.get(i).getFinishTime());
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
			
			//Execute each VM on activePM at current time
			executeVMOnPM(activePMList);
			 
			long startTime02 = System.currentTimeMillis();
			
			//Migrate the VMs on overload or lowload PMs
			migrateVM(activePMList);
			
			long endTime02 = System.currentTimeMillis();
			totalScheduleTime = totalScheduleTime + (endTime02 - startTime02);
			
			//Update the history CPU utilization,pes and ram usage of each PM in activePMList
			for(int i=0;i<activePMList.size();i++) {
				PM pm = activePMList.get(i);
				pm.setCpuUtilizeHistory(pm.getCpuUtilize());
				pm.setPesUsageHistory(pm.getTotalPes()-pm.getIdlePes());
				pm.setRamUsageHistory(pm.getTotalRam()-pm.getIdleRam());
			}
			
			//Update the usedPM and migratePM in each current time
			Parameters.activePMPerTimeList.add(activePMList.size());
			Parameters.migrateVMPerTimeList.add(migrateCountofCurrent);
			Parameters.shutdownPMPerTimeList.add(usedPMCountofCurrent);
			
			System.out.println("ecoCloud--CurrentTime: " +currentTime + " timeNo: " + timeNo + " arrivedVMNum : " + arrivedVMNum
					+ " finishedVMNum :" + finishedVMNum+" usedPMNum :" + usedPMList.size());
			currentTime++; //Go to next scheduling time
			
		} //End while, the total loop finished when the vmList is empty
		System.out.println("ecoCloud--All VMs in the vmlist have been scheduled at time: " +currentTime );
		
		//Check implementation 
		Check.checkUnfinishVMAndPM(vmList,activePMList,usedPMList);
		
		//Calculate the experimental results
		ExperimentResult.calculateExperimentResult("ecoCloud", vmList, usedPMList, migrateCount,totalScheduleTime);
		
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
			System.out.println("ecoCloud--Arriving VM #" + assignVM.getId() + " is assigned to PM #" + assignedPM.getId());
		}
	}

	/** Update the status of VM and PM when executing task at current time, including:
	 *  1.Set the VM as finish when it's finish time is less or equal to current time;
	 *  2.Set the CPU utilization of PM by the current time. */
	private void executeVMOnPM(List<PM> pmList) {
		List<PM> finishPM = new ArrayList<PM>();
		for(int i=0;i<pmList.size();i++) { //Get each PM, cannot remove the finish PM at here
			PM pm = pmList.get(i);
			List<VM> finishVM = new ArrayList<VM>(); //Store the finished VM on PM at this current time
			double currentCpuUtilize = 0;
			
			for(int j=0;j<pm.getActiveVMList().size();j++) { //Get each VM on a PM
				
				VM vm = pm.getActiveVMList().get(j);
				//Find the finish VM
				if(vm.getFinishTime()<=currentTime) {
					pm.finishedVM(vm, vm.getCpuUtilize(timeNo));
					finishVM.add(vm);
					finishedVMNum++;
					System.out.println("ecoCloud--VM #"  + vm.getId() + " with finish time: " + vm.getFinishTime() + " is finined at time: " +currentTime);
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
		usedPMCountofCurrent=usedPMCountofCurrent+finishPM.size();
		activePMList.removeAll(finishPM);
		for(PM pm : finishPM) {
			System.out.println("ecoCloud--Idle PM #" + pm.getId() + " is moved from activePMList to userPMList");
		}
		finishPM.clear();
	}
	
	/** Algorithm 2, VMMigration. Traverse all active PMs and migrate the VMs on overload or lowload PMs to the appropriate PM. */
	private void migrateVM(List<PM> pmList) {
		//Remove all PMs in the two group before classify the PMs of pmList at each run time
		migratePMList.clear();
		
		//Move the PM into migratePMList by migration probability functions
		for(int i=0;i<pmList.size();i++){
			PM pm = pmList.get(i);
			double cpuUtilizte = pm.getCpuUtilize();
			//Move lowload PMs by Eq.5 in the article
			if(cpuUtilizte<=Parameters.lowloadofCpu) {
				double pmMigratePro = Math.pow((1-cpuUtilizte)/Parameters.lowloadofCpu,Parameters.alpha);
				
				double random = new Random().nextDouble(); 
				if(random<pmMigratePro) {
					migratePMList.add(pm); //Add the migrate PM
				}
			}
			if(cpuUtilizte>=Parameters.overloadofCpu) {
				double pmMigratePro = Math.pow(1+(cpuUtilizte-1)/(1-Parameters.overloadofCpu),Parameters.beta);
				
				double random = new Random().nextDouble(); 
				if(random<pmMigratePro) {
					migratePMList.add(pm); //Add the migrate PM
				}
			}
		} //End for, check and move each PM in activePMList
		
		//Add the suitable VM into migrateVMList
		List<VM> migrateVMList = new ArrayList<VM>(); //Store the migrate VM 
		for(int i=0;i<migratePMList.size();i++){
			PM sourcePM = migratePMList.get(i);
			if(sourcePM.getCpuUtilize()<=Parameters.lowloadofCpu){
				int random = new Random().nextInt(sourcePM.getActiveVMList().size());//Random chose a VM from the lowload PM
				migrateVMList.add(sourcePM.getActiveVMList().get(random));
			}
			else {
				//Store VMs which's CPU utilization is larger than the difference between the assigned PM's CPU utilization and the upper threshold
				List<VM> suitableVMList = new ArrayList<VM>();
				double diffCpuUtilize = sourcePM.getCpuUtilize() - Parameters.overloadofCpu;
				for(int k=0;k<sourcePM.getActiveVMList().size();k++){
					VM vm = sourcePM.getActiveVMList().get(k);
					if(vm.getCpuUtilize(timeNo) > diffCpuUtilize) {
						suitableVMList.add(vm);
					}
				}
				if(suitableVMList.size()>0){
					int random = new Random().nextInt(suitableVMList.size());//Random chose a VM from the suitableVMList
					migrateVMList.add(suitableVMList.get(random));
				}
				suitableVMList.clear();
			}
		} //End add all migrate VM into the list
					
		//Migrate the VM in migrateVMList to suitable PM in the activePMList
		PM targetPM = null;
		for(int i=0;i<migrateVMList.size();i++) {
			VM migrateVM = migrateVMList.get(i);
			PM sourcePM = migrateVM.getPM();
			boolean migrateStatus = false; //Whether to migrate the migrateVM
			//Find a suitable PM
			if(getSuitablePMForMigrate(migrateVM,activePMList) != null) {
				targetPM = getSuitablePMForMigrate(migrateVM,activePMList);
			}
			else {
				if(sourcePM.getCpuUtilize()>=Parameters.overloadofCpu) { //Create a new PM for the migrate VM on overload PM
					targetPM = creatComponents.createRandomPM(migrateVM,currentTime);
					activePMList.add(targetPM);
					migrateStatus = true;
				}
				else {
					migrateStatus = false; //Set the migrateStatus as false when there is no suitable PM for the migrate VM on lowload PM
				}
				
			}
			//Migrate the VM
			if(targetPM == null && migrateStatus) {
				System.out.println("ecoCloud--There is no active or new PM can be creat for the migrate VM #" 
			+ migrateVM.getId() + "cpu/ram" + migrateVM.getRequestedPes()+"/"+migrateVM.getRequestedRam());
				System.exit(0);
			}
			if(migrateStatus) {
				sourcePM.migrateVM(migrateVM, migrateVM.getCpuUtilize(timeNo));
				targetPM.assignVM(migrateVM, migrateVM.getCpuUtilize(timeNo));
				
				migrateCount++; //Record the migrate number
				migrateCountofCurrent++;
				
				System.out.println("ecoCloud--VM #" + migrateVM.getId() + " is migrated from  PM #" + sourcePM.getId() + " to PM # " + targetPM.getId());
				
				//Shutdown the PM on which VMs are all migrated and remove it from activePMList into usedPMList
				if(sourcePM.getActiveVMList().size() == 0) {
					sourcePM.setFinishTime(currentTime);
					sourcePM.setStatus(false);
					System.out.println("ecoCloud--Idle PM #" + sourcePM.getId()+" is shutdown with the size of VMList:" + sourcePM.getActiveVMList().size());
					activePMList.remove(sourcePM);
					usedPMList.add(sourcePM);
					usedPMCountofCurrent++;
				}
				else {
					System.out.println("A lowload PM #: " + sourcePM.getId()+"the size of VMList:" + sourcePM.getActiveVMList().size());
				}
			}
		}//End migrate the VM in migrateVMList to suitable PM
	}
	
	/** Assign new VM: find a suitable PM from the list by the max probabilistic */
	private PM getSuitablePM(VM vm, List<PM> pmList) {
		//Store the PM that meet vm's require
		List<PM> suitablePMList = new ArrayList<PM>();

		//Find available PMs
		for (int i=0;i<pmList.size();i++) {
			PM chosedPM = pmList.get(i);
			double predictUtilize = vm.getCpuUtilize(timeNo) + chosedPM.getCpuUtilize();
			if (methods.isSuitPM(vm, chosedPM) && predictUtilize<Parameters.overloadofCpu) {
				double cpuUtilize = chosedPM.getCpuUtilize();
				//Calculate the PM's assignment function by the Eq.2 and 3 in the article
				double mp = (Math.pow(Parameters.p,Parameters.p)*Math.pow(Parameters.overloadofCpu,Parameters.p+1))/Math.pow(Parameters.p+1,Parameters.p+1); 
				double pmAllocationFun = (Math.pow(cpuUtilize,Parameters.p)*(Parameters.overloadofCpu-cpuUtilize))/mp;
				double random = new Random().nextDouble(); 
				//If the bernoulli trial is successful, add the PM into the suitable list
				if(random<pmAllocationFun) {
					suitablePMList.add(chosedPM);
				}
			} 
		} // End for, get the available PM list

		//Get a random PM 
		PM assignedPM = null;
		if(suitablePMList.size()>0) {
			int ranNo = new Random().nextInt(suitablePMList.size());
			assignedPM = suitablePMList.get(ranNo);
		}
		
		return assignedPM;
	}
	
	/** Assign migratedVM: Find a suitable PM from the list by the max probabilistic */
	private PM getSuitablePMForMigrate(VM vm, List<PM> pmList) {
		// Store the PM that meet vm's require
		PM assignedPM = null;
		List<PM> suitablePMList = new ArrayList<PM>();

		//Find available PMs for overload migratedVM 
		//The threshold is set to 0.9 times the resource utilization of the server that initiated the procedure
		if(vm.getPM().getCpuUtilize()>=Parameters.overloadofCpu) {
			for (int i=0;i<pmList.size();i++) {
				PM chosedPM = pmList.get(i);
				double predictUtilize = vm.getCpuUtilize(timeNo) + chosedPM.getCpuUtilize();
				if (methods.isSuitPM(vm, chosedPM) && predictUtilize<Parameters.overloadofCpu*0.9) {
					double cpuUtilize = chosedPM.getCpuUtilize();
					//Calculate the PM's assignment function by the Eq.2 and 3 in the article
					double mp = (Math.pow(Parameters.p,Parameters.p)*Math.pow(Parameters.overloadofCpu*0.9,Parameters.p+1))/Math.pow(Parameters.p+1,Parameters.p+1); 
					double pmAllocationFun = (Math.pow(cpuUtilize,Parameters.p)*(Parameters.overloadofCpu*0.9-cpuUtilize))/mp;
					double random = new Random().nextDouble(); 
					//If the bernoulli trial is successful, set the PM as the chosen PM
					if(random<pmAllocationFun) {
						suitablePMList.add(chosedPM);
					}
				} 
			}// End for, get the available PM list

			//Get a random PM 
			if(suitablePMList.size()>0) {
				int ranNo = new Random().nextInt(suitablePMList.size());
				assignedPM = suitablePMList.get(ranNo);
			}
		}
		else { //Find available PMs for lowload migratedVM 
			for (int i=0; i<pmList.size();i++) {
				PM chosedPM = pmList.get(i);
				double predictUtilize = vm.getCpuUtilize(timeNo) + chosedPM.getCpuUtilize();
				if (methods.isSuitPM(vm, chosedPM) && predictUtilize<Parameters.overloadofCpu) {
					suitablePMList.add(chosedPM);
				} 
			}// End for, get the available PM list

			//Get the random PM
			if(suitablePMList.size()>0){
				int random = new Random().nextInt(suitablePMList.size());//Random chose a VM from the suitablePMList
				assignedPM = suitablePMList.get(random);
			}
		}
				
		return assignedPM;
	}
	
	public String getName() {
		return "ecoCloud";
	}

}
