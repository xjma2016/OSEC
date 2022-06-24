package cloud.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cloud.components.ExperimentResult;
import cloud.components.PM;
import cloud.components.VM;
import cloud.configurations.Parameters;
import cloud.methods.Check;
import cloud.methods.CreatComponents;

/**Z. Zhou, M. Shojafar, M. Alazab, et al,
 *  “AFED-EF: An Energy-Efficient VM Allocation Algorithm for IoT Applications in a Cloud Data Center[J]. 
 *  IEEE Transactions on Green Communications and Networking, 2021, 5(2): 658-669.”
 *  */
public class AFEDEF  implements Scheduler {
	//Active and used PMs
	private List<PM> activePMList;
	private List<PM> usedPMList;

	//Two PMs group in activePMLists
	private List<PM> littleLoadedPMList; //Used for assign VMs when the PMs are all little loaded
	private List<PM> lightlyLoadedPMList; //serverListOne
	private List<PM> normallyLoadedPMList; //serverListTwo
	private List<PM> heailyloadedPMList;//Used for migrate VMs when the PMs are all heavy loaded
	//Four thresholds of PM's CPU
	private double tLight,tNormal,tMedium,tHeavy;
	
	//SAL metrics
	private double slatah; //SLA violation time per active host
	private double pdm; //Performance degradation caused by VM migration
	private double slav; //SLA violation
	
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

	//The methods and creatComponents used in scheduling
	private CreatComponents creatComponents;

	//Record the runtime of scheduling algorithm
	long totalScheduleTime;

	public void schedule(List<VM> vmList) {
		// Initialize the parameter
		activePMList = new ArrayList<PM>();
		usedPMList = new ArrayList<PM>();

		littleLoadedPMList = new ArrayList<PM>();
		lightlyLoadedPMList = new ArrayList<PM>();
		normallyLoadedPMList = new ArrayList<PM>();
		heailyloadedPMList = new ArrayList<PM>();
		migrateVMList = new ArrayList<VM>();

		tLight = 0;
		tNormal = 0;
		tMedium = 0;
		tHeavy = 0;
		
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
			timeNo = (int) Math.floor(currentTime / Parameters.cpuChangePeriod); //Calculate the actual time No.

			//Get the same arrive time VMs by compare the VMs after i because VMs are
			//Ascending by arrival time
			for (int i = arrivedVMNum; i < vmList.size(); i++) {
				if (vmList.get(i).getArriveTime() == currentTime) {
					arrivedVMNum++; // Record the arrived VM in the vmList
					arriveVMList.add(vmList.get(i));
					System.out.println("AFEDEF--VM #" + vmList.get(i).getId() + " arriving at time:" + currentTime
							+ " finish time: " + vmList.get(i).getFinishTime());
				} 
				else{
					break; //Break 'for i' after get all VMs that have the same arriveTime because the ascending arrival time
				}
			}

			long startTime01 = System.currentTimeMillis();

			//Schedule the new arriving VMs
			scheduleVMList(arriveVMList, activePMList);

			long endTime01 = System.currentTimeMillis();
			totalScheduleTime = totalScheduleTime + (endTime01 - startTime01);
			
			//Execute each VM at current time
			executeVMOnPM(activePMList);

			long startTime02 = System.currentTimeMillis();

			migrateVM(activePMList);

			long endTime02 = System.currentTimeMillis();
			totalScheduleTime = totalScheduleTime + (endTime02 - startTime02);

			//Update the history CPU utilization, pes and ram usage of each PM in
			//activePMList and usedPMList
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

			System.out.println("AFEDEF--CurrentTime: " + currentTime + " timeNo: " + timeNo + " arrivedVMNum : "
					+ arrivedVMNum + " finishedVMNum :" + finishedVMNum+" usedPMNum :" + usedPMList.size());
			currentTime++;
		} //End while, the total loop finished when the vmList is empty

		System.out.println("AFEDEF--All VMs in the vmlist have been scheduled at time: " + currentTime);

		//Check schedule results
		Check.checkUnfinishVMAndPM(vmList, activePMList, usedPMList);

		//Calculate the experimental results
		ExperimentResult.calculateExperimentResult("AFEDEF", vmList, usedPMList, migrateCount, totalScheduleTime);

		//Clear the lists
		vmList.clear();
		activePMList.clear();
		usedPMList.clear();
		lightlyLoadedPMList.clear();
		normallyLoadedPMList.clear();

		//Reset the id as 0 when finish the experiment of the algorithm
		PM.resetInnerId();
		VM.resetInnerId();
	}

	/**VM Allocation. */
	private void scheduleVMList(List<VM> vmList, List<PM> pmList) {
		while (vmList.size() != 0) {
			//1.Get the four thresholds by KMIR algorithm, and divide the active PMs
			KMIR(vmList,pmList);
			littleLoadedPMList.clear();
			lightlyLoadedPMList.clear();
			normallyLoadedPMList.clear();
			
			for(PM pm : activePMList) {
				//Little loaded PMs
				if(pm.getCpuUtilize() < tLight ) {
					littleLoadedPMList.add(pm);
				}
				//Lightly PMs
				if(pm.getCpuUtilize() < tNormal && pm.getCpuUtilize() >= tLight) {
					lightlyLoadedPMList.add(pm);
				}
				//Normal PMs
				if(pm.getCpuUtilize() < tMedium && pm.getCpuUtilize() >= tNormal) {
					normallyLoadedPMList.add(pm);
				}
			}
			
			//2.Sort VMs by CPU decreasing
			Collections.sort(vmList, new compareVMByCpuUtilize());

			//3.VMs placement
			VM vm = vmList.get(0);
			double minPower = Double.MIN_VALUE;
			PM assignedPM = null;
			for(PM pm : lightlyLoadedPMList) {
				if(pm.isSuitableForVM(vm,vm.getCpuUtilize(timeNo))) {
					double powerConsumBefore = pm.getIdlePower() + (pm.getMaxPower()-pm.getIdlePower())*pm.getCpuUtilize();
					double powerConsumAfter = pm.getIdlePower() + (pm.getMaxPower()-pm.getIdlePower())*(pm.getCpuUtilize()+vm.getCpuUtilize(timeNo));
					double powerConsumDiff = powerConsumAfter - powerConsumBefore;
					double slaBefore = calculateSLA(null,pm);//Get the SAL;
					List<VM> newVMList = new ArrayList<VM>();
					newVMList.add(vm);
					double slaAfter = calculateSLA(vm,pm);//Get the SAL
					double slaDiff = slaAfter - slaBefore;
					double ef = 1/(powerConsumDiff*slaDiff);
					if(ef>minPower) {
						minPower = ef;
						assignedPM = pm;
					}
				}
			} //End for lightlyLoaded PM list
			
			for(PM pm : normallyLoadedPMList) {
				if(pm.isSuitableForVM(vm,vm.getCpuUtilize(timeNo))) {
					double powerConsumBefore = pm.getIdlePower() + (pm.getMaxPower()-pm.getIdlePower())*pm.getCpuUtilize();
					double powerConsumAfter = pm.getIdlePower() + (pm.getMaxPower()-pm.getIdlePower())*(pm.getCpuUtilize()+vm.getCpuUtilize(timeNo));
					double powerConsumDiff = powerConsumAfter - powerConsumBefore;
					double slaBefore = pm.getDelta();
					double slaAfter = pm.getDelta();
					double slaDiff = slaAfter - slaBefore;
					double ef = 1/(powerConsumDiff*slaDiff);
					if(ef>minPower) {
						minPower = ef;
						assignedPM = pm;
					}
				}
			} //End for normallyLoaded PM list
		
			//When there has no suitable PMs in lightlyLoaded PMs and normallyLoaded, try to chose the littleLoaded PMs
			if(assignedPM == null) {
				for(PM pm : littleLoadedPMList) {
					if(pm.isSuitableForVM(vm,vm.getCpuUtilize(timeNo))) {
						double powerConsumBefore = pm.getIdlePower() + (pm.getMaxPower()-pm.getIdlePower())*pm.getCpuUtilize();
						double powerConsumAfter = pm.getIdlePower() + (pm.getMaxPower()-pm.getIdlePower())*(pm.getCpuUtilize()+vm.getCpuUtilize(timeNo));
						double powerConsumDiff = powerConsumAfter - powerConsumBefore;
						double slaBefore = pm.getDelta();
						double slaAfter = pm.getDelta();
						double slaDiff = slaAfter - slaBefore;
						double ef = 1/(powerConsumDiff*slaDiff);
						if(ef>minPower) {
							minPower = ef;
							assignedPM = pm;
						}
					}
				} //End for littleLoaded PM list
			}
			
			//Create new PM if there is no suitable PM
			if(assignedPM == null) {
				assignedPM = creatComponents.createRandomPM(vm,currentTime);
				activePMList.add(assignedPM);
			}
			
			// Assign VM on the best PM
			if (assignedPM.assignVM(vm, vm.getCpuUtilize(timeNo))) {
				vmList.remove(vm);
				if(vm.getLastPM() == assignedPM ) { //If the new assigned PM is the migrated PM then ignore the migrate count
					migrateCount--;
				}
			} 
			else {//Create new PM if cannot meet the requirement
				System.out.println(
						"There is no new PM can be creat for the requirement of VM #" + vm.getId());
				System.exit(0);
			}

		}
	}

	/**
	 * Update the status of VM and PM when executing task at current time,
	 * including: 1.Set the VM as finish when it's finish time is less or equal to
	 * current time; 2.Set the CPU utilization of PM by the current time.
	 */
	private void executeVMOnPM(List<PM> pmList) {
		List<PM> finishPM = new ArrayList<PM>();
		for (int i = 0; i < pmList.size(); i++) { // Get each PM, cannot remove the finish PM at here
			PM pm = pmList.get(i);
			List<VM> vmList = pm.getActiveVMList();
			List<VM> finishVM = new ArrayList<VM>();
			double currentCpuUtilize = 0;

			for (int j = 0; j < vmList.size(); j++) { // Get each VM on a PM
				VM vm = vmList.get(j);
				//Set the finish VM
				if (vm.getFinishTime() <= currentTime) {
					pm.finishedVM(vm, vm.getCpuUtilize(timeNo));
					finishVM.add(vm);
					finishedVMNum++;
					System.out.println("AFEDEF--VM #" + vm.getId() + " with finish time: " + vm.getFinishTime()
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
			System.out.println("AFEDEF--Idle PM #" + pm.getId() + " is moved from activePMList to userPMList");
		}
		finishPM.clear();
	}

	/**
	 * VMMigration. Traverse all active PMs and migrate the VMs on
	 * tLight/tHeavy PMs to the appropriate PM by Algorithm 1.
	 */
	private void migrateVM(List<PM> pmList) {
		migrateVMList.clear();
		
		//1.Get the four thresholds by KMIR algorithm, and divide the active PMs
		littleLoadedPMList.clear();
		heailyloadedPMList.clear();
		
		for(PM pm : pmList) {
			//Little loaded PMs
			if(pm.getCpuUtilize() < tLight ) {
				littleLoadedPMList.add(pm);
			}
			//Heavy PMs
			if(pm.getCpuUtilize() > tHeavy) {
				heailyloadedPMList.add(pm);
			}
		}
		
		//2.Migrate VMs of the littleLoaded PM list 
		while(littleLoadedPMList.size()>1) { //Migrate VM only there has more than one little loaded PM
			PM pm = littleLoadedPMList.get(littleLoadedPMList.size()-1); //Chose the last PM when assign object PM maybe the first one
			migrateVMList.addAll(pm.getActiveVMList());
			while(pm.getActiveVMList().size() != 0) {
				pm.migrateVM(pm.getActiveVMList().get(0), pm.getActiveVMList().get(0).getCpuUtilize(timeNo));
			}
			littleLoadedPMList.remove(pm);	
		}
		
		//3.Migrate VMs of the heailyloaded PM list
		for(PM pm : heailyloadedPMList) {
			double pmCPUUtilize = pm.getCpuUtilize();
				
			while(pmCPUUtilize > tHeavy) {
				double minMMT = Double.MAX_VALUE;
				VM selectedVM = null;
				for(VM vm : pm.getActiveVMList()) {
					double tempMMT = vm.getRequestedRam()/Parameters.bandwidth;
					if(tempMMT < minMMT) {
						minMMT = tempMMT;
						selectedVM = vm;
					}
				}
				if(selectedVM != null) {
					migrateVMList.add(selectedVM);
					pm.migrateVM(selectedVM, selectedVM.getCpuUtilize(timeNo));
					pmCPUUtilize = pm.getCpuUtilize();
				}
				else {
					System.out.println("The select VM is null on PM #" + pm.getId());
					System.exit(0);
				}
			}
		}
		
		if(migrateVMList.size() > 0) {
			for(VM vm : migrateVMList) {
				vm.setMigrateTimeNo(timeNo);
			}
			migrateCount += migrateVMList.size();
			migrateCountofCurrent += migrateVMList.size();	
			System.out.println("*************AFEDEF start migrate***********");
			scheduleVMList(migrateVMList, activePMList);
		}
	}

	/**Compare two VM by their CpuUtilize */
	private class compareVMByCpuUtilize implements Comparator<VM> {
		public int compare(VM v1, VM v2) {
			if (v1.getCpuUtilize(timeNo) > v2.getCpuUtilize(timeNo)) {
				return 1;
			} else if (v1.getCpuUtilize(timeNo) < v2.getCpuUtilize(timeNo)) {
				return -1;
			} else {
				return 0;
			}
		}
	}
	
	/**Get the four thresholds of PM, K-Means-Mad-IQR */
	private void KMIR(List<VM> vmList, List<PM> pmList) {
		List<Double> pastCPUUtilization = new ArrayList<Double>();
		for (PM pm : pmList) {
			for (double cpu : pm.getCpuUtilizeHistory()) {
				pastCPUUtilization.add(cpu);
			}
		}
		if(pastCPUUtilization.size() >= 12) {
			int k = 5; //The total cluster number
			double[][] g; //Store the k cluster
			g = cluster(pastCPUUtilization,k);
			double Midrange[] = new double [k];
			for (int i=0;i<g.length;i++) {
				double maxG = Double.MIN_VALUE;
				double minG = Double.MAX_VALUE;
				for(double d: g[i]) {
					if(d>maxG) {
						maxG = d;
					}
					if(d<minG) {
						minG = d;
					}
				}
				Midrange[i] = (maxG+minG)/2;
			}
			
			Arrays.sort(Midrange);
			int q1 = (int) Math.round(0.25 * (Midrange.length + 1)) - 1;
			int q3 = (int) Math.round(0.75 * (Midrange.length + 1)) - 1;
			double iqr = Midrange[q3] - Midrange[q1]; //Interquartile range
			//Eqs.3-6
			tLight = 0.5*(1-Parameters.c *iqr); //Set c as 1.0
			tNormal = 0.8*(1-Parameters.c *iqr); //Set c as 1.0
			tMedium = 0.9*(1-Parameters.c *iqr); //Set c as 1.0
			tHeavy = (1-Parameters.c *iqr); //Set c as 1.0
		}
		else {
			//Set the default value if there is not enough records
			tLight = 0.2;
			tNormal = 0.4;
			tMedium = 0.6;
			tHeavy = 0.8;
		}
	}
	
	/**Clustering one-dimensional double arrays p into k classes*/
	private double[][] cluster(List<Double> p, int k) {
		double[] c = new double[k]; //Store the old cluster center
		double[] nc = new double[k]; //Store the new cluster center
		double[][] g; //Store the cluster result		
		double[][] gg; //Store the cluster result from g without empty groups	
		
		//Initialize the cluster center with the first k value
		for(int i = 0;i<k;i++) {
			c[i] = p.get(i);
		}
		
		//Cyclic clustering with updated clustering centers
		while(true) {
			g = group(p,c); //Categorize elements according to the cluster centers
			
			//Remove the empty group from g into gg
			int emptyNum = 0;
			for(int i=0;i<c.length;i++) {
				if(g[i].length == 0) {
					emptyNum++;
				}
			}
			gg=new double[k-emptyNum][];
			
			int t3=0;
			for(int t1=0;t1<g.length;t1++) {
				if(g[t1].length == 0) {
					continue;
				}
				gg[t3] = new double [(g[t1]).length];
				for(int t2=0;t2<(g[t1]).length;t2++) {
					gg[t3][t2] = g[t1][t2];
				}
				t3++;
			}
			
			//Calculate the clustering center after classification
			for(int i=0;i<gg.length;i++) { 
				nc[i] = center(gg[i]);
			}
			
			//If nc is different with c
			if(!Arrays.equals(nc,c)) {
				c = nc;
				nc = new double[k];
			}
			else {
				break; //Finish clustering
			}
		} //endwhile		
		
		return gg;
	}

	/**Get the center of an array*/
	private double center(double[] p) {
		double sum = 0;
		for(int i=0;i<p.length;i++) {
			sum += p[i];
		}
		return sum/p.length;
	}

	/**Clustering the elements of array p according to the clustering center c,
	 * returning a two-dimensional array of the elements of each group
	 */
	private double[][] group(List<Double> p, double[] c) {
		int[] gi = new int[(p.size())]; //Group Marking
		
		//The smallest distance between pi and cj is classified as class j
		for(int i=0;i<p.size();i++) {
			double [] d = new double[c.length]; //Store the distance
			//Calculate the distance to each cluster center
			for(int j=0;j<c.length;j++) {
				d[j] = Math.abs(p.get(i)-c[j]);
			}
			 
			//Get the minimum distance
			int ci = Integer.MAX_VALUE;
			double tempMin = Integer.MAX_VALUE;
			for(int j=0;j<d.length;j++) {
				if(tempMin > d[j]) {
					tempMin = d[j];
					ci = j;
				}
			}
			 
			//Mark which group belongs to
			gi[i] = ci;
		}
		 
		//Store grouped results
		double[][] g = new double [c.length][];
		//Iterate through each clustering center, grouping
		for(int i=0;i<c.length;i++) {
			int s = 0; //Recording the size of each group after clustering		 
			//Calculate the length of each group
			for(int j=0;j<gi.length;j++) {
				if(gi[j] == i) {
					s++;
				}
			}
			 
			//Store the members of each group
			g[i] = new double[s];
			s=0;
			 
			//Place the elements according to the grouping markers
			for(int j=0;j<gi.length;j++) {
				if(gi[j] == i) {
					g[i][s] = p.get(j);
					s++;
				}
			}
		 }
		
		return g;
	}

	/**Calculate the SLA of a PM before/after assigned a VM*/
	private double calculateSLA(VM vm, PM pm) {
		int slaVT = 0; //The SLA violation times
		double cpuUtilize = 0;
		int pesUtilize = 0;
		double ramUtilize = 0;
		
		if(vm != null) { //Before/after assigned the VM on the PM
			cpuUtilize = pm.getCpuUtilize() + vm.getCpuUtilize(timeNo);
			pesUtilize = pm.getTotalPes() - pm.getIdlePes() + vm.getRequestedPes();
			ramUtilize = pm.getTotalRam() - pm.getIdleRam() + vm.getRequestedRam();
		}
		else {
			cpuUtilize = pm.getCpuUtilize();
			pesUtilize = pm.getTotalPes() - pm.getIdlePes();
			ramUtilize = pm.getTotalRam() - pm.getIdleRam();
		}
		
		if(cpuUtilize == 1 || pesUtilize == pm.getTotalPes() || ramUtilize == pm.getTotalRam()) {
			slaVT++;
		}
			
		if(pm.getCpuUtilizeHistory().size() == 0) {
			slaVT++;
		}
		if(pm.getCpuUtilizeHistory().size() == 0) {
			slatah= slaVT/1;
		}
		else {
			slatah= slaVT/pm.getCpuUtilizeHistory().size();
		}
		
		
		double totalPDM = 0;
		for(VM tempVM : pm.getActiveVMList()) {
			if(tempVM.getArriveTime() != currentTime) { //If is the migrate VM
				totalPDM += tempVM.getCpuUtilize(timeNo)*0.1/tempVM.getRequestedPes();
			}
		}
		
		if(vm != null) {
			if(vm.getArriveTime() != currentTime) { //If is the migrate VM
				totalPDM += vm.getCpuUtilize(timeNo)*0.1/vm.getRequestedPes();
			}
		}
		
		pdm = totalPDM / pm.getActiveVMList().size();
		
		if(pdm != 0) { //Consider assigned new VMs when there is no migrate VM
			slav = slatah * pdm;
		}
		else {
			slav = slatah;
		}
		
		return slav;
	}
	
	public String getName() {
		return "AFEDEF";
	}

}
