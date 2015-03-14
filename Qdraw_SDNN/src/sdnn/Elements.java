package sdnn;
import sdnn.CodePattern;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Elements {
	private static final int CODE = 200;			//コードパターンの次元数
	private CodePattern[] elements = null;		//コードパターンを管理する
	private int length = 0;						//elements.length
	private double resolution = 1.0;			//分解能
	public Elements(int n,double res){
		try{
			elements = new CodePattern [n];//コードパターンを生成
			for(int i=0; i<n; i++){
				elements[i] = new CodePattern(CODE);
			}
		}catch(NullPointerException e){
			System.out.println(e.getMessage());
		}
		length = n;
		resolution = res;
	}
	public Elements(int n){
		try{
			elements = new CodePattern [n];
			for(int i=0; i<n; i++){
				elements[i] = new CodePattern(CODE);
			}
		}catch(NullPointerException e){
			System.out.println(e.getMessage());
		}
		length = n;
	}
	//コードパターンを初期化
	protected void codePatternInit(){
		if(elements == null)
			System.out.println("elements error");
		//入力層を8等分するようにランダムにコードパターンを生成する
		for(int i=0; i<8; i++){
			//作成するコードパターンの場所
			int index = i*elements.length/8;
			Random rnd = new Random();
			for(int j=0; j<CODE; j++){
				int c;
				if((c = rnd.nextInt(2)) == 0){
					c = -1;
				}
				elements[index].setCode(j,c);
			}
		}
		//最後の1個にコードパターンを作成する
		Random rnd = new Random();
		for(int j=0; j<CODE; j++){
			int c;
			if((c = rnd.nextInt(2)) == 0){
				c = -1;
			}
			elements[elements.length-1].setCode(j,c);
		}
	}
	protected void codePatternCircleInit(){
		if(elements == null)
			System.out.println("elements error");
		//入力層を8等分するようにランダムにコードパターンを生成する
		for(int i=0; i<8; i++){
			//作成するコードパターンの場所
			int index = i*elements.length/8;
			Random rnd = new Random();
			for(int j=0; j<CODE; j++){
				int c;
				if((c = rnd.nextInt(2)) == 0){
					c = -1;
				}
				elements[index].setCode(j,c);
			}
		}
		//0番目と最後尾のコードパターンを一致させる
		elements[elements.length-1].copyCodePattern(elements[0]);
	}
	//作成したコードパターンの間を補完する
	protected void suppleCode_Circle(){
		int bet = length/8;		//補完するコードの間隔
		int balance = 5;   //最初に補完する数
		//200/(length/8)が割り切れないため5つだけ補完
		for(int i=0; i<8; i++){
			for(int j=1; j<balance; j++){
				int index = i*bet+j;  //作成するコードの場所
				//1つ前のコードを複製する
				elements[index].copyCodePattern(
						elements[index-1]);
				//2個のコードをランダムに変える
				for(int k=0; k<5; k++){
					int[] selectedIndex = randomListInt(5);
					elements[index].changeCode(selectedIndex[k]);
				}
			}
			//残りの50個のコードパターンを補完
			for(int j=balance; j<bet; j++){
				int index = i*bet+j;
				int[] selectedIndex = randomListInt(CODE);
				//1つ前のコードを複製する
				elements[index].copyCodePattern(
						elements[index-1]);
				/*コードをCODE/bet個ずつ前から順に変えていき*
				 * 最終的にbet個目のコードに近づいていく*/
				int change = 5;
				for(int k=0; k<change; k++){
					//int id = ((j-1)*change+k);
					
					int id = ((j-balance)*change+k);
					//次に設定するコード
					if(((i+1)*bet) <elements[i].getLength()){
						int c = elements[(i+1)*bet].getCode(selectedIndex[id]);
						elements[index].setCode(selectedIndex[id],c);
					}
				}
			}
		}
	}
	protected int[] randomListInt(int index){
		int[] result = new int [index];
		Random rnd = new Random();
		for (int i=0; i < index; i++)
			result[i] = i;
		for (int i = index-1; i > 1; i--) {
			int r = rnd.nextInt(i);
			int t = result[r];
			result[r] = result[i];
			result[i] = t;
		}
		return result;
	}
	/*1つ目のコードと真逆のコードを一番後ろに作る
	 * 最初と最後のコードパターンには類似性が出ないようにする*/
	public boolean createElements(){
		codePatternInit();	//一番前と最後尾の２つのコードパターンを生成
		suppleCode_Circle();
		/*
		//一番前から360個のコードパターンを生成
		Random rnd;
		for(int i=1; i<elements.length; i++){
			//1つ前のコードを複製する
			elements[i].copyCodePattern(
					elements[i-1]);
			rnd = new Random();
			for(int j=0; j<2; j++){
				int index = rnd.nextInt(CODE);
				elements[i].changeCode(index);
			}
		}
		*/
		return true;
	}
	/*1つ目のコードと同じコードを一番後ろに作る
	 * 最初と最後のコードパターンには類似性が出るようにする*/
	public void createCircleElements(){
		codePatternCircleInit();	//8つのコードパターンを等間隔に生成
		suppleCode_Circle();					//その後その間を44個のコードパターンで補完する
	}
	
	public CodePattern getCodePattern(int index){
		return elements[index];
	}
	public void setCodePattern(int index,String[] cp){
		elements[index].setCodePattern(cp);
	}
	public void setCodePattern(int index,CodePattern cp){
		elements[index].setCodePattern(cp);
	}
	public CodePattern[] getElements(){
		Elements tmp = new Elements(elements.length);
		for(int i=0; i<tmp.length; i++){
			tmp.setCodePattern(i,elements[i]);
		}
		return tmp.elements;
	}

	public void setElements(CodePattern[] el){
		for(int i=0; i<length; i++)
			elements[i].setCodePattern(el[i]);
	}

	public int getLength(){
		return length;
	}

	public double getResolution(){
		return resolution;
	}
	public final static int getDimension(){
		return CODE;
	}
}
