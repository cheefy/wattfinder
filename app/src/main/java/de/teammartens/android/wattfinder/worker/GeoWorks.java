package de.teammartens.android.wattfinder.worker;

import android.location.Location;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;

import de.teammartens.android.wattfinder.KartenActivity;
import de.teammartens.android.wattfinder.R;

import static de.teammartens.android.wattfinder.KartenActivity.layoutStyle;
import static de.teammartens.android.wattfinder.KartenActivity.mMap;

/**
 * Created by felix on 02.11.14.
 */
public class GeoWorks {
    private static final String LOG_TAG = "WattfinderGeoWorks";



    public static final Float DEFAULT_ZOOM = 10.5f;
    public static final Float DETAIL_ZOOM = 17f;
    public static final Float MY_LOCATION_ZOOM = 13.5f;
    public static final Float MAX_ZOOM = 6.0f; // wenn Zoom kleiner werde keine Säulen mehr geladen wiel kartenausschnitt zu groß
    private static String mQuery="";
    public static boolean CUSTOM_MAPVIEW=false;
    public static LatLng MarkerTarget;
    public static LatLng myPosition,mapPosition,animTarget;


    public static Float mapZoom;
    public static Marker Marker_Ich, Marker_Suche = null;

    public static LatLng getmyPosition() {
        return myPosition;
    }


    public static void setmyPosition() {
        setmyPosition(getmyPosition());
    }

    public static void setmyPosition(LatLng mapPosition) {
        setmyPosition(mapPosition,MY_LOCATION_ZOOM,false);
    }

    public static void setmyPosition(LatLng mPosition,Float zoom, boolean moveMap) {

        if (mPosition != null ){
            if (KartenActivity.mMap != null) {
             if (Marker_Ich != null) Marker_Ich.remove();
             Marker_Ich = KartenActivity.mMap.addMarker(new MarkerOptions().position(mPosition)
                    .title("Meine Position").icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_standort)));
                if (moveMap||!CUSTOM_MAPVIEW)
                    movemapPosition(mPosition, zoom, "setmyPositionZoom");
            }
            myPosition = mPosition;
        }
    }

    public static void movemapPosition(final String VERURSACHER){
        movemapPosition(getMapPosition(),getMapZoom(),VERURSACHER);
    }



    public static void movemapPosition(final LatLng mapPosition, final String VERURSACHER){
        if(mapPosition!=null){
        movemapPosition(mapPosition,getMapZoom(),VERURSACHER);
        }
    }




    public static void movemapPosition(final LatLng position, final float zoom, final String VERURSACHER){

        if(position!=null&& mMap != null&&zoom>2.0f) {
            if (LogWorker.isVERBOSE()) LogWorker.d(LOG_TAG, "moveMap to "+position.toString() + "/"+zoom+" wegen "+VERURSACHER);
            LatLng nPosition = new LatLng(position.latitude,position.longitude);

            Float currentZoom = mMap.getCameraPosition().zoom;



            if (LogWorker.isVERBOSE()) LogWorker.d(LOG_TAG, "moveMap: MapPosition "+(getMapPosition()!=null?getMapPosition().toString():"null") + " :: mMap"+(mMap!=null?mMap.getCameraPosition().target.toString():"null")+" :: nPostion "+nPosition.toString());
            if (!mMap.getCameraPosition().target.equals(nPosition)||zoom!=currentZoom) {
                //Hier wird jetzt unterschieden welche Animation gewählt wird
                if(getMapPosition()==null)setMapPosition(new LatLng(0,0),0f);
                if (LogWorker.isVERBOSE()) LogWorker.d(LOG_TAG, "moveMap to "+nPosition.toString());
                //zuerst prüfen wie groß ist der Abstand zwischen altem und neuen Mittelpunkt
                    Location alt = new Location("MapLocation");
                    Location neu = new Location("NewLocation");
                    Float D =  0f;

                        alt.setLatitude(GeoWorks.mapPosition.latitude);
                        alt.setLongitude(GeoWorks.mapPosition.longitude);
                        neu.setLatitude(nPosition.latitude);
                        neu.setLongitude(nPosition.longitude);
                        D=alt.distanceTo(neu);
                if (LogWorker.isVERBOSE()) LogWorker.d(LOG_TAG, "moveMap D="+D);
                        CameraUpdate CU = CameraUpdateFactory.zoomTo(zoom);
                       /* if(D>100000f){
                            //wenn Abstand größer als 100km dann move und zoom in
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nPosition, 5));
                            if (LogWorker.isVERBOSE()) LogWorker.d(LOG_TAG, "moveMap move&zoom "+nPosition.toString());
                            CU = CameraUpdateFactory.zoomTo(zoom);
                        }else{
                            // kleinerer Abstand dann abhängig, wenn zoom identsich nur bewegen, sonst noch zoomen

                            */
                            if(zoom==currentZoom)
                                    CU = CameraUpdateFactory.newLatLng(nPosition);
                            else{
                                //mMap.moveCamera(CameraUpdateFactory.newLatLng(nPosition));
                              /*  if (LogWorker.isVERBOSE()) LogWorker.d(LOG_TAG, "moveMap to "+nPosition.toString()+" zoomBy "+(zoom-currentZoom)+"("+currentZoom+"-"+zoom+")");
                                //CU = CameraUpdateFactory.zoomBy(zoom-currentZoom);

                                CU = CameraUpdateFactory.newLatLngZoom(nPosition, zoom);*/
                                mMap.moveCamera(CameraUpdateFactory.newLatLng(nPosition));
                                if (LogWorker.isVERBOSE()) LogWorker.d(LOG_TAG, "moveMap move&zoom "+nPosition.toString());
                                CU = CameraUpdateFactory.zoomTo(zoom);
                            }

                       // }


                mMap.animateCamera(CU, 1000, new GoogleMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        if (LogWorker.isVERBOSE())
                            LogWorker.d(LOG_TAG, "moveMap animateFinish -"+ mMap.getCameraPosition().target.toString()+" -Zoom:" + zoom +" Verursacher:" + VERURSACHER);

                        // nach dem Zoomen in den Details den Versatz nochmal korrigieren, deshalb zweite Animation starten
                        if ((AnimationWorker.isDetailsVisibile()||AnimationWorker.isFilterVisibile()||VERURSACHER=="showMapBackPress")&&VERURSACHER!="moveMap")
                                movemapPosition(position, zoom, "moveMap");

                        SaeulenWorks.checkMarkerCache(VERURSACHER);
                    }

                    @Override
                    public void onCancel() {
                        if (LogWorker.isVERBOSE())
                            LogWorker.d(LOG_TAG, "moveMap animateCancel -Zoom:" + zoom + " Verursacher:" + VERURSACHER);

                    }
                });
            }else if (LogWorker.isVERBOSE()) LogWorker.d(LOG_TAG, "moveMap Position unverändert");

            //GeoWorks.mapPosition = position;
            //GeoWorks.mapZoom = zoom;
        }
    }


    public static Float getMapZoom() {
        return (mapZoom!=null?mapZoom:DEFAULT_ZOOM);
    }

    public static LatLng getMapPosition() {
        return mapPosition;
    }

    public static void setMapPosition(LatLng mapPosition) {
        GeoWorks.mapPosition = mapPosition;
    }

    public static void setMapPosition(LatLng mapPosition,Float zoom) {
        if (LogWorker.isVERBOSE()) LogWorker.d(LOG_TAG,"setMapPosition:"+mapPosition.toString()+" z:"+zoom);
        GeoWorks.mapPosition = mapPosition;
        GeoWorks.mapZoom = zoom;
    }




    public static void starteSuche(String query){
        if(query==null){LogWorker.e(LOG_TAG, "EMPTY Search Triggered" + query);}else{


        if (LogWorker.isVERBOSE()) LogWorker.d(LOG_TAG,"Search Triggered"+query);
        mQuery=query;
        //LatLng result = getLatLongFromAddress(query);
        String uri = "http://maps.google.com/maps/api/geocode/json?address=" +
                URLEncoder.encode(query) + "&sensor=true&language=de&region=eu";

        CUSTOM_MAPVIEW=true;
        JsonObjectRequest req = new JsonObjectRequest(uri, null,
                new Response.Listener<JSONObject>() {


                    @Override
                    public void onResponse(JSONObject response) {


                        try {


                            double lng = ((JSONArray)response.get("results")).getJSONObject(0)
                                    .getJSONObject("geometry").getJSONObject("location")
                                    .getDouble("lng");

                            double lat = ((JSONArray)response.get("results")).getJSONObject(0)
                                    .getJSONObject("geometry").getJSONObject("location")
                                    .getDouble("lat");
                            String fAddress = ((JSONArray)response.get("results")).getJSONObject(0)
                                    .getString("formatted_address");

                            if (LogWorker.isVERBOSE()) LogWorker.d(LOG_TAG,"latitude "+ lat);
                            if (LogWorker.isVERBOSE()) LogWorker.d(LOG_TAG,"longitude "+ lng);
                            if (LogWorker.isVERBOSE()) LogWorker.d(LOG_TAG,"at "+ fAddress);
                            LatLng l = new LatLng(lat,lng);

                            Suchmarker(l, fAddress);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }


                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO Auto-generated method stub

            }
        });

// Access the RequestQueue through your singleton class.
        KartenActivity.getInstance().addToRequestQueue(req);

        }


    }

    /*

    gleiche Funktion wie oben aber diesmal nicht suchen nur anzeigen
     */
    public static void starteSucheSuggested(String query){
        if(query==null){LogWorker.e(LOG_TAG, "EMPTY Suggested Search Triggered" + query);}else{


            if (LogWorker.isVERBOSE()) LogWorker.d(LOG_TAG,"Suggested Search Triggered"+query);
            mQuery=query;



            String[] M1 = query.split("::");

            String fAddress = M1[0];
            Double mLat = Double.parseDouble(M1[1]);
            Double mLng = Double.parseDouble(M1[2]);

            if (LogWorker.isVERBOSE()) LogWorker.d(LOG_TAG,"latitude "+ mLat);
            if (LogWorker.isVERBOSE()) LogWorker.d(LOG_TAG,"longitude "+ mLng);
            if (LogWorker.isVERBOSE()) LogWorker.d(LOG_TAG,"at "+ fAddress);
            LatLng l = new LatLng(mLat,mLng);

            Suchmarker(l, fAddress);




                    }
            }



    public static void Suchmarker(LatLng Coord,String Desc){

        if (mMap != null) {

            if (LogWorker.isVERBOSE()) LogWorker.d(LOG_TAG, "Create Marker Suche " + Desc);

            if (Coord != null && !Desc.isEmpty()){
                if (Marker_Suche !=null) Marker_Suche.remove();
                Marker_Suche = mMap.addMarker(new MarkerOptions().position(Coord).title(Desc).icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_suche)));

                CUSTOM_MAPVIEW=true;
                AnimationWorker.hide_info();
                if(AnimationWorker.isDetailsVisibile())AnimationWorker.toggleDetails();
                if(AnimationWorker.isFilterVisibile())AnimationWorker.toggleFilter();
                movemapPosition(Coord,12f,"GeoWorks.Suchmarker");

            }

            //SaeulenWorks.ladeMarker(Coord.latitude, Coord.longitude, "GeoWorks.Suchmarker");

        }
    }






    public static boolean isPositionversetzt(){
        if(LogWorker.isVERBOSE())LogWorker.d(LOG_TAG,"isPositionVersetzt: Details:"+AnimationWorker.isDetailsVisibile()+" Filter:"+AnimationWorker.isFilterVisibile()+" layout:"+layoutStyle());

        if(AnimationWorker.isDetailsVisibile()||
                (AnimationWorker.isFilterVisibile()&&!KartenActivity.layoutStyle().equals("default")))
        {
            if(LogWorker.isVERBOSE())LogWorker.d(LOG_TAG,"isPositionVersetzt:true");


            return true;
        }

        if(LogWorker.isVERBOSE())LogWorker.d(LOG_TAG,"isPositionVersetzt:false");
        return false;
    }


    public static boolean validLatLng (LatLng mLatLng){

        LatLngBounds llb = new LatLngBounds(new LatLng(33.724339, -22.93945), new LatLng(66.478208, 34.628906));

        if(mLatLng== null ) return false;
        if (LogWorker.isVERBOSE())LogWorker.d(LOG_TAG,"mLatLng: "+mLatLng);
        if (llb != null
                && llb.contains(mLatLng))
            return true;

        return false;
    }
}
