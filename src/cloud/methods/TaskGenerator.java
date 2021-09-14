package cloud.methods;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import cloud.components.*;
import cloud.configurations.Parameters;

public class TaskGenerator {
	/**On the basis of the real-world workflow templates, the set of tasks from different workflows are generated
	 * @return */
	public  List<Task> generateTask(int taskNum) throws IOException, ClassNotFoundException {
		
		//Generate workflow template
		WorkflowTemplate wfTemplate = new WorkflowTemplate();
		wfTemplate.generateTemplate();
		
		//Generate the task set and write it into Parameters.ByteArrayOutputStream //"generatedTask.txt"
		List<Task> taskList = new ArrayList<Task>(); //The set of generated tasks
		List<Workflow> tempWKList = new ArrayList<Workflow>(); //Store the temp workflows to get tasks
		while(taskList.size()<taskNum) { 
			//Read the workflow template from "workflowTemplate.txt"
			if(tempWKList.size() == 0) {
				FileInputStream fi = new FileInputStream(Parameters.workflowTemplateFile);
				ObjectInputStream si = new ObjectInputStream(fi);
				try {
					for(int i=0;i<Parameters.templateNum;i++) {
						Workflow readWorkflow = (Workflow)si.readObject(); 
						tempWKList.add(readWorkflow);
					}
					si.close(); 
				}
				catch(IOException e) {
					System.out.println(e.getMessage());
				}
			}
			//Randomly choose a task into taskList from a workflow in tempWKList
			else {
				int selectedWKNum = (int)(Math.random()*tempWKList.size()); //Choose a workflow from tempWKList
				Workflow selectedWorkflow = tempWKList.get(selectedWKNum);
				if(selectedWorkflow.getTaskList().size()>0) {
					Task selectedTask = selectedWorkflow.getTaskList().get(0);
					taskList.add(selectedTask);
					selectedWorkflow.getTaskList().remove(0);
					if(selectedWorkflow.getTaskList().size() == 0) {
						tempWKList.remove(selectedWKNum);
					}
				}
			}
		} //End while, generate Parameters.workflowNum workflows
		
		return taskList;
	}
	
}
