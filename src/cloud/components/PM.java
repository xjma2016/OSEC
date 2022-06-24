package cloud.components;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cloud.configurations.Parameters;

@SuppressWarnings("serial")
public class PM implements Serializable{

	/** The internal number of the host，auto increased by 1 when generate a new PM. */
	private static int innerId = 0;
	
	/** The id of the host. */
	private int pmID;

	/** The total CPU cores of the host */
    private int totalPes;

	/** The CPU MIPS of the host */
    private double mips;

    /** The idle CPU cores of the host */
    private int idlePes;
    
    /** The total ram of the host. */
	private double totalRam;
	
    /** The idle Ram of the host */
    private double idleRam;
    
	/** The power consumption under the maximum utilization. */
	private double maxPower;
	  
	/** The power consumption under the idles. */
	private double idlePower;
		
	/** The fitness value calculated by FLS with EC and RU as input.*/
	private double fitness;
	
	/** The resource utilization calculated by FLS with CPU and RAM as input.*/
	private double ru;
	
	/** The attract value for a VM, it calculated by the tranfer data  between the task assigned on the VM and it's parent. */
	private double delta;

	/** The start time means the PM is create and the activeVMList is not empty. */
	private double startTime;

	/** The finish time means the PM is set as false status and activeVMList is empty. */
	private double finishTime;

	/** The real time CPU Utilization. */
	private double cpuUtilize;
	
	/** The history CPU Utilization in each run time. */
	private List<Double> cpuUtilizeHistory;
	
	/** The history pes usage in each run time. */
	private List<Integer> pesUsageHistory;
	
	/** The history ram usage in each run time. */
	private List<Double> ramUsageHistory;
	
	/** The list of active VMs executing on the host. */
	private List<VM> activeVMList;
	
	
	/** The list of total VMs finished on the host. */
	private List<VM> finishedVMList;

	/** Indicates the status of the PM, false means the PM is shutdown. */
	private boolean status;

	/** Create a new host. */
	public PM(int pes, double ram,double mips, double maxPower,double idlePower, double ratioPower,double startTime){
		this.pmID = innerId++;
		this.totalPes = pes;
		this.totalRam = ram;
		this.mips = mips;
		this.maxPower = maxPower;
		this.idlePower = idlePower;
		this.startTime = startTime;
				
		this.idlePes = pes;
		this.idleRam = ram;
		this.delta = -1;
		this.fitness = -1;
		this.ru = -1;
		this.finishTime = -1;
		this.cpuUtilize = 0;
		this.cpuUtilizeHistory = new ArrayList<Double>();
		this.pesUsageHistory = new ArrayList<Integer>();
		this.ramUsageHistory = new ArrayList<Double>();
		this.activeVMList = new ArrayList<VM>();
		this.finishedVMList = new ArrayList<VM>();
		this.status = true;
	}

	//-------------------------------------getters&setters--------------------------------
	/** Reset the host id */
	public static void resetInnerId() {
		innerId = 0;
	}

	/** Gets the host id.*/
	public int getId() {
		return pmID;
	}

	/** Gets the idle CPU cores. */
	public int getIdlePes() {
		return idlePes;
	}
	
	/** Gets the total CPU cores. */
	public int getTotalPes() {
		return totalPes;
	}
	
	/** Gets the idle ram. */
	public double getIdleRam() {
		return idleRam;
	}

	/** Gets the MIPS of CPU. */
	public double getMIPS() {
		return mips;
	}
	
	/** Gets the total ram. */
	public double getTotalRam() {
		return totalRam;
	}
	
	/** Gets the host maxPower. */
	public double getMaxPower() {
		return maxPower;
	}

	/** Gets the host idlePower. */
	public double getIdlePower() {
		return idlePower;
	}
	
	/** Sets the host fitness. */
	public void setfitness(double fitness) {
		this.fitness = fitness;
	}
	
	/** Gets the host fitness. */
	public double getfitness() {
		return fitness;
	}
	
	/** Sets the host ru. */
	public void setRU(double ru) {
		this.ru = ru;
	}
	
	/** Gets the host ru. */
	public double getRU() {
		return ru;
	}
	
	/** Sets the delta. */
	public void  setDelta(double delta) {
		this.delta = delta;
	}
	
	/** Gets the delta. */
	public double getDelta() {
		return delta;
	}
	
	/** gets the startTime. */
	public double getStartTime() {
		return startTime;
	}
	
	/** Sets the finishTime. */
	public void setFinishTime(double finishTime) {
		this.finishTime = finishTime;
	}
	
	/** gets the finishTime. */
	public double getFinishTime() {
		return finishTime;
	}
	
	/** Sets the real time CPU Utilization. */
	public void setCpuUtilize(double cpuUtilize) {
		this.cpuUtilize = cpuUtilize;
	}
	
	/** Gets the real time CPU Utilization. */
	public double getCpuUtilize() {
		return cpuUtilize;
	}
	
	/** Sets the total CPU Utilization at each run time. */
	public void setCpuUtilizeHistory(double cpuUtilizeHistory) {
		this.cpuUtilizeHistory.add(cpuUtilizeHistory);
	}
	
	/** Gets the total CPU Utilization list at each run time. */
	public List<Double> getCpuUtilizeHistory() {
		return cpuUtilizeHistory;
	}
	
	/** Sets the total pes usage at each run time. */
	public void setPesUsageHistory(int pesUsageHistory) {
		this.pesUsageHistory.add(pesUsageHistory);
	}
	
	/** Gets the total pes usage at each run time. */
	public List<Integer> getPesUsageHistory() {
		return pesUsageHistory;
	}
	
	/** Sets the total ram usage at each run time. */
	public void setRamUsageHistory(double ramUsageHistory) {
		this.ramUsageHistory.add(ramUsageHistory);
	}
	
	/** Gets the total ram usage at each run time. */
	public List<Double> getRamUsageHistory() {
		return ramUsageHistory;
	}
	
	/** Sets the host's status, false means shutdown. */
	public void setStatus(boolean status) {
		this.status = status;
	}

	/** Gets the host's status. */
	public boolean getStatus() {
		return status;
	}

	/** Gets the active VMs list. */
	public List<VM> getActiveVMList() {
		return activeVMList;
	}

	/** Gets the total finished VMs list. */
	public List<VM> getFinishedVMList() {
		return finishedVMList;
	}

	//-------------------------------------methods--------------------------------

	/** Checks if the host has enough resources for a vm.  */
	public boolean isSuitableForVM(VM vm,double vmCpuUtilize) {
		return (idlePes >= vm.getRequestedPes()
				&& idleRam >= vm.getRequestedRam()
				&& cpuUtilize + vmCpuUtilize <=  Parameters.overloadofCpu);
	}

	/** Try to allocate resources to a new VM in the Host. */
	public boolean assignVM(VM vm, double vmCpuUtilize) {
		if (idlePes < vm.getRequestedPes()) {
			//System.out.println("Assign VM #"+ vm.getId() + " to PM #"+ getId()+" failed by CPU core!");
			return false;
		}

		if (idleRam < vm.getRequestedRam()) {
			//System.out.println("Assign VM #"+ vm.getId() + " to PM #"+ getId()+" failed by RAM!");
			return false;
		}

		if (cpuUtilize + vmCpuUtilize >  Parameters.overloadofCpu) {
			//System.out.println("Assign VM #"+ vm.getId() + " to PM #"+ getId()+" failed by RAM!");
			return false;
		}
		
		idlePes = idlePes - vm.getRequestedPes(); 
		idleRam = idleRam - vm.getRequestedRam(); 
		cpuUtilize = cpuUtilize + vmCpuUtilize;
		activeVMList.add(vm);
		vm.setFinishTime(vm.getArriveTime()+vm.getTask().getBaseExecuteTime()/this.mips);
		vm.setPM(this);
		return true;
	}
	
	/** Migrate a VM when the host is overload or lowload. */
	public void migrateVM(VM vm, double vmCpuUtilize) {
		if (vm != null) {
			idlePes = idlePes + vm.getRequestedPes(); 
			idleRam = idleRam + vm.getRequestedRam(); 
			
			cpuUtilize = 0>(cpuUtilize-vmCpuUtilize) ? 0:(cpuUtilize-vmCpuUtilize); //Set the cpuUtilize as 0 If it's less than vmCpuUtilize
			activeVMList.remove(vm);
			
			vm.setPM(null);
			vm.setLastPM(this);
		}
	}

	/** Finished a VM when it's finish time is less or equal to the current time. */
	public void finishedVM(VM vm, double vmCpuUtilize) {
		if (activeVMList.contains(vm)) {
			idlePes = idlePes + vm.getRequestedPes(); 
			idleRam = idleRam + vm.getRequestedRam();
			cpuUtilize = 0>(cpuUtilize-vmCpuUtilize) ? 0:(cpuUtilize-vmCpuUtilize); //Set the cpuUtilize as 0 If it's less than vmCpuUtilize
			finishedVMList.add(vm);
			vm.setStatus(false);
		}
		else {
			System.out.println("VM #"+ vm.getId() + " is not exeute on this PM #"+ getId());
			System.exit(0);
		}
	}

}
