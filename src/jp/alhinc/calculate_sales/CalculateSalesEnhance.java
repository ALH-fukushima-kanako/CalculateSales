package jp.alhinc.CalculateSalesEnhance;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSalesEnhance {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";
	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// 商品定義ファイル名
	private static final String FILE_NAME_COMMODITY_LST = "commodity.lst";
	// 商品別集計ファイル名
	private static final String FILE_NAME_COMMODITY_OUT = "commodity.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "定義ファイルのフォーマットが不正です";
	private static final String FILE_SERIAL_NUMBER_ERROR = "売上ファイル名が連番になっていません";
	private static final String OVER_DIGIT_ERROR = "合計金額が10桁を超えました";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {

		//エラー処理3-1
		//コマンドライン引数チェック
		if(args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}

		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();

		// 支店定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales, "^[0-9]{3}$", "支店")) {
			return;
		}

		// 追加課題
		// 商品コードと商品名を保持するMap
		Map<String, String> commodityNames = new HashMap<>();
		// 商品コードと売上金額を保持するMap
		Map<String, Long> commoditySales = new HashMap<>();

		// 商品定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_COMMODITY_LST, commodityNames, commoditySales, "^[a-zA-Z0-9]{8}$", "商品")) {
			return;
		}

		// 処理内容2-1、2-2
		// 売上ファイル読み込み集計処理
		if(!readSalesFile(args[0], branchNames, branchSales, commodityNames, commoditySales)){
			return;
		}

		// 支店別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}

		// 追加課題
		// 商品別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_COMMODITY_OUT, commodityNames, commoditySales)) {
			return;
		}


	}

	/**
	 * 売上ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param 支店コードと売上金額を保持するMap
	 * @param 商品コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readSalesFile(String path, Map<String, String> branchNames, Map<String, Long> branchSales, Map<String, String> commodityNames, Map<String, Long> commoditySales) {

		File[] files = new File(path).listFiles();

		// 使用する売上ファイルリスト
		List<File> rcdFiles = new ArrayList<>();
		for(int i = 0; i < files.length ; i++) {
			//エラー処理3-1
			// 対象がファイルであるかとファイル名チェック「数字8桁.rcd」
			if(files[i].isFile() && files[i].getName().matches("^[0-9]{8}.rcd$")) {
				rcdFiles.add(files[i]);
			}
			// 上記に該当しないファイルはスキップ
			//else {
			//	System.out.println(UNKNOWN_ERROR);
			//	return false;
			//}
		}

		//エラー処理2-1
		// ファイル名を昇順に並び替え
		Collections.sort(rcdFiles);
		//ファイル名の連番チェック
		for(int i = 0; i < rcdFiles.size() - 1; i++) {

			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));
			int latter = Integer.parseInt(rcdFiles.get(i+1).getName().substring(0, 8));

		    //比較する2つのファイル名の先頭から数字の8文字を切り出しint型に変換
			if((latter - former) != 1) {
				//2つのファイル名の数字を比較して、差が1ではなかったら
				//エラーメッセージをコンソールに表示
				System.out.println(FILE_SERIAL_NUMBER_ERROR);
				return false;
			}
		}

		// 処理内容2-2
		BufferedReader br = null;
		FileReader fr = null;
		try {

			for(int i = 0; i < rcdFiles.size(); i++) {
				//エラー処理2-4【手本と差異有り】
				//売上ファイルのフォーマットチェック
				Path filepath = Paths.get(rcdFiles.get(i).getPath());
				if(3 != Files.lines(filepath).count()) {
					System.out.println(rcdFiles.get(i).getName() + "のフォーマットが不正です");
					return false;
				}
				// 売上ファイル読み込み
				File file = new File(rcdFiles.get(i).getPath());
				fr = new FileReader(file);
				br = new BufferedReader(fr);

				String line;
				int cnt = 0;
				String branchcode = null;
				String commodityCode = null;

				// 一行ずつ読み込む
				while((line = br.readLine()) != null) {
					// 処理内容1-2
					// 読み込んだ行数のカウント
					cnt++;
					// 1行目(支店コード)
					if(cnt==1) {
						// 支店コード保持
						branchcode = line;
						//エラー処理2-3
						//支店コード存在チェック
						if(!branchNames.containsKey(branchcode)) {
							System.out.println(file.getName() + "の支店コードが不正です");
							return false;
						}
					// 2行目(商品コード)
					}else if(cnt==2){
						// 商品コード保持
						commodityCode = line;
						//商品コード存在チェック
						if(!commodityNames.containsKey(commodityCode)) {
							System.out.println(file.getName() + "の商品コードが不正です");
							return false;
						}
					// 3行目以降(金額)
					}else {
						// 該当する支店コード/商品コードの売上金額を取得
						Long brancAmount = branchSales.get(branchcode);
						Long commodityAmont = commoditySales.get(commodityCode);

						//エラー処理3-3
						//売上金額が数字であるかチェック
						if(!line.matches("^[0-9]+$")) {
							System.out.println(UNKNOWN_ERROR);
							return false;
						}
						// ファイルから取得した売上金額を加算
						brancAmount += Long.parseLong(line);
						commodityAmont += Long.parseLong(line);

						//エラー処理2-2
						//売上金額の10桁以内チェック
						if(brancAmount >= 10000000000L || commodityAmont >= 10000000000L) {
							System.out.println(OVER_DIGIT_ERROR);
							return false;
						}
						// 支店コードと売上金額を保持
						branchSales.put(branchcode, brancAmount);
						System.out.println("支店別 売上ファイル:" + branchcode + ", \\" + line);

						// 商品コードと売上金額を保持
						commoditySales.put(commodityCode, commodityAmont);
						System.out.println("商品別 売上ファイル:" + commodityCode + ", \\" + line);

						// 3行分読み込んだら終了
						break;
					}
				}
			}

		}catch(IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return false;
		} finally {
			// ファイルを開いている場合
			if(br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
			if(fr != null) {
				try {
					// ファイルを閉じる
					fr.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}

		}

		return true;

	}

	/**
	 * 支店・商品定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店/商品コードと支店名/商品名を保持するMap
	 * @param 支店コード/商品コードと売上金額を保持するMap
	 * @param コードのチェック用正規表現
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> Names, Map<String, Long> Sales, String RegEx, String type) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);

			FileReader fr = new FileReader(file);

			//エラー処理1
			//ファイル存在値チェック
			if(!file.exists()) {
				System.out.println(type + FILE_NOT_EXIST);
				return false;
			}

			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while((line = br.readLine()) != null) {
				// 処理内容1-2
				System.out.println("読込データ:" + line);

				// コードと名称を抽出し格納
				String[] items = line.split(",");
				//エラー処理1
				//フォーマットチェック
				if((items.length !=2 ) || (!items[0].matches(RegEx))){
					System.out.println(type + FILE_INVALID_FORMAT);
					return false;
				}
				Names.put(items[0], items[1]);
				// 前日の売上金額を繰り越さないため0円とする
				Sales.put(items[0], 0L);
			}

		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if(br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 支店別/商品別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param コードと名称を保持するMap
	 * @param コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> Names, Map<String, Long> Sales) {
		// 処理内容3-1
		FileWriter fw =null;
		BufferedWriter bw = null;

		try {
			fw = new FileWriter(path + "\\" + fileName);
			bw = new BufferedWriter(fw);

			for (String key:Names.keySet()) {
				// コード,名称,売上金額の形式で書き込み
				bw.write(key + "," + Names.get(key) + "," + Sales.get(key));
				bw.newLine();
			}

		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if(bw != null) {
				try {
					// ファイルを閉じる
					bw.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

}
