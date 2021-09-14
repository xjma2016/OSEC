package cloud.mainProgram;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cloud.algorithms.*;
import cloud.components.*;
import cloud.configurations.Parameters;
import cloud.methods.*;

public class TestProgram {

	public static void main(String[] args)throws Exception {
		SimpleDateFormat cTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //Current Time
		System.out.println("The experiment start at: " + cTime.format(new Date()));
		
		//Test the impact of arrivalLamdaGroup, when other parameters is default value
		for(int i=0;i<Parameters.arrivalLamdaGroup.length;i++){//Test the impact of variance of arrivalLamdaGroup
			Parameters.arrivalLamda = Parameters.arrivalLamdaGroup[i];
			Parameters.testParameter = "arrivalLamdaGroup";
							
			//Set the first run flag
			if(Parameters.arrivalLamda==Parameters.arrivalLamdaGroup[0]) {
				Parameters.isFirstRepeat = true;
			}
			else {
				Parameters.isFirstRepeat = false;
			}
							
			for(int r=0;r<Parameters.totalRepeatNum;r++) { //Repeat 30 times for each group parameters
				Parameters.resultFile = "arrivalLamda factor.txt";
				Parameters.currentRepeatNum = r;
						
				/** Create Parameters.taskNum VMs into a list. */
				CreatComponents creatComponents = new CreatComponents();
				creatComponents.createVMList();
						
				System.out.println("VM Count: "+Parameters.vmNum  + " arrive rate: "+Parameters.arrivalLamda + 
						" cpuChangePeriod: " + Parameters.cpuChangePeriod + " repeat "+(r+1)+" time start at: " + cTime.format(new Date()));
								
				for(int j=0;j<Parameters.SCHEDULES.length;j++) { //Run each algorithm in SCHEDULES
					Parameters.currentAlgorithm = j;
					//Get the VM list
					List<VM> vmList = new ArrayList<VM>(); //Store the workflows used in experiments
					/** Read the test VM list from txt.*/
					FileInputStream fi = new FileInputStream(Parameters.vmTemplateFile);
					ObjectInputStream si = new ObjectInputStream(fi);
					int k =0;
					try {
						while(k<Parameters.vmNum) {
							VM readVM = (VM)si.readObject(); 
							vmList.add(readVM);
							Check.checkTask(readVM.getTask());
							k++;
						}
						si.close(); 
						fi.close();
					}
					catch(IOException e) {
						System.out.println(e.getMessage());
					}
						
					//Run the schedule function in the selected algorithm
					Scheduler s = Parameters.SCHEDULES[j];
					s.schedule(vmList);
									
				} //End run each algorithm in SCHEDULES
								
				System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------------");
			} //End repeat
		} //End test the impact of variance of VM count
		Parameters.arrivalLamda = 0.8; //Set the arrivalLamda as default value	
		
		//Test the impact of cpuChangePeriod, when other parameters is default value
		for(int i=0;i<Parameters.cpuChangePeriodGroup.length;i++){//Test the impact of variance of arrivalLamdaGroup
			Parameters.cpuChangePeriod = Parameters.cpuChangePeriodGroup[i];
			Parameters.testParameter = "cpuChangePeriod";
											
			//Set the first run flag
			if(Parameters.cpuChangePeriod==Parameters.cpuChangePeriodGroup[0]) {
				Parameters.isFirstRepeat = true;
			}
			else {
				Parameters.isFirstRepeat = false;
			}
											
			for(int r=0;r<Parameters.totalRepeatNum;r++) { //Repeat 30 times for each group parameters
				Parameters.resultFile = "cpuChangePeriod factor.txt";
				Parameters.currentRepeatNum = r;
										
				/** Create Parameters.taskNum VMs into a list. */
				CreatComponents creatComponents = new CreatComponents();
				creatComponents.createVMList();
										
				System.out.println("VM Count: "+Parameters.vmNum  + " arrive rate: "+Parameters.arrivalLamda + 
						" cpuChangePeriod: " + Parameters.cpuChangePeriod + " repeat "+(r+1)+" time start at: " + cTime.format(new Date()));
												
				for(int j=0;j<Parameters.SCHEDULES.length;j++) { //Run each algorithm in SCHEDULES
					Parameters.currentAlgorithm = j;
					//Get the VM list
					List<VM> vmList = new ArrayList<VM>(); //Store the workflows used in experiments
					/** Read the test VM list from txt.*/
					FileInputStream fi = new FileInputStream(Parameters.vmTemplateFile);
					ObjectInputStream si = new ObjectInputStream(fi);
					int k =0;
					try {
						while(k<Parameters.vmNum) {
							VM readVM = (VM)si.readObject(); 
							vmList.add(readVM);
							Check.checkTask(readVM.getTask());
							k++;
						}
						si.close(); 
						fi.close();
					}
					catch(IOException e) {
						System.out.println(e.getMessage());
					}
								
					//Run the schedule function in the selected algorithm
					Scheduler s = Parameters.SCHEDULES[j];
					s.schedule(vmList);
													
				} //End run each algorithm in SCHEDULES
												
				System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------------");
			} //End repeat
		} //End test the impact of variance of VM count
		Parameters.cpuChangePeriod = 5; //Set the cpuChangePeriod as default value	
		
		
		//Test the impact of variance of VM count, when other parameters is default value
		for(int i=0;i<Parameters.vmNumGroup.length;i++){//Test the impact of variance of VM count
			Parameters.vmNum = Parameters.vmNumGroup[i];
			Parameters.testParameter = "vmNum";
					
			//Set the first run flag
			if(Parameters.vmNum==Parameters.vmNumGroup[0]) {
				Parameters.isFirstRepeat = true;
			}
			else {
				Parameters.isFirstRepeat = false;
			}
					
			for(int r=0;r<Parameters.totalRepeatNum;r++) { //Repeat 30 times for each group parameters
				Parameters.resultFile = "VM number factor.txt";
				Parameters.currentRepeatNum = r;
				
				/** Create Parameters.taskNum VMs into a list. */
				CreatComponents creatComponents = new CreatComponents();
				creatComponents.createVMList();
				
				System.out.println("VM Count: "+Parameters.vmNum +  " arrive rate: "+Parameters.arrivalLamda + 
						" cpuChangePeriod: " + Parameters.cpuChangePeriod + " repeat "+(r+1)+" time start at: " + cTime.format(new Date()));
						
				for(int j=0;j<Parameters.SCHEDULES.length;j++) { //Run each algorithm in SCHEDULES
					Parameters.currentAlgorithm = j;
					//Get the VM list
					List<VM> vmList = new ArrayList<VM>(); //Store the workflows used in experiments
					/** Read the test VM list from txt.*/
					FileInputStream fi = new FileInputStream(Parameters.vmTemplateFile);
					ObjectInputStream si = new ObjectInputStream(fi);
					int k =0;
					try {
						while(k<Parameters.vmNum) {
							VM readVM = (VM)si.readObject(); 
							vmList.add(readVM);
							Check.checkTask(readVM.getTask());
							k++;
						}
						si.close(); 
						fi.close();
					}
					catch(IOException e) {
						System.out.println(e.getMessage());
					}
					
					//Run the schedule function in the selected algorithm
					Scheduler s = Parameters.SCHEDULES[j];
					getArriveVMNumofEachtime(vmList,s.getName());//Get the arrive VM number at each execute time
					s.schedule(vmList);
						
				} //End run each algorithm in SCHEDULES
						
				System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------------");
			} //End repeat
		} //End test the impact of variance of VM count

		Parameters.vmNum = 1000; //Set the VM number as default value	
		
		//Calculate the result in each current time.
		//Should remove the title at each repeat 0 text before run this, like "activePM-ecoCloud-0.txt" etc
		String[] parameters = {"activePM-","migrateVM-","shutdownPM-","arriveVM-"};
		String[] schedules = {"ecoCloud-","SAVE-","OSER-"};
		
		for(int i=0;i<parameters.length;i++) {
			Map<Integer,Map<Integer,List<String>>>  totalResultMap = new HashMap<Integer, Map<Integer,List<String>>>();//Store total list
			int maxLine = 0; //Recored the max line in the new result text
			for(int k=0;k<schedules.length;k++) {
				Map<Integer,List<String>>  eachResultMap = new HashMap<Integer, List<String>>();//Store each list of algorithms
				for(int r=0;r<Parameters.totalRepeatNum;r++) {
					String txtName = Parameters.eachTimeResult+parameters[i]+schedules[k]+r+".txt";
					FileInputStream fi = new FileInputStream(txtName);
					BufferedReader si = new BufferedReader(new InputStreamReader(fi));
					String lineTxt = null;
					List<String> list = new ArrayList<String>();//Store each result into list from the text 
					try {
						while((lineTxt = si.readLine()) != null) {
							String[] names = lineTxt.split("   ");
							list.add(names[1]);
						}
						si.close(); 
						fi.close();
						eachResultMap.put(r, list);//Get the totalrepeatNumber of a schedule into a <list> map
						if(list.size()>maxLine) {
							maxLine = list.size();
						}
					}
					catch(IOException e) {
						System.out.println(e.getMessage());
					}
				}
				totalResultMap.put(k, eachResultMap); //Get total result of a schedule into a <map> map
			}
			
			//Store the total and average results into text
			String eachTimeResultTxt = Parameters.eachTimeResult + parameters[i]+".txt";
			try	{
				FileOutputStream fos = new FileOutputStream(eachTimeResultTxt,false);  //Write the result to txt file
				OutputStreamWriter os = new OutputStreamWriter(fos,"utf-8");
				
				for(int k=0;k<schedules.length;k++) {
					String parameter ="current time  " + schedules[k]+ "--repeat 1-30 value";
					os.append(parameter+"\r\n");
					Map<Integer,List<String>>  resultMap =  totalResultMap.get(k);
					for(int j=0;j<maxLine;j++) {
						os.append(j+":");
						for(int r=0;r<Parameters.totalRepeatNum;r++) {
							String value = ",";
							if(j<resultMap.get(r).size()){
								value = resultMap.get(r).get(j)+",";
							}
							os.append(value);
						}
						os.append("\r\n");
					}
				}
				os.flush();
				os.close();
			}
			catch(IOException e){
				System.out.println(e.getMessage());
			}
		}
		
		System.out.println("The experiment finish at: " + cTime.format(new Date()));
	}

	/**Get the number and executeTime of arrive VM at each execute time*/
	public static void getArriveVMNumofEachtime(List<VM> vmList,String scheduleName) {
		if(Parameters.vmNum == 1000 && Parameters.arrivalLamda == 0.8 && Parameters.cpuChangePeriod == 5) {
			int currentTime = 0;
			int arrivedVMNum = 0;
			
			while(arrivedVMNum<vmList.size()){
				int currentVMNum = 0;
				for(int i=arrivedVMNum;i<vmList.size();i++) { 
					if(vmList.get(i).getArriveTime() == currentTime) {
						arrivedVMNum++;//Total arrived VM in the vmList
						currentVMNum++;//Each time arrived VM in the vmList
					}
					else {
						break;//Break 'for i' after get all VMs that have the same arriveTime because the ascending arrival time
					}
				}
				//Store the total and average results into text
				String textPath = Parameters.eachTimeResult+"arriveVM-";
				String eachTimeResultTxt = textPath+scheduleName+"-"+Parameters.currentRepeatNum+".txt";
				boolean appendText = true;
				try	{
					if(currentTime == 0){
						appendText = false;
					}
					FileOutputStream fos = new FileOutputStream(eachTimeResultTxt,appendText);  //Write the result to txt file
					OutputStreamWriter os = new OutputStreamWriter(fos,"utf-8");
					
					os.append(currentTime+"   "+currentVMNum+"\r\n");
					os.flush();
					os.close();
				}
				catch(IOException e){
					System.out.println(e.getMessage());
				}
				currentTime++;
			}
			double executeTime = 0;
			for(int i=0;i<vmList.size();i++) {
				//Store the execute time into text
				String textPath = Parameters.eachTimeResult+ "arriveVM-executtime-";
				String eachTimeResultTxt = textPath+scheduleName+"-"+Parameters.currentRepeatNum+".txt";
				boolean appendText = true;
				try	{
					if(currentTime == 0){
						appendText = false;
					}
					FileOutputStream fos = new FileOutputStream(eachTimeResultTxt,appendText);  //Write the result to txt file
					OutputStreamWriter os = new OutputStreamWriter(fos,"utf-8");
					executeTime = vmList.get(i).getFinishTime()-vmList.get(i).getArriveTime();
					os.append(i+"   "+executeTime+"\r\n");
					os.flush();
					os.close();
				}
				catch(IOException e){
					System.out.println(e.getMessage());
				}
			}
		}
	}
	
}
