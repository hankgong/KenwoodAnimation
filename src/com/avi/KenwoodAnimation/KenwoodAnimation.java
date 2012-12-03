package com.avi.KenwoodAnimation;

import android.app.Activity;
import android.app.Dialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class KenwoodAnimation extends Activity implements View.OnClickListener
{
    // imagebuttons
    private ImageButton _nexedgeImageButton;
    private ImageButton _portableImageButton;
    private ImageButton _mobileImageButton;
    private ImageButton _repeaterImageButton;
    private ImageButton _accessoryImageButton;
    ImageView _zoominImageView;
    ImageView _zoomoutImageView;

    private TextView _imagesetTextView;

    private ProgressBar _imageloadingProgBar;
    private LinearLayout _bottomScrollView;

    Product3DOnView _productView;
    Angle360View _angle360View;

    Set<String> _imageSets = new HashSet<String>();
    HashMap<String, ArrayList<String>> _fileListMapbyImageSet = new HashMap<String, ArrayList<String>>();
    PopupWindow _imagesetPopup;


    private int _productIndex = 0;
    private int _productName;

    //only one dialog instance should be enough
    Dialog _filmStripDialog;

    private SQLiteDatabase _db;

    //product group name
    String _prodgroupName;

    // tmp imagegroup hashmap used to check which image button is pressed
    private HashMap<Integer, String> _imageProdGroup = new HashMap<Integer, String>();

    private Globals _g;
    private Button _onlyButton;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);


        _g = Globals.getInstance(this);
        // get the database in product view
        _db = _g.getDB();

        // zoom in/out buttons
        _zoominImageView = (ImageView)findViewById(R.id.zoominImageBtn);
        _zoomoutImageView = (ImageView)findViewById(R.id.zoomoutImageBtn);

        _zoominImageView.setOnClickListener(this);
        _zoomoutImageView.setOnClickListener(this);



        // image loading progressive bar we don't need to show it at the
        // beginning
        _imageloadingProgBar = (ProgressBar) findViewById(R.id.Images3DLoadingProgressbar);
        _imageloadingProgBar.setVisibility(View.INVISIBLE);

        _angle360View = (Angle360View) findViewById(R.id.angle360View);

        _productView = (Product3DOnView) findViewById(R.id.Product3DView);
        _productView.setProgressBar(_imageloadingProgBar);
        // link these two view together
        _productView.setDegreeIndicator(_angle360View);
        _angle360View.setProductView(_productView);

        _onlyButton = (Button) findViewById(R.id.button);
        _onlyButton.setOnClickListener(this);

        //_filmStripDialog = new Dialog(this);

        //Get the parameters from home activity only used when called by home page
        //_prodgroupName = getIntent().getStringExtra("productgroup");

/*
        //Query db to get the image files name
        queryDB4ImagesetNFilelists(_prodgroupName);
*/



        //this is a tmp workaround to solve the different size and alignment for 2d images
        /*if(_imageSets.size() == 0) {
            _imageSets.add("NX200_300__Display_and_DTMF");
            _imageSets.add("NXR710_810");


            _fileListMapbyImageSet.put("NX200_300__Display_and_DTMF", new ArrayList<String>());
            _fileListMapbyImageSet.get("NX200_300__Display_and_DTMF").add("Kenwood NX-300 display and DTMF deg01.png");
            _fileListMapbyImageSet.get("NX200_300__Display_and_DTMF").add("Kenwood NX-300 display and DTMF deg18.png");


            _fileListMapbyImageSet.put("NXR710_810", new ArrayList<String>());
            _fileListMapbyImageSet.get("NXR710_810").add("Kenwood NXR-810 repeater deg01.png");
            _fileListMapbyImageSet.get("NXR710_810").add("Kenwood NXR-810 repeater deg10.png");
            _fileListMapbyImageSet.get("NXR710_810").add("Kenwood NXR-810 repeater deg19.png");
            _fileListMapbyImageSet.get("NXR710_810").add("Kenwood NXR-810 repeater deg28.png");
        }


        //if we do find something in the database, then..
        if (_imageSets.size()>0) {
            String tmpsetname = (String) _imageSets.toArray()[0];
            _productView.startLoadImages(tmpsetname, _fileListMapbyImageSet.get(tmpsetname));

        }*/
    }


    //query results should be 2 columns. col 0: product category, col 1: product group name map
    private void setupDialog(String filmstripTitle, String query) {

        _filmStripDialog = new Dialog(this);

        //Set dialog no title bar
        _filmStripDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        _filmStripDialog.setContentView(R.layout.filmstrip_dialog);

        ImageButton closeButton= (ImageButton)_filmStripDialog.findViewById(R.id.closeDialogImageButton);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _filmStripDialog.dismiss();
            }
        });

        ArrayList<String> category = new ArrayList<String>();
        HashMap<String, ArrayList<String> > prodgroups= new HashMap<String, ArrayList<String>>();

        //database query
        Cursor cur = _g.getDB().rawQuery(query, null);
        if(cur.getColumnCount() != 2)
            return;

        cur.moveToFirst();

        while(!cur.isAfterLast()){
            String c = cur.getString(0);
            String p = cur.getString(1);

            System.out.println("debug start: " + c + " " + p);

            if(!category.contains(c)) {
                category.add(c);
                prodgroups.put(c, new ArrayList<String>());
            }

            System.out.println("debugg end: " + prodgroups);
            prodgroups.get(c).add(p);
            cur.moveToNext();
        }

        //setup contents here
        TextView txt = (TextView)_filmStripDialog.findViewById(R.id.titleTextView);
        txt.setText(filmstripTitle);

        LinearLayout scrollLlayout = (LinearLayout) _filmStripDialog.findViewById(R.id.imageUpTextDownViewLayout);

        _imageProdGroup.clear();

        String tmpStr;

        for (String c : category) {
            LinearLayout catvertLLayout = new LinearLayout(this);
            catvertLLayout.setOrientation(LinearLayout.VERTICAL);
            TextView catTitle = new TextView(this);
            catTitle.setText(c);
            catTitle.setTextSize(20);
            catvertLLayout.addView(catTitle);

            LinearLayout prodgrouphoriLLayout = new LinearLayout(this);
            prodgrouphoriLLayout.setOrientation(LinearLayout.HORIZONTAL);

            //prodgrouphoriLLayout.setDividerDrawable(this.getResources().getDrawable(R.drawable.divider));
            //prodgrouphoriLLayout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);

            for (String s: prodgroups.get(c)) {
                tmpStr = s.replace("/", "_").replace("-", "_").toLowerCase();

                ImageUpTextDownView tmpIuTd = new ImageUpTextDownView(this);
                int imgRID = Globals.getDrawable(this, tmpStr);
                tmpIuTd.setImageResource(imgRID);
                tmpIuTd.setGravity(Gravity.BOTTOM);
                tmpIuTd.setTextViewText(s);
                tmpIuTd.setPadding(80, 0, 0, 0);

                _imageProdGroup.put(imgRID, s);

                prodgrouphoriLLayout.addView(tmpIuTd, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.MATCH_PARENT));


                //TextView dividerView = new TextView(this);
                //dividerView.setBackground(new ColorDrawable(Color.DKGRAY));

                //prodgrouphoriLLayout.addView(dividerView, new LayoutParams(1, LayoutParams.MATCH_PARENT));
                //prodgrouphoriLLayout.addView(dividerView);
            }

            catvertLLayout.addView(prodgrouphoriLLayout);
            scrollLlayout.addView(catvertLLayout);
        }

    }

    private void queryDB4ImagesetNFilelists(String prodgroupName) {
        _imageSets.clear();
        _fileListMapbyImageSet.clear();

        Cursor cursor = _db.rawQuery(String.format(_g.sqls().queryGetImageFilesByProdGroup, prodgroupName), null);

        cursor.moveToFirst();
        String tmpfilename, tmpimageset;

        while (!cursor.isAfterLast()) {
            tmpfilename = cursor.getString(0);
            tmpimageset = cursor.getString(1);

            if (!_imageSets.contains(tmpimageset)) {
                _imageSets.add(tmpimageset);
                _fileListMapbyImageSet.put(tmpimageset, new ArrayList<String>());
                //
            }
            _fileListMapbyImageSet.get(tmpimageset).add(tmpfilename);
            // retGList.add(cursor.getString(0));
            cursor.moveToNext();
        }
    }

    public void clickedImageupTextdown(View v) {
        String pg = _imageProdGroup.get(v.getTag());
        System.out.println("--" + pg + "--");

        queryDB4ImagesetNFilelists(pg);

        _filmStripDialog.dismiss();

        if(_imageSets.size() == 0) {
            _imageSets.add("NX200_300__Display_and_DTMF");
            _imageSets.add("NXR710_810");


            _fileListMapbyImageSet.put("NX200_300__Display_and_DTMF", new ArrayList<String>());
            _fileListMapbyImageSet.get("NX200_300__Display_and_DTMF").add("Kenwood NX-300 display and DTMF deg01.png");
            _fileListMapbyImageSet.get("NX200_300__Display_and_DTMF").add("Kenwood NX-300 display and DTMF deg18.png");


            _fileListMapbyImageSet.put("NXR710_810", new ArrayList<String>());
            _fileListMapbyImageSet.get("NXR710_810").add("Kenwood NXR-810 repeater deg01.png");
            _fileListMapbyImageSet.get("NXR710_810").add("Kenwood NXR-810 repeater deg10.png");
            _fileListMapbyImageSet.get("NXR710_810").add("Kenwood NXR-810 repeater deg19.png");
            _fileListMapbyImageSet.get("NXR710_810").add("Kenwood NXR-810 repeater deg28.png");
        }

        for (String s : _imageSets) {
            System.out.println(_fileListMapbyImageSet.get(s));
        }

        String tmpsetname = (String) _imageSets.toArray()[0];
        _productView.startLoadImages(tmpsetname, _fileListMapbyImageSet.get(tmpsetname));
        _prodgroupName = pg;

    }

    @Override
    public void onClick(View view) {
        if (view == _onlyButton){
            int searchid = _g.searchTags().get("NEXEDGE");
            setupDialog("Products", String.format(_g.sqls().queryGetFilmStripGroupBySearchID, searchid));
            _filmStripDialog.show();
        } else if (view == _zoominImageView) {
            _productView.zoomIn();
        } else if (view == _zoomoutImageView) {
            _productView.zoomOut();
        }
    }
}
