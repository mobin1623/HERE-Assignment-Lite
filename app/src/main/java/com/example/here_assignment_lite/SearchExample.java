/*
 * Copyright (C) 2019-2021 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package com.example.here_assignment_lite;

import android.app.AlertDialog;
import android.content.Context;

import android.content.DialogInterface;
import android.util.Log;
import android.widget.Toast;

import com.here.sdk.core.Anchor2D;
import com.here.sdk.core.CustomMetadataValue;
import com.here.sdk.core.GeoBox;
import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.core.GeoCorridor;
import com.here.sdk.core.GeoPolyline;
import com.here.sdk.core.LanguageCode;
import com.here.sdk.core.Metadata;
import com.here.sdk.core.Point2D;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.gestures.GestureState;
import com.here.sdk.mapviewlite.Camera;
import com.here.sdk.mapviewlite.MapImage;
import com.here.sdk.mapviewlite.MapImageFactory;
import com.here.sdk.mapviewlite.MapMarker;
import com.here.sdk.mapviewlite.MapMarkerImageStyle;
import com.here.sdk.mapviewlite.MapPolyline;
import com.here.sdk.mapviewlite.MapPolylineStyle;
import com.here.sdk.mapviewlite.MapViewLite;
import com.here.sdk.mapviewlite.PickMapItemsCallback;
import com.here.sdk.mapviewlite.PickMapItemsResult;
import com.here.sdk.mapviewlite.PixelFormat;
import com.here.sdk.routing.CalculateRouteCallback;
import com.here.sdk.routing.CarOptions;
import com.here.sdk.routing.Route;
import com.here.sdk.routing.RoutingEngine;
import com.here.sdk.routing.RoutingError;
import com.here.sdk.routing.Section;
import com.here.sdk.routing.Waypoint;
import com.here.sdk.search.Address;
import com.here.sdk.search.AddressQuery;
import com.here.sdk.search.Place;
import com.here.sdk.search.SearchCallback;
import com.here.sdk.search.SearchEngine;
import com.here.sdk.search.SearchError;
import com.here.sdk.search.SearchOptions;
import com.here.sdk.search.SuggestCallback;
import com.here.sdk.search.Suggestion;
import com.here.sdk.search.TextQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SearchExample {

    private static final String LOG_TAG = SearchExample.class.getName();

    private final Context context;
    private final MapViewLite mapView;
    private final Camera camera;
    private final List<MapMarker> mapMarkerList = new ArrayList<>();
    private SearchEngine searchEngine;



    private GeoCoordinates startGeoCoordinates;
    private GeoCoordinates destinationGeoCoordinates;
    private double startLatitude, startLongitude,
            endLatitude, endLongitude ;
    private int click = 0;
    private RoutingEngine routingEngine;
    private final List<MapPolyline> mapPolylines = new ArrayList<>();

    public SearchExample(Context context, MapViewLite mapView) {
        this.context = context;
        this.mapView = mapView;
        camera = mapView.getCamera();
        camera.setTarget(new GeoCoordinates(52.530932, 13.384915));
        camera.setZoomLevel(14);

        try {
            searchEngine = new SearchEngine();
            routingEngine = new RoutingEngine();
        } catch (InstantiationErrorException e) {
            throw new RuntimeException("Initialization of SearchEngine failed: " + e.error.name());
        }

        setTapGestureHandler();
        setLongPressGestureHandler();

//        Toast.makeText(context,"Long press on map to get the address for that position using reverse geocoding.", Toast.LENGTH_LONG).show();
    }

    public void onSearchButtonClicked() {
        // Search for "Pizza" and show the results on the map.
        searchExample();

        // Search for auto suggestions and log the results to the console.
        autoSuggestExample();
    }

    public void onGeocodeButtonClicked() {
        // Search for the location that belongs to an address and show it on the map.
        geocodeAnAddress();
    }

    private void searchExample() {
        String searchTerm = "restaurants";

        Toast.makeText(context,"Searching : " + searchTerm, Toast.LENGTH_LONG).show();
        searchInViewport(searchTerm);
    }

    private void geocodeAnAddress() {
        // Set map near to expected location.
        GeoCoordinates geoCoordinates = new GeoCoordinates(52.537931, 13.384914);
        camera.setTarget(geoCoordinates);
        camera.setZoomLevel(14);

        String queryString = "Invalidenstraße 116, Berlin";

//        Toast.makeText(context,"Finding locations for: " + queryString
//               + ". Tap marker to see the coordinates. Check the logs for the address.", Toast.LENGTH_LONG).show();

        geocodeAddressAtLocation(queryString, geoCoordinates);
    }

    private void setTapGestureHandler() {
        mapView.getGestures().setTapListener(touchPoint -> pickMapMarker(touchPoint));
    }

    private void setLongPressGestureHandler() {
        mapView.getGestures().setLongPressListener((gestureState, touchPoint) -> {
            if (gestureState == GestureState.BEGIN) {
                GeoCoordinates geoCoordinates = mapView.getCamera().viewToGeoCoordinates(touchPoint);
                addPoiMapMarker(geoCoordinates);
                getAddressForCoordinates(geoCoordinates);
            }
        });
    }

    private void getAddressForCoordinates(GeoCoordinates geoCoordinates) {
        int maxItems = 1;
        SearchOptions reverseGeocodingOptions = new SearchOptions(LanguageCode.EN_GB, maxItems);

        searchEngine.search(geoCoordinates, reverseGeocodingOptions, new SearchCallback() {
            @Override
            public void onSearchCompleted(@Nullable SearchError searchError, @Nullable List<Place> list) {
                if (searchError != null) {
//                    showDialog("Reverse geocoding", "Error: " + searchError.toString());
                    return;
                }

                // If error is null, list is guaranteed to be not empty.
//                showDialog("Reverse geocoded address:", list.get(0).getAddress().addressText);
            }
        });
    }

    private void pickMapMarker(final Point2D point2D) {
        float radiusInPixel = 2;
        mapView.pickMapItems(point2D, radiusInPixel, new PickMapItemsCallback() {
            @Override
            public void onMapItemsPicked(@Nullable PickMapItemsResult pickMapItemsResult) {
                if (pickMapItemsResult == null) {
                    return;
                }

                MapMarker topmostMapMarker = pickMapItemsResult.getTopmostMarker();
                if (topmostMapMarker == null) {
                    return;
                }

                Metadata metadata = topmostMapMarker.getMetadata();
                if (metadata != null) {
                    CustomMetadataValue customMetadataValue = metadata.getCustomValue("key_search_result");
                    if (customMetadataValue != null) {
                        SearchResultMetadata searchResultMetadata = (SearchResultMetadata) customMetadataValue;
                        String title = searchResultMetadata.searchResult.getTitle();
                        String vicinity = searchResultMetadata.searchResult.getAddress().addressText;




                        // checking if map marker clicked

                        if (click == 0){
                            startLatitude = topmostMapMarker.getCoordinates().latitude;
                            startLongitude = topmostMapMarker.getCoordinates().longitude;
                            click++;
                            clearRoute();
                            showDialog("Result",title + "\n Vicinity: " + vicinity , 0);
                        }else if (click == 1){
                            endLatitude = topmostMapMarker.getCoordinates().latitude;
                            endLongitude = topmostMapMarker.getCoordinates().longitude;
                            showDialog("Result",title + "\n Vicinity: " + vicinity , 1);
                            addRoute();
                        }



                        return;
                    }
                }

//                showDialog("Picked Map Marker",
//                        "Geographic coordinates: " +
//                                topmostMapMarker.getCoordinates().latitude + ", " +
//                                topmostMapMarker.getCoordinates().longitude);


            }
        });
    }

    private void searchInViewport(String queryString) {
        clearMap();

        GeoBox viewportGeoBox = getMapViewGeoBox();
        TextQuery query = new TextQuery(queryString, viewportGeoBox);

        int maxItems = 30;
        SearchOptions searchOptions = new SearchOptions(LanguageCode.EN_US, maxItems);

        searchEngine.search(query, searchOptions, new SearchCallback() {
            @Override
            public void onSearchCompleted(@Nullable SearchError searchError, @Nullable List<Place> list) {
                if (searchError != null) {
//                    showDialog("Search", "Error: " + searchError.toString());
                    return;
                }

                // If error is null, list is guaranteed to be not empty.
//                showDialog("Search", "Results: " + list.size());

                // Add new marker for each search result on map.
                for (Place searchResult : list) {
                    Metadata metadata = new Metadata();
                    metadata.setCustomValue("key_search_result", new SearchResultMetadata(searchResult));
                    addPoiMapMarker(searchResult.getGeoCoordinates(), metadata);
                }
            }
        });
    }

    private static class SearchResultMetadata implements CustomMetadataValue {

        public final Place searchResult;

        public SearchResultMetadata(Place searchResult) {
            this.searchResult = searchResult;
        }

        @NonNull
        @Override
        public String getTag() {
            return "SearchResult Metadata";
        }
    }

    private final SuggestCallback autosuggestCallback = new SuggestCallback() {
        @Override
        public void onSuggestCompleted(@Nullable SearchError searchError, @Nullable List<Suggestion> list) {
            if (searchError != null) {
                Log.d(LOG_TAG, "Autosuggest Error: " + searchError.name());
                return;
            }

            // If error is null, list is guaranteed to be not empty.
            Log.d(LOG_TAG, "Autosuggest results: " + list.size());

            for (Suggestion autosuggestResult : list) {
                String addressText = "Not a place.";
                Place place = autosuggestResult.getPlace();
                if (place != null) {
                    addressText = place.getAddress().addressText;
                }

                Log.d(LOG_TAG, "Autosuggest result: " + autosuggestResult.getTitle() +
                        " addressText: " + addressText);
            }
        }
    };

    private void autoSuggestExample() {
        GeoCoordinates centerGeoCoordinates = getMapViewCenter();
        int maxItems = 5;
        SearchOptions searchOptions = new SearchOptions(LanguageCode.EN_US, maxItems);

        // Simulate a user typing a search term.
        searchEngine.suggest(
                new TextQuery("p", // User typed "p".
                        centerGeoCoordinates),
                searchOptions,
                autosuggestCallback);

        searchEngine.suggest(
                new TextQuery("pi", // User typed "pi".
                        centerGeoCoordinates),
                searchOptions,
                autosuggestCallback);

        searchEngine.suggest(
                new TextQuery("piz", // User typed "piz".
                        centerGeoCoordinates),
                searchOptions,
                autosuggestCallback);
    }

    private void geocodeAddressAtLocation(String queryString, GeoCoordinates geoCoordinates) {
        clearMap();

        AddressQuery query = new AddressQuery(queryString, geoCoordinates);

        int maxItems = 30;
        SearchOptions options = new SearchOptions(LanguageCode.DE_DE, maxItems);

        searchEngine.search(query, options, new SearchCallback() {
            @Override
            public void onSearchCompleted(SearchError searchError, List<Place> list) {
                if (searchError != null) {
//                    showDialog("Geocoding", "Error: " + searchError.toString());
                    return;
                }

                for (Place geocodingResult : list) {
                    GeoCoordinates geoCoordinates = geocodingResult.getGeoCoordinates();
                    Address address = geocodingResult.getAddress();
                    String locationDetails = address.addressText
                            + ". GeoCoordinates: " + geoCoordinates.latitude
                            + ", " + geoCoordinates.longitude;

                    Log.d(LOG_TAG, "GeocodingResult: " + locationDetails);
                    addPoiMapMarker(geoCoordinates);
                }

//                showDialog("Geocoding result","Size: " + list.size());
            }
        });
    }

    private void addPoiMapMarker(GeoCoordinates geoCoordinates) {
        MapMarker mapMarker = createPoiMapMarker(geoCoordinates);
        mapView.getMapScene().addMapMarker(mapMarker);
        mapMarkerList.add(mapMarker);
    }

    private void addPoiMapMarker(GeoCoordinates geoCoordinates, Metadata metadata) {
        MapMarker mapMarker = createPoiMapMarker(geoCoordinates);
        mapMarker.setMetadata(metadata);
        mapView.getMapScene().addMapMarker(mapMarker);
        mapMarkerList.add(mapMarker);
    }

    private MapMarker createPoiMapMarker(GeoCoordinates geoCoordinates) {
        MapImage mapImage = MapImageFactory.fromResource(context.getResources(), R.drawable.poi);
        MapMarker mapMarker = new MapMarker(geoCoordinates);
        MapMarkerImageStyle mapMarkerImageStyle = new MapMarkerImageStyle();
        mapMarkerImageStyle.setAnchorPoint(new Anchor2D(0.5F, 1));
        mapMarker.addImage(mapImage, mapMarkerImageStyle);
        return mapMarker;
    }

    private GeoCoordinates getMapViewCenter() {
        return mapView.getCamera().getTarget();
    }

    private GeoBox getMapViewGeoBox() {
        return mapView.getCamera().getBoundingBox();
    }

    private void clearMap() {
        for (MapMarker mapMarker : mapMarkerList) {
            mapView.getMapScene().removeMapMarker(mapMarker);
        }
        mapMarkerList.clear();
    }

    private void clearRoute() {
        for (MapPolyline mapPolyline : mapPolylines) {
            mapView.getMapScene().removeMapPolyline(mapPolyline);
        }
        mapPolylines.clear();

    }

    private void showDialog(String title, String message, int TAG) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        if (TAG == 0){
            builder.setPositiveButton("Please select destination",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });
        }
        builder.show();
    }


    //Route added from start and end point of the selected markers.
    public void addRoute() {
//        clearMap();

        click = 0;
        startGeoCoordinates =  new GeoCoordinates(startLatitude,startLongitude);
        destinationGeoCoordinates = new GeoCoordinates(endLatitude,endLongitude);
        Waypoint startWaypoint = new Waypoint(startGeoCoordinates);
        Waypoint destinationWaypoint = new Waypoint(destinationGeoCoordinates);

        List<Waypoint> waypoints =
                new ArrayList<>(Arrays.asList(startWaypoint, destinationWaypoint));

        routingEngine.calculateRoute(
                waypoints,
                new CarOptions(),
                new CalculateRouteCallback() {
                    @Override
                    public void onRouteCalculated(@Nullable RoutingError routingError, @Nullable List<Route> routes) {
                        if (routingError == null) {
                            Route route = routes.get(0);

                            showRouteOnMap(route);
                        } else {
//                            showDialog("Error while calculating a route:", routingError.toString());
                        }
                    }
                });
    }


//    Showing routes on the map.
    private void showRouteOnMap(Route route) {
        // Show route as polyline.
        GeoPolyline routeGeoPolyline;
        try {
            routeGeoPolyline = new GeoPolyline(route.getPolyline());
        } catch (InstantiationErrorException e) {
            // It should never happen that the route polyline contains less than two vertices.
            return;
        }
        MapPolylineStyle mapPolylineStyle = new MapPolylineStyle();
        mapPolylineStyle.setColor(0x00908AA0, PixelFormat.RGBA_8888);
        mapPolylineStyle.setWidthInPixels(10);
        MapPolyline routeMapPolyline = new MapPolyline(routeGeoPolyline, mapPolylineStyle);
        mapView.getMapScene().addMapPolyline(routeMapPolyline);
        mapPolylines.add(routeMapPolyline);

        // Draw a circle to indicate starting point and destination.
        addCircleMapMarker(startGeoCoordinates, R.drawable.green_dot);
        addCircleMapMarker(destinationGeoCoordinates, R.drawable.green_dot);

        // Log maneuver instructions per route section.
//        List<Section> sections = route.getSections();
//        for (Section section : sections) {
//            logManeuverInstructions(section);
//        }
    }

//    Adding circle to the selected routes on the map.
    private void addCircleMapMarker(GeoCoordinates geoCoordinates, int resourceId) {
        MapImage mapImage = MapImageFactory.fromResource(context.getResources(), resourceId);
        MapMarker mapMarker = new MapMarker(geoCoordinates);
        mapMarker.addImage(mapImage, new MapMarkerImageStyle());
        mapView.getMapScene().addMapMarker(mapMarker);
        mapMarkerList.add(mapMarker);
    }



}
