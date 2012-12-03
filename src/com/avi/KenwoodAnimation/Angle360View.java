/**
 * 
 */
package com.avi.KenwoodAnimation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

/**
 * @author hgong
 * 
 */
public class Angle360View extends ImageView implements OnClickListener {

	private int _curAngleIndex = 0;
	private int _lastAngleIndex = -1;
    Product3DOnView _product3DOnView;
    
    //Center point of the angle indicator circle and circle radius
    Point _center = new Point(102*8/6, 71*8/6);
    int _radius = 59*8/6;


	Bitmap dotBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.dot);
	
	//on the picture, the center of the dot is located at (122, 81)
	// the radius for the dot moving around is 60
	// the radius of the dot is 10
	// the equation for calculating the (top, left) is
	// top = 122 + 60*sin(index*10) - 10
	//left = 81 - 60*cos(index*10) - 10
	
	private float distance(float x1, float x2, float y1, float y2) {
		return Math.max(FloatMath.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2)), Float.MIN_VALUE);
	}
	
	private Runnable drawUpdateThread = new Runnable() {

		@Override
		public void run() {
			while (true) {
				try {
					if (_curAngleIndex != _lastAngleIndex) {
						postInvalidate();
						_lastAngleIndex = _curAngleIndex;
					}
					Thread.sleep(30);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	};

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		//we only need to draw circle or line
		canvas.drawBitmap(dotBitmap, (float)(_center.x+_radius*Math.cos(_curAngleIndex*10*Math.PI/180f)-10),
				(float)(_center.y-_radius*Math.sin(_curAngleIndex*10*Math.PI/180f)-10), null);
	}

	/**
	 * @param context
	 */
	public Angle360View(Context context) {
		super(context);
	}

	/**
	 * @param context
	 * @param attrs
	 */
	public Angle360View(Context context, AttributeSet attrs) {
		super(context, attrs);

		new Thread(drawUpdateThread).start();
	}

	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public Angle360View(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View arg0) {
		System.out.println("angle indicator click event");
	}

	public void updateAngleIndex(int value) {
		_curAngleIndex = value;
	}

	/* (non-Javadoc)
	 * @see android.view.View#onTouchEvent(android.view.MotionEvent)
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {

		float X = event.getX();
		float Y = event.getY();

        System.out.println("display density "  + getResources().getDisplayMetrics().density + "vs " + getWidth() + "x" + getHeight());

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:

            //notify product 3D view that we are touching it
            _product3DOnView._ifAngleIndTouching = true;

			Log.i("touchevent", "action_down");
            
            //calculate corresponding image index on product view
            if(Y <= _center.y) {
                System.out.println("angle" + Math.acos((X-_center.x)/distance(X, _center.x, Y, _center.y))*18/Math.PI);
                _curAngleIndex = (int) (Math.acos((X-_center.x)/distance(X, _center.x, Y, _center.y))*18/Math.PI);
                
            } else {
            	System.out.println("angle" + (2*Math.PI - Math.acos((X-_center.x)/distance(X, _center.x, Y, _center.y)))*18/Math.PI);
            	_curAngleIndex = (int) ((2*Math.PI - Math.acos((X-_center.x)/distance(X, _center.x, Y, _center.y)))*18/Math.PI);
            }
            
            _product3DOnView._curShowingImage =_curAngleIndex;
            
			break;
		case MotionEvent.ACTION_UP:
			Log.i("touchevent", "action_up");
			//notify the 3d product view that the operation has finished
			//_ifTouching = false;
            _product3DOnView._ifAngleIndTouching = false;
			break;
		case MotionEvent.ACTION_MOVE:
			Log.i("touchevent", "action move");
			
			//calculate corresponding image index on product view
            if(Y <= _center.y) {
                System.out.println("angle" + Math.acos((X-_center.x)/distance(X, _center.x, Y, _center.y))*18/Math.PI);
                _curAngleIndex = (int) (Math.acos((X-_center.x)/distance(X, _center.x, Y, _center.y))*18/Math.PI);
            } else {
            	System.out.println("angle" + (2*Math.PI - Math.acos((X-_center.x)/distance(X, _center.x, Y, _center.y)))*18/Math.PI);
            	_curAngleIndex = (int) ((2*Math.PI - Math.acos((X-_center.x)/distance(X, _center.x, Y, _center.y)))*18/Math.PI);
            }
			
            _product3DOnView._curShowingImage =_curAngleIndex;
			break;
		default:
			Log.i("touchevent", "other type");
		}
		
		//return super.onTouchEvent(event);
		super.onTouchEvent(event);
		return true;
	}

    public void setProductView(Product3DOnView v){
        _product3DOnView = v;
    }

    
    
}
