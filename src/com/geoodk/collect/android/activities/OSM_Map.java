package com.geoodk.collect.android.activities;

/*
 * 06.30.2014
 * Jon Nordling
 * Matias Something?
 * 
 * This activity is to map the data offline
 * 
 */

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;









//import org.apache.james.mime4j.util.StringArrayMap;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.Marker.OnMarkerDragListener;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;



import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.geoodk.collect.android.R;
import com.geoodk.collect.android.R.id;
import com.geoodk.collect.android.R.layout;
import com.geoodk.collect.android.R.menu;
import com.geoodk.collect.android.database.ODKSQLiteOpenHelper;
import com.geoodk.collect.android.provider.InstanceProviderAPI;
import com.geoodk.collect.android.provider.FormsProviderAPI.FormsColumns;
import com.geoodk.collect.android.provider.InstanceProviderAPI.InstanceColumns;
import com.geoodk.collect.android.spatial.CustomMarkerHelper;
import com.geoodk.collect.android.spatial.XmlGeopointHelper;
import com.geoodk.collect.android.spatial.CustomPopupMaker;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Xml;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.os.Build;

public class OSM_Map extends Activity {
	private MapView mapView;
	private MapController myMapController;
	private ItemizedIconOverlay<OverlayItem> complete_overlays;
	private ItemizedIconOverlay<OverlayItem> final_overlays;
	private ItemizedIconOverlay<OverlayItem> defalt_overlays;
	private DefaultResourceProxyImpl resource_proxy;
	private Context self = this;
	private Marker loc_marker;
	private Criteria criteria = new Criteria();
	private String provider;
	//public XmlGeopointHelper geoheler = new XmlGeopointHelper();


	private static final String t = "Map";
	ArrayList marker_list = new ArrayList<OverlayItem>();
	private List<String[]> markerListArray = new ArrayList<String[]>();
	LocationManager locationManager;
	
	public static final int pos_url=0;
	public static final int pos_id=1;
	public static final int pos_name=2;
	public static final int pos_status=3;
	public static final int pos_uri=4;

	
	


    @Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);
		  MenuInflater inflater = getMenuInflater();
		  inflater.inflate(R.menu.map_click_menu, menu);
	}
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.map_menu_layout, menu);
        return true;
    }
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		return super.onOptionsItemSelected(item);
	}

    
    //This function comes after the onCreate function
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		//myMapController.setZoom(4);
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Initializing all the
		super.onCreate(savedInstanceState); // Find out what this does?
		setContentView(R.layout.osmmap_layout); //Setting Content to layout xml
		setTitle(getString(R.string.app_name) + " > Mapping"); // Setting title of the action bar

		//The locationManager 
		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		Location lastLocation 	= locationManager.getLastKnownLocation(	LocationManager.GPS_PROVIDER);

		//Layout Code MapView Connection and options
		mapView = (MapView)findViewById(R.id.MapViewId);
		mapView.setTileSource(TileSourceFactory.MAPQUESTOSM);
		mapView.setMultiTouchControls(true);
		mapView.setBuiltInZoomControls(true);
		mapView.setUseDataConnection(true);

		//Figure this out!!!!! I want to call this a a class and return the some value!!!!!!1
		//String name = geoheler.getGeopointDBField(temp); 
        
        //Sets the  Resource Proxy
        resource_proxy = new DefaultResourceProxyImpl(getApplicationContext());
        
        //This is the gps button and its functionality
        ImageButton gps_button = (ImageButton)findViewById(R.id.gps_button);
        gps_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	//Toast.makeText(self, "Works", Toast.LENGTH_LONG).show();
                // Perform action on click
           	 //locationManager.requestLocationUpdates(1000, 1, criteria, myLocationListener);
            	locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 1, myLocationListener);
            	 //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, LocationListener);
            }
        });
        
        //Spinner s = new Spinner(this);
        
        String selection = InstanceColumns.STATUS + " != ?"; // Find out what this does
        String[] selectionArgs = {InstanceProviderAPI.STATUS_SUBMITTED};  //Look like if arguments passed idk.
        
        //For each instance in the db if there is a point then add it to the overlay/marker list
        String sortOrder = InstanceColumns.STATUS + " DESC, " + InstanceColumns.DISPLAY_NAME + " ASC";
        Cursor instance_cur = getContentResolver().query(InstanceColumns.CONTENT_URI, null, selection, selectionArgs, sortOrder);
        //todo catch when c==null
        instance_cur.moveToFirst();
        while (!instance_cur.isAfterLast()) {
        	String instance_url = (String) instance_cur.getString(instance_cur.getColumnIndex("instanceFilePath"));
        	String instance_form_id = (String) instance_cur.getString(instance_cur.getColumnIndex("jrFormId"));
        	String instance_form_name = (String) instance_cur.getString(instance_cur.getColumnIndex("displayName"));
        	String instance_form_status = (String) instance_cur.getString(instance_cur.getColumnIndex("status"));
            Uri instanceUri = ContentUris.withAppendedId(InstanceColumns.CONTENT_URI, instance_cur.getLong(instance_cur.getColumnIndex(InstanceColumns._ID)));
            String instanceUriString = instanceUri.toString();
            String[] cur_mark = {instance_url,instance_form_id,instance_form_name,instance_form_status,instanceUriString};
            markerListArray.add(cur_mark);
            //Toast.makeText(this, instanceUri.toString(), Toast.LENGTH_SHORT).show();
            getGeoField(instance_form_id);
            //startActivity(new Intent(Intent.ACTION_EDIT, instanceUri));
        	//Determine the geoPoint Field
        	try {
				addGeoPointMarkerList(cur_mark);
				//addGeoPointMarkerList(instance_cur);
			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			instance_cur.moveToNext();
		}
        
        instance_cur.close();
        //set_marker_overlay_listners();
        
        //mapView.getOverlays().add(defalt_overlays);
        mapView.invalidate();
        
        //This is used to wait a second to wait the center the map on the points
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
		  @Override
		  public void run() {
		    //Do something after 100ms
				GeoPoint point = new GeoPoint(47.42625, 14.77417); 
				mapView.getController().setZoom(9);
				mapView.getController().setCenter(point);
		  }
		}, 100);
		loc_marker = new Marker(mapView);
		
		if(lastLocation != null){
			//Set the location of marker on the map
			//Toast.makeText(this,lastLocation.getLatitude()+" "+lastLocation.getLongitude(), Toast.LENGTH_SHORT).show();
			GeoPoint loc = new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude()); 
			loc_marker.setPosition(loc);
			
			loc_marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
			loc_marker.setIcon(getResources().getDrawable(R.drawable.loc_logo_small));
			mapView.getOverlays().add(loc_marker);
        }
		mapView.invalidate();
	}
	/*
	public void set_marker_overlay_listners(){
        Drawable marker = this.getResources().getDrawable(R.drawable.pin_marker);
        defalt_overlays = new ItemizedIconOverlay<OverlayItem>(marker_list, marker, new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
        	
        	//I need to know which marker is selected when close
			@Override
			public boolean onItemLongPress(int arg0, OverlayItem item) {
				Uri instanceUri =Uri.parse(item.getTitle());
				Toast.makeText(self, item.getTitle(), Toast.LENGTH_LONG).show();
				// TODO Auto-generated method stub
				//startActivity(new Intent,Intent.ACTION_EDIT,item.getTitle()));
        		startActivity(new Intent(Intent.ACTION_EDIT, instanceUri ));
				//Toast.makeText(self, arg0, Toast.LENGTH_LONG).show();
				return false;
			}

			@Override
			public boolean onItemSingleTapUp(int arg0, OverlayItem arg1) {
				// TODO Auto-generated method stub
				Toast.makeText(self, arg1.getTitle(), Toast.LENGTH_LONG).show();
				return false;
			}
		}, resource_proxy);
	}*/

	 public void addGeoPointMarkerList (String[] cur_mark) throws XmlPullParserException, IOException {
	         XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
	         factory.setNamespaceAware(true);
	         XmlPullParser xpp = factory.newPullParser();
	         xpp.setInput(new FileReader(new File(cur_mark[pos_url])));
	         int eventType = xpp.getEventType();
	         while (eventType != XmlPullParser.END_DOCUMENT) {
	        	 if (xpp.getName()!=null){
	        		if(xpp.getName().equals("location")){
	        			if (eventType == XmlPullParser.START_TAG){
	        				String tagname = xpp.getName();
	        				eventType = xpp.next();
	        				String value = xpp.getText();
	        				if (value != null){
	        					//marker_list.add(instance);
	        					String[] location = xpp.getText().split(" ");
		        				Double lat = Double.parseDouble(location[0]);
		        				Double lng = Double.parseDouble(location[1]);
		        				GeoPoint point = new GeoPoint(lat, lng); 
		        				CustomMarkerHelper startMarker = new CustomMarkerHelper(mapView);
		        				startMarker.setMarker_name(cur_mark[pos_name]);
		        				startMarker.setMarker_uri(Uri.parse(cur_mark[pos_uri]));
		        				startMarker.setMarker_status(cur_mark[pos_status]);
		        				startMarker.setMarker_url(cur_mark[pos_url]);
		        				startMarker.setMarker_id(cur_mark[pos_id]);
		        				startMarker.setPosition(point);
		        				startMarker.setIcon(getResources().getDrawable(R.drawable.map_marker));
		        				startMarker.setTitle("Name: "+ cur_mark[pos_name]);
		        				startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
		        				startMarker.setSnippet("Status: "+cur_mark[pos_status]);
		        				startMarker.setDraggable(true);
		        				startMarker.setOnMarkerDragListener(draglistner);
		        				startMarker.setInfoWindow(new CustomPopupMaker(mapView, Uri.parse(cur_mark[pos_uri])));
		        				
		        				//popup_button.setOnClickListener(new on);
		        				//startMarker.setSubDescription("Desc");

		        				//startMarker.setIcon(getResources().getDrawable(R.drawable.pin_marker));
		        				mapView.getOverlays().add(startMarker);

		        				//OverlayItem overlayitem = new OverlayItem("Title", "SampleDescription", point);
		        				//Drawable marker = this.getResources().getDrawable(R.drawable.d);
		        				//overlayitem.setMarker(marker);
		        				//marker_list.add(overlayitem);
		        				break;
	        				}else{
	        					break;
	        				}
	        			}
	        		}

				}
	        	 eventType = xpp.next();
	         }
	 }
	//Make this more eficient so that you dont have to use the cursor all the time only if the form has not be queried 
	 public void getGeoField(String form_id){
		String formsortOrder = FormsColumns.DISPLAY_NAME + " ASC, " + FormsColumns.JR_VERSION + " DESC";
        Cursor form_curser =  getContentResolver().query(FormsColumns.CONTENT_URI, null, null, null, formsortOrder);  
        form_curser.moveToFirst();
        //int count = 0;
        while(!form_curser.isAfterLast()){
        	 String tempformID = form_curser.getString(form_curser.getColumnIndex("jrFormId"));
        	 if(tempformID.equals(form_id)){
        		 //read xml and get geopoint table name
        		 //Toast.makeText(this,form_id+" == "+tempformID, Toast.LENGTH_SHORT).show();
        		 //Read the 
        		 //count++;
        	 }else{
        		 //Toast.makeText(this,form_id+" !: "+tempformID, Toast.LENGTH_SHORT).show();
        	 }
        	 form_curser.moveToNext();
        }
        

	 }

	 //This is going to be the listner for the devices locations

	    private LocationListener myLocationListener = new LocationListener(){

			@Override
			public void onLocationChanged(Location location) {
				// TODO Auto-generated method stub
				//updateLoc(location);
				mapView.getOverlays().remove(loc_marker);
				//Toast.makeText(OSM_Map.this,"Location Update", Toast.LENGTH_LONG).show();
				GeoPoint current_loc = new GeoPoint(location);
				loc_marker.setPosition(current_loc);
				mapView.getOverlays().add(loc_marker);
				mapView.invalidate();

				//loc_marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
				//loc_marker.setIcon(getResources().getDrawable(R.drawable.loc_logo_small));
				//mapView.getOverlays().add(loc_marker);
				//Toast.makeText(this,(location.getLatitude())+" "+location.getLongitude(), Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onProviderDisabled(String provider) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProviderEnabled(String provider) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
				// TODO Auto-generated method stub

			}

	    };
	    private OnMarkerDragListener draglistner = new OnMarkerDragListener(){

			@Override
			public void onMarkerDrag(Marker m) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onMarkerDragEnd(Marker m) {
				GeoPoint newlocation = m.getPosition();
				newlocation.getLatitude();
				newlocation.getLongitude();
				showRetryPrompt();
				
				// TODO Auto-generated method stub
				//Toast.makeText(OSM_Map.this,"Done Draging", Toast.LENGTH_LONG).show();
				
			}

			@Override
			public void onMarkerDragStart(Marker m) {
				// TODO Auto-generated method stub
				
			}
	    	
	    };
	    
	    protected void showRetryPrompt() {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                    //loadPublicLegends(mainActivity);
                                    break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                    // No button clicked
                                    break;
                            }
                    }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(OSM_Map.this);
            builder.setMessage(
                    "Loading the legends somehow failed. Do you want to try again?")
                    .setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();
    }
}