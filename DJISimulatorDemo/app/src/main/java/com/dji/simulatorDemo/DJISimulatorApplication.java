package com.dji.simulatorDemo;

import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class DJISimulatorApplication extends Application {

    private static final String TAG = DJISimulatorApplication.class.getName();

    public static final String FLAG_CONNECTION_CHANGE = "com_dji_simulatorDemo_connection_change";

    private static BaseProduct mProduct;

    private Handler mHandler;

    /**
     * Gets instance of the specific product connected after the
     * API KEY is successfully validated. Please make sure the
     * API_KEY has been added in the Manifest
     */
    public static synchronized BaseProduct getProductInstance() {
        if (null == mProduct) {
            mProduct = DJISDKManager.getInstance().getProduct();
        }
        return mProduct;
    }

    public static boolean isAircraftConnected() {
        return getProductInstance() != null && getProductInstance() instanceof Aircraft;
    }

    public static synchronized Aircraft getAircraftInstance() {
        if (!isAircraftConnected()) return null;
        return (Aircraft) getProductInstance();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new Handler(Looper.getMainLooper());

        //Check the permissions before registering the application for android system 6.0 above.
        int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionCheck2 = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (permissionCheck == 0 && permissionCheck2 == 0)) {

            //This is used to start SDK services and initiate SDK.
            DJISDKManager.getInstance().registerApp(this, mDJISDKManagerCallback);
        } else {
            Toast.makeText(getApplicationContext(), "Please check if the permission is granted.", Toast.LENGTH_LONG).show();
        }

    }

    private DJISDKManager.SDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.SDKManagerCallback() {

        @Override
        public void onRegister(DJIError error) {

            Handler handler = new Handler(Looper.getMainLooper());

            if(error == DJISDKError.REGISTRATION_SUCCESS) {

                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                R.string.success,
                                Toast.LENGTH_LONG).show();
                    }
                });

                DJISDKManager.getInstance().startConnectionToProduct();
            } else {
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                R.string.sdk_registration_message,
                                Toast.LENGTH_LONG).show();
                    }
                });

            }
            Log.v(TAG, error.getDescription());
        }

        @Override
        public void onProductChange(BaseProduct oldProduct, BaseProduct newProduct) {

            Log.v(TAG, String.format("onProductChanged oldProduct:%s, newProduct:%s", oldProduct, newProduct));
            mProduct = newProduct;
            if(mProduct != null) {
                mProduct.setBaseProductListener(mDJIBaseProductListener);
            }

            notifyStatusChange();
        }

        private BaseProduct.BaseProductListener mDJIBaseProductListener = new BaseProduct.BaseProductListener() {

            @Override
            public void onComponentChange(BaseProduct.ComponentKey key, BaseComponent oldComponent, BaseComponent newComponent) {

                if(newComponent != null) {
                    newComponent.setComponentListener(mDJIComponentListener);
                }
                Log.v(TAG, String.format("onComponentChange key:%s, oldComponent:%s, newComponent:%s", key, oldComponent, newComponent));

                notifyStatusChange();
            }

            @Override
            public void onConnectivityChange(boolean isConnected) {

                Log.v(TAG, "onConnectivityChange: " + isConnected);

                notifyStatusChange();
            }

        };

        private BaseComponent.ComponentListener mDJIComponentListener = new BaseComponent.ComponentListener() {

            @Override
            public void onConnectivityChange(boolean isConnected) {
                notifyStatusChange();
            }

        };

        private void notifyStatusChange() {
            mHandler.removeCallbacks(updateRunnable);
            mHandler.postDelayed(updateRunnable, 500);
        }

        private Runnable updateRunnable = new Runnable() {

            @Override
            public void run() {
                Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
                sendBroadcast(intent);
            }
        };
    };

}
