/**
 * This class is derived from View class, which may be too slow. This customized view
 * should have:
 * -image smooth 3d switching
 * -smooth zooming 
 * -image loading indicator
 * -multithread support
 * -360 angel indicator
 */
package com.avi.KenwoodAnimation;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ProgressBar;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * product imageset view class
 *
 * @author hgong
 */

public class Product3DOnView extends View {

	// main variables about product itself, but not used till now
//	private int _productIndex;
//	private String _productName;

	private String _imagesetName;   //name of image set (note that, one product may have multiple image sets)
	private ArrayList<String> _imagesetFilenameList;    //file names only, need imagesetName together to read file

	private int     _sampleSize;    //samplesize >=1 integer, 2 means down sample with half information
	private float _scaleImageReading;   //why do we need this? Todo: should we set it as 1.0f?

    //database is in _g
	private Globals _g;
	//private SQLiteDatabase _db;

    //parentWidthxheight is much bigger than _bmpWidthxheight,
    //parentWidthxheight is slightly bigger than canvasWidthxheight
	// parent width and height
	int _parentWidth;
	int _parentHeight;
    //bitmap width X height
    int _bmpWidth;
    int _bmpHeight;

	// variables about max number of images, image index and the bitmap matrix
	// number of images required for 3D view of each product
	final static int MaxNumImg = 36;
	// number of images for current image set
	int _numImgs = 1;

	// image storage matrix and indicator matrix
	Bitmap[] _imagesetBitmaps = new Bitmap[MaxNumImg];
	boolean[] _ifLoadedImages = new boolean[MaxNumImg];

	//Since the _curShowingImage is controlled by degree indicator, the initial value doesn't matter at all
	public int _curShowingImage = 0;
    //used to decide refreshing canvas and callout queries
	int _lastShowedImage = -1;

	// stored X/Y for temporary use
	float _lastX = 0;
	float _lastY = 0;

	// variable for blocking response to mouse moving
	boolean _ifTouching = false;

    //used by angle indicator, Todo: should be changed to method
	public boolean _ifAngleIndTouching = false;

	//Four boolean variables planned for animation control
	boolean _ifDrawingCalloutExtAnimation = false;
	boolean _ifDrawingCalloutDotAnimation = false;
	boolean _ifDrawingCalloutZoomIn = false;
	boolean _ifDrawingRemovingBattery = false;

	// the progressbar and angle indicator from parent component
	public ProgressBar _progressBar;
	public Angle360View _angle360View;

	// for scaling, no benefit to get higher scale ratio, so maximum is 1.0f
	public float _scaleFactor = 1.0f;
	private float _scaleFactorMax = 1.0f;
    //Todo: we may need to smartly decide min factor
	private float _scaleFactorMin = 0.8f;

    //everything about callout operation
	private Timer _calloutQueryTimer = new Timer();
    float _calloutRadius = 10.0f;

	private DBQueryTimerTask _calloutQueryTimerTask = null;
	private ArrayList<Pair<Point, String>> _callouts = new ArrayList<Pair<Point, String>>();
    private int _chosenCalloutIndex = -1;

    //zooming indicator & zooming bitmap loaded separately
    private boolean _ifCalloutZoom = false;
	private Bitmap _zoomInBitpmap;

	// painter used for drawing...
	Paint _paint = new Paint();

    //I'm going to use gradient drawable for the background of callout text
    GradientDrawable _gradientDrawable = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, new int[] { 0xFF1A476C, 0xFF1A354C });

	/**
	 * Never used it
	 *
	 * @param context
	 */
	public Product3DOnView(Context context) {
		super(context);
	}

	/**
	 * No need if we don't care about style
	 *
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public Product3DOnView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

	}

	/**
	 * Main constructor function called by us
	 *
	 * @param context
	 * @param attrs
	 */
	public Product3DOnView(Context context, AttributeSet attrs) {
		super(context, attrs);

		if (!isInEditMode())
			_g = Globals.getInstance(getContext());

        _gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        _gradientDrawable.setGradientRadius(10.0f);

        // There is always a drawing thread running at background; the benefit of doing
        // this is we can update data anytime and notify the UI thread to draw it in OnDraw()
		new Thread(drawUpdateThread).start();
	}

	/**
	 * @author hankgong TimerTask used to to read database query
	 */
	private class DBQueryTimerTask extends TimerTask {
		private String _filename;
        private String _sqlquery;

		public DBQueryTimerTask(String filename) {
			_filename = filename;

            //sql query
            _sqlquery = String.format("SELECT x, y, note FROM imagecallouts where filename='%s'", _filename);
		}

		@Override
		public void run() {
			Cursor cursor = _g.getDB().rawQuery(_sqlquery, null);

			// start to read data
			cursor.moveToFirst();

			int x, y;
			String note;

			//Todo: I may need to imageset as a field
			// column 0, column 1: x and y
			// column 2: note
			while (!cursor.isAfterLast()) {
				x = cursor.getInt(0);
				y = cursor.getInt(1);
				note = cursor.getString(2);

				//There can be only one timertask running one time, so we don't need vector
				_callouts.add(new Pair<Point, String>(new Point(x, y), note));
				cursor.moveToNext();
			}
            cursor.close();

			// safe to inform the draw thread to redraw
			Log.i("callout", "callout query finished");
			postInvalidate();
		}
	}


/*	*//**
	 * Set the number of images
	 * Reset the progress bar maximum value
	 *
	 *//*
	public void resetView(int imageNum) {
		_numImgs = imageNum;
		_progressBar.setMax(_numImgs);

	}*/

	public void zoomIn() {
		_scaleFactor = Math.min(_scaleFactor+0.01f, _scaleFactorMax);
        postInvalidate();
	}

	public void zoomOut() {
		_scaleFactor = Math.max(_scaleFactor-0.01f, _scaleFactorMin);
        postInvalidate();
	}

	/*public void updateCanvas() {
		//Log.i("updateCanvas", "image index " + _curShowingImage + " scalefactor " + _scaleFactor);

		//Here I set calloutindex as -1 to intentionally hide callout
		//_chosenCalloutIndex = -1;
		postInvalidate();
	}*/

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View#onMeasure(int, int) Still not very clear how to
	 * use it now... I mainly used it to get the parent Width X Height for
	 * bitmap plot positioning.
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		Log.i("on measure", "measure once");
		_parentWidth = MeasureSpec.getSize(widthMeasureSpec);
		_parentHeight = MeasureSpec.getSize(heightMeasureSpec);

		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	/**
	 * draw thread; every 30ms run once
	 */
	private Runnable drawUpdateThread = new Runnable() {
		@Override
		public void run() {
			while (true) {
				try {
					// I only want the canvas to redraw if there is difference
					if (_lastShowedImage != _curShowingImage % _numImgs) {
						_callouts.clear();
						_chosenCalloutIndex = -1;
						postInvalidate();
						//cancel query task and remove it from the timer task queue
						if (_calloutQueryTimerTask != null) {
							_calloutQueryTimerTask.cancel();
							_calloutQueryTimerTask = null;
							_calloutQueryTimer.purge();
						}

						//imageset filenames should not be null
						if (_imagesetFilenameList != null) {
							_calloutQueryTimerTask = new DBQueryTimerTask(_imagesetFilenameList.get(_curShowingImage % _numImgs));
							_calloutQueryTimer.schedule(_calloutQueryTimerTask, 1000);
						}
						_lastShowedImage = _curShowingImage % _numImgs;
					}
					Thread.sleep(30);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};


	/**
	 * Main function exposed to others to call for loading image sets
	 *
	 * @param imageSet  : a ground of images showing different views of one type of product
	 * @param filenames : a filename list stored the product shot in order
	 */
	public void startLoadImages(String imageSet, ArrayList<String> filenames) {
		_imagesetName = imageSet;
		_imagesetFilenameList = filenames;

        _curShowingImage = 0;
		_chosenCalloutIndex = -1;

        _numImgs = filenames.size();
        _progressBar.setMax(_numImgs);

        //load image task, current still slow, think about how to optimize speed
		LoadImagesTask loadTask = new LoadImagesTask();
		loadTask.execute();
	}

    Rect tmpbounds = new Rect();

	/* (non-Javadoc)
	 * @see android.view.View#onDraw(android.graphics.Canvas)
	 * main drawing method
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

        //canvas.save|restore will discard translate, rotate operations, but drawing are still there

		canvas.save();

        //scale canvas and center it
		canvas.scale(_scaleFactor, _scaleFactor);
        canvas.translate(-canvas.getWidth() * (1 - 1.0f/_scaleFactor) / 2, -canvas.getHeight()
                * (1 - 1.0f/_scaleFactor) / 2);
/*

        System.out.println("factor| Canvas width X Canvs height: " + _scaleFactor + "|" + canvas.getWidth() + "X" + canvas.getHeight());
        System.out.println("parent width x height: " + _parentWidth + "X" + _parentHeight);
*/

		_paint.setColor(Color.RED);
        _paint.setStyle(Paint.Style.STROKE);


		if (_imagesetBitmaps[_curShowingImage % _numImgs] != null) {
			int startX = _parentWidth / 2 - _imagesetBitmaps[_curShowingImage % _numImgs].getWidth() / 2;
			int startY = _parentHeight / 2 - _imagesetBitmaps[_curShowingImage % _numImgs].getHeight() / 2;

			//draw bitmap
			canvas.drawBitmap(_imagesetBitmaps[_curShowingImage % _numImgs], startX, startY, null);
            //draw callout dots; no need to consider callout is loaded or not
            for (Pair<Point, String> p : _callouts) {
                canvas.drawCircle(startX + p.first.x * _scaleImageReading / _sampleSize,
                        startY + p.first.y * _scaleImageReading / _sampleSize, _calloutRadius, _paint);
            }

            //notify other widget
			updateOthers(_curShowingImage % _numImgs);
            /*canvas.drawRect(startX, startY, startX+_bmpWidth, startY+_bmpHeight, _paint);
            canvas.drawLine(_parentWidth/2, 0, _parentWidth/2, _parentHeight, _paint);
            canvas.drawLine(0, _parentHeight/2, _parentWidth, _parentHeight/2, _paint);*/
            canvas.drawRect(0, 0, _parentWidth, _parentHeight, _paint);

            //callout text
			_paint.setTextSize(15 / _scaleFactor);
			_paint.setAntiAlias(true);

            Log.i("callout", "Chosed callout index: " + _chosenCalloutIndex);

			if (_callouts.size() > 0 && _chosenCalloutIndex >= 0) {

				//_paint.setStyle(Paint.Style.STROKE);


				//callout text processing
				String text = _callouts.get(_chosenCalloutIndex).second.trim();
				String textLines[] = text.split("-");
                //Log.i("callout", textLines);
                System.out.println("callout " + textLines.length);

				//System.out.println("fulltext:" + text);
				//System.out.println("alllines:" + textLines);

				float maxWidth = 0;
				float maxHeight = 0;

				for (int i = 0; i < textLines.length; i++) {
					_paint.getTextBounds(textLines[i], 0, textLines[i].length(), tmpbounds);
					maxWidth = Math.max(maxWidth, tmpbounds.width());
                    maxHeight += tmpbounds.height();
					//maxHeight += -_paint.ascent() + _paint.descent();
				}

                Log.i("callout", "maxwidth, maxheight " + maxWidth + "X" + maxHeight);

                _gradientDrawable.setBounds((int)(_parentWidth - maxWidth - (_scaleFactor - 0.8) * _parentWidth / 2.0f),
                        (int)(10 + (_scaleFactor - 0.8) * _parentHeight / 2.0f + _paint.ascent() -_paint.descent()),
                        (int)(_parentWidth - maxWidth - (_scaleFactor - 0.8) * _parentWidth / 2.0f + maxWidth + 5),
                        (int)(10 + (_scaleFactor - 0.8) * _parentHeight / 2.0f + maxHeight));
                _gradientDrawable.setCornerRadii(new float[] {10, 10, 10, 10, 0, 0, 0, 0});
                _gradientDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
                _gradientDrawable.draw(canvas);

				_paint.setColor(Color.GRAY);
//				for (int i = 0; i < textLines.length; i++) {
//					canvas.drawText(textLines[i], (float) (_parentWidth - maxWidth - (_scaleFactor - 0.8) * _parentWidth / 2.0f),
//							(float) (10 + (-_paint.ascent() + _paint.descent() + 2 / _scaleFactor) * i + (_scaleFactor - 0.8) * _parentHeight / 2.0f), _paint);
//				}
//
                Log.i("callout", "factor: " + _scaleFactor + " font size:  " + 15/_scaleFactor + " extra offset:  " + (_parentWidth - maxWidth - (_scaleFactor-1)*_parentWidth/2.0f));

                for (int i = 0; i < textLines.length; i++) {
					canvas.drawText(textLines[i], (float) (_parentWidth - maxWidth - (1-1/_scaleFactor)*_parentWidth/2.0f),
							(float) (10 + (-_paint.ascent() + _paint.descent() + 2 / _scaleFactor) * i + (_scaleFactor - 0.8) * _parentHeight / 2.0f), _paint);
				}



				_paint.setColor(Color.BLUE);
				canvas.drawLine(startX + _callouts.get(_chosenCalloutIndex).first.x * _scaleImageReading / _sampleSize,
						startY + _callouts.get(_chosenCalloutIndex).first.y * _scaleImageReading / _sampleSize,
						(float) (_parentWidth - maxWidth - (_scaleFactor - 0.8) * _parentWidth / 2.0f),
						(float) (15 + (-_paint.ascent() + _paint.descent() + 2 / _scaleFactor) * (textLines.length - 1) + (_scaleFactor - 0.8) * _parentHeight / 2.0f), _paint);


				canvas.drawLine((float) (_parentWidth - maxWidth - (_scaleFactor - 0.8) * _parentWidth / 2.0f),
						(float) (15 + (-_paint.ascent() + _paint.descent() + 2 / _scaleFactor) * (textLines.length - 1) + (_scaleFactor - 0.8) * _parentHeight / 2.0f),
						(float) (_parentWidth - maxWidth - (_scaleFactor - 0.8) * _parentWidth / 2.0f) + maxWidth,
						(float) (15 + (-_paint.ascent() + _paint.descent() + 2 / _scaleFactor) * (textLines.length - 1) + (_scaleFactor - 0.8) * _parentHeight / 2.0f), _paint);



			}



			if (_ifCalloutZoom == true) {
				System.out.println("Called????");

				canvas.drawBitmap(_zoomInBitpmap, _parentWidth / 2  -50, _parentHeight / 2 - 50, null);

                _ifCalloutZoom = false;
			}

		}
		canvas.restore();
	}

	private class LoadZoomInTask extends AsyncTask<Integer, Integer, String> {

		@Override
		protected String doInBackground(Integer... arg0) {
            System.out.println("arg0 " + arg0[0]);
            int index = arg0[0];
            System.out.println("image size: " + _imagesetBitmaps[_curShowingImage % _numImgs].getWidth() + "X" + _imagesetBitmaps[_curShowingImage % _numImgs].getHeight());
            _zoomInBitpmap = Bitmap.createBitmap(_imagesetBitmaps[_curShowingImage % _numImgs],
                    Math.max(_callouts.get(index).first.x/_sampleSize-50, 0),
                    Math.max(_callouts.get(index).first.y/_sampleSize - 50, 0),
                    Math.min(100, (int)(_bmpWidth - _callouts.get(index).first.x/_sampleSize) + 50),
                    Math.min(100, (int)(_bmpHeight- _callouts.get(index).first.y/_sampleSize) + 50));

			_ifCalloutZoom = true;
			postInvalidate();
			return null;
		}

	}


	/**
	 * Multithread class to load images, the image size is assumed to be same for all in same group
	 * - The scaling factor is expected to be automatic based on the size of the image and canvas size, choose the
	 * minimum factor - 0.15 for safety
	 * -
	 */
	private class LoadImagesTask extends AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(String... deviceName) {

			//for now, let me keep this setting because I fixed one dimension bettwen 400-500
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inSampleSize = 2;
			_sampleSize = opts.inSampleSize;

			for (int i = 0; i < _numImgs; i++) {
				InputStream tmpStream = null;

				try {
					System.out.println("loading .." + _imagesetFilenameList.get(i));
//					tmpStream = new FileInputStream("/mnt/extSdCard/Content/Images/" + _imagesetName + "/" + _imagesetFilenameList.get(i));
					tmpStream = new FileInputStream("/mnt/sdcard/Kenwood/Content/Images/" + _imagesetName + "/" + _imagesetFilenameList.get(i));
				} catch (IOException e) {
					e.printStackTrace();
				}

				Bitmap orgbmp = BitmapFactory.decodeStream(tmpStream, null, opts);
				_bmpWidth = orgbmp.getWidth();
				_bmpHeight = orgbmp.getHeight();

				Log.i("imageset image size", _bmpWidth + "x" + _bmpHeight);
				Log.i("parent canvas size", _parentWidth + "x" + _parentHeight);

				_scaleFactorMax = Math.min(_parentWidth*1.0f/_bmpWidth, _parentHeight*1.0f/_bmpHeight) - 0.15f;

				// I don't want to scale now, I want to use the maximum possible information here
				// fix the scale, no scaling at beginning
				float scale = 0.99f;
				_scaleImageReading = scale;

				//Matrix scaleMatrix = new Matrix();
				//scaleMatrix.postScale(scale, scale);

				//_imagesetBitmaps[i] = Bitmap.createBitmap(orgbmp, 0, 0, _bmpWidth, _bmpHeight, scaleMatrix, false);
                //Note that: The new bitmap may be the same object as source, or a copy may have been made.
                // It is initialized with the same density as the original bitmap
//                _imagesetBitmaps[i] = Bitmap.createBitmap(orgbmp, 0, 0, _bmpWidth-1, _bmpHeight-1);
                _imagesetBitmaps[i] = orgbmp;
				_ifLoadedImages[i] = true;
				//orgbmp.recycle();

				publishProgress(i + 1);

				// redraw when first image loading finished
//				if (i == 0) {
//					_lastShowedImage = -1;
//					postInvalidate();
//				}

				//doesn't really cause any problem if we notify the drawing area to repaint
				_lastShowedImage = -1;
				postInvalidate();
			}

			return null;
		}

		// update the progress bar
		protected void onProgressUpdate(Integer... progress) {
			if (progress[0] <= 1)
				_progressBar.setVisibility(View.VISIBLE);

			_progressBar.setProgress(progress[0]);

			if (progress[0] >= _numImgs)
				_progressBar.setVisibility(View.GONE);
		}
	}

	// set progress bar and customized degree
	public void setProgressBar(ProgressBar imageloadingProgBar) {
		_progressBar = imageloadingProgBar;
	}

	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			_scaleFactor *= detector.getScaleFactor();
			_scaleFactor = Math.max(1.0f, Math.min(_scaleFactor, 1.2f));

			invalidate();
			return true;
			// return super.onScale(detector);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View#onTouchEvent(android.view.MotionEvent) Override
	 * the touch event, but remember return true (not consuming the touch
	 * event); otherwise, the moving event can't be detected
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float X = event.getX();
		float Y = event.getY();

		float XOffset, YOffset;

		long nowTimeinMills = System.currentTimeMillis();

		System.out.println("checking imagesetbitmap size" + _numImgs);

        if(_imagesetBitmaps[_curShowingImage % _numImgs] == null )
            return true;

		int startX = _parentWidth / 2 - _imagesetBitmaps[0].getWidth() / 2;
		int startY = _parentHeight / 2 - _imagesetBitmaps[0].getHeight() / 2;

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:

                System.out.println("Chosen callout: " + _chosenCalloutIndex);
                //if there is callout showing and click is inside the click-to-zoom-in region, then zoom in and return
                if (X >= _parentWidth - 200 && X <= _parentWidth && Y >=0 && Y <=200 &&  _chosenCalloutIndex >= 0){
                    System.out.println("Zoom in is triggered");
                    LoadZoomInTask zoomInTask= new LoadZoomInTask();
                    zoomInTask.execute(_chosenCalloutIndex);
                    return true;
                }



				float minDist = Float.MAX_VALUE;
				float tmpDist;
				_chosenCalloutIndex = -1;
				//_ifCalloutZoom = false;

				for (Pair<Point, String> p : _callouts) {
					tmpDist = (startX + p.first.x * _scaleImageReading / _sampleSize - X) * (startX + p.first.x * _scaleImageReading / _sampleSize - X)
							+ (startY + p.first.y * _scaleImageReading / _sampleSize - Y) * (startY + p.first.y * _scaleImageReading / _sampleSize - Y);

					if (tmpDist < minDist) {
						minDist = tmpDist;
						_chosenCalloutIndex = _callouts.indexOf(p);
					}
				}

//			Dialog d = new Dialog(getContext());
//			d.show();

				Log.i("touchevent", "action_down");
				_ifTouching = true;
				postInvalidate();
				break;
			case MotionEvent.ACTION_UP:
				Log.i("touchevent", "action_up");
				_ifTouching = false;
				break;
			case MotionEvent.ACTION_MOVE:
				Log.i("touchevent", "action move");
				//_chosenCalloutIndex = -1;

//			if (!_ifBlockingMouseMove) {
//
//				Log.i("touchevent", "action move -- responding");
//
//				if (_ifTouching) {
//					Log.i("touchevent", "action move -- responding -- touch down");
//
//					XOffset = (int) (X - _lastX);
//
//					System.out.println("Xoffset" + XOffset);
//
//					if (XOffset > 0)
//						_curShowingImage--;
//					else if (XOffset < 0)
//						_curShowingImage++;
//				} else {
//					XOffset = 0;
//				}
//
//				_lastX = X;
//				_lastY = Y;
//			}

				break;
			default:
				Log.i("touchevent", "other type");
		}

		//_scaleDetector.onTouchEvent(event);
		super.onTouchEvent(event);
		return true;
	}

	public void setDegreeIndicator(Angle360View angle360View) {
		_angle360View = angle360View;
	}

	public void updateOthers(int value) {
		// update degree indicator
		_angle360View.updateAngleIndex(value);
	}

	public void recycleMemory() {
		//recycling image should be careful, otherwise it will trigger error sometimes
		for (int i = 0; i < _numImgs; i++) {
			if (_ifLoadedImages[i] == true) {
				_ifLoadedImages[i] = false;
				_imagesetBitmaps[i].recycle();
			}
		}

	}

}
