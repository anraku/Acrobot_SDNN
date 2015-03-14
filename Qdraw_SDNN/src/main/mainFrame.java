package main;

import java.io.File;
import java.io.IOException;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Font;
import java.awt.Image.*;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import sdnn.*;

public class mainFrame{
	
	public static void main(String args[]){
		String filename = "sdnn";
		if(args.length > 0)
			filename = args[0];
		mainFrame mainapp = new mainFrame(filename);
	}
	
	public mainFrame(String filename){
		int num = 360;		//入力層の素子の数
		double res[] = {1.0,1.0,1.0,1.0};		//各入力層の分解能
		//右のSDNNをQ値の視覚化に使う
		SDNN sdnn = new SDNN(num,res);
		//test
		try{
			sdnn.parameterFileReader(filename);
		}catch(IOException e){
			System.out.println(e);
			return;
		}
		File QValue = new File(filename+".png");  //画像を保存するためのファイル
		BufferedImage QValueImage = ImageInit(800,700);
		ImageInit(70, 70, QValueImage);
		
		//画像を作成する
		try{
			save(QValueImage,QValue);
		}catch(IOException e){
			System.out.println(e);
		}
		
		//SDNNの各状態変数での行動価値をQValueに出力
	    int[] color = new int [3];
		//ω1 = -90, 0, 90
		for(int i=0; i<=2; i++){
			//ω2 = -90, 0, 90
			for(int j=0;j<=2; j++){
				//0 <= θ1 <= 180
				for(int m=0; m<180; m++){
					//0 <= θ2 <= 180
					for(int n=0; n<180; n++){
						sdnn.input(179-m, 120+i*60, 179-n, 144+j*36);
						double q = (sdnn.out()-300)/30;
						color = judgeColor(q);
						int a = color[0] << 16 | color[1] << 8 | color[2] ;
						int x = 70+3+(5+180)*j+n;
						int y = 70+3+(5+180)*i+m;
						QValueImage.setRGB(x, y, a);
						//System.out.println("θ1: "+m+" θ2: "+n+" ω1: "+j+" ω2: "+i);
					}
				}
				//画像を作成する
				try{
					save(QValueImage,QValue);
				}catch(IOException e){
					System.out.println(e);
				}
				System.out.println(i+":"+j+" complete");
			}
		}
	}
	public BufferedImage ImageInit(final int width, final int height){
		return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	}
	public void save(BufferedImage img, File f) throws IOException{
		if(!ImageIO.write(img, "png", f)){
			throw new IOException("fomat error");
		}
	}
	public void ImageInit(int x, int y, BufferedImage img){
		int h = img.getHeight(null);
		int w = img.getWidth(null);
		Graphics g = img.getGraphics();
		//全体を白にする
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, w, h);
		
		//グラフの額を作成する
		//黒い線を引く
		g.setColor(Color.BLACK);
		g.drawRect(x, y, 560, 560);
		//縦に2本
		g.drawLine(x+185, y, x+185, y+560);
		g.drawLine(x+370, y, x+370, y+560);
		//横に2本
		g.drawLine(x, y+185, x+560, y+185);
		g.drawLine(x, y+370, x+560, y+370);
		//色とQ値の対応表を作成する
		colorTableInit(x+650, y+360, g);
		//画像に文字を書く
		writeWord(x, y, g);
		g.dispose();
	}
	//右下当たりにQ値毎の色分けの対応表を作成する
	public void colorTableInit(int x, int y, Graphics g){//x=720,y=430
		int[][] array = {
				{255, 0, 0},				//赤色
				{255, 127, 39},		//オレンジ色
				{255, 255, 0},			//黄色
				{153, 255, 51},		//薄い緑色
				{0, 255, 0},				//緑色
				{0, 219, 55},			//深い緑色
				{0, 255, 255},			//シアン
				{0, 0, 255},				//青色
				{0, 0, 160},				//深い青
		};
		for(int i=0; i<array.length; i++){
			Color c = new Color(array[i][0]<<16 | array[i][1]<<8 | array[i][2]);
			g.setColor(c);
			g.fillRect(x, y+(i*20), 40, 20);
		}
		g.setColor(Color.BLACK);
		//枠線
		g.drawRect(x, y, 40, 180);
		//対応する数値を描画
		Font f = new Font("MS　明朝",  Font.PLAIN, 24);
		g.setFont(f);
		g.drawString("Q値", x, y-10);
		g.drawString(" 4", x+40, y-10);
		g.drawString("-4", x+40, y+190);
	}
	//画像に必要な文字を書く
	public void writeWord(int x, int y, Graphics g){
		Font f = new Font("MS　明朝",  Font.PLAIN, 24);
		g.setFont(f);
		g.setColor(Color.BLACK);
		//[deg]の記述
		g.drawString("[deg]", x-60, y-30);
		//角度θ2について
		g.drawString("θ2", x+90, y-40);
		g.drawString("0", x, y-10);
		g.drawString("90", x+90, y-10);
		g.drawString("180", x+180, y-10);
		//角度θ1について
		g.drawString("θ1", x-70, y+100);
		g.drawString("  0", x-40, y+10);
		g.drawString(" 90", x-40, y+100);
		g.drawString("180", x-40, y+190);
		//ω2について
		g.drawString("ω2 [deg/s]", x+270, y+620);
		g.drawString("-180", x+70, y+590);
		g.drawString("0", x+270, y+590);
		g.drawString("180", x+450, y+590);
		//ω1について
		g.drawString("ω1 [deg/s]", x+600, y+270);
		g.drawString("-180", x+570, y+90);
		g.drawString("0", x+570, y+270);
		g.drawString("180", x+570, y+460);
	}
	//Q値に応じてRGB値を返す
	public int[] judgeColor(double n){
		if(n>4.0)
			return new int[] {255, 0, 0};		//赤色
		if(n>3.0)
			return new int[] {255, 127, 39};	//オレンジ色
		if(n>2.0)
			return new int[] {255, 255, 0};	//黄色
		if(n>1.0)
			return new int[] {153, 255, 51};	//薄い緑
		if(n>0.0)
			return new int[] {0, 255, 0};		//緑色
		if(n>-1.0)
			return new int[] {0, 219, 55};		//濃い緑色
		if(n>-2.0)
			return new int[] {0, 255, 255};	//シアン
		if(n>-3.0)
			return new int[] {0, 0, 255};		//青色
		if(n<=-3.0)
			return new int[] {0, 0, 160};		//深い青色
		return new int[] {0, 0, 0};				//ここは通らない
	}
}
