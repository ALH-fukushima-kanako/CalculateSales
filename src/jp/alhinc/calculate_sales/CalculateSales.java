package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "支店定義ファイルのフォーマットが不正です";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();

		// 支店定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales)) {
			return;
		}
		// 処理内容2-1、2-2
		// 売上ファイル読み込み集計処理
		if(!readSalesFile(args[0], branchSales)){
			return;
		}

		// 支店別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}

	}

	/**
	 * 売上ファイル読み込み処理
	 *
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readSalesFile(String path, Map<String, Long> branchSales) {

		File[] files = new File(path).listFiles();

		// 使用する売上ファイルリスト
		List<File> rcdFiles = new ArrayList<>();
		for(int i = 0; i < files.length ; i++) {
			// ファイル名チェック「数字8桁.rcd」判定
			if(files[i].getName().matches("^[0-9]{8}.rcd$")) {
				rcdFiles.add(files[i]);
			}
		}

		// 処理内容2-2
		BufferedReader br = null;
		try {

			for(int i = 0; i < rcdFiles.size(); i++) {
				// 売上ファイル読み込み
				File file = new File(rcdFiles.get(i).getPath());
				FileReader fr = new FileReader(file);
				br = new BufferedReader(fr);

				String line;
				int cnt = 0;
				String branchcode = null;

				// 一行ずつ読み込む
				while((line = br.readLine()) != null) {
					// 処理内容1-2
					// 読み込んだ行数のカウント
					cnt++;
					// 1行目
					if(cnt==1) {
						// 支店コード保持
						branchcode = line;
					// 2行目以降
					}else {
						// 該当する支店コードの売上金額を取得
						Long amount = branchSales.get(branchcode);
						// ファイルから取得した売上金額を加算
						amount += Long.parseLong(line);
						// 支店コードと売上金額を保持
						branchSales.put(branchcode, amount);
						System.out.println("支店別 売上ファイル:" + branchcode + ", \\" + line);
						// 2行分読み込んだら終了
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
		}

		return true;

	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);

			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while((line = br.readLine()) != null) {
				// 処理内容1-2
				System.out.println("支店ファイル:" + line);
				// 支店コードと支店名を抽出し格納
				String[] items = line.split(",");
				branchNames.put(items[0], items[1]);
				// 前日の売上金額を繰り越さないため0円とする
				branchSales.put(items[0], 0L);
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
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales) {
		// 処理内容3-1
		FileWriter fw =null;
		BufferedWriter bw = null;

		try {
			fw = new FileWriter(path + "\\" + fileName);
			bw = new BufferedWriter(fw);

			for (String key:branchNames.keySet()) {
				// 支店コード,支店名,売上金額の形式で書き込み
				bw.write(key + "," + branchNames.get(key) + "," + branchSales.get(key));
				bw.newLine();
			}

			bw.close();

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
