package com.example.jack.beepay;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jack on 2017/11/26.
 */

// 資料功能類別
public class ItemDAO {
    // 表格名稱
    public static final String TABLE_NAME = "item";

    // 編號表格欄位名稱，固定不變
    public static final String KEY_ID = "_id";

    // 其它表格欄位名稱
    public static final String ACCOUNTID_COLUMN = "account_id";
    public static final String ACCOUNTNAME_COLUMN = "acountname";
    public static final String ACCOUNTEMAIL_COLUMN = "acountemail";
    public static final String PUB1_COLUMN = "pub1";
    public static final String PUB2_COLUMN = "pub2";
    public static final String Priv1_COLUMN = "priv1";
    public static final String Priv2_COLUMN = "priv2";
    public static final String ACCOUNTMONEY_COLUMN = "accountmoney";
    public static final String ACCOUNTEXIST_COLUMN = "accountexist";


    // 使用上面宣告的變數建立表格的SQL指令
    public static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    ACCOUNTID_COLUMN + " INTEGER, " +
                    ACCOUNTNAME_COLUMN + " TEXT NOT NULL, " +
                    ACCOUNTEMAIL_COLUMN + " TEXT NOT NULL, " +
                    PUB1_COLUMN + " TEXT NOT NULL, " +
                    PUB2_COLUMN + " TEXT NOT NULL, " +
                    Priv1_COLUMN + " TEXT NOT NULL, " +
                    Priv2_COLUMN + " TEXT NOT NULL, " +
                    ACCOUNTMONEY_COLUMN + " INTEGER, " +
                    ACCOUNTEXIST_COLUMN + " INTEGER)";

    // 資料庫物件
    private SQLiteDatabase db;

    // 建構子，一般的應用都不需要修改
    public ItemDAO(Context context) {
        db = beepayDB.getDatabase(context);
    }

    // 關閉資料庫，一般的應用都不需要修改
    public void close() {
        db.close();
    }

    // 新增參數指定的物件
    public Item insert(Item item) {
        // 建立準備新增資料的ContentValues物件
        ContentValues cv = new ContentValues();

        // 加入ContentValues物件包裝的新增資料
        // 第一個參數是欄位名稱， 第二個參數是欄位的資料
        cv.put(ACCOUNTID_COLUMN, item.getId());
        cv.put(ACCOUNTNAME_COLUMN, item.getAcount_name());
        cv.put(ACCOUNTEMAIL_COLUMN, item.getAccount_email());
        cv.put(PUB1_COLUMN, item.getPub1());
        cv.put(PUB2_COLUMN, item.getPub2());
        cv.put(Priv1_COLUMN, item.getPriv1());
        cv.put(Priv2_COLUMN, item.getPriv2());
        cv.put(ACCOUNTMONEY_COLUMN, item.getAcount_money());
        cv.put(ACCOUNTEXIST_COLUMN, item.getAcount_exist());

        // 新增一筆資料並取得編號
        // 第一個參數是表格名稱
        // 第二個參數是沒有指定欄位值的預設值
        // 第三個參數是包裝新增資料的ContentValues物件
        long id = db.insert(TABLE_NAME, null, cv);

        // 設定編號
        item.setId(id);
        // 回傳結果
        return item;
    }

    // 修改參數指定的物件
    public boolean update(Item item) {
        // 建立準備修改資料的ContentValues物件
        ContentValues cv = new ContentValues();

        // 加入ContentValues物件包裝的修改資料
        // 第一個參數是欄位名稱， 第二個參數是欄位的資料
        cv.put(ACCOUNTID_COLUMN, item.getId());
        cv.put(ACCOUNTNAME_COLUMN, item.getAcount_name());
        cv.put(ACCOUNTEMAIL_COLUMN, item.getAccount_email());
        cv.put(PUB1_COLUMN, item.getPub1());
        cv.put(PUB2_COLUMN, item.getPub2());
        cv.put(Priv1_COLUMN, item.getPriv1());
        cv.put(Priv2_COLUMN, item.getPriv2());
        cv.put(ACCOUNTMONEY_COLUMN, item.getAcount_money());
        cv.put(ACCOUNTEXIST_COLUMN, item.getAcount_exist());

        // 設定修改資料的條件為編號
        // 格式為「欄位名稱＝資料」
        String where = KEY_ID + "=" + item.getId();

        // 執行修改資料並回傳修改的資料數量是否成功
        return db.update(TABLE_NAME, cv, where, null) > 0;
    }

    // 刪除參數指定編號的資料
    public boolean delete(int id){
        // 設定條件為編號，格式為「欄位名稱=資料」
        String where = KEY_ID + "=" + id;
        // 刪除指定編號資料並回傳刪除是否成功
        return db.delete(TABLE_NAME, where , null) > 0;
    }

    public List<Item> getAll() {
        List<Item> result = new ArrayList<>();
        Cursor cursor = db.query(
                TABLE_NAME, null, null, null, null, null, null, null);

        while (cursor.moveToNext()) {
            result.add(getRecord(cursor));
        }

        cursor.close();
        return result;
    }
    // 取得指定編號的資料物件
    public Item get(long id) {
        // 準備回傳結果用的物件
        Item item = null;
        // 使用編號為查詢條件
        String where = KEY_ID + "=" + id;
        // 執行查詢
        Cursor result = db.query(TABLE_NAME,null,where,null,null,null,null,null);

        // 如果有查詢結果
        if (result.moveToFirst()) {
            // 讀取包裝一筆資料的物件
            item = getRecord(result);
        }

        // 關閉Cursor物件
        result.close();
        // 回傳結果
        return item;
    }
    // 把Cursor目前的資料包裝為物件
    public Item getRecord(Cursor cursor) {
        // 準備回傳結果用的物件
        Item result = new Item();

        result.setId(cursor.getInt(0));
        result.setAcount_name(cursor.getString(1));
        result.setpub1(cursor.getString(2));
        result.setpub2(cursor.getString(3));
        result.setPriv1(cursor.getString(4));
        result.setPriv2(cursor.getString(5));
        result.setAcount_money(cursor.getInt(6));
        result.setAcount_exist(cursor.getInt(7));

        // 回傳結果
        return result;
    }




    // 取得資料數量
    public int getCount() {
        int result = 0;
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null);

        if (cursor.moveToNext()) {
            result = cursor.getInt(0);
        }

        return result;
    }
    public String getpub1() {
        String result=null;
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        int rows_num = cursor.getCount();    //取得資料表列數
        if (rows_num != 0) {
            cursor.moveToFirst();            //將指標移至第一筆資料
            for(int i=0; i<rows_num; i++) {
                result = cursor.getString(2);
                cursor.moveToNext();
            }
        }
        return result;
    }
    public int checkaccount(String account){
        int check=0;
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        int rows_num = cursor.getCount();
        if (rows_num != 0) {
            cursor.moveToFirst();            //將指標移至第一筆資料
            for(int i=0; i<rows_num; i++) {
                if (account.equals(cursor.getString(2))) {
                    check=1;
                }
                cursor.moveToNext();
            }
        }

        return check;
    }

    // 建立範例資料
    public void sample() {
//       Item item = new Item(1,"123","666","777","888","999",300,1);
//        Item item2 = new Item(1,"321","666","777","888","999",300,1);
//
//        insert(item);
//        insert(item2);

    }

}