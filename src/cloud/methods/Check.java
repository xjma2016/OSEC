package cloud.methods;

import java.text.SimpleDateFormat;
import java.util.List;

import cloud.components.*;

public class Check {
	public static SimpleDateFormat cTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //Current Time
	
	/**Check the task and edge*/
	public static void checkTask(Task task) {
		List<Edge> inEdgeList = task.getInEdges();
		for(int i=0;i<inEdgeList.size();i++) {
			Edge edge = inEdgeList.get(i);
			if(edge.getParentTask() == null || edge.getChildTask() == null || 
					edge.getTransDataSize() <= 0 ) {
				throw new IllegalArgumentException("There are some mistake at edge:" + edge.getParentTask().getTaskID() + " to " + edge.getChildTask().getTaskID());
			}
		}
		
		List<Edge> outEdgeList = task.getOutEdges();
		for(int i=0;i<outEdgeList.size();i++) {
			Edge edge = outEdgeList.get(i);
			if(edge.getParentTask() == null || edge.getChildTask() == null || 
					edge.getTransDataSize() <= 0 ) {
				throw new IllegalArgumentException("There are some mistake at edge:" + edge.getParentTask().getTaskID() + " to " + edge.getChildTask().getTaskID());
			}
		}
	}
	
	/**Check if there has a unfinish VM\PM*/
	public static void checkUnfinishVMAndPM(List<VM> vmList,List<PM> activePMList, List<PM> usedPMList)  {
		for(VM vm : vmList) {
			if(vm.getStatus()) {
				throw new IllegalArgumentException("There exist a unFinished VM!");
			}
			if(vm.getPM() == null) {
				throw new IllegalArgumentException("There exist a unAssigned VM!");
			}
			if(vm.getTask().getAssignedVM() == null || vm.getTask().getAssignedPM() == null) {
				throw new IllegalArgumentException("There exist a task has no assigned VM and PM!");
			}
		}
		
		for(PM pm : usedPMList) {
			if(pm.getActiveVMList().size()>0) {
				throw new IllegalArgumentException("There exist a PM has active VMs!");
			}
			if(pm.getStatus() || pm.getFinishTime() == -1) {
				throw new IllegalArgumentException("There exist a unFinished PM!");
			}
			if(pm.getStartTime() == -1) {
				throw new IllegalArgumentException("There exist a PM has no startTime!");
			}
			if(pm.getCpuUtilize() <0 || pm.getCpuUtilize()>100 ) {
				throw new IllegalArgumentException("There exist a PM has cpuUtilization not between 0 and 100!");
			}
			if(pm.getIdlePes() != pm.getTotalPes() || pm.getIdleRam() != pm.getTotalRam()) {
				throw new IllegalArgumentException("There exist a PM has wrong pes or ram!");
			}
			for(double cpuUtilize : pm.getCpuUtilizeHistory()) {
				if(cpuUtilize <0 || cpuUtilize>100 ) {
					throw new IllegalArgumentException("There exist a PM has cpuUtilization history not between 0 and 100!");
				}
			}
			for(int pesUsage : pm.getPesUsageHistory()) {
				if(pesUsage <0 || pesUsage>pm.getTotalPes() ) {
					throw new IllegalArgumentException("There exist a PM has the wrong used pes history!");
				}
			}
			for(double ramUsage : pm.getRamUsageHistory()) {
				if(ramUsage <0 || ramUsage>pm.getTotalRam() ) {
					throw new IllegalArgumentException("There exist a PM has the wrong used ram history!");
				}
			}
		}
		
		if(activePMList.size()>0) {
			throw new IllegalArgumentException("There exist a PM in activePMList!");
		}
	}
	
}
