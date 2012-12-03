package com.avi.KenwoodAnimation;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.Window;
import android.view.WindowManager;
import junit.framework.Assert;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: hankgong
 * Date: 01/12/12
 * Time: 1:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class Globals {
    private static Globals ourInstance = null;
    private final String DB_NAME = "kenwood.db";
    private Context _context;
    private SQLiteDatabase _db = null;
    private HashMap<String, Integer> _searchtags = new HashMap<String, Integer>();
    private SQLs _sql = new SQLs();

    private Globals(Context c) {
        _context = c;

        try {
            //justify if the database exists
            DBUtils.createDatabaseIfNotExists(_context, DB_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }

        _db = SQLiteDatabase.openDatabase(c.getDatabasePath(DB_NAME).getPath(), null, SQLiteDatabase.OPEN_READONLY);

        //do a query to get all search tags to save frequent query
        String sql = "SELECT _id, name FROM search_tag";
        Cursor cursor = _db.rawQuery(sql, null);
        cursor.moveToFirst();

        while(!cursor.isAfterLast()) {
            _searchtags.put(cursor.getString(1), cursor.getInt(0));
            cursor.moveToNext();
        }

        System.out.println("density" + c.getResources().getDisplayMetrics().density);

    }

    public static synchronized Globals getInstance(Context c) {
        if(ourInstance == null)
            ourInstance = new Globals(c);
        return ourInstance;
    }

    /**
     * Function used to set activity as landscape and full screen
     * @param a: current activity
     */
    public static void setLandscapeFullScreen(Activity a){
        //no window title and set as full screen
        a.requestWindowFeature(Window.FEATURE_NO_TITLE);
        a.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //activity direction set as landscape instead of portrait
        a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    public static int getDrawable(Context context, String name){
        Assert.assertNotNull(context);
        Assert.assertNotNull(name);

        return context.getResources().getIdentifier(name,
                "drawable", context.getPackageName());
    }

    public HashMap <String, Integer> searchTags() { return _searchtags;}

    public SQLs sqls() {return _sql;}

    public SQLiteDatabase getDB() {return _db;}

    public static class SQLs{
        public String product_features = "select s._id SRCHID, s.name FEATURE, count(plk.unit) UNIT_COUNT " +
                "from search_tag s, tag_link l, product_group pg, productgrp_link plk, price pr " +
                "where s.type = 'F' " +
                "and s.visible <> 0 " +
                "and s._id = l.searchtag_id " +
                "and l.productgrp_name = pg.name " +
                "and pg.visible <> 0 " +
                "and pg.name = plk.product_group " +
                "and plk.unit = pr.unit " +
                "and pr.visible <> 0 " +
                "group by s._id, s.name " +
                "having UNIT_COUNT > 0 " +
                "order by s.name";
        public String product_categories = "select s._id SRCHID, s.name FEATURE, count(plk.unit) UNIT_COUNT " +
                "from search_tag s, tag_link l, product_group pg, productgrp_link plk, price pr " +
                "where s.type = 'C'	" +
                "and s.visible <> 0 " +
                "and s._id = l.searchtag_id " +
                "and l.productgrp_name = pg.name " +
                "and pg.visible <> 0 " +
                "and pg.name = plk.product_group " +
                "and plk.unit = pr.unit " +
                "and pr.visible <> 0 " +
                "group by s._id, s.name " +
                "having UNIT_COUNT > 0 " +
                "order by s.name;";
        public String industry_sector = "select s._id SRCHID, s.name FEATURE, count(plk.unit) UNIT_COUNT " +
                "from search_tag s, tag_link l, product_group pg, productgrp_link plk, price pr " +
                "where s.type = 'I' " +
                "  and s.visible <> 0 " +
                "  and s._id = l.searchtag_id " +
                "  and l.productgrp_name = pg.name " +
                "  and pg.visible <> 0 " +
                "  and pg.name = plk.product_group " +
                "  and plk.unit = pr.unit " +
                "  and pr.visible <> 0 " +
                "group by s._id, s.name " +
                "having UNIT_COUNT > 0 " +
                "order by s.name;";
        public String queryGetGroupBySearchID = "select tl.productgrp_name PGROUP " +
                "from search_tag s, product_group g, product_type t, tag_link tl " +
                "where tl.searchtag_id = %d " +
                "and tl.searchtag_id = s._id " +
                "and tl.productgrp_name = g.name " +
                "and g.visible <> 0 " +
                "and s.visible <> 0 " +
                "group by tl.productgrp_name " +
                "order by s.sequ,s.name;";
        public String queryGetFilmStripGroupBySearchID = "select s.name, tl.productgrp_name PGROUP " +
                "from search_tag s, product_group g, product_type t, tag_link tl " +
                "where tl.searchtag_id = %d " +
                "and tl.searchtag_id = s._id " +
                "and tl.productgrp_name = g.name " +
                "and g.visible <> 0 " +
                "and s.visible <> 0 " +
                "group by tl.productgrp_name " +
                "order by s.sequ,s.name;";
        public String queryFilmStripText = "select s.name, tl.productgrp_name " +
                "from search_tag s, product_group g, product_type t, tag_link tl  " +
                "where s.type = 'C' " +
                "and tl.searchtag_id = s._id " +
                "and tl.productgrp_name = g.name " +
                "group by tl.productgrp_name " +
                "order by s.sequ,s.name;";
        public String queryGetImageFilesByProdGroup = "SELECT ifiles.filename FILENAME, ifiles.imageset IMAGESET " +
                "FROM imagesets isets, imagefiles ifiles " +
                "WHERE isets.product_group='%s' " +
                "AND ifiles.imageset = isets.name " +
                "ORDER BY isets._id";
        public String queryGetFeaturesGroupForProduct = "select detail_subgrp.description, details.description\n" +
                "from detail_group, detail_subgrp, details\n" +
                "where detail_group.name = '%s'\n" +
                "and     detail_group._id = detail_subgrp.detail_group_id\n" +
                "and     details.detail_subgrp = detail_subgrp.subgrp\n" +
                "and     details.product = '%s'";
    }
}
