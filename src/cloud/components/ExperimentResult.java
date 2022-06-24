package cloud.components;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import cloud.configurations.Parameters;
import cloud.methods.Methods;

public class ExperimentResult {
	
	/**Calculate the experiment result*/
	public static void calculateExperimentResult(String scheduleName, List<VM> vmList, List<PM> usedPMList, int migrateVMCount, double totalScheduleTime) {
		//Format the result
		java.text.DecimalFormat formatD = new java.text.DecimalFormat("0.00000");
		
		//Store the results
		double energyConsume = 0;
		double cpuUtilize = 0;
		double executeCost = 0;
		double executeVMTime = 0;
		double usedPMNum = 0;
		double sla = 0;
		double averageScheduleTimePreVM = 0; //The average schedule time of each VM, radio of total schedule time and all VM's number

		//Calculate the total energy consumption, CPU utilization and cost
		double totalAverageCpuUtilize = 0; //The sum of each PM's average cpuUtilization
		
		double energyofPM = 0; //The PM's energy consumption during running
		double powerConsumpofPM = 0; //Power consumption of each PM
		double costofVMonPM = 0; //Cost of each VM on the assigned PM
		double totalCpuUtilizeofPM = 0; //Total cpuUtilization of each PM
		double averageCpuUtilizeofPM = 0; //Average cpuUtilization of each PM
		for(int i=0;i<usedPMList.size();i++){
			PM pm = usedPMList.get(i);
			energyofPM = 0; 
			powerConsumpofPM = 0; 
			totalCpuUtilizeofPM = 0; 
			averageCpuUtilizeofPM = 0; 
			for(double cpuUtilizeofEachTime : pm.getCpuUtilizeHistory()) {
				totalCpuUtilizeofPM = totalCpuUtilizeofPM + cpuUtilizeofEachTime;
			}
			if(pm.getCpuUtilizeHistory().size()>0) {
				averageCpuUtilizeofPM = totalCpuUtilizeofPM/pm.getCpuUtilizeHistory().size();
			}
			
			//Each VM assigned on PM
			for(VM vm : pm.getFinishedVMList()) {
				executeVMTime += (vm.getFinishTime() - vm.getArriveTime())/3600;
				costofVMonPM += executeVMTime*vm.getPrice();
			}
			totalAverageCpuUtilize = totalAverageCpuUtilize + averageCpuUtilizeofPM;
					
			powerConsumpofPM = pm.getIdlePower() + (pm.getMaxPower()-pm.getIdlePower())*averageCpuUtilizeofPM;
			energyofPM = powerConsumpofPM*((pm.getFinishTime()-pm.getStartTime())/3600);
			energyConsume = energyConsume + energyofPM;
			executeCost = executeCost + costofVMonPM;
			System.out.println(scheduleName+"--PM#" + pm.getId()+" CPUcore: " + pm.getTotalPes()+" CPU uti: " +totalCpuUtilizeofPM+
					" CPU uti size: " +pm.getCpuUtilizeHistory().size()+" execute time:"+ (pm.getFinishTime()-pm.getStartTime()) +" energy:" +energyofPM);
		
		}
		cpuUtilize = totalAverageCpuUtilize/usedPMList.size();

		//Calculate the schedule time
		averageScheduleTimePreVM = (double)totalScheduleTime/vmList.size(); //The scheduling time per VM
		
		//Calculate the number of used servers
		usedPMNum = usedPMList.size();
		
		//Calculate the number of used servers
		sla = Methods.calculateSLA(vmList,usedPMList);
				
		//Output the results
		System.out.println(scheduleName + "--Total energy consumption: " + formatD.format(energyConsume) +
				"--Total Cost: " + formatD.format(executeCost) +" CPU Utilization: " 
			    + formatD.format(cpuUtilize) +" SLA violation: "   + formatD.format(sla) 
			    + " Used PM Number: " + formatD.format(usedPMNum) + " Migrate VM Count: " + formatD.format(migrateVMCount) 
			    + " schedule Time:" + formatD.format(averageScheduleTimePreVM));
		
		//Record the results to array
		double currentParameterValue = 0;
		switch (Parameters.testParameter) {
		case "vmNum":
			currentParameterValue = Parameters.vmNum;
		    break;
		case "cpuChangePeriod":
			currentParameterValue = Parameters.cpuChangePeriod;
			break;
		case "arrivalLamdaGroup":
			currentParameterValue = Parameters.arrivalLamda;
			break;
		default:
		    break;
		}
		Parameters.energyConsumeResult[Parameters.currentAlgorithm][Parameters.currentRepeatNum] = energyConsume;
		Parameters.executeCostResult[Parameters.currentAlgorithm][Parameters.currentRepeatNum] = executeCost;
		Parameters.usedPMNumResult[Parameters.currentAlgorithm][Parameters.currentRepeatNum] = usedPMNum;
		Parameters.slaResult[Parameters.currentAlgorithm][Parameters.currentRepeatNum] = sla;
		Parameters.migrateVMCountResult[Parameters.currentAlgorithm][Parameters.currentRepeatNum] = migrateVMCount;
		Parameters.scheduleTimeResult[Parameters.currentAlgorithm][Parameters.currentRepeatNum] = averageScheduleTimePreVM;
		
		String resultTxt = Parameters.Result_file_location + Parameters.resultFile;
		
		//Add the title to new result txt when conducting a new parameter experiment
		if(Parameters.currentRepeatNum == 0 && Parameters.currentAlgorithm == 0 && Parameters.isFirstRepeat) {
			try	{
				FileOutputStream fos = new FileOutputStream(resultTxt);  //Write the result to txt file
				OutputStreamWriter os = new OutputStreamWriter(fos,"utf-8");
				String parameter = "VM Count: "+Parameters.vmNum + " arrivalLamda: "+Parameters.arrivalLamda+ " PM type: "+Parameters.pmPes.length+
						" cpuChangePeriod: " + Parameters.cpuChangePeriod;
				String title = "Repeat"+ "	"+ Parameters.testParameter+ "	"+ "Name"+ "	"+ "energyConsume"+ "	"+ "executeCost"+ "	"
						+ "  usedPMNum"+ "	"+ "  SLA"+ "	"+ "  migrateVMCount"+ "	"+ "  scheduleTime";
				os.append(parameter+"\r\n");
				os.append(title+"\r\n");
				os.flush();
				os.close();
			}
			catch(IOException e){
				System.out.println(e.getMessage());
			}
		}
		
		//Add each repeat result into result txt
		String resultString = Parameters.currentRepeatNum+ "	"+currentParameterValue+ "	"+scheduleName + "	"+formatD.format(energyConsume)+ "	"
		        +formatD.format(executeCost) +"	"
				+"	"+formatD.format(usedPMNum)+"	"+formatD.format(sla)+"	"+formatD.format(migrateVMCount)+"	"+formatD.format(averageScheduleTimePreVM);
		try	{
			FileOutputStream fos = new FileOutputStream(resultTxt,true);  //Write the result to txt file
			OutputStreamWriter os = new OutputStreamWriter(fos,"utf-8");
			os.append(resultString+"\r\n");
			os.flush();
			os.close();
		}
		catch(IOException e){
			System.out.println(e.getMessage());
		}
		
		//Calculate the average result into result txt when repeat times equals the max value and run the last algorithm
		if(Parameters.currentRepeatNum == Parameters.totalRepeatNum - 1 && Parameters.currentAlgorithm == Parameters.SCHEDULES.length-1) {
			for(int i=0;i<Parameters.SCHEDULES.length;i++) {
				double totalEnergyConsume = 0;
				double totalExecuteCost = 0;
				double totalUsedPMNum = 0;
				double totalSLA = 0;
				double totalMigrateVMCount = 0;
				double totalScheduleTimeVM = 0;
				
				double averageEnergyConsume = 0;
				double averageExecuteCost = 0;
				double averageUsedPMNum = 0;
				double averageSLA = 0;
				double averageMigrateVMCount = 0;
				double averageScheduleTimeVM = 0;
				
				for(int j=0;j<Parameters.totalRepeatNum;j++) {
					totalEnergyConsume = totalEnergyConsume + Parameters.energyConsumeResult[i][j];
					totalExecuteCost = totalExecuteCost + Parameters.executeCostResult[i][j];
					totalUsedPMNum = totalUsedPMNum + Parameters.usedPMNumResult[i][j];
					totalSLA = totalSLA + Parameters.slaResult[i][j];
					totalMigrateVMCount = totalMigrateVMCount + Parameters.migrateVMCountResult[i][j];
					totalScheduleTimeVM = totalScheduleTimeVM + Parameters.scheduleTimeResult[i][j];
					
					Parameters.energyConsumeResult[i][j] = 0;
					Parameters.executeCostResult[i][j] = 0;
					Parameters.usedPMNumResult[i][j] = 0;
					Parameters.slaResult[i][j] = 0;
					Parameters.migrateVMCountResult[i][j] = 0;
					Parameters.scheduleTimeResult[i][j] = 0;

					System.gc();
				}
				averageEnergyConsume = totalEnergyConsume/Parameters.totalRepeatNum ;
				averageExecuteCost = totalExecuteCost/Parameters.totalRepeatNum ;
				averageUsedPMNum =  totalUsedPMNum/Parameters.totalRepeatNum ;
				averageSLA =  totalSLA/Parameters.totalRepeatNum ;
				averageMigrateVMCount =  totalMigrateVMCount/Parameters.totalRepeatNum ;
				averageScheduleTimeVM =  totalScheduleTimeVM/Parameters.totalRepeatNum ;
				
				resultString = "Average:" + "	"+currentParameterValue+ "	" +Parameters.SCHEDULES[i].getName() + "	"
				+formatD.format(averageEnergyConsume)+"	"+formatD.format(averageExecuteCost)+"	"+ formatD.format(averageUsedPMNum)	
				+"	"+formatD.format(averageSLA)+"	"+formatD.format(averageMigrateVMCount)	+"	"+formatD.format(averageScheduleTimeVM);
				
				try	{
					FileOutputStream fos = new FileOutputStream(resultTxt,true);  //Write the result to txt file
					OutputStreamWriter os = new OutputStreamWriter(fos,"utf-8");
					os.append(resultString+"\r\n");
					os.flush();
					os.close();
				}
				catch(IOException e){
					System.out.println(e.getMessage());
				}
			}
		
		}
		
		//Add the each time value into result text
		if(Parameters.vmNum == 1000 && Parameters.arrivalLamda == 1.2 && Parameters.cpuChangePeriod == 5) {
			String eachTimeResultTxt1 = Parameters.Result_file_location + "eachTime\\"+ "activePM-"+scheduleName+"-"+Parameters.currentRepeatNum+".txt";
			try	{
				FileOutputStream fos = new FileOutputStream(eachTimeResultTxt1,false);  //Write the result to txt file
				OutputStreamWriter os = new OutputStreamWriter(fos,"utf-8");
				for(int i=0;i<Parameters.activePMPerTimeList.size();i++) {
					os.append(i+"   "+Parameters.activePMPerTimeList.get(i)+"\r\n");
				}
				os.flush();
				os.close();
			}
			catch(IOException e){
				System.out.println(e.getMessage());
			}
			
			String eachTimeResultTxt2 = Parameters.Result_file_location + "eachTime\\"+ "migrateVM-"+scheduleName+"-"+Parameters.currentRepeatNum+".txt";
			try	{
				FileOutputStream fos = new FileOutputStream(eachTimeResultTxt2,false);  //Write the result to txt file
				OutputStreamWriter os = new OutputStreamWriter(fos,"utf-8");
				for(int i=0;i<Parameters.migrateVMPerTimeList.size();i++) {
					os.append(i+"   "+Parameters.migrateVMPerTimeList.get(i)+"\r\n");
				}
				os.flush();
				os.close();
			}
			catch(IOException e){
				System.out.println(e.getMessage());
			}
			
			String eachTimeResultTxt3 = Parameters.Result_file_location + "eachTime\\"+ "shutdownPM-"+scheduleName+"-"+Parameters.currentRepeatNum+".txt";
			try	{
				FileOutputStream fos = new FileOutputStream(eachTimeResultTxt3,false);  //Write the result to txt file
				OutputStreamWriter os = new OutputStreamWriter(fos,"utf-8");
				for(int i=0;i<Parameters.shutdownPMPerTimeList.size();i++) {
					os.append(i+"   "+Parameters.shutdownPMPerTimeList.get(i)+"\r\n");
				}
				os.flush();
				os.close();
			}
			catch(IOException e){
				System.out.println(e.getMessage());
			}
		}
		
		Parameters.activePMPerTimeList.clear();
		Parameters.migrateVMPerTimeList.clear();
		Parameters.shutdownPMPerTimeList.clear();
		
	}
	
}
