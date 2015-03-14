package sdnn;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Math;
import java.math.BigDecimal;

import sdnn.Elements;
import sdnn.CodePattern;

public class SDNN {
	
	private Elements inputLayer[]        = null; //入力層
	private Elements shuffledLayer[]     = null; //入力層をシャフルしたもの
	private int selectedIndex[]          = null; //入力値に対応するのコードパターンを管理
	private int middleLayer[]            = null; //中間層
	private int SUMOUT 					 = 600;	 //出力素子の数
	//weightの型をfloatに変えると動作が早くなる
	private double weight[][]            = null; //中間素子から出力層への結合荷重
	private double h[]					 = null; //各出力素子のしきい値
	private double target		= 1.0;	 //学習での目標値
	private final int CODE            = 200;

	/*入力層、中間層、出力層全体の初期化を行う*/
	public SDNN(int n,double r[]){    //各素子群毎のコードパターンの数
		/*入力層の初期化を行う*/
		try{
			inputLayer = new Elements [r.length];
			for(int i=0; i<r.length; i++){
				inputLayer[i] = new Elements(n,r[i]);
			}
		}catch(NullPointerException e){
			System.out.println(e.getMessage());
			return;
		}
		//inputLayerの初期化
		for(int i=0; i<r.length; i++){
			inputLayerInit(i);
		}
		/*入力層をシャフルした結果を求める*/
		shuffleInputLayer();
		/*中間層を生成*/
		middleLayerInit();
		/*weightを生成*/
		weightInit();
		/*しきい値を設定*/
		hInit();
		/*出力層の初期化を行う*/
		outputLayerInit();
	}
	
	/*入力層の初期化を行う*/
	protected void inputLayerInit(int index){
		try{
			if(inputLayer[index] == null)//test
				System.out.println("inputLayer[index] null");
			/*入力層にある各素子群のコードパターンを作成する
			 * 1つめと３つめのコードパターンは循環させる*/
			if(index == 0 || index == 2){
				inputLayer[index].createCircleElements();
			}else{
				inputLayer[index].createElements();
			}
		}catch(NullPointerException e){
			System.out.println("Elements InitializeError");
		}
	}
	
	/*入力層をシャフルした結果を別の配列に格納する*/
	protected void shuffleInputLayer(){
		if(inputLayer == null){
			return;
		}
		/*shuffleLayerの初期化*/
		try{
			shuffledLayer = new Elements [inputLayer.length];
			for(int i=0; i<inputLayer.length; i++){
				shuffledLayer[i] = new Elements(inputLayer[i].getLength());
			}
		}catch(NullPointerException e){
			System.out.println(e.getMessage());
			return;
		}
		/*入力層の各素子群のコードパターンをシャフル*/
		for(int i=0; i<inputLayer.length; i++){
			List<CodePattern> list = Arrays.asList(inputLayer[i].getElements());

			// リストの並びをシャッフルします。
    		Collections.shuffle(list);
    		
    		// listから配列へ戻します。
    		CodePattern[] cptmp = new CodePattern [list.size()];
    		for(int j=0; j<list.size(); j++){
    			cptmp[j] = new CodePattern(Elements.getDimension());
    			cptmp[j] = (CodePattern)list.get(j);
    		}
    		shuffledLayer[i].setElements(cptmp);
		}

		System.out.println("shuffle complete");
	}

	/*中間層の初期化を行う*/
	protected boolean middleLayerInit(){
		/*入力層が生成されてなければ中間層の初期化は不可*/
		if(inputLayer == null){
			return false;
		}
		/*すでに中間層が作られていれば初期化しなおす*/
		if(middleLayer != null){
			middleLayer = null;
		}
		try{
			middleLayer = new int [inputLayer.length*(inputLayer.length-1)*Elements.getDimension()];
			System.out.println("middleLayerInit complete");
		}catch(NullPointerException e){
			System.out.println("middleLayerInit error");
			System.out.println(e.getMessage());
			return false;
		}
		return true;
	}
	/*weight<0.005を満たすランダムな値を入れておく*/
	protected void weightInit(){
		weight = new double [middleLayer.length][SUMOUT];
		for(int i=0; i<weight.length; i++){
			for(int j=0; j<weight[0].length; j++){
				double n = (Math.random()-0.5)/10.0;
				weight[i][j] = (float)n;            //-0.05<=weight<=0.05
			}
		}
	}
	/*しきい値を設定する*/
	protected void hInit(){
		double a = 1.0;		//しきい値の傾き
		h = new double [SUMOUT];
		//最初の300個は1の傾斜をつけて負の数を与える
		for(int i=299; i>=0; i--){
			h[i] = (float) (-a*(300-i));
		}
		//300個目からは1の傾斜をつけて正の数を与える
		for(int i=300; i<600; i++){
			h[i] = (float) (a*(i-299));
		}
	}
	/*入力値を受け取り、中間層を生成及び出力層から出力値を求め,学習値の更新まで行う*/
	public void input(double... input){
		/*入力値からそれに対応する各層のコードパターンを選択*/
		selectedIndex = selectedInputIndex(input);
		if(selectedIndex == null){
			return;
		}
		/*選択したコードパターンをシャフルした修飾パターンで不感化させる
		不感化させたコードパターンはmiddleLayerに格納していく*/
		List<String> list = new ArrayList<String>();
		for(int i=0; i<inputLayer.length; i++){
			for(int j=0; j<inputLayer.length; j++){
				if(i == j){
					continue;
				}
				/*修飾される方のコードパターン*/
				CodePattern outputPattern = 
					inputLayer[i].getCodePattern(selectedIndex[i]);
				/*修飾パターン*/
				CodePattern modPattern = 
					shuffledLayer[j].getCodePattern(selectedIndex[i]);
				try{
					/*コードパターンを不感化した結果を返す*/
					CodePattern desPattern = 
						neuronDesensitise(outputPattern,modPattern);
					/*middleLayerにコードパターンをlistで追加していく。*/
					list.addAll(middleLayerAdd(desPattern));
				}catch(NullPointerException e){
					System.out.println("不感化エラー");
					return;
				}
			}
		}
		String tmp[] = list.toArray(new String[list.size()]);
		for(int i=0; i<tmp.length; i++){
			middleLayer[i] = Integer.parseInt(tmp[i]);
		}
		/*各出力素子の値を計算*/
		outputLayer = calcOutputLayer(middleLayer,weight);
		resultOutput(outputLayer);
	}

	/*入力値に対応するコードパターンを各素子群から選ぶ*/
	protected int[] selectedInputIndex(double... ns){
		if(inputLayer == null){
			return null;
		}
		if(ns.length != inputLayer.length){
			return null;
		}
		/*各入力層の入力値に対応する配列の要素の値をselectedに格納する*/
		int selected[] = new int [inputLayer.length];
		for(int i=0; i<selected.length; i++){
			int index = (int)(ns[i]/inputLayer[i].getResolution());  //入力値に対するコードパターン
			selected[i] = index;
		}
		return selected;
	}

	/*コードパターン同士を不感化させるメソッド*/
	protected CodePattern neuronDesensitise(CodePattern out,CodePattern mod){
		CodePattern result = new CodePattern (out.getLength());
		/*各素子について不感化していく*/
		for(int i=0; i<out.getLength(); i++){
			int c = (1+mod.getCode(i))/2*out.getCode(i);
			result.setCode(i,c);
		}
		return result;
	}

	/*middleLayerに不感化したコードパターンを追加していく*/
	protected List<String> middleLayerAdd(CodePattern cp){
		List<String> list = new ArrayList<String>();
		for(int i=0; i<cp.getLength(); i++){
			list.add(String.valueOf(cp.getCode(i)));
		}
		return list;
	}

	/*中間層と結合荷重の値を元に出力層を生成*/
	protected void outputLayerInit(){
		if(middleLayer == null){
			return;
		}
		outputLayer = new double [SUMOUT];
	}

	/*出力層を計算する*/
	private double outputLayer[]        = null; //出力層
	private double outputResult;	 //出力値を求めるための出力素子の数
	protected double[] calcOutputLayer(int x[],double w[][]){
		double out[] = new double [SUMOUT];
		/*各中間素子と結合荷重により出力素子を計算する*/
		for(int i=0; i<out.length; i++){
			for(int j=0; j<middleLayer.length; j++){
				out[i] += x[j] * w[j][i];
			}
			out[i] -= h[i];  //しきい値分を引く
		}
		return out;
	}

	/*各出力素子から出力値の合計を求める*/
	protected void resultOutput(double out[]){
		int result = 0;
		for(int i=0; i<out.length; i++){
			if(out[i] > 0){
				result += 1;
			}else{
				//なにもしない
			}
		}
		outputResult = result;
	}

	/*学習についてのパラメータの修正を行う*/
	private final double c 		= 0.05;   //学習係数
	public void learning(){
		int displace = (int)(Math.abs(target-outputResult));
		int[] outIndex = new int [displace];
		/*修正すべき出力素子を順に並べるようにする*/
		if(outputResult < target){
			outIndex = findLowerIndex(outputLayer,displace);
			/*|出力値-目標値|の数だけ学習パラメータを調整する*/
			for(int i=0; i<outIndex.length; i++){
				/*重みweightについて修正*/
				for(int j=0; j<middleLayer.length; j++){
					if(middleLayer[j] != 0){
						weight[j][outIndex[i]] += 
							c*(target-outputResult)*middleLayer[j];
					}
				}
				/*各出力素子のしきい値について修正*/
				h[outIndex[i]] += -c*(target-outputResult);
			}
		}else if(outputResult > target){
			outIndex = findHigherIndex(outputLayer,displace);
			/*|出力値-目標値|の数だけ学習パラメータを調整する*/
			for(int i=0; i<outIndex.length; i++){
				/*重みweightについて修正*/
				for(int j=0; j<middleLayer.length; j++){
					if(middleLayer[j] != 0){
						weight[j][outIndex[i]] += 
							c*(target-outputResult)*middleLayer[j];
					}
				}
				/*各出力素子のしきい値について修正*/
				h[outIndex[i]] += -c*(target-outputResult);
			}
		}
	}
	/*修正すべき無発火の出力素子の要素を調べるメソッド*/
	protected int[] findLowerIndex(final double out[],int count){
		int index[] = new int [out.length];
		//out[]の値のコピーを取る
		double[] outtmp = new double [out.length];
		for(int i=0; i<out.length; i++)
			outtmp[i] = out[i];
		//発火していない素子の要素を調べる
		int revcount=0;
		for(int i=0; i<out.length; i++){		
			if(out[i] <= 0){
				index[revcount] = i;
				revcount++;
			}
		}
		//修正できる素子の数がcountを超えない場合を考えて
		if(revcount < count)
			count = revcount;
		//修正を行う素子の要素を格納する
		int tmp[] = new int [count];
		//発火しそうな素子が発火するように修正する
		for(int i=0; i<count; i++){
			double max = -100000.0;//Double.MIN_VALUE;
			int maxindex = 0;
			for(int j=0; j<count; j++){
				if(max < outtmp[index[j]]){
					maxindex = index[j];
					max = outtmp[index[j]];
				}
			}
			tmp[i] = maxindex;
			outtmp[maxindex] = -100000.0;//Double.MIN_VALUE;
		}
		return tmp;
	}
	/*修正すべき発火している出力素子の要素を調べるメソッド(0より大きい)*/
	protected int[] findHigherIndex(final double out[],int count){
		int index[] = new int [out.length];
		//out[]のコピーをouttmpにとる
		double[] outtmp = new double [out.length];
		for(int i=0; i<out.length; i++)
			outtmp[i] = out[i];
		//発火している素子に対応する要素を調べる
		int revcount=0;
		for(int i=0; i<out.length; i++){		
			if(out[i] > 0){
				index[revcount] = i;
				revcount++;
			}
		}
		//修正できる素子の数がcountを超えない場合を考えて
		if(revcount < count)
			count = revcount;
		//修正を行う素子の要素を格納する
		int tmp[] = new int [count];
		//発火の弱い素子を発火しないように修正する
		for(int i=0; i<count; i++){
			double min = Double.MAX_VALUE;
			int minindex = 0;
			for(int j=0; j<count; j++){
				if(min > outtmp[index[j]]){
					minindex = index[j];
					min = outtmp[index[j]];
				}
			}
			tmp[i] = minindex;
			outtmp[minindex] = Double.MAX_VALUE;
		}
		return tmp;
	}
	/*目標値設定用のメソッド*/
	public void setTarget(double t){
		target = t;
	}
	//SDNNの出力値を返す
	public double out(){
		return outputResult;
	}
	//出力素子の数を設定する
	public void setSumout(int n){
		SUMOUT = n;
	}

	/*テスト出力用メソッド*/
	public void test_dump(){
		/*入力層の初期化に失敗した時は終了*/
		if(inputLayer == null){
			System.out.println("Error:inputLayer is not initialized");
			return;
		}
		
		/*入力層の各素子群の値を出力*/
		System.out.println("入力層出力            シャフルした結果");
		for(int i=0; i<inputLayer.length; i++){
			System.out.println((i+1)+"層目:");
			for(int j=0; j<inputLayer[i].getLength(); j++){
				System.out.println("    "+(j+1)+"番目:");
				System.out.print("        ");
				for(int k=0;k<inputLayer[i].getCodePattern(j).getLength(); k++){
					System.out.print(" "+inputLayer[i].getCodePattern(j).getCode(k));
				}
				System.out.print("    ");
				for(int k=0;k<shuffledLayer[i].getCodePattern(j).getLength(); k++){
					System.out.print(" "+shuffledLayer[i].getCodePattern(j).getCode(k));
				}
				System.out.println("");
			}
		}
		
		/*中間層を出力*/
		System.out.println("中間層を出力");
		for(int c:middleLayer){
			System.out.print(c+" ");
		}
		System.out.println("");
		/*最終的な出力値と目標値*/
		System.out.println("出力値  目標値");
		System.out.println(outputResult+" "+target);
		/*重みとしきい値を表示*/
		System.out.println("重み");
		for(int i=0; i<outputLayer.length; i++){
			for(int j=0; j<middleLayer.length; j++){
				System.out.print(weight[i*outputLayer.length+j]+" ");
			}
			System.out.println("");
		}
		System.out.println("しきい値");
		for(int i=0; i<h.length; i++){
			System.out.print(h[i]+" ");
		}
		System.out.println("");
	}
	
	public void parameterFileWriter(String id) throws IOException{
		if(!errorCheck()){
			System.out.println("データが不明です");
		}
		//SDNNのパラメータ、状態をデータとして保存するファイルを作成
		File parameterFile = new File(id+".txt");
		FileWriter fw = new FileWriter(parameterFile);
		BufferedWriter bw = new BufferedWriter(fw);
		String str = "";	//ファイル書き込み用変数
		//入力層のデータを書き込む
		for(int i=0; i<inputLayer.length; i++){
			for(int j=0; j<inputLayer[i].getLength(); j++){
				str = "";
				str += ""+inputLayer[i].getCodePattern(j).getCode(0);
				for(int k=1;k<CODE; k++){
					str += ","+inputLayer[i].getCodePattern(j).getCode(k);
				}
				bw.write(str);
				bw.newLine();
			}
		}
		System.out.println("finished inputLayer data");
		//シャフル後の入力層のデータを書き込む
		for(int i=0; i<shuffledLayer.length; i++){
			for(int j=0; j<shuffledLayer[i].getLength(); j++){
				str = "";
				str += ""+shuffledLayer[i].getCodePattern(j).getCode(0);
				for(int k=1;k<CODE; k++){
					str += ","+shuffledLayer[i].getCodePattern(j).getCode(k);
				}
				bw.write(str);
				bw.newLine();
			}
		}
		System.out.println("finished shuffledLayer data");
		//重みのデータを書き込む
		for(int i=0; i<middleLayer.length; i++){
			str = "";
			str += ""+weight[i][0];
			for(int j=1; j<outputLayer.length; j++){
				str += ","+weight[i][j];
			}
			bw.write(str);
			bw.newLine();
		}
		System.out.println("finished weight data");
		//しきい値のデータを書き込む
		str = "";
		str += ""+h[0];
		for(int i=1; i<h.length; i++){
			str += ","+h[i];
		}
		bw.write(str);
		bw.newLine();
		bw.flush();
		System.out.println("finished amount data");
		System.out.println("output file,complete!");
		bw.close();
		fw.close();
	}
	
	public void parameterFileReader(String id) throws IOException{
		try{
			BufferedReader br = new BufferedReader(new FileReader(id+".txt"));
			//入力層のデータを読み込む
			for(int i=0; i<inputLayer.length; i++){
				for(int j=0; j<inputLayer[i].getLength(); j++){
					String[] str = br.readLine().split(",");
					inputLayer[i].setCodePattern(j,str);
				}
			}
			//シャフル層のデータを読み込む
			for(int i=0; i<shuffledLayer.length; i++){
				for(int j=0; j<shuffledLayer[i].getLength(); j++){
					String[] str = br.readLine().split(",");
					shuffledLayer[i].setCodePattern(j,str);
				}
			}
			//重みのデータを読み込む
			for(int i=0; i<middleLayer.length; i++){
				String str[] = br.readLine().split(",");
				for(int j=0; j<h.length; j++){
					double w = Double.parseDouble(str[j]);
					weight[i][j] = (float)w;
				}
			}
			//しきい値データを読み込む
			String str[] = br.readLine().split(",");
			for(int i=0; i<h.length; i++){
				double n = Double.parseDouble(str[i]);
				h[i] = n;
			}
			br.close();
		}catch(IOException e){
			System.out.println(e);
		}
	}
	protected boolean errorCheck(){
		if(inputLayer == null||middleLayer==null||outputLayer==null||
				shuffledLayer==null){
			return false;
		}
		return true;
	}
}