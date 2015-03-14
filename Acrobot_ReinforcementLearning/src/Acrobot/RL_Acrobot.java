//SDNNを用いた強化学習でアクロボットの制御をシミュレーションします
package Acrobot;
import sdnn.*;

import java.lang.Math.*;
import java.util.Random;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

public class RL_Acrobot{
	private final static double T   = 0.1;                                   //1 step 0.1[s]
	private final static double TL  = 50.0;                                 //Maxtime per episode 50.0[s]
	private final static double EPI = 100.0;                               //Number of episodes 100 [times]
	private static double F   = 1.0;                                   //torque 1.0[N]
	private final static int GRID   = 360;                                   //number of grid, look up table, odd number
	private static       SDNN sdnnR = null;
	private static       SDNN sdnnL = null;
	//use Runge_kutta
	private static double kth1[] = new double [4];         
	private static double qth1[] = new double [4];
	private static double kth2[] = new double [4];
	private static double qth2[] = new double [4];
	//
	private static       double forceVector      = 0;                 //forceflag
	private final static double HGRID            = (GRID-1)/2;        //half grid
	private final static double Degree           = 0.0174532925;      //PI/180
	private final static double angle1Limit      = 180*Degree;        //limitation of theta 1
	private final static double angleSpeed1Limit = 2*180*Degree;      //limitation of thetad 1
	private final static double angle2Limit      = 180*Degree;        //limitation of theta 2
	private final static double angleSpeed2Limit = 4*180*Degree;      //limitation of thetad 2
	//current state
	private static       double currentAngle1;                //Angle of arm1,arn2(rad)
	private static double currentAngle2;
	private static       double currentAngleSpeed1;      //(rad/s)
	private static double currentAngleSpeed2;
	//privious state    
	private static       double priviousAngle1;
	private static double priviousAngle2;
	private static       double priviousAngleSpeed1;
	private static double priviousAngleSpeed2;  
	private static       boolean goalFlag   = false;          //Goal flag
	private static       double currentQ;                     //current Q value, next Q value
	private static double nextQ;
	private static double alpha = 0.5;                  //alpha
	private static double gam   = 0.9;                  //gamma
	private static       double e_greedy    = 0.1;            //0.0 => greedy
	private static       boolean isRandomAction = false;      //RandamAction Flag
	static double M1  = 1.0;                                  //link 1 1.0[kg]
	static double M2  = 1.0;                                  //link 2 1.0[kg]
	final static double I1  = 1.0;                            //moment of link 1
	final static double I2  = 1.0;                            //moment of link 2
	static double L1  = 1.0;                                  //link 1 1.0[m]
	static double L2  = 1.0;                                  //link 2 1.0[m]
	static double R1  = 0.5;                            //link 1 center of gravity 0.5[m]
	static double R2  = 0.5;                            //link 2 center of gravity 0.5[m]
	final static double G   = 9.8;  
	static String file_id = "";
	/*Acrobot*/
	/*Runge Kutta*/
	static void Runge_kutta(){
		                                 //acceleration of gravity
		double s11, s12, s21, s22, t1, t2, u1, u2;

		s11 = M1*R1*R1 + M2*L1*L1 + M2*R2*R2 + 2*M2*L1*R2*Math.cos(currentAngle2) + I1 + I2;
		s12 = M2*R2*R2 + M2*L1*R2*Math.cos(currentAngle2) + I2;
		s21 = s12;
		s22 = M2*R2*R2 + I2;
		t1 = (-1)*M2*L1*R2*(2*currentAngleSpeed1 + currentAngleSpeed2)*currentAngleSpeed2*Math.sin(currentAngle2);
		t2 = M2*L1*R2*currentAngleSpeed1*currentAngleSpeed1*Math.sin(currentAngle2);
		u1 = (M1*R1 + M2*L1)*G*Math.sin(currentAngle1) + M2*R2*G*Math.sin(currentAngle1 + currentAngle2);
		u2 = M2*R2*G*Math.sin(currentAngle1 + currentAngle2);
		kth1[0] = T*currentAngleSpeed1;
		qth1[0] = T*(s12*(forceVector - t2 - u2) + s22*(t1 + u1))/(s12*s21 - s11*s22);
		kth2[0] = T*currentAngleSpeed2;
		qth2[0] = T*(s11*(t2 + u2 - forceVector) - s21*(t1 + u1))/(s12*s21 - s11*s22);
		s11 = M1*R1*R1 + M2*L1*L1 + M2*R2*R2 + 2*M2*L1*R2*Math.cos(currentAngle2 + kth2[0]/2.0) + I1 + I2;
		s12 = M2*R2*R2 + M2*L1*R2*Math.cos(currentAngle2 + kth2[0]/2.0) + I2;
		s21 = s12;
		s22 = M2*R2*R2 + I2;
		t1 = (-1)*M2*L1*R2*(2*(currentAngleSpeed1 + qth1[0]/2.0) + (currentAngleSpeed2 + qth2[0]/2.0))*(currentAngleSpeed2 + qth2[0]/2.0)*Math.sin(currentAngle2 + kth2[0]/2.0);
		t2 = M2*L1*R2*(currentAngleSpeed1 + qth1[0]/2.0)*(currentAngleSpeed1 + qth1[0]/2.0)*Math.sin(currentAngle2 + kth2[0]/2.0);
		u1 = (M1*R1 + M2*L1)*G*Math.sin(currentAngle1 + kth1[0]/2.0) + M2*R2*G*Math.sin((currentAngle1 + kth1[0]/2.0) + (currentAngle2 + kth2[0]/2.0));
		u2 = M2*R2*G*Math.sin((currentAngle1 + kth1[0]/2.0) + (currentAngle2 + kth2[0]/2.0));
		kth1[1] = T*(currentAngleSpeed1+qth1[0]/2.0);
		qth1[1] = T*(s12*(forceVector - t2 - u2) + s22*(t1 + u1))/(s12*s21 - s11*s22);
		kth2[1] = T*(currentAngleSpeed2+qth2[0]/2.0);
		qth2[1] = T*(s11*(t2 + u2 - forceVector) - s21*(t1 + u1))/(s12*s21 - s11*s22);
		s11 = M1*R1*R1 + M2*L1*L1 + M2*R2*R2 + 2*M2*L1*R2*Math.cos(currentAngle2 + kth2[1]/2.0) + I1 + I2;
		s12 = M2*R2*R2 + M2*L1*R2*Math.cos(currentAngle2 + kth2[1]/2.0) + I2;
		s21 = s12;
		s22 = M2*R2*R2 + I2;
		t1 = (-1)*M2*L1*R2*(2*(currentAngleSpeed1 + qth1[1]/2.0) + (currentAngleSpeed2 + qth2[1]/2.0))*(currentAngleSpeed2 + qth2[1]/2.0)*Math.sin(currentAngle2 + kth2[1]/2.0);
		t2 = M2*L1*R2*(currentAngleSpeed1 + qth1[1]/2.0)*(currentAngleSpeed1 + qth1[1]/2.0)*Math.sin(currentAngle2 + kth2[1]/2.0);
		u1 = (M1*R1 + M2*L1)*G*Math.sin(currentAngle1 + kth1[1]/2.0) + M2*R2*G*Math.sin((currentAngle1 + kth1[1]/2.0) + (currentAngle2 + kth2[1]/2.0));
		u2 = M2*R2*G*Math.sin((currentAngle1 + kth1[1]/2.0) + (currentAngle2 + kth2[1]/2.0));
		kth1[2] = T*(currentAngleSpeed1+qth1[1]/2.0);
		qth1[2] = T*(s12*(forceVector - t2 - u2) + s22*(t1 + u1))/(s12*s21 - s11*s22);
		kth2[2] = T*(currentAngleSpeed2+qth2[1]/2.0);
		qth2[2] = T*(s11*(t2 + u2 - forceVector) - s21*(t1 + u1))/(s12*s21 - s11*s22);
		s11 = M1*R1*R1 + M2*L1*L1 + M2*R2*R2 + 2*M2*L1*R2*Math.cos(currentAngle2 + kth2[2]) + I1 + I2;
		s12 = M2*R2*R2 + M2*L1*R2*Math.cos(currentAngle2 + kth2[2]) + I2;
		s21 = s12;
		s22 = M2*R2*R2 + I2;
		t1 = (-1)*M2*L1*R2*(2*(currentAngleSpeed1 + qth1[2]) + (currentAngleSpeed2 + qth2[2]))*(currentAngleSpeed2 + qth2[2])*Math.sin(currentAngle2 + kth2[2]);
		t2 = M2*L1*R2*(currentAngleSpeed1 + qth1[2])*(currentAngleSpeed1 + qth1[2])*Math.sin(currentAngle2 + kth2[2]);
		u1 = (M1*R1 + M2*L1)*G*Math.sin(currentAngle1 + kth1[2]) + M2*R2*G*Math.sin((currentAngle1 + kth1[2]) + (currentAngle2 + kth2[2]));
		u2 = M2*R2*G*Math.sin((currentAngle1 + kth1[2]) + (currentAngle2 + kth2[2]));
		kth1[3] = T*(currentAngleSpeed1+qth1[2]);
		qth1[3] = T*(s12*(forceVector - t2 - u2) + s22*(t1 + u1))/(s12*s21 - s11*s22);
		kth2[3] = T*(currentAngleSpeed2+qth2[2]);
		qth2[3] = T*(s11*(t2 + u2 - forceVector) - s21*(t1 + u1))/(s12*s21 - s11*s22);
	}

	/*movement*/
	static void action(int act){
		//left action
		if(act==0){
			forceVector=-F;
		}
		//right action
		else{
			forceVector=F;
		}

		Runge_kutta();
		currentAngle1 += (kth1[0] + 2.0*kth1[1] + 2.0*kth1[2] + kth1[3])/6.0;
		currentAngle2 += (kth2[0] + 2.0*kth2[1] + 2.0*kth2[2] + kth2[3])/6.0;
		if(currentAngle1 < -angle1Limit){
			currentAngle1=angle1Limit+(currentAngle1+angle1Limit);
		}else if(currentAngle1>angle1Limit){
			currentAngle1=-angle1Limit+(currentAngle1-angle1Limit);
		}
		if(currentAngle2 < -angle2Limit){
			currentAngle2=angle2Limit+(currentAngle2+angle2Limit);
		}else if(currentAngle2 > angle2Limit){
			currentAngle2=-angle2Limit+(currentAngle2-angle2Limit);
		}
		currentAngleSpeed1 += (qth1[0] + 2.0*qth1[1] + 2.0*qth1[2] + qth1[3])/6.0;
		currentAngleSpeed2 += (qth2[0] + 2.0*qth2[1] + 2.0*qth2[2] + qth2[3])/6.0;
		if(currentAngleSpeed1 > angleSpeed1Limit){
			currentAngleSpeed1=angleSpeed1Limit;
		}else if(currentAngleSpeed1 < -angleSpeed1Limit){
			currentAngleSpeed1=-angleSpeed1Limit;
		}
		if(currentAngleSpeed2 > angleSpeed2Limit){
			currentAngleSpeed2=angleSpeed2Limit;
		}else if(currentAngleSpeed2 < -angleSpeed2Limit){
			currentAngleSpeed2=-angleSpeed2Limit;
		}
	}

	/*normalization*/
	static double cut(double a){
		if(a >= 1.0){
			return 0.9999999;
		}else if(a <= -1.0){
			return -0.9999999;
		}
		return a;
	}
	/*左か右のSDNNにアクロボットの状態を入力*/
	static double out(double x1, double x2, double x3, double x4, int a){
		double i,j,k,l;	    
		i = HGRID+(int)(cut(x1/angle1Limit)*HGRID);
		j = HGRID+(int)(cut(x2/angleSpeed1Limit)*HGRID);
		k = HGRID+(int)(cut(x3/angle2Limit)*HGRID);
		l = HGRID+(int)(cut(x4/angleSpeed2Limit)*HGRID);
		////////////////test
		//System.out.printf("θ1:%.1f					ω1:%.1f\n"
		//		+                  "θ2:%.1f					ω2:%.1f\n",
		//		i,j,k,l);
		////////////////test
		if(a==0){
			sdnnL.input(i,j,k,l);
			return sdnnL.out();
		}else{
			sdnnR.input(i,j,k,l);
			return sdnnR.out();
		}
	}
	/*select next action*/
	/*nextQに右か左か高い方のQ値を設定する*/
	static void selectNA(){
		double ql,qr;
		double qlc,qrc;
		/*
		boost::packaged_task<int> pt(boost::bind(thread_func,th1,currentAngleSpeed1,currentAngle2,currentAngleSpeed2,0));
		boost::unique_future<int> f = pt.get_future();
		new boost::thread(boost::ref(pt));
		boost::packaged_task<int> pt2(boost::bind(thread_func,th1,currentAngleSpeed1,currentAngle2,currentAngleSpeed2,1));
		boost::unique_future<int> f2 = pt2.get_future();
		new boost::thread(boost::ref(pt2));

	    ql=f.get();
	    qr=f2.get();
		 */
		/*右と左のSDNNの出力値を求める*/
		ql = out(currentAngle1,currentAngleSpeed1,currentAngle2,currentAngleSpeed2,0);
		qr = out(currentAngle1,currentAngleSpeed1,currentAngle2,currentAngleSpeed2,1);
		/*SDNNの出力値をQ値に変換*/
		qlc = (ql-300) / 30.0;
		qrc = (qr-300) / 30.0;
		if(qlc > qrc){
			nextQ = qlc;
		}else{
			nextQ = qrc;
		}
	}
	/*左と右のQ値を合計した値を返す*/
	static double getAmountQValue()
	{
		double ql, qr, amount;
		ql = out(currentAngle1,currentAngleSpeed1,currentAngle2,currentAngleSpeed2,0);
		qr = out(currentAngle1,currentAngleSpeed1,currentAngle2,currentAngleSpeed2,1);
		ql = (ql-300) / 30.0;
		qr = (qr-300) / 30.0;
		amount = ql + qr;
		return amount;
	}

	/*select current action*/
	/*左なら0、右なら1を返す*/
	static int selectA(){
		double ql,qr;
		double qlc,qrc;
		/*
		boost::packaged_task<int> pt(boost::bind(thread_func,currentAngle1,currentAngleSpeed1,currentAngle2,currentAngleSpeed2,0));
		boost::unique_future<int> f = pt.get_future();
		new boost::thread(boost::ref(pt));
		boost::packaged_task<int> pt2(boost::bind(thread_func,currentAngle1,currentAngleSpeed1,currentAngle2,currentAngleSpeed2,1));
		boost::unique_future<int> f2 = pt2.get_future();
		new boost::thread(boost::ref(pt2));

	  ql=f.get();
	  qr=f2.get();
		 */
		ql = out(currentAngle1,currentAngleSpeed1,currentAngle2,currentAngleSpeed2,0);
		qr = out(currentAngle1,currentAngleSpeed1,currentAngle2,currentAngleSpeed2,1);
		qlc = (ql-300) / 30.0;
		qrc = (qr-300) / 30.0;
		//ランダムにQ値を選択
		final double alpha   = 1.0;
		final int RANDOM_INT = 100;
		Random random = new Random();
		int rnd = random.nextInt(RANDOM_INT);
		if(qlc == qrc || rnd < alpha*RANDOM_INT*e_greedy){
			isRandomAction = true;
			//ランダムに右か左かのQ値を設定する
			if(rnd >= RANDOM_INT/2 ){
				currentQ=qlc;
				return 0;
			}else{
				currentQ=qrc;
				return 1;
			}
			//Q値の高い方を選択
		}
		else if(qlc>qrc){
			currentQ=qlc;
			return 0;
		}else{
			currentQ=qrc;
			return 1;
		}
	}

	/*Learning*/
	static void learn(double reward,int a){
		double i,j,k,l;
		double newQ;
		/*更新するQ値を求める*/
		newQ = (1.0-alpha)*(currentQ)+alpha*(reward+gam*(nextQ));
		/*求めたQ値をSDNNの教師値に変換する*/
		newQ = (newQ*30)+300;
		//System.out.printf("newQ:%.2f\n ",newQ);
		i=HGRID+(int)(cut(priviousAngle1/angle1Limit)*HGRID);
		j=HGRID+(int)(cut(priviousAngleSpeed1/angleSpeed1Limit)*HGRID);
		k=HGRID+(int)(cut(priviousAngle2/angle2Limit)*HGRID);
		l=HGRID+(int)(cut(priviousAngleSpeed2/angleSpeed2Limit)*HGRID);
		if(a==0){
			sdnnL.input(i, j, k, l);
			sdnnL.setTarget(newQ);
			sdnnL.learning();
		}else if(a==1){
			sdnnR.input(i, j, k, l);
			sdnnR.setTarget(newQ);
			sdnnR.learning();
		}
	}

	/*reward function*/
	static double reward(){
		/*
	    conditions:
	    height of tip > 1.0 => reward  200.0, episode end
	    currentAngle1 < 1 degree      => reward -10.0
	    normal              => reward  0.0
		 */

		if(-(Math.cos(currentAngle1)*Math.cos(currentAngle2)-Math.sin(currentAngle1)*Math.sin(currentAngle2)+Math.cos(currentAngle1))>1.0/*height of tip > 1.0*/){
			goalFlag = true;
			return 10.0;
		}else if(currentAngle1*currentAngle1<1.0*Degree*Degree ){
			return -5.0;
		}
		return 0.0;
	}
	
	static void startSimulation(int num){
		String filename = file_id+num+"_"+"episodeTime.csv";
		System.out.println("alpha: "+alpha+" gamma: "+gam);
		long start = System.currentTimeMillis();
		/*episode start*/
		for(int i = 0;i < EPI;i++){
			int act, counter = 0;
			double rew;
			double ql, qr;
			currentAngle1 = currentAngleSpeed1 = currentAngle2 = currentAngleSpeed2 = 0.0;
			while(!goalFlag){
				priviousAngle1=currentAngle1;
				priviousAngleSpeed1=currentAngleSpeed1;
				priviousAngle2=currentAngle2;
				priviousAngleSpeed2=currentAngleSpeed2;


				act=selectA();                   //select a current action

				action(act);                     //take the action
				rew=reward();                    //get a reward
				selectNA();                      //select a next action
				learn(rew,act);                  //modify the value function

				// LOG
				//log += tool.double2string(cut(priviousAngle1/angle1Limit)*HGRID);
				//log += ","+tool.double2string(cut(priviousAngle2/angle2Limit)*HGRID);
				double statusGoal = -(Math.cos(currentAngle1)*Math.cos(currentAngle2)-Math.sin(currentAngle1)*Math.sin(currentAngle2)+Math.cos(currentAngle1));
				ql = out(currentAngle1,currentAngleSpeed1,currentAngle2,currentAngleSpeed2,0);
				qr = out(currentAngle1,currentAngleSpeed1,currentAngle2,currentAngleSpeed2,1);
				double qlc = (ql-300)/30;
				double qrc = (qr-300)/30;
				//acrobotStatePrint();
				System.out.printf("Episode:%d\tTime:%.2f\n",i,counter*T);
				System.out.printf("Act:%d qlc:%.2f qrc:%.2f rew:%.1f\n",act,qlc,qrc,rew);
				//System.out.printf("Status:%.2f,%.2f,%.2f,%.2f\t",cut(currentAngle1/angle1Limit),cut(currentAngleSpeed1/angleSpeed1Limit),cut(currentAngle2/angle2Limit),cut(currentAngleSpeed2/angleSpeed2Limit));
				System.out.printf("height:%.3f\t",-(Math.cos(currentAngle1)*Math.cos(currentAngle2)-Math.sin(currentAngle1)*Math.sin(currentAngle2)+Math.cos(currentAngle1)));
				if (isRandomAction) {
					System.out.printf("RandomAction\n");
				} else {
					System.out.printf("\n");
				}
				isRandomAction = false;
				if(counter*T>=TL){
					break;
				}
				counter++;
			}
			System.out.printf("Goal Episode:%d Time:%.2f\n",i,counter*T);
			if(i==0){
				fileWrite(filename,"\nLearning Result\n");
			}
			if(i==9){
				try{
					sdnnL.parameterFileWriter(file_id+num+"_"+i+"count_"+"sdnnL");
					sdnnR.parameterFileWriter(file_id+num+"_"+i+"count_"+"sdnnR");
				}catch(IOException e){
					System.out.println(e);
				}
			}
			if(i==19){
				try{
					sdnnL.parameterFileWriter(file_id+num+"_"+i+"count_"+"sdnnL");
					sdnnR.parameterFileWriter(file_id+num+"_"+i+"count_"+"sdnnR");
				}catch(IOException e){
					System.out.println(e);
				}
			}
			if(i==29){
				try{
					sdnnL.parameterFileWriter(file_id+num+"_"+i+"count_"+"sdnnL");
					sdnnR.parameterFileWriter(file_id+num+"_"+i+"count_"+"sdnnR");
				}catch(IOException e){
					System.out.println(e);
				}
			}
			if(i==39){
				try{
					sdnnL.parameterFileWriter(file_id+num+"_"+i+"count_"+"sdnnL");
					sdnnR.parameterFileWriter(file_id+num+"_"+i+"count_"+"sdnnR");
				}catch(IOException e){
					System.out.println(e);
				}
			}
			if(i==49){
				try{
					sdnnL.parameterFileWriter(file_id+num+"_"+i+"count_"+"sdnnL");
					sdnnR.parameterFileWriter(file_id+num+"_"+i+"count_"+"sdnnR");
				}catch(IOException e){
					System.out.println(e);
				}
			}
			//episode timeを書き込む
			fileWrite(filename,i+","+(int)(counter*T)+"\n");
			if(e_greedy > 0)
				e_greedy -= 0.01;
			goalFlag  = false;
			counter=0;
		}
		e_greedy = 0.1;
		fileWrite(filename,"default setting ver. clear time: "+(System.currentTimeMillis()-start)+"\n");
	}
	//パラメータ調整メソッド一覧
	//リンクの重さが1.5[kg]
	static void changeTaskB(){
		M2 = 1.5;
	}
	//リンクの長さが1.5[m]、重さが1.5[kg]
	static void changeTaskC(){
		M2 = 1.5;
		L2 = 1.5; R2 = 0.75;
	}
	//加えるトルクが0.75[N]
	static void changeTaskD(){
		F = 0.75;
	}
	/*main function*/
	public static void main(String args[]){
		file_id = "";
		if(args.length > 0)
			file_id = args[0];
		System.out.println(file_id+" task simulation start");
		int perc = 360;
		double res[]   = {1.0, 1.0, 1.0, 1.0};
		for(int i=1; i<=10; i++){
			sdnnL = new SDNN(perc,res);
			sdnnR = new SDNN(perc,res);
			/*try{
				sdnnL.parameterFileReader("task01_"+i+"_"+"sdnnL");
				sdnnR.parameterFileReader("task01_"+i+"_"+"sdnnR");
			}catch(IOException e){
				System.out.println(e);
				return;
			}
			e_greedy = 0;*/
			//changeTaskB();
			//changeTaskC();
			//changeTaskD();
			startSimulation(i);
			try{
				sdnnL.parameterFileWriter(file_id+i+"_"+"sdnnL");
				sdnnR.parameterFileWriter(file_id+i+"_"+"sdnnR");
			}catch(IOException e){
				System.out.println(e);
			}
		}
	}
	public static void fileWrite(String filename,String str){
		//ファイルの作成
		File episodeFile = new File(filename);
		try{
			FileWriter filewriter = new FileWriter(episodeFile,true);
			filewriter.write(str);
			filewriter.close();
		}catch(IOException e){
			System.out.println(e);
		}
	}
	//テスト用：アクロボットの状態をprint
	public static void acrobotStatePrint(){
		System.out.printf("θ1:%.1f					ω1:%.1f\n"
				+                  "θ2:%.1f					ω2:%.1f\n",
				HGRID+(int)(cut(currentAngle1/angle1Limit)*HGRID),
				HGRID+(int)(cut(currentAngleSpeed1/angleSpeed1Limit)*HGRID),
				HGRID+(int)(cut(currentAngle2/angle2Limit)*HGRID),
				HGRID+(int)(cut(currentAngleSpeed2/angleSpeed2Limit)*HGRID));
	}
}
