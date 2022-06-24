package cloud.configurations;

import java.util.ArrayList;
import java.util.List;

import cloud.algorithms.*;

public class Parameters {
	//-------------------------------------used in the schedules--------------------------------
	/** The workflow's type "CYBERSHAKE","LIGO","GENOME","SIPHT" "MONTAGE" */
	public static final String[] WORKFLOWTYPE = {"CYBERSHAKE","LIGO","GENOME","SIPHT","MONTAGE"}; //"CYBERSHAKE","LIGO","GENOME","SIPHT","MONTAGE"
	
	/** The workflow's number 50,200,1000 */
	public static final int[] WORKFLOWNUM = {50,200,1000}; //50,200,1000
	
	/**The workflow's DAX files location*/
	public static final String file_location = "..\\OSEC\\DataSet"; 
	
	/**The workflow template's txt file*/
	public static final String workflowTemplateFile = "..\\OSEC\\workflowTemplate.txt"; 

	/**The VM template's txt file*/
	public static final String vmTemplateFile = "..\\OSEC\\vmTemplate.txt"; 
	
	/**The number of the workflow template*/	
	public static int templateNum = 15; 
	
	/**The number of VM' type*/
	public static int vmTypeNumber = 7;

	/**The max average cpu utilization of VM with task */
	//Should divide 100 when used, and then to get the average CPU utilization by a NormalDistribution method
	public static double maxAgCpuUtilize = 12; 
	
	/**The deviance cpu utilization of VM with task */
	public static double dvCpuUtilize = 2; //Used in the NormalDistribution method to get actual CPU utilization of each execute time 12 2
	
	/**The price group of each VM type*/
	public static double[] vmPrice = {0.0058,0.0116,0.023,0.0464,0.0928,0.1856,0.3712}; 
	
	/**The CPU core of each VM type*/
	public static int[] vmPes = {1,1,1,2,2,4,8}; //The CPU core group of all VM's type
	
	/**The mem of each VM type*/
	public static double[] vmRam = {0.5,1,2,4,8,16,32}; //The memory group of all VM's type

	/**The Million Instructions per Seconds for of each PM type*/
	public static double[] pmMIPS = {2.0,2.6,3.8}; 
	
	/**The CPU core of each PM type*/
	public static int[] pmPes = {8,56,64}; //The CPU core group of all PM's type 8,56,64
	
	/**The mem of each PM type*/
	public static double[] pmRam = {16,192,256}; //The memory group of all PM's type 16,192,256
	
	/**The power consumption under the maximum utilization of each PM type*/
	public static double[] maxPower = {57.4,347,250}; //The maxPower group of all PM's type 57.4,347,250
	
	/**The power consumption under the maximum utilization of each PM type*/
	public static double[] idlePower = {16.8,48.3,60.8}; //The idlePower group of all PM's type 16.8,48.3,60.8
	
	/**Unused--The k value represent the ratio of the power consumption of the PM in idle time to the maximum utilization */
	public static double[] ratioPower = {16.8,48.3,60.8}; //The ratioPower group of all PM's type 16.8,48.3,60.8

	/**The upper limit CPU utilization of PM*/
	public static double overloadofCpu = 0.8;
	
	/**The lower limit CPU utilization of PM*/
	public static double lowloadofCpu = 0.2;

	/**The weights of  energy consume*/
	public static double wFactor = 0.5;
	
	//-----------used in SAVE and ecoCloud---------------
	/**The alpha parameter used in Eq.2 for migration definition*/
	public static double alpha = 0.25;
	
	/**The beta parameter used in Eq.2 for migration definition*/
	public static double beta = 0.25;
	 
	/**The p parameter used in Eq.2/3 for assignment procedure of ecoCloud*/
	public static double p = 3;
	 
	/**The p parameter used in Eqs.3-5 for assignment procedure of KMIMPCU*/
	public static double d = 1.0;
	 
	/**The p parameter used in Eqs.3-6 for assignment procedure of AFEDEF*/
	public static double c = 1.0;
	
	//-------------------------------------used in the experiments--------------------------------
	/**The group of algorithms*/	
	public static Scheduler[] SCHEDULES = {new EcoCloud(),new KMIMPCU(),new AFEDEF(),new OSEC()};//Algorithms: new EcoCloud(),new KMIMPCU(),new AFEDEF(),new OSEC()
	
	/**The repeat number of an algorithm for one condition*/
	public static int totalRepeatNum = 10; //Repeat times for each experiment

	/**The bandwidth between PMs*/	
	public static int bandwidth = 1000;
	
	/**The number group of the VMs*/	
	public static int[] vmNumGroup = {100,500,1000,2000,3000}; 

	/**The number of the VMs, equals to the number of tasks*/	
	public static int vmNum = 1000;
	
	/**The arrival rate list of VM*/
	public static double[] arrivalLamdaGroup = {0.4,0.8,1.2,1.6,2.0};
	
	/**The arrival rate of VM*/
	public static double arrivalLamda = 1.2;
	
	/**The time period that the CPU utilization changed on VM, used to get the actual CPU utilization 
	 * Multiple of schedule execution cycle, that means the CPU utilization of each VM will be changed after 5 current time*/
	public static int[] cpuChangePeriodGroup = {1,2,5,10,20}; 
	public static int cpuChangePeriod = 5; 
	
	/**The time period slot group*/
	public static int[] timeSlotGroup = {9,5,2,1,1};
	
	/**unused--The assumed total execute time used for create the actual CPU utilization of each VM.
	 * The CPU utilization of each VM at the current time is obtained by timeNo (currentTime/cpuChangePeriod) during the scheduling operation*/
	public static int assumedExTime = vmNum/timeSlotGroup[2]; 
	
	//-------------------------------------used in the results--------------------------------
	/**The result files location*/
	public static String Result_file_location = "..\\OSEC\\Result\\"; 
	
	/**The result file name*/
	public static String resultFile = "VM Number.txt";
	
	/**The each time result file name*/
	public static String eachTimeResult = "..\\OSEC\\Result\\eachTime\\";
	
	/**The current algorithm number*/
	public static int currentAlgorithm = 0; 
	
	/**The current repeat flag*/
	public static boolean isFirstRepeat = false; //Set as true when first run a group of parameters

	/**The current repeat number*/
	public static int currentRepeatNum = 0; //0-29
	
	/**The current test parameter*/
	public static String testParameter = "vmNum"; //vmNum(VM number), cpuChangePeriod, alpha-betaFactor
	
	/**The total energy consumption result array*/
	public static double energyConsumeResult[][]= new double [SCHEDULES.length][totalRepeatNum]; 
	
	/**The total execute cost result array*/
	public static double executeCostResult[][]= new double [SCHEDULES.length][totalRepeatNum]; 

	/**The number of used servers result array*/
	public static double usedPMNumResult[][]= new double [SCHEDULES.length][totalRepeatNum]; 

	/**The SLA result array*/
	public static double slaResult[][]= new double [SCHEDULES.length][totalRepeatNum]; 
	
	/**The number of migrate VMs result array*/
	public static double migrateVMCountResult[][]= new double [SCHEDULES.length][totalRepeatNum]; 
	
	/**The schedule time result array*/
	public static double scheduleTimeResult[][]= new double [SCHEDULES.length][totalRepeatNum]; 

	/**The each currentTime used PM result array*/
	public static List<Integer> activePMPerTimeList= new ArrayList<Integer>(); 
	
	/**The each currentTime migrate VM result array*/
	public static List<Integer> migrateVMPerTimeList= new ArrayList<Integer>(); 
	
	/**The each currentTime shutdown PM result array*/
	public static List<Integer> shutdownPMPerTimeList= new ArrayList<Integer>();

	
}
