package edu.berkeley.eecs.emission.opcodeauth;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.gms.common.api.ResultCallback;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ReactModule(name = OPCodeAuthModule.TAG)
class OPCodeAuthModule extends ReactContextBaseJavaModule {
  public static final String TAG = "OPCodeAuth";

  private Context ctxt;

  public OPCodeAuthModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.ctxt = reactContext.getApplicationContext();
  }

  @Override
  @NonNull
  public String getName() {
    return TAG;
  }

  @ReactMethod
  public void getOPCode(Promise promise) {
    try {
      AuthTokenCreator tokenCreator = AuthTokenCreationFactory.getInstance(ctxt);
      AuthPendingResult result = tokenCreator.getOPCode();
      result.setResultCallback(new ResultCallback<AuthResult>() {
        @Override
        public void onResult(@NonNull AuthResult authResult) {
          if (authResult.getStatus().isSuccess()) {
            promise.resolve(authResult.getOPCode());
          } else {
            promise.reject(authResult.getStatus().getStatusCode() + " : "
                + authResult.getStatus().getStatusMessage());
          }
        }
      });
    } catch (Exception e) {
      promise.reject(e);
    }
  }

  @ReactMethod
  public void setOPCode(String opcode, Promise promise) {
    try {
      AuthTokenCreator tokenCreator = AuthTokenCreationFactory.getInstance(ctxt);
      tokenCreator.setOPCode(opcode);
      promise.resolve(opcode);
    } catch (Exception e) {
      promise.reject(e);
    }
  }
}

public class OPCodeAuthPackage implements ReactPackage {
  @NonNull
  @Override
  public List<NativeModule> createNativeModules(@NonNull ReactApplicationContext reactContext) {
    List<NativeModule> modules = new ArrayList<>();
    modules.add(new OPCodeAuthModule(reactContext));
    return modules;
  }

  @NonNull
  @Override
  public List<ViewManager> createViewManagers(@NonNull ReactApplicationContext reactContext) {
    return Collections.emptyList();
  }
}
