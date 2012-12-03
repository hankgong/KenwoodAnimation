/**
 * I will use this view to display the image with text 
 */
package com.avi.KenwoodAnimation;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * @author hgong
 *
 */
public class ImageUpTextDownView extends LinearLayout {
	
	private ImageView imageView;
	private TextView textView;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		return super.onTouchEvent(event);
		//return false;
	}

	/**
	 * @param context
	 */
	public ImageUpTextDownView(Context context) {
		//super(context);
		this(context, null);
		// TODO Auto-generated constructor stub
	}

//	@Override
//	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//		// TODO Auto-generated method stub
//		//super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//		//setMeasuredDimension(measuredWidth(widthMeasureSpec), measuredHeight(heightMeasureSpec));
//		setMeasuredDimension(40, 150);
//	}

	/**
	 * @param context
	 * @param attrs
	 */
	public ImageUpTextDownView(Context context, AttributeSet attrs) {
		super(context, attrs);
		//should be here
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.imageuptextdown, this);
		
		imageView = (ImageView)findViewById(R.id.imageupview);
		textView = (TextView)findViewById(R.id.textdownview);
		
		imageView.setImageResource(R.drawable.nx_200_300);
		textView.setText("Test Text");
		
//		if (!isInEditMode()){
//			imageView.setOnClickListener((OnClickListener) this.getContext());
//		}
		//textView.setTextColor(0x0F000000);
	}

//	/**
//	 * @param context
//	 * @param attrs
//	 * @param defStyle
//	 */
//	public ImageUpTextDownView(Context context, AttributeSet attrs, int defStyle) {
//		super(context, attrs, defStyle);
//		// TODO Auto-generated constructor stub
//	}
	
	public void setImageResource(int resId) {
		imageView.setImageResource(resId);
		imageView.setTag(resId);
	}

	public void setTextViewText(String text) {
		textView.setText(text);
	}
	
}
