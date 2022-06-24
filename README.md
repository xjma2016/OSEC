# README.md
-------------

#### Overview:
OSEC is used for the online VM scheduling in Cloud, and this code is simulated based on the actual application scenarios.
In the simulator,  VMs that arrive randomly on behalf of different user requests can be assigned to different PMs by the scheduler in real time. 
The simulator compares the performance of OSEC and the other three scheduling algorithms in terms of energy consumption, execution costs, SLA violation and the number of VM migrations under different conditions.

#### Directory structure:  
 1.cloud.algorithm: Different scheduling algorithms and the Scheduler interface;<br> 
 2.cloud.components:The components used in the VM scheduling;<br> 
 3.cloud.configurations: Parameter configuration used to set the experiment parameters such as the VM number, DAX files location, VM and PM type, and so on;<br> 
 4.cloud.mainProgram: Start the simulator. <br> 
 5.DataSet: the input dax files which can also be download from "https://confluence.pegasus.isi.edu/display/pegasus/Deprecated+Workflow+Generator";<br> 
 6.Results: the ouput results txt files.<br> 

### Operation steps of the simulator:
 1.Set the java compiler to 1.7 after importing the project into eclipse;<br> 
 2.Set the related parameters in cloud.configurations.Parameters.java, mainly including: file_location, workflowTemplateFile, Result_file_location;<br> 
 3.Run the simulator in cloud.mainProgram.TestProgram.java, and the experimental results can be found in the result file after finishing the program.<br> 

### Others:
More details can be found in the corresponding code comments.
