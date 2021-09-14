package cloud.algorithms;

import java.util.List;

import cloud.components.*;

public interface Scheduler {
	public void schedule(List<VM> vmList); //Run the schedule for each VM
	public String getName(); //Get the algorithm's name
}
