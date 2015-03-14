package sdnn;
public class CodePattern {
	
	private int code_pattern[] = null;  //1,-1,0の3値パターン
	private int length = 0;
	
	/*引数なしなら200次元のコードパターンを作成*/
	public CodePattern(int n){
		try{
			code_pattern = new int [n];
		}catch(NullPointerException e){
			System.out.println(e.getMessage());
		}
		length = n;
	}
	
	//コードパターンを別のコードパターンにコピーする          //int配列 ver
	public void copyCodePattern(final int[] cp){
		for(int i=0; i<code_pattern.length; i++){
			this.code_pattern[i] = cp[i];
		}
	}
	
	//コードパターンを別のコードパターンにコピーする          //CodePattern ver
	public void copyCodePattern(CodePattern cp){
		for(int i=0; i<code_pattern.length; i++){
			this.code_pattern[i] = cp.getCode(i);
		}
	}
	
	/*任意の場所に-1,1,0のいずれかの3値を代入する*/
	public void setCode(int n,int code){
		this.code_pattern[n] = code;
	}
	
	public void changeCode(int n){
		this.code_pattern[n] *= -1;
	}
	
	public int[] getCodePattern(){
		return code_pattern;
	}
	public void setCodePattern(CodePattern cp){
		for(int i=0; i<code_pattern.length; i++){
			code_pattern[i] = cp.getCode(i);
		}
	}
	public void setCodePattern(String str[]){
		for(int i=0; i<length; i++){
			this.code_pattern[i] = Integer.parseInt(str[i]);
		}
	}
	public int getCode(int n){
		return code_pattern[n];
	}
	
	public int getLength(){
		return length;
	}
}

