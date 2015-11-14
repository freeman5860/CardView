package com.freeman.carddemo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.freeman.cardview.R;
import com.freeman.cardview.views.BusinessCardChildView;
import com.freeman.cardview.views.BusinessCardView;

public class DeckDemoActivity extends Activity {
	
	BusinessCardView<DemoData> mDeckView;
	
	ArrayList<DemoData> mEntries;
	
	Bitmap mDefaultThumbnail;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_deck_view_sample);
		
		mDeckView = (BusinessCardView) findViewById(R.id.deckview);
		
		mDefaultThumbnail = BitmapFactory.decodeResource(getResources(),
                R.drawable.test_business_card);
        
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
                    null, "Loading...", Color.DKGRAY);
        }
	}
	
	private void changeDataSet(int size){
		ArrayList<DemoData> tempList = new ArrayList<DemoData>();
		for(int i = 0; i < size; i++){
			DemoData data = new DemoData();
    		data.strDesc = "test " + i;
			tempList.add(data);
		}
		mEntries.clear();
		mEntries.addAll(tempList);
		mDeckView.notifyDataSetChanged();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.deck_demo_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.action_set_two:
			changeDataSet(2);
			break;
		case R.id.action_set_four:
			changeDataSet(4);
			break;
		case R.id.action_set_ten:
			changeDataSet(10);
			break;
		case R.id.action_set_hundred:
			changeDataSet(100);
			break;
		case R.id.action_set_five_hundred:
			changeDataSet(500);
			break;
		case R.id.action_set_thousand:
			changeDataSet(1000);
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}
