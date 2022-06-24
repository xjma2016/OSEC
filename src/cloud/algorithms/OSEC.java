package cloud.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cloud.components.*;
import cloud.configurations.Parameters;
import cloud.methods.*;

public class OSEC implements Scheduler {
	//Active and used PMs
	private List<PM> activePMList;
	private List<PM> usedPMList;

	//Three PMs group in activePMLists
	private List<PM> lowloadPMList;
	private List<PM> appropriatePMList;
	private List<PM> overloadPMList;

	//Store the arrive VMs at currentTime
	private List<VM> arriveVMList;
	//Store the migrate VMs at currentTime
	private List<VM> migrateVMList;

	//The idle CPU and RAM of active PMs when scheduling the new arriving VMs
	List<Integer> idleCPU;
	List<Integer> idleRAM;

	//Store migrate VM count of total time
	int migrateCount;
	//Store migrate VM count of current time
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
	//The best PM index for each group arrive VM
	private int[] assignPMIndex;

	//The methods and creatComponents used in scheduling
	private Qlearning q;// Q-learning method
	private CreatComponents creatComponents;
 
	//Record the runtime of scheduling algorithm
	long totalScheduleTime;
	
	public void schedule(List<VM> vmList) {
		//Initialize the parameter
		activePMList = new ArrayList<PM>();
		usedPMList = new ArrayList<PM>();

		lowloadPMList = new ArrayList<PM>();
		appropriatePMList = new ArrayList<PM>();
		overloadPMList = new ArrayList<PM>();
		migrateVMList = new ArrayList<VM>();

		idleCPU = new ArrayList<Integer>();
		idleRAM = new ArrayList<Integer>();

		creatComponents = new CreatComponents();
		
		migrateCount = 0;
		currentTime = 0;
		timeNo = 0;
		arrivedVMNum = 0;
		finishedVMNum = 0;
		totalScheduleTime = 0;

		//Run the scheduling when there has new arrive task or unfinished tasks
		while (arrivedVMNum < vmList.size() || finishedVMNum < vmList.size()) {
			arriveVMList = new ArrayList<VM>();
			
			migrateCountofCurrent = 0;
			usedPMCountofCurrent = 0;
			timeNo = (int) Math.floor(currentTime / Parameters.cpuChangePeriod); // Calculate the actual time No.

			//Get the same arrive time VMs by compare the VMs after i because VMs are
			//Ascending by arrival time
			for (int i = arrivedVMNum; i < vmList.size(); i++) {
				if (vmList.get(i).getArriveTime() == currentTime) {
					arrivedVMNum++; //Record the arrived VM in the vmList
					arriveVMList.add(vmList.get(i));
					System.out.println("OSEC--VM #" + vmList.get(i).getId() + " arriving at time:" + currentTime + " finish time: " + vmList.get(i).getFinishTime());
				} 
				else {
					break; //Break 'for i' after get all VMs that have the same arriveTime because the ascending arrival time
				}
			}

			Collections.sort(overloadPMList, new comparePMByCpuUtilize());

			long startTime01 = System.currentTimeMillis();
			
			//Schedule the new arriving VMs
			scheduleVMList(arriveVMList,activePMList);
			
			long endTime01 = System.currentTimeMillis();
			totalScheduleTime = totalScheduleTime + (endTime01 - startTime01);
			
			//Execute each VM at current time
			executeVMOnPM(activePMList);

			long startTime02 = System.currentTimeMillis();
			
			migrateVM(activePMList);
			
			long endTime02 = System.currentTimeMillis();
			totalScheduleTime = totalScheduleTime + (endTime02 - startTime02);
			
			//Update the history CPU utilization, pes and ram usage of each PM in activePMList and usedPMList
			for (int i = 0; i < activePMList.size(); i++) {
				PM pm = activePMList.get(i);
				pm.setCpuUtilizeHistory(pm.getCpuUtilize());
				pm.setPesUsageHistory(pm.getTotalPes() - pm.getIdlePes());
				pm.setRamUsageHistory(pm.getTotalRam() - pm.getIdleRam());
			}

			//Update the usedPM and migratePM in each current time
			Parameters.activePMPerTimeList.add(activePMList.size());
			Parameters.migrateVMPerTimeList.add(migrateCountofCurrent);
			Parameters.shutdownPMPerTimeList.add(usedPMCountofCurrent);
			
			System.out.println("OSEC--CurrentTime: " +currentTime + " timeNo: " + timeNo + " arrivedVMNum : " + arrivedVMNum+ " finishedVMNum :" + finishedVMNum);
			currentTime++;
		} //End while, the total loop finished when the vmList is empty
		
		System.out.println("OSEC--All VMs in the vmlist have been scheduled at time: " + currentTime);

		//Check schedule results
		Check.checkUnfinishVMAndPM(vmList, activePMList, usedPMList);

		//Calculate the experimental results
		ExperimentResult.calculateExperimentResult("OSEC", vmList, usedPMList, migrateCount,totalScheduleTime);

		//Clear the lists
		vmList.clear();
		activePMList.clear();
		usedPMList.clear();
		overloadPMList.clear();
		appropriatePMList.clear();
		lowloadPMList.clear();

		//Reset the id as 0 when finish the experiment of the algorithm
		PM.resetInnerId();
		VM.resetInnerId();
	}

	/** Algorithm 1 VMAllocation. */
	private void scheduleVMList(List<VM> vmList,List<PM> pmList) {
		int assigneVMCount = 0;
		if (vmList.size() != 0) {
			q = new Qlearning(vmList, pmList, currentTime);
			assignPMIndex = new int[pmList.size()];
			assignPMIndex = q.getBestPMIndex();
			
			//Check the result of Q-learning
			VM assignVM;
			PM assignedPM;
			for (int i = 0; i < vmList.size(); i++) {
				assignVM = vmList.get(i);
				assignedPM = pmList.get(assignPMIndex[i]);
				
				//Assign VM on the best PM
				if (assignedPM.assignVM(assignVM, assignVM.getCpuUtilize(timeNo))) {
					assigneVMCount++;
					
					if(assignVM.getLastPM() == assignedPM ) { //If the new assigned PM is the migrated PM then ignore the migrate count
						migrateCount--;
					}
				} 
				else {//Create new PM if cannot meet the requirement
					PM newPM = creatComponents.createMinPowerPM(assignVM, currentTime, assignVM.getCpuUtilize(timeNo));
					if (newPM != null) {
						pmList.add(newPM);
						newPM.assignVM(assignVM, assignVM.getCpuUtilize(timeNo));
						assigneVMCount++;
					} 
					else {
						System.out.println("There is no new PM can be creat for the requirement of VM #" + assignVM.getId());
						System.exit(0);
					}
				}
			}
		}

		if (assigneVMCount != vmList.size()) {
			System.out.println("There is unassigned VM");
			System.exit(0);
		}
	}

	/**
	 * Update the status of VM and PM when executing task at current time,
	 * including: 1.Set the VM as finish when it's finish time is less or equal to
	 * current time; 2.Set the CPU utilization of PM by the current time.
	 */
	private void executeVMOnPM(List<PM> pmList) {
		List<PM> finishPM = new ArrayList<PM>();
		for (int i = 0; i < pmList.size(); i++) { //Get each PM, cannot remove the finish PM at here
			PM pm = pmList.get(i);
			List<VM> vmList = pm.getActiveVMList();
			List<VM> finishVM = new ArrayList<VM>();
			double currentCpuUtilize = 0;

			for (int j = 0; j < vmList.size(); j++) { //Get each VM on a PM
				VM vm = vmList.get(j);
				//Set the finish VM
				if (vm.getFinishTime() <= currentTime) {
					pm.finishedVM(vm, vm.getCpuUtilize(timeNo));
					finishVM.add(vm);
					finishedVMNum++;
					System.out.println("OSEC--VM #" + vm.getId() + " with finish time: " + vm.getFinishTime()
							+ " is finined at time: " + currentTime);
				}
				//Calculate the total CPU utilization by add the value of each VM
				currentCpuUtilize = currentCpuUtilize + vm.getCpuUtilize(timeNo);
			}

			//Remove the finished VM in the list
			pm.getActiveVMList().removeAll(finishVM);
			finishVM.clear();

			//Update the CPU utilization of PM
			pm.setCpuUtilize(currentCpuUtilize);

			//Add the finish PM into list when there is no VM in it's activeVMList
			if (pm.getActiveVMList().size() == 0) {
				finishPM.add(pm);
				pm.setFinishTime(currentTime);
				pm.setStatus(false);
			}
		}

		//Shutdown the PM after execute each PM
		usedPMList.addAll(finishPM);
		usedPMCountofCurrent = usedPMCountofCurrent + finishPM.size();
		activePMList.removeAll(finishPM);
		for (PM pm : finishPM) {
			System.out.println("OSEC--Idle PM #" + pm.getId() + " is moved from activePMList to userPMList");
		}
		finishPM.clear();
	}

	/**
	 * Algorithm 2, VMMigration. Traverse all active PMs and migrate the VMs on
	 * overload PMs to the appropriate/lowload PM by Algorithm 1.
	 */
	private void migrateVM(List<PM> pmList) {
		System.out.println("*************OSEC start migrate***********");
		overloadPMList.clear();
		appropriatePMList.clear();
		lowloadPMList.clear();
		migrateVMList.clear();

		//Move the PM with different CPU utilization to different PM list
		for (int i = 0; i < pmList.size(); i++) {
			PM pm = pmList.get(i);
			double cpuUtilize = pm.getCpuUtilize();
			if (cpuUtilize > Parameters.overloadofCpu) {
				overloadPMList.add(pm);
			} 
			else if (cpuUtilize < Parameters.lowloadofCpu) {
				lowloadPMList.add(pm);
			} 
			else {
				appropriatePMList.add(pm);
			}
		} //End for, check and move each PM in activePMList

		for (int i = 0; i < overloadPMList.size(); i++) {
			PM sourcePM = overloadPMList.get(i);
			double CPUUtilize = sourcePM.getCpuUtilize();
			//Chose the suitable VM in overloadPMList that let the PM has max CPU utilization while meet the upper limit
			while(CPUUtilize >= Parameters.overloadofCpu) {
				double maxCpuUtilizeAfterMigrate = Double.MIN_VALUE;
				VM migrateVM = null;
				for (int k = 0; k < sourcePM.getActiveVMList().size(); k++) {
					VM vm = sourcePM.getActiveVMList().get(k);
					double tempCpuUtilizeAferMigrate = sourcePM.getCpuUtilize() - vm.getCpuUtilize(timeNo);
					if (tempCpuUtilizeAferMigrate < Parameters.overloadofCpu
							&& tempCpuUtilizeAferMigrate > maxCpuUtilizeAfterMigrate) {
						maxCpuUtilizeAfterMigrate = tempCpuUtilizeAferMigrate;
						migrateVM = vm;
					}
				}
				//Chose the VM that has max CPU utilization when migrateVM is null
				if (migrateVM == null) {
					double maxCpuUtilize = Double.MIN_VALUE;
					for (int k = 0; k < sourcePM.getActiveVMList().size(); k++) {
						VM vm = sourcePM.getActiveVMList().get(k);
						double tempCpuUtilize = vm.getCpuUtilize(timeNo);
						if (tempCpuUtilize > maxCpuUtilize) {
							maxCpuUtilize = tempCpuUtilize;
							migrateVM = vm;
						}
					}
				}
				sourcePM.migrateVM(migrateVM, migrateVM.getCpuUtilize(timeNo));
				migrateVMList.add(migrateVM);// Add the migrateVM into list
				CPUUtilize = sourcePM.getCpuUtilize();
			}
			
		}// End for, choose the suitable VM in overloadPMList into migrateVMList
		
		//Migrate all VMs of suitable PM in lowloadPMList
		while(lowloadPMList.size()>1) { //Migrate VM only there has more than one little loaded PM
			PM pm = lowloadPMList.get(lowloadPMList.size()-1); //Chose the last PM when assign object PM maybe the first one
			migrateVMList.addAll(pm.getActiveVMList());
			while(pm.getActiveVMList().size() != 0) {
				pm.migrateVM(pm.getActiveVMList().get(0), pm.getActiveVMList().get(0).getCpuUtilize(timeNo));
			}
			lowloadPMList.remove(pm);	
		}
				
		for(VM vm : migrateVMList) {
			vm.setMigrateTimeNo(timeNo);
		}
		migrateCount += migrateVMList.size();
		migrateCountofCurrent += migrateVMList.size();	
		System.out.println("*************OSEC start migrate***********");
		scheduleVMList(migrateVMList,activePMList);
	}

	/** Compare two PM by their CpuUtilize */
	private class comparePMByCpuUtilize implements Comparator<PM> {
		public int compare(PM p1, PM p2) {
			if (p1.getCpuUtilize() > p2.getCpuUtilize()) {
				return 1;
			} else if (p1.getCpuUtilize() < p2.getCpuUtilize()) {
				return -1;
			} else {
				return 0;
			}
		}
	}

	public String getName() {
		return "OSEC";
	}

}
