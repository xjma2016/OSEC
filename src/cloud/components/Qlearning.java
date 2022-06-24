package cloud.components;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import cloud.configurations.Parameters;
import cloud.methods.CreatComponents;

public class Qlearning {
	private double alpha = 0.2; //Learning rate 0.3
	private double gamma = 0.6; //Reward decay 0.5
	private double epsilon = 0.8; //e greedy 0.85
	private static int maxTrainStep = 500; //The max training step of the Q-Table

	private double currentTime; //The current time in scheduling process
	private int timeNo; //The timeNo in total scheduling process
	private static List<VM> vmList; //The assigned VMs indicate states
	private static List<PM> pmList; //The active PMs indicate actions
	
	private CreatComponents creatComponents;
	
	/**
	 * State-row: the index of arrived VMs; 
	 * Action-column: the index of active PMs, indicate the PM that selected VM will be assigned 
	 * Q-Value: the fitness of each PM in DC.
	 */
	Map<Integer, double[]> qTable = new TreeMap<Integer, double[]>();

	public Qlearning(List<VM> arrivedVMs, List<PM> activePMs, double cTime) {
		
		vmList = arrivedVMs;
		pmList = activePMs;
        currentTime = cTime;
		timeNo = (int) Math.floor(currentTime / Parameters.cpuChangePeriod); //Calculate the actual time No.;
		creatComponents = new CreatComponents();
		
		//Add new PMs if the resource of active PMs cannot meet the requirement of arrived VMs
		initActivePM(vmList,pmList, currentTime);
			
		initQTable();
	}

	/** Add new PMs into activePMList to prevent existing PMs from failing to meet the demand of arrivedVM. */
	private void initActivePM(List<VM> vmList, List<PM> pmList, double currentTime) {
		//Add a new PM if the pmList'size is zero
		if(pmList.size() == 0) {
			PM newPM = creatComponents.createRandomPM(vmList.get(0), currentTime);
			pmList.add(newPM);
		}
	}
	
	/** Use Eq.14s to set the Q-Value. */
	private void initQTable() {
		for (int i = 0; i < vmList.size(); i++) { //Row from 1 to the VM count
			double[] temp = new double[pmList.size()];
			for (int j = 0; j < pmList.size(); j++) { //Column from 1 to the PM count
				PM pm = pmList.get(j);
				if(pm.getIdlePes()>=vmList.get(i).getRequestedPes() && pm.getIdleRam()>=vmList.get(i).getRequestedRam() 
						&& pm.getCpuUtilize() + vmList.get(i).getCpuUtilize(timeNo)<Parameters.overloadofCpu) {
					temp[j] = 0;
				}
				else {
					temp[j] = -1;
				}
				
			}
			qTable.put(i, temp); //Add each state and Q-Value into QTable
		}
	}

	/** Use the epsilon-greedy select the PM with max fitness. */
	private int chooseAction(int state) {
		int action = 0;
		List<Integer> suitablePMIndex = new ArrayList<Integer>();
		
		//Get suitable PM index
		boolean suitPM = false;
		for(int i=0;i<pmList.size();i++) {
			PM pm = pmList.get(i);
			if(pm.getIdlePes()>=vmList.get(state).getRequestedPes() && pm.getIdleRam()>=vmList.get(state).getRequestedRam() 
					&& pm.getCpuUtilize() + vmList.get(state).getCpuUtilize(timeNo)<=Parameters.overloadofCpu) {
				suitPM = true;
				suitablePMIndex.add(i);
			}
		}
		
		//Add new PM if no suitable PM in the pmList for vm state
		if(!suitPM) {
			PM newPM = creatComponents.createRandomPM(vmList.get(state), currentTime);
			pmList.add(newPM);
			suitablePMIndex.add(pmList.size()-1);
			for (int i = 0; i < vmList.size(); i++) { //Row from 1 to the VM count
				double[] temp = new double[pmList.size()];
				for (int j = 0; j < pmList.size(); j++) { //Column from 1 to the PM count
					if(j != pmList.size()-1) { //Copy the fitness from qTable
						temp[j] = qTable.get(i)[j];
					}
					else {
						if(newPM.getIdlePes()>=vmList.get(i).getRequestedPes() && newPM.getIdleRam()>=vmList.get(i).getRequestedRam() 
								&& newPM.getCpuUtilize() + vmList.get(i).getCpuUtilize(timeNo)<=Parameters.overloadofCpu) {
							temp[j] = 0; //Set the Qvalue as the fitness of newPM
						}
						else {
							temp[j] = -1; 
						}
					}
				}
				qTable.remove(i);
				qTable.put(i, temp); //Add each state and Q-Value into QTable
			}
			
		}

		double random = Math.random();
		double[] state_action = qTable.get(state);;
		if (random > epsilon) {
			if(state_action == null) {
				action = getIndexofMaxValue(state_action);
			}
			action = getIndexofMaxValue(state_action);
		} 
		else {
			action = new Random().nextInt(state_action.length);
		}

		return action;
	}

	/** QTable learning with state,action,reward and stateNext. */
	private void learn(int state, int action, double reward, int stateNext) {
		double qPredict = qTable.get(state)[action];
		double qTarget;
		if (stateNext != vmList.size() ) {
			qTarget = reward + gamma * getMaxValue(stateNext);
		} else {
			qTarget = reward;
		}
		qTable.get(state)[action] += alpha * (qTarget - qPredict);
		//outPut(qTable);
	}
	
	/** Get the index of max value in an array. */
	private int getIndexofMaxValue(double[] array) {
		List<Integer> index = new ArrayList<Integer>();
		double maxTemp = array[0];
		index.add(0);
		//Get the index list when there has more than one equal max value in the array
		for (int i = 1; i < array.length; i++) {
			if (maxTemp < array[i]) {
				maxTemp = array[i];
				index.clear();
				index.add(i);
			}
			if (maxTemp == array[i]) {
				index.add(i);
			}
		}
		
		//Random chose the index if there is more than one max equal value
		return index.get(new Random().nextInt(index.size()));
	}
	
	/** Get the max Qvalue of next state. */
	private double getMaxValue(int nextState) {
		double maxFitness = Double.MIN_VALUE;
		for(int i=0;i<pmList.size();i++) {
			double tempFitness = qTable.get(nextState)[i];
			if(maxFitness < tempFitness) {
				maxFitness = tempFitness;
			}
		}
		
		return maxFitness;
	}

	/**
	 * The reward is difference between the fitness of a PM when assigned the select
	 * VM on it or not.
	 */
	private double reward(int state, int action) {
		double[] fitness = new double[pmList.size()];
		
		for(int i = 0;i<pmList.size();i++) {
			PM pm = pmList.get(i);
			if (pm.getIdlePes() - vmList.get(state).getRequestedPes() >= 0
					&& pm.getIdleRam() - vmList.get(state).getRequestedRam() >= 0
					&& pm.getCpuUtilize() + vmList.get(state).getCpuUtilize(timeNo)<= Parameters.overloadofCpu) { //The action is suitable for the VM on the PM
				fitness[i] = calculatePMECFitness(vmList.get(state), pmList, timeNo, pm);
			}
			else {
				fitness[i] = Double.MAX_VALUE;
			}
		}
		
		List<Integer> pmIndexWithMinfitness = new ArrayList<Integer>();
		double tempMinfitness = Double.MAX_VALUE;
		for(int i=0;i<fitness.length;i++) {
			if(tempMinfitness>fitness[i]) {
				tempMinfitness = fitness[i];
				pmIndexWithMinfitness.clear();
				pmIndexWithMinfitness.add(i);
			}
			if(tempMinfitness==fitness[i]) {
				pmIndexWithMinfitness.add(i);
			}
		}
		
		if(pmIndexWithMinfitness.contains(action)) {
			return 1;
		}
		if(fitness[action] != Double.MAX_VALUE) {
			return 0;
		}
		else {
			return -1;
		}
	}
	
	/**
	 * Calculate estimate the select PM's fitness with an assign VM by EC and Cost
	 */
	public double calculatePMECFitness(VM vm, List<PM> pmList, int timeNo, PM pm) {
		double[] ec = new double[pmList.size()]; //Energy consumption
		double[] ct = new double[pmList.size()]; //Cost
		
		double[] yec = new double[pmList.size()]; //Energy consumption and cost after z-score method
		double[] yct = new double[pmList.size()];
		
		double aec, act, sEC,sCT, tEC = 0,tCT = 0,estFitness; //The average energy consumption and cost
		double totalEC = 0,totalCT = 0; //The total energy consumption and cost
		
		double vmPrice,vmBaseTime;

		if(vm == null) {
			vmPrice = 0;
			vmBaseTime = 0;
		}
		else {
			vmPrice = vm.getPrice();
			vmBaseTime = vm.getTask().getBaseExecuteTime();
		}
	
		for (int i = 0; i < pmList.size(); i++) {
			ec[i] = pmList.get(i).getIdlePower() + (pmList.get(i).getMaxPower() - pmList.get(i).getIdlePower())
					* (pmList.get(i).getCpuUtilize());
			ct[i] = (vmBaseTime/pmList.get(i).getMIPS())*vmPrice;//The rental cost  is calculated by Eq.3
			totalEC +=ec[i];
			totalCT +=ct[i]; 
		}
		aec = totalEC/pmList.size();
		act = totalCT/pmList.size();
		
		double tempEC = 0,tempCT = 0;
		for (int i = 0; i < pmList.size(); i++) {
			tempEC += (ec[i]-aec)*(ec[i]-aec);
			tempCT += (ct[i]-act)*(ct[i]-act);
		}
		
		//Calculate by Eq.14
		if(pmList.size() == 1) {
			sEC = Math.sqrt(tempEC);
			sCT = Math.sqrt(tempCT);
		}
		else {
			sEC = Math.sqrt(tempEC/(pmList.size()-1));
			sCT = Math.sqrt(tempCT/(pmList.size()-1));
		}
		
		double  tempSEC,tempSCT;
		tempSEC=sEC>0 ? sEC :1; 
		tempSCT=sCT>0 ? sEC :1; 
		
		//Calculate by Eq.12
		for (int i = 0; i < pmList.size(); i++) {
			yec[i] = (ec[i]-aec)/tempSEC;
			tEC +=yec[i];
			yct[i] = (ct[i]-act)/tempSCT;
			tCT += yct[i];
		}
		
		estFitness = Parameters.wFactor*tEC+(1-Parameters.wFactor)*tCT;
		return estFitness;
	}

	/** After training max steps return the best index of PM for each VM. */
	public int[] getBestPMIndex() {
		int[] bestPMIndex = new int[vmList.size()];
		int action = 0;
		int state = 0;
		int stateNext;
		double reward = 0;

		for (int i = 0; i < maxTrainStep; i++) {//Total training episode
			epsilon = epsilon - i*0.0005;
			while (state != vmList.size() ) {
				action = chooseAction(state);
				stateNext = state + 1;
				reward = reward(state, action);
				learn(state, action, reward, stateNext);
				state = stateNext;
			}
			
			//Output the trains result
			if(Parameters.vmNum == 1000 && Parameters.arrivalLamda == 1.2 && vmList.size() == 12) {
				qValue(i,qTable); //Output the value of qTable at each train step
				reward(i,reward); //Output the reward at each train step
			}
			
			//Reset the idle[] after each train loop
			for(VM vm : vmList) {
				PM pm = vm.getPM();
				if(pm != null) {
					pm.migrateVM(vm, vm.getCpuUtilize(timeNo));
				}
			}
			
			state = 0;
		}

		// Get the best PM index for each VM after the training
		double[] state_action;
		double tempMax;
		List<Integer> suitableAction = new ArrayList<Integer>();
		
		for (int i = 0; i < vmList.size() ; i++) {
			suitableAction.clear();
			state_action = qTable.get(i);
			tempMax = state_action[0];
			suitableAction.add(0);
			for (int j = 1; j < state_action.length; j++) {
				if (tempMax < state_action[j]) {
					tempMax = state_action[j];
					suitableAction.clear();
					suitableAction.add(j);
				}
				else if(tempMax == state_action[j]) {
					suitableAction.add(j);
				}
			}
			bestPMIndex[i] = suitableAction.get(new Random().nextInt(suitableAction.size())) ;
		}
		
		return bestPMIndex;
	}

	/** Output the value of QTable. */
	public void outPut(Map<Integer, double[]> qTable) {
		System.out.print("idleCPU: " );
		for(int i=0;i<pmList.size();i++) {
			System.out.print(pmList.get(i).getIdlePes()+" ");
		}
		System.out.println();
		System.out.print("idleRam: " );
		for(int i=0;i<pmList.size();i++) {
			System.out.print(pmList.get(i).getIdleRam()+" ");
		}
		System.out.println();
		System.out.print("CPUUtilize: " );
		for(int i=0;i<pmList.size();i++) {
			System.out.print(pmList.get(i).getCpuUtilize()+" ");
		}
		
		System.out.println();
		String columnTxt = "----PMIndex:          ";
		for(int i=0;i<pmList.size();i++) {
			columnTxt += i+"                   " ;
		}
		System.out.println(columnTxt);
		
		for (int i = 0; i < vmList.size(); i++) { //Row from 1 to the VM count
			String rawTxt = "----VM"+i +":   ";
			for (int j = 0; j < pmList.size(); j++) { //Column from 1 to the PM count
				rawTxt += qTable.get(i)[j] + "   "; //Get the Qvalue 
			}
			System.out.println(rawTxt);
		}
	}
	
	/** Output the value of QTable. */
	public void qValue(int trainNum, Map<Integer, double[]> qTable) {
		//Add the qTable value into result text
		String fitnessResultTxt = Parameters.Result_file_location + "qlearning\\"+"qValue"+Parameters.vmNum +Parameters.arrivalLamda+vmList.size()+".txt";
		boolean notFirst = true;
		if(trainNum == 0 ) {
			notFirst = false;
		}
		
		try	{
			FileOutputStream fos = new FileOutputStream(fitnessResultTxt,notFirst);  //Write the result to txt file
			OutputStreamWriter os = new OutputStreamWriter(fos,"utf-8");
			if(!notFirst) {
				String parameter = "VM Count: "+vmList.size() + " PM Count: "+pmList.size()+ " maxTrainStep: "+maxTrainStep+
						" alpha: " + alpha+" gamma: " + gamma +" epsilon: " + epsilon ;
				os.append(parameter+"\r\n");
				String parameter1 = "train Num"+ "     "+"qValue" ;
				os.append(parameter1+"\r\n");
			}
			
			//Calculate the energy consume and cost of pmList
			double qValue = 0;
			for(int i=0;i<qTable.size();i++){
				double[] state_action = qTable.get(i);
				for(double q : state_action) {
					qValue += q;
				}
			}
			
			os.append(trainNum+":   "+qValue + "\r\n");
			os.flush();
			os.close();
		}
		catch(IOException e){
			System.out.println(e.getMessage());
		}	
	}
	
	/** Output the reward. */
	public void reward(int trainNum, double reward) {
		//Add the reward into result text
		String fitnessResultTxt = Parameters.Result_file_location +  "qlearning\\"+"reward"+Parameters.vmNum +Parameters.arrivalLamda+vmList.size()+".txt";
		boolean notFirst = true;
		if(trainNum == 0 ) {
			notFirst = false;
		}
		
		try	{
			FileOutputStream fos = new FileOutputStream(fitnessResultTxt,notFirst);  //Write the result to txt file
			OutputStreamWriter os = new OutputStreamWriter(fos,"utf-8");
			if(!notFirst) {
				String parameter = "VM Count: "+vmList.size() + " PM Count: "+pmList.size()+ " maxTrainStep: "+maxTrainStep+
						" alpha: " + alpha+" gamma: " + gamma +" epsilon: " + epsilon ;
				os.append(parameter+"\r\n");
				String parameter1 = "train Num"+ "     "+"reward" ;
				os.append(parameter1+"\r\n");
			}
			os.append(trainNum+":   "+reward + "\r\n");
			os.flush();
			os.close();
		}
		catch(IOException e){
			System.out.println(e.getMessage());
		}	
	}
	
	/** Output the fitness of QTable. */
	public void fitness(int trainNum, Map<Integer, double[]> qTable) {
		//Add the fitness value into result text
		String fitnessResultTxt = Parameters.Result_file_location + "fitness"+".txt";
		boolean notFirst = true;
		if(trainNum == 0 ) {
			notFirst = false;
		}
		
		try	{
			FileOutputStream fos = new FileOutputStream(fitnessResultTxt,notFirst);  //Write the result to txt file
			OutputStreamWriter os = new OutputStreamWriter(fos,"utf-8");
			if(!notFirst) {
				String parameter = "VM Count: "+vmList.size() + " PM Count: "+pmList.size()+ " maxTrainStep: "+maxTrainStep+
						" alpha: " + alpha+" gamma: " + gamma +" epsilon: " + epsilon ;
				os.append(parameter+"\r\n");
				String parameter1 = "train Num"+ "     "+"energy consume" + "    " + "cost";
				os.append(parameter1+"\r\n");
			}
			
			//Calculate the energy consume and cost of pmList
			double energyConsume = 0;
			double executeCost = 0;
			for(int i=0;i<pmList.size();i++){
				PM pm = pmList.get(i);
				double executeVMTime = 0;
				double costofVMonPM = 0;
				
				//Each VM assigned on PM
				for(VM vm : pm.getFinishedVMList()) {
					executeVMTime += vm.getFinishTime() - vm.getArriveTime();
					costofVMonPM += (executeVMTime/3600)*vm.getPrice();
				}
				for(VM vm : pm.getActiveVMList()) {
					executeVMTime += vm.getFinishTime() - vm.getArriveTime();
					costofVMonPM += (executeVMTime/3600)*vm.getPrice();
				}

				//Add energy consume and cost of each PM into total
				double powerConsumpofPM = pm.getIdlePower() + (pm.getMaxPower()-pm.getIdlePower())*pm.getCpuUtilize();
				double energyofPM = powerConsumpofPM*((currentTime-pm.getStartTime())/3600);
				energyConsume = energyConsume + energyofPM;
				executeCost = executeCost + costofVMonPM;
			}
			
			os.append(trainNum+":   "+energyConsume + "    " + executeCost +"\r\n");
			os.flush();
			os.close();
		}
		catch(IOException e){
			System.out.println(e.getMessage());
		}	
	}

}
