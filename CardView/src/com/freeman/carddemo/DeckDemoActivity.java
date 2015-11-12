package com.freeman.carddemo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Toast;

import com.freeman.cardview.R;
import com.freeman.cardview.views.BusinessCardChildView;
import com.freeman.cardview.views.BusinessCardView;

public class DeckDemoActivity extends Activity {
	
	BusinessCardView<DemoData> mDeckView;
	
	ArrayList<DemoData> mEntries;
	
	Bitmap mDefaultThumbnail;
	
	Drawable mDefaultHeaderIcon;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_deck_view_sample);
		
		mDeckView = (BusinessCardView) findViewById(R.id.deckview);
		
		mDefaultThumbnail = BitmapFactory.decodeResource(getResources(),
                R.drawable.default_thumbnail);
        mDefaultHeaderIcon = getResources().getDrawable(R.drawable.default_header_icon);
        
        if(mEntries == null){
        	mEntries = new ArrayList<DemoData>();
        	
        	for(int i = 0; i < 10; i++){
        		DemoData data = new DemoData();
        		data.strDesc = "test " + i;
        		mEntries.add(data);
        	}
        }
		
        BusinessCardView.Callback<DemoData> deckViewCallBack = new BusinessCardView.Callback<DemoData>() {

			@Override
			public ArrayList<DemoData> getData() {
				return mEntries;
			}

			@Override
			public void loadViewData(
					WeakReference<BusinessCardChildView<DemoData>> dcv, DemoData item) {
				loadViewDataInternal(item,dcv);
			}

			@Override
			public void unloadViewData(DemoData item) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onViewDismissed(DemoData item) {
				mEntries.remove(item);
				
				mDeckView.notifyDataSetChanged();
			}

			@Override
			public void onItemClick(DemoData item) {
				Toast.makeText(DeckDemoActivity.this,
                        "Item with title: '" + item.strDesc + "' clicked",
                        Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onNoViewsToDeck() {
				Toast.makeText(DeckDemoActivity.this,
                        "No views to show",
                        Toast.LENGTH_SHORT).show();
			}
		};
		
		mDeckView.initialize(deckViewCallBack);
	}

	protected void loadViewDataInternal(DemoData item,
			WeakReference<BusinessCardChildView<DemoData>> weakView) {
		if (weakView.get() != null) {
            weakView.get().onDataLoaded(item, mDefaultThumbnail,
                    mDefaultHeaderIcon, "Loading...", Color.DKGRAY);
        }
	}
}
