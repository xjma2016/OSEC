package cloud.components;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
@SuppressWarnings("serial")
public class Task implements Serializable {
	private final String taskID; //The task's ID
	private int taskWorkflowID; //The workflow's ID, where task belongs to
	
	private double baseExecuteTime; //The task's base execution time, get from the DAX file
		
	private boolean isAssigned; //Whether this task has been assigned
	private VM assignedVM; //The assigned VM 
	private PM assignedPM; //The assigned PM 
	private boolean isFinished; //whether this task has been finished
	
	private List<Edge> inEdges; //The in edges set, includes all the immediate predecessors
	private List<Edge> outEdges; //The out edges set, includes all the immediate successors
	
	public Task(String taskID, int taskWorkflowID,  double baseExecuteTime) {
		this.taskID  = taskID;
		this.taskWorkflowID = taskWorkflowID;
		
		this.baseExecuteTime = baseExecuteTime;
		
		this.isAssigned = false;
		this.assignedVM = null;
		this.assignedPM = null;
		this.isFinished = false;
		
		inEdges = new ArrayList<Edge>();
		outEdges = new ArrayList<Edge>();
	}
	
	//-------------------------------------getters&setters--------------------------------
	public String getTaskID() {
		return taskID;
	}

	public int getTaskWorkflowID() {
		return taskWorkflowID;
	}
	
	public double getBaseExecuteTime() {
		return baseExecuteTime;
	}
		
	public boolean getIsAssigned() {
		return isAssigned;
	}
	public void setIsAssigned(boolean isAssigned) {
		this.isAssigned = isAssigned;
	}
	
	public VM getAssignedVM() {
		return assignedVM;
	}
	public void setAssignedVM(VM assignedVM) {
		this.assignedVM = assignedVM;
	}

	public PM getAssignedPM() {
		return assignedPM;
	}
	public void setAssignedPM(PM assignedPM) {
		 this.assignedPM = assignedPM;
	}
	
	public boolean getIsFinished() {
		return isFinished;
	}
	public void setIsFinished(boolean isFinished) {
		this.isFinished = isFinished;
	}
	
	public List<Edge> getInEdges() {
		return inEdges;
	}	
	public void addInEdges(Edge inEdge) {
		this.inEdges.add(inEdge);
	}
	
	public List<Edge> getOutEdges() {
		return outEdges;
	}	
	public void addOutEdges(Edge outEdge) {
		this.outEdges.add(outEdge);
	}
	
}
