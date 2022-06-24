package cloud.components;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cloud.configurations.Parameters;
import cloud.methods.Methods;

@SuppressWarnings("serial")
public class VM implements Serializable {
	/** The internal number of the VM，auto increased by 1 when generate a new VM. */
	private static int innerId = 0;
	
	/** The VM unique id. */
	private int vmID;

	/** The number of CPU cores required by the VM. */
	private int pes;

	/** The required ram. */
	private double ram;

	/** The price of the VM. */
	private double price;

	/** The arrive time. */
	private double arriveTime;
	
	/** The  finish time. */
	private double finishTime; 
	
	/** The cpu Utilization list used in executing tasks, <timeNo, cpuUtilize>, timeNo = currentTime/Parameters.cpuChangePeriod. */
	private HashMap<Integer,Double> cpuUtilizeList;
	
	/** The task that assigned on the VM. */
	private Task task;
	
	/** The PM that hosts the VM. */
	private PM pm;

	/** Indicates if the VM is in migration process. */
	private PM lastPM;

	/** Indicates which timeNO that the VM is in migration process. */
	private List<Integer> migrateTimeNo;
	
	/** Indicates the status of the VM, false means the VM is finished. */
	private boolean status;

	/** Creates a new VM. */
	public VM(int pes, double ram, double price) {
		this.vmID = innerId++;
		this.pes = pes;
		this.ram = ram;
		this.price = price;
		
		this.arriveTime = -1;
		this.finishTime = -1;
		this.cpuUtilizeList = new HashMap<Integer,Double>();
		this.migrateTimeNo = new ArrayList<Integer>();
		this.status = true;
		
	}

	//-------------------------------------getters&setters--------------------------------
	/** Reset the VM id */
	public static void resetInnerId() {
		innerId = 0;
	}
	
    /** Gets the VM id. */
	public int getId() {
		return vmID;
	}

	/** Gets the number of pes. */
	public int getRequestedPes() {
		return pes;
	}

	/** Gets VM's price. */
	public double getPrice() {
		return price;
	}
	
	/** Gets the amount of ram. */
	public double getRequestedRam() {
		return ram;
	}

	/** Sets the arrive time. */
	public void setArriveTime(double arriveTime) {
		this.arriveTime = arriveTime;
	}
	
	/** Gets the arrive time. */
	public double getArriveTime() {
		return arriveTime;
	}

	/** Sets the finish time. */
	public void setFinishTime(double finishTime) {
		 this.finishTime = finishTime;
	}
	
	/** Gets the finish time. */
	public double getFinishTime() {
		return finishTime;
	}
	
	/** Sets the cpu Utilization with the task's execute time. */
	public void  setCpuUtilize(Integer timeNo,double cpuUtilize) {
		this.cpuUtilizeList.put(timeNo, cpuUtilize);
	}
	
	/** Gets the cpu Utilization by the the task's execute time, if the status is false then return 0. */
	public double getCpuUtilize(Integer timeNo) {
		
		if(status) {
			if(cpuUtilizeList.containsKey(timeNo)) {
				return cpuUtilizeList.get(timeNo);
			}
			else {
				//return cpuUtilizeList.get(cpuUtilizeList.size()-1);
				double gtCpuUtilize = Methods.NormalDistribution(Parameters.maxAgCpuUtilize, Parameters.dvCpuUtilize); 
				if(gtCpuUtilize<0) {
					gtCpuUtilize = Parameters.maxAgCpuUtilize;
				}
				cpuUtilizeList.put(timeNo, gtCpuUtilize/100);
				return gtCpuUtilize/100;
			}
			
		}
		else {
			return 0;
		}
	}
	
	/** Sets the task that assigned on this VM. */
	public void setTask(Task task) {
		this.task = task;
		this.task.setAssignedVM(this);
	}

	/** Gets the task that assigned on this VM. */
	public Task getTask() {
		return task;
	}
	
	/** Sets the host that assign this VM. */
	public void setPM(PM pm) {
		this.pm = pm;
		this.task.setAssignedPM(pm);
	}

	/** Gets the assigned host. */
	public PM getPM() {
		return pm;
	}

	/** Sets the last PM before migration. */
	public void setLastPM(PM lastPM) {
		this.lastPM = lastPM;
	}

	/** Gets the last PM before migration. */
	public PM getLastPM() {
		return this.lastPM;
	}
	
	/** Sets the timeNo when migration. */
	public void setMigrateTimeNo(int timeNo) {
		this.migrateTimeNo.add(timeNo);
	}
	
	/** Get  the timeNo when migration. */
	public  List<Integer> getMigrateTimeNo() {
		return this.migrateTimeNo;
	}

	
	/** Sets the status. */
	public void setStatus(boolean status) {
		this.status = status;
	}

	/** Gets the status. */
	public boolean getStatus() {
		return status;
	}
	
}
